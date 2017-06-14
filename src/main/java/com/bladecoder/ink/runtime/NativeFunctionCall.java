package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
	public static final String Negate = "_"; // distinguish from "-" for
												// subtraction
	public static final String Not = "!";

	public static final String Has = "?";
	public static final String Hasnt = "!?";
	public static final String Intersect = "^";

	public static final String ListMax = "LIST_MAX";
	public static final String ListMin = "LIST_MIN";
	public static final String All = "LIST_ALL";
	public static final String Count = "LIST_COUNT";
	public static final String ValueOfList = "LIST_VALUE";
	public static final String Invert = "LIST_INVERT";

	public static final String NotEquals = "!=";

	public static final String Or = "||";

	public static final String Subtract = "-";

	static void addListBinaryOp(String name, BinaryOp op) {
		addOpToNativeFunc(name, 2, ValueType.List, op);
	}

	static void addListUnaryOp(String name, UnaryOp op) {
		addOpToNativeFunc(name, 1, ValueType.List, op);
	}

	static void addFloatBinaryOp(String name, BinaryOp op) {
		addOpToNativeFunc(name, 2, ValueType.Float, op);
	}

	static void addFloatUnaryOp(String name, UnaryOp op) {
		addOpToNativeFunc(name, 1, ValueType.Float, op);
	}

	static void addIntBinaryOp(String name, BinaryOp op) {
		addOpToNativeFunc(name, 2, ValueType.Int, op);
	}

	static void addIntUnaryOp(String name, UnaryOp op) {
		addOpToNativeFunc(name, 1, ValueType.Int, op);
	}

	static void addOpToNativeFunc(String name, int args, ValueType valType, Object op) {
		NativeFunctionCall nativeFunc = nativeFunctions.get(name);

		// Operations for each data type, for a single operation (e.g. "+")

		if (nativeFunc == null) {
			nativeFunc = new NativeFunctionCall(name, args);
			nativeFunctions.put(name, nativeFunc);
		}

		nativeFunc.addOpFuncForType(valType, op);
	}

	static void addStringBinaryOp(String name, BinaryOp op) {
		addOpToNativeFunc(name, 2, ValueType.String, op);
	}

	public static boolean callExistsWithName(String functionName) {
		generateNativeFunctionsIfNecessary();
		return nativeFunctions.containsKey(functionName);
	}

	public static NativeFunctionCall callWithName(String functionName) {
		return new NativeFunctionCall(functionName);
	}

	static void generateNativeFunctionsIfNecessary() {
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

			addStringBinaryOp(NotEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (!((String) left).equals(right)) ? (Integer) 1 : (Integer) 0;
				}
			});

			// List operations
			addListBinaryOp(Add, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).union((InkList) right);
				}
			});

			addListBinaryOp(Subtract, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).without((InkList) right);
				}
			});

			addListBinaryOp(Has, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).contains((InkList) right) ? (Integer) 1 : (Integer) 0;
				}
			});

			addListBinaryOp(Hasnt, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).contains((InkList) right) ? (Integer) 0 : (Integer) 1;
				}
			});

			addListBinaryOp(Intersect, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).intersect((InkList) right);
				}
			});

			addListBinaryOp(Equal, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).equals(right) ? (Integer) 1 : (Integer) 0;
				}
			});

			addListBinaryOp(Greater, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).size() > 0 && ((InkList) left).greaterThan((InkList) right) ? (Integer) 1
							: (Integer) 0;
				}
			});

			addListBinaryOp(Less, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).lessThan((InkList) right) ? (Integer) 1 : (Integer) 0;
				}
			});

			addListBinaryOp(GreaterThanOrEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).size() > 0 && ((InkList) left).greaterThanOrEquals((InkList) right)
							? (Integer) 1 : (Integer) 0;
				}
			});

			addListBinaryOp(LessThanOrEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return ((InkList) left).size() > 0 && ((InkList) left).lessThanOrEquals((InkList) right)
							? (Integer) 1 : (Integer) 0;
				}
			});

			addListBinaryOp(NotEquals, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (!((InkList) left).equals(right) ? (Integer) 1 : (Integer) 0);
				}
			});
			
			addListBinaryOp(And, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (((InkList) left).size() > 0 && ((InkList) right).size() > 0? (Integer) 1 : (Integer) 0);
				}
			});
			
			addListBinaryOp(Or, new BinaryOp() {
				@Override
				public Object invoke(Object left, Object right) {
					return (((InkList) left).size() > 0 || ((InkList) right).size() > 0? (Integer) 1 : (Integer) 0);
				}
			});

			addListUnaryOp(Not, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).size() == 0 ? (int) 1 : (int) 0;
				}
			});

			// Placeholder to ensure that Invert gets created at all,
			// since this function is never actually run, and is special cased
			// in Call
			addListUnaryOp(Invert, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).getInverse();
				}
			});

			addListUnaryOp(All, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).getAll();
				}
			});

			addListUnaryOp(ListMin, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).minAsList();
				}
			});

			addListUnaryOp(ListMax, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).maxAsList();
				}
			});

			addListUnaryOp(Count, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).size();
				}
			});

			addListUnaryOp(ValueOfList, new UnaryOp() {
				@Override
				public Object invoke(Object val) {
					return ((InkList) val).getMaxItem().getValue();
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
	public NativeFunctionCall() {
		generateNativeFunctionsIfNecessary();
	}

	public NativeFunctionCall(String name) {
		generateNativeFunctionsIfNecessary();
		this.setName(name);
	}

	// Only called internally to generate prototypes
	NativeFunctionCall(String name, int numberOfParamters) {
		isPrototype = true;
		this.setName(name);
		this.setNumberOfParameters(numberOfParamters);
	}

	void addOpFuncForType(ValueType valType, Object op) {
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

		boolean hasList = false;

		for (RTObject p : parameters) {
			if (p instanceof Void)
				throw new StoryException(
						"Attempting to perform operation on a void value. Did you forget to 'return' a value from a function you called here?");

			if (p instanceof ListValue)
				hasList = true;

		}

		// Binary operations on lists are treated outside of the standard
		// coerscion rules
		if (parameters.size() == 2 && hasList)
			return callBinaryListOperation(parameters);

		List<Value<?>> coercedParams = coerceValuesToSingleType(parameters);
		ValueType coercedType = coercedParams.get(0).getValueType();

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
		} else if (coercedType == ValueType.List) {
			return callType(coercedParams);
		}

		return null;

	}

	Value<?> callBinaryListOperation(List<RTObject> parameters) throws StoryException, Exception {
		// List-Int addition/subtraction returns a List (e.g. "alpha" + 1 =
		// "beta")
		if (("+".equals(name) || "-".equals(name)) && parameters.get(0) instanceof ListValue
				&& parameters.get(1) instanceof IntValue)
			return callListIncrementOperation(parameters);

		Value<?> v1 = (Value<?>) parameters.get(0);
		Value<?> v2 = (Value<?>) parameters.get(1);

		// And/or with any other type requires coerscion to bool (int)
		if ((name == "&&" || name == "||")
				&& (v1.getValueType() != ValueType.List || v2.getValueType() != ValueType.List)) {
			BinaryOp op = (BinaryOp) operationFuncs.get(ValueType.Int);
			int result = (int) op.invoke(v1.isTruthy() ? 1 : 0, v2.isTruthy() ? 1 : 0);
			return new IntValue(result);
		}

		// Normal (list • list) operation
		if (v1.getValueType() == ValueType.List && v2.getValueType() == ValueType.List) {
			List<Value<?>> p = new ArrayList<Value<?>>();
			p.add(v1);
			p.add(v2);

			return (Value<?>) callType(p);
		}

		throw new StoryException(
				"Can not call use '" + name + "' operation on " + v1.getValueType() + " and " + v2.getValueType());
	}

	Value<?> callListIncrementOperation(List<RTObject> listIntParams) throws StoryException, Exception {
		ListValue listVal = (ListValue) listIntParams.get(0);
		IntValue intVal = (IntValue) listIntParams.get(1);

		InkList resultRawList = new InkList();

		for (Entry<InkListItem, Integer> listItemWithValue : listVal.getValue().entrySet()) {

			InkListItem listItem = listItemWithValue.getKey();
			Integer listItemValue = listItemWithValue.getValue();

			// Find + or - operation
			BinaryOp intOp = (BinaryOp) operationFuncs.get(ValueType.Int);

			// Return value unknown until it's evaluated
			int targetInt = (int) intOp.invoke(listItemValue, intVal.value);

			// Find this item's origin (linear search should be ok, should be
			// short haha)
			ListDefinition itemOrigin = null;
			for (ListDefinition origin : listVal.getValue().origins) {
				if (origin.getName().equals(listItem.getOriginName())) {
					itemOrigin = origin;
					break;
				}
			}

			if (itemOrigin != null) {
				InkListItem incrementedItem = itemOrigin.getItemWithValue(targetInt);
				if (incrementedItem != null)
					resultRawList.put(incrementedItem, targetInt);
			}
		}

		return new ListValue(resultRawList);
	}

	private RTObject callType(List<Value<?>> parametersOfSingleType) throws StoryException, Exception {

		Value<?> param1 = parametersOfSingleType.get(0);
		ValueType valType = param1.getValueType();
		Value<?> val1 = param1;

		int paramCount = parametersOfSingleType.size();

		if (paramCount == 2 || paramCount == 1) {
			Object opForTypeObj = operationFuncs.get(valType);

			if (opForTypeObj == null) {
				throw new StoryException("Cannot perform operation '" + this.getName() + "' on " + valType);
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
		} else {
			throw new Exception(
					"Unexpected number of parameters to NativeFunctionCall: " + parametersOfSingleType.size());
		}
	}

	List<Value<?>> coerceValuesToSingleType(List<RTObject> parametersIn) throws Exception {
		ValueType valType = ValueType.Int;

		ListValue specialCaseList = null;

		for (RTObject obj : parametersIn) {
			// Find out what the output type is
			// "higher level" types infect both so that binary operations
			// use the same type on both sides. e.g. binary operation of
			// int and float causes the int to be casted to a float.
			Value<?> val = (Value<?>) obj;
			if (val.getValueType().ordinal() > valType.ordinal()) {
				valType = val.getValueType();
			}

			if (val.getValueType() == ValueType.List) {
				specialCaseList = (ListValue) val;
			}
		}

		// // Coerce to this chosen type
		ArrayList<Value<?>> parametersOut = new ArrayList<Value<?>>();
		// for (RTObject p : parametersIn) {
		// Value<?> val = (Value<?>) p;
		// Value<?> castedValue = (Value<?>) val.cast(valType);
		// parametersOut.add(castedValue);

		// Special case: Coercing to Ints to Lists
		// We have to do it early when we have both parameters
		// to hand - so that we can make use of the List's origin
		if (valType == ValueType.List) {

			for (RTObject p : parametersIn) {
				Value<?> val = (Value<?>) p;
				if (val.getValueType() == ValueType.List) {
					parametersOut.add(val);
				} else if (val.getValueType() == ValueType.Int) {
					int intVal = (int) val.getValueObject();
					ListDefinition list = specialCaseList.getValue().getOriginOfMaxItem();
					InkListItem item = list.getItemWithValue(intVal);

					if (item != null) {
						ListValue castedValue = new ListValue(item, intVal);
						parametersOut.add(castedValue);
					} else
						throw new StoryException(
								"Could not find List item with the value " + intVal + " in " + list.getName());
				} else
					throw new StoryException(
							"Cannot mix Lists and " + val.getValueType() + " values in this operation");
			}

		}

		// Normal Coercing (with standard casting)
		else {
			for (RTObject p : parametersIn) {
				Value<?> val = (Value<?>) p;
				Value<?> castedValue = (Value<?>) val.cast(valType);
				parametersOut.add(castedValue);
			}
		}

		return parametersOut;
	}

	public String getName() {
		return name;
	}

	public int getNumberOfParameters() {
		if (prototype != null) {
			return prototype.getNumberOfParameters();
		} else {
			return numberOfParameters;
		}
	}

	public void setName(String value) {
		name = value;
		if (!isPrototype)
			prototype = nativeFunctions.get(name);

	}

	public void setNumberOfParameters(int value) {
		numberOfParameters = value;
	}

	@Override
	public String toString() {
		return "Native '" + getName() + "'";

	}
}
