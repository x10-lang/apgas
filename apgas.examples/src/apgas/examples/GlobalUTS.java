/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2016.
 */

package apgas.examples;

import static apgas.Constructs.*;

import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import apgas.Configuration;
import apgas.Place;
import apgas.util.PlaceLocalObject;

final class GlobalUTS extends PlaceLocalObject {

  /** Brings the APGAS place to the class */
  final Place home = here();

  /** Number of places available for the computation */
  final int places = places().size();

  /**
   * Random generator used when thieving a random place
   * <p>
   * By initializing the seed with the place id (different from all the other
   * places), we avoid having the same sequence of places to thieve from for all
   * the places.
   */
  final Random random = new Random(home.id);

  /** UTS element necessary to the computation */
  final MessageDigest md = UTS.encoder();

  /** Tasks to be performed by the place */
  final UTS bag = new UTS(64);

  /**
   * List of thieves that asked for work when the current place was performing
   * computation
   */
  final ConcurrentLinkedQueue<Place> thieves = new ConcurrentLinkedQueue<>();

  /**
   * Signals the presence of a thief on the lifeline
   * <p>
   * In the current implementation, as each place has a unique potential
   * lifeline thief, a boolean value is enough. If there were multiple potential
   * thiefs, storing the id of the thieves would be necessary.
   *
   * @see #lifelinesteal()
   * @see #lifelinedeal(UTS)
   */
  final AtomicBoolean lifeline = new AtomicBoolean(home.id != places - 1);

  /**
   * Indicates the state of the place
   * <ul>
   * <li><em>-2</em> : inactive
   * <li><em>-1</em> : running
   * <li><em>p</em> in range [0,{@code places}] : stealing from place of id
   * <em>p</em>
   * </ul>
   * At initialization, is in inactive state.
   */
  int state = -2;

  void seed(int s, int d) {
    bag.seed(md, s, d);
  }

  /**
   * Main routine of the program
   * <p>
   * Performs 500 iterations of work before processing pending thieves
   * (distributing work). When this place runs out of work, attempts to steal
   * work from a randomly chosen place. If the steal was successful, repeats.
   * <p>
   * If the steal failed, processes possible thieves (race condition) before
   * establishing its lifeline and stopping. The place may be called back into
   * service by the place on its lifeline if it has work to share.
   *
   * @throws DigestException
   *           thrown by the computation performed
   */
  public void run() throws DigestException {
    System.err.println(home + " starting");

    // Switch to running mode
    synchronized (this) {
      state = -1;
    }

    // Checks if the performed steal was successful, i.e. if work was given.
    while (bag.size > 0) {
      while (bag.size > 0) {
        for (int n = 500; (n > 0) && (bag.size > 0); --n) {
          bag.expand(md);
        }
        distribute();
      }
      steal();
    }

    // Changes to inactive mode before establishing its lifelines.
    synchronized (this) {
      state = -2;
    }
    // Processes random thieves that may have asked for work between the
    // 'failed' deal (after state was switched back to -1- running) and the
    // moment this place was placed back to inactive. They need to be answered
    // before this place halts.
    distribute();

    // Establishes lifeline steals
    lifelinesteal();
    System.err.println(home + " stopping");
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
  void lifelinesteal() {
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
   * Attempts to steal work to a randomly chosen place. Will halt the process
   * until the target place answers (whether it indeed gave work or not).
   */
  void steal() {
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
   * Method used to signal the fact place {@code p} is requesting work from this
   * place.
   * <p>
   * If this place is currently working, adds the thief to its {@link #thieves}
   * queue which will be processed when it performs a certain number of
   * iterations. If this place is not working, i.e. is trying to steal work
   * (randomly or through a lifeline), asynchronously {@link #deal(Place, UTS)}
   * {@code null} work.
   *
   * @param p
   *          The place asking for work
   */
  void request(Place p) {
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

  /**
   * Wakes up a place waiting for work on its lifeline, giving it some work
   * {@code b} on the fly in response to a {@link #lifelinesteal()}
   * <p>
   * Only makes sense if this place is in inactive {@link #state}.
   *
   * @param b
   *          the work to be given to the place
   * @throws DigestException
   *           thrown by the {@link #run()}
   */
  void lifelinedeal(UTS b) throws DigestException {
    bag.merge(b);
    run();
  }

  /**
   * Yields back some work in response to a {@link #steal()}
   * <p>
   * Merges the proposed work {@code b} into this place's {@link #bag} and wakes
   * up the presumably waiting thread in the {@link #steal()} procedure. This
   * will in turn make this place check its {@link #bag} for any work and either
   * process the given work or switch to the lifeline steal procedure.
   *
   * @param p
   *          the place where the work is originating from
   * @param b
   *          the work given by place {@code p}, possibly <code>null</code>.
   */
  synchronized void deal(Place p, UTS b) {
    // We are presumably receiving work place p. Therefore this place should be
    // in state 'p'.
    assert state == p.id;

    // If place p couldn't share work with this place, the given b is null. A
    // check is therefore necessary.
    if (b != null) {
      bag.merge(b);
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
   * Splits this place's {@link #bag} and {@link #deal(Place, UTS)}s with random
   * thieves before {@link #lifelinedeal(UTS)}ing with the lifeline thieves.
   */
  void distribute() {
    if (places == 1) {
      return;
    }
    Place p;
    while ((p = thieves.poll()) != null) {
      final UTS b = bag.split();
      final Place h = home;
      uncountedAsyncAt(p, () -> {
        deal(h, b);
      });
    }
    if (bag.size > 0 && lifeline.get()) {
      final UTS b = bag.split();
      if (b != null) {
        p = place((home.id + 1) % places);
        lifeline.set(false);
        asyncAt(p, () -> {
          lifelinedeal(b);
        });
      }
    }
  }

  public void reset() {
    bag.count = 0;
    lifeline.set(home.id != places - 1);
  }

  public static void main(String[] args) {
    int depth = 13;
    try {
      depth = Integer.parseInt(args[0]);
    } catch (final Exception e) {
    }

    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, "4");
    }

    final GlobalUTS uts = PlaceLocalObject.make(places(),
        () -> new GlobalUTS());

    System.out.println("Warmup...");
    uts.seed(19, depth - 2);
    finish(uts::run);

    finish(() -> {
      for (final Place p : places()) {
        asyncAt(p, uts::reset);
      }
    });

    System.out.println("Starting...");
    long time = System.nanoTime();

    uts.seed(19, depth);
    finish(uts::run);

    long count = 0;
    // collect all counts
    for (final Place p : places()) {
      count += at(p, () -> uts.bag.count);
    }

    time = System.nanoTime() - time;
    System.out.println("Finished.");

    System.out.println("Depth: " + depth + ", Places: " + uts.places
        + ", Performance: " + count + "/" + UTS.sub("" + time / 1e9, 0, 6)
        + " = " + UTS.sub("" + (count / (time / 1e3)), 0, 6) + "M nodes/s");
  }
}
