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
package org.logic2j.core.solver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.logic2j.core.Prolog;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.model.symbol.TLong;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.holder.SolutionHolder;
import org.logic2j.core.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.solver.listener.Solution;
import org.logic2j.core.solver.listener.SolutionListener;

/**
 * Check {@link Solver} on extremely trivial goals, and also check the {@link SolutionHolder} API to extract solutions (results and
 * bindings).
 */
public class SolverTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolverTest.class);

    @Test
    public void veryBasic() {
        assertEquals(null, assertOneSolution("X=X").binding("X"));
    }

    @Test
    public void unique() {
        final Prolog prolog = new PrologReferenceImplementation(InitLevel.L2_BASE_LIBRARIES);
        //
        try {
            prolog.solve("1=2").unique();
            fail("There was no solution, unique() should have failed because it immediately solves the goal (unlike all()).");
        } catch (final Exception e) {
            // Expected
        }
        try {
            prolog.solve("2=2").unique().binding("X");
            fail("There was one solution, but no variable X, variable() should have failed.");
        } catch (final Exception e) {
            // Expected
        }
        // Value of a non-bound variable
        assertEquals(null, prolog.solve("Z=Z").unique().binding("Z"));
        assertEquals(null, prolog.solve("Z=Y").unique().binding("Z"));
        assertEquals(null, prolog.solve("write(Z_written_from_SolverTest)").unique().binding("Z_written_from_SolverTest"));
        // Obtain values of bound variables
        final UniqueSolutionHolder unique = prolog.solve("X=2, Y=3").unique();
        assertEquals(new TLong(2), unique.binding("X"));
        assertEquals(new TLong(3), unique.binding("Y"));
        // Obtain all variables
        final Map<String, Term> vars = new HashMap<String, Term>();
        vars.put("X", new TLong(2));
        vars.put("Y", new TLong(3));
        assertEquals(vars, unique.bindings());
        // Obtain resolved term
        assertEquals(prolog.term("2=2, 3=3"), unique.solution());
    }

    @Test
    public void multiple() {
        final Prolog prolog = new PrologReferenceImplementation();
        // Nothing should be actually solved by calling all()
        prolog.solve("1=2").all();

        try {
            prolog.solve("2=2").all().binding("X");
            fail("There was one solution, but no variable X, variable() should have failed.");
        } catch (final Exception e) {
            // Expected
        }
        //
        assertEquals(termList("a", "b"), prolog.solve("member(X, [a,b])").all().binding("X"));
    }

    @Test
    public void iterator() {
        final Prolog prolog = new PrologReferenceImplementation();
        final Iterator<Solution> iterator = prolog.solve("member(X, [1,2,3,4])").iterator();
        assertNotNull(iterator);
        int counter = 0;
        while (iterator.hasNext()) {
            logger.info(" value of next()={}", iterator.next());
            counter++;
        }
        assertEquals(4, counter);
    }

    /**
     * Just count solutions - won't request user cancellation.
     */
    static class CountingListener implements SolutionListener {
        int counter = 0;

        @Override
        public Continuation onSolution() {
            this.counter++;
            return Continuation.CONTINUE;
        }
    }

    /**
     * Will request user cancellation after the first solution was found.
     */
    static class Max1Listener implements SolutionListener {
        int counter = 0;

        @Override
        public Continuation onSolution() {
            this.counter++;
            return Continuation.USER_ABORT;
        }
    }

    /**
     * Will request user cancellation after 5 solutions were found.
     * 
     */
    static class Max5Listener implements SolutionListener {
        int counter = 0;

        @Override
        public Continuation onSolution() {
            this.counter++;
            final boolean requestContinue = this.counter < 5;
            return Continuation.requestContinuationWhen(requestContinue);
        }
    }

    @Test
    public void userCancel() {
        final PrologImplementation prolog = new PrologReferenceImplementation();
        final Term term = prolog.term("member(X, [0,1,2,3,4,5,6,7,8,9])");
        final CountingListener listenerAll = new CountingListener();
        getProlog().getSolver().solveGoal(new Bindings(term), new GoalFrame(), listenerAll);
        assertEquals(10, listenerAll.counter);
        //
        final Max1Listener listener1 = new Max1Listener();
        getProlog().getSolver().solveGoal(new Bindings(term), new GoalFrame(), listener1);
        assertEquals(1, listener1.counter);
        //
        final Max5Listener listener5 = new Max5Listener();
        getProlog().getSolver().solveGoal(new Bindings(term), new GoalFrame(), listener5);
        assertEquals(5, listener5.counter);
    }

}
