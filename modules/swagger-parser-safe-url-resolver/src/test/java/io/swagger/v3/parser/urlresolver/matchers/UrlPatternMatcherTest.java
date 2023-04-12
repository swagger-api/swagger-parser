package io.swagger.v3.parser.urlresolver.matchers;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

public class UrlPatternMatcherTest {

    @Test
    public void returnsFalseWhenUrlCannotBeParsed() {
        List<String> patterns = Collections.emptyList();
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("not a url"));
    }

    @Test
    public void returnsFalseWhenUrlIsNotHttpOrHttps() {
        List<String> patterns = Collections.emptyList();
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("file://not a url"));
    }

    @Test
    public void domainWithoutPortMatchesAnyPort() {
        List<String> patterns = Collections.singletonList("example.com");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertTrue(matcher.matches("http://example.com"));
        Assert.assertTrue(matcher.matches("https://example.com"));
        Assert.assertTrue(matcher.matches("http://example.com:12345"));
        Assert.assertTrue(matcher.matches("https://example.com:12345"));
        Assert.assertFalse(matcher.matches("https://not.example.com:12345"));
    }

    @Test
    public void domainWithPortMatchesOnlyThatPort() {
        List<String> patterns = Collections.singletonList("example.com:443");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("http://example.com"));
        Assert.assertTrue(matcher.matches("https://example.com"));
        Assert.assertTrue(matcher.matches("http://example.com:443"));
        Assert.assertFalse(matcher.matches("http://example.com:12345"));
        Assert.assertFalse(matcher.matches("https://example.com:1234"));
        Assert.assertFalse(matcher.matches("https://not.example.com:12345"));
    }

    @Test
    public void domainSupportsWildcards() {
        List<String> patterns = Collections.singletonList("*.example.com");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("http://example.com"));
        Assert.assertFalse(matcher.matches("https://example.com"));
        Assert.assertFalse(matcher.matches("https://fooexample.com"));
        Assert.assertTrue(matcher.matches("https://foo.example.com"));
        Assert.assertTrue(matcher.matches("https://foo.bar.example.com"));
    }

    @Test
    public void domainInUrlIsCaseInsensitive() {
        List<String> patterns = Collections.singletonList("*.example.com");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("http://ExAmPlE.CoM"));
        Assert.assertTrue(matcher.matches("https://FoO.ExAmPlE.CoM"));
    }

    @Test
    public void domainInPatternIsCaseInsensitive() {
        List<String> patterns = Collections.singletonList("*.EXamPLe.Com");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("http://ExAmPlE.CoM"));
        Assert.assertTrue(matcher.matches("https://FoO.ExAmPlE.CoM"));
    }

    @Test
    public void supportForMatchingInternationalizedDomainNames() {
        List<String> patterns = Collections.singletonList("*.😋.local");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("http://example.com"));
        Assert.assertTrue(matcher.matches("http://blah.😋.local"));
        Assert.assertTrue(matcher.matches("http://blah.xn--p28h.local"));
    }

    @Test
    public void domainsDoNotSupportWildcardsAtTheEnd() {
        List<String> patterns = Collections.singletonList("example.co*");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("https://example.net"));
        Assert.assertFalse(matcher.matches("https://example.co.uk"));
        Assert.assertFalse(matcher.matches("https://example.com"));
    }

    @Test
    public void ipAddressesSupportWildcardsAtTheEnd() {
        List<String> patterns = Collections.singletonList("10.100.*.*");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertTrue(matcher.matches("http://10.100.1.2"));
        Assert.assertFalse(matcher.matches("http://10.101.1.2"));
    }

    @Test
    public void worksWithUrlsWithAuthPathAndQueryComponents() {
        List<String> patterns = Collections.singletonList("*.example.com");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertFalse(matcher.matches("https://foo:bar@example.com/path?q=1"));
        Assert.assertTrue(matcher.matches("https://foo:bar@foo.example.com/path?q=1"));
    }

    @Test
    public void supportsIpAddressesInPatterns() {
        List<String> patterns = Collections.singletonList("1.*.3.4");
        UrlPatternMatcher matcher = new UrlPatternMatcher(patterns);

        Assert.assertTrue(matcher.matches("https://foo:bar@1.2.3.4/path?q=1"));
        Assert.assertFalse(matcher.matches("https://foo:bar@1.2.3.5/path?q=1"));
    }

}
