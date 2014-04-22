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
package org.logic2j.core.library.impl;

import org.logic2j.core.api.PLibrary;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.TermMarshaller;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.TermBindings;
import org.logic2j.core.impl.FinalVarTermMarshaller;
import org.logic2j.core.impl.PrologImplementation;

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
     * 
     * TODO Document example of typical overriding of dispatch()
     * 
     * @param theMethodName The name of the method, internalized using {@link String#intern()} so you can use ==
     * @param theGoalStruct Regular argument for invoking a primitive
     * @param theTermBindings Regular argument for invoking a primitive
     * @param theListener Regular argument for invoking a primitive
     */
    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, TermBindings theTermBindings, SolutionListener theListener) {
        return PLibrary.NO_DIRECT_INVOCATION_USE_REFLECTION;
    }

    /**
     * Convenience shortcut to have the current Prolog engine unifying 2 terms.
     * 
     * @param t1
     * @param theBindings1
     * @param t2
     * @param theBindings2
     * @return The result of unification.
     */
    protected boolean unify(Object t1, TermBindings theBindings1, Object t2, TermBindings theBindings2) {
        return this.prolog.getUnifier().unify(t1, theBindings1, t2, theBindings2);
    }

    protected void deunify() {
        this.prolog.getUnifier().deunify();
    }

    /**
     * Notify theSolutionListener that a solution has been found.
     * 
     * @param theSolutionListener
     * @return The {@link Continuation} as returned by theSolutionListener's {@link SolutionListener#onSolution()}
     */
    protected Continuation notifySolution(SolutionListener theSolutionListener) {
        final Continuation continuation = theSolutionListener.onSolution();
        return continuation;
    }

    /**
     * When unified is true, call {@link #notifySolution(SolutionListener)}, and then call {@link #deunify()}. Otherwise
     * nothing is done.
     * 
     * @param unified
     * @param theListener
     * @return The {@link Continuation} as returned by theSolutionListener's {@link SolutionListener#onSolution()}
     */
    protected Continuation notifyIfUnified(boolean unified, SolutionListener theListener) {
        final Continuation continuation;
        if (unified) {
            try {
                continuation = notifySolution(theListener);
            } finally {
                deunify();
            }
        } else {
            continuation = Continuation.CONTINUE;
        }
        return continuation;
    }

    /**
     * Make sure a {@link TermBindings} does not have a {@link TermBindings#getReferrer()} that is a free {@link Var}.
     * 
     * @param theBindings
     * @param nameOfPrimitive Non functional - only to report the name of the primitive in case an Exception is thrown
     * @throws InvalidTermException
     */
    protected void ensureBindingIsNotAFreeVar(TermBindings theBindings, String nameOfPrimitive) {
        if (theBindings.isFreeReferrer()) {
            // TODO Should be a kind of InvalidGoalException instead?
            throw new InvalidTermException("Cannot call primitive " + nameOfPrimitive + " with a Variable that is free");
        }
    }

    protected Object createConstantTerm(Object anyObject) {
        if (anyObject == null) {
            return Var.ANONYMOUS_VAR;
        }
        return this.prolog.getTermAdapter().term(anyObject, FactoryMode.ATOM);
    }

    // Only one use!
    protected void unifyAndNotify(Var[] theVariables, Object[] theValues, TermBindings theBindings, SolutionListener theListener) {
        final Object[] values = new Object[theValues.length];
        for (int i = 0; i < theValues.length; i++) {
            values[i] = createConstantTerm(theValues[i]);
        }
        final boolean unified = unify(new Struct("group", (Object[]) theVariables), theBindings, new Struct("group", values), theBindings);
        notifyIfUnified(unified, theListener);
    }

    /**
     * Format a Term with renditions of final vars, and taking operators into account.
     * @param theTerm
     * @param theBindings
     * @return The formatted String
     */
    protected String format(Object theTerm, final TermBindings theBindings) {
      final TermMarshaller niceFormat2 = new FinalVarTermMarshaller(getProlog(), theBindings);
      final String formatted = niceFormat2.marshall(theTerm).toString();
      return formatted;
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
