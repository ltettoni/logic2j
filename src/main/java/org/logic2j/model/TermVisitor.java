package org.logic2j.model;

import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.StructObject;
import org.logic2j.model.symbol.TDouble;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;

/**
 * Generic visitor for the {@link Term} hierarchy.
 * For reference, see the visitor pattern.
 *
 */
public interface TermVisitor<T> {

  public T visit(TLong theLong);

  public T visit(TDouble theDouble);

  public T visit(Var theVar);

  public T visit(Struct theStruct);

  public T visit(StructObject<?> theStructObject);
}
