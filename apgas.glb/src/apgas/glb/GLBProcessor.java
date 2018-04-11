/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author Patrick Finnerty
 *
 */
public interface GLBProcessor {

  /**
   * Allows to give some work to be computed by the GLBProcessor.
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
   * to make it clean for some new computation.
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
