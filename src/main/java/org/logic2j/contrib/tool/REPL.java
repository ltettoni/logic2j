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
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.impl.PrologImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Read, Eval, Print, Loop.
 */
public class REPL {

    public static void main(String args[]) throws IOException {
        new REPL().run(args);
    }


    public void run(String args[]) throws IOException {
        System.out.println("logic2j REPL");
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


    protected void runOne(String goal) {
        try {
            // Parse and extract vars
            final Object parsed = getProlog().getTermUnmarshaller().unmarshall(goal);
            final Var<?>[] vars = TermApi.distinctVars(parsed);
            // Solve
            final CountingSolutionListener listener = new CountingSolutionListener();
            final long startTime = System.currentTimeMillis();
            getProlog().getSolver().solveGoal(parsed, listener);
            final long endTime = System.currentTimeMillis();
            // Report
            final long count = listener.getCounter();
            System.out.println("Solutions: " + count);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e);
            e.printStackTrace();
        }
    }

    public PrologImplementation getProlog() {
        final PrologImplementation prolog = new FluentPrologBuilder().createInstance();
        return prolog;
    }
}
