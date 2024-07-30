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

import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.ATTRMASK;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.DOUBLE;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.EOF;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.FLOAT;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.FUNCTOR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.INTEGER;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.LONG;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.OPERATOR;
import static org.logic2j.core.impl.io.tuprolog.parse.MaskConstants.TYPEMASK;

import java.io.Serial;
import java.io.Serializable;

/**
 * This class represents a token read by the prolog term tokenizer
 */
class Token implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  // token textual representation
  final String text;
  // token type and attribute
  private final int type;

  public Token(String seq_, int type_) {
    this.text = seq_;
    this.type = type_;
  }

  /**
   * @return Type flag
   */
  public int getType() {
    return this.type & TYPEMASK;
  }

  /**
   * @return Attribute flag could be FUNCTOR, OPERATOR or EOF
   */
  private int getAttribute() {
    return this.type & ATTRMASK;
  }

  public boolean isOperator(boolean commaIsEndMarker) {
    return !(commaIsEndMarker && ",".equals(this.text)) && getAttribute() == OPERATOR;
  }

  public boolean isFunctor() {
    return getAttribute() == FUNCTOR;
  }

  public boolean isNumber() {
    return this.type == INTEGER || this.type == LONG || this.type == FLOAT || this.type == DOUBLE;
  }

  boolean isEOF() {
    return getAttribute() == EOF;
  }

  boolean isType(int theType) {
    return getType() == theType;
  }

  @Override
  public String toString() {
    return "Token('" + text + '\'' + ", type=" + type + '}';
  }
}
