package org.logic2j.io.format;

import org.logic2j.Formatter;
import org.logic2j.PrologImplementor;
import org.logic2j.io.operator.Operator;
import org.logic2j.io.operator.OperatorManager;
import org.logic2j.io.parse.tuprolog.Parser;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.StructObject;
import org.logic2j.model.symbol.TDouble;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;

/**
 * Formats terms.
 */
public class DefaultFormatter implements TermVisitor<Void>, Formatter {
  private StringBuilder sb = new StringBuilder();
  private final PrologImplementor prolog;

  // Separator of functor arguments: f(a,b), NOT the ',' functor for logical AND.
  private static final String ARG_SEPARATOR = ", ".intern();

  // Element separator in lists
  private static final String ELEM_SEPARATOR = ",".intern();

  /**
   * @param theProlog
   */
  public DefaultFormatter(PrologImplementor theProlog) {
    this.prolog = theProlog;
  }

  public DefaultFormatter() {
    this(null);
  }

  public String formatted() {
    return this.sb.toString();
  }

  @Override
  public Void visit(TLong theLong) {
    this.sb.append(theLong.longValue());
    return null;
  }

  @Override
  public Void visit(TDouble theDouble) {
    this.sb.append(theDouble.doubleValue());
    return null;
  }

  @Override
  public Void visit(Struct theStruct) {
    this.sb.append(formatStruct(theStruct));
    return null;
  }

  @Override
  public Void visit(StructObject<?> theStructObject) {
    this.sb.append(formatStruct(theStructObject));
    this.sb.append('(');
    this.sb.append(theStructObject.getObject());
    this.sb.append(')');
    return null;
  }

  @Override
  public Void visit(Var theVar) {
    this.sb.append(formatVar(theVar));
    return null;
  }

  private String formatVar(Var theVar) {
    if (theVar.isAnonymous()) {
      return Var.ANY; // + '_' + theVar.hashCode();
    } else {
      return theVar.getName();
    }
  }

  /**
   * Gets the string representation of this structure
   *
   * Specific representations are provided for lists and atoms.
   * Names starting with upper case letter are enclosed in apices.
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
    } else {
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
      } else {
        return (h.toString() + DefaultFormatter.ELEM_SEPARATOR + formatRecursive(tl));
      }
    } else {
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
  }

  /**
  * Gets the string representation of this term
  * as an X argument of an operator, considering the associative property.
  */
  private String toStringAsArgX(Term theTerm, OperatorManager op, int prio) {
    return toStringAsArg(theTerm, op, prio, true);
  }

  /**
  * Gets the string representation of this term
  * as an Y argument of an operator, considering the associative property.
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
    } else {
      return (toStringAsArgY(h, op, 0) + "|" + toStringAsArgY(t, op, 0));
    }
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
      } else {
        return ("[" + toStringAsList(theStruct, op) + "]");
      }
    }

    if (arity == 2) {
      if ((p = op.opPrio(name, Operator.XFX)) >= Operator.OP_LOW) {
        return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name + " "
            + toStringAsArgX(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
      }
      if ((p = op.opPrio(name, Operator.YFX)) >= Operator.OP_LOW) {
        return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), op, p) + " " + name + " "
            + toStringAsArgX(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
      }
      if ((p = op.opPrio(name, Operator.XFY)) >= Operator.OP_LOW) {
        if (!name.equals(DefaultFormatter.ARG_SEPARATOR)) {
          return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name
              + " " + toStringAsArgY(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")" : ""));
        } else {
          return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p)
              + DefaultFormatter.ARG_SEPARATOR + toStringAsArgY(theStruct.getRHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
              : ""));
        }
      }
    } else if (arity == 1) {
      if ((p = op.opPrio(name, Operator.FX)) >= Operator.OP_LOW) {
        return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgX(theStruct.getLHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
            : ""));
      }
      if ((p = op.opPrio(name, Operator.FY)) >= Operator.OP_LOW) {
        return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + name + " " + toStringAsArgY(theStruct.getLHS(), op, p) + (((x && p >= prio) || (!x && p > prio)) ? ")"
            : ""));
      }
      if ((p = op.opPrio(name, Operator.XF)) >= Operator.OP_LOW) {
        return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), op, p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")"
            : ""));
      }
      if ((p = op.opPrio(name, Operator.YF)) >= Operator.OP_LOW) {
        return ((((x && p >= prio) || (!x && p > prio)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), op, p) + " " + name + " " + (((x && p >= prio) || (!x && p > prio)) ? ")"
            : ""));
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
    if (this.prolog != null) {
      return toStringAsArgY(theTerm, this.prolog.getOperatorManager(), Operator.OP_HIGH);
    }
    theTerm.accept(this);
    return this.formatted();
  }

}
