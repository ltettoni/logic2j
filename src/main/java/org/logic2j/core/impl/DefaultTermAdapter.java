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
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.TermApi;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Default and reference implementation of {@link TermAdapter}.
 */
public class DefaultTermAdapter implements TermAdapter {

    private IdentityHashMap<String, Object> predefinedAtoms = null;

    private TermMapper normalizer = new NoopTermMapper();

    // TODO be smarter to handle Arrays and Collections, and Iterables
    @Override
    public Object term(Object theObject, FactoryMode theMode) {
        // FIXME TEMPORARY JUST FOR COMPATIBILITY - move this to TermUnmarshaller
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

    @Override
    public <T> T object(Term t, Class<T> theTargetClass) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Allow changing default behaviour of {@link #termFrom(Object, org.logic2j.core.api.TermAdapter.FactoryMode)} when FactoryMode is ATOM,
     * and the first agrument exactly matches one of the keys of the map: will return the value
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

}
