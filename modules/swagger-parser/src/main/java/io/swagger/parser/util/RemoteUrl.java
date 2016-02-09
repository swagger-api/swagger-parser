package io.swagger.parser.util;

import io.swagger.models.auth.AuthorizationValue;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteUrl {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteUrl.class);
    private static final String ACCEPT_HEADER_VALUE = "application/json, application/yaml, */*";
    private static final String USER_AGENT_HEADER_VALUE = "Apache-HttpClient/Swagger";

    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Cryptographic algorithm is not available", e);
        } catch (KeyManagementException e) {
            LOGGER.error("Exception dealing with key management", e);
        }
    }

    public static String urlToString(String url, List<AuthorizationValue> auths) throws Exception {
        InputStream is = null;
        URLConnection conn = null;
        BufferedReader br = null;

        try {
            if (auths != null) {
                StringBuilder queryString = new StringBuilder();
                // build a new url if needed
                for (AuthorizationValue auth : auths) {
                    if ("query".equals(auth.getType())) {
                        if (queryString.toString().length() == 0) {
                            queryString.append("?");
                        } else {
                            queryString.append("&");
                        }
                        queryString.append(URLEncoder.encode(auth.getKeyName(), "UTF-8"))
                                .append("=")
                                .append(URLEncoder.encode(auth.getValue(), "UTF-8"));
                    }
                }
                if (queryString.toString().length() != 0) {
                    url = url + queryString.toString();
                }
                conn = new URL(url).openConnection();

                for (AuthorizationValue auth : auths) {
                    if ("header".equals(auth.getType())) {
                        conn.setRequestProperty(auth.getKeyName(), auth.getValue());
                    }
                }
            } else {
                conn = new URL(url).openConnection();
            }

            conn.setRequestProperty("Accept", ACCEPT_HEADER_VALUE);
            conn.setRequestProperty("User-Agent", USER_AGENT_HEADER_VALUE);
            conn.connect();
            InputStream in = conn.getInputStream();

            StringBuilder contents = new StringBuilder();

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(in, "UTF-8"));

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
            System.out.println("there is a problem with the target SSL certificate");
            System.out.println("**** you may want to run with -Djsse.enableSNIExtension=false\n\n");
            LOGGER.error("Error in the operation of the SSL protocol", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception while execution", e);
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

    static {
        disableSslVerification();
    }
}
