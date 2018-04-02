/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.engine.model;

import org.logic2j.core.api.library.LibraryContent;
import org.logic2j.core.api.library.PrimitiveInfo;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.SolverException;
import org.logic2j.engine.visitor.TermVisitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Struct class represents either Prolog compound {@link Term}s or atoms (an atom is represented by a 0-arity compound).
 * This class is now final, one we'll have to carefully think if this could be user-extended.
 * Note: This class MUST be immutable.
 */
public class Struct extends Term implements Cloneable {
  private static final long serialVersionUID = 1L;

  // ---------------------------------------------------------------------------
  // Names of functors
  // ---------------------------------------------------------------------------

  // TODO Move these constants to a common place?
  /**
   * This is the logical "AND" operator, usable with /2 or /* arity.
   */
  public static final String FUNCTOR_COMMA = ","; // No need to "intern()" a compile-time constant

  /**
   * This is the logical "OR" operator, usable with /2 or /* arity.
   */
  public static final String FUNCTOR_SEMICOLON = ";"; // No need to "intern()" a compile-time constant

  public static final String FUNCTOR_TRUE = "true";
  // Would like .intern() but it's anyway the case, and using this constant from an annotation won't work

  public static final String FUNCTOR_FALSE = "false"; // TODO do we need "false" or is this "fail"? // No need to "intern()" a compile-time constant

  public static final String FUNCTOR_CUT = "!";
  // Would like .intern() but it's anyway the case, and using this constant from an annotation won't work

  public static final String FUNCTOR_CALL = "call"; // No need to "intern()" a compile-time constant

  public static final String FUNCTOR_CLAUSE = ":-"; // No need to "intern()" a compile-time constant


  // ---------------------------------------------------------------------------
  // Some key atoms as singletons
  // ---------------------------------------------------------------------------
  public static final Struct ATOM_TRUE = new Struct(FUNCTOR_TRUE);

  public static final Struct ATOM_FALSE = new Struct(FUNCTOR_FALSE);

  public static final Struct ATOM_CUT = new Struct(FUNCTOR_CUT);

  public static final String LIST_SEPARATOR = ","; // In notations pred(a, b, c)

  public static final char PAR_CLOSE = ')';

  public static final char PAR_OPEN = '(';

  /**
   * Indicate the arity of a variable arguments predicate, such as write/N.
   * This is an extension to classic Prolog where only fixed arity is supported.
   */
  public static final String VARARG_ARITY_SIGNATURE = "N";

  /**
   * Terminates a vararg predicate description: write/N
   */
  private static final String VARARG_PREDICATE_TRAILER = "/" + VARARG_ARITY_SIGNATURE;

  // Separator of functor arguments: f(a,b), NOT the ',' functor for logical AND.
  public static final String ARG_SEPARATOR = ", ";

  public static final char QUOTE = '\'';

  private static final Object[] EMPTY_ARGS_ARRAY = new Object[0];

  // TODO Findbugs found that PrimitiveInfo should be serializable too :-(
  private PrimitiveInfo primitiveInfo;


  /**
   * The functor of the Struct is its "name". This is a final value but due to implementation via
   * method setNameAndArity(), we cannot declare it final here the compiler is not that smart.
   */
  private String name; // Always "internalized" with String.intern(), you can compare with ==.

  private int arity;

  private Object[] args;

  /**
   * The signature is internalized and allows for fast matching during unification
   */
  private String signature;

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

  public Struct(String theFunctor, Object... argList) {
    this(theFunctor, argList.length);
    int i = 0;
    for (final Object element : argList) {
      if (element == null) {
        throw new InvalidTermException("Cannot create Struct \"" + theFunctor + Arrays.asList(argList) + "\", found null argument at index " + i);
      }
      this.args[i++] = element;
    }
  }

