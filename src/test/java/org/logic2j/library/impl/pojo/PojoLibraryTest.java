package org.logic2j.library.impl.pojo;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.pojo.PojoLibrary;

/**
 */
public class PojoLibraryTest extends PrologTestBase {

  @Test
  public void test_bind() throws Exception {
    loadLibrary(new PojoLibrary(getProlog()));
    bind("name", "value");
    //
    assertOneSolution("bind(name, X), X=value");
    assertNoSolution("bind(name, X), X=some_other");
    assertNoSolution("bind(name, a)");
    assertNoSolution("bind(name, name)");
    assertOneSolution("bind(name, value)");
  }

}
