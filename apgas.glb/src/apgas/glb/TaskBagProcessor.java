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
}
