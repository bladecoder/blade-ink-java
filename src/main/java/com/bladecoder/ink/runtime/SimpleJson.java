package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Simple custom JSON serialisation implementation that takes JSON-able
 * System.Collections that are produced by the ink engine and converts to and
 * from JSON text.
 */
class SimpleJson {
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
			if (!tryRead(expectedStr))
				expect(false, expectedStr);

		}

		boolean isNumberChar(char c) throws Exception {
			return c >= '0' && c <= '9' || c == '.' || c == '-' || c == '+';
		}

		List<Object> readArray() throws Exception {
			List<Object> list = new ArrayList<>();
			expect("[");
			skipWhitespace();
			// Empty list?
			if (tryRead("]"))
				return list;

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
			if (tryRead("}"))
				return dict;

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
				if (c == '.')
					isFloat = true;

				if (isNumberChar(c))
					continue;
				else
					break;
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

			throw new Exception("Failed to parse number value");
		}

		Object readObject() throws Exception {
			char currentChar = text.charAt(offset);

			if (currentChar == '{')
				return readHashMap();
			else if (currentChar == '[')
				return readArray();
			else if (currentChar == '"')
				return readString();
			else if (isNumberChar(currentChar))
				return readNumber();
			else if (tryRead("true"))
				return true;
			else if (tryRead("false"))
				return false;
			else if (tryRead("null"))
				return null;

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

						// c# expr: _text.Substring(_offset + 1, 4);
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
				if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
					offset++;
				else
					break;
			}
		}

		@SuppressWarnings("unchecked")
		public HashMap<String, Object> toHashMap() throws Exception {
			return (HashMap<String, Object>) rootObject;
		}

		boolean tryRead(String textToRead) throws Exception {
			if (offset + textToRead.length() > text.length())
				return false;

			for (int i = 0; i < textToRead.length(); i++) {
				if (textToRead.charAt(i) != text.charAt(offset + i))
					return false;

			}
			offset += textToRead.length();
			return true;
		}
	}

	static class Writer {
		StringBuilder sb;

		public Writer(Object rootObject) throws Exception {
			sb = new StringBuilder();
			writeObject(rootObject);
		}

		@Override
		public String toString() {
			try {
				return sb.toString();
			} catch (RuntimeException __dummyCatchVar0) {
				throw __dummyCatchVar0;
			} catch (Exception __dummyCatchVar0) {
				throw new RuntimeException(__dummyCatchVar0);
			}

		}

		void writeHashMap(HashMap<String, Object> dict) throws Exception {
			sb.append("{");
			boolean isFirst = true;
			for (Entry<String, Object> keyValue : dict.entrySet()) {
				if (!isFirst)
					sb.append(",");

				sb.append("\"");
				sb.append(keyValue.getKey());
				sb.append("\":");
				writeObject(keyValue.getValue());
				isFirst = false;
			}
			sb.append("}");
		}

		void writeList(List<Object> list) throws Exception {
			sb.append("[");
			boolean isFirst = true;
			for (Object obj : list) {
				if (!isFirst)
					sb.append(",");

				writeObject(obj);
				isFirst = false;
			}
			sb.append("]");
		}

		@SuppressWarnings("unchecked")
		void writeObject(Object obj) throws Exception {
			if (obj instanceof Integer) {
				sb.append(obj);
			} else if (obj instanceof Float) {
				String floatStr = obj.toString();
				sb.append(floatStr);
				if (!floatStr.contains("."))
					sb.append(".0");

			} else if (obj instanceof Boolean) {
				sb.append((Boolean) obj == true ? "true" : "false");
			} else if (obj == null) {
				sb.append("null");
			} else if (obj instanceof String) {
				String str = (String) obj;
				sb.ensureCapacity(sb.length() + str.length() + 2);
				sb.append('"');

				for (char c : str.toCharArray()) {
					if (c < ' ') {
						// Don't write any control characters except \n and \t
						switch (c) {
						case '\n':
							sb.append("\\n");
							break;
						case '\t':
							sb.append("\\t");
							break;
						}
					} else {
						switch (c) {
						case '\\':
						case '"':
							sb.append('\\').append(c);
							break;
						default:
							sb.append(c);
							break;
						}
					}
				}

				sb.append('"');
			} else if (obj instanceof HashMap<?, ?>) {
				writeHashMap((HashMap<String, Object>) obj);
			} else if (obj instanceof List<?>) {
				writeList((List<Object>) obj);
			} else {
				throw new Exception("ink's SimpleJson writer doesn't currently support this RTObject: " + obj);
			}
		}
	}

	public static String HashMapToText(HashMap<String, Object> rootObject) throws Exception {
		return new Writer(rootObject).toString();
	}

	public static HashMap<String, Object> textToHashMap(String text) throws Exception {
		return new Reader(text).toHashMap();
	}

}
