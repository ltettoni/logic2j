package org.logic2j.engine.model;

import static org.logic2j.engine.model.TermApiLocator.termApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.util.TypeUtils;

public final class PrologLists {


  // ---------------------------------------------------------------------------
  // Constants defining Prolog lists
  // ---------------------------------------------------------------------------

  public static final String FUNCTOR_LIST_NODE = ".";

  public static final String FUNCTOR_EMPTY_LIST = "[]"; // The list end marker

  /**
   * The empty list complete Struct.
   */
  public static final Struct<?> EMPTY_LIST = new Struct<>(FUNCTOR_EMPTY_LIST);

  public static final String LIST_ELEM_SEPARATOR = ","; // In notation of prolog lists: [a,b,c]

  public static final char LIST_CLOSE = ']';

  public static final char LIST_OPEN = '[';

  public static final char HEAD_TAIL_SEPARATOR = '|';

  public PrologLists() {
    // static functions only
  }


  /**
   * Create a Prolog list from head and tail.
   *
   * @param head
   * @param tail
   * @return A prolog list provided head and tail
   */
  public static Struct<?> createPList(Object head, Object tail) {
    final Struct<?> result = new Struct<>(FUNCTOR_LIST_NODE, head, tail);
    return result;
  }

  /**
   * Create a Prolog list from a Java collection.
   *
   * @param theJavaCollection We use a collection not an Iterable because we need to know its size at first.
   * @return A Prolog List structure from a Java {@link java.util.Collection}.
   */
  public static Struct<?> createPList(Collection<?> theJavaCollection) {
    final int size = theJavaCollection.size();
    // Unroll elements into an array (we need this since we don't have an index-addressable collection)
    final Object[] array = new Object[size];
    int index = 0;
    for (final Object element : theJavaCollection) {
      array[index++] = element;
    }
    return createPList(array);
  }

  /**
   * @param array
   * @return A Prolog List structure from a Java array.
   */
  public static Struct<?> createPList(final Object[] array) {
    // Assemble the prolog list (head|tail) nodes from the last to the first element
    Struct<?> tail = EMPTY_LIST;
    for (int i = array.length - 1; i >= 0; i--) {
      final Object head = array[i];
      tail = createPList(head, tail);
    }
    return tail;
  }


  /**
   * @param theTerm
   * @return true theTerm is a {@link Struct} and if {@link PrologLists#isList(Struct<?>)}
   */
  public static boolean isList(Object theTerm) {
    return theTerm instanceof Struct<?> && isList(((Struct<?>) theTerm));
  }

  /**
   * @param struct
   * @return true if this list is the empty list, or any prolog list of 1 or more elements.
   */
  public static boolean isList(Struct<?> struct) {
    return isListNode(struct) || isEmptyList(struct);
  }

  /**
   * True of struct denotes a list node, could be a list of one or many, but not the empty list.
   *
   * @param struct
   * @return true this is a predicate '.'/2.
   */
  public static boolean isListNode(Struct<?> struct) {
    return struct.getName() == FUNCTOR_LIST_NODE && struct.getArity() == 2;
  }


  /**
   * @param struct
   * @return true If this structure an empty list
   */
  public static boolean isEmptyList(Struct<?> struct) {
    return struct.getName() == FUNCTOR_EMPTY_LIST && struct.getArity() == 0;
  }

  /**
   * Make sure a Term is a Prolog List.
   *
   * @param thePList
   * @throws InvalidTermException If this is not a prolog list.
   */
  public static void requireList(Term thePList) {
    if (!isList(thePList)) {
      throw new InvalidTermException("The structure \"" + thePList + "\" is not a Prolog list.");
    }
  }

  /**
   * Gets the head of this structure, which is assumed to be a list.
   * <p/>
   * <p>
   * Gets the head of this structure, which is supposed to be a list. If the callee structure is not a list, throws an
   * <code>PrologNonSpecificError</code>
   * </p>
   *
   * @param prologList
   * @throws InvalidTermException If this is not a list.
   */
  public static Object listHead(Struct<?> prologList) {
    requireList(prologList);
    return prologList.getLHS();
  }

  /**
   * Gets the tail of this structure, which is supposed to be a list.
   *
   * @param prologList
   * @throws InvalidTermException if this is not a prolog list.
   */
  public static Object listTail(Struct<?> prologList) {
    requireList(prologList);
    return prologList.getRHS();
  }

