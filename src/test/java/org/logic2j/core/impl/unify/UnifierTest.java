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
package org.logic2j.core.impl.unify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.Unifier;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.model.var.Bindings.FreeVarRepresentation;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;

// TODO replace the use of class UnificationTester by hamctest 1.3 assertions
public class UnifierTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnifierTest.class);

    public Unifier unifier;
    private UnificationTester tester;

    /**
     * Use a bare {@link PrologImplementation} to test the unification.
     */
    @Override
    protected InitLevel initLevel() {
        return InitLevel.L0_BARE;
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.unifier = new DefaultUnifier();
        this.tester = new UnificationTester(this.unifier);
    }

    @Test
    public void unifyAnonymousToAnonymous() {
        // 2 anonymous
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(0);
        this.tester.unify2ways(Var.ANONYMOUS_VAR, Var.ANONYMOUS_VAR);
    }

    @Test
    public void unifyVarToAnonymous() {
        // Var against anonymous (must unify without any binding)
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(0);
        this.tester.unify2ways(new Var("Q"), Var.ANONYMOUS_VAR);
    }

    @Test
    public void unifyAnonymousToAtom() {
        // Anonymous against literal
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(0);
        this.tester.unify2ways(Var.ANONYMOUS_VAR, new Struct("a"));
    }

    @Test
    public void unifyVarToAtom() {
        // Left to right unification
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(1);
        this.tester.unify2ways(new Var("Q"), new Struct("a"));
    }

    @Test
    public void unifyAtomToAtom() {
        // No unification on 2 different literals
        this.tester.setExpectedUnificationResult(false);
        this.tester.setExpectedNbBindings(0);
        this.tester.unify2ways(new Struct("b"), new Struct("a"));
    }

    @Test
    public void unifyStructToStruct_1() {
        // Must deunify automatically when unification fails
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(1);
        this.tester.unify2ways(Struct.valueOf("f", "X", "X"), Struct.valueOf("f", "_", "a"));
    }

    @Test
    public void unifyStructToStruct_2() {
        // Must deunify automatically when unification fails
        this.tester.setExpectedUnificationResult(false);
        this.tester.setExpectedNbBindings(0);
        this.tester.unify2ways(Struct.valueOf("f", "X", "X"), Struct.valueOf("f", "a", "b"));
    }

    @Test
    public void unifyOne() {
        // Var against anonymous (must unify without any binding)
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(0);
        this.tester.unify2ways(new Var("Q"), Var.ANONYMOUS_VAR);
    }

    /**
     * Unifying X=X used to create a loop binding onto itself!
     */
    @Test
    public void unifyToItself() {
        // Test unifying X to X, should not create a binding that loops on
        // itself
        this.tester.setExpectedUnificationResult(true);
        this.tester.setExpectedNbBindings(0);
        final Object term = this.prolog.getTermExchanger().unmarshall("X");
        this.tester.unify2ways(term, term, new Bindings(term));
    }

    /**
     * First unify A to TInt(123), then Unify X to A, make sure X binds to TInt(123)
     */
    @Test
    public void unifyVarToBoundTerm() { // Once a nasty bug
        final Object varA = TermApi.normalize(new Var("A"), null);
        final Bindings bindingsA = new Bindings(varA);
        final Object tlong = TermApi.normalize(123L, null);
        final boolean aToLiteral = this.unifier.unify(varA, bindingsA, tlong, new Bindings(tlong));
        logger.info("A={}", varA);
        logger.info("A={}", TermApi.substitute(varA, bindingsA, null));
        assertTrue(aToLiteral);
        if (aToLiteral) {
            this.unifier.deunify();
        }
    }

    @Test
    public void unifyVarToVar() {
        final Object varA = TermApi.normalize(new Var("A"), null);
        final Bindings bindingsA = new Bindings(varA);
        final Object varX = TermApi.normalize(new Var("X"), null);
        final Bindings bindingsX = new Bindings(varX);
        final boolean xToA = this.unifier.unify(varA, bindingsA, varX, bindingsX);
        assertTrue(xToA);
        logger.info("X={}", varX);
        logger.info("X={}", TermApi.substitute(varX, bindingsX, null));
        if (xToA) {
            this.unifier.deunify();
        }
    }

    @Test
    public void unifyMoreDifficult() {
        Struct goalTerm;
        final Var x = new Var("X");
        final Var y = new Var("Y");
        final Long two = 2L;
        goalTerm = new Struct(Struct.FUNCTOR_COMMA, new Struct("unify", x, y), new Struct("unify", x, two));
        final Object goalTermNormalized = TermApi.normalize(goalTerm, null);
        final Bindings goalVars = new Bindings(goalTermNormalized);
        final boolean unify = this.unifier.unify(x, goalVars, y, goalVars);
        final boolean unify2 = this.unifier.unify(x, goalVars, two, goalVars);
        logger.info("goalTerm={}", goalTerm);
        logger.info("Vars: {}", goalVars);
        logger.info("Bindings: {}", goalVars.explicitBindings(FreeVarRepresentation.SKIPPED));
        logger.info("goalTerm={}", TermApi.substitute(goalTerm, goalVars, null));
        assertStaticallyEquals("unify(2,2),unify(2,2)", TermApi.substitute(goalTerm, goalVars, null));
        if (unify2) {
            this.unifier.deunify();
        }
        if (unify) {
            this.unifier.deunify();
        }
    }

    @Test
    public void explicitBindings_1() {
        // Bind bindings1 to var
        final Object t0 = this.prolog.getTermExchanger().unmarshall("t(U)");
        final Bindings bindings0 = new Bindings(t0);
        final Object t1 = this.prolog.getTermExchanger().unmarshall("t(X)");
        final Bindings bindings1 = new Bindings(t1);
        final boolean unify = this.unifier.unify(t1, bindings1, t0, bindings0);
        assertEquals("t(X)", TermApi.substitute(t1, bindings1, null).toString());
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        if (unify) {
            this.unifier.deunify();
        }
    }

    @Test
    public void explicitBindings_2() {
        // Bind bindings2 to const
        final Object t0 = this.prolog.getTermExchanger().unmarshall("t(U)");
        final Bindings bindings0 = new Bindings(t0);
        final Object t2 = this.prolog.getTermExchanger().unmarshall("t(123)");
        final Bindings bindings2 = new Bindings(t2);
        final boolean unify = this.unifier.unify(t0, bindings0, t2, bindings2);
        assertEquals("t(123)", TermApi.substitute(t0, bindings0, null).toString());
        assertEquals("{U=123}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        if (unify) {
            this.unifier.deunify();
        }
    }

    @Test
    public void explicitBindings_representation_1() {
        final Object t1 = this.prolog.getTermExchanger().unmarshall("t(X)");
        final Bindings bindings1 = new Bindings(t1);
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.FREE_NOT_SELF).toString());
        assertEquals("{X=X}", bindings1.explicitBindings(FreeVarRepresentation.FREE).toString());
        assertEquals("{X=null}", bindings1.explicitBindings(FreeVarRepresentation.NULL).toString());
    }

    @Test
    public void explicitBindings_representation_2() {
        // No bindings since no variable in this one:
        final Object t2 = this.prolog.getTermExchanger().unmarshall("t(_)");
        final Bindings bindings2 = new Bindings(t2);
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.FREE_NOT_SELF).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.FREE).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.NULL).toString());
    }

    // TODO Try to understand what I was trying to test here - I can't any longer. Refactor then in 2 tests, each calling unify() once...
    @Test
    public void explicitBindings2() {
        final Object t0 = this.prolog.getTermExchanger().unmarshall("append2([1],[2,3],X)");
        final Bindings bindings0 = new Bindings(t0);
        // Bind bindings1 to var
        final Struct clause = (Struct) this.prolog.getTermExchanger().unmarshall("append2([E|T1],L2,[E|T2]) :- append2(T1,L2,T2)");
        final Object t1 = clause.getLHS(); // Term of first hitting clause
        final Bindings bindings1 = new Bindings(t1);
        final boolean unify = this.unifier.unify(t1, bindings1, t0, bindings0);
        assertTrue(unify);
        assertEquals("append2([1], [2,3], [1|T2])", TermApi.substitute(t0, bindings0, null).toString());
        assertEquals("append2([1], [2,3], [1|T2])", TermApi.substitute(t1, bindings1, null).toString());
        assertEquals("{X=[1|_]}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{E=1, L2=[2,3], T1=[]}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        // Bind bindings2 to const
        final Object t1b = clause.getRHS(); // Body of first hitting clause
        final Object t2 = this.prolog.getTermExchanger().unmarshall("append2([],L2,L2)"); // Body of second hitting clause
        final Bindings bindings2 = new Bindings(t2);
        final boolean unify2 = this.unifier.unify(t1b, bindings1, t2, bindings2);
        assertTrue(unify2);
        assertEquals("append2([], [2,3], [2,3])", TermApi.substitute(t1b, bindings1, null).toString());
        assertEquals("append2([], [2,3], [2,3])", TermApi.substitute(t2, bindings2, null).toString());
        assertEquals("{E=1, L2=[2,3], T1=[], T2=[2,3]}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{X=[1,2,3]}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        if (unify2) {
            this.unifier.deunify();
        }
        if (unify) {
            this.unifier.deunify();
        }
    }

    public void assertStaticallyEquals(CharSequence expectedStr, Object theActual) {
        final Object theExpected = this.prolog.getTermExchanger().unmarshall(expectedStr);
        if (!TermApi.structurallyEquals(theExpected, theActual)) {
            assertEquals("Terms are not structurally equal", theExpected.toString(), theActual.toString());
            fail("Terms are not structurally equal yet strangely their toString are the same");
        }
    }

    public static void assertStaticallyEquals(Term theExpected, Term theActual) {
        if (!TermApi.structurallyEquals(theExpected, theActual)) {
            assertEquals("Terms are not structurally equal", theExpected.toString(), theActual.toString());
            fail("Terms are not structurally equal yet strangely their toString are the same");
        }
    }

}
