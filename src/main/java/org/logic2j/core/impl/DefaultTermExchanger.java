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

import org.logic2j.core.api.TermExchanger;
import org.logic2j.core.api.model.OperatorManager;
import org.logic2j.core.api.model.PartialTermVisitor;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.io.operator.Operator;
import org.logic2j.core.impl.io.parse.tuprolog.Parser;

/**
 * Default and reference implementation of {@link TermExchanger}.
 */
public class DefaultTermExchanger implements TermExchanger, PartialTermVisitor<String> {
    public static final char QUOTE = '\'';

    // Separator of functor arguments: f(a,b), NOT the ',' functor for logical AND.
    static final String ARG_SEPARATOR = ", ".intern();

    // Element separator in lists: [a,b,c]
    static final String ELEM_SEPARATOR = ",".intern();

    /**
     * @param theStruct
     * @param theBindings When null, will format the structure with raw variables names. When not null, will resolve bound vars.
     */
    @Override
    public String visit(Struct theStruct, Bindings theBindings) {
        final String formatted = formatStruct(theStruct, theBindings);
        return formatted;
    }

    @Override
    public String visit(Var theVar, Bindings theBindings) {
        if (theBindings == null) {
            return theVar.getName();
        }
        if (theVar.isAnonymous()) {
            return Var.ANONYMOUS_VAR_NAME;
        }
        // Go to fetch the effective variable value if any
        final Binding startingBinding = theVar.bindingWithin(theBindings);
        final Binding finalBinding = startingBinding.followLinks();
        if (finalBinding.isFree()) {
            return theVar.getName();
        } else {
            // Must be literal: recurse
            return TermApi.accept(this, finalBinding.getTerm(), finalBinding.getLiteralBindings());
        }

    }

    @Override
    public String visit(String theAtomString) {
        return possiblyQuote(theAtomString);
    }

    @Override
    public String visit(Long theLong) {
        return String.valueOf(theLong);
    }

    @Override
    public String visit(Double theDouble) {
        return String.valueOf(theDouble);
    }

    // @Override
    // public String visit(Object theObject) {
    // return String.valueOf(theObject);
    // }

    // protected String formatVar(Var theVar, Bindings theBindings) {
    // if (theBindings == null) {
    // return theVar.getName();
    // }
    // if (theVar.isAnonymous()) {
    // return Var.ANONYMOUS_VAR_NAME;
    // }
    // // Go to fetch the effective variable value if any
    // final Binding startingBinding = theVar.bindingWithin(theBindings);
    // final Binding finalBinding = startingBinding.followLinks();
    // if (finalBinding.isFree()) {
    // return theVar.getName();
    // } else {
    // // Must be literal: recurse
    // return TermApi.accept(this, finalBinding.getTerm(), finalBinding.getLiteralBindings());
    // }
    // }

