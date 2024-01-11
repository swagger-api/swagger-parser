package io.swagger.parser.util;

import java.util.List;

public class ParseOptions {
    private boolean resolve;
    private boolean flatten;
    private boolean safelyResolveURL;
    private List<String> remoteRefAllowList;
    private List<String> remoteRefBlockList;

    public boolean isResolve() {
        return resolve;
    }

    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }

    public boolean isFlatten() { return flatten; }

    public void setFlatten(boolean flatten) { this.flatten = flatten; }

    public boolean isSafelyResolveURL() {
        return safelyResolveURL;
    }

    public void setSafelyResolveURL(boolean safelyResolveURL) {
        this.safelyResolveURL = safelyResolveURL;
    }

    public List<String> getRemoteRefAllowList() {
        return remoteRefAllowList;
    }

    public void setRemoteRefAllowList(List<String> remoteRefAllowList) {
        this.remoteRefAllowList = remoteRefAllowList;
    }

    public List<String> getRemoteRefBlockList() {
        return remoteRefBlockList;
    }

    public void setRemoteRefBlockList(List<String> remoteRefBlockList) {
        this.remoteRefBlockList = remoteRefBlockList;
    }
}
