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

import java.util.List;

import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.Var;

/**
 * Convert from Java objects to {@link Term}s, and from Terms to Java objects.
 */
public interface TermAdapter {

    static enum FactoryMode {
        /**
         * Result will always be an atom (a {@link Struct} of 0-arity), will never be a {@link Var}iable.
         * In the case of null, will create an empty-string atom.
         */
        ATOM,

        /**
         * Result will be either an atom (a {@link Struct} of 0-arity), an object, but not a {@link Var}iable neither a
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
     * Describe the shape that complex data structures should take as {@link Term}s.
     */
    static enum AssertionMode {
        /**
         * Data is asserted as "named triples". For a dataset called myData, assertions will be such as:
         * myData(entityIdentifier, propertyName, propertyValue).
         */
        EAV_NAMED,
        /**
         * Data is asserted as "quads". The predicate is always "eavt" (entity, attribute, value, transaction).
         * The "transaction" identifier is the dataset name. For example:
         * eavt(entityIdentifier, propertyName, propertyValue, myData).
         */
        EAVT,
        /**
         * Data is asserted as full records with one argument per column. The order matters. This is the least
         * flexible format since changes to the tabularData (adding or removing or reordering columns) will change the assertions.
         * myData(valueOfColumn1, valueOfColumn2, valueOfColumn3, ..., valueOfColumnN).
         */
        RECORD
    }

    /**
     * Instantiate a Term from virtually any class of single {@link Object}; this is the highest-level factory
     * 
     * @param theObject
     * @param theMode
     * @return A factorized and normalized {@link Term}.
     */
    Object term(Object theObject, FactoryMode theMode);

    /**
     * Instantiate a Struct with arguments from virtually any class of {@link Object}; this is the highest-level factory
     * 
     * @param thePredicateName The predicate (functor)
     * @param theMode
     * @param theArguments
     * @return A factorized and normalized {@link Term}.
     */
    Object term(String thePredicateName, FactoryMode theMode, Object... theArguments);

    /**
     * Instantiate a list of Terms from one (possibly large) {@link Object}.
     * 
     * @param theObject
     * @return
     */
    List<Object> terms(Object theObject, AssertionMode theAssertionMode);

    /**
     * Convert a Term into the desired target Class.
     * 
     * @param theTargetClass
     * @return
     */
    <T> T object(Term t, Class<T> theTargetClass);
}
