/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.logic2j.core.impl.io.operator;

import java.io.Serializable;

/**
 * This class defines a tuProlog operator, in terms of a name, a type, and a priority.
 */
final public class Operator implements Serializable {
    private static final long serialVersionUID = 1L;

    // TODO Probably this should become an enum
    public static final String FX = "fx"; // prefix non-associative - (i.e. --5 not possible)
    public static final String FY = "fy"; // prefix associative
    public static final String XF = "xf"; // postfix non-associative
    public static final String XFX = "xfx"; // infix non-associative =, is, < (i.e. no nesting)
    public static final String XFY = "xfy"; // infix right-associative , (for subgoals)
    public static final String YF = "yf"; // postfix associative
    public static final String YFX = "yfx"; // infix left-associative +, -, *
    public static final String YFY = "yfy"; // makes no sense, structuring would be impossible

    /** highest operator priority */
    public static final int OP_HIGH = 1200;
    /** lowest operator priority */
    public static final int OP_LOW = 1;

    /** operator name */
    String name;

    /** priority */
    int prio;

    /** xf, yf, fx, fy, xfx, xfy, yfx, (yfy) */
    String type;

    public Operator(String theName, String theType, int thePrio) {
        this.name = theName;
        this.type = theType;
        this.prio = thePrio;
    }
}
