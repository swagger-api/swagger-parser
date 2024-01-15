package io.swagger.v3.parser.urlresolver.matchers;

import io.swagger.v3.parser.urlresolver.utils.NetUtils;

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.FilenameUtils.wildcardMatch;

public class UrlPatternMatcher {

    private final List<String> patterns;

    public UrlPatternMatcher(List<String> patterns) {
        this.patterns = new ArrayList<>();

        patterns.forEach(pattern -> {
            String patternLower = pattern.toLowerCase();
            String hostAndPort = pattern.contains(":") ? patternLower : patternLower + ":*";
            String[] split = hostAndPort.split(":");
            String host = Character.isDigit(split[0].charAt(0)) ? split[0] : IDN.toASCII(split[0], IDN.ALLOW_UNASSIGNED);
            String port = split.length > 1 ? split[1] : "*";

            // Ignore domains that end in a wildcard
            if (host.length() > 1 && !NetUtils.isIPv4(host.replace("*", "0")) && host.endsWith("*")) {
                return;
            }

            this.patterns.add(String.format("%s:%s", host, port));
        });
    }

    public boolean matches(String url) {
        URL parsed;
        try {
            parsed = new URL(url.toLowerCase());
        } catch (MalformedURLException e) {
            return false;
        }

        String host = IDN.toASCII(parsed.getHost(), IDN.ALLOW_UNASSIGNED);
        String hostAndPort;
        if (parsed.getPort() == -1) {
            if (parsed.getProtocol().equals("http")) {
                hostAndPort = host + ":80";
            } else if (parsed.getProtocol().equals("https")) {
                hostAndPort = host + ":443";
            } else {
                return false;
            }
        } else {
            hostAndPort = host + ":" + parsed.getPort();
        }

        return this.patterns.stream().anyMatch(pattern -> wildcardMatch(hostAndPort, pattern));
    }
}
