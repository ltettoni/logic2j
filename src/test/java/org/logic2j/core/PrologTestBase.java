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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Before;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.library.LibraryContent;
import org.logic2j.core.api.library.PLibrary;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.engine.exception.Logic2jException;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.holder.GoalHolder;
import org.logic2j.engine.solver.listener.CountingSolutionListener;

/**
 * Base class for tests, initialize a fresh {@link org.logic2j.core.impl.PrologReferenceImplementation} on every method (level of init is
 * {@link org.logic2j.core.impl.PrologReferenceImplementation.InitLevel#L1_CORE_LIBRARY}), and
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
   * Fully initialize the engine as per the {@link #initLevel()} specified before every test method.
   */
  @Before
  public void initProlog() {
    this.prolog = new PrologReferenceImplementation(initLevel());
  }

  /**
   * @return Our {@link org.logic2j.core.impl.PrologImplementation}. In normal application code it must be sufficient to use the {@link org.logic2j.core.api.Prolog} but since here
   * we test details of the implementation, we need more.
   */
  protected PrologImplementation getProlog() {
    return this.prolog;
  }


  // ---------------------------------------------------------------------------
  // Low-lever solution counting assertions just using a CountingSolutionListener
  // ---------------------------------------------------------------------------

  protected void countOneSolution(CharSequence... theGoals) {
    countNSolutions(1, theGoals);
  }


  protected void countNoSolution(CharSequence... theGoals) {
    countNSolutions(0, theGoals);
  }

  protected void countNSolutions(int nbr, CharSequence... theGoals) {
    for (CharSequence goalText : theGoals) {
      Object term = unmarshall(goalText);
      final CountingSolutionListener listener = new CountingSolutionListener();
      getProlog().getSolver().solveGoal(term, listener);
      assertThat(listener.count()).as("Solving goalText \"" + goalText + '"').isEqualTo(nbr);
    }
  }


  /**
   * Will replace many test methods with JUnit's "expected=" feature
   *
   * @param theGoals
   */
  protected void assertGoalMustFail(CharSequence... theGoals) {
    assertThat(theGoals.length > 0).as("theGoals must not be empty for assertGoalMustFail()").isTrue();
    for (final CharSequence theGoal : theGoals) {
      try {
        this.prolog.solve(theGoal).isPresent();
          //noinspection MaskedAssertion
          fail("Goal should have failed and did not: \"" + theGoal + '"');
      } catch (final Logic2jException | AssertionError e) {
        // Expected
      }
    }
  }


  /**
   * Make sure there is only one solution to goals.
   *
   * @param theGoals All goals to check for
   * @return The GoalHolder holding the last solution of theGoals
   */
  protected GoalHolder uniqueSolution(CharSequence... theGoals) {
    return internalAssert(1, theGoals);
  }

  /**
   * Make sure there are exactly theNumber of solutions.
   *
   * @param theNumber
   * @param theGoals  All goals to check for
   * @return The {@link GoalHolder}
   */
  protected GoalHolder nSolutions(int theNumber, CharSequence... theGoals) {
    return internalAssert(theNumber, theGoals);
  }

  /**
   * Make sure there are no solutions to the goals.
   *
   * @param theGoals All goals to check for
   * @return The {@link GoalHolder}
   */
  protected void noSolutions(CharSequence... theGoals) {
    internalAssert(0, theGoals);
  }

  /**
   * Solves once to calculate result size. So watch out this may trigger one extra solution in case you follow this
   * call by obtaining a particular variable value - the engine will run twice!
   *
   * @param theNumber
   * @param theGoals
   * @return The {@link GoalHolder} resulting from solving the last goal (i.e. the first when only one...). Null if no goal specified.
   */
  private GoalHolder internalAssert(int theNumber, CharSequence... theGoals) {
    assertThat(theGoals.length > 0).as("theGoals must not be empty for countOneSolution()").isTrue();
    GoalHolder result = null;
    for (final CharSequence goal : theGoals) {
      logger.info("Expecting {} solution(s) when solving goal \"{}\"", theNumber, goal);
      result = this.prolog.solve(goal);
      // Now execute the goal - only extracting the number of solutions
      final int nbr = result.count();
      assertThat(nbr).as("Checking number of solutions for goal \"" + goal + '"').isEqualTo(theNumber);
    }
    return result;
  }

  /**
   * Utility to unmarshall terms - just a shortcut.
   *
   * @param theString
   * @return The unmarshalled object.
   */
  protected Object unmarshall(CharSequence theString) {
    return this.prolog.getTermUnmarshaller().unmarshall(theString);
  }


  /**
   * Factory.
   *
   * @param theObject
   * @return A single Term, corresponding to theObject.
   */
  protected Object term(Object theObject) {
    return this.prolog.getTermAdapter().toTerm(theObject, FactoryMode.ANY_TERM);
  }

  /**
   * Factory.
   *
   * @param theText
   * @return A single Term, corresponding to theObject.
   */
  protected Object term(CharSequence theText) {
    return unmarshall(theText);
  }

  /**
   * Create a Java list of Terms by parsing the arguments.
   *
   * @param elements The elements of the list to parse as Terms
   * @return A List of term, corresponding to the related elements passed as argument.
   */
  protected List<Object> termList(CharSequence... elements) {
    assertThat(elements.length > 0).as("elements must not be empty for termList()").isTrue();
    final List<Object> result = new ArrayList<>(elements.length);
    for (final CharSequence element : elements) {
      result.add(term(element));
    }
    return result;
  }


  protected String marshall(Iterable<Object> terms) {
    ArrayList<String> marshalled = new ArrayList<>();
    for (Object term : terms) {
      marshalled.add(marshall(term));
    }
    return marshalled.toString();
  }

  protected String marshall(Object term) {
    return getProlog().getTermMarshaller().marshall(term).toString();
  }

  /**
   * @param theFile To be loaded
   */
  private void loadTheory(File theFile) throws IOException {
    final TheoryManager manager = this.prolog.getTheoryManager();
    final TheoryContent load = manager.load(theFile);
    manager.addTheory(load);

    logger.debug("Loaded theory from: {}", theFile);
  }

  /**
   * Syntactic sugar to load a theory located in the src/test/resources directory.
   * Also wraps the checked {@link java.io.IOException} into a {@link PrologNonSpecificException} runtime exception in case of problem.
   *
   * @param theTheoryFile
   */
  protected void loadTheoryFromTestResourcesDir(String theTheoryFile) {
    try {
      loadTheory(new File(TEST_RESOURCES_DIR, theTheoryFile));
    } catch (final IOException e) {
      // Avoid bothering with checked IOException in our TestCases (since this is a helper method, let's help)
      throw new PrologNonSpecificException("Could not load Theory from " + theTheoryFile + ": " + e);
    }
  }

  protected File[] allTheoryFilesFromTestResourceDir() {
    final FilenameFilter filesOnly = (dir, name) -> {
      final File file = new File(dir, name);
      return file.canRead() && file.isFile() && file.getName().endsWith(".pro");
    };
    return TEST_RESOURCES_DIR.listFiles(filesOnly);
  }

  protected LibraryContent loadLibrary(PLibrary theLibrary) {
    return this.prolog.getLibraryManager().loadLibrary(theLibrary);
  }


  /**
   * @param goalHolder
   * @return All variables of a GoalHolder, ordered by name, converted to String.
   */
  protected String varsSortedToString(GoalHolder goalHolder) {
    final List<Map<Var<?>, Object>> listOfSortedMaps = new ArrayList<>();
    for (Map<Var<?>, Object> unorderedMap : goalHolder.vars().list()) {
      final TreeMap<Var<?>, Object> orderedMap = new TreeMap<>(Var.COMPARATOR_BY_NAME);
      orderedMap.putAll(unorderedMap);
      listOfSortedMaps.add(orderedMap);
    }
    return listOfSortedMaps.toString();
  }


  protected ExtractingSolutionListener solveWithExtractingListener(Object goal) {
    final ExtractingSolutionListener listener = new ExtractingSolutionListener(goal);
    getProlog().getSolver().solveGoal(goal, listener);
    listener.report();
    return listener;
  }
}
