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
package org.logic2j.core.impl.io.parse.tuprolog;

import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.ATOM;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.DQ_SEQUENCE;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.END;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.FLOAT;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.INTEGER;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.LBRA;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.LBRA2;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.LPAR;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.RBRA;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.RBRA2;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.RPAR;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.SQ_SEQUENCE;
import static org.logic2j.core.impl.io.parse.tuprolog.MaskConstants.VARIABLE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.logic2j.core.api.model.OperatorManager;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.impl.io.operator.Operator;

/**
 * This class defines a parser of Prolog terms and sentences.
 * 
 * <pre>
 * term ::= exprA(1200)
 * exprA(n) ::= exprB(n) { op(yfx,n) exprA(n-1) |
 *                         op(yf,n) |
 *                        { op(yfy,n) exprA(n) }+  }*  << extension
 * exprB(n) ::= exprC(n-1) { op(xfx,n) exprA(n-1) |
 *                           op(xfy,n) exprA(n) |
 *                           op(xf,n) }*
 * exprC(n) ::= '-' integer |
 *              '-' float |
 *              op( fx,n ) exprA(n-1) |
 *              op( fy,n ) exprA(n) |
 *              exprA(n)
 * exprA(0) ::= integer |
 *              float |
 *              variable |
 *              ATOM_PATTERN |
 *              ATOM_PATTERN'(' exprA(1200) { ',' exprA(1200) }* ')' |
 *              '[' [ exprA(1200) { ',' exprA(1200) }* [ '|' exprA(1200) ] ] ']' |
 *              '(' { exprA(1200) }* ')'
 *              '{' { exprA(1200) }* '}'
 * 
 * op(type,n) ::= ATOM_PATTERN | { symbol }+
 * </pre>
 */
