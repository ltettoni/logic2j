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
package org.logic2j.core.api.model.symbol;

import org.logic2j.core.api.model.TermVisitor;

import java.io.Serializable;
import java.util.Collection;

/**
 * Term class is the root abstract class for all Prolog data types. The following notions apply on terms, see also the {@link TermApi} class
 * for methods to manage {@link Term}s.
 * <ul>
 * <li>Structural equality, see {@link TermApi#structurallyEquals(Object, Object)}</li>
 * <li>Factorization, see {@link TermApi#factorize(Object)}</li>
 * <li>Initialization of {@link Var} indexes, see {@link TermApi#assignIndexes(Object, int)}</li>
 * <li>Normalization: includes initialization of indexes, factorization, and identification of primitive functors</li>
 * </ul>
 *
 * @note Maybe one day we will need a subclass to represent timestamps.
 * @see Struct
 * @see Var
 */
public abstract class Term implements Serializable {
    /**
     * A value of index=={@value} means it was not initialized.
     */
    public static final int NO_INDEX = -1;
    /**
     * For {@link Var}s: defines the <br/>
     * For a {@link Struct}: defines <br/>
     * The default value is NO_INDEX.
     */
    public short index = NO_INDEX;
    /**
     * A value of index=={@value} means this is the anonymous variable.
     */
    public static final int ANON_INDEX = -2;
    private static final long serialVersionUID = 1L;

    // Use the following line instead to really isDebug the display of terms:
    // private static final Formatter defaultMarshaller = new
    // org.logic2j.core.optional.io.format.DetailedFormatter();

    // ---------------------------------------------------------------------------
    // Graph traversal methods, template methods with "protected" scope, user code should use TermApi methods instead.
    // Some traversal are implemented by the Visitor design pattern and the #accept() method
    // ---------------------------------------------------------------------------

//    /**
//     * Find the first {@link Term} that is either same, or structurally equal to this.
//     *
//     * @param findWithin
//     * @return The {@link Term} found or null when none found.
//     */
//    protected Object findStructurallyEqualWithin(Collection<Object> findWithin) {
//        for (final Object term : findWithin) {
//            if (term != this && TermApi.structurallyEquals(term, this)) {
//                return term;
//            }
//        }
//        return null;
//    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public short getIndex() {
        return this.index;
    }

    // ---------------------------------------------------------------------------
    // Visitor
    // ---------------------------------------------------------------------------

    public abstract <T> T accept(TermVisitor<T> theVisitor);


    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

}
