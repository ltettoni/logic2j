package org.logic2j.library.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.logic2j.PrologImplementor;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.core.CoreLibrary;

/**
 */
public class AdHocLibraryTest extends PrologTestBase {

  @Test
  public void test_getLibrary() throws Exception {
    AdHocLibraryForTesting library;
    try {
      library = getProlog().getLibraryManager().getLibrary(AdHocLibraryForTesting.class);
    } catch (Exception e) {
      // Expected to not find it
    }
    library = new AdHocLibraryForTesting(getProlog());
    loadLibrary(library);
    // Now we must find it - the same
    AdHocLibraryForTesting library2 = getProlog().getLibraryManager().getLibrary(AdHocLibraryForTesting.class);
    assertSame(library, library2);
    // But not another
    CoreLibrary library3 = getProlog().getLibraryManager().getLibrary(CoreLibrary.class);
    assertNotSame(library, library3);
  }

  @Test
  public void test_int_range() {
    PrologImplementor prolog = getProlog();
    prolog.getLibraryManager().loadLibrary(new AdHocLibraryForTesting(prolog));

    assertEquals(termList(12, 13, 14), assertNSolutions(3, "int_range(12, X, 14)").binding("X"));

    assertNoSolution("int_range(12, X, 10)");
    // Fixme: proper exception - not a class cast!
    // assertNoSolution("int_range(A, X, 10)");
  }

}
