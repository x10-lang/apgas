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
 * LoopGLBProcessor proposes a simple API to request for work to be computed
 * using the lifeline based global load balancing framework proposed by APGAS.
 * <p>
 * Initial {@link Bag}s to be processed can be added to this instance by calling
 * {@link #addTaskBag(Bag)}. Computation is launched using the
 * {@link #compute()} method. If the programmer wishes to use same computing
 * instance for several successive calculation, method {@link #reset()} should
 * be called before adding the new {@link Bag}s to be processed.
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

  /** Collection of folds handled by this computation place */
  @SuppressWarnings("rawtypes")
  private final Map<String, Fold> folds;

  /** Brings the APGAS place id to the class {@link GenericGLBProcessor} */
  private final Place home = here();

  /** Number of places available for the computation */
  private final int places = places().size();

  /**
   * Array of int containing the id's of the places on which lifelines can be
   * established
   */
  private final int lifelines[];

  /**
   * Random generator used when thieving a random place
   * <p>
   * By initializing the seed with the place id (different from all the other
   * places), we avoid having the same sequence of places to thieve from for all
   * the places.
   */
  private final Random random = new Random(home.id);

  /**
   * List of thieves that asked for work when the current place was performing
   * computation
   */
  private final ConcurrentLinkedQueue<Place> randomThieves = new ConcurrentLinkedQueue<>();

  /**
   * List of the lifeline thieves that have quiesced and are waiting to be waken
   * up
   *
   * @see #lifelineSteal()
   * @see #lifelineReply(Place)
   * @see #lifelineSend(Place)
   * @see #lifelineDeal(Bag)
   */
  private final ConcurrentLinkedQueue<Place> lifelineThieves = new ConcurrentLinkedQueue<>();

  /** Number of random steal attempts performed by this place */
  private final int RANDOM_STEAL_ATTEMPTS;

  /** Number of task processed by this place before dealing with thieves */
  private final int WORK_UNIT;

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

  /** Lifeline strategy instance */
  private final LifelineStrategy lifelineStrategy;

  private final Semaphore lifelineSem = new Semaphore(0);

  /**
   * Puts this local place to a ready to compute state.
   */
  private void clear() {
    randomThieves.clear();
    state = -2;
    bagsToDo.clear();
    bagsDone.clear();
    folds.clear();
    for (final int i : lifelineStrategy.reverseLifeline(home.id, places)) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }

    lifelineSem.drainPermits();
    if (home.id != 0) {
      lifelineSem.release();
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
   * random thieves before {@link GenericGLBProcessor#lifelinedeal(Bag)}ing with
   * the lifeline thieves.
   *
   * @param <B>the
   *          type of offered work given to thieves
   */
  private <B extends Bag<B> & Serializable> void distribute() {
    if (places == 1) {
      return;
    }
    Place p;
    while ((p = randomThieves.poll()) != null) {
      final String key = bagsToDo.keySet().iterator().next();
      final Bag<?> bag = bagsToDo.get(key);
      @SuppressWarnings("unchecked")
      final B toGive = (B) bag.split();
      final Place h = home;
      uncountedAsyncAt(p, () -> {
        deal(h, toGive);
      });
    }
    while (!bagsToDo.isEmpty() && (p = lifelineThieves.poll()) != null) {
      // TODO Check if there is any work available ?
      final Place h = home;
      final Place d = p;
      finish(() -> asyncAt(d, () -> {
        lifelineReply(h);
      }));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.WorkCollector#fold(apgas.glb.Fold)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized <F extends Fold<F> & Serializable> void giveFold(F fold) {
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
   * Folds all this instance folds with that of place 0 before clearing them.
   * Should not be called if this instance is place 0.
   *
   * @param <F>
   *          the type of the folds to be sent
   */
  @SuppressWarnings("unchecked")
  private <F extends Fold<F> & Serializable> void gather() {
    for (final Fold<?> f : folds.values()) {
      asyncAt(place(0), () -> giveFold((F) f));
    }
    folds.clear();
  }

  /**
   * Wakes up a place waiting for work on its lifeline, giving it some work
   * {@code a} on the fly in response to a {@link #lifelinesteal()}
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
  private synchronized <B extends Bag<B> & Serializable> void lifelineDeal(
      B q) {
    if (q != null) {
      final B d = (B) bagsDone.remove(q.getClass().getName()); // Possibly null
      if (d != null) {
        q.merge(d);
      }
      q.setWorkCollector(this);
      bagsToDo.put(q.getClass().getName(), q);
    }
    run();
  }

  /**
   * Method to call to reply to a lifeline thief. If the caller is the first to
   * reply, calls the {@link #lifelineSend} method to transmit the work to
   * share. Also removes the lifeline established on other places.
   * <p>
   * If an other place manages to call this method before its lifeline is
   * removed, will return immediately thanks to a non blocking semaphore.
   *
   * @param answer
   *          the place willing to give work to this place
   */
  private void lifelineReply(Place answer) {
    if (lifelineSem.tryAcquire()) {
      final Place h = home;
      asyncAt(answer, () -> {
        lifelineSend(h);
      });
      for (final int i : lifelineStrategy.reverseLifeline(home.id, places)) {
        if (i != answer.id) {
          uncountedAsyncAt(place(i), () -> {
            lifelineThieves.remove(h);
          });
        }
      }
    }
  }

  /**
   * Splits this place bag, gives the work to the place given as parameter and
   * launches its computation again.
   *
   * @param destination
   *          the destination of the work to be given
   */
  private <B extends Bag<B> & Serializable> void lifelineSend(
      Place destination) {
    final String key = bagsToDo.keySet().iterator().next();
    final Bag<?> bag = bagsToDo.get(key);
    @SuppressWarnings("unchecked")
    final B toGive = (B) bag.split();

    asyncAt(destination, () -> {
      lifelineDeal(toGive);
    });
  }

  /**
   * Registers this {@code LoopGLBProcessor} as asking for work from its
   * (remote) lifeline {@code LoopGLBProcessor} place. If there is only one
   * place, has no effect.
   * <p>
   * The lifeline strategy in the current implementation consists in a single
   * directed loop including all the nodes. That is :
   * <ul>
   * <li>place <em>n</em> connects with place <em>n-1</em></li>
   * <li>place <em>0</em> connects with <em>{@code places} - 1</em></li>
   * </ul>
   * Therefore if a simple boolean value is enough to signal the fact a place is
   * asking for work as for each place there is a unique potential thief.
   *
   * @see #lifeline
   */
  private void lifelineSteal() {
    if (places == 1) {
      // No other place exists, "this" is the only place.
      // Impossible to perform a steal.
      return;
    }

    // Sets lifeline
    final Place h = home;
    for (final int i : lifelines) {
      asyncAt(place(i), () -> {
        lifelineThieves.add(h);
      });
    }
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
        randomThieves.add(p);
        return;
      }
    }
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
   * {@link #run()} method on this place via {@link #lifelinedeal(Bag)}
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
    while ((p = randomThieves.poll()) != null) {
      uncountedAsyncAt(p, () -> {
        deal(h, null);
      });
    }

    // Folding this instance's folds into that of place 0
    if (home.id != 0) {
      gather();
    }

    // Establishing lifeline
    lifelineSem.release();
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

  /** Launches the computation of the given work */
  @Override
  public void compute() {
    finish(() -> {
      run();
    });
  }

  /**
   * Clears the {@link GenericGLBProcessor} of all its tasks and results and
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
   */
  GenericGLBProcessor(int workUnit, int randomStealAttempts,
      LifelineStrategy strategy) {
    WORK_UNIT = workUnit;
    RANDOM_STEAL_ATTEMPTS = randomStealAttempts;
    bagsToDo = new HashMap<>();
    bagsDone = new HashMap<>();
    folds = new HashMap<>();
    lifelines = strategy.lifeline(home.id, places);
    lifelineStrategy = strategy;
    clear();
  }
}
