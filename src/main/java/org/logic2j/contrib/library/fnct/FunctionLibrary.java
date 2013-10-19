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
            // Free variable, no transformatino
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
                    final Var xx = new Var("XX");
                    final Var zz = new Var("ZZ");
                    final Struct mappingGoal = (Struct) TermApi.normalize(new Struct((String) thePredicate, xx, zz), null);
                    final Bindings mappingBindings = new Bindings(mappingGoal);

                    final boolean unify = getProlog().getUnifier().unify(arg, theInputBindings, xx, mappingBindings);
                    transformedArgs[indx] = arg;
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

            final Var xx = new Var("XX");
            final Var zz = new Var("ZZ");
            final Struct mappingGoal = (Struct) TermApi.normalize(new Struct((String) thePredicate, xx, zz), null);
            final Bindings mappingBindings = new Bindings(mappingGoal);
            final Object[] sol = new Object[] { theInputBindings.getReferrer() };
            final boolean unify = getProlog().getUnifier().unify(theInputBindings.getReferrer(), theInputBindings, xx, mappingBindings);
            if (unify) {
                try {
                    final SolutionListener singleMappingResultListener = new SolutionListener() {
                        @Override
                        public Continuation onSolution() {
                            final Object substitute = TermApi.substitute(zz, mappingBindings);
                            sol[0] = substitute;
                            logger.debug("solution: substituted={}", substitute);
                            return Continuation.USER_ABORT;
                        }
                    };
                    getProlog().getSolver().solveGoal(mappingBindings, singleMappingResultListener);
                } finally {
                    deunify();
                }
            }
            final boolean unified = unify(sol[0], theInputBindings, theOutputBindings.getReferrer(), theOutputBindings);
            notifyIfUnified(unified, theListener);
        }
    }

}
