/**
 *
 */
package apgas.glb.util;

/**
 * Abstract Task to be be run and handled by a {@link TaskBag}
 *
 * @author Patrick Finnerty
 *
 */
public interface Task {
  /**
   * Processes this task.
   * <p>
   * Can (possibly) spawn other tasks and send them in the queue by calling the
   * available methods of the {@link TaskBag} provided by
   * {@link #setProcessor(TaskBag)}.
   */
  public void process();

  /**
   * Set the {@link TaskBag} responsible for processing this task. If the task
   * needs to spawn new tasks, it should keep the provided {@link TaskBag} as a
   * class member. However if that is not the case, the implementation can
   * simply leave this method empty.
   *
   * @param p
   *          the {@link TaskBag} responsible for handling the {@link Task}
   */
  public void setProcessor(TaskBag p);
}
