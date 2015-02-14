package io.swagger.report;

public enum Severity {
    ERROR(1), WARNING(2), RECOMMENDED(3), OPTIONAL(4);

    private final int level;

    Severity(int level) {
        this.level = level;
    }

    /**
     * Checks if this Severity level is higher than the given one.
     *
     * @param severity The Severity to check against.
     * @return true if the severity level of the instance is higher than the input, false otherwise.
     */
    public boolean isMoreSevere(Severity severity) {
        if (severity.level > level) {
            return true;
        } else {
            return false;
        }
    }
}