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
package org.logic2j.core.impl.unify;

import org.logic2j.core.api.Unifier;
import org.logic2j.core.api.model.DataFact;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.TermBindings;
import org.logic2j.core.impl.unify.BindingTrail.StepInfo;
import org.logic2j.core.impl.util.ReportUtils;

/**
 * Reference implementation of the unification - this must always work OK although probably not the best possible implementation.
 */
public class DefaultUnifier implements Unifier {

    @Override
    public boolean unify(Object term1, TermBindings theBindings1, Object term2, TermBindings theBindings2) {
        // Remember where we were so that we can deunify
        final StepInfo stepInfo = BindingTrail.markBeforeAddingBindings();
        // Now attempt unifiation
        final boolean unified = unifyInternal(stepInfo, term1, theBindings1, term2, theBindings2);
        if (!unified) {
            BindingTrail.undoBindingsUntilPreviousMark(stepInfo);
        }
        return unified;
    }

    /**
     * Starts the unification and recurse; this method DOES changes to both {@link TermBindings} and could leave changes even if it eventually
     * cannot succeed and will return false. You MUST make sure to deunify if it returned false.
     * 
     * @param stepInfo
     * 
     * @note The Orientation of method arguments tends to be variables on term1 and literals on term2, but of course this method is
     *       symmetric. In the case of 2 free vars, term1 is linked to term2.
     * 
     * @param term1
     * @param theBindings1
     * @param term2
     * @param theBindings2
     * @return true when unified, false when not (but partial changes might have been done to either {@link TermBindings})
     */
    private boolean unifyInternal(StepInfo stepInfo, Object term1, TermBindings theBindings1, Object term2, TermBindings theBindings2) {
        if (term1 == term2 && theBindings1 == theBindings2) {
            // Atoms now share the same address - we can optimize their unification.
            // Notice that due to factorization, struct such as [H|T] may also share the same location
            // so we can only assume they unify if the bindings are the same too
            return true;
        }
        if (term2 instanceof Var && !(term1 instanceof Var)) {
            // Prefer unifying Var to const so we swap args - this is purely conventional
            return unifyInternal(stepInfo, term2, theBindings2, term1, theBindings1);
        }
        if (term1 instanceof Var) {
            // Variable:
            // - when anonymous, unifies
            // - when free, bind it
            // - when bound, follow VARs until end of chain
            final Var var1 = (Var) term1;
            if (var1.isAnonymous()) {
                return true;
            }
            final Binding binding1 = var1.bindingWithin(theBindings1).followLinks();
            // Followed chain to the end until we hit either a FREE or LITERAL binding
            if (binding1.isFree()) {
                // Should not bind to an anonymous variable
                if ((term2 instanceof Var) && ((Var) term2).isAnonymous()) {
                    return true;
                }
                // Bind the free var
                if (binding1.bindTo(term2, theBindings2)) {
                    BindingTrail.addBinding(binding1);
                }
                return true;
            } else if (binding1.isLiteral()) {
                // We have followed term1 to end up with a literal. It may either unify or not depending if
                // term2 is a Var or the same literal. To simplify implementation we recurse with the constant
                // part as term2
                return unifyInternal(stepInfo, term2, theBindings2, binding1.getTerm(), binding1.getTermBindings());
            } else {
                throw new IllegalStateException("Internal error, unexpected binding type for " + binding1);
            }
        }
        // term1 can only be a Struct or an Object
        if (term1 instanceof Struct) {
            if (term2 instanceof Struct) {
                final Struct s1 = (Struct) term1;
                final Struct s2 = (Struct) term2;
                // Signatures are {@link String#intern()}alized so OK to check by reference
                if (s1.getPredicateSignature() != s2.getPredicateSignature()) {
                    return false;
                }
                final int arity = s1.getArity();
                for (int i = 0; i < arity; i++) {
                    if (!unifyInternal(stepInfo, s1.getArg(i), theBindings1, s2.getArg(i), theBindings2)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } else {
            if (term1 == null) {
                throw new NullPointerException("Cannot unify term1=null with term2=" + term2);
            }

            return term1.equals(term2);
        }
    }

    @Override
    public boolean unify(Object goalTerm, TermBindings theGoalBindings, DataFact dataFact) {
        if (!(goalTerm instanceof Struct)) {
            // Only Struct could match a DataFact
            return false;
        }
        final Struct s1 = (Struct) goalTerm;
        final Object[] elements = dataFact.elements;
        if (s1.getName() != elements[0]) {// Names are {@link String#intern()}alized so OK to check by reference
            // Functor must match
            return false;
        }
        final int arity = s1.getArity();
        if (arity != elements.length - 1) {
            // Arity must match as well
            return false;
        }
        final StepInfo stepInfo = BindingTrail.markBeforeAddingBindings();
        boolean unified = true;
        for (int i = 0; i < arity; i++) {
            final Object arg = s1.getArg(i);
            final Object term2 = elements[1 + i];
            if (!simpleUnification(stepInfo, arg, theGoalBindings, term2)) {
                unified = false;
                break;
            }
        }
        if (!unified) {
            BindingTrail.undoBindingsUntilPreviousMark(stepInfo);
        }
        return unified;
        /*
        // Now we are likely to unify something - remember where we were so that we can deunify
        
        // Now attempt unifiation
        boolean unified = true;
        for (int i = 0; i < arity; i++) {
        Term arg = s1.getArg(i);
        Term term2 = (Term) elements[1 + i];
        // if (!simpleUnification(arg, theGoalBindings, term2)) {
        // unified = false;
        // break;
        // }
        }
        deunify();
        if (!unified) {
        deunify();
        }
        return unified;
        */
    }

    private boolean simpleUnification(StepInfo stepInfo, Object term1, TermBindings theBindings1, Object term2) {
        if (term1 instanceof Var) {
            final Var var1 = (Var) term1;
            if (var1.isAnonymous()) {
                return true;
            }
            final Binding binding1 = var1.bindingWithin(theBindings1).followLinks();
            // Followed chain to the end until we hit either a FREE or LITERAL binding
            if (binding1.isFree()) {
                // Bind the free var
                if (binding1.bindTo(term2, theBindings1)) {
                    // We don't care about theBindings, it's a literal, so specify theBindings1
                    BindingTrail.addBinding(stepInfo, binding1);
                }
                return true;
            } else if (binding1.isLiteral()) {
                return simpleUnification(stepInfo, binding1.getTerm(), binding1.getTermBindings(), term2);
            } else {
                throw new IllegalStateException("Internal error, unexpected binding type for " + binding1);
            }
        }
        if (term1 instanceof Struct) {
            // return term1 == term2;
            if (term2 instanceof Struct) {
                final Struct s1 = (Struct) term1;
                final Struct s2 = (Struct) term2;
                // Signatures are {@link String#intern()}alized so OK to check by reference
                return s1.getPredicateSignature() == s2.getPredicateSignature();
            }
            return false;
        } else {
            return term1.equals(term2);
        }
    }

    @Override
    public void deunify() {
        BindingTrail.undoBindingsUntilPreviousMark();
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
