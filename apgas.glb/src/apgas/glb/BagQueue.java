/**
 *
 */
package apgas.glb;

import java.io.Serializable;

import apgas.glb.util.TaskBag;

/**
 * @author Patrick Finnerty
 *
 */
public class BagQueue {

  /** Index of the first bag in queue */
  private int first = 0;

  /** First free index in the bag queue */
  private int last = 0;

  /** Array used as circular buffer to contain {@link Bag}s */
  private Object bags[] = new Bag[16];

  /**
   * Doubles the capacity of the {@link #bags} array. Copying the contained
   * {@link Bag}s to the new array.
   */
  private void grow() {
    final Object newArray[] = new Object[bags.length * 2];
    int i = 0;
    while (first != last) {
      newArray[i] = bags[first];
      i++;
      first = (first + 1) % bags.length;
    }
    first = 0;
    last = i;
    bags = newArray;
  }

  /**
   * Adds the {@link Bag} given as parameter to the end of the {@link BagQueue}.
   * If another instance of the same class as the given parameter exists in the
   * {@link BagQueue}, it will merged into it.
   *
   * @param <B>
   *          parameter type
   * @param b
   *          the bag to add to the queue
   */
  public <B extends Bag<B> & Serializable> void addBag(B b) {
    synchronized (this) {
      for (int i = first; i != last; i = (i + 1) % bags.length) {
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
      last = (last + 1) % bags.length;
      if (first == last) {
        grow();
      }
    }
  }

  /**
   * Removes all {@link Bag}s from the {@link BagQueue}. The {@link BagQueue} is
   * empty when this method returns.
   */
  public void clear() {
    while (!isEmpty()) {
      bags[first] = null;
      first = (first + 1) % bags.length;
    }
  }

  /**
   * Indicates if this {@link TaskBag} is empty, that is that it does not
   * contain any {@link Bag}.
   *
   * @return true if empty, false otherwise
   */
  public boolean isEmpty() {
    synchronized (this) {
      return first == last;
    }
  }

  /**
   * Processes the given amount of work in the first Bag in the queue.
   *
   * @param workAmount
   *          amount of work to be processed
   */
  public void process(int workAmount) {
    synchronized (this) {
      @SuppressWarnings("rawtypes")
      final Bag b = (Bag) bags[first];
      b.process(workAmount);
      if (b.isEmpty()) {
        bags[first] = null;
        first = (first + 1) % bags.length;
      }
    }
  }

  /**
   * Tries to split the {@link Bag}s contained in the {@link BagQueue} and
   * returns the split. If no work could be be split from the any {@link Bag},
   * returns null.
   *
   * @param <B>
   *          type parameter
   * @return some {@link Bag} instance or null is no split could be performed.
   */
  @SuppressWarnings("unchecked")
  public <B extends Bag<B> & Serializable> B split() {
    synchronized (this) {
      B split = null;
      for (int i = first; i != last; i = (i + 1) % bags.length) {
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
   * <p>
   * Creates an empty BagQueue able to handle up to bagNumber different kind of
   * {@link Bag}s.
   */
  public BagQueue() {
  }

}
