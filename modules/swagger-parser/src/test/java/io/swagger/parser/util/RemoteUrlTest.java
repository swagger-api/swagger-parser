package io.swagger.parser.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.swagger.models.auth.AuthorizationValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
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
    public void testReadARemoteUrl() throws Exception {
    	final String path = "/v2/pet/1";
        final String expectedBody = setupStub(path);

        final String actualBody = RemoteUrl.urlToString(getUrl(path), null);
        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo(path))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER)));

    }

    private String getUrl(String path) {
        return "http://localhost:" + WIRE_MOCK_PORT + path;
    }

	@Test
	public void testParameterizedPathDoesNotThrow() throws Exception {
		final String path= "/v2/pet/1/{param}/2";
		final String encodedPath =  "/v2/pet/1/%7Bparam%7D/2";
		final String expectedBody = setupStub( encodedPath );
		
		final String actualBody = RemoteUrl.urlToString( getUrl(path), null);
		assertEquals( actualBody, expectedBody );

		verify(getRequestedFor(urlEqualTo(encodedPath))
                .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER)));
	}

    @Test
    public void testAuthorizationHeader() throws Exception {
        final String path= "/v2/pet/1";
        final String expectedBody = setupStub(path);

        final String headerName = "Authorization";
        final String headerValue = "foobar";
        final AuthorizationValue authorizationValue = new AuthorizationValue(headerName, headerValue, "header");
        final String actualBody = RemoteUrl.urlToString(getUrl(path), Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlEqualTo(path))
                        .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                        .withHeader(headerName, equalTo(headerValue))
        );
    }

    @Test
    public void testAuthorizationQueryParam() throws Exception {
        final String queryParamName = "Authorization";
        final String queryParamValue = "foobar";
        final String expectedBody = "a really good body";
        final String path = "/v2/pet/1";

        stubFor(get(urlPathEqualTo(path))
                        .withQueryParam(queryParamName, equalTo(queryParamValue))
                        .willReturn(aResponse()
                                .withBody(expectedBody)
                                .withHeader("Content-Type", "application/json"))

        );

        final AuthorizationValue authorizationValue = new AuthorizationValue(queryParamName, queryParamValue, "query");
        final String actualBody = RemoteUrl.urlToString(getUrl(path), Arrays.asList(authorizationValue));

        assertEquals(actualBody, expectedBody);

        verify(getRequestedFor(urlPathEqualTo(path))
                        .withHeader("Accept", equalTo(EXPECTED_ACCEPTS_HEADER))
                        .withQueryParam(queryParamName, equalTo(queryParamValue))
        );
    }

    private String setupStub( String path) {
        final String expectedBody = "a really good body";
        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                                .withBody(expectedBody)
                                .withHeader("Content-Type", "application/json")
                ));
        return expectedBody;
    }
}
