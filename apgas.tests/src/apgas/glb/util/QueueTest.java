/**
 *
 */
package apgas.glb.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link apgas.glb.util.Queue}
 *
 * @author Patrick Finnerty
 *
 */
public class QueueTest {

  /**
   * Tasks used to manipulate the taskqueue
   */
  static private SomeTask a, b, c;

  /**
   * Initializes three Task to be used for Queue testing
   */
  @BeforeClass
  public static void setup() {
    final QueueTest t = new QueueTest();
    a = t.new SomeTask();
    b = t.new SomeTask();
    c = t.new SomeTask();
  }

  /**
   * Test method for {@link apgas.glb.util.Queue#add(apgas.glb.util.Task)}.
   */
  @Test
  public void testAdd() {
    final Queue q = new Queue();
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
   * Test method for {@link apgas.glb.util.Queue#isEmpty()}.
   */
  @Test
  public void testIsEmpty() {
    final Queue t = new Queue();
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
   * Test method for {@link apgas.glb.util.Queue#merge(Queue)}.
   */
  @Test
  public void testMerge() {
    final Queue q = new Queue();
    final Queue r = new Queue();

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
   * Test method for {@link apgas.glb.util.Queue#peep()}.
   */
  @Test
  public void testPeep() {
    final Queue q = new Queue();
    q.add(a);

    assertEquals(a, q.peep());
    assertEquals(1, q.size());

    q.pop();
    assertEquals(0, q.size());
    assert (q.isEmpty());
  }

  /**
   * Test method for {@link apgas.glb.util.Queue#pop()}.
   */
  @Test
  public void testPop() {
    final Queue q = new Queue();

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
   * Test method for {@link apgas.glb.util.Queue#split()}.
   */
  @Test
  public void testSplit() {
    final Queue q = new Queue();
    q.add(a);
    q.add(b);

    final Queue r = q.split();
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
   * Test method for {@link apgas.glb.util.Queue#size()}.
   */
  @Test
  public void testSize() {
    final Queue q = new Queue();
    assertEquals(0, q.size());

    for (int i = 1; i < Queue.QUEUE_SIZE; i++) {
      q.add(a);
      assertEquals(i, q.size());
    }

    for (int i = q.size() - 1; i >= 0; i--) {
      q.pop();
      assertEquals(i, q.size());
    }
  }

  /**
   * Test method for {@link apgas.glb.util.Queue#Queue()}.
   */
  @Test
  public void testQueue() {
    final Queue q = new Queue();
    assert (q.isEmpty());
    assertEquals(0, q.size());
  }

  /**
   * Tests the method {@link Queue#process(int)}
   */
  @Test
  public void testProcess() {
    final Queue q = new Queue();
    q.process(1); // Should not throw an error

    for (int i = 0; i < 50; i++) {
      q.add(new SomeTask());
    }
    assertEquals(50, q.size());

    q.process(10);
    assertEquals(40, q.size());

    q.process(50); // Again processing more tasks than there are in the Queue.
    assertEquals(0, q.size());
  }

  /**
   * Dummy task used to test the Queue
   *
   * @author Patrick Finnerty
   *
   */
  private class SomeTask implements Task {
    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.ForkTask#process()
     */
    @Override
    public void process() {
      // Do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see apgas.glb.util.Task#setProcessor(apgas.glb.util.TaskQueue)
     */
    @Override
    public void setProcessor(TaskQueue p) {
      // not used
    }

  }
}
