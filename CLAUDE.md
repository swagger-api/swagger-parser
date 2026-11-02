# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branch context

This is the **v1.x line** (Swagger 2.0 / OpenAPI 2, `io.swagger:*`, swagger-core 1.6.x, Java 8 source/target). OpenAPI 3.x and `swagger-parser-v3` / `v2-converter` belong to the 2.x line on `master` — do not add 2.x material here (a 2.x-flavoured CLAUDE.md and fixtures were previously committed by accident and removed). Directories like `modules/swagger-parser-v3`, `-cli`, `-core`, `-v2-converter` may exist locally with only `target/` leftovers; they are not in the root `pom.xml` `<modules>`.

## Commands

```bash
mvn -B verify                                  # full build + tests (what CI runs; see .github/workflows/maven-v1.yml)
mvn -B verify -Dsurefire.forkCount=4 -DargLine="-XX:-OmitStackTraceInFastThrow"   # CI flags
mvn -pl modules/swagger-parser -am test        # one module (+ upstream deps)
mvn -pl modules/swagger-compat-spec-parser test -Dtest=JsonPatchTest              # single test class
mvn -pl modules/swagger-parser test -Dtest=SwaggerParserTest#methodName           # single test method
```

Tests mix TestNG (6.9.6), JUnit 4, JMockit and WireMock. CI builds on JDK 8 (Temurin) only.

## Modules (root `pom.xml`)

- `swagger-parser-safe-url-resolver` — SSRF-protection layer for fetching remote `$ref`s/specs (allow/deny host matchers). Built first; `swagger-parser` depends on it.
- `swagger-parser` — reads Swagger 2.0 JSON/YAML into swagger-core `Swagger` POJOs and resolves `$ref`s.
- `swagger-compat-spec-parser` — reads legacy Swagger 1.1/1.2 (resource listing + API declarations) and migrates them to 2.0.

## Architecture

**Extension-based entry point.** `SwaggerParser` (`io.swagger.parser`) is a facade: it discovers `SwaggerParserExtension` implementations via `ServiceLoader` (`META-INF/services/io.swagger.parser.SwaggerParserExtension`) and tries each in order until one returns a non-null result. `Swagger20Parser` is the built-in 2.0 implementation; `SwaggerCompatConverter` (compat module, registered in its own `META-INF/services` file) plugs in the same way, which is why adding the compat jar enables 1.x input with no code change.

**Parse → resolve pipeline (swagger-parser).** `Swagger20Parser` deserializes via `SwaggerDeserializer` (hand-written tree walker that collects validation messages into `SwaggerDeserializationResult` rather than throwing). If `ParseOptions.resolve` is set, `SwaggerResolver` runs the `processors/*` (Definitions/Paths/Operation/Parameter/Response/Model/Property/ExternalRefProcessor) over the model, using `ResolverCache` to load and cache external refs (file, classpath, remote via `RemoteUrl`, with auth headers from `AuthorizationValue`). `InlineModelResolver` optionally flattens inline schemas into `definitions`.

**Legacy migration (compat module).** `SwaggerCompatConverter` downloads/reads 1.x docs (`APIFileDownload`), then `transform/migrate/*` migrators (`V11*`, `ApiObjectMigrator`, …) rewrite the JSON tree to 1.2 shape using JSON patches in `src/main/resources/patches`, validated against JSON schemas in `resources/schemas`, before mapping to `Swagger`.

**java-json-tools was removed** (security fix, branch `remove-java-json-tools`). Its replacement lives in `io.swagger.transform.util`: `JsonPatch` (own RFC-6902 impl), `JacksonSupport`, `MutableJsonTree`, `SwaggerJsonSchemaFactory`. Don't reintroduce `com.github.fge.*` imports. When comparing `JsonNode`s in TestNG use `equals()`/`assertTrue`, not `assertEquals` (it picks the `Iterable` overload and ignores object member names).

## Release / CI

Releases are scripted via GitHub workflows (`prepare-release-v1.yml`, `release-v1.yml`, `next-snapshot-v1.yml`) calling `CI/*.sh|py`; see `CI/CI.md`. Don't hand-edit versions for releases.

`TRUST_ALL=true` env var disables SSL cert validation for remote fetches (documented in README).
