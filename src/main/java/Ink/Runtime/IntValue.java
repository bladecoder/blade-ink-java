package Ink.Runtime;

public class IntValue  extends Value<Integer> 
{
	@Override
    public ValueType getvalueType() throws Exception {
        return ValueType.Int;
    }

    public boolean getisTruthy() throws Exception {
        return getValue() != 0;
    }

    public IntValue(int intVal) throws Exception {
        super(intVal);
    }

    public IntValue() throws Exception {
        this(0);
    }

    @Override
    public AbstractValue cast(ValueType newType) throws Exception {
        if (newType == getvalueType())
        {
            return this;
        }
         
        if (newType == ValueType.Float)
        {
            return new FloatValue((float)this.getValue());
        }
         
        if (newType == ValueType.String)
        {
            return new StringValue(this.getValue().toString());
        }
         
        throw new Exception("Unexpected type cast of Value to new ValueType");
    }

}


