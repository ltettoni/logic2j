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
package org.logic2j.core.impl;

import org.logic2j.core.api.model.Operator;
import org.logic2j.core.api.model.symbol.Struct;

/**
 * This class defines an operator manager with some standard operators defined
 */
public class DefaultOperatorManager extends OperatorManagerBase {
    private static final long serialVersionUID = 1L;

    public DefaultOperatorManager() {
        addOperator(Struct.FUNCTOR_CLAUSE, Operator.XFX, 1200);
        addOperator("-->", Operator.XFX, 1200);
        addOperator(Struct.FUNCTOR_CLAUSE, Operator.FX, 1200); // Actually, query not clause // Any reason for this order - is registered in
                                                               // a LinkedMap...
        addOperator("?-", Operator.FX, 1200);
        addOperator(";", Operator.XFY, 1100); // OR
        addOperator("->", Operator.XFY, 1050);
        addOperator(Struct.FUNCTOR_COMMA, Operator.XFY, 1000); // AND
        // addOperator(Struct.FUNCTOR_COMMA, Operator.YFY, 1000); // To implement direct optimization of ','/n but this is quite
        // prototypical
        addOperator("\\+", Operator.FY, 900); // Surprisingly enough the operator \+ means "not provable".
        addOperator("not", Operator.FY, 900);
        addOperator("=", Operator.XFX, 700);
        addOperator("\\=", Operator.XFX, 700);
        addOperator("==", Operator.XFX, 700);
        addOperator("\\==", Operator.XFX, 700);
        // addOperator("@==", Operator.XFX,700);
        // addOperator("@\\==", Operator.XFX,700);
        addOperator("@>", Operator.XFX, 700);
        addOperator("@<", Operator.XFX, 700);
        addOperator("@=<", Operator.XFX, 700);
        addOperator("@>=", Operator.XFX, 700);
        addOperator("=:=", Operator.XFX, 700);
        addOperator("=\\=", Operator.XFX, 700);
        addOperator(">", Operator.XFX, 700);
        addOperator("<", Operator.XFX, 700);
        addOperator("=<", Operator.XFX, 700);
        addOperator(">=", Operator.XFX, 700);
        addOperator("is", Operator.XFX, 700);
        addOperator("=..", Operator.XFX, 700);
        // opNew("?", Operator.XFX,600);
        // opNew("@", Operator.XFX,550);
        addOperator("+", Operator.YFX, 500);
        addOperator("-", Operator.YFX, 500);
        addOperator("/\\", Operator.YFX, 500);
        addOperator("\\/", Operator.YFX, 500);
        addOperator("*", Operator.YFX, 400);
        addOperator("/", Operator.YFX, 400);
        addOperator("//", Operator.YFX, 400);
        addOperator(">>", Operator.YFX, 400);
        addOperator("<<", Operator.YFX, 400);
        addOperator("rem", Operator.YFX, 400);
        addOperator("mod", Operator.YFX, 400);
        addOperator("**", Operator.XFX, 200);
        addOperator("^", Operator.XFY, 200);
        addOperator("\\", Operator.FX, 200);
        addOperator("-", Operator.FY, 200);
    }

}
