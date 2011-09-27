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
package org.logic2j.io.parse;

import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory;
import org.logic2j.io.parse.tuprolog.Parser;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;

/**
 * Default implementation of {@link TermFactory}
 */
public class DefaultTermFactory implements TermFactory {

  private static final TermApi TERM_API = new TermApi();
  private final PrologImplementor prolog;

  public DefaultTermFactory(PrologImplementor theProlog) {
    this.prolog = theProlog;
  }

  @Override
  public Term normalize(Term theTerm) {
    return TERM_API.normalize(theTerm, this.prolog.getLibraryManager().wholeContent());
  }

  @Override
  public Term parse(CharSequence theExpression) {
    Parser parser = new Parser(this.prolog.getOperatorManager(), theExpression.toString());
    final Term parsed = parser.parseSingleTerm();
    return normalize(parsed);
  }

  // TODO: be smarter to handle Arrays and Collections, and iterables
  @Override
  public Term create(Object theObject, FactoryMode theMode) {
    if (theObject instanceof CharSequence) {
      if (theMode == FactoryMode.ATOM) {
        return new Struct(theObject.toString());
      }
      return parse((CharSequence) theObject);
    }
    final Term created = TERM_API.valueOf(theObject, theMode);
    normalize(created);
    return created;
  }
}
