/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Arrays;

/**
 * {@link ConcurrentBagQueue} is the class used to handle {@link Bag} instances
 * in the {@link GenericGLBProcessor} implementation. It is identical to
 * {@link BagQueue}, albeit protected against concurrency accesses susceptible
 * to occur with the {@link GenericGLBProcessor} implementation.
 *
 * @author Patrick Finnerty
 * @see BagQueue
 *
 */
class ConcurrentBagQueue<R extends Fold<R> & Serializable>
    implements WorkCollector<R> {

  /**
   * Index of the last Bag that actually had some work in it. This value is
   * updated by method {@link #process(int)} as the successive {@link Bag}s
   * become empty.
   */
  private int lastPlaceWithWork = 0;

  /** First free index in the {@link #bags} array */
  private int last = 0;

  /** Array used to contain the {@link Bag}s */
  private Object bags[] = new Bag[16];

  /**
   * Doubles the capacity of the {@link #bags} array, copying the contained
   * {@link Bag}s to the new array.
   */
  private void grow() {
    bags = Arrays.copyOf(bags, bags.length * 2);
  }

  /**
   * Adds the {@link Bag} given as parameter to the {@link BagQueue}. If another
   * instance of the same class as the given parameter exists in the
   * {@link BagQueue}, it will merged into it. If there are no such existing
   * instance, the {@link Bag} is added at the {@link #last} index in the array.
   * If array {@link #bags} becomes full as a consequence, increases its size.
   *
   * @param <B>
   *          type of the bag to be added
   * @param b
   *          the bag to add to the queue
   */
  @Override
  public <B extends Bag<B, R> & Serializable> void giveBag(B b) {
    synchronized (this) {
      for (int i = 0; i < last; i++) {
        @SuppressWarnings("unchecked")
        final B a = (B) bags[i];
        if (b.getClass().getName().equals(a.getClass().getName())) {

          a.merge(b);
          return;
        }
      }
      // No existing instance of the same class in the queue, we add the work at
      // the end
      bags[last] = b;
      last++;
      if (last == bags.length) {
        grow();
      }
    }
  }

  /**
   * Removes all {@link Bag}s from the {@link ConcurrentBagQueue}. The
   * {@link ConcurrentBagQueue} is empty when this method returns.
   */
  public void clear() {
    for (int i = 0; i < bags.length; i++) {
      bags[i] = null;
    }
    last = 0;
    lastPlaceWithWork = 0;
  }

  /**
   * Indicates if this {@link TaskBag} is empty, that is that it does not
   * contain any {@link Bag} or that all the {@link Bag}s it contains are empty.
   *
   * @return true if empty, false otherwise
   */
  @SuppressWarnings("rawtypes")
  public boolean isEmpty() {
    synchronized (this) {
      if (last == 0) {
        return true;
      }
      final int i = lastPlaceWithWork;
      Bag b = (Bag) bags[lastPlaceWithWork];
      if (b.isEmpty()) {
        do {
          lastPlaceWithWork = (lastPlaceWithWork + 1) % last;
          b = (Bag) bags[lastPlaceWithWork];
          if (!b.isEmpty()) {
            return false;
          }
        } while (i != lastPlaceWithWork);
        return true;
      }
      return false;
    }
  }

  /**
   * Finds a {@link Bag} in the queue with work and processes the given amount
   * of work on it.
   *
   * @param workAmount
   *          amount of work to be processed
   * @see Bag#process(int)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void process(int workAmount) {
    synchronized (this) {
      final Bag b = (Bag) bags[lastPlaceWithWork];
      b.process(workAmount, this);
    }
  }

  /**
   * Gathers the result from the bags contained by this bag queue. The result
   * instance in which all the bags should store their result is given as
   * parameter.
   *
   * @param res
   *          result instance in which the results of the bags are to be stored
   * @see Bag#submit(Fold)
   */
  @SuppressWarnings("unchecked")
  public void result(R res) {
    for (int i = 0; i < last; i++) {
      ((Bag<?, R>) bags[i]).submit(res);
    }
  }

  /**
   * Tries to split the {@link Bag}s contained in the {@link ConcurrentBagQueue}
   * and returns the split. If no work could be be split from the any
   * {@link Bag}, returns null.
   *
   * @param <B>
   *          return type
   * @return some {@link Bag} instance or null is no split could be performed.
   */
  @SuppressWarnings("unchecked")
  public <B extends Bag<B, R> & Serializable> B split() {
    synchronized (this) {
      B split = null;
      for (int i = 0; i != last; i++) {
        split = ((B) bags[i]).split();
        if (split != null) {
          break;
        }
      }
      return split;
    }
  }

  /**
   * Constructor
   */
  public ConcurrentBagQueue() {
  }

}
