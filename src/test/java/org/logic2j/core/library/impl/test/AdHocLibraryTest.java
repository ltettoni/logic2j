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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.exception.InvalidTermException;

public class AdHocLibraryTest extends PrologTestBase {

    @Test
    public void int_range() {
        this.prolog.getLibraryManager().loadLibrary(new AdHocLibraryForTesting(this.prolog));
        assertEquals(termList("12", "13", "14"), assertNSolutions(3, "int_range(12, X, 14)").binding("X"));
        assertNoSolution("int_range(12, X, 10)");
    }

    @Test(expected = InvalidTermException.class)
    public void exceptionThrownInJavaPredicate() {
        this.prolog.getLibraryManager().loadLibrary(new AdHocLibraryForTesting(this.prolog));
        assertNoSolution("int_range(A, X, 10)");
    }

}
