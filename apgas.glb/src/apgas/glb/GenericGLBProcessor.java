/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import apgas.Place;
import apgas.util.PlaceLocalObject;

/**
 * GenericGLBProcessor proposes a simple API to request for work to be computed
 * using the lifeline based global load balancing framework relying on APGAS.
 * <p>
 * This is an implementation of {@link GLBProcessor} like
 * {@link LoopGLBProcessor}. However, unlike {@link LoopGLBProcessor}, this
 * implementation can handle any kind of lifeline strategy. We propose an
 * hypercube lifeline strategy implementation (class {@link HypercubeStrategy})
 * but programmers can define their own strategies by implementing interface
 * {@link LifelineStrategy}. The desired {@link LifelineStrategy} to be used has
 * to be specified in the constructor.
 *
 * @author Patrick Finnerty
 * @see HypercubeStrategy
 * @see LoopGLBProcessor
 */
final class GenericGLBProcessor extends PlaceLocalObject
    implements GLBProcessor {

  /** Collection of tasks bags to be processed */
  @SuppressWarnings("rawtypes")
  private ConcurrentBagQueue bagsToDo;

  /**
   * Result instance local to this place. Used to gather the results from the
   * {@link Bag}s processed by this place contained in {@link #bagsToDo}.
   */
  @SuppressWarnings("rawtypes")
  private Fold result;

  /** Brings the APGAS place id to the class {@link LoopGLBProcessor} */
  private final Place home = here();

  /**
   * Integer ({@code int}) id's of the places which are susceptible to establish
   * their lifeline on this place.
   */
  private final int INCOMING_LIFELINES[];

  /**
   * Integer ({@code int}) id's of the places on which this place will establish
   * its lifelines.
   */
  private final int lifelines[];

  /**
   * Collection of lifeline thieves asking for work from this place. They will
   * be answered in the {@link #distribute()} method.
   */
  private final ConcurrentLinkedQueue<Place> lifelineThieves = new ConcurrentLinkedQueue<>();

  /** Number of places available for the computation */
  private final int places = places().size();

  /**
   * Random generator used when thieving a random place.
   * <p>
   * By initializing the seed with the place id (different from all the other
   * places), we avoid having the same sequence of places to thieve from for all
   * the places.
   */
  private final Random random = new Random(home.id);

  /**
   * Number of random steal attempts performed by this place. Can be adjusted to
   * the user's convenience with the constructor.
   */
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
   * List of thieves that asked for work while the current place was performing
   * computation. They will be answered in {@link #distribute()} method.
   */
  private final ConcurrentLinkedQueue<Place> thieves = new ConcurrentLinkedQueue<>();

  /** Amount of work processed by this place before dealing with thieves */
  private final int WORK_UNIT;

  /**
   * Puts this instance of LoopGLBPRocessor into a ready-to-compute state.
   * Parameter {@code init} is kept as member {@link #result}. Having the
   * parameter type of the result also allows us to initialize a new instance
   * for {@link #bagsToDo} with the proper generic parameter type.
   *
   * @param <R>
   *          result parameter type
   * @param init
   *          neutral element of the desired result type for the computation to
   *          come
   */
  private <R extends Fold<R> & Serializable> void clear(R init) {
    thieves.clear();
    lifelineThieves.clear();
    for (final int i : INCOMING_LIFELINES) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
    state = -2;
    bagsToDo = new ConcurrentBagQueue<R>();
    result = init;
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
   * @see #steal()
   */
  @SuppressWarnings("unchecked")
  private synchronized <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void deal(
      B gift) {

    /*
     * If answering place couldn't share work with this place, the given q is
     * null. A check is therefore necessary.
     */
    if (gift != null) {
      bagsToDo.giveBag(gift);
    }

    /*
     * Whichever the outcome, the worker thread blocked in method run needs to
     * be waken up
     */
    state = -1; // Switch back to 'running' state.
    notifyAll(); // Wakes up the halted thread in 'steal' procedure.
  }

  /**
   * Distributes {@link Bag}s to the random thieves and the lifeline thieves
   * asking for work from this place.
   *
   * @param <B>
   *          type of offered work given to thieves
   * @param <R>
   *          type of result type B produces
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void distribute() {
    if (places == 1) {
      return;
    }
    Place p;
    while ((p = thieves.poll()) != null) {

      final B toGive = (B) bagsToDo.split();
      uncountedAsyncAt(p, () -> {
        deal(toGive);
      });
    }

    while ((p = lifelineThieves.poll()) != null) {
      final B toGive = (B) bagsToDo.split();
      if (toGive != null) {
        asyncAt(p, () -> {
          lifelineDeal(toGive);
        });
      } else {
        /*
         * null split means no more work to share in bagsToDo. We put the thief
         * back into the collection
         */
        lifelineThieves.add(p);

        /*
         * All further calls to bagsToDo.split() would yield null at this stage,
         * it is not worth continuing
         */
        return;
      }
    }
  }

  /**
   * Computes the result from the bags processed at this place, storing it into
   * member {@link #result()} before sending those to place 0 if this place
   * isn't place 0.
   *
   * @param <R>
   *          result type
   *
   * @see #giveResult(Fold)
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <R extends Fold<R> & Serializable> void gather() {
    synchronized (result) {
      bagsToDo.result(result);
    }

    final Fold r = result;
    if (home.id != 0) {
      asyncAt(place(0), () -> {
        giveResult((R) r);
      });
    }
  }

  /**
   * Wakes up a place waiting for work on its lifeline, giving it some work
   * {@code a} on the fly in response to a {@link #lifelineSteal()}
   * <p>
   * As there are several lifelines established by this place, there will be a
   * call to this method originating from each of the lifeline, each giving some
   * work to this place. This is handled by the concurrency protection of
   * {@link ConcurrentBagQueue} which ensures mutual exclusion between the
   * processing of a bag and the addition of bags to the {@link #bagsToDo}
   * member.
   * <p>
   * Moreover, as a call to this method can happen at any time, a check on the
   * value of {@link #state} is necessary to ensure no two processes are running
   * method {@link #run()} at the same time.
   *
   * @param <B>
   *          type of the given work
   *
   * @param q
   *          the work to be given to the place
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void lifelineDeal(
      B q) {
    bagsToDo.giveBag(q);

    /*
     * Call to run needs to be done outside of the synchronized block so boolean
     * toLaunch is used to carry the information
     */
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
    final Place h = home;
    for (final int i : lifelines) {
      asyncAt(place(i), () -> {
        lifelineThieves.add(h);
      });
    }
  }

  /**
   * Method used to signal the fact place {@code p} given as parameter is
   * requesting work from this place.
   * <p>
   * If this place is currently working ({@link #state} = -1), adds the place
   * asking for work to member {@link #thieves}. The answer will be provided
   * when this place calls its {@link #distribute()} method.
   * <p>
   * If this place is not working, i.e. either trying to steal work randomly
   * ({@link #state} => 0) or inactive ({@link #state} == -2), a {@code null}
   * answer is dispatched immediately.
   *
   * @param p
   *          The place asking for work
   */
  private void request(Place p) {
    synchronized (this) {
      /*
       * If the place is currently performing computation, adds the thief to its
       * list of pending thief. The work will be shared when this place stops
       * processing its tasks in the main 'run' loop by the first 'distribute'
       * call.
       */
      if (state == -1) {
        thieves.add(p);
        return;
      }
    }

    uncountedAsyncAt(p, () -> {
      deal(null);
    });
  }

  /**
   * Main computation procedure.
   * <p>
   * While this place has some work, it processes {@link #WORK_UNIT} worth of
   * work in its {@link #bagsToDo} before answering to potential thieves (method
   * {@link #distribute()}).
   * <p>
   * When it runs out of work, attempts a maximum of
   * {@link #RANDOM_STEAL_ATTEMPTS} steals on other places (method
   * {@link #steal()}). If successful in one of its steals, resumes its
   * processing/distributing routine.
   * <p>
   * If all random steals fail, establishes its lifeline (method
   * {@link #lifelineSteal()}) and stops.
   */
  private void run() {

    synchronized (this) {
      state = -1;
    }

    for (;;) { // Is correct, loop is exited thanks with a break later on
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

    /**
     * Sending null to thieves that have managed to ask for work between to
     * random steals. This is absolutely necessary. The computation may not end
     * if this was not done.
     */
    Place p;
    while ((p = thieves.poll()) != null) {
      uncountedAsyncAt(p, () -> {
        deal(null);
      });
    }

    // Establishing lifeline
    lifelineSteal();
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

    // Selecting the random place
    int p = random.nextInt(places - 1);
    if (p >= h.id) {
      // We cannot steal on ourselves. Moreover the generated random integer has
      // a range of `places - 1`. By incrementing p we get an uniform
      // distribution for the target place.
      p++;
    }

    /*
     * Change state to 'p', i.e. thieving from place 'p' before requesting work
     * from it.
     */
    synchronized (this) {
      state = p;
    }

    /*
     * Calls "request" at place p, passing itself as parameter. The call is
     * 'uncounted' as this asynchronous call is about program "logistics" and
     * does not need to intervene in the enclosing "finish" construct
     */
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
   * Merges the given parameter into this instance {@link #result} member. Only
   * called on place 0. Mutual exclusion between several calls to this method as
   * well as {@link #gather()} method is ensured.
   *
   * @param <R>
   *          type of the result
   * @param fold
   *          the fold to be merged into this place
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable> void giveResult(R res) {
    synchronized (result) {
      result.fold(res);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#compute(apgas.glb.Bag,
   * java.util.function.Supplier)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      B bag, R result) {
    synchronized (bagsToDo) {
      reset(result);
      bagsToDo.giveBag(bag);
      finish(() -> {
        run();
      });
      return result();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.GLBProcessor#compute(apgas.glb.Bag,
   * java.util.function.Supplier)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      Collection<B> bags, R init) {
    synchronized (bagsToDo) {

      reset(init);
      for (final B bag : bags) {
        bagsToDo.giveBag(bag);
      }

      finish(() -> {
        run();
      });
      return result();
    }
  }

  /**
   * Clears the {@link LoopGLBProcessor} of all its tasks and results and
   * prepares it for a new computation.
   */
  public <R extends Fold<R> & Serializable> void reset(R init) {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> clear(init));
      }
    });
  }

  /**
   * Gives back the result that was gathered from the {@link Bag}s contained in
   * the {@link #bagsToDo} member of all the places. The computation needs to be
   * over before this method is called.
   *
   * @param <R>
   *          type of the returned {@link Fold}
   * @return the computation's result
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable> R result() {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, () -> gather());
      }
    });
    return (R) result;

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

    INCOMING_LIFELINES = s.reverseLifeline(home.id, places);
    lifelines = s.lifeline(home.id, places);
    for (final int i : INCOMING_LIFELINES) {
      if (i != 0) {
        lifelineThieves.add(place(i));
      }
    }
  }
}
