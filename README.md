# Swagger Parser <img src="https://raw.githubusercontent.com/swagger-api/swagger.io/wordpress/images/assets/SW-logo-clr.png" height="50" align="right">

**NOTE:** If you're looking for `swagger-parser` 1.X and OpenAPI 2.0, please refer to [v1 branch](https://github.com/swagger-api/swagger-parser/tree/v1)

**NOTE:** Since version 2.1.0 Swagger Parser supports OpenAPI 3.1; see [this page](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---OpenAPI-3.1) for details

![Build Master - Java 11, 14 and 17](https://github.com/swagger-api/swagger-parser/workflows/Build%20Test%20Deploy%20master/badge.svg?branch=master)

# Table of contents

  - [Overview](#overview)
  - [Table of Contents](#table-of-contents)
  - [Usage](#usage)
  - [Adding to your project](#adding-to-your-project)
    - [Prerequisites](#prerequisites)
  - [Authentication](#authentication)  
  - [Options](#options)
    - [Resolve](#1-resolve)
    - [ResolveFully](#2-resolvefully)
    - [Flatten](#3-flatten)
    - [ResolveCombinators](#4-resolvecombinators)
  - [Extensions](#extensions)
  - [OpenAPI 3.1 Support](#openapi-31-support)
  - [License](#license)
   
## Overview 

This is the Swagger Parser project, which parses OpenAPI definitions in JSON or YAML format into [swagger-core](https://github.com/swagger-api/swagger-core) representation as [Java POJO](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/OpenAPI.java#L36), returning any validation warnings/errors.  

It also provides a simple framework to add additional converters from different formats into the Swagger objects, making the entire toolchain available.


### Usage
Using the Swagger Parser is simple.  Once included in your project, you can read a OpenAPI Specification from any location:

```java
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.oas.models.OpenAPI;

// ... your code

  // parse a swagger description from the petstore and get the result
  SwaggerParseResult result = new OpenAPIParser().readLocation("https://petstore3.swagger.io/api/v3/openapi.json", null, null);
  
  // or from a file
  //   SwaggerParseResult result = new OpenAPIParser().readLocation("./path/to/openapi.yaml", null, null);
  
  // the parsed POJO
  OpenAPI openAPI = result.getOpenAPI();
  
  if (result.getMessages() != null) result.getMessages().forEach(System.err::println); // validation errors and warnings
  
  if (openAPI != null) {
    ...
  }
  
```

or from a string:

```java
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.oas.models.OpenAPI;

// ... your code

  // parse a swagger description from the petstore and get the result
  SwaggerParseResult result = new OpenAPIParser().readContents("https://petstore3.swagger.io/api/v3/openapi.json", null, null);
  
  // or from a file
  //   SwaggerParseResult result = new OpenAPIParser().readContents("./path/to/openapi.yaml", null, null);
  
  // the parsed POJO
  OpenAPI openAPI = result.getOpenAPI();
  
  if (result.getMessages() != null) result.getMessages().forEach(System.err::println); // validation errors and warnings
  
  if (openAPI != null) {
    ...
  }
  
```

If you are providing a Swagger/OpenAPI 2.0 document to the parser , e.g.:

```java
SwaggerParseResult result = new OpenAPIParser().readContents("./path/to/swagger.yaml", null, null);
```

the Swagger/OpenAPI 2.0 document will be first converted into a comparable OpenAPI 3.0 one.

You can also directly use `OpenAPIV3Parser` which only handles OpenAPI 3.0 documents, and provides a convenience method to get directly the parsed `OpenAPI object:

```java
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;

// ... your code

  // read a swagger description from the petstore
    
  OpenAPI openAPI = new OpenAPIV3Parser().read("https://petstore3.swagger.io/api/v3/openapi.json");
  
```

### Adding to your project
You can include this library from Sonatype OSS for SNAPSHOTS, or Maven central for releases.  In your dependencies:

```xml
<dependency>
  <groupId>io.swagger.parser.v3</groupId>
  <artifactId>swagger-parser</artifactId>
  <version>2.1.15</version>
</dependency>
```

#### Prerequisites
You need the following installed and available in your $PATH:

* Java 11
* [Apache maven 3.x](http://maven.apache.org/)

After cloning the project, you can build it from source with this command:

```
mvn package
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
    "https://petstore3.swagger.io/api/v3/openapi.json",
    Arrays.asList(mySpecialHeader, apiKey)
  );
```

#### Dealing with self-signed SSL certificates
If you're dealing with self-signed SSL certificates, or those signed by GoDaddy, you'll need to disable SSL Trust 
Manager.  That's done by setting a system environment variable as such:

```
export TRUST_ALL=true
```

And then the Swagger Parser will _ignore_ invalid certificates.  Of course this is generally a bad idea, but if you're 
working inside a firewall or really know what you're doing, well, there's your rope.

#### Dealing with Let's Encrypt
Depending on the version of Java that you use, certificates signed by the [Let's Encrypt](https://letsencrypt.org) certificate authority _may not work_ by default.  If you are using any version of Java prior to 1.8u101, you most likely _must_ install an additional CA in your
JVM.  Also note that 1.8u101 may _not_ be sufficient on it's own.  Some users have reported that certain operating systems are 
not accepting Let's Encrypt signed certificates.

Your options include:

* Accepting all certificates per above
* Installing the certificate manually in your JVM using the keystore using the `keytool` command
* Configuring the JVM on startup to load your certificate

But... this is all standard SSL configuration stuff and is well documented across the web.


### Options
Parser uses options as a way to customize the behavior while parsing:

#### 1. resolve:

```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolve(true); 
final OpenAPI openAPI = new OpenAPIV3Parser().read("a.yaml", null, parseOptions);
```


- When remote or relative references are found in the parsed document, parser will attempt to:

1. resolve the reference in the remote or relative location 
1. parse the resolved reference
1. add the resolved "component" (e.g. parameter, schema, response, etc.) to the resolved `OpenAPI` POJO components section
1. replace the remote/relative reference with a local reference,  e.g. : `#/components/schemas/NameOfRemoteSchema`. 

This applies to schemas, parameters, responses, pretty much everything containing a ref.

#### 2. resolveFully:

```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolve(true); // implicit
parseOptions.setResolveFully(true);
final OpenAPI openAPI = new OpenAPIV3Parser().read("a.yaml", null, parseOptions);
```

- In some scenarios, after references are resolved (with `resolve`, see above), you might need to have all local references removed replacing the reference with the content of the referenced element. This is for example used in [Swagger Inflector](https://github.com/swagger-api/swagger-inflector). Be aware that the result could be more heavy/long due to duplication
    
Original document:

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

Serialized result after parsing with option `resolveFully(true)`

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

#### 3. flatten: 

```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setFlatten(true); 
final OpenAPI openAPI = new OpenAPIV3Parser().read("a.yaml", null, parseOptions);
```


This is kind of the opposite of resolveFully, limited to defined schemas.

In some scenarios, you might need to have all schemas defined inline (e.g. a response schema) moved to the `components/schemas` section and replaced with a reference to the newly added schema within `components/schemas`. This is for example used in [Swagger Codegen](https://github.com/swagger-api/swagger-codegen).

Original document:

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

Serialized result after parsing with option `flatten(true)`

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

#### 4. resolveCombinators: 

```java
ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolve(true); // implicit
parseOptions.setResolveFully(true);
parseOptions.setResolveCombinators(false); // default is true 
final OpenAPI openAPI = new OpenAPIV3Parser().read("a.yaml", null, parseOptions);
```

This option (only available with `resolveFully = true`) allows to customize behaviour related to `allOf/anyOf/oneOf` (composed schemas)  processing. With option set to `true` (default), composed schemas are transformed into "non composed" ones, by having all properties merged into a single resulting schema (see example below).
If option is set to `false`, the resulting schema will instead maintain its "composed" nature, keeping properties within e.g. the `allOf` members.

Please see examples below:

**Unresolved yaml**

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

**resolvedCombinator = true (default) - Test case**

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

**resolvedCombinator = true (default) - Resolved Yaml**

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
 
 **resolvedCombinator = false - Test case**
 
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
  
  **resolvedCombinator = false - Resolved Yaml**
  
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

### Extensions
This project has a core artifact--`swagger-parser`, which uses Java Service Provider Interface (SPI) so additional extensions can be added. 

To build your own extension, you simply need to create a `src/main/resources/META-INF/services/io.swagger.v3.parser.core.extensions.SwaggerParserExtension` file with the full classname of your implementation.  Your class must also implement the `io.swagger.v3.parser.core.extensions.SwaggerParserExtension` interface.  Then, including your library with the `swagger-parser` module will cause it to be triggered automatically.

### OpenAPI 3.1 support

Since version 2.1.0 Swagger Parser supports OpenAPI 3.1; see [this page](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---OpenAPI-3.1) for details

## Security contact

Please disclose any security-related issues or vulnerabilities by emailing [security@swagger.io](mailto:security@swagger.io), instead of using the public issue tracker.
