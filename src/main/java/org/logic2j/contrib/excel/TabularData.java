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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.logic2j.contrib.excel.TabularDataClauseProvider.AssertionMode;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.impl.PrologImplementation;

/**
 * A tabular data set fully loaded in memory.
 */
public class TabularData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String dataSetName;
    private final String[] columnNames;
    private final Serializable[][] data;

    /**
     * The 0-based index of the column that contains unique identifiers of rows, or -1 when not used.
     */
    private int primaryKeyColumn = -1;

    /**
     * @param theDataSetName
     * @param colNames
     * @param serializables
     */
    public TabularData(String theDataSetName, String[] colNames, Serializable[][] serializables) {
        this.dataSetName = theDataSetName;
        this.columnNames = colNames;
        this.data = serializables;
        checkAll();
    }

    /**
     * @param theDataSetName
     * @param colNames
     * @param listData
     */
    public TabularData(String theDataSetName, List<String> colNames, List<List<Serializable>> listData) {
        this.dataSetName = theDataSetName;
        this.columnNames = colNames.toArray(new String[0]);
        this.data = new Serializable[listData.size()][];
        for (int i = 0; i < this.data.length; i++) {
            final Serializable[] row = listData.get(i).toArray(new Serializable[0]);
            this.data[i] = row;
        }
        checkAll();
    }

    private void checkAll() {
        checkColumnNames();
        squarify();
        checkPrimaryKeyColumn();
    }

    /**
     * Make the data square, ie extend or reduce every data row to match the {@link #columnNames} size.
     */
    private void squarify() {
        final int nbColumns = this.columnNames.length;
        for (int i = 0; i < data.length; i++) {
            Serializable[] row = data[i];
            if (nbColumns != row.length) {
                data[i] = Arrays.copyOf(data[i], nbColumns);
            }
        }
    }

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

    // ---------------------------------------------------------------------------
    // Methods
    // ---------------------------------------------------------------------------

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

    /**
     * Helper method to instantiate a {@link TabularDataClauseProvider} from this {@link TabularData} and
     * add it to a Prolog implementation.
     * 
     * @param mode
     * @param prolog
     */
    public void addClauseProviderTo(PrologImplementation prolog, AssertionMode mode) {
        final TabularDataClauseProvider cp = new TabularDataClauseProvider(prolog, this, mode);
        prolog.getTheoryManager().addClauseProvider(cp);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public String getDataSetName() {
        return dataSetName;
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
