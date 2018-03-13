/**
 *
 */
/**
 * Package glb contains the classes that implement a generic Lifeline-based
 * Global Load Balancing API relying on the APGAS (Asynchroneous Partitioned
 * Global Address Space) framework.
 * <p>
 * The main work for the programmer consists in breaking up its computation into
 * {@link apgas.glb.Task}. {@link apgas.glb.Task} will then be processed by the
 * {@link apgas.glb.TaskProcessor}. The internal functioning of the global load
 * balancer remains hidden from the programmer.
 *
 * @author Patrick
 */
package apgas.glb;