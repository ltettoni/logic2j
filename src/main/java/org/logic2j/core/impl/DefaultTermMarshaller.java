/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.logic2j.core.impl;

import static org.logic2j.engine.model.TermApiLocator.termApi;

import org.logic2j.core.api.OperatorManager;
import org.logic2j.core.api.TermMarshaller;
import org.logic2j.core.api.model.Operator;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.engine.visitor.ExtendedTermVisitor;


/**
 * Default and reference implementation of {@link org.logic2j.core.api.TermMarshaller#marshall(Object)}.
 * This implementation may be derived or composed to your wish.
 */
public class DefaultTermMarshaller implements TermMarshaller, ExtendedTermVisitor<CharSequence> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTermMarshaller.class);

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
  public CharSequence visit(Var<?> theVar) {
    // logger.info("Visiting: {}", theVar);
    final Object finalValue;
    if (this.currentVars != null) {
      finalValue = this.currentVars.reify(theVar);
    } else {
      finalValue = theVar;
    }
    if (finalValue instanceof Var) {
      // Must be free
      if (theVar == Var.anon()) {
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
  public CharSequence visit(Struct<?> theStruct) {
    final CharSequence formatted = formatStruct(theStruct);
    return formatted;
  }

  @Override
  public CharSequence visit(String theAtomString) {
    return termApi().quoteIfNeeded(theAtomString);
  }

  @Override
  public CharSequence visit(Object theObject) {
    return String.valueOf(theObject);
  }

  // ---------------------------------------------------------------------------
  // Support methods
  // ---------------------------------------------------------------------------

  /**
   * Just a derivable shortcut to {@link TermApi#accept(ExtendedTermVisitor, Object)}.
   *
   * @param theTerm
   * @return The formatted term.
   */
  protected CharSequence accept(Object theTerm) {
    return termApi().accept(this, theTerm);
  }

  /**
   * Gets the string representation of this structure
   * <p/>
   * Specific representations are provided for lists and atoms. Names starting with upper case letter are enclosed in apices.
   * TODO Remove if similar if not equal to Struct.formatStruct
   */
  private CharSequence formatStruct(Struct<?> theStruct) {
    // empty list case
    if (PrologLists.isEmptyList(theStruct)) {
      return PrologLists.FUNCTOR_EMPTY_LIST;
    }
    final StringBuilder sb = new StringBuilder();
    final String name = theStruct.getName();
    final int arity = theStruct.getArity();
    // list case
    if (PrologLists.isListNode(theStruct)) {
      sb.append(PrologLists.LIST_OPEN);
      sb.append(formatPListRecursive(theStruct));
      sb.append(PrologLists.LIST_CLOSE);
      return sb;
    }
    sb.append(termApi().quoteIfNeeded(name));
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

  // TODO Remove if similar (if not equal) to Struct.formatPListRecursive
  private CharSequence formatPListRecursive(Struct<?> theStruct) {
    final Object head = theStruct.getLHS();
    final Object tail = theStruct.getRHS();
    if (PrologLists.isList(tail)) {
      final Struct<?> tailStruct = (Struct<?>) tail;
      if (PrologLists.isEmptyList(tailStruct)) {
        return accept(head);
      }
      // Why this special test?
      if (head instanceof Var) {
        return visit((Var<?>) head) + PrologLists.LIST_ELEM_SEPARATOR + formatPListRecursive(tailStruct);
      }
      return accept(head) + PrologLists.LIST_ELEM_SEPARATOR + formatPListRecursive(tailStruct);
    }
    final StringBuilder sb = new StringBuilder();
    // Head
    final CharSequence h0;
    if (head instanceof Var) {
      h0 = visit((Var<?>) head);
    } else {
      h0 = accept(head);
    }
    sb.append(h0);
    sb.append(PrologLists.HEAD_TAIL_SEPARATOR);
    // Tail
    final CharSequence t0;
    if (tail instanceof Var) {
      t0 = visit((Var<?>) tail);
    } else {
      t0 = accept(tail);
    }
    sb.append(t0);
    return sb;
  }

  /**
   * Gets the string representation of this term as an X argument of an operator, considering the associative property.
   */
  private CharSequence toStringAsArgX(Object theTerm, int precedence) {
    return toStringAsArg(theTerm, precedence, true);
  }

  /**
   * Gets the string representation of this term as an Y argument of an operator, considering the associative property.
   */
  private CharSequence toStringAsArgY(Object theTerm, int precedence) {
    return toStringAsArg(theTerm, precedence, false);
  }

  private CharSequence toStringAsList(Struct<?> theStruct) {
    final Object h = theStruct.getLHS();
    final Object t = theStruct.getRHS();
    if (PrologLists.isList(t)) {
      final Struct<?> tl = (Struct<?>) t;
      if (PrologLists.isEmptyList(tl)) {
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
    sb.append(PrologLists.HEAD_TAIL_SEPARATOR);
    sb.append(toStringAsArgY(t, 0));
    return sb;
  }

  private CharSequence toStringAsArg(Object theTerm, int precedence, boolean x) {
    if (theTerm instanceof CharSequence) {
      return termApi().quoteIfNeeded((CharSequence) theTerm);
    }
    if (!(theTerm instanceof Struct)) {
      return accept(theTerm);
    }
    final Struct<?> theStruct = (Struct<?>) theTerm;
    int p;
    final String name = theStruct.getName();
    final int arity = theStruct.getArity();
    if (PrologLists.isEmptyList(theStruct)) {
      return PrologLists.FUNCTOR_EMPTY_LIST;
    }
    if (PrologLists.isListNode(theStruct)) {
      final StringBuilder sb = new StringBuilder();
      sb.append(PrologLists.LIST_OPEN);
      sb.append(toStringAsList(theStruct));
      sb.append(PrologLists.LIST_CLOSE);
      return sb;
    }

    if (arity == 2) {
      if ((p = operatorManager.precedence(name, Operator.XFX)) >= Operator.OP_LOWEST) {
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " "
                + toStringAsArgX(theStruct.getRHS(), p) + (((x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
      if ((p = operatorManager.precedence(name, Operator.YFX)) >= Operator.OP_LOWEST) {
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + toStringAsArgY(theStruct.getLHS(), p) + " " + name + " "
                + toStringAsArgX(theStruct.getRHS(), p) + (((x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
      if ((p = operatorManager.precedence(name, Operator.XFY)) >= Operator.OP_LOWEST) {
        if (!name.equals(Struct.ARG_SEPARATOR)) {
          return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + " " + name + " "
                  + toStringAsArgY(theStruct.getRHS(), p) + (((x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
        }
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + toStringAsArgX(theStruct.getLHS(), p) + Struct.ARG_SEPARATOR
                + toStringAsArgY(theStruct.getRHS(), p) + (((x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
    } else if (arity == 1) {
      if ((p = operatorManager.precedence(name, Operator.FX)) >= Operator.OP_LOWEST) {
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + name + " " + toStringAsArgX(theStruct.getArg(0), p) + ((
                (x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
      if ((p = operatorManager.precedence(name, Operator.FY)) >= Operator.OP_LOWEST) {
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + name + " " + toStringAsArgY(theStruct.getArg(0), p) + ((
                (x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
      if ((p = operatorManager.precedence(name, Operator.XF)) >= Operator.OP_LOWEST) {
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + toStringAsArgX(theStruct.getArg(0), p) + " " + name + " " + ((
                (x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
      if ((p = operatorManager.precedence(name, Operator.YF)) >= Operator.OP_LOWEST) {
        return ((((x && p >= precedence) || (!x && p > precedence)) ? "(" : "") + toStringAsArgY(theStruct.getArg(0), p) + " " + name + " " + ((
                (x && p >= precedence) || (!x && p > precedence)) ? ")" : ""));
      }
    }
    final StringBuilder sb = new StringBuilder(termApi().quoteIfNeeded(name));
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

}
