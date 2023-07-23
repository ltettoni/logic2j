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
package org.logic2j.core.impl.io.tuprolog.parse;

import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.ATOM;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.BAR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.DOUBLE;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.DQ_SEQUENCE;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.END;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.EOF;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.FLOAT;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.FUNCTOR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.INTEGER;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.LBRA;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.LBRA2;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.LONG;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.LPAR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.OPERATOR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.RBRA;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.RBRA2;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.RPAR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.SQ_SEQUENCE;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.VARIABLE;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.model.Struct;

/**
 * BNF for tuProlog
 * <p>
 * part 1: Lexer digit ::= 0 .. 9 lc_letter ::= a .. z uc_letter ::= A .. Z | _ symbol ::= \ | $ | & | ^ | @ | # | . | , | : | ; | = | < | >
 * | + | - | * | / | ~
 * <p>
 * letter ::= digit | lc_letter | uc_letter integer ::= { digit }+ float ::= { digit }+ . { digit }+ [ E|e [ +|- ] { digit }+ ] atom ::=
 * lc_letter { letter }* | ! variable ::= uc_letter { letter }*
 * <p>
 * from the super class, the super.nextToken() returns and updates the following relevant fields: - if the next token is a collection of
 * wordChars, the type returned is TT_WORD and the value is put into the field sval. - if the next token is an ordinary char, the type
 * returned is the same as the unicode int value of the ordinary character - other characters should be handled as ordinary characters.
 */
class Tokenizer extends StreamTokenizer {

  private static final char[] GRAPHIC_CHARS = {'\\', '$', '&', '?', '^', '@', '#', '.', ',', ':', ';', '=', '<', '>', '+', '-', '*', '/', '~'};

  static {
    Arrays.sort(GRAPHIC_CHARS); // must be done to ensure correct behavior of Arrays.binarySearch
  }

  // used to enable pushback from the parser. Not in any way connected with pushBack2 and super.pushBack().
  private final LinkedList<Token> tokenList = new LinkedList<>();

  // used in the double lookahead check that . following ints is a fraction marker or end marker (pushback() only works on one level)
  private PushBack pushBack2 = null;

  public Tokenizer(String text) {
    this(new StringReader(text));
  }

  /**
   * creating a tokenizer for the source stream
   */
  public Tokenizer(Reader text) {
    super(text);

    // Prepare the tokenizer for Prolog-style tokenizing rules
    resetSyntax();

    // letters
    wordChars('a', 'z');
    wordChars('A', 'Z');
    wordChars('_', '_');
    wordChars('0', '9'); // need to parse numbers as special words

    ordinaryChar('!');

    // symbols
    ordinaryChar('\\');
    ordinaryChar('$');
    ordinaryChar('&');
    ordinaryChar('^');
    ordinaryChar('@');
    ordinaryChar('#');
    ordinaryChar(',');
    ordinaryChar('.');
    ordinaryChar(':');
    ordinaryChar(';');
    ordinaryChar('=');
    ordinaryChar('<');
    ordinaryChar('>');
    ordinaryChar('+');
    ordinaryChar('-');
    ordinaryChar('*');
    ordinaryChar('/');
    ordinaryChar('~');

    // quotes
    ordinaryChar('\''); // must be parsed individually to handles \\ in quotes and character code constants
    ordinaryChar('\"'); // same as above?

    // comments
    ordinaryChar('%');
    // it is not possible to enable StreamTokenizer#slashStarComments and % as a StreamTokenizer#commentChar
    // and it is also not possible to use StreamTokenizer#whitespaceChars for ' '
  }

  /**
   * reads next available token
   */
  Token readToken() throws InvalidTermException, IOException {
    return !this.tokenList.isEmpty() ? this.tokenList.removeFirst() : readNextToken();
  }

  /**
   * puts back token to be read again
   */
  void unreadToken(Token token) {
    this.tokenList.addFirst(token);
  }

