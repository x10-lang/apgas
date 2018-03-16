/**
 *
 */
package apgas.glb;

/**
 * @author Patrick Finnerty
 *
 */
public interface FoldTask extends Task {

  /**
   * Fold method that performs the folding operation and adds itself to the task
   * queue
   *
   * @param t
   *          the other {@link FoldTask} to be folded with {@code this}
   */
  public void process(FoldTask t);

}
