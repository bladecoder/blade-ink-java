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
	     for (Entry<String, Integer> kv : otherDict.entrySet())
	         union.put(kv.getKey(), kv.getValue());
	     return union;
	 }
	 
	 public SetDictionary without (SetDictionary setToRemove) {
		 SetDictionary result = new SetDictionary (this);
	     for (String kv : setToRemove.keySet())
	         result.remove(kv);
	     return result;
	 }
}
