/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Patrick Finnerty
 *
 */
@RunWith(Parameterized.class)
public class StrategyTest {

  private final int GRAPH_SIZE = 16;

  /** instance currently in testing */
  LifelineStrategy strategy;

  /**
   * Tests that there are not twice the same node in the lifelines arrays. Both
   * the {@link LifelineStrategy#lifeline(int, int)} and
   * {@link LifelineStrategy#reverseLifeline(int, int)} are tested.
   */
  @Test
  public void noRedundancyTest() {
    for (int i = 0; i < GRAPH_SIZE; i++) {
      final ArrayList<Integer> nodes = new ArrayList<>();
      nodes.add(new Integer(i));
      final int lifelines[] = strategy.lifeline(i, GRAPH_SIZE);
      for (int j = 0; j < lifelines.length; j++) {
        final Integer target = new Integer(lifelines[j]);
        assert (!nodes.contains(target));
        nodes.add(target);
      }
    }

    for (int i = 0; i < GRAPH_SIZE; i++) {
      final ArrayList<Integer> nodes = new ArrayList<>();
      nodes.add(new Integer(i));
      final int lifelines[] = strategy.reverseLifeline(i, GRAPH_SIZE);
      for (int j = 0; j < lifelines.length; j++) {
        final Integer target = new Integer(lifelines[j]);
        assert (!nodes.contains(target));
        nodes.add(target);
      }
    }
  }

  /**
   * Tests the consistency of the
   * {@link LifelineStrategy#reverseLifeline(int, int)} method wrt the
   * {@link LifelineStrategy#lifeline(int, int)} method.
   */
  @Test
  public void reverseLifelineConsistenyTest() {
    for (int i = 0; i < GRAPH_SIZE; i++) {
      final int lifeline[] = strategy.lifeline(i, GRAPH_SIZE);
      for (final int j : lifeline) {
        final int reverse[] = strategy.reverseLifeline(j, GRAPH_SIZE);
        boolean found = false;
        for (final int k : reverse) {
          if (k == i) {
            found = true;
            break;
          }
        }
        if (!found) {
          fail(i + " was not in the reverse lifelines of " + j);
        }
      }
    }
  }

  /**
   * Tests if the graph described by the lifeline strategy is connected, that is
   * if any node can be accessed from any other. Lifeline strategies that fail
   * this property could potentially result in the failure of the work
   * distribution among all places : only a subset of all places could be used
   * to perform the computation rather than all of them.
   */
  @Test
  public void lifelineGraphConnectedTest() {
    for (int i = 0; i < GRAPH_SIZE; i++) {
      checkAllReachable(i);
    }
  }

  /**
   * @param i
   *          starting node
   */
  private void checkAllReachable(final int i) {
    final int nodeState[] = new int[GRAPH_SIZE]; // Value of 0 : not visited
                                                 // Value of 1 : visited
    final Stack<Integer> path = new Stack<>();

    // DFS algorithm
    path.push(new Integer(i));
    nodeState[i] = 1; // Node i is visited
    outerloop: while (!path.isEmpty()) {
      final int n = path.lastElement();
      for (final int j : strategy.lifeline(n, GRAPH_SIZE)) {
        if (nodeState[j] == 0) {
          path.push(new Integer(j));
          nodeState[j] = 1; // Node j visited
          continue outerloop;
        }
      }
      path.pop();
    }

    // Checking all the nodes were visited
    for (int j = 0; j < nodeState.length; j++) {
      if (nodeState[j] != 1) {
        fail("Node (" + j + ") could not be reached from node (" + i + ")");
      }
    }
  }

  /**
   * Constructor
   *
   * @param s
   *          the LifelineStrategy to be tested
   */
  public StrategyTest(LifelineStrategy s) {
    strategy = s;
  }

  /**
   * Creates the different strategies to be built
   *
   * @return collection of object array to be used as the test class constructor
   *         parameters
   */
  @Parameterized.Parameters
  public static Collection<Object[]> toTest() {
    final Collection<Object[]> toReturn = new ArrayList<>();
    final Object[] first = { new HypercubeStrategy() };
    toReturn.add(first);
    return toReturn;
  }

}
