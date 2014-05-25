package org.logic2j.core.api.model;


/**
 * Determine how free (unbound) variables will be represented.
 */
public enum FreeVarRepresentation {
    /**
     * Free variables will not be included in result bindings. Asking the value of a variable that is not bound to a literal Term is
     * likely to throw a {@link RuntimeException}.
     */
    SKIPPED,

    /**
     * Free variables will be represented by the existence of a Map {@link java.util.Map.Entry} with a valid key, and a value equal to null. You are
     * required to use {@link java.util.Map#containsKey(Object)} to identify this case, since {@link java.util.Map#get(Object)} returning null won't allow
     * you to distinguish between a free variable (asking for "X" when X is still unbound) or asking the value of an undefined variable
     * (asking "TOTO" when not in original goal).
     */
    NULL,

    /**
     * Free variables will be reported with their terminal value. If we have the chain of bindings being X -> Y -> Z, this mode will
     * represent X with the mapping "X"->"Z". A particular case is when X is unified to itself (via a loop), then the mapping becomes
     * "X" -> "X" which may not be desirable... See FREE_NOT_SELF.
     */
    FREE,

    /**
     * TBD.
     */
    FREE_NOT_SELF

}
