/**
 *
 */
package apgas.glb;

/**
 * Work stealing strategy for GLBProcessor. When a place runs out of work, the
 * {@link LifelineStrategy} determines which places the thief can steal work
 * from.
 * <p>
 * The places a thief can steal work from are called its 'lifelines'. To be
 * valid, a {@link LifelineStrategy} needs to satisfy several properties that
 * are easily explained in terms of graphs. Consider the oriented graph whose
 * vertices are the places of the system and where an edge from vertex A to B
 * means that A has a lifeline on B. A valid {@link LifelineStrategy} consists
 * in a connected graph, i.e. there must be a path (direct of with several
 * jumps) from each place to every other place. If this is not the case, some of
 * the places could starve when they run out of work since they are not able to
 * steal any work.
 *
 * @author Patrick Finnerty
 *
 */
public interface LifelineStrategy {

  /**
   * Gives the list of nodes that place {@code thief} can steal work from.
   *
   * @param thief
   *          id of the place stealing work
   * @param nbPlaces
   *          number of places in the system
   * @return array containing the ids of the places place {@code thief} should
   *         steal from
   */
  public int[] lifeline(int thief, int nbPlaces);

  /**
   * Gives the list of places that can steal work from place {@code target}.
   *
   * @param target
   *          id of the place victim of steals
   * @param nbPlaces
   *          number of places in the system
   * @return array containing the ids of the places that can steal work from
   *         place {@code target}
   */
  public int[] reverseLifeline(int target, int nbPlaces);
}
