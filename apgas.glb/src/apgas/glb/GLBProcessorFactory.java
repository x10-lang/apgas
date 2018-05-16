/**
 *
 */
package apgas.glb;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.function.Supplier;

import apgas.Configuration;
import apgas.util.PlaceLocalObject;

/**
 * Factory class for {@link GLBProcessor}s.
 * <p>
 * Prepares GLBProcessor and returns them.
 *
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
   * @param <R>
   *          Type of the result for the created GLBProcessor
   *
   * @return a new computing instance
   * @see #LoopGLBProcessor(int, int)
   */
  public static <R extends Result<R> & Serializable, S extends Supplier<R> & Serializable> GLBProcessor<R> LoopGLBProcessor(
      S resultInit) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final LoopGLBProcessor<R> glb = PlaceLocalObject.make(places(),
        () -> new LoopGLBProcessor<>(DEFAULT_WORK_UNIT,
            DEFAULT_RANDOM_STEAL_ATTEMPTS, resultInit));
    return glb;
  }

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
   * @param workUnit
   *          work amount processed by a place before dealing with thieves,
   *          <em>strictly positive</em>
   * @param stealAttempts
   *          number of steal attempts performed by a place before halting,
   *          <em>positive or nil</em>
   *
   * @return a new computing instance
   */
  public static <R extends Result<R> & Serializable, S extends Supplier<R> & Serializable> GLBProcessor<R> LoopGLBProcessor(
      int workUnit, int stealAttempts, S resultInit) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final LoopGLBProcessor<R> glb = PlaceLocalObject.make(places(),
        () -> new LoopGLBProcessor<>(workUnit, stealAttempts, resultInit));
    return glb;
  }

  /**
   * Creates a generic GLBProcessor following the given workUnit stealAttempts
   * and lifeline strategy provided
   *
   * @param <S>
   *          Type of the strategy parameter
   * @param <R>
   *          Type of the result for the created GLBProcessor
   * @param workUnit
   *          amount of work to process before distributing work
   * @param stealAttempts
   *          number of random steals attempted before resulting to the lifeline
   *          scheme
   * @param strategy
   *          the lifelines strategy to be used in this GLBProcessor instance
   * @param resultInit
   *          Supplier of <R> type neutral element. Functional interface.
   * @return a new computing instance
   */
  public static <S extends LifelineStrategy & Serializable, R extends Result<R> & Serializable, G extends Supplier<R> & Serializable> GLBProcessor<R> GLBProcessor(
      int workUnit, int stealAttempts, S strategy, G resultInit) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final GLBProcessor<R> glb = PlaceLocalObject.make(places(),
        () -> new GenericGLBProcessor<>(workUnit, stealAttempts, strategy,
            resultInit));
    return glb;
  }
}
