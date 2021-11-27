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
 * State of completion: from the original input to all possible choices.
 */
public class CompletionData {

  /**
   * Original input submitted
   */
  private final String original;

  /**
   * The first part of "original" until the position where we start searches for autocompletion.
   * If the original is "myPred(abc", we are typicallly completing on abc, so originalBeforeStripped="myPred("
   */
  private String originalBeforeStripped;

  /**
   * The part of "original" we search for autocompletion.
   * If the original is "myPred(abc", we are autocompleting on "abc", so stripped="abc"
   */
  private String stripped;


  /**
   * When we are autocompleting on arguments, this will be the functor before the first argument.
   */
  private String functor = null;

  /**
   * From the beginning of the predicate (functor), until before the stripped part
   */
  private String partialPredicate = null;

  private int argNo = -1;

  /**
   * The current completions
   */
  private Set<String> completions = Collections.emptySet();


  public CompletionData(String original) {
    this.original = original;
    this.stripped = original;
    this.originalBeforeStripped = "";
  }

  // --------------------------------------------------------------------------
  // Accessors
  // --------------------------------------------------------------------------


  public String getOriginal() {
    return original;
  }

  public String getOriginalBeforeStripped() {
    return originalBeforeStripped;
  }

  public void setOriginalBeforeStripped(String originalBeforeStripped) {
    this.originalBeforeStripped = originalBeforeStripped;
  }

  public String getStripped() {
    return stripped;
  }

  public void setStripped(String stripped) {
    this.stripped = stripped;
  }

  public String getFunctor() {
    return functor;
  }

  public void setFunctor(String functor) {
    this.functor = functor;
  }

  public String getPartialPredicate() {
    return partialPredicate;
  }

  public void setPartialPredicate(String partialPredicate) {
    this.partialPredicate = partialPredicate;
  }

  public int getArgNo() {
    return argNo;
  }

  public void setArgNo(int argNo) {
    this.argNo = argNo;
  }

  public Set<String> getCompletions() {
    return completions;
  }

  public void setCompletions(Set<String> completions) {
    this.completions = completions;
  }
}
