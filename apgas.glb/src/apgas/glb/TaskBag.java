/**
 *
 */
package apgas.glb;

/**
 * TaskBag presents the required methods for some Tasks to be processed
 * successfully by a {@link TaskBagProcessor}.
 *
 * @author Patrick Finnerty
 *
 */
public interface TaskBag<B extends TaskBag<B>> {

  /**
   * Processes a certain amount of tasks / work as specified by the parameter
   * and returns.
   *
   * @param workAmount
   *          number of tasks / amount of work to process
   */
  public void process(int workAmount);

  /**
   * Creates a new instance of TaskBag which contains tasks shared by this
   * instance. If no tasks can be shared, should return {@code null} rather than
   * an empty TaskBag.
   * <p>
   * As far as the splitting strategy is concerned (how much work is given,
   * specific , this is left to the programmer.
   *
   * @return A new TaskBag containing tasks shared by this instance, null if no
   *         tasks can be shared.
   */
  public B split();

  /**
   * Merges the task bag given as parameter into this instance.
   * <p>
   * Unlike {@link #split()} which can return {@code null}, the provided
   * parameter will never be null.
   *
   * @param b
   *          the tasks to be added to this task bag
   */
  public void merge(B b);

  /**
   * Indicates if the taskBag is empty, that is if all the tasks were performed.
   *
   * @return true if there are no tasks left in the TaskBag
   */
  public boolean isEmpty();

  /**
   * Sets the TaskProcessor in charge of computing this TaskBg. If at some point
   * the TaskBag creates some new TaskBag that should be computed by the task
   * processor, the value passed as paramater should be kept as a member of the
   * class.
   * <p>
   * When the TaskBag is split and transferred from one place to another, the
   * member is updated automatically by the taskProcessor. If the
   * {@link TaskBag} does not spawn any TaskBag, the implementation can be left
   * empty.
   * 
   * @param p
   *          the new {@link TaskBagProcessor} to be kept.
   */
  public void setProcessor(TaskBagProcessor p);
}
