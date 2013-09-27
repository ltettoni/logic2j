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
package org.logic2j.core.library.impl.io;

import java.io.PrintStream;

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

public class IOLibrary extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IOLibrary.class);

    private static final String QUOTE = "'";

    final PrintStream writer = System.out;

    public IOLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Primitive
    public Continuation write(SolutionListener theListener, Bindings theBindings, Object... terms) {
        for (final Object term : terms) {
            final Bindings b = theBindings.focus(term, Term.class);
            final Object value = b.getReferrer();

            String format = getProlog().getTermExchanger().marshall(value).toString();
            format = IOLibrary.unquote(format);
            this.writer.print(format);
        }
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation nl(SolutionListener theListener, Bindings theBindings) {
        this.writer.print('\n');
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation log(SolutionListener theListener, Bindings theBindings, Object... terms) {
        for (final Object term : terms) {
            final Bindings b = theBindings.focus(term, Term.class);
            ensureBindingIsNotAFreeVar(b, "write/*");
            final Object value = b.getReferrer();

            String format = getProlog().getTermExchanger().marshall(value).toString();
            format = IOLibrary.unquote(format);
            logger.info(format);
        }
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation nolog(SolutionListener theListener, Bindings theBindings, Object... terms) {
        // Do nothing, but succeeds!
        return notifySolution(theListener);
    }

    private static String unquote(String st) {
        if (st.startsWith(QUOTE) && st.endsWith(QUOTE)) {
            return st.substring(1, st.length() - 1);
        }
        return st;
    }

}
