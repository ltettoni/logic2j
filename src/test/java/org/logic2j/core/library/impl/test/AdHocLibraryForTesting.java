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

import org.logic2j.core.api.PLibrary;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

/**
 * A small ad-hoc implementation of a {@link PLibrary} just for testing.
 */
public class AdHocLibraryForTesting extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdHocLibraryForTesting.class);

    public AdHocLibraryForTesting(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Primitive
    public Continuation int_range(SolutionListener theListener, Bindings theBindings, Object theLowerBound, Object theIterable, Object theUpperBound) {
        final Bindings b1 = theBindings.focus(theLowerBound, Object.class);
        ensureBindingIsNotAFreeVar(b1, "int_range/3");
        final long lower = ((Number) b1.getReferrer()).longValue();

        final Bindings b2 = theBindings.focus(theUpperBound, Object.class);
        ensureBindingIsNotAFreeVar(b2, "int_range/3");
        final long upper = ((Number) b2.getReferrer()).longValue();

        for (long iter = lower; iter <= upper; iter++) {
            final boolean unified = unify(theIterable, theBindings, Long.valueOf(iter), theBindings);
            notifyIfUnified(unified, theListener);
        }
        return Continuation.CONTINUE;
    }
}
