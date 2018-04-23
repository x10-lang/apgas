/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * {@link Fold} provides the necessary methods for results to be gathered in a
 * single class by the {@link WorkCollector}.
 * <p>
 * Implementation classes should implement the interface on themselves as well
 * as the {@link Serializable} interface. Below is a simple example:
 *
 * <pre>
 * public class Sum implements Fold&lt;Sum&gt;, Serializable {
 *
 *   private static final long serialVersionUID = 3582168956043482749L;
 *
 *   public int sum;
 *   public String type;
 *
 *   &#64;Override
 *   public void fold(Sum f) {
 *     sum += f.sum;
 *   }
 *
 *   &#64;Override
 *   public String id() {
 *     return type;
 *   }
 *
 *   public Sum(int s, String t) {
 *     sum = s;
 *     type = t;
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

  /**
   * Gives back a String used to distinguish several {@link Fold}s that are
   * instances of the same class yet bear different meaning.
   * <p>
   * Assume your application counts apples and pears. You would use the same
   * class {@code Sum} to count both of them. However, in order to distinguish
   * between the two, instances that are counting apples should return a String
   * {@code "apple"} while the instances of the pear counting sum should return
   * {@code "pear"} when this method is called.
   * <p>
   * This feature is only necessary when your application requires several
   * instances of the same class to be treated as different {@link Fold}s. If
   * that is not the case, the implementation of this method can symply return
   * the empty {@link String}.
   *
   * @return any {@link String} that helps identify the fold.
   */
  public String id();
}
