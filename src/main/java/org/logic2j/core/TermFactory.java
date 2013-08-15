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
 * Factory methods to unmarshall {@link Term}s from data of a different nature such as {@link Object}s or streamable representations.
 * 
 * TODO Should become bidirectional: be extended towards marshalling Terms into Java objects or streamable representations (currently
 * covered by the {@link Formatter}). Currently we have a Term->String in {@link Formatter}, String->Term and Object->Term in
 * {@link TermFactory}, and nothing for Term->Object. Should we split by direction (in vs. out), or by type of representation (Stream vs.
 * Pojo).
 * 
 * @note A TermFactory must know its {@link PrologImplementor}, because it has to identify operators and primitives registered therein.
 * @note This interface is still deficient for handling channel streams - some more design needed.
 */
public interface TermFactory {

    static enum FactoryMode {
        /**
         * Result will always be an atom (a {@link Struct} of 0-arity), will never be a {@link Var}iable.
         */
        ATOM,

        /**
         * Result will be either an atom (a {@link Struct} of 0-arity), a numeric ({@link TNumber}), but not a {@link Var}iable neither a
         * compound {@link Struct}.
         */
        LITERAL,

        /**
         * Result will be any {@link Term} (atom, number, {@link Var}iable), but not a compound {@link Struct}.
         */
        ANY_TERM,

        /**
         * Result can be any term plus compound structures.
         */
        COMPOUND
    }

    /**
     * Create a Term from virtually any class of {@link Object}, in particular a {@link CharSequence}; this is the highest-level factory.
     * For a {@link CharSequence}, this will call {@link #parse(CharSequence)}.
     * 
     * @param theObject
     * @param theMode Kind of Term to instantiate
     * @return A factorized and normalized {@link Term}.
     */
    // TODO is "create" a good name ?
    Term create(Object theObject, FactoryMode theMode);

    /**
     * Create a Term from a character representation, will leverage the definitions of operators and primitives that currently apply.
     * 
     * @param theExpression
     * @return A factorized and normalized {@link Term}.
     */
    Term parse(CharSequence theExpression);

    /**
     * Normalize a {@link Term} using the current definitions of operators, primitives.
     * 
     * @param theTerm To be normalized
     * @return A {@link Term} ready to be used for inference (in a Theory ore as a goal)
     */
    Term normalize(Term theTerm);

}
