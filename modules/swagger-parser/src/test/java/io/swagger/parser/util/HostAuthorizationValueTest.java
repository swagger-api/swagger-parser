package io.swagger.parser.util;

import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

public class HostAuthorizationValueTest {

    @Test
    public void testExactMatcher() throws MalformedURLException {
        final HostAuthorizationValue value = new HostAuthorizationValue("swagger.io", "k", "v",
                "header");
        final Map<String, Boolean> urls = ImmutableMap.<String, Boolean> builder()
                .put("http://swagger.io", true).put("http://swagger.io/swagger.json", true)
                .put("http://SwAgGeR.iO/swagger.yaml", true).put("http://petstore.swagger.io", false)
                .build();
        for (Map.Entry<String, Boolean> url : urls.entrySet()) {
            Assert.assertEquals(value.process(new URL(url.getKey())), (boolean) url.getValue(),
                    url.getKey());
        }
    }

    @Test
    public void testPatternMatcher() throws MalformedURLException {
        final HostAuthorizationValue value = new HostAuthorizationValue(
                Pattern.compile("([^.]+\\.)*swagger.io", Pattern.CASE_INSENSITIVE), "k", "v",
                "header");

        final Map<String, Boolean> urls = ImmutableMap.<String, Boolean> builder()
                .put("http://swagger.io", true).put("http://a.b.c.swagger.io", true)
                .put("http://json.swagger.io/swagger.json", true)
                .put("http://yaml.SwAgGeR.iO/swagger.yaml", true)
                .put("http://not-swagger.io", false).put("http://petstore", false).build();
        for (Map.Entry<String, Boolean> url : urls.entrySet()) {
            Assert.assertEquals(value.process(new URL(url.getKey())), (boolean) url.getValue(),
                    url.getKey());
        }
    }
}
