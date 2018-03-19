/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Patrick Finnerty
 *
 */
public class TaskQueueTest {

  /**
   * Tasks used to manipulate the taskqueue
   */
  static private SomeTask a, b, c;

  /**
   * Initialises three Task to be used for TaskQueue testing
   */
  @BeforeClass
  public static void setup() {
    final TaskQueueTest t = new TaskQueueTest();
    a = t.new SomeTask();
    b = t.new SomeTask();
    c = t.new SomeTask();
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#add(apgas.glb.Task)}.
   */
  @Test
  public void testAdd() {
    final TaskQueue q = new TaskQueue();
    q.add(a);
    assertEquals(a, q.peep());

    for (int i = 2; i < 300; i++) {
      q.add(b);
      assertEquals(a, q.peep());
      assertEquals(i, q.size());

      i++;
      q.add(c);
      assertEquals(a, q.peep());
      assertEquals(i, q.size());
    }
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#isEmpty()}.
   */
  @Test
  public void testIsEmpty() {
    final TaskQueue t = new TaskQueue();
    assert (t.isEmpty());

    t.add(a);
    assert (!t.isEmpty());

    t.add(b);
    t.add(a);
    t.add(c);
    assert (!t.isEmpty());

    for (int i = 0; i < 4; i++) {
      t.pop();
    }
    assert (t.isEmpty());
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#merge(apgas.glb.TaskQueue)}.
   */
  @Test
  public void testMerge() {
    final TaskQueue q = new TaskQueue();
    final TaskQueue r = new TaskQueue();

    q.add(a);
    q.add(b);
    r.add(c);

    q.merge(r);

    assertEquals(3, q.size());
    final Collection<Task> collection = new ArrayList<>();
    while (!q.isEmpty()) {
      collection.add(q.pop());
    }

    assert collection.contains(a);
    assert collection.contains(b);
    assert collection.contains(c);
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#peep()}.
   */
  @Test
  public void testPeep() {
    final TaskQueue q = new TaskQueue();
    q.add(a);

    assertEquals(a, q.peep());
    assertEquals(1, q.size());

    q.pop();
    assertEquals(0, q.size());
    assert (q.isEmpty());
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#pop()}.
   */
  @Test
  public void testPop() {
    final TaskQueue q = new TaskQueue();

    q.add(a);
    assertEquals(a, q.pop());

    q.add(a);
    q.add(b);
    q.add(c);
    assertEquals(a, q.pop());
    assertEquals(b, q.pop());
    assertEquals(c, q.pop());
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#split()}.
   */
  @Test
  public void testSplit() {
    final TaskQueue q = new TaskQueue();
    q.add(a);
    q.add(b);

    final TaskQueue r = q.split();
    assertEquals(2, r.size() + q.size());

    final Collection<Task> c = new ArrayList<>();

    while (!q.isEmpty()) {
      c.add(q.pop());
    }

    while (!r.isEmpty()) {
      c.add(r.pop());
    }

    assertEquals(2, c.size());
    assert (c.contains(a));
    assert (c.contains(b));
  }

  /**
   * Test method for
   * {@link apgas.glb.TaskQueue#setProcessor(apgas.glb.TaskProcessor)}.
   */
  @Test
  public void testSetProcessor() {
    final TaskQueue q = new TaskQueue();
    q.add(a);
    q.add(b);
    q.add(c);

    final TaskProcessor p = new SomeTaskProcessor();

    q.setProcessor(p);
    while (!q.isEmpty()) {
      final Task t = q.pop();
      assertEquals(p, ((SomeTask) t).getTaskProcessor());
    }
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#size()}.
   */
  @Test
  public void testSize() {
    final TaskQueue q = new TaskQueue();
    assertEquals(0, q.size());

    for (int i = 1; i < TaskQueue.QUEUE_SIZE; i++) {
      q.add(a);
      assertEquals(i, q.size());
    }

    for (int i = q.size() - 1; i >= 0; i--) {
      q.pop();
      assertEquals(i, q.size());
    }
  }

  /**
   * Test method for {@link apgas.glb.TaskQueue#TaskQueue()}.
   */
  @Test
  public void testTaskQueue() {
    final TaskQueue q = new TaskQueue();
    assert (q.isEmpty());
    assertEquals(0, q.size());
  }

  /**
   * Dummy task used to test the TaskQueue
   *
   * @author Patrick Finnerty
   *
   */
  private class SomeTask implements ForkTask {

    private TaskProcessor processor;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Task#setTaskProcessor(apgas.glb.TaskProcessor)
     */
    @Override
    public void setTaskProcessor(TaskProcessor p) {
      processor = p;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.ForkTask#process()
     */
    @Override
    public void process() {
      // Do nohting
    }

    /**
     * Gives back the current processor of this task
     *
     * @return the current processor
     */
    public TaskProcessor getTaskProcessor() {
      return processor;
    }

  }

  /**
   * Dummy class used for testing
   *
   * @author Patrick Finnerty
   *
   */
  private class SomeTaskProcessor implements TaskProcessor {

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.TaskProcessor#addTask(apgas.glb.Task)
     */
    @Override
    public void addTask(Task t) {
      // not Used
    }

  }
}
