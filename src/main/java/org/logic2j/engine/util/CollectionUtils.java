/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.engine.util;

import java.util.*;
import java.util.Map.Entry;

/**
 * Utilities to format {@link java.util.Collection}s, by extension functions are also handling {@link java.util.Map}s and arrays.
 *
 * @note Quite of a nice to have, and not functionally required... Not really much used (only in contribs, and in one test case). We could
 * as well use Guava but do we want dependencies just for that?
 */
public final class CollectionUtils {

  private CollectionUtils() {
    // Forbid instantiation: this class is a set of static function
  }

  /**
   * Format a collection using it's element's {@link String#valueOf(Object)} method, but inserting a separator between
   * consecutive elements, and not surrounding the result by any braket, brace or parenthesis.
   *
   * @param theCollection The collection to format. Must not be null.
   * @param theSeparator  The string used to interleave between consecutive elements. May be "" to pack elements together. Normally use a
   *                      space around, e.g. " OR ". If null, then the empty string is used.
   * @return A formatter string, never null. May span several lines depending on the element's toString() or on the separator value.
   * @throws IllegalArgumentException If coll is null.
   */
  public static String formatSeparated(Collection<?> theCollection, String theSeparator) {
    if (theCollection == null) {
      throw new IllegalArgumentException("Cannot format null collection");
    }
    String separator = theSeparator;
    if (separator == null) {
      separator = "";
    }
    final StringBuilder sb = new StringBuilder();
    for (final Iterator<?> iter = theCollection.iterator(); iter.hasNext(); ) {
      final String element = String.valueOf(iter.next());
      sb.append(element);
      if (iter.hasNext()) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

  /**
   * Format an array using it's element's {@link String#valueOf(Object)} method, but inserting a separator between consecutive
   * elements, and not surrounding the result by any braket, brace or parenthesis.
   *
   * @param theArray     The array to format. Must not be null.
   * @param theSeparator The string used to interleave between consecutive elements. May be "" to pack elements together. Normally use a
   *                     space around, e.g. " OR ". If null, then the empty string is used.
   * @return A formatter string, never null. May span several lines depending on the element's toString() or on the separator value.
   * @throws IllegalArgumentException If coll is null.
   */
  public static String formatSeparated(Object[] theArray, String theSeparator) {
    if (theArray == null) {
      throw new IllegalArgumentException("Cannot format null array");
    }
    String separator = theSeparator;
    if (separator == null) {
      separator = "";
    }
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < theArray.length; i++) {
      final String element = String.valueOf(theArray[i]);
      sb.append(element);
      if (i < theArray.length - 1) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

  /**
   * Format a collection, array, or map (internal method).
   *
   * @param theLabel
   * @param coll
   * @param maxNumberReported
   * @param theClassName
   * @return The formatted collection, spans multiple lines.
   */
  private static String format(String theLabel, Collection<?> coll, int maxNumberReported, String theClassName) {
    final boolean showCollectionIndexes = false;
    final int half = (maxNumberReported == 0) ? 10000 : ((maxNumberReported - 1) / 2) + 1;

    String label = theLabel;
    if (label == null) {
      label = "";
    }

    final Map<Class<?>, Integer> instancesByClass = new HashMap<Class<?>, Integer>();
    final StringBuilder sb = new StringBuilder(label);

    if (label.length() > 0) {
      sb.append(' ');
    }

    final int size = (coll != null) ? coll.size() : 0;

    if (size > 0) {
      sb.append('\n');
    }

    int counter = 0;
    boolean shownEllipsis = false;

    if (coll == null) {
      sb.append("null Collection or Map");
      return sb.toString();
    }

    for (final Object element : coll) {
      // Statistics
      final Class<?> theElementClass = (element != null) ? element.getClass() : null;
      Integer nbrOfThisClass = instancesByClass.get(theElementClass);

      if (nbrOfThisClass == null) {
        nbrOfThisClass = 0;
      }
      nbrOfThisClass = nbrOfThisClass.intValue() + 1;
      instancesByClass.put(theElementClass, nbrOfThisClass);

      // Report
      if (counter < half || counter >= size - half) {
        if (element instanceof Entry<?, ?>) {
          final Entry<?, ?> entry = (Entry<?, ?>) element;
          if (entry.getValue() instanceof Collection<?>) {
            final int colLSize = ((Collection<?>) entry.getValue()).size();
            sb.append(" ").append(entry.getKey()).append('[').append(colLSize).append("]=").append(entry.getValue()).append('\n');
          } else {
            sb.append(" ").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
          }
        } else {
          sb.append(' ');
          if (showCollectionIndexes) {
            sb.append('[');
            sb.append(counter);
            sb.append("]=");
          }
          sb.append(String.valueOf(element));
          sb.append('\n');
        }
      } else {
        if (!shownEllipsis) {
          sb.append(" [").append(half).append('-').append(size - half - 1).append("]=(").append(size - half - half).append(" skipped)\n");
        }
        shownEllipsis = true;
      }
      counter++;
    }

    // Special case for Arrays$ArrayList
    String className = theClassName;
    if ("Arrays$ArrayList".equals(className)) {
      className = "Object[]";
    }
    sb.append(className);
    sb.append(" size=");
    sb.append(size);

    if (!className.endsWith("Map")) {
      // Report number of classes
      for (final Entry<Class<?>, Integer> entry : instancesByClass.entrySet()) {
        final Class<?> key = entry.getKey();
        final Integer value = entry.getValue();
        sb.append(", ");
        sb.append(value);
        sb.append(' ');
        sb.append(key.getSimpleName());
      }
    }
    sb.append('.');
    return sb.toString();
  }

  /**
   * Generate a usually multiline String reporting a collection's elements. If the collection is a Map.entrySet(), actually if elements
   * are instances of Map.Entry then their key is reported instead of the element's index.
   *
   * @param theLabel          A label to display first, as is without change. If null, "" is used.
   * @param coll              A collection whose elements will be listed (if not too large)
   * @param maxNumberReported The maximum number of elements to report in case of large collections, or 0 to report all whatever the size.
   * @return A usually multiline String describing the collection. This can be logged, or output to System.out, for instance. If the
   * collection is empty, one line is output. If the collection is large, only the first and last elements are output, while "..."
   * is shown in the middle.
   */
  public static String format(String theLabel, Collection<?> coll, int maxNumberReported) {
    String label = theLabel;
    if (coll == null) {
      if (label == null) {
        label = "";
      }
      return label + " null Collection";
    }
    return format(label, coll, maxNumberReported, coll.getClass().getSimpleName());
  }

  /**
   * Generate a usually multiline String reporting an array's elements.
   *
   * @param theLabel          See related function.
   * @param array             See related function.
   * @param maxNumberReported See related function.
   * @return See related function.
   * @see #format(String, java.util.Collection, int)
   */
  public static String format(String theLabel, Object[] array, int maxNumberReported) {
    String label = theLabel;
    if (array == null) {
      if (label == null) {
        label = "";
      }
      return label + " null Object[]";
    }
    return format(label, Arrays.asList(array), maxNumberReported, "Object[]");
  }

  /**
   * Generate a usually multiline String reporting a Map's entries.
   *
   * @param theLabel          See related function.
   * @param map               See related function.
   * @param maxNumberReported See related function.
   * @return See related function.
   * @see #format(String, java.util.Collection, int)
   */
  public static String format(String theLabel, Map<?, ?> map, int maxNumberReported) {
    String label = theLabel;
    if (map == null) {
      if (label == null) {
        label = "";
      }
      return label + " null Map";
    }
    return format(label, map.entrySet(), maxNumberReported, map.getClass().getSimpleName());
  }

}
