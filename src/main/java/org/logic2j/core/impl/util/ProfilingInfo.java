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

package org.logic2j.core.impl.util;

import java.util.HashMap;

/**
 * Counters and data structures that can be used (temporarily) to instrument logic2j
 * to collect usage / profiling information
 */
public class ProfilingInfo {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProfilingInfo.class);
    public static long counter1;
    public static int max1;
    public static long nbInferences;
    public static long nbBindings;
    public static int threadLocal;
    public static int nbFollowVar;

    public static long timer1;

    public static final HashMap<Object, Integer> events = new HashMap<Object, Integer>();

    static {
        resetAll();
    }



    public static void reportAll(String label) {
        final long now = System.currentTimeMillis();
        logger.info("Profile report for: {}", label);
        if (counter1 > 0) {
            logger.info("  counter1     = {}", counter1);
        }
        if (max1 > 0) {
            logger.info("  max1         = {}", max1);
        }
        if (nbInferences > 0) {
            logger.info("  nbInferences = {}", nbInferences);
        }
        if (nbFollowVar > 0) {
            logger.info("  nbFollowVar  = {}", nbFollowVar);
        }
        if (nbBindings > 0) {
            logger.info("  nbBindings   = {}", nbBindings);
        }
        if (threadLocal > 0) {
            logger.info("  threadLocal  = {}", threadLocal);
        }
        if (timer1 >= 0) {
            logger.info("  timer1       = {}", now - timer1);
        }
        if (!events.isEmpty()) {
            logger.info("  events       = {}", events);
        }
        resetAll();
    }

    private static void resetAll() {
        counter1 = 0;
        max1 = 0;
        nbBindings = 0;
        nbInferences = 0;
        threadLocal = 0;
        nbFollowVar = 0;
        timer1 = -1;
    }

    public static long setTimer1() {
        timer1 = System.currentTimeMillis();
        return timer1;
    }

    public static int countEvent(Object theEventKey) {
        final Integer val = events.get(theEventKey);
        if (val == null) {
            events.put(theEventKey, 1);
            return 1;
        }
        events.put(theEventKey, val + 1);
        return val + 1;
    }

}
