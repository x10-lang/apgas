/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Service provider for {@link Bag}. It allows {@link Bag} instances to submit a
 * new {@link Bag} instance to be processed by the {@link GLBProcessor} as part
 * of their {@link Bag#process(int)} method.
 * <p>
 * {@link WorkCollector} instance are provided to {@link Bag} instances through
 * their {@link Bag#setWorkCollector(WorkCollector)} method. The implementation
 * is free to keep the provided instance as a class member to be used later or
 * not to store the instance if it is not used.
 *
 * @author Patrick Finnerty
 *
 */
public interface WorkCollector<R extends Fold<R>> {

  /**
   * Adds the bag given as parameter to the bags to be processed in the current
   * computation.
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
