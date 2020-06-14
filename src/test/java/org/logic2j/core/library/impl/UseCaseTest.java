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

package org.logic2j.core.library.impl;

import org.junit.Test;
import org.logic2j.contrib.helper.FluentPrologBuilder;
import org.logic2j.core.api.Prolog;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrate simple use cases for Logic2j.
 */
public class UseCaseTest {

  @Test
  public void instantiateViaFactory() {
    final Prolog prolog = new FluentPrologBuilder().build();
    assertThat(prolog).isNotNull();
  }


  @Test
  public void configureDefaultLibs() {
    assertThat(new FluentPrologBuilder().withoutLibraries(false).isNoLibraries()).isFalse();
    assertThat(new FluentPrologBuilder().withoutLibraries(true).isNoLibraries()).isTrue();
    assertThat(new FluentPrologBuilder().withCoreLibraries(false).isCoreLibraries()).isFalse();
    assertThat(new FluentPrologBuilder().withCoreLibraries(true).isCoreLibraries()).isTrue();
  }

  @Test
  public void obtainDefaultPrologAndSolve() {
    final Prolog prolog = new FluentPrologBuilder().build();
    assertThat(prolog.solve("member(X, [a,b,c,d])").count()).isEqualTo(4);
  }


  @Test
  public void loadTheoryAndSolve() {
    final File th1 = new File("src/test/resources/queens.pro");
    final File th2 = new File("src/test/resources/hanoi.pro");
    final Prolog prolog = new FluentPrologBuilder().withTheory(th1, th2).build();
    assertThat(prolog.solve("queens(4, _)").count()).isEqualTo(2);
  }


  @Test
  public void solve() {
    final Prolog prolog = new FluentPrologBuilder()
            .withTheory(new File("src/test/resources/queens.pro"))
            .build();
    final List<List> objectList = prolog.solve("queens(4, Q)").var("Q", List.class).list();
    assertThat(objectList.size()).isEqualTo(2);
    assertThat(objectList.get(0).toString()).isEqualTo("[3, 1, 4, 2]");
    assertThat(objectList.get(1).toString()).isEqualTo("[2, 4, 1, 3]");
  }
}
