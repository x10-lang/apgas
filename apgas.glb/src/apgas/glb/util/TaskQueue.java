/**
 *
 */
package apgas.glb.util;

import apgas.glb.Fold;
import apgas.glb.TaskBag;

/**
 * Interface {@link TaskQueue} presents the services offered by its
 * implementation to the Task it contains.
 *
 * @author Patrick Finnerty
 *
 */
public interface TaskQueue {

  /**
   * Adds an extra task in the {@link TaskQueue}
   * 
   * @param t
   *          the task to add
   */
  public void addTask(Task t);

  /**
   * Adds a new {@link TaskBag} to the computation
   * 
   * @param bag
   *          the {@link TaskBag} to be added
   */
  public <B extends TaskBag<B>> void addTaskBag(B bag);

  /**
   * Adds a new {@link Fold} to the computation
   * 
   * @param fold
   *          the {@link Fold} to be added
   */
  public <F extends Fold<F>> void addFold(F fold);
}
