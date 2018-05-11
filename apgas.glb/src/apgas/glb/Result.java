/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Abstraction of a result computed by the {@link GLBProcessor}.
 * <p>
 * The {@link Result} interface can be seen as a binary operation whose operands
 * are two instances of the implementing class and whose result is also a
 * instance of the implementing class. This operation is embodied by the
 * {@link #fold(Result)} method: the operands are the given parameter {@code r}
 * and {@code this} and the result is When the {@link GLBProcessor} computation
 * ends, there will be as many {@link Result} instance as there were places used
 * for the computation. There is no guarantee as for the order in which these
 * (many) instances will be folded into a single instance. Therefore the
 * {@link #fold(Result)} implementation has to be symmetric in order for results
 * to be consistent.
 * <p>
 * Implementation classes should implement the interface with themselves as
 * parameter type as well as the {@link Serializable} interface. Below is a
 * simple example:
 *
 * <pre>
 * public class Sum implements Result&lt;Sum&gt;, Serializable {
 *
 *   private static final long serialVersionUID = 3582168956043482749L;
 *
 *   public int sum;
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
 * @param <R>
 *          implementing class itself (reflective method implementation)
 *
 * @author Patrick Finnerty
 *
 */
public interface Result<R extends Result<R>> {

  /**
   * Folds (merges) the given parameter's result into this instance.
   *
   * @param r
   *          the Result to be folded into {@code this}.
   */
  public void fold(R r);

}
