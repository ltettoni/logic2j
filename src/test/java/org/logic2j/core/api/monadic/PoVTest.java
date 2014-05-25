package org.logic2j.core.api.monadic;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.impl.DefaultTermMarshaller;
import org.logic2j.core.impl.DefaultTermUnmarshaller;
import org.logic2j.core.impl.util.ProfilingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

public class PoVTest {
    private static final Logger logger = LoggerFactory.getLogger(PoVTest.class);

    public static final DefaultTermUnmarshaller UNMARSHALLER = new DefaultTermUnmarshaller();
    public static final DefaultTermMarshaller MARSHALLER = new DefaultTermMarshaller();

    protected Var X;

    protected Var Y;

    protected Var Z;

    protected Object _anon;

    protected Object a;

    protected Object b;

    protected Object a2;

    protected Object f_ab, f_aZ, f_XY, f_XX, f_aZZ, f_XXa, f_XXb;

    protected PoV initialPoV;

    @Before
    public void configureProlog() {
        a = unmarshall("a");
        b = unmarshall("b");
        a2 = unmarshall("a");
        _anon = unmarshall("_");

        X = (Var) unmarshall("X");
        X.index = 1;
        Y = (Var) unmarshall("Y");
        Y.index = 2;
        Z = (Var) unmarshall("Z");
        Z.index = 3;

        f_ab = unmarshall("f(a,b)");
        f_aZ = unmarshall("f(a,Z)");
        f_XY = unmarshall("f(X,Y)");
        f_XX = unmarshall("f(X2,X2)");
        f_aZZ = unmarshall("f(a,X3,X3)");
        f_XXa = unmarshall("f(X4,X4,a)");
        f_XXb = unmarshall("f(X5,X5,b)");

        initialPoV = new StateEngineByLookup().emptyPoV();

        ProfilingInfo.setTimer1();
    }

    protected Object unmarshall(String text) {
        return UNMARSHALLER.unmarshall(text);
    }


    private PoV bind(Var v, Object t2) {
        logger.info("Binding   : {} -> {}", v, t2);
        PoV m = initialPoV;
        assertNotNull(m);
        PoV m2 = m.bind(v, t2);
        assertNotNull(m2);
        assertNotSame(m, m2);
        //
        assertSame(v, m.finalValue(v));
        //logger.info("Reify under original monad: {}", reified(m, v));
        logger.info("Term reified with returned PoV: {}", reified(m2, v));
        return m2;
    }


    private PoV unify(Object t1, Object t2) {
        logger.info("Unifying   : {}  ~  {}", t1, t2);
        PoV m = initialPoV;
        PoV m2 = m.unify(t1, t2);
        if (m2 != null) {
            logger.info("Unified");
            logger.info("Monad after: {}", m2);
            logger.info("Terms after: {}  =  {}", reified(m2, t1), reified(m2, t2));
        } else {
            logger.info("Not unified");
        }
        return m2;
    }

    public CharSequence reified(PoV r, Object term) {
        return MARSHALLER.marshall(r.reify(term));
    }




    @Test
    public void bindVarToLiteral() throws Exception {
        bind(X, "literal");
    }

    @Test
    public void bindVarToVar() throws Exception {
        PoV m2 = bind(X, Y);
        assertSame(Y, m2.finalValue(X));
    }

    @Test
    public void varToAtom() throws Exception {
        unify(X, a);
    }

    @Test
    public void varToAnon() throws Exception {
        unify(X, _anon);
    }

    @Test
    public void atomToVar() throws Exception {
        unify(a, X);
    }


    @Test
    public void atomToSameAtom() throws Exception {
        assertNotNull(unify(a, a2));
    }


    @Test
    public void atomToDifferentAtom() throws Exception {
        assertNull(unify(a, b));
    }

    @Test
    public void varToStruct() throws Exception {
        assertNotNull(unify(f_ab, X));
    }


    @Test
    public void structToStruct() throws Exception {
        assertNotNull(unify(f_ab, f_XY));
    }


    @Test
    public void structToStruct2() throws Exception {
        assertNotNull(unify(f_aZ, f_XX));
    }

    @Test
    public void structToStruct3() throws Exception {
        assertNotNull(unify(f_aZZ, f_XXa));
    }

    @Test
    public void structToStruct4() throws Exception {
        assertNull(unify(f_aZZ, f_XXb));
    }


}