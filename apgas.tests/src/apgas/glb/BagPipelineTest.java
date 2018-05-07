/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests that a pipeline of {@link Bag}s is properly executed by the
 * GLBProcessor implementations
 *
 * @author Patrick Finnerty
 *
 */
@RunWith(Parameterized.class)
public class BagPipelineTest {

  /** Instance in testing */
  private final GLBProcessor processor;

  /**
   * Tests a simple to tasks pipeline.
   */
  @Test(timeout = 5000)
  public void pipelineTest() {
    final int RESULT = 500;
    processor.addBag(new FirstBag(RESULT));
    processor.compute();
    @SuppressWarnings("rawtypes")
    final Collection<Fold> res = processor.result();
    assertEquals(1, res.size());
    final Fold<?> f = res.iterator().next();
    assert f instanceof Sum;
    final Sum s = (Sum) f;
    assertEquals(RESULT, s.sum);
  }

  /**
   * Resets the computer for a new computation
   */
  @Before
  public void setup() {
    processor.reset();
  }

  /**
   * Constructor
   *
   * @param p
   *          the GLBProcessor instance to be tested
   */
  public BagPipelineTest(GLBProcessor p) {
    processor = p;
  }

  /**
   * Yields the {@link GLBProcessor} implementation to be tested.
   *
   * @return collection of GLBProcessor
   */
  @Parameterized.Parameters
  public static Collection<Object[]> toTest() {
    return Arrays.asList(
        new Object[] { GLBProcessorFactory.LoopGLBProcessor(50, 1) },
        new Object[] {
            GLBProcessorFactory.GLBProcessor(50, 1, new HypercubeStrategy()) });
  }

  private class FirstBag implements Bag<FirstBag>, Serializable {

    private static final long serialVersionUID = -6717229606753663109L;
    private int qtt;
    private WorkCollector collector;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      int amount = 0;
      while (workAmount > 0 && qtt > 0) {
        workAmount--;
        qtt--;
        amount++;
      }
      collector.giveBag(new SecondBag(amount));
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public FirstBag split() {
      final int split = qtt / 2;
      if (split == 0) {
        return null;
      } else {
        qtt -= split;
        return new FirstBag(split);
      }

    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(FirstBag b) {
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
     * @see apgas.glb.Bag#setWorkCollector(apgas.glb.WorkCollector)
     */
    @Override
    public void setWorkCollector(WorkCollector p) {
      collector = p;
    }

    /**
     * Constructor
     *
     * @param amount
     *          amount of work, result of the final Sum
     */
    public FirstBag(int amount) {
      qtt = amount;
    }
  }

  private class SecondBag implements Bag<SecondBag>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2184650631781234106L;

    private int qtt;

    private WorkCollector collector;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      int amount = 0;
      while (workAmount > 0 && qtt > 0) {
        workAmount--;
        qtt--;
        amount++;
      }
      collector.giveFold(new Sum(amount));
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public SecondBag split() {
      final int split = qtt / 2;
      if (split == 0) {
        return null;
      } else {
        qtt -= split;
        return new SecondBag(split);
      }

    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(SecondBag b) {
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
     * @see apgas.glb.Bag#setWorkCollector(apgas.glb.WorkCollector)
     */
    @Override
    public void setWorkCollector(WorkCollector p) {
      collector = p;
    }

    /**
     * Constructor
     *
     * @param amount
     *          amount of work, result of the final Sum
     */
    public SecondBag(int amount) {
      qtt = amount;
    }
  }

  private class Sum implements Fold<Sum>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 1939234687984405861L;

    /** Sum term */
    int sum;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Fold#fold(apgas.glb.Fold)
     */
    @Override
    public void fold(Sum f) {
      sum += f.sum;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Fold#id()
     */
    @Override
    public String id() {
      return "";
    }

    public Sum(int s) {
      sum = s;
    }
  }
}
