/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;

import org.junit.Test;

/**
 * @author Patrick Finnerty
 *
 */
public class GLBProcessorTest {

  /**
   * Tests the merging mechanism of the GLBProcessor on a small scale
   */
  @Test
  public void testSum1Task() {
    final Sum s = new Sum(1);

    // We use a very low work unit to make steals happen often
    final GLBProcessor p = GLBProcessor.GLBProcessorFactory(1, 1);
    p.addTask(s);

    p.launchComputation();

    final Sum result = (Sum) p.result();
    assertEquals(1, result.sum);
  }

  /**
   * Tests a single merge operation (two Sum instances as original tasks)
   */
  @Test
  public void testSum2Tasks() {
    final Sum s = new Sum(1);

    // We use a very low work unit to make steals happen often
    final GLBProcessor p = GLBProcessor.GLBProcessorFactory(1, 1);

    // Adding two tasks sum of 1 and 1 should yield 2.
    p.addTask(s);
    p.addTask(s.copy());

    p.launchComputation();

    final Sum result = (Sum) p.result();
    assertEquals(2, result.sum);
  }

  /**
   * Tests the merging sum of 50 sum elements of initial value 1
   */
  @Test
  public void testSum50Tasks() {
    sum(50, 5);
  }

  /**
   * Tests the merging sum of 50 sum elements of initial value 1
   */
  @Test
  public void testSum250Tasks() {
    sum(250, 5);
  }

  /**
   * Tests the minimum computation with 50 elements.
   */
  @Test
  public void testMin50Tasks() {
    min(50, 5);
  }

  /**
   * Tests the minimum computation with 250 elements.
   */
  @Test
  public void testMin250Tasks() {
    min(250, 10);
  }

  /**
   * Computes the sum of nbSum Sum tasks of initial value one and checks the
   * result
   *
   * @param nbSum
   *          the number of Sum tasks to be added to the GKBProcessor initially
   * @param workUnit
   *          the workunit setting of the GLBProcessor
   */
  private void sum(int nbSum, int workUnit) {
    final Sum s = new Sum(1);

    final GLBProcessor p = GLBProcessor.GLBProcessorFactory(workUnit, 1);
    p.addTask(s);

    for (int i = 1; i < nbSum; i++) {
      p.addTask(s.copy());
    }

    p.launchComputation();

    final Sum result = (Sum) p.result();
    assertEquals(nbSum, result.sum);
  }

  /**
   * Computes the minimum value among all integers between 0 and nbValues. The
   * expected result if of course 0.
   *
   * @param nbValues
   *          the number of initial {@link Min} values to be added to the
   *          {@link GLBProcessor}
   * @param workUnit
   *          the workUnit setting for the created {@link GLBProcessor}.
   */
  private void min(int nbValues, int workUnit) {
    final GLBProcessor p = GLBProcessor.GLBProcessorFactory(workUnit, 1);

    for (int i = 0; i < nbValues; i++) {
      p.addTask(new Min(i));
    }

    p.launchComputation();
    final Min result = (Min) p.result();
    assertEquals(0, result.min);
  }

  /**
   * Fold task performing the sum operation on integers
   *
   * @author Patrick Finnerty
   *
   */
  private class Sum implements FoldTask, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2606773637334864644L;
    private int sum;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Task#setTaskProcessor(apgas.glb.TaskProcessor)
     */
    @Override
    public void setTaskProcessor(TaskProcessor p) {
      // not used
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.FoldTask#process(apgas.glb.FoldTask)
     */
    @Override
    public void process(FoldTask t) {
      sum += ((Sum) t).sum;

    }

    /**
     * Creates a new instance of {@code this} with the same value for the sum.
     *
     * @return a new instance with identical sum value
     */
    public Sum copy() {
      return new Sum(sum);
    }

    /**
     * Constructor
     *
     * @param a
     *          the initial value
     */
    public Sum(int a) {
      sum = a;
    }

  }

  /**
   * Implements the minimum reduction operation for integers
   *
   * @author Patrick Finnerty
   *
   */
  private class Min implements FoldTask, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -9160902745114102741L;

    /** Minimum value carried by {@code this} */
    int min;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Task#setTaskProcessor(apgas.glb.TaskProcessor)
     */
    @Override
    public void setTaskProcessor(TaskProcessor p) {
      // unused
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.FoldTask#process(apgas.glb.FoldTask)
     */
    @Override
    public void process(FoldTask t) {
      final int otherMin = ((Min) t).min;

      if (otherMin < min) {
        min = otherMin;
      }
    }

    /**
     * Constructor
     *
     * @param a
     *          the minimum value
     */
    public Min(int a) {
      min = a;
    }
  }
}
