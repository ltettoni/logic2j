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
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation;

/**
 * Test the cut and user abort features.
 */
public class ExecutionPruningTest extends PrologTestBase {

    @Test
    public void placeholder() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(4, "cut4");
    }

    @Test
    public void cutAndOr() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "!; true");
        assertNSolutions(2, "true; !");
        assertNSolutions(2, "true; !; true");
        assertNSolutions(3, "true; true; !; true");
        assertNSolutions(2, "true; !; true; !; true");
    }

    @Test
    public void withoutCut() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(3, "a(X)");
        assertNSolutions(9, "a(X), b(Y)");
        assertNSolutions(27, "a(X), b(Y), c(Z)");
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
        assertNSolutions(3, "a(X), !, b(Y)");
        assertNSolutions(3, "a(X), !, !, b(Y)");
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
        //
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

    @Test
    public void max_green_cut() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertOneSolution("max(2,3,3)");
        assertOneSolution("max(3,2,3)");
        //
        assertEquals(term(3), assertOneSolution("max(2,3,X)").binding("X"));
        assertEquals(term(3), assertOneSolution("max(3,2,X)").binding("X"));
        //
        assertOneSolution("max4(3,1,7,5,7)");
        assertOneSolution("max4(1,3,5,7,7)");
        assertOneSolution("max4(7,5,3,1,7)");
        assertEquals(term(7), assertOneSolution("max4(3,1,7,5,X)").binding("X"));
    }

    @Test
    public void sign() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("positive"), assertOneSolution("sign(5,X)").binding("X"));
        assertEquals(term("negative"), assertOneSolution("sign(-5,X)").binding("X"));
        assertEquals(term("zero"), assertOneSolution("sign(0,X)").binding("X"));
    }

    @Test
    public void sign2() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("zero"), assertOneSolution("sign2(0,X)").binding("X"));
        //
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("sign2(-5,X)").all();
        assertEquals("[negative]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("sign2(5,X)").all();
        assertEquals("[positive]", solutions.binding("X").toString());
    }

    @Test
    public void sign3() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("zero"), assertOneSolution("sign3(0,X)").binding("X"));
        //
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("sign3(-5,X)").all();
        assertEquals("[negative]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("sign3(5,X)").all();
        assertEquals("[positive]", solutions.binding("X").toString());
    }

    @Test
    public void sign4() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("zero"), assertOneSolution("sign4(0,X)").binding("X"));
        //
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("sign4(-5,X)").all();
        assertEquals("[negative]", solutions.binding("X").toString());
        //
        solutions = this.prolog.solve("sign4(5,X)").all();
        assertEquals("[positive]", solutions.binding("X").toString());
    }

    // ---------------------------------------------------------------------------
    // Former tests from FunctionalTest
    // ---------------------------------------------------------------------------

    @Test
    public void cut() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(0, "pc(X)");
        assertNSolutions(3, "p(X), X>1");
        assertNSolutions(1, "a(X), !, cut1(Y)");
        assertNSolutions(4, "cut4", "cut4b");
    }

    // ---------------------------------------------------------------------------
    // Testing user abort
    // ---------------------------------------------------------------------------

    /**
     * A {@link SolutionListener} that will request user cancellation after the first solution was found.
     */
    private static class Max1Listener implements SolutionListener {
        private int counter = 0;

        @Override
        public Continuation onSolution() {
            this.counter++;
            return Continuation.USER_ABORT;
        }
    }

    /**
     * A {@link SolutionListener} that will request user cancellation after 5 solutions were found.
     */
    private static class Max5Listener implements SolutionListener {
        private int counter = 0;

        @Override
        public Continuation onSolution() {
            this.counter++;
            final boolean requestContinue = this.counter < 5;
            return Continuation.requestContinuationWhen(requestContinue);
        }
    }

    /**
     * A {@link SolutionListener} that counts solutions - won't request user cancellation.
     */
    private static class CountingListener implements SolutionListener {
        private int counter = 0;

        @Override
        public Continuation onSolution() {
            this.counter++;
            return Continuation.CONTINUE;
        }
    }

    @Test
    public void userCancel() {
        final PrologImplementation prolog = new PrologReferenceImplementation();
        final Object term = this.prolog.getTermExchanger().unmarshall("member(X, [0,1,2,3,4,5,6,7,8,9])");
        final CountingListener listenerAll = new CountingListener();
        prolog.getSolver().solveGoal(new Bindings(term), listenerAll);
        assertEquals(10, listenerAll.counter);
        // Only one
        final Max1Listener maxOneSolution = new Max1Listener();
        prolog.getSolver().solveGoal(new Bindings(term), maxOneSolution);
        assertEquals(1, maxOneSolution.counter);
        // Only five
        final Max5Listener maxFiveSolutions = new Max5Listener();
        prolog.getSolver().solveGoal(new Bindings(term), maxFiveSolutions);
        assertEquals(5, maxFiveSolutions.counter);
    }

}
