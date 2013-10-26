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

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
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
        final Bindings theInputBindings = theBindings.narrow(theInput, Object.class);
        if (theInputBindings == null) {
            // Anonymous var. No need to try to unify it will succeed. Notify one solution.
            notifySolution(theListener);
        } else {
            final Bindings theOutputBindings = theBindings.narrow(theOutput, Object.class);

            // traverseAndMap((String) thePredicate, theInputBindings, theOutputBindings, theListener);

            Object[] termAndBindings = new Object[] { theInputBindings.getReferrer(), theInputBindings };
            transformOnce((String) thePredicate, termAndBindings, 1, 1);
            final boolean unified = unify(termAndBindings[0], (Bindings) termAndBindings[1], theOutputBindings.getReferrer(), theOutputBindings);
            notifyIfUnified(unified, theListener);
        }
        return Continuation.CONTINUE;
    }

    /**
     * Repeat transformation(s) until nothing changes.
     * 
     * @param termAndBindings
     */
    public boolean transformAll(final String theTransformationPredicate, final Object[] termAndBindings) {
        boolean anyTransformed = false;
        boolean transformed;
        int iterationLimiter = 10;
        do {
            transformed = transformOnce(theTransformationPredicate, termAndBindings, 0, 0);
            anyTransformed |= transformed;
            iterationLimiter--;
        } while (transformed && iterationLimiter > 0);
        return anyTransformed;
    }

    /**
     * 
     * @param theTransformationPredicate
     * @param termAndBindings
     * @param childrenBefore
     * @param childrenAfter
     * @return
     */
    public boolean transformOnce(final String theTransformationPredicate, final Object[] termAndBindings, int childrenBefore, int childrenAfter) {
        boolean anyTransform = false;
        if (termAndBindings[0] instanceof Struct && ((Struct) (termAndBindings[0])).getArity() > 0) {
            Struct struct = (Struct) termAndBindings[0];
            logger.debug("Found a struct {}", struct);

            // Transform children first (before structure)
            final Object[] preArgs = struct.getArgs();
            final Object[] preTransformedArgs = new Object[preArgs.length];
            if (childrenBefore > 0) {
                int index = -1;
                for (Object arg : preArgs) {
                    ++index;
                    logger.debug("Going to attempt to transform element {}", arg);

                    final Object[] trans2 = new Object[] { arg, termAndBindings[1] };
                    final boolean argTransformed = transformOnce(theTransformationPredicate, trans2, childrenBefore, childrenAfter);
                    anyTransform |= argTransformed;
                    preTransformedArgs[index] = trans2[0];
                }
            }
            if (anyTransform) {
                // If any children have changed, we need to build a new structure
                struct = new Struct(struct.getName(), preTransformedArgs);
            }

            // Now transform the main structure
            final Object[] trans3 = new Object[] { struct, termAndBindings[1] };
            final boolean structTransformed = transformOnce(theTransformationPredicate, trans3);
            anyTransform |= structTransformed;

            // Assemble result; note the side-effect on arg :-(
            termAndBindings[0] = trans3[0];
            termAndBindings[1] = trans3[1];
        } else {
            anyTransform = transformOnce(theTransformationPredicate, termAndBindings);
        }

        //
        // Transform children after
        if (termAndBindings[0] instanceof Struct && childrenAfter > 0) {
            Struct newStruct = (Struct) termAndBindings[0];
            if (newStruct.getArity() > 0) {
                boolean postArgTransformed = false;
                final Object[] postArgs = newStruct.getArgs();
                final Object[] postTransformedArgs = new Object[postArgs.length];
                int index = -1;
                for (Object arg : postArgs) {
                    ++index;
                    final Object[] trans2 = new Object[] { arg, termAndBindings[1] };
                    final boolean argTransformed = transformOnce(theTransformationPredicate, trans2, childrenBefore, childrenAfter);
                    postArgTransformed |= argTransformed;
                    postTransformedArgs[index] = trans2[0];
                }
                if (postArgTransformed) {
                    newStruct = new Struct(newStruct.getName(), postTransformedArgs);
                    termAndBindings[0] = newStruct;
                }
            }
        }

        return anyTransform;
    }

    /**
     * Transform exactly one term, without recursing on children (in case of Struct), and without
     * repeated iterations. The result is passed in the mutable :-( termAndBindings elements.
     * 
     * @param theTransformationPredicate The predicate to apply to transform, the fact or rule should be in the form
     *            theTransformationPredicate(input, output). Variables are allowed. Rules (with :- ) are allowed too.
     *            Only the first matching hit will be used - no need to add a cut (!) in every rule.
     * @param termAndBindings An Object[2] with {term, bindings} to specify argument, will be mutated by the function (in order
     *            to return 2 values...) only when true is returned.
     * @return true when a transformation occured, false when nothing was changed.
     */
    public boolean transformOnce(final String theTransformationPredicate, final Object[] termAndBindings) {
        final Object inputTerm = termAndBindings[0];
        final Bindings inputBindings = (Bindings) termAndBindings[1];
        if (inputTerm instanceof Var) {
            final Var var = (Var) inputTerm;
            if (var.isAnonymous()) {
                // Anonymous var never transformed
                return false;
            }
            if (var.bindingWithin(inputBindings).followLinks().isFree()) {
                // Free variable never transformed
                return false;
            }
        }

        // Build the transformation goal in form of "theTransformationPredicate(TransIn, TransOu)"
        final Var transIn = new Var("TransIn");
        final Var transOut = new Var("TransOut");
        final Struct transformationGoal = (Struct) TermApi.normalize(new Struct((String) theTransformationPredicate, transIn, transOut), /* no library to consider */null);
        final Bindings transformationBindings = new Bindings(transformationGoal);

        // Now bind our transIn var to the original term. Note: we won't have to remember and unbind here since our modified bindings are a
        // local var!
        transIn.bindingWithin(transformationBindings).bindTo(inputTerm, inputBindings);

        // A local listener to extract the solution in case it was found
        final SolutionListener singleMappingResultListener = new SolutionListener() {
            @Override
            public Continuation onSolution() {
                logger.debug("solution: transformationBindings={}", transformationBindings);
                final Bindings narrowed = transformationBindings.narrow(transOut, Object.class);
                termAndBindings[0] = narrowed.getReferrer();
                termAndBindings[1] = Bindings.deepCopyWithSameReferrer(narrowed);
                logger.debug("solution: narrow={} bindings={}", termAndBindings[0], termAndBindings[1]);
                // Don't need anything more than the first solution. Also this value will be returned
                // from Solver.solveGoal() and this will indicate we reached one solution!
                return Continuation.USER_ABORT;
            }
        };
        // Now attempt the transformation
        final Continuation continuation = getProlog().getSolver().solveGoal(transformationBindings, singleMappingResultListener);
        // We found a transformation only if the result USER_ABORT from onSolution() was hit.
        final boolean oneSolutionFound = continuation == Continuation.USER_ABORT;
        return oneSolutionFound;
    }

}
