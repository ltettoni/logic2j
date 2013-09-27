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

package org.logic2j.core.api.model;

import java.util.Arrays;

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;

/**
 * Represent one constant data element that can unify to a n-arity flat {@link Struct},
 * for example functor(a, 'B', 12).
 */
public final class DataFact {

    public Object[] elements;

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

    public String functor() {
        return (String) elements[0];
    }

    public int arity() {
        return elements.length - 1;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + Arrays.asList(elements).toString();
    }
}
