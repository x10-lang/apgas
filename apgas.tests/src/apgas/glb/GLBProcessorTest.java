/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link apgas.glb.GLBProcessor}
 *
 * @author Patrick Finnerty
 *
 */
public class GLBProcessorTest {

  /**
   * GLBProcessor instance used for the tests.
   */
  private static GLBProcessor processor = null;

  /**
   * Creates a new processor instance to be used during the tests
   */
  @BeforeClass
  public static void setup() {
    processor = GLBProcessor.GLBProcessorFactory(50, 1);
  }

  /**
   * Tests the functioning of the folding mechanism using the Sum class
   * 
   * @param value
   *          the sum value to be obtained
   */
  private void sum(int value) {
    processor.addTaskBag(new SpawnSum(value));

    processor.compute();

    @SuppressWarnings("rawtypes")
    final Collection<Fold> res = processor.result();
    assertEquals(1, res.size());
    assert (res.toArray()[0] instanceof Sum);

    final Sum s = (Sum) res.toArray()[0];
    assertEquals(value, s.sum);
  }

  /**
   * Resets the GLBProcessor before a new computation is performed
   */
  @Before
  public void before() {
    processor.reset();
  }

  /**
   * Test the display of one display task
   */
  @Test
  public void sumTest30() {
    sum(30);
  }

  /**
   * Tests the fold of 500 Sum of value 1 As 500 is above the work amount to be
   * processed by the GLBProcessor, steal occurs.
   */
  @Test
  public void sumTest500() {
    sum(500);
  }

  /**
   * Simple Task class that performs the sum on integers.
   *
   * @author Patrick Finnerty
   *
   */
  private class Sum implements Fold<Sum>, Serializable {
    /** Serial Version UID */
    private static final long serialVersionUID = -3766700434988512611L;
    /** Message to be displayed by this task */
    int sum;

    @Override
    public void fold(Sum s) {
      sum += s.sum;
    }

    /**
     * Constructor
     *
     * @param s
     *          value to be added
     */
    public Sum(int s) {
      sum = s;
    }
  }

  /**
   * {@link Bag} used to spawn a pre-determined number of Sum {@link Fold}s
   *
   * @author Patrick Finnerty
   *
   */
  private class SpawnSum implements Bag<SpawnSum>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 6180125722875830207L;

    /** Processor in charge of this Bag */
    Processor processor;

    /** Number of Sum yet to spawn */
    int toSpawn;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      while (!isEmpty() && workAmount > 0) {
        processor.fold(new Sum(1));
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

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#setProcessor(apgas.glb.Processor)
     */
    @Override
    public void setProcessor(Processor p) {
      processor = p;
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

  }
}