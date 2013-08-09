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
package org.logic2j.core.library.impl.rdb;

import javax.sql.DataSource;

import org.logic2j.core.PrologImplementor;
import org.logic2j.core.TermFactory;
import org.logic2j.core.io.parse.DefaultTermFactory;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.TermApi;

/**
 * Base class for RDB access using JDBC.
 */
public class RDBBase {

  /**
   * A {@link TermFactory} that will parse all strings as atoms 
   * (especially those starting with uppercase that must not become bindings).
   */
  public static class AllStringsAsAtoms extends DefaultTermFactory {
    private static final TermApi TERM_API = new TermApi();

    public AllStringsAsAtoms(PrologImplementor theProlog) {
      super(theProlog);
    }

    @Override
    public Term parse(CharSequence theExpression) {
      return new Struct(theExpression.toString());
    }

    @Override
    public Term create(Object theObject, FactoryMode theMode) {
      // Ignore theMode argument, and use forcing of atom instead
      return TERM_API.valueOf(theObject, FactoryMode.ATOM);
    }

  }

  private final PrologImplementor prolog;
  private TermFactory termFactory;
  private DataSource dataSource;

  public RDBBase(PrologImplementor theProlog, DataSource theDataSource) {
    this.prolog = theProlog;
    this.termFactory = new RDBBase.AllStringsAsAtoms(this.prolog);
    this.dataSource = theDataSource;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public DataSource getDataSource() {
    return this.dataSource;
  }

  public void setDataSource(DataSource theDataSource) {
    this.dataSource = theDataSource;
  }

  public TermFactory getTermFactory() {
    return this.termFactory;
  }

  public void setTermFactory(TermFactory theTermFactory) {
    this.termFactory = theTermFactory;
  }

  public PrologImplementor getProlog() {
    return this.prolog;
  }

}
