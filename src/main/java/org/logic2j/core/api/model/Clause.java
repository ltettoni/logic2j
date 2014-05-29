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
package org.logic2j.core.api.model;

import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.util.ProfilingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Represents a fact or a rule in a Theory; this is described by a {@link Struct}. This class provides extra features for efficient lookup
 * and matching by {@link org.logic2j.core.impl.theory.TheoryManager}s. We implement by composition (by wrapping a {@link Struct}), not by derivation. Simple facts may
 * be represented in two manners:
 * <ol>
 * <li>A Struct with any functor different from ':-' (this is recommended and more optimal)</li>
 * <li>A Struct with functor ':-' and "true" as body (this is less optimal because this is actually a rule)</li>
 * </ol>
 */
public class Clause {
    private static final Logger logger = LoggerFactory.getLogger(Clause.class);
    /**
     * The {@link Struct} that represents the content of either a rule (when it has a body) or a fact (does not have a body - or has "true"
     * as body), see description of {@link #isFact()}.
     */
    private final Struct content; // Immutable, not null
    private final Var[] vars;

    private final boolean isFact;
    private final boolean isWithClauseFunctor;

    private final Object head;
    private final Object body;

    private TreeMap<Integer, Clause> cache;

    /**
     * Make a Term (must be a Struct) read for inference, this requires to normalize it.
     *
     * @param theProlog Required to normalize theClauseTerm according to the current libraries.
     * @param theClauseTerm
     */
    public Clause(PrologImplementation theProlog, Struct theClauseTerm) {
        // if (!(theClauseTerm instanceof Struct)) {
        // throw new InvalidTermException("Need a Struct to build a clause, not " + theClauseTerm);
        // }
        // Any Clause must be normalized otherwise we won't be able to infer on it!
        this.content = (Struct)TermApi.normalize(theClauseTerm, theProlog.getLibraryManager().wholeContent());
        // Store vars into an array, indexed by the var's index
        final Set<Var> varSet = TermApi.allVars(content).keySet();
        this.vars = new Var[varSet.size()];
        for (Var var : varSet) {
            this.vars[var.getIndex()] = var;
        }
        this.isFact = evaluateIsFact();
        this.isWithClauseFunctor = evaluateIsWithClauseFunctor();
        this.head = evaluateHead();
        this.body = evaluateBody();
    }


    private Clause(Clause original, Struct cloned, Var[] clonedVars) {
        this.content = cloned;
        this.vars = clonedVars;
        this.cache = null; // That one should never be modified - we are on a clone
        this.isFact = original.isFact;
        this.isWithClauseFunctor = original.isWithClauseFunctor;
        this.head = evaluateHead();
        this.body = evaluateBody();
    }


    public Clause cloned(PoV pov) {
        if (this.cache==null) {
            this.cache = new TreeMap<Integer, Clause>();
//            logger.warn("Instantiating Clause cache for {}", this.content);
        }
        final Map.Entry<Integer, Clause> ceilingEntry = this.cache.ceilingEntry(pov.topVarIndex);
        if (ceilingEntry==null) {
//            logger.warn("Cloning {}", this);
            // No such entry: create and insert
            final Clause clonedClause = cloneClauseAndRemapIndexes2(this, pov);
            final int initialVarIndex = clonedClause.vars[0].getIndex(); // There MUST be at least one var otherwise we would not be cloning
            this.cache.put(initialVarIndex, clonedClause);
            return clonedClause;
        }
        final Clause reused = ceilingEntry.getValue();
        pov.topVarIndex = reused.vars[reused.vars.length-1].getIndex() + 1;
//        logger.warn("Reusing cloned clause {}", reused);
        return reused;
    }



    public Clause cloneClauseAndRemapIndexes2(Clause theClause, PoV pov) {
        ProfilingInfo.counter1++;
//            audit.info("Clone  {}  (base={})", content, this.topVarIndex);
        final Var[] originalVars = theClause.getVars();
        final int nbVars = originalVars.length;
        // Allocate the new vars by cloning the original ones. Index is preserved meaning that
        // when we traverse the original structure and find a Var of index N, we can replace it
        // in the cloned structure by the clonedVar[N]
        final Var[] clonedVars = new Var[nbVars];
        for (int i = 0; i < nbVars; i++) {
            clonedVars[i] = new Var(originalVars[i]);
        }

        final Struct cloned = cloneStruct(theClause.getContent(), clonedVars);
        // Now reindex the cloned vars
        for (int i = 0; i < nbVars; i++) {
            clonedVars[i].index += pov.topVarIndex;
        }
        // And increment the highest Var index accordingly
        pov.topVarIndex += nbVars;

        if (pov.topVarIndex > ProfilingInfo.max1) {
            ProfilingInfo.max1 = pov.topVarIndex;
        }
//    audit.info("Cloned {}  (base={})", cloned, this.topVarIndex);
        return new Clause(theClause, cloned, clonedVars);
    }

