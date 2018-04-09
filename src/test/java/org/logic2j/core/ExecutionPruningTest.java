/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.core;

import org.junit.Test;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.holder.GoalHolder;
import org.logic2j.engine.solver.listener.CountingSolutionListener;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.engine.solver.listener.SolutionListener;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the cut and user abort features.
 */
public class ExecutionPruningTest extends PrologTestBase {

    //@After
    //public void reportProfilingInfo() {
    //    ProfilingInfo.reportAll("After ExecutionPruningTest test case");
    //}

    @Override
    protected PrologReferenceImplementation.InitLevel initLevel() {
        // In case we use logging predicates we need the IO Library
        return PrologReferenceImplementation.InitLevel.L2_BASE_LIBRARIES;
    }

    @Test
    public void cutAndOr() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(1, "!; true");
        nSolutions(2, "true; !");
        nSolutions(2, "true; !; true");
        nSolutions(3, "true; true; !; true");
        nSolutions(2, "true; !; true; !; true");
    }

    @Test
    public void withoutCut() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(3, "a(X)");
        nSolutions(9, "a(X), b(Y)");
        nSolutions(27, "a(X), b(Y), c(Z)");
    }

    @Test
    public void cutAtEnd() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(1, "a(X), !");
        nSolutions(1, "a(X), b(Y), !");
        nSolutions(1, "a(X), b(Y), c(Z), !");
    }

    @Test
    public void cutInMiddle() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(3, "a(X), !, b(Y)");
        nSolutions(3, "a(X), !, !, b(Y)");
        nSolutions(3, "a(X), b(Y), !, c(Z)");
    }

    @Test
    public void cutAtBeginning() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(27, "!, a(X), b(Y), c(Z)");
    }

    @Test
    public void cutOtherCases() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(1, "!");
        nSolutions(1, "!, !");
        //
        nSolutions(1, "a(X), !, b(Y), !");
        nSolutions(1, "a(X), !, b(Y), !, !");
    }

    @Test
    public void cut1() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(1, "cut1(X)");
    }

    @Test
    public void cut2() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(2, "cut2(X)");
    }

    @Test
    public void cut4() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(4, "cut4");
    }

    @Test
    public void cut4b() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(4, "cut4b");
    }

    @Test
    public void max_green_cut() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        //
        uniqueSolution("max(2,3,3)");
        uniqueSolution("max(3,2,3)");
        //
        assertThat(uniqueSolution("max(2,3,X)").var("X").unique()).isEqualTo(term(3));
        assertThat(uniqueSolution("max(3,2,X)").var("X").unique()).isEqualTo(term(3));
        //
        uniqueSolution("max4(3,1,7,5,7)");
        uniqueSolution("max4(1,3,5,7,7)");
        uniqueSolution("max4(7,5,3,1,7)");
        assertThat(uniqueSolution("max4(3,1,7,5,X)").var("X").unique()).isEqualTo(term(7));
    }

    @Test
    public void sign() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        //
        assertThat(uniqueSolution("sign(5,X)").var("X").unique()).isEqualTo(term("positive"));
        assertThat(uniqueSolution("sign(-5,X)").var("X").unique()).isEqualTo(term("negative"));
        assertThat(uniqueSolution("sign(0,X)").var("X").unique()).isEqualTo(term("zero"));
    }

    @Test
    public void sign2() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        //
        assertThat(uniqueSolution("sign2(0,X)").var("X").unique()).isEqualTo(term("zero"));
        //
        GoalHolder solutions;
        solutions = this.prolog.solve("sign2(-5,X)");
        assertThat(solutions.var("X").unique().toString()).isEqualTo("negative");
        //
        solutions = this.prolog.solve("sign2(5,X)");
        assertThat(solutions.var("X").unique().toString()).isEqualTo("positive");
    }

    @Test
    public void sign3() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        //
        assertThat(uniqueSolution("sign3(0,X)").var("X").unique()).isEqualTo(term("zero"));
        //
        GoalHolder solutions;
        solutions = this.prolog.solve("sign3(-5,X)");
        assertThat(solutions.var("X").unique().toString()).isEqualTo("negative");
        //
        solutions = this.prolog.solve("sign3(5,X)");
        assertThat(solutions.var("X").unique().toString()).isEqualTo("positive");
    }

    @Test
    public void sign4() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        //
        assertThat(uniqueSolution("sign4(0,X)").var("X").unique()).isEqualTo(term("zero"));
        //
    }

    @Test
    public void sign5neg() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        GoalHolder solutions;
        solutions = this.prolog.solve("sign4(-5,X)");
        assertThat(solutions.var("X").unique().toString()).isEqualTo("negative");
    }

    @Test
    public void sign5pos() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        GoalHolder solutions;
        //
        solutions = this.prolog.solve("sign4(5,X)");
        assertThat(solutions.var("X").unique().toString()).isEqualTo("positive");
    }

    @Test
    public void transformWithCutAndCatchAll() {
        //
        loadTheoryFromTestResourcesDir("test-functional.pro");
        final List<Map<Var, Object>> list = this.prolog.solve("transform(complex, Z)").vars().list();
        assertThat(list.toString()).isEqualTo("[{Z=verySimple}]");
    }

    // ---------------------------------------------------------------------------
    // Former tests from FunctionalTest
    // ---------------------------------------------------------------------------

    @Test
    public void cut() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        nSolutions(0, "pc(X)");
        nSolutions(3, "p(X), X>1");
        nSolutions(1, "a(X), !, cut1(Y)");
    }

    // ---------------------------------------------------------------------------
    // Testing user abort
    // ---------------------------------------------------------------------------

    /**
     * A {@link SolutionListener} that will request user cancellation after the first solution was found.
     */
    private static class Max1Listener extends CountingSolutionListener {

        public Max1Listener() {
            // Nothing - just create public constructor for accessibility
        }

        @Override
        public Integer onSolution(UnifyContext currentVars) {
            super.onSolution(currentVars);
            return Continuation.USER_ABORT;
        }
    }

    /**
     * A {@link SolutionListener} that will request user cancellation after 5 solutions were found.
     */
    private static class Max5Listener extends CountingSolutionListener {

        public Max5Listener() {
            // Nothing - just create public constructor for accessibility
        }

        @Override
        public Integer onSolution(UnifyContext currentVars) {
            super.onSolution(currentVars);
            final boolean requestContinue = count() < 5;
            return requestContinue ? Continuation.CONTINUE : Continuation.USER_ABORT;
        }
    }

    @Test
    public void userCancel() {
        final Object term = unmarshall("member(X, [0,1,2,3,4,5,6,7,8,9])");
        final CountingSolutionListener listenerAll = new CountingSolutionListener();
        this.prolog.getSolver().solveGoal(term, listenerAll);
        assertThat(listenerAll.count()).isEqualTo(10);
        // Only one
        final Max1Listener maxOneSolution = new Max1Listener();
        this.prolog.getSolver().solveGoal(term, maxOneSolution);
        assertThat(maxOneSolution.count()).isEqualTo(1);
        // Only five
        final Max5Listener maxFiveSolutions = new Max5Listener();
        this.prolog.getSolver().solveGoal(term, maxFiveSolutions);
        assertThat(maxFiveSolutions.count()).isEqualTo(5);
    }


    @Test
    public void level() {
        final PrologReferenceImplementation.InitLevel initLevel = initLevel();
        System.out.println("Level=" + initLevel);

    }
}
