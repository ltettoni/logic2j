/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.core.library.impl;

import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

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
  public Object dispatch(String theMethodName, Struct theGoalStruct, UnifyContext currentVars) {
    final SolutionListener theListener = currentVars.getSolutionListener();
    final Object result;
    final Object[] args = theGoalStruct.getArgs();
    // Argument methodName is {@link String#intern()}alized so OK to check by reference
    if (theMethodName == "write") {
      result = write(theListener, currentVars, args);
    } else if (theMethodName == "nl") {
      result = nl(theListener, currentVars);
    } else if (theMethodName == "nolog") {
      result = nolog(theListener, currentVars, args);
    } else if (theMethodName == "debug") {
      result = debug(theListener, currentVars, args);
    } else if (theMethodName == "info") {
      result = info(theListener, currentVars, args);
    } else if (theMethodName == "warn") {
      result = warn(theListener, currentVars, args);
    } else if (theMethodName == "error") {
      result = error(theListener, currentVars, args);
    } else {
      result = NO_DIRECT_INVOCATION_USE_REFLECTION;
    }
    return result;
  }

  @Predicate
  public Integer write(SolutionListener theListener, UnifyContext currentVars, Object... terms) {
    for (final Object term : terms) {
      final Object value = currentVars.reify(term);
      final String formatted = getProlog().getTermMarshaller().marshall(value).toString();
      final String unquoted = IOLibrary.unquote(formatted);
      this.writer.print(unquoted);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public Integer nl(SolutionListener theListener, UnifyContext currentVars) {
    this.writer.println();
    return notifySolution(currentVars);
  }

  @Predicate
  public Integer debug(SolutionListener theListener, UnifyContext currentVars, Object... terms) {
    if (logger.isDebugEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.debug(substring);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public Integer info(SolutionListener theListener, UnifyContext currentVars, Object... terms) {
    if (logger.isInfoEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.info(substring);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public Integer warn(SolutionListener theListener, UnifyContext currentVars, Object... terms) {
    if (logger.isWarnEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.warn(substring);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public Integer error(SolutionListener theListener, UnifyContext currentVars, Object... terms) {
    if (logger.isErrorEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.error(substring);
    }
    return notifySolution(currentVars);
  }

  private String formatForLog(UnifyContext currentVars, Object... terms) {
    final StringBuilder sb = new StringBuilder("P ");
    for (final Object term : terms) {
      Object value = currentVars.reify(term);
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
   * @param currentVars
   * @param terms
   * @return This predicate succeeds with one solution, {@link Continuation#CONTINUE}
   */
  @Predicate
  public Integer nolog(SolutionListener theListener, UnifyContext currentVars, Object... terms) {
    // Do nothing, but succeeds!
    return notifySolution(currentVars);
  }

  private static String unquote(String st) {
    if (st.startsWith(QUOTE) && st.endsWith(QUOTE)) {
      return st.substring(1, st.length() - 1);
    }
    return st;
  }

}