public class Parser {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Parser.class);

    private static final Pattern ATOM_PATTERN = Pattern.compile("(!|[a-z][a-zA-Z_0-9]*)");

    private static class IdentifiedTerm {
        int priority;
        Object result;

        public IdentifiedTerm(int thePriority, Object theResult) {
            this.priority = thePriority;
            this.result = theResult;
        }
    }

    private final Tokenizer tokenizer;
    private final OperatorManager opManager;

    public Parser(OperatorManager theOperatorManager, Reader theoryText) {
        this.tokenizer = new Tokenizer(new BufferedReader(theoryText));
        this.opManager = theOperatorManager;
    }

    public Parser(OperatorManager theOperatorManager, String theoryText) {
        this.tokenizer = new Tokenizer(theoryText);
        this.opManager = theOperatorManager;
    }

    /**
     * Parses next term from the stream built on string.
     * 
     * @param endNeeded <tt>true</tt> if it is required to parse the end token (a period), <tt>false</tt> otherwise.
     * @throws InvalidTermException if a syntax error is found.
     */
    // TODO I would prefer that the parser exposes an Iterable instead of a direct method
    public Object nextTerm(boolean endNeeded) throws InvalidTermException {
        try {
            final Token t = this.tokenizer.readToken();
            if (t.isEOF()) {
                return null;
            }

            this.tokenizer.unreadToken(t);
            final Object term = expr(false);
            if (term == null) {
                throw new InvalidTermException("The parser was unable to finish.");
            }

            if (endNeeded && this.tokenizer.readToken().getType() != END) {
                throw new InvalidTermException("The term " + term + " is not ended with a period.");
            }

            return term;
        } catch (final IOException ex) {
            throw new InvalidTermException("An I/O error occured.");
        }
    }

    public Object parseSingleTerm() throws InvalidTermException {
        try {
            final Token t = this.tokenizer.readToken();
            if (t.isEOF()) {
                throw new InvalidTermException("Term starts with EOF");
            }
            this.tokenizer.unreadToken(t);
            final Object term = expr(false);
            if (term == null) {
                throw new InvalidTermException("Term is null");
            }
            if (!this.tokenizer.readToken().isEOF()) {
                throw new InvalidTermException("The entire term could not be parsed, parsing failed before the end, stopped after \"" + term + '"');
            }
            return term;
        } catch (final IOException ex) {
            throw new InvalidTermException("An I/O error occured");
        }
    }

    // ---------------------------------------------------------------------------
    // internal parsing procedures
    // ---------------------------------------------------------------------------

    private Object expr(boolean commaIsEndMarker) throws InvalidTermException, IOException {
        return exprA(Operator.OP_HIGH, commaIsEndMarker).result;
    }

    private IdentifiedTerm exprA(int maxPriority, boolean commaIsEndMarker) throws InvalidTermException, IOException {
        IdentifiedTerm leftSide = exprB(maxPriority, commaIsEndMarker);

        // {op(yfx,n) exprA(n-1) | op(yf,n)}*
        Token oper = this.tokenizer.readToken();
        for (; oper.isOperator(commaIsEndMarker); oper = this.tokenizer.readToken()) {

            int yfx = this.opManager.opPrio(oper.text, Operator.YFX);
            if (yfx < leftSide.priority || yfx > maxPriority) {
                yfx = -1;
            }

            int yf = this.opManager.opPrio(oper.text, Operator.YF);
            if (yf < leftSide.priority || yf > maxPriority) {
                yf = -1;
            }

            int yfy = this.opManager.opPrio(oper.text, Operator.YFY);
            if (yfy < leftSide.priority || yfy > maxPriority) {
                yfy = -1;
            }

            // VERY VERY PROTOTYPICAL - SHOULD ACTUALLY NOT BE USED
            if (yfy >= yfx && yfy >= yf && yfy >= Operator.OP_LOW) {
                final List<Object> elements = new ArrayList<Object>();
                elements.add(leftSide.result);
                final String functor = oper.text;
                while (yfy >= yfx && yfy >= yf && yfy >= Operator.OP_LOW) {
                    final IdentifiedTerm tb = exprB(yfy, commaIsEndMarker);
                    elements.add(tb.result);
                    oper = this.tokenizer.readToken();
                    if (!oper.text.equals(functor)) {
                        // Changing operator - parsing must stop here
                        this.tokenizer.unreadToken(oper);
                        break;
                    }
                    yfy = this.opManager.opPrio(oper.text, Operator.YFY);
                    if (yfy < leftSide.priority || yfy > maxPriority) {
                        yfy = -1;
                    }
                }
                logger.info("Stop loop, found so far: {}", elements);
                return new IdentifiedTerm(yfy, new Struct(functor, elements.toArray(new Object[elements.size()])));
            }

            // YFX has priority over YF
            if (yfx >= yf && yfx >= Operator.OP_LOW) {
                final IdentifiedTerm ta = exprA(yfx - 1, commaIsEndMarker);
                if (ta != null) {
                    leftSide = new IdentifiedTerm(yfx, new Struct(oper.text, leftSide.result, ta.result));
                    continue;
                }
                throw new IllegalStateException("Should we really get to here in the Parser?");
            }
            // either YF has priority over YFX or YFX failed
            if (yf >= Operator.OP_LOW) {
                leftSide = new IdentifiedTerm(yf, new Struct(oper.text, leftSide.result));
                continue;
            }
            break;
        }
        this.tokenizer.unreadToken(oper);
        return leftSide;
    }

    private IdentifiedTerm exprB(int maxPriority, boolean commaIsEndMarker) throws InvalidTermException, IOException {

        // 1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
        IdentifiedTerm left = exprC(commaIsEndMarker, maxPriority);

        // 2.left is followed by either xfx, xfy or xf operators, parse these
        Token oper = this.tokenizer.readToken();
        for (; oper.isOperator(commaIsEndMarker); oper = this.tokenizer.readToken()) {
            int xfx = this.opManager.opPrio(oper.text, Operator.XFX);
            int xfy = this.opManager.opPrio(oper.text, Operator.XFY);
            int xf = this.opManager.opPrio(oper.text, Operator.XF);

            // check that no operator has a priority higher than permitted
            // or a lower priority than the left side expression
            if (xfx > maxPriority || xfx < Operator.OP_LOW) {
                xfx = -1;
            }
            if (xfy > maxPriority || xfy < Operator.OP_LOW) {
                xfy = -1;
            }
            if (xf > maxPriority || xf < Operator.OP_LOW) {
                xf = -1;
            }

            // XFX
            boolean haveAttemptedXFX = false;
            if (xfx >= xfy && xfx >= xf && xfx >= left.priority) { // XFX has priority
                final IdentifiedTerm found = exprA(xfx - 1, commaIsEndMarker);
                if (found != null) {
                    final Struct xfxStruct = new Struct(oper.text, left.result, found.result);
                    left = new IdentifiedTerm(xfx, xfxStruct);
                    continue;
                }
                haveAttemptedXFX = true;
                logger.warn("Is this normal to pass here in the Parser?");
                throw new IllegalStateException("Probably not OK to be here in the Parser");
            }
            // XFY
            if (xfy >= xf && xfy >= left.priority) { // XFY has priority, or XFX has failed
                final IdentifiedTerm found = exprA(xfy, commaIsEndMarker);
                if (found != null) {
                    final Struct xfyStruct = new Struct(oper.text, left.result, found.result);
                    left = new IdentifiedTerm(xfy, xfyStruct);
                    continue;
                }
                throw new IllegalStateException("Should we really get to here in the Parser?");
            }
            // XF
            if (xf >= left.priority) {
                return new IdentifiedTerm(xf, new Struct(oper.text, left.result));
            }

            // XFX did not have top priority, but XFY failed
            if (!haveAttemptedXFX && xfx >= left.priority) {
                final IdentifiedTerm found = exprA(xfx - 1, commaIsEndMarker);
                if (found != null) {
                    final Struct xfxStruct = new Struct(oper.text, left.result, found.result);
                    left = new IdentifiedTerm(xfx, xfxStruct);
                    continue;
                }
                throw new IllegalStateException("Should we really get to here in the Parser?");
            }
            break;
        }
        this.tokenizer.unreadToken(oper);
        return left;
    }

    /**
     * Parses and returns a valid 'leftside' of an expression. If the left side starts with a prefix, it consumes other expressions with a
     * lower priority than itself. If the left side does not have a prefix it must be an expr0.
     * 
     * @param commaIsEndMarker used when the leftside is part of an argument list of expressions
     * @param maxPriority operators with a higher priority than this will effectivly end the expression
     * @return a wrapper of: 1. term correctly structured and 2. the priority of its root operator
     * @throws InvalidTermException
     */
    private IdentifiedTerm exprC(boolean commaIsEndMarker, int maxPriority) throws InvalidTermException, IOException {
        // 1. prefix expression
        final Token oper = this.tokenizer.readToken();
        if (oper.isOperator(commaIsEndMarker)) {
            int fx = this.opManager.opPrio(oper.text, Operator.FX);
            int fy = this.opManager.opPrio(oper.text, Operator.FY);

            if (oper.text.equals("-")) {
                final Token t = this.tokenizer.readToken();
                if (t.isNumber()) {
                    return new IdentifiedTerm(0, createNumber("-" + t.text));
                }
                this.tokenizer.unreadToken(t);
            }

            // check that no operator has a priority higher than permitted
            if (fy > maxPriority) {
                fy = -1;
            }
            if (fx > maxPriority) {
                fx = -1;
            }

            // FX has priority over FY
            boolean haveAttemptedFX = false;
            if (fx >= fy && fx >= Operator.OP_LOW) {
                final IdentifiedTerm found = exprA(fx - 1, commaIsEndMarker); // op(fx, n) exprA(n - 1)
                if (found != null) {
                    return new IdentifiedTerm(fx, new Struct(oper.text, found.result));
                }
                haveAttemptedFX = true;
                throw new IllegalStateException("Should we really get to here in the Parser?");
            }
            // FY has priority over FX, or FX has failed
            if (fy >= Operator.OP_LOW) {
                final IdentifiedTerm found = exprA(fy, commaIsEndMarker); // op(fy,n) exprA(1200) or op(fy,n) exprA(n)
                if (found != null) {
                    return new IdentifiedTerm(fy, new Struct(oper.text, found.result));
                }
                throw new IllegalStateException("Should we really get to here in the Parser?");
            }
            // FY has priority over FX, but FY failed
            if (!haveAttemptedFX && fx >= Operator.OP_LOW) {
                final IdentifiedTerm found = exprA(fx - 1, commaIsEndMarker); // op(fx, n) exprA(n - 1)
                if (found != null) {
                    return new IdentifiedTerm(fx, new Struct(oper.text, found.result));
                }
                throw new IllegalStateException("Should we really get to here in the Parser?");
            }
        }
        this.tokenizer.unreadToken(oper);
        // 2. expr0
        return new IdentifiedTerm(0, exprA0());
    }

    private Object exprA0() throws InvalidTermException, IOException {
        final Token t1 = this.tokenizer.readToken();

        if (t1.isType(INTEGER)) {
            return Long.valueOf(t1.text);
        }

        if (t1.isType(FLOAT)) {
            return Double.valueOf(t1.text);
        }

        if (t1.isType(VARIABLE)) {
            return new Var(t1.text); // todo switched to use the internal check for "_" in Var(String)
        }

        if (t1.isType(ATOM) || t1.isType(SQ_SEQUENCE) || t1.isType(DQ_SEQUENCE)) {
            final String functor = t1.text.intern();
            if (!t1.isFunctor()) {
                if (functor == Struct.FUNCTOR_CUT || functor == Struct.FUNCTOR_TRUE || functor == Struct.FUNCTOR_FALSE) {
                    return new Struct(functor);
                } else {
                    return functor.intern();
                }
            }

            final Token t2 = this.tokenizer.readToken(); // reading left par
            if (!t2.isType(LPAR)) {
                throw new InvalidTermException("bug in parsing process. Something identified as functor misses its first left parenthesis");// todo
            }
            final LinkedList<Object> a = exprA0_arglist(); // reading arguments
            final Token t3 = this.tokenizer.readToken();
            if (t3.isType(RPAR)) {
                return new Struct(functor, a.toArray(new Object[a.size()]));
            }
            throw new InvalidTermException("Missing right parenthesis: (" + a + " -> here <-");
        }

        if (t1.isType(LPAR)) {
            final Object term = expr(false);
            if (this.tokenizer.readToken().isType(RPAR)) {
                return term;
            }
            throw new InvalidTermException("Missing right parenthesis: (" + term + " -> here <-");
        }

        if (t1.isType(LBRA)) {
            final Token t2 = this.tokenizer.readToken();
            if (t2.isType(RBRA)) {
                return Struct.EMPTY_LIST;
            }

            this.tokenizer.unreadToken(t2);
            final Term term = exprA0_list();
            if (this.tokenizer.readToken().isType(RBRA)) {
                return term;
            }
            throw new InvalidTermException("Missing right bracket: [" + term + " -> here <-");
        }

        if (t1.isType(LBRA2)) {
            Token t2 = this.tokenizer.readToken();
            if (t2.isType(RBRA2)) {
                return new Struct("{}");
            }

            this.tokenizer.unreadToken(t2);
            final Object arg = expr(false);
            t2 = this.tokenizer.readToken();
            if (t2.isType(RBRA2)) {
                return new Struct("{}", arg);
            }
            throw new InvalidTermException("Missing right braces: {" + arg + " -> here <-");
        }

        throw new InvalidTermException("The following token could not be identified: " + t1.text);
    }

    private Term exprA0_list() throws InvalidTermException, IOException {
        final Object head = expr(true);
        final Token t = this.tokenizer.readToken();
        if (Struct.LIST_SEPARATOR.equals(t.text)) {
            return Struct.createPList(head, exprA0_list());
        }
        if ("|".equals(t.text)) {
            return Struct.createPList(head, expr(true));
        }
        if ("]".equals(t.text)) {
            this.tokenizer.unreadToken(t);
            return Struct.createPList(head, Struct.EMPTY_LIST);
        }
        throw new InvalidTermException("The expression: \"" + head + "\" is not followed by either a ',' or '|'  or ']'.");
    }

    private LinkedList<Object> exprA0_arglist() throws InvalidTermException, IOException {
        final Object head = expr(true);
        final Token t = this.tokenizer.readToken();
        if (Struct.LIST_SEPARATOR.equals(t.text)) {
            final LinkedList<Object> l = exprA0_arglist();
            l.addFirst(head);
            return l;
        }
        if (")".equals(t.text)) {
            this.tokenizer.unreadToken(t);
            final LinkedList<Object> l = new LinkedList<Object>();
            l.add(head);
            return l;
        }
        throw new InvalidTermException("The argument: \"" + head + "\" is not followed by either a ',' or ')'; at line: " + this.tokenizer.lineno());
    }

    // commodity methods to parse numbers

    Object createNumber(String s) {
        try {
            return Long.valueOf(s);
        } catch (final Exception e) {
            return Double.valueOf(s);
        }
    }

    public int getCurrentLine() {
        return this.tokenizer.lineno();
    }

    /**
     * @return true if the String could be a prolog ATOM_PATTERN
     */
    public static boolean isAtom(String s) {
        return ATOM_PATTERN.matcher(s).matches();
    }

}
