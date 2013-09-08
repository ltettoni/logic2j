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
package org.logic2j.core.impl.theory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.io.parse.tuprolog.Parser;
import org.logic2j.core.impl.util.ReportUtils;

/**
 * Prolog's most classic way of providing {@link Clause}s to the {@link Solver} inference engine: all clauses are parsed and normalized from
 * one or several theories' textual content managed by this class. TODO Does the name "Manager" make sense here?
 */
public class DefaultTheoryManager implements TheoryManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTheoryManager.class);

    private final PrologImplementation prolog;
    private final TheoryContent wholeContent = new TheoryContent();
    private List<ClauseProvider> clauseProviders = new ArrayList<ClauseProvider>();

    /**
     * Constructor
     * 
     * @param theProlog
     */
    public DefaultTheoryManager(PrologImplementation theProlog) {
        this.prolog = theProlog;
        this.clauseProviders.add(this);
    }

    public TheoryContent load(CharSequence theTheoryText) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theTheoryText.toString());
        // final Iterator<Term> iterator = parser.iterator();
        return loadAllClauses(parser);
    }

    public TheoryContent load(Reader theReader) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theReader);
        return loadAllClauses(parser);
    }

    @Override
    public TheoryContent load(File theFile) throws IOException {
        // TODO Should use a LineNumberReader to improve error reporting further down
        final FileReader reader = new FileReader(theFile);
        try {
            return load(reader);
        } catch (final InvalidTermException e) {
            throw new PrologNonSpecificError("Theory could not be loaded from file \"" + theFile + "\" into " + this.prolog + ": " + e, e);
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

    private TheoryContent loadAllClauses(Parser theParser) {
        final TheoryContent content = new TheoryContent();
        Term clauseTerm = theParser.nextTerm(true);
        Term specialInitializeGoalBody = null; // Body of the last clause whose head is "initialize"
        while (clauseTerm != null) {
            logger.debug("Parsed clause: {}", clauseTerm);
            final Clause cl = new Clause(this.prolog, clauseTerm);

            // Handling of the "initialize" special clause - we should provide IoC callback for that, not inline code!!!
            if ("initialize".equals(cl.getHead().getName())) {
                specialInitializeGoalBody = cl.getBody();
            }
            content.add(cl);
            clauseTerm = theParser.nextTerm(true);
        }
        // Invoke the "initialize" goal
        // TODO should be done elsewhere
        if (specialInitializeGoalBody != null) {
            final Bindings bindings = new Bindings(specialInitializeGoalBody);
            final SolutionListener solutionListener = new SolutionListener() {
                @Override
                public Continuation onSolution() {
                    return Continuation.USER_ABORT;
                }
            };
            this.prolog.getSolver().solveGoal(bindings, solutionListener);
        }
        return content;
    }

    /**
     * @param theContent to add
     */
    @Override
    public void addTheory(TheoryContent theContent) {
        this.wholeContent.addAll(theContent);
    }

    @Override
    public void addClauseProvider(ClauseProvider theNewProvider) {
        this.clauseProviders.add(theNewProvider);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    // TODO is it reasonable to return a mutable list so that callers can add() to it???
    @Override
    public List<ClauseProvider> getClauseProviders() {
        return this.clauseProviders;
    }

    public void setClauseProviders(List<ClauseProvider> clauseProviders) {
        this.clauseProviders = clauseProviders;
    }

    // ---------------------------------------------------------------------------
    // Implementation of ClauseProvider
    // ---------------------------------------------------------------------------

    /**
     * @param theGoal
     * @param theGoalBindings Not used in this method
     * @return All {@link Clause}s from the {@link TheoryContent} that may match theGoal.
     */
    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
        return this.wholeContent.find(theGoal);
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
