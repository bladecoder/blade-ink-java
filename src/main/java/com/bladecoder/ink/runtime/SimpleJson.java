package com.bladecoder.ink.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Simple custom JSON serialisation implementation that takes JSON-able
 * System.Collections that are produced by the ink engine and converts to and
 * from JSON text.
 */
class SimpleJson {

    public static HashMap<String, Object> textToDictionary(String text) throws Exception {
        return new Reader(text).toHashMap();
    }

    public static List<Object> textToArray(String text) throws Exception {
        return new Reader(text).toArray();
    }

    static class Reader {
        private int offset;

        private Object rootObject;

        private String text;

        public Reader(String text) throws Exception {
            this.text = text;
            offset = 0;
            skipWhitespace();
            rootObject = readObject();
        }

        void expect(boolean condition, String message) throws Exception {
            if (!condition) {
                if (message == null) {
                    message = "Unexpected token";
                } else {
                    message = "Expected " + message;
                }
                message += " at offset " + offset;
                throw new Exception(message);
            }
        }

        void expect(String expectedStr) throws Exception {
            if (!tryRead(expectedStr)) expect(false, expectedStr);
        }

        boolean isNumberChar(char c) throws Exception {
            return c >= '0' && c <= '9' || c == '.' || c == '-' || c == '+' || c == 'E' || c == 'e';
        }

        boolean IsFirstNumberChar(char c) {
            return c >= '0' && c <= '9' || c == '-' || c == '+';
        }

        List<Object> readArray() throws Exception {
            List<Object> list = new ArrayList<>();
            expect("[");
            skipWhitespace();
            // Empty list?
            if (tryRead("]")) return list;

            do {
                skipWhitespace();
                // Value
                Object val = readObject();
                // Add to array
                list.add(val);
                skipWhitespace();
            } while (tryRead(","));
            expect("]");
            return list;
        }

        HashMap<String, Object> readHashMap() throws Exception {
            HashMap<String, Object> dict = new HashMap<>();
            expect("{");
            skipWhitespace();
            // Empty HashMap?
            if (tryRead("}")) return dict;

            do {
                skipWhitespace();
                // Key
                String key = readString();
                expect(key != null, "dictionary key");
                skipWhitespace();
                // :
                expect(":");
                skipWhitespace();
                // Value
                Object val = readObject();
                expect(val != null, "dictionary value");
                // Add to HashMap
                dict.put(key, val);
                skipWhitespace();
            } while (tryRead(","));
            expect("}");
            return dict;
        }

        Object readNumber() throws Exception {
            int startOffset = offset;
            boolean isFloat = false;
            for (; offset < text.length(); offset++) {
                char c = text.charAt(offset);
                if (c == '.' || c == 'e' || c == 'E') isFloat = true;

                if (isNumberChar(c)) continue;
                else break;
            }

            String numStr = text.substring(startOffset, offset);
            if (isFloat) {
                try {
                    float f = Float.parseFloat(numStr);
                    return f;
                } catch (NumberFormatException e) {

                }
            } else {
                try {
                    int i = Integer.parseInt(numStr);
                    return i;
                } catch (NumberFormatException e) {

                }
            }

            throw new Exception("Failed to parse number value: " + numStr);
        }

        Object readObject() throws Exception {
            char currentChar = text.charAt(offset);

            if (currentChar == '{') return readHashMap();
            else if (currentChar == '[') return readArray();
            else if (currentChar == '"') return readString();
            else if (IsFirstNumberChar(currentChar)) return readNumber();
            else if (tryRead("true")) return true;
            else if (tryRead("false")) return false;
            else if (tryRead("null")) return null;

            throw new Exception("Unhandled RTObject type in JSON: " + text.substring(offset, offset + 30));
        }

