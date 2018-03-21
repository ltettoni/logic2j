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
package org.logic2j.core.api;

import org.logic2j.engine.model.Term;

/**
 * Umnarshalls streamable representations back to create new {@link Term}s.
 */
public interface TermUnmarshaller {

  /**
   * Parse a character stream into a {@link Term}.
   *
   * @param theChars
   * @return The new {@link Term} obtained from its textual representation.
   */
  Object unmarshall(CharSequence theChars);

}
