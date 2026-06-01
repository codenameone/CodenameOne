# Multi-protocol end-to-end test (REST / GraphQL / gRPC)

This test exercises the full Codename One generated-client stack for all three
"spec → typed client" protocols against a single real server.

## Layout

- `server/` — a Spring Boot app exposing the same logical "greeting" service
  three ways:
  - **REST** (`GET /api/greeting`, `POST /api/echo`) — JSON, described by
    [`specs/openapi.json`](specs/openapi.json).
  - **GraphQL** (`POST /graphql`) — Spring for GraphQL, schema
    [`specs/schema.graphqls`](specs/schema.graphqls).
  - **gRPC-Web** (`POST /grpc/e2e.Greeter/SayHello`) — the gRPC-Web binary
    framing the CN1 client speaks (`application/grpc-web+proto`) is implemented
    directly, so no Envoy/proxy sidecar is needed. Contract:
    [`specs/greeter.proto`](specs/greeter.proto).
- `client/` — a Codename One app (`common` + `javase`). Its
  `@RestClient` / `@GraphQLClient` / `@GrpcClient` sources under
  `client/common/src/main/java/com/codename1/e2e/` mirror what
  `cn1:generate-openapi` / `cn1:generate-graphql` / `cn1:generate-grpc` produce
  from the specs; `cn1:process-annotations` generates the impls and the
  `cn1app.*Bootstrap` registrars at build time. The `AbstractTest` classes
  under `client/common/src/test/java/com/codename1/e2e/` perform real
  round-trips against the running server and are executed on the JavaSE
  simulator via `cn1:test`.
- `run-protocol-e2e.sh` — builds and starts the server, then builds and runs
  the client tests against it.

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
