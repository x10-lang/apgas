/**
 *
 */
package apgas.glb;

import java.io.Serializable;

/**
 * Abstraction of work to be processed by a {@link GLBProcessor}.
 * <p>
 * There are two type parameters to the interface, <em>B</em> and <em>R</em>.
 * The first parameter <em>B</em> should be the implementing class itself. The
 * second parameter <em>R</em> is the kind of result this {@link Bag} produces
 * and should be an implementation of inteface {@link Fold}
 * <P>
 * Moreover, in order for {@link Bag} implementations to be processed
 * successfully by the {@link GLBProcessor}, they will also need to implement
 * the {@link Serializable} interface.
 * <p>
 * Example:
 *
 * <pre>
 * public class MyBag implements Bag&lt;MyBag, MyResult&gt;, Serializable {
 * 
 *   private static final long serialVersionUID = 3582168956043482749L;
 *   // implementation ...
 * }
 * </pre>
 *
 * @author Patrick Finnerty
 * @see Fold
 *
 */
public interface Bag<B extends Bag<B, R>, R extends Fold<R>> {

  /**
   * Indicates if the taskBag is empty, that is if there is no more work to
   * process.
   *
   * @return true if there are no tasks left in the Bag
   */
  public boolean isEmpty();

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
   * Processes a certain amount of work as specified by the parameter and
   * returns. If there is less work than the given parameter to be done,
   * processes the remaining work until the {@link Bag} is empty.
   *
   * @param workAmount
   *          amount of work to process
   */
  public void process(int workAmount);

  /**
   * Allows the Bag to submit its result into the user-defined data structure
   * given as parameter.
   *
   * @param r
   *          the instance in which this bag's result are to be stored
   */
  public void submit(R r);

  /**
   * Creates a new instance of Bag which contains a fraction of the work to be
   * process and returns it. If no tasks can be shared, should return
   * {@code null} rather than an empty Bag.
   * <p>
   * As far as the bag splitting strategy is concerned (how much work is given
   * with a split), this is left to the programmer. Of course, the programmer
   * will be careful and remove the work given from this instance so that it is
   * not processed twice.
   *
   * @return A new Bag containing work shared by this instance, null if no work
   *         can be shared.
   */
  public B split();

  /**
   * Sets a {@link WorkCollector} which will collect work spawned by this Bag.
   * If at some point the {@link Bag} needs to create some new Bag that should
   * be computed, the {@link WorkCollector} given as parameter should be kept as
   * a member of the class. Calls to the {@link WorkCollector} services can then
   * be made in the {@link #process(int)} method.
   * <p>
   * When the Bag is split and transferred from one place to another, the member
   * is updated. If the {@link Bag} does not spawn any {@link Bag}, the
   * implementation of this method should be left empty.
   *
   * @param p
   *          the new {@link WorkCollector} instance to be kept.
   */
  public void setWorkCollector(WorkCollector<R> p);
}
