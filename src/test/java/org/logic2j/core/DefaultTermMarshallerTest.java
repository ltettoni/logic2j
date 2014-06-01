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

package org.logic2j.core;

import org.junit.Test;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.UnifyContext;
import org.logic2j.core.impl.DefaultTermMarshaller;

import static org.junit.Assert.assertEquals;

public class DefaultTermMarshallerTest extends PrologTestBase {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTermMarshallerTest.class);

    private static final String REFERENCE_EXPRESSION = "a,b;c,d;e";
    private static final String EXPECTED_TOSTRING = "';'(','(a, b), ';'(','(c, d), e))";

    @Test
    public void simpleToString() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        String formatted = term.toString();
        logger.info("toString: {}", formatted);
        assertEquals(EXPECTED_TOSTRING, formatted);
    }

    @Test
    public void defaultMarshallerUninitialized() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        CharSequence formatted = new DefaultTermMarshaller().marshall(term);
        logger.info("uninitialized marshaller: {}", formatted);
        assertEquals("a , b ; c , d ; e", formatted.toString());
    }

    @Test
    public void defaultMarshaller() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        CharSequence formatted = getProlog().getTermMarshaller().marshall(term);
        logger.info("prolog initialized marshaller: {}", formatted);
        assertEquals("a , b ; c , d ; e", formatted);
    }

    @Test
    public void xBoundToY() {
        Object term = unmarshall("f(X, Y)");
        final Var v1 = TermApi.findVar(term, "X");
        final Var v2 = TermApi.findVar(term, "Y");
        final UnifyContext initialContext = getProlog().getSolver().initialContext();
        final UnifyContext nextContext = initialContext.unify(v1, v2);
        CharSequence formatted = new DefaultTermMarshaller(nextContext).marshall(term);
        assertEquals("f(X, Y)", formatted);
    }

    @Test
    public void yBoundToX() {
        Object term = unmarshall("f(X, Y)");
        final Var v1 = TermApi.findVar(term, "X");
        final Var v2 = TermApi.findVar(term, "Y");
        final UnifyContext initialContext = getProlog().getSolver().initialContext();
        final UnifyContext nextContext = initialContext.unify(v1, v2);
        CharSequence formatted = new DefaultTermMarshaller(nextContext).marshall(term);
        assertEquals("f(X, Y)", formatted);
    }

}
