/**
 *
 */
package apgas.glb;

/**
 * Abstract Task to be be run.
 *
 * @author Patrick Finnerty
 *
 */
interface Task {
  /**
   * Allows {@link TaskProcessor}s to claim this task as their own, enabling the
   * task to call it if need be.
   *
   * @param p
   *          the {@link TaskProcessor} to set.
   */
  public void setTaskProcessor(TaskProcessor p);
}
