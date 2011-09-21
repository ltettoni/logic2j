package org.logic2j;

import java.util.List;

import org.logic2j.io.operator.OperatorManager;
import org.logic2j.solve.GoalSolver;
import org.logic2j.unify.Unifyer;

/**
 * An interface that Prolog implementations must provide; this goes 
 * beyond the lighter application interface ({@link Prolog}), by exposing accessors
 * to the internal state of the effective implementation.
 *
 */
public interface PrologImplementor extends Prolog {

  public abstract Unifyer getUnifyer();

  public abstract GoalSolver getSolver();

  public abstract List<ClauseProvider> getClauseProviders();

  public abstract OperatorManager getOperatorManager();

  public abstract LibraryManager getLibraryManager();

  public abstract Formatter getFormatter();

}
