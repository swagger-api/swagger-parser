package com.wordnik.swagger.transform.util;

public final class SwaggerTransformException
    extends Exception
{
    public SwaggerTransformException()
    {
    }

    public SwaggerTransformException(final String message)
    {
        super(message);
    }

    public SwaggerTransformException(final Throwable cause)
    {
        super(cause);
    }
}