  private Token readNextToken() throws IOException, InvalidTermException {
    int typea;
    String svala;
    if (this.pushBack2 != null) {
      typea = this.pushBack2.typea;
      svala = this.pushBack2.svala;
      this.pushBack2 = null;
    } else {
      typea = super.nextToken();
      svala = this.sval;
    }

    // skips whitespace
    // could be simplified if lookahead for blank space in functors wasn't necessary
    // and if '.' in numbers could be written with blank space
    while (isWhite(typea)) {
      typea = super.nextToken();
      svala = this.sval;
    }

    // skips single line comments
    // could be simplified if % was not a legal character in quotes
    if (typea == '%') {
      do {
        typea = super.nextToken();
      } while (typea != '\r' && typea != '\n' && typea != TT_EOF);
      pushBack(); // pushes back \r or \n. These are whitespace, so when readNextToken() finds them, they are marked as whitespace
      return readNextToken();
    }

    // skips /* comments */
    if (typea == '/') {
      int typeb = super.nextToken();
      if (typeb == '*') {
        do {
          typea = typeb;
          typeb = super.nextToken();
        } while (typea != '*' || typeb != '/');
        return readNextToken();
      }
      pushBack();
    }

    // syntactic characters
    if (typea == TT_EOF) {
      return new Token("", EOF);
    }
    if (typea == '(') {
      return new Token("(", LPAR);
    }
    if (typea == ')') {
      return new Token(")", RPAR);
    }
    if (typea == '{') {
      return new Token("{", LBRA2);
    }
    if (typea == '}') {
      return new Token("}", RBRA2);
    }
    if (typea == '[') {
      return new Token("[", LBRA);
    }
    if (typea == ']') {
      return new Token("]", RBRA);
    }
    if (typea == '|') {
      return new Token("|", BAR);
    }

    if (typea == '!') {
      return new Token(Struct.FUNCTOR_CUT, ATOM);
    }
    if (typea == ',') {
      return new Token(",", OPERATOR);
    }

    if (typea == '.') { // check that '.' as end token is followed by a layout character, see ISO Standard 6.4.8 endnote
      final int typeb = super.nextToken();
      if (isWhite(typeb) || typeb == '%' || typeb == StreamTokenizer.TT_EOF) {
        return new Token(".", END);
      }
      pushBack();
    }

    boolean isNumber = false;

    // variable, atom or number
    if (typea == TT_WORD) {
      final char firstChar = svala.charAt(0);
      // variable
      if (Character.isUpperCase(firstChar) || '_' == firstChar) {
        return new Token(svala, VARIABLE);
      } else if (firstChar >= '0' && firstChar <= '9') {
        isNumber = true; // set type to number and handle later
      } else { // otherwise, it must be an atom (or wrong)
        final int typeb = super.nextToken(); // lookahead 1 to identify what type of atom
        pushBack(); // this does not skip whitespaces, only readNext does so.
        if (typeb == '(') {
          return new Token(svala, ATOM | FUNCTOR);
        }
        if (isWhite(typeb)) {
          return new Token(svala, ATOM | OPERATOR);
        }
        return new Token(svala, ATOM);
      }
    }

    // quotes
    if (typea == '\'' || typea == '\"' || typea == '`') {
      int qType = typea;
      final StringBuilder quote = new StringBuilder();
      while (true) { // run through entire quote and added body to quote buffer
        typea = super.nextToken();
        svala = this.sval;
        // Backslash could be a continuation escape sequence, or an escaped char
        if (typea == '\\') {
          final int typeb = super.nextToken();
          final String svalb = this.sval;
          if (typeb == '\n') {
            continue;
          }
          if (typeb == '\r') {
            final int typec = super.nextToken();
            if (typec == '\n') {
              continue; // continuation escape sequence marker \\r\n
            }
            pushBack();
            continue; // continuation escape sequence marker \\r
          }
          if (typeb == '\\') {
            // Double backslash. Output only one and don't pushback
            quote.append('\\');
            continue; // 2 baclslashes eaten
          }
          // Sneak peek at the first character following the backslash
          if (svalb != null && svalb.length()>=1) {
            final char afterBackslash = svalb.charAt(0);
            switch (afterBackslash) {
              case 'n':
                quote.append('\n');
                svala = this.sval.substring(1); // "Remove that first char from what we will append below
                break;
              case 'r':
                quote.append('\r');
                svala = this.sval.substring(1); // "Remove that first char from what we will append below
                break;
              default:
                // Any other should be handled as a normal token
                pushBack();
            }
          } else {
            pushBack(); // pushback typeb we continue parsing on next line
          }
        }
        // double '' or "" or ``
        if (typea == qType) {
          final int typeb = super.nextToken();
          if (typeb == qType) { // escaped '' or "" or ``
            quote.append((char) qType);
            continue;
          }
          pushBack();
          break; // otherwise, break on single quote
        }
        if (typea == '\n' || typea == '\r') {
          throw new InvalidTermException("line break in quote not allowed (unless they are escaped \\ first)");
        }

        if (svala != null) {
          quote.append(svala);
        } else {
          quote.append((char) typea);
        }
      }

      final String quoteBody = quote.toString();

      qType = qType == '\'' ? SQ_SEQUENCE : qType == '\"' ? DQ_SEQUENCE : SQ_SEQUENCE;
      if (qType == SQ_SEQUENCE) {
        if (Parser.isAtom(quoteBody)) {
          qType = ATOM;
        }
        final int typeb = super.nextToken(); // lookahead 1 to identify what type of quote
        pushBack(); // nextToken() does not skip whitespaces, only readNext does so.
        if (typeb == '(') {
          return new Token(quoteBody, qType | FUNCTOR);
        }
      }
      return new Token(quoteBody, qType);
    }

    // symbols
    if (Arrays.binarySearch(GRAPHIC_CHARS, (char) typea) >= 0) {

      // the symbols are parsed individually by the super.nextToken(), so accumulate symbollist
      final StringBuilder symbols = new StringBuilder();
      int typeb = typea;
      // String svalb = null;
      while (Arrays.binarySearch(GRAPHIC_CHARS, (char) typeb) >= 0) {
        symbols.append((char) typeb);
        typeb = super.nextToken();
        // svalb = sval;
      }
      pushBack();

      // special symbols: unary + and unary -
      // try {
      // if (symbols.length() == 1 && typeb == TT_WORD && Long.parseLong(svalb) > 0) {
      // if (typea == '+') //todo, issue of handling + and -. I don't think this is ISO..
      // return readNextToken(); //skips + and returns the next number
      // if (typea == '-') {
      // Token t = readNextToken(); //read the next number
      // t.seq = "-" + t.seq; //add minus to value
      // return t; //return token
      // }
      // } //ps. the reason why the number isn't returned right away, but through nextToken(), is because the number might be for
      // instance a float
      // } catch (NumberFormatException e) {
      // }
      return new Token(symbols.toString(), OPERATOR);
    }

    // numbers: 1. integer, 2. float
    if (isNumber) {
      try { // the various parseInt checks will throw exceptions when parts of numbers are written illegally

        // 1.a. complex integers
        if (svala.startsWith("0")) {
          if (svala.indexOf('b') == 1) {
            return new Token(String.valueOf(Long.parseLong(svala.substring(2), 2)), INTEGER); // try binary
          }
          if (svala.indexOf('o') == 1) {
            return new Token(String.valueOf(Long.parseLong(svala.substring(2), 8)), INTEGER); // try octal
          }
          if (svala.indexOf('x') == 1) {
            return new Token(String.valueOf(Long.parseLong(svala.substring(2), 16)), INTEGER); // try hex
          }
        }

        // lookahead 1
        final int typeb = super.nextToken();
        final String svalb = this.sval;

        // 1.b ordinary integers
        if (typeb != '.' && typeb != '\'') { // i.e. not float or character constant
          pushBack(); // lookahead 0

          if (svala.toUpperCase().endsWith("L")) {
            svala = svala.substring(0, svala.length() - 1);
            return new Token(String.valueOf(Long.parseLong(svala)), LONG);
          } else {
            return new Token(String.valueOf(Integer.parseInt(svala)), INTEGER);
          }
        }

        // 1.c character code constant
        if (typeb == '\'' && "0".equals(svala)) {
          final int typec = super.nextToken(); // lookahead 2
          final String svalc = this.sval;
          int intVal;
          if ((intVal = isCharacterCodeConstantToken(typec, svalc)) != -1) {
            return new Token(String.valueOf(intVal), INTEGER);
          }

          // this is an invalid character code constant int
          throw new InvalidTermException("Character code constant starting with 0'<X> at line: " + super.lineno() + " cannot be recognized.");
        }

        // 2.a check that the value of the word prior to period is a valid long
        Long.parseLong(svala); // throws an exception if not

        // 2.b first int is followed by a period
        if (typeb != '.') {
          throw new InvalidTermException(
                  "A number starting with 0-9 cannot be rcognized as an int and does not have a fraction '.' at line: " + super.lineno());
        }

        // lookahead 2
        final int typec = super.nextToken();
        final String svalc = this.sval;

        // 2.c check that the next token after '.' is a possible fraction
        if (typec != TT_WORD) { // if its not, the period is an End period
          pushBack(); // pushback 1 the token after period
          this.pushBack2 = new PushBack(typeb, svalb); // pushback 2 the period token
          if (svala.toUpperCase().endsWith("L")) {
            svala = svala.substring(0, svala.length() - 1);
            return new Token(String.valueOf(Long.parseLong(svala)), LONG);
          } else {
            return new Token(String.valueOf(Integer.parseInt(svala)), INTEGER);
          }
        }

        // 2.d checking for exponent
        int exponent = svalc.indexOf("E");
        if (exponent == -1) {
          exponent = svalc.indexOf("e");
        }

        if (exponent >= 1) { // the float must have a valid exponent
          if (exponent == svalc.length() - 1) { // the exponent must be signed exponent
            final int typeb2 = super.nextToken();
            if (typeb2 == '+' || typeb2 == '-') {
              final int typec2 = super.nextToken();
              final String svalc2 = this.sval;
              if (typec2 == TT_WORD) {
                // verify the remaining parts of the float and return
                Long.parseLong(svalc.substring(0, exponent));
                Integer.parseInt(svalc2);

                String nbtext = svala + "." + svalc + (char) typeb2 + svalc2;
                if (nbtext.toUpperCase().endsWith("F")) {
                  nbtext = nbtext.substring(0, nbtext.length() - 1);
                  Double.parseDouble(nbtext);
                  return new Token(nbtext, FLOAT);
                } else {
                  Double.parseDouble(nbtext);
                  return new Token(nbtext, DOUBLE);
                }
              }
            }
          }
        }
        // 2.e verify lastly that ordinary floats and unsigned exponent floats are Java legal and return them
        String nbtext = svala + "." + svalc;
        if (nbtext.toUpperCase().endsWith("F")) {
          nbtext = nbtext.substring(0, nbtext.length() - 1);
          Double.parseDouble(nbtext);
          return new Token(nbtext, FLOAT);
        } else {
          Double.parseDouble(nbtext);
          return new Token(nbtext, DOUBLE);
        }
      } catch (final NumberFormatException e) {
        // return more info on what was wrong with the number given ?
        throw new InvalidTermException("A term starting with 0-9 cannot be parsed as a number at line: " + lineno());
      }
    }
    throw new InvalidTermException("Unknown Unicode character: " + typea + "  (" + svala + ")");
  }

