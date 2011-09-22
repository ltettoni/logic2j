/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.logic2j.model.symbol;

import java.util.Collection;
import java.util.IdentityHashMap;

import org.logic2j.model.InvalidTermException;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.VarBindings;

/**
 * This class represents a variable term.
 * Variables are identified by a name (which must starts with
 * an upper case letter) or the anonymous ('_') name.
 *
 * @see Term
 */
public class Var extends Term {

  private static final long serialVersionUID = 1L;

  // TODO move these constants to a common place?
  public static final String ANY = "_";
  public static final Term ANONYMOUS_VAR = new Var();

  // the name identifying the var
  private String name; // Will be null when variable is ANY

  /**
   * Creates a variable identified by a name.
   *
   * The name must starts with an upper case letter or the underscore. If an underscore is
   * specified as a name, the variable is anonymous.
   *
   * @param theName is the name
   * @throws InvalidTermException if n is not a valid Prolog variable name
   */
  public Var(String theName) {
    if (ANY.equals(theName)) {
      this.name = null;
    } else if (Character.isUpperCase(theName.charAt(0)) || (theName.startsWith(ANY))) {
      this.name = theName.intern();
    } else {
      throw new InvalidTermException("Illegal variable name: " + theName);
    }
  }

  /**
   * Creates an anonymous variable
   *
   *  This is equivalent to build a variable with name _
   */
  private Var() {
    this.name = null;
  }

  /**
   * Gets the name of the variable
   */
  public String getName() {
    if (this.name != null) {
      return this.name;
    } else {
      return ANY;
    }
  }

  @Override
  public boolean isAtom() {
    return false;
  }

  @Override
  public boolean isList() {
    return false;
  }

  //

  /**
   * Tests if this variable is ANY
   */
  public boolean isAnonymous() {
    return this.name == null;
  }

  @Override
  public Var findVar(String theVariableName) {
    if (ANY.equals(theVariableName)) {
      throw new IllegalArgumentException("Cannot find the anonymous variable");
    }
    if (theVariableName.equals(getName())) {
      return this;
    }
    return null;
  }

  @Override
  protected Term substitute(VarBindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
    if (isAnonymous()) {
      // Anonymous variable is never bound - won't substitute
      return this;
    }
    Binding binding = derefToBinding(theBindings);
    while (binding.isVar()) {
      binding = binding.getLink();
    }
    switch (binding.getType()) {
      case LIT:
        // For a literal, we keep a reference to the term and to its own variables,
        // so recurse further
        return binding.getTerm().substitute(binding.getLiteralBindings(), theBindingsToVars);
      case FREE:
        // Free variable has no value, so substitution ends up on the last 
        // variable of the chain
        if (theBindingsToVars != null) {
          final Var originVar = theBindingsToVars.get(binding);
          if (originVar != null) {
            return originVar;
          } else {
            return ANONYMOUS_VAR;
          }
        }
        return this;
      default:
        // In case of VAR: that's impossible since we have followed the complete linked chain
        throw new IllegalStateException("substitute() internal error");
    }
  }

  @Override
  protected void flattenTerms(Collection<Term> theFlatTerms) {
    this.index = NO_INDEX;
    theFlatTerms.add(this);
  }

  @Override
  protected Term compact(Collection<Term> theFlatTerms) {
    // If this term already has an equivalent in the provided collection, return that one
    final Term alreadyThere = findStaticallyEqual(theFlatTerms);
    if (alreadyThere != null) {
      return alreadyThere;
    }
    for (Term term : theFlatTerms) {
      if (term instanceof Var) {
        final Var var = (Var) term;
        if (this.getName().equals(var.getName())) {
          return var;
        }
      }
    }
    return this;
  }

  @Override
  public boolean staticallyEquals(Term theOther) {
    if (theOther == this) {
      return true; // Same reference
    }
    return false;
  }

  @Override
  protected short assignVarOffsets(short theIndexOfNextUnindexedVar) {
    if (this.index != NO_INDEX) {
      // Already assigned, do nothing and return same index since we did not assign a new var
      return theIndexOfNextUnindexedVar;
    }
    if (isAnonymous()) {
      // Anonymous variable is not a var, don't count it, but assign an index that 
      // is different from NO_INDEX but that won't be ever used
      this.index = ANON_INDEX;
      return theIndexOfNextUnindexedVar;
    }
    // Index this var
    this.index = theIndexOfNextUnindexedVar;
    return ++theIndexOfNextUnindexedVar;
  }

  /**
   * Obtain the current {@link Binding} of this Var from the {@link VarBindings}.
   * Notice that the variable index must have been assigned, and this var must NOT
   * be the anonymous variable (that cannot be bound to anyhting).
   * @param theVarBindings
   * @return The current binding of this Var.
   */
  public Binding derefToBinding(VarBindings theVarBindings) {
    if (this.index < 0) {
      // An error situation
      if (this.index == NO_INDEX) {
        throw new IllegalStateException("Cannot dereference variable whose offset was not initialized");
      }
      if (this.index == ANON_INDEX) {
        throw new IllegalStateException("Cannot dereference the anonymous variable");
      }
    }
    if (this.index >= theVarBindings.nbBindings()) {
      throw new IllegalStateException("Bindings " + theVarBindings + " has space for " + theVarBindings.nbBindings()
          + " vars, trying to dereference " + this + " at index " + this.index);
    }
    return theVarBindings.getBinding(this.index);
  }

  @Override
  public <T> T accept(TermVisitor<T> theVisitor) {
    return theVisitor.visit(this);
  }

  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Var)) {
      return false;
    }
    final Var that = (Var) other;
    return this.name == that.name;
  }

  @Override
  public int hashCode() {
    if (this.name == null) {
      // Anonymous var
      return 0;
    }
    return this.name.hashCode();
  }

}
