/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import apgas.Place;
import apgas.util.PlaceLocalObject;

/**
 * GenericGLBProcessor proposes a simple API to request for work to be computed
 * using the lifeline based global load balancing framework proposed by APGAS.
 * <p>
 * From the user perspective, this is an implementation of {@link GLBProcessor}
 * like {@link LoopGLBProcessor}. However, unlike {@link LoopGLBProcessor}, this
 * implementation can handle any kind {@link LifelineStrategy}. The desired
 * {@link LifelineStrategy} to be used is specefied as a constructor's
 * parameter.
 *
 * @author Patrick Finnerty
 *
 */
final class GenericGLBProcessor extends PlaceLocalObject
    implements WorkCollector, GLBProcessor {

  /** Collection of tasks bags to be processed */
  @SuppressWarnings("rawtypes")
  private final Map<String, Bag> bagsToDo;

  /** Collection of tasks bags that have been processed */
  @SuppressWarnings("rawtypes")
  private final Map<String, Bag> bagsDone;

  /**
   * Indicates if the {@link #folds} member is the folded result of all the
   * places or not. Only useful for place 0.
   */
  private boolean foldCompleted = false;

  /** Collection of folds handled by this computation place */
  @SuppressWarnings("rawtypes")
  private final Map<String, Fold> folds;

  /** Brings the APGAS place id to the class {@link LoopGLBProcessor} */
  private final Place home = here();

  /**
   * id's of the places which are susceptible to establish their lifeline on
   * this place
   */
  private final int incomingLifelines[];

  /**
   * Semaphore used to lift conflict if several lifelines answer this place at
   * the same time. Before establishing its lifelines, a ticket is given to the
   * semaphore. When lifelines try to give some work by calling lifelineReply
   * asynchroneously, they try to acquire it. If successful work will be given,
   * if not, no work is given (an other lifeline was quicker to reply).
   *
   * @see #lifelineReply(Place)
   */
  private final Semaphore lifelineAnswer = new Semaphore(0, false);

  /** id's of the places on which this place will establish its lifelines */
  private final int lifelines[];

  /** List of lifeline thieves waiting for work */
  private final ConcurrentLinkedQueue<Place> lifelineThieves = new ConcurrentLinkedQueue<>();

  /** Number of places available for the computation */
  private final int places = places().size();

  /**
   * Random generator used when thieving a random place
   * <p>
   * By initializing the seed with the place id (different from all the other
   * places), we avoid having the same sequence of places to thieve from for all
   * the places.
   */
  private final Random random = new Random(home.id);

  /** Number of random steal attempts performed by this place */
  private final int RANDOM_STEAL_ATTEMPTS;

  /**
   * Indicates the state of the place at any given time.
   * <ul>
   * <li><em>-2</em> : inactive
   * <li><em>-1</em> : running
   * <li><em>p</em> in range [0,{@code places}] : stealing from place of id
   * <em>p</em>
   * </ul>
   * At initialization, is in inactive state. Due to race conditions, it has to
   * be protected at each read/write.
   */
  private int state = -2;

  /**
   * List of thieves that asked for work when the current place was performing
   * computation
   */
  private final ConcurrentLinkedQueue<Place> thieves = new ConcurrentLinkedQueue<>();

  /**
   * Flag indicating if a work split is in progress. Used as safeguard for the
   * {@link #wait()} call in the {@link #distribute()} method.
   */
  private boolean workSplit = false;

  /** Number of task processed by this place before dealing with thieves */
  private final int WORK_UNIT;

  /**
   * Puts this local place into a ready to compute state.
   */
  private void clear() {
    thieves.clear();
    lifelineThieves.clear();
    for (final int i : incomingLifelines) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
    state = -2;
    bagsToDo.clear();
    bagsDone.clear();
    folds.clear();
    workSplit = false;
    lifelineAnswer.drainPermits();
    if (home.id != 0) {
      lifelineAnswer.release();
    }
  }

  /**
   * Yields back some work in response to a {@link #steal()}
   * <p>
   * Merges the proposed {@link Bag} {@code gift} into this place's
   * corresponding type {@link Bag} if any and puts it into {@link #bagsToDo}
   * before waking up the waiting thread in the {@link #steal()} procedure. This
   * will in turn make this place check its {@link #bagsToDo} for any work and
   * either process the given work or switch to the lifeline steal procedure.
   *
   * @param <B>
   *          the type of gift given
   * @param p
   *          the place where the work is originating from
   * @param gift
   *          the work given by place {@code p}, possibly <code>null</code>.
   */
  @SuppressWarnings("unchecked")
  private synchronized <B extends Bag<B> & Serializable> void deal(Place p,
      B gift) {
    // We are presumably receiving work from place p. Therefore this place
    // should be in state 'p'.
    assert state == p.id;
    // If place p couldn't share work with this place, the given q is null. A
    // check is therefore necessary.
    if (gift != null) {
      B d = (B) bagsDone.remove(gift.getClass().getName()); // Possibly
                                                            // null
      if (d != null) {
        d.merge(gift);
      } else {
        d = gift;
      }
      d.setWorkCollector(this);

      bagsToDo.put(gift.getClass().getName(), d);
    }
    // Switch back to 'running' state.
    state = -1;
    // Wakes up the halted thread in 'steal' procedure.
    notifyAll();
  }

  /**
   * Distributes {@link Bag}s to the random thieves and the lifeline thieves
   * asking for work from this place.
   * <p>
   * Splits this place's {@link #bagsToDo} and {@link #deal(Place, Bag)}s with
   * random thieves before taking care of the lifeline thieves.
   * <p>
   * When making a deal with a lifeline thief, the progress in this method is
   * stopped until either the deal is made ({@link #lifelineSend(Place)} or this
   * place is rejected by its lifeline in the {@link #lifelineReply(Place)}.
   *
   * @param <B>
   *          type of offered work given to thieves
   */
  private <B extends Bag<B> & Serializable> void distribute() {
    if (places == 1 || bagsToDo.isEmpty()) {
      return;
    }

    final Place h = home;
    Place p;
    while ((p = thieves.poll()) != null) {
      final String key = bagsToDo.keySet().iterator().next();
      final Bag<?> bag = bagsToDo.get(key);
      @SuppressWarnings("unchecked")
      final B toGive = (B) bag.split();
      System.err.println(p + " stole from " + home);
      uncountedAsyncAt(p, () -> {
        deal(h, toGive);
      });
    }

    while ((p = lifelineThieves.poll()) != null) {
      workSplit = true; // Will be set back to false by either mehtod
                        // lifelineReply or lifelineSend
      asyncAt(p, () -> {
        lifelineReply(h);
      });

      // Synchronized necessary because of the wait call
      synchronized (this) {
        while (workSplit) {
          try {
            wait();
          } catch (final InterruptedException e) {
          }
        }
      }
    }

  }

  /**
   * Folds all this instance folds with that of place 0 before clearing them.
   * Should not be called if this instance is place 0.
   *
   * @param <F>
   *          the type of the folds to be sent
   */
  @SuppressWarnings("unchecked")
  private <F extends Fold<F> & Serializable> void gather() {
    for (final Fold<?> f : folds.values()) {
      asyncAt(place(0), () -> synchronizedGiveFold((F) f));
    }
    folds.clear();
  }

  /**
   * Method to be called by lifelines when willing to offer some work to this
   * place.
   * <p>
   * The main objective of this method is to lift conflicts between lifelines
   * potentially answering at the same time. This is done by using the
   * non-blocking Semaphore {@link #lifelineAnswer}. Only the place that manages
   * to take the only permit will give work to this place, the others are
   * rejected.
   * <p>
   * In practice, conflicts do not happen very often as the lifelines of this
   * place are disabled by the first (successful) call to this method.
   *
   * @param answer
   *          the place offering some work.
   */
  private void lifelineReply(Place answer) {
    final Place h = home;
    if (lifelineAnswer.tryAcquire()) {
      System.err.println(answer + " will give to " + h);
      asyncAt(answer, () -> {
        lifelineSend(h);
      });

      // Removing other lifelines
      for (final int i : lifelines) {
        if (i != answer.id) {
          System.err
              .println(home + " cancels its lifeline on place(" + i + ")");
          uncountedAsyncAt(place(i), () -> {
            lifelineThieves.remove(h);
          });
        }
      }

    } else {
      // A lifeline made an answer but was not first, we unlock its progress.
      System.err.println(answer + " is turned down by " + home);
      asyncAt(answer, () -> {
        workSplit = false;
        synchronized (this) {
          notifyAll();
        }
      });
    }

  }

  /**
   * Sends work to the specified place and wakes it up. Also unblocks the
   * progress of this place in the {@link #distribute()} method.
   *
   * @param <B>
   *          type of Bag
   *
   * @param destination
   *          the place to which work is to be given.
   */
  private <B extends Bag<B> & Serializable> void lifelineSend(
      Place destination) {
    System.err.println(home + " sending to " + destination);
    final String key = bagsToDo.keySet().iterator().next();
    final Bag<?> bag = bagsToDo.get(key);
    @SuppressWarnings("unchecked")
    final B toGive = (B) bag.split();
    asyncAt(destination, () -> {
      lifelineDeal(toGive);
    });
    workSplit = false;
    synchronized (this) {
      notifyAll();
    }
  }

  /**
   * Registers this {@link GenericGLBProcessor} as asking for work from its
   * (remote) lifeline places. If there is only one place, has no effect.
   *
   * @see #lifelineThieves
   */
  private void lifelineSteal() {
    if (places == 1) {
      // No other place exists, "this" is the only place.
      // Impossible to perform a steal.
      return;
    }
    // Sets lifeline on the place bearing id `home.id - 1` or `places - 1` if
    // this place's is 0.
    // The resulting lifeline graph is then a directed loop :
    // 0 <- 1 <- 2 <- ... <- (places-1) <- (places) <- 0
    // The work will be given (if ever) when the target place performs its
    // distribute routine.
    final Place h = home;
    for (final int i : lifelines) {
      System.err.println(home + " set lifeline on place(" + i + ")");
      asyncAt(place(i), () -> {
        lifelineThieves.add(h);
      });
    }
  }

  /**
   * Wakes up a place waiting for work on its lifeline, giving it some work
   * {@code a} on the fly in response to a {@link #lifelineSteal()}
   * <p>
   * Only makes sense if this place is in inactive {@link #state}.
   *
   * @param <B>the
   *          type of the given {@link Bag}
   *
   * @param q
   *          the work to be given to the place
   */
  @SuppressWarnings("unchecked")
  private <B extends Bag<B> & Serializable> void lifelineDeal(B q) {
    if (q != null) {
      final B d = (B) bagsDone.remove(q.getClass().getName()); // Possibly null
      if (d != null) {
        q.merge(d);
      }
      q.setWorkCollector(this);
      bagsToDo.put(q.getClass().getName(), q);
    }
    System.err.println(home + " work received");
    run();
  }

  /**
   * Method used to signal the fact place {@code p} is requesting work from this
   * place.
   * <p>
   * If this place is currently working, adds the thief to its {@link #thieves}
   * queue which will be processed when it performs a certain number of
   * iterations. If this place is not working, i.e. is trying to steal work
   * (randomly or through a lifeline), asynchronously {@link #deal(Place, Bag)}
   * {@code null} work.
   *
   * @param p
   *          The place asking for work
   */
  private void request(Place p) {
    synchronized (this) {
      // If the place is currently performing computation, adds the thief to its
      // list of pending thief.
      // The work will be shared when this place stops processing its tasks in
      // the main 'run' loop by the first 'distribute' call.
      if (state == -1) {
        System.err.println(p + " waiting for " + home);
        thieves.add(p);
        return;
      }
    }
    System.err.println(p + " could not steal from " + home);
    final Place h = home;
    uncountedAsyncAt(p, () -> {
      deal(h, null);
    });
  }

  /**
   * Main computation procedure.
   * <p>
   * Processes WORK_UNIT's worth of task in its bagsToDo before answering to
   * potential thief (method {@link #distribute()}. When it runs out of task
   * bags to process, attempts a certain number of steals on other workers
   * (method {@link #request(Place)}). If successfull in its steals, processes
   * the given tasks. If not answers to thieves that might have had the time to
   * put in a request between two attempted steals before establishing its
   * lifeline and stopping. This procedure can be called again if the lifeline
   * steal is successful : the place offering the work will call the
   * {@link #run()} method on this place via {@link #lifelineDeal(Bag)}
   */
  private void run() {
    System.err.println(home + " starting");

    synchronized (this) {
      state = -1;
    }
    while (!bagsToDo.isEmpty()) {
      while (!bagsToDo.isEmpty()) {
        final String key = bagsToDo.keySet().iterator().next();
        final Bag<?> bag = bagsToDo.get(key);

        bag.process(WORK_UNIT);

        if (bag.isEmpty()) {
          bagsToDo.remove(key);
          bagsDone.put(key, bag);
          System.err.println(home + " has no more work");
        }
        distribute();
      }

      // Perform steals attempts
      int attempts = RANDOM_STEAL_ATTEMPTS;
      while (attempts > 0 && bagsToDo.isEmpty()) {
        attempts--;
        steal();
      }
    }

    synchronized (this) {
      state = -2;
    }

    // Sending null work to all the thieves
    Place p;
    final Place h = home;
    while ((p = thieves.poll()) != null) {
      uncountedAsyncAt(p, () -> {
        deal(h, null);
      });
    }

    // Establishing lifeline
    lifelineAnswer.release();
    lifelineSteal();
    System.err.println(home + " stopping");
  }

  /**
   * Attempts to steal work to a randomly chosen place. Will halt the process
   * until the target place answers (whether it indeed gave work or not).
   */
  private void steal() {
    if (places == 1) {
      // No other place exists, "this" is the only one.
      // Cannot perform a steal.
      return;
    }
    final Place h = home;

    // Ask to a random place
    int p = random.nextInt(places - 1);
    if (p >= h.id) {
      // We cannot thief on ourselves. Moreover the generated random integer has
      // a range of `places - 1`. By incrementing p we keep an uniform
      // distribution for the target place.
      p++;
    }

    // Change state to 'p', i.e. thieving from p before requesting work from it.
    synchronized (this) {
      state = p;
    }

    // Calls "request" at place p, passing itself as parameter
    // The call is 'uncounted' as this Async is about program "logistics" and
    // does not intervene in the enclosing "finish" construct
    uncountedAsyncAt(place(p), () -> {
      request(h);
    });

    synchronized (this) {
      while (state >= 0) {
        try {
          wait();
        } catch (final InterruptedException e) {
        }
      }
    }
  }

  /**
   * Merges the given Fold into this instance folds, ensuring mutual exclusion
   *
   * @param <F>
   *          type parameter
   * @param fold
   *          the fold to be merged into this place
   */
  private <F extends Fold<F> & Serializable> void synchronizedGiveFold(F fold) {
    synchronized (this) {
      giveFold(fold);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#addWork(apgas.glb.Bag)
   */
  @Override
  public <B extends Bag<B> & Serializable> void addBag(B bag) {
    bag.setWorkCollector(this);
    bagsToDo.put(bag.getClass().getName(), bag);
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#compute()
   */
  @Override
  public void compute() {
    foldCompleted = false;
    finish(() -> {
      run();
    });
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.WorkCollector#giveBag(apgas.glb.Bag)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <B extends Bag<B> & Serializable> void giveBag(B b) {
    final B done = (B) bagsToDo.remove(b.getClass().getName());
    if (done != null) {
      b.merge(done);
    }
    final B toDo = (B) bagsDone.remove(b.getClass().getName());
    if (toDo != null) {
      b.merge(toDo);
    }
    b.setWorkCollector(this);
    bagsToDo.put(b.getClass().getName(), b);
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.WorkCollector#fold(apgas.glb.Fold)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <F extends Fold<F> & Serializable> void giveFold(F fold) {
    /*
     * This method needs to be synchronized since distant places are suceptible
     * to call it when sending their results before quiescing.
     */

    final String key = fold.getClass().getName();
    final F existing = (F) folds.get(key);

    if (existing != null) {
      existing.fold(fold);
    } else {
      folds.put(key, fold);
    }
  }

  /**
   * Clears the {@link LoopGLBProcessor} of all its tasks and results and
   * prepares it for a new computation.
   */
  @Override
  public void reset() {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> clear());
      }
    });
  }

  /**
   * Gives back the {@link Fold} that were computed during the previous
   * computation. Method {@link #compute()} should be called before to ensure
   * the computation is actually performed.
   *
   * @return a collection containing all the {@link Fold} known to the
   *         LoopGLBProcessor, every instance being from a different class
   */
  @Override
  @SuppressWarnings("rawtypes")
  public Collection<Fold> result() {
    if (!foldCompleted) {

      finish(() -> {
        for (final Place p : places()) {
          // Folding this instance's folds into that of place 0
          if (p.id != 0) {
            asyncAt(p, () -> gather());
          }
        }
      });
      foldCompleted = true;
    }
    return folds.values();
  }

  /**
   * Private Constructor
   *
   * @param workUnit
   *          the amount of work to be processed before tending to thieves
   * @param randomStealAttempts
   *          number of random steals attempts before resaulting to the lifeline
   *          thief scheme
   * @param s
   *          {@link LifelineStrategy} to be followed
   */
  GenericGLBProcessor(int workUnit, int randomStealAttempts,
      LifelineStrategy s) {
    WORK_UNIT = workUnit;
    RANDOM_STEAL_ATTEMPTS = randomStealAttempts;
    bagsToDo = new HashMap<>();
    bagsDone = new HashMap<>();
    folds = new HashMap<>();
    incomingLifelines = s.reverseLifeline(home.id, places);
    lifelines = s.lifeline(home.id, places);
    for (final int i : incomingLifelines) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
    if (home.id != 0) {
      lifelineAnswer.release();
    }
  }
}
