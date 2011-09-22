package org.logic2j.unify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.var.VarBindings;
import org.logic2j.model.var.VarBindings.FreeVarBehaviour;
import org.logic2j.solve.GoalFrame;
import org.logic2j.unify.Unifyer;

/**
 * Support the thorough testing of unification using the {@link Unifyer} interface.
 */
class UnificationTester {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnificationTester.class);

  private static final TermApi TERM_API = new TermApi();

  private final Unifyer unifyer;

  public Term left;
  public Term right;
  public VarBindings leftVars;
  public VarBindings rightVars;
  private GoalFrame frame;
  private Boolean expectedResult = null;
  private Boolean result = null;

  private Integer expectedNbBindings = null;

  /**
   * @param theUnifyer
   */
  public UnificationTester(Unifyer theUnifyer) {
    this.unifyer = theUnifyer;
  }

  /**
   * Every term has its own {@link VarBindings}.
   * @param theLeft
   * @param theRight
   */
  private void init2(Term theLeft, Term theRight) {
    this.left = TERM_API.normalize(theLeft, null);
    this.right = TERM_API.normalize(theRight, null);
    this.leftVars = new VarBindings(this.left);
    this.rightVars = new VarBindings(this.right);
    this.frame = new GoalFrame();
  }

  /**
   * Share same {@link VarBindings} for both terms.
   * @param theLeft
   * @param theRight
   * @param varBindings
   */
  private void init1(Term theLeft, Term theRight, VarBindings varBindings) {
    this.left = TERM_API.normalize(theLeft, null);
    this.right = TERM_API.normalize(theRight, null);
    this.leftVars = varBindings;
    this.rightVars = varBindings;
    this.frame = new GoalFrame();
  }

  /**
   * Execute the unification and do some state checking if expected results have been defined.
   * @return The unification result
   */
  private boolean unifyLR(StringBuilder theSignature) {
    logger.info("Unifying {} to {}", this.left, this.right);
    boolean unified = this.unifyer.unify(this.left, this.leftVars, this.right, this.rightVars, this.frame);
    logger.debug(" result={}, trailFrame={}", unified, this.frame);
    logger.debug(" leftVars={}", this.leftVars);
    logger.debug(" rightVars={}", this.rightVars);
    logger.debug(" left={}   bindings={}", TERM_API.substitute(this.left, this.leftVars, null),
        this.leftVars.explicitBindings(FreeVarBehaviour.SKIPPED));
    logger.debug(" right={}  bindings={}", TERM_API.substitute(this.right, this.rightVars, null),
        this.rightVars.explicitBindings(FreeVarBehaviour.SKIPPED));
    appendSignature(theSignature, this.leftVars, this.rightVars, this.frame);
    this.result = unified;
    if (this.expectedResult != null) {
      assertUnificationResult(unified);
    }
    if (unified) {
      if (this.expectedNbBindings != null) {
        assertNbBindings(this.expectedNbBindings);
      }
    } else {
      assertNbBindings(0);
    }
    return unified;
  }

  private boolean unifyRL(StringBuilder theSignature) {
    logger.info("Unifying {} to {}", this.right, this.left);
    boolean unified = this.unifyer.unify(this.right, this.rightVars, this.left, this.leftVars, this.frame);
    logger.debug(" result={}, trailFrame={}", unified, this.frame);
    logger.debug(" left={}   bindings={}", TERM_API.substitute(this.left, this.leftVars, null),
        this.leftVars.explicitBindings(FreeVarBehaviour.SKIPPED));
    logger.debug(" right={}  bindings={}", TERM_API.substitute(this.right, this.rightVars, null),
        this.rightVars.explicitBindings(FreeVarBehaviour.SKIPPED));
    appendSignature(theSignature, this.leftVars, this.rightVars, this.frame);
    this.result = unified;
    if (this.expectedResult != null) {
      assertUnificationResult(unified);
    }
    if (unified) {
      if (this.expectedNbBindings != null) {
        assertNbBindings(this.expectedNbBindings);
      }
    } else {
      assertNbBindings(0);
    }
    return unified;
  }

  public void unify2ways(Term theLeft, Term theRight) {
    init2(theLeft, theRight);
    StringBuilder signatureLR = new StringBuilder();
    boolean unifyLR = unifyLR(signatureLR);
    if (unifyLR) {
      deunify();
    }
    StringBuilder signatureRL = new StringBuilder();
    boolean unifyRL = unifyRL(signatureRL);
    if (unifyRL) {
      deunify();
    }
    assertEquals("Same unification results between LR and RL", unifyLR, unifyRL);
    assertEquals("Same post-unification state signatures", signatureLR.toString(), signatureRL.toString());
  }

  public void unify2ways(Term theLeft, Term theRight, VarBindings varBindings) {
    init1(theLeft, theRight, varBindings);
    StringBuilder signatureLR = new StringBuilder();
    boolean unifyLR = unifyLR(signatureLR);
    if (unifyLR) {
      deunify();
    }
    StringBuilder signatureRL = new StringBuilder();
    boolean unifyRL = unifyRL(signatureRL);
    if (unifyRL) {
      deunify();
    }
    assertEquals("Same unification results between LR and RL", unifyLR, unifyRL);
    assertEquals("Same post-unification state signatures", signatureLR.toString(), signatureRL.toString());
  }

  /**
   * @param theSignature
   * @param theVars1
   * @param theVars2
   * @param theFrame
   */
  private void appendSignature(StringBuilder theSignature, VarBindings theVars1, VarBindings theVars2, GoalFrame theFrame) {
    theSignature.append(theVars1.toString());
    theSignature.append("  ");
    theSignature.append(theVars2.toString());
    theSignature.append("  ");
    theSignature.append(theFrame.toString());
  }

  /**
   * Deunify and do some state checking.
   */
  private void deunify() {
    this.unifyer.deunify(this.frame);
    logger.debug("Deunify, trailFrame={}", this.frame);
    // No more bindings expected
    assertNbBindings(0);
  }

  /**
   * Assert on expected unification result.
   * @param theExpectedResult
   */
  private void assertUnificationResult(boolean theExpectedResult) {
    assertEquals("unification result", theExpectedResult, this.result.booleanValue());
  }

  /**
   * @param theExpectedNbBindings 
   */
  @SuppressWarnings("deprecation")
  private void assertNbBindings(int theExpectedNbBindings) {
    assertNotNull(this.frame);
    assertEquals("Number of var bindings", theExpectedNbBindings, this.frame.nbBindings());
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public void setExpectedUnificationResult(boolean theExpectedResult) {
    this.expectedResult = theExpectedResult;
  }

  /**
   * @param theExpectedNbBindings TNumber of effective bindings created by unification, and remembered in the
   * trailing vars to be deunified.
   */
  public void setExpectedNbBindings(int theExpectedNbBindings) {
    this.expectedNbBindings = theExpectedNbBindings;
  }

}
