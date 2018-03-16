/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * @author Patrick Finnerty
 *
 */
public abstract class FoldTask extends Task implements Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -4660919112620208786L;

  /**
   * Constructor
   */
  public FoldTask() {
    super();
  }

  /**
   * Processes the FoldTask.
   * <p>
   * Should only be called if it is certain that the next Task in the queue is
   * also a fold task.
   */
  @Override
  public void process() {
  }

  /**
   * Fold method that performs the folding operation and adds itself to the task
   * queue
   *
   * @param t
   *          the other {@link FoldTask} to be folded with {@code this}
   */
  protected abstract void process(FoldTask t);

}
