# Swagger Parser Safe URL Resolver

The `swagger-parser-safe-url-resolver` is a library used for verifying that the hostname of URLs does not resolve to a private/restricted IPv4/IPv6 address range.     
This library can be used in services that deal with user-submitted URLs that get fetched (like in swagger-parser when resolving external URL $refs) to protect against Server-Side Request Forgery and DNS rebinding attacks.

## How does it work?  
The main class of the package is the `PermittedUrlsChecker` which has one method: `verify(String url)`.  
This method takes in a string URL and performs the following steps:

1. Gets the hostname portion from the URL  
2. Resolves the hostname to an IP address
3. Checks if that IP address is in a private/restricted IP address range (and throws an exception if it is)
4. Returns a `ResolvedUrl` object which contains   
  4.1. `String url` where the original URL has the hostname replaced with the IP address  
  4.2. A `String hostHeader` which contains the hostname from the original URL to be added as a host header  

This behavior can also be customized with the allowlist and denylist in the constructor, whereby:

- An entry in the allowlist will allow the URL to pass even if it resolves to a private/restricted IP address
- An entry in the denylist will throw an exception even when the URL resolves to a public IP address

## Installation
Add the following to you `pom.xml` file under `dependencies`
```xml
<dependency>
    <groupId>io.swagger.parser.v3</groupId>
    <artifactId>swagger-parser-safe-url-resolver</artifactId>
    // version of swagger-parser being used
    <version>2.1.14</version> 
</dependency>
```

## Example usage

```java
import io.swagger.v3.parser.urlresolver.PermittedUrlsChecker;
import io.swagger.v3.parser.urlresolver.exceptions.HostDeniedException;
import io.swagger.v3.parser.urlresolver.models.ResolvedUrl;

import java.util.List;

public class Main {
    public static void main() {
        List<String> allowlist = List.of("mysite.local");
        List<String> denylist = List.of("*.example.com:443");
        var checker = new PermittedUrlsChecker(allowlist, denylist);

        try {
            // Will throw a HostDeniedException as `localhost`  
            // resolves to local IP and is not in allowlist
            checker.verify("http://localhost/example");

            // Will return a ResolvedUrl if `github.com` 
            // resolves to a public IP
            checker.verify("https://github.com/swagger-api/swagger-parser");

            // Will throw a HostDeniedException as `*.example.com` is 
            // explicitly deny listed, even if it resolves to public IP
            checker.verify("https://subdomain.example.com/somepage");

            // Will return a `ResolvedUrl` as `mysite.local` 
            // is explicitly allowlisted
            ResolvedUrl resolvedUrl = checker.verify("http://mysite.local/example");
            System.out.println(resolvedUrl.getUrl()); // "http://127.0.0.1/example"
            System.out.println(resolvedUrl.getHostHeader()); // "mysite.local"
        } catch (HostDeniedException e) {
            e.printStackTrace();
        }
    }
}
```