/**
 * 
 */
package org.logic2j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.logic2j.model.symbol.Struct;

/**
 * @author Vincent Berthet
 */
public class ClauseProviderResolver {

	private static final List<ClauseProvider> EMPTY = Collections.emptyList();
  private final HashMap<String, List<ClauseProvider>> registre = new HashMap<String, List<ClauseProvider>>();
	
	public void register(String thePredicateKey, ClauseProvider provider){
	  List<ClauseProvider> setProvider = this.registre.get(thePredicateKey);
		if(setProvider == null){
			setProvider = new ArrayList<ClauseProvider>();
			this.registre.put(thePredicateKey, setProvider);
		}
		if (! setProvider.contains(provider)) {
		  setProvider.add(provider);
		}

	}
	
	public Iterable<ClauseProvider> find(Struct struct){
	  List<ClauseProvider> list = this.registre.get(struct.getPredicateIndicator());
		return  ((list == null) ? EMPTY : list);
	}
}
