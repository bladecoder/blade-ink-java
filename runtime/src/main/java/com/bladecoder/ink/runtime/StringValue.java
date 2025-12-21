package com.bladecoder.ink.runtime;

public class StringValue extends Value<String> {
    private boolean isInlineWhitespace;

    private boolean isNewline;

    public StringValue() {
        this("");
    }

    public StringValue(String str) {
        super(str);
        // Classify whitespace status
        setIsNewline("\n".equals(getValue()));

        setIsInlineWhitespace(true);
        for (char c : getValue().toCharArray()) {
            if (c != ' ' && c != '\t') {
                setIsInlineWhitespace(false);
                break;
            }
        }
    }

    @Override
    public AbstractValue cast(ValueType newType) throws Exception {
        if (newType == getValueType()) {
            return this;
        }

        if (newType == ValueType.Int) {
            try {
                int parsedInt = Integer.parseInt(getValue());

                return new IntValue(parsedInt);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (newType == ValueType.Float) {
            try {
                float parsedFloat = Float.parseFloat(getValue());

                return new FloatValue(parsedFloat);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        throw BadCastException(newType);
    }

    public boolean isInlineWhitespace() {
        return isInlineWhitespace;
    }

    public boolean isNewline() {
        return isNewline;
    }

    public boolean isNonWhitespace() {
        return !isNewline() && !isInlineWhitespace();
    }

    @Override
    public boolean isTruthy() {
        return getValue().length() > 0;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.String;
    }

    public void setIsInlineWhitespace(boolean value) {
        isInlineWhitespace = value;
    }

    public void setIsNewline(boolean value) {
        isNewline = value;
    }
}
