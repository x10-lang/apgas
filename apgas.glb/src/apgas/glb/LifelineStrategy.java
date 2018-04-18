/**
 *
 */
package apgas.glb;

/**
 * {@link LifelineStrategy} describes an oriented graph between nodes
 * represented by their number. A valid {@link LifelineStrategy} consists in a
 * connected graph.
 *
 * @author Patrick Finnerty
 *
 */
public interface LifelineStrategy {

  /**
   * Gives the list of nodes as an array that start from the node {@code home}.
   *
   * @param home
   *          the starting node
   * @param nbPlaces
   *          the number of nodes in the graph
   * @return array of int. Each int, say 'a' present in the array means that
   *         there exist a vertex from node 'home'to node 'a'.
   */
  public int[] lifeline(int home, int nbPlaces);

  /**
   * Gives the list of nodes who have a vertex from them to the node 'home'
   * 
   * @param target
   *          destination node
   * @param nbPlaces
   *          number of nodes in the graph
   * @return array of node id's who have a vertex from them to node
   *         {@code target}
   */
  public int[] reverseLifeline(int target, int nbPlaces);
}
