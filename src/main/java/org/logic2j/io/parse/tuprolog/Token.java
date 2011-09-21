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
package org.logic2j.io.parse.tuprolog;

import static org.logic2j.io.parse.tuprolog.MaskConstants.ATTRMASK;
import static org.logic2j.io.parse.tuprolog.MaskConstants.EOF;
import static org.logic2j.io.parse.tuprolog.MaskConstants.FLOAT;
import static org.logic2j.io.parse.tuprolog.MaskConstants.FUNCTOR;
import static org.logic2j.io.parse.tuprolog.MaskConstants.INTEGER;
import static org.logic2j.io.parse.tuprolog.MaskConstants.OPERATOR;
import static org.logic2j.io.parse.tuprolog.MaskConstants.TYPEMASK;

import java.io.Serializable;

/**
 * This class represents a token read by the prolog term tokenizer
 */
class Token implements Serializable {
  private static final long serialVersionUID = 1L;
  // token textual representation
  String text;
  // token type and attribute
  int type;

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
    if (commaIsEndMarker && ",".equals(this.text)) {
      return false;
    }
    return getAttribute() == OPERATOR;
  }

  public boolean isFunctor() {
    return getAttribute() == FUNCTOR;
  }

  public boolean isNumber() {
    return this.type == INTEGER || this.type == FLOAT;
  }

  boolean isEOF() {
    return getAttribute() == EOF;
  }

  boolean isType(int theType) {
    return getType() == theType;
  }
}
