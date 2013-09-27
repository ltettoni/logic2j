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

package org.logic2j.contrib.excel;

import java.io.IOException;

import org.junit.Test;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.impl.unify.DefaultUnifier;

public class PublicSchoolsCaliforniaTest extends ExcelReaderTest {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublicSchoolsCaliforniaTest.class);

    // @Ignore("really heavy!")
    @Test
    public void withClauseProvider() throws IOException {
        setExcelClauseProvider("cde.ca.gov/pubschls.xls", TermAdapter.AssertionMode.EAV_NAMED);
        int number = getProlog().solve("pubschls(E, 'City', 'Fortuna')").all().number();
        // logger.info(assertNSolutions(15, "pubschls(E, 'City', 'Fortuna')").binding("E").toString());
        logger.info("unification: {}, solutions: {}", ((DefaultUnifier) getProlog().getUnifier()).counter, number);
    }

}
