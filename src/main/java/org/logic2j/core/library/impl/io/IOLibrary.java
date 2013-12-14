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
import org.logic2j.core.api.model.symbol.Struct;
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

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, Bindings theGoalVars, SolutionListener theListener) {
        final Object result;
        final Object[] args = theGoalStruct.getArgs();
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        if (theMethodName == "nolog") {
            result = nolog(theListener, theGoalVars, args);
        } else if (theMethodName == "write") {
            result = write(theListener, theGoalVars, args);
        } else if (theMethodName == "info") {
            result = info(theListener, theGoalVars, args);
        } else if (theMethodName == "debug") {
            result = debug(theListener, theGoalVars, args);
        } else if (theMethodName == "warn") {
            result = warn(theListener, theGoalVars, args);
        } else if (theMethodName == "error") {
            result = error(theListener, theGoalVars, args);
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }

    @Primitive
    public Continuation write(SolutionListener theListener, Bindings theBindings, Object... terms) {
        for (final Object term : terms) {
            final Bindings b = theBindings.narrow(term, Term.class);
            final Object value = b.getReferrer();

            String format = getProlog().getTermExchanger().marshall(value).toString();
            format = IOLibrary.unquote(format);
            this.writer.print(format);
        }
        return notifySolution(theListener);
    }

    @SuppressWarnings("unused")
    @Primitive
    public Continuation nl(SolutionListener theListener, Bindings theBindings) {
        this.writer.print('\n');
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation debug(SolutionListener theListener, Bindings theBindings, Object... terms) {
      if (logger.isDebugEnabled()) {
        final String substring = formatForLog(theBindings, terms);
        logger.debug(substring);
      }
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation info(SolutionListener theListener, Bindings theBindings, Object... terms) {
      if (logger.isInfoEnabled()) {
        final String substring = formatForLog(theBindings, terms);
        logger.info(substring);
      }
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation warn(SolutionListener theListener, Bindings theBindings, Object... terms) {
      if (logger.isWarnEnabled()) {
        final String substring = formatForLog(theBindings, terms);
        logger.warn(substring);
      }
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation error(SolutionListener theListener, Bindings theBindings, Object... terms) {
      if (logger.isErrorEnabled()) {
        final String substring = formatForLog(theBindings, terms);
        logger.error(substring);
      }
        return notifySolution(theListener);
    }

    private String formatForLog(Bindings theBindings, Object... terms) {
        final StringBuilder sb = new StringBuilder("P ");
        for (final Object term : terms) {
            final Bindings b = theBindings.narrow(term, Object.class);
            ensureBindingIsNotAFreeVar(b, "log/*");
            final String format = getProlog().getTermExchanger().marshall(b).toString();
            sb.append(format);
            sb.append(' ');
        }
        final String substring = sb.substring(0, sb.length() - 1);
        return substring;
    }

    /**
     * Replace any logging predicate by "nolog" to avoid any overhead with logging.
     * 
     * @param theListener
     * @param theBindings
     * @param terms
     * @return This predicate succeeds with one solution, {@link Continuation#CONTINUE}
     */
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
