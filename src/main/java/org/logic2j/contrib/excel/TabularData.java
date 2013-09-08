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

import java.io.Serializable;
import java.util.List;

/**
 * A tabular data set all loaded into memory.
 */
public class TabularData {

    public String predicateName;
    public String[] columnNames;
    public Serializable[][] data;

    public int rowIdentifierColumn = -1;

    public TabularData() {
    }

    /**
     * @param colNames
     * @param listData
     */
    public TabularData(List<String> colNames, List<List<Serializable>> listData) {
        this.columnNames = colNames.toArray(new String[0]);
        this.data = new Serializable[listData.size()][];
        for (int i = 0; i < this.data.length; i++) {
            Serializable[] row = listData.get(i).toArray(new Serializable[0]);
            this.data[i] = row;
        }
    }

    /**
     * @return Number of rows (entities) in the data.
     */
    public int getNbRows() {
        return data.length;
    }

    /**
     * @return Number of columns (properties) in the data.
     */
    public int getNbColumns() {
        return columnNames.length;
    }

    /**
     * @return
     */
    public String getPredicateSignature() {
        return this.predicateName + "/3";
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        if (this.data == null) {
            return this.getClass().getSimpleName() + "[no data]";
        }
        return this.getClass().getSimpleName() + '[' + this.getNbRows() + " x " + this.getNbColumns() + ']';
    }
}
