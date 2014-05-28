package org.logic2j.core.impl;


import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.model.visitor.TermVisitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TBD
 */
public class CloningTermVisitor implements TermVisitor<Object> {
    public final Map<Var, Var> vars = new LinkedHashMap<Var, Var>();

    @Override
    public Object visit(Struct theStruct) {
        final int arity = theStruct.getArity();
        final Object[] elements = new Object[arity];
        for (int i = 0; i < arity; i++) {
            final Object arg = theStruct.getArg(i);
            if (arg instanceof Term) {
                final Object recursedClonedElement = ((Term)arg).accept(this);
                elements[i] = recursedClonedElement;
            } else {
                elements[i] = arg;
            }
        }
        final Struct struct = new Struct(theStruct, elements);
        return struct;
    }

    @Override
    public Object visit(Var theVar) {
        if (theVar == Var.ANONYMOUS_VAR) {
            return theVar;
        }
        Var alreadyCloned = vars.get(theVar);
        if (alreadyCloned!=null) {
            return alreadyCloned;
        }
        final Var cloned = new Var(theVar);
        vars.put(theVar, cloned);
        return cloned;
    }

}
