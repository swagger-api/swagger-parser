package io.swagger.parser.urlresolver.exceptions;

public class HostDeniedException extends Exception {
    public HostDeniedException(String message) {
        super(message);
    }

    public HostDeniedException(String message, Throwable e) {
        super(message, e);
    }
}
