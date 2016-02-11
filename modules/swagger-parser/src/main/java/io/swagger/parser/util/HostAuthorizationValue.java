package io.swagger.parser.util;

import io.swagger.models.auth.AuthorizationValue;

import java.net.URL;
import java.util.regex.Pattern;

public class HostAuthorizationValue extends AuthorizationValue implements ManagedValue {
    private final HostMatcher matcher;

    public HostAuthorizationValue(String host, String name, String value, String type) {
        this(new ExactHostMatcher(host), name, value, type);
    }

    public HostAuthorizationValue(Pattern host, String name, String value, String type) {
        this(new RxHostMatcher(host), name, value, type);
    }

    protected HostAuthorizationValue(HostMatcher matcher, String name, String value, String type) {
        super(name, value, type);
        this.matcher = matcher;
    }

    @Override
    public boolean process(URL url) {
        return matcher.match(url.getHost());
    }

    protected interface HostMatcher {

        boolean match(String host);
    }

    protected static class ExactHostMatcher implements HostMatcher {
        private final String host;

        public ExactHostMatcher(String host) {
            this.host = host;
        }

        @Override
        public boolean match(String host) {
            return this.host.equalsIgnoreCase(host);
        }
    }

    protected static class RxHostMatcher implements HostMatcher {
        private final Pattern rx;

        public RxHostMatcher(Pattern rx) {
            this.rx = rx;
        }

        @Override
        public boolean match(String host) {
            return rx.matcher(host).matches();
        }
    }
}
