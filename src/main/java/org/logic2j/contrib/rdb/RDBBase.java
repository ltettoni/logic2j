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
package org.logic2j.contrib.rdb;

import javax.sql.DataSource;

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.impl.DefaultTermAdapter;
import org.logic2j.core.impl.PrologImplementation;

/**
 * Base class for RDB access using JDBC.
 */
public class RDBBase {
    /**
     * A {@link TermAdapter} that will parse all strings as atoms (especially those starting with uppercase that must not become bindings).
     */
    public static class AllStringsAsAtoms extends DefaultTermAdapter {

        public AllStringsAsAtoms(PrologImplementation theProlog) {
            super(theProlog);
        }

        @Override
        public Object term(Object theObject, FactoryMode theMode) {
            return super.term(theObject, FactoryMode.ATOM);
        }

    }

    private final PrologImplementation prolog;
    private TermAdapter termAdapter;
    private DataSource dataSource;

    public RDBBase(PrologImplementation theProlog, DataSource theDataSource) {
        this.prolog = theProlog;
        this.termAdapter = new RDBBase.AllStringsAsAtoms(this.prolog);
        this.dataSource = theDataSource;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void setDataSource(DataSource theDataSource) {
        this.dataSource = theDataSource;
    }

    public TermAdapter getTermAdapter() {
        return this.termAdapter;
    }

    public void setTermAdapter(TermAdapter theTermAdapter) {
        this.termAdapter = theTermAdapter;
    }

    public PrologImplementation getProlog() {
        return this.prolog;
    }

}
