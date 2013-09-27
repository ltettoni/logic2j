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
package org.logic2j.core.api.model.symbol;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.TermExchanger;
import org.logic2j.core.api.model.PartialTermVisitor;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.util.ReflectUtils;
import org.logic2j.core.library.mgmt.LibraryContent;

/**
 * Facade API to the {@link Term} hierarchy, to ease their handling. This class resides in the same package than the {@link Term}
 * subclasses, so they can invoke its package-scoped methods. See important notes re. Term factorization ({@link #factorize(Term)}) and
 * normalization ({@link #normalize(Term, LibraryContent)} .
 * 
 * @note This class knows about the subclasses of {@link Term}, it breaks the OO design pattern a little but avoid defining many methods
 *       there. I find it acceptable since subclasses of {@link Term} don't sprout every day and are not for end-user extension.
 * @note Avoid static methods, prefer instantiating this class where needed.
 */
public class TermApi {

    private TermApi() {
        // Forbid instantiation
    }

    public static <T> T accept(Object theTerm, PartialTermVisitor<T> theVisitor) {
        // TermVisitor
        if (theTerm instanceof Struct) {
            return theVisitor.visit((Struct) theTerm);
        }
        if (theTerm instanceof Var) {
            return theVisitor.visit((Var) theTerm);
        }
        // throw new PrologNonSpecificError("Should not happen here");
        // Extension
        if (theTerm instanceof String) {
            return theVisitor.visit((String) theTerm);
        }
        if (theTerm instanceof Long) {
            return theVisitor.visit((Long) theTerm);
        }
        if (theTerm instanceof Double) {
            return theVisitor.visit((Double) theTerm);
        }
        return theVisitor.visit(theTerm);
    }

    public static boolean isAtom(Object theTerm) {
        if (theTerm instanceof String) {
            // Now plain Strings are atoms!
            return true;
        }
        if (theTerm instanceof Struct) {
            final Struct s = (Struct) theTerm;
            return s.getArity() == 0 || s.isEmptyList();
        }
        // In the future
        // if (theTerm instanceof String) {
        // return true;
        // }
        return false;
    }

    /**
     * Recursively collect all terms and add them to the collectedTerms collection, and also initialize their {@link #index} to
     * {@link #NO_INDEX}. This is an internal template method: the public API entry point is {@link TermApi#collectTerms(Term)}; see a more
     * detailed description there.
     * 
     * @param collection Recipient collection, {@link Term}s add here.
     */
    public static void collectTermsInto(Object theTerm, Collection<Object> collection) {
        if (theTerm instanceof Struct) {
            ((Struct) theTerm).collectTermsInto(collection);
        } else if (theTerm instanceof Var) {
            ((Var) theTerm).collectTermsInto(collection);
        } else {
            // Not a Term but a plain Java object - won't collect
            collection.add(theTerm);
        }
    }

    /**
     * Recursively collect all terms at and under theTerm, and also initialize their {@link #index} to {@link #NO_INDEX}. For example for a
     * structure "s(a,b(c),d(b(a)),X,X,Y)", the result will hold [a, c, b(c), b(a), c(b(a)), X, X, Y]
     * 
     * @param theTerm
     * @return A collection of terms, never empty. Same terms may appear multiple times.
     */
    static Collection<Object> collectTerms(Object theTerm) {
        final ArrayList<Object> recipient = new ArrayList<Object>();
        collectTermsInto(theTerm, recipient);
        // Remove ourself from the result - we are always at the end of the collection
        recipient.remove(recipient.size() - 1);
        return recipient;
    }

    /**
     * Factorize a {@link Term}, this means recursively traversing the {@link Term} structure and assigning any duplicates substructures to
     * the same references.
     * 
     * @param theTerm
     * @return The factorized term, may be same as argument theTerm in case nothing was needed, or a new object.
     */
    static Object factorize(Object theTerm) {
        final Collection<Object> collection = collectTerms(theTerm);
        return factorize(theTerm, collection);
    }

    /**
     * Factorizing will either return a new {@link Term} or this {@link Term} depending if it already exists in the supplied Collection.
     * This will factorize duplicated atoms, numbers, variables, or even structures that are statically equal. A factorized {@link Struct}
     * will have all occurences of the same {@link Var}iable sharing the same object reference. This is an internal template method: the
     * public API entry point is {@link TermApi#factorize(Term)}; see a more detailed description there.
     * 
     * @param theCollectedTerms The reference Terms to search for.
     * @return Either this, or a new equivalent but factorized Term.
     */
    public static Object factorize(Object theTerm, Collection<Object> collection) {
        if (theTerm instanceof Struct) {
            return ((Struct) theTerm).factorize(collection);
        } else if (theTerm instanceof Var) {
            return ((Var) theTerm).factorize(collection);
        } else {
            // Not a Term but a plain Java object - won't factorize
            return theTerm;
        }
    }

