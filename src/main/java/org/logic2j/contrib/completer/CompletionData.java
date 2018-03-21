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

package org.logic2j.contrib.completer;

import java.util.Collections;
import java.util.Set;

/**
 * Created by tettoni on 2015-10-11.
 */
public class CompletionData {

  public String original; // Complete input submitted

  public String originalBeforeStripped;

  public String stripped; // Only the last part where we search for completion
  public String functor;  // When processing arguments, the functor of these arguments

  /**
   * From the beginning of the predicate (functor), until before the stripped part
   */
  public String partialPredicate;

  public int argNo;

  Set<String> completions = Collections.emptySet();

  public Set<String> getCompletions() {
    return completions;
  }

  public void setCompletions(Set<String> completions) {
    this.completions = completions;
  }
}
