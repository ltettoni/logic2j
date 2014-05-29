package org.logic2j.core.api.monadic;

import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.impl.util.ProfilingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Created by Laurent on 07.05.2014.
 */
public class StateEngineByLookup {
    private static final Logger logger = LoggerFactory.getLogger(StateEngineByLookup.class);
    private static final int LOOKUP_CHUNK = 1000;


    int[] transaction;
    Var[] var;
    Object[] literal;
    int[] boundVarIndex;
    int[] logOfWrittenSlots;  // Indexed by transaction number
    int   logWatermark;

    public StateEngineByLookup() {
        transaction = new int[LOOKUP_CHUNK];
        Arrays.fill(transaction, -1);
        var = new Var[LOOKUP_CHUNK];
        literal = new Object[LOOKUP_CHUNK];
        boundVarIndex = new int[LOOKUP_CHUNK];
        logOfWrittenSlots = new int[LOOKUP_CHUNK];
        logWatermark = 0;
    }


    public PoV emptyPoV() {
        return new PoV(this);
    }


    public PoV bind(PoV pov, Var theVar, Object theRef) {
        logger.debug(" bind {}->{}", theVar, theRef);
        final int transactionNumber = pov.currentTransaction;
        cleanupTo(transactionNumber);
        final int slot = theVar.getIndex();
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
        for (int slot=0; slot< LOOKUP_CHUNK; slot++) {
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
