package org.logic2j;

import org.logic2j.model.symbol.Term;

/**
 * Factory methods to create {@link Term}s from various data of a different nature
 * and representation.
 *
 */
public interface TermFactory {

  public static enum FactoryMode {
    /**
     * Result must be an atom, may not be a Var.
     */
    ATOM,
    /**
     * Result may be an atom, a numeric, but not a Var neither a Struct.
     */
    LITERAL,

    /**
     * Result may be any Term (atom, number, Var), but not a compound Struct.
     */
    ANY_TERM,
    /**
     * Result is the outcome of parsing a complex structure.
     */
    COMPOUND
  }

  /**
   * Create a Term from a String representation, will use the definitions of 
   * operators and primitives that currently apply.
   * @param theExpression
   * @return A compacted and normalized {@link Term}.
   */
  public abstract Term parse(CharSequence theExpression);

  /**
   * Create a Term from virtually any type of object, in particular a CharSequence;
   * this is the highest-level factory. On CharSequence, will call {@link #parse(CharSequence)}.
   * @param theObject
   * @param theMode Kind of Term to instantiate
   * @return A compacted and normalized {@link Term}.
   */
  public abstract Term create(Object theObject, FactoryMode theMode);

  /**
   * Normalize a {@link Term} using the current definitions of operators, primitives, etc.
   */
  public abstract Term normalize(Term theTerm);

}
