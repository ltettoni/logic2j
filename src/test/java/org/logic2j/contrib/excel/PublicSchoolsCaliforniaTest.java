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

package org.logic2j.contrib.excel;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.api.TermAdapter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * This test requires test data, see build.gradle, target "downloadTestResources".
 */
@Ignore("It's a little slow - uncomment it if you are in frequent-testing mood - or necessity")
public class PublicSchoolsCaliforniaTest extends ExcelClauseProviderTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublicSchoolsCaliforniaTest.class);

    @Before
    public void init() throws IOException {
        setExcelClauseProvider("cde.ca.gov/pubschls.xls", TermAdapter.AssertionMode.EAV_NAMED);
    }

    @Test
    public void withClauseProvider() throws IOException {
        final long number = getProlog().solve("pubschls(E, 'City', 'Fortuna')").count();
        // logger.info(assertNSolutions(15, "pubschls(E, 'City', 'Fortuna')").var("E").list().toString());
        logger.info("unification: solutions: {}", number);
        assertThat(number).isEqualTo(15);
    }

    @Test
    public void withDataProvider() throws IOException {
        final long number = getProlog().solve("pubschls(E, 'City', 'Fortuna')").count();
        // logger.info(assertNSolutions(15, "pubschls(E, 'City', 'Fortuna')").var("E").list().toString());
        logger.info("unification: solutions: {}", number);
        assertThat(number).isEqualTo(15);
    }

}
