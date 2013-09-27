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
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.mgmt.PrimitiveInfo;
import org.logic2j.core.library.mgmt.PrimitiveInfo.PrimitiveType;

/**
 * Base class for libraries.
 */
public class LibraryBase implements PLibrary {
    private final PrologImplementation prolog;

    public LibraryBase(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, Bindings theGoalVars, SolutionListener theListener) {
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
    protected boolean unify(Object t1, Bindings theBindings1, Object t2, Bindings theBindings2) {
        return getProlog().getUnifier().unify(t1, theBindings1, t2, theBindings2);
    }

    protected void deunify() {
        getProlog().getUnifier().deunify();
    }

    /**
     * Notify theSolutionListener that a solution has been found.
     * 
     * @param theSolutionListener
     * 
     * @return
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
     * @return
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
     * Make sure a {@link Bindings} does not have a {@link Bindings#getReferrer()} that is a free {@link Var}.
     * 
     * @param theBindings
     * @param nameOfPrimitive Non functional - only to report the name of the primitive in case an Exception is thrown
     * @throws InvalidTermException
     */
    protected void ensureBindingIsNotAFreeVar(Bindings theBindings, String nameOfPrimitive) {
        if (theBindings.isFreeReferrer()) {
            // TODO should be sort of an InvalidGoalException?
            throw new InvalidTermException("Cannot call primitive " + nameOfPrimitive + " with a Variable that is free");
        }
    }

    // TODO assess if needed - used only once
    protected Binding dereferencedBinding(Object theTerm, Bindings theBindings) {
        if (theTerm instanceof Var) {
            return ((Var) theTerm).bindingWithin(theBindings).followLinks();
        }
        return Binding.createLiteralBinding(theTerm, theBindings);
    }

    /**
     * Evaluates an expression. Returns null value if the argument is not an evaluable expression
     */
    protected Object evaluate(Object theTerm, Bindings theBindings) {
        if (theTerm == null) {
            return null;
        }
        // TODO are the lines below this exactly as in resolve() / substitute() method?
        if (theTerm instanceof Var && !((Var) theTerm).isAnonymous()) {
            final Binding binding = ((Var) theTerm).bindingWithin(theBindings).followLinks();
            if (!binding.isLiteral()) {
                return null;
            }
            theTerm = binding.getTerm();
        }

        if (theTerm instanceof Struct) {
            final Struct struct = (Struct) theTerm;
            final PrimitiveInfo primInfo = struct.getPrimitiveInfo();
            if (primInfo == null) {
                // throw new IllegalArgumentException("Predicate's functor " + struct.getName() + " is not a primitive");
                return null;
            }
            if (primInfo.getType() != PrimitiveType.FUNCTOR) {
                // throw new IllegalArgumentException("Predicate's functor " + struct.getName() + " is a primitive, but not a functor");
                return null;
            }
            final Object result = primInfo.invoke(struct, theBindings, /* no listener */null);
            return result;
        }
        return theTerm;
    }

    protected Object createConstantTerm(Object anyObject) {
        if (anyObject == null) {
            return Var.ANONYMOUS_VAR;
        }
        return getProlog().getTermAdapter().term(anyObject, FactoryMode.ATOM);
    }

    // Only one use!
    protected void unifyAndNotify(Var[] theVariables, Object[] theValues, Bindings theBindings, SolutionListener theListener) {
        final Object[] values = new Object[theValues.length];
        for (int i = 0; i < theValues.length; i++) {
            values[i] = createConstantTerm(theValues[i]);
        }
        final boolean unified = unify(new Struct("group", (Object[]) theVariables), theBindings, new Struct("group", values), theBindings);
        notifyIfUnified(unified, theListener);
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
