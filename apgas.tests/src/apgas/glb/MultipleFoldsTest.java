/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests that if several instances of {@link Fold}s with different
 * {@link Fold#id()} return Strings are indeed folded separately.
 *
 * @author Patrick Finnerty
 *
 */
@RunWith(Parameterized.class)
public class MultipleFoldsTest {

  private static final String KEY_A = "hundred";

  private static final String KEY_B = "one";

  private static final int MIN_VALUE = 100;

  /** GLBProcessor instance in test */
  private final GLBProcessor processor;

  /**
   * Puts the GLBProcessor instance back in a ready to cumpute state.
   */
  @Before
  public void before() {
    processor.reset();
  }

  /**
   * Tests that two instances of Sum are not folded together when their
   * {@link Fold#id()} return value is different.
   */
  @SuppressWarnings("rawtypes")
  @Test(timeout = 10000)
  public void multipleFoldTest() {
    processor.addBag(new FoldSpawn(100, 100, 0));
    processor.compute();

    Sum sumHundred = null;
    Sum sumOne = null;
    int nbFolds = 0;
    final Iterator<Fold> it = processor.result().iterator();
    while (it.hasNext()) {
      nbFolds++;
      final Fold f = it.next();
      if (f.id() == KEY_A) {
        sumHundred = (Sum) f;
      } else if (f.id() == KEY_B) {
        sumOne = (Sum) f;
      }
    }
    assertEquals(2, nbFolds);
    assertEquals(100, sumOne.sum);
    assertEquals(10000, sumHundred.sum);
  }

  /**
   * Tests that two Fold instances that have the same {@link Fold#id()} return
   * value are not folded together.
   */
  @SuppressWarnings("rawtypes")
  @Test(timeout = 10000)
  public void differentClassSameIdTest() {
    processor.addBag(new FoldSpawn(100, 100, 0));
    processor.compute();

    Min min = null;
    Sum sum = null;
    int nbFolds = 0;
    final Iterator<Fold> it = processor.result().iterator();
    while (it.hasNext()) {
      nbFolds++;
      final Fold f = it.next();
      if (f.id() == "hundred") {
        fail("Unexpected key : <hundred>");
      } else if (f.id() == "one") {

        if (f instanceof Sum) {
          sum = (Sum) f;
        } else if (f instanceof Min) {
          min = (Min) f;
        }
      }
    }
    assertEquals(2, nbFolds);
    assertEquals(100, sum.sum);
    assertEquals(MIN_VALUE, min.min);
  }

  /**
   * Constructor
   *
   * @param p
   *          the GLBProcessor instance to be tested
   */
  public MultipleFoldsTest(GLBProcessor p) {
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

  private class FoldSpawn implements Bag<FoldSpawn>, Serializable {

    private static final long serialVersionUID = 6060367976925949517L;
    int nbSumHundred;
    int nbSumOne;
    int nbMin;

    WorkCollector collector;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#process(int)
     */
    @Override
    public void process(int workAmount) {
      while (workAmount > 0 && nbSumHundred > 0 && nbSumOne > 0 && nbMin > 0) {
        nbSumHundred--;
        nbSumOne--;
        nbMin--;
        collector.giveFold(new Sum(100, KEY_B));
        collector.giveFold(new Sum(1, KEY_A));
        collector.giveFold(new Min(MIN_VALUE, KEY_B));
        workAmount--;
      }

      while (workAmount > 0 && nbSumHundred > 0) {
        nbSumHundred--;
        collector.giveFold(new Sum(100, KEY_B));
        workAmount--;
      }

      while (workAmount > 0 && nbSumOne > 0) {
        nbSumOne--;
        collector.giveFold(new Sum(1, KEY_A));
        workAmount--;
      }

      while (workAmount > 0 && nbMin > 0) {
        nbMin--;
        collector.giveFold(new Min(MIN_VALUE, KEY_B));
        workAmount--;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#split()
     */
    @Override
    public FoldSpawn split() {
      final int hundredToGive = nbSumHundred / 2;
      final int oneToGive = nbSumOne / 2;
      final int minToGive = nbMin / 2;

      nbSumHundred -= hundredToGive;
      nbSumOne -= oneToGive;
      nbMin -= minToGive;

      return new FoldSpawn(hundredToGive, oneToGive, minToGive);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#merge(apgas.glb.Bag)
     */
    @Override
    public void merge(FoldSpawn b) {
      nbSumHundred += b.nbSumHundred;
      nbSumOne += b.nbSumOne;
      nbMin += b.nbMin;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
      return nbMin == 0 && nbSumHundred == 0 && nbSumOne == 0;
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

    public FoldSpawn(int sumHundred, int sumOne, int min) {
      nbSumHundred = sumHundred;
      nbSumOne = sumOne;
      nbMin = min;
    }

  }

  private class Min implements Fold<Min>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 1939234687984405861L;

    /** Min term */
    int min;

    /** String key */
    String key;

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Fold#fold(apgas.glb.Fold)
     */
    @Override
    public void fold(Min f) {
      if (min > f.min) {
        min = f.min;
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glb.Fold#id()
     */
    @Override
    public String id() {
      return key;
    }

    public Min(int m, String k) {
      min = m;
      key = k;
    }
  }

  private class Sum implements Fold<Sum>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 1939234687984405861L;

    /** Sum term */
    int sum;

    /** String key */
    String key;

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
      return key;
    }

    public Sum(int s, String k) {
      sum = s;
      key = k;
    }
  }
}
