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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.logic2j.core.TermFactory;
import org.logic2j.core.TermFactory.FactoryMode;
import org.logic2j.core.library.mgmt.LibraryContent;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.util.ReflectUtils;

/**
 * Facade API to the {@link Term} hierarchy, to ease their handling. This class resides in the same package than the {@link Term} subclasses, so they can invoke
 * its package-scoped methods. See important notes re. Term factorization ({@link #factorize(Term)}) and normalization ({@link #normalize(Term, LibraryContent)}.
 * 
 * @note This class knows about the subclasses of {@link Term}, it breaks the OO design pattern a little but avoid defining many methods there. I find it
 *       acceptable since subclasses of {@link Term} don't sprout every day and are not for end-user extension.
 * @note Avoid static methods, prefer instantiating this class where needed.
 */
public class TermApi {

    public boolean isAtom(Term theTerm) {
        if (!(theTerm instanceof Struct)) {
            return false;
        }
        final Struct s = (Struct) theTerm;
        return s.getArity() == 0 || s.isEmptyList();
    }

    /**
     * Recursively collect all terms at and under theTerm, and also initialize their {@link #index} to {@link #NO_INDEX}.
     * For example for a structure "s(a,b(c),d(b(a)),X,X,Y)", the result will hold [a, c, b(c), b(a), c(b(a)), X, X, Y]
     * @param theTerm
     * @return A collection of terms, never empty. Same terms may appear multiple times. 
     */
    Collection<Term> collectTerms(Term theTerm) {
        final Collection<Term> collection = new ArrayList<Term>();
        theTerm.collectTermsInto(collection);
        return collection;
    }

    /**
     * Factorize a {@link Term}, this means recursively traversing the {@link Term} structure and assigning any duplicates substructures to the same references.
     * 
     * @param theTerm
     * @return The factorized term, may be same as argument theTerm in case nothing was needed, or a new object.
     */
    Term factorize(Term theTerm) {
        final Collection<Term> collection = collectTerms(theTerm);
        return theTerm.factorize(collection);
    }

    void avoidCycle(Struct theClause) {
        List<Term> visited = new ArrayList<Term>(20);
        theClause.avoidCycle(visited);
    }

    /**
     * Assign the {@link Term#index} value for any {@link Term} hierarchy.
     * 
     * @param theTerm
     * @return The number of variables found (recursively).
     */

    short assignIndexes(Term theTerm) {
        return theTerm.assignIndexes((short) 0); // Start assigning indexes with zero
    }

    /**
     * Normalize a {@link Term} using the specified definitions of operators, primitives.
     * 
     * @note This method must EXCLUSIVELY be called from {@link TermFactory#normalize(Term)}, with the exception of test cases.
     * @param theTerm To be normalized
     * @param theLibraryContent Defines primitives to be recognized
     * @return A normalized COPY of theTerm ready to be used for inference (in a Theory ore as a goal)
     */
    public Term normalize(Term theTerm, LibraryContent theLibraryContent) {
        final Term factorized = factorize(theTerm);
        assignIndexes(factorized);
        if (factorized instanceof Struct && theLibraryContent != null) {
            ((Struct) factorized).assignPrimitiveInfo(theLibraryContent);
        }
        return factorized;
    }

    /**
     * Substitute, recursively, any bound {@link Var}s to their actual values. This delegates the call to {@link Term#substitute(Bindings, IdentityHashMap)}.
     * 
     * @param theTerm
     * @param theBindings
     * @param theBindingsToVars
     * @return An equivalent Term with all bound variables pointing to literals, this implies a deep cloning of substructures that contain variables. When no
     *         variables are bound, then the same refernce is returned. Important note: the caller cannot know if the returned reference was cloned or not, so
     *         it must never mutate it!
     */
    public Term substitute(Term theTerm, final Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) throws InvalidTermException {
        if ((theTerm instanceof Struct && theTerm.index == 0) || theBindings.getSize() == 0) {
            // No variables identified in the term, or no variables passed as argument: do not need to substitute
            return theTerm;
        }
        // Delegate to actual subclass
        return theTerm.substitute(theBindings, theBindingsToVars);
    }

