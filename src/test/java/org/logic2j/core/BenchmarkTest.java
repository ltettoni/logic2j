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
package org.logic2j.core;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.engine.solver.holder.GoalHolder;
import org.logic2j.engine.util.ProfilingInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Performance benchmarks.
 */
public class BenchmarkTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BenchmarkTest.class);

    /**
     * Still failing with stack overflow if more than hanoi(8)! unless stack expanded with -Xss10m, for example, instead of the ridiculous
     * 512k
     */
    @Test
    public void hanoi() {
        loadTheoryFromTestResourcesDir("hanoi.pro");
        uniqueSolution("move(7,left,right,center)"); // Watch out 7 is the limit with Java's ridiculous default stack size
    }

    @Test
    public void thousandLoops() {
        loadTheoryFromTestResourcesDir("test-data.pro");
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

    @Test
    public void profileThousandsLoops() {
        loadTheoryFromTestResourcesDir("test-data.pro");
        final long t1 = System.currentTimeMillis();
        nSolutions(10000, "int10(_),int10(_),int10(_),int10(_)");
        final long t2 = System.currentTimeMillis();
        logger.info("Elapse {}", t2 - t1);
    }

    /**
     * Takes lots of time and stack - use with parsimony and with -Xss10m. By default we will ignore this test.
     */
    @Test
    public void millionLoops() {
        loadTheoryFromTestResourcesDir("test-data.pro");
        // Using regular binary operator ","
        long t1 = System.currentTimeMillis();
        nSolutions(10000000, "int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_)");
        long t2 = System.currentTimeMillis();
        logger.info("1000000 iterations with binary AND, elapse {}", t2 - t1);
        // Using n-ary operator ","
        t1 = System.currentTimeMillis();
        nSolutions(10000000, "','(int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_))");
        t2 = System.currentTimeMillis();
        logger.info("1000000 iterations with N-ary AND, elapse {}", t2 - t1);
    }

    @Test
    public void queensForReferenceTiming() {
        loadTheoryFromTestResourcesDir("queens.pro");
        final String goal = "queens(9, Q)";
        // Numbers
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
//        long count = holder.var("Q").list().size();
        long count = holder.count();
//        long count = holder.exists() ? 1 : 0;
        ProfilingInfo.reportAll("Number of solutions to " + goal + " is " + count);
    }

    @Ignore("Use this in conjunction with jvisualvm to profile - this will typically never end unless user input")
    @Test
    public void queensForJVisualVMInteractive() throws IOException {
        loadTheoryFromTestResourcesDir("queens.pro");
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


    @Ignore("Use this in conjunction with jvisualvm to profile - sleeps for ages")
    @Test
    public void queensForJVisualVMSleeping() throws InterruptedException {
        loadTheoryFromTestResourcesDir("queens.pro");
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        Thread.sleep(15000);
        final String goal = "queens(8, X)";
        getProlog().solve(goal).count();
        ProfilingInfo.reportAll("VisualVM profiling");
        Thread.sleep(1000000);
    }


}
