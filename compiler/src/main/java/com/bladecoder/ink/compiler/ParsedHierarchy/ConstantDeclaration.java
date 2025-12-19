package com.bladecoder.ink.compiler.ParsedHierarchy;

public class ConstantDeclaration extends ParsedObject {
    public Identifier constantIdentifier;
    public Expression expression;

    public ConstantDeclaration(Identifier name, Expression assignedExpression) {
        constantIdentifier = name;

        if (assignedExpression != null) {
            expression = addContent(assignedExpression);
        }
    }

    public String getConstantName() {
        return constantIdentifier != null ? constantIdentifier.name : null;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        return null;
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        context.checkForNamingCollisions(this, constantIdentifier, Story.SymbolType.Var, null);
    }

    @Override
    public String getTypeName() {
        return "Constant";
    }
}
