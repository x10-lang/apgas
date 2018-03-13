/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.security.DigestException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import apgas.Configuration;
import apgas.Place;
import apgas.util.PlaceLocalObject;

/**
 * GLBProcessor proposes a simple API to request for work to be computed using
 * the lifeline based global load balancing framework proposed by APGAS.
 *
 * @author Patrick Finnerty
 *
 */
public final class GLBProcessor extends PlaceLocalObject
    implements TaskProcessor {

  /** Number of tasks to process before responding to thieves */
  private static final int WORK_UNIT = 40;

  /**
   * Collection of tasks to be processed
   * <p>
   * The tasks are processed from the top of the queue by
   * {@link TaskQueue#pop()}. When a steal occurs, they are removed from the
   * bottom of the queue using {@link TaskQueue#split()}
   */
  private final TaskQueue tasks;

  /** Brings the APGAS place id to the class {@link GLBProcessor} */
  private final Place home = here();

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

  /**
   * List of thieves that asked for work when the current place was performing
   * computation
   */
  private final ConcurrentLinkedQueue<Place> thieves = new ConcurrentLinkedQueue<>();

  /**
   * Signals the presence of a thief on the lifeline
   * <p>
   * In the current implementation, as each place has a unique potential
   * lifeline thief, a boolean value is enough. If there were multiple potential
   * thiefs, storing the id of the thieves would be necessary.
   *
   * @see #lifelinesteal()
   * @see #lifelinedeal(TaskQueue)
   */
  private final AtomicBoolean lifeline = new AtomicBoolean(false);

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
   * Yields back some work in response to a {@link #steal()}
   * <p>
   * Merges the proposed work {@code q} into this place's {@link #tasks} and
   * wakes up the waiting thread in the {@link #steal()} procedure. This will in
   * turn make this place check its {@link #tasks} for any work and either
   * process the given work or switch to the lifeline steal procedure.
   *
   * @param p
   *          the place where the work is originating from
   * @param q
   *          the work given by place {@code p}, possibly <code>null</code>.
   */
  private synchronized void deal(Place p, TaskQueue q) {
    // We are presumably receiving work place p. Therefore this place should be
    // in state 'p'.
    assert state == p.id;

    // If place p couldn't share work with this place, the given q is null. A
    // check is therefore necessary.
    if (q != null) {
      q.setProcessor(this);
      tasks.merge(q);
    }
    // Switch back to 'running' state.
    state = -1;

    // Wakes up the halted thread in 'steal' procedure.
    notifyAll();
  }

  /**
   * Processes the random thieves and the lifeline thieves asking for work from
   * this place.
   * <p>
   * Splits this place's {@link #tasks} and {@link #deal(Place, TaskQueue)}s
   * with random thieves before {@link #lifelinedeal(TaskQueue)}ing with the
   * lifeline thieves.
   */
  private void distribute() {
    if (places == 1) {
      return;
    }
    Place p;
    while ((p = thieves.poll()) != null) {
      final TaskQueue b = tasks.split();
      final Place h = home;
      uncountedAsyncAt(p, () -> {
        deal(h, b);
      });
    }
    if (!tasks.isEmpty() && lifeline.get()) {
      final TaskQueue b = tasks.split();
      if (b != null) {
        p = place((home.id + 1) % places);
        lifeline.set(false);
        asyncAt(p, () -> {
          lifelinedeal(b);
        });
      }
    }
  }

  /**
   * Registers this {@code GlobalUTS} as asking for work at its (remote)
   * lifeline {@code GlobalUTS} places. If there is only one place, has no
   * effect.
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
  private void lifelinesteal() {
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
    asyncAt(place((home.id + places - 1) % places), () -> {
      lifeline.set(true);
    });
  }

  /**
   * Wakes up a place waiting for work on its lifeline, giving it some work
   * {@code a} on the fly in response to a {@link #lifelinesteal()}
   * <p>
   * Only makes sense if this place is in inactive {@link #state}.
   *
   * @param q
   *          the work to be given to the place
   * @throws DigestException
   *           thrown by {@link #run()}
   */
  private void lifelinedeal(TaskQueue q) throws DigestException {
    q.setProcessor(this);
    tasks.merge(q);
    run();
  }

  /**
   * Method used to signal the fact place {@code p} is requesting work from this
   * place.
   * <p>
   * If this place is currently working, adds the thief to its {@link #thieves}
   * queue which will be processed when it performs a certain number of
   * iterations. If this place is not working, i.e. is trying to steal work
   * (randomly or through a lifeline), asynchronously
   * {@link #deal(Place, TaskQueue)} {@code null} work.
   *
   * @param p
   *          The place asking for work
   */
  private void request(Place p) {
    synchronized (this) {
      // If the place is currently performing computation, adds the thief to its
      // list of pending thief.
      // The work will be shared when this place stops running 'expand' in the
      // main 'run' loop by the first 'distribute' call.
      if (state == -1) {
        thieves.add(p);
        return;
      }
    }
    final Place h = home;
    uncountedAsyncAt(p, () -> {
      deal(h, null);
    });
  }

  private void run() throws DigestException {
    System.err.println(home + " starting");

    synchronized (this) {
      state = -1;
    }

    while (!tasks.isEmpty()) {

      while (!tasks.isEmpty()) {
        System.err.println(home + " process");
        for (int n = WORK_UNIT; (n > 0) && (!tasks.isEmpty()); --n) {
          tasks.pop().process();
        }
        System.err.println(home + " distributing");
        distribute();
      }
      System.err.println(home + " stealing");
      steal();
    }

    synchronized (this) {
      state = -2;
    }
    distribute();

    lifelinesteal();
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
   * @see glb.TaskProcessor#addTask(tasks.Task)
   */
  @Override
  public void addTask(Task t) {
    t.setTaskProcessor(this);
    tasks.add(t);
  }

  /** Launches the computation of the given work */
  public void launchComputation() {
    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, this::run);
      }
    });
  }

  /**
   * Factory method for GLBProcessor
   *
   * @return a new computing instance
   */
  public static GLBProcessor GLBProcessorFactory() {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, "4");
    }

    final GLBProcessor glb = PlaceLocalObject.make(places(),
        () -> new GLBProcessor());
    return glb;
  }

  /**
   * Private Constructor
   */
  private GLBProcessor() {
    tasks = new TaskQueue();
  }
}
