# Swagger Parser <img src="https://raw.githubusercontent.com/swagger-api/swagger.io/wordpress/images/assets/SW-logo-clr.png" height="50" align="right">

**NOTE:** If you're looking for `swagger-parser` 1.X and OpenAPI 2.0, please refer to [v1 branch](https://github.com/swagger-api/swagger-parser/tree/v1)

[![Build Status](https://img.shields.io/jenkins/s/https/jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v2.svg)](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swagger-parser-v2)

# Table of contents

  - [Overview](#overview)
  - [Table of Contents](#table-of-contents)
  - [Usage](#usage)
    - [Options](#options)
      - [Resolve](#1-resolve)
      - [ResolveFully](#2-resolvefully)
      - [Flatten](#3-flatten)
      - [ResolveCombinators](#4-resolvecombinators)
    - [Authentication](#authentication)
    - [Adding to your project](#adding-to-your-project)
    - [Prerequisites](#prerequisites)
    - [Extensions](#extensions)
  - [License](#license)
   
## Overview 
This is the Swagger Parser project, which reads OpenAPI definitions into current Java POJOs.  It also provides a simple framework to add additional converters from different formats into the Swagger objects, making the entire toolchain available.


### Usage
Using the Swagger Parser is simple.  Once included in your project, you can read a OpenAPI Specification from any location:

```java
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;

// ... your code

  // read a swagger description from the petstore
  
  
  OpenAPI openAPI = new OpenAPIV3Parser().read("http://petstore.swagger.io/v3/openapi.json");

```

You can read from a file location as well:
```java
  OpenAPI openAPI = new OpenAPIV3Parser().read("./path/to/openapi.yaml");

```
### Options
Parser uses options as a way to personalize the behavior while parsing:

#### 1. Resolve:
- When remote or relative references are found, parser will find the reference in the (remote/relative) location and add the     object to the main openapi object under the components part of the spec and changing the previous remote/local reference for a local one e.g. : `#/components/schemas/NameOfRemoteSchema`. This applies to schemas, parameters, responses, pretty much everything containing a ref.

#### 2. ResolveFully:
- After references are resolved (brought back from the remote location and added to the internal components location), then maybe it is needed that the resolved objects are no longer referenced, but put at the same place they were referenced initially, meaning that in the spec there will not be anymore referenced objects, all will be put inline in the place they are being called, replacing the reference it self. This will make the spec longer.

```
 @Test
    public void referringSpecWithoutComponentsTag() throws Exception {
        ParseOptions resolve = new ParseOptions();
        resolve.setResolveFully(true);
        final OpenAPI openAPI = new OpenAPIV3Parser().read("a.yaml", null, resolve);
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Assert.assertEquals("Example value", schemas.get("CustomerType").getExample());
    }
```
    
Spec before resolving:

`a.yaml` 
```
openapi: 3.0.1
paths:
  "/newPerson":
    post:
      summary: Create new person
      description: Create new person
      responses:
        '200':
          description: ok
          content:
            "*/*":
              schema:
                "$ref": "./ref-without-component/b.yaml#/components/schemas/CustomerType"
```
`b.yaml`
```
openapi: 3.0.1
components:
  schemas:
    CustomerType:
      type: string
      example: Example value
```

Spec after Resolving, with option `resolveFully(true)`

`a.yaml`
```
openapi: 3.0.1
servers:
- url: /
paths:
  /newPerson:
    post:
      summary: Create new person
      description: Create new person
      responses:
        200:
          description: ok
          content:
            '*/*':
              schema:
                type: string
                example: Example value
components:
  schemas:
    CustomerType:
      type: string
      example: Example value
```

#### 3. Flatten: 
(opposite of resolveFully)
- This option can be used in case you need to make your object lighter, so you ask parser to flatten all inline references (this only applies to schemas) this will mean that all the schemas will be a local reference to `#/components/schemas/...`

```
  @Test
    public void testIssue705() throws Exception {
        ParseOptions options = new ParseOptions();
        options.setFlatten(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read("flatten.yaml",null, options);
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents().getSchemas().get("inline_response_200").getType());
    }
```

Spec before flattening:

`flatten.yaml`

```
openapi: 3.0.0
info:
  version: 1.0.0
  title: Swagger Petstore
  license:
    name: MIT
paths:
  /pets:
    get:
      summary: List all pets
      operationId: listPets
      responses:
        '200':
          description: An paged array of pets
          headers:
            x-next:
              description: A link to the next page of responses
              schema:
                type: string
          content:
            application/json:
              schema:
                 type: object
                 properties:
                    id:
                      type: integer
                      format: int64
                    name:
                      type: string
                    tag:
                      type: string
```

Spec after option flatten(true):

```
openapi: 3.0.0
info:
  title: Swagger Petstore
  license:
    name: MIT
  version: 1.0.0
servers:
- url: /
paths:
  /pets:
    get:
      tags:
      - pets
      summary: List all pets
      responses:
        200:
          description: An paged array of pets
          headers:
            x-next:
              description: A link to the next page of responses
              style: simple
              explode: false
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/inline_response_200'
components:
  schemas:
    inline_response_200:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        tag:
          type: string
```

#### 4. ResolveCombinators: 
- Some users don't want to aggregate anyOf/allOf/oneOf schemas but simply want all refs solved.
In case user doesn't want to add the schemas to the properties of the resolved composedSchema, this option generates a new list of subschemas solved and does not aggregated them to the resulting object as properties.
This option is meant to be used with `resolveFully = true`.

`Unresolved yaml`

```
openapi: 3.0.1
servers:
- url: http://petstore.swagger.io/api

info:
  description: 'This is a sample server Petstore'
  version: 1.0.0
  title: testing source file
  termsOfService: http://swagger.io/terms/

paths:
  "/withInvalidComposedModel":
    post:
      operationId: withInvalidComposedModel
      requestBody:
        content:
          "application/json":
            schema:
              "$ref": "#/components/schemas/ExtendedAddress"
        required: false
      responses:
        '200':
          description: success!
components:
  schemas:
    ExtendedAddress:
      type: object
      allOf:
        - $ref: '#/components/schemas/Address'
        - type: object
          required:
          - gps
          properties:
            gps:
              type: string
    Address:
      required:
      - street
      type: object
      properties:
        street:
          type: string
          example: 12345 El Monte Road
        city:
          type: string
          example: Los Altos Hills
        state:
          type: string
          example: CA
        zip:
          type: string
          example: '94022'
```
Test - when resolveCombinator is set to true. (default parser behavior)

```
@Test
    public void resolveAllOfWithoutAggregatingParameters(@Injectable final List<AuthorizationValue> auths) {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(true);

        // Testing components/schemas
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("src/test/resources/composed.yaml",auths,options).getOpenAPI();
        
        ComposedSchema allOf = (ComposedSchema) openAPI.getComponents().getSchemas().get("ExtendedAddress");
        assertEquals(allOf.getAllOf().size(), 2);

        assertTrue(allOf.getAllOf().get(0).get$ref() != null);
        assertTrue(allOf.getAllOf().get(1).getProperties().containsKey("gps"));


        // Testing path item
        ObjectSchema schema = (ObjectSchema) openAPI.getPaths().get("/withInvalidComposedModel").getPost().getRequestBody().getContent().get("application/json").getSchema();

        assertEquals(schema.getProperties().size(), 5);
        assertTrue(schema.getProperties().containsKey("street"));
        assertTrue(schema.getProperties().containsKey("gps"));

    }
```
`Resolved Yaml`

```
openapi: 3.0.1
info:
  title: testing source file
  description: This is a sample server Petstore
  termsOfService: http://swagger.io/terms/
  version: 1.0.0
servers:
- url: http://petstore.swagger.io/api
paths:
  /withInvalidComposedModel:
    post:
      operationId: withInvalidComposedModel
      requestBody:
        content:
          application/json:
            schema:
              required:
              - gps
              - street
              type: object
              properties:
                street:
                  type: string
                  example: 12345 El Monte Road
                city:
                  type: string
                  example: Los Altos Hills
                state:
                  type: string
                  example: CA
                zip:
                  type: string
                  example: "94022"
                gps:
                  type: string
        required: false
      responses:
        200:
          description: success!
components:
  schemas:
    ExtendedAddress:
      type: object
      allOf:
      - $ref: '#/components/schemas/Address'
      - required:
        - gps
        type: object
        properties:
          gps:
            type: string
    Address:
      required:
      - street
      type: object
      properties:
        street:
          type: string
          example: 12345 El Monte Road
        city:
          type: string
          example: Los Altos Hills
        state:
          type: string
          example: CA
        zip:
          type: string
          example: "94022"
 ```
 
 Test - when resolveCombinator is set to false.
 
 ```
 @Test
    public void resolveAllOfWithoutAggregatingParameters(@Injectable final List<AuthorizationValue> auths) {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolveCombinators(false);

        // Testing components/schemas
        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("src/test/resources/composed.yaml",auths,options).getOpenAPI();
       
        ComposedSchema allOf = (ComposedSchema) openAPI.getComponents().getSchemas().get("ExtendedAddress");
        assertEquals(allOf.getAllOf().size(), 2);
        assertTrue(allOf.getAllOf().get(0).getProperties().containsKey("street"));
        assertTrue(allOf.getAllOf().get(1).getProperties().containsKey("gps"));

        // Testing path item
        ComposedSchema schema = (ComposedSchema) openAPI.getPaths().get("/withInvalidComposedModel").getPost().getRequestBody().getContent().get("application/json").getSchema();
        // In fact the schema resolved previously is the same of /withInvalidComposedModel
        assertEquals(schema, allOf);
        assertEquals(schema.getAllOf().size(), 2);
        assertTrue(schema.getAllOf().get(0).getProperties().containsKey("street"));
        assertTrue(schema.getAllOf().get(1).getProperties().containsKey("gps"));

    }
  ```
  `Resolved Yaml`
  
  ```
openapi: 3.0.1
info:
  title: testing source file
  description: This is a sample server Petstore
  termsOfService: http://swagger.io/terms/
  version: 1.0.0
servers:
- url: http://petstore.swagger.io/api
paths:
  /withInvalidComposedModel:
    post:
      operationId: withInvalidComposedModel
      requestBody:
        content:
          application/json:
            schema:
              type: object
              allOf:
              - required:
                - street
                type: object
                properties:
                  street:
                    type: string
                    example: 12345 El Monte Road
                  city:
                    type: string
                    example: Los Altos Hills
                  state:
                    type: string
                    example: CA
                  zip:
                    type: string
                    example: "94022"
              - required:
                - gps
                type: object
                properties:
                  gps:
                    type: string
        required: false
      responses:
        200:
          description: success!
components:
  schemas:
    ExtendedAddress:
      type: object
      allOf:
      - required:
        - street
        type: object
        properties:
          street:
            type: string
            example: 12345 El Monte Road
          city:
            type: string
            example: Los Altos Hills
          state:
            type: string
            example: CA
          zip:
            type: string
            example: "94022"
      - required:
        - gps
        type: object
        properties:
          gps:
            type: string
    Address:
      required:
      - street
      type: object
      properties:
        street:
          type: string
          example: 12345 El Monte Road
        city:
          type: string
          example: Los Altos Hills
        state:
          type: string
          example: CA
        zip:
          type: string
          example: "94022"
```

### Authentication

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
    "http://petstore.swagger.io/v2/swagger.json",
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
  <version>2.0.9-SNAPSHOT</version>
</dependency>

```

or

```xml
<dependency>
  <groupId>io.swagger.parser.v3</groupId>
  <artifactId>swagger-parser</artifactId>
  <version>2.0.9-SNAPSHOT</version>
</dependency>

```


License
-------

Copyright 2018 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

---