  /**
   * Copy constructor.
   * Creates a shallow copy but with all children which are Struct also cloned.
   */
  public Struct(Struct original) {
    this.name = original.getName();
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
   * Clone with new arguments.
   *
   * @param newArguments New arguments, length must be same arity as original Struct
   * @return A clone of this.
   */
  public Struct cloneWithNewArguments(Object[] newArguments) {
    // We can actually change arity, this is used when we clone ","(X,Y) to ","(X,Y,Z)
    try {
      final Struct clone = (Struct) this.clone();
      clone.args = newArguments;
      clone.setNameAndArity(clone.name, clone.args.length); // Also calculate the signature
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new SolverException("Could not clone Struct " + this + ": " + e);
    }
  }

  /**
   * Obtain an atom from the catalog if it pre-existed, or create one an register in the catalog.
   *
   * @param theFunctor
   * @return Either a new one created or an existing one. It's actually either a String (if it can be),
   * but can be also a Struct of zero-arity for special functors such as "true", "false"
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
   * Factory to builds a compound, with non-{@link Term} arguments that will be converted
   * by {@link TermApi#valueOf(Object)}.
   *
   * @note This method is a static factory, not a constructor, to emphasize that arguments
   * are not of the type needed by this class, but need transformation.
   */
  public static Struct valueOf(String theFunctor, Object... argList) {
    final Struct newInstance = new Struct(theFunctor, argList.length);
    int i = 0;
    for (final Object element : argList) {
      newInstance.args[i++] = TermApi.valueOf(element);
    }
    return newInstance;
  }

  // ---------------------------------------------------------------------------
  // Template methods defined in abstract class Term
  // ---------------------------------------------------------------------------

  /**
   * Set {@link Term#index} to {@link Term#NO_INDEX}, recursively collect all argument's terms first,
   * then finally add this {@link Struct} to theCollectedTerms.
   * The functor alone (without its children) is NOT collected as a term. An atom is collected as itself.
   *
   * @param theCollectedTerms
   */
  void collectTermsInto(Collection<Object> theCollectedTerms) {
    clearIndex();
    if (this.arity > 0) {
      Arrays.stream(this.args).forEach(child -> TermApi.collectTermsInto(child, theCollectedTerms));
    }
    theCollectedTerms.add(this);
  }

  Object factorize(Collection<Object> theCollectedTerms) {
    // Recursively factorize all arguments of this Struct
    final Object[] newArgs = new Object[this.arity];
    boolean anyChange = false;
    for (int i = 0; i < this.arity; i++) {
      newArgs[i] = TermApi.factorize(this.args[i], theCollectedTerms);
      anyChange |= (newArgs[i] != this.args[i]);
    }
    // Now initialize result - a new Struct only if any change was found below
    final Struct factorized;
    if (anyChange) {
      factorized = new Struct(this);
      factorized.args = newArgs;
    } else {
      factorized = this;
    }
    // If this Struct already has an equivalent in the provided collection, return that one
    final Object betterEquivalent = factorized.findStructurallyEqualWithin(theCollectedTerms);
    if (betterEquivalent != null) {
      return betterEquivalent;
    }
    return factorized;
  }

  Var findVar(String theVariableName) {
    for (int i = 0; i < this.arity; i++) {
      final Object term = this.args[i];
      final Var found = TermApi.findVar(term, theVariableName);
      if (found != null) {
        return found;
      }
    }
    return null;
  }


  /**
   * @param theOther
   * @return true when references are the same, or when theOther Struct has same predicate name, arity, and all arguments are also equal.
   */
  boolean structurallyEquals(Object theOther) {
    if (theOther == this) {
      return true; // Same reference
    }
    if (!(theOther instanceof Struct)) {
      return false;
    }
    final Struct that = (Struct) theOther;
    // Arity and names must match.
    if (this.arity == that.arity && this.name == that.name) { // Names are {@link String#intern()}alized so OK to check by reference
      for (int i = 0; i < this.arity; i++) {
        if (!TermApi.structurallyEquals(this.args[i], that.args[i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Write major properties of the Struct, and also calculate read-only indexing signature for efficient access.
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
    this.signature = (this.getName() + '/' + this.arity).intern();
  }

  /**
   * Will set the "primitiveInfo" field to directly relate a token to an existing primitive
   * defined in theContent
   *
   * @param theContent Primitives in a Library
   */
  public void assignPrimitiveInfo(LibraryContent theContent) {
    // Find by exact arity match
    this.primitiveInfo = theContent.getPrimitive(getPredicateSignature());
    if (this.primitiveInfo == null) {
      // Alternate find by wildcard (varargs signature)
      this.primitiveInfo = theContent.getPrimitive(getVarargsPredicateSignature());
    }
    for (int i = 0; i < this.arity; i++) {
      Object child = this.args[i];
      if (child instanceof String) {
        if (theContent.hasPrimitive(TermApi.predicateSignature(child))) {
          // Convert to Struct so that we can assign a primitive
          child = new Struct((String) child);
          child = TermApi.normalize(child, theContent);
          this.args[i] = child; // Not 100% sure it's good to mutate
        }
      }
      if (child instanceof Struct) {
        ((Struct) child).assignPrimitiveInfo(theContent);
      }
    }
  }


  // --------------------------------------------------------------------------
  // Accessors
  // --------------------------------------------------------------------------

  /**
   * @return A cloned array of all arguments (cloned to avoid any possibility to mutate)
   */
  public Object[] getArgs() {
    if (this.args == null) {
      return EMPTY_ARGS_ARRAY;
    }
    return this.args;
  }

  /**
   * Gets the i-th element of this structure
   * <p/>
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

  public String getVarargsPredicateSignature() {
    return this.getName() + VARARG_PREDICATE_TRAILER;
  }

  // ---------------------------------------------------------------------------
  // Helpers for binary predicates: defined LHS (left-hand side) and RHS (right-hand side)
  // ---------------------------------------------------------------------------

  /**
   * @return Left-hand-side term, this is, {@link #getArg(int)} at index 0.
   * It is assumed that the term MUST have
   * an arity of exactly 2, because when there's a LHS, there's also a RHS!
   */
  public Object getLHS() {
    if (this.arity != 2) {
      throw new InvalidTermException(
          "Can't get the left-hand-side argument of \"" + this + "\" (predicate arity is: " + getPredicateSignature() + ")");
    }
    return this.args[0];
  }

  /**
   * @return Right-hand-side term, this is, {@link #getArg(int)} at index 1.
   * It is assumed that the term MUST have an arity of 2.
   */
  public Object getRHS() {
    if (this.arity != 2) {
      throw new InvalidTermException(
          "Can't get the right-hand-side argument of \"" + this + "\" (predicate arity is: " + getPredicateSignature() + ")");
    }
    return this.args[1];
  }

  // ---------------------------------------------------------------------------
  // TermVisitor
  // ---------------------------------------------------------------------------

  @Override
  public <T> T accept(TermVisitor<T> theVisitor) {
    return theVisitor.visit(this);
  }


  // ---------------------------------------------------------------------------
  // Management of index, cycles, and traversal
  // ---------------------------------------------------------------------------

  /**
   * For {@link Struct}s, the {@link Term#index} will be the maximal index of any variables that can be found, recursively, under all
   * children arguments.
   *
   * @note Assigning indexes, for example with a base index of 0, will proceed sequentially by depth-first
   * traversal. The first Vars encountered in sequence will receive indexes 0, 1, 2. Therefore a term such as
   * goal(A, Z, Y) will guarantee that indexes are: A=0, Z=1, Y=2.
   */
  int assignIndexes(int theIndexOfNextNonIndexedVar) {
    if (hasIndex()) {
      // Already assigned, do nothing and return the argument since we did
      // not assigned anything new
      return theIndexOfNextNonIndexedVar;
    }
    // Recursive assignment
    int runningIndex = theIndexOfNextNonIndexedVar;
    for (int i = 0; i < this.arity; i++) {
      runningIndex = TermApi.assignIndexes(this.args[i], runningIndex);
    }
    setIndex(runningIndex);
    return runningIndex;
  }

  public void avoidCycle(List<Term> visited) {
    for (final Term term : visited) {
      if (term == this) {
        throw new InvalidTermException("Cycle detected");
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
  // Accessors
  // ---------------------------------------------------------------------------

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

  public PrimitiveInfo getPrimitiveInfo() {
    return this.primitiveInfo;
  }


  // ---------------------------------------------------------------------------
  // Methods of java.lang.Object
  // ---------------------------------------------------------------------------


  @Override
  public int hashCode() {
    int result = this.getName().hashCode();
    result ^= this.arity << 8;
    for (int i = 0; i < this.arity; i++) {
      result ^= this.args[i].hashCode();
    }
    return result;
  }

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

  // ---------------------------------------------------------------------------
  // Basic formatting
  // ---------------------------------------------------------------------------

  public String toString() {
    return formatStruct();
  }

  private String formatStruct() {
    if (PrologLists.isEmptyList(this)) {
      return PrologLists.FUNCTOR_EMPTY_LIST;
    }
    final StringBuilder sb = new StringBuilder();
    final int nArity = getArity();
    // list case
    if (PrologLists.isListNode(this)) {
      sb.append(PrologLists.LIST_OPEN);
      sb.append(PrologLists.formatPListRecursive(this));
      sb.append(PrologLists.LIST_CLOSE);
      return sb.toString();
    }
    sb.append(TermApi.quoteIfNeeded(getName()));
    if (nArity > 0) {
      sb.append(PAR_OPEN);
      for (int c = 0; c < nArity; c++) {
        final Object arg = getArg(c);
        final String formatted = arg.toString();
        sb.append(formatted);
        if (c < nArity - 1) {
          sb.append(ARG_SEPARATOR);
        }
      }
      sb.append(PAR_CLOSE);
    }
    return sb.toString();
  }

}
