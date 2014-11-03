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
package org.logic2j.contrib.library;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A series of options passed to primitives.
 * Non-efficient data structure but efficient allocation speed / time.
 */
public class OptionsString {

    private static final String SEPARATOR = ",";
    private final String internalCommaSurrounded;

    public OptionsString(String commaSeparatedOptions) {
        String str = commaSeparatedOptions;
        if (str==null) {
            str = ""; // No option
        }
        str = str.trim();
        str = str.replace(" ", "");  // Space is not allowed anywhere (not in the middle of any option)
        if (! str.startsWith(SEPARATOR)) {
            str = SEPARATOR + str;
        }
        if (! str.endsWith(SEPARATOR)) {
            str = str + SEPARATOR;
        }
        this.internalCommaSurrounded = str;
    }


    public OptionsString(UnifyContext context, Object term) {
        this(String.valueOf(context.reify(term)));
    }

    public OptionsString(UnifyContext context, Object term, String defValue) {
        this(term == null ? defValue : String.valueOf(context.reify(term)));
    }


    public boolean hasOption(String optionText) {
        return internalCommaSurrounded.contains(new StringBuilder().append(SEPARATOR).append(optionText).append(SEPARATOR).toString());
    }

    public void assertValidOptions(String[] options) {
        final List<String> optionsList = Arrays.asList(options);
        final Set<String> set = new HashSet<String>(optionsList);
        for (String presentOption : internalCommaSurrounded.split(SEPARATOR)) {
            if (presentOption.isEmpty()) {
                continue; // Initial and final ","
            }
            if (! set.contains(presentOption)) {
                throw new IllegalArgumentException("Option \"" + presentOption + "\" of specified options \"" + this.internalCommaSurrounded + "\" not allowed. Allowed values are: " + optionsList);
            }
        }
    }

}
