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

import org.logic2j.core.api.model.PrimitiveInfo;
import org.logic2j.core.api.model.TermVisitor;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.impl.util.ReflectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Struct class represents either Prolog compound {@link Term}s or atoms (an atom is represented by a 0-arity compound).
 * This class is now final, one we'll have to carefully think if this could be user-extended.
 * Note: This class MUST be immutable.
 */
public final class Struct extends Term {
    private static final long serialVersionUID = 1L;

    /**
     * Indicate the arity of a variable arguments predicate, such as write/N. (this is an extension to classic Prolog where only fixed arity
     * is supported).
     */
    public static final String VARARG_ARITY_SIGNATURE = "N";

    /**
     * Terminates a vararg predicate description: write/N
     */
    private static final String VARARG_PREDICATE_TRAILER = "/" + VARARG_ARITY_SIGNATURE;

    // TODO Move these constants to a common place?
    // TODO Replace all calls to intern() by some factory to initialize our constants. Useless to do it here in Java all constant strings are already internalized?
    public static final String FUNCTOR_COMMA = ",".intern();
    public static final String FUNCTOR_SEMICOLON = ";".intern();
    public static final String LIST_SEPARATOR = ",".intern(); // In notations [a,b,c]

    public static final String FUNCTOR_LIST = ".".intern();
    public static final String FUNCTOR_LIST_EMPTY = "[]".intern(); // The

    public static final String FUNCTOR_CLAUSE = ":-".intern();
    public static final String FUNCTOR_CLAUSE_QUOTED = ("'" + FUNCTOR_CLAUSE + "'").intern();

    public static final String FUNCTOR_TRUE = "true";
    public static final String FUNCTOR_FALSE = "false".intern();
    public static final String FUNCTOR_CUT = "!";
    public static final String FUNCTOR_CALL = "call";

    // true and false constants
    public static final Struct ATOM_FALSE = new Struct(FUNCTOR_FALSE);
    public static final Struct ATOM_TRUE = new Struct(FUNCTOR_TRUE);
    public static final Struct ATOM_CUT = new Struct(FUNCTOR_CUT);

    /**
     * The empty list.
     */
    public static final Struct EMPTY_LIST = new Struct(FUNCTOR_LIST_EMPTY, 0);

    private String name; // Always "internalized" with String.intern(), you can compare with == !
    private int arity;
    private Object[] args;
    private String signature;

    // TODO Findbugs found that PrimitiveInfo should be serializable too :-(
    public PrimitiveInfo primitiveInfo;

    /**
     * Low-level constructor.
     *
     * @param theFunctor
     * @param theArity
     */
    private Struct(String theFunctor, int theArity) {
        setNameAndArity(theFunctor, theArity);
        // When arity is zero, don't even bother to allocate arguments!
        if (this.arity > 0) {
            this.args = new Object[this.arity];
        }
    }

    public Struct(String theFunctor, Object... argList) throws InvalidTermException {
        this(theFunctor, argList.length);
        int i = 0;
        for (final Object element : argList) {
            if (element == null) {
                throw new InvalidTermException("Cannot create Term from with null argument");
            }
            this.args[i++] = element;
        }
    }

    /**
     * Copy constructor.
     * Creates a shallow copy but with all children which are Struct also cloned.
     */
    public Struct(Struct original) {
        this.name = original.name;
        this.arity = original.arity;
        this.signature = original.signature;
        this.primitiveInfo = original.primitiveInfo;
        // What about "this.index" ?
        if (this.arity > 0) {
            this.args = new Object[this.arity];
            for (int i = 0; i < this.arity; i++) {
                Object cloned = original.args[i];
                if (cloned instanceof Struct) {
                    cloned = new Struct((Struct) cloned);
                }
                this.args[i] = cloned;
            }
        }
    }

    /**
     * Efficient cloning of the structure header - but passing a specified set of already-cloned args
     * @param original
     * @param newArguments
     */
    public Struct(Struct original, Object[] newArguments) {
        if (newArguments.length != original.arity) {
            throw new IllegalArgumentException("Different number of arguments than arity of original Struct");
        }
        this.name = original.name;
        this.arity = original.arity;
        this.signature = original.signature;
        this.primitiveInfo = original.primitiveInfo;
        this.index = original.index;
        this.args = newArguments;
    }

//    /**
//     * Factory to builds a compound, with non-{@link Term} arguments that will be converted
//     * by {@link TermApi#valueOf(Object, FactoryMode)}.
//     *
//     * @note This method is a static factory, not a constructor, to emphasize that arguments
//     *       are not of the type needed by this class, but need transformation.
//     */
//    public static Struct valueOf(String theFunctor, Object... argList) throws InvalidTermException {
//        final Struct newInstance = new Struct(theFunctor, argList.length);
//        int i = 0;
//        for (final Object element : argList) {
//            newInstance.args[i++] = TermApi.valueOf(element, FactoryMode.ANY_TERM);
//        }
//        return newInstance;
//    }

