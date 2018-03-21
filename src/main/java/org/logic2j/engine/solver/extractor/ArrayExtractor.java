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
package org.logic2j.engine.solver.extractor;

import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.unify.UnifyContext;

/**
 * A {@link SolutionExtractor} that will extract values of
 * a set of variables, returned as an Array, indiced by every Var's index.
 * Typically used to find all bindings of a multi-variable goal, in a very efficient way.
 */
public class ArrayExtractor implements SolutionExtractor<Object[]> {

  private final Var<?>[] vars;
  private final int highestIndex;

  public ArrayExtractor(Object goal) {
    int high = 0;
    this.vars = TermApi.distinctVars(goal);
    for (Var<?> var : this.vars) {
      high = Math.max(high, var.getIndex());
    }
    this.highestIndex = high;
  }


  /**
   * @param currentVars
   * @return Actually a HashMap, meaning there is no particular order in the Var keys.
   */
  @Override
  public Object[] extractSolution(UnifyContext currentVars) {
    final Object[] result = new Object[this.highestIndex + 1];
    for (Var<?> var : this.vars) {
      final Object value = currentVars.reify(var);
      result[var.getIndex()] = value;
    }
    return result;
  }
}
