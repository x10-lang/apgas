/**
 *
 */
package apgas.glb;

/**
 * Interface allowing {@link Task#process()} to add tasks to be performed by its
 * {@link Task#processor}
 *
 * @author Patrick Finnerty
 *
 */
public interface TaskProcessor {

  /**
   * Allows callers to add a task to be performed by the {@link TaskProcessor}.
   *
   * @param t
   *          the task to be added
   */
  public void addTask(Task t);
}
