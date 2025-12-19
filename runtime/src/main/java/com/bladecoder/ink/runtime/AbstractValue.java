package com.bladecoder.ink.runtime;

public abstract class AbstractValue extends RTObject {
    public abstract ValueType getValueType();

    public abstract boolean isTruthy() throws Exception;

    public abstract AbstractValue cast(ValueType newType) throws Exception;

    public abstract Object getValueObject();

    public static AbstractValue create(Object val) {
        // Implicitly lose precision from any doubles we get passed in
        if (val instanceof Double) {
            double doub = (Double) val;
            val = (float) doub;
        }

        if (val instanceof Boolean) {
            return new BoolValue((Boolean) val);
        } else if (val instanceof Integer) {
            return new IntValue((Integer) val);
        } else if (val instanceof Long) {
            return new IntValue(((Long) val).intValue());
        } else if (val instanceof Float) {
            return new FloatValue((Float) val);
        } else if (val instanceof Double) {
            return new FloatValue((((Double) val).floatValue()));
        } else if (val instanceof String) {
            return new StringValue((String) val);
        } else if (val instanceof Path) {
            return new DivertTargetValue((Path) val);
        } else if (val instanceof InkList) {
            return new ListValue((InkList) val);
        }

        return null;
    }

    @Override
    RTObject copy() {
        return create(getValueObject());
    }

    protected StoryException BadCastException(ValueType targetType) throws Exception {
        return new StoryException(
                "Can't cast " + this.getValueObject() + " from " + this.getValueType() + " to " + targetType);
    }
}
