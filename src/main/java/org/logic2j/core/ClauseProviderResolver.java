/**
 * 
 */
package org.logic2j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.logic2j.core.model.Clause;
import org.logic2j.core.model.symbol.Struct;

/**
 * A resolver which will tell which {@link ClauseProvider}s are able to provide {@link Clause}s
 * for a given goal predicate.
 * This is a static registry.
 * This should be refactored in some way, maybe using DI or a more efficient way to obtain all
 * {@link Clause}s matching a given predicate.
 * @note Consider this as work in progress
 * @author Vincent Berthet
 */
public class ClauseProviderResolver {

	private static final List<ClauseProvider> EMPTY = Collections.emptyList();
	private final HashMap<String, List<ClauseProvider>> register = new HashMap<String, List<ClauseProvider>>();

	public void register(String thePredicateKey, ClauseProvider provider) {
		List<ClauseProvider> currentProviders = this.register.get(thePredicateKey);
		if (currentProviders == null) {
			currentProviders = new ArrayList<ClauseProvider>();
			this.register.put(thePredicateKey, currentProviders);
		}
		if (!currentProviders.contains(provider)) {
			currentProviders.add(provider);
		}
	}

	public Iterable<ClauseProvider> providersFor(Struct struct) {
		final String key = struct.getPredicateIndicator();
		final List<ClauseProvider> list = this.register.get(key);
		return list == null ? EMPTY : list;
	}
}
