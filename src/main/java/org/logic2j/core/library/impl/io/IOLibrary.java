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

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

import java.io.PrintStream;

public class IOLibrary extends LibraryBase {
    /**
     * Name of the logger to which all logging events go.
     */
    private static final String LOGIC2J_PROLOG_LOGGER = "org.logic2j.logger";
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LOGIC2J_PROLOG_LOGGER);

    private static final String QUOTE = "'";

    final PrintStream writer = System.out;

    public IOLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, PoV pov, SolutionListener theListener) {
        final Object result;
        final Object[] args = theGoalStruct.getArgs();
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        if (theMethodName == "nolog") {
            result = nolog(theListener, pov, args);
        } else if (theMethodName == "write") {
            result = write(theListener, pov, args);
        } else if (theMethodName == "info") {
            result = info(theListener, pov, args);
        } else if (theMethodName == "isDebug") {
            result = debug(theListener, pov, args);
        } else if (theMethodName == "warn") {
            result = warn(theListener, pov, args);
        } else if (theMethodName == "error") {
            result = error(theListener, pov, args);
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }

    @Primitive
    public Continuation write(SolutionListener theListener, PoV pov, Object... terms) {
        for (final Object term : terms) {
            final Object value = pov.reify(term);

            String format = getProlog().getTermMarshaller().marshall(value).toString();
            format = IOLibrary.unquote(format);
            this.writer.print(format);
        }
        return notifySolution(theListener, pov);
    }

    @SuppressWarnings("unused")
    @Primitive
    public Continuation nl(SolutionListener theListener, PoV pov) {
        this.writer.print('\n');
        return notifySolution(theListener, pov);
    }

    @Primitive
    public Continuation debug(SolutionListener theListener, PoV pov, Object... terms) {
        if (logger.isDebugEnabled()) {
            final String substring = formatForLog(pov, terms);
            logger.debug(substring);
        }
        return notifySolution(theListener, pov);
    }

    @Primitive
    public Continuation info(SolutionListener theListener, PoV pov, Object... terms) {
        if (logger.isInfoEnabled()) {
            final String substring = formatForLog(pov, terms);
            logger.info(substring);
        }
        return notifySolution(theListener, pov);
    }

    @Primitive
    public Continuation warn(SolutionListener theListener, PoV pov, Object... terms) {
        if (logger.isWarnEnabled()) {
            final String substring = formatForLog(pov, terms);
            logger.warn(substring);
        }
        return notifySolution(theListener, pov);
    }

    @Primitive
    public Continuation error(SolutionListener theListener, PoV pov, Object... terms) {
        if (logger.isErrorEnabled()) {
            final String substring = formatForLog(pov, terms);
            logger.error(substring);
        }
        return notifySolution(theListener, pov);
    }

    private String formatForLog(PoV pov, Object... terms) {
        final StringBuilder sb = new StringBuilder("P ");
        for (final Object term : terms) {
            Object value = pov.reify(term);
            ensureBindingIsNotAFreeVar(value, "log/*");
            final String format = getProlog().getTermMarshaller().marshall(value).toString();
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
    public Continuation nolog(SolutionListener theListener, PoV pov, Object... terms) {
        // Do nothing, but succeeds!
        return notifySolution(theListener, pov);
    }

    private static String unquote(String st) {
        if (st.startsWith(QUOTE) && st.endsWith(QUOTE)) {
            return st.substring(1, st.length() - 1);
        }
        return st;
    }

}