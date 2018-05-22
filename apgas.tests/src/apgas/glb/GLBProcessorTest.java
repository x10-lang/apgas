/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import apgas.Constructs;
import apgas.glb.example.Sum;
import apgas.glb.example.UTSBag;

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

  private static final int UTS_SEED = 19;
  private static final int UTS_DEPTH = 13;
  private static final long UTS_EXPECTED_NODES = 264459392;

  /**
   * LoopGLBProcessor instance used for the tests.
   */
  private GLBProcessor processor = null;

  /**
   * Creates a new processor instance to be used during the tests
   *
   * @param computer
   *          GLBProcessor instance to be tested
   */
  public GLBProcessorTest(GLBProcessor computer) {
    processor = computer;
  }

  /**
   * Tests the functioning of the folding mechanism using the Sum class
   *
   * @param value
   *          the sum value to be obtained
   */
  private void sum(int value) {

    final Sum res = processor.compute(new SpawnSum(value), new Sum(0));
    assert res != null;

    assertEquals(value, res.sum);
  }

  /**
   * Test with small number of tasks (no steals performed)
   */
  @Test(timeout = 5000)
  public void sumTest30() {
    sum(30);
  }

  /**
   * Tests the fold of 500 Sum of value 1 As 500 is above the work amount to be
   * processed by the GLBProcessor, steals occur.
   */
  @Test(timeout = 5000)
  public void sumTest500() {
    sum(500);
  }

  /**
   * Tests that several computations submitted to the same instance
   * synchronously are handled correctly.
   */
  @Test(timeout = 30000)
  public void testSeveralComputations() {
    final SpawnSum firstComputation = new SpawnSum(500);
    final UTSBag secondComputation = new UTSBag(64);
    secondComputation.seed(UTSBag.encoder(), UTS_SEED, UTS_DEPTH);

    final Sum first = new Sum(0);
    final Sum second = new Sum(0);

    Constructs.finish(() -> {
      Constructs.async(() -> {
        first.fold(processor.compute(firstComputation, new Sum(0)));
        second.fold(processor.compute(secondComputation, new Sum(0)));
      });
    });

    assertEquals(500, first.sum);
    assertEquals(UTS_EXPECTED_NODES, second.sum);
  }

  /**
   * Tests that bags having the same result type have their result effectively
   * merged together.
   *
   * @param <B>
   *          type parameter used to pass parameters to the GLBPRocessor
   */
  @SuppressWarnings("unchecked")
  @Test(timeout = 30000)
  public <B extends Bag<B, Sum> & Serializable> void testMultipleInitialBags() {
    final SpawnSum firstComputation = new SpawnSum(500);
    final UTSBag secondComputation = new UTSBag(64);
    secondComputation.seed(UTSBag.encoder(), UTS_SEED, UTS_DEPTH);

    final Collection<B> initialBags = new ArrayList<>();
    initialBags.add((B) firstComputation);
    initialBags.add((B) secondComputation);

    final Sum s = processor.compute(initialBags, new Sum(0));

    assertEquals(UTS_EXPECTED_NODES + 500, s.sum);
  }

  /**
   * Yields the {@link GLBProcessor} implementations to be tested.
   *
   * @return collection of GLBProcessor
   */
  @Parameterized.Parameters
  public static Collection<Object[]> toTest() {
    final Collection<Object[]> toReturn = new ArrayList<>();
    final GLBProcessor a = GLBProcessorFactory.LoopGLBProcessor(500, 1);
    final Object[] first = { a };
    toReturn.add(first);
    final GLBProcessor b = GLBProcessorFactory.GLBProcessor(500, 1,
        new HypercubeStrategy());
    final Object[] second = { b };
    toReturn.add(second);
    return toReturn;
  }

  /**
   * {@link Bag} used to spawn a pre-determined number of Sum {@link Fold}s
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
      if (half > 0) {
        toSpawn -= half;
        return new SpawnSum(half);
      } else {
        return null;
      }
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
