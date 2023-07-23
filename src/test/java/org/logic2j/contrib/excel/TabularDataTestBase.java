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

import java.io.Serializable;
import java.util.Arrays;

import org.logic2j.core.PrologTestBase;

class TabularDataTestBase extends PrologTestBase {

  protected TabularData smallData() {
    final TabularData td = new TabularData("smallData", new String[]{"countryName", "countryCode", "population"}, new Serializable[][]{{"Switzerland", "CHE", 7.8},
            {"France", "FRA", 65.0}, {"Germany", "DEU", 85.4}});
    td.setPrimaryKeyColumn(1);
    return td;
  }

  protected static final int LARGE_DATA_NB_ROWS = 10000;

  protected TabularData largeData() {
    final String[] headers = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    final String[] contents = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
    final Serializable[][] dataArray = new Serializable[LARGE_DATA_NB_ROWS][];
    Arrays.fill(dataArray, contents);
    final TabularData tabData = new TabularData("largeData", headers, dataArray);
    tabData.setPrimaryKeyColumn(0);
    return tabData;
  }

}
