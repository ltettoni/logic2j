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

package org.logic2j.core.api;

import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TNumber;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.Var;

/**
 * Convert from Java objects to {@link Term}s, and from Terms to Java objects.
 */
public interface TermAdapter {

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
     * Create a Term from virtually any class of {@link Object}; this is the highest-level factory
     * 
     * @param theObject
     * @param theMode
     * @return A factorized and normalized {@link Term}.
     */
    Term term(Object theObject, FactoryMode theMode);

    /**
     * Create a Struct with arguments from virtually any class of {@link Object}; this is the highest-level factory
     * 
     * @param thePredicateName The predicate (functor)
     * @param theMode
     * @param theArguments
     * @return A factorized and normalized {@link Term}.
     */
    Term term(String thePredicateName, FactoryMode theMode, Object... theArguments);

    /**
     * Convert a Term into the desired target Class.
     * 
     * @param theTargetClass
     * @return
     */
    <T> T object(Term t, Class<T> theTargetClass);
}
