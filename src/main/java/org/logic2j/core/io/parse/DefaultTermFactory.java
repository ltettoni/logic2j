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
package org.logic2j.core.io.parse;

import org.logic2j.core.TermFactory;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.io.parse.tuprolog.Parser;
import org.logic2j.core.library.mgmt.LibraryContent;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.TermApi;

/**
 * Default implementation of {@link TermFactory}
 */
public class DefaultTermFactory implements TermFactory {

    private static final TermApi TERM_API = new TermApi();
    private final PrologImplementation prolog;

    public DefaultTermFactory(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    /**
     * Calls {@link TermApi#normalize(Term, LibraryContent)}.
     */
    @Override
    public Term normalize(Term theTerm) {
        return TERM_API.normalize(theTerm, this.prolog.getLibraryManager().wholeContent());
    }

    @Override
    public Term parse(CharSequence theExpression) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theExpression.toString());
        final Term parsed = parser.parseSingleTerm();
        final Term normalized = normalize(parsed);
        return normalized;
    }

    // TODO: be smarter to handle Arrays and Collections, and Iterables
    @Override
    public Term create(Object theObject, FactoryMode theMode) {
        if (theObject instanceof CharSequence) {
            if (theMode == FactoryMode.ATOM) {
                return new Struct(theObject.toString());
            }
            return parse((CharSequence) theObject);
        }
        final Term created = TERM_API.valueOf(theObject, theMode);
        final Term normalized = normalize(created);
        return normalized;
    }
}
