/**
 *
 */
package apgas.glb.example;

import java.io.Serializable;

import apgas.glb.Fold;

/**
 *
 * Implementation of the {@link Fold} interface that performs the addition on
 * {@code long} integers. The class also implements interface
 * {@link Serializable} in order to be used by the GLB library.
 *
 * @author Patrick Finnerty
 *
 */
public class Sum implements Fold<Sum>, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 3582168956043482749L;

  /** Integer in which the sum is performed */
  public long sum;

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Fold#fold(apgas.glb.Fold)
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
