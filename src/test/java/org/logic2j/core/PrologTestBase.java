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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.logic2j.core.api.PLibrary;
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.api.solver.holder.SolutionHolder;
import org.logic2j.core.api.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.core.impl.unify.BindingTrailTestUtils;
import org.logic2j.core.library.mgmt.LibraryContent;

/**
 * Base class for tests, initiazlize a fresh {@link PrologReferenceImplementation} on every method (level of init is
 * {@link InitLevel#L1_CORE_LIBRARY}, and
 * provides utility methods.
 */
public abstract class PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrologTestBase.class);

    protected static final File TEST_RESOURCES_DIR = new File("src/test/resources");

    /**
     * Will be initialized for every test method, as per {@link #initLevel()}
     */
    protected PrologImplementation prolog;

    /**
     * @return Use only the core library - no extra features loaded.
     */
    protected InitLevel initLevel() {
        return InitLevel.L1_CORE_LIBRARY;
    }

    /**
     * Initialize the engine as per the {@link #initLevel()} specified.
     */
    @Before
    public void setUp() {
        this.prolog = new PrologReferenceImplementation(initLevel());
        // Here we should NOT NEED to reset the BindingTrail stack - however in certain cases it's useful just to enable this
        BindingTrailTestUtils.reset();
        // Struct.ATOM_CATALOG.clear(); // While debugging - we may clear cache - requires public access
    }

    @After
    public void tearDown() {
        assertEquals("BindingTrail should be empty after any test method, checking stack size", 0, BindingTrailTestUtils.size());
        this.prolog = null;
    }

    /**
     * @return Our {@link PrologImplementation}. In normal application code it must be sufficient to use the {@link Prolog} but since here
     *         we test details of the implmenetation, we need more.
     */
    protected PrologImplementation getProlog() {
        return this.prolog;
    }

    /**
     * Make sure there is only one solution to goals.
     * 
     * @param theGoals All goals to check for
     * @return The UniqueSolutionHolder holding the last solution of theGoals
     */
    protected UniqueSolutionHolder assertOneSolution(CharSequence... theGoals) {
        UniqueSolutionHolder result = null;
        for (final CharSequence goal : theGoals) {
            logger.info("Expecting 1 solution when solving goal \"{}\"", goal);
            final SolutionHolder solution = this.prolog.solve(goal);
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
                this.prolog.solve(theGoal).number();
                fail("Goal should have failed and did not: \"" + theGoal + '"');
            } catch (final RuntimeException e) {
                // logger.warn("Goal failing - but expected: " + e, e);
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
            logger.info("Expecting {} solutions to solving goal \"{}\"", theNumber, goal);
            solve = this.prolog.solve(goal);
            assertEquals("Goal \"" + goal + "\" has different number of solutions", theNumber, solve.number());
        }
        return solve;
    }

    /**
     * Factory.
     * 
     * @param theObject
     * @return A single Term, corresponding to theObject.
     */
    protected Object term(Object theObject) {
        return this.prolog.getTermAdapter().term(theObject, FactoryMode.ANY_TERM);
    }

    /**
     * Factory.
     * 
     * @param theText
     * @return A single Term, corresponding to theObject.
     */
    protected Object term(CharSequence theText) {
        return this.prolog.getTermExchanger().unmarshall(theText);
    }

    /**
     * Utility factory.
     * 
     * @param elements The elements of the list to parse as Terms
     * @return A List of term, corresponding to the related elements passed as argument.
     */
    protected List<Object> termList(CharSequence... elements) {
        final List<Object> result = new ArrayList<Object>(elements.length);
        for (final CharSequence element : elements) {
            result.add(term(element));
        }
        return result;
    }

    /**
     * @param theFile To be loaded
     * 
     */
    private void loadTheory(File theFile) throws IOException {
        final TheoryManager manager = this.prolog.getTheoryManager();
        final TheoryContent load = manager.load(theFile);
        manager.addTheory(load);
        logger.debug("Loaded theory from: {}", theFile);
    }

    /**
     * Syntactic sugar to load a theory located in the src/test/resources directory.
     * Also wraps the checked {@link IOException} into a {@link PrologNonSpecificError} runtime exception in case of problem.
     * 
     * @param theTheoryFile
     */
    protected void loadTheoryFromTestResourcesDir(String theTheoryFile) {
        try {
            loadTheory(new File(TEST_RESOURCES_DIR, theTheoryFile));
        } catch (final IOException e) {
            // Avoid bothernig with checked IOException in our TestCases (since this is a helper method, let's help)
            throw new PrologNonSpecificError("Could not load Theory from " + theTheoryFile + ": " + e);
        }
    }

    protected File[] allTheoryFilesFromTestResourceDir() {
        final FilenameFilter filesOnly = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                final File file = new File(dir, name);
                return file.canRead() && file.isFile();
            }
        };
        return TEST_RESOURCES_DIR.listFiles(filesOnly);
    }

    protected LibraryContent loadLibrary(PLibrary theLibrary) {
        return this.prolog.getLibraryManager().loadLibrary(theLibrary);
    }

}
