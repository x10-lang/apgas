/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;

import apgas.Configuration;
import apgas.util.PlaceLocalObject;

/**
 * @author Patrick Finnerty
 *
 */
public class GLBProcessorFactory {

  /** Default number of tasks to process before responding to thieves */
  public static final int DEFAULT_WORK_UNIT = 40;

  /**
   * Number of attempted random steals before the place skips to its lifeline
   * steal strategy
   */
  public static final int DEFAULT_RANDOM_STEAL_ATTEMPTS = 1;

  /**
   * Default number of places on which the computation is going to take place
   */
  public static final String DEFAULT_PLACE_COUNT = "4";

  /**
   * Creates a LoopGLBProcessor (factory method)
   * <p>
   * This yields a LoopGLBProcessor using default configuration.
   *
   * @return a new computing instance
   * @see #GLBProcessorFactory(int, int)
   */
  public static GLBProcessor LoopGLBProcessor() {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final LoopGLBProcessor glb = PlaceLocalObject.make(places(),
        () -> new LoopGLBProcessor(DEFAULT_WORK_UNIT,
            DEFAULT_RANDOM_STEAL_ATTEMPTS));
    return glb;
  }

  /**
   * Creates a LoopGLBProcessor (factory method)
   * <p>
   * The returned LoopGLBProcessor will follow the provided configuration that
   * is :
   * <ul>
   * <li>The number of work to be processed by {@link Bag#process(int)} before
   * dealing with potential thieves
   * <li>The number of random steal attempts performed before turning to the the
   * lifeline-steal scheme.
   * </ul>
   *
   * @param workUnit
   *          work amount processed by a place before dealing with thieves,
   *          <em>strictly positive</em>
   * @param stealAttempts
   *          number of steal attempts performed by a place before halting,
   *          <em>positive or nil</em>
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
   * and strategy provided
   *
   * @param workUnit
   *          amount of work to process before distributing work
   * @param stealAttempts
   *          number of random steals attempted before resulting to the lifeline
   *          scheme
   * @param strategy
   *          the lifelines strategy to be used in this GLBProcessor instance
   * @return a new computing instance
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
