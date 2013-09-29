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

package org.logic2j.core;

import java.util.HashMap;

/**
 * Counters and data structures that can be used (temporarily) to instrument logic2j
 * to collect usage / profiling information
 */
public class ProfilingInfo {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProfilingInfo.class);
    public static long counter1;
    public static long timer1;

    public static HashMap<Object, Integer> events = new HashMap<Object, Integer>();

    static {
        resetAll();
    }

    public static void reportAll(String label) {
        final long now = System.currentTimeMillis();
        logger.info("Profile report for: {}", label);
        if (counter1 >= 0) {
            logger.info("  counter1={}", counter1);
        }
        if (timer1 >= 0) {
            logger.info("  timer1={}", now - timer1);
        }
        if (!events.isEmpty()) {
            logger.info("  events={}", events);
        }
        resetAll();
    }

    private static void resetAll() {
        counter1 = -1;
        timer1 = -1;
    }

    public static long setTimer1() {
        timer1 = System.currentTimeMillis();
        return timer1;
    }

    /**
     * @param theMethodName
     */
    public static int addEvent(Object theEventKey) {
        final Integer val = events.get(theEventKey);
        if (val == null) {
            events.put(theEventKey, 1);
            return 1;
        }
        events.put(theEventKey, val + 1);
        return val + 1;
    }

}
