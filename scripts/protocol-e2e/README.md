# Multi-protocol end-to-end test (REST / GraphQL / gRPC)

This test exercises the full Codename One generated-client stack -- including
the build-time **code generation** -- for all three "spec to typed client"
protocols against a single real server, over a non-trivial catalog API
(enums, nested objects, repeated/list fields, and multiple methods).

## Layout

- `specs/` -- the canonical contracts: [`openapi.json`](specs/openapi.json),
  [`schema.graphqls`](specs/schema.graphqls) +
  [`operations.graphql`](specs/operations.graphql), and
  [`catalog.proto`](specs/catalog.proto).
- `server/` -- a Spring Boot app exposing the same catalog three ways:
  - **REST** (`GET /api/products`, `/api/products/{id}`,
    `/api/products/category/{category}` with an enum path parameter).
  - **GraphQL** (`POST /graphql`, Spring for GraphQL) -- list/single queries
    and an enum-typed variable.
  - **gRPC-Web** (`POST /grpc/e2e.Catalog/{GetProduct,ListProducts}`) -- the
    gRPC-Web binary framing and protobuf wire format are implemented directly,
    so no Envoy/proxy sidecar is required.
- `client/` -- a Codename One app (`common` + `javase`). The `common` module
  runs `cn1:generate-openapi`, `cn1:generate-grpc`, and `cn1:generate-graphql`
  at **build time** (from the specs under `client/common/cn1specs/`) into
  `target/generated-sources/cn1`; `cn1:process-annotations` then generates the
  client impls and the `cn1app.*Bootstrap` registrars. The `AbstractTest`
  classes under `client/common/src/test/java/com/codename1/e2e/` perform real
  round-trips against the running server and run on the JavaSE simulator via
  `cn1:test`.
- `run-protocol-e2e.sh` -- builds and starts the server, then builds and runs
  the client tests against it.

## What it covers

| Nuance | REST | GraphQL | gRPC |
|--------|------|---------|------|
| Enum field in a response | yes | yes | yes |
| Enum as a parameter/variable | path param | variable | request field |
| Nested object | `Dimensions` | nested selection | -- |
| List / repeated field | `tags` | `tags` | `tags`, `repeated Product` |
| Multiple methods | 3 | 3 | 2 |
| Varied scalars | long, double, string | ID, Float, String | int64, double, string |

Because the clients are generated during the build, a regression in
`cn1:generate-openapi` / `-grpc` / `-graphql` breaks this test.

## Running locally

```bash
# Prerequisite: install this checkout's CN1 artifacts into your local repo
cd maven
mvn -pl core,javase,css-compiler,codenameone-maven-plugin -am install \
    -Plocal-dev-javase -DskipTests

# Then run the end-to-end test (JDK 17 on PATH)
cd ..
scripts/protocol-e2e/run-protocol-e2e.sh
```

CI runs the same script in `.github/workflows/protocol-e2e.yml`.
