//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;


/**
* Exception that represents an error when running a Story at runtime.
* An exception being thrown of this type is typically when there's
* a bug in your ink, rather than in the ink engine itself!
*/
public class StoryException  extends System.Exception 
{
    public boolean useEndLineNumber = new boolean();
    /**
    * Constructs a default instance of a StoryException without a message.
    */
    public StoryException() throws Exception {
    }

    /**
    * Constructs an instance of a StoryException with a message.
    * 
    *  @param message The error message.
    */
    public StoryException(String message) throws Exception {
        super(message);
    }

}


