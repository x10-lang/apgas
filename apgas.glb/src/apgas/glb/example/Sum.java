/**
 *
 */
package apgas.glb.example;

import java.io.Serializable;

import apgas.glb.Result;

/**
 * Performs the addition on integers.
 *
 * @author Patrick Finnerty
 *
 */
public class Sum implements Result<Sum>, Serializable {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 3582168956043482749L;

  /** Integer in which the sum is performed */
  public long sum;

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Result#fold(apgas.glb.Result)
   */
  @Override
  public void fold(Sum f) {
    sum += f.sum;
  }

  /**
   * Construtor
   *
   * @param s
   *          the initial value of the sum
   */
  public Sum(int s) {
    sum = s;
  }

}
