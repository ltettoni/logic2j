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

import org.logic2j.core.api.TermMarshaller;
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
 * Default and reference implementation of {@link TermMarshaller#marshall(Object)}.
 * This implementation may be derived or composed to your wish.
 */
public class DefaultTermMarshaller implements TermMarshaller, PartialTermVisitor<CharSequence> {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTermMarshaller.class);
    static final boolean isDebug = logger.isDebugEnabled();
  
    static final char QUOTE = '\'';

    // Separator of functor arguments: f(a,b), NOT the ',' functor for logical AND.
    static final String ARG_SEPARATOR = ", ".intern();

    // Element separator in lists: [a,b,c]
    static final String ELEM_SEPARATOR = ",".intern();

    // Watch out, may be null
    private final PrologImplementation prolog;

    /**
     * This constructor should only be used internally - for basic formatting.
     */
    public DefaultTermMarshaller() {
        this.prolog = null;
    }

    public DefaultTermMarshaller(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    /**
     * Entry point for mashalling, normally this.prolog is initialized, but
     * Term.toString() will use this method with this.prolog==null.
     */
    @Override
    public CharSequence marshall(Object theTerm) {
        if (theTerm instanceof Struct && this.prolog != null) {
            // Rich formatting takes care of operators and lists
            return this.toStringAsArgY(theTerm, Operator.OP_HIGHEST);
        }
        if (theTerm instanceof Bindings) {
            final Bindings b = (Bindings) theTerm;
            return accept(b.getReferrer(), b);
        }
        return accept(theTerm, null);
    }

    // ---------------------------------------------------------------------------
    // Visitor
    // ---------------------------------------------------------------------------


    /**
     * @param theStruct
     * @param theBindings When null, will format the structure with raw variables names. When not null, will resolve bound vars.
     */
    @Override
    public CharSequence visit(Struct theStruct, Bindings theBindings) {
        final CharSequence formatted = formatStruct(theStruct, theBindings);
        return formatted;
    }

    @Override
    public CharSequence visit(Var theVar, Bindings theBindings) {
      final StringBuilder sb = new StringBuilder();
        if (theBindings == null) {
            sb.append(theVar.getName());
            if (isDebug) {
                // Report index of var
                sb.append('#');
                sb.append(theVar.getIndex());
            }
            return sb.toString();
        }
        if (theVar.isAnonymous()) {
            return Var.ANONYMOUS_VAR_NAME;
        }
        // Go to fetch the effective variable value if any
        final Binding startingBinding = theVar.bindingWithin(theBindings);
        final Binding finalBinding = startingBinding.followLinks();
        final CharSequence formatted;
        if (finalBinding.isFree()) {
            formatted = theVar.getName();
        } else {
            // Here it can only be a literal: recurse
            formatted = accept(finalBinding.getTerm(), finalBinding.getLiteralBindings());
        }
        return formatted;
    }

    @Override
    public CharSequence visit(String theAtomString) {
        return possiblyQuote(theAtomString);
    }

    @Override
    public CharSequence visit(Long theLong) {
        return String.valueOf(theLong);
    }

    @Override
    public CharSequence visit(Double theDouble) {
        return String.valueOf(theDouble);
    }

    @Override
    public CharSequence visit(Object theObject) {
        return String.valueOf(theObject);
    }

    // ---------------------------------------------------------------------------
    // Support methods
    // ---------------------------------------------------------------------------

    /**
     * Just a derivable shortcut to {@link TermApi#accept(PartialTermVisitor, Object, Bindings)}.
     * 
     * @param theTerm
     * @param theBindings
     * @return The formatted term.
     */
    protected CharSequence accept(Object theTerm, Bindings theBindings) {
      return TermApi.accept(this, theTerm, theBindings);
    }
    
    /**
     * Gets the string representation of this structure
     * 
     * Specific representations are provided for lists and atoms. Names starting with upper case letter are enclosed in apices.
     * 
     * @param theBindings
     */
    private CharSequence formatStruct(Struct theStruct, Bindings theBindings) {
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
                final CharSequence formatted = accept(arg, theBindings);
                sb.append(formatted);
                if (c < arity - 1) {
                    sb.append(ARG_SEPARATOR);
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }

    private CharSequence formatPListRecursive(Struct theStruct, Bindings theBindings) {
        final Object head = theStruct.getLHS();
        final Object tail = theStruct.getRHS();
        if (TermApi.isList(tail)) {
            final Struct tailStruct = (Struct) tail;
            if (tailStruct.isEmptyList()) {
                return accept(head, theBindings);
            }
            if (head instanceof Var) {
                return visit((Var) head, theBindings) + ELEM_SEPARATOR + formatPListRecursive(tailStruct, theBindings);
            }
            return accept(head, theBindings) + ELEM_SEPARATOR + formatPListRecursive(tailStruct, theBindings);
        }
        CharSequence h0;
        CharSequence t0;
        if (head instanceof Var) {
            h0 = visit((Var) head, theBindings);
        } else {
            h0 = accept(head, theBindings);
        }
        if (tail instanceof Var) {
            t0 = visit((Var) tail, theBindings);
        } else {
            t0 = accept(tail, theBindings);
        }
        return (h0 + "|" + t0);
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
            return (toStringAsArgY(h, 0) + ARG_SEPARATOR + toStringAsList(tl));
        }
        return (toStringAsArgY(h, 0) + "|" + toStringAsArgY(t, 0));
    }

    private CharSequence toStringAsArg(Object theTerm, int prio, boolean x) {
        if (theTerm instanceof CharSequence) {
            return possiblyQuote((CharSequence) theTerm);
        }
        if (!(theTerm instanceof Struct)) {
            return accept(theTerm, null);
        }
        final Struct theStruct = (Struct) theTerm;
        int p = 0;
        final String name = theStruct.getName();
        final int arity = theStruct.getArity();

        if (name.equals(Struct.FUNCTOR_LIST) && arity == 2) {
            if (theStruct.getLHS() instanceof Struct && ((Struct) theStruct.getLHS()).isEmptyList()) {
                return Struct.FUNCTOR_LIST_EMPTY;
            }
            return ("[" + toStringAsList(theStruct) + "]");
        }

        final OperatorManager op = this.prolog.getOperatorManager();
        if (arity == 2) {
            if ((p = op.opPrio(name, Operator.XFX)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " " + toStringAsArgX(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                        : ""));
            }
            if ((p = op.opPrio(name, Operator.YFX)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), p) + " " + name + " " + toStringAsArgX(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                        : ""));
            }
            if ((p = op.opPrio(name, Operator.XFY)) >= Operator.OP_LOWEST) {
                if (!name.equals(ARG_SEPARATOR)) {
                    return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " " + toStringAsArgY(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                            : ""));
                }
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + ARG_SEPARATOR + toStringAsArgY(theStruct.getRHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                        : ""));
            }
        } else if (arity == 1) {
            if ((p = op.opPrio(name, Operator.FX)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgX(theStruct.getLHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = op.opPrio(name, Operator.FY)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgY(theStruct.getLHS(), p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = op.opPrio(name, Operator.XF)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
            if ((p = op.opPrio(name, Operator.YF)) >= Operator.OP_LOWEST) {
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
            }
        }
        final StringBuilder sb = new StringBuilder(Parser.isAtom(name) ? name : "'" + name + "'");
        if (arity == 0) {
            return sb.toString();
        }
        sb.append('(');
        for (p = 1; p < arity; p++) {
            sb.append(toStringAsArgY(theStruct.getArg(p - 1), 0));
            sb.append(ARG_SEPARATOR);
        }
        sb.append(toStringAsArgY(theStruct.getArg(arity - 1), 0));
        sb.append(')');
        return sb.toString();
    }

    private static CharSequence possiblyQuote(CharSequence theText) {
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

}
