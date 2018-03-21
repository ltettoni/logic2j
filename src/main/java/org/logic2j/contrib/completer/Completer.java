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

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.model.Clause;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.extractor.SingleVarExtractor;
import org.logic2j.engine.solver.listener.SingleVarSolutionListener;
import org.logic2j.core.impl.PrologImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Created by tettoni on 2015-10-11.
 */
public class Completer {
  private static final Logger logger = LoggerFactory.getLogger(Completer.class);
  private static final String COMPLETION_VAR = "CompleterVar";
  private static final int MAX_FETCH = 10000; // Number of completion results to seek

  private final PrologImplementation prolog;

  public Completer(PrologImplementation prolog) {
    this.prolog = prolog;
  }

  /**
   * Avoid predicates such as '->', ';', etc
   */
  private static final Pattern ACCEPTABLE = Pattern.compile("[\\w']+/\\d+");

  /**
   * To avoid technical predicates such as pred_, pred_1, pred_2)
   */
  private static final Pattern TECH_PREDICATE = Pattern.compile("[\\w']+_\\d*/\\d+");


  static CompletionData strip(String str) {
    final CompletionData result = new CompletionData();
    result.original = str;
    result.stripped = str;
    result.originalBeforeStripped = "";
    result.partialPredicate = null;
    result.functor = null;
    if (str.length() > 0) {
      char c;
      int pos = str.length() - 1;
      for (pos = str.length() - 1; pos >= 0; pos--) {
        c = str.charAt(pos);
        final boolean cont = Character.isJavaIdentifierPart(c) || Character.isJavaIdentifierStart(c) || Character.isDigit(c) || c == '\'';
        if (!cont) {
          // stripped is only the last part we are completing on
          result.stripped = str.substring(pos + 1);
          result.originalBeforeStripped = str.substring(0, pos + 1);

          int end = pos + 1;
          int argNo = 0;
          for (; pos >= 0; pos--) {
            c = str.charAt(pos);
            if (c == ',') {
              argNo++;
            } else if (c == '(') {
              int parenth = pos;
              for (pos--; pos >= 0; pos--) {
                c = str.charAt(pos);
                if (!(Character.isJavaIdentifierPart(c) || Character.isJavaIdentifierStart(c) || Character.isDigit(c))) {
                  break;
                }
              }
              result.partialPredicate = str.substring(pos + 1, end);
              result.functor = str.substring(pos + 1, parenth);
              result.argNo = argNo;
              return result;
            } else if (c == ')') {
              result.functor = null;
              break;
            }
          }
          return result;
        }
      }
      return result;
    }
    return result;
  }

  /**
   * @param partialInput
   * @return Ordered set of predicates signatures (a/1, append/3, )
   */
  Set<String> allSignatures(CharSequence partialInput) {
    final Set<String> signatures = new TreeSet<String>();
    // From all loaded clause providers
    for (final ClauseProvider cp : this.prolog.getTheoryManager().getClauseProviders()) {
      for (final Clause clause : cp.listMatchingClauses(new Var<Object>("unused"), null)) {
        final String predicateKey = clause.getPredicateKey();
        if (acceptPredicateKey(predicateKey) && predicateKey.startsWith(partialInput.toString())) {
          signatures.add(predicateKey);
        } else {
          logger.debug("Signature not retained for completion: {}", predicateKey);
        }
      }
    }
    // From libraries (TBD)

    logger.debug("Distinct signatures: {}", signatures);
    return signatures;
  }

  static boolean acceptPredicateKey(String predicateKey) {
    // Check if this is a technical predicate,
    return !TECH_PREDICATE.matcher(predicateKey).matches() && ACCEPTABLE.matcher(predicateKey).matches();
  }


  /**
   * Entry point
   *
   * @param partialInput
   * @return The result of completing the partialInput
   */
  public CompletionData complete(CharSequence partialInput) {
    final CompletionData completionData = strip(partialInput.toString());
    final Set<String> completions = new TreeSet<String>();
    if (partialInput.toString().endsWith(")")) {
      // Nothing
    } else if (completionData.functor != null) {
      // Find arity
      final Set<String> signatures = allSignatures(completionData.functor);
      if (signatures.isEmpty()) {
        return completionData;
      }
      // find same predicate
      for (String signature : signatures) {
        if (TermApi.functorFromSignature(signature).equals(completionData.functor)) {
          int arity = TermApi.arityFromSignature(signature);

          final int commaCount = commaCount(completionData.partialPredicate);
          String goal = buildGoal(completionData.partialPredicate, arity);
          logger.info("Going to execute: {}", goal);
          Object goalObj = prolog.getTermUnmarshaller().unmarshall(goal);

          SingleVarExtractor<Object> stringSingleVarExtractor = new SingleVarExtractor<Object>(goalObj, COMPLETION_VAR, Object.class);
          SingleVarSolutionListener<Object> listener = new SingleVarSolutionListener<Object>(stringSingleVarExtractor);
          listener.setMaxFetch(MAX_FETCH);

          try {
            this.prolog.getSolver().solveGoal(goalObj, listener);
          } catch (StackOverflowError e) {
            // Typical completion for "member(" will try to solve "member(CompletionVar, _)" which has infinite solutions.
            return completionData;
          }
          boolean hasVar = false;
          final String termination = (arity > commaCount + 1) ? ", " : ")";
          for (Object sol : listener.getResults()) {
            String compl;
            String envisagedCompletion;
            if (sol instanceof Var<?>) {
              hasVar = true;
              envisagedCompletion = null;
            } else if (sol instanceof Number) {
              envisagedCompletion = sol.toString();
            } else {
              compl = String.valueOf(sol);
              compl = TermApi.quoteIfNeeded(compl).toString();
              envisagedCompletion = compl;
            }
            // FIXME: watch out - instead of using all the solutions, we must first make sure
            // the solution matches the beginning of the text specified!

            if (envisagedCompletion != null && envisagedCompletion.startsWith(completionData.stripped)) {
              completions.add(completionData.originalBeforeStripped + envisagedCompletion + termination);
            }
          }
          if (hasVar) {
            completions.add(partialInput + "X" + termination);
            completions.add(partialInput + "_" + termination);
          }
        }
      }

    } else {
      // Find all predicate's signatures starting with the stripped fragment
      for (String signature : allSignatures(completionData.stripped)) {
        // Generate fragment from signature:  "append/3" -> "append("
        final String functor = TermApi.functorFromSignature(signature);
        final String fragment = functor + '(';
        if (fragment.startsWith(completionData.stripped)) {
          completions.add(completionData.originalBeforeStripped + fragment);
        }
      }
    }
    completionData.setCompletions(completions);
    return completionData;
  }

  private String buildGoal(String partialPredicate, int arity) {
    final int commaCount = commaCount(partialPredicate);
    StringBuilder sb = new StringBuilder(partialPredicate);
    sb.append(COMPLETION_VAR);
    for (int i = arity - 1; i > commaCount; i--) {
      sb.append(", _");
    }
    sb.append(')');
    return sb.toString();
  }

  private int commaCount(String partialPredicate) {
    int commaCount = 0;
    for (char c : partialPredicate.toCharArray()) {
      if (c == ',') {
        commaCount++;
      }
    }
    return commaCount;
  }
}
