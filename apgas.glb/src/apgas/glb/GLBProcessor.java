/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Computing service abstraction. GLBProcessor uses a lifeline-based global load
 * balancing algorithm.
 * <p>
 * The work that can be handled by the GLBProcessor is a {@link Bag}. Initial
 * {@link Bag}s for your computation can be added to this instance by calling
 * {@link #addBag(Bag)}. Note that several {@link Bag}s can be given to the
 * GLBProcessor, as long as they all produce the type of {@link Fold} handled
 * by your instance.
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
public interface GLBProcessor<R extends Fold<R> & Serializable> {

  /**
   * Allows to give some work to be computed by the GLBProcessor.
   * <p>
   * Several {@link Bag}s can be given to the {@link GLBProcessor} before
   * launching the computation, however if two bags of the same class are given,
   * the second one will overwrite the first.
   *
   * @param <B>
   *          Class implementing interface {@link Bag} on itself and
   *          Serializable
   * @param bag
   *          the work to be computed by the GLBProcessor
   */
  public <B extends Bag<B, R> & Serializable> void addBag(B bag);

  /**
   * Launches the computation of the work given to the GLBProcessor. The results
   * will then be available by calling the {@link #result()} method.
   * <p>
   * Note that if you add some extra computation without calling
   * {@link #reset()} beforehand, the existing {@link Bag}s and {@link Fold}s
   * from the previous computation will still be there and might interfere with
   * your next computation.
   */
  public void compute();

  /**
   * Discards all {@link Bag}s and {@link Fold}s remaining in the GLBProcessor
   * to make it clean and ready for some new computation.
   */
  public void reset();

  /**
   * Gives back the {@link Fold}s computed by the {@link GLBProcessor} in the
   * previous computation.
   *
   * @return instance of the user-defined Fold instance
   */
  public R result();
}
