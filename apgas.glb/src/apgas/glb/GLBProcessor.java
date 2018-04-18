/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Collection;

/**
 * Initial {@link Bag}s to be processed can be added to this instance by calling
 * {@link #addBag(Bag)}. Computation is launched using the {@link #compute()}
 * method. If the programmer wishes to use same computing instance for several
 * successive computations, method {@link #reset()} should be called before
 * adding the new {@link Bag}s to be processed to avoid existing results from
 * previous computations to interfere with the new ones.
 *
 * @author Patrick Finnerty
 *
 */
public interface GLBProcessor {

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
  public <B extends Bag<B> & Serializable> void addBag(B bag);

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
   * @return collection of {@link Fold}s
   */
  @SuppressWarnings("rawtypes")
  public Collection<Fold> result();
}
