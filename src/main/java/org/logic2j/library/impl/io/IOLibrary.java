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
package org.logic2j.library.impl.io;

import java.io.PrintStream;

import org.logic2j.PrologImplementor;
import org.logic2j.io.format.FormatUtils;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;

/**
 */
public class IOLibrary extends LibraryBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IOLibrary.class);

  PrintStream writer = System.out;

  public IOLibrary(PrologImplementor theProlog) {
    super(theProlog);
  }

  @Primitive
  public void write(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term... terms) {
    for (Term term : terms) {
      final Term value = resolve(term, theBindings, Term.class);
      String format = getProlog().getFormatter().format(value);
      format = FormatUtils.removeApices(format);
      this.writer.print(format);
    }
    notifySolution(theGoalFrame, theListener);
  }

  @Primitive
  public void nl(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings) {
    this.writer.print('\n');
    notifySolution(theGoalFrame, theListener);
  }
  
  
  @Primitive
  public void log(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term... terms) {
    for (Term term : terms) {
      final Term value = resolve(term, theBindings, Term.class);
      String format = getProlog().getFormatter().format(value);
      format = FormatUtils.removeApices(format);
      logger.info(format);
    }
    notifySolution(theGoalFrame, theListener);
  }
  
  @Primitive
  public void nolog(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term... terms) {
    // Do nothing, but succeeds!
    notifySolution(theGoalFrame, theListener);
  }


}
