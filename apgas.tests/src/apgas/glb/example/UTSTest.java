/**
 *
 */
package apgas.glb.example;

import static org.junit.Assert.*;

import java.security.MessageDigest;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import apgas.glb.GLBProcessor;

/**
 * @author Patrick Finnerty
 *
 */
public class UTSTest {

  /** Depth of the tree to be explored */
  static final int TREE_DEPTH = 13;

  /** Seed used for the tree */
  static final int SEED = 19;

  /**
   * Expected result for the number of nodes of the tree of depth 13 and seed 19
   */
  static final int EXPECTED_NODES = 264459392;

  /** GLBProcessor used to perform the computation */
  static GLBProcessor processor;

  /** Message dialect used by UTS computation to generate the tree */
  static MessageDigest MD;

  /** Setup method */
  @BeforeClass
  public static void setup() {
    processor = GLBProcessor.GLBProcessorFactory(500, 1);
    MD = UTSBag.encoder();

    final UTSBag taskBag = new UTSBag(64);
    processor.giveBag(taskBag);
    taskBag.seed(MD, SEED, TREE_DEPTH - 2);

    processor.compute();

  }

  /** Cleaning performed between tests */
  @Before
  public void beforeTest() {
    processor.reset();
  }

  /**
   * Tests the termination in reasonable time of the unbalanced tree search
   * computation
   */
  @Test(timeout = 30000)
  public void UTS() {
    final UTSBag bag = new UTSBag(64);
    processor.giveBag(bag);
    bag.seed(MD, SEED, TREE_DEPTH);

    processor.compute();

    final long count = ((Sum) processor.result().toArray()[0]).sum;
    assertEquals(EXPECTED_NODES, count);
  }

}
