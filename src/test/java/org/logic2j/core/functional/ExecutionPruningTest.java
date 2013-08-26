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
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.Continuation;
import org.logic2j.core.solver.listener.SolutionListener;

/**
 * Test the cut and user abort features.
 */
public class ExecutionPruningTest extends PrologTestBase {

    @Test
    public void justForDebugging() {
        // Empty use just for debugging one particular case
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

    // ---------------------------------------------------------------------------
    // Former tests from FunctionalTest
    // ---------------------------------------------------------------------------

    @Test
    public void cut() throws Exception {
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
        final Term term = prolog.term("member(X, [0,1,2,3,4,5,6,7,8,9])");
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
