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

import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Term;
import org.logic2j.engine.model.Var;

import java.util.List;

/**
 * Interaction between the environment (host, programs, variables, streams/files) and the Prolog engine, in both directions.
 * In particular, convert from Java objects to {@link Term}s, and vice-versa from {@link Term}s to Java objects.
 * Do not confuse with TermMarshaller and TermUnmarshaller that deal with string conversion (formatting and parsing).
 */
public interface TermAdapter {

  enum FactoryMode {
    /**
     * Result will always be an atom (a {@link Struct} of 0-arity), will never be a {@link Var}iable.
     * In the case of null, will create an empty-string atom.
     */
    ATOM,

    /**
     * Result will be either an atom (a {@link Struct} of 0-arity), an object, but not a {@link Var}iable neither a
     * compound {@link Struct}.
     */
    LITERAL,

    /**
     * Result will be any {@link Term} (atom, number, {@link Var}iable), but not a compound {@link Struct}.
     */
    ANY_TERM,

    /**
     * Result can be any term plus compound structures.
     */
    COMPOUND
  }


  /**
   * Describe the form that data structures should take when represented as Prolog compounds (Struct).
   * See TabularData and related classes.
   */
  enum AssertionMode {
    /**
     * Data is asserted as "named triples". For a dataset called myData, assertions will be such as:
     * myData(entityIdentifier, propertyName, propertyValue).
     */
    EAV_NAMED,
    /**
     * Data is asserted as "quads". The predicate is always "eavt(entity, attribute, value, transaction)".
     * The "transaction" identifier is the dataset name. For example:
     * eavt(entityIdentifier, propertyName, propertyValue, myData).
     */
    EAVT,
    /**
     * Data is asserted as full records with one argument per column, such as
     * "myData(valueOfColumn1, valueOfColumn2, valueOfColumn3, ..., valueOfColumnN)."
     * The order of columns obviously matters.
     * If your data is already triples, use this mode.
     * This is the least flexible form since changes to the tabularData (adding or removing or reordering columns) will change the assertions.
     */
    RECORD
  }

  /**
   * Convert: From regular Java Object to Prolog internal Term.
   * Convert from virtually any possible instance of singular {@link Object} into to a Prolog term
   * (usually a Struct but any object is valid in logic2j).
   * This is the highest-level factory for terms.
   *
   * @param theObject
   * @param theMode
   * @return A factorized and normalized {@link Term}.
   */
  Object toTerm(Object theObject, FactoryMode theMode);

  /**
   * Instantiate a Struct with arguments from virtually any class of {@link Object}
   * This is the highest-level factory for Struct.
   *
   * @param thePredicateName The predicate (functor)
   * @param theMode
   * @param theArguments
   * @return A factorized and normalized {@link Term}.
   */
  Struct toStruct(String thePredicateName, FactoryMode theMode, Object... theArguments);

  /**
   * Instantiate a list of Terms from one (possibly large) {@link Object}.
   *
   * @param theObject
   * @return A List of terms.
   */
  List<Object> toTerms(Object theObject, AssertionMode theAssertionMode);

  /**
   * Convert: From Prolog internal Term to regular Java Object.
   *
   * @param theTargetClass Either very specific or quite general like Enum, or even Object
   * @return The converted instance
   */
  <T> T fromTerm(Object theTerm, Class<T> theTargetClass);


  Object getVariable(String theExpression);

  TermAdapter setVariable(String theExpression, Object theValue);
}
