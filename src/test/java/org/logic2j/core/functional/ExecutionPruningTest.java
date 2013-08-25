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

/**
 * Test the cut and user abort features.
 */
public class ExecutionPruningTest extends PrologTestBase {

    @Test
    public void justForDebugging() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "a(X), b(Y), !");
    }

    @Test
    public void cutAtEnd() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "a(X), !");
        assertNSolutions(1, "a(X), b(Y), !");
        assertNSolutions(1, "a(X), b(Y), c(Z), !");
    }

    @Test
    public void cutInMiddle() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(3, "a(X), b(Y), !, c(Z)");
    }

    @Test
    public void cutAtBeginning() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(27, "!, a(X), b(Y), c(Z)");
    }

    @Test
    public void cutOtherCases() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "!");
        assertNSolutions(1, "!, !");
        assertNSolutions(3, "a(X)");
        assertNSolutions(1, "a(X), !");
        //
        assertNSolutions(9, "a(X), b(Y)");
        assertNSolutions(3, "a(X), !, b(Y)");
        assertNSolutions(3, "a(X), !, !, b(Y)");
        assertNSolutions(1, "a(X), !, b(Y), !");
        assertNSolutions(1, "a(X), !, b(Y), !, !");
    }

    @Test
    public void cut1() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "cut1(X)");
    }

    @Test
    public void cut2() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(2, "cut2(X)");
    }

    @Test
    public void cut4b() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(4, "cut4b");
    }

    // ---------------------------------------------------------------------------
    // Former tests from FunctionalTest
    // ---------------------------------------------------------------------------

    @Test
    public void cut() throws Exception {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "cut1(_)", "a(X), !", "a(X), b(Y), c(Z), !", "p(X), X=4");

        assertNSolutions(2, "cut2(_)");
        assertNSolutions(0, "pc(X)");
        assertNSolutions(3, "p(X), X>1");

        assertNSolutions(1, "a(X), !, cut1(Y)");
        assertNSolutions(4, "cut4", "cut4b");
    }

}
