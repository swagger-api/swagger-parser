package io.swagger.v3.parser.util;

import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class RemoteUrl {

    static final Logger LOGGER = LoggerFactory.getLogger(RemoteUrl.class);


    private static final String TRUST_ALL = String.format("%s.trustAll", RemoteUrl.class.getName());
    private static final ConnectionConfigurator CONNECTION_CONFIGURATOR = createConnectionConfigurator();
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String ACCEPT_HEADER_VALUE = "application/json, application/yaml, */*";
    private static final String USER_AGENT_HEADER_VALUE = "Apache-HttpClient/Swagger";
    static final int CONNECTION_TIMEOUT = 30000;
    static final int READ_TIMEOUT = 60000;
    private static final int MAX_REDIRECTS = 5;

    static ConnectionConfigurator createConnectionConfigurator() {
        if (Boolean.parseBoolean(System.getProperty(TRUST_ALL))) {
            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};

                // Install the all-trusting trust manager
                final SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                final SSLSocketFactory sf = sc.getSocketFactory();

                // Create all-trusting host name verifier
                final HostnameVerifier trustAllNames = (hostname, session) -> true;

                return connection -> {
                    if (connection instanceof HttpsURLConnection) {
                        final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                        httpsConnection.setSSLSocketFactory(sf);
                        httpsConnection.setHostnameVerifier(trustAllNames);
                        httpsConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                        httpsConnection.setReadTimeout(READ_TIMEOUT);
                        httpsConnection.setInstanceFollowRedirects(false);
                    }
                };
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                LOGGER.error("Not Supported", e);
            }
        }
        return new ConnectionConfigurator() {

            @Override
            public void process(URLConnection connection) {
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
            }
        };
    }

    public static String cleanUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replace("{", "%7B")
                .replace("}", "%7D")
                .replace(" ", "%20");
    }

    public static String urlToString(String url, List<AuthorizationValue> auths) throws Exception {
        return urlToString(url, auths, null);
    }

    public static String urlToString(String url, List<AuthorizationValue> auths, PermittedUrlsChecker permittedUrlsChecker) throws Exception {

        try {
            URLConnection conn;
            int redirectCount = 0;

            while (redirectCount <= MAX_REDIRECTS) {
                //redirect count > 0 means we are checking only redirections
                if (redirectCount > 0 && permittedUrlsChecker != null) {
                    permittedUrlsChecker.verify(url);
                }
                final URL inUrl = new URL(cleanUrl(url));
                final List<AuthorizationValue> query = new ArrayList<>();
                final List<AuthorizationValue> header = new ArrayList<>();
                if (auths != null && !auths.isEmpty()) {
                    for (AuthorizationValue auth : auths) {
                        if (auth.getUrlMatcher() != null && auth.getUrlMatcher().test(inUrl)) {
                            if ("query".equals(auth.getType())) {
                                appendValue(inUrl, auth, query);
                            } else if ("header".equals(auth.getType())) {
                                appendValue(inUrl, auth, header);
                            }
                        }
                    }
                }
                conn = prepareConnection(query, inUrl);
                CONNECTION_CONFIGURATOR.process(conn);
                setRequestHeaders(header, conn);

                conn.connect();
                HttpURLConnection httpConn = (HttpURLConnection) conn;

                if (isRedirect(httpConn)) {
                    url = conn.getHeaderField("Location");
                    redirectCount++;
                    if (url == null) {
                        throw new IOException("Redirect response missing 'Location' header");
                    }
                } else {
                    return readResponse(conn);
                }
            }
            throw new IOException("Too many redirects (> " + MAX_REDIRECTS + ")");
        } catch (javax.net.ssl.SSLProtocolException e) {
            LOGGER.warn("there is a problem with the target SSL certificate");
            LOGGER.warn("**** you may want to run with -Djsse.enableSNIExtension=false\n\n");
            LOGGER.error("unable to read {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.error("unable to read {}", e.getMessage());
            throw e;
        }
    }

    private static String readResponse(URLConnection conn) throws IOException {

        try (InputStream in = conn.getInputStream();
             BufferedReader input = new BufferedReader(new InputStreamReader(in, UTF_8))) {

            StringBuilder contents = new StringBuilder();
            int ch;
            while ((ch = input.read()) != -1) {
                char c = (char) ch;
                if (!Character.isISOControl(c) || c == '\n') {
                    contents.append(c);
                }
            }
            return contents.toString();
        }
    }

    private static void setRequestHeaders(List<AuthorizationValue> header, URLConnection conn) {
        for (AuthorizationValue item : header) {
            conn.setRequestProperty(item.getKeyName(), item.getValue());
        }

        conn.setRequestProperty("Accept", ACCEPT_HEADER_VALUE);
        conn.setRequestProperty("User-Agent", USER_AGENT_HEADER_VALUE);
    }

    private static URLConnection prepareConnection(List<AuthorizationValue> query, URL inUrl) throws URISyntaxException, IOException {
        URLConnection conn;
        if (!query.isEmpty()) {
            final URI inUri = inUrl.toURI();
            final StringBuilder newQuery = new StringBuilder(inUri.getQuery() == null ? "" : inUri.getQuery());
            for (AuthorizationValue item : query) {
                if (newQuery.length() > 0) {
                    newQuery.append("&");
                }
                newQuery.append(URLEncoder.encode(item.getKeyName(), UTF_8.name())).append("=")
                        .append(URLEncoder.encode(item.getValue(), UTF_8.name()));
            }
            conn = new URI(inUri.getScheme(), inUri.getAuthority(), inUri.getPath(), newQuery.toString(),
                    inUri.getFragment()).toURL().openConnection();
        } else {
            conn = inUrl.openConnection();
        }
        return conn;
    }

    private static boolean isRedirect(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        return code == 301 || code == 302 || code == 307 || code == 308;
    }

    private static void appendValue(URL url, AuthorizationValue value, Collection<AuthorizationValue> to) {
        if (value instanceof ManagedValue) {
            if (!((ManagedValue) value).process(url)) {
                return;
            }
        }
        to.add(value);
    }

    interface ConnectionConfigurator {

        void process(URLConnection connection);
    }
}
