/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import apgas.Place;
import apgas.util.PlaceLocalObject;

/**
 * LoopGLBProcessor proposes a simple API to request for work to be computed
 * using the lifeline based global load balancing framework relying on APGAS.
 *
 * @author Patrick Finnerty
 *
 */
final class LoopGLBProcessor extends PlaceLocalObject implements GLBProcessor {

  /**
   * Collection of bags to be processed. A new instance is created each time a
   * new computation is called in order to have the proper parameter type for
   * the computation.
   */
  @SuppressWarnings("rawtypes")
  private BagQueue bagsToDo;

  /**
   * Fold instance for this local place. Is initialized with the neutral element
   * before a computation takes place in method {@link #reset(Fold)}. When
   * computing the result, the member is then passed to each bag for them to add
   * in their contribution ({@link Bag#submit(Fold)}).
   */
  @SuppressWarnings("rawtypes")
  private Fold result = null;

  /** APGAS place, used to identify the instance */
  private final Place home = here();

  /**
   * Signals the presence of a thief on the lifeline
   * <p>
   * In {@link LoopGLBProcessor}, each place has a unique potential lifeline
   * thief. Hence a boolean value is enough to signal the fact the lifeline is
   * asking for work. By initializing the value at `home.id != 3` we position
   * every place as waiting for work from its lifeline except place 0 since this
   * is the place which is going to receive all the work when the computation
   * start.
   *
   * @see #lifelineSteal()
   * @see #lifelineDeal(Bag)
   */
  private final AtomicBoolean lifeline = new AtomicBoolean(home.id != 3);

  /** Number of places available for the computation */
  private final int places = places().size();

  /**
   * Random generator used when stealing work from a random place
   * <p>
   * By initializing the seed with the place id (different from all the other
   * places), we avoid having the same sequence of places to thieve from for all
   * the places.
   */
  private final Random random = new Random(home.id);

  /** Number of random steal attempts performed by this place */
  private final int RANDOM_STEAL_ATTEMPTS;

  /**
   * Indicates the state of the place at any given time. Possible values
   * include:
   * <ul>
   * <li><em>-2</em> : inactive
   * <li><em>-1</em> : working
   * <li><em>p</em> in range [0, number of places] : randomly stealing from
   * place <em>p</em>
   * </ul>
   * At initialization is in inactive state.
   */
  private int state = -2;

  /**
   * List of thieves that asked for work while the current place was performing
   * computation. Each will be answered in the {@link #distribute()} method.
   */
  private final ConcurrentLinkedQueue<Place> thieves = new ConcurrentLinkedQueue<>();

  /** A mount of work processed by this place before dealing with thieves */
  private final int WORK_UNIT;

