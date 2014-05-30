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

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.impl.DefaultTermAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link org.logic2j.core.api.TermAdapter} capable of handling {@link TabularData}.
 */
public class TabularDataTermAdapter extends DefaultTermAdapter {
    private static final String EAVT = "eavt";

    private final TermAdapter baseTermAdapter;

    public TabularDataTermAdapter(TermAdapter baseTermAdapter) {
        this.baseTermAdapter = baseTermAdapter;
    }

    @Override
    public List<Object> terms(Object theObject, AssertionMode theAssertionMode) {
        if (!(theObject instanceof TabularData)) {
            return super.terms(theObject, theAssertionMode);
        }
        final TabularData tabularData = (TabularData) theObject;
        final String dataSetName = tabularData.getDataSetName();
        final int nbRows = tabularData.getNbRows();
        final int nbColumns = tabularData.getNbColumns();
        final List<Object> result = new ArrayList<Object>();
        for (int r = 0; r < nbRows; r++) {
            try {
                final Serializable[] row = tabularData.getData()[r];
                switch (theAssertionMode) {
                case EAV_NAMED: {
                    if (tabularData.getPrimaryKeyColumn() < 0) {
                        throw new PrologNonSpecificError("Exposing tabular tabularData with mode EAV requires the entities have a unique identifier, specify the 'primaryKeyColumn' attribute");
                    }
                    final String identifier = row[tabularData.getPrimaryKeyColumn()].toString();
                    for (int c = 0; c < nbColumns; c++) {
                        if (c != tabularData.getPrimaryKeyColumn()) {
                            final String property = tabularData.getColumnNames()[c];
                            final Serializable value = row[c];
                            final Struct term = baseTermAdapter.term(dataSetName, FactoryMode.ATOM, identifier, property, value);
                            result.add(term);
                        }
                    }
                    break;
                }
                case EAVT: {
                    if (tabularData.getPrimaryKeyColumn() < 0) {
                        throw new PrologNonSpecificError("Exposing tabular tabularData with mode EAVT requires the entities have a unique identifier, specify the 'primaryKeyColumn' attribute");
                    }
                    final String identifier = row[tabularData.getPrimaryKeyColumn()].toString();
                    for (int c = 0; c < nbColumns; c++) {
                        if (c != tabularData.getPrimaryKeyColumn()) {
                            final String property = tabularData.getColumnNames()[c];
                            final Serializable value = row[c];
                            final Struct term = baseTermAdapter.term(EAVT, FactoryMode.ATOM, identifier, property, value, dataSetName);
                            result.add(term);
                        }
                    }
                    break;
                }
                case RECORD: {
                    final Struct term = baseTermAdapter.term(dataSetName, FactoryMode.ATOM, (Object[]) row);
                    result.add(term);
                    break;
                }
                default:
                    throw new PrologNonSpecificError("Unknown AssertionMode " + theAssertionMode);
                }
            } catch (final Exception e) {
                throw new PrologNonSpecificError("Could not initClauses on row=" + r, e);
            }
        }
        return result;
    }


}
