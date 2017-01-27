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

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.impl.PrologImplementation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A tabular (grid) data set fully loaded in memory. The data has the form of a 2-dimension array
 * of Serializable elements.
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
        this.columnNames = colNames.toArray(new String[colNames.size()]);
        this.data = new Serializable[listData.size()][];
        for (int i = 0; i < this.data.length; i++) {
            List<Serializable> var = listData.get(i);
            final Serializable[] row = var.toArray(new Serializable[var.size()]);
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
        for (int i = 0; i < this.data.length; i++) {
            final Serializable[] row = this.data[i];
            if (nbColumns != row.length) {
                this.data[i] = Arrays.copyOf(this.data[i], nbColumns);
            }
        }
    }

    private void checkColumnNames() {
        final HashSet<Serializable> duplicateKeys = new HashSet<Serializable>();
        final HashSet<Serializable> existingKeys = new HashSet<Serializable>();
        for (final String name : this.columnNames) {
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
            for (final Serializable[] element : this.data) {
                final Serializable value = element[this.primaryKeyColumn];
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
    public void addClauseProviderTo(PrologImplementation prolog, TermAdapter.AssertionMode mode) {
        final TabularDataClauseProvider cp = new TabularDataClauseProvider(prolog, this, mode);
        prolog.getTheoryManager().addClauseProvider(cp);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public String getDataSetName() {
        return this.dataSetName;
    }

    public String[] getColumnNames() {
        return this.columnNames;
    }

    public Serializable[][] getData() {
        return this.data;
    }

    public int getPrimaryKeyColumn() {
        return this.primaryKeyColumn;
    }

    public void setPrimaryKeyColumn(int thePrimaryKeyColumn) {
        this.primaryKeyColumn = thePrimaryKeyColumn;
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
