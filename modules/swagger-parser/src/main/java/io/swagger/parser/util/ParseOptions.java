package io.swagger.parser.util;

public class ParseOptions {
    private boolean resolve;
    private boolean flatten;

    public boolean isResolve() {
        return resolve;
    }

    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }

    public boolean isFlatten() { return flatten; }

    public void setFlatten(boolean flatten) { this.flatten = flatten; }
}
