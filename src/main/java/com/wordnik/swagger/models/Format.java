package com.wordnik.swagger.models;

/**
 * Created by ron on 11/04/14.
 */
public enum Format {
    INT32("int32"), INT64("int64"), FLOAT("float"), DOUBLE("double"), BYTE("byte"), DATE("date"), DATE_TIME("date-time");

    private final String format;

    Format(String format) {
         this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }
}
