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
package org.logic2j.core.theory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.logic2j.core.ClauseProvider;
import org.logic2j.core.PrologImplementor;
import org.logic2j.core.io.parse.tuprolog.Parser;
import org.logic2j.core.library.PLibrary;
import org.logic2j.core.model.Clause;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.exception.PrologNonSpecificError;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.GoalFrame;
import org.logic2j.core.solver.Solver;
import org.logic2j.core.solver.listener.SolutionListener;
import org.logic2j.core.util.ReportUtils;

/**
 * Prolog's most classic way of providing {@link Clause}s to the {@link Solver} inference engine: all clauses are parsed and normalized from
 * one or several theories' textual content managed by this class. TODO Does the name "Manager" make sense here?
 */
public class DefaultTheoryManager implements TheoryManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTheoryManager.class);

    private final PrologImplementor prolog;
    private TheoryContent wholeContent = new TheoryContent();
    private ClauseProviderResolver clauseProviderResolver = new ClauseProviderResolver();
    private List<ClauseProvider> clauseProviders = new ArrayList<ClauseProvider>();

    /**
     * Constructor
     * 
     * @param theProlog
     */
    public DefaultTheoryManager(PrologImplementor theProlog) {
        this.prolog = theProlog;
        this.clauseProviders.add(this);
    }

    public TheoryContent load(CharSequence theTheoryText) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theTheoryText.toString());
        // final Iterator<Term> iterator = parser.iterator();
        return addClauses(parser);
    }

    public TheoryContent load(Reader theReader) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theReader);
        // final Iterator<Term> iterator = parser.iterator();
        return addClauses(parser);
    }

    @Override
    public TheoryContent load(File theFile) throws IOException {
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
    public TheoryContent load(PLibrary theLibrary) {
        // Load prolog theory from a classloadable resource
        final Class<? extends PLibrary> libraryClass = theLibrary.getClass();
        final String name = libraryClass.getSimpleName() + ".prolog";
        final URL contentUrl = libraryClass.getResource(name);
        if (contentUrl != null) {
            Object text;
            try {
                text = contentUrl.getContent();
            } catch (final IOException e) {
                throw new InvalidTermException("Could not load library from classloadable resource " + name + ": " + e);
            }
            if (text instanceof InputStream) {
                // FIXME: there will be encoding issues when using InputStream instead of Reader
                final Reader reader = new InputStreamReader((InputStream) text);
                return load(reader);
            }
            throw new InvalidTermException("Could not load library from classloadable resource " + name + ": could not getContent()");
        }
        logger.debug("Library \"{}\" loaded; no associated theory found", theLibrary);
        return new TheoryContent();
    }

    private TheoryContent addClauses(Parser theParser) {
        final TheoryContent content = new TheoryContent();
        Term clauseTerm = theParser.nextTerm(true);
        Term initializeGoal = null;
        while (clauseTerm != null) {
            // Term clauseTerm = iterator.next();
            // TODO Dubious we should not need to normalize here.
            logger.debug("Adding clause {}", clauseTerm);
            final Clause cl = new Clause(this.prolog, clauseTerm);
            if ("initialize".equals(cl.getHead().getName())) {
                initializeGoal = cl.getBody();
            } else {
                this.clauseProviderResolver.register(cl.getPredicateKey(), this);
            }
            content.add(cl);
            clauseTerm = theParser.nextTerm(true);
        }
        // Invoke the "initialize" goal
        if (initializeGoal != null) {
            final Bindings bindings = new Bindings(initializeGoal);
            final GoalFrame goalFrame = new GoalFrame();
            final SolutionListener solutionListener = new SolutionListener() {
                @Override
                public Continuation onSolution() {
                    return Continuation.USER_ABORT;
                }
            };
            this.prolog.getSolver().solveGoal(bindings, goalFrame, solutionListener);
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

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    /**
     * @param theContent to set - will replace any previously defined content.
     */
    @Override
    public void setTheory(TheoryContent theContent) {
        this.wholeContent = theContent;
    }

    @Override
    public ClauseProviderResolver getClauseProviderResolver() {
        return this.clauseProviderResolver;
    }

    public void setClauseProviderResolver(ClauseProviderResolver clauseProviderResolver) {
        this.clauseProviderResolver = clauseProviderResolver;
    }

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
    // Core java.lang.Object methods
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
