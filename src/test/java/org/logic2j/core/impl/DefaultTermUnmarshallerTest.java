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

import static org.assertj.core.api.Assertions.assertThat;
import static org.logic2j.core.impl.DefaultTermMarshallerTest.MARSHALLER;

import org.junit.Test;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test both the DefaultTermUnmarshaller and DefaultTermMarshaller
 */
public class DefaultTermUnmarshallerTest {
  private static final Logger logger = LoggerFactory.getLogger(DefaultTermUnmarshallerTest.class);

  static final DefaultTermUnmarshaller UNMARSHALLER = new DefaultTermUnmarshaller();

  @Test
  public void numbers() {
    assertThat(UNMARSHALLER.unmarshall("2323")).isEqualTo(2323);
    assertThat(UNMARSHALLER.unmarshall("3.14")).isEqualTo(3.14);
    assertThat(UNMARSHALLER.unmarshall("2323L")).isEqualTo(2323L);
    assertThat(UNMARSHALLER.unmarshall("3.14f")).isEqualTo(3.14f);
  }


  @Test
  public void basicTerms() {
    assertThat(UNMARSHALLER.unmarshall("a")).isEqualTo("a");
    assertThat(UNMARSHALLER.unmarshall("X") instanceof Var<?>).isTrue();
    assertThat(UNMARSHALLER.unmarshall("_")).isEqualTo(Var.anon());
  }

  @Test
  public void struct() {
    assertThat(MARSHALLER.marshall(UNMARSHALLER.unmarshall("f(a)"))).isEqualTo("f(a)");
    assertThat(MARSHALLER.marshall(UNMARSHALLER.unmarshall("f(1, 3.14, a, X)"))).isEqualTo("f(1, 3.14, a, X)");
  }

  @Test(expected = InvalidTermException.class)
  public void emptyString() {
    UNMARSHALLER.unmarshall("");
  }


  @Test(expected = InvalidTermException.class)
  public void nullString() {
    UNMARSHALLER.unmarshall(null);
  }

  @Test
  public void normalization() {
    final Struct<?> term = (Struct<?>) UNMARSHALLER.unmarshall("f(a(1,2,X), Y, X, a(1,2,X))");
    logger.info("raw: {}", term);
    assertThat(term.getArg(3)).isEqualTo(term.getArg(0));
  }


  @Test
  public void spaces() {
    assertThat(UNMARSHALLER.unmarshall("' txt  '")).isEqualTo(" txt  ");
  }

  @Test
  public void tabs() {
    assertThat(UNMARSHALLER.unmarshall("'a\tb'")).isEqualTo("a\tb");
  }

  @Test
  public void nl() {
    assertThat(UNMARSHALLER.unmarshall("'a\\nb'")).isEqualTo("a\nb");
  }

  @Test
  public void cr() {
    assertThat(UNMARSHALLER.unmarshall("'a\\rb'")).isEqualTo("a\rb");
  }

  @Test
  public void backslash() {
    assertThat(UNMARSHALLER.unmarshall("'a\\\\b\\\\\\\\c'")).isEqualTo("a\\b\\\\c");
  }

  @Test
  public void complicated() {
    assertThat(UNMARSHALLER.unmarshall("'\t\\n\\na\\rb\t '")).isEqualTo("\t\n\na\rb\t ");
  }

}
