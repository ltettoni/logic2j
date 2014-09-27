package org.logic2j.contrib.tool;/*
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

import org.logic2j.contrib.helper.FluentPrologBuilder;
import org.logic2j.core.api.TermMarshaller;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Read, Eval, Print, Loop.
 */
public class REPL {

    private PrologImplementation prolog;

    public void run(String args[]) throws IOException {
        System.out.println("logic2j REPL");

        final List<File> theories = new ArrayList<File>();
        for (String arg : args) {
            theories.add(new File(arg));
        }

        prolog = new FluentPrologBuilder().withTheory(theories.toArray(new File[]{})).createInstance();

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
            final Var<?>[] vars = TermApi.distinctVars(goal);
            // Solve
            final SolutionListener listener = new CountingSolutionListener() {
                @Override
                public Integer onSolution(UnifyContext currentVars) {
                    super.onSolution(currentVars);
                    final Object solution = currentVars.reify(goal);
                    System.out.println("Solution " + getCounter() + ": " + termMarshaller.marshall(solution));
                    for (Var v : vars) {
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

    public static void main(String args[]) throws IOException {
        new REPL().run(args);
    }
}