    /**
     * Obtain an atom from the catalog if it pre-existed, or create one an register in the catalog.
     *
     * @param theFunctor
     * @return Either a new one created or an existing one
     */
    public static Object atom(String theFunctor) {
        // Search in the catalog of atoms for exact match
        final String functor = theFunctor.intern();
        final boolean specialAtomRequiresStruct = functor == Struct.FUNCTOR_CUT || functor == Struct.FUNCTOR_TRUE || functor == Struct.FUNCTOR_FALSE;
        if (!specialAtomRequiresStruct) {
            // We can return an internalized String
            return functor;
        }
        final Struct instance = new Struct(functor, 0);
        return instance;
    }

    /**
     * Static factory (instead of constructor).
     *
     * @param head
     * @param tail
     * @return A prolog list provided head and tail
     */
    public static Struct createPList(Object head, Object tail) {
        final Struct result = new Struct(FUNCTOR_LIST, 2);
        result.args[0] = head;
        result.args[1] = tail;
        return result;
    }

    /**
     * @param theJavaCollection
     * @return A Prolog List structure from a Java {@link java.util.Collection}.
     */
    public static Struct createPList(Collection<?> theJavaCollection) {
        final int size = theJavaCollection.size();
        // Unroll elements into an array (we need this since we don't have an index-addressable structure)
        final Object[] array = new Object[size];
        int index = 0;
        for (final Object element : theJavaCollection) {
            array[index++] = element;
        }
        return createPList(array);
    }

    /**
     * @param array
     * @return A Prolog List structure from a Java array.
     */
    public static Struct createPList(final Object[] array) {
        // Assemble the prolog list (head|tail) nodes from the last to the first element
        Struct tail = Struct.EMPTY_LIST;
        for (int i = array.length - 1; i >= 0; i--) {
            final Object head = array[i];
            tail = Struct.createPList(head, tail);
        }
        return tail;
    }

    /**
     * Write major properties of the Struct, and also store read-only fields for efficient access.
     *
     * @param theFunctor whose named is internalized by {@link String#intern()}
     * @param theArity
     */
    private void setNameAndArity(String theFunctor, int theArity) {
        if (theFunctor == null) {
            throw new InvalidTermException("The functor of a Struct cannot be null");
        }
        if (theFunctor.isEmpty() && theArity > 0) {
            throw new InvalidTermException("The functor of a non-atom Struct cannot be an empty string");
        }
        this.name = theFunctor.intern();
        this.arity = theArity;
        this.signature = (this.name + '/' + this.arity).intern();
    }

    /**
     * Gets the number of elements of this structure
     */
    public int getArity() {
        return this.arity;
    }

    /**
     * Gets the functor name of this structure
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return A cloned array of all arguments (cloned to avoid any possibility to mutate)
     */
    public Object[] getArgs() {
        if (this.args == null) {
            return new Object[0];
        }
        final int length = this.args.length;
        final Object[] copy = new Object[length];
        System.arraycopy(this.args, 0, copy, 0, length);
        return copy;
    }

    /**
     * Gets the i-th element of this structure
     *
     * No bound check is done
     */
    public Object getArg(int theIndex) {
        return this.args[theIndex];
    }

    /**
     * A unique identifier that determines the family of the predicate represented by this {@link Struct}.
     *
     * @return The predicate's name + '/' + arity
     */
    public String getPredicateSignature() {
        return this.signature;
    }

    /**
     * @return Left-hand-side term, this is, {@link #getArg(int)} at index 0. It is assumed that the term MUST have an arity of 2, because
     *         when there's a LHS, there's also a RHS!
     */
    public Object getLHS() {
        if (this.arity != 2) {
            throw new PrologNonSpecificError("Can't get the left-hand-side argument of " + this + " (not a binary predicate)");
        }
        return this.args[0];
    }

    /**
     * @return Right-hand-side term, this is, {@link #getArg(int)} at index 1. It is assumed that the term MUST have an arity of 2
     */
    public Object getRHS() {
        if (this.arity != 2) {
            throw new PrologNonSpecificError("Can't get the left-hand-side argument of " + this + " (not a binary predicate)");
        }
        return this.args[1];
    }

    public String getVarargsPredicateSignature() {
        return this.name + VARARG_PREDICATE_TRAILER;
    }

    public PrimitiveInfo getPrimitiveInfo() {
        return this.primitiveInfo;
    }

//    /**
//     * @param theContent
//     */
//    public void assignPrimitiveInfo(LibraryContent theContent) {
//        // Find by exact arity match
//        this.primitiveInfo = theContent.getPrimitive(getPredicateSignature());
//        if (this.primitiveInfo == null) {
//            // Alternate find by wildcard (varargs signature)
//            this.primitiveInfo = theContent.getPrimitive(getVarargsPredicateSignature());
//        }
//        for (int i = 0; i < this.arity; i++) {
//            final Object child = this.args[i];
//            if (child instanceof Struct) {
//                ((Struct) child).assignPrimitiveInfo(theContent);
//            }
//        }
//    }