    /**
     * Check structural equality, this means that the names of atoms, functors, arity and numeric values are all equal, that the same
     * variables are referred to, but irrelevant of the bound values of those variables.
     * 
     * @param theOther
     * @return true when theOther is structurally equal to this. Same references (==) will always yield true.
     */
    public static boolean structurallyEquals(Object theTerm, Object theOther) {
        if (theTerm instanceof Struct) {
            return ((Struct) theTerm).structurallyEquals(theOther);
        } else if (theTerm instanceof Var) {
            return ((Var) theTerm).structurallyEquals(theOther);
        } else {
            // Not a Term but a plain Java object - calculate equality
            return theTerm.equals(theOther);
        }
    }

    /**
     * Find the first instance of {@link Var} by name inside a Term, most often a {@link Struct}.
     * 
     * @param theVariableName
     * @return A {@link Var} with the specified name, or null when not found.
     */
    public static Var findVar(Object theTerm, String theVariableName) {
        if (theTerm instanceof Struct) {
            return ((Struct) theTerm).findVar(theVariableName);
        } else if (theTerm instanceof Var) {
            return ((Var) theTerm).findVar(theVariableName);
        } else {
            // Not a Term but a plain Java object - no var
            return null;
        }
    }

    /**
     * @return true if this Term denotes a Prolog list.
     */
    public static boolean isList(Object theTerm) {
        if (theTerm instanceof Struct) {
            return ((Struct) theTerm).isList();
        }
        return false;
    }

    /**
     * Assign the {@link Term#index} value for {@link Var} and {@link Struct}s. This is an internal template method: the public API entry
     * point is {@link TermApi#assignIndexes(Term)}; see a more detailed description there.
     * 
     * @param theIndexOfNextNonIndexedVar
     * @return The next value for theIndexOfNextNonIndexedVar, allow successive calls to increment.
     */
    public static short assignIndexes(Object theTerm, short theIndexOfNextNonIndexedVar) {
        if (theTerm instanceof Struct) {
            return ((Struct) theTerm).assignIndexes(theIndexOfNextNonIndexedVar);
        } else if (theTerm instanceof Var) {
            return ((Var) theTerm).assignIndexes(theIndexOfNextNonIndexedVar);
        } else {
            // Not a Term but a plain Java object - can't assign an index
            return theIndexOfNextNonIndexedVar;
        }
    }

    /**
     * Assign the {@link Term#index} value for any {@link Term} hierarchy.
     * 
     * @param theTerm
     * @return The number of variables found (recursively).
     */
    static short assignIndexes(Object theTerm) {
        return TermApi.assignIndexes(theTerm, (short) 0); // Start assigning indexes with zero
    }

    /**
     * A unique identifier that determines the family of the predicate represented by this {@link Struct}.
     * 
     * @return The predicate's name + '/' + arity for normal {@link Struct}, or just the toString() of any other Object
     */
    public static String getPredicateSignature(Object thePredicate) {
        if (thePredicate instanceof Struct) {
            return ((Struct) thePredicate).getPredicateSignature();
        }
        return String.valueOf(thePredicate) + "/0";
    }

    /**
     * 
     * @param theBindings
     * @param theBindingsToVars
     * @return An equivalent Term with all bound variables pointing to literals, this implies a deep cloning of substructures that contain
     *         variables. When no variables are bound, then the same refernce is returned. Important note: the caller cannot know if the
     *         returned reference was cloned or not, so it must never mutate it!
     */
    public static Object substitute(Object theTerm, Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
        if ((theTerm instanceof Struct && ((Struct) theTerm).index == 0) || theBindings.isEmpty()) {
            // No variables identified in the term, or no variables passed as argument: do not need to substitute
            return theTerm;
        }
        if (theTerm instanceof Struct) {
            return ((Struct) theTerm).substitute(theBindings, theBindingsToVars);
        } else if (theTerm instanceof Var) {
            return ((Var) theTerm).substitute(theBindings, theBindingsToVars);
        } else {
            // Not a Term but a plain Java object - can't assign an index
            return theTerm;
        }
    }

    // TODO Currently unused - but probably we should
    void avoidCycle(Struct theClause) {
        final List<Term> visited = new ArrayList<Term>(20);
        theClause.avoidCycle(visited);
    }

    /**
     * Normalize a {@link Term} using the specified definitions of operators, primitives.
     * 
     * @param theTerm To be normalized
     * @param theLibraryContent Defines primitives to be recognized
     * @return A normalized COPY of theTerm ready to be used for inference (in a Theory ore as a goal)
     */
    public static Object normalize(Object theTerm, LibraryContent theLibraryContent) {
        final Object factorized = factorize(theTerm);
        assignIndexes(factorized);
        if (factorized instanceof Struct && theLibraryContent != null) {
            ((Struct) factorized).assignPrimitiveInfo(theLibraryContent);
        }
        return factorized;
    }

