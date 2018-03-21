/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.logic2j.engine.unify;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.engine.model.Var;
import org.logic2j.core.impl.DefaultTermMarshaller;
import org.logic2j.core.impl.DefaultTermUnmarshaller;
import org.logic2j.engine.util.ProfilingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class UnifyContextTest {
    private static final Logger logger = LoggerFactory.getLogger(UnifyContextTest.class);

    public static final DefaultTermUnmarshaller UNMARSHALLER = new DefaultTermUnmarshaller();
    public static final DefaultTermMarshaller MARSHALLER = new DefaultTermMarshaller();

    protected Var<?> X;

    protected Var<?> Y;

    protected Var<?> Z;

    protected Object _anon;

    protected Object a;

    protected Object b;

    protected Object a2;

    protected Object f_ab, f_aZ, f_XY, f_XX, f_aZZ, f_XXa, f_XXb;

    protected UnifyContext initialContext;

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

        initialContext = new UnifyStateByLookup().emptyContext();

        ProfilingInfo.setTimer1();
    }

    protected Object unmarshall(String text) {
        return UNMARSHALLER.unmarshall(text);
    }


    private UnifyContext bind(Var<?> v, Object t2) {
        logger.info("Binding   : {} -> {}", v, t2);
        UnifyContext m = initialContext;
        assertThat(m).isNotNull();
        UnifyContext m2 = m.bind(v, t2);
        assertThat(m2).isNotNull();
        assertThat(m2).isNotEqualTo(m);
        //
        assertThat(m.reify(v)).isEqualTo(v);
        //logger.info("Reify under original monad: {}", reified(m, v));
        logger.info("Term reified with returned UnifyContext: {}", reified(m2, v));
        return m2;
    }


    private UnifyContext unify(Object t1, Object t2) {
        logger.info("Unifying   : {}  ~  {}", t1, t2);
        UnifyContext m = initialContext;
        UnifyContext m2 = m.unify(t1, t2);
        if (m2 != null) {
            logger.info("Unified");
            logger.info("Monad after: {}", m2);
            logger.info("Terms after: {}  =  {}", reified(m2, t1), reified(m2, t2));
        } else {
            logger.info("Not unified");
        }
        return m2;
    }

    public CharSequence reified(UnifyContext r, Object term) {
        return MARSHALLER.marshall(r.reify(term));
    }




    @Test
    public void bindVarToLiteral() {
        bind(X, "literal");
    }

    @Test
    public void bindVarToVar() {
        UnifyContext m2 = bind(X, Y);
        assertThat(m2.reify(X)).isEqualTo(Y);
    }

    @Test
    public void varToAtom() {
        unify(X, a);
    }

    @Test
    public void varToAnon() {
        unify(X, _anon);
    }

    @Test
    public void atomToVar() {
        unify(a, X);
    }


    @Test
    public void atomToSameAtom() {
        assertThat(unify(a, a2)).isNotNull();
    }


    @Test
    public void atomToDifferentAtom() {
        assertThat(unify(a, b)).isNull();
    }

    @Test
    public void varToStruct() {
        assertThat(unify(f_ab, X)).isNotNull();
    }


    @Test
    public void structToStruct() {
        assertThat(unify(f_ab, f_XY)).isNotNull();
    }


    @Test
    public void structToStruct2() {
        assertThat(unify(f_aZ, f_XX)).isNotNull();
    }

    @Test
    public void structToStruct3() {
        assertThat(unify(f_aZZ, f_XXa)).isNotNull();
    }

    @Test
    public void structToStruct4() {
        assertThat(unify(f_aZZ, f_XXb)).isNull();
    }


}
