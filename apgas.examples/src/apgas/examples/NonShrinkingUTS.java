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

import java.io.Serializable;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionalTaskContext;

import apgas.Configuration;
import apgas.DeadPlaceException;
import apgas.DeadPlacesException;
import apgas.GlobalRuntime;
import apgas.Place;
import apgas.util.GlobalRef;
import apgas.util.PlaceLocalObject;
import apgas.util.ResilientPlaceGroup;

final class NonShrinkingUTS extends PlaceLocalObject {
  final HazelcastInstance hz = Hazelcast.getHazelcastInstanceByName("apgas");
  final IMap<Integer, UTS> map = hz.getMap("uts"); // the resilient map
  final ResilientPlaceGroup group; // the places in the current wave
  final Worker[] workers; // the workers at the current place
  final int power; // 1<<power workers per place
  final int mask;

  @FunctionalInterface
  static interface Fun extends Serializable {
    void run(Worker w);
  }

  @FunctionalInterface
  static interface FunE extends Serializable {
    void run(Worker w) throws Exception;
  }

  void myAsyncAt(int dst, FunE f) {
    asyncAt(group.get(dst >> power), () -> f.run(workers[dst & mask]));
  }

  void myUncountedAsyncAt(int dst, Fun f) {
    if (group.get(dst >> power).equals(here())) {
      f.run(workers[dst & mask]);
    } else {
      uncountedAsyncAt(group.get(dst >> power),
          () -> f.run(workers[dst & mask]));
    }
  }

  NonShrinkingUTS(ResilientPlaceGroup group, int power) {
    this.group = group;
    this.power = power;
    mask = (1 << power) - 1;
    workers = new Worker[1 << power];
    for (int i = 0; i <= mask; i++) {
      workers[i] = new Worker(i);
    }
    GlobalRuntime.getRuntime().setPlaceFailureHandler(this::unblock);
  }

  void unblock(Place p) {
    if (!group.contains(p)) {
      return;
    }
    if (p.id > 0) {
      System.err.println("Observing failure of " + p + " from " + here());
    }
    for (int i = 0; i <= mask; ++i) {
      workers[i].unblock(p);
    }
  }

  final class Worker { // not serializable
    final int me; // my index in the lifeline graph
    final int prev; // index of tfhe predecessor in the lifeline graph
    final int next; // index of the successor in the lifeline graph
    final Random random;
    final MessageDigest md = UTS.encoder();
    final UTS bag;
    // pending requests from thieves
    final ConcurrentLinkedQueue<Integer> thieves = new ConcurrentLinkedQueue<>();
    // pending lifeline request?
    final AtomicBoolean lifeline = new AtomicBoolean(false);
    int state = -2; // -3: abort, -2: inactive, -1: running, p: stealing from p

    Worker(int id) {
      me = (group.indexOf(here()) << power) + id;
      random = new Random(me);
      prev = (me + (group.size() << power) - 1) % (group.size() << power);
      next = (me + 1) % (group.size() << power);
      bag = new UTS(64);
      final UTS b = map.get(me);
      if (b != null) {
        if (b.size != 0) {
          bag.merge(b);
        }
        bag.count = b.count;
      }
    }

    synchronized void abort() {
      if (state == -3) {
        throw new DeadPlaceException(here());
      }
    }

    public void run() throws DigestException {
      try {
        // System.err.println(me + " starting");
        synchronized (this) {
          abort();
          state = -1;
        }
        while (bag.size > 0) {
          while (bag.size > 0) {
            for (int n = 500; (n > 0) && (bag.size > 0); --n) {
              bag.expand(md);
            }
            synchronized (this) {
              if (state == -3) {
                map.set(me, bag.trim());
                throw new DeadPlaceException(here());
              }
            }
            distribute();
          }
          map.set(me, bag.trim());
          steal();
        }
        synchronized (this) {
          abort();
          state = -2;
        }
        distribute();
        lifelinesteal();
      } finally {
        // System.err.println(me + " stopping");
      }
    }

    void lifelinesteal() {
      if (group.size() == 1 && power == 0) {
        return;
      }
      myUncountedAsyncAt(prev, w -> w.lifeline.set(true));
    }

    void steal() {
      if (group.size() == 1 && power == 0) {
        return;
      }
      final int me = this.me;
      int p = random.nextInt((group.size() << power) - 1);
      if (p >= me) {
        p++;
      }
      synchronized (this) {
        abort();
        state = p;
      }
      myUncountedAsyncAt(p, w -> w.request(me));
      synchronized (this) {
        while (state >= 0) {
          try {
            wait();
          } catch (final InterruptedException e) {
          }
        }
      }
    }

    void request(int thief) {
      synchronized (this) {
        if (state == -3) {
          return;
        }
        if (state == -1) {
          thieves.add(thief);
          return;
        }
      }
      myUncountedAsyncAt(thief, w -> w.deal(null));
    }

    void lifelinedeal(UTS b) throws DigestException {
      bag.merge(b);
      run();
    }

    synchronized void deal(UTS loot) {
      if (state == -3) {
        return;
      }
      if (loot != null) {
        bag.merge(loot);
      }
      state = -1;
      notifyAll();
    }

