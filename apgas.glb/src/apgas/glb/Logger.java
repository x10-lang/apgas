/**
 *
 */
package apgas.glb;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * Class Logger
 * <p>
 * Class used to track the runtime of the global load balancer execution. It is
 * inspired by the Logger class of the X10 GLB library
 *
 * @author Patrick Finnerty
 *
 */
public class Logger implements Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 6482746530921995488L;

  /* Trackers for random steals */
  /** Number of random steals attempted */
  public long stealsAttempted = 0;
  /** Number of successful random steals */
  public long stealsSuccess = 0;
  /** Number of random steals received */
  public long stealsReceived = 0;
  /** Number of random steals received successful */
  public long stealsSuffered = 0;

  /* Trackers for lifeline steals */
  /** Number of lifeline steals attempted */
  public long lifelineStealsAttempted = 0;
  /** Number of successful lifeline steals */
  public long lifelineStealsSuccess = 0;
  /** Number of lifeline steals received */
  public long lifelineStealsReceived = 0;
  /** Number of lifeline steals received successful */
  public long lifelineStealsSuffered = 0;

  /* Timing variables */
  public long lastStartStopTimeStamp = -1;
  public long timeAlive = 0;
  public long timeDead = 0;
  public long startTime = 0;
  public transient long timeReference;

  private static String nanoToMilliSeconds(long nanoInterval) {
    return String.valueOf(nanoInterval / 1e6);
  }

  public void reset() {
    stealsAttempted = 0;
    stealsSuccess = 0;
    stealsReceived = 0;
    stealsSuffered = 0;

    lifelineStealsAttempted = 0;
    lifelineStealsSuccess = 0;
    lifelineStealsReceived = 0;
    lifelineStealsSuffered = 0;

    lastStartStopTimeStamp = -1;
    timeAlive = 0;
    timeDead = 0;
    startTime = 0;
  }

  /**
   * To be called when a worker thread (re)starts working on a place.
   */
  public void startLive() {
    final long time = System.nanoTime();
    if (startTime == 0) {
      startTime = time;
    }
    if (lastStartStopTimeStamp >= 0) {
      timeDead += time - lastStartStopTimeStamp;
    }
    lastStartStopTimeStamp = time;
  }

  /**
   * To be called when a worker thread stops, either because it is waiting for a
   * steal answer or has run out of tasks.
   */
  public void stopLive() {
    final long time = System.nanoTime();
    timeAlive += time - lastStartStopTimeStamp;
    lastStartStopTimeStamp = time;
  }

  public void print(PrintStream o) {
    o.println("Steal Type;Success;Failed;Total;");
    o.println("Random Steals;" + stealsSuccess + ";"
        + (stealsAttempted - stealsSuccess) + ";" + stealsAttempted + ";");
    o.println("Lifeline Steals;" + lifelineStealsSuccess + ";"
        + (lifelineStealsAttempted - lifelineStealsSuccess) + ";"
        + lifelineStealsAttempted + ";");
    o.println("Activity;Time (ms);");
    o.println("Alive;" + nanoToMilliSeconds(timeAlive) + ";");
    o.println("Dead;" + nanoToMilliSeconds(timeDead) + ";");
    o.println("Total;" + nanoToMilliSeconds(timeAlive + timeDead) + ";");

  }

  /**
   * Constructor
   */
  public Logger() {
    timeReference = System.nanoTime();
  }
}
