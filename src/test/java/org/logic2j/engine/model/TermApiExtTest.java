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
package org.logic2j.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.logic2j.engine.model.TermApiLocator.termApiExt;

import org.junit.Test;
import org.logic2j.engine.exception.InvalidTermException;

/**
 * Low-level tests of the {@link TermApi} facade.
 */
public class TermApiExtTest {

  @Test
  public void selectTerm() {
    final Object arg0 = Struct.valueOf("b", "c", "c2");
    final Struct<?> term = Struct.valueOf("a", arg0, "b2");
    //        unmarshall("a(b(c,c2),b2)");
    //
    assertThat(termApiExt().selectTerm(term, "", Struct.class)).isEqualTo(term);
    assertThat(termApiExt().selectTerm(term, "a", Struct.class)).isEqualTo(term);
    try {
      termApiExt().selectTerm(term, "a[-1]", Struct.class);
      fail("Should fail");
    } catch (final InvalidTermException e) {
      // OK
    }
    //
    try {
      termApiExt().selectTerm(term, "a[0]", Struct.class);
      fail("Should fail");
    } catch (final InvalidTermException e) {
      // OK
    }
    //
    try {
      termApiExt().selectTerm(term, "a[4]", Struct.class);
      fail("Should fail");
    } catch (final InvalidTermException e) {
      // OK
    }
    //
    try {
      termApiExt().selectTerm(term, "z", Struct.class);
      fail("Should fail");
    } catch (final InvalidTermException e) {
      // OK
    }
    //
    final Struct<?> sTerm = term;
    assertThat(termApiExt().selectTerm(term, "a/", Struct.class)).isEqualTo(arg0);
    assertThat(termApiExt().selectTerm(term, "a[1]", Struct.class)).isEqualTo(arg0);
    assertThat(termApiExt().selectTerm(term, "[1]", Struct.class)).isEqualTo(arg0);
    assertThat(termApiExt().selectTerm(term, "a/b", Struct.class)).isEqualTo(arg0);
    assertThat(termApiExt().selectTerm(term, "a[1]/b", Struct.class)).isEqualTo(arg0);
    assertThat(termApiExt().selectTerm(term, "a[2]", String.class)).isEqualTo("b2");
    assertThat(termApiExt().selectTerm(term, "a[2]/b2", String.class)).isEqualTo("b2");
    assertThat(termApiExt().selectTerm(term, "a/b/c", String.class)).isEqualTo("c");
    assertThat(termApiExt().selectTerm(term, "a/b[1]", String.class)).isEqualTo("c");
    assertThat(termApiExt().selectTerm(term, "a/[1]", String.class)).isEqualTo("c");
    assertThat(termApiExt().selectTerm(term, "a/b[2]", String.class)).isEqualTo("c2");
  }

}
