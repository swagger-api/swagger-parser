package io.swagger.parser.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.swagger.models.auth.AuthorizationValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.testng.Assert.assertEquals;

public class RemoteUrlTest {

    private static final int WIRE_MOCK_PORT = 9999;
    private static final String EXPECTED_ACCEPTS_HEADER = "application/json, application/yaml, */*";
    private static final String LOCALHOST = "localhost";
    private static final String SOME_HOST = "somehost";
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
    public void testHostHeader() throws Exception {

        final String expectedBody = setupStub();

        final String headerName = "Authorization";
        final String headerValue = "foobar";
        final AuthorizationValue authorizationValue = new HostAuthorizationValue(LOCALHOST,
                headerName, headerValue, "header");
        final String actualBody = RemoteUrl.urlToString(getUrl(),
                Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo("/v2/pet/1"))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                .withHeader(headerName, equalTo(headerValue)));
    }

    @Test
    public void testSkippedHeader() throws Exception {

        final String expectedBody = setupStub();

        final String headerName = "Authorization";
        final String headerValue = "foobar";
        final AuthorizationValue authorizationValue = new HostAuthorizationValue(SOME_HOST,
                headerName, headerValue, "header");
        final String actualBody = RemoteUrl.urlToString(getUrl(),
                Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo("/v2/pet/1"))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER)).withoutHeader(headerName));
    }

    @Test
    public void testAuthorizationQueryParam() throws Exception {
        final String queryParamName = "Authorization";
        final String queryParamValue = "foobar";
        final String expectedBody = "a really good body";

        stubFor(get(urlPathEqualTo("/v2/pet/1"))
                        .withQueryParam(queryParamName, equalTo(queryParamValue))
                        .willReturn(aResponse()
                                .withBody(expectedBody)
                                .withHeader("Content-Type", "application/json"))

        );

        final AuthorizationValue authorizationValue = new AuthorizationValue(queryParamName, queryParamValue, "query");
        final String actualBody = RemoteUrl.urlToString(getUrl(), Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlPathEqualTo("/v2/pet/1"))
                        .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                        .withQueryParam(queryParamName, equalTo(queryParamValue))
        );
    }

    @Test
    public void testHostQueryParam() throws Exception {
        final String queryParamName = "Authorization";
        final String queryParamValue = "foobar";
        final String expectedBody = "a really good body";

        stubFor(get(urlPathEqualTo("/v2/pet/1"))
                .withQueryParam(queryParamName, equalTo(queryParamValue)).willReturn(aResponse()
                        .withBody(expectedBody).withHeader("Content-Type", "application/json"))

        );

        final AuthorizationValue authorizationValue = new HostAuthorizationValue(LOCALHOST,
                queryParamName, queryParamValue, "query");
        final String actualBody = RemoteUrl.urlToString(getUrl(),
                Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlPathEqualTo("/v2/pet/1"))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                .withQueryParam(queryParamName, equalTo(queryParamValue)));
    }

    @Test
    public void testSkippedQueryParam() throws Exception {
        final String queryParamName = "Authorization";
        final String queryParamValue = "foobar";
        final String expectedBody = "a really good body";

        stubFor(get(urlPathEqualTo("/v2/pet/1")).willReturn(
                aResponse().withBody(expectedBody).withHeader("Content-Type", "application/json")));

        final AuthorizationValue authorizationValue = new HostAuthorizationValue(SOME_HOST,
                queryParamName, queryParamValue, "query");
        final String actualBody = RemoteUrl.urlToString(getUrl(),
                Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlPathEqualTo("/v2/pet/1"))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                .withoutHeader(queryParamName));
    }

    @Test
    public void testAppendQueryParam() throws Exception {
        final String firstParamName = "first";
        final String firstParamValue = "first-value";
        final String queryParamName = "Authorization";
        final String queryParamValue = "foobar";
        final String expectedBody = "a really good body";

        stubFor(get(urlPathEqualTo("/v2/pet/1"))
                .withQueryParam(firstParamName, equalTo(firstParamValue))
                .withQueryParam(queryParamName, equalTo(queryParamValue)).willReturn(aResponse()
                        .withBody(expectedBody).withHeader("Content-Type", "application/json")));

        final AuthorizationValue authorizationValue = new HostAuthorizationValue(LOCALHOST,
                queryParamName, queryParamValue, "query");
        final String actualBody = RemoteUrl.urlToString(
                String.format("%s?%s=%s", getUrl(), firstParamName, firstParamValue),
                Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlPathEqualTo("/v2/pet/1"))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                .withQueryParam(firstParamName, equalTo(firstParamValue))
                .withQueryParam(queryParamName, equalTo(queryParamValue)));
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
