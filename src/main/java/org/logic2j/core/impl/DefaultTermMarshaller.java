/*
 * logic2j - "Bring Logic to your Java" - Copyright (QUOTE) 2011 Laurent.Tettoni@gmail.com
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

package org.logic2j.core.impl;

import org.logic2j.core.api.OperatorManager;
import org.logic2j.core.api.TermMarshaller;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.api.model.visitor.ExtendedTermVisitor;
import org.logic2j.core.api.model.Operator;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;

/**
 * Default and reference implementation of {@link org.logic2j.core.api.TermMarshaller#marshall(Object)}.
 * This implementation may be derived or composed to your wish.
 */
public class DefaultTermMarshaller implements TermMarshaller, ExtendedTermVisitor<CharSequence> {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTermMarshaller.class);

    private static final boolean isDebug = logger.isDebugEnabled();

    public static final DefaultOperatorManager DEFAULT_OPERATOR_MANAGER = new DefaultOperatorManager();

    private final OperatorManager operatorManager = DEFAULT_OPERATOR_MANAGER;

    private final UnifyContext currentVars;

    public DefaultTermMarshaller() {
        this(null);
    }


    public DefaultTermMarshaller(UnifyContext currentVars) {
        this.currentVars = currentVars;
    }

    @Override
    public CharSequence marshall(Object theTerm) {
        if (theTerm instanceof Struct) {
            // Rich formatting takes care of operators and lists
            return this.toStringAsArgY(theTerm, Operator.OP_HIGHEST);
        }
        return accept(theTerm);
    }

    // ---------------------------------------------------------------------------
    // Visitor
    // ---------------------------------------------------------------------------

    @Override
    public CharSequence visit(Var theVar) {
        // logger.info("Visiting: {}", theVar);
        final Object finalValue;
        if (this.currentVars != null) {
            finalValue = this.currentVars.finalValue(theVar);
        } else {
            finalValue = theVar;
        }
        if (finalValue instanceof Var) {
            // Must be free
            if (theVar.isAnonymous()) {
                return Var.ANONYMOUS_VAR_NAME;
            }
            return theVar.getName();
        }
        return this.marshall(finalValue);
    }

    /**
     * @param theStruct
     */
    @Override
    public CharSequence visit(Struct theStruct) {
        final CharSequence formatted = formatStruct(theStruct);
        return formatted;
    }

    @Override
    public CharSequence visit(String theAtomString) {
        return possiblyQuote(theAtomString);
    }

    @Override
    public CharSequence visit(Object theObject) {
        return String.valueOf(theObject);
    }

    // ---------------------------------------------------------------------------
    // Support methods
    // ---------------------------------------------------------------------------

    /**
     * Just a derivable shortcut to {@link org.logic2j.core.api.model.term.TermApi#accept(ExtendedTermVisitor, Object)}.
     *
     * @param theTerm
     * @return The formatted term.
     */
    protected CharSequence accept(Object theTerm) {
        return TermApi.accept(this, theTerm);
    }

    /**
     * Gets the string representation of this structure
     * <p/>
     * Specific representations are provided for lists and atoms. Names starting with upper case letter are enclosed in apices.
     */
    // TODO similar if not equal to the one in TermApi
    private CharSequence formatStruct(Struct theStruct) {
        // empty list case
        if (theStruct.isEmptyList()) {
            return Struct.FUNCTOR_EMPTY_LIST;
        }
        final StringBuilder sb = new StringBuilder();
        final String name = theStruct.getName();
        final int arity = theStruct.getArity();
        // list case
        if (name.equals(Struct.FUNCTOR_LIST_NODE) && arity == 2) {
            sb.append(Struct.LIST_OPEN);
            sb.append(formatPListRecursive(theStruct));
            sb.append(Struct.LIST_CLOSE);
            return sb;
        }
        if (TermApi.isAtom(name)) {
            sb.append(name);
        } else {
            sb.append(Struct.QUOTE);
            sb.append(name);
            sb.append(Struct.QUOTE);
        }
        if (arity > 0) {
            sb.append(Struct.PAR_OPEN);
            for (int c = 0; c < arity; c++) {
                final Object arg = theStruct.getArg(c);
                final CharSequence formatted = accept(arg);
                sb.append(formatted);
                if (c < arity - 1) {
                    sb.append(Struct.ARG_SEPARATOR);
                }
            }
            sb.append(Struct.PAR_CLOSE);
        }
        return sb;
    }

    // TODO similar if not equal to the one in TermApi
    private CharSequence formatPListRecursive(Struct theStruct) {
        final Object head = theStruct.getLHS();
        final Object tail = theStruct.getRHS();
        if (TermApi.isList(tail)) {
            final Struct tailStruct = (Struct) tail;
            if (tailStruct.isEmptyList()) {
                return accept(head);
            }
            // Why this special test?
            if (head instanceof Var) {
                return visit((Var) head) + Struct.LIST_ELEM_SEPARATOR + formatPListRecursive(tailStruct);
            }
            return accept(head) + Struct.LIST_ELEM_SEPARATOR + formatPListRecursive(tailStruct);
        }
        final StringBuilder sb = new StringBuilder();
        // Head
        final CharSequence h0;
        if (head instanceof Var) {
            h0 = visit((Var) head);
        } else {
            h0 = accept(head);
        }
        sb.append(h0);
        sb.append(Struct.HEAD_TAIL_SEPARATOR);
        // Tail
        final CharSequence t0;
        if (tail instanceof Var) {
            t0 = visit((Var) tail);
        } else {
            t0 = accept(tail);
        }
        sb.append(t0);
        return sb;
    }

    /**
     * Gets the string representation of this term as an X argument of an operator, considering the associative property.
     */
    private CharSequence toStringAsArgX(Object theTerm, int prio) {
        return toStringAsArg(theTerm, prio, true);
    }

    /**
     * Gets the string representation of this term as an Y argument of an operator, considering the associative property.
     */
    private CharSequence toStringAsArgY(Object theTerm, int prio) {
        return toStringAsArg(theTerm, prio, false);
    }

    private CharSequence toStringAsList(Struct theStruct) {
        final Object h = theStruct.getLHS();
        final Object t = theStruct.getRHS();
        if (TermApi.isList(t)) {
            final Struct tl = (Struct) t;
            if (tl.isEmptyList()) {
                return toStringAsArgY(h, 0);
            }
            final StringBuilder sb = new StringBuilder();
            sb.append(toStringAsArgY(h, 0));
            sb.append(Struct.ARG_SEPARATOR);
            sb.append(toStringAsList(tl));
            return sb;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(toStringAsArgY(h, 0));
        sb.append(Struct.HEAD_TAIL_SEPARATOR);
        sb.append(toStringAsArgY(t, 0));
        return sb;
    }

    private CharSequence toStringAsArg(Object theTerm, int prio, boolean x) {
        if (theTerm instanceof CharSequence) {
            return possiblyQuote((CharSequence) theTerm);
        }
        if (!(theTerm instanceof Struct)) {
            return accept(theTerm);
        }
        final Struct theStruct = (Struct) theTerm;
        int p = 0;
        final String name = theStruct.getName();
        final int arity = theStruct.getArity();

        if (name.equals(Struct.FUNCTOR_LIST_NODE) && arity == 2) {
            if (theStruct.getLHS() instanceof Struct && ((Struct) theStruct.getLHS()).isEmptyList()) {
                return Struct.FUNCTOR_EMPTY_LIST;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append(Struct.LIST_OPEN);
            sb.append(toStringAsList(theStruct));
            sb.append(Struct.LIST_CLOSE);
            return sb;
        }


        if (arity == 2) {
            if ((p = operatorManager.opPrio(name, Operator.XFX)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " " + toStringAsArgX(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                : ""));
            }
            if ((p = operatorManager.opPrio(name, Operator.YFX)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), p) + " " + name + " " + toStringAsArgX(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                : ""));
            }
            if ((p = operatorManager.opPrio(name, Operator.XFY)) >= Operator.OP_LOWEST) {
                if (!name.equals(Struct.ARG_SEPARATOR)) {
                    return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " " + toStringAsArgY(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                    : ""));
                }
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + Struct.ARG_SEPARATOR + toStringAsArgY(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                : ""));
            }
        } else if (arity == 1) {
            if ((p = operatorManager.opPrio(name, Operator.FX)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgX(theStruct.getLHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = operatorManager.opPrio(name, Operator.FY)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgY(theStruct.getLHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = operatorManager.opPrio(name, Operator.XF)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = operatorManager.opPrio(name, Operator.YF)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
        }
        final StringBuilder sb = new StringBuilder(TermApi.isAtom(name) ? name : "'" + name + "'");
        if (arity == 0) {
            return sb.toString();
        }
        sb.append(Struct.PAR_OPEN);
        for (p = 1; p < arity; p++) {
            sb.append(toStringAsArgY(theStruct.getArg(p - 1), 0));
            sb.append(Struct.ARG_SEPARATOR);
        }
        sb.append(toStringAsArgY(theStruct.getArg(arity - 1), 0));
        sb.append(Struct.PAR_CLOSE);
        return sb.toString();
    }

    /**
     * Quote atoms if needed.
     *
     * @param theText
     * @return theText, quoted if necessary (typically "X" will become "'X'" whereas "x" will remain unchanged.
     * Null will return null. The empty string will become "''". If not quoted, the same reference (theText) is returned.
     */
    private static CharSequence possiblyQuote(CharSequence theText) {
        if (theText == null) {
            return null;
        }
        if (theText.length() == 0) {
            // Probably that the empty string is not allowed in regular Prolog
            return "''";
        }
        final boolean needQuote = !Character.isLowerCase(theText.charAt(0)) || theText.toString().indexOf('.') >= 0;
        if (needQuote) {
            final StringBuilder sb = new StringBuilder(theText.length() + 2);
            sb.append(Struct.QUOTE);
            sb.append(theText);
            sb.append(Struct.QUOTE);
            return sb;
        }
        return theText;
    }

}