  /**
   * @param typec
   * @param svalc
   * @return the intValue of the next character token, -1 if invalid needs a lookahead if typec is \
   */
  private int isCharacterCodeConstantToken(int typec, String svalc) {
    if (svalc != null) {
      if (svalc.length() == 1) {
        return svalc.charAt(0);
      }
      if (svalc.length() > 1) {
        // The following characters are not implemented:
        // * 1 meta escape sequence (* 6.4.2.1 *)
        // * 1 control escape sequence (* 6.4.2.1 *)
        // * 1 octal escape sequence (* 6.4.2.1 *)
        // * 1 hexadecimal escape sequence (* 6.4.2.1 *)
        return -1;
      }
    }
    if (typec == ' ' || // space char (* 6.5.4 *)
            Arrays.binarySearch(GRAPHIC_CHARS, (char) typec) >= 0) {
      return typec;
    }

    return -1;
  }

  private boolean isWhite(int type) {
    return type == ' ' || type == '\r' || type == '\n' || type == '\t' || type == '\f';
  }

  /**
   * used to implement lookahead for two tokens, super.pushBack() only handles one pushBack..
   */
  private static class PushBack {
    final int typea;
    final String svala;

    public PushBack(int i, String s) {
      this.typea = i;
      this.svala = s;
    }
  }
}
