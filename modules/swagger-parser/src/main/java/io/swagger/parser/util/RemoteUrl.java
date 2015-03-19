package io.swagger.parser.util;

import com.wordnik.swagger.models.auth.AuthorizationValue;

import java.net.*;
import java.io.*;
import java.util.*;

public class RemoteUrl {
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
        for(AuthorizationValue auth: auths) {
          if("header".equals(auth.getType())) {
            conn.setRequestProperty(auth.getKeyName(), auth.getValue());
          }
        }
      }
      else {
        conn = new URL(url).openConnection();
      }

      StringBuilder sb = new StringBuilder();
   
      String line;
      is = conn.getInputStream();
      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      return sb.toString();
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
