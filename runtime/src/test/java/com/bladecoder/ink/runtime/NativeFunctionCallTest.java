package com.bladecoder.ink.runtime;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class NativeFunctionCallTest {
    private void assertComparison(RTObject left, RTObject right, boolean equal) throws Exception {
        Assert.assertEquals(
                equal, ((BoolValue) NativeFunctionCall.callWithName("==").call(Arrays.asList(left, right))).getValue());
        Assert.assertEquals(
                !equal,
                ((BoolValue) NativeFunctionCall.callWithName("!=").call(Arrays.asList(left, right))).getValue());
    }

    @Test
    public void integerEqualityUsesNumericValues() throws Exception {
        for (int value : new int[] {19, 127, 128, -129, 7634, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertComparison(new IntValue(value), new IntValue(value), true);
            assertComparison(new IntValue(value), new IntValue(value - 1), false);
        }
    }

    @Test
    public void floatEqualityUsesPrimitiveSemantics() throws Exception {
        assertComparison(new FloatValue(7634f), new FloatValue(7634f), true);
        assertComparison(new FloatValue(1.5f), new FloatValue(2.5f), false);
        assertComparison(new FloatValue(0f), new FloatValue(-0f), true);
        assertComparison(new FloatValue(Float.POSITIVE_INFINITY), new FloatValue(Float.POSITIVE_INFINITY), true);
        FloatValue nan = new FloatValue(Float.NaN);
        assertComparison(nan, nan, false);
    }

    @Test
    public void mixedNumericEqualityUsesCoercedValues() throws Exception {
        assertComparison(new IntValue(7634), new FloatValue(7634f), true);
        assertComparison(new FloatValue(7634f), new IntValue(7634), true);
        assertComparison(new IntValue(7634), new FloatValue(7634.5f), false);
    }
}
