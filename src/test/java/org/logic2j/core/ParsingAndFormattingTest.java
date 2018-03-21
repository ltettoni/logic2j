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
import org.logic2j.core.api.model.Operator;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
        assertThat(marshall(unmarshall("p(X,Y) :- a;b,c,d"))).isEqualTo("p(X, Y) :- a ; b , c , d");
        assertThat(marshall(unmarshall("[1,2,3]"))).isEqualTo("[1, 2, 3]");
    }

    @Test
    public void parseNarityOperator() {
        this.prolog.getOperatorManager().addOperator("oo", Operator.YFY, 1020);
        final Object t = unmarshall("a oo b oo c oo d");
        logger.info("Result: {}", t);
        assertThat(marshall(t)).isEqualTo("oo(a, b, c, d)");
    }


    @Test
    public void formatting() {
        Object t;
        //
        t = unmarshall("'An atom'");
        assertThat(t.getClass()).isEqualTo(String.class);
        //
        t = unmarshall("t('A', b, 'C')");
        logger.info("Formatted: {}", t);
        assertThat(marshall(t)).isEqualTo("t('A', b, 'C')");
    }

}