  /**
   * Gets the number of elements of this structure, which is supposed to be a list.
   *
   * @param prologList
   * @throws InvalidTermException if this is not a prolog list.
   */
  public static int listSize(Struct<?> prologList) {
    requireList(prologList);
    Struct<?> running = prologList;
    int count = 0;
    while (!isEmptyList(running)) {
      count++;
      running = (Struct<?>) running.getRHS();
    }
    return count;
  }

  /**
   * From a Prolog List, obtain a Struct with the first list element as functor, and all other elements as arguments. This returns
   * a(b,c,d) form [a,b,c,d]. This is the =.. predicate.
   *
   * @param prologList
   * @throws InvalidTermException if this is not a prolog list.
   */
  // TODO (issue) Only used from Library. Clarify how it works, see https://github.com/ltettoni/logic2j/issues/14
  public static Struct<?> predicateFromPList(Struct<?> prologList) {
    requireList(prologList);
    final Object functor = prologList.getLHS();
    if (!termApi().isAtom(functor)) {
      return null;
    }
    Struct<?> runningElement = (Struct<?>) prologList.getRHS();
    final ArrayList<Object> elements = new ArrayList<>();
    while (!isEmptyList(runningElement)) {
      if (!isList(runningElement)) {
        return null;
      }
      elements.add(runningElement.getLHS());
      runningElement = (Struct<?>) runningElement.getRHS();
    }
    final String fnct;
    if (functor instanceof String) {
      fnct = (String) functor;
    } else {
      fnct = ((Struct<?>) functor).getName();
    }

    return new Struct<>(fnct, elements.toArray(new Object[0]));
  }

  /**
   * Traverse Prolog List adding all elements (in right order) into a target collection.
   *
   * @param prologList
   * @param theCollectionToFillOrNull
   * @param theElementRequiredClass
   * @param <Q>
   * @param <T>
   * @return
   * @throws InvalidTermException if this is not a prolog list.
   */
  @SuppressWarnings("unchecked")
  public static <Q, T extends Collection<Q>> T javaListFromPList(Struct<?> prologList, T theCollectionToFillOrNull, Class<Q> theElementRequiredClass) {
    return javaListFromPList(prologList, theCollectionToFillOrNull, theElementRequiredClass, false);
  }

  /**
   * Traverse Prolog List adding all elements (in right order) into a target collection, possibly recursing if elements are
   * Prolog lists too.
   *
   * @param prologList
   * @param theCollectionToFillOrNull
   * @param theElementRequiredClass
   * @param <Q>
   * @param <T>
   * @return
   * @throws InvalidTermException if this is not a prolog list.
   */
  @SuppressWarnings("unchecked")
  public static <Q, T extends Collection<Q>> T javaListFromPList(Struct<?> prologList, T theCollectionToFillOrNull, Class<Q> theElementRequiredClass,
                                                                 boolean recursive) {
    final T result;
      result = Objects.requireNonNullElseGet(theCollectionToFillOrNull, () -> (T) new ArrayList<Q>());
    // In case not a list, we just add a single element to the collection to fill
    if (!isList(prologList)) {
      result.add(TypeUtils.safeCastNotNull("casting single value", prologList, theElementRequiredClass));
      return result;
    }

    Struct<?> runningElement = prologList;
    int idx = 0;
    while (!isEmptyList(runningElement)) {
      requireList(runningElement);
      final Object lhs = runningElement.getLHS();
      if (recursive && isList(lhs)) {
        javaListFromPList(((Struct<?>) lhs), theCollectionToFillOrNull, theElementRequiredClass, recursive);
      } else {
        final Q term = TypeUtils.safeCastNotNull("obtaining element " + idx + " of PList " + prologList, lhs, theElementRequiredClass);
        result.add(term);
      }
      runningElement = (Struct<?>) runningElement.getRHS();
      idx++;
    }
    return result;
  }

  public static StringBuilder formatPListRecursive(Struct<?> prologList, StringBuilder sb) {
    final Object head = listHead(prologList);
    final Object tail = listTail(prologList);
    if (isList(tail)) {
      final Struct<?> tailStruct = (Struct<?>) tail;
      // .(h, []) will be displayed as h
      if (isEmptyList(tailStruct)) {
        sb.append(head.toString());
      } else {
        sb.append(head.toString());
        sb.append(LIST_ELEM_SEPARATOR);
        formatPListRecursive(tailStruct, sb);
      }
    } else {
      // Head
      sb.append(head.toString());
      sb.append(HEAD_TAIL_SEPARATOR);
      // Tail
      sb.append(tail.toString());
    }
    return sb;
  }

}
