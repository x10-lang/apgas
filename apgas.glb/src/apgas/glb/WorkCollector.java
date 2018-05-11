/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * {@link WorkCollector} provides services to the {@link Bag}s it is responsible
 * for, namely it enables {@link Bag}s to give it new {@link Bag}s to compute
 * and new {@link Result} to handle.
 *
 * @author Patrick Finnerty
 *
 */
public interface WorkCollector<R extends Result<R>> {

  /**
   * Adds the bag given as parameter to the bags to be processed by the
   * WorkCollector.
   * <p>
   * If an other instance of the same class as the {@link Bag} given as
   * parameter is already contained by the {@link WorkCollector}, they will be
   * {@link Bag#merge(Bag)}d together. If other cases, the Task bag given as
   * parameter will be kept as is.
   *
   * @param <B>
   *          the type of {@link Bag} to be added
   *
   * @param b
   *          the task bag to be added to the WorkCollector
   */
  public <B extends Bag<B, R> & Serializable> void giveBag(B b);
}
