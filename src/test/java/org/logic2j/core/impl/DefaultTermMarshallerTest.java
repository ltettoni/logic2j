package org.logic2j.core.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.logic2j.engine.model.Struct;

/**
 * Check how quoting works. See also {@link org.logic2j.engine.model.TermApi#quoteIfNeeded(CharSequence)} and its associated test cases.
 */
public class DefaultTermMarshallerTest {

  static final DefaultTermMarshaller MARSHALLER = new DefaultTermMarshaller();

  @Test
  public void spaces() {
    assertThat(MARSHALLER.marshall( new Struct(" txt  ")) ).isEqualTo("' txt  '");
  }

  @Test
  public void tabs() {
    assertThat(MARSHALLER.marshall( new Struct("a\tb")) ).isEqualTo("'a\tb'");
  }

  @Test
  public void nl() {
    assertThat(MARSHALLER.marshall( new Struct("a\nb")) ).isEqualTo("'a\\nb'");
  }


  @Test
  public void cr() {
    assertThat(MARSHALLER.marshall( new Struct("a\rb")) ).isEqualTo("'a\\rb'");
  }


  @Test
  public void backslash() {
    assertThat(MARSHALLER.marshall(new Struct("a\\b\\\\c"))).isEqualTo("'a\\\\b\\\\\\\\c'");
  }


  @Test
  public void complicated() {
    assertThat(MARSHALLER.marshall( new Struct("\t\n\na\rb\t ")) ).isEqualTo("'\t\\n\\na\\rb\t '");
  }
}