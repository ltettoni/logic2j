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

import org.logic2j.core.api.library.Primitive;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;

import java.util.Set;

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
    public Object dispatch(String theMethodName, Struct theGoalStruct, UnifyContext currentVars, SolutionListener theListener) {
        final Object result;
        final Object[] args = theGoalStruct.getArgs();
        final int arity = theGoalStruct.getArity();
        if (arity == 3) {
            // Argument methodName is {@link String#intern()}alized so OK to check by reference
            if (theMethodName == "map") {
                result = map(theListener, currentVars, args[0], args[1], args[2]);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 4) {
            // Argument methodName is {@link String#intern()}alized so OK to check by reference
            if (theMethodName == "map") {
                result = map(theListener, currentVars, args[0], args[1], args[2], args[3]);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }


    @Primitive
    public Continuation map(SolutionListener theListener, final UnifyContext currentVars,
                            final Object thePredicate, final Object theInput, final Object theOutput) {
        return map(theListener, currentVars, thePredicate, theInput, theOutput, "one");
    }

    @Primitive
    public Continuation map(SolutionListener theListener, final UnifyContext currentVars,
                            final Object thePredicate, final Object theInput, final Object theOutput, final Object options) {
        if (!(thePredicate instanceof String)) {
            throw new InvalidTermException("Predicate (argument 1) for map/3 must be a String, was " + thePredicate);
        }
        // All options, concatenated, enclosed in commas
        final String optionsCsv = "," + options.toString().trim().toLowerCase().replace(" ", "") + ",";

        final UnifyContext afterUnification = mapGeneric((String) thePredicate, currentVars, theInput, theOutput, optionsCsv);
        if (afterUnification!=null) {
            return notifySolution(theListener, afterUnification);
        }
        return Continuation.USER_ABORT;
    }

    protected UnifyContext mapGeneric(String thePredicate, UnifyContext currentVars, Object theInput, Object theOutput, String optionsCsv) {
        final Object[] returnValues = new Object[1];
        UnifyContext runningMonad = currentVars;

        // Transform children BEFORE the main input
        final boolean isBefore = optionsCsv.contains(",before,");
        if (isBefore) {
            final Object effectiveInput = runningMonad.reify(theInput);
            if (effectiveInput instanceof Struct) {
                final Struct struct = (Struct)effectiveInput;
                final Object[] preArgs = struct.getArgs();
                final Object[] postArgs = new Object[preArgs.length];
                for (int i=0; i<postArgs.length; i++) {
                    postArgs[i] = runningMonad.createVar("beforeOut" + i);
                }

                for (int i=0; i<preArgs.length; i++) {
                    logger.debug("'before' should transform {}", preArgs[i]);
                    runningMonad = mapGeneric(thePredicate, runningMonad, preArgs[i], postArgs[i], optionsCsv);
                    logger.debug("'                     got {}", runningMonad.reify(postArgs[i]));
                }
                final Struct transformedStruct = new Struct(struct.getName(), postArgs);
                int highestVarIndex = Term.NO_INDEX;
                final Set<Var> vars = TermApi.allVars(transformedStruct).keySet();
                for (Var var : vars) {
                    if (var.getIndex() > highestVarIndex) {
                        highestVarIndex = var.getIndex();
                    }
                }
                transformedStruct.index = highestVarIndex+1;
                logger.debug("'before' has transformed  {}", runningMonad.reify(transformedStruct));
                theInput = transformedStruct;
            }
        }

        final boolean isAfter = optionsCsv.contains(",after,");

        // What the core of the mapping will affect depends if we have post-processing or not
        // In case of post-processing we have to use a temporary var
        final Object target;
        if (isAfter) {
            target = runningMonad.createVar("tempOutput");
        } else {
            target = theOutput;
        }
        // Transform the main input
        final UnifyContext afterUnification;
        final boolean isIterative = optionsCsv.contains(",iter,");
        if (isIterative) {
            runningMonad = mapIter((String) thePredicate, runningMonad, theInput, target, returnValues);
        } else {
            runningMonad = mapOne((String) thePredicate, runningMonad, theInput, target, returnValues);
        }


        if (isAfter) {
            final Object effectiveOutput = runningMonad.reify(target);
            if (effectiveOutput instanceof Struct) {
                final Struct struct = (Struct)effectiveOutput;
                final Object[] preArgs = struct.getArgs();
                final Object[] postArgs = new Object[preArgs.length];
                for (int i=0; i<postArgs.length; i++) {
                    postArgs[i] = runningMonad.createVar("afterOut" + i);
                }

                for (int i=0; i<preArgs.length; i++) {
                    logger.info("'after' should transform {}", preArgs[i]);
                    runningMonad = mapGeneric(thePredicate, runningMonad, preArgs[i], postArgs[i], optionsCsv);
                    logger.info("'                    got {}", runningMonad.reify(postArgs[i]));
                }
                final Struct transformedStruct = new Struct(struct.getName(), postArgs);
                int highestVarIndex = Term.NO_INDEX;
                final Set<Var> vars = TermApi.allVars(transformedStruct).keySet();
                for (Var var : vars) {
                    if (var.getIndex() > highestVarIndex) {
                        highestVarIndex = var.getIndex();
                    }
                }
                transformedStruct.index = highestVarIndex+1;
                logger.info("'after' has transformed  {}", runningMonad.reify(transformedStruct));
                runningMonad = runningMonad.unify(transformedStruct, theOutput);
            } else {
                runningMonad = runningMonad.unify(target, theOutput);
            }
        }

        return runningMonad;
    }

    public UnifyContext mapIter(final String mappingPredicate, final UnifyContext currentVars, final Object input, final Object output, final Object[] results) {
        Object runningSource = input;
        UnifyContext runningMonad = currentVars;
        boolean anyTransformation = false;
        for (int iter=0; iter<100; iter++) {
            final Var tmpVar = runningMonad.createVar("MapTmp" + iter);
            logger.info("mapIter created temp var {}", tmpVar);
            final Object[] returnValues = new Object[1];
            runningMonad = mapOne(mappingPredicate, runningMonad, runningSource, tmpVar, returnValues);
            if (runningMonad==null) {
                return null;
            }
            boolean transformed = (Boolean) returnValues[0];
            anyTransformation |= transformed;
            if (!transformed) {
                results[0] = anyTransformation;
                runningMonad = runningMonad.unify(tmpVar, output);
                return runningMonad;
            }
            runningSource = tmpVar;
        }
        return runningMonad;
    }


    public UnifyContext mapOne(final String mappingPredicate, final UnifyContext currentVars, final Object input, final Object output, final Object[] results) {
        final Object effectiveInput = currentVars.reify(input);
        final Object effectiveOutput = currentVars.reify(output);
        if (effectiveInput instanceof Var) {
            // Free var - return untransformed
            final UnifyContext unified = currentVars.unify(effectiveOutput, effectiveInput);
            // Should always unify...
            assert unified != null : "A free var must always unify to anything";
            results[0] = Boolean.FALSE; // Nothing transformed
        } else {
            final Object transformationResult[] = new Object[1];
            final SolutionListener listenerForSubGoal = new SolutionListener() {

                @Override
                public Continuation onSolution(UnifyContext currentVars) {
                    final Object reified = currentVars.reify(effectiveOutput);
                    transformationResult[0] = reified;
                    // logger.info("Subgoal results in effectiveOutput={}", reified);
                    // We only seek for the first solution - not going farther
                    return Continuation.USER_ABORT;
                }
            };
            // Now solve the target sub goal
            final Struct transformationGoal = new Struct(mappingPredicate, effectiveInput, effectiveOutput);
            int highestVarIndex = Term.NO_INDEX;
            final Set<Var> vars = TermApi.allVars(transformationGoal).keySet();
            for (Var var : vars) {
                if (var.getIndex() > highestVarIndex) {
                    highestVarIndex = var.getIndex();
                }
            }
            transformationGoal.index = highestVarIndex+1;
//            transformationGoal = TermApi.normalize(transformationGoal, getProlog().getLibraryManager().wholeContent());

            getProlog().getSolver().solveGoal(transformationGoal, currentVars, listenerForSubGoal);

            if (transformationResult[0] == null) {
                final UnifyContext after = currentVars.unify(effectiveInput, effectiveOutput);
                // No solution was hit
                results[0] = Boolean.FALSE; // Nothing transformed
                return after;
            } else {
                final UnifyContext after = currentVars.unify(transformationResult[0], effectiveOutput);
                results[0] = Boolean.TRUE; // Something transformed
                return after;
            }
        }
        return currentVars;
    }

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

    /**
     * @param theTransformationPredicate
     * @param termAndBindings
     * @param transformArgsBefore
     * @param transformArgsAfter
     * @return true when a transformation occured
     */
    boolean transformOnce(final String theTransformationPredicate, final Object[] termAndBindings, boolean transformArgsBefore, boolean transformArgsAfter) {
        boolean anyTransform = false;
        boolean needTransformAfter = false;
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
        return false;
    }
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
