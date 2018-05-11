/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

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
final class GenericGLBProcessor<R extends Result<R> & Serializable>
    extends PlaceLocalObject implements GLBProcessor<R> {

  /** Collection of tasks bags to be processed */
  private final ConcurrentBagQueue<R> bagsToDo;

  /**
   * Indicates if the {@link #folds} member is the folded result of all the
   * places or not. Only useful for place 0.
   */
  private boolean foldCompleted = false;

  /**
   * R instance local to this place, contains the result gathered from the
   * {@link Bag}s completed at this local place.
   */
  private R result;

  /** Brings the APGAS place id to the class {@link LoopGLBProcessor} */
  private final Place home = here();

  /**
   * id's of the places which are susceptible to establish their lifeline on
   * this place
   */
  private final int incomingLifelines[];

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
    result = null;
  }

  /**
   * Yields back some work in response to a {@link #steal()}
   * <p>
   * Merges the proposed {@link Bag} {@code gift} into this place's
   * corresponding type {@link Bag} if any and puts it into {@link #bagsToDo}
   * before waking up the waiting thread in the {@link #steal()} procedure. This
   * will in turn make this place check its {@link #bagsToDo} for any work in
   * the {@link #run()} method and either process the given work or switch to
   * the lifeline steal scheme.
   *
   * @param <B>
   *          the type of gift given
   * @param p
   *          the place where the work is originating from
   * @param gift
   *          the work given by place {@code p}, possibly <code>null</code>.
   */
  private synchronized <B extends Bag<B, R> & Serializable> void deal(Place p,
      B gift) {
    // We are presumably receiving work from place p. Therefore this place
    // should be in state 'p'.
    assert state == p.id;
    // If place p couldn't share work with this place, the given q is null. A
    // check is therefore necessary.
    if (gift != null) {
      bagsToDo.giveBag(gift);
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
   *
   * @param <B>
   *          type of offered work given to thieves
   */
  private <B extends Bag<B, R> & Serializable> void distribute() {
    if (places == 1) {
      return;
    }
    final Place h = home;
    Place p;
    while ((p = thieves.poll()) != null) {

      final B toGive = bagsToDo.split();
      System.err.println(p + " stole from " + home);
      uncountedAsyncAt(p, () -> {
        deal(h, toGive);
      });
    }

    while ((p = lifelineThieves.poll()) != null) {
      final B toGive = bagsToDo.split();
      if (toGive != null) {
        asyncAt(p, () -> {
          lifelineDeal(toGive);
        });
      } else {
        lifelineThieves.add(p);
        return;
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
  private <F extends Result<F> & Serializable> void gather() {
    final R res = bagsToDo.result();
    if (res != null) {
      if (home.id != 0) {
        asyncAt(place(0), () -> {
          giveResult(res);
        });
      } else {
        giveResult(res);
      }
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
  private <B extends Bag<B, R> & Serializable> void lifelineDeal(B q) {
    bagsToDo.giveBag(q);
    System.err.println(home + " received work");

    boolean toLaunch = false;
    synchronized (this) {
      if (state == -2) {
        state = -1;
        toLaunch = true;
      }
    }
    if (toLaunch) {
      run();
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

    for (;;) { // Is correct, loop is exited thanks to a break
      while (!bagsToDo.isEmpty()) {

        bagsToDo.process(WORK_UNIT);

        distribute();
      }

      // Perform steals attempts
      int attempts = RANDOM_STEAL_ATTEMPTS;
      while (attempts > 0 && bagsToDo.isEmpty()) {
        attempts--;
        steal();
      }

      // Synchronized block for possible state change.
      // Necessary because of the lifelineDeal method.
      synchronized (this) {
        if (bagsToDo.isEmpty()) {
          state = -2;
          break;
        }
      }
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
   * Merges the given Result into this instance folds, ensuring mutual exclusion
   *
   * @param <F>
   *          type parameter
   * @param fold
   *          the fold to be merged into this place
   */
  private synchronized void giveResult(R res) {
    if (result == null) {
      result = res;
    } else {
      result.fold(res);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#addWork(apgas.glb.Bag)
   */
  @Override
  public <B extends Bag<B, R> & Serializable> void addBag(B bag) {
    bagsToDo.giveBag(bag);
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
   * Gives back the {@link Result} that were computed during the previous
   * computation. Method {@link #compute()} should be called before to ensure
   * the computation is actually performed.
   *
   * @return a collection containing all the {@link Result} known to the
   *         LoopGLBProcessor, every instance being from a different class
   */
  @Override
  public R result() {
    if (!foldCompleted) {
      finish(() -> {
        for (final Place p : places()) {
          asyncAt(p, () -> gather());
        }
      });
      foldCompleted = true;
    }
    return result;
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
    bagsToDo = new ConcurrentBagQueue<>();

    incomingLifelines = s.reverseLifeline(home.id, places);
    lifelines = s.lifeline(home.id, places);
    for (final int i : incomingLifelines) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
  }
}
