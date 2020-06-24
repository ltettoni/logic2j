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

import org.logic2j.core.api.library.PLibrary;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;

/**
 * Base class for libraries, provides convenience methods to unify, deunify, and access the underlying {@link PrologImplementation}
 * features.
 */
public class LibraryBase implements PLibrary {
  private final PrologImplementation prolog;

  public LibraryBase(PrologImplementation theProlog) {
    this.prolog = theProlog;
  }

  /**
   * Direct dispatch to avoid reflective invocation using Method.invoke() due to performance reasons.
   * You MAY override this method, if you don't, reflection will be used instead at a little performance cost.
   * <p/>
   * TODO Document example of typical overriding of dispatch()
   *
   * @param theMethodName The name of the method, internalized using {@link String#intern()} so you can use ==
   * @param theGoalStruct Regular argument for invoking a primitive
   * @param currentVars   Regular argument for invoking a primitive
   */
  @Override
  public Object dispatch(String theMethodName, Struct<?> theGoalStruct, UnifyContext currentVars) {
    return PLibrary.NO_DIRECT_INVOCATION_USE_REFLECTION;
  }


  /**
   * Notify the SolutionListener that a solution has been found.
   *
   * @param currentVars
   * @return The {@link Continuation} as returned by {@link SolutionListener#onSolution(UnifyContext)}
   */
  protected int notifySolution(UnifyContext currentVars) {
    final int continuation = currentVars.getSolutionListener().onSolution(currentVars);
    return continuation;
  }

  /**
   * Make sure term is not a free {@link Var}.
   *
   * @param term
   * @param nameOfPrimitive Non functional - only to report the name of the primitive in case an Exception is thrown
   * @param indexOfArg      zero-based index of argument causing error
   * @throws InvalidTermException
   */
  protected void ensureBindingIsNotAFreeVar(Object term, String nameOfPrimitive, int indexOfArg) {
    if (term instanceof Var<?>) {
      // TODO Should be a kind of InvalidGoalException instead?
      final int positionOfArgument = indexOfArg + 1;
      throw new InvalidTermException(
              "Cannot invoke primitive \"" + nameOfPrimitive + "\" with a free variable, check argument #" + positionOfArgument);
    }
  }

  /**
   * Unify terms t1 and t2, and if they could be unified, call theListener with the solution of the newly
   * unified variables; return the result from notifying. If not, return CONTINUE.
   *
   * @param currentVars
   * @param t1
   * @param t2
   * @return
   */
  protected int unifyAndNotify(UnifyContext currentVars, Object t1, Object t2) {
    final UnifyContext after = currentVars.unify(t1, t2);
    if (after == null) {
      // Not unified: do not notify a solution and inform to continue solving
      return Continuation.CONTINUE;
    }
    // Unified
    return notifySolution(after);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  /**
   * @return the prolog
   */
  protected PrologImplementation getProlog() {
    return this.prolog;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

}
