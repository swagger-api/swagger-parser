# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build and run all tests
mvn package

# Skip tests
mvn package -DskipTests

# Run tests for a single module
mvn test -pl modules/swagger-parser-v3

# Run a single test class
mvn test -pl modules/swagger-parser-v3 -Dtest=OpenAPIResolverTest

# Run a single test method
mvn test -pl modules/swagger-parser-v3 -Dtest=OpenAPIResolverTest#testMethod

# Build one module plus the modules it depends on (e.g. v3 needs core)
mvn test -pl modules/swagger-parser-v3 -am

# Install all modules to local repo (needed for cross-module development)
mvn install -DskipTests
```

## Branches

- `master` is the PR target and the main branch (snapshot versions auto-deploy from it).
- `v1` is the 1.x line (currently `1.0.x-SNAPSHOT`). Check `git branch --show-current` before assuming which line you are on; feature branches may be cut from either.

Tests use **TestNG** (not JUnit) with **JMockit** for mocking. The surefire plugin is configured with a JMockit javaagent — do not remove it or tests will fail.

## Module Structure

The project is a multi-module Maven build (`groupId: io.swagger.parser.v3`, current version tracked in root `pom.xml`):

| Module | Artifact | Role |
|---|---|---|
| `swagger-parser-core` | `swagger-parser-core` | Interfaces & models only: `SwaggerParserExtension`, `ParseOptions`, `SwaggerParseResult`, `AuthorizationValue` |
| `swagger-parser-v3` | `swagger-parser-v3` | Core implementation — parses OAS 3.0 and 3.1, resolves refs |
| `swagger-parser-v2-converter` | `swagger-parser-v2-converter` | Converts Swagger/OAS 2.0 docs into OAS 3.0 POJOs |
| `swagger-parser` | `swagger-parser` | Thin aggregator; `OpenAPIParser` is the recommended user-facing entry point |
| `swagger-parser-cli` | `swagger-parser-cli` | CLI wrapper |
| `swagger-parser-safe-url-resolver` | `swagger-parser-safe-url-resolver` | URL allowlist/blocklist enforcement (`PermittedUrlsChecker`) |
| `swagger-compat-spec-parser` | `swagger-compat-spec-parser` | Legacy compatibility reader classes (`io.swagger.reader.SwaggerReader`, `SwaggerReaderFactory`, etc.) |

## Architecture: Parsing & Resolution Pipeline

### Entry Points

- **`OpenAPIParser`** (`swagger-parser`) — recommended entry point; iterates all registered `SwaggerParserExtension` implementations via Java SPI and returns the first successful result.
- **`OpenAPIV3Parser`** (`swagger-parser-v3`) — the main `SwaggerParserExtension` implementation, handles both OAS 3.0 and 3.1.

### Parse Flow (inside `OpenAPIV3Parser`)

1. **Load content** from URL/file/classpath via `readContentFromLocation`.
2. **Deserialize** JSON/YAML string into a `JsonNode` tree using `DeserializationUtils` (handles YAML anchors safely) or falls back to a plain Jackson `ObjectMapper`.
3. **Deserialize POJO** — `OpenAPIDeserializer.deserialize()` maps the tree to an `OpenAPI` POJO.
4. **Resolve refs** (if `ParseOptions.resolve` or `resolveFully` is set):
   - **OAS 3.1**: `OpenAPIDereferencer` chain (in the `reference/` package). `DereferencersFactory` loads implementations via SPI. The default implementation is `OpenAPIDereferencer31`.
   - **OAS 3.0**: `OpenAPIResolver` coordinates a set of Processors (one per OpenAPI component type: paths, parameters, schemas, responses, etc.) plus a `ResolverCache` that memoises remote/file fetches.
5. **`ResolverFully`** — if `resolveFully` is set, inlines all remaining local `$ref`s by replacing them with copied schema content.
6. **`InlineModelResolver`** — if `flatten` is set, hoists inline schemas up into `components/schemas` and replaces them with `$ref`s (opposite of resolveFully).

### Key Classes

- **`ResolverCache`** — caches loaded external files/URLs and extracted sub-nodes to avoid repeated I/O. Handles `RefFormat` (internal, relative, absolute, URL).
- **`ExternalRefProcessor`** — resolves an external `$ref`, fetches the target, and registers it under `components/`.
- **`OpenAPIDeserializer`** — large hand-written deserializer (not generated); produces validation messages alongside the POJO.
- **`RefUtils`** / **`PathUtils`** — low-level helpers for ref string parsing, relative path computation, definition name extraction.
- **`RemoteUrl`** — HTTP fetching, respects `AuthorizationValue` headers/query params and the `TRUST_ALL` system property for SSL.

### Extension Points

Custom parser extensions must:
1. Implement `io.swagger.v3.parser.core.extensions.SwaggerParserExtension`.
2. Register in `META-INF/services/io.swagger.v3.parser.core.extensions.SwaggerParserExtension`.

`OpenAPIV3Parser.getExtensions()` loads them via `ServiceLoader` from both the thread context classloader and this class's classloader (for OSGi compatibility).

### OAS 3.1 vs 3.0 Resolution

The codebase has two distinct resolution paths that must be kept in sync when fixing ref-resolution bugs:

- **OAS 3.0** → `OpenAPIResolver` + processors in `processors/` package
- **OAS 3.1** → `OpenAPIDereferencer` chain in `reference/` package; the traversal uses a Visitor pattern (`Traverser`, `Visitor`, `AbstractVisitor`)

OAS 3.1 resolution can be disabled entirely via the `disableOas31Resolve` environment variable.

## Test Conventions

- Test resources (YAML/JSON fixtures) live in `modules/swagger-parser-v3/src/test/resources/`.
- Issue-specific tests are typically named `Issue<number>Test.java` or placed in the main `OpenAPIResolverTest.java`.
- JMockit `@Injectable` is used to mock `List<AuthorizationValue>` parameters in many tests.
- WireMock (`wiremock-jre8`) is used in `NetworkReferenceTest` for tests that simulate remote refs.

## Release Process

Two-phase semi-automated release via GitHub Actions:
1. Run `Prepare Release` workflow → creates a PR with bumped versions and draft release.
2. After merging that PR, run `Release` workflow → publishes to Maven Central and creates the GitHub release.

Snapshot versions are auto-deployed on pushes to `master`. Release builds enforce no-snapshot dependencies (`maven-enforcer-plugin`).
