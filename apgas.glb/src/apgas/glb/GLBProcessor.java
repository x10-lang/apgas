/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Collection;

/**
 * Computing service abstraction. GLBProcessor uses a lifeline-based global load
 * balancing algorithm.
 * <p>
 * The work that can be handled by the GLBProcessor is a {@link Bag}. Initial
 * {@link Bag}s for your computation can be added to this instance by calling
 * {@link #addBag(Bag)}. Note that several {@link Bag}s can be given to the
 * GLBProcessor, as long as they all produce the type of {@link Fold} handled by
 * your instance.
 * <p>
 * Computation is launched using the {@link #compute()} method. The result can
 * then be obtained using the {@link #result()} method.
 * <p>
 * If one wishes to use same computing instance for several successive
 * computations, method {@link #reset()} should be called before adding the new
 * {@link Bag}s in order to avoid existing results from previous computations to
 * get mixed with the new ones.
 *
 * @author Patrick Finnerty
 *
 */
public interface GLBProcessor {

  /**
   * Launches the computation of the work given to the GLBProcessor and returns
   * the result.
   *
   * @param <R>
   *          result type the computation yields
   * @param <B>
   *          initial bag type, producing result of type <em>R</em>
   *
   * @param bag
   *          {@link Bag} to be processed
   * @param result
   *          neutral element instance of the result produced by the computation
   * @return computation result
   */
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      B bag, R result);

  /**
   * Launches the computation of the work given to the GLBProcessor and returns
   * the result.
   *
   * @param <R>
   *          result type the computation yields
   * @param <B>
   *          initial bag type, producing result of type <em>R</em>
   *
   * @param bags
   *          collection of {@link Bag} to be processed
   * @param result
   *          neutral element instance of the result produced by the computation
   * @return computation result
   */
  public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
      Collection<B> bags, R result);

  /**
   * Gives back the object which stores all the information about the runtime of
   * the last computation performed by the GLB. The instance of every place is
   * arranged in an array, the indeces correspond to the places' number.
   *
   * @return array of {@link Logger}, one logger for each place.
   */
  public Logger[] getLogger();
}
