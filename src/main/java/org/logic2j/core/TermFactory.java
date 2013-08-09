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
package org.logic2j.core;

import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.TNumber;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.Var;

/**
 * Factory methods to create {@link Term}s from various data of a different nature
 * and representation.
 */
public interface TermFactory {

  public static enum FactoryMode {
    /**
     * Result will always be an atom (a non-compound {@link Struct}), may not be a {@link Var}iable.
     */
    ATOM,
    
    /**
     * Result will be either an atom (a non-compound {@link Struct}), a numeric ({@link TNumber}), 
     * but not a {@link Var}iable neither a compound.
     */
    LITERAL,

    /**
     * Result will be any {@link Term} (atom, number, {@link Var}iable), but not a compound {@link org.logic2j.core.core.model.symbol.Struct}.
     */
    ANY_TERM,
    
    /**
     * Result will be the outcome of parsing a complex structure.
     */
    COMPOUND
  }

  /**
   * Create a Term from a String representation, will use the definitions of 
   * operators and primitives that currently apply.
   * @param theExpression
   * @return A compacted and normalized {@link Term}.
   */
   Term parse(CharSequence theExpression);

  /**
   * Create a Term from virtually any type of object, in particular a CharSequence;
   * this is the highest-level factory. On CharSequence, will call {@link #parse(CharSequence)}.
   * @param theObject
   * @param theMode Kind of Term to instantiate
   * @return A compacted and normalized {@link Term}.
   */
   Term create(Object theObject, FactoryMode theMode);

  /**
   * Normalize a {@link Term} using the current definitions of operators, primitives, etc.
   */
   Term normalize(Term theTerm);

}
