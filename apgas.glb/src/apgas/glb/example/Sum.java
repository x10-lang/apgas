/**
 *
 */
package apgas.glb.example;

import java.io.Serializable;

import apgas.glb.Result;

/**
 *
 * Implementation of the {@link Result} interface that performs the addition on
 * {@code long} integers. The class also implements interface
 * {@link Serializable} in order to be used by the GLB library.
 *
 * @author Patrick Finnerty
 *
 */
public class Sum implements Result<Sum>, Serializable {

  /** Serial Version UID */
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
   * Constructor
   *
   * @param s
   *          initial value for the sum
   */
  public Sum(long s) {
    sum = s;
  }

}
