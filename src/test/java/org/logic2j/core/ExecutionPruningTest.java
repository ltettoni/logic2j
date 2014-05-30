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
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.api.solver.holder.GoalHolder;

import static org.junit.Assert.assertEquals;

/**
 * Test the cut and user abort features.
 */
public class ExecutionPruningTest extends PrologTestBase {

    @Test
    public void placeholderToReproduceError() {
        //
    }

    @Test
    public void cutAndOr() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(1, "!; true");
        nSolutions(2, "true; !");
        nSolutions(2, "true; !; true");
        nSolutions(3, "true; true; !; true");
        nSolutions(2, "true; !; true; !; true");
    }

    @Test
    public void withoutCut() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(3, "a(X)");
        nSolutions(9, "a(X), b(Y)");
        nSolutions(27, "a(X), b(Y), c(Z)");
    }

    @Test
    public void cutAtEnd() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(1, "a(X), !");
        nSolutions(1, "a(X), b(Y), !");
        nSolutions(1, "a(X), b(Y), c(Z), !");
    }

    @Test
    public void cutInMiddle() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(3, "a(X), !, b(Y)");
        nSolutions(3, "a(X), !, !, b(Y)");
        nSolutions(3, "a(X), b(Y), !, c(Z)");
    }

    @Test
    public void cutAtBeginning() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(27, "!, a(X), b(Y), c(Z)");
    }

    @Test
    public void cutOtherCases() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(1, "!");
        nSolutions(1, "!, !");
        //
        nSolutions(1, "a(X), !, b(Y), !");
        nSolutions(1, "a(X), !, b(Y), !, !");
    }

    @Test
    public void cut1() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(1, "cut1(X)");
    }

    @Test
    public void cut2() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(2, "cut2(X)");
    }

    @Test
    public void cut4b() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(4, "cut4b");
    }

    @Test
    public void max_green_cut() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        uniqueSolution("max(2,3,3)");
        uniqueSolution("max(3,2,3)");
        //
        assertEquals(term(3), uniqueSolution("max(2,3,X)").var("X").unique());
        assertEquals(term(3), uniqueSolution("max(3,2,X)").var("X").unique());
        //
        uniqueSolution("max4(3,1,7,5,7)");
        uniqueSolution("max4(1,3,5,7,7)");
        uniqueSolution("max4(7,5,3,1,7)");
        assertEquals(term(7), uniqueSolution("max4(3,1,7,5,X)").var("X").unique());
    }

    @Test
    public void sign() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("positive"), uniqueSolution("sign(5,X)").var("X").unique());
        assertEquals(term("negative"), uniqueSolution("sign(-5,X)").var("X").unique());
        assertEquals(term("zero"), uniqueSolution("sign(0,X)").var("X").unique());
    }

    @Test
    public void sign2() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("zero"), uniqueSolution("sign2(0,X)").var("X").unique());
        //
        GoalHolder solutions;
        solutions = this.prolog.solve("sign2(-5,X)");
        assertEquals("negative", solutions.var("X").unique().toString());
        //
        solutions = this.prolog.solve("sign2(5,X)");
        assertEquals("positive", solutions.var("X").unique().toString());
    }

    @Test
    public void sign3() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("zero"), uniqueSolution("sign3(0,X)").var("X").unique());
        //
        GoalHolder solutions;
        solutions = this.prolog.solve("sign3(-5,X)");
        assertEquals("negative", solutions.var("X").unique().toString());
        //
        solutions = this.prolog.solve("sign3(5,X)");
        assertEquals("positive", solutions.var("X").unique().toString());
    }

    @Test
    public void sign4() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        assertEquals(term("zero"), uniqueSolution("sign4(0,X)").var("X").unique());
        //
        GoalHolder solutions;
        solutions = this.prolog.solve("sign4(-5,X)");
        assertEquals("negative", solutions.var("X").unique().toString());
        //
        solutions = this.prolog.solve("sign4(5,X)");
        assertEquals("positive", solutions.var("X").unique().toString());
    }

    // ---------------------------------------------------------------------------
    // Former tests from FunctionalTest
    // ---------------------------------------------------------------------------

    @Test
    public void cut() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        nSolutions(0, "pc(X)");
        nSolutions(3, "p(X), X>1");
        nSolutions(1, "a(X), !, cut1(Y)");
        nSolutions(4, "cut4", "cut4b");
    }

    // ---------------------------------------------------------------------------
    // Testing user abort
    // ---------------------------------------------------------------------------

    /**
     * A {@link org.logic2j.core.api.SolutionListener} that will request user cancellation after the first solution was found.
     */
    private static class Max1Listener implements SolutionListener {
        int counter = 0;

        public Max1Listener() {
            // Nothing - just create public constructor for accessibility
        }

        @Override
        public Continuation onSolution(PoV pov) {
            this.counter++;
            return Continuation.USER_ABORT;
        }
    }

    /**
     * A {@link org.logic2j.core.api.SolutionListener} that will request user cancellation after 5 solutions were found.
     */
    private static class Max5Listener implements SolutionListener {
        int counter = 0;

        public Max5Listener() {
            // Nothing - just create public constructor for accessibility
        }

        @Override
        public Continuation onSolution(PoV pov) {
            this.counter++;
            final boolean requestContinue = this.counter < 5;
            return requestContinue ? Continuation.CONTINUE : Continuation.USER_ABORT;
        }
    }

    /**
     * A {@link org.logic2j.core.api.SolutionListener} that counts solutions - won't request user cancellation.
     */
    private static class CountingListener implements SolutionListener {
        int counter = 0;

        public CountingListener() {
            // Nothing - just create public constructor for accessibility
        }

        @Override
        public Continuation onSolution(PoV pov) {
            this.counter++;
            return Continuation.CONTINUE;
        }
    }

    @Test
    public void userCancel() {
        final Object term = unmarshall("member(X, [0,1,2,3,4,5,6,7,8,9])");
        final CountingListener listenerAll = new CountingListener();
        this.prolog.getSolver().solveGoal(term, listenerAll);
        assertEquals(10, listenerAll.counter);
        // Only one
        final Max1Listener maxOneSolution = new Max1Listener();
        this.prolog.getSolver().solveGoal(term, maxOneSolution);
        assertEquals(1, maxOneSolution.counter);
        // Only five
        final Max5Listener maxFiveSolutions = new Max5Listener();
        this.prolog.getSolver().solveGoal(term, maxFiveSolutions);
        assertEquals(5, maxFiveSolutions.counter);
    }

}
