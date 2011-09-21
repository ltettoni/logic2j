package org.logic2j.model.symbol;

import org.logic2j.model.TermVisitor;

/**
 * A {@link Struct} that holds a wrapped object.
 *
 */
public class StructObject<T> extends Struct {
  private static final long serialVersionUID = 1L;

  private T wrapped;

  /**
   * @param theString
   * @param theObject
   */
  public StructObject(String theString, T theObject) {
    super(theString);
    this.wrapped = theObject;
  }

  public T getObject() {
    return this.wrapped;
  }

  @Override
  public <U> U accept(TermVisitor<U> theVisitor) {
    return theVisitor.visit(this);
  }

}
