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
 * Test class for {@link apgas.glb.LoopGLBProcessor}
 *
 * @author Patrick Finnerty
 *
 */
public class LoopGLBProcessorTest {

  /**
   * LoopGLBProcessor instance used for the tests.
   */
  private static LoopGLBProcessor processor = null;

  /**
   * Creates a new processor instance to be used during the tests
   */
  @BeforeClass
  public static void setup() {
    processor = LoopGLBProcessor.GLBProcessorFactory(50, 1);
  }

  /**
   * Tests the functioning of the folding mechanism using the Sum class
   *
   * @param value
   *          the sum value to be obtained
   */
  private void sum(int value) {
    processor.giveBag(new SpawnSum(value));

    processor.compute();

    @SuppressWarnings("rawtypes")
    final Collection<Fold> res = processor.result();
    assertEquals(1, res.size());
    assert (res.toArray()[0] instanceof Sum);

    final Sum s = (Sum) res.toArray()[0];
    assertEquals(value, s.sum);
  }

  /**
   * Tests the functionning of the folding mechanism of a Minimum class
   *
   * @param minimum
   *          the target result
   * @param qtt
   *          the number of Min instance to be generated
   */
  private void min(int minimum, int qtt) {
    processor.giveBag(new SpawnMinimum(minimum, qtt));
    processor.compute();

    @SuppressWarnings("rawtypes")
    final Collection<Fold> res = processor.result();
    assertEquals(1, res.size());
    assert (res.toArray()[0] instanceof Min);

    final Min m = (Min) res.toArray()[0];
    assertEquals(minimum, m.value);
  }

  /**
   * Resets the LoopGLBProcessor before a new computation is performed
   */
  @Before
  public void before() {
    processor.reset();
  }

  /**
   * Tests the fold of 30 Min instances. No work sharing in this case as the
   * number of tasks to process is lower than the workAmount given to the
   * {@link WorkCollector}
   */
  @Test
  public void minTest30() {
    min(0, 30);
  }

  /**
   * Tests the fold of 500 Min instances with work stealing being performed as
   * the number of tasks to process exceeds the work amount given to hte
   * {@link WorkCollector}.
   */
  @Test
  public void minTest500() {
    min(0, 500);
  }

  /**
   * Tests the behaviour of the {@link LoopGLBProcessor#reset()} method.
   */
  @Test
  public void resetTest() {
    min(0, 30);
    processor.reset();

    @SuppressWarnings("rawtypes")
    Collection<Fold> res = processor.result();
    assertEquals(0, res.size());

    processor.compute(); // Re-launching computation but should be empty

    res = processor.result(); // Result should still be empty
    assertEquals(0, res.size());
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
   * processed by the LoopGLBProcessor, steal occurs.
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

    /** WorkCollector in charge of this Bag */
    WorkCollector processor;

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
        processor.giveFold(new Sum(1));
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
     * @see apgas.glb.Bag#setProcessor(apgas.glb.WorkCollector)
     */
    @Override
    public void setWorkCollector(WorkCollector p) {
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

  /**
   * Class keeping the minimum value on integers
   *
   * @author Patrick Finnerty
   *
   */
  private class Min implements Fold<Min>, Serializable {
    /** Serial Version UID */
    private static final long serialVersionUID = 2117157664169192829L;
    /** Minimum value */
    public int value;

    @Override
    public void fold(Min m) {
      if (m.value < value) {
        value = m.value;
      }
    }

    /**
     * Constructor
     *
     * @param v
     *          the initial minimum value
     */
    public Min(int v) {
      value = v;
    }
  }

  private class SpawnMinimum implements Bag<SpawnMinimum>, Serializable {
    /** Serial version UID */
    private static final long serialVersionUID = 5783449607642360994L;
    int min;
    int qtt;
    WorkCollector processor;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      while (workAmount > 0 && qtt > 1) {
        processor.giveFold(new Min(min + 42));
        workAmount--;
        qtt--;
      }
      if (workAmount > 0) {
        processor.giveFold(new Min(min));
        qtt = 0;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public SpawnMinimum split() {
      final int toSend = qtt / 2;

      qtt -= toSend;

      return new SpawnMinimum(min, toSend);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(SpawnMinimum b) {
      min = b.min;
      qtt += b.qtt;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
      return qtt <= 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#setProcessor(apgas.glb.WorkCollector)
     */
    @Override
    public void setWorkCollector(WorkCollector p) {
      processor = p;
    }

    /**
     * Constructor
     *
     * @param m
     *          minimum value to be spawned
     * @param amount
     *          number of Min instance to be spawned
     */
    public SpawnMinimum(int m, int amount) {
      min = m;
      qtt = amount;
    }
  }
}