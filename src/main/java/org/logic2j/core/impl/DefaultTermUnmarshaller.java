/*
 * logic2j - "Bring Logic to your Java" - Copyright (QUOTE) 2011 Laurent.Tettoni@gmail.com
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

import org.logic2j.core.api.TermUnmarshaller;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.impl.io.parse.tuprolog.Parser;

/**
 * Default and reference implementation of {@link TermExchanger}.
 */
public class DefaultTermUnmarshaller implements TermUnmarshaller {

    private final PrologImplementation prolog;

    /**
     * This constructor should only be used internally - for basic formatting.
     */
    public DefaultTermUnmarshaller() {
        this(null);
    }

    public DefaultTermUnmarshaller(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    @Override
    public Object unmarshall(CharSequence theChars) {
        final Parser parser = new Parser(this.prolog, theChars);
        final Object parsed = parser.parseSingleTerm();
        final Object normalized = TermApi.normalize(parsed, this.prolog.getLibraryManager().wholeContent());
        return normalized;
    }

}
