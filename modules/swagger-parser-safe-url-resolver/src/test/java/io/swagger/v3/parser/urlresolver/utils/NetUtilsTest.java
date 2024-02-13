package io.swagger.v3.parser.urlresolver.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class NetUtilsTest {

    @Test
    public void getHostFromUrlWithDomainNameShouldReturnHostname() throws MalformedURLException {
        String url = "https://example.com/hello?query=world";

        String hostname = NetUtils.getHostFromUrl(url);

        Assert.assertEquals(hostname, "example.com");
    }

    @Test
    public void getHostFromUrlWithIPv4AddressShouldReturnIPAddress() throws MalformedURLException {
        String url = "https://1.2.3.4/hello?query=world";

        String hostname = NetUtils.getHostFromUrl(url);

        Assert.assertEquals(hostname, "1.2.3.4");
    }

    @Test
    public void getHostFromUrlWithIPv6AddressShouldReturnIPAddress() throws MalformedURLException {
        String url = "https://[::1]/hello?query=world";

        String hostname = NetUtils.getHostFromUrl(url);

        Assert.assertEquals(hostname, "::1");
    }

    @Test
    public void setHostShouldSetIPv4AddressInUrl() throws MalformedURLException {
        String url = "https://example.com/hello?query=world";
        String ip = "1.2.3.4";

        String result = NetUtils.setHost(url, ip);

        Assert.assertEquals(result, "https://1.2.3.4/hello?query=world");
    }

    @Test
    public void setHostShouldSetIPv6AddressInUrlWithBrackets() throws MalformedURLException {
        String url = "https://example.com/hello?query=world";
        String ip = "::1";

        String result = NetUtils.setHost(url, ip);

        Assert.assertEquals(result, "https://[::1]/hello?query=world");
    }

    @Test
    public void isIPv4WithIPv4AddressShouldReturnTrue() {
        String ip = "1.2.3.4";

        Assert.assertTrue(NetUtils.isIPv4(ip));
    }

    @Test
    public void isIPv4WithIPv6AddressShouldReturnFalse() {
        String ip = "::1";

        Assert.assertFalse(NetUtils.isIPv4(ip));
    }

    @Test
    public void isIPv6WithIPv6AddressShouldReturnTrue() {
        String ip = "::1";

        Assert.assertTrue(NetUtils.isIPv6(ip));
    }

    @Test
    public void isIPv6WithIPv4AddressShouldReturnFalse() {
        String ip = "1.2.3.4";

        Assert.assertFalse(NetUtils.isIPv6(ip));
    }

    @Test
    public void isIPv6WithImproperAddressShouldReturnFalse() {
        String ip = "999.999.999.999";

        Assert.assertFalse(NetUtils.isIPv6(ip));
    }

    @Test
    public void isUniqueLocalAddressWithULAShouldReturnTrue() throws UnknownHostException {
        InetAddress ulaIp = InetAddress.getByName("fc00::1");
        InetAddress ulaIpWithLBit = InetAddress.getByName("fd00:ec2::254");

        Assert.assertTrue(NetUtils.isUniqueLocalAddress(ulaIp));
        Assert.assertTrue(NetUtils.isUniqueLocalAddress(ulaIpWithLBit));
    }

    @Test
    public void isUniqueLocalAddressWithNonULAIPv6AddressShouldReturnFalse() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("::1");

        Assert.assertFalse(NetUtils.isUniqueLocalAddress(ip));
    }

    @Test
    public void isUniqueLocalAddressWithIPv4AddressShouldReturnFalse() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("1.2.3.4");

        Assert.assertFalse(NetUtils.isUniqueLocalAddress(ip));
    }

    @Test
    public void isNAT64WithNAT64AddressShouldReturnTrue() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("64:ff9b::");

        Assert.assertTrue(NetUtils.isNAT64Address(ip));
    }

    @Test
    public void isNAT64WithRegularIPv6AddressShouldReturnFalse() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("fc00::1");

        Assert.assertFalse(NetUtils.isNAT64Address(ip));
    }

    @Test
    public void isNAT64WithIPv4AddressShouldReturnFalse() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("1.2.3.4");

        Assert.assertFalse(NetUtils.isNAT64Address(ip));
    }

}
