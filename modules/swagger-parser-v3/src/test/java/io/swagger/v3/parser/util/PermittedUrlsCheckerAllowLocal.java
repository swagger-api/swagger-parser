package io.swagger.v3.parser.util;

import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;

import java.net.InetAddress;

class PermittedUrlsCheckerAllowLocal extends PermittedUrlsChecker {
    @Override
    protected boolean isRestrictedIpRange(InetAddress ip) {
        // Allow all IPs for testing purposes
        return false;
    }
}
