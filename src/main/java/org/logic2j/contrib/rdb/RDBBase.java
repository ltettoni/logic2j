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
package org.logic2j.contrib.rdb;

import javax.sql.DataSource;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.impl.DefaultTermAdapter;
import org.logic2j.core.impl.PrologImplementation;

/**
 * Base class for RDB access using JDBC.
 */
public abstract class RDBBase {
  /**
   * A {@link org.logic2j.core.api.TermAdapter} that will parse all strings as atoms (especially those starting with uppercase that must not become bindings).
   */
  public static class AllStringsAsAtoms extends DefaultTermAdapter {

    public AllStringsAsAtoms() {
      super();
    }

    @Override
    public Object toTerm(Object theObject, FactoryMode theMode) {
      return super.toTerm(theObject, FactoryMode.ATOM);
    }

  }


  private final PrologImplementation prolog;
  private DataSource dataSource;
  private TermAdapter termAdapter;

  protected RDBBase(PrologImplementation theProlog, DataSource theDataSource) {
    this.prolog = theProlog;
    this.dataSource = theDataSource;
    this.termAdapter = new RDBBase.AllStringsAsAtoms();
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
