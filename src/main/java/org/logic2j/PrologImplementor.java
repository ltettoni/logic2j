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

  ClauseProviderResolver getClauseProviderResolver();
    
  public abstract Unifyer getUnifyer();

  public abstract GoalSolver getSolver();

  /**
   * @return All clause providers, in same order as when registered.
   * TODO But the actual ordering may not be always needed, it's only important when the same
   * predicate is available from several providers (not frequent). Could we in certain cases
   * use multi-threaded access to all clause providers?
   */
  public abstract List<ClauseProvider> getClauseProviders();

  public abstract OperatorManager getOperatorManager();

  public abstract LibraryManager getLibraryManager();

  public abstract Formatter getFormatter();

}
