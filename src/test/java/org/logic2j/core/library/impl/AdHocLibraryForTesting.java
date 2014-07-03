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
package org.logic2j.core.library.impl;

import org.logic2j.core.api.library.Primitive;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.MultiResult;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
    @Primitive
    public Continuation int_range_classic(SolutionListener theListener, UnifyContext currentVars, Object theLowerBound, Object theIterable, Object theUpperBound) {
        final Object lowerBound = currentVars.reify(theLowerBound);
        final Object upperBound = currentVars.reify(theUpperBound);

        ensureBindingIsNotAFreeVar(lowerBound, "int_range_classic/3");
        ensureBindingIsNotAFreeVar(upperBound, "int_range_classic/3");

        final long lower = ((Number) lowerBound).longValue();
        final long upper = ((Number) upperBound).longValue();

        for (long iter = lower; iter < upper; iter++) {
            final Continuation continuation = unifyInternal(theListener, currentVars, theIterable, Long.valueOf(iter));
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
    @Primitive
    public Continuation int_range_multi(SolutionListener theListener, final UnifyContext currentVars, Object theMinBound, final Object theIterable, Object theMaxBound) {
        final Object minBound = currentVars.reify(theMinBound);
        final Object maxBound = currentVars.reify(theMaxBound);

        ensureBindingIsNotAFreeVar(minBound, "int_range_classic/3");
        ensureBindingIsNotAFreeVar(maxBound, "int_range_classic/3");

        final long min = ((Number) minBound).longValue();
        final long max = ((Number) maxBound).longValue();

        final MultiResult multi = new MultiResult() {
            long currentValue = min;

            @Override
            public Iterator<UnifyContext> iterator() {
                return new Iterator<UnifyContext>() {

                    @Override
                    public boolean hasNext() {
                        return currentValue < max;
                    }

                    @Override
                    public UnifyContext next() {
                        final Long next = currentValue++;
                        final UnifyContext after = currentVars.unify(theIterable, next);
                        return after;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Feature not yet implemented");
                    }
                };
            }
        };

        return theListener.onSolutions(multi);
    }
}
