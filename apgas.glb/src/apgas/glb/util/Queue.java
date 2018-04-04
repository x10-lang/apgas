/**
 *
 */
package apgas.glb.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.NoSuchElementException;

import apgas.glb.TaskBag;
import apgas.glb.TaskBagProcessor;

/**
 * Class managing the queue of tasks to be performed by the
 * {@link TaskBagProcessor}.
 * <p>
 * The tasks to be performed are stored in an array but behave as a double-ended
 * queue. Two indices, {@link #firstIndex} and {@link #lastIndex} keep track of
 * the top and bottom of the queue respectively. The queue loops around the
 * whole array but is not overflow-proof : if the number of waiting tasks
 * reaches the maximum size of {@link #tasks}, the queue will appear to be
 * empty. Tasks are added at the bottom of the queue and processed from the top.
 *
 * @author Patrick Finnerty
 *
 */
public class Queue<T extends Task> implements TaskBag<Queue<T>>, Serializable {

  /** Default size of the array {@link #tasks} containing the tasks */
  public static final int QUEUE_SIZE = 10;

  /** Serial Version UID */
  private static final long serialVersionUID = 1257150853488353701L;

  /** Container for the tasks */
  private Object[] tasks;

  /** First occupied index in the array */
  private int firstIndex;

  /** First free index in the array */
  private int lastIndex;

  /**
   * Adds the given task to the end of the task queue. If the tasks array
   * becomes full as a result, it is expanded to accommodate for extra new
   * tasks.
   *
   * @param t
   *          the task to be added
   */
  public void add(T t) {
    tasks[lastIndex] = t;
    lastIndex = (lastIndex + 1) % tasks.length;

    if (lastIndex == firstIndex) {
      // Overflow problem, the array is full
      int newLast = 0;
      @SuppressWarnings("unchecked")
      final T newTaskArray[] = (T[]) Array.newInstance(t.getClass(),
          QUEUE_SIZE + tasks.length);
      do {
        newTaskArray[newLast] = pop();
        newLast++;
      } while (!isEmpty());
      firstIndex = 0;
      lastIndex = newLast;
      tasks = newTaskArray;
      return;
    }
  }

  /**
   * Indicates if there are no tasks left in the queue
   *
   * @return {@code true} if the {@link Queue} if empty, {@code false} otherwise
   */
  @Override
  public boolean isEmpty() {
    return lastIndex == firstIndex;
  }

  /**
   * Adds all the tasks contained in the {@link Collection} passed as parameter
   * in the task queue
   *
   * @param q
   *          the queue of tasks to be added
   */
  @Override
  public void merge(Queue<T> q) {
    while (!q.isEmpty()) {
      add(q.pop());
    }
  }

  /**
   * Gives the first Task in the {@link Queue} without removing it from the
   * Queue
   *
   * @return the first Task in the Queue or null if the Queue is empty
   */
  @SuppressWarnings("unchecked")
  public T peep() {
    if (isEmpty()) {
      return null;
    } else {
      return (T) tasks[firstIndex];
    }
  }

  /**
   * Pops the first task in the queue and returns it.
   *
   * @return the first task in the queue
   * @throws NoSuchElementException
   *           is there is no task in the queue
   */
  public T pop() throws NoSuchElementException {
    @SuppressWarnings("unchecked")
    final T t = (T) tasks[firstIndex];
    firstIndex = (firstIndex + 1) % tasks.length;
    return t;
  }

  /**
   * Removes half of the current tasks of this tasks queue and return the
   * collection of the removed element (presumably to be {@link #merge(Queue)}d
   * in an other instance of GLBProcessor.
   *
   * @return Collection of Tasks removed from this task queue.
   */
  @Override
  public Queue<T> split() {
    final Object[] sharedTasks = tasks.clone();

    final int elements = size() / 2; // number of elements to be passed on

    final int splitIndex = (tasks.length + lastIndex - elements) % tasks.length;
    final int newTaskLastIndex = lastIndex;
    lastIndex = splitIndex;

    return new Queue<>(sharedTasks, splitIndex, newTaskLastIndex);
  }

  /**
   * Returns the number of tasks in the Queue
   *
   * @return the number of tasks in the queue
   */
  public int size() {
    return (tasks.length + lastIndex - firstIndex) % tasks.length;
  }

  /**
   * Constructor
   * <p>
   * Creates an empty Queue
   */
  public Queue() {
    tasks = new Object[QUEUE_SIZE];
    firstIndex = 0;
    lastIndex = 0;
  }

  /**
   * Private constructor used when splitting the queue in method
   * {@link #split()}
   *
   * @param c
   *          array containing the tasks to be sent starting at index 0
   * @param first
   *          the index of the task to be processed
   * @param last
   *          the index of the first free slot in the task array
   */
  private Queue(Object[] c, int first, int last) {
    tasks = c;
    firstIndex = first;
    lastIndex = last;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.TaskBag#process(int)
   */
  @Override
  public void process(int workAmount) {
    while (workAmount > 0 && !isEmpty()) {
      pop().process();
      workAmount--;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.TaskBag#setProcessor(apgas.glb.TaskBagProcessor)
   */
  @Override
  public void setProcessor(TaskBagProcessor p) {
    for (int i = firstIndex; i < lastIndex; i = (i + 1) % tasks.length) {
      ((Task) tasks[i]).setProcessor(p);
    }

  }
}
