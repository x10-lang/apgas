/**
 *
 */
package apgas.glb.util;

import apgas.glb.TaskBagProcessor;

/**
 * Abstract Task to be be run.
 *
 * @author Patrick Finnerty
 *
 */
public interface Task {
  /**
   * Processes this task.
   * <p>
   * Can (possibly) spawn other tasks and send them in the queue by calling the
   * available methods of the {@link TaskBagProcessor} provided by
   * {@link #setProcessor(TaskBagProcessor)}
   */
  public void process();

  /**
   * Set the {@link TaskBagProcessor} responsible for processing this task. If
   * the task needs to spawn new tasks, it can then add them to this
   * {@link TaskBagProcessor} which should be kept as a class member. However if
   * that is not the case, the implementation can simply leave this method
   * empty.
   *
   * @param p
   *          the {@link TaskBagProcessor}
   */
  public void setProcessor(TaskBagProcessor p);
}
