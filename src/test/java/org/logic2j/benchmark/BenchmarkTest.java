package org.logic2j.benchmark;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.solve.DefaultGoalSolver;

/**
 * Benchmarking the Prolog engine (unification, inference engine).
 *
 */
public class BenchmarkTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BenchmarkTest.class);

  @Test
  public void testHanoi() throws IOException {
    addTheory("test/input/hanoi.pl");
    // TODO: Still failing with stack overflow if more than hanoi(8)! unless stack expanded with -Xss10m, for example, instead of the ridiculous 500k
    assertOneSolution("move(7,left,right,center)");
    logger.info("Number of solutions processed: {}", ((DefaultGoalSolver) getProlog().getSolver()).internalCounter);
  }

  @Ignore
  @Test
  public void testMillionLoops() throws IOException {
    addTheory("test/input/test-data.pl");
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
    addTheory("test/input/test-data.pl");
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
