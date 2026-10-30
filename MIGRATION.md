# Migrating to Swagger Parser 3.0

## Overview

Swagger Parser 3.0 uses Jackson 3 and needs Java 17 or later. It is built on Swagger Core 3.0.
Read the [Swagger Core 3.0 migration guide](https://github.com/swagger-api/swagger-core/blob/3.0.0/MIGRATION.md)
together with this guide. This guide only describes what is different in Swagger Parser.

## Compatibility

| | Swagger Parser 2.x | Swagger Parser 3.0 |
|---|---|---|
| Java | 8+ | **17+** |
| Jackson | 2 (`com.fasterxml.jackson`) | **3** (`tools.jackson`) |
| Swagger Core | 2.2.x | 3.0.x |
| OpenAPI | 3.0 and 3.1 | 3.0 and 3.1 |

## Known issue: Jackson 2 and Jackson 3 on the same classpath

The table shows the Jackson version for each module.

| Module | Jackson 3 | Jackson 2 |
|---|---|---|
| `swagger-parser-core` | - | `jackson-annotations` only |
| `swagger-parser-v3` | compile | `jackson-annotations` only |
| `swagger-parser-v2-converter` | compile | through the old 1.x library |
| `swagger-parser` | through `swagger-parser-v3` | through `swagger-parser-v2-converter` |
| `swagger-parser-cli` | compile | `jackson-annotations` only |
| `swagger-parser-safe-url-resolver` | - | - |

`jackson-annotations` stays on `com.fasterxml.jackson.core`. Jackson 3 still uses these annotations.

## Breaking changes

### Java 17 is required

**Impact:** High

Java versions below 17 are not supported. Update your Java runtime and your build tools to Java 17 or later.

### Jackson types in the public API

**Impact:** High

The public API uses types from `tools.jackson`. For example, `JsonNode` and `ObjectMapper`, and the mappers that
you get from Swagger Core (`Json.mapper()`, `Yaml.mapper()`).

**Migration:**

1. Change the Maven or Gradle coordinates from `com.fasterxml.jackson.core` / `.dataformat` to `tools.jackson.core` / `.dataformat`.
2. Change the imports from `com.fasterxml.jackson.*` to `tools.jackson.*`. Do not change `com.fasterxml.jackson.annotation`.
3. Use the new names:

| Jackson 2 | Jackson 3 |
|---|---|
| `JsonProcessingException` (checked) | `JacksonException` (unchecked) |
| `TextNode` | `StringNode` |
| `node.fieldNames()` | `node.propertyNames()` |
| `node.elements()` | `node.values()` |
| `node.isContainerNode()` | `node.isContainer()` |
| `node.textValue()` | `node.stringValue()` (throws for a node that is not a string) or `node.stringValue(null)` |
| `node.asText()` | `node.asString()` |
| `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` | `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` |

   Other API changes:

   | Jackson 2 | Jackson 3 |
   |---|---|
   | `node.fields()` | `node.properties()` |
   | `mapper.copy()` | `mapper.rebuild()...build()` |
   | `mapper.writer(prettyPrinter)` | `mapper.writer().with(prettyPrinter)` |
   | `DeserializationUtils.setYaml30Mapper(YAMLFactory)` and `setYaml31Mapper(YAMLFactory)` | Same methods. They now take the Jackson 3 `tools.jackson.dataformat.yaml.YAMLFactory`. |

   `JacksonException` is unchecked. A `throws JsonProcessingException` or `throws IOException` clause for mapper calls is not needed any more.
   A `catch (IOException e)` block around mapper calls does not compile if nothing else in the block throws `IOException`.
4. Remove `JavaTimeModule`. Jackson 3 has this support built in.
5. Mappers are immutable. Build them with `JsonMapper.builder()` or `YAMLMapper.builder()`.
   You cannot call `mapper.configure(...)` after the mapper is built.

### Maximum nesting depth is 500

**Impact:** Low

Jackson 3 limits the nesting depth of a JSON document to 500. Jackson 2 allowed 1000. A deeper document is rejected.

### Dependencies that are no longer transitive

**Impact:** Medium

Swagger Core 3.0 does not bring these libraries any more. `swagger-parser-v3` now declares
`commons-lang3` and `slf4j-api` itself. If your code uses `commons-lang3` or the classic `org.yaml:snakeyaml`
and it did not declare them, add them to your build. Jackson 3 YAML uses `snakeyaml-engine`.
See the Swagger Core guide.

## Behavior differences

### Jackson 3 defaults

Swagger Parser builds its mappers on the Jackson 3 defaults. It sets these options by itself:

| Option | Value |
|---|---|
| Property inclusion | `NON_NULL` |
| `SerializationFeature.FAIL_ON_EMPTY_BEANS` | off |
| `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` | off |
| `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` | off |
| `StreamReadFeature.STRICT_DUPLICATE_DETECTION` | on |

Jackson 3 changed other defaults. For example, `FAIL_ON_TRAILING_TOKENS`, `FAIL_ON_NULL_FOR_PRIMITIVES` and
`READ_ENUMS_USING_TO_STRING` are now on, and properties are sorted alphabetically. See the
[Jackson 3 migration guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md).

### Command line tool output encoding

The command line tool (`swagger-parser-cli`) now writes the output file in UTF-8. Before, it used the default encoding of the platform.
