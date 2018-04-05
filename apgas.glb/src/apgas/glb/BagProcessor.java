/**
 *
 */
package apgas.glb;

/**
 * {@link BagProcessor} provides services to the {@link Bag}s it is responsible
 * for, namely it enables {@link Bag}s to give it new {@link Bag}s to compute
 * and new {@link Fold} to handle.
 *
 * @author Patrick Finnerty
 *
 */
public interface BagProcessor {

  /**
   * Adds the bag given as parameter to the bags to be processed by the
   * BagProcessor.
   * <p>
   * If an other instance of the same class as the {@link Bag} given as
   * parameter is already contained by the {@link BagProcessor}, they will be
   * {@link Bag#merge(Bag)}d together. If other cases, the Task bag given as
   * parameter will be kept as is.
   *
   * @param <B>
   *          the type of {@link Bag} to be added
   *
   * @param b
   *          the task bag to be added to the BagProcessor
   */
  public <B extends Bag<B>> void addTaskBag(B b);

  /**
   * Folds the given {@link Fold} into an existing {@link Fold} instance of the
   * same type or keeps it in order to be folded with later {@link Fold}s of the
   * same type.
   *
   * @param <F>
   *          the type of {@link Fold} to be folded
   * @param fold
   *          the {@link Fold} to be folded by the {@link BagProcessor}.
   */
  public <F extends Fold<F>> void fold(F fold);
}
