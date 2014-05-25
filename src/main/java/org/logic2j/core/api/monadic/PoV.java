package org.logic2j.core.api.monadic;

import org.logic2j.core.api.model.FreeVarRepresentation;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.impl.CloningTermVisitor;
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
    int topVarIndex;


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




    public Object cloneTermAndRemapIndexes(Object theTerm) {
        if (theTerm instanceof Term) {
//            audit.info("Clone  {}  (base={})", theTerm, this.topVarIndex);
            final CloningTermVisitor cloningTermVisitor = new CloningTermVisitor();
            final Object cloned = ((Term) theTerm).accept(cloningTermVisitor);
            for (Var newVar: cloningTermVisitor.vars.values()) {
                newVar.index += this.topVarIndex;
            }
            this.topVarIndex += cloningTermVisitor.vars.size();
//            audit.info("Cloned {}  (base={})", cloned, this.topVarIndex);
            return cloned;
        }
        return theTerm;
    }



    public PoV bind(Var var, Object ref) {
        if (var==ref) {
            logger.debug("Not mapping {} onto itself", var);
            return this;
        }
//        audit.info("Bind   {} -> {} at t=" + this.currentTransaction, var, ref);
        return impl.bind(this, var, ref);
    }


    /**
     * In principle one must use the recursive form reify()
     * @param theVar
     * @return
     */
    public Object finalValue(Var theVar) {
        final Object dereference = this.impl.dereference(theVar, this.currentTransaction);
//        audit.info("Deref  {} yields {}", theVar, dereference);
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
            if (s.getIndex()==0) {
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
            if (! (finalValue instanceof Var)) {
                result.put(entry.getValue(), finalValue);
            } else {
                result.put(entry.getValue(), null);
            }
        }
        return result;
    }
    // --- Move elsewhere

    public PoV unify(Object t1, Object t2) {
//        audit.info("Unify  {}  ~  {}", t1, t2);
        if (t1 == t2) {
            return this;
        }
        if (t2 instanceof Var && !(t1 instanceof Var)) {
            // Recursive inversion
            return unify(t2, t1);
        }
        if (t1 instanceof Var) {
            Var v1 = (Var) t1;
            Object f1 = finalValue(v1);
            if (!(f1 instanceof Var)) {
                return unify(f1, t2);
            }
            // Ending up on free var f1
            v1 = (Var) f1;
            if (v1.isAnonymous()) {
                return this; // Unified without effect
            }
            // Free var to bind
            if (t2 instanceof Var) {
                final Var v2 = (Var) t2;
                Object f2 = finalValue(v2);
                // Link one to two (should we link to the final or the initial value???)
                if (f2 instanceof Var && ((Var) f2).isAnonymous()) {
                    return this; // Unified without effect
                }
                return bind(v1, v2);
            } else {
                return bind(v1, t2);
            }
        } else if (t1 instanceof Struct) {
            // Struct - var already taken care of by recursive inversion
            if (!(t2 instanceof Struct)) {
                return null;
            }
            Struct s1 = (Struct) t1;
            Struct s2 = (Struct) t2;
            //noinspection StringEquality
            if (s1.getPredicateSignature() != s2.getPredicateSignature()) {
                return null;
            }
            Object[] s1Args = s1.getArgs();
            Object[] s2Args = s2.getArgs();
            PoV runningMonad = this;
            // Mark the monad
            for (int i = 0; i != s1Args.length; i++) {
                runningMonad = runningMonad.unify(s1Args[i], s2Args[i]);
                if (runningMonad == null) {
                    return null;
                }
            }
            return runningMonad;
        } else {
            return t1.equals(t2) ? this : null;
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