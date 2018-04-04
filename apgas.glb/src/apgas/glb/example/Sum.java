/**
 *
 */
package apgas.glb.example;

import java.io.Serializable;

import apgas.glb.Fold;

/**
 * Performs the addition on integers.
 * 
 * @author Patrick Finnerty
 *
 */
public class Sum implements Fold<Sum>, Serializable {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 3582168956043482749L;

  /** Integer in which the sum is performed */
  public int sum;

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
   * Construtor
   *
   * @param s
   *          the initial value of the sum
   */
  public Sum(int s) {
    sum = s;
  }
}
