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
package org.logic2j.engine.solver.listener;

import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.extractor.SolutionExtractor;
import org.logic2j.engine.unify.UnifyContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link SolutionListener} that will count and limit
 * the number of solutions generated, and possibly handle underflow or overflow.
 */
public class MultiVarSolutionListener extends RangeSolutionListener {
  private final SolutionExtractor<Map<Var, Object>> extractor;
  private final List<Map<Var, Object>> results;

  /**
   * Create a {@link SolutionListener} that will enumerate
   * solutions up to theMaxCount before aborting by "user request". We will usually
   * supply 1 or 2, see derived classes.
   */
  public MultiVarSolutionListener(SolutionExtractor<Map<Var, Object>> extractor) {
    this.extractor = extractor;
    this.results = new ArrayList<Map<Var, Object>>();
  }


  @Override
  public Integer onSolution(UnifyContext currentVars) {
    results.add(extractor.extractSolution(currentVars));
    return super.onSolution(currentVars);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------


  public List<Map<Var, Object>> getResults() {
    return results;
  }


}
