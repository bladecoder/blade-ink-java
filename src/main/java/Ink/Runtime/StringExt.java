//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;


public class StringExt   
{
    public static <T>String join(String separator, List<T> RTObjects) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ sb = new StringBuilder();
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ isFirst = true;
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ o : RTObjects)
        {
            if (!isFirst)
                sb.Append(separator);
             
            sb.Append(o.ToString());
            isFirst = false;
        }
        return sb.ToString();
    }

}


