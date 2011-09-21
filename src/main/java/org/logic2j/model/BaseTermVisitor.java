package org.logic2j.model;

import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.StructObject;
import org.logic2j.model.symbol.TDouble;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.Var;

/**
 * Base implementation of {@link TermVisitor} that does nothing.
 *
 */
public class BaseTermVisitor<T> implements TermVisitor<T> {

  @Override
  public T visit(TLong theLong) {
    return null;
  }

  @Override
  public T visit(TDouble theDouble) {
    return null;
  }

  @Override
  public T visit(Var theVar) {
    return null;
  }

  /**
   * Delegate to all subelements.
   */
  @Override
  public T visit(Struct theStruct) {
    for (int i = 0; i < theStruct.getArity(); i++) {
      theStruct.getArg(i).accept(this);
    }
    return null;
  }

  @Override
  public T visit(StructObject<?> theStructObject) {
    return null;
  }
}
