/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Abstraction of a result computed by the {@link GLBProcessor}. The programmer
 * can implement their own data structure implementing this insterface and use
 * it as a {@link Bag}'s second parameter type.
 * <p>
 * The {@link Fold} interface can be seen as a binary operation whose operands
 * are two instances of the implementing class and whose result is also a
 * instance of the implementing class. This operation is embodied by the
 * {@link #fold(Fold)} method: the operands are the given parameter {@code r}
 * and {@code this} and the result is {@code this} instance. When the
 * {@link GLBProcessor} computation ends, there will be as many {@link Fold}
 * implementation instances as there were places used for the computation. There
 * is no guarantee as for the order in which these (many) instances will be
 * folded into a single instance. Therefore the {@link #fold(Fold)}
 * implementation has to be symmetric in order for results to be consistent from
 * a computation to an other.
 * <p>
 * Implementation classes should implement the interface with themselves as
 * parameter type as well as the {@link Serializable} interface. Below is a
 * simple example:
 *
 * <pre>
 * public class Sum implements Fold&lt;Sum&gt;, Serializable {
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
 * @see Bag
 *
 */
public interface Fold<R extends Fold<R> & Serializable> {

  /**
   * Folds (merges) the given parameter's result into this instance.
   *
   * @param r
   *          the Fold to be folded into {@code this}.
   */
  public void fold(R r);

}
