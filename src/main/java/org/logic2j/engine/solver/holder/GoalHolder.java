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
package org.logic2j.engine.solver.holder;

import org.logic2j.engine.solver.Solver;
import org.logic2j.engine.solver.extractor.ObjectFactory;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.listener.CountingSolutionListener;
import org.logic2j.engine.solver.listener.RangeSolutionListener;
import org.logic2j.core.impl.PrologReferenceImplementation;

import java.util.Map;

/**
 * An intermediate class in the fluent API to extract solutions; a GoalHolder holds the state of what the user
 * wishes to calculate, being either the existence of, the number of solutions, or individual or multiple values
 * of one particular variable or of all vars.
 * This object will launch the solver only for methods exists() or count(). For other
 * methods it just returns instances of SolutionHolder which further delay the execution.
 */
public class GoalHolder {
  final PrologReferenceImplementation prolog;

  final Object goal;

  public GoalHolder(PrologReferenceImplementation prolog, Object theGoal) {
    this.prolog = prolog;
    this.goal = theGoal;
  }

  public boolean exists() {
    final RangeSolutionListener listener = new RangeSolutionListener();
    listener.setMaxCount(2); // We won't get there - but we don't want to put a max to 1 otherwise an Exception will be thrown
    listener.setMaxFetch(1);
    prolog.getSolver().solveGoal(goal, listener);
    return listener.getNbSolutions() >= 1;
  }

  public long count() {
    final CountingSolutionListener listener = new CountingSolutionListener();
    prolog.getSolver().solveGoal(goal, listener);
    return listener.count();
  }

  /**
   * @return Solution to the whole goal. If the goal was a(X), will return a(1), a(2), etc.
   */
  public SolutionHolder<Object> solution() {
    return new SolutionHolder<Object>(this, Var.WHOLE_SOLUTION_VAR_NAME, Object.class, this.prolog.getTermAdapter()::fromTerm);
  }

  /**
   * Seek solutions for only one variable of the goal, of the desired type. Does not yet execute the goal.
   *
   * @param varName             The name of the variable to solve for.
   * @param desiredTypeOfResult
   * @param <T>
   * @return A SolutionHolder for only the specified variable.
   */
  public <T> SolutionHolder<T> var(String varName, Class<? extends T> desiredTypeOfResult) {
    final SolutionHolder<T> solutionHolder = new SolutionHolder<T>(this, varName, desiredTypeOfResult, this.prolog.getTermAdapter()::fromTerm);
    return solutionHolder;
  }

  /**
   * Seek solutions for onle one variable of the goal, of any type.
   *
   * @param varName The name of the variable to solve for.
   * @return A SolutionHolder for only the specified variable.
   */
  public SolutionHolder<Object> var(String varName) {
    return var(varName, Object.class);
  }

  public SolutionHolder<Map<Var, Object>> vars() {
    return SolutionHolder.extractingMaps(this);
  }

  /**
   * @return A SolutionHolder that returns solutions as array of Objects
   */
  public SolutionHolder<Object[]> varsArray() {
    return SolutionHolder.extractingArrays(this);
  }

  /**
   * Instantiate objects directly.
   *
   * @param factory
   * @param <T>     Type of objects to create
   * @return A SolutionHolder for objects created by the factory
   */
  public <T> SolutionHolder<T> varsToFactory(ObjectFactory<T> factory) {
    return SolutionHolder.extractingFactory(this, factory);
  }


  // ---------------------------------------------------------------------------
  // Syntactic sugars
  // ---------------------------------------------------------------------------


  public Object intValue(String varName) {
    return var(varName, Integer.class).unique();
  }

  public String toString(String varName) {
    return var(varName).unique().toString();
  }

  public Object getGoal() {
    return goal;
  }

  public Solver getSolver() {
    return this.prolog.getSolver();
  }
}
