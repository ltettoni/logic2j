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

package org.logic2j.contrib.tool;

import static org.logic2j.engine.model.TermApiLocator.termApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.logic2j.contrib.helper.FluentPrologBuilder;
import org.logic2j.core.api.TermMarshaller;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.CountingSolutionListener;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;

/**
 * Read, Eval, Print, Loop.
 */
public class REPL {

  private PrologImplementation prolog;

  public void run(String[] args) throws IOException {
    System.out.println("logic2j REPL");

    final List<File> theories = new ArrayList<>();
    for (String arg : args) {
      theories.add(new File(arg));
    }

    prolog = new FluentPrologBuilder().withTheory(theories.toArray(new File[0])).build();

    final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      System.out.print("?- ");
      String goal = br.readLine();
      goal = goal.trim();
      // Removing trailing period
      if (goal.lastIndexOf('.') >= 0) {
        goal = goal.substring(0, goal.lastIndexOf('.'));
      }
      if ("quit".equals(goal) || "exit".equals(goal) || "bye".equals(goal)) {
        System.out.println("Bye");
        return;
      }
      runOne(goal);
    }
  }


  protected void runOne(String goalText) {
    try {
      // Parse and extract vars
      final TermMarshaller termMarshaller = prolog.getTermMarshaller();
      final Object goal = this.prolog.getTermUnmarshaller().unmarshall(goalText);
      final Var<?>[] vars = termApi().distinctVars(goal);
      // Solve
      final SolutionListener listener = new CountingSolutionListener() {
        @Override
        public int onSolution(UnifyContext currentVars) {
          super.onSolution(currentVars);
          final Object solution = currentVars.reify(goal);
          System.out.println("Solution " + count() + ": " + termMarshaller.marshall(solution));
          for (Var<?> v : vars) {
            final Object varValue = currentVars.reify(v);
            System.out.println(" " + v + "=" + termMarshaller.marshall(varValue));
          }
          return Continuation.CONTINUE;
        }
      };
      final long startTime = System.currentTimeMillis();
      this.prolog.getSolver().solveGoal(goal, listener);
      final long endTime = System.currentTimeMillis();
      // Report
      // System.out.println("Solutions: " + count);
    } catch (Exception e) {
      System.out.println("Exception caught: " + e);
      // e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    new REPL().run(args);
  }
}
