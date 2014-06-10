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
package org.logic2j.core.api.model.term;

import org.logic2j.core.api.model.exception.InvalidTermException;

import java.util.Collection;
import java.util.Comparator;

/**
 * This class represents a variable term. Variables are identified by a name (which must starts with an upper case letter) or the anonymous
 * ('_') name.
 * Note: This class MUST be immutable.
 */
public final class Var extends Term {
    private static final long serialVersionUID = 1L;

    public static final String WHOLE_SOLUTION_VAR_NAME = ".".intern();

    /**
     * Name of the anonymous variable is always "_". This constant is internalized, you
     * can safely compare it with ==.
     */
    public static final String ANONYMOUS_VAR_NAME = "_".intern();

    /**
     * Singleton anonymous variable. You can safely compare them with ==.
     */
    public static final Var ANONYMOUS_VAR = new Var();

    /**
     * Singleton "special" var that holds the value of a whole goal.
     */
    public static final Var WHOLE_SOLUTION_VAR = new Var(WHOLE_SOLUTION_VAR_NAME);

    public static final Comparator<Var> COMPARATOR_BY_NAME = new Comparator<Var>() {
        @Override
        public int compare(Var left, Var right) {
            return left.getName().compareTo(right.getName());
        }
    };

    /**
     * The immutable name of the variable, usually starting with uppercase when this Var was instantiated by the default parser, but when instantiated
     * by {@link #Var(CharSequence)} it can actually be anything (although it may not be the smartest idea).<br/>
     * A value of Var.ANONYMOUS_VAR_NAME means it's the anonymous variable<br/>
     * Note: all variables' names are internalized, i.e. it is legal to compare their names with ==.
     */
    private final String name;

    /**
     * Create the anonymous variable singleton.
     */
    private Var() {
        this.name = ANONYMOUS_VAR_NAME;
        this.index = NO_INDEX;  // Actually the default value but let's enforce that here
    }

    /**
     * Creates a variable identified by a name.
     * <p/>
     * The name must starts with an upper case letter or the underscore. If an underscore is specified as a name, the variable is anonymous.
     *
     * @param theName is the name
     * @throws InvalidTermException if n is not a valid Prolog variable name
     * @note Internally the {@link #name} is {@link String#intern()}alized so it's OK to compare by reference.
     */
    public Var(CharSequence theName) {
        if (theName == Var.ANONYMOUS_VAR_NAME) {
            throw new InvalidTermException("Must not instantiate the anonymous variable (which is a singleton)!");
        }
        if (theName == null) {
            throw new InvalidTermException("Name of a variable cannot be null");
        }
        final String str = theName.toString();
        if (str.isEmpty()) {
            throw new InvalidTermException("Name of a variable may not be the empty String");
        }
        this.name = str.intern();
    }

    /**
     * Copy constructor. OOPs - we must not copy the anonymous!
     * Clones the name and the index.
     *
     * @param original
     */
    public Var(Var original) {
        if (original.name == Var.ANONYMOUS_VAR_NAME) {
            throw new InvalidTermException("Cannot clone the anonymous variable via a copy constructor!");
        }
        this.name = original.name;
        this.index = original.getIndex();
    }

    /**
     * Gets the name of the variable.
     *
     * @note Names are {@link String#intern()}alized so OK to check by reference (with ==)
     */
    public String getName() {
        return this.name;
    }

    /**
     * Tests if this variable is anonymous.
     */
    public boolean isAnonymous() {
        return this.name == ANONYMOUS_VAR_NAME; // Names are {@link String#intern()}alized so OK to check by reference
    }


    // ---------------------------------------------------------------------------
    // TermVisitor
    // ---------------------------------------------------------------------------

    @Override
    public <T> T accept(TermVisitor<T> theVisitor) {
        return theVisitor.visit(this);
    }

    // ---------------------------------------------------------------------------
    // Template methods defined in abstract class Term
    // ---------------------------------------------------------------------------

    /**
     * Just add this to theCollectedTerms and set {@link Term#index} to {@link Term#NO_INDEX}.
     *
     * @param theCollectedTerms
     */
    void collectTermsInto(Collection<Object> theCollectedTerms) {
        this.index = NO_INDEX;
        theCollectedTerms.add(this);
    }

    Object factorize(Collection<Object> theCollectedTerms) {
        // If this term already has an equivalent in the provided collection, return that one
        final Object alreadyThere = findStructurallyEqualWithin(theCollectedTerms);
        if (alreadyThere != null) {
            return alreadyThere;
        }
        // Not found by structural equality, we match variables by their name
        // TODO I'm not actually sure why we do this - we should probably log and identify why this case
        for (final Object term : theCollectedTerms) {
            if (term instanceof Var) {
                final Var var = (Var) term;
                if (this.getName().equals(var.getName())) {
                    return var;
                }
            }
        }
        return this;
    }

    /**
     * @param theOther
     * @return true only when references are the same, otherwise two distinct {@link Var}s will always be considered different, despite
     * their name, index, or whatever.
     */
    boolean structurallyEquals(Object theOther) {
        return theOther == this; // Check memory reference only
    }

    /**
     * Assign a new {@link Term#index} to a Var if it was not assigned before.
     */
    int assignIndexes(int theIndexOfNextNonIndexedVar) {
        if (this.index != NO_INDEX) {
            // assert false : "We are re-indexing an indexed Var but return a wrong value";
            // Already assigned, avoid changing the index! Do nothing
            return theIndexOfNextNonIndexedVar; // return same index since we did nothing
        }
        if (isAnonymous()) {
            // Anonymous variable is not a var, don't count it, but assign an
            // index that is different from NO_INDEX but that won't be ever used
            this.index = ANON_INDEX;
            return theIndexOfNextNonIndexedVar; // return same index since we did nothing
        }
        // Index this var
        this.index = (short) theIndexOfNextNonIndexedVar;
        return theIndexOfNextNonIndexedVar + 1;
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public int hashCode() {
        return this.name.hashCode() ^ this.index;
    }

    /**
     * Equality is done by name - but does that make any sense?
     */
    @Override
    public boolean equals(Object other) {
        if (other==this) {
            return true;
        }
        if (!(other instanceof Var)) {
            return false;
        }
        final Var that = (Var) other;
        return this.name == that.name && this.index == that.index; // Names are {@link String#intern()}alized so OK to check by reference
    }

    @Override
    public String toString() {
        if (logger.isDebugEnabled()) {
            return this.name + '#' + this.getIndex();
        }
        return this.name;
    }

}
