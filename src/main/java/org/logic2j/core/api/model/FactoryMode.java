package org.logic2j.core.api.model;

/**
 * Created by Laurent on 18.05.2014.
 */
public enum FactoryMode {
    /**
     * Result will always be an atom (a {@link org.logic2j.core.api.model.symbol.Struct} of 0-arity), will never be a {@link org.logic2j.core.api.model.symbol.Var}iable.
     * In the case of null, will create an empty-string atom.
     */
    ATOM,

    /**
     * Result will be either an atom (a {@link org.logic2j.core.api.model.symbol.Struct} of 0-arity), an object, but not a {@link org.logic2j.core.api.model.symbol.Var}iable neither a
     * compound {@link org.logic2j.core.api.model.symbol.Struct}.
     */
    LITERAL,

    /**
     * Result will be any {@link org.logic2j.core.api.model.symbol.Term} (atom, number, {@link org.logic2j.core.api.model.symbol.Var}iable), but not a compound {@link org.logic2j.core.api.model.symbol.Struct}.
     */
    ANY_TERM,

    /**
     * Result can be any term plus compound structures.
     */
    COMPOUND
}
