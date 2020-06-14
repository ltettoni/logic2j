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

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;

public class TabularDataSerializerTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TabularDataSerializerTest.class);

  @Test
  public void loadExcelAndSerialize() throws IOException {
    final File file = new File(TEST_RESOURCES_DIR, "cde.ca.gov/pubschls.xls");
    final TabularData data = new ExcelReader(file, true, 0).read();
    new TabularDataSerializer(new File(TEST_RESOURCES_DIR, "cde.ca.gov/pubschls.ser")).write(data);
  }

  @Test
  public void deserialize() throws IOException, ClassNotFoundException {
    final TabularData td = new TabularDataSerializer(new File(TEST_RESOURCES_DIR, "cde.ca.gov/pubschls.ser")).read();
    logger.info("Deserialized: {}", td);
  }

}
