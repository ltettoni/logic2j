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
package org.logic2j.core.library.impl;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.extractor.SingleVarExtractor;
import org.logic2j.core.api.solver.listener.SingleVarSolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.api.solver.listener.multi.MultiResult;
import org.logic2j.core.api.unify.UnifyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class AdHocLibraryTest extends PrologTestBase {
    private static final Logger logger = LoggerFactory.getLogger(AdHocLibraryTest.class);

    @Before
    public void registerLibrary() {
        this.prolog.getLibraryManager().loadLibrary(new AdHocLibraryForTesting(this.prolog));
    }

    @Test
    public void int_range_classic_1() {
        assertThat(nSolutions(3, "int_range_classic(12, Q, 15)").var("Q").list()).isEqualTo(termList("12", "13", "14"));
        noSolutions("int_range_classic(12, X, 10)");
    }

    @Test
    public void int_range_classic_2() {
        noSolutions("int_range_classic(10, X, 10)");
    }

    @Test(expected = InvalidTermException.class)
    public void int_range_classic_minNotBound() {
        noSolutions("int_range_classic(Min, Q, 10)");
    }

    @Test(expected = InvalidTermException.class)
    public void int_range_classic_maxNotBound() {
        noSolutions("int_range_classic(5, Q, Max)");
    }

    // ---------------------------------------------------------------------------
    // Multiple solutions with a normal listener
    // ---------------------------------------------------------------------------

    @Test
    public void int_range_multi() {
        final String goalText;
        goalText = "int_range_multi(10, Q, 15)";
        assertThat(nSolutions(5, goalText).var("Q").list()).isEqualTo(termList("10", "11", "12", "13", "14"));
    }

    @Test
    public void int_range_multi_OR() {
        final String goalText;
        goalText = "int_range_multi(10, Q, 15) ; int_range_multi(12, Q, 18)";
        assertThat(nSolutions(11, goalText).var("Q").list()).isEqualTo(termList("10", "11", "12", "13", "14", "12", "13", "14", "15", "16", "17"));
    }

    @Test
    public void int_range_multi_AND() {
        final String goalText;
        goalText = "int_range_multi(10, Q, 15) , int_range_multi(12, Q, 18)";
        assertThat(nSolutions(3, goalText).var("Q").list()).isEqualTo(termList("12", "13", "14"));
    }

    // ---------------------------------------------------------------------------
    // Multiple solutions with special listener
    // ---------------------------------------------------------------------------

    @Test
    public void int_range_multi_with_listener() throws Exception {
        final String goalText;
        goalText = "int_range_multi(10, Q, 15) , int_range_multi(12, Q, 18)";
        Object goal = getProlog().getTermUnmarshaller().unmarshall(goalText);
        final Var<?> q = TermApi.findVar(goal, "Q");
        final SolutionListenerBase listener = new SolutionListenerBase() {

            @Override
            public Integer onSolution(UnifyContext currentVars) {
                logger.info("App listener got one solution: {}", currentVars.reify(q));
                return Continuation.CONTINUE;
            }

            @Override
            public Integer onSolutions(MultiResult multi) {
                logger.info("App listener got multi solutions: {}", multi);
                return Continuation.CONTINUE;
            }
        };
        getProlog().getSolver().solveGoal(goal, listener);

    }
}
