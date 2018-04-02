package org.logic2j.engine.model;

import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.util.TypeUtils;

import java.util.ArrayList;
import java.util.Collection;

public final class PrologLists {

  public static final String FUNCTOR_EMPTY_LIST = "[]"; // The list end marker
  /**
   * The empty list complete Struct.
   */
  public static final Struct EMPTY_LIST = new Struct(FUNCTOR_EMPTY_LIST);

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
  public static Struct createPList(Object head, Object tail) {
    final Struct result = new Struct(Struct.FUNCTOR_LIST_NODE, head, tail);
    return result;
  }

  /**
   * Create a Prolog list from a Java collection.
   *
   * @param theJavaCollection We use a collection not an Iterable because we need to know its size at first.
   * @return A Prolog List structure from a Java {@link java.util.Collection}.
   */
  public static Struct createPList(Collection<?> theJavaCollection) {
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
  public static Struct createPList(final Object[] array) {
    // Assemble the prolog list (head|tail) nodes from the last to the first element
    Struct tail = EMPTY_LIST;
    for (int i = array.length - 1; i >= 0; i--) {
      final Object head = array[i];
      tail = createPList(head, tail);
    }
    return tail;
  }

  /**
   * @return true If this structure an empty list
   * @param struct
   */
  public static boolean isEmptyList(Struct struct) {
    return struct.getName() == FUNCTOR_EMPTY_LIST && struct.getArity() == 0;
  }

  /**
   * @return true if this list is the empty list, or if it is a Prolog list.
   * @param struct
   */
  public static boolean isList(Struct struct) {
    return (struct.getName() == Struct.FUNCTOR_LIST_NODE && struct.getArity() == 2 && TermApi.isList(struct.getArg(1))) || isEmptyList(struct);
  }

  /**
   * Make sure a Term is a Prolog List.
   *
   * @param thePList
   * @throws InvalidTermException If this is not a list.
   */
  public static void assertPList(Term thePList) {
    if (!TermApi.isList(thePList)) {
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
   * @throws InvalidTermException If this is not a list.
   * @param prologList
   */
  public static Object listHead(Struct prologList) {
    assertPList(prologList);
    return prologList.getLHS();
  }

  /**
   * Gets the tail of this structure, which is supposed to be a list.
   *
   * @throws InvalidTermException if this is not a prolog list.
   * @param prologList
   */
  public static Struct listTail(Struct prologList) {
    assertPList(prologList);
    return (Struct) prologList.getRHS();
  }

  /**
   * Gets the number of elements of this structure, which is supposed to be a list.
   *
   * @throws InvalidTermException if this is not a prolog list.
   * @param prologList
   */
  public static int listSize(Struct prologList) {
    assertPList(prologList);
    Struct running = prologList;
    int count = 0;
    while (!isEmptyList(running)) {
      count++;
      running = (Struct) running.getRHS();
    }
    return count;
  }

  /**
   * From a Prolog List, obtain a Struct with the first list element as functor, and all other elements as arguments. This returns
   * a(b,c,d) form [a,b,c,d]. This is the =.. predicate.
   *
   * @throws InvalidTermException if this is not a prolog list.
   * @param prologList
   */
  // TODO (issue) Only used from Library. Clarify how it works, see https://github.com/ltettoni/logic2j/issues/14
  public static Struct predicateFromPList(Struct prologList) {
    assertPList(prologList);
    final Object functor = prologList.getLHS();
    if (!TermApi.isAtom(functor)) {
      return null;
    }
    Struct runningElement = (Struct) prologList.getRHS();
    final ArrayList<Object> elements = new ArrayList<Object>();
    while (!isEmptyList(runningElement)) {
      if (!isList(runningElement)) {
        return null;
      }
      elements.add(runningElement.getLHS());
      runningElement = (Struct) runningElement.getRHS();
    }
    final String fnct;
    if (functor instanceof String) {
      fnct = (String) functor;
    } else {
      fnct = ((Struct) functor).getName();
    }

    return new Struct(fnct, elements.toArray(new Object[elements.size()]));
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
  public static <Q, T extends Collection<Q>> T javaListFromPList(Struct prologList, T theCollectionToFillOrNull, Class<Q> theElementRequiredClass) {
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
  public static <Q, T extends Collection<Q>> T javaListFromPList(Struct prologList, T theCollectionToFillOrNull, Class<Q> theElementRequiredClass,
      boolean recursive) {
    final T result;
    if (theCollectionToFillOrNull == null) {
      result = (T) new ArrayList<Q>();
    } else {
      result = theCollectionToFillOrNull;
    }
    // In case not a list, we just add a single element to the collection to fill
    if (!isList(prologList)) {
      result.add(TypeUtils.safeCastNotNull("casting single value", prologList, theElementRequiredClass));
      return result;
    }

    Struct runningElement = prologList;
    int idx = 0;
    while (!isEmptyList(runningElement)) {
      assertPList(runningElement);
      final Object lhs = runningElement.getLHS();
      if (recursive && TermApi.isList(lhs)) {
        javaListFromPList(((Struct) lhs), theCollectionToFillOrNull, theElementRequiredClass, recursive);
      } else {
        final Q term = TypeUtils.safeCastNotNull("obtaining element " + idx + " of PList " + prologList, lhs, theElementRequiredClass);
        result.add(term);
      }
      runningElement = (Struct) runningElement.getRHS();
      idx++;
    }
    return result;
  }
}