  /**
   * Puts this instance of LoopGLBPRocessor into a ready-to-compute state.
   * Parameter {@code init} is kept as member {@link #result}. Having the
   * parameter type of the result also allows us to initialize a new instance
   * for {@link #bagsToDo} with the proper generic parameter type.
   *
   * @param<R>
   *
   * @param init
   *          neutral element of the desired result type for the computation to
   *          come
   */
  private <R extends Fold<R> & Serializable> void clear(R init) {
    thieves.clear();
    lifeline.set(home.id != 3);
    state = -2;
    bagsToDo = new BagQueue<R>();
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
   */
  @SuppressWarnings("unchecked")
  private synchronized <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void deal(
      B gift) {
    // If the place from which the random steal is performed couldn't share work
    // with this place, the given q is null. A check is therefore necessary.
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
   *
   * @param <B>
   *          type of offered work given to thieves
   * @param <R>
   *          type of result type B produces
   */
  private <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void distribute() {
    if (places == 1) {
      return;
    }
    Place p;

    while ((p = thieves.poll()) != null) {
      @SuppressWarnings("unchecked")
      final B toGive = (B) bagsToDo.split();
      uncountedAsyncAt(p, () -> {
        deal(toGive);
      });
    }
    if (lifeline.get()) {
      @SuppressWarnings("unchecked")
      final B toGive = (B) bagsToDo.split();
      if (toGive != null) {
        p = place((home.id + 1) % places);
        lifeline.set(false);
        asyncAt(p, () -> {
          lifelineDeal(toGive);
        });
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
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable> void gather() {
    /*
     * This part is in mutual exclusion with method giveResult(). This is
     * necessary for place 0 where the bags processed by place 0 submit their
     * result into the result member in this method while the other places will
     * send their member result for it to be folded into the result member in
     * method giveResult(). Incidentally, using member result for the lock.
     */
    synchronized (result) {
      bagsToDo.result(result);
    }

    final R r = (R) result;
    if (home.id != 0) {
      asyncAt(place(0), () -> {
        giveResult(r);
      });
    }
  }

  /**
   * Wakes up a place waiting for work, giving it some work to process. Called
   * in response to a {@link #lifelineSteal()} by the place giving the work. The
   * given work is merged into the {@link #bagsToDo} member and method
   * {@link #run()} is called, restarting the worker.
   *
   * @param <B>
   *          type of the given work
   * @param <R>
   *          result produced by the bag given
   *
   * @param q
   *          work to be given to the place
   */
  @SuppressWarnings("unchecked")
  private <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> void lifelineDeal(
      B q) {
    bagsToDo.giveBag(q);
    run();
  }

  /**
   * Registers this place as asking for work on its lifeline place.
   * <p>
   * The lifeline strategy in this implementation consists in a single directed
   * loop linking all the nodes. That is:
   * <ul>
   * <li>place <em>n</em> establishes its lifeline on place <em>n-1</em></li>
   * <li>place <em>0</em> establishes its lifeline on place <em>({@link #places}
   * - 1)</em></li>
   * </ul>
   *
   * @see #lifeline
   */
  private void lifelineSteal() {
    if (places == 1) {
      // No other place exists, "this" is the only place.
      // Impossible to perform a steal.
      return;
    }
    asyncAt(place((home.id + places - 1) % places), () -> {
      lifeline.set(true);
    });
  }

  /**
   * Called when a place asks work from this instance, the place asking for work
   * is passed as parameter.
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
   *          place asking for work
   */
  private void request(Place p) {
    synchronized (this) {
      // If performing some computation
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
    System.err.println(home + " starting");
    synchronized (this) {
      state = -1; // Switch to 'working' mode
    }
    do {
      /*
       * While the place has work, computes a certain amount of it before
       * replying to thieves.
       */
      while (!bagsToDo.isEmpty()) {
        bagsToDo.process(WORK_UNIT);
        distribute();
      }

      // Perform at most RANDOM_STEAL_ATTEMPTS
      int attempts = RANDOM_STEAL_ATTEMPTS;
      while (attempts > 0 && bagsToDo.isEmpty()) {
        attempts--;
        steal();
      }

    } while (!bagsToDo.isEmpty());

    /**
     * At this stage, the place has no more work and its random steals attempts
     * have failed. It is now time to turn to the lifeline scheme.
     */

    synchronized (this) {
      state = -2; // Switch to 'inactive' mode
    }

    /**
     * Sending null to thieves that have managed to ask for work between to
     * random steals. This is absolutely necessary. The computation may not end
     * if this was not done.
     *
     */
    Place p;
    while ((p = thieves.poll()) != null) {
      uncountedAsyncAt(p, () -> {
        deal(null);
      });
    }

    // Establishing lifeline
    lifelineSteal();
    System.err.println(home + " stopping");
  }

  /**
   * Attempts to steal work to a randomly chosen place. Will halt the process
   * until the target place answers (whether it indeed gave work or not).
   *
   * @see #deal(Bag)
   */
  private void steal() {
    if (places == 1) {
      // No other place exists, "this" is the only one.
      // Cannot perform a steal.
      return;
    }

    final Place h = home; // Other name for home. Needed since when calling
                          // asyncAt,

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
    // The call is 'uncounted' as this is about program "logistics" and
    // does not need to intervene in the enclosing "finish" for the computation.
    uncountedAsyncAt(place(p), () -> {
      request(h);
    });

    synchronized (this) { // synchronized block necessary for the wait() call.
      while (state >= 0) { // enclosing safety loop. State is set back to -1
                           // ('running') in method deal when the answer is
                           // received.
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
   * @see apgas.glb.GLBProcessor#compute(apgas.glb.Bag, apgas.glb.Fold)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      B bag, R init) {
    synchronized (bagsToDo) {

      reset(init);
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
   *
   * @param <R>
   *          type of the result to expect
   * @param init
   *          initial result instance to be put at each place.
   */
  private <R extends Fold<R> & Serializable> void reset(R init) {
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
        // Folding this instance's folds into that of place 0
        asyncAt(p, () -> gather());
      }
    });
    return (R) result;
  }

  /**
   * Package visibility Constructor. Called by {@link GLBProcessorFactory} when
   * creating a new GLBProcessor.
   *
   * @param workUnit
   *          the amount of work to be processed before tending to thieves
   * @param randomStealAttempts
   *          number of random steals attempts before resaulting to the lifeline
   *          thief scheme
   */
  LoopGLBProcessor(int workUnit, int randomStealAttempts) {
    WORK_UNIT = workUnit;
    RANDOM_STEAL_ATTEMPTS = randomStealAttempts;
    bagsToDo = new BagQueue<>();
  }
}
