package io.swagger.exception;

public class RefException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RefException(String ref) {
		super("Ref [" + ref + "] not found in yaml!");
	}

	public RefException(String ref, String file) {
		super("Ref [" + ref + "] not found in yaml [" + file + "]");
	}

}
