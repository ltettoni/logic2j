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
package org.logic2j.core.io.format;

import org.logic2j.core.Formatter;
import org.logic2j.core.PrologImplementor;
import org.logic2j.core.io.operator.Operator;
import org.logic2j.core.io.operator.OperatorManager;
import org.logic2j.core.io.parse.tuprolog.Parser;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.StructObject;
import org.logic2j.core.model.symbol.TDouble;
import org.logic2j.core.model.symbol.TLong;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.Var;

/**
 * Formats {@link Term}s in a quite classical manner (at least it's readable and looks like Prolog).
 */
public class DefaultFormatter implements Formatter {
    private final PrologImplementor prolog; // When provided, will be able to format using the defined operators

    // Separator of functor arguments: f(a,b), NOT the ',' functor for logical AND.
    private static final String ARG_SEPARATOR = ", ".intern();

    // Element separator in lists: [a,b,c]
    private static final String ELEM_SEPARATOR = ",".intern();

    /**
     * A {@link Formatter} that will use the defined operators to render structures.
     * 
     * @param theProlog
     */
    public DefaultFormatter(PrologImplementor theProlog) {
        prolog = theProlog;
    }

    /**
     * A {@link Formatter} that won't be capable of using defined operators to render predicates. Hence, any structure such as "a,b" will be
     * rendered as "','(a,b)".
     */
    public DefaultFormatter() {
        this(null);
    }

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
    public String visit(StructObject<?> theStructObject) {
        final StringBuilder sb = new StringBuilder();
        sb.append(formatStruct(theStructObject));
        sb.append('(');
        sb.append(theStructObject.getObject());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public String visit(Var theVar) {
        return String.valueOf(formatVar(theVar));
    }

    protected String formatVar(Var theVar) {
        return theVar.getName();
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
        String s = (Parser.isAtom(name) ? name : ('\'' + name + '\''));
        if (arity > 0) {
            s = s + "(";
            for (int c = 1; c < arity; c++) {
                Term arg = theStruct.getArg(c - 1);
                if (!(arg instanceof Var)) {
                    s = s + arg.toString() + DefaultFormatter.ARG_SEPARATOR;
                } else {
                    s = s + formatVar((Var) arg) + DefaultFormatter.ARG_SEPARATOR;
                }
            }
            if (!(theStruct.getArg(arity - 1) instanceof Var)) {
                s = s + theStruct.getArg(arity - 1).toString() + ")";
            } else {
                s = s + formatVar((Var) theStruct.getArg(arity - 1)) + ")";
            }
        }
        return s;
    }

    private String formatRecursive(Struct theStruct) {
        Term h = theStruct.getLHS();
        Term t = theStruct.getRHS();
        if (t.isList()) {
            Struct tl = (Struct) t;
            if (tl.isEmptyList()) {
                return h.toString();
            }
            if (h instanceof Var) {
                return (formatVar((Var) h) + DefaultFormatter.ELEM_SEPARATOR + formatRecursive(tl));
            }
            return (h.toString() + DefaultFormatter.ELEM_SEPARATOR + formatRecursive(tl));
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
        Term h = theStruct.getLHS();
        Term t = theStruct.getRHS();
        if (t.isList()) {
            Struct tl = (Struct) t;
            if (tl.isEmptyList()) {
                return toStringAsArgY(h, op, 0);
            }
            return (toStringAsArgY(h, op, 0) + DefaultFormatter.ARG_SEPARATOR + toStringAsList(tl, op));
        }
        return (toStringAsArgY(h, op, 0) + "|" + toStringAsArgY(t, op, 0));
    }

    private String toStringAsArg(Term theTerm, OperatorManager op, int prio, boolean x) {
        if (!(theTerm instanceof Struct)) {
            return theTerm.toString();
        }
        if (theTerm instanceof StructObject<?>) {
            return theTerm.toString();
        }
        Struct theStruct = (Struct) theTerm;
        int p = 0;
        String v = "";
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
                if (!name.equals(DefaultFormatter.ARG_SEPARATOR)) {
                    return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name + " " + toStringAsArgY(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
                            : ""));
                }
                return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + DefaultFormatter.ARG_SEPARATOR + toStringAsArgY(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
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
        v = (Parser.isAtom(name) ? name : "'" + name + "'");
        if (arity == 0) {
            return v;
        }
        v = v + "(";
        for (p = 1; p < arity; p++) {
            v = v + toStringAsArgY(theStruct.getArg(p - 1), op, 0) + DefaultFormatter.ARG_SEPARATOR;
        }
        v = v + toStringAsArgY(theStruct.getArg(arity - 1), op, 0);
        v = v + ")";
        return v;
    }

    @Override
    public String format(Term theTerm) {
        if (prolog != null) {
            return toStringAsArgY(theTerm, prolog.getOperatorManager(), Operator.OP_HIGH);
        }
        return theTerm.accept(this);
    }

}
