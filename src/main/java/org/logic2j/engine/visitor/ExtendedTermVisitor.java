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
package org.logic2j.engine.visitor;

import org.logic2j.engine.model.Term;
import org.logic2j.engine.model.TermApi;

/**
 * Extension of the {@link TermVisitor} for type of classes that are NOT
 * subclasses of {@link Term}.
 * This requires calling {@link TermApi#accept(ExtendedTermVisitor, Object theTerm)}
 */
public interface ExtendedTermVisitor<T> extends TermVisitor<T> {

  /**
   * Extra visiting method for String, because often String need special handling,
   * for example in a visitor to marshall a Term, quoting needs to be done.
   *
   * @param theAtomString
   * @return
   */
  T visit(String theAtomString);

  /**
   * The "fallback" method on Object will be invoked if no other more specific visit() method
   * could be found.
   *
   * @param theObject
   * @return
   */
  T visit(Object theObject);
}
