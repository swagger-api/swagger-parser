package io.swagger.v3.parser.urlresolver;

import io.swagger.v3.parser.urlresolver.exceptions.HostDeniedException;
import io.swagger.v3.parser.urlresolver.models.ResolvedUrl;
import io.swagger.v3.parser.urlresolver.utils.NetUtils;
import mockit.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

public class PermittedUrlsCheckerTest {

    private final List<String> emptyAllowlist = Collections.emptyList();
    private final List<String> emptyDenylist = Collections.emptyList();
    @Mocked
    private NetUtils netUtils;

    private PermittedUrlsChecker checker;

    @BeforeMethod
    void beforeMethod() {
        this.checker = new PermittedUrlsChecker(Collections.emptyList(), Collections.emptyList());
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectPrivateSIITIPv4in6HostReferencesInABCDFormat() throws Exception {
        String url = "https://[0:0:0:0:0:ffff:10.1.33.147]:8000/v1/operation?theThing=something";
        String expectedIp = "10.1.33.147";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedIp;
            NetUtils.getHostByName(expectedIp); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = url;
        }};

        checker.verify(url);
    }

    @Test
    public void shouldAllowPublicSIITIPv4in6HostReferencesInABCDFormat() throws Exception {
        String url = "https://[0:0:0:0:0:ffff:1.2.3.4]:8000/v1/operation?theThing=something";
        String expectedIp = "1.2.3.4";
        String expectedUrl = "https://1.2.3.4:8000/v1/operation?theThing=something";
        String expectedHostHeader = "0:0:0:0:0:ffff:1.2.3.4";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHostHeader;
            NetUtils.getHostByName(expectedHostHeader); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = expectedUrl;
        }};

        ResolvedUrl result = checker.verify(url);

        Assert.assertEquals(result.getUrl(), expectedUrl);
        Assert.assertEquals(result.getHostHeader(), expectedHostHeader);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectPrivateSIITIPv4in6HostReferencesInIPv6Format() throws Exception {
        String url = "https://[0:0:0:0:0:ffff:a01:219]:8000/v1/operation?theThing=something";
        String expectedIp = "10.1.2.25";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedIp;
            NetUtils.getHostByName(expectedIp); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = url;
        }};

        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectNAT64HostReferences() throws Exception {
        String url = "https://[64:ff9b::]:8000/v1/operation?theThing=something";
        String expectedIp = "64:ff9b:0:0:0:0:0:0";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedIp;
            NetUtils.getHostByName(expectedIp); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = url;
            NetUtils.isNAT64Address(withInstanceOf(InetAddress.class)); times = 1; result = true;
        }};

        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldRejectDecimalIPsThatResolveToLocalIPs() throws Exception {
        String url = "https://3232235778:8000/api/v3/pet/findByStatus?status=available";
        String expectedIp = "192.168.1.2";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedIp;
            NetUtils.getHostByName(expectedIp); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = url;
        }};

        checker.verify(url);
    }

    @Test
    public void shouldPreferAllowlistOverEverythingElse() throws Exception {
        String url = "https://localhost:3000/1";
        String expectedHostname = "localhost";
        List<String> allowlist = Collections.singletonList("localhost");
        this.checker = new PermittedUrlsChecker(allowlist, emptyDenylist);

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHostname;
        }};

        ResolvedUrl result = checker.verify(url);

        Assert.assertEquals(result.getUrl(), "https://localhost:3000/1");
        Assert.assertEquals(result.getHostHeader(), "localhost");
    }

    @Test
    public void shouldAllowPublicDomainsByDefault() throws Exception {
        String url = "https://smartbear.com:3000/1";
        String expectedUrl = "https://1.2.3.4:3000/1";
        String expectedHost = "smartbear.com";
        String expectedIp = "1.2.3.4";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHost;
            NetUtils.getHostByName(expectedHost); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = expectedUrl;
        }};

        this.checker = new PermittedUrlsChecker(emptyAllowlist, emptyDenylist);
        ResolvedUrl result = checker.verify(url);

        Assert.assertEquals(result.getUrl(), expectedUrl);
        Assert.assertEquals(result.getHostHeader(), expectedHost);
    }

    @Test
    public void shouldAllowPublicIPsByDefault() throws Exception {
        String url = "https://1.2.3.4:3000/1";
        String expectedHost = "1.2.3.4";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHost;
            NetUtils.getHostByName(expectedHost); times = 1; result = InetAddress.getByName(expectedHost);
            NetUtils.setHost(url, expectedHost); times = 1; result = url;
        }};

        this.checker = new PermittedUrlsChecker(emptyAllowlist, emptyDenylist);
        ResolvedUrl result = checker.verify(url);

        Assert.assertEquals(result.getUrl(), url);
        Assert.assertEquals(result.getHostHeader(), expectedHost);
    }

    @Test(
            dataProvider = "shouldBlockRestrictedIPv4sByDefault",
            expectedExceptions = HostDeniedException.class,
            expectedExceptionsMessageRegExp = ".*IP is restricted.*"
    )
    public void shouldBlockIPv4Localhost(String url, String expectedIp) throws Exception {
        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedIp;
            NetUtils.getHostByName(expectedIp); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = url;
        }};

        checker.verify(url);
    }

    @DataProvider(name = "shouldBlockRestrictedIPv4sByDefault")
    private Object[][] shouldBlockRestrictedIPv4sByDefault() {
        return new Object[][]{
                {"https://localhost:3000/1", "127.0.0.1"},
                {"https://127.0.0.1/", "127.0.0.1"},
                {"https://192.168.1.2/", "192.168.1.2"},
                {"https://127.3", "127.0.0.3"}
        };
    }

    @Test(
            dataProvider = "shouldBlockRestrictedIPv6sByDefault",
            expectedExceptions = HostDeniedException.class,
            expectedExceptionsMessageRegExp = ".*IP is restricted.*"
    )
    public void shouldBlockIPv6Localhost(String url, String expectedIp) throws Exception {
        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedIp;
            NetUtils.getHostByName(expectedIp); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = url;
            NetUtils.isUniqueLocalAddress(withInstanceOf(InetAddress.class)); result = true;
        }};

        checker.verify(url);
    }

    @DataProvider(name = "shouldBlockRestrictedIPv6sByDefault")
    private Object[][] shouldBlockRestrictedIPv6sByDefault() {
        return new Object[][]{
                {"https://[fc00::1]/", "fc00:0:0:0:0:0:0:1"},
                {"https://[fd00:ec2::254]/", "fd00:ec2:0:0:0:0:0:254"}
        };
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is restricted.*")
    public void shouldBlockDomainNamesThatResolveToPrivateIPs() throws Exception {
        String url = "https://evil.com";
        String expectedUrl = "https://192.168.1.1:3000/1";
        String expectedHost = "evil.com";
        String expectedIp = "192.168.1.1";

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHost;
            NetUtils.getHostByName(expectedHost); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = expectedUrl;
        }};

        this.checker = new PermittedUrlsChecker(emptyAllowlist, emptyDenylist);
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*URL is part of the explicit denylist.*")
    public void shouldBlockSpecificallyDenylistedURLs() throws Exception {
        String url = "https://smartbear.com";
        List<String> denylist = Collections.singletonList("smartbear.com");

        this.checker = new PermittedUrlsChecker(emptyAllowlist, denylist);
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*IP is part of the explicit denylist.*")
    public void shouldBlockBasedOnResolvedIP() throws Exception {
        String url = "https://smartbear.com";
        String expectedUrl = "https://1.2.3.4:3000/1";
        String expectedHost = "smartbear.com";
        String expectedIp = "1.2.3.4";
        List<String> denylist = Collections.singletonList("1.2.3.4");

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHost;
            NetUtils.getHostByName(expectedHost); times = 1; result = InetAddress.getByName(expectedIp);
            NetUtils.setHost(url, expectedIp); times = 1; result = expectedUrl;
        }};

        this.checker = new PermittedUrlsChecker(emptyAllowlist, denylist);
        checker.verify(url);
    }

    @Test(expectedExceptions = HostDeniedException.class, expectedExceptionsMessageRegExp = ".*URL is part of the explicit denylist.*")
    public void shouldBlockURLMatchingWildcardPattern() throws Exception {
        String url = "https://foo.example.com";
        String expectedHost = "foo.example.com";
        List<String> denylist = Collections.singletonList("f*.example.com");

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHost;
        }};

        this.checker = new PermittedUrlsChecker(emptyAllowlist, denylist);
        checker.verify(url);
    }

    @Test
    public void shouldAllowURLMatchingWildcardPattern() throws Exception {
        String url = "https://foo.example.com";
        String expectedHost = "foo.example.com";
        List<String> allowlist = Collections.singletonList("f*.example.com");

        new Expectations() {{
            NetUtils.getHostFromUrl(url); times = 1; result = expectedHost;
        }};

        this.checker = new PermittedUrlsChecker(allowlist, emptyDenylist);
        checker.verify(url);
    }

}
