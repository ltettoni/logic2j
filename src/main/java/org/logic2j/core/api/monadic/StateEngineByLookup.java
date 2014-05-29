package org.logic2j.core.api.monadic;

import org.logic2j.core.api.model.term.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Created by Laurent on 07.05.2014.
 */
public class StateEngineByLookup {
    private static final Logger logger = LoggerFactory.getLogger(StateEngineByLookup.class);
    private static final int INITIAL_SIZE = 500;

    private int[] transaction;
    private Var[] var;
    private Object[] literal;
    private int[] boundVarIndex;
    private int[] logOfWrittenSlots;  // Indexed by transaction number
    private int   logWatermark;

    public StateEngineByLookup() {
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
        logger.info("Resizing arrays to {}", newLength);
        // Increase size and copy
        transaction = Arrays.copyOf(transaction, newLength);
        Arrays.fill(transaction, initialLength, newLength, -1); // Need some init (only the extra part)
        var = Arrays.copyOf(var, newLength);
        literal = Arrays.copyOf(literal, newLength);
        boundVarIndex = Arrays.copyOf(boundVarIndex, newLength);
        logOfWrittenSlots = Arrays.copyOf(logOfWrittenSlots, newLength);
    }

    public PoV emptyPoV() {
        return new PoV(this);
    }

    public PoV bind(PoV pov, Var theVar, Object theRef) {
        logger.debug(" bind {}->{}", theVar, theRef);
        final int transactionNumber = pov.currentTransaction;
        cleanupTo(transactionNumber);
        final int slot = theVar.getIndex();
        // Handle array sizing overflow
        while (slot >= transaction.length) {
            resizeArrays();
        }
        transaction[slot] = transactionNumber;
        var[slot] = theVar;

        final Object finalRef = (theRef instanceof Var) ? dereference((Var) theRef, transactionNumber) : theRef;
        if (finalRef instanceof Var) {
            if (finalRef==theVar) {
                // OOps, trying to bound Var to same Var (after its the ref was dereferenced)
                return pov; // So no change
            }
            final Var finalVar = (Var) finalRef;
            final short finalVarIndex = finalVar.getIndex();
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
        return new PoV(this, transactionNumber + 1, pov.topVarIndex);
    }




    Object dereference(Var theVar, int transactionNumber) {
        if (theVar == Var.ANONYMOUS_VAR) {
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
            if (slotTrans==-1 || slotTrans >= transactionNumber) {
                // Not bound or bound in the future
                return runningVar;
            }
            final Object slotLiteral = literal[slot];
            if (slotLiteral != null) {
                return slotLiteral;
            }
            int slotVarIndex = boundVarIndex[slot];
            slot = slotVarIndex;
            runningVar = var[slot];
        }
        throw new IllegalStateException("Infinite loop detected during dereferencing of variable \"" + theVar + '"');
    }


    private void cleanupTo(int transactionNumber) {
        if (logWatermark > transactionNumber) {
            // Reifier.audit.info("Cleanup  {} up to {}", logWatermark, transactionNumber);
            while (logWatermark > transactionNumber) {
                int slotToCleanup = logOfWrittenSlots[--logWatermark];
                transaction[slotToCleanup] = -1;
            }
            // Reifier.audit.info(" after, watermark={}", logWatermark);
        }
    }

    public String toString() {
        final TreeMap<Integer, String> sorted = new TreeMap<Integer, String>();
        final StringBuilder sb = new StringBuilder();
        for (int slot=0; slot< INITIAL_SIZE; slot++) {
            final int transactionNumber = transaction[slot];
            if (transactionNumber ==-1) {
                continue;
            }
            sb.append(transactionNumber);
            sb.append(':');
            sb.append(var[slot]);
            if (slot != var[slot].getIndex()) {
                sb.append("??");
            }
            sb.append("->");
            if (literal[slot]!=null) {
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
