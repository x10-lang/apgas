/**
 *
 */
package apgas.glb;

/**
 * @author Patrick Finnerty
 *
 */
public interface Fold<F extends Fold<F>> {

  /**
   * Folds the given parameter into this instance
   * 
   * @param f
   *          the Fold to be folded into {@code this}.
   */
  public void fold(F f);
}
