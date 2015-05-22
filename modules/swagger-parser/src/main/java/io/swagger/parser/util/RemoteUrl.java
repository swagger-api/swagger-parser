package io.swagger.parser.util;

import com.wordnik.swagger.models.auth.AuthorizationValue;

import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class RemoteUrl {
  static {
    disableSslVerification();
  }

  private static void disableSslVerification() {
    try {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }
          public void checkClientTrusted(X509Certificate[] certs, String authType) { }
          public void checkServerTrusted(X509Certificate[] certs, String authType) { }
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
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
  }

  public static String urlToString(String url, List<AuthorizationValue> auths) throws Exception {
    InputStream is = null;
    URLConnection conn = null;
    BufferedReader br = null;

    try{
      if(auths != null) {
        StringBuilder queryString = new StringBuilder();
        // build a new url if needed
        for(AuthorizationValue auth: auths) {
          if("query".equals(auth.getType())) {
            if(queryString.toString().length() == 0)
              queryString.append("?");
            else
              queryString.append("&");
            queryString.append(URLEncoder.encode(auth.getKeyName(), "UTF-8"))
              .append("=")
              .append(URLEncoder.encode(auth.getValue(), "UTF-8"));
          }
        }
        if(queryString.toString().length() != 0)
          url = url + queryString.toString();
        conn = new URL(url).openConnection();
        conn.setRequestProperty("Accept", "application/json, */*");

        for(AuthorizationValue auth: auths) {
          if("header".equals(auth.getType())) {
            conn.setRequestProperty(auth.getKeyName(), auth.getValue());
          }
        }
      }
      else {
        conn = new URL(url).openConnection();
        conn.setRequestProperty("Accept", "application/json, application/yaml, */*");
      }

      conn.connect();
      InputStream in = conn.getInputStream();

      String inputLine;
      StringBuilder contents = new StringBuilder();

      for(int i = 0; i != -1; i = in.read()) {
        char c = (char)i;
        if(!Character.isISOControl(c))
          contents.append((char)i);
      }

      in.close();

      return contents.toString();
    }
    catch (javax.net.ssl.SSLProtocolException e){
      System.out.println("there is a problem with the target SSL certificate");
      System.out.println("**** you may want to run with -Djsse.enableSNIExtension=false\n\n");
      e.printStackTrace();
      throw e;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    finally {
      if(is != null) 
        is.close();
      if(br != null)
        br.close();
    }
  }
}
