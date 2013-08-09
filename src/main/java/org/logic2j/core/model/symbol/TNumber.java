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
package org.logic2j.core.model.symbol;

import java.util.Collection;
import java.util.IdentityHashMap;

import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;

/**
 * {@link TNumber}s are numeric {@link Term}s.
 */
public abstract class TNumber extends Term implements Comparable<TNumber> {
  private static final long serialVersionUID = 1L;

  /**
   *  Returns the value of the number as long
   */
  public abstract long longValue();

  /**
   *  Returns the value of the number as double
   */
  public abstract double doubleValue();


  //---------------------------------------------------------------------------
  // Template methods defined in abstract class Term
  //---------------------------------------------------------------------------

  @Override
  final public boolean isAtom() {
    return false;
  }

  @Override
  final public boolean isList() {
    return false;
  }

  @Override
  protected void flattenTerms(Collection<Term> theFlatTerms) {
    this.index = NO_INDEX;
    theFlatTerms.add(this);
  }

  @Override
  protected Term compact(Collection<Term> theFlatTerms) {
    // If this term already has an equivalent in the provided collection, return that one
    Term alreadyThere = findStaticallyEqual(theFlatTerms);
    if (alreadyThere != null) {
      return alreadyThere;
    }
    return this;
  }

  @Override
  public Var findVar(String theVariableName) {
    return null;
  }

  /**
   * No substitution occurs on literals.
   */
  @Override
  protected Term substitute(Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
    return this;
  }

  @Override
  public boolean staticallyEquals(Term theOther) {
    if (theOther == this) {
      return true; // Same reference
    }
    return equals(theOther);
  }

  @Override
  public short assignVarOffsets(short theIndexOfNextUnindexedVar) {
    // Don't leave the default NO_INDEX value otherwise this term won't be considered 
    // properly normalized.
    this.index = 0;
    // No variable found - return same index
    return theIndexOfNextUnindexedVar;
  }

}
