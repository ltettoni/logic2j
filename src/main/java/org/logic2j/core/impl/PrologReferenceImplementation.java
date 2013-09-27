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
package org.logic2j.core.impl;

import org.logic2j.core.api.LibraryManager;
import org.logic2j.core.api.PLibrary;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermExchanger;
import org.logic2j.core.api.Unifier;
import org.logic2j.core.api.model.OperatorManager;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.solver.holder.SolutionHolder;
import org.logic2j.core.impl.io.operator.DefaultOperatorManager;
import org.logic2j.core.impl.theory.DefaultTheoryManager;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.core.impl.unify.DefaultUnifier;
import org.logic2j.core.impl.util.ReportUtils;
import org.logic2j.core.library.impl.core.CoreLibrary;
import org.logic2j.core.library.impl.io.IOLibrary;
import org.logic2j.core.library.mgmt.DefaultLibraryManager;

/**
 * Reference implementation of logic2j's {@link PrologImplementation} API.
 */
public class PrologReferenceImplementation implements PrologImplementation {
    /**
     * Describe the level of initialization of the Prolog system, between bare mimimum, and full-featured.
     * This enum is not in the {@link PrologImplementation} interface since it's particular to the reference implementation.
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

    private TermAdapter termAdapter = new DefaultTermAdapter(this);
    private final TermExchanger termExchanger = new DefaultTermExchanger(this);
    private final TheoryManager theoryManager = new DefaultTheoryManager(this);
    private LibraryManager libraryManager = new DefaultLibraryManager(this);
    private OperatorManager operatorManager = new DefaultOperatorManager();
    private Solver solver = new DefaultSolver(this);
    private Unifier unifier = new DefaultUnifier();

    /**
     * Default constructor will only provide an engine with the {@link CoreLibrary} loaded.
     */
    public PrologReferenceImplementation() {
        this(InitLevel.L1_CORE_LIBRARY);
    }

    /**
     * Constructor for a specific level of initialization.
     * 
     * @param theLevel
     */
    public PrologReferenceImplementation(InitLevel theLevel) {
        // Here we load libs in order

        /*
         * NOTE: the ConfigLibrary was part of the "core" and has been pushed to "contrib" since it was only used in rdb-related
         * configuration. I comment out this initialization since I don't want the "core" to depend on "contrib". We'll have to find a way
         * to allow easy loading of libs upon initialization. I would really rely on DI to care about this.
         */
        // (commented out since ConfigLibrary no longer on core):
        // libraryManager.loadLibrary(new ConfigLibrary(this));

        if (theLevel.ordinal() >= InitLevel.L1_CORE_LIBRARY.ordinal()) {
            final PLibrary lib = new CoreLibrary(this);
            this.libraryManager.loadLibrary(lib);
        }
        if (theLevel.ordinal() >= InitLevel.L2_BASE_LIBRARIES.ordinal()) {
            final PLibrary lib = new IOLibrary(this);
            this.libraryManager.loadLibrary(lib);
        }
    }

    // ---------------------------------------------------------------------------
    // Implementation of interface Prolog.
    // These are actually shortcuts or "syntactic sugars".
    // ---------------------------------------------------------------------------

    @Override
    public SolutionHolder solve(CharSequence theGoal) {
        final Object parsed = getTermExchanger().unmarshall(theGoal);
        return solve(parsed);
    }

    @Override
    public SolutionHolder solve(Object theGoal) {
        final Bindings theBindings = new Bindings(theGoal);
        return new SolutionHolder(this, theBindings);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // You may use DI to inject all sub-features into setters
    // TODO I think that we probably have to reinit/load the full engine's configuration everytime one setter is called, but we lack an
    // "afterPropertiesSet()" feature. We should use the standard @PostConstruct instead.
    // ---------------------------------------------------------------------------

    @Override
    public TermAdapter getTermAdapter() {
        return this.termAdapter;
    }

    public void setTermAdapter(TermAdapter termAdapter) {
        this.termAdapter = termAdapter;
    }

    @Override
    public TermExchanger getTermExchanger() {
        return this.termExchanger;
    }

    @Override
    public TheoryManager getTheoryManager() {
        return this.theoryManager;
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

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
