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
package org.logic2j.core.api.model.visitor;


import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;

/**
 * Base implementation of {@link TermVisitor} that does nothing except for
 * {@link org.logic2j.core.api.model.term.Struct}, which are traversed recursively until the first
 * accept() returns a non-null value - then returns that value.<br/>
 * Intended to be derived when implementing useful {@link TermVisitor}s.
 * Appropriate for searching through structures, or traversing all.
 */
public class ExtendedTermVisitorBase<T> implements ExtendedTermVisitor<T> {

    /**
     * Delegate to all sub-elements, will traverse children in depth-first order starting with 0, 1, ...
     * until the last OR until the first that returns a non-null result.
     * 
     * @return The first returning a non-null result breaks the traveral of children and the result is returned.
     */
    @Override
    public T visit(Struct theStruct) {
        // Recurse through children
        final Object[] args = theStruct.getArgs();
        for (int i = 0; i < args.length; i++) {
            final T result = TermApi.accept(this, args[i]);
            // Until the first returning a non-null result
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public T visit(Var theVar) {
        return null;
    }

    @Override
    public T visit(String theAtomString) {
        return null;
    }

    @Override
    public T visit(Object theObject) {
        return null;
    }
}
