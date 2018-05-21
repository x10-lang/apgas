/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import org.junit.Test;

import apgas.glb.example.Sum;
import apgas.glb.example.UTSBag;

/**
 * @author Patrick Finnerty
 *
 */
public class MultipleGLBProcessorTest {

  /**
   * Tests if multiple instances of GLBProcessors can be used at the same time.
   */
  @Test
  public void multipleGLBProcessorTest() {
    final GLBProcessor<Sum> a = GLBProcessorFactory.LoopGLBProcessor(100, 1);

    final UTSBag uts = new UTSBag(64);
    uts.seed(UTSBag.encoder(), 13, 13);

    final long RESULT = a.compute(uts, () -> new Sum(0)).sum;

    final GLBProcessor<Sum> b = GLBProcessorFactory.GLBProcessor(100, 1,
        new HypercubeStrategy());

    final UTSBag otherUts = new UTSBag(64);
    otherUts.seed(UTSBag.encoder(), 13, 13);

    final long bResult = b.compute(otherUts, () -> new Sum(0)).sum;
    assertEquals(RESULT, bResult);

    final UTSBag yetAnotherUts = new UTSBag(64);
    yetAnotherUts.seed(UTSBag.encoder(), 13, 13);

    final long aResult = a.compute(yetAnotherUts, () -> new Sum(0)).sum;
    assertEquals(RESULT, aResult);
  }

}
