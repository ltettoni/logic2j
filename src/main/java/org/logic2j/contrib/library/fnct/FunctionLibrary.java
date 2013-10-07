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

package org.logic2j.contrib.library.fnct;

import java.util.ArrayList;

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

/**
 * Functional features (mapping, etc).
 */
public class FunctionLibrary extends LibraryBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibrary.class);

    public FunctionLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Primitive
    public Continuation mapBottomUp(SolutionListener theListener, final Bindings theBindings, final Object thePredicate, final Object theInput, final Object theOutput) {
        if (!(thePredicate instanceof String)) {
            throw new InvalidTermException("Predicate for mapBottomUp/3 must be a String, was " + thePredicate);
        }
        final Bindings theInputBindings = theBindings.focus(theInput, Object.class);
        final Bindings theOutputBindings = theBindings.focus(theOutput, Object.class);

        traverseAndMap((String) thePredicate, theInputBindings, theOutputBindings, theListener);

        // final boolean unified = unify(theInput, theBindings, theOutput, theBindings);
        // return notifyIfUnified(unified, theListener);
        return Continuation.CONTINUE;
    }

    /**
     * @param thePredicate
     * @param theInputBindings
     * @param theOutputBindings
     * @param theListener
     */
    private void traverseAndMap(String thePredicate, final Bindings theInputBindings, final Bindings theOutputBindings, final SolutionListener theListener) {
        if (theInputBindings == null) {
            // Anonymous var specified
            notifySolution(theListener);
            return;
        }
        if (theInputBindings.isFreeReferrer()) {
            // Free variable
            notifySolution(theListener);
            return;
        }
        // Depth first traversal, traverse children first
        if (theInputBindings.getReferrer() instanceof Struct) {
            Struct struct = (Struct) (theInputBindings.getReferrer());
            final Struct theTransformedStructure = new Struct(struct);
            logger.info("Found a struct {}", struct);
            final Object[] args = struct.getArgs();
            final Object[] transformedArgs = new Object[args.length];
            int index = -1;
            for (Object arg : args) {
                index++;
                logger.info("Going to attempt to transform {}", arg);

                // Create a method for this whole mess : create a new normalized structured piggy-backed on another
                final Var zz = new Var("ZZ");
                Struct transformationGoal = new Struct((String) thePredicate, arg, zz);
                transformationGoal = (Struct) TermApi.normalize(transformationGoal, null);
                final Bindings goal = Bindings.shallowCopy(theInputBindings, transformationGoal);

                final SolutionListenerBase singleMappingResultListener = new SolutionListenerBase() {
                    @Override
                    public Continuation onSolution() {
                        return Continuation.USER_ABORT;
                    }
                };
                Continuation mappingResult = getProlog().getSolver().solveGoal(goal, singleMappingResultListener);
                if (mappingResult == Continuation.USER_ABORT) {
                    // One was found
                    transformedArgs[index] = goal.getBinding(zz.getIndex()).followLinks().getTerm();
                } else {
                    // None was found: same arg
                    transformedArgs[index] = arg;
                }
            }
            final boolean unified = unify(theTransformedStructure, theInputBindings, theOutputBindings.getReferrer(), theOutputBindings);
            notifyIfUnified(unified, theListener);
        }

        final Struct transformationGoal = new Struct((String) thePredicate, theInputBindings.getReferrer(), theOutputBindings.getReferrer());
        final Bindings goal = Bindings.shallowCopy(theInputBindings, transformationGoal);

        final SolutionListenerBase singleMappingResultListener = new SolutionListenerBase() {
            @Override
            public Continuation onSolution() {
                logger.info("Found mapping for {} as {}", goal, theOutputBindings);
                notifySolution(theListener);
                super.onSolution();
                return Continuation.USER_ABORT;
            }
        };
        Continuation mappingResult = getProlog().getSolver().solveGoal(goal, singleMappingResultListener);
        if (mappingResult == Continuation.CONTINUE) {
            // No solution found (otherwise we would have returned a USER_ABORT), no mapping, return original
            final boolean unified = unify(theInputBindings.getReferrer(), theInputBindings, theOutputBindings.getReferrer(), theOutputBindings);
            notifyIfUnified(unified, theListener);
        }
    }

    // ---------------------------------------------------------------------------------------

    private Continuation findall(SolutionListener theListener, final Bindings theBindings, final Object theTemplate, final Object theGoal, final Object theResult) {
        final Bindings subGoalBindings = theBindings.focus(theGoal, Object.class);
        ensureBindingIsNotAFreeVar(subGoalBindings, "findall/3");

        // Define a listener to collect all solutions for the goal specified
        final ArrayList<Object> javaResults = new ArrayList<Object>(100); // Our internal collection of results
        final SolutionListener listenerForSubGoal = new SolutionListenerBase() {

            @Override
            public Continuation onSolution() {
                // Calculate the substituted goal value (resolve all bound vars)
                // FIXME !!! This is most certainly wrong: how can we call substitute on a variable expressed in a different bindings?????
                // The case is : findall(X, Expr, Result) where Expr -> something -> expr(a,b,X,c)
                final Object resolvedTemplate = TermApi.substitute(theTemplate, subGoalBindings);
                // Map<String, Term> explicitBindings = goalBindings.explicitBindings(FreeVarRepresentation.FREE);
                // And add as extra solution
                javaResults.add(resolvedTemplate);
                return Continuation.CONTINUE;
            }

        };

        // Now solve the target sub goal
        getProlog().getSolver().solveGoal(subGoalBindings, listenerForSubGoal);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(javaResults);

        // And unify with result
        final boolean unified = unify(theResult, theBindings, plist, theBindings);
        return notifyIfUnified(unified, theListener);
    }

}
