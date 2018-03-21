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

import org.logic2j.core.ObjectFactory;
import org.logic2j.engine.unify.UnifyContext;

/**
 * A SolutionExtractor that will delegate the extraction to an ArrayExtractor and then
 * use the specified factory method valueOf() to instantiate an POJO from the array
 * of objects.
 */
public class FactoryExtractor<T> implements SolutionExtractor<T> {
  /**
   * We will delegate the extraction to a regular ArrayExtractor, to come up with
   * Object[] first.
   */
  private final ArrayExtractor wrappedExtractor;
  private final ObjectFactory<T> factory;

  public FactoryExtractor(Object goal, ObjectFactory<T> factory) {
    this.wrappedExtractor = new ArrayExtractor(goal);
    this.factory = factory;
  }

  @Override
  public T extractSolution(UnifyContext currentVars) {
    final Object[] solution = this.wrappedExtractor.extractSolution(currentVars);
    return this.factory.valueOf(solution);
  }
}
