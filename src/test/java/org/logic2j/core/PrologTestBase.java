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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.logic2j.core.PrologImpl.InitLevel;
import org.logic2j.core.TermFactory.FactoryMode;
import org.logic2j.core.library.PLibrary;
import org.logic2j.core.library.mgmt.LibraryContent;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.solver.holder.SolutionHolder;
import org.logic2j.core.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.theory.TheoryContent;
import org.logic2j.core.theory.TheoryManager;

/**
 * Base class for tests, initiazlize a fresh {@link PrologImpl} on every method (level of init is {@link InitLevel#L1_CORE_LIBRARY}, and
 * provides utility methods.
 */
public abstract class PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrologTestBase.class);

    protected static final String TEST_RESOURCES_DIR = "src/test/resources";

    private PrologImplementor prolog;

    protected InitLevel initLevel() {
        return InitLevel.L1_CORE_LIBRARY;
    }

    @Before
    public void setUp() {
        this.prolog = new PrologImpl(initLevel());
    }

    @After
    public void tearDown() {
        this.prolog = null;
    }

    protected PrologImplementor getProlog() {
        return this.prolog;
    }

    /**
     * Make sure there is only one solution to goals.
     * 
     * @param theGoals All goals to check for
     * @return The UniqueSolutionHolder holding the solution
     */
    protected UniqueSolutionHolder assertOneSolution(CharSequence... theGoals) {
        UniqueSolutionHolder result = null;
        for (final CharSequence goal : theGoals) {
            final Term parsed = getProlog().term(goal);
            logger.info("Expecting 1 solution when solving goal \"{}\"", goal);
            final SolutionHolder solution = getProlog().solve(parsed);
            result = solution.unique();
        }
        return result;
    }

    /**
     * Make sure there are no soutions.
     * 
     * @param theGoals All goals to check for
     * @return
     */
    protected SolutionHolder assertNoSolution(CharSequence... theGoals) {
        return internalAssert(0, theGoals);
    }

    /**
     * Make sure there are exatly theNumber of solutions.
     * 
     * @param theNumber
     * @param theGoals All goals to check for
     * @return
     */
    protected MultipleSolutionsHolder assertNSolutions(int theNumber, CharSequence... theGoals) {
        return internalAssert(theNumber, theGoals).all();
    }

    // FIXME Not good, should use direct Junit and @Expected
    protected void assertGoalMustFail(CharSequence... theGoals) {
        for (final CharSequence theGoal : theGoals) {
            try {
                getProlog().solve(theGoal).number();
                fail("Goal should have failed and did not: \"" + theGoals + '"');
            } catch (final RuntimeException e) {
                // Normal
            }
        }
    }

    /**
     * @param theNumber
     * @param theGoals
     * @return The {@link SolutionHolder} resulting from solving the last goal (i.e. the first when only one...). Null if no goal specified.
     */
    private SolutionHolder internalAssert(int theNumber, CharSequence... theGoals) {
        SolutionHolder solve = null;
        for (final CharSequence goal : theGoals) {
            final Term parsed = getProlog().term(goal);
            logger.info("Expecting {} solutions to solving goal \"{}\"", theNumber, goal);
            solve = getProlog().solve(parsed);
            assertEquals("Goal " + goal + " did has different number of solutions", theNumber, solve.number());
        }
        return solve;
    }

    /**
     * Factory.
     * 
     * @param theObject
     * @return A single Term, corresponding to theObject.
     */
    protected Term term(Object theObject) {
        return getProlog().getTermFactory().create(theObject, FactoryMode.ANY_TERM);
    }

    /**
     * Utility factory.
     * 
     * @param elements The elements of the list to parse as Terms
     * @return A List of term, corresponding to the related elements passed as argument.
     */
    protected List<Term> termList(Object... elements) {
        final List<Term> result = new ArrayList<Term>(elements.length);
        for (final Object element : elements) {
            result.add(term(element));
        }
        return result;
    }

    /**
     * @param theFile To be loaded
     * @throws IOException
     */
    private void addTheory(File theFile) throws IOException {
        final TheoryManager manager = getProlog().getTheoryManager();
        final TheoryContent load = manager.load(theFile);
        manager.addTheory(load);
        logger.debug("Loaded theory from: {}", theFile);
    }

    protected void addTheoryFromTestResourceDir(String theFilename) throws IOException {
        addTheory(new File(TEST_RESOURCES_DIR, theFilename));
    }

    protected LibraryContent loadLibrary(PLibrary theLibrary) {
        return getProlog().getLibraryManager().loadLibrary(theLibrary);
    }

}
