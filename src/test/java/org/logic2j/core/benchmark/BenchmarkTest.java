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
package org.logic2j.core.benchmark;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.solver.DefaultSolver;

/**
 * Benchmarking the Prolog engine (unification, inference engine).
 * 
 */
public class BenchmarkTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BenchmarkTest.class);

    /**
     * Still failing with stack overflow if more than hanoi(8)! unless stack expanded with -Xss10m, for example, instead of the ridiculous
     * 512k
     * 
     * @throws IOException
     */
    @Test
    public void testHanoi() throws IOException {
        addTheory("src/test/resources/hanoi.pl");
        assertOneSolution("move(7,left,right,center)"); // Watch out 7 is the limit with Java's ridiculous default stack size
        logger.info("Number of solutions processed: {}", ((DefaultSolver) getProlog().getSolver()).internalCounter);
    }

    /**
     * Takes lots of time and stack - use with parcimony and with -Xss10m
     * 
     * @throws IOException
     */
    @Ignore
    // See note above
    @Test
    public void testMillionLoops() throws IOException {
        addTheory("src/test/resources/test-data.pl");
        long t1 = System.currentTimeMillis();
        assertNSolutions(10000000, "int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_)");
        long t2 = System.currentTimeMillis();
        logger.info("Elapse {}", t2 - t1);

        t1 = System.currentTimeMillis();
        assertNSolutions(10000000, "','(int10(_),int10(_),int10(_),int10(_),int10(_),int10(_),int10(_))");
        t2 = System.currentTimeMillis();
        logger.info("Elapse {}", t2 - t1);
    }

    @Test
    public void testProfileMillionLoops() throws IOException {
        addTheory("src/test/resources/test-data.pl");
        long t1 = System.currentTimeMillis();
        assertNSolutions(10000, "int10(_),int10(_),int10(_),int10(_)");
        long t2 = System.currentTimeMillis();
        logger.info("Elapse {}", t2 - t1);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        BenchmarkTest benchmarkTest = new BenchmarkTest();
        benchmarkTest.setUp();
        Thread.sleep(20000);
        benchmarkTest.testMillionLoops();
    }

}
