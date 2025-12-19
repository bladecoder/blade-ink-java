package com.bladecoder.ink.compiler.StringParser;

import com.bladecoder.ink.compiler.CharacterSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringParser {
    @FunctionalInterface
    public interface ParseRule {
        Object parse();
    }

    @FunctionalInterface
    public interface SpecificParseRule<T> {
        T parse();
    }

    @FunctionalInterface
    public interface ErrorHandler {
        void onError(String message, int index, int lineIndex, boolean isWarning);
    }

    public StringParser(String str) {
        str = preProcessInputString(str);

        state = new StringParserState();

        if (str != null) {
            _chars = str.toCharArray();
        } else {
            _chars = new char[0];
        }

        inputString = str;
    }

    public static class ParseSuccessStruct {}

    public static final ParseSuccessStruct ParseSuccess = new ParseSuccessStruct();

    public static final CharacterSet numbersCharacterSet = new CharacterSet("0123456789");

    protected ErrorHandler errorHandler;

    public char getCurrentCharacter() {
        if (getIndex() >= 0 && remainingLength() > 0) {
            return _chars[getIndex()];
        }
        return 0;
    }

    public StringParserState state;

    public boolean hadError;

    protected String preProcessInputString(String str) {
        return str;
    }

    protected int beginRule() {
        return state.push();
    }

    protected Object failRule(int expectedRuleId) {
        state.pop(expectedRuleId);
        return null;
    }

    protected void cancelRule(int expectedRuleId) {
        state.pop(expectedRuleId);
    }

    protected Object succeedRule(int expectedRuleId, Object result) {
        StringParserState.Element stateAtSucceedRule = state.peek(expectedRuleId);
        StringParserState.Element stateAtBeginRule = state.peekPenultimate();

        ruleDidSucceed(result, stateAtBeginRule, stateAtSucceedRule);

        state.squash();

        if (result == null) {
            result = ParseSuccess;
        }

        return result;
    }

    protected void ruleDidSucceed(
            Object result, StringParserState.Element startState, StringParserState.Element endState) {}

    protected Object expect(ParseRule rule, String message, ParseRule recoveryRule) {
        Object result = parseObject(rule);
        if (result == null) {
            if (message == null) {
                message = rule.getClass().getName();
            }

            String butSaw;
            String lineRemainder = lineRemainder();
            if (lineRemainder == null || lineRemainder.isEmpty()) {
                butSaw = "end of line";
            } else {
                butSaw = "'" + lineRemainder + "'";
            }

            error("Expected " + message + " but saw " + butSaw, false);

            if (recoveryRule != null) {
                result = recoveryRule.parse();
            }
        }
        return result;
    }

    protected Object expect(ParseRule rule, String message) {
        return expect(rule, message, null);
    }

    protected Object expect(ParseRule rule) {
        return expect(rule, null, null);
    }

    protected void error(String message, boolean isWarning) {
        errorOnLine(message, getLineIndex() + 1, isWarning);
    }

    protected void errorWithParsedObject(
            String message, com.bladecoder.ink.compiler.ParsedHierarchy.ParsedObject result, boolean isWarning) {
        errorOnLine(message, result.getDebugMetadata().startLineNumber, isWarning);
    }

    protected void errorOnLine(String message, int lineNumber, boolean isWarning) {
        if (!state.isErrorReportedAlreadyInScope()) {
            String errorType = isWarning ? "Warning" : "Error";

            if (errorHandler == null) {
                throw new RuntimeException(errorType + " on line " + lineNumber + ": " + message);
            } else {
                errorHandler.onError(message, getIndex(), lineNumber - 1, isWarning);
            }

            state.noteErrorReported();
        }

        if (!isWarning) {
            hadError = true;
        }
    }

    protected void warning(String message) {
        error(message, true);
    }

    public boolean isEndOfInput() {
        return getIndex() >= _chars.length;
    }

    public String remainingString() {
        return new String(_chars, getIndex(), remainingLength());
    }

    public String lineRemainder() {
        return (String) peek(() -> parseUntilCharactersFromString("\n\r"));
    }

    public int remainingLength() {
        return _chars.length - getIndex();
    }

    public String inputString;

    public int getLineIndex() {
        return state.getLineIndex();
    }

    public void setLineIndex(int value) {
        state.setLineIndex(value);
    }

    public int getCharacterInLineIndex() {
        return state.getCharacterInLineIndex();
    }

    public void setCharacterInLineIndex(int value) {
        state.setCharacterInLineIndex(value);
    }

    public int getIndex() {
        return state.getCharacterIndex();
    }

    public void setIndex(int value) {
        state.setCharacterIndex(value);
    }

    public void setFlag(long flag, boolean trueOrFalse) {
        if (trueOrFalse) {
            state.setCustomFlags(state.getCustomFlags() | flag);
        } else {
            state.setCustomFlags(state.getCustomFlags() & ~flag);
        }
    }

    public boolean getFlag(long flag) {
        return (state.getCustomFlags() & flag) != 0;
    }

    public Object parseObject(ParseRule rule) {
        int ruleId = beginRule();

        int stackHeightBefore = state.getStackHeight();

        Object result = rule.parse();

        if (stackHeightBefore != state.getStackHeight()) {
            throw new RuntimeException("Mismatched Begin/Fail/Succeed rules");
        }

        if (result == null) {
            return failRule(ruleId);
        }

        return succeedRule(ruleId, result);
    }

    public <T> T parse(SpecificParseRule<T> rule) {
        int ruleId = beginRule();

        T result = rule.parse();
        if (result == null) {
            failRule(ruleId);
            return null;
        }

        succeedRule(ruleId, result);
        return result;
    }

    public Object oneOf(ParseRule... rules) {
        for (ParseRule rule : rules) {
            Object result = parseObject(rule);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public List<Object> oneOrMore(ParseRule rule) {
        List<Object> results = new ArrayList<>();

        Object result;
        do {
            result = parseObject(rule);
            if (result != null) {
                results.add(result);
            }
        } while (result != null);

        if (!results.isEmpty()) {
            return results;
        }

        return null;
    }

    public ParseRule optional(ParseRule rule) {
        return () -> {
            Object result = parseObject(rule);
            if (result == null) {
                result = ParseSuccess;
            }
            return result;
        };
    }

    public ParseRule exclude(ParseRule rule) {
        return () -> {
            Object result = parseObject(rule);
            if (result == null) {
                return null;
            }
            return ParseSuccess;
        };
    }

    public ParseRule optionalExclude(ParseRule rule) {
        return () -> {
            parseObject(rule);
            return ParseSuccess;
        };
    }

    protected ParseRule stringRule(String str) {
        return () -> parseString(str);
    }

    private <T> void tryAddResultToList(Object result, List<T> list, boolean flatten) {
        if (result == ParseSuccess) {
            return;
        }

        if (flatten && result instanceof Collection) {
            for (Object obj : (Collection<?>) result) {
                list.add((T) obj);
            }
            return;
        }

        list.add((T) result);
    }

    public <T> List<T> interleave(ParseRule ruleA, ParseRule ruleB, ParseRule untilTerminator, boolean flatten) {
        int ruleId = beginRule();

        List<T> results = new ArrayList<>();

        Object firstA = parseObject(ruleA);
        if (firstA == null) {
            return (List<T>) failRule(ruleId);
        }
        tryAddResultToList(firstA, results, flatten);

        Object lastMainResult;
        Object outerResult;
        do {
            if (untilTerminator != null && peek(untilTerminator) != null) {
                break;
            }

            lastMainResult = parseObject(ruleB);
            if (lastMainResult == null) {
                break;
            }
            tryAddResultToList(lastMainResult, results, flatten);

            outerResult = null;
            if (lastMainResult != null) {
                outerResult = parseObject(ruleA);
                if (outerResult == null) {
                    break;
                }
                tryAddResultToList(outerResult, results, flatten);
            }

        } while ((lastMainResult != null || outerResult != null)
                && !(lastMainResult == ParseSuccess && outerResult == ParseSuccess)
                && remainingLength() > 0);

        if (results.isEmpty()) {
            return (List<T>) failRule(ruleId);
        }

        return (List<T>) succeedRule(ruleId, results);
    }

    public <T> List<T> interleave(ParseRule ruleA, ParseRule ruleB) {
        return interleave(ruleA, ruleB, null, true);
    }

    public String parseString(String str) {
        if (str.length() > remainingLength()) {
            return null;
        }

        int ruleId = beginRule();

        int i = getIndex();
        int cli = getCharacterInLineIndex();
        int li = getLineIndex();

        boolean success = true;
        for (char c : str.toCharArray()) {
            if (_chars[i] != c) {
                success = false;
                break;
            }
            if (c == '\n') {
                li++;
                cli = -1;
            }
            i++;
            cli++;
        }

        setIndex(i);
        setCharacterInLineIndex(cli);
        setLineIndex(li);

        if (success) {
            return (String) succeedRule(ruleId, str);
        }

        return (String) failRule(ruleId);
    }

    public char parseSingleCharacter() {
        if (remainingLength() > 0) {
            char c = _chars[getIndex()];
            if (c == '\n') {
                setLineIndex(getLineIndex() + 1);
                setCharacterInLineIndex(-1);
            }
            setIndex(getIndex() + 1);
            setCharacterInLineIndex(getCharacterInLineIndex() + 1);
            return c;
        }

        return 0;
    }

    public String parseUntilCharactersFromString(String str, int maxCount) {
        return parseCharactersFromString(str, false, maxCount);
    }

    public String parseUntilCharactersFromString(String str) {
        return parseUntilCharactersFromString(str, -1);
    }

    public String parseUntilCharactersFromCharSet(CharacterSet charSet, int maxCount) {
        return parseCharactersFromCharSet(charSet, false, maxCount);
    }

    public String parseUntilCharactersFromCharSet(CharacterSet charSet) {
        return parseUntilCharactersFromCharSet(charSet, -1);
    }

    public String parseCharactersFromString(String str, int maxCount) {
        return parseCharactersFromString(str, true, maxCount);
    }

    public String parseCharactersFromString(String str) {
        return parseCharactersFromString(str, true, -1);
    }

    public String parseCharactersFromString(String str, boolean shouldIncludeStrChars, int maxCount) {
        return parseCharactersFromCharSet(new CharacterSet(str), shouldIncludeStrChars, maxCount);
    }

    public String parseCharactersFromCharSet(CharacterSet charSet, boolean shouldIncludeChars, int maxCount) {
        if (maxCount == -1) {
            maxCount = Integer.MAX_VALUE;
        }

        int startIndex = getIndex();

        int i = getIndex();
        int cli = getCharacterInLineIndex();
        int li = getLineIndex();

        int count = 0;
        while (i < _chars.length && charSet.contains(_chars[i]) == shouldIncludeChars && count < maxCount) {
            if (_chars[i] == '\n') {
                li++;
                cli = -1;
            }
            i++;
            cli++;
            count++;
        }

        setIndex(i);
        setCharacterInLineIndex(cli);
        setLineIndex(li);

        int lastCharIndex = getIndex();
        if (lastCharIndex > startIndex) {
            return new String(_chars, startIndex, getIndex() - startIndex);
        }

        return null;
    }

    public String parseCharactersFromCharSet(CharacterSet charSet) {
        return parseCharactersFromCharSet(charSet, true, -1);
    }

    public Object peek(ParseRule rule) {
        int ruleId = beginRule();
        Object result = rule.parse();
        cancelRule(ruleId);
        return result;
    }

    public String parseUntil(ParseRule stopRule, CharacterSet pauseCharacters, CharacterSet endCharacters) {
        int ruleId = beginRule();

        CharacterSet pauseAndEnd = new CharacterSet();
        if (pauseCharacters != null) {
            pauseAndEnd.addCharacters(pauseCharacters);
        }
        if (endCharacters != null) {
            pauseAndEnd.addCharacters(endCharacters);
        }

        StringBuilder parsedString = new StringBuilder();
        Object ruleResultAtPause;

        do {
            String partialParsedString = parseUntilCharactersFromCharSet(pauseAndEnd);
            if (partialParsedString != null) {
                parsedString.append(partialParsedString);
            }

            ruleResultAtPause = peek(stopRule);

            if (ruleResultAtPause != null) {
                break;
            }

            if (isEndOfInput()) {
                break;
            }

            char pauseCharacter = getCurrentCharacter();
            if (pauseCharacters != null && pauseCharacters.contains(pauseCharacter)) {
                parsedString.append(pauseCharacter);
                if (pauseCharacter == '\n') {
                    setLineIndex(getLineIndex() + 1);
                    setCharacterInLineIndex(-1);
                }
                setIndex(getIndex() + 1);
                setCharacterInLineIndex(getCharacterInLineIndex() + 1);
            } else {
                break;
            }

        } while (true);

        if (parsedString.length() > 0) {
            return (String) succeedRule(ruleId, parsedString.toString());
        }

        return (String) failRule(ruleId);
    }

    public Integer parseInt() {
        int oldIndex = getIndex();
        int oldCharacterInLineIndex = getCharacterInLineIndex();

        boolean negative = parseString("-") != null;

        parseCharactersFromString(" \t");

        String parsedString = parseCharactersFromCharSet(numbersCharacterSet);
        if (parsedString == null) {
            setIndex(oldIndex);
            setCharacterInLineIndex(oldCharacterInLineIndex);
            return null;
        }

        try {
            int parsedInt = Integer.parseInt(parsedString);
            return negative ? -parsedInt : parsedInt;
        } catch (NumberFormatException e) {
            error(
                    "Failed to read integer value: " + parsedString
                            + ". Perhaps it's out of the range of acceptable numbers ink supports? ("
                            + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE + ")",
                    false);
            return null;
        }
    }

    public Float parseFloat() {
        int oldIndex = getIndex();
        int oldCharacterInLineIndex = getCharacterInLineIndex();

        Integer leadingInt = parseInt();
        if (leadingInt != null) {
            if (parseString(".") != null) {
                String afterDecimalPointStr = parseCharactersFromCharSet(numbersCharacterSet);
                return Float.parseFloat(leadingInt + "." + afterDecimalPointStr);
            }
        }

        setIndex(oldIndex);
        setCharacterInLineIndex(oldCharacterInLineIndex);
        return null;
    }

    protected String parseNewline() {
        int ruleId = beginRule();

        parseString("\r");

        if (parseString("\n") == null) {
            return (String) failRule(ruleId);
        }

        return (String) succeedRule(ruleId, "\n");
    }

    private char[] _chars;
}