    private Struct cloneStruct(Struct theStruct, Var[] clonedVars) {
        final Object[] args = theStruct.getArgs();
        final int arity = args.length;
        final Object[] clonedArgs = new Object[arity];
        for (int i = 0; i < arity; i++) {
            final Object arg = args[i];
            if (arg instanceof Struct) {
                final Struct recursedClonedElement = cloneStruct((Struct) arg, clonedVars);
                clonedArgs[i] = recursedClonedElement;
            } else if (arg instanceof Var && arg != Var.ANONYMOUS_VAR) {
                final short originalVarIndex = ((Var) arg).getIndex();
                clonedArgs[i] = clonedVars[originalVarIndex];
            } else {
                clonedArgs[i] = arg;
            }
        }
        final Struct struct = new Struct(theStruct, clonedArgs);
        return struct;
    }

    // ---------------------------------------------------------------------------
    // Methods to denormalize immutable fields
    // ---------------------------------------------------------------------------

    /**
     * Use this method to determine if the {@link Clause} is a fact, before calling {@link #getBody()} that would return "true" and entering
     * a sub-goal demonstration.
     *
     * @return True if the clause is a fact: if the {@link Struct} does not have ":-" as functor, or if the body is "true".
     */
    public boolean evaluateIsFact() {
        final Struct s = (Struct) this.content;
        // TODO (issue) Cache this value, see https://github.com/ltettoni/logic2j/issues/16
        if (Struct.FUNCTOR_CLAUSE != s.getName()) { // Names are {@link String#intern()}alized so OK to check by reference
            return true;
        }
        // We know it's a clause functor, arity must be 2, check the body
        final Object clauseBody = s.getRHS();
        if (Struct.ATOM_TRUE.equals(clauseBody)) {
            return true;
        }
        // The body is more complex, it's certainly a rule
        return false;
    }

    private boolean evaluateIsWithClauseFunctor() {
        return Struct.FUNCTOR_CLAUSE == ((Struct) (this.content)).getName(); // Names are {@link String#intern()}alized so OK to check by
                                                                             // reference
    }

    /**
     * Obtain the head of the clause: for facts, this is the underlying {@link Struct}; for rules, this is the first argument to the clause
     * functor.
     *
     * @return The clause's head as a {@link Term}, normally a {@link Struct}.
     */
    public Object evaluateHead() {
        if (this.isWithClauseFunctor) {
            return ((Struct) this.content).getLHS();
        }
        return this.content;
    }

    /**
     * @return The clause's body as a {@link Term}, normally a {@link Struct}.
     */
    public Object evaluateBody() {
        if (this.isWithClauseFunctor) {
            return ((Struct) this.content).getRHS();
        }
        return Struct.ATOM_TRUE;
    }


    public boolean needCloning() {
        if (! (this.content instanceof Struct)) {
            return false;
        }
        final Struct cs = (Struct)this.content;
        if (cs.getIndex()<=0) {
            // No variables!
            return false;
        }
        // Otherwise assume we need cloning
        return true;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public boolean isFact() {
        return this.isFact;
    }

    public Struct getContent() {
        return content;
    }

    public Var[] getVars() {
        return vars;
    }

    /**
     * Obtain the head of the clause: for facts, this is the underlying {@link Struct}; for rules, this is the first argument to the clause
     * functor.
     *
     * @return The clause's head as a {@link Term}, normally a {@link Struct}.
     */
    public Object getHead() {
        return this.head;
    }

    /**
     * Obtain the head of the clause: for facts, this is the underlying {@link Struct}; for rules, this is the first argument to the clause
     * functor.
     *
     * @return The clause's head as a {@link Term}, normally a {@link Struct}.
     */
    public Object getBody() {
        return this.body;
    }

    /**
     * @return The key that uniquely identifies the family of the {@link Clause}'s head predicate.
     */
    public String getPredicateKey() {
        return TermApi.getPredicateSignature(getHead());
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.content.toString();
    }

}
