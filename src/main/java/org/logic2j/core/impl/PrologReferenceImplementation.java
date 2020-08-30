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
package org.logic2j.core.impl;

import static org.logic2j.engine.model.TermApiLocator.termApiExt;

import org.logic2j.core.api.LibraryManager;
import org.logic2j.core.api.OperatorManager;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermMapper;
import org.logic2j.core.api.TermMarshaller;
import org.logic2j.core.api.TermUnmarshaller;
import org.logic2j.core.api.library.PLibrary;
import org.logic2j.core.impl.theory.DefaultTheoryManager;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.core.library.DefaultLibraryManager;
import org.logic2j.core.library.impl.CoreLibrary;
import org.logic2j.core.library.impl.IOLibrary;
import org.logic2j.engine.predicates.impl.generator.Digit;
import org.logic2j.engine.predicates.impl.math.compare.GE;
import org.logic2j.engine.predicates.impl.math.compare.GT;
import org.logic2j.engine.predicates.impl.math.compare.LE;
import org.logic2j.engine.predicates.impl.math.compare.LT;
import org.logic2j.engine.predicates.impl.math.function.Format;
import org.logic2j.engine.predicates.impl.math.function.LowerCase;
import org.logic2j.engine.predicates.impl.math.function.UpperCase;
import org.logic2j.engine.solver.holder.GoalHolder;

/**
 * Reference implementation of logic2j's {@link PrologImplementation} API.
 */
public class PrologReferenceImplementation implements PrologImplementation {

  /**
   * Describe the level of initialization of the Prolog system, between bare minimum, and full-featured.
   * This enum is not in the {@link PrologImplementation} interface since it's particular to the reference implementation.
   */
  public enum InitLevel {
    /**
     * A completely bare prolog engine, not functional for running programs (misses core predicates such as true/1, fail/1, !/1 (cut),
     * =/2, call/1, not/1, etc. Yet unification and inference of user predicates will work. No libraries loaded at all.
     */
    L0_BARE,
    /**
     * This is the default initialization level, it loads the {@link org.logic2j.core.library.impl.CoreLibrary},
     * containing (among others), core predicates such as
     * true/1, fail/1, !/1 (cut), =/2, call/1, not/1, findall/3, etc.
     */
    L1_CORE_LIBRARY,
    /**
     * Higher level libraries loaded, such as {@link org.logic2j.core.library.impl.IOLibrary}.
     */
    L2_BASE_LIBRARIES,
  }

  // ---------------------------------------------------------------------------
  // Define all sub-features of this reference implementation and initialize with default, reference implementations
  // ---------------------------------------------------------------------------

  private TermAdapter termAdapter = new DefaultTermAdapter();

  private TermMarshaller termMarshaller = new DefaultTermMarshaller();

  private final DefaultTermUnmarshaller termUnmarshaller = new DefaultTermUnmarshaller();

  private final TheoryManager theoryManager = new DefaultTheoryManager(this);

  private LibraryManager libraryManager = new DefaultLibraryManager(this);

  private OperatorManager operatorManager = new DefaultOperatorManager();

  private Solver solver = new Solver(this);

  /**
   * Default constructor will only provide an engine with the {@link org.logic2j.core.library.impl.CoreLibrary} loaded.
   */
  public PrologReferenceImplementation() {
    this(InitLevel.L1_CORE_LIBRARY);
  }

  /**
   * Constructor for a specific level of initialization.
   *
   * @param theLevel
   */
  public PrologReferenceImplementation(InitLevel theLevel) {
    // Here we load libs, watch out for the order

    // First we load libraries that define primitives, and only after libraries that define theories
    // This is because at the time a theory is parsed, it will initialize PrimitiveInfo only to those
    // primitive that have already been loaded
    if (theLevel.ordinal() >= InitLevel.L2_BASE_LIBRARIES.ordinal()) {
      final PLibrary lib = new IOLibrary(this);
      this.libraryManager.loadLibrary(lib);
    }
    if (theLevel.ordinal() >= InitLevel.L1_CORE_LIBRARY.ordinal()) {
      final PLibrary lib = new CoreLibrary(this);
      this.libraryManager.loadLibrary(lib);

      this.libraryManager.wholeContent().addFOPredicateFactory(
              Digit::valueOf,
              LT::valueOf, LE::valueOf, GT::valueOf , GE::valueOf,
              LowerCase::valueOf, UpperCase::valueOf,
              Format::valueOf);
    }
    final TermMapper normalizer = theTerm -> termApiExt().normalize(theTerm, getLibraryManager().wholeContent());
    this.termUnmarshaller.setNormalizer(normalizer);
    this.termUnmarshaller.setOperatorManager(getOperatorManager());
    ((DefaultTermAdapter) this.termAdapter).setNormalizer(normalizer);
  }

  // ---------------------------------------------------------------------------
  // Implementation of interface Prolog.
  // These are actually shortcuts or "syntactic sugars".
  // ---------------------------------------------------------------------------

  @Override
  public GoalHolder solve(CharSequence theGoal) {
    final Object parsed = termUnmarshaller.unmarshall(theGoal);
    return solve(parsed);
  }


  @Override
  public GoalHolder solve(Object theGoal) {
    return new GoalHolder(this.getSolver(), theGoal, this.getTermAdapter()::fromTerm);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // You may use DI to inject all sub-features into setters
  // TODO I think that we probably have to reinit/load the full engine's configuration every time one setter is called, but we lack an
  // "afterPropertiesSet()" feature. We should use the standard @PostConstruct instead.
  // ---------------------------------------------------------------------------

  @Override
  public TermAdapter getTermAdapter() {
    return this.termAdapter;
  }

  @Override
  public void setTermAdapter(TermAdapter theTermAdapter) {
    this.termAdapter = theTermAdapter;
  }

  @Override
  public TheoryManager getTheoryManager() {
    return this.theoryManager;
  }

  @Override
  public LibraryManager getLibraryManager() {
    return this.libraryManager;
  }

  public void setLibraryManager(LibraryManager theLibraryManager) {
    this.libraryManager = theLibraryManager;
  }

  @Override
  public Solver getSolver() {
    return this.solver;
  }

  @Override
  public OperatorManager getOperatorManager() {
    return this.operatorManager;
  }

  public TermMarshaller getTermMarshaller() {
    return this.termMarshaller;
  }

  public void setTermMarshaller(TermMarshaller newOne) {
    this.termMarshaller = newOne;
  }

  @Override
  public TermUnmarshaller getTermUnmarshaller() {
    return this.termUnmarshaller;
  }

  public void setOperatorManager(OperatorManager theOperatorManager) {
    this.operatorManager = theOperatorManager;
  }

  public void setSolver(Solver theSolver) {
    this.solver = theSolver;
  }


  // ---------------------------------------------------------------------------
  // Methods of java.lang.Object
  // ---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }


}
