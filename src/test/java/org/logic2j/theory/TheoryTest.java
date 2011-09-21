package org.logic2j.theory;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.PrologImpl.InitLevel;
import org.logic2j.library.impl.core.CoreLibrary;
import org.logic2j.theory.DefaultTheoryManager;
import org.logic2j.theory.TheoryContent;
import org.logic2j.theory.TheoryManager;

/**
 */
public class TheoryTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TheoryTest.class);

  @Override
  protected InitLevel initLevel() {
    return InitLevel.L0_BARE;
  }

  @Test
  public void testLoadTheory() {
    final TheoryManager theoryManager = new DefaultTheoryManager(getProlog());
    final TheoryContent content = theoryManager.load(new CoreLibrary(getProlog()));
    logger.info("Loaded theory: {}", content);
  }

}
