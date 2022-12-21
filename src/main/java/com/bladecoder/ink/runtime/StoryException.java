package com.bladecoder.ink.runtime;

/**
 * Exception that represents an error when running a Story at runtime. An
 * exception being thrown of this type is typically when there's a bug in your
 * ink, rather than in the ink engine itself!
 */
@SuppressWarnings("serial")
public class StoryException extends Exception {
    public boolean useEndLineNumber;

    /**
     * Constructs a default instance of a StoryException without a message.
     */
    public StoryException() throws Exception {}

    /**
     * Constructs an instance of a StoryException with a message.
     *
     * @param message
     *            The error message.
     */
    public StoryException(String message) throws Exception {
        super(message);
    }
}
