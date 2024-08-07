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

import java.io.PrintStream;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.unify.UnifyContext;

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
  public Object dispatch(String theMethodName, Struct<?> theGoalStruct, UnifyContext currentVars) {
    final Object result;
    final Object[] args = theGoalStruct.getArgs();
    // Argument methodName is {@link String#intern()}alized so OK to check by reference
      result = switch (theMethodName) {
          case "write" -> write(currentVars, args);
          case "nl" -> nl(currentVars);
          case "nolog" -> nolog(currentVars, args);
          case "debug" -> debug(currentVars, args);
          case "info" -> info(currentVars, args);
          case "warn" -> warn(currentVars, args);
          case "error" -> error(currentVars, args);
          case null, default -> NO_DIRECT_INVOCATION_USE_REFLECTION;
      };
    return result;
  }

  @Predicate
  public int write(UnifyContext currentVars, Object... terms) {
    for (final Object term : terms) {
      final Object value = currentVars.reify(term);
      final String formatted = getProlog().getTermMarshaller().marshall(value).toString();
      final String unquoted = IOLibrary.unquote(formatted);
      this.writer.print(unquoted);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public int nl(UnifyContext currentVars) {
    this.writer.println();
    return notifySolution(currentVars);
  }

  @Predicate
  public int debug(UnifyContext currentVars, Object... terms) {
    if (logger.isDebugEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.debug(substring);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public int info(UnifyContext currentVars, Object... terms) {
    if (logger.isInfoEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.info(substring);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public int warn(UnifyContext currentVars, Object... terms) {
    if (logger.isWarnEnabled()) {
      final String substring = formatForLog(currentVars, terms);
      logger.warn(substring);
    }
    return notifySolution(currentVars);
  }

  @Predicate
  public int error(UnifyContext currentVars, Object... terms) {
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
   * @param currentVars
   * @param terms
   * @return This predicate succeeds with one solution, {@link Continuation#CONTINUE}
   */
  @Predicate
  public int nolog(UnifyContext currentVars, Object... terms) {
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
