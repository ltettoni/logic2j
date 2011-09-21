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
      } else {
        return parse((CharSequence) theObject);
      }
    }
    final Term created = TERM_API.valueOf(theObject, theMode);
    normalize(created);
    return created;
  }
}
