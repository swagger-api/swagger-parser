package io.swagger.parser.models;

public class ParseOptions {
    private boolean resolve;
    private boolean resolveFully;

    public boolean isResolve() {
        return resolve;
    }

    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }

    public boolean isResolveFully() {
        return resolveFully;
    }

    public void setResolveFully(boolean resolveFully) {
        this.resolveFully = resolveFully;
    }
}
