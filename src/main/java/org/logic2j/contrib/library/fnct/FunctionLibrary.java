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
import org.logic2j.core.api.model.exception.RecursionException;
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

    public static final String OPTION_ONE = "one";
    public static final String OPTION_ITER = "iter";
    public static final String OPTION_BEFORE = "before";
    public static final String OPTION_AFTER = "after";

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

    /**
     * Check an option within a set of options - conventionally encoded as ",opt1,opt2,...optn,"
     * @param optionsCsv
     * @param option
     * @return True if option in found in optionsCsv
     */
    private boolean matchOption(String optionsCsv, String option) {
        return optionsCsv.contains("," + option + ",");
    }

    @Primitive
    public Continuation map(SolutionListener theListener, final UnifyContext currentVars,
                            final Object thePredicate, final Object theInput, final Object theOutput) {
        return map(theListener, currentVars, thePredicate, theInput, theOutput, OPTION_ONE);
    }

    @Primitive
    public Continuation map(SolutionListener theListener, final UnifyContext currentVars,
                            final Object thePredicate, final Object theInput, final Object theOutput, final Object options) {
        if (!(thePredicate instanceof String)) {
            throw new InvalidTermException("Predicate (argument 1) for map/3 must be a String, was " + thePredicate);
        }
        // All options, concatenated, enclosed in commas
        final String optionsCsv = "," + options.toString().trim().toLowerCase().replace(" ", "") + ",";

        final UnifyContext afterUnification = mapGeneric((String) thePredicate, currentVars, theInput, theOutput, optionsCsv, MAX_TRANFORM_ITERATIONS);
        if (afterUnification!=null) {
            return notifySolution(theListener, afterUnification);
        }
        return Continuation.USER_ABORT;
    }

    /**
     * Map with options-driven behaviour:
     *   may map children of Struct before mapping the structure itself
     *   then
     *   may map the main structure either once, or repeating
     *   then
     *   may map the children of the transformed structure
     *
     * @param thePredicate
     * @param currentVars
     * @param theInput
     * @param theOutput
     * @param optionsCsv
     * @param recursionLimit
     * @return
     */
    protected UnifyContext mapGeneric(String thePredicate, UnifyContext currentVars, Object theInput, Object theOutput,
                                      String optionsCsv, int recursionLimit) {
        UnifyContext runningMonad = currentVars;

        // Transform children BEFORE the main input
        final boolean isBefore = matchOption(optionsCsv, OPTION_BEFORE);
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
                    runningMonad = mapGeneric(thePredicate, runningMonad, preArgs[i], postArgs[i], optionsCsv, --recursionLimit);
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

        final boolean isAfter = matchOption(optionsCsv, OPTION_AFTER);

        // What the core of the mapping will affect depends if we have post-processing or not
        // In case of post-processing we have to use a temporary var
        final Object target;
        if (isAfter) {
            target = runningMonad.createVar("tempOutput");
        } else {
            target = theOutput;
        }

        runningMonad = mapFirstLevel(thePredicate, runningMonad, theInput, target, optionsCsv, --recursionLimit);

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
                    logger.debug("'after' should transform {}", preArgs[i]);
                    runningMonad = mapGeneric(thePredicate, runningMonad, preArgs[i], postArgs[i], optionsCsv, --recursionLimit);
                    logger.debug("'                    got {}", runningMonad.reify(postArgs[i]));
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
                logger.debug("'after' has transformed  {}", runningMonad.reify(transformedStruct));
                runningMonad = runningMonad.unify(transformedStruct, theOutput);
            } else {
                runningMonad = runningMonad.unify(target, theOutput);
            }
        }

        return runningMonad;
    }

    private UnifyContext mapFirstLevel(String thePredicate, UnifyContext runningMonad, Object theInput, Object theOutput, String optionsCsv, int recursionLimit) {
        UnifyContext result;
        // Transform the main input
        final boolean isIterative = matchOption(optionsCsv, OPTION_ITER);
        if (isIterative) {
            result = mapRepeating((String) thePredicate, runningMonad, theInput, theOutput, --recursionLimit);
        } else {
            final Object[] returnValues = new Object[1];
            result = mapOne((String) thePredicate, runningMonad, theInput, theOutput, returnValues, --recursionLimit);
        }
        return result;
    }

    /**
     * Iterative version of mapOne: transform only the root predicate (does not impact any of its children), but
     * repeat transforming until nothing changes.
     * @param mappingPredicate
     * @param currentVars
     * @param input
     * @param output
     * @param recursionLimit
     * @return
     */
    public UnifyContext mapRepeating(final String mappingPredicate, final UnifyContext currentVars, final Object input, final Object output,
                                     int recursionLimit) {
        Object runningInput = input;
        UnifyContext runningMonad = currentVars;
        boolean anyTransformation = false;
        // Repeat until no more transformation (return within loop) or recursion limit hit
        final Object[] returnValues = new Object[1];
        for (int iter=0; ; iter++) {
            final Var tmpVar = runningMonad.createVar("MapIter" + iter);
            logger.debug("mapRepeating created temp var {}", tmpVar);
            runningMonad = mapOne(mappingPredicate, runningMonad, runningInput, tmpVar, returnValues, --recursionLimit);
            if (runningMonad==null) {
                // Could not transform (unification failed)
                return null;
            }
            final boolean lastMapOneDidTransform = (Boolean) returnValues[0];
            anyTransformation |= lastMapOneDidTransform;
            if (!lastMapOneDidTransform) {
                runningMonad = runningMonad.unify(tmpVar, output);
                return runningMonad;
            }
            runningInput = tmpVar;
        }
    }


    /**
     * Transform only the root predicate, does not impact any of its children.
     * @param mappingPredicate
     * @param currentVars
     * @param input
     * @param output
     * @param results
     * @param recursionLimit
     * @return
     */
    public UnifyContext mapOne(final String mappingPredicate, final UnifyContext currentVars, final Object input, final Object output,
                               final Object[] results, int recursionLimit) {
        checkRecursionLimit(recursionLimit, mappingPredicate);
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

    private void checkRecursionLimit(int recursionLimit, String mappingPredicate) {
        if (recursionLimit<0) {
            throw new RecursionException("Too many recursive calls while attempting to map/3 with predicate \"" + mappingPredicate + '"');
        }
    }

}
