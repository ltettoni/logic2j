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
package org.logic2j.core.impl;

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.impl.io.parse.tuprolog.Parser;

/**
 * Default and reference implementation of {@link TermAdapter}.
 */
public class DefaultTermAdapter implements TermAdapter {

    private static final TermApi TERM_API = new TermApi();
    private final PrologImplementation prolog;

    public DefaultTermAdapter(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    // TODO be smarter to handle Arrays and Collections, and Iterables
    @Override
    public Term term(Object theObject, FactoryMode theMode) {
        // FIXME TEMPORARY JUST FOR COMPATIBILITY - move this to TermExchanger
        if (theObject instanceof CharSequence) {
            if (theMode == FactoryMode.ATOM) {
                return new Struct(theObject.toString());
            }
            final Parser parser = new Parser(this.prolog.getOperatorManager(), ((CharSequence) theObject).toString());
            final Term parsed = parser.parseSingleTerm();
            final Term normalized = TERM_API.normalize(parsed, this.prolog.getLibraryManager().wholeContent());
            return normalized;
        }

        final Term created = termFrom(theObject, theMode);
        final Term normalized = TERM_API.normalize(created, this.prolog.getLibraryManager().wholeContent());
        return normalized;
    }

    /**
     * Factory that can be overridden.
     * 
     * @param theObject
     * @param theMode
     * @return An instance of Term
     */
    protected Term termFrom(Object theObject, FactoryMode theMode) {
        if (theObject == null) {
            throw new InvalidTermException("Cannot create Term from a null argument");
        }
        Term result = null;
        if (theObject instanceof CharSequence || theObject instanceof Character) {
            // Rudimentary parsing
            final String chars = theObject.toString();
            if (theMode == FactoryMode.ATOM) {
                // Anything becomes an atom, actually only a Struct since we don't have powerful parsing here
                result = new Struct(chars);
            }
        }
        // Otherwise apply basic algorithm from TermApi
        if (result == null) {
            result = TERM_API.valueOf(theObject);
        }
        return result;
    }

    @Override
    public <T> T object(Term t, Class<T> theTargetClass) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
