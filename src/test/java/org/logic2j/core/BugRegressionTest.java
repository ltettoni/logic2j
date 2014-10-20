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
package org.logic2j.core;

import org.junit.Test;
import org.logic2j.core.library.impl.IOLibrary;

/**
 * Check non-regression on issues that have been found and solved over time.
 */
public class BugRegressionTest extends PrologTestBase {



    /**
     * There was a serious bug with CUT within subgoals. It's now fixed.
     */
    @Test
    public void int5() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(5, "int5(X)");
        nSolutions(5, "int5_rule(X)");
        //
        uniqueSolution("int5(X), !");
        uniqueSolution("int5_rule(X), !"); // Horrible bug ! yields 5 solutions instead of 1!!!
    }

    /**
     * Avoid creating cycles in bindings, or looping forever during unification.
     */
    @Test
    public void infiniteLoopWhenUnifying2Vars() {
        // This is a simple case: once we have bound, we should not redo it!
        uniqueSolution("Free=X, Free=X");
        // This is more complex: we must avoid binding X to Free, when Free is already linked to X!
        uniqueSolution("Free=X, X=Free");
    }

    /**
     * See documentation in resource bug-cut-propagated-too-high.pro
     */
    @Test
    public void bugWithCutPropagatedTooHighIntoCaller() {
        loadTheoryFromTestResourcesDir("bug-cut-propagated-too-high.pro");
        // Correct behaviour (used to work)
        nSolutions(4, "distinct(X, a(X), L), member(E, L), existsOk1(a(E))");
        nSolutions(4, "distinct(X, a(X), L), member(E, L), existsOk2(a(E))");
        // Used to return only one solution instead of two!
        nSolutions(4, "distinct(X,a(X), L), member(E, L), existsKo1(a(E))");
        nSolutions(4, "distinct(X,a(X), L), member(E, L), existsKo2(a(E))");

        nSolutions(2, "(E=1;E=2), existsOk1(a(E))");
        nSolutions(2, "(E=1;E=2), existsOk2(a(E))");

        nSolutions(2, "(E=1;E=2), existsKo1(a(E))"); // Used to return only one solution instead of two!
        nSolutions(2, "(E=1;E=2), existsKo2(a(E))"); // Used to return only one solution instead of two!
    }


    /**
     * Had an issue with calling primitives (eg. "nolog/*" and following a cut.
     */
    @Test
    public void bugAddingPrimitiveBreaksNormalProcessingOfCut() {
        loadLibrary(new IOLibrary(getProlog()));
        loadTheoryFromTestResourcesDir("bug-cut-propagated-too-high.pro");
        nSolutions(2, "(E=1;E=2), existsKo3(a(E))"); // Used to return only one solution instead of two!
    }
}
