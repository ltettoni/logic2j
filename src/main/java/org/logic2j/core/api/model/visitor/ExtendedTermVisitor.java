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

import org.logic2j.core.api.model.term.TermVisitor;

/**
 * Extension of the {@link org.logic2j.core.api.model.term.TermVisitor} for type of classes that are NOT
 * subclasses of {@link org.logic2j.core.api.model.term.Term}.
 * This requires calling {@link org.logic2j.core.api.model.term.TermApi#accept(ExtendedTermVisitor, Object theTerm)}
 */
public interface ExtendedTermVisitor<T> extends TermVisitor<T> {

    /**
     * Extra visiting method for String, because often String need special handling,
     * for example in a visitor to marshall a Term, quoting needs to be done.
     * @param theAtomString
     * @return
     */
    T visit(String theAtomString);

    /**
     * The "fallback" method on Object will be invoked if no other more specific visit() method
     * could be found.
     * @param theObject
     * @return
     */
    T visit(Object theObject);
}
