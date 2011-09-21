package org.logic2j.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utilities to parse and format Collections, by extension functions are also handling Maps
 * and arrays.
 * For rich manipulations of Collections, consider Jakarta Commons "BeanUtils" instead.
 * 
 */
public final class CollectionUtils {

  private CollectionUtils() {
    // Forbid instantiation: it's a set of static function
  }

  //  /**
  //   * A Selector that will match {@link CharSequence}s 
  //   * against a regular expression.
  //   *
  //   */
  //  public static class RegexpSelector implements Selector<String> {
  //
  //    private CharSequence regexp;
  //    private Pattern pattern;
  //
  //    public RegexpSelector(CharSequence theRegexp) {
  //      super();
  //      this.regexp = theRegexp;
  //      this.pattern = Pattern.compile(theRegexp.toString());
  //    }
  //
  //    @Override
  //    public boolean select(String theText) {
  //      final Matcher matcher = this.pattern.matcher(theText);
  //      return matcher.matches();
  //    }
  //
  //    @Override
  //    public String toString() {
  //      return this.getClass().getSimpleName() + '(' + this.regexp + ')';
  //    }
  //
  //  }
  //
  //  /**
  //   * A Mapper transforms a value into another.
  //   *
  //   */
  //  public interface Mapper<T, U> {
  //    public U map(T arg);
  //  }
  //
  //  /**
  //   * A Selector decides if a value matches a criterion or not.
  //   *
  //   */
  //  public interface Selector<T> {
  //    public boolean select(T arg);
  //  }
  //
  /**
   * Format a collection using it's element's {@link String#valueOf(java.lang.Object)} method, but inserting
   * a separator between consecutive elements, and not surrounding the result by any braket, brace or
   * parenthesis.
   * @param theCollection The collection to format. Must not be null.
   * @param theSeparator The string used to interleave between consecutive elements. May be "" to pack
   * elements together. Normally use a space around, e.g. " OR ". If null, then the empty string is used.
   * @return A formatter string, never null. May span several lines depending on the element's toString() or on the
   * separator value.
   * @throws IllegalArgumentException If coll is null.
   */
  public static String formatSeparated(Collection<? extends Object> theCollection, String theSeparator) {
    if (theCollection == null) {
      throw new IllegalArgumentException("Cannot format null collection");
    }
    String separator = theSeparator;
    if (separator == null) {
      separator = "";
    }
    final StringBuffer sb = new StringBuffer();
    for (final Iterator<? extends Object> iter = theCollection.iterator(); iter.hasNext();) {
      final String element = String.valueOf(iter.next());
      sb.append(element);
      if (iter.hasNext()) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

  /**
   * Format an array using it's element's {@link String#valueOf(java.lang.Object)} method, but inserting
   * a separator between consecutive elements, and not surrounding the result by any braket, brace or
   * parenthesis.
   * @param theArray The array to format. Must not be null.
   * @param theSeparator The string used to interleave between consecutive elements. May be "" to pack
   * elements together. Normally use a space around, e.g. " OR ". If null, then the empty string is used.
   * @return A formatter string, never null. May span several lines depending on the element's toString() or on the
   * separator value.
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
    final StringBuffer sb = new StringBuffer();
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
  private static String format(String theLabel, Collection<? extends Object> coll, int maxNumberReported, String theClassName) {
    final boolean showCollectionIndexes = false;
    final int half = (maxNumberReported == 0) ? 10000 : ((maxNumberReported - 1) / 2) + 1;

    String label = theLabel;
    if (label == null) {
      label = "";
    }

    final Map<Class<?>, Integer> instancesByClass = new HashMap<Class<?>, Integer>();
    final StringBuffer sb = new StringBuffer(label);

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

    for (Object element : coll) {
      // Statistics
      final Class<?> theElementClass = (element != null) ? element.getClass() : null;
      Integer nbrOfThisClass = instancesByClass.get(theElementClass);

      if (nbrOfThisClass == null) {
        nbrOfThisClass = Integer.valueOf(0);
      }
      nbrOfThisClass = Integer.valueOf(nbrOfThisClass.intValue() + 1);
      instancesByClass.put(theElementClass, nbrOfThisClass);

      // Report
      if (counter < half || counter >= size - half) {
        if (element instanceof Map.Entry<?, ?>) {
          final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) element;
          if (entry.getValue() instanceof Collection<?>) {
            final int colLSize = ((Collection<?>) entry.getValue()).size();
            sb.append(" " + entry.getKey() + '[' + colLSize + "]=" + entry.getValue() + '\n');
          } else {
            sb.append(" " + entry.getKey() + '=' + entry.getValue() + '\n');
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
          sb.append(" [" + (half) + '-' + (size - half - 1) + "]=(" + (size - half - half) + " skipped)\n");
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
      for (Entry<Class<?>, Integer> entry : instancesByClass.entrySet()) {
        final Class<?> key = entry.getKey();
        final Integer value = entry.getValue();
        sb.append(", ");
        sb.append(value);
        sb.append(' ');
        sb.append(key.getClass().getSimpleName());
      }
    }
    sb.append('.');
    return sb.toString();
  }

  /**
   * Generate a usually multiline String reporting a collection's elements.
   * If the collection is a Map.entrySet(), actually if elements are instances of Map.Entry
   * then their key is reported instead of the element's index.
   *
   * @param theLabel A label to display first, as is without change. If null, "" is used.
   * @param coll A collection whose elements will be listed (if not too large)
   * @param maxNumberReported The maximum number of elements to report in case of large
   * collections, or 0 to report all whatever the size.
   * @return A usually multiline String describing the collection. This can be
   * logged, or output to System.out, for instance.
   * If the collection is empty, one line is output. If the collection is large,
   * only the first and last elements are output, while "..." is shown in the middle.
   */
  public static String format(String theLabel, Collection<? extends Object> coll, int maxNumberReported) {
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
   * @param theLabel See related function.
   * @param array See related function.
   * @param maxNumberReported See related function.
   * @return See related function.
   * @see #format(String, Collection, int)
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
   * @param theLabel See related function.
   * @param map See related function.
   * @param maxNumberReported See related function.
   * @return See related function.
   * @see #format(String, Collection, int)
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
  //
  //  public static <T> Collection<T> select(Collection<T> theCollection, Selector<T> aSelector) {
  //    if (theCollection == null) {
  //      return null;
  //    }
  //    final Collection<T> destination = createCollection(theCollection);
  //    selectInto(theCollection, aSelector, destination);
  //    return destination;
  //  }
  //
  //  public static <T> Collection<T> selectInto(Collection<T> theCollection, Selector<T> aSelector, Collection<T> destination) {
  //    if (theCollection == null) {
  //      return destination;
  //    }
  //    if (destination == null) {
  //      throw new IllegalArgumentException("Cannot select into a null destination collection");
  //    }
  //    for (T element : theCollection) {
  //      if (aSelector.select(element)) {
  //        destination.add(element);
  //      }
  //    }
  //    return destination;
  //  }
  //
  //  /**
  //   * Map (apply a mapping function) all elements of a collection and return a new collection
  //   * with the elements mapped.
  //   * @param <T>
  //   * @param theCollection Source data, not modified.
  //   * @param theMapper
  //   * @return theDestination argument (pass through function).
  //   */
  //  public static <T> Collection<T> map(Collection<T> theCollection, Mapper<T, T> theMapper) {
  //    if (theCollection == null) {
  //      return null;
  //    }
  //    final Collection<T> destination = createCollection(theCollection);
  //    mapInto(theCollection, theMapper, destination);
  //    return destination;
  //  }
  //
  //  /**
  //   * Map (apply a mapping function) all elements of a collection into a target other collection.
  //   * @param <T>
  //   * @param <U>
  //   * @param theCollection Source data, not modified.
  //   * @param theMapper
  //   * @param theDestination
  //   * @return theDestination argument (pass through function).
  //   */
  //  public static <T, U> Collection<U> mapInto(Collection<T> theCollection, Mapper<T, U> theMapper, Collection<U> theDestination) {
  //    if (theCollection == null) {
  //      return theDestination;
  //    }
  //    if (theDestination == null) {
  //      throw new IllegalArgumentException("Cannot map into a null destination collection");
  //    }
  //    for (T element : theCollection) {
  //      final U result = theMapper.map(element);
  //      if (result != null) {
  //        theDestination.add(result);
  //      }
  //    }
  //    return theDestination;
  //  }
  //
  //  /**
  //   * Remove all entries in the Map whose keys do not match the selector.
  //   * @param theMap
  //   * @param theSelector
  //   * @return theMap (pass through function).
  //   */
  //  public static <T, U> Map<T, U> removeUnselected(Map<T, U> theMap, Selector<T> theSelector) {
  //    final Iterator<Map.Entry<T, U>> iter = theMap.entrySet().iterator();
  //    while (iter.hasNext()) {
  //      final Entry<T, U> entry = iter.next();
  //      final T key = entry.getKey();
  //      if (!theSelector.select(key)) {
  //        iter.remove();
  //      }
  //    }
  //    return theMap;
  //  }
  //
  //  /**
  //   * Instantiate a new empty Collection of the same runtime class as the argument. 
  //   * This method will attempt to obtain a new instance by using {@link Class#newInstance()}; if this
  //   * fails, then an approximate equivalent is returned, either an {@link ArrayList} if theOriginal is a 
  //   * List, or an {@link HashSet} if theOriginal is a Set.
  //   * @param theOriginal The original collection to get an empty equivanent to.
  //   * @return An empty Collection, never null.
  //   */
  //  @SuppressWarnings("unchecked")
  //  public static <T> Collection<T> createCollection(Collection<T> theOriginal) {
  //    try {
  //      // Try to instantiate of same class
  //      Collection<T> newInstance = theOriginal.getClass().newInstance();
  //      return newInstance;
  //    } catch (InstantiationException e) {
  //      return createSimilarCollection(theOriginal);
  //    } catch (IllegalAccessException e) {
  //      return createSimilarCollection(theOriginal);
  //    }
  //  }
  //
  //  private static <T> Collection<T> createSimilarCollection(Collection<T> theOriginal) {
  //    if (theOriginal == null) {
  //      throw new IllegalArgumentException("Cannot create collection similar to null");
  //    }
  //    final int size = theOriginal.size();
  //    if (theOriginal instanceof List<?>) {
  //      return new ArrayList<T>(size);
  //    } else if (theOriginal instanceof Set<?>) {
  //      return new HashSet<T>(size + size / 2);
  //    } else {
  //      throw new IllegalArgumentException("Don't know how to create new instance of empty Collection similar to "
  //          + theOriginal.getClass());
  //    }
  //  }

}
