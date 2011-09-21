package org.logic2j;

import org.logic2j.model.symbol.Term;

/**
 * Convert Prolog {@link Term} hierarchies to {@link String}s or other streamable representations.
 *
 */
public interface Formatter {

  public String format(Term theTerm);

}
