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

import java.io.Serializable;
import java.util.Collection;

import org.logic2j.core.api.TermExchanger;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.DefaultTermExchanger;

/**
 * Term class is the root abstract class for all Prolog data types. The following notions apply on terms, see also the {@link TermApi} class
 * for methods to manage {@link Term}s.
 * <ul>
 * <li>Structural equality, see {@link #structurallyEquals(Term)}</li>
 * <li>Factorization, see {@link #factorize(Collection)}</li>
 * <li>Initialization of {@link Var} indexes, see {@link #assignIndexes(short)}</li>
 * <li>Normalization: includes initialization of indexes, factorization, and identification of primitive functors</li>
 * </ul>
 * 
 * @note Maybe one day we will need a subclass to represent timestamps.
 * @see Struct
 * @see Var
 */
public abstract class Term implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * A value of index=={@value} means it was not initialized.
     */
    public static final int NO_INDEX = -1;

    /**
     * A value of index=={@value} means this is the anonymous variable.
     */
    public static final int ANON_INDEX = -2;

    /**
     * For {@link Var}s: defines the position index within {@link Bindings} where the {@link Binding} of this variable can be found.<br/>
     * For a {@link Struct}: defines the maximal index of any variables that can be found, recursively, under all arguments.<br/>
     * For anyghing else: value is 0 to indicate it was processed (non-default).<br/>
     * The default value is NO_INDEX.
     */
    protected short index = NO_INDEX;

    /**
     * A {@link TermExchanger} for the default's {@link #toString()} to render this {@link Term}.
     * Calling {@link #toString()} should be avoided from caller code, use your preferred
     * instance of {@link TermExchanger} instead. Yet we need {@link #toString()} to work for the cases of
     * logging and while debugging, so let's use a fixed (static) one.
     */
    private static final TermExchanger basicExchanger = new DefaultTermExchanger();

    // Use the following line instead to really debug the display of terms:
    // private static final Formatter basicExchanger = new
    // org.logic2j.core.optional.io.format.DetailedFormatter();

    // ---------------------------------------------------------------------------
    // Graph traversal methods, template methods with "protected" scope, user code should use TermApi methods instead.
    // Some traversal are implemented by the Visitor design pattern and the #accept() method
    // ---------------------------------------------------------------------------

    /**
     * Find the first {@link Term} that is either same, or structurally equal to this.
     * 
     * @param findWithin
     * @return The {@link Term} found or null when none found.
     */
    protected Object findStructurallyEqualWithin(Collection<Object> findWithin) {
        for (final Object term : findWithin) {
            if (term != this && TermApi.structurallyEquals(term, this)) {
                return term;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public short getIndex() {
        return this.index;
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    /**
     * Delegate formatting to our basic {@link TermExchanger}.
     */
    @Override
    public String toString() {
        return basicExchanger.marshall(this).toString();
    }

}
