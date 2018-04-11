/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import apgas.glb.example.Sum;

/**
 * @author Patrick Finnerty
 *
 */
public class InheritanceTest {

  /** Computation ressource used for the test */
  static GLBProcessor processor;

  /**
   * Prepares a LoopGLBProcessor for computation
   */
  @BeforeClass
  public static void setUpBeforeClass() {
    processor = GLBProcessorFactory.LoopGLBProcessor(50, 1);
  }

  /**
   * Resets the computer back to a clean state
   */
  @Before
  public void setUp() {
    processor.reset();
  }

  /**
   * Tests a simple inheritance situation First will spawn an AMOUNT number of
   * Second instances which in turn are going to spawn the same amount of Sum(1)
   * instances. Hence the final expected result of Sum is AMOUNT.
   */
  @Test(timeout = 5000)
  public void inheritanceTest1() {
    final int RESULT = 400;
    final First bag = new First(RESULT);
    processor.addBag(bag);
    processor.compute();
    final Sum s = (Sum) processor.result().iterator().next();
    assertEquals(RESULT, s.sum);
  }

  /**
   * Parent class for {@link First} and {@link Second}
   *
   * @author Patrick Finnerty
   *
   */
  @SuppressWarnings("serial")
  private abstract class AbstractBag implements Bag<AbstractBag>, Serializable {

    /** Data shared amongst children */
    public int data;

    /** WorkCollector responsible for processing the tasks spawned */
    protected WorkCollector processor;

    /**
     * Constructor
     * <p>
     * Supplies the initial value for the data.
     *
     * @param d
     *          initial value for data.
     */
    protected AbstractBag(int d) {
      data = d;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(AbstractBag b) {
      data += b.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
      return data <= 0;
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
  }

  /**
   * Spawns {@link Second} instances as specified by the number given in the
   * constructor.
   *
   * @author Patrick Finnerty
   *
   */
  @SuppressWarnings("serial")
  private class First extends AbstractBag {

    /**
     * Construcotr
     *
     * @param d
     *          initial data value
     */
    protected First(int d) {
      super(d);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      while (data > 0 && workAmount > 0) {
        processor.giveBag(new Second(1));
        data--;
        workAmount--;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public AbstractBag split() {
      final int split = data / 2;
      data -= split;
      return new First(split);
    }
  }

  @SuppressWarnings("serial")
  private class Second extends AbstractBag {

    public Second(int i) {
      super(i);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public AbstractBag split() {
      final int split = data / 2;
      data -= split;
      return new Second(split);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      while (data > 0 && workAmount > 0) {
        processor.giveFold(new Sum(1));
        data--;
        workAmount--;
      }
    }
  }
}
