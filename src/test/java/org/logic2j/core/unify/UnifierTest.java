/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.core.unify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.PrologImpl.InitLevel;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.TLong;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.TermApi;
import org.logic2j.core.model.symbol.Var;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.model.var.Bindings.FreeVarRepresentation;
import org.logic2j.core.solver.GoalFrame;

// TODO replace the use of class UnificationTester by hamctest 1.3 assertions
public class UnifierTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnifierTest.class);

    private static final TermApi TERM_API = new TermApi();

    public Unifier unifier;
    private UnificationTester tester;

    @Override
    protected InitLevel initLevel() {
        return InitLevel.L0_BARE;
    }

    @Before
    public void setUp() {
        super.setUp();
        this.unifier = new DefaultUnifier();
        this.tester = new UnificationTester(this.unifier);
    }

    @Test
    public void testUnifyAnonymousToAnonymous() {
        // 2 anonymous
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(0);
        tester.unify2ways(Var.ANONYMOUS_VAR, Var.ANONYMOUS_VAR);
    }

    @Test
    public void testUnifyVarToAnonymous() {
        // Var against anonymous (must unify without any binding)
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(0);
        tester.unify2ways(new Var("Q"), Var.ANONYMOUS_VAR);
    }

    @Test
    public void testUnifyAnonymousToAtom() {
        // Anonymous against literal
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(0);
        tester.unify2ways(Var.ANONYMOUS_VAR, new Struct("a"));
    }

    @Test
    public void testUnifyVarToAtom() {
        // Left to right unification
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(1);
        tester.unify2ways(new Var("Q"), new Struct("a"));
    }

    @Test
    public void testUnifyAtomToAtom() {
        // No unification on 2 different literals
        tester.setExpectedUnificationResult(false);
        tester.setExpectedNbBindings(0);
        tester.unify2ways(new Struct("b"), new Struct("a"));
    }

    @Test
    public void testUnifyStructToStruct_1() {
        // Must deunify automatically when unification fails
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(1);
        tester.unify2ways(new Struct("f", "X", "X"), new Struct("f", "_", "a"));
    }

    @Test
    public void testUnifyStructToStruct_2() {
        // Must deunify automatically when unification fails
        tester.setExpectedUnificationResult(false);
        tester.setExpectedNbBindings(0);
        tester.unify2ways(new Struct("f", "X", "X"), new Struct("f", "a", "b"));
    }

    @Test
    public void testUnifyOne() {
        // Var against anonymous (must unify without any binding)
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(0);
        tester.unify2ways(new Var("Q"), Var.ANONYMOUS_VAR);
    }

    /**
     * Unifying X=X used to create a loop binding onto itself!
     */
    @Test
    public void testUnifyToItself() {
        // Test unifying X to X, should not create a binding that loops on
        // itself
        tester.setExpectedUnificationResult(true);
        tester.setExpectedNbBindings(0);
        Term term = getProlog().term("X");
        tester.unify2ways(term, term, new Bindings(term));
    }

    /**
     * First unify A to TInt(123), then Unify X to A, make sure X binds to TInt(123)
     */
    @Test
    public void testUnifyVarToBoundTerm() { // Once a nasty bug
        Term varA = TERM_API.normalize(new Var("A"), null);
        Bindings bindingsA = new Bindings(varA);
        Term tlong = TERM_API.normalize(new TLong(123), null);
        boolean aToLiteral = this.unifier.unify(varA, bindingsA, tlong, new Bindings(tlong), new GoalFrame());
        logger.info("A={}", varA);
        logger.info("A={}", TERM_API.substitute(varA, bindingsA, null));
        assertTrue(aToLiteral);
    }

    @Test
    public void testUnifyVarToVar() {
        Term varA = TERM_API.normalize(new Var("A"), null);
        Bindings bindingsA = new Bindings(varA);
        Term varX = TERM_API.normalize(new Var("X"), null);
        Bindings bindingsX = new Bindings(varX);
        boolean xToA = this.unifier.unify(varA, bindingsA, varX, bindingsX, new GoalFrame());
        assertTrue(xToA);
        logger.info("X={}", varX);
        logger.info("X={}", TERM_API.substitute(varX, bindingsX, null));
    }

    @Test
    public void testUnifyMoreDifficult() {
        Struct goalTerm;
        Var x = new Var("X");
        Var y = new Var("Y");
        TLong two = new TLong(2);
        goalTerm = new Struct(Struct.FUNCTOR_COMMA, new Struct("unify", x, y), new Struct("unify", x, two));
        final Term goalTermNormalized = TERM_API.normalize(goalTerm, null);
        final Bindings goalVars = new Bindings(goalTermNormalized);
        GoalFrame gf = new GoalFrame();
        this.unifier.unify(x, goalVars, y, goalVars, gf);
        this.unifier.unify(x, goalVars, two, goalVars, gf);
        logger.info("goalTerm={}", goalTerm);
        logger.info("Vars: {}", goalVars);
        logger.info("Bindings: {}", goalVars.explicitBindings(FreeVarRepresentation.SKIPPED));
        logger.info("goalTerm={}", TERM_API.substitute(goalTerm, goalVars, null));
        assertStaticallyEquals("unify(2,2),unify(2,2)", TERM_API.substitute(goalTerm, goalVars, null));
    }

    @Test
    public void testExplicitBindings_1() {
        // Bind bindings1 to var
        Term t0 = getProlog().term("t(U)");
        Bindings bindings0 = new Bindings(t0);
        Term t1 = getProlog().term("t(X)");
        Bindings bindings1 = new Bindings(t1);
        GoalFrame goalFrame = new GoalFrame();
        this.unifier.unify(t1, bindings1, t0, bindings0, goalFrame);
        assertEquals("t(X)", TERM_API.substitute(t1, bindings1, null).toString());
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
    }

    @Test
    public void testExplicitBindings_2() {
        GoalFrame goalFrame = new GoalFrame();
        // Bind bindings2 to const
        Term t0 = getProlog().term("t(U)");
        Bindings bindings0 = new Bindings(t0);
        Term t2 = getProlog().term("t(123)");
        Bindings bindings2 = new Bindings(t2);
        this.unifier.unify(t0, bindings0, t2, bindings2, goalFrame);
        assertEquals("t(123)", TERM_API.substitute(t0, bindings0, null).toString());
        assertEquals("{U=123}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
    }

    @Test
    public void testExplicitBindings_representation_1() {
        Term t1 = getProlog().term("t(X)");
        Bindings bindings1 = new Bindings(t1);
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.FREE_NOT_SELF).toString());
        assertEquals("{X=X}", bindings1.explicitBindings(FreeVarRepresentation.FREE).toString());
        assertEquals("{X=null}", bindings1.explicitBindings(FreeVarRepresentation.NULL).toString());
    }

    @Test
    public void testExplicitBindings_representation_2() {
        // No bindings since no variable in this one:
        Term t2 = getProlog().term("t(_)");
        Bindings bindings2 = new Bindings(t2);
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.FREE_NOT_SELF).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.FREE).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.NULL).toString());
    }

    // TODO Try to understand what I was trying to test here - I can't any longer. Refactor then in 2 tests, each calling unify() once...
    @Test
    public void testExplicitBindings2() {
        Term t0 = getProlog().term("append2([1],[2,3],X)");
        Bindings bindings0 = new Bindings(t0);
        // Bind bindings1 to var
        Struct clause = (Struct) getProlog().term("append2([E|T1],L2,[E|T2]) :- append2(T1,L2,T2)");
        Term t1 = clause.getLHS(); // Term of first hitting clause
        Bindings bindings1 = new Bindings(t1);
        GoalFrame goalFrame = new GoalFrame();
        assertTrue(this.unifier.unify(t1, bindings1, t0, bindings0, goalFrame));
        assertEquals("append2([1], [2,3], [1|T2])", TERM_API.substitute(t0, bindings0, null).toString());
        assertEquals("append2([1], [2,3], [1|T2])", TERM_API.substitute(t1, bindings1, null).toString());
        assertEquals("{X=[1|_]}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{E=1, L2=[2,3], T1=[]}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        // Bind bindings2 to const
        Term t1b = clause.getRHS(); // Body of first hitting clause
        Term t2 = getProlog().term("append2([],L2,L2)"); // Body of second
                                                         // hitting clause
        Bindings bindings2 = new Bindings(t2);
        assertTrue(this.unifier.unify(t1b, bindings1, t2, bindings2, goalFrame));
        assertEquals("append2([], [2,3], [2,3])", TERM_API.substitute(t1b, bindings1, null).toString());
        assertEquals("append2([], [2,3], [2,3])", TERM_API.substitute(t2, bindings2, null).toString());
        assertEquals("{E=1, L2=[2,3], T1=[], T2=[2,3]}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{X=[1,2,3]}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
    }

    public void assertStaticallyEquals(CharSequence expectedStr, Term theActual) {
        Term theExpected = getProlog().term(expectedStr);
        if (!theExpected.structurallyEquals(theActual)) {
            assertEquals("Terms are not structurally equal", theExpected.toString(), theActual.toString());
            fail("Terms are not structurally equal yet strangely their toString are the same");
        }
    }

    /**
     * @param theExpected
     * @param theActual
     */
    public static void assertStaticallyEquals(Term theExpected, Term theActual) {
        if (!theExpected.structurallyEquals(theActual)) {
            assertEquals("Terms are not structurally equal", theExpected.toString(), theActual.toString());
            fail("Terms are not structurally equal yet strangely their toString are the same");
        }
    }

}
