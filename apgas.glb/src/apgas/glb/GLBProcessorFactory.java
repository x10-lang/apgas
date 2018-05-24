/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;

import apgas.Configuration;
import apgas.util.PlaceLocalObject;

/**
 * Factory class for {@link GLBProcessor}s.
 * <p>
 * Sets up GLBProcessors instances to make them readily available for
 * computation.
 *
 * @author Patrick Finnerty
 *
 */
public class GLBProcessorFactory {

  /**
   * Default number of places on which the computation is going to take place
   */
  public static final String DEFAULT_PLACE_COUNT = "4";

  /**
   * Creates a LoopGLBProcessor (factory method)
   * <p>
   * The returned LoopGLBProcessor will follow the provided configuration i.e. :
   * <ul>
   * <li>The amount of work to be processed by {@link Bag#process(int)} before
   * dealing with potential thieves
   * <li>The number of random steal attempts performed before turning to the the
   * lifeline-steal scheme.
   * </ul>
   *
   * @param <R>
   *          Type of the result for the created GLBProcessor
   * @param <S>
   *          Type for the Supplier of <R>
   * @param workUnit
   *          work amount processed by a place before dealing with thieves,
   *          <em>strictly positive</em>
   * @param stealAttempts
   *          number of steal attempts performed by a place before halting,
   *          <em>positive or nil</em>
   * @param resultInit
   *          Supplier of <R> type neutral element. Functional interface
   *          implementation, should use a lambda expression as parameter.
   * @return a new computing instance
   */
  public static GLBProcessor LoopGLBProcessor(int workUnit, int stealAttempts) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final LoopGLBProcessor glb = PlaceLocalObject.make(places(),
        () -> new LoopGLBProcessor(workUnit, stealAttempts));
    return glb;
  }

  /**
   * Creates a generic GLBProcessor following the given workUnit stealAttempts
   * and lifeline strategy provided.
   *
   * @param <S>
   *          Type of the strategy parameter
   * @param <G>
   *          Type of the result for the created GLBProcessor
   * @param workUnit
   *          amount of work to process before distributing work
   * @param stealAttempts
   *          number of random steals attempted before resulting to the lifeline
   *          scheme
   * @param strategy
   *          the lifelines strategy to be used in this GLBProcessor instance
   * @param resultInit
   *          Supplier of <R> type neutral element. Functional interface
   *          implementation, should use a lambda expression as parameter.
   * @return a new computing instance
   * @see LifelineStrategy
   */
  public static <S extends LifelineStrategy & Serializable> GLBProcessor GLBProcessor(
      int workUnit, int stealAttempts, S strategy) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final GLBProcessor glb = PlaceLocalObject.make(places(),
        () -> new GenericGLBProcessor(workUnit, stealAttempts, strategy));
    return glb;
  }
}
