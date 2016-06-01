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
package org.logic2j.core.api.solver.extractor;

import org.logic2j.core.ObjectFactory;
import org.logic2j.core.api.unify.UnifyContext;

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
