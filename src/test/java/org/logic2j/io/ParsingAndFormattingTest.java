package org.logic2j.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.Prolog;
import org.logic2j.PrologImpl;
import org.logic2j.PrologImplementor;
import org.logic2j.PrologImpl.InitLevel;
import org.logic2j.io.operator.Operator;
import org.logic2j.model.symbol.Term;

/**
 * Test parsing and formatting.
 *
 */
public class ParsingAndFormattingTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParsingAndFormattingTest.class);

  @Test
  public void test_parsing() {
    final Prolog prolog = new PrologImpl(InitLevel.L0_BARE);
    logger.info("Term: {}", prolog.term("p(X,Y) :- a;b,c,d"));
    logger.info("Term: {}", prolog.term("[1,2,3]"));
  }

  @Test
  public void test_parseNarityOperator() {
    final PrologImplementor prolog = new PrologImpl();
    prolog.getOperatorManager().addOperator("oo", Operator.YFY, 1020);
    logger.info("Result: {}", prolog.term("a oo b oo c oo d"));
  }

  @Test
  public void test_formatting() {
    final Prolog prolog = new PrologImpl(InitLevel.L0_BARE);
    Term t;
    //
    t = prolog.term("'An atom'");
    logger.info("Formatted: {}", t);
    assertEquals("'An atom'", t.toString());
    //
    t = prolog.term("t('A')");
    logger.info("Formatted: {}", t);
    assertEquals("t('A')", t.toString());
  }

}
