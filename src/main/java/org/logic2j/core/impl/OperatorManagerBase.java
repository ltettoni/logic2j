/*
 * tuProlog - Copyright (C) 2001-2006  aliCE team at deis.unibo.it
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.logic2j.core.impl;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import org.logic2j.core.api.OperatorManager;
import org.logic2j.core.api.model.Operator;
import org.logic2j.engine.exception.PrologNonSpecificException;

/**
 * Base implementation for OperatorManager
 */
abstract class OperatorManagerBase implements OperatorManager, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * currently known operators
   */
  private final OperatorRegister operatorList = new OperatorRegister();

  /**
   * Creates and register a new operator. If the operator is already provided, it replaces it with the new one
   *
   * @throws PrologNonSpecificException
   */
  @Override
  public void addOperator(String operatorText, String associativity, int precedence) {
    final Operator op = new Operator(operatorText, associativity, precedence);
    if (precedence >= Operator.OP_LOWEST && precedence <= Operator.OP_HIGHEST) {
      this.operatorList.addOperator(op);
    } else {
      throw new PrologNonSpecificException("Operator priority not in valid range for " + op);
    }
  }

  /**
   * @param operatorText  Text representation of the operator
   * @param associativity
   * @return the precedence (priority) of an operator (0 if the operator is not defined).
   */
  @Override
  public int precedence(String operatorText, String associativity) {
    final Operator operator = this.operatorList.findOperator(operatorText, associativity);
    return (operator == null) ? 0 : operator.getPrecedence();
  }

  /**
   * A register for operators; caches operator by name+type description.
   * Retains insertion order.
   * <p/>
   * Not 100% sure if 'insertion-order-priority' should be completely replaced by the explicit priority given to operators.
   *
   * @author ivar.orstavik@hist.no
   */
  static class OperatorRegister implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // map of operators by name and type
    // key is the nameType of an operator (for example ":-xfx") - value is an Operator
    private final HashMap<String, Operator> nameTypeToKey = new HashMap<>();
    private final LinkedHashSet<Operator> operators = new LinkedHashSet<>();

    public boolean addOperator(Operator op) {
      final String key = mapKey(op.getText(), op.getAssociativity());
      final Operator matchingOp = this.nameTypeToKey.get(key);
      if (matchingOp != null) {
        this.operators.remove(matchingOp); // removes found match from the main list
      }
      this.nameTypeToKey.put(key, op); // writes over found match in nameTypeToKey map
      return this.operators.add(op); // adds new operator to the main list
    }

    public Operator findOperator(String operatorText, String associativity) {
      final String key = mapKey(operatorText, associativity);
      return this.nameTypeToKey.get(key);
    }

    private String mapKey(String operatorText, String associativity) {
      return operatorText + associativity;
    }
  }

}
