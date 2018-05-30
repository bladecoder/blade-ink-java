package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.bladecoder.ink.runtime.ControlCommand.CommandType;

public class Json {
	public static <T extends RTObject> List<Object> listToJArray(List<T> serialisables) throws Exception {
		List<Object> jArray = new ArrayList<Object>();

		for (RTObject s : serialisables) {
			jArray.add(runtimeObjectToJToken(s));
		}

		return jArray;
	}

	public static List<RTObject> jArrayToRuntimeObjList(List<Object> jArray, boolean skipLast) throws Exception {
		int count = jArray.size();

		if (skipLast)
			count--;

		List<RTObject> list = new ArrayList<RTObject>(jArray.size());

		for (int i = 0; i < count; i++) {
			Object jTok = jArray.get(i);
			RTObject runtimeObj = jTokenToRuntimeObject(jTok);
			list.add(runtimeObj);
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public static <T extends RTObject> List<T> jArrayToRuntimeObjList(List<Object> jArray) throws Exception {
		return (List<T>) jArrayToRuntimeObjList(jArray, false);
	}

	public static HashMap<String, Object> hashMapRuntimeObjsToJObject(HashMap<String, RTObject> hashMap)
			throws Exception {
		HashMap<String, Object> jsonObj = new HashMap<String, Object>();
		for (Entry<String, RTObject> keyVal : hashMap.entrySet()) {
			RTObject runtimeObj = keyVal.getValue();

			if (runtimeObj != null)
				jsonObj.put(keyVal.getKey(), runtimeObjectToJToken(runtimeObj));

		}
		return jsonObj;
	}

	public static HashMap<String, RTObject> jObjectToHashMapRuntimeObjs(HashMap<String, Object> jRTObject)
			throws Exception {
		HashMap<String, RTObject> dict = new HashMap<String, RTObject>(jRTObject.size());

		for (Entry<String, Object> keyVal : jRTObject.entrySet()) {
			dict.put(keyVal.getKey(), jTokenToRuntimeObject(keyVal.getValue()));
		}

		return dict;
	}

	public static HashMap<String, Integer> jObjectToIntHashMap(HashMap<String, Object> jRTObject) throws Exception {
		HashMap<String, Integer> dict = new HashMap<String, Integer>(jRTObject.size());

		for (Entry<String, Object> keyVal : jRTObject.entrySet()) {
			dict.put(keyVal.getKey(), (Integer) keyVal.getValue());
		}

		return dict;
	}

	public static HashMap<String, Object> intHashMapToJObject(HashMap<String, Integer> dict) throws Exception {
		HashMap<String, Object> jObj = new HashMap<String, Object>();

		for (Entry<String, Integer> keyVal : dict.entrySet()) {
			jObj.put(keyVal.getKey(), keyVal.getValue());
		}

		return jObj;
	}

	// ----------------------
	// JSON ENCODING SCHEME
	// ----------------------
	//
	// Glue: "<>", "G<", "G>"
	//
	// ControlCommand: "ev", "out", "/ev", "du" "pop", "->->", "~ret", "str",
	// "/str", "nop",
	// "choiceCnt", "turns", "visit", "seq", "thread", "done", "end"
	//
	// NativeFunction: "+", "-", "/", "*", "%" "~", "==", ">", "<", ">=", "<=",
	// "!=", "!"... etc
	//
	// Void: "void"
	//
	// Value: "^string value", "^^string value beginning with ^"
	// 5, 5.2
	// {"^->": "path.target"}
	// {"^var": "varname", "ci": 0}
	//
	// Container: [...]
	// [...,
	// {
	// "subContainerName": ...,
	// "#f": 5, // flags
	// "#n": "containerOwnName" // only if not redundant
	// }
	// ]
	//
	// Divert: {"->": "path.target", "c": true }
	// {"->": "path.target", "var": true}
	// {"f()": "path.func"}
	// {"->t->": "path.tunnel"}
	// {"x()": "externalFuncName", "exArgs": 5}
	//
	// Var Assign: {"VAR=": "varName", "re": true} // reassignment
	// {"temp=": "varName"}
	//
	// Var ref: {"VAR?": "varName"}
	// {"CNT?": "stitch name"}
	//
	// ChoicePoint: {"*": pathString,
	// "flg": 18 }
	//
	// Choice: Nothing too clever, it's only used in the save state,
	// there's not likely to be many of them.
	//
	// Tag: {"#": "the tag text"}
	@SuppressWarnings("unchecked")
	public static RTObject jTokenToRuntimeObject(Object token) throws Exception {
		if (token instanceof Integer || token instanceof Float) {
			return AbstractValue.create(token);
		}

		if (token instanceof String) {
			String str = (String) token;
			// String value
			char firstChar = str.charAt(0);
			if (firstChar == '^')
				return new StringValue(str.substring(1));
			else if (firstChar == '\n' && str.length() == 1)
				return new StringValue("\n");

			// Glue
			if ("<>".equals(str))
				return new Glue();

			for (int i = 0; i < controlCommandNames.length; ++i) {
				// Control commands (would looking up in a hash set be faster?)
				String cmdName = controlCommandNames[i];
				if (str.equals(cmdName)) {
					return new ControlCommand(CommandType.values()[i + 1]);
				}

			}

			// Native functions
			// "^" conflicts with the way to identify strings, so now
			// we know it's not a string, we can convert back to the proper
			// symbol for the operator.
			if ("L^".equals(str))
				str = "^";
			if (NativeFunctionCall.callExistsWithName(str))
				return NativeFunctionCall.callWithName(str);

			// Pop
			if ("->->".equals(str))
				return ControlCommand.popTunnel();
			else if ("~ret".equals(str))
				return ControlCommand.popFunction();

			// Void
			if ("void".equals(str))
				return new Void();

		}

		if (token instanceof HashMap<?, ?>) {
			HashMap<String, Object> obj = (HashMap<String, Object>) token;

			Object propValue;

			// Divert target value to path
			propValue = obj.get("^->");

			if (propValue != null) {
				return new DivertTargetValue(new Path((String) propValue));
			}

			// VariablePointerValue
			propValue = obj.get("^var");
			if (propValue != null) {
				VariablePointerValue varPtr = new VariablePointerValue((String) propValue);

				propValue = obj.get("ci");

				if (propValue != null)
					varPtr.setContextIndex((Integer) propValue);

				return varPtr;
			}

			// Divert
			boolean isDivert = false;
			boolean pushesToStack = false;
			PushPopType divPushType = PushPopType.Function;
			boolean external = false;

			propValue = obj.get("->");
			if (propValue != null) {
				isDivert = true;
			} else {
				propValue = obj.get("f()");
				if (propValue != null) {
					isDivert = true;
					pushesToStack = true;
					divPushType = PushPopType.Function;
				} else {
					propValue = obj.get("->t->");
					if (propValue != null) {
						isDivert = true;
						pushesToStack = true;
						divPushType = PushPopType.Tunnel;
					} else {
						propValue = obj.get("x()");
						if (propValue != null) {
							isDivert = true;
							external = true;
							pushesToStack = false;
							divPushType = PushPopType.Function;
						}

					}
				}
			}

			if (isDivert) {
				Divert divert = new Divert();
				divert.setPushesToStack(pushesToStack);
				divert.setStackPushType(divPushType);
				divert.setExternal(external);
				String target = propValue.toString();

				propValue = obj.get("var");
				if (propValue != null) {
					divert.setVariableDivertName(target);
				} else {
					divert.setTargetPathString(target);
				}

				propValue = obj.get("c");
				divert.setConditional(propValue != null);

				if (external) {
					propValue = obj.get("exArgs");
					if (propValue != null) {
						divert.setExternalArgs((Integer) propValue);
					}

				}

				return divert;
			}

			// Choice
			propValue = obj.get("*");
			if (propValue != null) {
				ChoicePoint choice = new ChoicePoint();
				choice.setPathStringOnChoice(propValue.toString());
				propValue = obj.get("flg");

				if (propValue != null) {
					choice.setFlags((Integer) propValue);
				}

				return choice;
			}

			// Variable reference
			propValue = obj.get("VAR?");
			if (propValue != null) {
				return new VariableReference(propValue.toString());
			} else {
				propValue = obj.get("CNT?");
				if (propValue != null) {
					VariableReference readCountVarRef = new VariableReference();
					readCountVarRef.setPathStringForCount(propValue.toString());
					return readCountVarRef;
				}

			}
			// Variable assignment
			boolean isVarAss = false;
			boolean isGlobalVar = false;

			propValue = obj.get("VAR=");
			if (propValue != null) {
				isVarAss = true;
				isGlobalVar = true;
			} else {
				propValue = obj.get("temp=");
				if (propValue != null) {
					isVarAss = true;
					isGlobalVar = false;
				}

			}
			if (isVarAss) {
				String varName = propValue.toString();
				propValue = obj.get("re");
				boolean isNewDecl = propValue == null;

				VariableAssignment varAss = new VariableAssignment(varName, isNewDecl);
				varAss.setIsGlobal(isGlobalVar);
				return varAss;
			}

			// Tag
			propValue = obj.get("#");
			if (propValue != null) {
				return new Tag((String) propValue);
			}

			// List value
			propValue = obj.get("list");

			if (propValue != null) {
				HashMap<String, Object> listContent = (HashMap<String, Object>) propValue;
				InkList rawList = new InkList();

				propValue = obj.get("origins");

				if (propValue != null) {
					List<String> namesAsObjs = (List<String>) propValue;

					rawList.setInitialOriginNames(namesAsObjs);
				}

				for (Entry<String, Object> nameToVal : listContent.entrySet()) {
					InkListItem item = new InkListItem(nameToVal.getKey());
					int val = (int) nameToVal.getValue();
					rawList.put(item, val);
				}

				return new ListValue(rawList);
			}

			// Used when serialising save state only
			if (obj.get("originalChoicePath") != null)
				return jObjectToChoice(obj);

		}

		// Array is always a Runtime.Container
		if (token instanceof List<?>) {
			return jArrayToContainer((List<Object>) token);
		}

		if (token == null)
			return null;

		throw new Exception("Failed to convert token to runtime RTObject: " + token);
	}

	public static Object runtimeObjectToJToken(RTObject obj) throws Exception {
		Container container = obj instanceof Container ? (Container) obj : (Container) null;

		if (container != null) {
			return containerToJArray(container);
		}

		Divert divert = obj instanceof Divert ? (Divert) obj : (Divert) null;
		if (divert != null) {
			String divTypeKey = "->";
			if (divert.isExternal())
				divTypeKey = "x()";
			else if (divert.getPushesToStack()) {
				if (divert.getStackPushType() == PushPopType.Function)
					divTypeKey = "f()";
				else if (divert.getStackPushType() == PushPopType.Tunnel)
					divTypeKey = "->t->";

			}

			String targetStr = new String();
			if (divert.hasVariableTarget())
				targetStr = divert.getVariableDivertName();
			else
				targetStr = divert.getTargetPathString();
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put(divTypeKey, targetStr);
			if (divert.hasVariableTarget())
				jObj.put("var", true);

			if (divert.isConditional())
				jObj.put("c", true);

			if (divert.getExternalArgs() > 0)
				jObj.put("exArgs", divert.getExternalArgs());

			return jObj;
		}

		ChoicePoint choicePoint = obj instanceof ChoicePoint ? (ChoicePoint) obj : (ChoicePoint) null;
		if (choicePoint != null) {
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put("*", choicePoint.getPathStringOnChoice());
			jObj.put("flg", choicePoint.getFlags());
			return jObj;
		}

		IntValue intVal = obj instanceof IntValue ? (IntValue) obj : (IntValue) null;
		if (intVal != null)
			return intVal.value;

		FloatValue floatVal = obj instanceof FloatValue ? (FloatValue) obj : (FloatValue) null;
		if (floatVal != null)
			return floatVal.value;

		StringValue strVal = obj instanceof StringValue ? (StringValue) obj : (StringValue) null;
		if (strVal != null) {
			if (strVal.isNewline())
				return "\n";
			else
				return "^" + strVal.value;
		}

		ListValue listVal = null;

		if (obj instanceof ListValue)
			listVal = (ListValue) obj;

		if (listVal != null) {
			return inkListToJObject(listVal);
		}

		DivertTargetValue divTargetVal = obj instanceof DivertTargetValue ? (DivertTargetValue) obj
				: (DivertTargetValue) null;
		if (divTargetVal != null) {
			HashMap<String, Object> divTargetJsonObj = new HashMap<String, Object>();
			divTargetJsonObj.put("^->", divTargetVal.value.getComponentsString());
			return divTargetJsonObj;
		}

		VariablePointerValue varPtrVal = obj instanceof VariablePointerValue ? (VariablePointerValue) obj
				: (VariablePointerValue) null;
		if (varPtrVal != null) {
			HashMap<String, Object> varPtrJsonObj = new HashMap<String, Object>();
			varPtrJsonObj.put("^var", varPtrVal.value);
			varPtrJsonObj.put("ci", varPtrVal.getContextIndex());
			return varPtrJsonObj;
		}

		Glue glue = obj instanceof Glue ? (Glue) obj : (Glue) null;
		if (glue != null) {
			return "<>";
		}

		ControlCommand controlCmd = obj instanceof ControlCommand ? (ControlCommand) obj : (ControlCommand) null;
		if (controlCmd != null) {
			return controlCommandNames[controlCmd.getCommandType().ordinal()];
		}

		NativeFunctionCall nativeFunc = obj instanceof NativeFunctionCall ? (NativeFunctionCall) obj
				: (NativeFunctionCall) null;
		if (nativeFunc != null) {
			String name = nativeFunc.getName();

			// Avoid collision with ^ used to indicate a string
			if ("^".equals(name))
				name = "L^";
			return name;
		}

		// Variable reference
		VariableReference varRef = obj instanceof VariableReference ? (VariableReference) obj
				: (VariableReference) null;
		if (varRef != null) {
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			String readCountPath = varRef.getPathStringForCount();
			if (readCountPath != null) {
				jObj.put("CNT?", readCountPath);
			} else {
				jObj.put("VAR?", varRef.getName());
			}
			return jObj;
		}

		// Variable assignment
		VariableAssignment varAss = obj instanceof VariableAssignment ? (VariableAssignment) obj
				: (VariableAssignment) null;
		if (varAss != null) {
			String key = varAss.isGlobal() ? "VAR=" : "temp=";
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put(key, varAss.getVariableName());
			// Reassignment?
			if (!varAss.isNewDeclaration())
				jObj.put("re", true);

			return jObj;
		}

		// Void
		Void voidObj = obj instanceof Void ? (Void) obj : (Void) null;
		if (voidObj != null)
			return "void";

		// Tag
		Tag tag = obj instanceof Tag ? (Tag) obj : (Tag) null;

		if (tag != null) {
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put("#", tag.getText());
			return jObj;
		}

		// Used when serialising save state only
		Choice choice = obj instanceof Choice ? (Choice) obj : (Choice) null;
		if (choice != null)
			return choiceToJObject(choice);

		throw new Exception("Failed to convert runtime RTObject to Json token: " + obj);
	}

	@SuppressWarnings("unchecked")
	static List<Object> containerToJArray(Container container) throws Exception {
		List<Object> jArray = listToJArray(container.getContent());

		// Container is always an array [...]
		// But the final element is always either:
		// - a HashMap containing the named content, as well as possibly
		// the key "#f" with the count flags
		// - null, if neither of the above
		HashMap<String, RTObject> namedOnlyContent = container.getNamedOnlyContent();
		int countFlags = container.getCountFlags();
		if (namedOnlyContent != null && namedOnlyContent.size() > 0 || countFlags > 0 || container.getName() != null) {
			HashMap<String, Object> terminatingObj = new HashMap<String, Object>();
			if (namedOnlyContent != null) {
				terminatingObj = hashMapRuntimeObjsToJObject(namedOnlyContent);
				for (Entry<String, Object> namedContentObj : terminatingObj.entrySet()) {
					// Strip redundant names from containers if necessary
					List<Object> subContainerJArray = namedContentObj.getValue() instanceof List<?>
							? (List<Object>) namedContentObj.getValue()
							: (List<Object>) null;

					if (subContainerJArray != null) {
						HashMap<String, Object> attrJObj = subContainerJArray
								.get(subContainerJArray.size() - 1) instanceof HashMap<?, ?>
										? (HashMap<String, Object>) subContainerJArray
												.get(subContainerJArray.size() - 1)
										: (HashMap<String, Object>) null;
						if (attrJObj != null) {
							attrJObj.remove("#n");
							if (attrJObj.size() == 0)
								subContainerJArray.set(subContainerJArray.size() - 1, null);

						}

					}

				}
			} else
				terminatingObj = new HashMap<String, Object>();
			if (countFlags > 0)
				terminatingObj.put("#f", countFlags);

			if (container.getName() != null)
				terminatingObj.put("#n", container.getName());

			jArray.add(terminatingObj);
		} else {
			// Add null terminator to indicate that there's no HashMap
			jArray.add(null);
		}
		return jArray;
	}

	@SuppressWarnings("unchecked")
	static Container jArrayToContainer(List<Object> jArray) throws Exception {
		Container container = new Container();
		container.setContent(jArrayToRuntimeObjList(jArray, true));
		// Final RTObject in the array is always a combination of
		// - named content
		// - a "#" key with the countFlags
		// (if either exists at all, otherwise null)
		HashMap<String, Object> terminatingObj = (HashMap<String, Object>) jArray.get(jArray.size() - 1);
		if (terminatingObj != null) {
			HashMap<String, RTObject> namedOnlyContent = new HashMap<String, RTObject>(terminatingObj.size());
			for (Entry<String, Object> keyVal : terminatingObj.entrySet()) {
				if ("#f".equals(keyVal.getKey())) {
					container.setCountFlags((int) keyVal.getValue());
				} else if ("#n".equals(keyVal.getKey())) {
					container.setName(keyVal.getValue().toString());
				} else {
					RTObject namedContentItem = jTokenToRuntimeObject(keyVal.getValue());
					Container namedSubContainer = namedContentItem instanceof Container ? (Container) namedContentItem
							: (Container) null;
					if (namedSubContainer != null)
						namedSubContainer.setName(keyVal.getKey());

					namedOnlyContent.put(keyVal.getKey(), namedContentItem);
				}
			}
			container.setNamedOnlyContent(namedOnlyContent);
		}

		return container;
	}

	static Choice jObjectToChoice(HashMap<String, Object> jObj) throws Exception {
		Choice choice = new Choice();
		choice.setText(jObj.get("text").toString());
		choice.setIndex((int) jObj.get("index"));
		choice.sourcePath  = jObj.get("originalChoicePath").toString();
		choice.originalThreadIndex = (int) jObj.get("originalThreadIndex");
		choice.setPathStringOnChoice(jObj.get("targetPath").toString());
		return choice;
	}

	static HashMap<String, Object> choiceToJObject(Choice choice) throws Exception {
		HashMap<String, Object> jObj = new HashMap<String, Object>();
		jObj.put("text", choice.getText());
		jObj.put("index", choice.getIndex());
		jObj.put("originalChoicePath", choice.sourcePath);
		jObj.put("originalThreadIndex", choice.originalThreadIndex);
		jObj.put("targetPath", choice.getPathStringOnChoice());

		return jObj;
	}

	static HashMap<String, Object> inkListToJObject(ListValue listVal) {
		InkList rawList = listVal.value;

		HashMap<String, Object> dict = new HashMap<String, Object>();

		HashMap<String, Object> content = new HashMap<String, Object>();

		for (Entry<InkListItem, Integer> itemAndValue : rawList.entrySet()) {
			InkListItem item = itemAndValue.getKey();
			int val = itemAndValue.getValue();
			content.put(item.toString(), val);
		}

		dict.put("list", content);

		if (rawList.size() == 0 && rawList.getOriginNames().size() > 0) {
			dict.put("origins", rawList.getOriginNames());
		}

		return dict;
	}

	public static HashMap<String, Object> listDefinitionsToJToken(ListDefinitionsOrigin origin) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		for (ListDefinition def : origin.getLists()) {
			HashMap<String, Object> listDefJson = new HashMap<String, Object>();
			for (Entry<InkListItem, Integer> itemToVal : def.getItems().entrySet()) {
				InkListItem item = itemToVal.getKey();
				int val = itemToVal.getValue();
				listDefJson.put(item.getItemName(), (Object) val);
			}
			result.put(def.getName(), listDefJson);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static ListDefinitionsOrigin jTokenToListDefinitions(Object obj) {
		HashMap<String, Object> defsObj = (HashMap<String, Object>) obj;

		List<ListDefinition> allDefs = new ArrayList<ListDefinition>();

		for (Entry<String, Object> kv : defsObj.entrySet()) {
			String name = (String) kv.getKey();
			HashMap<String, Object> listDefJson = (HashMap<String, Object>) kv.getValue();

			// Cast (string, object) to (string, int) for items
			HashMap<String, Integer> items = new HashMap<String, Integer>();
			for (Entry<String, Object> nameValue : listDefJson.entrySet())
				items.put(nameValue.getKey(), (int) nameValue.getValue());

			ListDefinition def = new ListDefinition(name, items);
			allDefs.add(def);
		}

		return new ListDefinitionsOrigin(allDefs);
	}

	private final static String[] controlCommandNames;

	static {
		controlCommandNames = new String[CommandType.values().length - 1];
		controlCommandNames[CommandType.EvalStart.ordinal() - 1] = "ev";
		controlCommandNames[CommandType.EvalOutput.ordinal() - 1] = "out";
		controlCommandNames[CommandType.EvalEnd.ordinal() - 1] = "/ev";
		controlCommandNames[CommandType.Duplicate.ordinal() - 1] = "du";
		controlCommandNames[CommandType.PopEvaluatedValue.ordinal() - 1] = "pop";
		controlCommandNames[CommandType.PopFunction.ordinal() - 1] = "~ret";
		controlCommandNames[CommandType.PopTunnel.ordinal() - 1] = "->->";
		controlCommandNames[CommandType.BeginString.ordinal() - 1] = "str";
		controlCommandNames[CommandType.EndString.ordinal() - 1] = "/str";
		controlCommandNames[CommandType.NoOp.ordinal() - 1] = "nop";
		controlCommandNames[CommandType.ChoiceCount.ordinal() - 1] = "choiceCnt";
		controlCommandNames[CommandType.Turns.ordinal() - 1] = "turn";
		controlCommandNames[CommandType.TurnsSince.ordinal() - 1] = "turns";
		controlCommandNames[CommandType.ReadCount.ordinal() - 1] = "readc";
		controlCommandNames[CommandType.Random.ordinal() - 1] = "rnd";
		controlCommandNames[CommandType.SeedRandom.ordinal() - 1] = "srnd";
		controlCommandNames[CommandType.VisitIndex.ordinal() - 1] = "visit";
		controlCommandNames[CommandType.SequenceShuffleIndex.ordinal() - 1] = "seq";
		controlCommandNames[CommandType.StartThread.ordinal() - 1] = "thread";
		controlCommandNames[CommandType.Done.ordinal() - 1] = "done";
		controlCommandNames[CommandType.End.ordinal() - 1] = "end";
		controlCommandNames[CommandType.ListFromInt.ordinal() - 1] = "listInt";
		controlCommandNames[CommandType.ListRange.ordinal() - 1] = "range";
		controlCommandNames[CommandType.ListRandom.ordinal() - 1] = "lrnd";

		for (int i = 0; i < CommandType.values().length - 1; ++i) {
			if (controlCommandNames[i] == null)
				throw new ExceptionInInitializerError("Control command not accounted for in serialisation");
		}

	}
}
