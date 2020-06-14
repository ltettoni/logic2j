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

package org.logic2j.contrib.pojo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.logic2j.contrib.library.pojo.PojoLibrary;
import org.logic2j.core.PrologTestBase;
import org.logic2j.engine.model.Struct;

import java.awt.*;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicClauseProviderTest extends PrologTestBase {

  private DynamicClauseProvider dynamic;

  @Before
  public void init() {
    this.dynamic = new DynamicClauseProvider(getProlog());
    getProlog().getTheoryManager().addClauseProvider(this.dynamic);
    getProlog().getLibraryManager().loadLibrary(new PojoLibrary(getProlog()));
  }

  @After
  public void retractAll() {
    this.dynamic.retractAll();
    // Make sure the next assertion would return an index of zero
    final int index = this.dynamic.assertClause("one");
    assertThat(index).isEqualTo(0);
    this.dynamic.retractAll();
  }

  @Test
  public void assertSingleAtomFact() {
    noSolutions("aYetUnknownFact");
    // Assert
    final Object theFact = "aYetUnknownFact";
    final int index = this.dynamic.assertClause(theFact); // Note: we use another string!
    assertThat(index).isEqualTo(0);
    uniqueSolution("" + "aYetUnknownFact"); // Add empty string to make another reference
    noSolutions("aStillUnknownFact");
    // Retract
    this.dynamic.retractFactAt(index); // Note: each time we use another string!
    noSolutions("" + "aYetUnknownFact");
    noSolutions("aStillUnknownFact");
  }

  @Test
  public void assertStructFact() {
    noSolutions("zz(X)");
    // Assert
    final Struct fact1 = new Struct("zz", 11);
    final int index1 = this.dynamic.assertClause(fact1);
    assertThat(index1).isEqualTo(0);
    assertThat(uniqueSolution("zz(Q)").var("Q").unique()).isEqualTo(11);
    // Assert
    final Struct fact2 = new Struct("zz", 22);
    final int index2 = this.dynamic.assertClause(fact2);
    assertThat(index2).isEqualTo(1);
    assertThat(nSolutions(2, "zz(Q)").var("Q").list()).isEqualTo(Arrays.asList(11, 22));
    // Retract
    this.dynamic.retractFactAt(index1);
    assertThat(uniqueSolution("zz(Q)").var("Q").unique()).isEqualTo(22);
    this.dynamic.retractFactAt(index2);
    noSolutions("zz(X)");
  }

  @Test
  public void assertObjectFact() {
    noSolutions("zz(X)");
    // Assert
    final Object awtRectangle = new Rectangle(1, 2, 3, 4);
    final Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
    final int index1 = this.dynamic.assertClause(fact1);
    assertThat(index1).isEqualTo(0);
    assertThat(uniqueSolution("eav(_, _, X)").var("X").unique()).isEqualTo(awtRectangle);
    assertThat(uniqueSolution("eav(_,_,X)").var("X").unique()).isEqualTo(new Rectangle(1, 2, 3, 4));
    // Retract
    this.dynamic.retractFactAt(index1);
    noSolutions("eav(_,_,X)");
  }

  @Test
  public void assertObjectFactProperties() {
    // Assert
    final Object awtRectangle = new Rectangle(1, 2, 3, 4);
    final Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
    this.dynamic.assertClause(fact1);
    assertThat(uniqueSolution("eav(_,_,Rect)").var("Rect").unique()).isEqualTo(new Rectangle(1, 2, 3, 4));
    assertThat(uniqueSolution("eav(_,_,Rect), property(Rect, width, W)").var("W").unique()).isEqualTo(3.0);
    uniqueSolution("eav(_,_,Rect), property(Rect, height, 4.0)");
    noSolutions("eav(_,_,Rect), property(Rect, height, 3.0)");
  }

  @Test
  public void retractToIndex() {
    noSolutions("iter");
    // Assert first range
    for (int i = 0; i < 50; i++) {
      this.dynamic.assertClause("iter");
    }
    nSolutions(50, "iter");
    final int middleIndex = this.dynamic.assertClause("iter");
    nSolutions(51, "iter");
    // Assert second range
    for (int i = 0; i < 50; i++) {
      this.dynamic.assertClause("iter");
    }
    nSolutions(101, "iter");
    // Retract to middle
    this.dynamic.retractToBeforeIndex(middleIndex);
    nSolutions(50, "iter");
    this.dynamic.retractToBeforeIndex(5);
    nSolutions(5, "iter");
    this.dynamic.retractToBeforeIndex(0);
    noSolutions("iter");
  }
}
