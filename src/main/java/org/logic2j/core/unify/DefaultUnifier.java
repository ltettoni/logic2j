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
package org.logic2j.core.unify;

import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.TNumber;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.Var;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.GoalFrame;
import org.logic2j.core.util.ReportUtils;

/**
 * Reference implementation of the unification - this must always work OK although probably not the best possible implementation.
 */
public class DefaultUnifier implements Unifier {

    @Override
    public boolean unify(Term term1, Bindings theBindings1, Term term2, Bindings theBindings2, GoalFrame theGoalFrame) {
        // Remember where we were so that we can deunify
        theGoalFrame.markBeforeAddingBindings();
        // Now attempt unifiation
        final boolean unified = unifyInternal(term1, theBindings1, term2, theBindings2, theGoalFrame);
        if (!unified /* && theGoalFrame != null */) {
            deunify(theGoalFrame);
        }
        return unified;
    }

    /**
     * Starts the unification and recurse; this method DOES changes to both {@link Bindings} and could leave changes even if it eventually
     * cannot succeed and will return false. You MUST make sure to deunify if it returned false.
     *
     * @note The Orientation of method arguments tends to be variables on term1 and literals on term2, but of course this method is
     *       symmetric.
     *
     * @param term1
     * @param theBindings1
     * @param term2
     * @param theBindings2
     * @param theGoalFrame
     * @return true when unified, false when not (but partial changes might have been done to either {@link Bindings})
     */
    private boolean unifyInternal(Term term1, Bindings theBindings1, Term term2, Bindings theBindings2, GoalFrame theGoalFrame) {
        if (term2 instanceof Var && !(term1 instanceof Var)) {
            return unifyInternal(term2, theBindings2, term1, theBindings1, theGoalFrame);
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
                binding1.bindTo(term2, theBindings2, theGoalFrame);
                return true;
            } else if (binding1.isLiteral()) {
                // We have followed term1 to end up with a literal. It may either unify or not depending if
                // term2 is a Var or the same literal. To simplify implementation we recurse with the constant
                // part as term2
                return unifyInternal(term2, theBindings2, binding1.getTerm(), binding1.getLiteralBindings(), theGoalFrame);
            } else {
                throw new IllegalStateException("Internal error, unexpected binding type for " + binding1);
            }
        }
        // term1 can only be a TNumber or Struct
        if (term1 instanceof TNumber) {
            if (term2 instanceof TNumber) {
                return term1.equals(term2);
            }
            return false;
        } else if (term1 instanceof Struct) {
            if (term2 instanceof Struct) {
                final Struct s1 = (Struct) term1;
                final Struct s2 = (Struct) term2;
                if (!(s1.nameAndArityMatch(s2))) {
                    return false;
                }
                if (s1.getClass() != s2.getClass()) {
                    // Must unify same (sub?) classes of Struct
                    return false;
                }
                final int arity1 = s1.getArity();
                for (int i = 0; i < arity1; i++) {
                    if (!unifyInternal(s1.getArg(i), theBindings1, s2.getArg(i), theBindings2, theGoalFrame)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } else {
            throw new IllegalStateException("Internal bug, term1 is of unexpected " + term1.getClass());
        }
        // return result;
    }

    @Override
    public void deunify(GoalFrame theGoalFrame) {
        theGoalFrame.undoBindingsUntilPreviousMark();
    }

    // ---------------------------------------------------------------------------
    // Core java.lang.Object methods
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
