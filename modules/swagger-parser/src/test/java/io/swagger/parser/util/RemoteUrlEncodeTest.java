package io.swagger.parser.util;

import static org.junit.Assert.*;
import static org.testng.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;

import org.junit.Test;

public class RemoteUrlEncodeTest {

    static final String noparams = "http://localhost:8080/v1/api/thing";
    static final String withparams = "http://localhost:8080/v1/api/{thing}";
    static final String trailingslash = "http://localhost:8080/v1/api/thing/";
    static final String nopath = "http://localhost:8080/";
    static final String nopathnoslash = "http://localhost:8080";
    static final String noparamswargs = "http://localhost:8080/v1/api/thing?arg=0";
    static final String withparamswargs = "http://localhost:8080/v1/api/{thing}?arg";
    static final String withhashpathparam = "http://localhost:8080/v1/api/thing#foo/bar";
    static final String nopathwargs = "http://localhost:8080/";
    static final String nopathnoslashwargs = "http://localhost:8080";
    static final String malformed="http://{localhost}:8080/path/part/{param}/operation";
    static final String alreadyencoded = "http://localhost:8080/v1/api/%7Bthing%7D";

	@Test(expected = Exception.class)
	public void testRaisesMalformedURL() throws Exception {
		RemoteUrl.urlToString(malformed, null);
	}

	@Test
	public void testEncode_noparams() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(noparams), noparams);
	}

	@Test
	public void testEncode_withparams() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(withparams), "http://localhost:8080/v1/api/%7Bthing%7D");
	}

	@Test
	public void testEncode_trailingslash() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(trailingslash), trailingslash);
	}

	@Test
	public void testEncode_nopath() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(nopath), nopath);
	}

	@Test
	public void testEncode_nopathnoslash() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(nopathnoslash), nopathnoslash);
	}

	@Test
	public void testEncode_noparamswargs() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(noparamswargs), noparamswargs);
	}

	@Test
	public void testEncode_withparamswargs() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(withparamswargs), "http://localhost:8080/v1/api/%7Bthing%7D?arg");
	}

	@Test
	public void testEncode_nopathwargs() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(nopathwargs), nopathwargs);
	}

	@Test
	public void testEncode_alreadyencoded() throws Exception {
		assertEquals( RemoteUrl.encodePathParameters(alreadyencoded),alreadyencoded);
	}
	
	@Test
	public void testEncode_nopathnoslashwargs() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(nopathnoslashwargs), nopathnoslashwargs);
	}

	@Test(expected=Exception.class)
	public void testEncode_malformed() throws Exception {
		assertEquals(RemoteUrl.encodePathParameters(malformed), "http://{localhost}:8080/path/part/%7Bparam%7D/op" );
	}    

}
