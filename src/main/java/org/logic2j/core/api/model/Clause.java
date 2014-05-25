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
import org.logic2j.core.impl.PrologImplementation;

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

    /**
     * The {@link Struct} that represents the content of either a rule (when it has a body) or a fact (does not have a body - or has "true"
     * as body), see description of {@link #isFact()}.
     */
    private final Object content; // Immutable, not null

    private final boolean isFact;
    private final boolean isWithClauseFunctor;

    private final Object head;
    private final Object body;

    /**
     * Make a Term (must be a Struct) read for inference, this requires to normalize it.
     *
     * @param theProlog Required to normalize theClauseTerm according to the current libraries.
     * @param theClauseTerm
     */
    public Clause(PrologImplementation theProlog, Object theClauseTerm) {
        // if (!(theClauseTerm instanceof Struct)) {
        // throw new InvalidTermException("Need a Struct to build a clause, not " + theClauseTerm);
        // }
        // Any Clause must be normalized otherwise we won't be able to infer on it!
        this.content = TermApi.normalize(theClauseTerm, theProlog.getLibraryManager().wholeContent());
        this.isFact = evaluateIsFact();
        this.isWithClauseFunctor = evaluateIsWithClauseFunctor();
        this.head = evaluateHead();
        this.body = evaluateBody();
    }

    /**
     * Copy constructor. Will clone the {@link Clause}'s content and the current {@link TermBindings}.
     *
     * @param theOriginal
     */
    public Clause(Clause theOriginal) {
        if (theOriginal.content instanceof Struct) {
            // TODO Why do we need cloning the content in a structure-sharing design???
            this.content = new Struct((Struct) theOriginal.content);
        } else {
            this.content = theOriginal;
        }
        // Clone the block of variables
        this.isFact = theOriginal.isFact;
        this.isWithClauseFunctor = theOriginal.isWithClauseFunctor;
        this.head = theOriginal.head;
        this.body = theOriginal.body;
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
        if (!(this.content instanceof Struct)) {
            return true;
        }
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
        if (this.content instanceof String) {
            return false;
        }
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

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public boolean isFact() {
        return this.isFact;
    }

    public Object getContent() {
        return content;
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
