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
package org.logic2j.core.adapter;

import org.logic2j.core.TermAdapter;
import org.logic2j.core.TermFactory;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.TermApi;

/**
 * Default implementation of {@link TermFactory}
 */
public class DefaultTermAdapter implements TermAdapter {

    private static final TermApi TERM_API = new TermApi();
    private final PrologImplementation prolog;

    public DefaultTermAdapter(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    // TODO be smarter to handle Arrays and Collections, and Iterables
    @Override
    public Term term(Object theObject) {
        final Term created = TERM_API.valueOf(theObject, TermFactory.FactoryMode.COMPOUND);
        final Term normalized = TERM_API.normalize(created, this.prolog.getLibraryManager().wholeContent());
        return normalized;
    }

    @Override
    public <T> T object(Term t, Class<T> theTargetClass) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
