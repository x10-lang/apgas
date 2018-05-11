/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * {@link Result} provides the necessary methods for results to be gathered in a
 * single class by the {@link GLBProcessor}.
 * <p>
 * Implementation classes should implement the interface on themselves as well
 * as the {@link Serializable} interface. Below is a simple example:
 *
 * <pre>
 * public class Sum implements Result&lt;Sum&gt;, Serializable {
 *
 *   private static final long serialVersionUID = 3582168956043482749L;
 *
 *   public int sum;
 *   public String type;
 *
 *   &#64;Override
 *   public void fold(Sum r) {
 *     sum += f.sum;
 *   }
 *
 *   &#64;Override
 *   public
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
public interface Result<R extends Result<R>> {

  /**
   * Folds the given parameter into this instance.
   * <p>
   * This method is called when the {@link Result} instances located at each
   * place are merged (folded) back into a single instance before being returned
   * to the user.
   *
   * @param f
   *          the Result to be folded into {@code this}.
   */
  public void fold(R r);

}
