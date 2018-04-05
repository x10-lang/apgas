/**
 *
 */
package apgas.glb;

/**
 * Fold provides the necessary methods for results to be gathered in a single
 * class by the {@link Processor}.
 * <p>
 * Implementation classes should implement the interface on themselves. Below is
 * a simple example of a {@link Fold} implementation :
 *
 * <pre>
 * public class Sum implements Fold&lt;Sum&gt;, Serializable {
 *
 *   private static final long serialVersionUID = 3582168956043482749L;
 *
 *   public int sum;
 *
 *   &#64;Override
 *   public void fold(Sum f) {
 *     sum += f.sum;
 *   }
 *
 *   public Sum(int s) {
 *     sum = s;
 *   }
 * }
 * </pre>
 *
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
