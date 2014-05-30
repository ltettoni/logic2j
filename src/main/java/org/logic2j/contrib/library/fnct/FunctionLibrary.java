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
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;

/**
 * Functional features (mapping, etc).
 */
public class FunctionLibrary extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibrary.class);

    /**
     * Maximal number of iterations when transforming all (until nothing changes). This prevents infinit
     * recursion in case of a cycle in the transformation mappings.
     */
    private static final int MAX_TRANFORM_ITERATIONS = 100;

    public FunctionLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }


    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, PoV pov, SolutionListener theListener) {
        final Object result;
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        final int arity = theGoalStruct.getArity();
        if (arity == 3) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            final Object arg2 = theGoalStruct.getArg(2);
            if (theMethodName == "map") {
//                result = map(theListener, pov, arg0, arg1, arg2);
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }

    // To be reworked completely - now that we don't have Bindings any longer
    
//    @Primitive
//    public Continuation map(SolutionListener theListener, final PoV pov, final Object thePredicate, final Object theInput, final Object theOutput) {
//        if (!(thePredicate instanceof String)) {
//            throw new InvalidTermException("Predicate for map/3 must be a String, was " + thePredicate);
//        }
//        final Object theInputBindings = pov.reify(theInput);
//        if (theInputBindings == null) {
//            // Anonymous var. No need to try to unify it will succeed. Notify one solution.
//            notifySolution(theListener, pov);
//        } else {
//            final Object theOutputBindings = pov.reify(theOutput);
//
//            final Object[] termAndBindings = new Object[] { theInputBindings, theInputBindings };
//
////            transformOnce((String) thePredicate, termAndBindings, true, true);
//            transformAll((String) thePredicate, termAndBindings, true, true);
//
//            final boolean unified = unify(termAndBindings[0], (TermBindings) termAndBindings[1], theOutputBindings.getReferrer(), theOutputBindings);
//            notifyIfUnified(unified, theListener);
//        }
//        return Continuation.CONTINUE;
//    }
//
//    /**
//     * Repeat transformation(s) until nothing changes.
//     *
//     * @param termAndBindings
//     */
//    boolean transformAll(final String theTransformationPredicate, final Object[] termAndBindings, boolean transformArgsBefore, boolean transformArgsAfter) {
//        boolean anyTransformed = false;
//        boolean transformed;
//        int iterationLimiter = MAX_TRANFORM_ITERATIONS;
//        do {
//            transformed = transformOnce(theTransformationPredicate, termAndBindings, transformArgsBefore, transformArgsAfter);
//            anyTransformed |= transformed;
//            iterationLimiter--;
//        } while (transformed && iterationLimiter > 0);
//        return anyTransformed;
//    }
//
//    /**
//     *
//     * @param theTransformationPredicate
//     * @param termAndBindings
//     * @param transformArgsBefore
//     * @param transformArgsAfter
//     * @return true when a transformation occured
//     */
//    boolean transformOnce(final String theTransformationPredicate, final Object[] termAndBindings, boolean transformArgsBefore, boolean transformArgsAfter) {
//        boolean anyTransform = false;
//        boolean needTransformAfter = false;
//        logger.debug("> Enter transform with {}, {}", termAndBindings[0], termAndBindings[1]);
//        if (termAndBindings[0] instanceof Struct && ((Struct) (termAndBindings[0])).getArity() > 0) {
//            Struct struct = (Struct) termAndBindings[0];
//
//            // Transform children first (before structure)
//            final Object[] preArgs = struct.getArgs();
//            final Object[][] preTransformedArgs = new Object[preArgs.length][2];
//            if (transformArgsBefore) {
//                int index = -1;
//                for (final Object arg : preArgs) {
//                    ++index;
//                    final Object[] trans2 = new Object[] { arg, termAndBindings[1] };
//                    final boolean argTransformed = transformOnce(theTransformationPredicate, trans2, transformArgsBefore, transformArgsAfter);
//                    anyTransform |= argTransformed;
//                    preTransformedArgs[index][0] = trans2[0];
//                    preTransformedArgs[index][1] = trans2[1];
//                }
//            }
//            if (anyTransform) {
//                // If any children have changed, we need to build a new structure
//                // Merge all bindings (every transformation might have produced a new TermBindings)
//
//                // Collect all transformed results
//                final List<TermBindings> allTransformedBindings = new ArrayList<TermBindings>();
//                final List<Object> allTransformedArgs = new ArrayList<Object>();
//                for (final Object[] pair : preTransformedArgs) {
//                    allTransformedBindings.add((TermBindings) pair[1]);
//                    allTransformedArgs.add(pair[0]);
//                }
//
//                final IdentityHashMap<Object, Object> remappedVar = new IdentityHashMap<Object, Object>();
//                final TermBindings mergedBindings = TermBindings.merge(allTransformedBindings, remappedVar);
//                termAndBindings[1] = mergedBindings;
//
//                // Clone the structure
//                final Struct structWithNewArgs = new Struct(struct, allTransformedArgs.toArray());
//                final Object cloned = TermApi.clone(structWithNewArgs, mergedBindings, remappedVar);
//                TermApi.assignIndexes(cloned, 0);
//                struct = (Struct) cloned;
//            }
//
//            // Now transform the main structure
//            final Object[] trans3 = new Object[] { struct, termAndBindings[1] };
//            final boolean structTransformed = transformOnce(theTransformationPredicate, trans3);
//            if (structTransformed) {
//                needTransformAfter = true;
//            }
//            anyTransform |= structTransformed;
//
//            // Assemble result; note the side-effect on arg :-(
//            termAndBindings[0] = trans3[0];
//            termAndBindings[1] = trans3[1];
//        } else {
//            // Anything else than Struct
//            anyTransform = transformOnce(theTransformationPredicate, termAndBindings);
//            needTransformAfter |= anyTransform;
//        }
//
//        //
//        // Transform children after
//        if (needTransformAfter && transformArgsAfter && termAndBindings[0] instanceof Struct) {
//            Struct newStruct = (Struct) termAndBindings[0];
//            if (newStruct.getArity() > 0) {
//                boolean postArgTransformed = false;
//                final Object[] postArgs = newStruct.getArgs();
//                final Object[] postTransformedArgs = new Object[postArgs.length];
//                int index = -1;
//                for (final Object arg : postArgs) {
//                    ++index;
//                    final Object[] trans2 = new Object[] { arg, termAndBindings[1] };
//                    final boolean argTransformed = transformOnce(theTransformationPredicate, trans2, transformArgsBefore, transformArgsAfter);
//                    postArgTransformed |= argTransformed;
//                    postTransformedArgs[index] = trans2[0];
//                }
//                if (postArgTransformed) {
//                    newStruct = new Struct(newStruct, postTransformedArgs);
//                    termAndBindings[0] = newStruct;
//                }
//            }
//        }
//        logger.debug("<  Exit transform with {}, {}: " + anyTransform, termAndBindings[0], termAndBindings[1]);
//        return anyTransform;
//    }
//
//    /**
//     * Transform exactly one term, without recursing on children (in case of Struct), and without
//     * repeated iterations. The result is passed in the mutable :-( termAndBindings elements.
//     *
//     * @param theTransformationPredicate The predicate to apply to transform, the fact or rule should be in the form
//     *            theTransformationPredicate(input, output). Variables are allowed. Rules (with :- ) are allowed too.
//     *            Only the first matching hit will be used - no need to add a cut (!) in every rule.
//     * @param termAndBindings An Object[2] with {term, bindings} to specify argument, will be mutated by the function (in order
//     *            to return 2 values...) only when true is returned.
//     * @return true when a transformation occured, false when nothing was changed.
//     */
//    public boolean transformOnce(final String theTransformationPredicate, final Object[] termAndBindings) {
//        final Object inputTerm = termAndBindings[0];
//        final TermBindings inputBindings = (TermBindings) termAndBindings[1];
//        logger.debug(" > Enter transformOnce with {}, {}", termAndBindings[0], termAndBindings[1]);
//        if (inputTerm instanceof Var) {
//            final Var var = (Var) inputTerm;
//            if (var.isAnonymous()) {
//                // Anonymous var never transformed
//                logger.debug(" <  Exit transformOnce (is anonymous var)");
//                return false;
//            }
//            if (var.bindingWithin(inputBindings).followLinks().isFree()) {
//                // Free variable never transformed
//                logger.debug(" <  Exit transformOnce (is free var)");
//                return false;
//            }
//        }
//
//        // Build the transformation goal in form of "theTransformationPredicate(TransIn, TransOu)"
//        final Var transIn = new Var("TransIn");
//        final Var transOut = new Var("TransOut");
//        final Struct transformationGoal = (Struct) TermApi.normalize(new Struct(theTransformationPredicate, transIn, transOut), /* no library to consider */
//                null);
//        final TermBindings transformationBindings = new TermBindings(transformationGoal);
//
//        // Now bind our transIn var to the original term. Note: we won't have to remember and unbind here since our modified bindings are a
//        // local var!
//        transIn.bindingWithin(transformationBindings).bindTo(inputTerm, inputBindings);
//
//        // A local listener to extract the solution in case it was found
//        final SolutionListener singleMappingResultListener = new SolutionListenerBase() {
//            @Override
//            public Continuation onSolution() {
//                logger.debug("solution: transformationBindings={}", transformationBindings);
//                final TermBindings narrowed = transformationBindings.narrow(transOut, Object.class);
//                termAndBindings[0] = narrowed.getReferrer();
//                termAndBindings[1] = TermBindings.deepCopyWithSameReferrer(narrowed);
//                logger.debug("solution: narrow={} bindings={}", termAndBindings[0], termAndBindings[1]);
//                // Don't need anything more than the first solution. Also this value will be returned
//                // from Solver.solveGoal() and this will indicate we reached one solution!
//                return Continuation.USER_ABORT;
//            }
//        };
//        // Now attempt the transformation
//        final Continuation continuation = getProlog().getSolver().solveGoal(transformationBindings, singleMappingResultListener);
//        // We found a transformation only if the result USER_ABORT from onSolution() was hit.
//        final boolean oneSolutionFound = continuation == Continuation.USER_ABORT;
//        logger.debug(" <  Exit transformOnce with {}, {}: " + oneSolutionFound, termAndBindings[0], termAndBindings[1]);
//        return oneSolutionFound;
//    }

}
