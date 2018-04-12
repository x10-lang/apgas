/**
 *
 */
package apgas.glb;

/**
 * @author Patrick Finnerty
 *
 */
public interface LifelineStrategy {

  public int[] lifeline(int home, int nbPlaces);

  public int[] reverseLifeline(int target, int nbPlaces);
}
