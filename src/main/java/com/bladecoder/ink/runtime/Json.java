package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.bladecoder.ink.runtime.ControlCommand.CommandType;

public class Json {
	public static <T extends RTObject> List<Object> listToJArray(List<T> serialisables) throws Exception {
		List<Object> jArray = new ArrayList<Object>();
		for (T s : serialisables) {
			jArray.add(runtimeRTObjectToJToken(s));
		}
		return jArray;
	}

	public static <T extends RTObject> List<T> jArrayToRuntimeObjList(List<Object> jArray, boolean skipLast)
			throws Exception {
		int count = jArray.size();

		if (skipLast)
			count--;

		List<T> list = new ArrayList<T>(jArray.size());

		for (int i = 0; i < count; i++) {
			Object jTok = jArray.get(i);
			T runtimeObj = (T) jTokenToRuntimeRTObject(jTok);
			list.add(runtimeObj);
		}

		return list;
	}

	public static <T extends RTObject> List<T> jArrayToRuntimeObjList(List<Object> jArray) throws Exception {
		return jArrayToRuntimeObjList(jArray, false);
	}

	// TODO
	// public static List<RTObject> jArrayToRuntimeObjList(List<RTObject>
	// jArray, boolean skipLast) throws Exception {
	// return JArrayToRuntimeObjList<RTObject>(jArray, skipLast);
	// }

	public static HashMap<String, Object> HashMapRuntimeObjsToJRTObject(HashMap<String, RTObject> hashMap)
			throws Exception {
		HashMap<String, Object> jsonObj = new HashMap<String, Object>();
		for (Entry<String, RTObject> keyVal : hashMap.entrySet()) {
			RTObject runtimeObj = keyVal.getValue();

			if (runtimeObj != null)
				jsonObj.put(keyVal.getKey(), runtimeRTObjectToJToken(runtimeObj));

		}
		return jsonObj;
	}

	public static HashMap<String, RTObject> jRTObjectToHashMapRuntimeObjs(HashMap<String, Object> jRTObject)
			throws Exception {
		HashMap<String, RTObject> dict = new HashMap<String, RTObject>(jRTObject.size());

		for (Entry<String, Object> keyVal : jRTObject.entrySet()) {
			dict.put(keyVal.getKey(), jTokenToRuntimeRTObject(keyVal.getValue()));
		}

		return dict;
	}

	public static HashMap<String, Integer> jRTObjectToIntHashMap(HashMap<String, Object> jRTObject) throws Exception {
		HashMap<String, Integer> dict = new HashMap<String, Integer>(jRTObject.size());

		for (Entry<String, Object> keyVal : jRTObject.entrySet()) {
			dict.put(keyVal.getKey(), (Integer) keyVal.getValue());
		}

		return dict;
	}

