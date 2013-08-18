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
package org.logic2j.core;

import java.util.ArrayList;
import java.util.List;

import org.logic2j.core.TermFactory.FactoryMode;
import org.logic2j.core.io.format.DefaultFormatter;
import org.logic2j.core.io.operator.DefaultOperatorManager;
import org.logic2j.core.io.operator.OperatorManager;
import org.logic2j.core.io.parse.DefaultTermFactory;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.impl.core.CoreLibrary;
import org.logic2j.core.library.impl.io.IOLibrary;
import org.logic2j.core.library.mgmt.DefaultLibraryManager;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.DefaultSolver;
import org.logic2j.core.solver.Solver;
import org.logic2j.core.solver.holder.SolutionHolder;
import org.logic2j.core.theory.DefaultTheoryManager;
import org.logic2j.core.theory.TheoryManager;
import org.logic2j.core.unify.DefaultUnifier;
import org.logic2j.core.unify.Unifier;
import org.logic2j.core.util.ReflectUtils;
import org.logic2j.core.util.ReportUtils;

/**
 * Reference implementation of logic2j's {@link PrologImplementor} API.
 */
public class PrologImpl implements PrologImplementor {
    /**
     * Describe the level of initialization of the Prolog system, between bare mimimum, and full-featured.
     */
    public enum InitLevel {
        /**
         * A completely bare prolog engine, not functional for running programs (misses base predicates such as true/1, fail/1, !/1 (cut),
         * =/2, call/1, not/1, etc. But unification and inference of user predicates do work. No libraries loaded at all.
         */
        L0_BARE,
        /**
         * This is the default initialization level, it loads the {@link CoreLibrary}, containing (among others), core predicates such as
         * true/1, fail/1, !/1 (cut), =/2, call/1, not/1, etc.
         */
        L1_CORE_LIBRARY,
        /**
         * Higher level libraries loaded, such as {@link IOLibrary}.
         */
        L2_BASE_LIBRARIES,
    }

    // ---------------------------------------------------------------------------
    // Define all sub-features of this reference implementation and initialize with default, reference implementations
    // ---------------------------------------------------------------------------

    private TermFactory termFactory = new DefaultTermFactory(this);
    private Formatter formatter = new DefaultFormatter(this);
    private LibraryManager libraryManager = new DefaultLibraryManager(this);
    private OperatorManager operatorManager = new DefaultOperatorManager();
    private Solver solver = new DefaultSolver(this);
    private Unifier unifier = new DefaultUnifier();

    // TODO (issue) Does the clauseProviders belong here or from the Solver where they are solely used??? See
    // https://github.com/ltettoni/logic2j/issues/17
    private List<ClauseProvider> clauseProviders = new ArrayList<ClauseProvider>();

    /**
     * Default constructor will only provide an engine with the {@link CoreLibrary} loaded.
     */
    public PrologImpl() {
        this(InitLevel.L1_CORE_LIBRARY);
    }

    /**
     * Constructor for a specific level of initialization.
     * 
     * @param theLevel
     */
    public PrologImpl(InitLevel theLevel) {
        // The first clause provider is always the TheoryManager. Others may be added.
        final TheoryManager tm = new DefaultTheoryManager(this);
        this.clauseProviders.add(tm);

        // Here we load libs in order

        /*
         * NOTE: the ConfigLibrary was part of the "core" and has been pushed to "contrib" since it was only used in rdb-related
         * configuration. I comment out this initialization since I don't want the "core" to depend on "contrib". We'll have to find a way
         * to allow easy loading of libs upon initialization. I would really rely on DI to care about this.
         */
        // (commented out since ConfigLibrary no longer on core):
        // libraryManager.loadLibrary(new ConfigLibrary(this));

        if (theLevel.ordinal() >= InitLevel.L1_CORE_LIBRARY.ordinal()) {
            final LibraryBase lib = new CoreLibrary(this);
            this.libraryManager.loadLibrary(lib);
        }
        if (theLevel.ordinal() >= InitLevel.L2_BASE_LIBRARIES.ordinal()) {
            final IOLibrary lib = new IOLibrary(this);
            this.libraryManager.loadLibrary(lib);
        }
    }

    // ---------------------------------------------------------------------------
    // Implementation of interface Prolog.
    // These are actually shortcuts or "syntactic sugars".
    // ---------------------------------------------------------------------------

    @Override
    public Term term(Object theSource) {
        return getTermFactory().create(theSource, FactoryMode.ANY_TERM);
    }

    @Override
    public SolutionHolder solve(Term theGoal) {
        return new SolutionHolder(this, new Bindings(theGoal));
    }

    @Override
    public SolutionHolder solve(CharSequence theGoal) {
        // Perhaps we could transform the goal into a real clause?
        final Term parsed = term(theGoal);
        return solve(parsed);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // You may use DI to inject all sub-features into setters
    // TODO I think that we probably have to reinit/load the full engine's configuration everytime one setter is called, but we lack an
    // "afterPropertiesSet()" feature. We should use the standard @PostConstruct instead.
    // ---------------------------------------------------------------------------

    @Override
    public TheoryManager getTheoryManager() {
        // TODO is this normal to look for the first ClauseProvider only??? Seems OK, see constructor.
        return ReflectUtils.safeCastNotNull("getting TheoryManager of the first ClauseProvider", this.clauseProviders.get(0), TheoryManager.class);
    }

    @Override
    public TermFactory getTermFactory() {
        return this.termFactory;
    }

    public void setTermFactory(TermFactory termFactory) {
        this.termFactory = termFactory;
    }

    @Override
    public Formatter getFormatter() {
        return this.formatter;
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public LibraryManager getLibraryManager() {
        return this.libraryManager;
    }

    public void setLibraryManager(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    @Override
    public OperatorManager getOperatorManager() {
        return this.operatorManager;
    }

    public void setOperatorManager(OperatorManager operatorManager) {
        this.operatorManager = operatorManager;
    }

    @Override
    public Solver getSolver() {
        return this.solver;
    }

    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public Unifier getUnifier() {
        return this.unifier;
    }

    public void setUnifier(Unifier unifier) {
        this.unifier = unifier;
    }

    @Override
    public List<ClauseProvider> getClauseProviders() {
        return this.clauseProviders;
    }

    public void setClauseProviders(List<ClauseProvider> clauseProviders) {
        this.clauseProviders = clauseProviders;
    }

    // ---------------------------------------------------------------------------
    // Core java.lang.Object methods
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
