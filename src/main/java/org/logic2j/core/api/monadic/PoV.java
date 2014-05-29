package org.logic2j.core.api.monadic;

import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.FreeVarRepresentation;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.impl.util.ProfilingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Laurent on 07.05.2014.
 */
public class PoV {
    private static final Logger logger = LoggerFactory.getLogger(PoV.class);
//    static final Logger audit = LoggerFactory.getLogger("audit");

    final int currentTransaction;

    public int topVarIndex;


    private final StateEngineByLookup impl;

    PoV(StateEngineByLookup implem) {
        this(implem, 0, 5);
    }

    PoV(StateEngineByLookup implem, int currentTransaction, int topVarIndex) {
        this.impl = implem;
        this.currentTransaction = currentTransaction;
        this.topVarIndex = topVarIndex;
//        audit.info("New at t={}", currentTransaction);
//        audit.info("    this={}", this);
    }


    public PoV bind(Var var, Object ref) {
        if (var == ref) {
            logger.debug("Not mapping {} onto itself", var);
            return this;
        }
//        audit.info("Bind   {} -> {} at t=" + this.currentTransaction, var, ref);
        return impl.bind(this, var, ref);
    }


    /**
     * In principle one must use the recursive form reify()
     *
     * @param theVar
     * @return
     */
    public Object finalValue(Var theVar) {
        final Object dereference = this.impl.dereference(theVar, this.currentTransaction);
        return dereference;
    }

    public Object reify(Object term) {
        if (term instanceof Var) {
            term = finalValue((Var) term);
            // The var might end up on a Struct, that needs recursive reification
        }
        if (term instanceof Struct) {
//            audit.info("Reify Struct at t={}  {}", this.currentTransaction, term);
            final Struct s = (Struct) term;
            if (s.getIndex() == 0) {
                // Structure is an atom or a constant term - no need to further transform
                return term;
            }
            final Object[] arr = new Object[s.getArity()];
            for (int i = 0; i < s.getArity(); i++) {
                arr[i] = reify(s.getArg(i));
            }
            final Struct res = new Struct(s, arr);
            // TODO here one must recalculate the index (number of sub vars)
//            audit.info("               yields {}", res);
            return res;
        }
        return term;
    }


    public Map<String, Object> bindings(Object term, FreeVarRepresentation representation) {
        final Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<Var, String> entry : TermApi.allVars(term).entrySet()) {
            final Object finalValue = this.reify(entry.getKey());
            if (!(finalValue instanceof Var)) {
                result.put(entry.getValue(), finalValue);
            } else {
                result.put(entry.getValue(), null);
            }
        }
        return result;
    }

    public PoV unify(Object term1, Object term2) {
//        audit.info("Unify  {}  ~  {}", term1, term2);
        if (term1 == term2) {
            return this;
        }
        if (term2 instanceof Var) {
            // Switch arguments - we prefer having term1 being the var.
            // Notice that formally, we should check  && !(term1 instanceof Var)
            // to avoid possible useless switching when unifying Var <-> Var.
            // However, the extra instanceof total costs 3% more than a useless switch.
            final Object term1held = term1;
            term1 = term2;
            term2 = term1held;
        }
        if (term1 instanceof Var) {
            // term1 is a Var: we need to check if it is bound or not
            Var var1 = (Var) term1;
            final Object final1 = finalValue(var1);
            if (! (final1 instanceof Var)) {
                // term1 is bound - unify
                return unify(final1, term2);
            }
            // Ended up with final1 being a free Var, so term1 was a free var
            var1 = (Var) final1;
            if (var1 == Var.ANONYMOUS_VAR) {
                // Anonymous cannot be bound
                // Unified successfully, yet without side-effect
                return this;
            }
            // free Var var1 need to be bound
            if (term2 instanceof Var) {
                // Binding two vars
                final Var var2 = (Var) term2;
                final Object final2 = finalValue(var2);
                // Link one to two (should we link to the final or the initial value???)
                if (final2 == Var.ANONYMOUS_VAR) {
                    // term2 was the anonymous Var, cannot be bound
                    // Unified successfully, yet without side-effect
                    return this; // Unified without effect
                }
                // Now do the binding of two vars
                return bind(var1, var2);
            } else {
                // Do the binding of one var to a literal
                return bind(var1, term2);
            }
        } else if (term1 instanceof Struct) {
            // Case of Struct <-> Var: already taken care of by switching, see above
            if (!(term2 instanceof Struct)) {
                // Not unified - we can only unify 2 Struct
                return null;
            }
            final Struct s1 = (Struct) term1;
            final Struct s2 = (Struct) term2;
            // The two Struct must have compatible signatures (functor and arity)
            //noinspection StringEquality
            if (s1.getPredicateSignature() != s2.getPredicateSignature()) {
                return null;
            }
            // Now we will unify all arguments, stopping at the first that do not match
            final Object[] s1Args = s1.getArgs();
            final Object[] s2Args = s2.getArgs();
            final int arity = s1Args.length;
            PoV runningMonad = this;
            for (int i = 0; i < arity; i++) {
                runningMonad = runningMonad.unify(s1Args[i], s2Args[i]);
                if (runningMonad == null) {
                    // Struct sub-element not unified - fail the whole unification
                    return null;
                }
            }
            // All matched, return the latest monad
            return runningMonad;
        } else {
            return term1.equals(term2) ? this : null;
        }
    }


    @Override
    public String toString() {
        return "pov#" + this.currentTransaction + impl.toString();
    }


    // ---------------------------------------------------------------------------
    // Oldies: ThreadLocal
    // ---------------------------------------------------------------------------


    // We will use a separate class to implement a ThreadLocal if needed - maybe not
//    private static final ThreadLocal<Reifier> currentInThread = new ThreadLocal<Reifier>() {
//        @Override
//        protected Reifier initialValue() {
//            return new ReifierLookup().emptyPoV();
////            return new ReifierStack().emptyPoV();
//        }
//    };


//    /**
//     * Make current
//     */
//    public void activate() {
//        currentInThread.set(this);
//    }
//
//    public static Reifier current() {
//        return currentInThread.get();
//    }

}