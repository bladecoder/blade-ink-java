//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package com.bladecoder.ink.runtime;


public enum ValueType
{
    // Order is significant for type coersion.
    // If types aren't directly compatible for an operation,
    // they're coerced to the same type, downward.
    // Higher value types "infect" an operation.
    // (This may not be the most sensible thing to do, but it's worked so far!)
    // Used in coersion
    Int,
    Float,
    String,
    // Not used for coersion described above
    DivertTarget,
    VariablePointer
}

