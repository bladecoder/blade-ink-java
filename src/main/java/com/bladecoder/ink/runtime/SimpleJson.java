package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class SimpleJson {
	public static String HashMapToText(HashMap<String, Object> rootObject) throws Exception {
		return new Writer(rootObject).toString();
	}

	public static HashMap<String, Object> textToHashMap(String text) throws Exception {
		return new Reader(text).toHashMap();
	}

	static class Reader {
		public Reader(String text) throws Exception {
			_text = text;
			_offset = 0;
			skipWhitespace();
			_rootObject = readObject();
		}

		public HashMap<String, Object> toHashMap() throws Exception {
			return (HashMap<String, Object>) _rootObject;
		}

		boolean isNumberChar(char c) throws Exception {
			return c >= '0' && c <= '9' || c == '.' || c == '-' || c == '+';
		}

		Object readObject() throws Exception {
			char currentChar = _text.charAt(_offset);

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

			throw new Exception("Unhandled RTObject type in JSON: " + _text.substring(_offset, _offset + 30));
		}

		HashMap<String, Object> readHashMap() throws Exception {
			HashMap<String, Object> dict = new HashMap<String, Object>();
			expect("{");
			skipWhitespace();
			// Empty HashMap?
			if (tryRead("}"))
				return dict;

			do {
				skipWhitespace();
				// Key
				String key = readString();
				expect(key != null, "HashMap key");
				skipWhitespace();
				// :
				expect(":");
				skipWhitespace();
				// Value
				String val = readObject().toString();
				expect(val != null, "HashMap value");
				// Add to HashMap
				dict.put(key, val);
				skipWhitespace();
			} while (tryRead(","));
			expect("}");
			return dict;
		}

		List<Object> readArray() throws Exception {
			List<Object> list = new ArrayList<Object>();
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

		String readString() throws Exception {
			expect("\"");
			int startOffset = _offset;
			for (; _offset < _text.length(); _offset++) {
				char c = _text.charAt(_offset);
				// Escaping. Escaped character will be skipped over in next
				// loop.
				if (c == '\\') {
					_offset++;
				} else if (c == '"') {
					break;
				}

			}
			expect("\"");
			String str = _text.substring(startOffset, _offset - 1);
			str = str.replace("\\\\", "\\");
			str = str.replace("\\\"", "\"");
			str = str.replace("\\r", "");
			str = str.replace("\\n", "\n");
			return str;
		}

		Object readNumber() throws Exception {
            int startOffset = _offset;
            boolean isFloat = false;
            for (;_offset < _text.length();_offset++)
            {
                char c = _text.charAt(_offset);
                if (c == '.')
                    isFloat = true;
                 
                if (isNumberChar(c))
                    continue;
                else
                    break; 
            }
            String numStr = _text.substring(startOffset, _offset);
            if (isFloat)
            {
                try {
                	float f = Float.parseFloat(numStr);
            		return f;
                }catch (NumberFormatException e) {
                	
                }
            }
            else
            {
            	 try {
                 	int i = Integer.parseInt(numStr);
             		return i;
                 }catch (NumberFormatException e) {
                 	
                 }
                 
            } 
            
            throw new Exception("Failed to parse number value");
        }

		boolean tryRead(String textToRead) throws Exception {
			if (_offset + textToRead.length() > _text.length())
				return false;

			for (int i = 0; i < textToRead.length(); i++) {
				if (textToRead.charAt(i) != _text.charAt(_offset + i))
					return false;

			}
			_offset += textToRead.length();
			return true;
		}

		void expect(String expectedStr) throws Exception {
			if (!tryRead(expectedStr))
				expect(false, expectedStr);

		}

		void expect(boolean condition, String message) throws Exception {
			if (!condition) {
				if (message == null) {
					message = "Unexpected token";
				} else {
					message = "Expected " + message;
				}
				message += " at offset " + _offset;
				throw new Exception(message);
			}

		}

		void skipWhitespace() throws Exception {
			while (_offset < _text.length()) {
				char c = _text.charAt(_offset);
				if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
					_offset++;
				else
					break;
			}
		}

		String _text;
		int _offset;
		Object _rootObject ;
	}

	static class Writer {
		public Writer(Object rootObject) throws Exception {
			_sb = new StringBuilder();
			writeObject(rootObject);
		}

		void writeObject(Object obj) throws Exception {
            if (obj instanceof Integer)
            {
                _sb.append((Integer)obj);
            }
            else if (obj instanceof Float)
            {
                String floatStr = obj.toString();
                _sb.append(floatStr);
                if (!floatStr.contains("."))
                    _sb.append(".0");
                 
            }
            else if (obj instanceof Boolean)
            {
                _sb.append((Boolean)obj == true ? "true" : "false");
            }
            else if (obj == null)
            {
                _sb.append("null");
            }
            else if (obj instanceof String)
            {
                String str = (String)obj;
                // Escape backslashes, quotes and newlines
                str = str.replace("\\", "\\\\");
                str = str.replace("\"", "\\\"");
                str = str.replace("\n", "\\n");
                str = str.replace("\r", "");
                _sb.append(String.format("\"{0}\"", str));
            }
            else if (obj instanceof HashMap<?, ?>)
            {
                writeHashMap((HashMap<String, Object>)obj);
            }
            else if (obj instanceof List<?>)
            {
                writeList((List<Object>)obj);
            }
            else
            {
                throw new Exception("ink's SimpleJson writer doesn't currently support this RTObject: " + obj);
            }       
        }

		void writeHashMap(HashMap<String, Object> dict) throws Exception {
            _sb.append("{");
            boolean isFirst = true;
            for (Entry<String,Object> keyValue : dict.entrySet())
            {
                if (!isFirst)
                    _sb.append(",");
                 
                _sb.append("\"");
                _sb.append(keyValue.getKey());
                _sb.append("\":");
                writeObject(keyValue.getValue());
                isFirst = false;
            }
            _sb.append("}");
        }

		void writeList(List<Object> list) throws Exception {
            _sb.append("[");
            boolean isFirst = true;
            for (Object obj : list)
            {
                if (!isFirst)
                    _sb.append(",");
                 
                writeObject(obj);
                isFirst = false;
            }
            _sb.append("]");
        }

		public String toString() {
			try {
				return _sb.toString();
			} catch (RuntimeException __dummyCatchVar0) {
				throw __dummyCatchVar0;
			} catch (Exception __dummyCatchVar0) {
				throw new RuntimeException(__dummyCatchVar0);
			}

		}

		StringBuilder _sb = new StringBuilder();
	}

}
