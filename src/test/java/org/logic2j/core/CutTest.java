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

import org.junit.Test;
import org.logic2j.core.impl.PrologReferenceImplementation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the cut and user abort features.
 */
public class CutTest extends PrologTestBase {

  @Override
  protected PrologReferenceImplementation.InitLevel initLevel() {
    // In case we use logging predicates we need the IO Library
    return PrologReferenceImplementation.InitLevel.L2_BASE_LIBRARIES;
  }

  @Test
  public void withoutCut() {
    loadTheoryFromTestResourcesDir("test-cut1.pro");
    assertThat(nSolutions(7, "s(X,Y)").vars().list().toString()).isEqualTo("[{X=1, Y=1}, {X=1, Y=2}, {X=1, Y=3}, {X=2, Y=1}, {X=2, Y=2}, {X=2, Y=3}, {X=0, Y=0}]");
  }

  @Test
  public void withCut() {
    loadTheoryFromTestResourcesDir("test-cut2.pro");
    assertThat(nSolutions(4, "s(X,Y)").vars().list().toString()).isEqualTo("[{X=1, Y=1}, {X=1, Y=2}, {X=1, Y=3}, {X=0, Y=0}]");
  }

  @Test
  public void basicOr() {
    loadTheoryFromTestResourcesDir("test-cut3.pro");
    nSolutions(2, "';'(th, el)");
  }

  @Test
  public void orPreceededByCut() {
    loadTheoryFromTestResourcesDir("test-cut3.pro");
    nSolutions(2, "!, ';'(th, el)");
  }

  @Test
  public void orFollowedByCut() {
    loadTheoryFromTestResourcesDir("test-cut3.pro");
    nSolutions(1, "';'(th, el), !");
  }

  @Test
  public void ifThenElseTrue() {
    loadTheoryFromTestResourcesDir("test-cut3.pro");
    nSolutions(1, "tt -> th ; el");
  }

  @Test
  public void ifThenElseFalse() {
    loadTheoryFromTestResourcesDir("test-cut3.pro");
    nSolutions(1, "ff -> th ; el");
  }
}
