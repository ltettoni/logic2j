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

package org.logic2j.core.impl;

import static org.logic2j.engine.model.TermApiLocator.termApi;

import org.logic2j.core.api.OperatorManager;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermMapper;
import org.logic2j.core.api.TermUnmarshaller;
import org.logic2j.core.impl.io.tuprolog.parse.Parser;
import org.logic2j.engine.exception.InvalidTermException;

/**
 * Reference implementation of {@link org.logic2j.core.api.TermUnmarshaller}.
 */
public class DefaultTermUnmarshaller implements TermUnmarshaller {

  private static final TermAdapter termAdapter = new DefaultTermAdapter();

  private OperatorManager operatorManager = new DefaultOperatorManager();

  /**
   * A default TermMapper that will not handle primitives or operators defined in libraries.
   */
  private TermMapper normalizer = theTerm -> {
    return termApi().normalize(theTerm); // Uh, will ignore any existing primitives, etc.?
  };

  @Override
  public Object unmarshall(CharSequence theChars) {
    /*
       Due to the highly recursive nature of the parser,
       any Exception thrown in deep invocations will not carry any context,
       such as "The following token could not be identified: "<""
       Therefore we will throw a new exception that adds the context in the message.
     */
    try {
      final Parser parser = new Parser(this.operatorManager, termAdapter, theChars);
      final Object parsed = parser.parseSingleTerm();
      final Object normalized = normalizer.apply(parsed);
      return normalized;
    } catch (InvalidTermException e) {
      final String newMessage = "Parsing of expression \"" + theChars + "\" failed: " + e.getMessage();
      throw new InvalidTermException(newMessage, e);
    }
  }


  public TermMapper getNormalizer() {
    return normalizer;
  }

  public void setNormalizer(TermMapper normalizer) {
    this.normalizer = normalizer;
  }

  public OperatorManager getOperatorManager() {
    return operatorManager;
  }

  public void setOperatorManager(OperatorManager operatorManager) {
    this.operatorManager = operatorManager;
  }
}
