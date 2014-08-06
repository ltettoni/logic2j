/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.core.impl;

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermMapper;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;

import java.util.*;
import java.util.Map.Entry;

/**
 * Default and reference implementation of {@link TermAdapter}.
 */
public class DefaultTermAdapter implements TermAdapter {

    private final TermMapper NOOP_MAPPER = new TermMapper() {

        /**
         * Identity - return its argument.
         * @param theTerm
         * @return theTerm
         */
        @Override
        public Object apply(Object theTerm) {
            return theTerm;
        }
    };

    // FIXME use the current Prolog's configured TermMarshaller, not the default one!
    public static final DefaultTermMarshaller DEFAULT_TERM_MARSHALLER = new DefaultTermMarshaller();

    private IdentityHashMap<String, Object> predefinedAtoms = null;

    private TermMapper normalizer = NOOP_MAPPER;

    // TODO be smarter to handle Arrays and Collections, and Iterables
    @Override
    public Object term(Object theObject, FactoryMode theMode) {
        // TODO Temporary just for compatibility - move this to TermUnmarshaller
        if (theObject instanceof CharSequence) {
            if (theMode == FactoryMode.ATOM) {
                final String string = theObject.toString();
                if (this.predefinedAtoms != null) {
                    final Object predefinedAtom = this.predefinedAtoms.get(string);
                    if (predefinedAtom != null) {
                        return predefinedAtom;
                    }
                }
                return Struct.atom(string);
            }
            throw new UnsupportedOperationException("TermAdapter cannot parse complex CharSequences, use TermUnmarshaller instead");
        }
        final Object created = termFrom(theObject, theMode);
        final Object normalized = normalizer.apply(created);
        return normalized;
    }


    @Override
    public Struct term(String thePredicateName, FactoryMode theMode, Object... theArguments) {
        final Object[] convertedArgs = new Object[theArguments.length];
        for (int i = 0; i < theArguments.length; i++) {
            convertedArgs[i] = termFrom(theArguments[i], theMode);
        }
        final Struct created = new Struct(thePredicateName, convertedArgs);
        final Struct normalized = (Struct) normalizer.apply(created);
        return normalized;
    }

    /**
     * Factory that can be overridden.
     * 
     * @param theObject
     * @param theMode
     * @return An instance of Term
     */
    protected Object termFrom(Object theObject, FactoryMode theMode) {
        Object result = null;
        if (theObject == null) {
            if (theMode == FactoryMode.ATOM) {
                result = Struct.atom(""); // The empty string atom, see note on FactoryMode.ATOM
            } else {
                throw new InvalidTermException("Cannot create Term from a null argument");
            }
        }
        if (theObject instanceof CharSequence || theObject instanceof Character) {
            // Rudimentary parsing
            final String chars = String.valueOf(theObject);
            if (theMode == FactoryMode.ATOM) {
                // Anything becomes an atom, actually only a Struct since we don't have powerful parsing here
                // result = new Struct(chars);
                result = Struct.atom(chars);
            }
        }
        // Otherwise apply basic algorithm from TermApi
        if (result == null) {
            result = TermApi.valueOf(theObject, theMode);
        }
        return result;
    }

    /**
     * @return A List of one single Term from {@link #term(Object, org.logic2j.core.api.TermAdapter.FactoryMode)}.
     */
    @Override
    public List<Object> terms(Object theObject, AssertionMode theAssertionMode) {
        final List<Object> result = new ArrayList<Object>();
        final Object term = term(theObject, FactoryMode.ATOM);
        result.add(term);
        return result;
    }

    /**
     * Default conversion allows for:
     * - exact matching
     * - castable interface or subclass
     * - String and CharSequence
     * - Enum  (handles exact value 'VAL' that must match Enum.name(), or any_wrapping_functor('VAL')
     * @param theTerm
     * @param theTargetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T object(Object theTerm, Class<T> theTargetClass) {
        if (theTerm==null) {
            // Pass-through for nulls
            return null;
        }
        final Class<?> termClass = theTerm.getClass();
        if (termClass ==theTargetClass) {
            // Exact class
            return (T)theTerm;
        }
        if (theTargetClass.isAssignableFrom(termClass)) {
            // Allowed cast as per class hierarchy or interface implementation
            return (T)theTerm;
        }
        if (theTargetClass==String.class || theTargetClass==CharSequence.class) {
            if (theTerm instanceof Struct) {
                return (T) DEFAULT_TERM_MARSHALLER.marshall(theTerm).toString();
            }
            return (T)String.valueOf(theTerm);
        }
        if (theTerm instanceof Struct && theTargetClass==List.class) {
            final Collection<?> collection = ((Struct) theTerm).javaListFromPList(null, Object.class);
            return (T)collection;
        }
        // Now these conversions are getting a bit rare. Prepare error message it's likely we end up in error
        final String termDescription = "term \"" + theTerm + "\" of " + termClass;
        String adapterInstanceName = this.toString();
        // For anonymous classes we end up with "" !
        if (adapterInstanceName.isEmpty()) {
            adapterInstanceName = TermAdapter.class.getSimpleName();
        }
        final String message = adapterInstanceName + " cannot convert " + termDescription + " to " + theTargetClass;


        if (Enum.class.isAssignableFrom(theTargetClass)) {
            if (theTargetClass == Enum.class) {
                throw new IllegalArgumentException(message + ": converting to any Enum will require a custom TermAdapter, " + this + " cannot guess your intended Enum class");
            }

            // For converting to Enum, we expect that theTerm will match the name() of the target Enum.
            // We will allow theTerm to be either the exact atom (eg 'VAL'), or
            // a struct/1 with any functor and the value, (eg my_enum_type_ignored('VAL')).
            // The reason is for consistency with the cases when the Prolog will return an enum value of a class that
            // Java cannot pre-determine. In that case user has to override this method in a dedicated TermAdapter,
            // and lookup the effective enum from the specified functor.

            final String effectiveEnumName;
            if (theTerm instanceof String) {
                effectiveEnumName = theTerm.toString();
            } else if (theTerm instanceof Struct) {
                Struct s = (Struct)theTerm;
                if (s.getArity()!=1) {
                    throw new IllegalArgumentException(message + ": if a Struct is passed, arity must be 1, was " + s.getArity());
                }
                effectiveEnumName = s.getArg(0).toString();
            } else {
                throw new IllegalArgumentException(message + ": converting to an Enum requires either an atom or a struct of arity=1");
            }


            final Enum[] enumConstants = ((Class<Enum>) theTargetClass).getEnumConstants();
            for (Enum c: enumConstants) {
                if (c.name().equals(effectiveEnumName)) {
                    return (T)c;
                }
            }
            throw new IllegalArgumentException(message + ": no such enum value");
        }
        throw new UnsupportedOperationException(message);
    }

    /**
     * Allow changing default behaviour of {@link #termFrom(Object, org.logic2j.core.api.TermAdapter.FactoryMode)} when FactoryMode is ATOM,
     * and the first argument exactly matches one of the keys of the map: will return the value
     * as the atom.
     * 
     * @param theAtoms
     */
    public void setPredefinedAtoms(Map<String, Object> theAtoms) {
        this.predefinedAtoms = new IdentityHashMap<String, Object>(theAtoms.size());
        for (final Entry<String, Object> entry : theAtoms.entrySet()) {
            this.predefinedAtoms.put(entry.getKey().intern(), entry.getValue());
        }
    }

    public void setNormalizer(TermMapper normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
