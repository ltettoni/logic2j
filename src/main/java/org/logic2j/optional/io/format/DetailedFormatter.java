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

package org.logic2j.optional.io.format;

import org.logic2j.core.api.TermExchanger;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.DefaultTermExchanger;

/**
 * A {@link TermExchanger} with more detailed information, use for debugging.
 */
public class DetailedFormatter extends DefaultTermExchanger {

    /**
     * Variables are reported with their offset in their {@link Bindings}.
     */
    @Override
    protected String formatVar(Var theVar) {
        if (theVar.isAnonymous()) {
            // The anonymous variable has no index - won't report it!
            return Var.ANONYMOUS_VAR_NAME;
        }
        return theVar.getName() + '@' + theVar.getIndex();
    }

}
