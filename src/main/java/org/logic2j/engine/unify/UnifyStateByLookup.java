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
package org.logic2j.engine.unify;

import org.logic2j.engine.model.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.TreeMap;


/**
 * This is the central implementation to process and provide UnifyContext monads.
 */
public class UnifyStateByLookup {
  private static final Logger logger = LoggerFactory.getLogger(UnifyStateByLookup.class);
  private static final int INITIAL_SIZE = 500;

  private int[] transaction;
  private Var[] var;
  private Object[] literal;
  private int[] boundVarIndex;
  private int[] logOfWrittenSlots;  // Indexed by transaction number
  private int logWatermark;

  public UnifyStateByLookup() {
    transaction = new int[INITIAL_SIZE];
    Arrays.fill(transaction, -1);
    var = new Var[INITIAL_SIZE];
    literal = new Object[INITIAL_SIZE];
    boundVarIndex = new int[INITIAL_SIZE];
    logOfWrittenSlots = new int[INITIAL_SIZE];
    logWatermark = 0;
  }

  /**
   * Increase the size of all the arrays and copy existing data
   * plus initialize extra space according to needs.
   */
  private void resizeArrays() {
    // Current and new sizing algorithm
    final int initialLength = transaction.length;
    final int newLength = initialLength * 2;
    logger.info("Resizing UnifyState arrays to {} elements", newLength);
    // Increase size and copy
    transaction = Arrays.copyOf(transaction, newLength);
    Arrays.fill(transaction, initialLength, newLength, -1); // Need some init (only the extra part)
    var = Arrays.copyOf(var, newLength);
    literal = Arrays.copyOf(literal, newLength);
    boundVarIndex = Arrays.copyOf(boundVarIndex, newLength);
    logOfWrittenSlots = Arrays.copyOf(logOfWrittenSlots, newLength);
  }

  public UnifyContext emptyContext() {
    return new UnifyContext(this);
  }

  /**
   * Binding theVar to theRef; theVar will further appear "modified" in
   * the resulting UnifyContext; theRef is not altered.
   *
   * @param currentVars
   * @param theVar
   * @param theRef
   * @return
   */
  public UnifyContext bind(UnifyContext currentVars, Var theVar, Object theRef) {
    logger.debug(" bind {}->{}", theVar, theRef);
    final int transactionNumber = currentVars.currentTransaction;
    if (theVar == Var.anon()) {
      // assert theRef != Var.ANONYMOUS_VAR: "must not bind an anonymous var to another anonymous var";
      final Object finalRef = (theRef instanceof Var) ? dereference((Var) theRef, transactionNumber) : theRef;
      if (finalRef == Var.anon()) {
        return currentVars; // Nothing done
      } else if (finalRef instanceof Var) {
        return bind(currentVars, (Var) theRef, theVar);
      } else {
        return currentVars;
      }
    }
    cleanupTo(transactionNumber);
    final int slot = theVar.getIndex();
    // Handle array sizing overflow
    while (slot >= transaction.length) {
      resizeArrays();
    }
    transaction[slot] = transactionNumber;
    var[slot] = theVar;

    final Object finalRef = (theRef instanceof Var) ? dereference((Var) theRef, transactionNumber) : theRef;
    if (finalRef instanceof Var && finalRef != Var.anon()) {
      if (finalRef == theVar) {
        // OOps, trying to bound Var to same Var (after its the ref was dereferenced)
        return currentVars; // So no change
      }
      final Var finalVar = (Var) finalRef;
      final int finalVarIndex = finalVar.getIndex();
      var[finalVarIndex] = finalVar;
      literal[slot] = null; // Not a literal!
      boundVarIndex[slot] = finalVarIndex;
    } else {
      literal[slot] = finalRef;
    }
    logOfWrittenSlots[logWatermark++] = slot;
    //        if (slot > ProfilingInfo.max1) {
    //            ProfilingInfo.max1 = slot;
    //        }
    return new UnifyContext(this, transactionNumber + 1, currentVars.topVarIndex);
  }



  Object dereference(Var theVar, int transactionNumber) {
    if (theVar == Var.anon()) {
      return theVar;
    }
    Var runningVar = theVar;

    int slot = runningVar.getIndex();
    int limiter = Integer.MAX_VALUE;
    while (--limiter >= 0) {
      // Handle array sizing overflow
      while (slot >= transaction.length) {
        resizeArrays();
      }
      final int slotTrans = transaction[slot];
      if (slotTrans == -1 || slotTrans >= transactionNumber) {
        // Not bound or bound in the future
        return runningVar;
      }
      final Object slotLiteral = literal[slot];
      if (slotLiteral != null) {
        return slotLiteral;
      }
      slot = boundVarIndex[slot];
      runningVar = var[slot];
    }
    throw new IllegalStateException("Infinite loop detected during dereferencing of variable \"" + theVar + '"');
  }


  private void cleanupTo(int transactionNumber) {
    if (logWatermark > transactionNumber) {
      // UnifyContext.audit.info("Cleanup  {} up to {}", logWatermark, transactionNumber);
      while (logWatermark > transactionNumber) {
        int slotToCleanup = logOfWrittenSlots[--logWatermark];
        transaction[slotToCleanup] = -1;
      }
      // UnifyContext.audit.info(" after, watermark={}", logWatermark);
    }
  }

  public String toString() {
    final TreeMap<Integer, String> sorted = new TreeMap<Integer, String>();
    final StringBuilder sb = new StringBuilder();
    for (int slot = 0; slot < INITIAL_SIZE; slot++) {
      final int transactionNumber = transaction[slot];
      if (transactionNumber == -1) {
        continue;
      }
      sb.append(transactionNumber);
      sb.append(':');
      sb.append(var[slot]);
      if (slot != var[slot].getIndex()) {
        sb.append("??");
      }
      sb.append("->");
      if (literal[slot] != null) {
        sb.append(literal[slot]);
      } else {
        int boundVarSlot = boundVarIndex[slot];
        sb.append(var[boundVarSlot]);
        if (boundVarSlot != var[boundVarSlot].getIndex()) {
          sb.append("??");
        }
      }
      sorted.put(transactionNumber, sb.toString());
      sb.setLength(0);
    }
    return sorted.values().toString();
  }

}
