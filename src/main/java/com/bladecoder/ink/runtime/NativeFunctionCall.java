package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NativeFunctionCall extends RTObject {
	static interface BinaryOp {
		Object invoke(Object left, Object right);
	}
	static interface UnaryOp {
		Object invoke(Object val);
	}
	public static final String Add = "+";
	public static final String And = "&&";
	public static final String Divide = "/";
	public static final String Equal = "==";
	public static final String Greater = ">";
	public static final String GreaterThanOrEquals = ">=";
	public static final String Less = "<";
	public static final String LessThanOrEquals = "<=";
	public static final String Max = "MAX";
	public static final String Min = "MIN";
	public static final String Mod = "%";
	public static final String Multiply = "*";
	private static HashMap<String, NativeFunctionCall> nativeFunctions;
	public static final String Negate = "~";
	public static final String Not = "!";

	public static final String NotEquals = "!=";

	public static final String Or = "||";

	public static final String Subtract = "-";

	static void addFloatBinaryOp(String name, BinaryOp op) throws Exception {
		addOpToNativeFunc(name, 2, ValueType.Float, op);
	}

	static void addFloatUnaryOp(String name, UnaryOp op) throws Exception {
		addOpToNativeFunc(name, 1, ValueType.Float, op);
	}

	static void addIntBinaryOp(String name, BinaryOp op) throws Exception {
		addOpToNativeFunc(name, 2, ValueType.Int, op);
	}

	static void addIntUnaryOp(String name, UnaryOp op) throws Exception {
		addOpToNativeFunc(name, 1, ValueType.Int, op);
	}

	static void addOpToNativeFunc(String name, int args, ValueType valType, Object op) throws Exception {
		NativeFunctionCall nativeFunc = nativeFunctions.get(name);

		// Operations for each data type, for a single operation (e.g. "+")

		if (nativeFunc == null) {
			nativeFunc = new NativeFunctionCall(name, args);
			nativeFunctions.put(name, nativeFunc);
		}

		nativeFunc.addOpFuncForType(valType, op);
	}

	static void addStringBinaryOp(String name, BinaryOp op) throws Exception {
		addOpToNativeFunc(name, 2, ValueType.String, op);
	}

	public static boolean callExistsWithName(String functionName) throws Exception {
		generateNativeFunctionsIfNecessary();
		return nativeFunctions.containsKey(functionName);
	}

	public static NativeFunctionCall callWithName(String functionName) throws Exception {
		return new NativeFunctionCall(functionName);
	}

	// TODO
	static void generateNativeFunctionsIfNecessary() throws Exception {
		if (nativeFunctions == null) {
			nativeFunctions = new HashMap<String, NativeFunctionCall>();

			// Int operations
			addIntBinaryOp(Add, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left + (Integer) right;
				}
			});

			addIntBinaryOp(Subtract, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left - (Integer) right;
				}
			});

			addIntBinaryOp(Multiply, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left * (Integer) right;
				}
			});

			addIntBinaryOp(Divide, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left / (Integer) right;
				}
			});

			addIntBinaryOp(Mod, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left % (Integer) right;
				}
			});

			addIntUnaryOp(Negate, new UnaryOp() {

				@Override
				public Object invoke(Object val) {
					return -(Integer) val;
				}
			});

			addIntBinaryOp(Equal, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left == (Integer) right ? 1 : 0;
				}
			});

			addIntBinaryOp(Greater, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left > (Integer) right ? 1 : 0;
				}
			});

			addIntBinaryOp(Less, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left < (Integer) right ? 1 : 0;
				}
			});
			addIntBinaryOp(GreaterThanOrEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left >= (Integer) right ? 1 : 0;
				}
			});
			addIntBinaryOp(LessThanOrEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left <= (Integer) right ? 1 : 0;
				}
			});
			addIntBinaryOp(NotEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left != (Integer) right ? 1 : 0;
				}
			});

			addIntUnaryOp(Not, new UnaryOp() {

				@Override
				public Object invoke(Object val) {
					return (Integer) val == 0 ? 1 : 0;
				}
			});

			addIntBinaryOp(And, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left != 0 && (Integer) right != 0 ? 1 : 0;
				}
			});
			addIntBinaryOp(Or, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Integer) left != 0 || (Integer) right != 0 ? 1 : 0;
				}
			});
			addIntBinaryOp(Max, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return Math.max((Integer) left, (Integer) right);
				}
			});
			addIntBinaryOp(Min, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return Math.min((Integer) left, (Integer) right);
				}
			});

			// Float operations
			addFloatBinaryOp(Add, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left + (Float) right;
				}
			});

			addFloatBinaryOp(Subtract, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left - (Float) right;
				}
			});

			addFloatBinaryOp(Multiply, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left * (Float) right;
				}
			});

			addFloatBinaryOp(Divide, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left / (Float) right;
				}
			});

			addFloatBinaryOp(Mod, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left % (Float) right;
				}
			});

			addFloatUnaryOp(Negate, new UnaryOp() {

				@Override
				public Object invoke(Object val) {
					return -(Float) val;
				}
			});

			addFloatBinaryOp(Equal, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left == (Float) right ? (Integer) 1 : (Integer) 0;
				}
			});

			addFloatBinaryOp(Greater, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left > (Float) right ? (Integer) 1 : (Integer) 0;
				}
			});

			addFloatBinaryOp(Less, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left < (Float) right ? (Integer) 1 : (Integer) 0;
				}
			});
			addFloatBinaryOp(GreaterThanOrEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left >= (Float) right ? (Integer) 1 : (Integer) 0;
				}
			});
			addFloatBinaryOp(LessThanOrEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left <= (Float) right ? (Integer) 1 : (Integer) 0;
				}
			});
			addFloatBinaryOp(NotEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left != (Float) right ? (Integer) 1 : (Integer) 0;
				}
			});

			addFloatUnaryOp(Not, new UnaryOp() {

				@Override
				public Object invoke(Object val) {
					return (Float) val == 0 ? (Integer) 1 : (Integer) 0;
				}
			});

			addFloatBinaryOp(And, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left != 0 && (Float) right != 0 ? (Integer) 1 : (Integer) 0;
				}
			});
			addFloatBinaryOp(Or, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (Float) left != 0 || (Float) right != 0 ? (Integer) 1 : (Integer) 0;
				}
			});
			addFloatBinaryOp(Max, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return Math.max((Float) left, (Float) right);
				}
			});
			addFloatBinaryOp(Min, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return Math.min((Float) left, (Float) right);
				}
			});

			// String operations
			addStringBinaryOp(Add, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (String) left + (String) right;
				}
			});
			// concat
			addStringBinaryOp(Equal, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((String) left).equals(right) ? (Integer) 1 : (Integer) 0;
				}
			});

			BinaryOp divertTargetsEqual = new BinaryOp() {

				@Override
				public Object invoke(Object left, Object right) {
					return ((Path) left).equals((Path) left) ? (Integer) 1 : (Integer) 0;
				}

			};

			addOpToNativeFunc(Equal, 2, ValueType.DivertTarget, divertTargetsEqual);
		}

	}

	private String name;

	private int numberOfParameters;

	private boolean isPrototype;

	private HashMap<ValueType, Object> operationFuncs;

	private NativeFunctionCall prototype;

	// Require default constructor for serialisation
	public NativeFunctionCall() throws Exception {
		generateNativeFunctionsIfNecessary();
	}

	public NativeFunctionCall(String name) throws Exception {
		generateNativeFunctionsIfNecessary();
		this.setName(name);
	}

	// Only called internally to generate prototypes
	NativeFunctionCall(String name, int numberOfParamters) throws Exception {
		isPrototype = true;
		this.setName(name);
		this.setNumberOfParameters(numberOfParamters);
	}

	void addOpFuncForType(ValueType valType, Object op) throws Exception {
		if (operationFuncs == null) {
			operationFuncs = new HashMap<ValueType, Object>();
		}

		operationFuncs.put(valType, op);
	}

	public RTObject call(List<RTObject> parameters) throws Exception {

		if (prototype != null) {
			return prototype.call(parameters);
		}

		if (getNumberOfParameters() != parameters.size()) {
			throw new Exception("Unexpected number of parameters");
		}

		for (RTObject p : parameters) {
			if (p instanceof Void)
				throw new StoryException(
						"Attempting to perform operation on a void value. Did you forget to 'return' a value from a function you called here?");

		}

		List<Value<?>> coercedParams = coerceValuesToSingleType(parameters);
		ValueType coercedType = coercedParams.get(0).getvalueType();

		// Originally CallType gets a type parameter taht is used to do some
		// casting, but we can do without.
		if (coercedType == ValueType.Int) {
			return callType(coercedParams);
		} else if (coercedType == ValueType.Float) {
			return callType(coercedParams);
		} else if (coercedType == ValueType.String) {
			return callType(coercedParams);
		} else if (coercedType == ValueType.DivertTarget) {
			return callType(coercedParams);
		}

		return null;

	}

	private RTObject callType(List<Value<?>> parametersOfSingleType) throws Exception {

		Value<?> param1 = parametersOfSingleType.get(0);
		ValueType valType = param1.getvalueType();
		Value<?> val1 = param1;

		int paramCount = parametersOfSingleType.size();

		if (paramCount == 2 || paramCount == 1) {
			Object opForTypeObj = operationFuncs.get(valType);

			if (opForTypeObj == null) {
				throw new StoryException("Can not perform operation '" + this.getName() + "' on " + valType);
			}

			// Binary
			if (paramCount == 2) {
				Value<?> param2 = parametersOfSingleType.get(1);
				Value<?> val2 = param2;

				BinaryOp opForType = (BinaryOp) opForTypeObj;

				// Return value unknown until it's evaluated
				Object resultVal = opForType.invoke(val1.getValue(), val2.getValue());

				return AbstractValue.create(resultVal);
			} else { // Unary
				UnaryOp opForType = (UnaryOp) opForTypeObj;

				Object resultVal = opForType.invoke(val1.getValue());

				return AbstractValue.create(resultVal);
			}
		} else

		{
			throw new Exception(
					"Unexpected number of parameters to NativeFunctionCall: " + parametersOfSingleType.size());
		}
	}

	List<Value<?>> coerceValuesToSingleType(List<RTObject> parametersIn) throws Exception {
		ValueType valType = ValueType.Int;

		for (RTObject obj : parametersIn) {
			// Find out what the output type is
			// "higher level" types infect both so that binary operations
			// use the same type on both sides. e.g. binary operation of
			// int and float causes the int to be casted to a float.
			Value<?> val = (Value<?>) obj;
			if (val.getvalueType().ordinal() > valType.ordinal()) {
				valType = val.getvalueType();
			}

		}
		// // Coerce to this chosen type
		ArrayList<Value<?>> parametersOut = new ArrayList<Value<?>>();
		for (RTObject __dummyForeachVar2 : parametersIn) {
			Value<?> val = (Value<?>) __dummyForeachVar2;
			Value<?> castedValue = (Value<?>) val.cast(valType);
			parametersOut.add(castedValue);
		}
		return parametersOut;
	}

	public String getName() throws Exception {
		return name;
	}

	public int getNumberOfParameters() throws Exception {
		if (prototype != null) {
			return prototype.getNumberOfParameters();
		} else {
			return numberOfParameters;
		}
	}
	public void setName(String value) throws Exception {
		name = value;
		if (!isPrototype)
			prototype = nativeFunctions.get(name);

	}
	public void setNumberOfParameters(int value) {
		numberOfParameters = value;
	}
	@Override
	public String toString() {
		try {
			return "Native '" + getName() + "'";
		} catch (RuntimeException __dummyCatchVar0) {
			throw __dummyCatchVar0;
		} catch (Exception __dummyCatchVar0) {
			throw new RuntimeException(__dummyCatchVar0);
		}

	}
}
