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
package org.logic2j.core.api.solver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Solution;
import org.logic2j.core.api.solver.holder.SolutionHolder;
import org.logic2j.core.api.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;

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
        // Use a different init level for this test
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
        assertEquals(2L, unique.binding("X"));
        assertEquals(3L, unique.binding("Y"));
        // Obtain all variables
        final Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("X", 2L);
        vars.put("Y", 3L);
        assertEquals(vars, unique.bindings());
        // Obtain resolved term
        assertEquals(this.prolog.getTermExchanger().unmarshall("2=2, 3=3"), unique.solution());
    }

    @Test
    public void multiple() {
        // Nothing should be actually solved by calling all()
        this.prolog.solve("1=2").all();

        try {
            this.prolog.solve("2=2").all().binding("X");
            fail("There was one solution, but no variable X, variable() should have failed.");
        } catch (final Exception e) {
            // Expected
        }
        //
        assertEquals(termList("a", "b"), this.prolog.solve("member(X, [a,b])").all().binding("X"));
    }

    @Test
    public void iterator() {
        final Iterator<Solution> iterator = this.prolog.solve("member(X, [1,2,3,4])").iterator();
        assertNotNull(iterator);
        int counter = 0;
        while (iterator.hasNext()) {
            logger.info(" value of next()={}", iterator.next());
            counter++;
        }
        assertEquals(4, counter);
    }

    @Test
    public void iterable() {
        int counter = 0;
        for (final Solution solution : this.prolog.solve("member(X, [1,2,3,4])")) {
            logger.info(" iterable Solution={}", solution);
            counter++;
        }
        assertEquals(4, counter);
    }

}
