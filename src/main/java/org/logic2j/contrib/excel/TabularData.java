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
import java.util.HashSet;
import java.util.List;

import org.logic2j.core.api.model.exception.PrologNonSpecificError;

/**
 * A tabular data set all loaded into memory.
 */
public class TabularData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String[] columnNames;
    private Serializable[][] data;
    private String dataSetName;

    private int primaryKeyColumn = -1;

    public TabularData() {
    }

    /**
     * @param strings
     * @param serializables
     */
    public TabularData(String[] colNames, Serializable[][] serializables) {
        this.columnNames = colNames;
        this.data = serializables;
        checkColumnNames();
        checkPrimaryKeyColumn();
    }

    /**
     * @param colNames
     * @param listData
     */
    public TabularData(List<String> colNames, List<List<Serializable>> listData) {
        this.columnNames = colNames.toArray(new String[0]);
        this.data = new Serializable[listData.size()][];
        for (int i = 0; i < this.data.length; i++) {
            final Serializable[] row = listData.get(i).toArray(new Serializable[0]);
            this.data[i] = row;
        }
        checkPrimaryKeyColumn();
    }

    /**
     * 
     */
    private void checkColumnNames() {
        final HashSet<Serializable> duplicateKeys = new HashSet<Serializable>();
        final HashSet<Serializable> existingKeys = new HashSet<Serializable>();
        for (String name : this.columnNames) {
            if (existingKeys.contains(name)) {
                duplicateKeys.add(name);
            } else {
                existingKeys.add(name);
            }
        }
        if (!duplicateKeys.isEmpty()) {
            throw new PrologNonSpecificError("Tabular data " + this.dataSetName + " contains duplicate column names: " + duplicateKeys);
        }
    }

    /**
     * Make sure there are no duplicate values within the column defined as being the primary key.
     */
    private void checkPrimaryKeyColumn() {
        final HashSet<Serializable> duplicateKeys = new HashSet<Serializable>();
        final HashSet<Serializable> existingKeys = new HashSet<Serializable>();
        if (this.primaryKeyColumn >= 0) {
            for (int r = 0; r < this.data.length; r++) {
                Serializable value = this.data[r][this.primaryKeyColumn];
                if (existingKeys.contains(value)) {
                    duplicateKeys.add(value);
                } else {
                    existingKeys.add(value);
                }
            }
        }
        if (!duplicateKeys.isEmpty()) {
            throw new PrologNonSpecificError("Tabular data " + this.dataSetName + " contains duplicate keys in column " + this.primaryKeyColumn + ": " + duplicateKeys);
        }
    }

    /**
     * @return Number of rows (entities) in the data.
     */
    public int getNbRows() {
        return this.data.length;
    }

    /**
     * @return Number of columns (properties) in the data.
     */
    public int getNbColumns() {
        return this.columnNames.length;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public Serializable[][] getData() {
        return data;
    }

    public int getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public void setPrimaryKeyColumn(int primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
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
