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
package org.logic2j.core.model.symbol;

import java.util.Collection;
import java.util.IdentityHashMap;

import org.logic2j.core.model.TermVisitor;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;

/**
 * This class represents a variable term. Variables are identified by a name (which must starts with an upper case letter) or the anonymous ('_') name.
 */
public class Var extends Term {
    private static final long serialVersionUID = 1L;

    public static final String ANONYMOUS_VAR_NAME = "_".intern(); // TODO
                                                                  // Document
                                                                  // this
    public static final Term ANONYMOUS_VAR = new Var();

    /**
     * The name of the variable, usually starting with uppercase when this Var was instantiated by the default parser, but when instantiated by
     * {@link #Var(String)} it may actually be anything (although it may not be the smartest idea).<br/>
     * A value of null means it's the anonymous variable (even though the anonymous variable is formatted as "_")<br/>
     * Note: all variable names are internalized, i.e. it is legal to compare them with ==.
     */
    private String name;

    /**
     * Creates a variable identified by a name.
     * 
     * The name must starts with an upper case letter or the underscore. If an underscore is specified as a name, the variable is anonymous.
     * @note Internally the {@link #name} is {@link String#intern()}alized so it's OK to compare by reference.
     * @param theName is the name
     * @throws InvalidTermException if n is not a valid Prolog variable name
     */
    public Var(String theName) {
        this.name = theName.intern();
    }

    /**
     * Create an anonymous variable.
     */
    private Var() {
        this.name = ANONYMOUS_VAR_NAME;
    }

    /**
     * Gets the name of the variable.
     * @note Names are {@link String#intern()}alized so OK to check by reference
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

    /**
     * Obtain the current {@link Binding} of this Var from the {@link Bindings}. Notice that the variable index must have been assigned, and this var must NOT
     * be the anonymous variable (that cannot be bound to anything).
     * 
     * @param theBindings
     * @return The current binding of this Var.
     */
    public Binding bindingWithin(Bindings theBindings) {
        if (this.index < 0) {
            // An error situation
            if (this.index == NO_INDEX) {
                // TODO Should throw a subclass of PrologException
                throw new IllegalStateException("Cannot dereference variable whose index was not initialized");
            }
            if (this.index == ANON_INDEX) {
                // TODO Should throw a subclass of PrologException
                throw new IllegalStateException("Cannot dereference the anonymous variable");
            }
        }
        if (this.index >= theBindings.getSize()) {
            // TODO Should throw a subclass of PrologException
            throw new IllegalStateException("Bindings " + theBindings + " has space for " + theBindings.getSize() + " bindings, trying to dereference " + this + " at index " + this.index);
        }
        return theBindings.getBinding(this.index);
    }

    // ---------------------------------------------------------------------------
    // Template methods defined in abstract class Term
    // ---------------------------------------------------------------------------

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public Var findVar(String theVariableName) {
        if (ANONYMOUS_VAR_NAME.equals(theVariableName)) {
            // TODO Should throw a subclass of PrologException
            throw new IllegalArgumentException("Cannot find the anonymous variable");
        }
        if (theVariableName.equals(getName())) {
            return this;
        }
        return null;
    }

    @Override
    protected Term substitute(Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
        if (isAnonymous()) {
            // Anonymous variable is never bound - won't substitute
            return this;
        }
        final Binding binding = bindingWithin(theBindings).followLinks();
        switch (binding.getType()) {
        case LIT:
            // For a literal, we have a reference to the literal term and to its
            // own variables,
            // so recurse further
            return binding.getTerm().substitute(binding.getLiteralBindings(), theBindingsToVars);
        case FREE:
            // Free variable has no value, so substitution ends up on the last
            // variable of the chain
            if (theBindingsToVars != null) {
                final Var originVar = theBindingsToVars.get(binding);
                if (originVar != null) {
                    return originVar;
                }
                return ANONYMOUS_VAR;
            }
            // Return the free variable
            return this;
        default:
            // In case of LINK: that's impossible since we have followed the
            // complete linked chain
            // TODO Should throw a subclass of PrologException
            throw new IllegalStateException("substitute() internal error");
        }
    }

    /**
     * Just add this to theCollectedTerms and set {@link Term#index} to {@link Term#NO_INDEX}.
     * 
     * @param theCollectedTerms
     */
    @Override
    protected void collectTermsInto(Collection<Term> theCollectedTerms) {
        this.index = NO_INDEX;
        theCollectedTerms.add(this);
    }

    @Override
    protected Term factorize(Collection<Term> theCollectedTerms) {
        // If this term already has an equivalent in the provided collection, return that one
        final Term alreadyThere = findStructurallyEqualWithin(theCollectedTerms);
        if (alreadyThere != null) {
            return alreadyThere;
        }
        // Not found by structural equality, we match variables by their name
        // TODO I'm not actually sure why we do this - we should probably log and identify why this case
        for (final Term term : theCollectedTerms) {
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
     * @return true only when references are the same, otherwise two distinct {@link Var}s will always be considered different, despite their name, index, or
     *         whatever.
     */
    @Override
    public boolean structurallyEquals(Term theOther) {
        return theOther == this; // Check memory reference only
    }

    /**
     * Assign a new {@link Term#index} to a Var if it was not assigned before.
     */
    @Override
    protected short assignIndexes(short theIndexOfNextNonIndexedVar) {
        if (this.index != NO_INDEX) {
            // Already assigned, do nothing
            return theIndexOfNextNonIndexedVar; // return same index since we did nothing
        }
        if (isAnonymous()) {
            // Anonymous variable is not a var, don't count it, but assign an
            // index that is different from NO_INDEX but that won't be ever used
            this.index = ANON_INDEX;
            return theIndexOfNextNonIndexedVar; // return same index since we did nothing
        }
        // Index this var
        this.index = theIndexOfNextNonIndexedVar;
        return ++theIndexOfNextNonIndexedVar;
    }

    @Override
    public <T> T accept(TermVisitor<T> theVisitor) {
        return theVisitor.visit(this);
    }

    // ---------------------------------------------------------------------------
    // Core java.lang.Object methods
    // ---------------------------------------------------------------------------

    /**
     * Equality is done by name.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Var)) {
            return false;
        }
        final Var that = (Var) other;
        return this.name == that.name; // Names are {@link String#intern()}alized so OK to check by reference
    }

    @Override
    public int hashCode() {
        if (this.name == null) {
            // Anonymous var
            return 0;
        }
        return this.name.hashCode();
    }

}
