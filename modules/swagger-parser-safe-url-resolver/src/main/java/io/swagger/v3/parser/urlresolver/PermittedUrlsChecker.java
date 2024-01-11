package io.swagger.v3.parser.urlresolver;

import io.swagger.v3.parser.urlresolver.exceptions.HostDeniedException;
import io.swagger.v3.parser.urlresolver.matchers.UrlPatternMatcher;
import io.swagger.v3.parser.urlresolver.models.ResolvedUrl;
import io.swagger.v3.parser.urlresolver.utils.NetUtils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class PermittedUrlsChecker {

    protected final UrlPatternMatcher allowlistMatcher;
    protected final UrlPatternMatcher denylistMatcher;

    public PermittedUrlsChecker() {
        this.allowlistMatcher = new UrlPatternMatcher(Collections.emptyList());
        this.denylistMatcher = new UrlPatternMatcher(Collections.emptyList());
    }

    public PermittedUrlsChecker(List<String> allowlist, List<String> denylist) {
        if(allowlist != null) {
            this.allowlistMatcher = new UrlPatternMatcher(allowlist);
        } else {
            this.allowlistMatcher = new UrlPatternMatcher(Collections.emptyList());
        }

        if(denylist != null) {
            this.denylistMatcher = new UrlPatternMatcher(denylist);
        } else {
            this.denylistMatcher = new UrlPatternMatcher(Collections.emptyList());
        }
    }

    public ResolvedUrl verify(String url) throws HostDeniedException {
        URL parsed;

        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            throw new HostDeniedException(String.format("Failed to parse URL. URL [%s]", url), e);
        }

        if (!parsed.getProtocol().equals("http") && !parsed.getProtocol().equals("https")) {
            throw new HostDeniedException(String.format("URL does not use a supported protocol. URL [%s]", url));
        }

        String hostname;
        try {
            hostname = NetUtils.getHostFromUrl(url);
        } catch (MalformedURLException e) {
            throw new HostDeniedException(String.format("Failed to get hostname from URL. URL [%s]", url), e);
        }

        if (this.allowlistMatcher.matches(url)) {
            return new ResolvedUrl(url, hostname);
        }

        if (this.denylistMatcher.matches(url)) {
            throw new HostDeniedException(String.format("URL is part of the explicit denylist. URL [%s]", url));
        }

        InetAddress ip;
        try {
            ip = NetUtils.getHostByName(hostname);
        } catch (UnknownHostException e) {
            throw new HostDeniedException(
                    String.format("Failed to resolve IP from hostname. Hostname [%s]", hostname), e);
        }

        String urlWithIp;
        try {
            urlWithIp = NetUtils.setHost(url, ip.getHostAddress());
        } catch (MalformedURLException e) {
            throw new HostDeniedException(
                    String.format("Failed to create new URL with IP. IP [%s] URL [%s]", ip.getHostAddress(), url), e);
        }

        if (this.allowlistMatcher.matches(urlWithIp)) {
            return new ResolvedUrl(urlWithIp, hostname);
        }

        if (isRestrictedIpRange(ip)) {
            throw new HostDeniedException(String.format("IP is restricted. URL [%s]", urlWithIp));
        }

        if (this.denylistMatcher.matches(urlWithIp)) {
            throw new HostDeniedException(String.format("IP is part of the explicit denylist. URL [%s]", urlWithIp));
        }

        return new ResolvedUrl(urlWithIp, hostname);
    }

    protected boolean isRestrictedIpRange(InetAddress ip) {
        return ip.isLinkLocalAddress()
                || ip.isSiteLocalAddress()
                || ip.isLoopbackAddress()
                || ip.isAnyLocalAddress()
                || NetUtils.isUniqueLocalAddress(ip)
                || NetUtils.isNAT64Address(ip);
    }
}
