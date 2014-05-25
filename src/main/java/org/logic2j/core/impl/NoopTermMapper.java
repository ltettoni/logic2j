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
 * A TermMapper that does nothing.
 */
public class NoopTermMapper implements TermMapper {

    /**
     * Does nothing.
     * @param theTerm
     * @return theTerm
     */
    @Override
    public Object apply(Object theTerm) {
        return theTerm;
    }
}