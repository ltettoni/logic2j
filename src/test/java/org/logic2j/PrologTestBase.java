package org.logic2j;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.logic2j.PrologImpl;
import org.logic2j.PrologImplementor;
import org.logic2j.PrologImpl.InitLevel;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.library.impl.pojo.PojoLibrary;
import org.logic2j.library.mgmt.LibraryContent;
import org.logic2j.model.prim.PLibrary;
import org.logic2j.model.symbol.Term;
import org.logic2j.solve.SolutionHolder;
import org.logic2j.solve.SolutionHolder.MultipleSolutionsHolder;
import org.logic2j.solve.SolutionHolder.UniqueSolutionHolder;
import org.logic2j.theory.TheoryManager;

/**
 * Base class for tests.
 *
 */
public abstract class PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrologTestBase.class);

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

  protected UniqueSolutionHolder assertOneSolution(CharSequence... theGoals) {
    SolutionHolder.UniqueSolutionHolder result = null;
    for (int i = 0; i < theGoals.length; i++) {
      final CharSequence goal = theGoals[i];
      logger.info("Expecting 1 solution when solving goal \"{}\"", goal);
      SolutionHolder solve = getProlog().solve(goal);
      result = solve.unique();
      //      assertEquals(theNumber, result.number());
    }
    return result;
    //    return assertInternal(1, theGoals).unique();
  }

  protected SolutionHolder assertNoSolution(CharSequence... theGoals) {
    return assertInternal(0, theGoals);
  }

  protected MultipleSolutionsHolder assertNSolutions(int theNumber, CharSequence... theGoals) {
    return assertInternal(theNumber, theGoals).all();
  }

  protected void assertGoalMustFail(CharSequence... theGoals) {
    for (int i = 0; i < theGoals.length; i++) {
      try {
        getProlog().solve(theGoals[i]).number();
        fail("Goal should have failed and did not: \"" + theGoals + '"');
      } catch (RuntimeException e) {
        // Normal
      }
    }
  }

  /**
   * @param theNumber
   * @param theGoals
   * @return The {@link SolutionHolder} resulting from solving the last 
   * goal (i.e. the first when only one...). Null if no goal specified.
   */
  private SolutionHolder assertInternal(int theNumber, CharSequence... theGoals) {
    SolutionHolder solve = null;
    for (int i = 0; i < theGoals.length; i++) {
      final CharSequence goal = theGoals[i];
      logger.info("Expecting {} solutions to solving goal \"{}\"", theNumber, goal);
      solve = getProlog().solve(goal);
      assertEquals(theNumber, solve.number());
    }
    return solve;
  }

  /**
   * Factory.
   * @param theObject
   * @return A single Term, corresponding to theObject.
   */
  protected Term term(Object theObject) {
    return getProlog().getTermFactory().create(theObject, FactoryMode.ANY_TERM);
  }

  /**
   * Utility factory.
   * @param elements The elements of the list to parse as Terms
   * @return A List of term, corresponding to the related elements passed as argument.
   */
  protected List<Term> termList(Object... elements) {
    final List<Term> result = new ArrayList<Term>(elements.length);
    for (int i = 0; i < elements.length; i++) {
      result.add(term(elements[i]));
    }
    return result;
  }

  /**
   * @param theFilename A String denoting the file path, should be a {@link File},
   * yes it's inelegant, but this is a test helper!
   * @throws IOException 
   */
  protected void addTheory(String theFilename) throws IOException {
    TheoryManager mgr = getProlog().getTheoryManager();
    mgr.addTheory(mgr.load(new File(theFilename)));
  }

  protected LibraryContent loadLibrary(PLibrary theLibrary) {
    return getProlog().getLibraryManager().loadLibrary(theLibrary);
  }

  /**
   * Helper method for test cases: bind a Java object by name.
   * @param theKey
   * @param theValue
   */
  protected void bind(String theKey, Object theValue) {
    // At the moment where the bind predicate is...
    //    final PojoLibrary library = new PojoLibrary(getProlog());
    //    loadLibrary(library);
    //    library.bind(key, value);
    PojoLibrary.bind(theKey, theValue);
  }

}