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
package org.logic2j.core.functional;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.benchmark.BenchmarkTest;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.io.IOLibrary;
import org.logic2j.core.solver.DefaultSolver;

/**
 * Run higher-level tests such as whole programs.
 * When this works, there is much chance that the {@link PrologImplementation} is satisfactory for solving real-life problems, although full
 * test coverage is far from guaranteed.
 * For performance testing see {@link BenchmarkTest}.
 */
public class HigherLevelTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HigherLevelTest.class);

    /**
     * Reasonably-sided Towers of Hanoi. See also {@link BenchmarkTest#hanoi()}
     */
    @Test
    public void hanoi() {
        loadTheoryFromTestResourcesDir("hanoi.pl");
        assertOneSolution("move(5, left, right, center)"); // Watch out 7 is the limit with Java's ridiculous default stack size
        logger.info("Number of solutions processed: {}", ((DefaultSolver) prolog.getSolver()).internalCounter);
    }

    /**
     * This simple Prolog program checks or generates change adding up to a dollar consisting of half-dollars, quarters, dimes, nickels, and
     * pennies.
     */
    @Test
    public void changeForOneDollar() {
        final IOLibrary library = new IOLibrary(prolog);
        loadLibrary(library);
        loadTheoryFromTestResourcesDir("dollar.pl");
        assertNSolutions(292, "change([H,Q,D,N,P])");
    }

}
