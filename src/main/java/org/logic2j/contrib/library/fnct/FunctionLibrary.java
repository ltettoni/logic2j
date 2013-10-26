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
        final Bindings theOutputBindings = theBindings.narrow(theOutput, Object.class);

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
        logger.debug("Enter traverseAndMap, from {} to {}", theInputBindings, theOutputBindings);
        if (theInputBindings == null) {
            // Anonymous var specified
            notifySolution(theListener);
            return;
        }
        if (theInputBindings.isFreeReferrer()) {
            // Free variable, no transformation
            final boolean unify = getProlog().getUnifier().unify(theInputBindings.getReferrer(), theInputBindings, theOutputBindings.getReferrer(), theOutputBindings);
            notifyIfUnified(unify, theListener);
            return;
        }
        // Depth first traversal, traverse children first
        if (theInputBindings.getReferrer() instanceof Struct) {
            final Struct struct = (Struct) (theInputBindings.getReferrer());
            logger.debug("Found a struct {}", struct);
            final Object[] args = struct.getArgs();
            if (args != null) {

                final Object[] transformedArgs = new Object[args.length];
                int index = -1;
                for (Object arg : args) {
                    index++;
                    final int indx = index;
                    logger.debug("Going to attempt to transform {}", arg);

                    final Object[] termAndBindings = new Object[] { arg, theInputBindings };
                    transformOnce(thePredicate, termAndBindings, 0, 0);
                    // If struct we should recurse here!

                    transformedArgs[indx] = termAndBindings[0];

                    /*
                    // Won't transform free vars
                    if (arg instanceof Var && theInputBindings.getBinding(((Var) arg).getIndex()).followLinks().isFree()) {
                        continue;
                    }
                    final Var xx = new Var("XX");
                    final Var zz = new Var("ZZ");
                    final Struct mappingGoal = (Struct) TermApi.normalize(new Struct((String) thePredicate, xx, zz), null);
                    final Bindings mappingBindings = new Bindings(mappingGoal);

                    final boolean unify = getProlog().getUnifier().unify(arg, theInputBindings, xx, mappingBindings);
                    if (unify) {
                        try {
                            final SolutionListener singleMappingResultListener = new SolutionListener() {
                                @Override
                                public Continuation onSolution() {
                                    final Object substitute = TermApi.substitute(zz, mappingBindings);
                                    logger.debug("solution: substituted={}", substitute);
                                    transformedArgs[indx] = substitute;
                                    return Continuation.USER_ABORT;
                                }
                            };
                            traverseAndMap(thePredicate, mappingBindings.narrow(xx, Object.class), mappingBindings.narrow(zz, Object.class), singleMappingResultListener);
                        } finally {
                            deunify();
                        }
                    }
                    */
                }
                final Struct withTransformedArguments = new Struct(struct.getName(), transformedArgs);

                // Now after having done the arguments, we should transform the structure itself!
                logger.debug("Going to attempt to transform {}", withTransformedArguments);
                final Var xx = new Var("XX");
                final Var zz = new Var("ZZ");
                final Struct mappingGoal = (Struct) TermApi.normalize(new Struct((String) thePredicate, xx, zz), null);
                final Bindings mappingBindings = new Bindings(mappingGoal);
                final boolean unify = getProlog().getUnifier().unify(withTransformedArguments, theInputBindings, xx, mappingBindings);
                final Object transformedStructHolder[] = new Struct[1];
                transformedStructHolder[0] = withTransformedArguments;
                if (unify) {
                    try {
                        final SolutionListener singleMappingResultListener = new SolutionListener() {
                            @Override
                            public Continuation onSolution() {
                                final Object substitute = TermApi.substitute(zz, mappingBindings);
                                logger.debug("solution: substituted={}", substitute);
                                transformedStructHolder[0] = substitute;
                                return Continuation.USER_ABORT;
                            }
                        };
                        getProlog().getSolver().solveGoal(mappingBindings, singleMappingResultListener);
                        // traverseAndMap(thePredicate, mappingBindings.narrow(xx, Object.class), mappingBindings.narrow(zz, Object.class),
                        // singleMappingResultListener);
                    } finally {
                        deunify();
                    }
                }
                Object transformedStruct = transformedStructHolder[0];

                final boolean unified = unify(transformedStruct, theInputBindings, theOutputBindings.getReferrer(), theOutputBindings);
                notifyIfUnified(unified, theListener);
            } else {
                // Just an atom to transform
                final boolean unified = unify(theInputBindings.getReferrer(), theInputBindings, theOutputBindings.getReferrer(), theOutputBindings);
                notifyIfUnified(unified, theListener);
            }
        } else {
            // Not a Struct
            final Object[] termAndBindings = new Object[] { theInputBindings.getReferrer(), theInputBindings };
            transformOnce(thePredicate, termAndBindings, 0, 0);

            final boolean unified = unify(termAndBindings[0], (Bindings) termAndBindings[1], theOutputBindings.getReferrer(), theOutputBindings);
            notifyIfUnified(unified, theListener);
        }
    }

    /**
     * @param termAndBindings
     */
    public boolean transformAll(final String transformationPredicate, final Object[] termAndBindings) {
        boolean anyTransformed = false;
        boolean transformed;
        int iterationLimiter = 10;
        do {
            transformed = transformOnce(transformationPredicate, termAndBindings, 0, 0);
            anyTransformed |= transformed;
            iterationLimiter--;
        } while (transformed && iterationLimiter > 0);
        return anyTransformed;
    }

    public boolean transformOnce(final String transformationPredicate, final Object[] termAndBindings, int childrenBefore, int childrenAfter) {
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
                    final boolean argTransformed = transformOnce(transformationPredicate, trans2, childrenBefore, childrenAfter);
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
            final boolean structTransformed = transformOnce(transformationPredicate, trans3);
            anyTransform |= structTransformed;

            // Assemble result; note the side-effect on arg :-(
            termAndBindings[0] = trans3[0];
            termAndBindings[1] = trans3[1];
        } else {
            anyTransform = transformOnce(transformationPredicate, termAndBindings);
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
                    final boolean argTransformed = transformOnce(transformationPredicate, trans2, childrenBefore, childrenAfter);
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
     * @param termAndBindings
     * @param deep TODO
     */
    public boolean transformOnce(final String transformationPredicate, final Object[] termAndBindings) {
        final Object inputTerm = termAndBindings[0];
        if (inputTerm instanceof Var) {
            Var var = (Var) inputTerm;
            if (var.isAnonymous()) {
                // Anonymous var not transformed
                return false;
            }
            final Bindings inputBindings = (Bindings) termAndBindings[1];
            if (var.bindingWithin(inputBindings).followLinks().isFree()) {
                // Free variable, no transformation
                return false;
            }
        }

        final Var transIn = new Var("TransIn");
        final Var transOut = new Var("TransOut");
        final Struct transformationGoal = (Struct) TermApi.normalize(new Struct((String) transformationPredicate, transIn, transOut), null);
        final Bindings transformationBindings = new Bindings(transformationGoal);

        // Now bind our transIn var to the original term. Note: we won't have to unbind here since our modified bindings are a local
        // var!
        transIn.bindingWithin(transformationBindings).bindTo(termAndBindings[0], (Bindings) termAndBindings[1]);

        // Now solving
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
        final Continuation continuation = getProlog().getSolver().solveGoal(transformationBindings, singleMappingResultListener);
        final boolean oneSolutionFound = continuation == Continuation.USER_ABORT;
        return oneSolutionFound;
    }

}
