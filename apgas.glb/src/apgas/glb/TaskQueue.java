/**
 *
 */
package apgas.glb;

import java.io.Serializable;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Class managing the queue of tasks to be performed by the
 * {@link TaskProcessor}.
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
class TaskQueue implements Serializable {

  /** Default size of the array {@link #tasks} containing the tasks */
  public static final int QUEUE_SIZE = 10;

  /** Serial Version UID */
  private static final long serialVersionUID = 1257150853488353701L;

  /** Container for the tasks */
  private Task[] tasks;

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
  public void add(Task t) {
    tasks[lastIndex] = t;
    lastIndex = (lastIndex + 1) % tasks.length;

    if (lastIndex == firstIndex) {
      // Overflow problem, the array is full
      int newLast = 0;
      final Task newTaskArray[] = new Task[tasks.length + QUEUE_SIZE];
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
   * @return {@code true} if the {@link TaskQueue} if empty, {@code false}
   *         otherwise
   */
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
  public void merge(TaskQueue q) {
    while (!q.isEmpty()) {
      add(q.pop());
    }
  }

  /**
   * Gives the first Task in the {@link TaskQueue} without removing it from the
   * TaskQueue
   *
   * @return the first Task in the Queue or null if the Queue is empty
   */
  public Task peep() {
    return isEmpty() ? null : tasks[firstIndex];
  }

  /**
   * Pops the first task in the queue and returns it.
   *
   * @return the first task in the queue
   * @throws NoSuchElementException
   *           is there is no task in the queue
   */
  public Task pop() throws NoSuchElementException {
    final Task t = tasks[firstIndex];
    firstIndex = (firstIndex + 1) % tasks.length;
    return t;
  }

  /**
   * Removes half of the current tasks of this tasks queue and return the
   * collection of the removed element (presumably to be
   * {@link #merge(TaskQueue)}d in an other instance of GLBProcessor.
   *
   * @return Collection of Tasks removed from this task queue.
   */
  public TaskQueue split() {
    final Task sharedTasks[] = tasks.clone();

    final int elements = size() / 2; // number of elements to be passed on

    final int splitIndex = (tasks.length + lastIndex - elements) % tasks.length;
    final int newTaskLastIndex = lastIndex;
    lastIndex = splitIndex;

    return new TaskQueue(sharedTasks, splitIndex, newTaskLastIndex);
  }

  /**
   * Allows a TaskProcessor to appropriate oneself with all the tasks in this
   * {@link TaskQueue}. Needs to be performed by the {@link TaskProcessor} which
   * received work from another place : if the tasks need to ask something from
   * their {@link Task#processor}, it will then be up to date.
   *
   * @param p
   *          the task processor to be assigned to each task of this
   *          {@link TaskQueue}
   */
  public void setProcessor(TaskProcessor p) {
    int i = firstIndex;
    while (i < lastIndex) {
      tasks[i].setTaskProcessor(p);
      i++;
    }
  }

  /**
   * Returns the number of tasks in the TaskQueue
   *
   * @return the number of tasks in the queue
   */
  public int size() {
    return (tasks.length + lastIndex - firstIndex) % tasks.length;
  }

  /**
   * Constructor
   * <p>
   * Creates an empty TaskQueue
   */
  public TaskQueue() {
    tasks = new Task[QUEUE_SIZE];
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
  private TaskQueue(Task[] c, int first, int last) {
    tasks = c;
    firstIndex = first;
    lastIndex = last;
  }
}
