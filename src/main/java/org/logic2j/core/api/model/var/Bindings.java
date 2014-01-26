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
package org.logic2j.core.api.model.var;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.logic2j.core.api.model.TermVisitor;
import org.logic2j.core.api.model.TermVisitorBase;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.PrologInternalError;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.impl.util.ReflectUtils;

/**
 * Store a reference to a {@link Term} together with the actual values of its variables as a list of {@link Binding}s, one per {@link Var}
 * iable found within the Term.<br/>
 * Provide methods to extract values from {@link Var}iables.
 * 
 * @note Usually the {@link Term} is a {@link Struct} that represents a goal to be demonstrated or unified.
 * @note The Term referring to this object is called the "referrer".<br/>
 * 
 *       TODO Improve performance: pre instantiation of {@link #Bindings(Term)} instead of many times during solving. See note on
 *       constructor.
 */
public class Bindings {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Bindings.class);
    private static boolean isDebug = logger.isDebugEnabled();

    private static final Binding[] EMPTY_BINDINGS_ARRAY = new Binding[0];

    /**
     * The Term, usually a {@link Struct}, whose {@link Var}iables refer to this {@link Bindings} through their indexes.
     */
    private final Object referrer;

    /**
     * All {@link Binding}s, one per instance of {@link Var}iable. There are as many bindings as the distinct number of variables in the
     * referrer Term, i.e. the length of bindings equals the maximum of all indexes in all {@link Var}s of the referrer, plus one. See also
     * {@link Var#getIndex()}. This array is never null, but may be empty (length=0) when the referrer Term does not contain any {@link Var}
     * iable.
     */
    private final Binding[] bindings;

    /**
     * Create a new Binding by just assign the 2 fields, we use this when deep-copying
     * with a new array of {@link Binding} of when shallow-copying with the same reference to the array.
     * 
     * @param theNewReferrerTerm
     * @param theArrayOfBinding
     */
    private Bindings(Object theNewReferrerTerm, Binding[] theArrayOfBinding) {
        this.referrer = theNewReferrerTerm;
        this.bindings = theArrayOfBinding;
    }

    /**
     * Allocate a {@link Bindings} to hold the values of all {@link Var}iables of a given term,
     * named further the "referrer", which is ususally a {@link Struct}.
     * 
     * @note This constructor needs quite a lot of processing - there should be ways to optimize it
     * @param theReferrer The Term whose {@link Var}iables's values are to be found in this object.
     * @see Bindings#getReferrer() to further access theTerm
     */
    public Bindings(Object theReferrer) {
        this.referrer = theReferrer;
        if (!(theReferrer instanceof Term)) {
            // Will be no variables - used constant empty array
            this.bindings = EMPTY_BINDINGS_ARRAY;
        } else {
            // Can now only be Var or Struct
            final short index;
            final int nbVars; // Determine number of distinct variables
            final boolean isVar = theReferrer instanceof Var;
            if (isVar) {
                final Var var = (Var) theReferrer;
                index = var.getIndex();
                if (var.isAnonymous()) {
                    nbVars = 0;
                } else {
                    nbVars = 1;
                }
            } else {
                // Must be Struct
                index = ((Struct) theReferrer).getIndex();
                // Will be a Struct; in that case the index is the number of distinct variables
                nbVars = index;
            }
            if (index == Term.NO_INDEX) {
                throw new InvalidTermException("Index of Term '" + theReferrer
                                + "' is not yet initialized, cannot create Bindings because Term is not ready for inference. Normalize it first.");
            }

            //
            // Allocate and initialize a Binding for every variable
            //
            this.bindings = new Binding[nbVars];
            for (int varIndex = 0; varIndex < nbVars; varIndex++) {
                final Binding binding = Binding.newFree();
                this.bindings[varIndex] = binding;
                // Assign Binding.var field
                // TODO (issue) This is costly see https://github.com/ltettoni/logic2j/issues/26
                TermApi.accept(new VisitorToAssignVarWithinBinding(binding, varIndex), theReferrer, this);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Copy methods
    // ---------------------------------------------------------------------------

    /**
     * @param theOriginal
     * @return A new {@link Bindings} with the same referrer, but a deep copy of the {@link Binding} array
     * contained therein. A new array is allocated, and every {@link Binding} is cloned also.
     */
    public static Bindings deepCopyWithSameReferrer(Bindings theOriginal) {
        return deepCopyWithNewReferrer(theOriginal.getReferrer(), theOriginal);
    }

    private static Bindings deepCopyWithNewReferrer(Object theNewReferrer, Bindings theOriginal) {
        // Deep cloning of the individual Binding
        final Binding[] originalBindings = theOriginal.bindings;
        final int nbVars = originalBindings.length;
        final Binding[] copiedArrayOfBinding = new Binding[nbVars];
        // All bindings need cloning
        for (int i = 0; i < nbVars; i++) {
            copiedArrayOfBinding[i] = new Binding(originalBindings[i]);
        }
        return new Bindings(theNewReferrer, copiedArrayOfBinding);
    }

    public Binding toLiteralBinding() {
        return Binding.newLiteral(this.referrer, this);
    }
    
    // ---------------------------------------------------------------------------
    // Methods for extracting values from variable Bindings
    // ---------------------------------------------------------------------------

    /**
     * Create a new {@link Bindings} with the specified {@link Term} as new Referrer.
     * When Term is a bound {@link Var}iable, will follow through until a free Var or literal is found.
     * 
     * @param theTerm Must be either the referrer or on of the children {@link Term}s that was
     *            used to instantiate this {@link Bindings}
     * @param theClass of the expected referrer Term, or Object.class for any
     * @return null if theTerm was the anonymous {@link Var}iable
     */
    public Bindings narrow(Object theTerm, Class<? extends Object> theClass) {
        if (theTerm instanceof Var) {
            final Var origin = (Var) theTerm;
            if (origin.isAnonymous()) {
                return null; // See method contract - but I don't like returning nulls!
            }
            // Go to fetch the effective variable value if any
            final Binding startingBinding = origin.bindingWithin(this);
            final Binding finalBinding = startingBinding.followLinks();
            switch (finalBinding.getType()) {
                case LITERAL:
                    return new Bindings(finalBinding.getTerm(), finalBinding.getBindings().bindings);
                case FREE:
                    // Refocus on original var (we now know it is free), keep the same original bindings
                    return new Bindings(origin, this.bindings); // I wonder if we should not focus on the final var instead?
                default:
                    throw new PrologInternalError("Should never have been here");
            }
        }
        // It's anything else than a Var - no need to follow links

        // Make sure it's of the desired class
        ReflectUtils.safeCastNotNull("obtaining resolved term", theTerm, theClass);

        // If we already have the same referrer, don't shallow-copy
        if (this.getReferrer() == theTerm) {
            return this;
        }

        // will return a shallow-copy Bindings with theTerm as referrer
        return new Bindings(theTerm, this.bindings);
    }

    /**
     * Considering this object's current bindings as a snapshot to a solution, extract the content of the variables and their bound values
     * in a safe place (a Map) so that inference can resume towards other solutions.
     * 
     * @param theRepresentation How to represent free (non-ground) variables
     * @return All variable bindings resolved, represented as specified for the case of free bindings.
     */
    public Map<String, Object> explicitBindings(FreeVarRepresentation theRepresentation) {
        final IdentityHashMap<Binding, Var> bindingToInitialVar = finalBindingsToInitialVar();

        final Map<String, Object> result = new TreeMap<String, Object>();
        for (final Binding initialBinding : this.bindings) {
            final Var originalVar = initialBinding.getReferrer();
            if (originalVar == null) {
                throw new PrologNonSpecificError("Bindings not properly initialized: Binding " + initialBinding + " does not refer to Var (null)");
            }
            // The original name of our variable
            final String originalVarName = originalVar.getName();
            // Now reach the effective lastest binding
            final Binding finalBinding = initialBinding.followLinks(); // FIXME we did this above already - can't we remember it???
            final Var finalVar = finalBinding.getReferrer();
            switch (finalBinding.getType()) {
                case LITERAL:
                    if (originalVarName == null) {
                        throw new PrologNonSpecificError("Cannot assign null (undefined) var, not all variables of " + this + " are referenced from Term " + this.referrer
                                        + ", binding " + initialBinding
                                        + " can't be assigned a variable name");
                    }
                    final Object boundTerm = finalBinding.getTerm();
                    final Object substitute = TermApi.substituteOld(boundTerm, finalBinding.getBindings(), bindingToInitialVar);
                    // Literals are not unbound terms, they are returned the same way for all types of representations asked
                    result.put(originalVarName, substitute);
                    break;
                case FREE:
                    // Here we will use different representations for free vars
                    switch (theRepresentation) {
                        case SKIPPED:
                            // Nothing added to the resulting bindings: no Map entry (no key, no value)
                            break;
                        case NULL:
                            // Add one entry with null value
                            result.put(originalVarName, null);
                            break;
                        case FREE:
                            result.put(originalVarName, finalVar);
                            break;
                        case FREE_NOT_SELF:
                            // Names are {@link String#intern()}alized so OK to check by reference
                            if (originalVar.getName() != finalVar.getName()) {
                                // Avoid reporting "X=null" for free variables or "X=X" as a binding...
                                result.put(originalVarName, finalVar);
                            }
                            break;
                    }
                    break;
                case LINK:
                    throw new PrologNonSpecificError("Should not happen we have followed links already");
            }
        }
        return result;
    }

    /**
     * @return An {@link IdentityHashMap} from the final bindings (literals or free vars) to the initial {@link Var}s.
     * Notice that because several vars may lead to the same final bindings, this reverse mapping may only be partial. Which
     * var is referenced is not deterministic.
     */
    public IdentityHashMap<Binding, Var> finalBindingsToInitialVar() {
        // For every Binding in this object, identify to which Var it initially refered (following linked bindings)
        // ending up with either null (on a literal), or a real Var (on a free var).
        final IdentityHashMap<Binding, Var> bindingToInitialVar = new IdentityHashMap<Binding, Var>();
        for (final Binding initialBinding : this.bindings) {
            final Var initialVar = initialBinding.getReferrer();
            // Follow linked bindings
            final Binding finalBinding = initialBinding.followLinks();
            // At this stage finalBinding may be either literal, or free
            bindingToInitialVar.put(finalBinding, initialVar);
        }
        return bindingToInitialVar;
    }

    public IdentityHashMap<Var, Binding> initialVartoFinalBinding() {
        // For every Binding in this object, identify to which Var it initially refered (following linked bindings)
        // ending up with either null (on a literal), or a real Var (on a free var).
        final IdentityHashMap<Var, Binding> result = new IdentityHashMap<Var, Binding>();
        for (final Binding initialBinding : this.bindings) {
            final Var initialVar = initialBinding.getReferrer();
            // Follow linked bindings
            final Binding finalBinding = initialBinding.followLinks();
            // At this stage finalBinding may be either literal, or free
            result.put(initialVar, finalBinding);
        }
        return result;
    }

    /**
     * Detection of goals in the form of X(...) where X is free.
     * 
     * @return true if this {@link Bindings}'s {@link #getReferrer()} is a free variable.
     */
    public boolean isFreeReferrer() {
        if (!(this.referrer instanceof Var)) {
            return false;
        }
        // Only in case of Var we proceed testing
        return ((Var) this.referrer).bindingWithin(this).followLinks().isFree();
    }

    /**
     * Find the local bindings corresponding to one of the variables of the Struct referred to by this Bindings. FIXME Uh - what does this
     * mean??? 
     * TODO This method is only used once from a library - ensure it makes sense and belongs here
     * 
     * @param theVar
     * @return null when not found
     */
    public Bindings findBindings(Var theVar) {
        // Search root level
        int index = 0;
        for (final Binding binding : this.bindings) {
            // FIXME dubious use of == instead of structural equality
            if (binding.getReferrer() == theVar && index == theVar.getIndex()) {
                return this;
            }
            index++;
        }
        // Not found: search deeper through bindings
        for (final Binding binding : this.bindings) {
            if (binding.getType() == BindingType.LITERAL) {
                final Bindings foundDeeper = binding.getBindings().findBindings(theVar);
                if (foundDeeper != null) {
                    return foundDeeper;
                }
            }
        }
        // Not found
        return null;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public boolean isEmpty() {
        return this.bindings.length == 0;
    }

    /**
     * @return The number of {@link Binding}s held in this object, corresponds to the number of distinct variables in {@link #getReferrer()}
     */
    public int getSize() {
        return this.bindings.length;
    }

    /**
     * @param theIndex
     * @return The {@link Binding} at theIndex.
     */
    public Binding getBinding(int theIndex) {
        return this.bindings[theIndex];
    }

    /**
     * @return The Term whose variable values are held in this structure, actually the one that was provided to the consturctor
     *         {@link #Bindings(Term)}.
     */
    public Object getReferrer() {
        return this.referrer;
    }

    /**
     * @param allBindings
     * @param theRemappedVars
     * @return A new Bindings with a number of {@link Bindings} into a single new one, making sure all variables are
     * distinct.
     */
    public static Bindings merge(List<Bindings> allBindings, IdentityHashMap<Object, Object> theRemappedVars) {
        // Keep only distinct ones (as per object equality, in our case same references), but preseving order
        final LinkedHashSet<Bindings> distinctBindings = new LinkedHashSet<>(allBindings);
        // Count total number of vars
        int numberOfVars = 0;
        for (final Bindings element : distinctBindings) {
            numberOfVars += element.bindings.length;
        }
        // Allocate
        final Binding[] array = new Binding[numberOfVars];
        int index = 0;
        for (final Bindings element : distinctBindings) {
            for (final Binding binding : element.bindings) {
                final Binding clonedBinding = new Binding(binding);
                array[index] = clonedBinding;
                final Var originalVar = clonedBinding.getReferrer();
                final Var clonedVar = new Var(originalVar.getName());
                theRemappedVars.put(originalVar, clonedVar);
                clonedBinding.setReferrer(clonedVar);
                index++;
            }
        }
        return new Bindings(null, array);
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        // Name is "BindingN", with N the number of Binding
        sb.append(this.getClass().getSimpleName());
        sb.append(this.bindings.length);
        if (isDebug) {
            // Java reference (hex address) when on isDebug
            sb.append('@');
            sb.append(Integer.toHexString(super.hashCode()));
        }
        if (!isEmpty()) {
            // Just format with the Binding's toString() method, as a list (enclosed in brackets [])
            sb.append(Arrays.asList(this.bindings));
        }
        // Referrer
        sb.append(':');
        sb.append(getReferrer());
        return sb.toString();
    }

    // ---------------------------------------------------------------------------
    // Support classes
    // ---------------------------------------------------------------------------

    /**
     * Determine how free (unbound) variables will be represented in resulting bindings returned by
     * {@link Bindings#explicitBindings(FreeVarRepresentation)}.
     */
    public enum FreeVarRepresentation {
        /**
         * Free variables will not be included in result bindings. Asking the value of a variable that is not bound to a literal Term is
         * likely to throw a {@link RuntimeException}.
         */
        SKIPPED,

        /**
         * Free variables will be represented by the existence of a Map {@link Entry} with a valid key, and a value equal to null. You are
         * required to use {@link Map#containsKey(Object)} to identify this case, since {@link Map#get(Object)} returning null won't allow
         * you to distinguish between a free variable (asking for "X" when X is still unbound) or asking the value of an undefined variable
         * (asking "TOTO" when not in original goal).
         */
        NULL,

        /**
         * Free variables will be reported with their terminal value. If we have the chain of bindings being X -> Y -> Z, this mode will
         * represent X with the mapping "X"->"Z". A particular case is when X is unified to itself (via a loop), then the mapping becomes
         * "X" -> "X" which may not be desireable... See FREE_NOT_SELF.
         */
        FREE,

        /**
         * TBD.
         */
        FREE_NOT_SELF

    }

    /**
     * A {@link TermVisitor} used to assign a reference to the original {@link Var}iable into a {@link Binding}.
     */
    private static class VisitorToAssignVarWithinBinding extends TermVisitorBase<Var> {

        private final Binding binding;
        private final int index;

        /**
         * @param theBinding The {@link Binding} whose {@link Binding#setReferrer(Var)} needs to be initialized.
         * @param theIndex Initialize only {@link Var} whose {@link Term#getIndex()} match this index value.
         */
        VisitorToAssignVarWithinBinding(Binding theBinding, int theIndex) {
            this.binding = theBinding;
            this.index = theIndex;
        }

        @Override
        public Var visit(Var theVar, Bindings theBindings) {
            if (theVar.getIndex() == this.index) {
                this.binding.setReferrer(theVar);
                // Job done - there are no other variable to set for this index, since goals are always factorized (see def of factorized
                // Term)
                // Returning non-null will stop useless recursion
                return theVar;
            }
            // Nothing done - continue visiting more
            return null;
        }

    }

}
