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

import org.logic2j.core.api.*;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.api.solver.holder.SolutionHolder;
import org.logic2j.core.impl.theory.DefaultTheoryManager;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.core.impl.util.ReportUtils;
import org.logic2j.core.library.impl.core.CoreLibrary;
import org.logic2j.core.library.impl.io.IOLibrary;
import org.logic2j.core.library.mgmt.DefaultLibraryManager;

/**
 * Reference implementation of logic2j's {@link PrologImplementation} API.
 */
public class PrologReferenceImplementation implements PrologImplementation {
    public static final boolean PROFILING = true;

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
         * This is the default initialization level, it loads the {@link org.logic2j.core.library.impl.core.CoreLibrary}, containing (among others), core predicates such as
         * true/1, fail/1, !/1 (cut), =/2, call/1, not/1, etc.
         */
        L1_CORE_LIBRARY,
        /**
         * Higher level libraries loaded, such as {@link org.logic2j.core.library.impl.io.IOLibrary}.
         */
        L2_BASE_LIBRARIES,
    }

    // ---------------------------------------------------------------------------
    // Define all sub-features of this reference implementation and initialize with default, reference implementations
    // ---------------------------------------------------------------------------

    private TermAdapter termAdapter = new DefaultTermAdapter();

    private TermMarshaller termMarshaller = new DefaultTermMarshaller();

    private DefaultTermUnmarshaller termUnmarshaller = new DefaultTermUnmarshaller();

    private final TheoryManager theoryManager = new DefaultTheoryManager(this);

    private LibraryManager libraryManager = new DefaultLibraryManager(this);

    private OperatorManager operatorManager = new DefaultOperatorManager();

    private Solver solver = new DefaultSolver(this);

    /**
     * Default constructor will only provide an engine with the {@link org.logic2j.core.library.impl.core.CoreLibrary} loaded.
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
        if (theLevel.ordinal() >= InitLevel.L1_CORE_LIBRARY.ordinal()) {
            final PLibrary lib = new CoreLibrary(this);
            this.libraryManager.loadLibrary(lib);
        }
        if (theLevel.ordinal() >= InitLevel.L2_BASE_LIBRARIES.ordinal()) {
            final PLibrary lib = new IOLibrary(this);
            this.libraryManager.loadLibrary(lib);
        }
        this.termUnmarshaller.setNormalizer(new TermMapper() {
            @Override
            public Object apply(Object theTerm) {
                return TermApi.normalize(theTerm, getLibraryManager().wholeContent());
            }
        });
        this.termUnmarshaller.setOperatorManager(getOperatorManager());
    }

    // ---------------------------------------------------------------------------
    // Implementation of interface Prolog.
    // These are actually shortcuts or "syntactic sugars".
    // ---------------------------------------------------------------------------

    @Override
    public GoalHolder solve(CharSequence theGoal) {
        final Object parsed = termUnmarshaller.unmarshall(theGoal);
        return solve(parsed);
    }


    @Override
    public GoalHolder solve(Object theGoal) {
        return new GoalHolder(this, theGoal);
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

    @Override
    public void setTermAdapter(TermAdapter theTermAdapter) {
        this.termAdapter = theTermAdapter;
    }

    @Override
    public TheoryManager getTheoryManager() {
        return this.theoryManager;
    }

    @Override
    public LibraryManager getLibraryManager() {
        return this.libraryManager;
    }

    public void setLibraryManager(LibraryManager theLibraryManager) {
        this.libraryManager = theLibraryManager;
    }

    @Override
    public Solver getSolver() {
        return this.solver;
    }

    @Override
    public OperatorManager getOperatorManager() {
        return this.operatorManager;
    }

    public TermMarshaller getTermMarshaller() {
        return this.termMarshaller;
    }

    public void setTermMarshaller(TermMarshaller newOne) {
        this.termMarshaller = newOne;
    }

    @Override
    public TermUnmarshaller getTermUnmarshaller() {
        return this.termUnmarshaller;
    }

    public void setOperatorManager(OperatorManager theOperatorManager) {
        this.operatorManager = theOperatorManager;
    }

    public void setSolver(Solver theSolver) {
        this.solver = theSolver;
    }


    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }


}
