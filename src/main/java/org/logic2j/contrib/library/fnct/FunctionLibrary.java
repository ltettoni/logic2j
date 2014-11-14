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

import org.logic2j.contrib.library.OptionsString;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.RecursionException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;

/**
 * Functional features (mapping, etc).
 * TODO Currently transformations using map/2 or map/3 only find the first solution, not all.
 */
public class FunctionLibrary extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibrary.class);

    /**
     * Maximal number of iterations when transforming all (until nothing changes). This prevents infinit
     * recursion in case of a cycle in the transformation mappings.
     */
    private static final int MAX_TRANSFORM_ITERATIONS = 100;

    /**
     * Transform only the "flat" term, ie not its children in case of a Struct, and only once.
     */
    public static final String OPTION_ONE = "one";

    /**
     * Transform only the "flat" term, ie not its children in case of a Struct, but possibly several times
     * until no more transformation happens.
     */
    public static final String OPTION_ITER = "iter";

    /**
     * In case of a Struct, request to transform all children before transforming the struct.
     * This may be used in conjunction with OPTION_AFTER.
     */
    public static final String OPTION_BEFORE = "before";

    /**
     * In case of a Struct, request to re-transform all children after the main struct was transformed.
     * This may be used in conjunction with OPTION_BEFORE.
     */
    public static final String OPTION_AFTER = "after";

    public FunctionLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }


    @Override
    public Object dispatch(String methodName, Struct goal, UnifyContext currentVars, SolutionListener listener) {
        final Object result;
        final Object[] args = goal.getArgs();
        final int arity = goal.getArity();
        if (arity == 3) {
            // Argument methodName is {@link String#intern()}alized so OK to check by reference
            if (methodName == "map") {
                result = map(listener, currentVars, args[0], args[1], args[2]);
            } else if (methodName == "mapOne") {
                result = mapOneProto(listener, currentVars, args[0], args[1], args[2]);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 4) {
            // Argument methodName is {@link String#intern()}alized so OK to check by reference
            if (methodName == "map") {
                result = map(listener, currentVars, args[0], args[1], args[2], args[3]);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }

    /**
     * Basic transformation, will only transform the flat term, see OPTION_ONE.
     *
     * @param listener
     * @param currentVars
     * @param mappingPredicate
     * @param inputTerm
     * @param outputTerm
     * @return
     */
    @Predicate
    public Integer map(SolutionListener listener, final UnifyContext currentVars,
                       final Object mappingPredicate, final Object inputTerm, final Object outputTerm) {
        return map(listener, currentVars, mappingPredicate, inputTerm, outputTerm, OPTION_ONE);
    }

    /**
     * Map with (comma-separated) options.
     *
     * @param listener
     * @param currentVars
     * @param mappingPredicate
     * @param inputTerm
     * @param outputTerm
     * @param theOptions
     * @return
     */
    @Predicate
    public Integer map(SolutionListener listener, final UnifyContext currentVars,
                       final Object mappingPredicate,
                       final Object inputTerm, final Object outputTerm, final Object theOptions) {
        if (!(mappingPredicate instanceof String)) {
            throw new InvalidTermException("Predicate (argument 1) for map/3 must be a String, was " + mappingPredicate);
        }
        // All options, concatenated, enclosed in commas
        final OptionsString options = new OptionsString(currentVars, theOptions);

        final TransformationInfo callingInfo = new TransformationInfo(options);
        final UnifyContext afterUnification = mapCompletely((String) mappingPredicate, currentVars, inputTerm, outputTerm, callingInfo);
        if (afterUnification != null) {
            return notifySolution(listener, afterUnification);
        }
        return Continuation.USER_ABORT;
    }

    /**
     * Map with options-driven behaviour:
     * may map children of Struct before mapping the structure itself
     * then
     * may map the main structure either once, or repeating
     * then
     * may map the children of the transformed structure
     *
     * @param mappingPredicate
     * @param currentVars
     * @param inputTerm
     * @param outputTerm
     * @return
     */
    protected UnifyContext mapCompletely(String mappingPredicate, UnifyContext currentVars,
                                         Object inputTerm, Object outputTerm,
                                         final TransformationInfo returnInfo) {
        returnInfo.recursionCounter++;
        UnifyContext runningMonad = currentVars;

        final TransformationInfo callingInfo = returnInfo.copy();

        // Transform children BEFORE the main input

        if (returnInfo.isBefore) {
            final Object effectiveInput = runningMonad.reify(inputTerm);
            if (effectiveInput instanceof Struct) {
                final Struct struct = (Struct) effectiveInput;
                final Object[] preArgs = struct.getArgs();
                final Object[] postArgs = new Object[preArgs.length];
                for (int i = 0; i < postArgs.length; i++) {
                    postArgs[i] = runningMonad.createVar("_map_before_" + i);
                }

                for (int i = 0; i < preArgs.length; i++) {
                    logger.debug("'before' should transform {}", preArgs[i]);
                    runningMonad = mapCompletely(mappingPredicate, runningMonad, preArgs[i], postArgs[i], callingInfo);
                    logger.debug("'                     got {}", runningMonad.reify(postArgs[i]));
                    returnInfo.raiseHasTransformedFrom(callingInfo);
                }
                final Struct transformedStruct = new Struct(struct.getName(), postArgs);
                int highestVarIndex = highestVarIndex(transformedStruct);
                transformedStruct.index = highestVarIndex + 1;
                logger.debug("'before' has transformed  {}", runningMonad.reify(transformedStruct));
                inputTerm = transformedStruct;
            }
        }


        // What the core of the mapping will affect depends if we have post-processing or not
        // In case of post-processing we have to use a temporary var
        final Object target = returnInfo.isAfter ? runningMonad.createVar("_map_tgt") : outputTerm;

        runningMonad = mapOneLevel(mappingPredicate, runningMonad, inputTerm, target, callingInfo);
        returnInfo.raiseHasTransformedFrom(callingInfo);

        if (returnInfo.isAfter) {
            final Object effectiveOutput = runningMonad.reify(target);
            if (effectiveOutput instanceof Struct) {
                final Struct struct = (Struct) effectiveOutput;
                final Object[] preArgs = struct.getArgs();
                final Object[] postArgs = new Object[preArgs.length];
                for (int i = 0; i < postArgs.length; i++) {
                    postArgs[i] = runningMonad.createVar("_map_after_" + i);
                }

                for (int i = 0; i < preArgs.length; i++) {
                    logger.debug("'after' should transform {}", preArgs[i]);
                    runningMonad = mapCompletely(mappingPredicate, runningMonad, preArgs[i], postArgs[i], callingInfo);
                    logger.debug("'                    got {}", runningMonad.reify(postArgs[i]));
                    returnInfo.raiseHasTransformedFrom(callingInfo);
                }
                final Struct transformedStruct = new Struct(struct.getName(), postArgs);
                int highestVarIndex = highestVarIndex(transformedStruct);
                transformedStruct.index = highestVarIndex + 1;
                logger.debug("'after' has transformed  {}", runningMonad.reify(transformedStruct));
                runningMonad = runningMonad.unify(transformedStruct, outputTerm);
            } else {
                runningMonad = runningMonad.unify(target, outputTerm);
            }
        }
        return runningMonad;
    }

    private int highestVarIndex(Struct transformedStruct) {
        int highestVarIndex = Term.NO_INDEX;
        final Var<?>[] distinctVars = TermApi.distinctVars(transformedStruct);
        for (Var<?> var : distinctVars) {
            if (var.getIndex() > highestVarIndex) {
                highestVarIndex = var.getIndex();
            }
        }
        return highestVarIndex;
    }

    private UnifyContext mapOneLevel(String mappingPredicate, UnifyContext currentVars,
                                     Object inputTerm, Object outputTerm,
                                     final TransformationInfo returnInfo) {
        returnInfo.recursionCounter++;
        UnifyContext runningMonad = currentVars;
        // Transform the main inputTerm
        if (returnInfo.isIterative) {

            final Var<?> tmpVar = runningMonad.createVar("_map_one_inter" + returnInfo.recursionCounter);
            logger.debug("mapRepeating created temp var {}", tmpVar);

            final TransformationInfo callingInfo = returnInfo.copy();
            runningMonad = mapOne(mappingPredicate, runningMonad, inputTerm, tmpVar, callingInfo);
            if (callingInfo.hasTransformed) {
                runningMonad = mapCompletely(mappingPredicate, runningMonad, tmpVar, outputTerm, returnInfo);
            } else {
                // Nothing transformed the second time, it would have been nicer if we had done the first transformation
                // towards output Term instead of a temporary var (but at the time, we did not know that!)
                // So we unify the temporary var to the output term
                runningMonad = runningMonad.unify(tmpVar, outputTerm);
            }
        } else {
            runningMonad = mapOne((String) mappingPredicate, runningMonad, inputTerm, outputTerm, returnInfo);
        }
        return runningMonad;
    }


    /**
     * Transform only the root predicate, does not impact any of its children.
     *
     * @param mappingPredicate
     * @param currentVars
     * @param inputTerm
     * @param outputTerm
     * @param returnInfo
     * @return
     */
    public UnifyContext mapOne(final String mappingPredicate, final UnifyContext currentVars,
                               final Object inputTerm, final Object outputTerm,
                               final TransformationInfo returnInfo) {
        returnInfo.recursionCounter++;
        checkRecursionLimit(returnInfo, mappingPredicate);

        UnifyContext runningMonad = currentVars;
        final Object effectiveInput = runningMonad.reify(inputTerm);
        final Object effectiveOutput = runningMonad.reify(outputTerm);
        if (effectiveInput instanceof Var) {
            // Free var - return untransformed
            runningMonad = runningMonad.unify(inputTerm, outputTerm);
            // Should always unify...
            assert runningMonad != null : "A free var must always unify to anything";
            returnInfo.hasTransformed = false; // Nothing was transformed
        } else {
            // Pattern of using an immutable array to store the mutable result of a callback...
            final Object transformationResult[] = new Object[1];
            final SolutionListener listenerForSubGoal = new SolutionListenerBase() {

                @Override
                public Integer onSolution(UnifyContext currentVars) {
                    final Object result = currentVars.reify(effectiveOutput);
                    transformationResult[0] = result;
                    // We only seek for the first solution - not proceeding further
                    return Continuation.USER_ABORT;
                }
            };
            // Create the transformation goal
            final Struct transformationGoal = new Struct(mappingPredicate, effectiveInput, effectiveOutput);
            int highestVarIndex = highestVarIndex(transformationGoal);
            transformationGoal.index = highestVarIndex + 1;
            // Now solve the target sub goal
            getProlog().getSolver().solveGoal(transformationGoal, runningMonad, listenerForSubGoal);

            final Object result = transformationResult[0];
            if (result == null) {
                // There was no result - we will return the outputTerm as the inputTerm unchanged
                runningMonad = runningMonad.unify(effectiveInput, effectiveOutput); // Unsure if we should not unify with inputTerm and outputTerm
                // No solution was hit
                returnInfo.hasTransformed = false; // Nothing was transformed
            } else {
                // We got a result
                runningMonad = runningMonad.unify(result, effectiveOutput); // Unsure if we should not unify with outputTerm
                returnInfo.hasTransformed = true; // Something was transformed
            }
        }
        return runningMonad;
    }

    private void checkRecursionLimit(TransformationInfo info, String mappingPredicate) {
        if (info.recursionCounter > MAX_TRANSFORM_ITERATIONS) {
            throw new RecursionException("Too many recursive calls while attempting to map/3 with predicate \"" + mappingPredicate + '"');
        }
    }


    // ---------------------------------------------------------------------------
    // WORK IN PROGRESS - toward multi-solution transformation
    // ---------------------------------------------------------------------------


    /**
     * Transform only the root predicate, does not impact any of its children.
     *
     * @param mappingPredicate
     * @param currentVars
     * @param inputTerm
     * @param outputTerm
     * @return
     */
    @Predicate(name="mapOne")
    public Integer mapOneProto(final SolutionListener listener, final UnifyContext currentVars,
                               final Object mappingPredicate, final Object inputTerm, final Object outputTerm) {
        UnifyContext runningMonad = currentVars;
        final Object effectiveInput = runningMonad.reify(inputTerm);
        final Object effectiveOutput = runningMonad.reify(outputTerm);
        if (effectiveInput instanceof Var) {
            // Free var - return untransformed
            runningMonad = runningMonad.unify(inputTerm, outputTerm);
            // Should always unify...
            assert runningMonad != null : "A free var must always unify to anything";
        } else {
            // Pattern of using an immutable array to store the mutable result of a callback...
            final CountingSolutionListener listenerForSubGoal = new CountingSolutionListener() {

                @Override
                public Integer onSolution(UnifyContext currentVars) {
                    super.onSolution(currentVars);
                    // final Object result = currentVars.reify(effectiveOutput);
                    final Integer continuation = listener.onSolution(currentVars);
                    return continuation;
                }
            };
            // Create the transformation goal
            final Struct transformationGoal = new Struct((String)mappingPredicate, effectiveInput, effectiveOutput);
            int highestVarIndex = highestVarIndex(transformationGoal);
            transformationGoal.index = highestVarIndex + 1;
            // Now solve the target sub goal
            getProlog().getSolver().solveGoal(transformationGoal, runningMonad, listenerForSubGoal);
            if (listenerForSubGoal.hadNoSolution()) {
                // No transformation found - relay untransformed value
                final UnifyContext newVars = runningMonad.unify(effectiveInput, effectiveOutput);
                if (newVars != currentVars) {
                    final Integer continuation = listener.onSolution(newVars);
                    return continuation;
                }
            }
        }
        return Continuation.CONTINUE;
    }


    // ---------------------------------------------------------------------------
    // Support class to provide and extract data across these many recursive calls
    // ---------------------------------------------------------------------------

    private static class TransformationInfo {
        private boolean hasTransformed;

        private int recursionCounter;

        boolean isBefore;

        boolean isIterative;

        boolean isAfter;

        private TransformationInfo() {
            recursionCounter = 0;
            hasTransformed = false;
        }

        /**
         * Set the flags based on the options.
         *
         * @param options
         */
        private TransformationInfo(OptionsString options) {
            this();
            isBefore = options.hasOption(OPTION_BEFORE);
            isIterative = options.hasOption(OPTION_ITER);
            isAfter = options.hasOption(OPTION_AFTER);
        }

        /**
         * Clone this except for the hasTransformed that is reset to false.
         *
         * @return A copy.
         */
        private TransformationInfo copy() {
            TransformationInfo copy = new TransformationInfo();
            copy.recursionCounter = this.recursionCounter;
            copy.isBefore = this.isBefore;
            copy.isIterative = this.isIterative;
            copy.isAfter = this.isAfter;
            return copy;
        }

        public void raiseHasTransformedFrom(TransformationInfo other) {
            if (other.hasTransformed) {
                this.hasTransformed = true;
            }
        }


    }

}
