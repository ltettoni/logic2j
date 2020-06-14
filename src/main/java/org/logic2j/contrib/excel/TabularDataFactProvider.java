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

import org.logic2j.core.api.DataFactProvider;
import org.logic2j.core.api.TermAdapter.AssertionMode;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.model.DataFact;
import org.logic2j.engine.unify.UnifyContext;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * An implementation of DataFactProvider for TabularData.
 */
public class TabularDataFactProvider implements DataFactProvider {
  private final TabularData tabularData;
  private final AssertionMode assertionMode;
  final ArrayList<DataFact> dataFacts = new ArrayList<>();

  public TabularDataFactProvider(TabularData theTabularData, AssertionMode theMode) {
    this.tabularData = theTabularData;
    this.assertionMode = theMode;
    initDataFacts();
  }

  private void initDataFacts() {
    final String dataSetName = this.tabularData.getDataSetName();
    final int nbColumns = this.tabularData.getNbColumns();
    final int primaryKeyColumn = this.tabularData.getPrimaryKeyColumn();
    final String[] columnNames = this.tabularData.getColumnNames();
    final Serializable[][] data = this.tabularData.getData();
    for (final Serializable[] row : data) {
      switch (this.assertionMode) {
        case EAV_NAMED: {
          if (primaryKeyColumn < 0) {
            throw new InvalidTermException(
                    "Exposing tabular tabularData with mode EAV requires the entities have a unique identifier, specify the 'primaryKeyColumn' attribute");
          }
          final String identifier = row[primaryKeyColumn].toString();
          for (int c = 0; c < nbColumns; c++) {
            if (c != primaryKeyColumn) {
              final String property = columnNames[c];
              // TODO Null values should actually just not be asserted - so we use "n/a" but it this a good idea?
              Serializable value = row[c];
              if (value == null) {
                value = "n/a";
              }
              final DataFact fact = new DataFact(dataSetName, identifier, property, value);
              this.dataFacts.add(fact);
            }
          }
          break;
        }
        case EAVT: {
          if (primaryKeyColumn < 0) {
            throw new InvalidTermException(
                    "Exposing tabular tabularData with mode EAVT requires the entities have a unique identifier, specify the 'primaryKeyColumn' attribute");
          }
          final String identifier = row[primaryKeyColumn].toString();
          for (int c = 0; c < nbColumns; c++) {
            if (c != primaryKeyColumn) {
              final String property = columnNames[c];
              final Serializable value = row[c];
              final DataFact fact = new DataFact(identifier, property, value, dataSetName);
              this.dataFacts.add(fact);
            }
          }
          break;
        }
        case RECORD: {
          this.dataFacts.add(new DataFact((Object[]) row));
          break;
        }
        default:
          throw new InvalidTermException("Unknown AssertionMode " + this.assertionMode);

      }
    }
  }

  @Override
  public Iterable<DataFact> listMatchingDataFacts(Object theGoal, UnifyContext currentVars) {
    return this.dataFacts;
  }
}
