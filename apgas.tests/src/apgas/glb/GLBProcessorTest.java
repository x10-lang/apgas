/**
 *
 */
package apgas.glb;

import java.io.Serializable;

import org.junit.Test;

import apgas.glb.util.TaskQueue;
import apgas.glb.util.Task;
import apgas.glb.util.TaskBag;

/**
 * Test class for {@link apgas.glb.GLBProcessor}
 *
 * @author Patrick Finnerty
 *
 */
public class GLBProcessorTest {

  /**
   * Test the display of one display task
   */
  @Test
  public void display1TaskTest() {
    display(1, 50);
  }

  private void display(int tasknumber, int workUnit) {
    final TaskQueue queue = new TaskQueue();
    for (int i = 0; i < tasknumber; i++) {
      queue.add(new DisplayTask(Integer.toString(i)));
    }

    final GLBProcessor p = GLBProcessor.GLBProcessorFactory(workUnit, 1);

    p.addTaskBag(queue);
    p.compute();
  }

  /**
   * Simple Task class that implements a task which consists in displaying a
   * String given as parameter for the constructor.
   *
   * @author Patrick Finnerty
   *
   */
  private class DisplayTask implements Task, Serializable {
    /** Serial Version UID */
    private static final long serialVersionUID = -3766700434988512611L;
    /** Message to be displayed by this task */
    String message;

    /**
     * Constructor
     *
     * @param m
     *          the String to display
     */
    public DisplayTask(String m) {
      message = m;
    }

    /**
     * Displays the string on the standard output
     */
    @Override
    public void process() {
      System.out.println(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see apgas.glb.util.Task#setProcessor(apgas.glb.util.TaskBag)
     */
    @Override
    public void setProcessor(TaskBag p) {
      // Not used
    }

  }
}