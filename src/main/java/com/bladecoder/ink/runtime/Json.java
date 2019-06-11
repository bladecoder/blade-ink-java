package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.bladecoder.ink.runtime.ControlCommand.CommandType;

public class Json {

	public static List<RTObject> jArrayToRuntimeObjList(List<Object> jArray, boolean skipLast) throws Exception {
		int count = jArray.size();

		if (skipLast)
			count--;

		List<RTObject> list = new ArrayList<>(jArray.size());

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

	public static void writeDictionaryRuntimeObjs(SimpleJson.Writer writer, HashMap<String, RTObject> dictionary)
			throws Exception {
		writer.writeObjectStart();
		for (Entry<String, RTObject> keyVal : dictionary.entrySet()) {
			writer.writePropertyStart(keyVal.getKey());
			writeRuntimeObject(writer, keyVal.getValue());
			writer.writePropertyEnd();
		}
		writer.writeObjectEnd();
	}

	public static void writeListRuntimeObjs(SimpleJson.Writer writer, List<RTObject> list) throws Exception {
		writer.writeArrayStart();
		for (RTObject val : list) {
			writeRuntimeObject(writer, val);
		}
		writer.writeArrayEnd();
	}

	public static void WriteIntDictionary(SimpleJson.Writer writer, HashMap<String, Integer> dict) throws Exception {
		writer.writeObjectStart();

		for (Entry<String, Integer> keyVal : dict.entrySet())
			writer.writeProperty(keyVal.getKey(), keyVal.getValue());

		writer.writeObjectEnd();
	}

	public static void writeRuntimeObject(SimpleJson.Writer writer, RTObject obj) throws Exception {

		if (obj instanceof Container) {
			writeRuntimeContainer(writer, (Container) obj);
			return;
		}

		if (obj instanceof Divert) {
			Divert divert = (Divert) obj;
			String divTypeKey = "->";
			if (divert.isExternal())
				divTypeKey = "x()";
			else if (divert.getPushesToStack()) {
				if (divert.getStackPushType() == PushPopType.Function)
					divTypeKey = "f()";
				else if (divert.getStackPushType() == PushPopType.Tunnel)
					divTypeKey = "->t->";
			}

			String targetStr;
			if (divert.hasVariableTarget())
				targetStr = divert.getVariableDivertName();
			else
				targetStr = divert.getTargetPathString();

			writer.writeObjectStart();

			writer.writeProperty(divTypeKey, targetStr);

			if (divert.hasVariableTarget())
				writer.writeProperty("var", true);

			if (divert.isConditional())
				writer.writeProperty("c", true);

			if (divert.getExternalArgs() > 0)
				writer.writeProperty("exArgs", divert.getExternalArgs());

			writer.writeObjectEnd();
			return;
		}

		if (obj instanceof ChoicePoint) {
			ChoicePoint choicePoint = (ChoicePoint) obj;
			writer.writeObjectStart();
			writer.writeProperty("*", choicePoint.getPathStringOnChoice());
			writer.writeProperty("flg", choicePoint.getFlags());
			writer.writeObjectEnd();
			return;
		}

		if (obj instanceof IntValue) {
			IntValue intVal = (IntValue) obj;
			writer.write(intVal.value);
			return;
		}

		if (obj instanceof FloatValue) {
			FloatValue floatVal = (FloatValue) obj;

			writer.write(floatVal.value);
			return;
		}

		if (obj instanceof StringValue) {
			StringValue strVal = (StringValue) obj;
			if (strVal.isNewline())
				writer.write("\\n", false);
			else {
				writer.writeStringStart();
				writer.writeStringInner("^");
				writer.writeStringInner(strVal.value);
				writer.writeStringEnd();
			}
			return;
		}

		if (obj instanceof ListValue) {
			writeInkList(writer, (ListValue) obj);
			return;
		}

		if (obj instanceof DivertTargetValue) {
			DivertTargetValue divTargetVal = (DivertTargetValue) obj;
			writer.writeObjectStart();
			writer.writeProperty("^->", divTargetVal.value.getComponentsString());
			writer.writeObjectEnd();
			return;
		}

		if (obj instanceof VariablePointerValue) {
			VariablePointerValue varPtrVal = (VariablePointerValue) obj;
			writer.writeObjectStart();
			writer.writeProperty("^var", varPtrVal.value);
			writer.writeProperty("ci", varPtrVal.getContextIndex());
			writer.writeObjectEnd();
			return;
		}

		if (obj instanceof Glue) {
			writer.write("<>");
			return;
		}

		if (obj instanceof ControlCommand) {
			ControlCommand controlCmd = (ControlCommand) obj;
			writer.write(controlCommandNames[controlCmd.getCommandType().ordinal()]);
			return;
		}

		if (obj instanceof NativeFunctionCall) {
			NativeFunctionCall nativeFunc = (NativeFunctionCall) obj;
			String name = nativeFunc.getName();

			// Avoid collision with ^ used to indicate a string
			if (name == "^")
				name = "L^";

			writer.write(name);
			return;
		}

		// Variable reference
		VariableReference varRef = (VariableReference) obj;
		if (obj instanceof VariableReference) {
			writer.writeObjectStart();

			String readCountPath = varRef.getPathStringForCount();
			if (readCountPath != null) {
				writer.writeProperty("CNT?", readCountPath);
			} else {
				writer.writeProperty("VAR?", varRef.getName());
			}

			writer.writeObjectEnd();
			return;
		}

		// Variable assignment
		if (obj instanceof VariableAssignment) {
			VariableAssignment varAss = (VariableAssignment) obj;
			writer.writeObjectStart();

			String key = varAss.isGlobal() ? "VAR=" : "temp=";
			writer.writeProperty(key, varAss.getVariableName());

			// Reassignment?
			if (!varAss.isNewDeclaration())
				writer.writeProperty("re", true);

			writer.writeObjectEnd();

			return;
		}

		// Void
		if (obj instanceof Void) {
			writer.write("void");
			return;
		}

		// Tag
		if (obj instanceof Tag) {
			Tag tag = (Tag) obj;
			writer.writeObjectStart();
			writer.writeProperty("#", tag.getText());
			writer.writeObjectEnd();
			return;
		}

		// Used when serialising save state only

		if (obj instanceof Choice) {
			Choice choice = (Choice) obj;
			writeChoice(writer, choice);
			return;
		}

		throw new Exception("Failed to write runtime object to JSON: " + obj);
	}

	public static HashMap<String, RTObject> jObjectToHashMapRuntimeObjs(HashMap<String, Object> jRTObject)
			throws Exception {
		HashMap<String, RTObject> dict = new HashMap<>(jRTObject.size());

		for (Entry<String, Object> keyVal : jRTObject.entrySet()) {
			dict.put(keyVal.getKey(), jTokenToRuntimeObject(keyVal.getValue()));
		}

		return dict;
	}

	public static HashMap<String, Integer> jObjectToIntHashMap(HashMap<String, Object> jRTObject) throws Exception {
		HashMap<String, Integer> dict = new HashMap<>(jRTObject.size());

		for (Entry<String, Object> keyVal : jRTObject.entrySet()) {
			dict.put(keyVal.getKey(), (Integer) keyVal.getValue());
		}

		return dict;
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

	public static void writeRuntimeContainer(SimpleJson.Writer writer, Container container) throws Exception {
		writeRuntimeContainer(writer, container, false);
	}

	public static void writeRuntimeContainer(SimpleJson.Writer writer, Container container, boolean withoutName)
			throws Exception {
		writer.writeArrayStart();

		for (RTObject c : container.getContent())
			writeRuntimeObject(writer, c);

		// Container is always an array [...]
		// But the final element is always either:
		// - a dictionary containing the named content, as well as possibly
		// the key "#" with the count flags
		// - null, if neither of the above
		HashMap<String, RTObject> namedOnlyContent = container.getNamedOnlyContent();
		int countFlags = container.getCountFlags();
		boolean hasNameProperty = container.getName() != null && !withoutName;

		boolean hasTerminator = namedOnlyContent != null || countFlags > 0 || hasNameProperty;

		if (hasTerminator)
			writer.writeObjectStart();

		if (namedOnlyContent != null) {

			for (Entry<String, RTObject> namedContent : namedOnlyContent.entrySet()) {
				String name = namedContent.getKey();
				Container namedContainer = namedContent.getValue() instanceof Container
						? (Container) namedContent.getValue()
						: null;

				writer.writePropertyStart(name);
				writeRuntimeContainer(writer, namedContainer, true);
				writer.writePropertyEnd();
			}
		}

		if (countFlags > 0)
			writer.writeProperty("#f", countFlags);

		if (hasNameProperty)
			writer.writeProperty("#n", container.getName());

		if (hasTerminator)
			writer.writeObjectEnd();
		else
			writer.writeNull();

		writer.writeArrayEnd();

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
			HashMap<String, RTObject> namedOnlyContent = new HashMap<>(terminatingObj.size());
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
		choice.sourcePath = jObj.get("originalChoicePath").toString();
		choice.originalThreadIndex = (int) jObj.get("originalThreadIndex");
		choice.setPathStringOnChoice(jObj.get("targetPath").toString());
		return choice;
	}

	public static void writeChoice(SimpleJson.Writer writer, Choice choice) throws Exception {
		writer.writeObjectStart();
		writer.writeProperty("text", choice.getText());
		writer.writeProperty("index", choice.getIndex());
		writer.writeProperty("originalChoicePath", choice.sourcePath);
		writer.writeProperty("originalThreadIndex", choice.originalThreadIndex);
		writer.writeProperty("targetPath", choice.getPathStringOnChoice());
		writer.writeObjectEnd();
	}

	static void writeInkList(SimpleJson.Writer writer, ListValue listVal) throws Exception {
		InkList rawList = listVal.getValue();

		writer.writeObjectStart();

		writer.writePropertyStart("list");

		writer.writeObjectStart();

		for (Entry<InkListItem, Integer> itemAndValue : rawList.entrySet()) {
			InkListItem item = itemAndValue.getKey();
			int itemVal = itemAndValue.getValue();

			writer.writePropertyNameStart();
			writer.writePropertyNameInner(item.getOriginName() != null ? item.getOriginName() : "?");
			writer.writePropertyNameInner(".");
			writer.writePropertyNameInner(item.getItemName());
			writer.writePropertyNameEnd();

			writer.write(itemVal);

			writer.writePropertyEnd();
		}

		writer.writeObjectEnd();

		writer.writePropertyEnd();

		if (rawList.size() == 0 && rawList.getOriginNames() != null && rawList.getOriginNames().size() > 0) {
			writer.writePropertyStart("origins");
			writer.writeArrayStart();
			for (String name : rawList.getOriginNames())
				writer.write(name);
			writer.writeArrayEnd();
			writer.writePropertyEnd();
		}

		writer.writeObjectEnd();
	}

	public static HashMap<String, Object> listDefinitionsToJToken(ListDefinitionsOrigin origin) {
		HashMap<String, Object> result = new HashMap<>();
		for (ListDefinition def : origin.getLists()) {
			HashMap<String, Object> listDefJson = new HashMap<>();
			for (Entry<InkListItem, Integer> itemToVal : def.getItems().entrySet()) {
				InkListItem item = itemToVal.getKey();
				int val = itemToVal.getValue();
				listDefJson.put(item.getItemName(), val);
			}
			result.put(def.getName(), listDefJson);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static ListDefinitionsOrigin jTokenToListDefinitions(Object obj) {
		HashMap<String, Object> defsObj = (HashMap<String, Object>) obj;

		List<ListDefinition> allDefs = new ArrayList<>();

		for (Entry<String, Object> kv : defsObj.entrySet()) {
			String name = kv.getKey();
			HashMap<String, Object> listDefJson = (HashMap<String, Object>) kv.getValue();

			// Cast (string, object) to (string, int) for items
			HashMap<String, Integer> items = new HashMap<>();
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
