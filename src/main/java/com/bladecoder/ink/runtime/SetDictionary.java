package com.bladecoder.ink.runtime;

import java.util.HashMap;

//Helper class purely to make it less unweildly to type Dictionary<string, int> all the time.
@SuppressWarnings("serial")
public class SetDictionary extends HashMap<String, Integer> {
	 public SetDictionary () { 
		 
	 }
	 
	 public SetDictionary (HashMap<String, Integer> otherDict) {
		 super(otherDict);
	 }
	 
	 public SetDictionary unionWith (SetDictionary otherDict) {
		 SetDictionary union = new SetDictionary (this);
	     for (String key : otherDict.keySet())
	         union.put(key, otherDict.get(key));
	     
	     return union;
	 }
	 
	 public SetDictionary without (SetDictionary setToRemove) {
		 SetDictionary result = new SetDictionary (this);
	     for (String kv : setToRemove.keySet())
	         result.remove(kv);
	     
	     return result;
	 }
	 
	 public SetDictionary IntersectWith (SetDictionary otherDict) {
	     SetDictionary intersection = new SetDictionary ();
	     for (Entry<String, Integer> kv : this.entrySet()) {
	         if (otherDict.containsKey (kv.getKey()))
	             intersection.put (kv.getKey(), kv.getValue());
	     }
	     return intersection;
	 }
}
