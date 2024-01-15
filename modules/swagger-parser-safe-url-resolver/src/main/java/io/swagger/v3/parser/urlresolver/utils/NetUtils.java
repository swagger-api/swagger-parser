package io.swagger.v3.parser.urlresolver.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class NetUtils {

    private NetUtils() {}

    public static InetAddress getHostByName(String hostname) throws UnknownHostException {
        return InetAddress.getByName(hostname);
    }

    public static String getHostFromUrl(String url) throws MalformedURLException {
        String hostnameOrIP = new URL(url).getHost();
        //IPv6 addresses in URLs are surrounded by square brackets
        if (hostnameOrIP.length() > 2 && hostnameOrIP.startsWith("[") && hostnameOrIP.endsWith("]")) {
            return hostnameOrIP.substring(1, hostnameOrIP.length() - 1);
        }
        return hostnameOrIP;
    }

    public static String setHost(String url, String host) throws MalformedURLException {
        URL parsed = new URL(url);
        if (isIPv6(host)) {
            return url.replace(parsed.getHost(), "[" + host + "]");
        } else {
            return url.replace(parsed.getHost(), host);
        }
    }

    public static boolean isIPv4(String ipAddress) {
        boolean isIPv4 = false;

        if (ipAddress != null) {
            try {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                isIPv4 = (inetAddress instanceof Inet4Address);
            } catch (UnknownHostException ignored) {
                return false;
            }
        }

        return isIPv4;
    }

    public static boolean isIPv6(String ipAddress) {
        boolean isIPv6 = false;

        if (ipAddress != null) {
            try {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                isIPv6 = (inetAddress instanceof Inet6Address);
            } catch (UnknownHostException ignored) {
                return false;
            }
        }

        return isIPv6;
    }

    // Not picked up by Inet6Address.is*Address() checks
    public static boolean isUniqueLocalAddress(InetAddress ip) {
        // Only applies to IPv6
        if (ip instanceof Inet4Address) {
            return false;
        }

        byte[] address = ip.getAddress();
        return (address[0] & 0xff) == 0xfc || (address[0] & 0xff) == 0xfd;
    }

    // Not picked up by Inet6Address.is*Address() checks
    public static boolean isNAT64Address(InetAddress ip) {
        // Only applies to IPv6
        if (ip instanceof Inet4Address) {
            return false;
        }

        byte[] address = ip.getAddress();
        return (address[0] & 0xff) == 0x00
                && (address[1] & 0xff) == 0x64
                && (address[2] & 0xff) == 0xff
                && (address[3] & 0xff) == 0x9b;
    }
}
