package org.logic2j.core.api.monadic;

import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.impl.util.ProfilingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Laurent on 07.05.2014.
 */
public class StateEngineByStack {
    private static final Logger logger = LoggerFactory.getLogger(StateEngineByStack.class);
    private static final int REIFIER_STACK_CHUNK = 1000;

    Var[] vars;
    Object[] refs;

    public StateEngineByStack() {
        vars = new Var[REIFIER_STACK_CHUNK];
        refs = new Object[REIFIER_STACK_CHUNK];
    }

/*

  public Reifier emptyReifier() {
        return new Reifier(this);
    }

    public Reifier bind(Var theVar, Object theRef, int topIndex, int topVarIndex) {
        vars[topIndex] = theVar;
        final Object finalRef = (theRef instanceof Var) ? dereference((Var) theRef, topIndex) : theRef;
        refs[topIndex] = finalRef; // If theRef is a Var??? should we dereference it right now?
        logger.debug(" bind {}->{}", theVar, theRef);
        if (topIndex > ProfilingInfo.max1) {
            ProfilingInfo.max1 = topIndex;
        }
        return new Reifier(this, topIndex + 1, topVarIndex);
    }
*/





    Object dereference(Var theVar, int startTop) {
        if (theVar == Var.ANONYMOUS_VAR) {
            return theVar;
        }
        final int begin = startTop - 1;
        Object res = theVar;
        boolean found;
        do {
            found = false;
            for (int scan = begin; scan >= 0; scan--) {
                // TODO We could use a sentinel scan for efficiency
                if (res != vars[scan]) {
                    ProfilingInfo.counter1++;
                    continue;
                }
                if (scan < 0) {
                    // Not mapping found - return the original (free) var
                    return theVar;
                }
                found = true;
                ProfilingInfo.nbFollowVar++;
                res = refs[scan];
                break;
            }
        } while (found && res instanceof Var);  // the test instanceof is not much costly
        return res;
    }


    public String toString(int topIndex) {
        final StringBuilder sb = new StringBuilder();
        for (int scan = 0; scan < topIndex; scan++) {
            sb.append(scan);
            sb.append(':');
            sb.append(this.vars[scan]);
            sb.append("->");
            sb.append(this.refs[scan]);
            if (scan < topIndex-1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

}