    public void avoidCycle(List<Term> visited) {
        for (final Term term : visited) {
            if (term == this) {
                throw new PrologNonSpecificError("Cycle detected");
            }
        }
        visited.add(this);
        for (final Object term : this.args) {
            if (term instanceof Struct) {
                ((Struct) term).avoidCycle(visited);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Template methods defined in abstract class Term
    // ---------------------------------------------------------------------------

//    /**
//     * Set {@link Term#index} to {@link Term#NO_INDEX}, recursively collect all argument's terms, and finally add this {@link Struct} to
//     * theCollectedTerms. The functor alone (without its children) is NOT collected as a term. An atom is collected as itself.
//     *
//     * @param theCollectedTerms
//     */
//    void collectTermsInto(Collection<Object> theCollectedTerms) {
//        this.index = NO_INDEX;
//        for (int i = 0; i < this.arity; i++) {
//            final Object child = this.args[i];
//            TermApi.collectTermsInto(child, theCollectedTerms);
//        }
//        theCollectedTerms.add(this);
//    }

//    Object factorize(Collection<Object> theCollectedTerms) {
//        // Recursively factorize all arguments of this Struct
//        final Object[] newArgs = new Object[this.arity];
//        boolean anyChange = false;
//        for (int i = 0; i < this.arity; i++) {
//            newArgs[i] = TermApi.factorize(this.args[i], theCollectedTerms);
//            anyChange |= (newArgs[i] != this.args[i]);
//        }
//        // Now initialize result - a new Struct only if any change was found below
//        final Struct factorized;
//        if (anyChange) {
//            factorized = new Struct(this);
//            factorized.args = newArgs;
//        } else {
//            factorized = this;
//        }
//        // If this Struct already has an equivalent in the provided collection, return that one
//        final Object betterEquivalent = factorized.findStructurallyEqualWithin(theCollectedTerms);
//        if (betterEquivalent != null) {
//            return betterEquivalent;
//        }
//        return factorized;
//    }

//    Var findVar(String theVariableName) {
//        for (int i = 0; i < this.arity; i++) {
//            final Object term = this.args[i];
//            final Var found = TermApi.findVar(term, theVariableName);
//            if (found != null) {
//                return found;
//            }
//        }
//        return null;
//    }



//    /**
//     * @param theOther
//     * @return true when references are the same, or when theOther Struct has same predicate name, arity, and all arguments are also equal.
//     */
//    boolean structurallyEquals(Object theOther) {
//        if (theOther == this) {
//            return true; // Same reference
//        }
//        if (!(theOther instanceof Struct)) {
//            return false;
//        }
//        final Struct that = (Struct) theOther;
//        // Arity and names must match.
//        if (this.arity == that.arity && this.name == that.name) { // Names are {@link String#intern()}alized so OK to check by reference
//            for (int i = 0; i < this.arity; i++) {
//                if (!TermApi.structurallyEquals(this.args[i], that.args[i])) {
//                    return false;
//                }
//            }
//            return true;
//        }
//        return false;
//    }

    /**
     * For {@link Struct}s, the {@link Term#index} will be the maximal index of any variables that can be found, recursively, under all
     * arguments.
     */
    int assignIndexes(int theIndexOfNextNonIndexedVar) {
        if (this.index != NO_INDEX) {
            // Already assigned, do nothing and return same index since we did
            // not assigned a new var
            return theIndexOfNextNonIndexedVar;
        }
        // Recursive assignment
        int runningCounter = theIndexOfNextNonIndexedVar;
        for (int i = 0; i < this.arity; i++) {
            runningCounter = TermApi.assignIndexes(this.args[i], runningCounter);
        }
        this.index = (short)runningCounter;
        return runningCounter;
    }

    // ---------------------------------------------------------------------------
    // Methods for Prolog list structures (named "PList" hereafter)
    // ---------------------------------------------------------------------------

    /**
     * Is this structure an empty list?
     */
    public boolean isEmptyList() {
        return this.name == FUNCTOR_LIST_EMPTY && this.arity == 0;
    }

    boolean isList() {
        return (this.name == FUNCTOR_LIST && this.arity == 2 && TermApi.isList(this.args[1])) || isEmptyList();
    }

    protected void assertPList(Term thePList) {
        if (!TermApi.isList(thePList)) {
            throw new PrologNonSpecificError("The structure \"" + thePList + "\" is not a Prolog list.");
        }
    }

    /**
     * Gets the head of this structure, which is supposed to be a list.
     * 
     * <p>
     * Gets the head of this structure, which is supposed to be a list. If the callee structure is not a list, throws an
     * <code>UnsupportedOperationException</code>
     * </p>
     */
    public Object listHead() {
        assertPList(this);
        return getLHS();
    }

    /**
     * Gets the tail of this structure, which is supposed to be a list.
     * 
     * <p>
     * Gets the tail of this structure, which is supposed to be a list. If the callee structure is not a list, throws an
     * <code>UnsupportedOperationException</code>
     * </p>
     */
    public Struct listTail() {
        assertPList(this);
        return (Struct) getRHS();
    }

    /**
     * Gets the number of elements of this structure, which is supposed to be a list.
     * 
     * <p>
     * Gets the number of elements of this structure, which is supposed to be a list. If the callee structure is not a list, throws an
     * <code>UnsupportedOperationException</code>
     * </p>
     */
    public int listSize() {
        assertPList(this);
        Struct t = this;
        int count = 0;
        while (!t.isEmptyList()) {
            count++;
            t = (Struct) t.getRHS();
        }
        return count;
    }

    /**
     * From a Prolog List, obtain a Struct with the first list element as functor, and all other elements as arguments. This returns
     * a(b,c,d) form [a,b,c,d]. This is the =.. predicate.
     * 
     * If this structure is not a list, null object is returned
     */
    // TODO (issue) Clarify how it works, see https://github.com/ltettoni/logic2j/issues/14
    public Struct predicateFromPList() {
        assertPList(this);
        final Object functor = getLHS();
        if (!TermApi.isAtom(functor)) {
            return null;
        }
        Struct runningElement = (Struct) getRHS();
        final ArrayList<Object> elements = new ArrayList<Object>();
        while (!runningElement.isEmptyList()) {
            if (!runningElement.isList()) {
                return null;
            }
            elements.add(runningElement.getLHS());
            runningElement = (Struct) runningElement.getRHS();
        }
        final String fnct;
        if (functor instanceof String) {
            fnct = (String) functor;
        } else {
            fnct = ((Struct) functor).name;
        }

        return new Struct(fnct, elements.toArray(new Object[elements.size()]));
    }

    @SuppressWarnings("unchecked")
    public <Q, T extends Collection<Q>> T javaListFromPList(T theCollectionToFillOrNull, Class<Q> theElementClassOrNull) {
        final T result;
        if (theCollectionToFillOrNull == null) {
            result = (T) new ArrayList<Q>();
        } else {
            result = theCollectionToFillOrNull;
        }
        // In case not a list, we just return a Java list with one element
        if (!this.isList()) {
            result.add(ReflectUtils.safeCastNotNull("casting single value", this, theElementClassOrNull));
            return result;
        }

        Struct runningElement = this;
        int idx = 0;
        while (!runningElement.isEmptyList()) {
            assertPList(runningElement);
            final Q term = ReflectUtils.safeCastNotNull("obtaining element " + idx + " of PList " + this, runningElement.getLHS(), theElementClassOrNull);
            result.add(term);
            runningElement = (Struct) runningElement.getRHS();
            idx++;
        }
        return result;
    }

    /**
     * Appends an element to this structure (supposed to be a list)
     */
    public void append(Term t) {
        assertPList(this);
        if (isEmptyList()) {
            setNameAndArity(FUNCTOR_LIST, 2);
            this.args = new Object[this.arity];
            this.args[0] = t;
            this.args[1] = Struct.EMPTY_LIST;
        } else if (TermApi.isList(this.args[1])) {
            ((Struct) this.args[1]).append(t);
        } else {
            this.args[1] = t;
        }
    }

    /**
     * Inserts (at the head) an element to this structure (supposed to be a list)
     */
    void insert(Term t) {
        assertPList(this);
        final Struct co = Struct.EMPTY_LIST;
        co.args[0] = getLHS();
        co.args[1] = getRHS();
        this.args[0] = t;
        this.args[1] = co;
    }

    @Override
    public <T> T accept(TermVisitor<T> theVisitor) {
        return theVisitor.visit(this);
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    /**
     * @param other
     * @return Structures are {@link #equals(Object)} if other is a Struct of same arity, name, and all params are equal too.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Struct)) {
            return false;
        }
        final Struct that = (Struct) other;
        if (!(this.arity == that.arity && this.name == that.name)) { // Names are {@link String#intern()}alized so OK to check by reference
            return false;
        }
        for (int i = 0; i < this.arity; i++) {
            if (!this.args[i].equals(that.args[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = this.name.hashCode();
        result ^= this.arity << 8;
        for (int i = 0; i < this.arity; i++) {
            result ^= this.args[i].hashCode();
        }
        return result;
    }



    //---------------------------------------------------------------------------
    // Oldies
    //---------------------------------------------------------------------------

    
}