    /**
     * Lowest-level factory for simple {@link Term}s from plain Java {@link Object}s. 
     * @note This method is not for unmarshalling (parsing) from {@link String}s into {@link Term}s; use the {@link TermFactory} instead.
     * 
     * @param theObject
     * @param theMode
     * @return An instance of a subclass of {@link Term}.
     * @throws InvalidTermException
     */
    public Term valueOf(Object theObject, FactoryMode theMode) throws InvalidTermException {
        if (theObject == null) {
            throw new InvalidTermException("Cannot create Term from null argument");
        }
        final Term term;
        if (theObject instanceof Term) {
            term = (Term) theObject;
        } else if (theObject instanceof Integer) {
            term = new TLong((Integer) theObject);
        } else if (theObject instanceof Long) {
            term = new TLong((Long) theObject);
        } else if (theObject instanceof Double) {
            term = new TDouble((Double) theObject);
        } else if (theObject instanceof Float) {
            term = new TDouble((Float) theObject);
        } else if (theObject instanceof Boolean) {
            term = (Boolean) theObject ? Struct.ATOM_TRUE : Struct.ATOM_FALSE;
        } else if (theObject instanceof CharSequence || theObject instanceof Character) {
            // Rudimentary parsing
            final String chars = theObject.toString();
            if (theMode == FactoryMode.ATOM) {
                // Anything becomes an atom, actually only a Struct since we don't have powerful parsing here
                term = new Struct(chars);
            } else {
                if (Var.ANONYMOUS_VAR_NAME.equals(chars)) {
                    term = Var.ANONYMOUS_VAR;
                } else if (chars.isEmpty()) {
                    // Dubious for real programming, but data sources may contain empty fields, and this is the only way to represent them as a Term
                    term = new Struct("");
                } else if (Character.isUpperCase(chars.charAt(0)) || chars.startsWith(Var.ANONYMOUS_VAR_NAME)) {
                    term = new Var(chars);
                } else {
                    // Otherwise it's an atom
                    term = new Struct(chars);
                }
            }
        } else {
            throw new InvalidTermException("Cannot create Term from '" + theObject + "' of " + theObject.getClass());
        }
        return term;
    }

    /**
     * @param theTPathExpression
     * @param theClass
     */
    // TODO Should this go to TermFactory - since we return a new Term
    @SuppressWarnings("unchecked")
    public <T extends Term> T selectTerm(Term theTerm, String theTPathExpression, Class<T> theClass) {
        if (theTPathExpression.isEmpty()) {
            return ReflectUtils.safeCastNotNull("selecting term", theTerm, theClass);
        }
        if (theTerm instanceof Struct) {
            final Struct s = (Struct) theTerm;
            int position = 0;
            String level0 = theTPathExpression;
            int end = theTPathExpression.length();
            final int slash = theTPathExpression.indexOf('/');
            if (slash >= 1) {
                end = slash;
                level0 = theTPathExpression.substring(0, slash);
                position = 1;
            }
            String functor = level0;
            final int par = level0.indexOf('[');
            if (par >= 0) {
                end = max(par, end);
                functor = level0.substring(0, par);
                if (!level0.endsWith("]")) {
                    throw new InvalidTermException("Malformed TPath expresson: \"" + theTPathExpression + "\": missing ending ']'");
                }
                position = Integer.parseInt(level0.substring(par + 1, level0.length() - 1));
                if (position <= 0) {
                    throw new InvalidTermException("Index " + position + " in \"" + theTPathExpression + "\" is <=0");
                }
                if (position > s.getArity()) {
                    throw new InvalidTermException("Index " + position + " in \"" + theTPathExpression + "\" is > arity of " + s.getArity());
                }
            }
            // In case functor was defined ("f[n]", since the expression "[n]" without f is also allowed)
            if (!functor.isEmpty()) {
                // Make sure the root name matches the struct at level 0
                if (!s.getName().equals(functor)) {
                    throw new InvalidTermException("Term \"" + theTerm + "\" does not start with functor  \"" + functor + '"');
                }
            }
            if (position >= 1) {
                String levelsTail = theTPathExpression.substring(min(theTPathExpression.length(), end + 1));
                return selectTerm(s.getArg(position - 1), levelsTail, theClass);
            }
            if (!(theClass.isAssignableFrom(theTerm.getClass()))) {
                throw new ClassCastException("Cannot extract Term of " + theClass + " at expression=" + theTPathExpression + " from " + theTerm);
            }
            return (T) theTerm;
        }
        throw new IllegalArgumentException("Cannot extract \"" + theTPathExpression + "\" from " + theTerm);
    }

}
