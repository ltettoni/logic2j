package org.logic2j.library.impl.io;

import java.io.PrintStream;

import org.logic2j.PrologImplementor;
import org.logic2j.io.format.FormatUtils;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
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
  public void write(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1) {
    final Term value = resolve(t1, vars, Term.class);
    String format = getProlog().getFormatter().format(value);
    format = FormatUtils.removeApices(format);
    this.writer.print(format);
    notifySolution(theGoalFrame, theListener);
  }

  @Primitive
  public void nl(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars) {
    this.writer.print('\n');
    notifySolution(theGoalFrame, theListener);
  }
}