    /**
     * Primitive factory for simple {@link Term}s from plain Java {@link Object}s, use this
     * with parcimony at low-level.
     * Higher-level must use {@link TermAdapter} or {@link TermExchanger} instead which can be
     * overridden and defined with user logic and features.
     * 
     * Character input will be converted to Struct or Var according to Prolog's syntax convention:
     * when starting with an underscore or an uppercase, this is a {@link Var}.
     * This method is not capable of instantiating a compound {@link Struct}, it may only create atoms.
     * 
     * @param theObject Should usually be {@link CharSequence}, {@link Number}, {@link Boolean}
     * @param theMode
     * @return An instance of a subclass of {@link Term}.
     * @throws InvalidTermException If theObject cannot be converted to a Term
     */
    public static Object valueOf(Object theObject, FactoryMode theMode) throws InvalidTermException {
        if (theObject == null) {
            throw new InvalidTermException("Cannot create Term from a null argument");
        }
        final Object result;
        if (theObject instanceof Term) {
            // Idempotence
            result = (Term) theObject;
        } else if (theObject instanceof Integer) {
            result = Long.valueOf(((Integer) theObject).longValue());
        } else if (theObject instanceof Long) {
            result = (Long) theObject;
        } else if (theObject instanceof Double) {
            result = (Double) theObject;
        } else if (theObject instanceof Float) {
            result = Double.valueOf(((Float) theObject).doubleValue());
        } else if (theObject instanceof Boolean) {
            result = (Boolean) theObject ? Struct.ATOM_TRUE : Struct.ATOM_FALSE;
        } else if (theObject instanceof CharSequence || theObject instanceof Character) {
            // Very very vary rudimentary parsing
            final String chars = theObject.toString();
            switch (theMode) {
            case ATOM:
                result = Struct.atom(chars);
                break;
            default:
                if (Var.ANONYMOUS_VAR_NAME.equals(chars)) {
                    result = Var.ANONYMOUS_VAR;
                } else if (chars.isEmpty()) {
                    // Dubious for real programming, but some data sources may contain empty fields, and this is the only way to represent
                    // them
                    // as a Term
                    result = new Struct("");
                } else if (Character.isUpperCase(chars.charAt(0)) || chars.startsWith(Var.ANONYMOUS_VAR_NAME)) {
                    // Use Prolog's convention re variables starting with uppercase or underscore
                    result = new Var(chars);
                } else {
                    // Otherwise it's an atom
                    result = new Struct(chars);
                }
                break;
            }
        } else if (theObject instanceof Number) {
            // Other types of numbers
            final Number nbr = (Number) theObject;
            if (nbr.doubleValue() % 1 != 0) {
                // Has floating point number
                result = Double.valueOf(nbr.doubleValue());
            } else {
                // Is just an integer
                result = Long.valueOf(nbr.longValue());
            }
        } else {
            throw new InvalidTermException("Cannot create Term from '" + theObject + "' of " + theObject.getClass());
        }
        return result;
    }

    /**
     * Extract one {@link Term} from within another ({@link Struct}) using a rudimentary XPath-like expression language.
     * 
     * @param theTerm To select from
     * @param theTPathExpression The expression to select from theTerm, see the associated TestCase for specification.
     * @param theClass The {@link Term} class or one of its subclass that the desired returned object should be.
     */
    // TODO Should this go to TermAdapter instead? - since we return a new Term
    @SuppressWarnings("unchecked")
    public static <T extends Object> T selectTerm(Object theTerm, String theTPathExpression, Class<T> theClass) {
        if (theTPathExpression.isEmpty()) {
            return ReflectUtils.safeCastNotNull("selecting term", theTerm, theClass);
        }
        if (theTerm instanceof String) {
            if (!((String) theTerm).equals(theTPathExpression)) {
                throw new InvalidTermException("Term \"" + theTerm + "\" cannot match expression \"" + theTPathExpression + '"');
            }
            return ReflectUtils.safeCastNotNull("selecting term", theTerm, theClass);
        }

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
            final String levelsTail = theTPathExpression.substring(min(theTPathExpression.length(), end + 1));
            return selectTerm(s.getArg(position - 1), levelsTail, theClass);
        }
        if (!(theClass.isAssignableFrom(theTerm.getClass()))) {
            throw new PrologNonSpecificError("Cannot extract Term of " + theClass + " at expression=" + theTPathExpression + " from " + theTerm);
        }
        return (T) theTerm;
    }
}
