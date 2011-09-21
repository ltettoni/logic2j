package org.logic2j.model.var;

/**
 * Types of bindings that a variable may have.
 *
 */
public enum BindingType {

  /**
   * Variable linking to this binding is currently free.
   */
  FREE,

  /**
   * Variable linking to this binding is bound to a literal term.
   */
  LIT,

  /**
   * Variable linking to this binding is linked (bound) to another variable
   * (which may itself be bound to any of these {@link BindingType}s).
   */
  VAR

}
