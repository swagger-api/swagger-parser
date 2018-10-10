package io.swagger.v3.parser.models;

/**
 * Created by gracekarina on 16/06/17.
 */
public enum RefType {
    COMPONENTS("#/components/"),
    PATH("#/paths/");

    private final String internalPrefix;

    RefType(final String prefix) {
        this.internalPrefix = prefix;
    }

    /**
     * The prefix in an internal reference of this type.
     */
    public String getInternalPrefix() {
        return internalPrefix;
    }
}