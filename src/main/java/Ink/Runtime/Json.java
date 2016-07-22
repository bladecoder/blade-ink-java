package Ink.Runtime;

import java.util.List;

public class Json   
{
    public static <T extends RTObject>List<RTObject> listToJArray(List<T> serialisables) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jArray = new List<RTObject>();
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ s : serialisables)
        {
            jArray.Add(RuntimeRTObjectToJToken(s));
        }
        return jArray;
    }

    public static <T extends RTObject>List<T> jArrayToRuntimeObjList(List<RTObject> jArray, boolean skipLast) throws Exception {
        int count = jArray.Count;
        if (skipLast)
            count--;
         
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ list = new List<T>(jArray.Count);
        for (int i = 0;i < count;i++)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jTok = jArray[i];
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ runtimeObj = jTokenToRuntimeRTObject(jTok) instanceof T ? (T)jTokenToRuntimeRTObject(jTok) : (T)null;
            list.Add(runtimeObj);
        }
        return list;
    }

    public static List<RTObject> jArrayToRuntimeObjList(List<RTObject> jArray, boolean skipLast) throws Exception {
        return JArrayToRuntimeObjList<RTObject>(jArray, skipLast);
    }

    public static HashMap<String, RTObject> HashMapRuntimeObjsToJRTObject(HashMap<String, RTObject> HashMap) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jsonObj = new HashMap<String, RTObject>();
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ keyVal : HashMap)
        {
            RTObject runtimeObj = keyVal.Value instanceof RTObject ? (RTObject)keyVal.Value : (RTObject)null;
            if (runtimeObj != null)
                jsonObj[keyVal.Key] = RuntimeRTObjectToJToken(runtimeObj);
             
        }
        return jsonObj;
    }

    public static HashMap<String, RTObject> jRTObjectToHashMapRuntimeObjs(HashMap<String, RTObject> jRTObject) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ dict = new HashMap<String, RTObject>(jRTObject.Count);
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ keyVal : jRTObject)
        {
            dict[keyVal.Key] = jTokenToRuntimeRTObject(keyVal.Value);
        }
        return dict;
    }

    public static HashMap<String, int> jRTObjectToIntHashMap(HashMap<String, RTObject> jRTObject) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ dict = new HashMap<String, int>(jRTObject.Count);
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ keyVal : jRTObject)
        {
            dict[keyVal.Key] = (int)keyVal.Value;
        }
        return dict;
    }

    public static HashMap<String, RTObject> intHashMapToJRTObject(HashMap<String, int> dict) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jObj = new HashMap<String, RTObject>();
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ keyVal : dict)
        {
            jObj[keyVal.Key] = keyVal.Value;
        }
        return jObj;
    }

    // ----------------------
    // JSON ENCODING SCHEME
    // ----------------------
    //
    // Glue:           "<>", "G<", "G>"
    //
    // ControlCommand: "ev", "out", "/ev", "du" "pop", "->->", "~ret", "str", "/str", "nop",
    //                 "choiceCnt", "turns", "visit", "seq", "thread", "done", "end"
    //
    // NativeFunction: "+", "-", "/", "*", "%" "~", "==", ">", "<", ">=", "<=", "!=", "!"... etc
    //
    // Void:           "void"
    //
    // Value:          "^string value", "^^string value beginning with ^"
    //                 5, 5.2
    //                 {"^->": "path.target"}
    //                 {"^var": "varname", "ci": 0}
    //
    // Container:      [...]
    //                 [...,
    //                     {
    //                         "subContainerName": ...,
    //                         "#f": 5,                    // flags
    //                         "#n": "containerOwnName"    // only if not redundant
    //                     }
    //                 ]
    //
    // Divert:         {"->": "path.target", "c": true }
    //                 {"->": "path.target", "var": true}
    //                 {"f()": "path.func"}
    //                 {"->t->": "path.tunnel"}
    //                 {"x()": "externalFuncName", "exArgs": 5}
    //
    // Var Assign:     {"VAR=": "varName", "re": true}   // reassignment
    //                 {"temp=": "varName"}
    //
    // Var ref:        {"VAR?": "varName"}
    //                 {"CNT?": "stitch name"}
    //
    // ChoicePoint:    {"*": pathString,
    //                  "flg": 18 }
    //
    // Choice:         Nothing too clever, it's only used in the save state,
    //                 there's not likely to be many of them.
    public static RTObject jTokenToRuntimeRTObject(RTObject token) throws Exception {
        if (token instanceof int || token instanceof float)
        {
            return Value.create(token);
        }
         
        if (token instanceof String)
        {
            String str = (String)token;
            // String value
            char firstChar = str[0];
            if (firstChar == '^')
                return new StringValue(str.Substring(1));
            else if (firstChar == '\n' && str.Length == 1)
                return new StringValue("\n");
              
            // Glue
            if (StringSupport.equals(str, "<>"))
                return new Glue(GlueType.Bidirectional);
            else if (StringSupport.equals(str, "G<"))
                return new Glue(GlueType.Left);
            else if (StringSupport.equals(str, "G>"))
                return new Glue(GlueType.Right);
               
            for (int i = 0;i < _controlCommandNames.Length;++i)
            {
                // Control commands (would looking up in a hash set be faster?)
                String cmdName = _controlCommandNames[i];
                if (StringSupport.equals(str, cmdName))
                {
                    return new ControlCommand(Ink.Runtime.ControlCommand.CommandType.values()[i]);
                }
                 
            }
            // Native functions
            if (NativeFunctionCall.callExistsWithName(str))
                return NativeFunctionCall.callWithName(str);
             
            // Pop
            if (StringSupport.equals(str, "->->"))
                return ControlCommand.popTunnel();
            else if (StringSupport.equals(str, "~ret"))
                return ControlCommand.popFunction();
              
            // Void
            if (StringSupport.equals(str, "void"))
                return new Void();
             
        }
         
        if (token instanceof HashMap<String, RTObject>)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ obj = (HashMap<String, RTObject>)token;
            RTObject propValue = new RTObject();
            // Divert target value to path
            boolean boolVar___0 = obj.TryGetValue("^->", refVar___0);
            if (boolVar___0)
            {
                RefSupport<RTObject> refVar___0 = new RefSupport<RTObject>();
                DivertTargetValue resVar___1 = new DivertTargetValue(new Path((String)propValue));
                propValue = refVar___0.getValue();
                return resVar___1;
            }
             
            // VariablePointerValue
            RefSupport<RTObject> refVar___1 = new RefSupport<RTObject>();
            boolean boolVar___2 = obj.TryGetValue("^var", refVar___1);
            propValue = refVar___1.getValue();
            if (boolVar___2)
            {
                VariablePointerValue varPtr = new VariablePointerValue((String)propValue);
                boolean boolVar___3 = obj.TryGetValue("ci", refVar___2);
                if (boolVar___3)
                {
                    RefSupport<RTObject> refVar___2 = new RefSupport<RTObject>();
                    varPtr.setcontextIndex((Integer)propValue);
                    propValue = refVar___2.getValue();
                }
                 
                return varPtr;
            }
             
            // Divert
            boolean isDivert = false;
            boolean pushesToStack = false;
            PushPopType divPushType = PushPopType.Function;
            boolean external = false;
            boolean boolVar___4 = obj.TryGetValue("->", refVar___3);
            if (boolVar___4)
            {
                isDivert = true;
            }
            else
            {
                boolean boolVar___5 = obj.TryGetValue("f()", refVar___4);
                if (boolVar___5)
                {
                    isDivert = true;
                    pushesToStack = true;
                    divPushType = PushPopType.Function;
                }
                else
                {
                    boolean boolVar___6 = obj.TryGetValue("->t->", refVar___5);
                    if (boolVar___6)
                    {
                        isDivert = true;
                        pushesToStack = true;
                        divPushType = PushPopType.Tunnel;
                    }
                    else
                    {
                        RefSupport<RTObject> refVar___3 = new RefSupport<RTObject>();
                        RefSupport<RTObject> refVar___4 = new RefSupport<RTObject>();
                        RefSupport<RTObject> refVar___5 = new RefSupport<RTObject>();
                        RefSupport<RTObject> refVar___6 = new RefSupport<RTObject>();
                        boolean boolVar___7 = obj.TryGetValue("x()", refVar___6);
                        propValue = refVar___3.getValue();
                        propValue = refVar___4.getValue();
                        propValue = refVar___5.getValue();
                        propValue = refVar___6.getValue();
                        if (boolVar___7)
                        {
                            isDivert = true;
                            external = true;
                            pushesToStack = false;
                            divPushType = PushPopType.Function;
                        }
                         
                    } 
                } 
            } 
            if (isDivert)
            {
                Divert divert = new Divert();
                divert.setpushesToStack(pushesToStack);
                divert.stackPushType = divPushType;
                divert.setisExternal(external);
                String target = propValue.ToString();
                boolean boolVar___8 = obj.TryGetValue("var", refVar___7);
                if (boolVar___8)
                {
                    RefSupport<RTObject> refVar___7 = new RefSupport<RTObject>();
                    divert.setvariableDivertName(target);
                    propValue = refVar___7.getValue();
                }
                else
                    divert.settargetPathString(target); 
                RefSupport<RTObject> refVar___8 = new RefSupport<RTObject>();
                divert.setisConditional(obj.TryGetValue("c", refVar___8));
                propValue = refVar___8.getValue();
                if (external)
                {
                    boolean boolVar___9 = obj.TryGetValue("exArgs", refVar___9);
                    if (boolVar___9)
                    {
                        RefSupport<RTObject> refVar___9 = new RefSupport<RTObject>();
                        divert.setexternalArgs((Integer)propValue);
                        propValue = refVar___9.getValue();
                    }
                     
                }
                 
                return divert;
            }
             
            // Choice
            RefSupport<RTObject> refVar___10 = new RefSupport<RTObject>();
            boolean boolVar___10 = obj.TryGetValue("*", refVar___10);
            propValue = refVar___10.getValue();
            if (boolVar___10)
            {
                ChoicePoint choice = new ChoicePoint();
                choice.setpathStringOnChoice(propValue.ToString());
                boolean boolVar___11 = obj.TryGetValue("flg", refVar___11);
                if (boolVar___11)
                {
                    RefSupport<RTObject> refVar___11 = new RefSupport<RTObject>();
                    choice.setflags((Integer)propValue);
                    propValue = refVar___11.getValue();
                }
                 
                return choice;
            }
             
            // Variable reference
            boolean boolVar___12 = obj.TryGetValue("VAR?", refVar___12);
            if (boolVar___12)
            {
                return new VariableReference(propValue.ToString());
            }
            else
            {
                RefSupport<RTObject> refVar___12 = new RefSupport<RTObject>();
                RefSupport<RTObject> refVar___13 = new RefSupport<RTObject>();
                boolean boolVar___13 = obj.TryGetValue("CNT?", refVar___13);
                propValue = refVar___12.getValue();
                propValue = refVar___13.getValue();
                if (boolVar___13)
                {
                    VariableReference readCountVarRef = new VariableReference();
                    readCountVarRef.setpathStringForCount(propValue.ToString());
                    return readCountVarRef;
                }
                 
            } 
            // Variable assignment
            boolean isVarAss = false;
            boolean isGlobalVar = false;
            boolean boolVar___14 = obj.TryGetValue("VAR=", refVar___14);
            if (boolVar___14)
            {
                isVarAss = true;
                isGlobalVar = true;
            }
            else
            {
                RefSupport<RTObject> refVar___14 = new RefSupport<RTObject>();
                RefSupport<RTObject> refVar___15 = new RefSupport<RTObject>();
                boolean boolVar___15 = obj.TryGetValue("temp=", refVar___15);
                propValue = refVar___14.getValue();
                propValue = refVar___15.getValue();
                if (boolVar___15)
                {
                    isVarAss = true;
                    isGlobalVar = false;
                }
                 
            } 
            if (isVarAss)
            {
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ varName = propValue.ToString();
                RefSupport<RTObject> refVar___16 = new RefSupport<RTObject>();
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ isNewDecl = !obj.TryGetValue("re", refVar___16);
                propValue = refVar___16.getValue();
                VariableAssignment varAss = new VariableAssignment(varName, isNewDecl);
                varAss.setisGlobal(isGlobalVar);
                return varAss;
            }
             
            if (obj["originalChoicePath"] != null)
                return JRTObjectToChoice(obj);
             
        }
         
        // Array is always a Runtime.Container
        if (token instanceof List<RTObject>)
        {
            return JArrayToContainer((List<RTObject>)token);
        }
         
        if (token == null)
            return null;
         
        throw new System.Exception("Failed to convert token to runtime RTObject: " + token);
    }

    public static RTObject runtimeRTObjectToJToken(RTObject obj) throws Exception {
        Container container = obj instanceof Container ? (Container)obj : (Container)null;
        if (container)
        {
            return containerToJArray(container);
        }
         
        Divert divert = obj instanceof Divert ? (Divert)obj : (Divert)null;
        if (divert)
        {
            String divTypeKey = "->";
            if (divert.getisExternal())
                divTypeKey = "x()";
            else if (divert.getpushesToStack())
            {
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
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jObj = new HashMap<String, RTObject>();
            jObj[divTypeKey] = targetStr;
            if (divert.gethasVariableTarget())
                jObj["var"] = true;
             
            if (divert.getisConditional())
                jObj["c"] = true;
             
            if (divert.getexternalArgs() > 0)
                jObj["exArgs"] = divert.getexternalArgs();
             
            return jObj;
        }
         
        ChoicePoint choicePoint = obj instanceof ChoicePoint ? (ChoicePoint)obj : (ChoicePoint)null;
        if (choicePoint)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jObj = new HashMap<String, RTObject>();
            jObj["*"] = choicePoint.getpathStringOnChoice();
            jObj["flg"] = choicePoint.getflags();
            return jObj;
        }
         
        IntValue intVal = obj instanceof IntValue ? (IntValue)obj : (IntValue)null;
        if (intVal)
            return intVal.getvalue();
         
        FloatValue floatVal = obj instanceof FloatValue ? (FloatValue)obj : (FloatValue)null;
        if (floatVal)
            return floatVal.getvalue();
         
        StringValue strVal = obj instanceof StringValue ? (StringValue)obj : (StringValue)null;
        if (strVal)
        {
            if (strVal.getisNewline())
                return "\n";
            else
                return "^" + strVal.getvalue(); 
        }
         
        DivertTargetValue divTargetVal = obj instanceof DivertTargetValue ? (DivertTargetValue)obj : (DivertTargetValue)null;
        if (divTargetVal)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ divTargetJsonObj = new HashMap<String, RTObject>();
            divTargetJsonObj["^->"] = divTargetVal.getvalue().getcomponentsString();
            return divTargetJsonObj;
        }
         
        VariablePointerValue varPtrVal = obj instanceof VariablePointerValue ? (VariablePointerValue)obj : (VariablePointerValue)null;
        if (varPtrVal)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ varPtrJsonObj = new HashMap<String, RTObject>();
            varPtrJsonObj["^var"] = varPtrVal.getvalue();
            varPtrJsonObj["ci"] = varPtrVal.getcontextIndex();
            return varPtrJsonObj;
        }
         
        Glue glue = obj instanceof Glue ? (Glue)obj : (Glue)null;
        if (glue)
        {
            if (glue.getisBi())
                return "<>";
            else if (glue.getisLeft())
                return "G<";
            else
                return "G>";  
        }
         
        ControlCommand controlCmd = obj instanceof ControlCommand ? (ControlCommand)obj : (ControlCommand)null;
        if (controlCmd)
        {
            return _controlCommandNames[((Enum)controlCmd.getcommandType()).ordinal()];
        }
         
        NativeFunctionCall nativeFunc = obj instanceof NativeFunctionCall ? (NativeFunctionCall)obj : (NativeFunctionCall)null;
        if (nativeFunc)
            return nativeFunc.getname();
         
        // Variable reference
        VariableReference varRef = obj instanceof VariableReference ? (VariableReference)obj : (VariableReference)null;
        if (varRef)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jObj = new HashMap<String, RTObject>();
            String readCountPath = varRef.getpathStringForCount();
            if (readCountPath != null)
            {
                jObj["CNT?"] = readCountPath;
            }
            else
            {
                jObj["VAR?"] = varRef.getname();
            } 
            return jObj;
        }
         
        // Variable assignment
        VariableAssignment varAss = obj instanceof VariableAssignment ? (VariableAssignment)obj : (VariableAssignment)null;
        if (varAss)
        {
            String key = varAss.getisGlobal() ? "VAR=" : "temp=";
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jObj = new HashMap<String, RTObject>();
            jObj[key] = varAss.getvariableName();
            // Reassignment?
            if (!varAss.getisNewDeclaration())
                jObj["re"] = true;
             
            return jObj;
        }
         
        Void voidObj = obj instanceof Void ? (Void)obj : (Void)null;
        if (voidObj)
            return "void";
         
        // Used when serialising save state only
        Choice choice = obj instanceof Choice ? (Choice)obj : (Choice)null;
        if (choice)
            return choiceToJRTObject(choice);
         
        throw new System.Exception("Failed to convert runtime RTObject to Json token: " + obj);
    }

    static List<RTObject> containerToJArray(Container container) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jArray = listToJArray(container.getcontent());
        // Container is always an array [...]
        // But the final element is always either:
        //  - a HashMap containing the named content, as well as possibly
        //    the key "#" with the count flags
        //  - null, if neither of the above
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ namedOnlyContent = container.getnamedOnlyContent();
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ countFlags = container.getcountFlags();
        if (namedOnlyContent != null && namedOnlyContent.Count > 0 || countFlags > 0 || container.getname() != null)
        {
            HashMap<String, RTObject> terminatingObj = new HashMap<String, RTObject>();
            if (namedOnlyContent != null)
            {
                terminatingObj = HashMapRuntimeObjsToJRTObject(namedOnlyContent);
                for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ namedContentObj : terminatingObj)
                {
                    // Strip redundant names from containers if necessary
                    /* [UNSUPPORTED] 'var' as type is unsupported "var" */ subContainerJArray = namedContentObj.Value instanceof List<RTObject> ? (List<RTObject>)namedContentObj.Value : (List<RTObject>)null;
                    if (subContainerJArray != null)
                    {
                        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ attrJObj = subContainerJArray[subContainerJArray.Count - 1] instanceof HashMap<String, RTObject> ? (HashMap<String, RTObject>)subContainerJArray[subContainerJArray.Count - 1] : (HashMap<String, RTObject>)null;
                        if (attrJObj != null)
                        {
                            attrJObj.Remove("#n");
                            if (attrJObj.Count == 0)
                                subContainerJArray[subContainerJArray.Count - 1] = null;
                             
                        }
                         
                    }
                     
                }
            }
            else
                terminatingObj = new HashMap<String, RTObject>(); 
            if (countFlags > 0)
                terminatingObj["#f"] = countFlags;
             
            if (container.getname() != null)
                terminatingObj["#n"] = container.getname();
             
            jArray.Add(terminatingObj);
        }
        else
        {
            // Add null terminator to indicate that there's no HashMap
            jArray.Add(null);
        } 
        return jArray;
    }

    static Container jArrayToContainer(List<RTObject> jArray) throws Exception {
        Container container = new Container();
        container.setcontent(JArrayToRuntimeObjList(jArray));
        // Final RTObject in the array is always a combination of
        //  - named content
        //  - a "#" key with the countFlags
        // (if either exists at all, otherwise null)
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ terminatingObj = jArray[jArray.Count - 1] instanceof HashMap<String, RTObject> ? (HashMap<String, RTObject>)jArray[jArray.Count - 1] : (HashMap<String, RTObject>)null;
        if (terminatingObj != null)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ namedOnlyContent = new HashMap<String, RTObject>(terminatingObj.Count);
            for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ keyVal : terminatingObj)
            {
                if (StringSupport.equals(keyVal.Key, "#f"))
                {
                    container.setcountFlags((int)keyVal.Value);
                }
                else if (StringSupport.equals(keyVal.Key, "#n"))
                {
                    container.setname(keyVal.Value.ToString());
                }
                else
                {
                    /* [UNSUPPORTED] 'var' as type is unsupported "var" */ namedContentItem = jTokenToRuntimeRTObject(keyVal.Value);
                    Container namedSubContainer = namedContentItem instanceof Container ? (Container)namedContentItem : (Container)null;
                    if (namedSubContainer)
                        namedSubContainer.setname(keyVal.Key);
                     
                    namedOnlyContent[keyVal.Key] = namedContentItem;
                }  
            }
            container.setnamedOnlyContent(namedOnlyContent);
        }
         
        return container;
    }

    static Choice jRTObjectToChoice(HashMap<String, RTObject> jObj) throws Exception {
        Choice choice = new Choice();
        choice.settext(jObj["text"].ToString());
        choice.setindex((int)jObj["index"]);
        choice.originalChoicePath = jObj["originalChoicePath"].ToString();
        choice.originalThreadIndex = (int)jObj["originalThreadIndex"];
        return choice;
    }

    static HashMap<String, RTObject> choiceToJRTObject(Choice choice) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ jObj = new HashMap<String, RTObject>();
        jObj["text"] = choice.gettext();
        jObj["index"] = choice.getindex();
        jObj["originalChoicePath"] = choice.originalChoicePath;
        jObj["originalThreadIndex"] = choice.originalThreadIndex;
        return jObj;
    }

    static {
        try
        {
            _controlCommandNames = new String[((Enum)Ink.Runtime.ControlCommand.CommandType.TOTAL_VALUES).ordinal()];
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.EvalStart).ordinal()] = "ev";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.EvalOutput).ordinal()] = "out";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.EvalEnd).ordinal()] = "/ev";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.Duplicate).ordinal()] = "du";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.PopEvaluatedValue).ordinal()] = "pop";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.PopFunction).ordinal()] = "~ret";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.PopTunnel).ordinal()] = "->->";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.BeginString).ordinal()] = "str";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.EndString).ordinal()] = "/str";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.NoOp).ordinal()] = "nop";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.ChoiceCount).ordinal()] = "choiceCnt";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.TurnsSince).ordinal()] = "turns";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.VisitIndex).ordinal()] = "visit";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.SequenceShuffleIndex).ordinal()] = "seq";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.StartThread).ordinal()] = "thread";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.Done).ordinal()] = "done";
            _controlCommandNames[((Enum)Ink.Runtime.ControlCommand.CommandType.End).ordinal()] = "end";
            for (int i = 0;i < ((Enum)Ink.Runtime.ControlCommand.CommandType.TOTAL_VALUES).ordinal();++i)
            {
                if (_controlCommandNames[i] == null)
                    throw new System.Exception("Control command not accounted for in serialisation");
                 
            }
        }
        catch (Exception __dummyStaticConstructorCatchVar0)
        {
            throw new ExceptionInInitializerError(__dummyStaticConstructorCatchVar0);
        }
    
    }

    static String[] _controlCommandNames = new String[]();
}


