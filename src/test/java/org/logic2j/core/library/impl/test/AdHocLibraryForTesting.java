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
package org.logic2j.core.library.impl.test;

import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.PLibrary;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;
import org.logic2j.core.model.symbol.TLong;
import org.logic2j.core.model.symbol.TNumber;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.GoalFrame;
import org.logic2j.core.solver.listener.Continuation;
import org.logic2j.core.solver.listener.SolutionListener;

/**
 * A small ad-hoc implementation of a {@link PLibrary} just for testing.
 */
public class AdHocLibraryForTesting extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdHocLibraryForTesting.class);

    public AdHocLibraryForTesting(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Primitive
    public Continuation int_range(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theLowerBound, Term theIterable, Term theUpperBound) {
        final Bindings b1 = theBindings.focus(theLowerBound, TNumber.class);
        assertValidBindings(b1, "int_range/3");
        final long lower = ((TNumber) b1.getReferrer()).longValue();

        final Bindings b2 = theBindings.focus(theUpperBound, TNumber.class);
        assertValidBindings(b2, "int_range/3");
        final long upper = ((TNumber) b2.getReferrer()).longValue();

        for (long iter = lower; iter <= upper; iter++) {
            final TLong iterTerm = new TLong(iter);
            final boolean unified = unify(theIterable, theBindings, iterTerm, theBindings, theGoalFrame);
            notifyIfUnified(unified, theGoalFrame, theListener);
        }
        return Continuation.CONTINUE;
    }
}
