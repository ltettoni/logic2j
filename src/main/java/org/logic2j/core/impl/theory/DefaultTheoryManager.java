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
package org.logic2j.core.impl.theory;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.DataFactProvider;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.Solver;
import org.logic2j.core.impl.io.tuprolog.parse.Parser;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.solver.listener.CountingSolutionListener;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.engine.util.TypeUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.logic2j.engine.model.TermApiLocator.termApiExt;

/**
 * Prolog's most classic way of providing {@link Clause}s to the {@link Solver}
 * inference engine: all clauses are parsed and normalized from
 * one or several theories' textual content managed by this class.
 * TODO Does the name "Manager" make sense here?
 */
public class DefaultTheoryManager implements TheoryManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTheoryManager.class);

  public static final String INITIALIZATION_PREDICATE = "initialization";

  private final PrologImplementation prolog;

  private final TheoryContent wholeContent = new TheoryContent();

  private List<ClauseProvider> clauseProviders = new ArrayList<>();

  private List<DataFactProvider> dataFactProviders = new ArrayList<>();

  public DefaultTheoryManager(PrologImplementation theProlog) {
    this.prolog = theProlog;
    this.clauseProviders.add(this);
  }

  public TheoryContent load(CharSequence theTheoryText) {
    final Parser parser = new Parser(this.prolog.getOperatorManager(), this.prolog.getTermAdapter(), theTheoryText.toString());
    return loadAllClauses(parser);
  }

  public TheoryContent load(Reader theReader) {
    final Parser parser = new Parser(this.prolog.getOperatorManager(), this.prolog.getTermAdapter(), theReader);
    return loadAllClauses(parser);
  }

  @Override
  public TheoryContent load(File theFile) throws IOException {
    final FileReader reader = new FileReader(theFile); // Note: the Parser further below will use a LineNumberReader
    try {
      return load(reader);
    } catch (final InvalidTermException e) {
      throw new PrologNonSpecificException("Theory could not be loaded from file \"" + theFile + "\" into " + this.prolog + ": " + e, e);
    } finally {
      reader.close();
    }
  }

  @Override
  public TheoryContent load(URL theTheory) {
    Object text;
    try {
      text = theTheory.getContent();
    } catch (final IOException e) {
      throw new InvalidTermException("Could not load theory from resource " + theTheory + ": " + e);
    }
    if (text instanceof InputStream) {
      // FIXME there will be encoding issues when using InputStream instead of Reader
      final Reader reader = new InputStreamReader((InputStream) text);
      return load(reader);
    }
    throw new InvalidTermException("Could not load theory from resource " + theTheory + ": could not getContent()");
  }

  /**
   * @param theClassloadableResource must start with "/" otherwise considered a URL
   */
  @Override
  public TheoryContent load(String theClassloadableResource) {
    if (theClassloadableResource == null) {
      throw new PrologNonSpecificException("Resource for rules content cannot be null");
    }
    final URL url;
    try {
      if (theClassloadableResource.startsWith("/")) {
        url = this.getClass().getResource(theClassloadableResource);
      } else {
        url = new URL(theClassloadableResource);
      }
      if (url == null) {
        throw new PrologNonSpecificException("No content at resource path: " + theClassloadableResource);
      }
      InputStream in = null;
      try {
        in = TypeUtils.safeCastNotNull("obtaining rules content from URL", url.getContent(), InputStream.class);
        // FIXME there will be encoding issues when using InputStream instead of Reader
        final Reader reader = new InputStreamReader(in);
        return load(reader);
      } finally {
        if (in != null) {
          in.close();
        }
      }
    } catch (final IOException e) {
      throw new InvalidTermException("Could not load theory from resource " + theClassloadableResource + ": " + e);
    }
  }

  // TODO is it reasonable to return a mutable list so that callers can add() to it???
  @Override
  public List<ClauseProvider> getClauseProviders() {
    return this.clauseProviders;
  }

  public void setClauseProviders(List<ClauseProvider> theClauseProviders) {
    this.clauseProviders = theClauseProviders;
  }

  @Override
  public boolean hasDataFactProviders() {
    return !this.dataFactProviders.isEmpty();
  }

  @Override
  public Iterable<DataFactProvider> getDataFactProviders() {
    return this.dataFactProviders;
  }

  @Override
  public void addClauseProvider(ClauseProvider theNewProvider) {
    this.clauseProviders.add(theNewProvider);
  }

  @Override
  public void addDataFactProvider(DataFactProvider theNewProvider) {
    this.dataFactProviders.add(theNewProvider);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  /**
   * @param theContent to add
   */
  @Override
  public void addTheory(TheoryContent theContent) {
    this.wholeContent.addAll(theContent);
    final Object initializationGoal = this.wholeContent.getInitializationGoal();
    if (initializationGoal != null) {
      executeDirective(initializationGoal);
      this.wholeContent.setInitializationGoal(null); // Next file loaded should not re-use same!
    }
  }

  public void setDataFactProviders(List<DataFactProvider> theDataFactProviders) {
    this.dataFactProviders = theDataFactProviders;
  }

  private TheoryContent loadAllClauses(Parser theParser) {
    final TheoryContent content = new TheoryContent();
    // Need to split parsing into terms and loading Clauses into Content separately, because of import/1
    for (Object clauseTerm = theParser.nextTerm(true); clauseTerm != null; clauseTerm = theParser.nextTerm(true)) {
      logger.debug("Parsed clause: {}", clauseTerm);
      if (clauseTerm instanceof CharSequence) {
        // Very rare case of facts being just a string (see "cut4" in our tests)
        clauseTerm = Struct.valueOf(clauseTerm.toString());
      }
      if (!(clauseTerm instanceof Struct)) {
        throw new InvalidTermException("Non-Struct term \"" + clauseTerm + "\" cannot be used for a Clause");
      }
      final Struct clauseStruct = (Struct) clauseTerm;
      if (isDirective(clauseStruct)) {
        // Identify directive
        final Object directiveGoal = clauseStruct.getArg(0);
        if (directiveGoal instanceof Struct && ((Struct) directiveGoal).getName() == INITIALIZATION_PREDICATE
            && ((Struct) directiveGoal).getArity() == 1) {
          final Object goal = ((Struct) directiveGoal).getArg(0);
          content.setInitializationGoal(goal);
        } else {
          executeDirective(directiveGoal);
        }
      } else {
        final Clause cl = new Clause(this.prolog, clauseTerm);
        content.add(cl);
      }

    }
    // End of loading
    return content;
  }

  private void executeDirective(Object directiveGoal) {
    directiveGoal = termApiExt().normalize(directiveGoal, this.prolog.getLibraryManager().wholeContent());
    // Execute right now
    final CountingSolutionListener countingListener = new CountingSolutionListener();
    this.prolog.getSolver().solveGoal(directiveGoal, countingListener);
    logger.debug("Execution of directive or initialization predicate \"{}\" gave {} solutions", directiveGoal, countingListener.count());
  }

  private boolean isDirective(Struct clauseStruct) {
    return clauseStruct.getName() == Struct.FUNCTOR_CLAUSE && clauseStruct.getArity() == 1;
  }

  // ---------------------------------------------------------------------------
  // Implementation of ClauseProvider
  // ---------------------------------------------------------------------------

  /**
   * @param theGoal
   * @param currentVars Not used in this method
   * @return All {@link Clause}s from the {@link TheoryContent} that may match theGoal.
   */
  @Override
  public Iterable<Clause> listMatchingClauses(Object theGoal, UnifyContext currentVars) {
    return this.wholeContent.find(theGoal);
  }

  // ---------------------------------------------------------------------------
  // Methods of java.lang.Object
  // ---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

}
