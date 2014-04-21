package com.wordnik.swagger.transform.util;

public final class UncheckedSwaggerTransformException
    extends RuntimeException
{
    public UncheckedSwaggerTransformException()
    {
    }

    public UncheckedSwaggerTransformException(final String message)
    {
        super(message);
    }

    public UncheckedSwaggerTransformException(final Throwable cause)
    {
        super(cause);
    }
}
