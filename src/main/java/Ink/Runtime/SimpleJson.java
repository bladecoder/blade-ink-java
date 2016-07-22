//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package Ink.Runtime;

import CS2JNet.JavaSupport.language.RefSupport;

public class SimpleJson   
{
    public static String HashMapToText(HashMap<String, RTObject> rootRTObject) throws Exception {
        return new Writer(rootRTObject).toString();
    }

    public static HashMap<String, RTObject> textToHashMap(String text) throws Exception {
        return new Reader(text).toHashMap();
    }

    static class Reader   
    {
        public Reader(String text) throws Exception {
            _text = text;
            _offset = 0;
            skipWhitespace();
            _rootRTObject = readRTObject();
        }

        public HashMap<String, RTObject> toHashMap() throws Exception {
            return (HashMap<String, RTObject>)_rootRTObject;
        }

        boolean isNumberChar(char c) throws Exception {
            return c >= '0' && c <= '9' || c == '.' || c == '-' || c == '+';
        }

        RTObject readRTObject() throws Exception {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ currentChar = _text[_offset];
            if (currentChar == '{')
                return readHashMap();
            else if (currentChar == '[')
                return readArray();
            else if (currentChar == '"')
                return readString();
            else if (IsNumberChar(currentChar))
                return readNumber();
            else if (tryRead("true"))
                return true;
            else if (tryRead("false"))
                return false;
            else if (tryRead("null"))
                return null;
                   
            throw new System.Exception("Unhandled RTObject type in JSON: " + _text.Substring(_offset, 30));
        }

        HashMap<String, RTObject> readHashMap() throws Exception {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ dict = new HashMap<String, RTObject>();
            expect("{");
            skipWhitespace();
            // Empty HashMap?
            if (tryRead("}"))
                return dict;
             
            do
            {
                skipWhitespace();
                // Key
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ key = readString();
                expect(key != null,"HashMap key");
                skipWhitespace();
                // :
                expect(":");
                skipWhitespace();
                // Value
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ val = readRTObject();
                expect(val != null,"HashMap value");
                // Add to HashMap
                dict[key] = val;
                skipWhitespace();
            }
            while (tryRead(","));
            expect("}");
            return dict;
        }

        List<RTObject> readArray() throws Exception {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ list = new List<RTObject>();
            expect("[");
            skipWhitespace();
            // Empty list?
            if (tryRead("]"))
                return list;
             
            do
            {
                skipWhitespace();
                // Value
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ val = readRTObject();
                // Add to array
                list.Add(val);
                skipWhitespace();
            }
            while (tryRead(","));
            expect("]");
            return list;
        }

        String readString() throws Exception {
            expect("\"");
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ startOffset = _offset;
            for (;_offset < _text.Length;_offset++)
            {
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ c = _text[_offset];
                // Escaping. Escaped character will be skipped over in next loop.
                if (c == '\\')
                {
                    _offset++;
                }
                else if (c == '"')
                {
                    break;
                }
                  
            }
            expect("\"");
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ str = _text.Substring(startOffset, _offset - startOffset - 1);
            str = str.Replace("\\\\", "\\");
            str = str.Replace("\\\"", "\"");
            str = str.Replace("\\r", "");
            str = str.Replace("\\n", "\n");
            return str;
        }

        RTObject readNumber() throws Exception {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ startOffset = _offset;
            boolean isFloat = false;
            for (;_offset < _text.Length;_offset++)
            {
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ c = _text[_offset];
                if (c == '.')
                    isFloat = true;
                 
                if (IsNumberChar(c))
                    continue;
                else
                    break; 
            }
            String numStr = _text.Substring(startOffset, _offset - startOffset);
            if (isFloat)
            {
                float f = new float();
                RefSupport<float> refVar___0 = new RefSupport<float>();
                boolean boolVar___0 = float.TryParse(numStr, refVar___0);
                f = refVar___0.getValue();
                if (boolVar___0)
                {
                    return f;
                }
                 
            }
            else
            {
                int i = new int();
                RefSupport<int> refVar___1 = new RefSupport<int>();
                boolean boolVar___1 = int.TryParse(numStr, refVar___1);
                i = refVar___1.getValue();
                if (boolVar___1)
                {
                    return i;
                }
                 
            } 
            throw new System.Exception("Failed to parse number value");
        }

