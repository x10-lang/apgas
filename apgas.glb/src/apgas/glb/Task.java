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
   * task to call it if need be. Storing the given parameter is up to the task
   * to determine if it is needed. If the procedures do not need the
   * {@link TaskProcessor} in their computation, the method should be left
   * empty.
   *
   * @param p
   *          the {@link TaskProcessor} to set.
   */
  public void setTaskProcessor(TaskProcessor p);

  /**
   * Defines a weight for this task to make it possible to balance various tasks
   * among themselves by the task processor. The GLBProcessor processes a
   * certain amount of work before adressing potential thieves. This method
   * allows you to decide how much your task is worth.
   * <p>
   * For example if your application has 3 Tasks, A B and C which are equivalent
   * in terms of computation computation time required, you can simply choose
   * for your tasks to return the same weight, for instance 1. The
   * {@link GLBProcessor} will then process `workUnit` number of tasks before
   * answering thieves.
   * <p>
   * However if you expect Task B to take 10 times the execution time of tasks A
   * and C, you would choose 1, 10 and 1 as return values for Tasks A B and C
   * respectively.
   * <p>
   * If you wish to take into account the specific execution that took place in
   * your Task instance into account in order to give a specific weight which
   * varies between instances, it is safe to do so as the {@link GLBProcessor}
   * will call this method <em>after</em> the tasks's work has been processed.
   *
   * @return a positive integer representing the worth of this task
   */
  public int getWeight();
}