        String readString() throws Exception {
            expect("\"");
            StringBuilder sb = new StringBuilder();

            for (; offset < text.length(); offset++) {
                char c = text.charAt(offset);

                if (c == '\\') {
                    // Escaped character
                    offset++;
                    if (offset >= text.length()) {
                        throw new Exception("Unexpected EOF while reading string");
                    }
                    c = text.charAt(offset);
                    switch (c) {
                        case '"':
                        case '\\':
                        case '/': // Yes, JSON allows this to be escaped
                            sb.append(c);
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'r':
                        case 'b':
                        case 'f':
                            // Ignore other control characters
                            break;
                        case 'u':
                            // 4-digit Unicode
                            if (offset + 4 >= text.length()) {
                                throw new Exception("Unexpected EOF while reading string");
                            }

                            // c# expr: _text.SubString(_offset + 1, 4);
                            String digits = text.substring(offset + 1, offset + 6);

                            int uchar;

                            try {
                                uchar = Integer.parseInt(digits, 16);
                                sb.append((char) uchar);
                                offset += 4;
                            } catch (NumberFormatException e) {
                                throw new Exception("Invalid Unicode escape character at offset " + (offset - 1));
                            }
                            break;

                        default:
                            // The escaped character is invalid per json spec
                            throw new Exception("Invalid Unicode escape character at offset " + (offset - 1));
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            expect("\"");

            return sb.toString();
        }

        void skipWhitespace() throws Exception {
            while (offset < text.length()) {
                char c = text.charAt(offset);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') offset++;
                else break;
            }
        }

        @SuppressWarnings("unchecked")
        public HashMap<String, Object> toHashMap() throws Exception {
            return (HashMap<String, Object>) rootObject;
        }

        @SuppressWarnings("unchecked")
        public List<Object> toArray() {
            return (List<Object>) rootObject;
        }

        boolean tryRead(String textToRead) throws Exception {
            if (offset + textToRead.length() > text.length()) return false;

            for (int i = 0; i < textToRead.length(); i++) {
                if (textToRead.charAt(i) != text.charAt(offset + i)) return false;
            }
            offset += textToRead.length();
            return true;
        }
    }

    public static class Writer {
        Stack<StateElement> stateStack = new Stack<>();
        java.io.Writer writer;

        public Writer() {
            writer = new StringWriter();
        }

        public Writer(OutputStream stream) throws UnsupportedEncodingException {
            writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
        }

        public void writeObject(InnerWriter inner) throws Exception {
            writeObjectStart();
            inner.write(this);
            writeObjectEnd();
        }

        public void clear() {
            StringWriter stringWriter = writer instanceof StringWriter ? (StringWriter) writer : null;
            if (stringWriter == null) {
                throw new UnsupportedOperationException(
                        "Writer.Clear() is only supported for the StringWriter variant, not for streams");
            }

            stringWriter.getBuffer().setLength(0);
        }

        public void writeObjectStart() throws Exception {
            startNewObject(true);
            stateStack.push(new StateElement(State.Object, 0));
            writer.write("{");
        }

        public void writeObjectEnd() throws Exception {
            Assert(getState() == State.Object);
            writer.write("}");
            stateStack.pop();
            if (getState() == State.None) writer.flush();
        }

        public void writeProperty(String name, InnerWriter inner) throws Exception {
            writePropertyString(name, inner);
        }

        public void writeProperty(int id, InnerWriter inner) throws Exception {
            writePropertyInteger(id, inner);
        }

        public void writeProperty(String name, String content) throws Exception {
            writePropertyStart(name);
            write(content);
            writePropertyEnd();
        }

        public void writeProperty(String name, int content) throws Exception {
            writePropertyStart(name);
            write(content);
            writePropertyEnd();
        }

        public void writeProperty(String name, boolean content) throws Exception {
            writePropertyStart(name);
            write(content);
            writePropertyEnd();
        }

        public void writePropertyStart(String name) throws Exception {
            Assert(getState() == State.Object);

            if (getChildCount() > 0) writer.write(",");

            writer.write("\"");
            writer.write(name);
            writer.write("\":");

            incrementChildCount();

            stateStack.push(new StateElement(State.Property, 0));
        }

        public void writePropertyStart(int id) throws Exception {
            writePropertyStart(Integer.toString(id));
        }

        public void writePropertyEnd() throws Exception {
            Assert(getState() == State.Property);
            Assert(getChildCount() == 1);
            stateStack.pop();
        }

        public void writePropertyNameStart() throws Exception {
            Assert(getState() == State.Object);

            if (getChildCount() > 0) writer.write(",");

            writer.write("\"");

            incrementChildCount();

            stateStack.push(new StateElement(State.Property, 0));
            stateStack.push(new StateElement(State.PropertyName, 0));
        }

        public void writePropertyNameEnd() throws Exception {
            Assert(getState() == State.PropertyName);

            writer.write("\":");

            // Pop PropertyName, leaving Property state
            stateStack.pop();
        }

        public void writePropertyNameInner(String str) throws Exception {
            Assert(getState() == State.PropertyName);
            writer.write(str);
        }

        // allow name to be String or int
        void writePropertyString(String name, InnerWriter inner) throws Exception {
            writePropertyStart(name);

            inner.write(this);

            writePropertyEnd();
        }

        void writePropertyInteger(Integer name, InnerWriter inner) throws Exception {
            writePropertyStart(name);

            inner.write(this);

            writePropertyEnd();
        }

        public void writeArrayStart() throws Exception {
            startNewObject(true);
            stateStack.push(new StateElement(State.Array, 0));
            writer.write("[");
        }

        public void writeArrayEnd() throws Exception {
            Assert(getState() == State.Array);
            writer.write("]");
            stateStack.pop();
        }

        public void write(int i) throws Exception {
            startNewObject(false);
            writer.write(Integer.toString(i));
        }

        public void write(float f) throws Exception {
            startNewObject(false);

            // TODO: Find an heap-allocation-free way to do this please!
            // writer.write(formatStr, obj (the float)) requires boxing
            // Following implementation seems to work ok but requires creating temporary
            // garbage String.
            String floatStr = Float.toString(f);

            if (floatStr == "Infinity") {
                writer.write("3.4E+38"); // JSON doesn't support, do our best alternative
            } else if (floatStr == "-Infinity") {
                writer.write("-3.4E+38"); // JSON doesn't support, do our best alternative
            } else if (floatStr == "NaN") {
                writer.write("0.0"); // JSON doesn't support, not much we can do
            } else {
                writer.write(floatStr);
                if (!floatStr.contains(".") && !floatStr.contains("E"))
                    writer.write(".0"); // ensure it gets read back in as a floating point value
            }
        }

        public void write(String str) throws Exception {
            write(str, true);
        }

        public void write(String str, boolean escape) throws Exception {
            startNewObject(false);

            writer.write("\"");
            if (escape) writeEscapedString(str);
            else writer.write(str);
            writer.write("\"");
        }

        public void write(boolean b) throws Exception {
            startNewObject(false);
            writer.write(b ? "true" : "false");
        }

        public void writeNull() throws Exception {
            startNewObject(false);
            writer.write("null");
        }

        public void writeStringStart() throws Exception {
            startNewObject(false);
            stateStack.push(new StateElement(State.String, 0));
            writer.write("\"");
        }

        public void writeStringEnd() throws Exception {
            Assert(getState() == State.String);
            writer.write("\"");
            stateStack.pop();
        }

        public void writeStringInner(String str) throws Exception {
            writeStringInner(str, true);
        }

        public void writeStringInner(String str, boolean escape) throws Exception {
            Assert(getState() == State.String);
            if (escape) writeEscapedString(str);
            else writer.write(str);
        }

        void writeEscapedString(String str) throws IOException {
            for (char c : str.toCharArray()) {
                if (c < ' ') {
                    // Don't write any control characters except \n and \t
                    switch (c) {
                        case '\n':
                            writer.write("\\n");
                            break;
                        case '\t':
                            writer.write("\\t");
                            break;
                    }
                } else {
                    switch (c) {
                        case '\\':
                        case '"':
                            writer.write("\\");
                            writer.write(c);
                            break;
                        default:
                            writer.write(c);
                            break;
                    }
                }
            }
        }

        void startNewObject(boolean container) throws Exception {

            if (container)
                Assert(getState() == State.None || getState() == State.Property || getState() == State.Array);
            else Assert(getState() == State.Property || getState() == State.Array);

            if (getState() == State.Array && getChildCount() > 0) writer.write(",");

            if (getState() == State.Property) Assert(getChildCount() == 0);

            if (getState() == State.Array || getState() == State.Property) incrementChildCount();
        }

        State getState() {
            if (stateStack.size() > 0) return stateStack.peek().type;
            else return State.None;
        }

        int getChildCount() {

            if (stateStack.size() > 0) return stateStack.peek().childCount;
            else return 0;
        }

        void incrementChildCount() throws Exception {
            Assert(stateStack.size() > 0);
            StateElement currEl = stateStack.pop();
            currEl.childCount++;
            stateStack.push(currEl);
        }

        // Shouldn't hit this Assert outside of initial JSON development,
        // so it's save to make it debug-only.
        void Assert(boolean condition) throws Exception {
            if (!condition) throw new Exception("Assert failed while writing JSON");
        }

        @Override
        public String toString() {
            return writer.toString();
        }

        enum State {
            None,
            Object,
            Array,
            Property,
            PropertyName,
            String
        };

        // Struct in C#
        class StateElement {
            public State type;
            public int childCount;

            public StateElement(State type, int childCount) {
                this.type = type;
                this.childCount = childCount;
            }
        }
    }

    interface InnerWriter {
        void write(Writer w) throws Exception;
    }
}
