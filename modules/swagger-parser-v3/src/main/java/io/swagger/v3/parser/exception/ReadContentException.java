package io.swagger.v3.parser.exception;

/**
 * Happens when it's unable to read content from file or other resource
 */
public class ReadContentException extends RuntimeException {
    private static final long serialVersionUID = 4720926576862628428L;

    public ReadContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
