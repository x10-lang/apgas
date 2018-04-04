/**
 *
 */
package apgas.glb;

/**
 * @author Patrick Finnerty
 *
 */
public interface TaskBagProcessor {

  /**
   * Adds the task bag given as parameter to the task processor.
   * <p>
   * If an other instance of the same class as the {@link TaskBag} given as
   * parameter is already contained by the {@link TaskBagProcessor}, they will
   * be {@link TaskBag#merge(TaskBag)}d together. If other cases, the Task bag
   * given as parameter will be kept as is.
   *
   * @param <B>
   *          the type of {@link TaskBag} to be added
   *
   * @param b
   *          the task bag to be added to the TaskBagProcessor
   */
  public <B extends TaskBag<B>> void addTaskBag(B b);

  /**
   * Folds the given {@link Fold} into either an existing {@link Fold} instance
   * of the same type or keeps it in order to be folded with later {@link Fold}s
   * of the same type.
   *
   * @param fold
   *          the {@link Fold} to be folded by the {@link TaskBagProcessor}.
   */
  public <F extends Fold<F>> void fold(F fold);
}
