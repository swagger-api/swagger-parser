# Swagger Parser

[![Build Status](https://travis-ci.org/swagger-api/swagger-parser.png)](https://travis-ci.org/swagger-api/swagger-parser)

## Overview
This is the swagger parser project, which reads swagger specifications into current Java POJOs.  It also provides a simple framework to add additional converters from different formats into the Swagger objects, making the entire toolchain available.

## What's Swagger?

The goal of Swagger™ is to define a standard, language-agnostic interface to REST APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined via Swagger, a consumer can understand and interact with the remote service with a minimal amount of implementation logic. Similar to what interfaces have done for lower-level programming, Swagger removes the guesswork in calling the service.


Check out [Swagger-Spec](https://github.com/swagger-api/swagger-spec) for additional information about the Swagger project, including additional libraries with support for other languages and more. 


### Usage
Using the swagger-parser is simple.  Once included in your project, you can read a swagger specification from any location:

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
  Swagger swagger = new SwaggerParser().read("http://petstore.swagger.io/api/api-docs");
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

### Prerequisites
You need the following installed and available in your $PATH:

* [Java 1.7](http://java.oracle.com)

Note!  Some folks have had issues with OOM errors with java version "1.6.0_51".  It's strongly suggested that you upgrade to 1.7!

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
  <version>1.0.0</version>
</dependency>

```

To add swagger parsing support for older versions of swagger, add the `compat` module.  Since it depends on `swagger-parser`, you don't need to include both:
```xml
<dependency>
  <groupId>io.swagger</groupId>
  <artifactId>swagger-compat-spec-parser</artifactId>
  <version>1.0.0</version>
</dependency>

```


License
-------

Copyright 2015 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
