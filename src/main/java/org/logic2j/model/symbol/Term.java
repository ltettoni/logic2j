/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.model.symbol;

import java.util.Collection;
import java.util.IdentityHashMap;

import org.logic2j.io.format.DefaultFormatter;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.VarBindings;

/**
 * Term class is the root abstract class for prolog data type
 *
 * @see Struct
 * @see Var
 * @see TNumber
 */
public abstract class Term implements java.io.Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  public static final int NO_INDEX = -1;
  public static final int ANON_INDEX = -2; // Index of the anonymous variable

  /**
   * Offset into the stackframe, where var value to be found
   */
  protected short index = NO_INDEX;

  /** is this term a prolog (alphanumeric) atom? */
  // TODO Remove this it's only used from one place! Have a separate place (CoreLibrary?) for such methods.
  public abstract boolean isAtom();

  /** is this term a prolog list? */
  public abstract boolean isList();

  //---------------------------------------------------------------------------
  // Graph traversal methods
  // Notice that some traversal is accomplished by the #accept() method and the visitor pattern
  //---------------------------------------------------------------------------

  protected abstract void flattenTerms(Collection<Term> theFlatTerms);

  protected abstract Term compact(Collection<Term> theFlatTerms);

  public abstract boolean staticallyEquals(Term theOther);

  protected abstract short assignVarOffsets(short theIndexOfNextUnindexedVar);

  /**
   * Internal template method; the public API entry point is {@link TermApi#substitute(Term, VarBindings, IdentityHashMap)}.
   * @param theBindings
   * @return A possibly new (cloned) term with all non-free vars resolved.
   */
  protected abstract Term substitute(VarBindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars);

  /**
   * Find the first instance of {@link Var} by name inside a Term, most often Struct.
   * @param theVariableName
   * @return A {@link Var} with the specified name, or null when not found.
   */
  public abstract Var findVar(String theVariableName);

  protected Term findStaticallyEqual(Collection<Term> theTerms) {
    for (Term term : theTerms) {
      if (term != this && term.staticallyEquals(this)) {
        return term;
      }
    }
    return null;
  }

  /**
   * @return A deep copy of this Term.
   */
  public Term cloneIt() {
    try {
      return (Term) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InvalidTermException("Could not clone: " + e, e);
    }
  }

  public abstract <T> T accept(TermVisitor<T> theVisitor);

  public short getIndex() {
    return this.index;
  }

  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  // Require subclass to implement it!
  @Override
  public abstract boolean equals(Object that);

  // Require subclass to implement it!
  @Override
  public abstract int hashCode();

  /**
   * Delegate formatting to the {@link DefaultFormatter}.
   */
  @Override
  public String toString() {
    final DefaultFormatter formatter = new DefaultFormatter();
    accept(formatter);
    return formatter.formatted();
  }

}
