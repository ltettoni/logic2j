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
package org.logic2j.theory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.logic2j.PrologImplementor;
import org.logic2j.io.parse.tuprolog.Parser;
import org.logic2j.library.PLibrary;
import org.logic2j.model.Clause;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalSolver;
import org.logic2j.util.ReportUtils;

/**
 * Prolog's most classic way of providing sequence of clauses to the {@link GoalSolver} inference engine:
 * all clauses are parsed and normalized from one or several theories' textual content managed
 * by this class.
 * TODO Is the name "Manager" correct here???
 */
public class DefaultTheoryManager implements TheoryManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTheoryManager.class);

  private PrologImplementor prolog;
  private TheoryContent wholeContent = new TheoryContent();

  /**
   * @param theProlog
   */
  public DefaultTheoryManager(PrologImplementor theProlog) {
    this.prolog = theProlog;
  }

  public TheoryContent load(CharSequence theTheoryText) {
    Parser parser = new Parser(this.prolog.getOperatorManager(), theTheoryText.toString());
    //    final Iterator<Term> iterator = parser.iterator();
    return addClauses(parser);
  }

  public TheoryContent load(Reader theReader) {
    Parser parser = new Parser(this.prolog.getOperatorManager(), theReader);
    //    final Iterator<Term> iterator = parser.iterator();
    return addClauses(parser);
  }

  @Override
  public TheoryContent load(File theFile) throws IOException {
    final FileReader reader = new FileReader(theFile);
    try {
      return load(reader);
    } finally {
      reader.close();
    }
  }

  @Override
  public TheoryContent load(PLibrary theLibrary) {
    // Load prolog theory from a classloadable resource
    final Class<? extends PLibrary> libraryClass = theLibrary.getClass();
    String name = libraryClass.getSimpleName() + ".prolog";
    URL contentUrl = libraryClass.getResource(name);
    if (contentUrl != null) {
      Object text;
      try {
        text = contentUrl.getContent();
      } catch (IOException e) {
        throw new InvalidTermException("Could not load library from classloadable resource " + name + ": " + e);
      }
      if (text instanceof InputStream) {
        // FIXME: there will be encoding issues when using InputStream instead of Reader
        Reader reader = new InputStreamReader((InputStream) text);
        return load(reader);
      }
      throw new InvalidTermException("Could not load library from classloadable resource " + name + ": could not getContent()");
    }
    logger.info("Library \"{}\" loaded; no associated theory found", theLibrary);
    return new TheoryContent();
  }

  private TheoryContent addClauses(Parser theParser) {
    TheoryContent content = new TheoryContent();
    //    final Iterator<Term> iterator = theParser.iterator();
    //    while (iterator.hasNext()) {
    Term clauseTerm = theParser.nextTerm(true);
    while (clauseTerm != null) {
      //      Term clauseTerm = iterator.next();
      // TODO Dubious we should not need to normalize here.
      logger.debug("Adding clause {}", clauseTerm);
      final Clause cl = new Clause(this.prolog, clauseTerm);
      content.add(cl);
      clauseTerm = theParser.nextTerm(true);
    }
    return content;
  }

  /**
   * @param theContent to set - will replace any previously defined content.
   */
  @Override
  public void setTheory(TheoryContent theContent) {
    this.wholeContent = theContent;
  }

  /**
   * @param theContent to add
   */
  @Override
  public void addTheory(TheoryContent theContent) {
    this.wholeContent.add(theContent);
  }

  //TODO => Check if the parameter theGoalBindings may be used because up to now it is declared in that method only because it has to implement it with those parameters (cf ClauseProvider).
  /**
   * 
   * @param theGoalBindings : is not used in that method, so you can give null as a parameter.
   */
  @Override
  public Iterable<Clause> listMatchingClauses(Struct theGoalTerm, Bindings theGoalBindings) {
    return this.wholeContent.find(theGoalTerm);
  }

  @Override
  public void assertZ(Struct theClause, boolean theB, String theName, boolean theB2) {
    throw new UnsupportedOperationException("Method assertZ() not implemented");
  }

  //---------------------------------------------------------------------------
  // Core java.lang.Object methods
  //---------------------------------------------------------------------------

  @Override
  public String toString() {
    return ReportUtils.shortDescription(this);
  }

}
