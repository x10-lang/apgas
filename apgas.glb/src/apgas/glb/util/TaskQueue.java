/**
 *
 */
package apgas.glb.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.NoSuchElementException;

import apgas.glb.Bag;
import apgas.glb.Fold;
import apgas.glb.Processor;

/**
 * Class managing the queue of tasks to be performed by the {@link Processor}.
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
public class TaskQueue implements Bag<TaskQueue>, TaskBag, Serializable {

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

  private Processor processor = null;

  /**
   * Adds the given task to the end of the task queue. If the tasks array
   * becomes full as a result, it is expanded to accommodate for extra new
   * tasks.
   *
   * @param t
   *          the task to be added
   */
  public void add(Task t) {
    t.setProcessor(this);
    tasks[lastIndex] = t;
    lastIndex = (lastIndex + 1) % tasks.length;

    if (lastIndex == firstIndex) {
      // Overflow problem, the array is full
      int newLast = 0;
      final Task newTaskArray[] = new Task[QUEUE_SIZE + tasks.length];
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
  public void merge(TaskQueue q) {
    while (!q.isEmpty()) {
      final Task t = q.pop();
      t.setProcessor(this);
      add(t);
    }
  }

  /**
   * Gives the first Task in the {@link TaskQueue} without removing it from the
   * TaskQueue
   *
   * @return the first Task in the TaskQueue or null if the TaskQueue is empty
   */
  public Task peep() {
    if (isEmpty()) {
      return null;
    } else {
      return tasks[firstIndex];
    }
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
  @Override
  public TaskQueue split() {
    final Task[] sharedTasks = tasks.clone();

    final int elements = size() / 2; // number of elements to be passed on

    final int splitIndex = (tasks.length + lastIndex - elements) % tasks.length;
    final int newTaskLastIndex = lastIndex;
    lastIndex = splitIndex;

    return new TaskQueue(sharedTasks, splitIndex, newTaskLastIndex);
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

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Bag#process(int)
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
   * @see apgas.glb.Bag#setProcessor(apgas.glb.Processor)
   */
  @Override
  public void setProcessor(Processor p) {
    processor = p;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.util.TaskBag#addTask(apgas.glb.util.Task)
   */
  @Override
  public <T extends Task & Serializable> void addTask(T t) {
    add(t);
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.util.TaskBag#addTaskBag(apgas.glb.Bag)
   */
  @Override
  public <B extends Bag<B> & Serializable> void addTaskBag(B bag) {
    processor.addTaskBag(bag);
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.util.TaskBag#addFold(apgas.glb.Fold)
   */
  @Override
  public <F extends Fold<F> & Serializable> void addFold(F fold) {
    processor.fold(fold);
  }
}
