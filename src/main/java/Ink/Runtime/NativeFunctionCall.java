package Ink.Runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class NativeFunctionCall  extends RTObject 
{
    public static final String Add = "+";
    public static final String Subtract = "-";
    public static final String Divide = "/";
    public static final String Multiply = "*";
    public static final String Mod = "%";
    public static final String Negate = "~";
    public static final String Equal = "==";
    public static final String Greater = ">";
    public static final String Less = "<";
    public static final String GreaterThanOrEquals = ">=";
    public static final String LessThanOrEquals = "<=";
    public static final String NotEquals = "!=";
    public static final String Not = "!";
    public static final String And = "&&";
    public static final String Or = "||";
    public static final String Min = "MIN";
    public static final String Max = "MAX";
    public static NativeFunctionCall callWithName(String functionName) throws Exception {
        return new NativeFunctionCall(functionName);
    }

    public static boolean callExistsWithName(String functionName) throws Exception {
        generateNativeFunctionsIfNecessary();
        return _nativeFunctions.containsKey(functionName);
    }

    public String getname() throws Exception {
        return _name;
    }

    public void setname(String value) throws Exception {
        _name = value;
        if (!_isPrototype)
            _prototype = _nativeFunctions.get(_name);
         
    }

    String _name;
    
    public int getnumberOfParameters() throws Exception {
        if (_prototype != null)
        {
            return _prototype.getnumberOfParameters();
        }
        else
        {
            return _numberOfParameters;
        } 
    }

    public void setnumberOfParameters(int value) throws Exception {
        _numberOfParameters = value;
    }

    int _numberOfParameters;
    
    // TODO
    /*
    public RTObject call(List<RTObject> parameters) throws Exception {
        if (_prototype != null)
        {
            return _prototype.call(parameters);
        }
         
        if (getnumberOfParameters() != parameters.size())
        {
            throw new Exception("Unexpected number of parameters");
        }
         
        for (RTObject p : parameters)
        {
            if (p instanceof Void)
                throw new StoryException("Attempting to perform operation on a void value. Did you forget to 'return' a value from a function you called here?");
             
        }
        TypeXXXX coercedParams = CoerceValuesToSingleType(parameters);
        ValueType coercedType = coercedParams[0].valueType;
        if (coercedType == ValueType.Int)
        {
            return Call<int>(coercedParams);
        }
        else if (coercedType == ValueType.Float)
        {
            return Call<float>(coercedParams);
        }
        else if (coercedType == ValueType.String)
        {
            return Call<String>(coercedParams);
        }
        else if (coercedType == ValueType.DivertTarget)
        {
            return Call<Path>(coercedParams);
        }
            
        return null;
    }

    Value<T> call(List<Value> parametersOfSingleType) throws Exception {
        Value param1 = (Value)parametersOfSingleType.get(0);
        ValueType valType = param1.getvalueType();
        Value<T> val1 = (Value<T>)param1;
        int paramCount = parametersOfSingleType.Count;
        if (paramCount == 2 || paramCount == 1)
        {
            RTObject opForTypeObj = null;
            RefSupport<RTObject> refVar___0 = new RefSupport<RTObject>();
            boolean boolVar___0 = !_operationFuncs.TryGetValue(valType, refVar___0);
            opForTypeObj = refVar___0.getValue();
            if (boolVar___0)
            {
                throw new StoryException("Can not perform operation '" + this.getname() + "' on " + valType);
            }
             
            // Binary
            if (paramCount == 2)
            {
                Value param2 = (Value)parametersOfSingleType[1];
                Value<T> val2 = (Value<T>)param2;
                BinaryOp<T> opForType = (BinaryOp<T>)opForTypeObj;
                // Return value unknown until it's evaluated
                RTObject resultVal = opForType.invoke(val1.getvalue(),val2.getvalue());
                return Value.create(resultVal);
            }
            else
            {
                // Unary
                UnaryOp<T> opForType = (UnaryOp<T>)opForTypeObj;
                TypeXXXX resultVal = opForType.invoke(val1.getvalue());
                return Value.create(resultVal);
            } 
        }
        else
        {
            throw new Exception("Unexpected number of parameters to NativeFunctionCall: " + parametersOfSingleType.Count);
        } 
    }
    */

    List<Value> coerceValuesToSingleType(List<RTObject> parametersIn) throws Exception {
    	// TODO:
//        ValueType valType = ValueType.Int;
//        for (RTObject obj : parametersIn)
//        {
//            // Find out what the output type is
//            // "higher level" types infect both so that binary operations
//            // use the same type on both sides. e.g. binary operation of
//            // int and float causes the int to be casted to a float.
//            Value val = (Value)obj;
//            if (val.getvalueType() > valType)
//            {
//                valType = val.getvalueType();
//            }
//             
//        }
//        // Coerce to this chosen type
        ArrayList<Value> parametersOut = new ArrayList<Value>();
//        for (RTObject __dummyForeachVar2 : parametersIn)
//        {
//            Value val = (Value)__dummyForeachVar2;
//            Value castedValue = val.cast(valType);
//            parametersOut.add(castedValue);
//        }
        return parametersOut;
    }

    public NativeFunctionCall(String name) throws Exception {
        generateNativeFunctionsIfNecessary();
        this.setname(name);
    }

    // Require default constructor for serialisation
    public NativeFunctionCall() throws Exception {
        generateNativeFunctionsIfNecessary();
    }

    // Only called internally to generate prototypes
    NativeFunctionCall(String name, int numberOfParamters) throws Exception {
        _isPrototype = true;
        this.setname(name);
        this.setnumberOfParameters(numberOfParamters);
    }

    // TODO
    static void generateNativeFunctionsIfNecessary() throws Exception {
//        if (_nativeFunctions == null)
//        {
//            _nativeFunctions = new HashMap<String, NativeFunctionCall>();
//            // Int operations
//            AddIntBinaryOp(Add, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x + y;
//            }" */);
//            AddIntBinaryOp(Subtract, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x - y;
//            }" */);
//            AddIntBinaryOp(Multiply, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x * y;
//            }" */);
//            AddIntBinaryOp(Divide, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x / y;
//            }" */);
//            AddIntBinaryOp(Mod, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x % y;
//            }" */);
//            AddIntUnaryOp(/* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(Negate, x) => {
//                return -x;
//            }" */);
//            AddIntBinaryOp(Equal, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x == y ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(Greater, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x > y ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(Less, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x < y ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(GreaterThanOrEquals, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x >= y ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(LessThanOrEquals, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x <= y ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(NotEquals, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x != y ? 1 : 0;
//            }" */);
//            AddIntUnaryOp(/* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(Not, x) => {
//                return (x == 0) ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(And, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x != 0 && y != 0 ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(Or, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x != 0 || y != 0 ? 1 : 0;
//            }" */);
//            AddIntBinaryOp(Max, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return Math.Max(x, y);
//            }" */);
//            AddIntBinaryOp(Min, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return Math.Min(x, y);
//            }" */);
//            // Float operations
//            AddFloatBinaryOp(Add, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x + y;
//            }" */);
//            AddFloatBinaryOp(Subtract, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x - y;
//            }" */);
//            AddFloatBinaryOp(Multiply, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x * y;
//            }" */);
//            AddFloatBinaryOp(Divide, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x / y;
//            }" */);
//            AddFloatBinaryOp(Mod, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x % y;
//            }" */);
//            // TODO: Is this the operation we want for floats?
//            AddFloatUnaryOp(/* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(Negate, x) => {
//                return -x;
//            }" */);
//            AddFloatBinaryOp(Equal, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x == y ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(Greater, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x > y ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(Less, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x < y ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(GreaterThanOrEquals, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x >= y ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(LessThanOrEquals, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x <= y ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(NotEquals, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x != y ? (int)1 : (int)0;
//            }" */);
//            AddFloatUnaryOp(/* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(Not, x) => {
//                return (x == 0.0f) ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(And, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x != 0.0f && y != 0.0f ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(Or, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x != 0.0f || y != 0.0f ? (int)1 : (int)0;
//            }" */);
//            AddFloatBinaryOp(Max, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return Math.Max(x, y);
//            }" */);
//            AddFloatBinaryOp(Min, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return Math.Min(x, y);
//            }" */);
//            // String operations
//            AddStringBinaryOp(Add, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x + y;
//            }" */);
//            // concat
//            AddStringBinaryOp(Equal, /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "(x, y) => {
//                return x.Equals(y) ? (int)1 : (int)0;
//            }" */);
//            BinaryOp<Path> divertTargetsEqual = new BinaryOp<Path>() 
//              { 
//                // Special case: The only operation you can do on divert target values
//                public System.RTObject invoke(Path d1, Path d2) throws Exception {
//                    return d1.equals(d2) ? 1 : 0;
//                }
//
//                public List<BinaryOp<Path>> getInvocationList() throws Exception {
//                    List<BinaryOp<Path>> ret = new ArrayList<BinaryOp<Path>>();
//                    ret.add(this);
//                    return ret;
//                }
//            
//              };
//            addOpToNativeFunc(Equal,2,ValueType.DivertTarget,divertTargetsEqual);
//        }
//         
    }
//
//    void addOpFuncForType(ValueType valType, RTObject op) throws Exception {
//        if (_operationFuncs == null)
//        {
//            _operationFuncs = new HashMap<ValueType, RTObject>();
//        }
//         
//        _operationFuncs[valType] = op;
//    }
//
//    static void addOpToNativeFunc(String name, int args, ValueType valType, RTObject op) throws Exception {
//        NativeFunctionCall nativeFunc = null;
//        // Operations for each data type, for a single operation (e.g. "+")
//        RefSupport<NativeFunctionCall> refVar___1 = new RefSupport<NativeFunctionCall>();
//        boolean boolVar___1 = !_nativeFunctions.TryGetValue(name, refVar___1);
//        nativeFunc = refVar___1.getValue();
//        if (boolVar___1)
//        {
//            nativeFunc = new NativeFunctionCall(name,args);
//            _nativeFunctions[name] = nativeFunc;
//        }
//         
//        nativeFunc.addOpFuncForType(valType,op);
//    }
//
//    static void addIntBinaryOp(String name, BinaryOp<int> op) throws Exception {
//        addOpToNativeFunc(name,2,ValueType.Int,op);
//    }
//
//    static void addIntUnaryOp(String name, UnaryOp<int> op) throws Exception {
//        addOpToNativeFunc(name,1,ValueType.Int,op);
//    }
//
//    static void addFloatBinaryOp(String name, BinaryOp<float> op) throws Exception {
//        addOpToNativeFunc(name,2,ValueType.Float,op);
//    }
//
//    static void addStringBinaryOp(String name, BinaryOp<String> op) throws Exception {
//        addOpToNativeFunc(name,2,ValueType.String,op);
//    }
//
//    static void addFloatUnaryOp(String name, UnaryOp<float> op) throws Exception {
//        addOpToNativeFunc(name,1,ValueType.Float,op);
//    }
//
//    public String toString() {
//        try
//        {
//            return "Native '" + getname() + "'";
//        }
//        catch (RuntimeException __dummyCatchVar0)
//        {
//            throw __dummyCatchVar0;
//        }
//        catch (Exception __dummyCatchVar0)
//        {
//            throw new RuntimeException(__dummyCatchVar0);
//        }
//    
//    }
//
//    static class __MultiBinaryOp <T>  implements BinaryOp<T>
//    {
//        public RTObject invoke(T left, T right) throws Exception {
//            IList<BinaryOp<T>> copy = new IList<BinaryOp<T>>(), members = this.getInvocationList();
//            synchronized (members)
//            {
//                copy = new LinkedList<BinaryOp<T>>(members);
//            }
//            BinaryOp<T> prev = null;
//            for (RTObject __dummyForeachVar3 : copy)
//            {
//                BinaryOp<T> d = (BinaryOp<T>)__dummyForeachVar3;
//                if (prev != null)
//                    prev.invoke(left, right);
//                 
//                prev = d;
//            }
//            return prev.invoke(left, right);
//        }
//
//        private System.Collections.Generic.IList<BinaryOp<T>> _invocationList = new ArrayList<BinaryOp<T>>();
//        public static <T>BinaryOp<T> combine(BinaryOp<T> a, BinaryOp<T> b) throws Exception {
//            if (a == null)
//                return b;
//             
//            if (b == null)
//                return a;
//             
//            __MultiBinaryOp<T> ret = new __MultiBinaryOp<T>();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static <T>BinaryOp<T> remove(BinaryOp<T> a, BinaryOp<T> b) throws Exception {
//            if (a == null || b == null)
//                return a;
//             
//            System.Collections.Generic.IList<BinaryOp<T>> aInvList = a.getInvocationList();
//            System.Collections.Generic.IList<BinaryOp<T>> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList)
//            {
//                return a;
//            }
//            else
//            {
//                __MultiBinaryOp<T> ret = new __MultiBinaryOp<T>();
//                ret._invocationList = newInvList;
//                return ret;
//            } 
//        }
//
//        public System.Collections.Generic.IList<BinaryOp<T>> getInvocationList() throws Exception {
//            return _invocationList;
//        }
//    
//    }
//
//    static interface BinaryOp <T>  
//    {
//        RTObject invoke(T left, T right) throws Exception ;
//
//        System.Collections.Generic.IList<BinaryOp<T>> getInvocationList() throws Exception ;
//    
//    }
//
//    static class __MultiUnaryOp <T>  implements UnaryOp<T>
//    {
//        public RTObject invoke(T val) throws Exception {
//            IList<UnaryOp<T>> copy = new IList<UnaryOp<T>>(), members = this.getInvocationList();
//            synchronized (members)
//            {
//                copy = new LinkedList<UnaryOp<T>>(members);
//            }
//            UnaryOp<T> prev = null;
//            for (RTObject __dummyForeachVar4 : copy)
//            {
//                UnaryOp<T> d = (UnaryOp<T>)__dummyForeachVar4;
//                if (prev != null)
//                    prev.invoke(val);
//                 
//                prev = d;
//            }
//            return prev.invoke(val);
//        }
//
//        private System.Collections.Generic.IList<UnaryOp<T>> _invocationList = new ArrayList<UnaryOp<T>>();
//        public static <T>UnaryOp<T> combine(UnaryOp<T> a, UnaryOp<T> b) throws Exception {
//            if (a == null)
//                return b;
//             
//            if (b == null)
//                return a;
//             
//            __MultiUnaryOp<T> ret = new __MultiUnaryOp<T>();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static <T>UnaryOp<T> remove(UnaryOp<T> a, UnaryOp<T> b) throws Exception {
//            if (a == null || b == null)
//                return a;
//             
//            System.Collections.Generic.IList<UnaryOp<T>> aInvList = a.getInvocationList();
//            System.Collections.Generic.IList<UnaryOp<T>> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList)
//            {
//                return a;
//            }
//            else
//            {
//                __MultiUnaryOp<T> ret = new __MultiUnaryOp<T>();
//                ret._invocationList = newInvList;
//                return ret;
//            } 
//        }
//
//        public System.Collections.Generic.IList<UnaryOp<T>> getInvocationList() throws Exception {
//            return _invocationList;
//        }
//    
//    }
//
//    static interface UnaryOp <T>  
//    {
//        RTObject invoke(T val) throws Exception ;
//
//        System.Collections.Generic.IList<UnaryOp<T>> getInvocationList() throws Exception ;
//    
//    }

    NativeFunctionCall _prototype;
    boolean _isPrototype;
    HashMap<ValueType, RTObject> _operationFuncs = new HashMap<ValueType, RTObject>();
    static HashMap<String, NativeFunctionCall> _nativeFunctions = new HashMap<String, NativeFunctionCall>();
}


