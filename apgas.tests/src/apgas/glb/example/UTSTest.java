/**
 *
 */
package apgas.glb.example;

import static org.junit.Assert.*;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import apgas.glb.GLBProcessor;
import apgas.glb.GLBProcessorFactory;
import apgas.glb.HypercubeStrategy;

/**
 * Tests that the computation of the UTS completes in reasonable time
 *
 * @author Patrick Finnerty
 *
 */
@RunWith(Parameterized.class)
public class UTSTest {

  /** Depth of the tree to be explored */
  static final int TREE_DEPTH = 13;

  /** Seed used for the tree */
  static final int SEED = 19;

  /**
   * Expected result for the number of nodes of the tree of depth 13 and seed 19
   */
  static final int EXPECTED_NODES = 264459392;

  /** LoopGLBProcessor used to perform the computation */
  static GLBProcessor processor;

  /** Message dialect used by UTS computation to generate the tree */
  static MessageDigest MD;

  /**
   * Constructor
   *
   * @param p
   *          GLBProcessor to be tested
   */
  public UTSTest(GLBProcessor p) {
    processor = p;
    MD = UTSBag.encoder();

    final UTSBag taskBag = new UTSBag(64);
    taskBag.seed(MD, SEED, TREE_DEPTH - 2);
    processor.compute(taskBag, new Sum(0));
  }

  /**
   * Tests the termination of the unbalanced tree search computation. The
   * timeout is set such that if there is an issue with the GLB procedures which
   * blocks a thread, the test will fail thanks to the timeout.
   */
  @Test(timeout = 20000)
  public void UTS() {
    final UTSBag bag = new UTSBag(64);
    bag.seed(MD, SEED, TREE_DEPTH);

    final Sum s = processor.compute(bag, new Sum(0));

    assert s != null;
    assertEquals(EXPECTED_NODES, s.sum);
  }

  /**
   * Creates the {@link GLBProcessor} implementation to be tested.
   *
   * @return collection of arguments to be given to the constructor of this test
   *         class.
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
}
