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

package org.logic2j.core;

import org.junit.Test;
import org.logic2j.core.impl.DefaultTermMarshaller;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.unify.UnifyContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logic2j.engine.model.TermApiLocator.termApi;

public class DefaultTermMarshallerTest extends PrologTestBase {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTermMarshallerTest.class);

    private static final String REFERENCE_EXPRESSION = "a,b;c,d;e";
    private static final String EXPECTED_TOSTRING = "';'(','(a, b), ';'(','(c, d), e))";

    @Test
    public void simpleToString() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        String formatted = term.toString();
        logger.info("toString: {}", formatted);
        assertThat(formatted).isEqualTo(EXPECTED_TOSTRING);
    }

    @Test
    public void defaultMarshallerUninitialized() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        CharSequence formatted = new DefaultTermMarshaller().marshall(term);
        logger.info("uninitialized marshaller: {}", formatted);
        assertThat(formatted.toString()).isEqualTo("a , b ; c , d ; e");
    }

    @Test
    public void defaultMarshaller() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        CharSequence formatted = getProlog().getTermMarshaller().marshall(term);
        logger.info("prolog initialized marshaller: {}", formatted);
        assertThat(formatted).isEqualTo("a , b ; c , d ; e");
    }

    @Test
    public void xBoundToY() {
        Object term = unmarshall("f(X, Y)");
        final Var v1 = termApi().findVar(term, "X");
        final Var v2 = termApi().findVar(term, "Y");
        final UnifyContext initialContext = new UnifyContext(null, null);
        final UnifyContext nextContext = initialContext.unify(v1, v2);
        CharSequence formatted = new DefaultTermMarshaller(nextContext).marshall(term);
        assertThat(formatted).isEqualTo("f(X, Y)");
    }

    @Test
    public void yBoundToX() {
        Object term = unmarshall("f(X, Y)");
        final Var v1 = termApi().findVar(term, "X");
        final Var v2 = termApi().findVar(term, "Y");
        final UnifyContext initialContext = new UnifyContext(null, null);
        final UnifyContext nextContext = initialContext.unify(v1, v2);
        CharSequence formatted = new DefaultTermMarshaller(nextContext).marshall(term);
        assertThat(formatted).isEqualTo("f(X, Y)");
    }

}