        boolean tryRead(String textToRead) throws Exception {
            if (_offset + textToRead.Length > _text.Length)
                return false;
             
            for (int i = 0;i < textToRead.Length;i++)
            {
                if (textToRead[i] != _text[_offset + i])
                    return false;
                 
            }
            _offset += textToRead.Length;
            return true;
        }

        void expect(String expectedStr) throws Exception {
            if (!tryRead(expectedStr))
                expect(false,expectedStr);
             
        }

        void expect(boolean condition, String message) throws Exception {
            if (!condition)
            {
                if (message == null)
                {
                    message = "Unexpected token";
                }
                else
                {
                    message = "Expected " + message;
                } 
                message += " at offset " + _offset;
                throw new System.Exception(message);
            }
             
        }

        void skipWhitespace() throws Exception {
            while (_offset < _text.Length)
            {
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ c = _text[_offset];
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
                    _offset++;
                else
                    break; 
            }
        }

        String _text = new String();
        int _offset = new int();
        RTObject _rootRTObject = new RTObject();
    }

    static class Writer   
    {
        public Writer(RTObject rootRTObject) throws Exception {
            _sb = new StringBuilder();
            writeRTObject(rootRTObject);
        }

        void writeRTObject(RTObject obj) throws Exception {
            if (obj instanceof int)
            {
                _sb.Append((Integer)obj);
            }
            else if (obj instanceof float)
            {
                String floatStr = obj.ToString();
                _sb.Append(floatStr);
                if (!floatStr.Contains("."))
                    _sb.Append(".0");
                 
            }
            else if (obj instanceof boolean)
            {
                _sb.Append((Boolean)obj == true ? "true" : "false");
            }
            else if (obj == null)
            {
                _sb.Append("null");
            }
            else if (obj instanceof String)
            {
                String str = (String)obj;
                // Escape backslashes, quotes and newlines
                str = str.Replace("\\", "\\\\");
                str = str.Replace("\"", "\\\"");
                str = str.Replace("\n", "\\n");
                str = str.Replace("\r", "");
                _sb.AppendFormat("\"{0}\"", str);
            }
            else if (obj instanceof HashMap<String, RTObject>)
            {
                WriteHashMap((HashMap<String, RTObject>)obj);
            }
            else if (obj instanceof List<RTObject>)
            {
                WriteList((List<RTObject>)obj);
            }
            else
            {
                throw new System.Exception("ink's SimpleJson writer doesn't currently support this RTObject: " + obj);
            }       
        }

        void writeHashMap(HashMap<String, RTObject> dict) throws Exception {
            _sb.Append("{");
            boolean isFirst = true;
            for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ keyValue : dict)
            {
                if (!isFirst)
                    _sb.Append(",");
                 
                _sb.Append("\"");
                _sb.Append(keyValue.Key);
                _sb.Append("\":");
                writeRTObject(keyValue.Value);
                isFirst = false;
            }
            _sb.Append("}");
        }

        void writeList(List<RTObject> list) throws Exception {
            _sb.Append("[");
            boolean isFirst = true;
            for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ obj : list)
            {
                if (!isFirst)
                    _sb.Append(",");
                 
                writeRTObject(obj);
                isFirst = false;
            }
            _sb.Append("]");
        }

        public String toString() {
            try
            {
                return _sb.ToString();
            }
            catch (RuntimeException __dummyCatchVar0)
            {
                throw __dummyCatchVar0;
            }
            catch (Exception __dummyCatchVar0)
            {
                throw new RuntimeException(__dummyCatchVar0);
            }
        
        }

        StringBuilder _sb = new StringBuilder();
    }

}


