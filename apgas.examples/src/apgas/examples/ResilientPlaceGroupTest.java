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

import apgas.Configuration;
import apgas.GlobalRuntime;
import apgas.Place;
import apgas.util.ResilientPlaceGroup;

final class ResilientPlaceGroupTest {
  static ResilientPlaceGroup group;

  public static void handler(Place p) {
    group.fix();
  }

  public static void main(String[] args) {
    System.setProperty(Configuration.APGAS_RESILIENT, "true");
    System.setProperty(Configuration.APGAS_PLACES, "16");

    GlobalRuntime.getRuntime()
        .setPlaceFailureHandler(ResilientPlaceGroupTest::handler);

    group = new ResilientPlaceGroup(8);

    for (;;) {
      System.err.print("places: " + places().size() + ", group: ");
      for (int i = 0; i < group.size(); i++) {
        System.err.print(group.get(i) + " ");
      }
      System.err.println();
      try {
        Thread.sleep(1000);
      } catch (final InterruptedException e) {
      }
    }
  }
}
