package com.bladecoder.ink.compiler;

import com.bladecoder.ink.compiler.StringParser.StringParser;
import java.util.List;

public class CommentEliminator extends StringParser {
    public CommentEliminator(String input) {
        super(input);
    }

    public String process() {
        List<String> stringList = interleave(optional(this::commentsAndNewlines), optional(this::mainInk));

        if (stringList != null) {
            return String.join("", stringList);
        }

        return null;
    }

    private String mainInk() {
        return parseUntil(this::commentsAndNewlines, commentOrNewlineStartCharacter, null);
    }

    private String commentsAndNewlines() {
        List<String> newlines = interleave(optional(this::parseNewline), optional(this::parseSingleComment));

        if (newlines != null) {
            return String.join("", newlines);
        }

        return null;
    }

    private Object parseSingleComment() {
        return oneOf(this::endOfLineComment, this::blockComment);
    }

    private String endOfLineComment() {
        if (parseString("//") == null) {
            return null;
        }

        parseUntilCharactersFromCharSet(newlineCharacters);

        return "";
    }

    private String blockComment() {
        if (parseString("/*") == null) {
            return null;
        }

        int startLineIndex = getLineIndex();

        String commentResult = parseUntil(stringRule("*/"), commentBlockEndCharacter, null);

        if (!isEndOfInput()) {
            parseString("*/");
        }

        if (commentResult != null) {
            return repeatChar('\n', getLineIndex() - startLineIndex);
        }

        return null;
    }

    private String repeatChar(char ch, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private final CharacterSet commentOrNewlineStartCharacter = new CharacterSet("/\r\n");
    private final CharacterSet commentBlockEndCharacter = new CharacterSet("*");
    private final CharacterSet newlineCharacters = new CharacterSet("\n\r");
}
