/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import apgas.glb.example.Sum;

/**
 * Test class for {@link apgas.glb.LoopGLBProcessor} and
 * {@link GenericGLBProcessor}, implementations of interface
 * {@link GLBProcessor}
 *
 * @author Patrick Finnerty
 *
 */
@RunWith(Parameterized.class)
public class GLBProcessorTest {

  /**
   * LoopGLBProcessor instance used for the tests.
   */
  private GLBProcessor<Sum> processor = null;

  /**
   * Creates a new processor instance to be used during the tests
   *
   * @param computer
   *          GLBProcessor instance to be tested
   */
  public GLBProcessorTest(GLBProcessor<Sum> computer) {
    processor = computer;
  }

  /**
   * Tests the functioning of the folding mechanism using the Sum class
   *
   * @param value
   *          the sum value to be obtained
   */
  private void sum(int value) {
    processor.addBag(new SpawnSum(value));

    processor.compute();

    final Sum res = processor.result();
    assert res != null;

    assertEquals(value, res.sum);
  }

  /**
   * Resets the LoopGLBProcessor before a new computation is performed
   */
  @Before
  public void before() {
    processor.reset();
  }

  /**
   * Tests the behaviour of the {@link LoopGLBProcessor#reset()} method.
   */
  @Test(timeout = 5000)
  public void resetTest() {

    sum(10);
    processor.compute();
    processor.reset();

    Sum res = processor.result();
    assert res != null;
    assertEquals(0, res.sum);

    processor.compute(); // Re-launching empty computation
    res = processor.result();
    assertEquals(0, res.sum);
    assert res != null;
  }

  /**
   * Test the display of one display task
   */
  @Test(timeout = 5000)
  public void sumTest30() {
    sum(30);
  }

  /**
   * Tests the fold of 500 Sum of value 1 As 500 is above the work amount to be
   * processed by the LoopGLBProcessor, steal occurs.
   */
  @Test(timeout = 5000)
  public void sumTest500() {
    sum(500);
  }

  /**
   * Yields the {@link GLBProcessor} implementations to be tested.
   *
   * @return collection of GLBProcessor
   */
  @Parameterized.Parameters
  public static Collection<Object[]> toTest() {
    final Collection<Object[]> toReturn = new ArrayList<>();
    final GLBProcessor<Sum> a = GLBProcessorFactory.LoopGLBProcessor(500, 1,
        () -> new Sum(0));
    final Object[] first = { a };
    toReturn.add(first);
    final GLBProcessor<Sum> b = GLBProcessorFactory.GLBProcessor(500, 1,
        new HypercubeStrategy(), () -> new Sum(0));
    final Object[] second = { b };
    toReturn.add(second);
    return toReturn;
  }

  /**
   * {@link Bag} used to spawn a pre-determined number of Sum {@link Result}s
   *
   * @author Patrick Finnerty
   *
   */
  private class SpawnSum implements Bag<SpawnSum, Sum>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 6180125722875830207L;

    /** Number of Sum yet to spawn */
    int toSpawn;

    int result = 0;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      while (!isEmpty() && workAmount > 0) {
        result++;
        workAmount--;
        toSpawn--;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public SpawnSum split() {
      final int half = toSpawn / 2;
      toSpawn -= half;
      return new SpawnSum(half);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(SpawnSum b) {
      toSpawn += b.toSpawn;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
      return toSpawn == 0;
    }

    /**
     * Constructor
     *
     * @param qtt
     *          the number of Sum tasks to be spawned by this bag
     */
    public SpawnSum(int qtt) {
      toSpawn = qtt;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#submit()
     */
    @Override
    public void submit(Sum r) {
      r.sum += result;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#setWorkCollector(apgas.glb.WorkCollector)
     */
    @Override
    public void setWorkCollector(WorkCollector<Sum> p) { // Not used
    }
  }
}