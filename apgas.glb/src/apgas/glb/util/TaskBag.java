/**
 *
 */
package apgas.glb.util;

import apgas.glb.Bag;
import apgas.glb.Fold;

/**
 * Interface {@link TaskBag} presents the services offered by its implementation
 * to the Task it contains.
 *
 * @author Patrick Finnerty
 *
 */
public interface TaskBag {

  /**
   * Adds an extra task in the {@link TaskBag}
   *
   * @param t
   *          the task to add
   */
  public void addTask(Task t);

  /**
   * Adds a new {@link Bag} to the computation
   *
   * @param <B>
   *          the type of {@link Bag} given as parameter
   * @param bag
   *          the {@link Bag} to be added
   */
  public <B extends Bag<B>> void addTaskBag(B bag);

  /**
   * Adds a new {@link Fold} to the computation
   * 
   * @param <F>
   *          the type of {@link Fold} given as parameter
   * @param fold
   *          the {@link Fold} to be added
   */
  public <F extends Fold<F>> void addFold(F fold);
}
