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

package org.logic2j.core.impl;

import org.logic2j.core.api.TermExchanger;
import org.logic2j.core.api.model.OperatorManager;
import org.logic2j.core.api.model.TermVisitor;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TDouble;
import org.logic2j.core.api.model.symbol.TLong;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.impl.io.operator.Operator;
import org.logic2j.core.impl.io.parse.tuprolog.Parser;

/**
 * Default and reference implementation of {@link TermExchanger}.
 */
public class DefaultTermExchanger implements TermExchanger {

    private static final TermApi TERM_API = new TermApi();

    // Separator of functor arguments: f(a,b), NOT the ',' functor for logical AND.
    private static final String ARG_SEPARATOR = ", ".intern();

    // Element separator in lists: [a,b,c]
    private static final String ELEM_SEPARATOR = ",".intern();

    private class FormattingVisitor implements TermVisitor<String> {

        @Override
        public String visit(TLong theLong) {
            return String.valueOf(theLong.longValue());
        }

        @Override
        public String visit(TDouble theDouble) {
            return String.valueOf(theDouble.doubleValue());
        }

        @Override
        public String visit(Struct theStruct) {
            return String.valueOf(formatStruct(theStruct));
        }

        @Override
        public String visit(Var theVar) {
            return String.valueOf(formatVar(theVar));
        }

        /**
         * Gets the string representation of this structure
         * 
         * Specific representations are provided for lists and atoms. Names starting with upper case letter are enclosed in apices.
         */
        private String formatStruct(Struct theStruct) {
            // empty list case
            if (theStruct.isEmptyList()) {
                return Struct.FUNCTOR_LIST_EMPTY;
            }
            final String name = theStruct.getName();
            final int arity = theStruct.getArity();
            // list case
            if (name.equals(Struct.FUNCTOR_LIST) && arity == 2) {
                return ("[" + formatRecursive(theStruct) + "]");
            }
            final StringBuilder sb = new StringBuilder((Parser.isAtom(name) ? name : ('\'' + name + '\'')));
            if (arity > 0) {
                sb.append('(');
                for (int c = 1; c < arity; c++) {
                    final Term arg = theStruct.getArg(c - 1);
                    if (!(arg instanceof Var)) {
                        sb.append(arg.toString());
                        sb.append(ARG_SEPARATOR);
                    } else {
                        sb.append(formatVar((Var) arg));
                        sb.append(ARG_SEPARATOR);
                    }
                }
                if (!(theStruct.getArg(arity - 1) instanceof Var)) {
                    sb.append(theStruct.getArg(arity - 1).toString());
                    sb.append(')');
                } else {
                    sb.append(formatVar((Var) theStruct.getArg(arity - 1)));
                    sb.append(')');
                }
            }
            return sb.toString();
        }

        private String formatRecursive(Struct theStruct) {
            final Term h = theStruct.getLHS();
            final Term t = theStruct.getRHS();
            if (t.isList()) {
                final Struct tl = (Struct) t;
                if (tl.isEmptyList()) {
                    return h.toString();
                }
                if (h instanceof Var) {
                    return (formatVar((Var) h) + ELEM_SEPARATOR + formatRecursive(tl));
                }
                return (h.toString() + ELEM_SEPARATOR + formatRecursive(tl));
            }
            String h0;
            String t0;
            if (h instanceof Var) {
                h0 = formatVar((Var) h);
            } else {
                h0 = h.toString();
            }
            if (t instanceof Var) {
                t0 = formatVar((Var) t);
            } else {
                t0 = t.toString();
            }
            return (h0 + "|" + t0);
        }

        /**
         * Gets the string representation of this term as an X argument of an operator, considering the associative property.
         */
        private String toStringAsArgX(Term theTerm, OperatorManager op, int prio) {
            return toStringAsArg(theTerm, op, prio, true);
        }

        /**
         * Gets the string representation of this term as an Y argument of an operator, considering the associative property.
         */
        private String toStringAsArgY(Term theTerm, OperatorManager op, int prio) {
            return toStringAsArg(theTerm, op, prio, false);
        }

        private String toStringAsList(Struct theStruct, OperatorManager op) {
            final Term h = theStruct.getLHS();
            final Term t = theStruct.getRHS();
            if (t.isList()) {
                final Struct tl = (Struct) t;
                if (tl.isEmptyList()) {
                    return toStringAsArgY(h, op, 0);
                }
                return (toStringAsArgY(h, op, 0) + ARG_SEPARATOR + toStringAsList(tl, op));
            }
            return (toStringAsArgY(h, op, 0) + "|" + toStringAsArgY(t, op, 0));
        }

        private String toStringAsArg(Term theTerm, OperatorManager op, int prio, boolean x) {
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

    }

    private final PrologImplementation prolog;

    protected String formatVar(Var theVar) {
        return theVar.getName();
    }

    public DefaultTermExchanger(PrologReferenceImplementation theProlog) {
        this.prolog = theProlog;
    }

    public DefaultTermExchanger() {
        this.prolog = null;
    }

    @Override
    public Term unmarshall(CharSequence theChars) {
        final Parser parser = new Parser(this.prolog.getOperatorManager(), theChars.toString());
        final Term parsed = parser.parseSingleTerm();
        final Term normalized = TERM_API.normalize(parsed, this.prolog.getLibraryManager().wholeContent());
        return normalized;
    }

    @Override
    public CharSequence marshall(Term theTerm) {
        final FormattingVisitor fv = new FormattingVisitor();
        if (this.prolog != null) {
            return fv.toStringAsArgY(theTerm, this.prolog.getOperatorManager(), Operator.OP_HIGH);
        }
        return theTerm.accept(fv);
    }

}
