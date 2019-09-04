package io.swagger.v3.parser.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class RemoteUrlTest {

    private static final int WIRE_MOCK_PORT = 9999;
    private static final String EXPECTED_ACCEPTS_HEADER = "application/json, application/yaml, */*";
    private static final String LOCALHOST = "localhost";
    private WireMockServer wireMockServer;


    @AfterMethod
    public void tearDown() throws Exception {
        wireMockServer.stop();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
        wireMockServer.start();
        WireMock.configureFor(WIRE_MOCK_PORT);
    }

    @Test
    public void testCleanUrl() {
        String cleaned = RemoteUrl.cleanUrl("http://foo/bar/com/{v2}/fun");
        assertEquals(cleaned, "http://foo/bar/com/%7Bv2%7D/fun");

        cleaned = RemoteUrl.cleanUrl("http://westus.dev.cognitive.microsoft.com/docs/services/563879b61984550e40cbbe8d/export?DocumentFormat=Swagger&ApiName=Face API - V1.0");
        assertEquals(cleaned, "http://westus.dev.cognitive.microsoft.com/docs/services/563879b61984550e40cbbe8d/export?DocumentFormat=Swagger&ApiName=Face%20API%20-%20V1.0");
    }

    @Test
    public void testReadARemoteUrl() throws Exception {
        final String expectedBody = setupStub();
        final String actualBody = RemoteUrl.urlToString(getUrl(), null);
        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo("/v2/pet/1"))
            .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER)));
    }

    @Test
    public void testAuthorizationHeader() throws Exception {

        final String expectedBody = setupStub();

        final String headerName = "Authorization";
        final String headerValue = "foobar";
        final AuthorizationValue authorizationValue = new AuthorizationValue(headerName, headerValue, "header");
        final String actualBody = RemoteUrl.urlToString(getUrl(), Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo("/v2/pet/1"))
                        .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                        .withHeader(headerName, equalTo(headerValue))
        );
    }

    @Test
    public void testAuthorizationHeaderWithMatchingUrl() throws Exception {

        final String expectedBody = setupStub();

        final String headerName = "Authorization";
        final String headerValue = "foobar";
        String url = getUrl();
        final AuthorizationValue authorizationValue = new AuthorizationValue(headerName, headerValue, "header",
            Collections.singletonList(new URL(url)));
        final String actualBody = RemoteUrl.urlToString(url, Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo("/v2/pet/1"))
                        .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                        .withHeader(headerName, equalTo(headerValue))
        );
    }

    @Test
    public void testAuthorizationHeaderWithNonMatchingUrl() throws Exception {

        final String expectedBody = setupStub();

        final String headerValue = "foobar";
        String url = getUrl();
        String authorization = "Authorization";
        final AuthorizationValue authorizationValue = new AuthorizationValue(authorization,
            headerValue, "header", Collections.singletonList(new URL("http://foo.com")));
        final String actualBody = RemoteUrl.urlToString(url, Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        List<LoggedRequest> requests = WireMock.findAll(getRequestedFor(urlEqualTo("/v2/pet/1")));
        assertEquals(1, requests.size());
        assertFalse(requests.get(0).containsHeader(authorization));
    }

    private String getUrl() {
        return String.format("http://%s:%d/v2/pet/1", LOCALHOST, WIRE_MOCK_PORT);
    }

    private String setupStub() {
        final String expectedBody = "a really good body";
        stubFor(get(urlEqualTo("/v2/pet/1"))
                .willReturn(aResponse()
                    .withBody(expectedBody)
                    .withHeader("Content-Type", "application/json")
                ));
        return expectedBody;
    }
}
