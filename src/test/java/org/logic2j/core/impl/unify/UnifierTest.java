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
import org.logic2j.core.api.model.var.TermBindings;
import org.logic2j.core.api.model.var.TermBindings.FreeVarRepresentation;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;

// TODO replace the use of class UnificationTester by hamcrest assertions
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

    @Before
    public void initUnifier() {
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
        final Object term = unmarshall("X");
        this.tester.unify2ways(term, term, new TermBindings(term));
    }

    /**
     * First unify A to TInt(123), then Unify X to A, make sure X binds to TInt(123)
     */
    @Test
    public void unifyVarToBoundTerm() { // Once a nasty bug
        final Object varA = TermApi.normalize(new Var("A"), null);
        final TermBindings bindingsA = new TermBindings(varA);
        final Object tlong = TermApi.normalize(123L, null);
        final boolean aToLiteral = this.unifier.unify(varA, bindingsA, tlong, new TermBindings(tlong));
        logger.info("A={}", varA);
        logger.info("A={}", TermApi.substitute(varA, bindingsA));
        assertTrue(aToLiteral);
        if (aToLiteral) {
            this.unifier.deunify();
        }
    }

    @Test
    public void unifyVarToVar() {
        final Object varA = TermApi.normalize(new Var("A"), null);
        final TermBindings bindingsA = new TermBindings(varA);
        final Object varX = TermApi.normalize(new Var("X"), null);
        final TermBindings bindingsX = new TermBindings(varX);
        final boolean xToA = this.unifier.unify(varA, bindingsA, varX, bindingsX);
        assertTrue(xToA);
        logger.info("X={}", varX);
        logger.info("X={}", TermApi.substitute(varX, bindingsX));
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
        final TermBindings theTermBindings = new TermBindings(goalTermNormalized);
        final boolean unify = this.unifier.unify(x, theTermBindings, y, theTermBindings);
        final boolean unify2 = this.unifier.unify(x, theTermBindings, two, theTermBindings);
        logger.info("goalTerm={}", goalTerm);
        logger.info("Vars: {}", theTermBindings);
        logger.info("TermBindings: {}", theTermBindings.explicitBindings(FreeVarRepresentation.SKIPPED));
        logger.info("goalTerm={}", TermApi.substitute(goalTerm, theTermBindings));
        assertStaticallyEquals("unify(2,2),unify(2,2)", TermApi.substitute(goalTerm, theTermBindings));
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
        final Object tU = unmarshall("t(U)");
        final TermBindings tbU = new TermBindings(tU);
        final Object tX = unmarshall("t(X)");
        final TermBindings tbX = new TermBindings(tX);
        final boolean unify = this.unifier.unify(tX, tbX, tU, tbU);
        assertTrue(unify);
        assertEquals("t(U)", TermApi.substitute(tX, tbX).toString());
        assertEquals("{}", tbX.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        if (unify) {
            this.unifier.deunify();
        }
    }

    @Test
    public void explicitBindings_2() {
        // Bind bindings2 to const
        final Object t0 = unmarshall("t(U)");
        final TermBindings bindings0 = new TermBindings(t0);
        final Object t2 = unmarshall("t(123)");
        final TermBindings bindings2 = new TermBindings(t2);
        final boolean unify = this.unifier.unify(t0, bindings0, t2, bindings2);
        assertEquals("t(123)", TermApi.substitute(t0, bindings0).toString());
        assertEquals("{U=123}", bindings0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        if (unify) {
            this.unifier.deunify();
        }
    }

    @Test
    public void explicitBindings_representation_1() {
        final Object t1 = unmarshall("t(X)");
        final TermBindings bindings1 = new TermBindings(t1);
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{}", bindings1.explicitBindings(FreeVarRepresentation.FREE_NOT_SELF).toString());
        assertEquals("{X=X}", bindings1.explicitBindings(FreeVarRepresentation.FREE).toString());
        assertEquals("{X=null}", bindings1.explicitBindings(FreeVarRepresentation.NULL).toString());
    }

    @Test
    public void explicitBindings_representation_2() {
        // No bindings since no variable in this one:
        final Object t2 = unmarshall("t(_)");
        final TermBindings bindings2 = new TermBindings(t2);
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.FREE_NOT_SELF).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.FREE).toString());
        assertEquals("{}", bindings2.explicitBindings(FreeVarRepresentation.NULL).toString());
    }

    // TODO Try to understand what I was trying to test here - I can't any longer. Refactor then in 2 tests, each calling unify() once...
    @Test
    public void explicitBindings2() {
        final Object t0 = unmarshall("append2([1],[2,3],X)");
        final TermBindings tb0 = new TermBindings(t0);
        // Bind bindings1 to var
        final Struct clause = (Struct) unmarshall("append2([E|T1],L2,[E|T2]) :- append2(T1,L2,T2)");
        final Object t1 = clause.getLHS(); // Term of first hitting clause
        final TermBindings tb1 = new TermBindings(t1);
        final boolean unify = this.unifier.unify(t1, tb1, t0, tb0);
        assertTrue(unify);
        
        assertEquals("append2([1], [2,3], [1|T2])", TermApi.substitute(t0, tb0).toString());
        assertEquals("append2([1], [2,3], [1|T2])", TermApi.substitute(t1, tb1).toString());
//        assertEquals("{X=[1|_]}", tb0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{X=[1|T2]}", tb0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{E=1, L2=[2,3], T1=[]}", tb1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        // Bind bindings2 to const
        final Object t1b = clause.getRHS(); // Body of first hitting clause
        final Object t2 = unmarshall("append2([],L2,L2)"); // Body of second hitting clause
        final TermBindings bindings2 = new TermBindings(t2);
        final boolean unify2 = this.unifier.unify(t1b, tb1, t2, bindings2);
        assertTrue(unify2);
        assertEquals("append2([], [2,3], [2,3])", TermApi.substitute(t1b, tb1).toString());
        assertEquals("append2([], [2,3], [2,3])", TermApi.substitute(t2, bindings2).toString());
        assertEquals("{E=1, L2=[2,3], T1=[], T2=[2,3]}", tb1.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        assertEquals("{X=[1,2,3]}", tb0.explicitBindings(FreeVarRepresentation.SKIPPED).toString());
        if (unify2) {
            this.unifier.deunify();
        }
        if (unify) {
            this.unifier.deunify();
        }
    }

    public void assertStaticallyEquals(CharSequence expectedStr, Object theActual) {
        final Object theExpected = unmarshall(expectedStr);
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
