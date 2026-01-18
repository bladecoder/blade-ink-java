package com.bladecoder.ink.compiler;

import com.bladecoder.ink.compiler.ParsedHierarchy.Identifier;
import com.bladecoder.ink.compiler.ParsedHierarchy.ParsedObject;
import com.bladecoder.ink.compiler.ParsedHierarchy.Story;
import com.bladecoder.ink.compiler.StringParser.StringParser;
import com.bladecoder.ink.compiler.StringParser.StringParser.ParseRule;
import com.bladecoder.ink.compiler.StringParser.StringParser.SpecificParseRule;
import com.bladecoder.ink.compiler.StringParser.StringParserState;
import com.bladecoder.ink.runtime.DebugMetadata;
import com.bladecoder.ink.runtime.Error.ErrorType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InkParser extends StringParser {
    public InkParser(
            String str,
            String filenameForMetadata,
            com.bladecoder.ink.runtime.Error.ErrorHandler externalErrorHandler,
            IFileHandler fileHandler) {
        this(str, filenameForMetadata, externalErrorHandler, null, fileHandler);
    }

    public InkParser(String str) {
        this(str, null, null, null, null);
    }

    private InkParser(
            String str,
            String inkFilename,
            com.bladecoder.ink.runtime.Error.ErrorHandler externalErrorHandler,
            InkParser rootParser,
            IFileHandler fileHandler) {
        super(str);
        filename = inkFilename;
        registerExpressionOperators();
        generateStatementLevelRules();

        this.errorHandler = this::onStringParserError;

        this.externalErrorHandler = externalErrorHandler;

        this.fileHandler = fileHandler != null ? fileHandler : new DefaultFileHandler();

        if (rootParser == null) {
            rootParserRef = this;
            openFilenames = new HashSet<>();
            if (inkFilename != null) {
                String fullRootInkPath = this.fileHandler.resolveInkFilename(inkFilename);
                openFilenames.add(fullRootInkPath);
            }
        } else {
            rootParserRef = rootParser;
        }
    }

    public Story parse() {
        List<ParsedObject> topLevelContent = statementsAtLevel(StatementLevel.Top);
        return new Story(topLevelContent, rootParserRef != this);
    }

    protected <T> List<T> separatedList(SpecificParseRule<T> mainRule, ParseRule separatorRule) {
        T firstElement = parse(mainRule);
        if (firstElement == null) {
            return null;
        }

        List<T> allElements = new ArrayList<>();
        allElements.add(firstElement);

        while (true) {
            int nextElementRuleId = beginRule();

            Object sep = separatorRule.parse();
            if (sep == null) {
                failRule(nextElementRuleId);
                break;
            }

            T nextElement = parse(mainRule);
            if (nextElement == null) {
                failRule(nextElementRuleId);
                break;
            }

            succeedRule(nextElementRuleId, null);
            allElements.add(nextElement);
        }

        return allElements;
    }

    @Override
    protected String preProcessInputString(String str) {
        return new CommentEliminator(str).process();
    }

    protected DebugMetadata createDebugMetadata(
            StringParserState.Element stateAtStart, StringParserState.Element stateAtEnd) {
        DebugMetadata md = new DebugMetadata();
        md.startLineNumber = stateAtStart.lineIndex + 1;
        md.endLineNumber = stateAtEnd.lineIndex + 1;
        md.startCharacterNumber = stateAtStart.characterInLineIndex + 1;
        md.endCharacterNumber = stateAtEnd.characterInLineIndex + 1;
        md.fileName = filename;
        return md;
    }

    @Override
    protected void ruleDidSucceed(
            Object result, StringParserState.Element stateAtStart, StringParserState.Element stateAtEnd) {
        ParsedObject parsedObj = result instanceof ParsedObject ? (ParsedObject) result : null;
        if (parsedObj != null) {
            parsedObj.setDebugMetadata(createDebugMetadata(stateAtStart, stateAtEnd));
            return;
        }

        if (result instanceof List) {
            List<?> parsedListObjs = (List<?>) result;
            for (Object obj : parsedListObjs) {
                ParsedObject parsedListObj = obj instanceof ParsedObject ? (ParsedObject) obj : null;
                if (parsedListObj != null && !parsedListObj.hasOwnDebugMetadata()) {
                    parsedListObj.setDebugMetadata(createDebugMetadata(stateAtStart, stateAtEnd));
                }
            }
        }

        Identifier id = result instanceof Identifier ? (Identifier) result : null;
        if (id != null) {
            id.debugMetadata = createDebugMetadata(stateAtStart, stateAtEnd);
        }
    }

    protected boolean isParsingStringExpression() {
        return getFlag(CustomFlags.ParsingString.flag);
    }

    protected void setParsingStringExpression(boolean value) {
        setFlag(CustomFlags.ParsingString.flag, value);
    }

    protected boolean isTagActive() {
        return getFlag(CustomFlags.TagActive.flag);
    }

    protected void setTagActive(boolean value) {
        setFlag(CustomFlags.TagActive.flag, value);
    }

    protected enum CustomFlags {
        ParsingString(0x1),
        TagActive(0x2);

        final long flag;

        CustomFlags(long flag) {
            this.flag = flag;
        }
    }

    protected static class FlowDecl {
        public com.bladecoder.ink.compiler.ParsedHierarchy.Identifier name;
        public List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument> arguments;
        public boolean isFunction;
    }

    private void onStringParserError(String message, int index, int lineIndex, boolean isWarning) {
        String warningType = isWarning ? "WARNING:" : "ERROR:";
        String fullMessage;

        if (filename != null) {
            fullMessage = String.format("%s '%s' line %d: %s", warningType, filename, lineIndex + 1, message);
        } else {
            fullMessage = String.format("%s line %d: %s", warningType, lineIndex + 1, message);
        }

        if (externalErrorHandler != null) {
            externalErrorHandler.error(fullMessage, isWarning ? ErrorType.Warning : ErrorType.Error);
        } else {
            throw new RuntimeException(fullMessage);
        }
    }

    protected Object endOfLine() {
        return oneOf(this::newline, this::endOfFile);
    }

    protected Object newline() {
        whitespace();

        boolean gotNewline = parseNewline() != null;

        if (!gotNewline) {
            return null;
        }

        return ParseSuccess;
    }

    protected Object endOfFile() {
        whitespace();

        if (!isEndOfInput()) {
            return null;
        }

        return ParseSuccess;
    }

    protected Object multilineWhitespace() {
        List<Object> newlines = oneOrMore(this::newline);
        if (newlines == null) {
            return null;
        }

        int numNewlines = newlines.size();
        if (numNewlines >= 1) {
            return ParseSuccess;
        }

        return null;
    }

    protected Object whitespace() {
        if (parseCharactersFromCharSet(inlineWhitespaceChars) != null) {
            return ParseSuccess;
        }

        return null;
    }

    protected ParseRule spaced(ParseRule rule) {
        return () -> {
            whitespace();

            Object result = parseObject(rule);
            if (result == null) {
                return null;
            }

            whitespace();

            return result;
        };
    }

    protected Object anyWhitespace() {
        boolean anyWhitespace = false;
        while (oneOf(this::whitespace, this::multilineWhitespace) != null) {
            anyWhitespace = true;
        }
        return anyWhitespace ? ParseSuccess : null;
    }

    protected ParseRule multiSpaced(ParseRule rule) {
        return () -> {
            anyWhitespace();

            Object result = parseObject(rule);
            if (result == null) {
                return null;
            }

            anyWhitespace();

            return result;
        };
    }

    protected enum StatementLevel {
        InnerBlock,
        Stitch,
        Knot,
        Top
    }

    protected List<ParsedObject> statementsAtLevel(StatementLevel level) {
        if (level == StatementLevel.InnerBlock) {
            Object badGatherDashCount = parse(this::gatherDashes);
            if (badGatherDashCount != null) {
                error(
                        "You can't use a gather (the dashes) within the { curly braces } context. For multi-line sequences and conditions, you should only use one dash.",
                        false);
            }
        }

        List<ParsedObject> results = interleave(
                optional(this::multilineWhitespace),
                () -> statementAtLevel(level),
                () -> statementsBreakForLevel(level),
                true);
        if (results == null) {
            return null;
        }
        return results;
    }

    protected void registerExpressionOperators() {
        maxBinaryOpLength = 0;
        binaryOperators = new ArrayList<>();

        registerBinaryOperator("&&", 1, false);
        registerBinaryOperator("||", 1, false);
        registerBinaryOperator("and", 1, true);
        registerBinaryOperator("or", 1, true);

        registerBinaryOperator("==", 2, false);
        registerBinaryOperator(">=", 2, false);
        registerBinaryOperator("<=", 2, false);
        registerBinaryOperator("<", 2, false);
        registerBinaryOperator(">", 2, false);
        registerBinaryOperator("!=", 2, false);

        registerBinaryOperator("?", 3, false);
        registerBinaryOperator("has", 3, true);
        registerBinaryOperator("!?", 3, false);
        registerBinaryOperator("hasnt", 3, true);
        registerBinaryOperator("^", 3, false);

        registerBinaryOperator("+", 4, false);
        registerBinaryOperator("-", 5, false);
        registerBinaryOperator("*", 6, false);
        registerBinaryOperator("/", 7, false);

        registerBinaryOperator("%", 8, false);
        registerBinaryOperator("mod", 8, true);
    }

    protected void generateStatementLevelRules() {
        for (StatementLevel level : StatementLevel.values()) {
            List<ParseRule> rulesAtLevel = new ArrayList<>();
            List<ParseRule> breakingRules = new ArrayList<>();

            rulesAtLevel.add(line(this::multiDivert));

            if (level.ordinal() >= StatementLevel.Top.ordinal()) {
                rulesAtLevel.add(this::knotDefinition);
            }

            rulesAtLevel.add(line(this::choice));
            rulesAtLevel.add(line(this::authorWarning));

            if (level.ordinal() > StatementLevel.InnerBlock.ordinal()) {
                rulesAtLevel.add(this::gather);
            }

            if (level.ordinal() >= StatementLevel.Knot.ordinal()) {
                rulesAtLevel.add(this::stitchDefinition);
            }

            rulesAtLevel.add(line(this::listDeclaration));
            rulesAtLevel.add(line(this::variableDeclaration));
            rulesAtLevel.add(line(this::constDeclaration));
            rulesAtLevel.add(line(this::externalDeclaration));

            rulesAtLevel.add(line(this::includeStatement));

            rulesAtLevel.add(this::logicLine);
            rulesAtLevel.add(this::lineOfMixedTextAndLogic);

            if (level.ordinal() <= StatementLevel.Knot.ordinal()) {
                breakingRules.add(this::knotDeclaration);
            }

            if (level.ordinal() <= StatementLevel.Stitch.ordinal()) {
                breakingRules.add(this::stitchDeclaration);
            }

            if (level.ordinal() <= StatementLevel.InnerBlock.ordinal()) {
                breakingRules.add(this::parseDashNotArrow);
                breakingRules.add(stringRule("}"));
            }

            statementRulesAtLevel.put(level, rulesAtLevel);
            statementBreakRulesAtLevel.put(level, breakingRules);
        }
    }

    protected Object statementsBreakForLevel(StatementLevel level) {
        whitespace();

        List<ParseRule> breakRules = statementBreakRulesAtLevel.get(level);
        if (breakRules == null || breakRules.isEmpty()) {
            return null;
        }

        Object breakRuleResult = oneOf(breakRules.toArray(new ParseRule[0]));
        if (breakRuleResult == null) {
            return null;
        }

        return breakRuleResult;
    }

    protected Object statementAtLevel(StatementLevel level) {
        List<ParseRule> rulesAtLevel = statementRulesAtLevel.get(level);
        if (rulesAtLevel == null || rulesAtLevel.isEmpty()) {
            return null;
        }
        Object statement = oneOf(rulesAtLevel.toArray(new ParseRule[0]));

        if (level == StatementLevel.Top && statement instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Return) {
            error("should not have return statement outside of a knot", false);
        }

        return statement;
    }

    protected List<ParsedObject> lineOfMixedTextAndLogic() {
        parse(this::whitespace);

        List<ParsedObject> result = mixedTextAndLogic();
        if (result == null || result.isEmpty()) {
            return null;
        }

        ParsedObject first = result.get(0);
        if (first instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Text) {
            com.bladecoder.ink.compiler.ParsedHierarchy.Text text =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.Text) first;
            if (text.getText().startsWith("return")) {
                warning(
                        "Do you need a '~' before 'return'? If not, perhaps use a glue: <> (since it's lowercase) or rewrite somehow?");
            }
        }

        ParsedObject lastObj = result.get(result.size() - 1);
        if (!(lastObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert)) {
            trimEndWhitespace(result, false);
        }

        endTagIfNecessary(result);

        boolean lineIsPureTag = !result.isEmpty()
                && result.get(0) instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Tag
                && ((com.bladecoder.ink.compiler.ParsedHierarchy.Tag) result.get(0)).isStart;
        if (!lineIsPureTag) {
            result.add(new com.bladecoder.ink.compiler.ParsedHierarchy.Text("\n"));
        }

        expect(this::endOfLine, "end of line", this::skipToNextLine);

        return result;
    }

    protected List<ParsedObject> mixedTextAndLogic() {
        Object disallowedTilda = parseObject(spaced(stringRule("~")));
        if (disallowedTilda != null) {
            error(
                    "You shouldn't use a '~' here - tildas are for logic that's on its own line. To do inline logic, use { curly braces } instead",
                    false);
        }

        List<ParsedObject> results =
                interleave(optional(this::contentText), optional(this::inlineLogicOrGlueOrStartTag));

        if (!parsingChoice) {
            List<ParsedObject> diverts = parse(this::multiDivert);
            if (diverts != null) {
                if (results == null) {
                    results = new ArrayList<>();
                }

                endTagIfNecessary(results);
                trimEndWhitespace(results, true);
                results.addAll(diverts);
            }
        }

        return results;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Text contentText() {
        return contentTextAllowingEscapeChar();
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Text contentTextAllowingEscapeChar() {
        StringBuilder sb = null;

        while (true) {
            String str = parse(this::contentTextNoEscape);
            boolean gotEscapeChar = parseString("\\") != null;

            if (gotEscapeChar || str != null) {
                if (sb == null) {
                    sb = new StringBuilder();
                }

                if (str != null) {
                    sb.append(str);
                }

                if (gotEscapeChar) {
                    char c = parseSingleCharacter();
                    sb.append(c);
                }
            } else {
                break;
            }
        }

        if (sb != null) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.Text(sb.toString());
        }

        return null;
    }

    protected String contentTextNoEscape() {
        if (nonTextPauseCharacters == null) {
            nonTextPauseCharacters = new CharacterSet("-<");
        }

        if (nonTextEndCharacters == null) {
            nonTextEndCharacters = new CharacterSet("{}|\n\r\\#");
            notTextEndCharactersChoice = new CharacterSet(nonTextEndCharacters);
            notTextEndCharactersChoice.addCharacters("[]");
            notTextEndCharactersString = new CharacterSet(nonTextEndCharacters);
            notTextEndCharactersString.addCharacters("\"");
        }

        ParseRule nonTextRule =
                () -> oneOf(this::parseDivertArrow, this::parseThreadArrow, this::endOfLine, this::glue);

        CharacterSet endChars;
        if (isParsingStringExpression()) {
            endChars = notTextEndCharactersString;
        } else if (parsingChoice) {
            endChars = notTextEndCharactersChoice;
        } else {
            endChars = nonTextEndCharacters;
        }

        return parseUntil(nonTextRule, nonTextPauseCharacters, endChars);
    }

    private void trimEndWhitespace(List<ParsedObject> mixedTextAndLogicResults, boolean terminateWithSpace) {
        if (mixedTextAndLogicResults.isEmpty()) {
            return;
        }
        int lastObjIdx = mixedTextAndLogicResults.size() - 1;
        ParsedObject lastObj = mixedTextAndLogicResults.get(lastObjIdx);
        if (lastObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Text) {
            com.bladecoder.ink.compiler.ParsedHierarchy.Text text =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.Text) lastObj;
            text.setText(text.getText().replaceAll("[ \\t]+$", ""));

            if (terminateWithSpace) {
                text.setText(text.getText() + " ");
            } else if (text.getText().isEmpty()) {
                mixedTextAndLogicResults.remove(lastObjIdx);
                trimEndWhitespace(mixedTextAndLogicResults, false);
            }
        }
    }

    private void endTagIfNecessary(List<ParsedObject> results) {
        if (isTagActive()) {
            if (results != null) {
                com.bladecoder.ink.compiler.ParsedHierarchy.Tag tag =
                        new com.bladecoder.ink.compiler.ParsedHierarchy.Tag();
                tag.isStart = false;
                results.add(tag);
            }
            setTagActive(false);
        }
    }

    private void endTagIfNecessary(com.bladecoder.ink.compiler.ParsedHierarchy.ContentList contentList) {
        if (isTagActive()) {
            if (contentList != null) {
                com.bladecoder.ink.compiler.ParsedHierarchy.Tag tag =
                        new com.bladecoder.ink.compiler.ParsedHierarchy.Tag();
                tag.isStart = false;
                contentList.addContent(tag);
            }
            setTagActive(false);
        }
    }

    protected Object skipToNextLine() {
        parseUntilCharactersFromString("\n\r");
        parseNewline();
        return ParseSuccess;
    }

    protected ParseRule line(ParseRule inlineRule) {
        return () -> {
            Object result = parseObject(inlineRule);
            if (result == null) {
                return null;
            }

            expect(this::endOfLine, "end of line", this::skipToNextLine);

            return result;
        };
    }

    private ParsedObject inlineLogicOrGlueOrStartTag() {
        return (ParsedObject) oneOf(this::inlineLogic, this::glue, this::startTag);
    }

    protected ParsedObject tempDeclarationOrAssignment() {
        whitespace();

        boolean isNewDeclaration = parseTempKeyword();

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier varIdentifier = null;
        if (isNewDeclaration) {
            varIdentifier = (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier)
                    expect(this::identifierWithMetadata, "variable name");
        } else {
            varIdentifier =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        }

        if (varIdentifier == null) {
            return null;
        }

        whitespace();

        boolean isIncrement = parseString("+") != null;
        boolean isDecrement = parseString("-") != null;
        if (isIncrement && isDecrement) {
            error("Unexpected sequence '+-'", false);
        }

        if (parseString("=") == null) {
            if (isNewDeclaration) {
                error("Expected '='", false);
            }
            return null;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression assignedExpression =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Expression)
                        expect(this::expression, "value expression to be assigned");

        if (isIncrement || isDecrement) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.IncDecExpression(
                    varIdentifier, assignedExpression, isIncrement);
        } else {
            com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment result =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment(
                            varIdentifier, assignedExpression);
            result.isNewTemporaryDeclaration = isNewDeclaration;
            return result;
        }
    }

    protected void disallowIncrement(ParsedObject expr) {
        if (expr instanceof com.bladecoder.ink.compiler.ParsedHierarchy.IncDecExpression) {
            error("Can't use increment/decrement here. It can only be used on a ~ line", false);
        }
    }

    protected boolean parseTempKeyword() {
        int ruleId = beginRule();

        String id = parse(this::identifier);
        if ("temp".equals(id)) {
            succeedRule(ruleId, null);
            return true;
        }

        failRule(ruleId);
        return false;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Return returnStatement() {
        whitespace();

        String returnOrDone = parse(this::identifier);
        if (!"return".equals(returnOrDone)) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr = expression();

        return new com.bladecoder.ink.compiler.ParsedHierarchy.Return(expr);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expression() {
        return expression(0);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expression(int minimumPrecedence) {
        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr = expressionUnary();
        if (expr == null) {
            return null;
        }

        whitespace();

        while (true) {
            int ruleId = beginRule();

            InfixOperator infixOp = parseInfixOperator();
            if (infixOp != null && infixOp.precedence > minimumPrecedence) {
                String expectationMessage = String.format("right side of '%s' expression", infixOp.type);
                com.bladecoder.ink.compiler.ParsedHierarchy.Expression leftExpr = expr;
                InfixOperator opSnapshot = infixOp;
                com.bladecoder.ink.compiler.ParsedHierarchy.Expression multiaryExpr =
                        (com.bladecoder.ink.compiler.ParsedHierarchy.Expression)
                                expect(() -> expressionInfixRight(leftExpr, opSnapshot), expectationMessage, null);
                if (multiaryExpr == null) {
                    failRule(ruleId);
                    return null;
                }

                expr = (com.bladecoder.ink.compiler.ParsedHierarchy.Expression) succeedRule(ruleId, multiaryExpr);
                continue;
            }

            failRule(ruleId);
            break;
        }

        whitespace();

        return expr;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionUnary() {
        com.bladecoder.ink.compiler.ParsedHierarchy.Expression divertTarget =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Expression) parse(this::expressionDivertTarget);
        if (divertTarget != null) {
            return divertTarget;
        }

        String prefixOp = (String) oneOf(stringRule("-"), stringRule("!"));
        if (prefixOp == null) {
            prefixOp = parse(this::expressionNot);
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Expression) oneOf(
                        this::expressionList,
                        this::expressionParen,
                        this::expressionFunctionCall,
                        this::expressionVariableName,
                        this::expressionLiteral);

        if (expr == null && prefixOp != null) {
            expr = expressionUnary();
        }

        if (expr == null) {
            return null;
        }

        if (prefixOp != null) {
            expr = com.bladecoder.ink.compiler.ParsedHierarchy.UnaryExpression.withInner(expr, prefixOp);
        }

        whitespace();

        String postfixOp = (String) oneOf(stringRule("++"), stringRule("--"));
        if (postfixOp != null) {
            boolean isInc = "++".equals(postfixOp);

            if (!(expr instanceof com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference)) {
                error("can only increment and decrement variables, but saw '" + expr + "'", false);
            } else {
                com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference varRef =
                        (com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference) expr;
                expr = new com.bladecoder.ink.compiler.ParsedHierarchy.IncDecExpression(varRef.getIdentifier(), isInc);
            }
        }

        return expr;
    }

    protected String expressionNot() {
        String id = identifier();
        if ("not".equals(id)) {
            return id;
        }

        return null;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionLiteral() {
        return (com.bladecoder.ink.compiler.ParsedHierarchy.Expression)
                oneOf(this::expressionFloat, this::expressionInt, this::expressionBool, this::expressionString);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionDivertTarget() {
        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Divert divert = parse(this::singleDivert);
        if (divert == null) {
            return null;
        }

        if (divert.isThread) {
            return null;
        }

        whitespace();

        return new com.bladecoder.ink.compiler.ParsedHierarchy.DivertTarget(divert);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Number expressionInt() {
        Integer intOrNull = parseInt();
        if (intOrNull == null) {
            return null;
        }
        return new com.bladecoder.ink.compiler.ParsedHierarchy.Number(intOrNull);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Number expressionFloat() {
        Float floatOrNull = parseFloat();
        if (floatOrNull == null) {
            return null;
        }
        return new com.bladecoder.ink.compiler.ParsedHierarchy.Number(floatOrNull);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression expressionString() {
        String openQuote = parseString("\"");
        if (openQuote == null) {
            return null;
        }

        setParsingStringExpression(true);

        List<ParsedObject> textAndLogic = parse(this::mixedTextAndLogic);

        expect(stringRule("\""), "close quote for string expression", null);

        setParsingStringExpression(false);

        if (textAndLogic == null) {
            textAndLogic = new ArrayList<>();
            textAndLogic.add(new com.bladecoder.ink.compiler.ParsedHierarchy.Text(""));
        } else {
            for (ParsedObject obj : textAndLogic) {
                if (obj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert) {
                    error("String expressions cannot contain diverts (->)", false);
                    break;
                }
            }
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression(textAndLogic);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Number expressionBool() {
        String id = parse(this::identifier);
        if ("true".equals(id)) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.Number(true);
        } else if ("false".equals(id)) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.Number(false);
        }

        return null;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionFunctionCall() {
        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier iden =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (iden == null) {
            return null;
        }

        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.Expression> arguments =
                parse(this::expressionFunctionCallArguments);
        if (arguments == null) {
            return null;
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.FunctionCall(iden, arguments);
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.Expression> expressionFunctionCallArguments() {
        if (parseString("(") == null) {
            return null;
        }

        ParseRule commas = exclude(stringRule(","));
        List<com.bladecoder.ink.compiler.ParsedHierarchy.Expression> arguments =
                interleave(this::expression, commas, null, true);
        if (arguments == null) {
            arguments = new ArrayList<>();
        }

        whitespace();

        expect(stringRule(")"), "closing ')' for function call", null);

        return arguments;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionVariableName() {
        List<com.bladecoder.ink.compiler.ParsedHierarchy.Identifier> path =
                interleave(this::identifierWithMetadata, exclude(spaced(stringRule("."))), null, true);

        if (path == null
                || path.isEmpty()
                || com.bladecoder.ink.compiler.ParsedHierarchy.Story.isReservedKeyword(path.get(0).name)) {
            return null;
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference(path);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionParen() {
        if (parseString("(") == null) {
            return null;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression innerExpr = parse(this::expression);
        if (innerExpr == null) {
            return null;
        }

        whitespace();

        expect(stringRule(")"), "closing parenthesis ')' for expression", null);

        return innerExpr;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression expressionInfixRight(
            com.bladecoder.ink.compiler.ParsedHierarchy.Expression left, InfixOperator op) {
        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression right = parse(() -> expression(op.precedence));
        if (right != null) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.BinaryExpression(left, right, op.type);
        }

        return null;
    }

    private InfixOperator parseInfixOperator() {
        for (InfixOperator op : binaryOperators) {
            int ruleId = beginRule();

            if (parseString(op.type) != null) {
                if (op.requireWhitespace) {
                    if (whitespace() == null) {
                        failRule(ruleId);
                        continue;
                    }
                }

                return (InfixOperator) succeedRule(ruleId, op);
            }

            failRule(ruleId);
        }

        return null;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.List expressionList() {
        whitespace();

        if (parseString("(") == null) {
            return null;
        }

        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.Identifier> memberNames =
                separatedList(this::listMember, spaced(stringRule(",")));

        whitespace();

        if (parseString(")") == null) {
            return null;
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.List(memberNames);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Identifier listMember() {
        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier identifier =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (identifier == null) {
            return null;
        }

        String dot = parseString(".");
        if (dot != null) {
            com.bladecoder.ink.compiler.ParsedHierarchy.Identifier identifier2 =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier)
                            expect(this::identifierWithMetadata, "element name within the set " + identifier, null);
            identifier.name = identifier.name + "." + (identifier2 != null ? identifier2.name : "");
        }

        whitespace();

        return identifier;
    }

    private void registerBinaryOperator(String op, int precedence, boolean requireWhitespace) {
        binaryOperators.add(new InfixOperator(op, precedence, requireWhitespace));
        maxBinaryOpLength = Math.max(maxBinaryOpLength, op.length());
    }

    private void registerBinaryOperator(String op, int precedence) {
        registerBinaryOperator(op, precedence, false);
    }

    protected static class InfixOperator {
        public final String type;
        public final int precedence;
        public final boolean requireWhitespace;

        public InfixOperator(String type, int precedence, boolean requireWhitespace) {
            this.type = type;
            this.precedence = precedence;
            this.requireWhitespace = requireWhitespace;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    private List<ParsedObject> multiDivert() {
        whitespace();

        List<ParsedObject> diverts = null;

        com.bladecoder.ink.compiler.ParsedHierarchy.Divert threadDivert = parse(this::startThread);
        if (threadDivert != null) {
            diverts = new ArrayList<>();
            diverts.add(threadDivert);
            return diverts;
        }

        List<Object> arrowsAndDiverts =
                interleave(this::parseDivertArrowOrTunnelOnwards, this::divertIdentifierWithArguments, null, true);
        if (arrowsAndDiverts == null) {
            return null;
        }

        diverts = new ArrayList<>();

        endTagIfNecessary(diverts);

        for (int i = 0; i < arrowsAndDiverts.size(); ++i) {
            boolean isArrow = (i % 2) == 0;

            if (isArrow) {
                if ("->->".equals(arrowsAndDiverts.get(i))) {
                    boolean tunnelOnwardsPlacementValid =
                            (i == 0 || i == arrowsAndDiverts.size() - 1 || i == arrowsAndDiverts.size() - 2);
                    if (!tunnelOnwardsPlacementValid) {
                        error("Tunnel onwards '->->' must only come at the begining or the start of a divert", false);
                    }

                    com.bladecoder.ink.compiler.ParsedHierarchy.TunnelOnwards tunnelOnwards =
                            new com.bladecoder.ink.compiler.ParsedHierarchy.TunnelOnwards();
                    if (i < arrowsAndDiverts.size() - 1) {
                        com.bladecoder.ink.compiler.ParsedHierarchy.Divert tunnelOnwardDivert =
                                (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) arrowsAndDiverts.get(i + 1);
                        tunnelOnwards.divertAfter = tunnelOnwardDivert;
                        tunnelOnwards.addContent(tunnelOnwardDivert);
                    }

                    diverts.add(tunnelOnwards);
                    break;
                }
            } else {
                com.bladecoder.ink.compiler.ParsedHierarchy.Divert divert =
                        (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) arrowsAndDiverts.get(i);
                if (i < arrowsAndDiverts.size() - 1) {
                    divert.isTunnel = true;
                }
                diverts.add(divert);
            }
        }

        if (diverts.isEmpty() && arrowsAndDiverts.size() == 1) {
            com.bladecoder.ink.compiler.ParsedHierarchy.Divert gatherDivert =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.Divert((ParsedObject) null);
            gatherDivert.isEmpty = true;
            diverts.add(gatherDivert);

            if (!parsingChoice) {
                error("Empty diverts (->) are only valid on choices", false);
            }
        }

        return diverts;
    }

    private Object parseDivertArrow() {
        return parseString("->");
    }

    private Object parseThreadArrow() {
        return parseString("<-");
    }

    protected ParsedObject logicLine() {
        whitespace();

        if (parseString("~") == null) {
            return null;
        }

        whitespace();

        ParseRule afterTilda = () -> oneOf(this::returnStatement, this::tempDeclarationOrAssignment, this::expression);

        ParsedObject result = (ParsedObject) expect(afterTilda, "expression after '~'", this::skipToNextLine);

        if (result == null) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList();
        }

        if (result instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Expression
                && !(result instanceof com.bladecoder.ink.compiler.ParsedHierarchy.FunctionCall
                        || result instanceof com.bladecoder.ink.compiler.ParsedHierarchy.IncDecExpression)) {
            com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference varRef =
                    result instanceof com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference
                            ? (com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference) result
                            : null;
            if (varRef != null && "include".equals(varRef.getName())) {
                error(
                        "'~ include' is no longer the correct syntax - please use 'INCLUDE your_filename.ink', without the tilda, and in block capitals.",
                        false);
            } else {
                error(
                        "Logic following a '~' can't be that type of expression. It can only be something like:\n\t~ return\n\t~ var x = blah\n\t~ x++\n\t~ myFunction()",
                        false);
            }
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.FunctionCall funCall =
                result instanceof com.bladecoder.ink.compiler.ParsedHierarchy.FunctionCall
                        ? (com.bladecoder.ink.compiler.ParsedHierarchy.FunctionCall) result
                        : null;
        if (funCall != null) {
            funCall.shouldPopReturnedValue = true;
        }

        if (result.find(com.bladecoder.ink.compiler.ParsedHierarchy.FunctionCall.class) != null) {
            result = new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(
                    result, new com.bladecoder.ink.compiler.ParsedHierarchy.Text("\n"));
        }

        expect(this::endOfLine, "end of line", this::skipToNextLine);

        return result;
    }

    protected ParsedObject variableDeclaration() {
        whitespace();

        String id = parse(this::identifier);
        if (!"VAR".equals(id)) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier varName =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier)
                        expect(this::identifierWithMetadata, "variable name");

        whitespace();

        expect(
                stringRule("="),
                "the '=' for an assignment of a value, e.g. '= 5' (initial values are mandatory)",
                null);

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression definition =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Expression)
                        expect(this::expression, "initial value for ", null);

        if (definition != null) {
            if (!(definition instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Number
                    || definition instanceof com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression
                    || definition instanceof com.bladecoder.ink.compiler.ParsedHierarchy.DivertTarget
                    || definition instanceof com.bladecoder.ink.compiler.ParsedHierarchy.VariableReference
                    || definition instanceof com.bladecoder.ink.compiler.ParsedHierarchy.List)) {
                error("initial value for a variable must be a number, constant, list or divert target", false);
            }

            if (parse(this::listElementDefinitionSeparator) != null) {
                error("Unexpected ','. If you're trying to declare a new list, use the LIST keyword, not VAR", false);
            } else if (definition instanceof com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression) {
                com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression strExpr =
                        (com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression) definition;
                if (!strExpr.isSingleString()) {
                    error("Constant strings cannot contain any logic.", false);
                }
            }

            com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment result =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment(varName, definition);
            result.isGlobalDeclaration = true;
            return result;
        }

        return null;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment listDeclaration() {
        whitespace();

        String id = parse(this::identifier);
        if (!"LIST".equals(id)) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier varName =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier)
                        expect(this::identifierWithMetadata, "list name");

        whitespace();

        expect(stringRule("="), "the '=' for an assignment of the list definition", null);

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition definition =
                (com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition)
                        expect(this::listDefinition, "list item names", null);

        if (definition != null) {
            definition.identifier = varName;
            return new com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment(varName, definition);
        }

        return null;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition listDefinition() {
        anyWhitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.ListElementDefinition> allElements =
                separatedList(this::listElementDefinition, this::listElementDefinitionSeparator);
        if (allElements == null) {
            return null;
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition(allElements);
    }

    protected String listElementDefinitionSeparator() {
        anyWhitespace();

        if (parseString(",") == null) {
            return null;
        }

        anyWhitespace();

        return ",";
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.ListElementDefinition listElementDefinition() {
        boolean inInitialList = parseString("(") != null;
        boolean needsToCloseParen = inInitialList;

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier name =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (name == null) {
            return null;
        }

        whitespace();

        if (inInitialList) {
            if (parseString(")") != null) {
                needsToCloseParen = false;
                whitespace();
            }
        }

        Integer elementValue = null;
        if (parseString("=") != null) {
            whitespace();

            com.bladecoder.ink.compiler.ParsedHierarchy.Number elementValueNum =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.Number)
                            expect(this::expressionInt, "value to be assigned to list item", null);
            if (elementValueNum != null && elementValueNum.value instanceof Integer) {
                elementValue = (Integer) elementValueNum.value;
            }

            if (needsToCloseParen) {
                whitespace();
                if (parseString(")") != null) {
                    needsToCloseParen = false;
                }
            }
        }

        if (needsToCloseParen) {
            error("Expected closing ')'", false);
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.ListElementDefinition(name, inInitialList, elementValue);
    }

    protected ParsedObject constDeclaration() {
        whitespace();

        String id = parse(this::identifier);
        if (!"CONST".equals(id)) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier varName =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier)
                        expect(this::identifierWithMetadata, "constant name");

        whitespace();

        expect(
                stringRule("="),
                "the '=' for an assignment of a value, e.g. '= 5' (initial values are mandatory)",
                null);

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Expression)
                        expect(this::expression, "initial value for ", null);
        if (!(expr instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Number
                || expr instanceof com.bladecoder.ink.compiler.ParsedHierarchy.DivertTarget
                || expr instanceof com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression)) {
            error("initial value for a constant must be a number or divert target", false);
        } else if (expr instanceof com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression) {
            com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression strExpr =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.StringExpression) expr;
            if (!strExpr.isSingleString()) {
                error("Constant strings cannot contain any logic.", false);
            }
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.ConstantDeclaration(varName, expr);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.AuthorWarning authorWarning() {
        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier identifier =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (identifier == null || !"TODO".equals(identifier.name)) {
            return null;
        }

        whitespace();

        parseString(":");

        whitespace();

        String message = parseUntilCharactersFromString("\n\r");

        return new com.bladecoder.ink.compiler.ParsedHierarchy.AuthorWarning(message);
    }

    protected Object includeStatement() {
        whitespace();

        if (parseString("INCLUDE") == null) {
            return null;
        }

        whitespace();

        String filename =
                (String) expect(() -> parseUntilCharactersFromString("\n\r"), "filename for include statement", null);
        if (filename != null) {
            filename = filename.trim();
        }

        String fullFilename = rootParserRef.fileHandler.resolveInkFilename(filename);

        if (filenameIsAlreadyOpen(fullFilename)) {
            error("Recursive INCLUDE detected: '" + fullFilename + "' is already open.", false);
            parseUntilCharactersFromString("\r\n");
            return new com.bladecoder.ink.compiler.ParsedHierarchy.IncludedFile(null);
        } else {
            addOpenFilename(fullFilename);
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Story includedStory = null;
        String includedString = null;
        try {
            includedString = rootParserRef.fileHandler.loadInkFileContents(fullFilename);
        } catch (Exception e) {
            error("Failed to load: '" + filename + "'", false);
        }

        if (includedString != null) {
            InkParser parser =
                    new InkParser(includedString, filename, externalErrorHandler, rootParserRef, fileHandler);
            includedStory = parser.parse();
        }

        removeOpenFilename(fullFilename);

        return new com.bladecoder.ink.compiler.ParsedHierarchy.IncludedFile(includedStory);
    }

    private boolean filenameIsAlreadyOpen(String fullFilename) {
        return rootParserRef.openFilenames.contains(fullFilename);
    }

    private void addOpenFilename(String fullFilename) {
        rootParserRef.openFilenames.add(fullFilename);
    }

    private void removeOpenFilename(String fullFilename) {
        rootParserRef.openFilenames.remove(fullFilename);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Choice choice() {
        boolean onceOnlyChoice = true;
        List<String> bullets = interleave(optionalExclude(this::whitespace), stringRule("*"), null, true);
        if (bullets == null) {
            bullets = interleave(optionalExclude(this::whitespace), stringRule("+"), null, true);
            if (bullets == null) {
                return null;
            }

            onceOnlyChoice = false;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier optionalName =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::bracketedName);

        whitespace();

        if (optionalName != null) {
            newline();
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression conditionExpr = parse(this::choiceCondition);

        whitespace();

        if (parsingChoice) {
            throw new RuntimeException("Already parsing a choice - shouldn't have nested choices");
        }
        parsingChoice = true;

        com.bladecoder.ink.compiler.ParsedHierarchy.ContentList startContent = null;
        List<ParsedObject> startTextAndLogic = parse(this::mixedTextAndLogic);
        if (startTextAndLogic != null) {
            startContent = new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(startTextAndLogic);
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.ContentList optionOnlyContent = null;
        com.bladecoder.ink.compiler.ParsedHierarchy.ContentList innerContent = null;

        boolean hasWeaveStyleInlineBrackets = parseString("[") != null;
        if (hasWeaveStyleInlineBrackets) {
            endTagIfNecessary(startContent);

            List<ParsedObject> optionOnlyTextAndLogic = parse(this::mixedTextAndLogic);
            if (optionOnlyTextAndLogic != null) {
                optionOnlyContent = new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(optionOnlyTextAndLogic);
            }

            expect(stringRule("]"), "closing ']' for weave-style option", null);

            endTagIfNecessary(optionOnlyContent);

            List<ParsedObject> innerTextAndLogic = parse(this::mixedTextAndLogic);
            if (innerTextAndLogic != null) {
                innerContent = new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(innerTextAndLogic);
            }
        }

        whitespace();

        endTagIfNecessary(innerContent != null ? innerContent : startContent);

        List<ParsedObject> diverts = parse(this::multiDivert);

        parsingChoice = false;

        whitespace();

        boolean emptyContent = startContent == null && innerContent == null && optionOnlyContent == null;
        if (emptyContent && diverts == null) {
            warning(
                    "Choice is completely empty. Interpretting as a default fallback choice. Add a divert arrow to remove this warning: * ->");
        } else if (startContent == null && hasWeaveStyleInlineBrackets && optionOnlyContent == null) {
            warning("Blank choice - if you intended a default fallback choice, use the `* ->` syntax");
        }

        if (innerContent == null) {
            innerContent = new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList();
        }

        endTagIfNecessary(innerContent);

        if (diverts != null) {
            for (ParsedObject divObj : diverts) {
                com.bladecoder.ink.compiler.ParsedHierarchy.Divert div =
                        divObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert
                                ? (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) divObj
                                : null;

                if (div != null && div.isEmpty) {
                    continue;
                }

                innerContent.addContent(divObj);
            }
        }

        innerContent.addContent(new com.bladecoder.ink.compiler.ParsedHierarchy.Text("\n"));

        com.bladecoder.ink.compiler.ParsedHierarchy.Choice choice =
                new com.bladecoder.ink.compiler.ParsedHierarchy.Choice(startContent, optionOnlyContent, innerContent);
        choice.identifier = optionalName;
        choice.indentationDepth = bullets.size();
        choice.hasWeaveStyleInlineBrackets = hasWeaveStyleInlineBrackets;
        choice.setCondition(conditionExpr);
        choice.onceOnly = onceOnlyChoice;
        choice.isInvisibleDefault = emptyContent;

        return choice;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression choiceCondition() {
        List<com.bladecoder.ink.compiler.ParsedHierarchy.Expression> conditions =
                interleave(this::choiceSingleCondition, this::choiceConditionsSpace, null, true);
        if (conditions == null) {
            return null;
        } else if (conditions.size() == 1) {
            return conditions.get(0);
        }
        return new com.bladecoder.ink.compiler.ParsedHierarchy.MultipleConditionExpression(conditions);
    }

    protected Object choiceConditionsSpace() {
        newline();
        whitespace();
        return ParseSuccess;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression choiceSingleCondition() {
        if (parseString("{") == null) {
            return null;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression condExpr =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Expression)
                        expect(this::expression, "choice condition inside { }", null);
        disallowIncrement(condExpr);

        expect(stringRule("}"), "closing '}' for choice condition", null);

        return condExpr;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Gather gather() {
        Object gatherDashCountObj = parse(this::gatherDashes);
        if (gatherDashCountObj == null) {
            return null;
        }

        int gatherDashCount = (int) gatherDashCountObj;

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier optionalName =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::bracketedName);

        com.bladecoder.ink.compiler.ParsedHierarchy.Gather gather =
                new com.bladecoder.ink.compiler.ParsedHierarchy.Gather(optionalName, gatherDashCount);

        newline();

        return gather;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Identifier bracketedName() {
        if (parseString("(") == null) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier name =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (name == null) {
            return null;
        }

        whitespace();

        expect(stringRule(")"), "closing ')' for bracketed name", null);

        return name;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Knot knotDefinition() {
        FlowDecl knotDecl = parse(this::knotDeclaration);
        if (knotDecl == null) {
            return null;
        }

        expect(this::endOfLine, "end of line after knot name definition", this::skipToNextLine);

        ParseRule innerKnotStatements = () -> statementsAtLevel(StatementLevel.Knot);

        List<ParsedObject> content = (List<ParsedObject>)
                expect(innerKnotStatements, "at least one line within the knot", this::knotStitchNoContentRecoveryRule);

        return new com.bladecoder.ink.compiler.ParsedHierarchy.Knot(
                knotDecl.name, content, knotDecl.arguments, knotDecl.isFunction);
    }

    protected FlowDecl knotDeclaration() {
        whitespace();

        if (knotTitleEquals() == null) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier identifier =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier knotName;

        boolean isFunc = identifier != null && "function".equals(identifier.name);
        if (isFunc) {
            expect(this::whitespace, "whitespace after the 'function' keyword", null);
            knotName = (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        } else {
            knotName = identifier;
        }

        if (knotName == null) {
            error("Expected the name of the " + (isFunc ? "function" : "knot"), false);
            knotName = new com.bladecoder.ink.compiler.ParsedHierarchy.Identifier();
            knotName.name = "";
        }

        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument> parameterNames =
                parse(this::bracketedKnotDeclArguments);

        whitespace();

        parse(this::knotTitleEquals);

        FlowDecl decl = new FlowDecl();
        decl.name = knotName;
        decl.arguments = parameterNames;
        decl.isFunction = isFunc;
        return decl;
    }

    protected String knotTitleEquals() {
        String multiEquals = parseCharactersFromString("=");
        if (multiEquals == null || multiEquals.length() <= 1) {
            return null;
        }
        return multiEquals;
    }

    protected ParsedObject stitchDefinition() {
        FlowDecl decl = parse(this::stitchDeclaration);
        if (decl == null) {
            return null;
        }

        expect(this::endOfLine, "end of line after stitch name", this::skipToNextLine);

        ParseRule innerStitchStatements = () -> statementsAtLevel(StatementLevel.Stitch);

        List<ParsedObject> content = (List<ParsedObject>) expect(
                innerStitchStatements, "at least one line within the stitch", this::knotStitchNoContentRecoveryRule);

        return new com.bladecoder.ink.compiler.ParsedHierarchy.Stitch(
                decl.name, content, decl.arguments, decl.isFunction);
    }

    protected FlowDecl stitchDeclaration() {
        whitespace();

        if (parseString("=") == null) {
            return null;
        }

        if (parseString("=") != null) {
            return null;
        }

        whitespace();

        boolean isFunc = parseString("function") != null;
        if (isFunc) {
            whitespace();
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier stitchName =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (stitchName == null) {
            return null;
        }

        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument> flowArgs =
                parse(this::bracketedKnotDeclArguments);

        whitespace();

        FlowDecl decl = new FlowDecl();
        decl.name = stitchName;
        decl.arguments = flowArgs;
        decl.isFunction = isFunc;
        return decl;
    }

    protected Object knotStitchNoContentRecoveryRule() {
        parseUntil(this::knotDeclaration, new CharacterSet("="), null);

        List<ParsedObject> recoveredFlowContent = new ArrayList<>();
        recoveredFlowContent.add(new com.bladecoder.ink.compiler.ParsedHierarchy.Text("<ERROR IN FLOW>"));
        return recoveredFlowContent;
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument> bracketedKnotDeclArguments() {
        if (parseString("(") == null) {
            return null;
        }

        List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument> flowArguments =
                interleave(spaced(this::flowDeclArgument), exclude(stringRule(",")), null, true);

        expect(stringRule(")"), "closing ')' for parameter list", null);

        if (flowArguments == null) {
            flowArguments = new ArrayList<>();
        }

        return flowArguments;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument flowDeclArgument() {
        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier firstIden =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        whitespace();
        Object divertArrow = parse(this::parseDivertArrow);
        whitespace();
        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier secondIden =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);

        if (firstIden == null && secondIden == null) {
            return null;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument flowArg =
                new com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument();
        if (divertArrow != null) {
            flowArg.isDivertTarget = true;
        }

        if (firstIden != null && "ref".equals(firstIden.name)) {
            if (secondIden == null) {
                error("Expected an parameter name after 'ref'", false);
            }

            flowArg.identifier = secondIden;
            flowArg.isByReference = true;
        } else {
            if (flowArg.isDivertTarget) {
                flowArg.identifier = secondIden;
            } else {
                flowArg.identifier = firstIden;
            }

            if (flowArg.identifier == null) {
                error("Expected an parameter name", false);
            }

            flowArg.isByReference = false;
        }

        return flowArg;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.ExternalDeclaration externalDeclaration() {
        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier external =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (external == null || !"EXTERNAL".equals(external.name)) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier funcIdentifier =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier)
                        expect(this::identifierWithMetadata, "name of external function", null);
        if (funcIdentifier == null) {
            funcIdentifier = new com.bladecoder.ink.compiler.ParsedHierarchy.Identifier();
        }

        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument> parameterNames =
                (List<com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument>) expect(
                        this::bracketedKnotDeclArguments,
                        "declaration of arguments for EXTERNAL, even if empty, i.e. 'EXTERNAL " + funcIdentifier
                                + "()'",
                        null);
        if (parameterNames == null) {
            parameterNames = new ArrayList<>();
        }

        List<String> argNames = new ArrayList<>();
        for (com.bladecoder.ink.compiler.ParsedHierarchy.FlowBase.Argument arg : parameterNames) {
            argNames.add(arg.identifier != null ? arg.identifier.name : null);
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.ExternalDeclaration(funcIdentifier, argNames);
    }

    protected ParsedObject inlineLogic() {
        if (parseString("{") == null) {
            return null;
        }

        boolean wasParsingString = isParsingStringExpression();
        boolean wasTagActive = isTagActive();

        whitespace();

        ParsedObject logic = (ParsedObject)
                expect(this::innerLogic, "some kind of logic, conditional or sequence within braces: { ... }", null);
        if (logic == null) {
            setParsingStringExpression(wasParsingString);
            return null;
        }

        disallowIncrement(logic);

        com.bladecoder.ink.compiler.ParsedHierarchy.ContentList contentList =
                logic instanceof com.bladecoder.ink.compiler.ParsedHierarchy.ContentList
                        ? (com.bladecoder.ink.compiler.ParsedHierarchy.ContentList) logic
                        : new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(logic);

        whitespace();

        expect(stringRule("}"), "closing brace '}' for inline logic", null);

        setParsingStringExpression(wasParsingString);

        if (!wasTagActive) {
            endTagIfNecessary(contentList.getContent());
        }

        return contentList;
    }

    protected ParsedObject innerLogic() {
        whitespace();

        Integer explicitSeqType = (Integer) parseObject(this::sequenceTypeAnnotation);
        if (explicitSeqType != null) {
            @SuppressWarnings("unchecked")
            List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> contentLists =
                    (List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList>)
                            expect(this::innerSequenceObjects, "sequence elements (for cycle/stoping etc)", null);
            if (contentLists == null) {
                return null;
            }
            return new com.bladecoder.ink.compiler.ParsedHierarchy.Sequence(contentLists, explicitSeqType);
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression initialQueryExpression =
                parse(this::conditionExpression);
        if (initialQueryExpression != null) {
            com.bladecoder.ink.compiler.ParsedHierarchy.Conditional conditional =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.Conditional) expect(
                            () -> innerConditionalContent(initialQueryExpression),
                            "conditional content following query",
                            null);
            return conditional;
        }

        ParseRule[] rules = {
            this::innerConditionalContent, this::innerSequence, this::innerExpression,
        };

        for (ParseRule rule : rules) {
            int ruleId = beginRule();

            ParsedObject result = (ParsedObject) parseObject(rule);
            if (result != null) {
                if (peek(spaced(stringRule("}"))) == null) {
                    failRule(ruleId);
                } else {
                    return (ParsedObject) succeedRule(ruleId, result);
                }
            } else {
                failRule(ruleId);
            }
        }

        return null;
    }

    protected ParsedObject innerExpression() {
        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr = parse(this::expression);
        if (expr != null) {
            expr.setOutputWhenComplete(true);
        }
        return expr;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Sequence innerSequence() {
        whitespace();

        int seqType = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Stopping;

        Integer parsedSeqType = (Integer) parseObject(this::sequenceTypeAnnotation);
        if (parsedSeqType != null) {
            seqType = parsedSeqType;
        }

        List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> contentLists = parse(this::innerSequenceObjects);
        if (contentLists == null || contentLists.size() <= 1) {
            return null;
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.Sequence(contentLists, seqType);
    }

    protected Object sequenceTypeAnnotation() {
        Integer annotation = (Integer) parseObject(this::sequenceTypeSymbolAnnotation);

        if (annotation == null) {
            annotation = (Integer) parseObject(this::sequenceTypeWordAnnotation);
        }

        if (annotation == null) {
            return null;
        }

        int value = annotation;
        int shuffleStopping = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Shuffle
                | com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Stopping;
        int shuffleOnce = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Shuffle
                | com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Once;

        if (value != com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Once
                && value != com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Cycle
                && value != com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Stopping
                && value != com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Shuffle
                && value != shuffleStopping
                && value != shuffleOnce) {
            error("Sequence type combination not supported: " + value, false);
            return com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Stopping;
        }

        return annotation;
    }

    protected Object sequenceTypeSymbolAnnotation() {
        if (sequenceTypeSymbols == null) {
            sequenceTypeSymbols = new CharacterSet("!&~$ ");
        }

        int sequenceType = 0;
        String sequenceAnnotations = parseCharactersFromCharSet(sequenceTypeSymbols);
        if (sequenceAnnotations == null) {
            return null;
        }

        for (char symbolChar : sequenceAnnotations.toCharArray()) {
            switch (symbolChar) {
                case '!':
                    sequenceType |= com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Once;
                    break;
                case '&':
                    sequenceType |= com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Cycle;
                    break;
                case '~':
                    sequenceType |= com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Shuffle;
                    break;
                case '$':
                    sequenceType |= com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Stopping;
                    break;
                default:
                    break;
            }
        }

        if (sequenceType == 0) {
            return null;
        }

        return sequenceType;
    }

    protected Object sequenceTypeWordAnnotation() {
        List<Integer> sequenceTypes = interleave(this::sequenceTypeSingleWord, exclude(this::whitespace), null, true);
        if (sequenceTypes == null || sequenceTypes.isEmpty()) {
            return null;
        }

        if (parseString(":") == null) {
            return null;
        }

        int combinedSequenceType = 0;
        for (Integer seqType : sequenceTypes) {
            combinedSequenceType |= seqType;
        }

        return combinedSequenceType;
    }

    protected Object sequenceTypeSingleWord() {
        Integer seqType = null;

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier word =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Identifier) parse(this::identifierWithMetadata);
        if (word != null) {
            switch (word.name) {
                case "once":
                    seqType = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Once;
                    break;
                case "cycle":
                    seqType = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Cycle;
                    break;
                case "shuffle":
                    seqType = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Shuffle;
                    break;
                case "stopping":
                    seqType = com.bladecoder.ink.compiler.ParsedHierarchy.SequenceType.Stopping;
                    break;
                default:
                    break;
            }
        }

        if (seqType == null) {
            return null;
        }

        return seqType;
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> innerSequenceObjects() {
        boolean multiline = parse(this::newline) != null;

        List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> result;
        if (multiline) {
            result = parse(this::innerMultilineSequenceObjects);
        } else {
            result = parse(this::innerInlineSequenceObjects);
        }

        return result;
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> innerInlineSequenceObjects() {
        List<Object> interleavedContentAndPipes =
                interleave(optional(this::mixedTextAndLogic), stringRule("|"), null, false);
        if (interleavedContentAndPipes == null) {
            return null;
        }

        List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> result = new ArrayList<>();

        boolean justHadContent = false;
        for (Object contentOrPipe : interleavedContentAndPipes) {
            if (contentOrPipe instanceof String && "|".equals(contentOrPipe)) {
                if (!justHadContent) {
                    result.add(new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList());
                }

                justHadContent = false;
            } else {
                @SuppressWarnings("unchecked")
                List<ParsedObject> content = (List<ParsedObject>) contentOrPipe;
                if (content == null) {
                    error("Expected content, but got " + contentOrPipe + " (this is an ink compiler bug!)", false);
                } else {
                    result.add(new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(content));
                }

                justHadContent = true;
            }
        }

        if (!justHadContent) {
            result.add(new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList());
        }

        return result;
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> innerMultilineSequenceObjects() {
        multilineWhitespace();

        List<Object> contentLists = oneOrMore(this::singleMultilineSequenceElement);
        if (contentLists == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList> result =
                (List<com.bladecoder.ink.compiler.ParsedHierarchy.ContentList>) (List<?>) contentLists;
        return result;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.ContentList singleMultilineSequenceElement() {
        whitespace();

        if (parseString("->") != null) {
            return null;
        }

        if (parseString("-") == null) {
            return null;
        }

        whitespace();

        List<ParsedObject> content = statementsAtLevel(StatementLevel.InnerBlock);

        if (content == null) {
            multilineWhitespace();
        } else {
            content.add(0, new com.bladecoder.ink.compiler.ParsedHierarchy.Text("\n"));
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList(content);
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Conditional innerConditionalContent() {
        com.bladecoder.ink.compiler.ParsedHierarchy.Expression initialQueryExpression =
                parse(this::conditionExpression);
        com.bladecoder.ink.compiler.ParsedHierarchy.Conditional conditional =
                parse(() -> innerConditionalContent(initialQueryExpression));
        if (conditional == null) {
            return null;
        }

        return conditional;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Conditional innerConditionalContent(
            com.bladecoder.ink.compiler.ParsedHierarchy.Expression initialQueryExpression) {
        List<com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch> alternatives;

        boolean canBeInline = initialQueryExpression != null;
        boolean isInline = parse(this::newline) == null;

        if (isInline && !canBeInline) {
            return null;
        }

        if (isInline) {
            alternatives = inlineConditionalBranches();
        } else {
            alternatives = multilineConditionalBranches();
            if (alternatives == null) {
                if (initialQueryExpression != null) {
                    List<ParsedObject> soleContent = statementsAtLevel(StatementLevel.InnerBlock);
                    if (soleContent != null) {
                        com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch soleBranch =
                                new com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch(soleContent);
                        alternatives = new ArrayList<>();
                        alternatives.add(soleBranch);

                        com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch elseBranch =
                                parse(this::singleMultilineCondition);
                        if (elseBranch != null) {
                            if (!elseBranch.isElse) {
                                errorWithParsedObject(
                                        "Expected an '- else:' clause here rather than an extra condition",
                                        elseBranch,
                                        false);
                                elseBranch.isElse = true;
                            }
                            alternatives.add(elseBranch);
                        }
                    }
                }

                if (alternatives == null) {
                    return null;
                }
            } else if (alternatives.size() == 1 && alternatives.get(0).isElse && initialQueryExpression != null) {
                com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch emptyTrueBranch =
                        new com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch(null);
                emptyTrueBranch.isTrueBranch = true;
                alternatives.add(0, emptyTrueBranch);
            }

            if (initialQueryExpression != null) {
                boolean earlierBranchesHaveOwnExpression = false;
                for (int i = 0; i < alternatives.size(); ++i) {
                    com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch branch = alternatives.get(i);
                    boolean isLast = (i == alternatives.size() - 1);

                    if (branch.getOwnExpression() != null) {
                        branch.matchingEquality = true;
                        earlierBranchesHaveOwnExpression = true;
                    } else if (earlierBranchesHaveOwnExpression && isLast) {
                        branch.matchingEquality = true;
                        branch.isElse = true;
                    } else {
                        if (!isLast && alternatives.size() > 2) {
                            errorWithParsedObject(
                                    "Only final branch can be an 'else'. Did you miss a ':'?", branch, false);
                        } else {
                            if (i == 0) {
                                branch.isTrueBranch = true;
                            } else {
                                branch.isElse = true;
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < alternatives.size(); ++i) {
                    com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch alt = alternatives.get(i);
                    boolean isLast = (i == alternatives.size() - 1);
                    if (alt.getOwnExpression() == null) {
                        if (isLast) {
                            alt.isElse = true;
                        } else {
                            if (alt.isElse) {
                                com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch finalClause =
                                        alternatives.get(alternatives.size() - 1);
                                if (finalClause.isElse) {
                                    errorWithParsedObject(
                                            "Multiple 'else' cases. Can have a maximum of one, at the end.",
                                            finalClause,
                                            false);
                                } else {
                                    errorWithParsedObject(
                                            "'else' case in conditional should always be the final one", alt, false);
                                }
                            } else {
                                errorWithParsedObject(
                                        "Branch doesn't have condition. Are you missing a ':'? ", alt, false);
                            }
                        }
                    }
                }

                if (alternatives.size() == 1 && alternatives.get(0).getOwnExpression() == null) {
                    errorWithParsedObject("Condition block with no conditions", alternatives.get(0), false);
                }
            }
        }

        if (alternatives == null) {
            return null;
        }

        for (com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch branch : alternatives) {
            branch.isInline = isInline;
        }

        return new com.bladecoder.ink.compiler.ParsedHierarchy.Conditional(initialQueryExpression, alternatives);
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch> inlineConditionalBranches() {
        List<List<ParsedObject>> listOfLists =
                interleave(this::mixedTextAndLogic, exclude(stringRule("|")), null, false);
        if (listOfLists == null || listOfLists.isEmpty()) {
            return null;
        }

        List<com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch> result = new ArrayList<>();

        if (listOfLists.size() > 2) {
            error("Expected one or two alternatives separated by '|' in inline conditional", false);
        } else {
            com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch trueBranch =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch(listOfLists.get(0));
            trueBranch.isTrueBranch = true;
            result.add(trueBranch);

            if (listOfLists.size() > 1) {
                com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch elseBranch =
                        new com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch(listOfLists.get(1));
                elseBranch.isElse = true;
                result.add(elseBranch);
            }
        }

        return result;
    }

    protected List<com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch> multilineConditionalBranches() {
        multilineWhitespace();

        List<Object> multipleConditions = oneOrMore(this::singleMultilineCondition);
        if (multipleConditions == null) {
            return null;
        }

        multilineWhitespace();

        @SuppressWarnings("unchecked")
        List<com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch> result =
                (List<com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch>)
                        (List<?>) multipleConditions;
        return result;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch singleMultilineCondition() {
        whitespace();

        if (parseString("->") != null) {
            return null;
        }

        if (parseString("-") == null) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr = null;
        boolean isElse = parse(this::elseExpression) != null;

        if (!isElse) {
            expr = parse(this::conditionExpression);
        }

        List<ParsedObject> content = statementsAtLevel(StatementLevel.InnerBlock);
        if (expr == null && content == null) {
            error("expected content for the conditional branch following '-'", false);

            content = new ArrayList<>();
            content.add(new com.bladecoder.ink.compiler.ParsedHierarchy.Text(""));
        }

        multilineWhitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch branch =
                new com.bladecoder.ink.compiler.ParsedHierarchy.ConditionalSingleBranch(content);
        branch.setOwnExpression(expr);
        branch.isElse = isElse;
        return branch;
    }

    protected com.bladecoder.ink.compiler.ParsedHierarchy.Expression conditionExpression() {
        com.bladecoder.ink.compiler.ParsedHierarchy.Expression expr = parse(this::expression);
        if (expr == null) {
            return null;
        }

        disallowIncrement(expr);

        whitespace();

        if (parseString(":") == null) {
            return null;
        }

        return expr;
    }

    protected Object elseExpression() {
        if (parseString("else") == null) {
            return null;
        }

        whitespace();

        if (parseString(":") == null) {
            return null;
        }

        return ParseSuccess;
    }

    private ParsedObject startTag() {
        whitespace();

        if (parseString("#") == null) {
            return null;
        }

        if (isParsingStringExpression()) {
            error("Tags aren't allowed inside of strings. Please use \\# if you want a hash symbol.", false);
        }

        ParsedObject result;

        if (isTagActive()) {
            com.bladecoder.ink.compiler.ParsedHierarchy.ContentList contentList =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.ContentList();
            com.bladecoder.ink.compiler.ParsedHierarchy.Tag endTag =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.Tag();
            endTag.isStart = false;
            contentList.addContent(endTag);
            com.bladecoder.ink.compiler.ParsedHierarchy.Tag startTag =
                    new com.bladecoder.ink.compiler.ParsedHierarchy.Tag();
            startTag.isStart = true;
            contentList.addContent(startTag);
            result = contentList;
        } else {
            com.bladecoder.ink.compiler.ParsedHierarchy.Tag tag = new com.bladecoder.ink.compiler.ParsedHierarchy.Tag();
            tag.isStart = true;
            result = tag;
        }

        setTagActive(true);

        whitespace();

        return result;
    }

    private ParsedObject glue() {
        String glueStr = parseString("<>");
        if (glueStr != null) {
            return new com.bladecoder.ink.compiler.ParsedHierarchy.Glue(new com.bladecoder.ink.runtime.Glue());
        }
        return null;
    }

    private String identifier() {
        String name = parseCharactersFromCharSet(getIdentifierCharSet());
        if (name == null) {
            return null;
        }

        boolean isNumberCharsOnly = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                isNumberCharsOnly = false;
                break;
            }
        }

        if (isNumberCharsOnly) {
            return null;
        }

        return name;
    }

    private Object identifierWithMetadata() {
        String id = identifier();
        if (id == null) {
            return null;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Identifier identifier =
                new com.bladecoder.ink.compiler.ParsedHierarchy.Identifier();
        identifier.name = id;
        return identifier;
    }

    private Object gatherDashes() {
        whitespace();

        int gatherDashCount = 0;

        while (parseDashNotArrow() != null) {
            gatherDashCount++;
            whitespace();
        }

        if (gatherDashCount == 0) {
            return null;
        }

        return gatherDashCount;
    }

    private Object parseDashNotArrow() {
        int ruleId = beginRule();

        if (parseString("->") == null && parseSingleCharacter() == '-') {
            return succeedRule(ruleId, ParseSuccess);
        }

        return failRule(ruleId);
    }

    private com.bladecoder.ink.compiler.ParsedHierarchy.Divert startThread() {
        whitespace();

        if (parseThreadArrow() == null) {
            return null;
        }

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Divert divert =
                (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) expect(
                        this::divertIdentifierWithArguments,
                        "target for new thread",
                        () -> new com.bladecoder.ink.compiler.ParsedHierarchy.Divert((ParsedObject) null));
        divert.isThread = true;
        return divert;
    }

    private com.bladecoder.ink.compiler.ParsedHierarchy.Divert divertIdentifierWithArguments() {
        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.Identifier> targetComponents =
                parse(this::dotSeparatedDivertPathComponents);
        if (targetComponents == null) {
            return null;
        }

        whitespace();

        List<com.bladecoder.ink.compiler.ParsedHierarchy.Expression> optionalArguments =
                parse(this::expressionFunctionCallArguments);

        whitespace();

        com.bladecoder.ink.compiler.ParsedHierarchy.Path targetPath =
                new com.bladecoder.ink.compiler.ParsedHierarchy.Path(targetComponents);
        return new com.bladecoder.ink.compiler.ParsedHierarchy.Divert(targetPath, optionalArguments);
    }

    private com.bladecoder.ink.compiler.ParsedHierarchy.Divert singleDivert() {
        List<ParsedObject> diverts = parse(this::multiDivert);
        if (diverts == null) {
            return null;
        }

        if (diverts.size() != 1) {
            return null;
        }

        ParsedObject singleDivert = diverts.get(0);
        if (singleDivert instanceof com.bladecoder.ink.compiler.ParsedHierarchy.TunnelOnwards) {
            return null;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Divert divert =
                singleDivert instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert
                        ? (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) singleDivert
                        : null;
        if (divert != null && divert.isTunnel) {
            return null;
        }

        return divert;
    }

    private List<com.bladecoder.ink.compiler.ParsedHierarchy.Identifier> dotSeparatedDivertPathComponents() {
        return interleave(spaced(this::identifierWithMetadata), exclude(stringRule(".")), null, true);
    }

    private Object parseDivertArrowOrTunnelOnwards() {
        int numArrows = 0;
        while (parseString("->") != null) {
            numArrows++;
        }

        if (numArrows == 0) {
            return null;
        } else if (numArrows == 1) {
            return "->";
        } else if (numArrows == 2) {
            return "->->";
        } else {
            error("Unexpected number of arrows in divert. Should only have '->' or '->->'", false);
            return "->->";
        }
    }

    public static final CharacterRange LatinBasic =
            CharacterRange.define('\u0041', '\u007A', new CharacterSet().addRange('\u005B', '\u0060'));
    public static final CharacterRange LatinExtendedA = CharacterRange.define('\u0100', '\u017F');
    public static final CharacterRange LatinExtendedB = CharacterRange.define('\u0180', '\u024F');
    public static final CharacterRange Greek = CharacterRange.define(
            '\u0370',
            '\u03FF',
            new CharacterSet()
                    .addRange('\u0378', '\u0385')
                    .addCharacters("\u0374\u0375\u0378\u0387\u038B\u038D\u03A2"));
    public static final CharacterRange Cyrillic =
            CharacterRange.define('\u0400', '\u04FF', new CharacterSet().addRange('\u0482', '\u0489'));
    public static final CharacterRange Armenian = CharacterRange.define(
            '\u0530',
            '\u058F',
            new CharacterSet()
                    .addCharacters("\u0530")
                    .addRange('\u0557', '\u0560')
                    .addRange('\u0588', '\u058E'));
    public static final CharacterRange Hebrew = CharacterRange.define('\u0590', '\u05FF');
    public static final CharacterRange Arabic = CharacterRange.define('\u0600', '\u06FF');
    public static final CharacterRange Korean = CharacterRange.define('\uAC00', '\uD7AF');
    public static final CharacterRange Latin1Supplement = CharacterRange.define('\u0080', '\u00FF');
    public static final CharacterRange CJKUnifiedIdeographs = CharacterRange.define('\u4E00', '\u9FFF');
    public static final CharacterRange Hiragana = CharacterRange.define('\u3041', '\u3096');
    public static final CharacterRange Katakana = CharacterRange.define('\u30A0', '\u30FC');

    public static CharacterRange[] listAllCharacterRanges() {
        return new CharacterRange[] {
            LatinBasic,
            LatinExtendedA,
            LatinExtendedB,
            Arabic,
            Armenian,
            Cyrillic,
            Greek,
            Hebrew,
            Korean,
            Latin1Supplement,
            CJKUnifiedIdeographs,
            Hiragana,
            Katakana,
        };
    }

    private void extendIdentifierCharacterRanges(CharacterSet identifierCharSet) {
        for (CharacterRange range : listAllCharacterRanges()) {
            identifierCharSet.addCharacters(range.toCharacterSet());
        }
    }

    private CharacterSet getIdentifierCharSet() {
        if (identifierCharSet == null) {
            identifierCharSet = new CharacterSet()
                    .addRange('A', 'Z')
                    .addRange('a', 'z')
                    .addRange('0', '9')
                    .addCharacters("_");
            extendIdentifierCharacterRanges(identifierCharSet);
        }
        return identifierCharSet;
    }

    public CommandLineInput CommandLineUserInput() {
        CommandLineInput result = new CommandLineInput();

        whitespace();

        if (parseString("help") != null) {
            result.isHelp = true;
            return result;
        }

        if (parseString("exit") != null || parseString("quit") != null) {
            result.isExit = true;
            return result;
        }

        return (CommandLineInput) oneOf(
                this::debugSource, this::debugPathLookup, this::userChoiceNumber, this::userImmediateModeStatement);
    }

    private CommandLineInput debugSource() {
        whitespace();

        if (parseString("DebugSource") == null) {
            return null;
        }

        whitespace();

        String expectMsg = "character offset in parentheses, e.g. DebugSource(5)";
        if (expect(stringRule("("), expectMsg, null) == null) {
            return null;
        }

        whitespace();

        Integer characterOffset = parseInt();
        if (characterOffset == null) {
            error(expectMsg, false);
            return null;
        }

        whitespace();

        expect(stringRule(")"), "closing parenthesis", null);

        CommandLineInput inputStruct = new CommandLineInput();
        inputStruct.debugSource = characterOffset;
        return inputStruct;
    }

    private CommandLineInput debugPathLookup() {
        whitespace();

        if (parseString("DebugPath") == null) {
            return null;
        }

        if (whitespace() == null) {
            return null;
        }

        String pathStr = (String) expect(this::runtimePath, "path", null);

        CommandLineInput inputStruct = new CommandLineInput();
        inputStruct.debugPathLookup = pathStr;
        return inputStruct;
    }

    private String runtimePath() {
        if (runtimePathCharacterSet == null) {
            runtimePathCharacterSet = new CharacterSet(getIdentifierCharSet());
            runtimePathCharacterSet.addCharacters("-");
            runtimePathCharacterSet.addCharacters(".");
        }

        return parseCharactersFromCharSet(runtimePathCharacterSet);
    }

    private CommandLineInput userChoiceNumber() {
        whitespace();

        Integer number = parseInt();
        if (number == null) {
            return null;
        }

        whitespace();

        if (parse(this::endOfLine) == null) {
            return null;
        }

        CommandLineInput inputStruct = new CommandLineInput();
        inputStruct.choiceInput = number;
        return inputStruct;
    }

    private CommandLineInput userImmediateModeStatement() {
        Object statement = oneOf(this::singleDivert, this::tempDeclarationOrAssignment, this::expression);

        CommandLineInput inputStruct = new CommandLineInput();
        inputStruct.userImmediateModeStatement = statement;
        return inputStruct;
    }

    private final CharacterSet inlineWhitespaceChars = new CharacterSet(" \t");
    private final java.util.Map<StatementLevel, List<ParseRule>> statementRulesAtLevel =
            new java.util.EnumMap<>(StatementLevel.class);
    private final java.util.Map<StatementLevel, List<ParseRule>> statementBreakRulesAtLevel =
            new java.util.EnumMap<>(StatementLevel.class);
    private CharacterSet nonTextPauseCharacters;
    private CharacterSet nonTextEndCharacters;
    private CharacterSet notTextEndCharactersChoice;
    private CharacterSet notTextEndCharactersString;
    private CharacterSet sequenceTypeSymbols;
    private CharacterSet identifierCharSet;
    private CharacterSet runtimePathCharacterSet;
    private boolean parsingChoice;
    private List<InfixOperator> binaryOperators = new ArrayList<>();
    private int maxBinaryOpLength;

    private IFileHandler fileHandler;
    private com.bladecoder.ink.runtime.Error.ErrorHandler externalErrorHandler;
    private String filename;

    private InkParser rootParserRef;
    private Set<String> openFilenames;
    private List<ParsedObject> basicParsedContent;
}
