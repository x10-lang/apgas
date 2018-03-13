/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Abstract Task to be be run. Method {@link #process()} is here for
 * specialization : user can extend this class to add the necessary data and
 * implement {@link #process()}. Derived classes need to implement
 * {@link Serializable} and need not to contain any java.util data structures as
 * they cannot be processed by serialization when relocating Tasks.
 *
 * @author Patrick Finnerty
 *
 */
public abstract class Task {

  /**
   * Processor which is going to call {@link #process()}. Can be asked to
   * perform certain actions in {@link #process()} such as adding a task to its
   * queue. This field is set at the {@link TaskProcessor}'s initiative,
   * specialization of {@link Task} do not need to worry about it.
   */
  protected TaskProcessor processor = null;

  /**
   * Processes this task.
   * <p>
   * Can (possibly) spawn other tasks and send them in the queue by calling the
   * available methods of the {@link #processor}.
   */
  public abstract void process();

  /**
   * Allows {@link TaskProcessor}s to claim this task as their own, enabling the
   * task to call it if need be.
   *
   * @param p
   *          the {@link TaskProcessor} to set.
   */
  public void setTaskProcessor(TaskProcessor p) {
    processor = p;
  }

  /**
   * Constructor
   */
  public Task() {
  }
}
