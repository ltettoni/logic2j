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
package org.logic2j.core.impl.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.impl.io.operator.Operator;

/**
 * Test parsing and formatting.
 */
public class ParsingAndFormattingTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParsingAndFormattingTest.class);

    /**
     * No need for special init for only testing parsing and formatting.
     */
    @Override
    protected InitLevel initLevel() {
        return InitLevel.L0_BARE;
    }

    @Test
    public void parsing() {
        logger.info("Term: {}", this.prolog.getTermExchanger().unmarshall("p(X,Y) :- a;b,c,d"));
        logger.info("Term: {}", this.prolog.getTermExchanger().unmarshall("[1,2,3]"));
    }

    @Test
    public void parseNarityOperator() {
        this.prolog.getOperatorManager().addOperator("oo", Operator.YFY, 1020);
        logger.info("Result: {}", this.prolog.getTermExchanger().unmarshall("a oo b oo c oo d"));
    }

    @Test
    public void formatting() {
        Object t;
        //
        t = this.prolog.getTermExchanger().unmarshall("'An atom'");
        assertEquals(String.class, t.getClass());
        //
        t = this.prolog.getTermExchanger().unmarshall("t('A', b, 'C')");
        logger.info("Formatted: {}", t);
        assertEquals("t('A', b, 'C')", t.toString());
    }

}
