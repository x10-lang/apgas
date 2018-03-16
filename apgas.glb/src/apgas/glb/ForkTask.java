/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Method {@link #process()} is here for specialization : user can add the
 * necessary data to the computation and implement {@link #process()} in their
 * own class. Said class implementing this interface also need to implement
 * {@link Serializable} and need not to contain any java.util data structures as
 * they cannot be processed by serialization when relocating Tasks.
 *
 * @author Patrick Finnerty
 *
 */
public interface ForkTask extends Task {

  /**
   * Processes this task.
   * <p>
   * Can (possibly) spawn other tasks and send them in the queue by calling the
   * available methods of the {@link #processor}.
   */
  public void process();
}
