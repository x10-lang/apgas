package apgas.util;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import apgas.Place;

/**
 * Class meant to handle failure of a place.
 * <p>
 * Places are put into groups. Should one of the place fail, the group will try
 * to recover from it.
 */
public class ResilientPlaceGroup implements Serializable {

  /**
   * Serial Version UID (Generated)
   */
  private static final long serialVersionUID = -1299742557832923401L;

  /** Array containing the places that are part of the group */
  protected final Place[] array;

  /**
   * Maximum index in {@link #array} of working places. All places in the array
   * at indeces lower or equal to {@link #max} are in use.
   */
  protected int max;

  /**
   * Constructor
   *
   * The number of places to be in the group is specified as parameter
   *
   * @param size
   *          the number of places to include in the group
   */
  public ResilientPlaceGroup(int size) {
    array = new Place[size];
    int id = 0;
    for (final Place p : places()) {
      array[id++] = p;
      if (id == size) {
        max = p.id;
        return;
      }
    }
    System.err.println(
        "[APGAS] Too few places to initialize the ResilientPlaceGroup. Aborting.");
    System.exit(1);
  }

  /**
   * Attempts to find a spare place in the resilient place group which is not in
   * use yet.
   */
  public void fix() {
    final List<? extends Place> places = places();
    final Iterator<? extends Place> it = places.iterator();
    Place spare;
    try {
      for (int id = 0; id < array.length; ++id) {
        if (!places.contains(array[id])) {
          for (;;) {
            spare = it.next();
            if (spare.id > max) {
              break;
            }
          }
          array[id] = spare;
          max = spare.id;
        }
      }
    } catch (final NoSuchElementException e) {
      System.err.println(
          "[APGAS] Too few places to fix the ResilientPlaceGroup. Aborting.");
      System.exit(1);
    }
  }

  /**
   * Returns the place located at the specified index in {@link #array}
   *
   * @param id
   *          the index in the array
   * @return the place contained at the specified index
   */
  public Place get(int id) {
    return array[id];
  }

  /**
   * Returns the size of member {@link #array}.
   *
   * @return size of the array
   */
  public int size() {
    return array.length;
  }

  /**
   * Checks if the specified place is contained in the
   * {@link ResilientPlaceGroup}.
   *
   * @param place
   *          the place on which the inquiry is about.
   * @return true is the place is in the group, false otherwise.
   */
  public boolean contains(Place place) {
    for (int id = 0; id < array.length; ++id) {
      if (array[id].id == place.id) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the specified place in the {@link ResilientPlaceGroup}
   *
   * @param place
   *          the place to look for.
   * @return the index of the place in {@link #array}, {@code -1} if it is not
   *         contained in the array.
   */
  public int indexOf(Place place) {
    for (int id = 0; id < array.length; ++id) {
      if (array[id].id == place.id) {
        return id;
      }
    }
    return -1;
  }

  /**
   * Returns all the places contains in the {@link ResilientPlaceGroup} as an
   * unmodifiable list.
   *
   * @return an unmodifiable list of the places contained in the group.
   */
  public List<Place> asList() {
    return Collections.unmodifiableList(Arrays.asList(array));
  }
}
