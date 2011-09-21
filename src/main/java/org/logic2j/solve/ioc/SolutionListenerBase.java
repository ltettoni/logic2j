package org.logic2j.solve.ioc;

/**
 * A base implementation of {@link SolutionListener} holds a counter of the number of solutions reached,
 * and whose {@link #onSolution()} returns always true (potentially infinite generation).
 * 
 */
public class SolutionListenerBase implements SolutionListener {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionListenerBase.class);
  private static boolean debug = logger.isDebugEnabled();

  private long startTime = System.currentTimeMillis();
  private long lastSolutionTime = this.startTime;
  private int counter = 0;

  @Override
  public boolean onSolution() {
    if (debug) {
      logger.debug(" onSolution(), iter=#{}", this.counter);
    }
    this.counter++;
    this.lastSolutionTime = System.currentTimeMillis();
    return true;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public int getCounter() {
    return this.counter;
  }

  public long elapse() {
    return this.lastSolutionTime - this.startTime;
  }

}
