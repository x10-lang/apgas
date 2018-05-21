/**
 *
 */
package apgas.glb;

import static org.junit.Assert.*;

import org.junit.Test;

import apgas.glb.GLBProcessor;
import apgas.glb.GLBProcessorFactory;
import apgas.glb.HypercubeStrategy;
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
    final GLBProcessor<Sum> a = GLBProcessorFactory.LoopGLBProcessor(100, 1,
        () -> new Sum(0));

    final UTSBag uts = new UTSBag(64);
    uts.seed(UTSBag.encoder(), 13, 13);

    a.addBag(uts);
    a.compute();

    final long RESULT = a.result().sum;

    final GLBProcessor<Sum> b = GLBProcessorFactory.GLBProcessor(100, 1,
        new HypercubeStrategy(), () -> new Sum(0));

    final UTSBag otherUts = new UTSBag(64);
    otherUts.seed(UTSBag.encoder(), 13, 13);

    b.addBag(otherUts);
    b.compute();
    assertEquals(RESULT, b.result().sum);

    final UTSBag yetAnotherUts = new UTSBag(64);
    yetAnotherUts.seed(UTSBag.encoder(), 13, 13);

    a.reset();
    a.addBag(yetAnotherUts);
    a.compute();
    assertEquals(RESULT, a.result().sum);
  }

}
