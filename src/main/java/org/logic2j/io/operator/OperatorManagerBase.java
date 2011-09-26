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
package org.logic2j.io.operator;

import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Base implementation.
 */
public class OperatorManagerBase implements java.io.Serializable, OperatorManager {
  private static final long serialVersionUID = 1L;

  /** current known operators */
  private OperatorRegister operatorList = new OperatorRegister();

  /**
   * Creates a new operator. If the operator is already provided,
   * it replaces it with the new one
   */
  @Override
  public void addOperator(String theName, String theAssociativityType, int thePriority) {
    final Operator op = new Operator(theName, theAssociativityType, thePriority);
    if (thePriority >= Operator.OP_LOW && thePriority <= Operator.OP_HIGH) {
      this.operatorList.addOperator(op);
    } else {
      throw new IllegalArgumentException("Operator priority not in valid range for " + op);
    }
  }

  /**
   * Returns the priority of an operator (0 if the operator is not defined).
   */
  @Override
  public int opPrio(String name, String type) {
    final Operator operator = this.operatorList.getOperator(name, type);
    return (operator == null) ? 0 : operator.prio;
  }

  /**
   * Register for operators; caches operator by name+type description. Retains insertion order.
   * <p/>
   * TODO Not 100% sure if 'insertion-order-priority' should be completely replaced
   * by the explicit priority given to operators.
   * 
   * @author ivar.orstavik@hist.no
   */
  static class OperatorRegister {
    //map of operators by name and type
    //key is the nameType of an operator (for example ":-xfx") - value is an Operator
    private HashMap<String, Operator> nameTypeToKey = new HashMap<String, Operator>();
    private LinkedHashSet<Operator> operators = new LinkedHashSet<Operator>();

    public boolean addOperator(Operator op) {
      final String nameTypeKey = op.name + op.type;
      Operator matchingOp = this.nameTypeToKey.get(nameTypeKey);
      if (matchingOp != null) {
        this.operators.remove(matchingOp); //removes found match from the main list
      }
      this.nameTypeToKey.put(nameTypeKey, op); //writes over found match in nameTypeToKey map
      return this.operators.add(op); //adds new operator to the main list
    }

    public Operator getOperator(String name, String type) {
      return this.nameTypeToKey.get(name + type);
    }
  }

}
