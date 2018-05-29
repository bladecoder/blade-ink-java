package com.bladecoder.ink.runtime;

/**
 * NOTE: This is a structure in C#: all objects have to be finals.
 * When looking up content within the story (e.g. in Container.ContentAtPath),
 * the result is generally found, but if the story is modified, then when loading
 * up an old save state, then some old paths may still exist. In this case we
 * try to recover by finding an approximate result by working up the story hierarchy
 * in the path to find the closest valid container. Instead of crashing horribly,
 * we might see some slight oddness in the content, but hopefully it recovers!

 * @author rgarcia
 */
class SearchResult {
	public RTObject obj;
	public boolean approximate;
	
	public SearchResult() {
		
	}
	
	public SearchResult(SearchResult sr) {
		obj = sr.obj;
		approximate = sr.approximate;
	}

	public RTObject correctObj() {
		return approximate ? null : obj;
	}

	public Container getContainer() {
		return obj instanceof Container? (Container)obj:null; 
	}
}
