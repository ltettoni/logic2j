/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.core.library.impl;

import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.solver.listener.UnifyContextIterator;
import org.logic2j.engine.unify.UnifyContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A small ad-hoc implementation of a {@link org.logic2j.core.api.library.PLibrary} just for testing.
 */
public class AdHocLibraryForTesting extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdHocLibraryForTesting.class);

    public AdHocLibraryForTesting(PrologImplementation theProlog) {
        super(theProlog);
    }

    /**
     * Classic production of several solutions.
     *
     * @param theListener
     * @param currentVars
     * @param theLowerBound
     * @param theIterable
     * @param theUpperBound
     * @return
     */
    @Predicate
    public Integer int_range_classic(SolutionListener theListener, UnifyContext currentVars, Object theLowerBound, Object theIterable, Object theUpperBound) {
        final Object lowerBound = currentVars.reify(theLowerBound);
        final Object upperBound = currentVars.reify(theUpperBound);

        ensureBindingIsNotAFreeVar(lowerBound, "int_range_classic/3", 0);
        ensureBindingIsNotAFreeVar(upperBound, "int_range_classic/3", 2);

        final int lower = ((Number) lowerBound).intValue();
        final int upper = ((Number) upperBound).intValue();

        for (int iter = lower; iter < upper; iter++) {
            logger.info("{} is going to unify an notify one solution: {}", this, iter);
            final Integer continuation = unifyAndNotify(currentVars, theIterable, iter);
            if (continuation != Continuation.CONTINUE) {
                return continuation;
            }
        }
        return Continuation.CONTINUE;
    }


    /**
     * Special production of several solutions in one go.
     *
     * @param theListener
     * @param currentVars
     * @param theMinBound
     * @param theIterable
     * @param theMaxBound Upper bound + 1 (ie theIterable will go up to theMaxBound-1)
     * @return
     */
    @Predicate
    public Integer int_range_multi(SolutionListener theListener, final UnifyContext currentVars, Object theMinBound, final Object theIterable, Object theMaxBound) {
        final Object minBound = currentVars.reify(theMinBound);
        final Object iterating = currentVars.reify(theIterable);
        final Object maxBound = currentVars.reify(theMaxBound);

        ensureBindingIsNotAFreeVar(minBound, "int_range_classic/3", 0);
        ensureBindingIsNotAFreeVar(maxBound, "int_range_classic/3", 2);

        final int min = ((Number) minBound).intValue();
        final int max = ((Number) maxBound).intValue();

        if (iterating instanceof Var) {
            final List<Integer> values = new ArrayList<Integer>();
            for (int val = min; val < max; val++) {
                values.add(val);
            }

            final UnifyContextIterator multi = new UnifyContextIterator(currentVars, (Var)theIterable, values);
            logger.info("{} is going to notify multi solutions: {}", this, multi);
            return currentVars.getSolutionListener().onSolutions(multi);
        } else {
            // Check
            final int iter = ((Number) iterating).intValue();
            if (min <= iter && iter < max) {
                return notifySolution(currentVars);
            } else {
                return Continuation.CONTINUE;
            }

        }
    }
}
