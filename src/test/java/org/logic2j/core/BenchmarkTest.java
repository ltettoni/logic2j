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

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.util.ProfilingInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * Performance benchmarks.
 */
public class BenchmarkTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BenchmarkTest.class);

    @Test
    public void placeholderToReproduceError() {
        //
    }

    /**
     * Still failing with stack overflow if more than hanoi(8)! unless stack expanded with -Xss10m, for example, instead of the ridiculous
     * 512k
     */
    @Test
    public void hanoi() {
        loadTheoryFromTestResourcesDir("hanoi.pl");
        uniqueSolution("move(7,left,right,center)"); // Watch out 7 is the limit with Java's ridiculous default stack size
    }

    @Test
    public void thousandLoops() {
        loadTheoryFromTestResourcesDir("test-data.pl");
        // Using regular binary operator ","
        long t1 = System.currentTimeMillis();
        nSolutions(1000, "int10(_),int10(_),int10(_)");
        long t2 = System.currentTimeMillis();
        logger.info("1000 iterations, elapse {}", t2 - t1);
        // Using ternary operator ","
        t1 = System.currentTimeMillis();
        nSolutions(10000000, "','(int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_))");
        t2 = System.currentTimeMillis();
        logger.info("1000 iterations, elapse {}", t2 - t1);
    }

    /**
     * Takes lots of time and stack - use with parsimony and with -Xss10m. By default we will ignore this test.
     */
    @Ignore("Takes a lot of time and resources - temporarily disabled. Use with -Xss10m")
    @Test
    public void millionLoops() {
        loadTheoryFromTestResourcesDir("test-data.pl");
        // Using regular binary operator ","
        long t1 = System.currentTimeMillis();
        nSolutions(10000000, "int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_)");
        long t2 = System.currentTimeMillis();
        logger.info("1000000 iterations, elapse {}", t2 - t1);
        // Using n-ary operator ","
        t1 = System.currentTimeMillis();
        nSolutions(10000000, "','(int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_))");
        t2 = System.currentTimeMillis();
        logger.info("1000000 iterations, elapse {}", t2 - t1);
    }

    @Test
    public void profileMillionLoops() {
        loadTheoryFromTestResourcesDir("test-data.pl");
        final long t1 = System.currentTimeMillis();
        nSolutions(10000, "int10(_),int10(_),int10(_),int10(_)");
        final long t2 = System.currentTimeMillis();
        logger.info("Elapse {}", t2 - t1);
    }

    @Test
    public void queensNumbers() throws IOException {
        loadTheoryFromTestResourcesDir("queens.pl");

        assertEquals(1, getProlog().solve("queens(1, _)").count());
        assertEquals(0, getProlog().solve("queens(2, _)").count());
        assertEquals(0, getProlog().solve("queens(3, _)").count());
        assertEquals(2, getProlog().solve("queens(4, _)").count());
        assertEquals(10, getProlog().solve("queens(5, _)").count());
        assertEquals(4, getProlog().solve("queens(6, _)").count());
        assertEquals(40, getProlog().solve("queens(7, _)").count());
        assertEquals(92, getProlog().solve("queens(8, _)").count());
        assertEquals(352, getProlog().solve("queens(9, _)").count());
        assertEquals(724, getProlog().solve("queens(10, _)").count());
        // Comment out heavy ones
        // assertEquals(2680, getProlog().solve("queens(11, _)").count());
        // assertEquals(14200, getProlog().solve("queens(12, _)").count());
    }

    @Test
    public void queensForTiming() throws IOException {
        loadTheoryFromTestResourcesDir("queens.pl");
        final String goal = "queens(9, Q)";
        // Numbers
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
//        long count = holder.var("Q").list().size();
        long count = holder.count();
//        long count = holder.exists() ? 1 : 0;
        ProfilingInfo.reportAll("Number of solutions to " + goal + " is " + count);

        /*
        // TermBindings
        ProfilingInfo.setTimer1();
        getProlog().solve(goal).all().bindings();
        ProfilingInfo.reportAll("TermBindings of solutions to " + goal);
        */
    }

    @Ignore("Use this in conjunction with jvisualvm to profile")
    @Test
    public void queensForJVisualVMInteractive() throws IOException {
        loadTheoryFromTestResourcesDir("queens.pl");
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            final String goal = "queens(7, X)";
            System.out.print("Press any key to run, q to quit");
            final String readLine = br.readLine();
            if (readLine != null && readLine.startsWith("q")) {
                break;
            }
            final long startTime = System.currentTimeMillis();
            getProlog().solve(goal).count();
            logger.info("Timing for {}: {}", goal, (System.currentTimeMillis() - startTime));
        }
    }


    @Ignore("Use this in conjunction with jvisualvm to profile")
    @Test
    public void queensForJVisualVMSleeping() throws IOException, InterruptedException {
        loadTheoryFromTestResourcesDir("queens.pl");
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        Thread.sleep(15000);
        final String goal = "queens(8, X)";
        getProlog().solve(goal).count();
        ProfilingInfo.reportAll("VisualVM profiling");
        Thread.sleep(1000000);
    }


}
