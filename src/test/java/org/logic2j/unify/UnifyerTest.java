package org.logic2j.unify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.PrologImpl.InitLevel;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.VarBindings;
import org.logic2j.model.var.VarBindings.FreeVarBehaviour;
import org.logic2j.solve.GoalFrame;
import org.logic2j.unify.DefaultUnifyer;
import org.logic2j.unify.Unifyer;

/**
 */
public class UnifyerTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnifyerTest.class);

  public Unifyer unifyer = new DefaultUnifyer();
  private static final TermApi TERM_API = new TermApi();

  @Override
  protected InitLevel initLevel() {
    return InitLevel.L0_BARE;
  }

  private UnificationTester createDefaultUnificationTester() {
    return new UnificationTester(new DefaultUnifyer());
  }

  @Test
  public void testUnifyMany() {
    UnificationTester tester;
    // 2 anonymous
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(0);
    tester.unify2ways(Var.ANONYMOUS_VAR, Var.ANONYMOUS_VAR);
    // Var against anonymous (must unify without any binding)
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(0);
    tester.unify2ways(new Var("Q"), Var.ANONYMOUS_VAR);
    // Anonymous against literal
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(0);
    tester.unify2ways(Var.ANONYMOUS_VAR, new Struct("a"));
    // Left to right unification
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(1);
    tester.unify2ways(new Var("Q"), new Struct("a"));
    // No unifiation on 2 different literals
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(false);
    tester.setExpectedNbBindings(0);
    tester.unify2ways(new Struct("b"), new Struct("a"));
    // Must deunify automatically when unification fails
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(1);
    tester.unify2ways(new Struct("f", "X", "X"), new Struct("f", "_", "a"));
    // Must deunify automatically when unification fails
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(false);
    tester.setExpectedNbBindings(0);
    tester.unify2ways(new Struct("f", "X", "X"), new Struct("f", "a", "b"));
  }

  @Test
  public void testUnifyOne() {
    UnificationTester tester;
    // Var against anonymous (must unify without any binding)
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(0);
    tester.unify2ways(new Var("Q"), Var.ANONYMOUS_VAR);
  }

  /**
   * Unifying X=X used to create a loop binding onto itself!
   */
  @Test
  public void testUnifyToItself() {
    UnificationTester tester;
    // Test unifying X to X, should not create a binding that loops on itself
    tester = createDefaultUnificationTester();
    tester.setExpectedUnificationResult(true);
    tester.setExpectedNbBindings(0);
    Term term = getProlog().term("X");
    tester.unify2ways(term, term, new VarBindings(term));
  }

  /**
   * First unify A to TInt(123), then Unify X to A, make sure X binds to TInt(123)
   */
  @Test
  public void testUnifyVarToBoundTerm() { // Once a nasty bug
    Term varA = TERM_API.normalize(new Var("A"), null);
    VarBindings varsA = new VarBindings(varA);
    Term tlong = TERM_API.normalize(new TLong(123), null);
    boolean aToLiteral = this.unifyer.unify(varA, varsA, tlong, new VarBindings(tlong), new GoalFrame());
    logger.info("A={}", varA);
    logger.info("A={}", TERM_API.substitute(varA, varsA, null));
    assertTrue(aToLiteral);
    Term varX = TERM_API.normalize(new Var("X"), null);
    VarBindings varsX = new VarBindings(varX);
    boolean xToA = this.unifyer.unify(varA, varsA, varX, varsX, new GoalFrame());
    assertTrue(xToA);
    logger.info("X={}", varX);
    logger.info("X={}", TERM_API.substitute(varX, varsX, null));
  }

  @Test
  public void testUnifyMoreDifficult() {
    Struct goalTerm;
    Var x = new Var("X");
    Var y = new Var("Y");
    TLong two = new TLong(2);
    goalTerm = new Struct(Struct.FUNCTOR_COMMA, new Struct("unify", x, y), new Struct("unify", x, two));
    final Term goalTermCompact = TERM_API.normalize(goalTerm, null);
    final VarBindings goalVars = new VarBindings(goalTermCompact);
    GoalFrame gf = new GoalFrame();
    this.unifyer.unify(x, goalVars, y, goalVars, gf);
    this.unifyer.unify(x, goalVars, two, goalVars, gf);
    logger.info("goalTerm={}", goalTerm);
    logger.info("Vars: {}", goalVars);
    logger.info("Bindings: {}", goalVars.explicitBindings(FreeVarBehaviour.SKIPPED));
    logger.info("goalTerm={}", TERM_API.substitute(goalTerm, goalVars, null));
    assertStaticallyEquals("unify(2,2),unify(2,2)", TERM_API.substitute(goalTerm, goalVars, null));
  }

  @Test
  public void testExplicitBindings() {
    Term t0 = getProlog().term("t(U)");
    VarBindings var0 = new VarBindings(t0);
    // Bind var1 to var
    Term t1 = getProlog().term("t(X)");
    VarBindings var1 = new VarBindings(t1);
    GoalFrame goalFrame = new GoalFrame();
    this.unifyer.unify(t1, var1, t0, var0, goalFrame);
    assertEquals("t(X)", TERM_API.substitute(t1, var1, null).toString());
    assertEquals("{}", var1.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
    // Bind var2 to const
    Term t2 = getProlog().term("t(123)");
    VarBindings var2 = new VarBindings(t2);
    this.unifyer.unify(t0, var0, t2, var2, goalFrame);
    assertEquals("t(123)", TERM_API.substitute(t0, var0, null).toString());
    assertEquals("{U=123}", var0.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
  }

  @Test
  public void testExplicitBindings_behaviour() {
    Term t1 = getProlog().term("t(X)");
    VarBindings var1 = new VarBindings(t1);
    assertEquals("{}", var1.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
    assertEquals("{}", var1.explicitBindings(FreeVarBehaviour.FREE_NOT_SELF).toString());
    assertEquals("{X=X}", var1.explicitBindings(FreeVarBehaviour.FREE).toString());
    assertEquals("{X=null}", var1.explicitBindings(FreeVarBehaviour.NULL_ENTRY).toString());
    //
    //    Term t2 = getProlog().term("t(_)");
    //    VarBindings var2 = new VarBindings(t2);
    //    assertEquals("{}", var2.explicitBindings(t1, FreeVarBehaviour.SKIPPED).toString());
    //    assertEquals("{}", var2.explicitBindings(t1, FreeVarBehaviour.FREE_NOT_SELF).toString());
    //    assertEquals("{X=X}", var2.explicitBindings(t1, FreeVarBehaviour.FREE).toString());
    //    assertEquals("{X=null}", var2.explicitBindings(t1, FreeVarBehaviour.NULL_ENTRY).toString());
  }

  @Test
  public void testExplicitBindings2() {
    Term t0 = getProlog().term("append2([1],[2,3],X)");
    VarBindings var0 = new VarBindings(t0);
    // Bind var1 to var
    Struct clause = (Struct) getProlog().term("append2([E|T1],L2,[E|T2]) :- append2(T1,L2,T2)");
    Term t1 = clause.getLHS(); // Term of first hitting clause
    VarBindings var1 = new VarBindings(t1);
    GoalFrame goalFrame = new GoalFrame();
    assertTrue(this.unifyer.unify(t1, var1, t0, var0, goalFrame));
    assertEquals("append2([1], [2,3], [1|T2])", TERM_API.substitute(t0, var0, null).toString());
    assertEquals("append2([1], [2,3], [1|T2])", TERM_API.substitute(t1, var1, null).toString());
    assertEquals("{X=[1|_]}", var0.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
    assertEquals("{E=1, L2=[2,3], T1=[]}", var1.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
    // Bind var2 to const
    Term t1b = clause.getRHS(); // Body of first hitting clause
    Term t2 = getProlog().term("append2([],L2,L2)"); // Body of second hitting clause
    VarBindings var2 = new VarBindings(t2);
    assertTrue(this.unifyer.unify(t1b, var1, t2, var2, goalFrame));
    assertEquals("append2([], [2,3], [2,3])", TERM_API.substitute(t1b, var1, null).toString());
    assertEquals("append2([], [2,3], [2,3])", TERM_API.substitute(t2, var2, null).toString());
    assertEquals("{E=1, L2=[2,3], T1=[], T2=[2,3]}", var1.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
    assertEquals("{X=[1,2,3]}", var0.explicitBindings(FreeVarBehaviour.SKIPPED).toString());
  }

  public void assertStaticallyEquals(CharSequence expectedStr, Term theActual) {
    Term theExpected = getProlog().term(expectedStr);
    if (!theExpected.staticallyEquals(theActual)) {
      assertEquals("Terms are not statically equal", theExpected.toString(), theActual.toString());
      fail("Terms are not statically equal yet strangely their toString are the same");
    }
  }

  /**
   * @param theExpected
   * @param theActual
   */
  public static void assertStaticallyEquals(Term theExpected, Term theActual) {
    if (!theExpected.staticallyEquals(theActual)) {
      assertEquals("Terms are not statically equal", theExpected.toString(), theActual.toString());
      fail("Terms are not statically equal yet strangely their toString are the same");
    }
  }

}
