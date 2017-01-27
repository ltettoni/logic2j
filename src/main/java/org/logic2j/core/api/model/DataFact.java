/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.logic2j.core.api.model;

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;

import java.util.Arrays;

/**
 * Represent one constant data element that can unify to a n-arity flat {@link Struct},
 * for example functor(a, 'B', 12).
 * This is intended for efficient storage of data instead of using Clauses.
 * This is an immutable value object.
 */
public final class DataFact {

    /**
     * Elments are actually public - this object id just a data container, not a JavaBean.
     */
    public final Object[] elements;

    public DataFact(Object... arguments) {
        if (arguments == null || arguments.length < 2) {
            throw new PrologNonSpecificError("Dubious instantiation of DataFact with null record, or arity < 1");
        }
        this.elements = new Object[arguments.length];
        this.elements[0] = ((String) arguments[0]).intern();
        // Internalize all strings
        for (int i = 1; i < arguments.length; i++) {
            this.elements[i] = TermApi.valueOf(arguments[i], TermAdapter.FactoryMode.ATOM);
        }
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public String functor() {
        return (String) this.elements[0];
    }

    public int arity() {
        return this.elements.length - 1;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + Arrays.asList(this.elements).toString();
    }
}
