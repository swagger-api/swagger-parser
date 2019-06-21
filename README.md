**NOTE:** If you're looking for `swagger-parser` 2.X and OpenApi 3.0, please refer to [master branch](https://github.com/swagger-api/swagger-parser)

# Swagger Parser

[![Build Status](https://img.shields.io/jenkins/s/https/jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v1.svg)](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v1)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.swagger/swagger-parser/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/io.swagger/swagger-parser)
[![PR Stats](https://img.shields.io/github/issues-pr/swagger-api/swagger-parser.svg)](https://github.com/swagger-api/swagger-parser/pulls)
[![Issue Stats](https://img.shields.io/github/issues/swagger-api/swagger-parser.svg)](https://github.com/swagger-api/swagger-parser/issues)

[![Build Status](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v1/badge/icon?subject=jenkins%20build)](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v1/)

## Overview 
This is the swagger parser project, which reads OpenAPI Specifications into current Java POJOs.  It also provides a simple framework to add additional converters from different formats into the Swagger objects, making the entire toolchain available.

## What's Swagger?

The goal of Swagger™ is to define a standard, language-agnostic interface to REST APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined via Swagger, a consumer can understand and interact with the remote service with a minimal amount of implementation logic. Similar to what interfaces have done for lower-level programming, Swagger removes the guesswork in calling the service.


Check out [Swagger-Spec](https://github.com/OAI/OpenAPI-Specification) for additional information about the Swagger project, including additional libraries with support for other languages and more. 


### Usage
Using the swagger-parser is simple.  Once included in your project, you can read a OpenAPI Specification from any location:

```java
import io.swagger.parser.SwaggerParser;
import io.swagger.models.Swagger;

// ... your code

  // read a swagger description from the petstore
  Swagger swagger = new SwaggerParser().read("http://petstore.swagger.io/v2/swagger.json");
```

You can read from a file location as well:
```java
  Swagger swagger = new SwaggerParser().read("./path/to/swagger.json");

```

And with the swagger-compat-spec-parser module, you can read older formats, and convert them into swagger 2.0:
```java
  Swagger swagger = new SwaggerCompatConverter().read("http://petstore.swagger.io/api/api-docs");
```

If your swagger resource is protected, you can pass headers in the request:
```java
import io.swagger.models.auth.AuthorizationValue;

// ... your code

  // build a authorization value
  AuthorizationValue mySpecialHeader = new AuthorizationValue()
    .keyName("x-special-access")  //  the name of the authorization to pass
    .value("i-am-special")        //  the value of the authorization
    .type("header");              //  the location, as either `header` or `query`

  // or in a single constructor
  AuthorizationValue apiKey = new AuthorizationValue("api_key", "special-key", "header");
  Swagger swagger = new SwaggerParser().read(
    "http://petstore.swagger.io/v2/swagger.json",
    Arrays.asList(mySpecialHeader, apiKey)
  );
```

And, if your intent is to read in a Swagger 1.2 spec so that you can then output a Swagger 2 spec to string/file
then you are in luck - you have the spec in pojo form, now pass it to pretty() and you're good to go.
```
    String swaggerString = Json.pretty(swagger);
```

### Dealing with self-signed SSL certificates
If you're dealing with self-signed SSL certificates, or those signed by GoDaddy, you'll need to disable SSL Trust 
Manager.  That's done by setting a system environment variable as such:

```
export TRUST_ALL=true
```

And then the swagger-parser will _ignore_ invalid certificates.  Of course this is generally a bad idea, but if you're 
working inside a firewall or really know what you're doing, well, there's your rope.

### Dealing with Let's Encrypt
Depending on the version of Java that you use, certificates signed by the [Let's Encrypt](https://letsencrypt.org) certificate authority _may not work_
by default.  If you are using any version of Java prior to 1.8u101, you most likely _must_ install an additional CA in your
JVM.  Also note that 1.8u101 may _not_ be sufficient on it's own.  Some users have reported that certain operating systems are 
not accepting Let's Encrypt signed certificates.

Your options include:

* Accepting all certificates per above
* Installing the certificate manually in your JVM using the keystore using the `keytool` command
* Configuring the JVM on startup to load your certificate

But... this is all standard SSL configuration stuff and is well documented across the web.

### Prerequisites
You need the following installed and available in your $PATH:

* [Java 1.7](http://java.oracle.com)
* [Apache maven 3.0.3 or greater](http://maven.apache.org/)

After cloning the project, you can build it from source with this command:

```
mvn package
```

### Extensions
This project has a core artifact--`swagger-parser`, which uses Java Service Provider Inteface (SPI) so additional extensions can be added.  To read Swagger 1.0, 1.1, and 1.2 specifications, a module is included called `swagger-compat-spec-parser`.  This reads those older versions of the spec and produces 2.0 objects.

To build your own extension, you simply need to create a `src/main/resources/META-INF/services/io.swagger.parser.SwaggerParserExtension` file with the full classname of your implementation.  Your class must also implement the `io.swagger.parser.SwaggerParserExtension` interface.  Then, including your library with the `swagger-parser` module will cause it to be triggered automatically.

### Adding to your project
You can include this library from Sonatype OSS for SNAPSHOTS, or Maven central for releases.  In your dependencies:

```xml
<dependency>
  <groupId>io.swagger</groupId>
  <artifactId>swagger-parser</artifactId>
  <version>1.0.44</version>
</dependency>

```

To add swagger parsing support for older versions of swagger, add the `compat` module.  Since it depends on `swagger-parser`, you don't need to include both:
```xml
<dependency>
  <groupId>io.swagger</groupId>
  <artifactId>swagger-compat-spec-parser</artifactId>
  <version>1.0.44</version>
</dependency>

```

## Security contact

Please disclose any security-related issues or vulnerabilities by emailing [security@swagger.io](mailto:security@swagger.io), instead of using the public issue tracker.

License
-------

Copyright 2017 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

---
<img src="http://swagger.io/wp-content/uploads/2016/02/logo.jpg"/>