	public static HashMap<String, Object> intHashMapToJRTObject(HashMap<String, Integer> dict) throws Exception {
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
	public static RTObject jTokenToRuntimeRTObject(Object token) throws Exception {
		if (token instanceof Integer || token instanceof Float) {
			return Value.create(token);
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
				return new Glue(GlueType.Bidirectional);
			else if ("G<".equals(str))
				return new Glue(GlueType.Left);
			else if ("G>".equals(str))
				return new Glue(GlueType.Right);

			for (int i = 0; i < _controlCommandNames.length; ++i) {
				// Control commands (would looking up in a hash set be faster?)
				String cmdName = _controlCommandNames[i];
				if (str.equals(cmdName)) {
					return new ControlCommand(CommandType.values()[i + 1]);
				}

			}
			// Native functions
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
					varPtr.setcontextIndex((Integer) propValue);

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
				divert.setpushesToStack(pushesToStack);
				divert.stackPushType = divPushType;
				divert.setisExternal(external);
				String target = propValue.toString();

				propValue = obj.get("var");
				if (propValue != null) {
					divert.setvariableDivertName(target);
				} else {
					divert.settargetPathString(target);
				}

				propValue = obj.get("c");
				divert.setisConditional(propValue != null);

				if (external) {
					propValue = obj.get("exArgs");
					if (propValue != null) {
						divert.setexternalArgs((Integer) propValue);
					}

				}

				return divert;
			}

			// Choice
			propValue = obj.get("*");
			if (propValue != null) {
				ChoicePoint choice = new ChoicePoint();
				choice.setpathStringOnChoice(propValue.toString());
				propValue = obj.get("flg");
				if (propValue != null) {
					RefSupport<RTObject> refVar___11 = new RefSupport<RTObject>();
					choice.setflags((Integer) propValue);
					propValue = refVar___11.getValue();
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
					readCountVarRef.setpathStringForCount(propValue.toString());
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
				varAss.setisGlobal(isGlobalVar);
				return varAss;
			}

			if (obj.get("originalChoicePath") != null)
				return jRTObjectToChoice(obj);

		}

		// Array is always a Runtime.Container
		if (token instanceof List<?>) {
			return jArrayToContainer((List<Object>) token);
		}

		if (token == null)
			return null;

		throw new Exception("Failed to convert token to runtime RTObject: " + token);
	}

	public static Object runtimeRTObjectToJToken(RTObject obj) throws Exception {
		Container container = obj instanceof Container ? (Container) obj : (Container) null;

		if (container != null) {
			return containerToJArray(container);
		}

		Divert divert = obj instanceof Divert ? (Divert) obj : (Divert) null;
		if (divert != null) {
			String divTypeKey = "->";
			if (divert.getisExternal())
				divTypeKey = "x()";
			else if (divert.getpushesToStack()) {
				if (divert.stackPushType == PushPopType.Function)
					divTypeKey = "f()";
				else if (divert.stackPushType == PushPopType.Tunnel)
					divTypeKey = "->t->";

			}

			String targetStr = new String();
			if (divert.gethasVariableTarget())
				targetStr = divert.getvariableDivertName();
			else
				targetStr = divert.gettargetPathString();
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put(divTypeKey, targetStr);
			if (divert.gethasVariableTarget())
				jObj.put("var", true);

			if (divert.getisConditional())
				jObj.put("c", true);

			if (divert.getexternalArgs() > 0)
				jObj.put("exArgs", divert.getexternalArgs());

			return jObj;
		}

		ChoicePoint choicePoint = obj instanceof ChoicePoint ? (ChoicePoint) obj : (ChoicePoint) null;
		if (choicePoint != null) {
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put("*", choicePoint.getpathStringOnChoice());
			jObj.put("flg", choicePoint.getflags());
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
			if (strVal.getisNewline())
				return "\n";
			else
				return "^" + strVal.value;
		}

		DivertTargetValue divTargetVal = obj instanceof DivertTargetValue ? (DivertTargetValue) obj
				: (DivertTargetValue) null;
		if (divTargetVal != null) {
			HashMap<String, Object> divTargetJsonObj = new HashMap<String, Object>();
			divTargetJsonObj.put("^->", divTargetVal.value.getcomponentsString());
			return divTargetJsonObj;
		}

		VariablePointerValue varPtrVal = obj instanceof VariablePointerValue ? (VariablePointerValue) obj
				: (VariablePointerValue) null;
		if (varPtrVal != null) {
			HashMap<String, Object> varPtrJsonObj = new HashMap<String, Object>();
			varPtrJsonObj.put("^var", varPtrVal.value);
			varPtrJsonObj.put("ci", varPtrVal.getcontextIndex());
			return varPtrJsonObj;
		}

		Glue glue = obj instanceof Glue ? (Glue) obj : (Glue) null;
		if (glue != null) {
			if (glue.getisBi())
				return "<>";
			else if (glue.getisLeft())
				return "G<";
			else
				return "G>";
		}

		ControlCommand controlCmd = obj instanceof ControlCommand ? (ControlCommand) obj : (ControlCommand) null;
		if (controlCmd != null) {
			return _controlCommandNames[((Enum) controlCmd.getcommandType()).ordinal()];
		}

		NativeFunctionCall nativeFunc = obj instanceof NativeFunctionCall ? (NativeFunctionCall) obj
				: (NativeFunctionCall) null;
		if (nativeFunc != null)
			return nativeFunc.getname();

		// Variable reference
		VariableReference varRef = obj instanceof VariableReference ? (VariableReference) obj
				: (VariableReference) null;
		if (varRef != null) {
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			String readCountPath = varRef.getpathStringForCount();
			if (readCountPath != null) {
				jObj.put("CNT?", readCountPath);
			} else {
				jObj.put("VAR?", varRef.getname());
			}
			return jObj;
		}

		// Variable assignment
		VariableAssignment varAss = obj instanceof VariableAssignment ? (VariableAssignment) obj
				: (VariableAssignment) null;
		if (varAss != null) {
			String key = varAss.getisGlobal() ? "VAR=" : "temp=";
			HashMap<String, Object> jObj = new HashMap<String, Object>();
			jObj.put(key, varAss.getvariableName());
			// Reassignment?
			if (!varAss.getisNewDeclaration())
				jObj.put("re", true);

			return jObj;
		}

		Void voidObj = obj instanceof Void ? (Void) obj : (Void) null;
		if (voidObj != null)
			return "void";

		// Used when serialising save state only
		Choice choice = obj instanceof Choice ? (Choice) obj : (Choice) null;
		if (choice != null)
			return choiceToJRTObject(choice);

		throw new Exception("Failed to convert runtime RTObject to Json token: " + obj);
	}

	static List<Object> containerToJArray(Container container) throws Exception {
		List<Object> jArray = listToJArray(container.getcontent());

		// Container is always an array [...]
		// But the final element is always either:
		// - a HashMap containing the named content, as well as possibly
		// the key "#" with the count flags
		// - null, if neither of the above
		HashMap<String, RTObject> namedOnlyContent = container.getnamedOnlyContent();
		int countFlags = container.getcountFlags();
		if (namedOnlyContent != null && namedOnlyContent.size() > 0 || countFlags > 0 || container.getname() != null) {
			HashMap<String, Object> terminatingObj = new HashMap<String, Object>();
			if (namedOnlyContent != null) {
				terminatingObj = HashMapRuntimeObjsToJRTObject(namedOnlyContent);
				for (Entry<String, Object> namedContentObj : terminatingObj.entrySet()) {
					// Strip redundant names from containers if necessary
					List<Object> subContainerJArray = namedContentObj.getValue() instanceof List<?>
							? (List<Object>) namedContentObj.getValue() : (List<Object>) null;
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

			if (container.getname() != null)
				terminatingObj.put("#n", container.getname());

			jArray.add(terminatingObj);
		} else {
			// Add null terminator to indicate that there's no HashMap
			jArray.add(null);
		}
		return jArray;
	}

	static Container jArrayToContainer(List<Object> jArray) throws Exception {
		Container container = new Container();
		container.setcontent(jArrayToRuntimeObjList(jArray, true));
		// Final RTObject in the array is always a combination of
		// - named content
		// - a "#" key with the countFlags
		// (if either exists at all, otherwise null)
		HashMap<String, Object> terminatingObj = (HashMap<String, Object>) jArray.get(jArray.size() - 1);
		if (terminatingObj != null) {
			HashMap<String, RTObject> namedOnlyContent = new HashMap<String, RTObject>(terminatingObj.size());
			for (Entry<String, Object> keyVal : terminatingObj.entrySet()) {
				if ("#f".equals(keyVal.getKey())) {
					container.setcountFlags((int) keyVal.getValue());
				} else if ("#n".equals(keyVal.getKey())) {
					container.setname(keyVal.getValue().toString());
				} else {
					RTObject namedContentItem = jTokenToRuntimeRTObject(keyVal.getValue());
					Container namedSubContainer = namedContentItem instanceof Container ? (Container) namedContentItem
							: (Container) null;
					if (namedSubContainer != null)
						namedSubContainer.setname(keyVal.getKey());

					namedOnlyContent.put(keyVal.getKey(), namedContentItem);
				}
			}
			container.setnamedOnlyContent(namedOnlyContent);
		}

		return container;
	}

	static Choice jRTObjectToChoice(HashMap<String, Object> jObj) throws Exception {
		Choice choice = new Choice();
		choice.settext(jObj.get("text").toString());
		choice.setindex((int) jObj.get("index"));
		choice.originalChoicePath = jObj.get("originalChoicePath").toString();
		choice.originalThreadIndex = (int) jObj.get("originalThreadIndex");
		return choice;
	}

	static HashMap<String, Object> choiceToJRTObject(Choice choice) throws Exception {
		HashMap<String, Object> jObj = new HashMap<String, Object>();
		jObj.put("text", choice.gettext());
		jObj.put("index", choice.getindex());
		jObj.put("originalChoicePath", choice.originalChoicePath);
		jObj.put("originalThreadIndex", choice.originalThreadIndex);

		return jObj;
	}

	static String[] _controlCommandNames;

	static {
		_controlCommandNames = new String[CommandType.values().length - 1];
		_controlCommandNames[CommandType.EvalStart.ordinal() - 1] = "ev";
		_controlCommandNames[CommandType.EvalOutput.ordinal() - 1] = "out";
		_controlCommandNames[CommandType.EvalEnd.ordinal() - 1] = "/ev";
		_controlCommandNames[CommandType.Duplicate.ordinal() - 1] = "du";
		_controlCommandNames[CommandType.PopEvaluatedValue.ordinal() - 1] = "pop";
		_controlCommandNames[CommandType.PopFunction.ordinal() - 1] = "~ret";
		_controlCommandNames[CommandType.PopTunnel.ordinal() - 1] = "->->";
		_controlCommandNames[CommandType.BeginString.ordinal() - 1] = "str";
		_controlCommandNames[CommandType.EndString.ordinal() - 1] = "/str";
		_controlCommandNames[CommandType.NoOp.ordinal() - 1] = "nop";
		_controlCommandNames[CommandType.ChoiceCount.ordinal() - 1] = "choiceCnt";
		_controlCommandNames[CommandType.TurnsSince.ordinal() - 1] = "turns";
		_controlCommandNames[CommandType.VisitIndex.ordinal() - 1] = "visit";
		_controlCommandNames[CommandType.SequenceShuffleIndex.ordinal() - 1] = "seq";
		_controlCommandNames[CommandType.StartThread.ordinal() - 1] = "thread";
		_controlCommandNames[CommandType.Done.ordinal() - 1] = "done";
		_controlCommandNames[CommandType.End.ordinal() - 1] = "end";

		for (int i = 0; i < CommandType.values().length - 1; ++i) {
			if (_controlCommandNames[i] == null)
				throw new ExceptionInInitializerError("Control command not accounted for in serialisation");
		}

	}
}
