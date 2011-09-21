package org.logic2j;

import org.logic2j.library.mgmt.LibraryContent;
import org.logic2j.model.prim.PLibrary;

/**
 * An API to manage libraries implementing Prolog features in Java.
 *
 */
public interface LibraryManager {

  /**
   * Indicate the arity of a variable arguments predicate, such as write/N.
   * (this is an extension to classic Prolog where only fixed arity is supported).
   */
  public static final String VARARG_ARITY_INDICATOR = "N";

  public abstract LibraryContent loadLibrary(PLibrary theLibrary);

  /**
   * @return The whole library's content.
   */
  public abstract LibraryContent wholeContent();

  public abstract <T extends PLibrary> T getLibrary(Class<T> theClass);

}