    synchronized void unblock(Place p) {
      state = -3;
      notifyAll();
    }

    void transfer(int thief, UTS loot) {
      final UTS bag = this.bag.trim();
      final int me = this.me;
      hz.executeTransaction((TransactionalTaskContext context) -> {
        final TransactionalMap<Integer, UTS> map = context.getMap("uts");
        map.set(me, bag);
        final UTS old = map.getForUpdate(thief);
        loot.count = old == null ? 0 : old.count;
        map.set(thief, loot);
        return null;
      });
    }

    void distribute() {
      if (group.size() == 1 && power == 0) {
        return;
      }
      Integer thief;
      while ((thief = thieves.poll()) != null) {
        final UTS loot = bag.split();
        if (loot != null) {
          transfer(thief, loot);
        }
        myUncountedAsyncAt(thief, w -> w.deal(loot));
      }
      if (bag.size > 0 && lifeline.get()) {
        final UTS loot = bag.split();
        if (loot != null) {
          thief = next;
          transfer(thief, loot);
          lifeline.set(false);
          myAsyncAt(next, w -> w.lifelinedeal(loot));
        }
      }
    }

  }

  public static long step(ResilientPlaceGroup group, int power) {
    group.fix();
    final List<Place> list = group.asList();
    final NonShrinkingUTS uts = PlaceLocalObject.make(list,
        () -> new NonShrinkingUTS(group, power));
    finish(() -> {
      for (final Place p : list) {
        asyncAt(p, () -> {
          for (int i = 1; i < 1 << power; ++i) {
            final int _i = i;
            async(() -> uts.workers[_i].run());
          }
          if (here().id != 0) {
            uts.workers[0].run();
          }
        });
      }
      if (GlobalRuntime.getRuntime().lastfailureTime() != null) {
        System.err.println("Recovered in "
            + (System.nanoTime() - GlobalRuntime.getRuntime().lastfailureTime())
                / 1e9
            + "s");
      }
      uts.workers[0].run();
    });
    final GlobalRef<AtomicLong> ref = new GlobalRef<>(new AtomicLong());
    finish(() -> {
      for (final Place p : list) {
        asyncAt(p, () -> {
          long _count = 0;
          for (int i = 0; i < 1 << power; i++) {
            _count += uts.workers[i].bag.count;
          }
          final long count = _count;
          asyncAt(new Place(0), () -> ref.get().getAndAdd(count));
        });
      }
    });
    return ref.get().get();
  }

  static void explode(UTS bag, ResilientPlaceGroup group, int power) {
    if (bag.upper[0] > group.size()) {
      Hazelcast.getHazelcastInstanceByName("apgas").getMap("uts").put(0, bag);
      return;
    }
    finish(() -> {
      for (int i = 0; i < bag.upper[0]; i++) {
        final UTS b = new UTS(64);
        b.merge(bag);
        b.lower[0] = i;
        b.upper[0] = i + 1;
        if (i == 0) {
          b.count = 1;
          Hazelcast.getHazelcastInstanceByName("apgas").getMap("uts").put(0, b);
        } else {
          final int d = (i * group.size()) / bag.upper[0];
          asyncAt(group.get(d),
              () -> Hazelcast.getHazelcastInstanceByName("apgas").getMap("uts")
                  .put(d << power, b));
        }
      }
    });
  }

  public static void main(String[] args) {
    int depth = 13;
    try {
      depth = Integer.parseInt(args[0]);
    } catch (final Exception e) {
    }
    int _power = 1;
    try {
      _power = Integer.parseInt(args[1]);
    } catch (final Exception e) {
    }
    final int power = _power;

    System.setProperty(Configuration.APGAS_RESILIENT, "true");
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, "4");
    }
    System.setProperty(Configuration.APGAS_THREADS, "" + ((1 << power) + 1));

    final int spares = Integer.getInteger("apgas.spares", 1);

    final ResilientPlaceGroup world = new ResilientPlaceGroup(places().size());
    final int maxPlaces = places().size() - spares;
    final ResilientPlaceGroup group = new ResilientPlaceGroup(maxPlaces);

    final MessageDigest md = UTS.encoder();

    System.out.println("Warmup...");

    UTS bag = new UTS(64);
    bag.seed(md, 19, depth - 2);
    explode(bag, world, power);
    finish(() -> NonShrinkingUTS.step(world, power));

    Hazelcast.getHazelcastInstanceByName("apgas").getMap("uts").clear();

    System.out.println("Starting...");
    Long time = -System.nanoTime();

    bag = new UTS(64);
    bag.seed(md, 19, depth);
    explode(bag, group, power);

    int wave = 0;
    long count;
    for (;;) {
      System.out.println("Wave: " + wave++);
      try {
        count = finish(() -> NonShrinkingUTS.step(group, power));
        break;
      } catch (final DeadPlacesException e) {
      }
    }

    time += System.nanoTime();
    System.out.println("Finished.");

    System.out.println(
        "Depth: " + depth + ", Places: " + maxPlaces + ", Waves: " + wave
            + ", Performance: " + count + "/" + UTS.sub("" + time / 1e9, 0, 6)
            + " = " + UTS.sub("" + (count / (time / 1e3)), 0, 6) + "M nodes/s");
  }
}
