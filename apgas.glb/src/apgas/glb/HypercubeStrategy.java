/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Lifeline strategy implementing an hypercube lifeline.
 *
 * @author Patrick Finnerty
 *
 */
public class HypercubeStrategy implements LifelineStrategy, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 5106410194659222967L;

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.LifelineStrategy#lifeline(int, int)
   */
  @Override
  public int[] lifeline(int home, int nbPlaces) {
    // TODO make a real implementation
    home = (nbPlaces + home - 1) % nbPlaces;
    final int toReturn[] = new int[1];
    toReturn[0] = home;
    return toReturn;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.LifelineStrategy#reverseLifeline(int, int)
   */
  @Override
  public int[] reverseLifeline(int target, int nbPlaces) {
    // TODO make a real implementation
    target = (target + 1) % nbPlaces;
    final int toReturn[] = new int[1];
    toReturn[0] = target;
    return toReturn;
  }

}
