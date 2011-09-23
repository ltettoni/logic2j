package org.logic2j;

import java.util.ArrayList;
import java.util.List;

import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.io.format.DefaultFormatter;
import org.logic2j.io.operator.DefaultOperatorManager;
import org.logic2j.io.operator.OperatorManager;
import org.logic2j.io.parse.DefaultTermFactory;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.impl.core.CoreLibrary;
import org.logic2j.library.impl.io.IOLibrary;
import org.logic2j.library.mgmt.DefaultLibraryManager;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.DefaultGoalSolver;
import org.logic2j.solve.GoalSolver;
import org.logic2j.solve.SolutionHolder;
import org.logic2j.theory.DefaultTheoryManager;
import org.logic2j.theory.TheoryManager;
import org.logic2j.unify.DefaultUnifyer;
import org.logic2j.unify.Unifyer;
import org.logic2j.util.ReflectUtils;
import org.logic2j.util.ReportUtils;

/**
 * Root Prolog API implementation.
 *
 */
public class PrologImpl implements PrologImplementor {
  /**
   * Describe the level of initialization of the Prolog system, between
   * bare minimal, and full-featured.
   */
  public enum InitLevel {
    /**
     * A completely bare prolog engine, not functional for running programs (misses
     * base predicates such as true/1, fail/1, !/1 (cut), =/2, call/1, not/1, etc. But unification and 
     * inference of user predicates do work.
     * No libraries loaded at all.
     */
    L0_BARE,
    /**
     * This is the default initialization level, it loads the {@link CoreLibrary},
     * containing (among others), core predicates such as true/1, fail/1, !/1 (cut), =/2, call/1, not/1, etc.
     */
    L1_CORE_LIBRARY,
    /**
     * Higher level libraries loaded, such as {@link IOLibrary}.
     */
    L2_BASE_LIBRARIES,
  }

  private TermFactory termFactory = new DefaultTermFactory(this);
  private Formatter formatter = new DefaultFormatter(this);
  private LibraryManager libraryManager = new DefaultLibraryManager(this);
  private OperatorManager operatorManager = new DefaultOperatorManager();
  private GoalSolver solver = new DefaultGoalSolver(this);
  private Unifyer unifyer = new DefaultUnifyer();
  // TODO Does the clauseProviders belong here or from the GoalSolver where they are solely used???
  // Note: the first ClauseProvider (at index 0) is always the TheoryManager.
  private List<ClauseProvider> clauseProviders = new ArrayList<ClauseProvider>();

  public PrologImpl() {
    this(InitLevel.L1_CORE_LIBRARY);
  }

  public PrologImpl(InitLevel theLevel) {
    // The first clause provider is always the TheoryManager. Others may be added.
    final TheoryManager tm = new DefaultTheoryManager(this);
    this.clauseProviders.add(tm);
    if (theLevel.ordinal() >= InitLevel.L1_CORE_LIBRARY.ordinal()) {
      final LibraryBase lib = new CoreLibrary(this);
      this.libraryManager.loadLibrary(lib);
    }
    if (theLevel.ordinal() >= InitLevel.L2_BASE_LIBRARIES.ordinal()) {
      final IOLibrary lib = new IOLibrary(this);
      this.libraryManager.loadLibrary(lib);
    }
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  @Override
  public OperatorManager getOperatorManager() {
    return this.operatorManager;
  }

  @Override
  public LibraryManager getLibraryManager() {
    return this.libraryManager;
  }

  @Override
  public TheoryManager getTheoryManager() {
    return ReflectUtils.safeCastNotNull("getting TheoryManager as first ClauseProvider", this.clauseProviders.get(0),
        TheoryManager.class);
  }

  @Override
  public TermFactory getTermFactory() {
    return this.termFactory;
  }

  @Override
  public Formatter getFormatter() {
    return this.formatter;
  }

  /**
   * @return the solver
   */
  @Override
  public GoalSolver getSolver() {
    return this.solver;
  }

  /**
   * @param theSolver the solver to set
   */
  public void setSolver(GoalSolver theSolver) {
    this.solver = theSolver;
  }

  /**
   * @return the unifyer
   */
  @Override
  public Unifyer getUnifyer() {
    return this.unifyer;
  }

  /**
   * @param theUnifyer the unifyer to set
   */
  public void setUnifyer(Unifyer theUnifyer) {
    this.unifyer = theUnifyer;
  }

  /**
   * @return the clauseProviders
   */
  @Override
  public List<ClauseProvider> getClauseProviders() {
    return this.clauseProviders;
  }

  /**
   * @param theClauseProviders the clauseProviders to set
   */
  public void setClauseProviders(List<ClauseProvider> theClauseProviders) {
    this.clauseProviders = theClauseProviders;
  }

  //---------------------------------------------------------------------------
  // Helper methods
  //---------------------------------------------------------------------------

  @Override
  public Term term(Object theSource) {
    return getTermFactory().create(theSource, FactoryMode.ANY_TERM);
  }

  @Override
  public SolutionHolder solve(CharSequence theGoal) {
    // Perhaps we could transform the goal into a real clause?
    final Term parsed = term(theGoal);
    final VarBindings vars = new VarBindings(parsed);
    return new SolutionHolder(this, parsed, vars);
  }

  @Override
  public String toString() {
    return ReportUtils.shortDescription(this);
  }

}