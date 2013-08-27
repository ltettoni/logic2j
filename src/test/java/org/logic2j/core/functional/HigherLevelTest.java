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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.benchmark.BenchmarkTest;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.library.impl.io.IOLibrary;
import org.logic2j.core.solver.holder.MultipleSolutionsHolder;

/**
 * Run higher-level tests such as whole programs.
 * When this works, there is much chance that the {@link PrologImplementation} is satisfactory for solving real-life problems, although full
 * test coverage is far from guaranteed.
 * For performance testing see {@link BenchmarkTest}.
 */
public class HigherLevelTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HigherLevelTest.class);

    @Override
    protected InitLevel initLevel() {
        return InitLevel.L0_BARE;
    }

    /**
     * Reasonably-sided Towers of Hanoi. See also {@link BenchmarkTest#hanoi()}
     */
    @Test
    public void hanoi() {
        loadTheoryFromTestResourcesDir("hanoi.pl");
        assertOneSolution("move(5, left, right, center)"); // Watch out 7 is the limit with Java's ridiculous default stack size
    }

    /**
     * This simple Prolog program checks or generates change adding up to a dollar consisting of half-dollars, quarters, dimes, nickels, and
     * pennies.
     */
    @Test
    public void changeForOneDollar() {
        final IOLibrary library = new IOLibrary(this.prolog);
        loadLibrary(library);
        loadTheoryFromTestResourcesDir("dollar.pl");
        assertNSolutions(292, "change([H,Q,D,N,P])");
    }

    @Test
    public void delete() throws Exception {
        loadTheoryFromTestResourcesDir("sorting.pl");
        MultipleSolutionsHolder solutions;
        //
        assertNoSolution("delete(a, [], X)");
        //
        solutions = this.prolog.solve("delete(a, [a], X)").all();
        assertEquals("[[]]", solutions.binding("X").toString());
        //
        assertNoSolution("delete(k, [a], X)");
        //
        solutions = this.prolog.solve("delete(a, [a, b, c], X)").all();
        assertEquals("[[b,c]]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("delete(b, [a, b, c], X)").all();
        assertEquals("[[a,c]]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("delete(c, [a, b, c], X)").all();
        assertEquals("[[a,b]]", solutions.binding("X").toString());
        //
        assertNoSolution("delete(k, [a, b, c], X)");
        //
        solutions = this.prolog.solve("delete(X, [a, b, c], Y)").all();
        assertEquals("[a, b, c]", solutions.binding("X").toString());
        assertEquals("[[b,c], [a,c], [a,b]]", solutions.binding("Y").toString());
    }

    @Test
    public void permutations() throws Exception {
        loadTheoryFromTestResourcesDir("sorting.pl");
        MultipleSolutionsHolder solutions;
        //
        solutions = this.prolog.solve("perm([], X)").all();
        assertEquals("[[]]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("perm([a], X)").all();
        assertEquals("[[a]]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("perm([a,b], X)").all();
        assertEquals("[[a,b], [b,a]]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("perm([a,b,c], X)").all();
        assertEquals("[[a,b,c], [a,c,b], [b,a,c], [b,c,a], [c,a,b], [c,b,a]]", solutions.binding("X").toString());
    }

}
