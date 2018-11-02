package io.swagger.parser.util;

import io.swagger.models.auth.AuthorizationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class RemoteUrl {
    static Logger LOGGER = LoggerFactory.getLogger(RemoteUrl.class);

    private static final String TRUST_ALL = String.format("%s.trustAll", RemoteUrl.class.getName());
    private static final ConnectionConfigurator CONNECTION_CONFIGURATOR = createConnectionConfigurator();
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String ACCEPT_HEADER_VALUE = "application/json, application/yaml, */*";
    private static final String USER_AGENT_HEADER_VALUE = "Apache-HttpClient/Swagger";

    private static ConnectionConfigurator createConnectionConfigurator() {
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
                final SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                final SSLSocketFactory sf = sc.getSocketFactory();

                // Create all-trusting host name verifier
                final HostnameVerifier trustAllNames = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                return new ConnectionConfigurator() {

                    @Override
                    public void process(URLConnection connection) {
                        if (connection instanceof HttpsURLConnection) {
                            final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                            httpsConnection.setSSLSocketFactory(sf);
                            httpsConnection.setHostnameVerifier(trustAllNames);
                        }
                    }
                };
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("Not Supported", e);
            } catch (KeyManagementException e) {
                LOGGER.error("Not Supported", e);
            }
        }
        return new ConnectionConfigurator() {

            @Override
            public void process(URLConnection connection) {
                // Do nothing
            }
        };
    }

    public static String cleanUrl(String url) {
        return url.replaceAll("\\{", "%7B").
                replaceAll("\\}", "%7D").
                replaceAll(" ", "%20");
    }

    public static String urlToString(String url, List<AuthorizationValue> auths) throws Exception {
        InputStream is = null;
        BufferedReader br = null;

        try {
            URLConnection conn;
            do {
                final URL inUrl = new URL(cleanUrl(url));
                final List<AuthorizationValue> query = new ArrayList<>();
                final List<AuthorizationValue> header = new ArrayList<>();
                if (auths != null) {
                    for (AuthorizationValue auth : auths) {
                        if ("query".equals(auth.getType())) {
                            appendValue(inUrl, auth, query);
                        } else if ("header".equals(auth.getType())) {
                            appendValue(inUrl, auth, header);
                        }
                    }
                }
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
                CONNECTION_CONFIGURATOR.process(conn);
                for (AuthorizationValue item : header) {
                    conn.setRequestProperty(item.getKeyName(), item.getValue());
                }

                conn.setRequestProperty("Accept", ACCEPT_HEADER_VALUE);
                conn.setRequestProperty("User-Agent", USER_AGENT_HEADER_VALUE);

                conn.connect();
                url = ((HttpURLConnection) conn).getHeaderField("Location");
            } while (301 == ((HttpURLConnection) conn).getResponseCode());

            InputStream in = conn.getInputStream();

            StringBuilder contents = new StringBuilder();

            BufferedReader input = new BufferedReader(new InputStreamReader(in, UTF_8));

            for (int i = 0; i != -1; i = input.read()) {
                char c = (char) i;
                if (!Character.isISOControl(c)) {
                    contents.append((char) i);
                }
                if (c == '\n') {
                    contents.append('\n');
                }
            }

            in.close();

            return contents.toString();
        } catch (javax.net.ssl.SSLProtocolException e) {
            LOGGER.warn("there is a problem with the target SSL certificate");
            LOGGER.warn("**** you may want to run with -Djsse.enableSNIExtension=false\n\n");
            LOGGER.error("unable to read", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("unable to read", e);
            throw e;
        } finally {
            if (is != null) {
                is.close();
            }
            if (br != null) {
                br.close();
            }
        }
    }

    private static void appendValue(URL url, AuthorizationValue value, Collection<AuthorizationValue> to) {
        if (value instanceof ManagedValue) {
            if (!((ManagedValue) value).process(url)) {
                return;
            }
        }
        to.add(value);
    }

    private interface ConnectionConfigurator {

        void process(URLConnection connection);
    }
}