    /**
     * Gets the string representation of this structure
     * 
     * Specific representations are provided for lists and atoms. Names starting with upper case letter are enclosed in apices.
     * 
     * @param theBindings
     */
    private String formatStruct(Struct theStruct, Bindings theBindings) {
        // empty list case
        if (theStruct.isEmptyList()) {
            return Struct.FUNCTOR_LIST_EMPTY;
        }
        final String name = theStruct.getName();
        final int arity = theStruct.getArity();
        // list case
        if (name.equals(Struct.FUNCTOR_LIST) && arity == 2) {
            return ("[" + formatPListRecursive(theStruct, theBindings) + "]");
        }
        final StringBuilder sb = new StringBuilder((Parser.isAtom(name) ? name : (QUOTE + name + QUOTE)));
        if (arity > 0) {
            sb.append('(');
            for (int c = 0; c < arity; c++) {
                final Object arg = theStruct.getArg(c);
                final String accept = TermApi.accept(this, arg, theBindings);
                sb.append(accept);
                if (c < arity - 1) {
                    sb.append(ARG_SEPARATOR);
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }

    private String formatPListRecursive(Struct theStruct, Bindings theBindings) {
        final Object head = theStruct.getLHS();
        final Object tail = theStruct.getRHS();
        if (TermApi.isList(tail)) {
            final Struct tailS = (Struct) tail;
            if (tailS.isEmptyList()) {
                return head.toString();
            }
            if (head instanceof Var) {
                return (visit((Var) head, theBindings) + ELEM_SEPARATOR + formatPListRecursive(tailS, theBindings));
            }
            return (head.toString() + ELEM_SEPARATOR + formatPListRecursive(tailS, theBindings));
        }
        String h0;
        String t0;
        if (head instanceof Var) {
            h0 = visit((Var) head, theBindings);
        } else {
            h0 = head.toString();
        }
        if (tail instanceof Var) {
            t0 = visit((Var) tail, theBindings);
        } else {
            t0 = tail.toString();
        }
        return (h0 + "|" + t0);
    }

    /**
     * Gets the string representation of this term as an X argument of an operator, considering the associative property.
     */
    private String toStringAsArgX(Object theTerm, OperatorManager op, int prio) {
        return toStringAsArg(theTerm, op, prio, true);
    }

    /**
     * Gets the string representation of this term as an Y argument of an operator, considering the associative property.
     */
    String toStringAsArgY(Object theTerm, OperatorManager op, int prio) {
        return toStringAsArg(theTerm, op, prio, false);
    }

    private String toStringAsList(Struct theStruct, OperatorManager op) {
        final Object h = theStruct.getLHS();
        final Object t = theStruct.getRHS();
        if (TermApi.isList(t)) {
            final Struct tl = (Struct) t;
            if (tl.isEmptyList()) {
                return toStringAsArgY(h, op, 0);
            }
            return (toStringAsArgY(h, op, 0) + ARG_SEPARATOR + toStringAsList(tl, op));
        }
        return (toStringAsArgY(h, op, 0) + "|" + toStringAsArgY(t, op, 0));
    }

    private String toStringAsArg(Object theTerm, OperatorManager op, int prio, boolean x) {
        if (theTerm instanceof CharSequence) {
            return possiblyQuote((CharSequence) theTerm);
        }
        if (!(theTerm instanceof Struct)) {
            return theTerm.toString();
        }
        final Struct theStruct = (Struct) theTerm;
        int p = 0;
        final String name = theStruct.getName();
        final int arity = theStruct.getArity();

        if (name.equals(Struct.FUNCTOR_LIST) && arity == 2) {
            if (theStruct.getLHS() instanceof Struct && ((Struct) theStruct.getLHS()).isEmptyList()) {
                return Struct.FUNCTOR_LIST_EMPTY;
            }
            return ("[" + toStringAsList(theStruct, op) + "]");
        }

        if (arity == 2) {
            if ((p = op.opPrio(name, Operator.XFX)) >= Operator.OP_LOW) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name + " " + toStringAsArgX(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                        : ""));
            }
            if ((p = op.opPrio(name, Operator.YFX)) >= Operator.OP_LOW) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), op, p) + " " + name + " " + toStringAsArgX(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                        : ""));
            }
            if ((p = op.opPrio(name, Operator.XFY)) >= Operator.OP_LOW) {
                if (!name.equals(ARG_SEPARATOR)) {
                    return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name + " " + toStringAsArgY(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                            : ""));
                }
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + ARG_SEPARATOR + toStringAsArgY(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                        : ""));
            }
        } else if (arity == 1) {
            if ((p = op.opPrio(name, Operator.FX)) >= Operator.OP_LOW) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgX(theStruct.getLHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = op.opPrio(name, Operator.FY)) >= Operator.OP_LOW) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgY(theStruct.getLHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = op.opPrio(name, Operator.XF)) >= Operator.OP_LOW) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = op.opPrio(name, Operator.YF)) >= Operator.OP_LOW) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), op, p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
        }
        final StringBuilder sb = new StringBuilder(Parser.isAtom(name) ? name : "'" + name + "'");
        if (arity == 0) {
            return sb.toString();
        }
        sb.append('(');
        for (p = 1; p < arity; p++) {
            sb.append(toStringAsArgY(theStruct.getArg(p - 1), op, 0));
            sb.append(ARG_SEPARATOR);
        }
        sb.append(toStringAsArgY(theStruct.getArg(arity - 1), op, 0));
        sb.append(')');
        return sb.toString();
    }

    private final PrologImplementation prolog;

    /**
     * @param theText
     * @return
     */
    public static String possiblyQuote(CharSequence theText) {
        if (theText == null) {
            return null;
        }
        if (theText.length() == 0) {
            return "''";
        }
        final String str = theText.toString();
        final boolean needQuote = !Character.isLowerCase(str.charAt(0)) || str.indexOf('.') >= 0;
        if (needQuote) {
            final StringBuilder sb = new StringBuilder(str.length() + 2);
            sb.append(QUOTE);
            sb.append(theText);
            sb.append(QUOTE);
            return sb.toString();
        }
        return theText.toString();
    }

    public DefaultTermExchanger(PrologReferenceImplementation theProlog) {
        this.prolog = theProlog;
    }

    public DefaultTermExchanger() {
        this.prolog = null;
    }

    @Override
    public Object unmarshall(CharSequence theChars) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theChars.toString());
        final Object parsed = parser.parseSingleTerm();
        final Object normalized = TermApi.normalize(parsed, this.prolog.getLibraryManager().wholeContent());
        return normalized;
    }

    @Override
    public CharSequence marshall(Object theTerm) {
        // Basic Term.toString() will use this method. For normal marshalling we have this.prolog insantiated!
        if (this.prolog != null && theTerm instanceof Struct) {
            return this.toStringAsArgY(theTerm, this.prolog.getOperatorManager(), Operator.OP_HIGH);
        }
        if (theTerm instanceof Bindings) {
            final Bindings b = (Bindings) theTerm;
            return TermApi.accept(this, b.getReferrer(), b);
        }
        return TermApi.accept(this, theTerm, null);
    }

}
