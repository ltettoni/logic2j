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
package org.logic2j.core.api.model.var;

import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.Var;

/**
 * Define the effective value of a variable, it can be either free, bound to a final term, or unified to another variable (either free,
 * bound, or chaining).
 * 
 * The properties of a {@link Binding} depend on its type according to the following table:
 * 
 * <pre>
 * Value of fields depending on the BindingType
 * -----------------------------------------------------------------------------------------------------
 * type     literalBindings               term           link                                         var
 * -----------------------------------------------------------------------------------------------------
 * FREE     null                          null(*)        null
 * LITERAL      bindings of the literal term  ref to term    null
 * LINK     null                          null           ref to a Binding representing the bound var
 * 
 * (*) In case of a variable, there is a method in Bindings that post-assigns the "term"
 *     member to point to the variable, this allows retrieving its name for reporting
 *     bindings to the application code.
 * </pre>
 */
public class Binding {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Binding.class);
    private static final boolean debug = logger.isDebugEnabled();

    // See description of fields in this class' Javadoc, and on getters.

    private BindingType type;
    private Object term;
    private Bindings literalBindings;
    private Binding link;

    // Ref to the variable associated to this binding in the "owning" Struct. This is solely
    // used for reporting and extracting the "original" variable name from solutions.
    // This field is not "functionally" needed
    private Var var;

    /**
     * New binding, for a (yet) free variable.
     */
    public Binding() {
        this.type = BindingType.FREE;
    }

    /**
     * Copy constructor.
     * 
     * @param toClone
     */
    public Binding(Binding toClone) {
        this.link = toClone.link;
        this.literalBindings = toClone.literalBindings;
        this.term = toClone.term;
        this.type = toClone.type;
        this.var = toClone.var;
    }

    /**
     * Factory method to create one "fake" binding to a literal.
     * 
     * @param theLiteral
     * @param theLiteralBindings
     * @return This is used to return a pair (Term, Bindings) where needed.
     */
    // TODO assess if needed - used only once
    public static Binding createLiteralBinding(Object theLiteral, Bindings theLiteralBindings) {
        final Binding binding = new Binding();
        binding.type = BindingType.LITERAL;
        binding.link = null;
        binding.term = theLiteral;
        binding.literalBindings = theLiteralBindings;
        binding.setVar(null);
        return binding;
    }

    /**
     * Bind this to a {@link Term}, may be a literal or another variable.
     * 
     * @param theTerm
     * @param theFrame When theTerm is a literal, here are its current value bindings
     * @return true if a binding was done, false otherwise. Caller needs to know if future un-binding will be needed.
     */
    public boolean bindTo(Object theTerm, Bindings theFrame) {
        if (!isFree()) {
            throw new PrologNonSpecificError("Should never attempt to re-bind a Binding that is not free!");
        }
        if (theTerm instanceof Var && !((Var) theTerm).isAnonymous()) {
            // Bind Var -> Var, see description at top of class
            final Var other = (Var) theTerm;
            final Binding targetBinding = other.bindingWithin(theFrame);
            if (targetBinding == this) {
                // Don't bind on oneself
                return false;
            }
            this.type = BindingType.LINK;
            this.term = null;
            this.literalBindings = null;
            this.link = targetBinding;
        } else {
            this.type = BindingType.LITERAL;
            this.term = theTerm;
            this.literalBindings = theFrame;
            this.link = null;
        }
        // Bound OK
        return true;
    }

    /**
     * Free the binding, i.e. revert a possibly bound variable to the {@value BindingType#FREE} state.
     */
    public void free() {
        this.type = BindingType.FREE;
        // The following is not functionally necessary but let's have values as documented (useful while debugging)
        this.term = null;
        this.literalBindings = null;
        this.link = null;
    }

    /**
     * Follow chains of linked bindings.
     * 
     * @return The last binding of a chain, or this instance if it is not {@link BindingType#LINK}. The result is guaranteed to satisfy
     *         either of {@link #isFree()} or {@link #isLiteral()}.
     */
    public Binding followLinks() {
        Binding result = this;
        // Maybe we should use isLink() and getters, but let's be efficient!
        while (result.type == BindingType.LINK) {
            result = result.link;
        }
        return result;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public BindingType getType() {
        return this.type;
    }

    /**
     * When {@link #getType()} is {@link BindingType#LITERAL}, the {@link #getTerm()} is a literal {@link Struct}, and this represents the
     * {@link Bindings} storing the content of those variables.
     * 
     * @return The {@link Bindings} associated to the Term from {@link #getTerm()}.
     */
    public Bindings getLiteralBindings() {
        return this.literalBindings;
    }

    /**
     * Reference to a bound term: for {@link BindingType#LITERAL}, this is the literal, for {@link BindingType#LINK}, it refers to the
     * {@link Term} of subclass {@link Var}.
     */
    public Object getTerm() {
        return this.term;
    }

    public Var getVar() {
        return this.var;
    }

    void setVar(Var theVar) {
        this.var = theVar;
    }

    public boolean isFree() {
        return this.type == BindingType.FREE;
    }

    public boolean isLiteral() {
        return this.type == BindingType.LITERAL;
    }

    // ---------------------------------------------------------------------------
    // Core java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getVar());
        sb.append(':');
        sb.append(this.type);

        switch (this.type) {
        case LITERAL:
            sb.append("->");
            sb.append(this.term.toString());
            if (debug) {
                sb.append('@');
                sb.append(Integer.toHexString(this.literalBindings.hashCode()));
            }
            break;
        case LINK:
            sb.append("->");
            sb.append(this.link);
            break;
        default:
            break;
        }
        return sb.toString();
    }

}
