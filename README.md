# Swagger Parser <img src="https://raw.githubusercontent.com/swagger-api/swagger.io/wordpress/images/assets/SW-logo-clr.png" height="50" align="right">

**NOTE:** If you're looking for `swagger-parser` 1.X and OpenAPI 2.0, please refer to [v1 branch](https://github.com/swagger-api/swagger-parser/tree/v1)

[![Build Status](https://img.shields.io/jenkins/build.svg?jobUrl=https://jenkins.swagger.io/job/oss-swagger-parser-v2/)](https://jenkins.swagger.io/job/oss-swagger-parser-v2/)

[![Build Status](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v2/badge/icon?subject=jenkins%20build%20-%20java%208)](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v2/)

[![Build Status](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v2-java9/badge/icon?subject=jenkins%20build%20-%20java%209)](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v2-java9/)

## Overview 
This is the Swagger Parser project, which reads OpenAPI definitions into current Java POJOs.  It also provides a simple framework to add additional converters from different formats into the Swagger objects, making the entire toolchain available.


### Usage
Using the Swagger Parser is simple.  Once included in your project, you can read a OpenAPI Specification from any location:

```java
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;

// ... your code

  // read a swagger description from the petstore
  
  
  OpenAPI openAPI = new OpenAPIV3Parser().read("https://petstore3.swagger.io/api/v3/openapi.json");

```

You can read from a file location as well:
```java
  OpenAPI openAPI = new OpenAPIV3Parser().read("./path/to/openapi.yaml");

```


If your OpenAPI definition is protected, you can pass headers in the request:
```java
import io.swagger.v3.parser.core.models.AuthorizationValue;

// ... your code

  // build a authorization value
  AuthorizationValue mySpecialHeader = new AuthorizationValue()
    .keyName("x-special-access")  //  the name of the authorization to pass
    .value("i-am-special")        //  the value of the authorization
    .type("header");              //  the location, as either `header` or `query`

  // or in a single constructor
  AuthorizationValue apiKey = new AuthorizationValue("api_key", "special-key", "header");
  OpenAPI openAPI = new OpenAPIV3Parser().readWithInfo(
    "https://petstore3.swagger.io/api/v3/openapi.json",
    Arrays.asList(mySpecialHeader, apiKey)
  );
```

### Dealing with self-signed SSL certificates
If you're dealing with self-signed SSL certificates, or those signed by GoDaddy, you'll need to disable SSL Trust 
Manager.  That's done by setting a system environment variable as such:

```
export TRUST_ALL=true
```

And then the Swagger Parser will _ignore_ invalid certificates.  Of course this is generally a bad idea, but if you're 
working inside a firewall or really know what you're doing, well, there's your rope.

### Dealing with Let's Encrypt
Depending on the version of Java that you use, certificates signed by the [Let's Encrypt](https://letsencrypt.org) certificate authority _may not work_ by default.  If you are using any version of Java prior to 1.8u101, you most likely _must_ install an additional CA in your
JVM.  Also note that 1.8u101 may _not_ be sufficient on it's own.  Some users have reported that certain operating systems are 
not accepting Let's Encrypt signed certificates.

Your options include:

* Accepting all certificates per above
* Installing the certificate manually in your JVM using the keystore using the `keytool` command
* Configuring the JVM on startup to load your certificate

But... this is all standard SSL configuration stuff and is well documented across the web.

### Prerequisites
You need the following installed and available in your $PATH:

* [Java 1.8](http://java.oracle.com)
* [Apache maven 3.x](http://maven.apache.org/)

After cloning the project, you can build it from source with this command:

```
mvn package
```

### Extensions
This project has a core artifact--`swagger-parser`, which uses Java Service Provider Inteface (SPI) so additional extensions can be added. 

To build your own extension, you simply need to create a `src/main/resources/META-INF/services/io.swagger.parser.SwaggerParserExtension` file with the full classname of your implementation.  Your class must also implement the `io.swagger.parser.SwaggerParserExtension` interface.  Then, including your library with the `swagger-parser` module will cause it to be triggered automatically.

### Adding to your project
You can include this library from Sonatype OSS for SNAPSHOTS, or Maven central for releases.  In your dependencies:

```xml
<dependency>
  <groupId>io.swagger.parser.v3</groupId>
  <artifactId>swagger-parser</artifactId>
  <version>2.0.22</version>
</dependency>
```

## Security contact

Please disclose any security-related issues or vulnerabilities by emailing [security@swagger.io](mailto:security@swagger.io), instead of using the public issue tracker.
