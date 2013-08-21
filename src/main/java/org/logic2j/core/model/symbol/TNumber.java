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

import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;

/**
 * {@link TNumber}s are numeric {@link Term}s.
 */
public abstract class TNumber extends Term implements Comparable<TNumber> {
    private static final long serialVersionUID = 1L;

    /**
     * Returns the value of the number as long
     */
    public abstract long longValue();

    /**
     * Returns the value of the number as double
     */
    public abstract double doubleValue();

    // ---------------------------------------------------------------------------
    // Template methods defined in abstract class Term
    // ---------------------------------------------------------------------------

    @Override
    final public boolean isList() {
        return false;
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
        // If this term already has an equivalent in the provided collection,
        // return that one
        final Term alreadyThere = findStructurallyEqualWithin(theCollectedTerms);
        if (alreadyThere != null) {
            return alreadyThere;
        }
        return this;
    }

    @Override
    public Var findVar(String theVariableName) {
        return null;
    }

    /**
     * No substitution occurs on literals.
     */
    @Override
    protected Term substitute(Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
        return this;
    }

    /**
     * @param theOther
     * @return true when references are the same, or when values represented by the associated numbers are the same.
     */
    @Override
    public boolean structurallyEquals(Term theOther) {
        if (theOther == this) {
            return true; // Same reference
        }
        return equals(theOther);
    }

    @Override
    public short assignIndexes(short theIndexOfNextNonIndexedVar) {
        // Don't leave the default NO_INDEX value otherwise this term won't be considered properly normalized.
        this.index = 0;
        return theIndexOfNextNonIndexedVar; // return same index since we did not assign a new Var's index
    }

}
