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

import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.TermBindings;

/**
 * Like {@link DefaultTermMarshaller} but for every free var, report the variable at the end of
 * bound variables chain.
 */
public class FinalVarTermMarshaller extends DefaultTermMarshaller {

    // Never null
    private final TermBindings bindings;

    /**
     * @param theProlog
     * @param theBindingsForCriteria
     */
    public FinalVarTermMarshaller(PrologImplementation theProlog, TermBindings theBindingsForCriteria) {
        super(theProlog);
        this.bindings = theBindingsForCriteria;
    }


    @Override
    protected CharSequence accept(Object theTerm, TermBindings theBindingsIgnored) {
      return TermApi.accept(this, theTerm, theBindingsIgnored);
    }
    
    @Override
    public CharSequence visit(Var theVar, TermBindings theBindingsIgnored) {
        if (theVar.isAnonymous()) {
            return Var.ANONYMOUS_VAR_NAME;
        }
        if (theBindingsIgnored == null) {
          theBindingsIgnored = this.bindings;
        }
        final Binding initialBinding = theVar.bindingWithin(theBindingsIgnored);
        final Binding finalBinding = initialBinding.followLinks();
        final CharSequence formatted;
        if (finalBinding.isFree()) {
            formatted = finalBinding.getReferrer().getName();
        } else {
            formatted = TermApi.accept(this, finalBinding.getTerm(), finalBinding.getBindings());
        }
        return formatted;
    }
}
