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
 * second parameter <em>R</em> is the kind of result this {@link Bag} yields and
 * should be an implementation of inteface {@link Result}
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
 *
 */
public interface Bag<B extends Bag<B, R>, R extends Result<R>> {

  /**
   * Indicates if the taskBag is empty, that is if all the tasks were performed.
   *
   * @return true if there are no tasks left in the Bag
   */
  public boolean isEmpty();

  /**
   * Merges the task bag given as parameter into this instance.
   * <p>
   * Unlike {@link #split()} which can return {@code null}, the provided
   * parameter will never be null (this is checked by the
   * {@link WorkCollector}).
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
   * Allows the Bag to submit its result into the provided (user defined) data
   * structure.
   *
   * @return new instance of the user defined {@link Result} data structure
   */
  public R submit();

  /**
   * Creates a new instance of Bag which contains tasks shared by this instance
   * to be given to an other computation place. If no tasks can be shared,
   * should return {@code null} rather than an empty Bag.
   * <p>
   * As far as the bag splitting strategy is concerned (how much work is given),
   * this is left to the programmer.
   *
   * @return A new Bag containing tasks shared by this instance, null if no
   *         tasks can be shared.
   */
  public B split();

  /**
   * Sets a WorkCollector which will handle work spawned by this Bag. If at some
   * point the Bag needs to create some new Bag that should be computed, a
   * {@link WorkCollector} should be kept as a member of the class and be set by
   * calling this method.
   * <p>
   * When the Bag is split and transferred from one place to another, the member
   * is updated automatically by the {@link WorkCollector}. If the {@link Bag}
   * does not spawn any Bag, the implementation of this method can be left
   * empty.
   *
   * @param p
   *          the new {@link WorkCollector} instance to be kept.
   */
  public void setWorkCollector(WorkCollector<R> p);
}
