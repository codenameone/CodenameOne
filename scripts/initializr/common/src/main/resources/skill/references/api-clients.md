# Generated typed API clients (REST / gRPC / GraphQL)

Codename One ships three "spec to typed client" code generators that share one
architecture. You point a Maven goal at a contract file (an OpenAPI spec, a
`.proto`, or a GraphQL schema), it writes **editable** model classes plus one
annotated **client interface** into `common/src/main/java`, and a build-time
annotation processor turns that interface into a working wire implementation on
the next compile. You never hand-write HTTP/marshalling code, and the generated
interface is small enough to read and commit.

All three follow the same shape, so once you know one you know all three:

| | REST / OpenAPI | gRPC | GraphQL |
|---|---|---|---|
| Generate goal | `cn1:generate-openapi` | `cn1:generate-grpc` | `cn1:generate-graphql` |
| Contract | OpenAPI 3.x JSON/YAML | `.proto` | `.graphqls` (+ optional `.graphql` operations) |
| Client annotation | `@RestClient` | `@GrpcClient` | `@GraphQLClient` |
| Method annotations | `@GET`/`@POST`/`@PUT`/`@PATCH`/`@DELETE` | `@Rpc` | `@Query`/`@Mutation`/`@Subscription` |
| Param annotations | `@Path`/`@Query`/`@Header`/`@Body`/`@Cookie` | (positional message) | `@Var`/`@Header` |
| Model annotation | `@Mapped` + `@JsonProperty` | `@ProtoMessage`/`@ProtoField`/`@ProtoEnum` | `@Mapped` + `@JsonProperty` |
| Runtime package | `com.codename1.io.rest` | `com.codename1.io.grpc` | `com.codename1.io.graphql` |
| Response envelope | `Response<T>` | `GrpcResponse<T>` | `GraphQLResponse<T>` |
| Transport | HTTPS (JSON) | gRPC-Web (`application/grpc-web+proto`) | HTTPS POST (JSON); subscriptions over WebSocket |

**Pick by what the backend already speaks** — you don't choose the protocol, the
server does. Use OpenAPI for a normal JSON REST API, gRPC for a protobuf service
exposed via gRPC-Web, GraphQL for a GraphQL endpoint. (For a *handful* of ad-hoc
REST calls, the fluent `Rest` builder in `references/java-api-subset.md` is
lighter than generating a client — reach for codegen when there's a real spec
with many endpoints/messages.)

## How the pipeline works (identical for all three)

1. Run the `generate-*` goal once (or wire it into `generate-sources` so it
   re-runs on every build — that's what the end-to-end test under
   `scripts/protocol-e2e/` does). It writes model classes + the client interface
   under your `basePackage` into `common/src/main/java`. **These are yours** —
   edit them, commit them, regenerate when the contract changes.
2. On the next `compile`, the annotation processor scans the annotated interface
   and emits `<Name>Impl` + a `cn1app.*Bootstrap` registrar into
   `common/target/generated-sources` (not project source).
3. The bootstrap is auto-installed before `Display.init` (the simulator
   `Class.forName`s it; the iOS/Android build server detects it in the app zip),
   so no manual registration is needed.
4. Call `<ClientName>.of(baseUrl)` to get an instance and call its methods.

If a client returns `null` data with an otherwise-normal response, the usual
cause is that `process-annotations` did not run (stale `target/`) or a model
class is not `@Mapped` — rebuild with a clean `compile`.

## Calling convention

Every operation is **asynchronous and non-blocking**: the last parameter is a
callback that fires **on the EDT** (queries/mutations via
`com.codename1.util.OnComplete<...>`; subscriptions via a streaming `Handler`).
There is no synchronous variant — kick off the call, return, and update the UI
from the callback.

## REST / OpenAPI

```bash
mvn -pl common cn1:generate-openapi \
  -Dcn1.openapi.spec=petstore.json \
  -Dcn1.openapi.basePackage=com.example.petstore
```

Emits one `@Mapped` model per schema and one `@RestClient` interface per OpenAPI
**tag** (so `PetApi`, `StoreApi`, ...). String `enum` schemas become real Java
`enum` types and bind through `@Mapped` (a value that is not a legal Java
identifier degrades to `String`).

```java
@RestClient("https://petstore.example.com/v2")
public interface PetApi {
    @GET("/pet/{petId}")
    void getPetById(@Path("petId") long petId,
                    @Header("Authorization") String bearer,
                    OnComplete<Response<Pet>> callback);

    @POST("/pet")
    void addPet(@Body Pet pet, OnComplete<Response<Pet>> callback);

    static PetApi of(String baseUrl) { return RestClients.create(PetApi.class, baseUrl); }
}

// usage
PetApi.of(baseUrl).getPetById(42, "Bearer " + token, response -> {
    if (response.getResponseCode() == 200) {
        Pet pet = response.getResponseData();   // already typed
        renderPet(pet);
    }
});
```

`Response<T>` exposes `getResponseData()`, `getResponseCode()`, and the raw bytes.

## gRPC

```bash
mvn -pl common cn1:generate-grpc \
  -Dcn1.grpc.proto=catalog.proto \
  -Dcn1.grpc.basePackage=com.example.catalog
```

Emits `@ProtoMessage` classes (`@ProtoField` per field, `@ProtoEnum` for proto
enums) and one `@GrpcClient` interface per `service`. The transport is
**gRPC-Web** (`com.codename1.io.grpc.GrpcWeb`) so it works over plain
HTTPS — the server must expose a gRPC-Web endpoint (Envoy, grpc-web filter, or a
direct implementation); raw HTTP/2 gRPC is not reachable from a mobile client.

```java
@GrpcClient("e2e.Catalog")               // package-qualified proto service name
public interface CatalogClient {
    @Rpc("GetProduct")
    void getProduct(GetProductRequest req, OnComplete<GrpcResponse<Product>> callback);

    @Rpc("ListProducts")
    void listProducts(ListProductsRequest req, OnComplete<GrpcResponse<ProductList>> callback);

    static CatalogClient of(String baseUrl) { return GrpcClients.create(CatalogClient.class, baseUrl); }
}

// usage
CatalogClient.of(baseUrl).getProduct(req, response -> {
    if (response.isOk()) {
        Product p = response.getResponseData();
    } else {
        Log.p("grpc-status " + response.getStatus() + ": " + response.getStatusMessage());
    }
});
```

`GrpcResponse<T>` carries the decoded message plus the gRPC `status`/`statusMessage`
trailer and `isOk()`.

## GraphQL

```bash
# Operations mode (precise) — generate exactly the named operations you author:
mvn -pl common cn1:generate-graphql \
  -Dcn1.graphql.schema=schema.graphqls \
  -Dcn1.graphql.operations=operations.graphql \
  -Dcn1.graphql.basePackage=com.example.catalog \
  -Dcn1.graphql.clientName=CatalogGraphApi \
  -Dcn1.graphql.endpoint=https://api.example.com/graphql

# Schema-only quick-start — no operations file; one method per root field with an
# auto-generated selection set expanded to cn1.graphql.maxDepth (default 2):
mvn -pl common cn1:generate-graphql \
  -Dcn1.graphql.schema=schema.graphqls \
  -Dcn1.graphql.basePackage=com.example.catalog
```

The schema's `enum` types become Java enums, `input`/object types become
`@Mapped` classes. In **operations mode** each named `query`/`mutation`/
`subscription` becomes one interface method, its `$variables` become `@Var`
parameters, and a `<OpName>Data` response model is synthesised from the
selection set. Schema-only mode is a convenience that may over- or under-fetch;
operations mode is the precise path.

```java
@GraphQLClient("https://api.example.com/graphql")
public interface CatalogGraphApi {
    @Query(value = "query Products($cat: Category) { products(category: $cat) { id name category } }",
           operationName = "Products")
    void products(@Var("cat") Category category,
                  @Header("Authorization") String bearer,
                  OnComplete<GraphQLResponse<ProductsData>> callback);

    @Mutation(value = "mutation Rate($id: ID!, $stars: Int!) { rate(id: $id, stars: $stars) { id stars } }",
              operationName = "Rate")
    void rate(@Var("id") String id, @Var("stars") int stars,
              OnComplete<GraphQLResponse<RateData>> callback);

    @Subscription(value = "subscription OnRated($id: ID!) { rated(id: $id) { id stars } }",
                  operationName = "OnRated")
    GraphQLSubscription onRated(@Var("id") String id,
                                GraphQLSubscription.Handler<OnRatedData> handler);

    static CatalogGraphApi of(String endpoint) { return GraphQLClients.create(CatalogGraphApi.class, endpoint); }
}

// query/mutation — callback on the EDT
CatalogGraphApi.of(endpoint).products(Category.TOOLS, "Bearer " + token, response -> {
    if (response.isOk()) {                       // HTTP 2xx AND no GraphQL errors
        ProductsData data = response.getData();
    } else if (response.hasErrors()) {
        Log.p(response.getResponseErrorMessage());   // first error message
    }
});

// subscription — streamed over WebSocket (graphql-transport-ws). cancel() to stop.
GraphQLSubscription sub = CatalogGraphApi.of(endpoint).onRated("p-1", new GraphQLSubscription.Handler<OnRatedData>() {
    public void onNext(OnRatedData data) { updateStars(data); }
    public void onError(GraphQLResponse<OnRatedData> err) { Log.p(err.getResponseErrorMessage()); }
    public void onComplete() { }
});
// later: sub.cancel();
```

`GraphQLResponse<T>` is unusual in that GraphQL can return **data and errors at
once**: `getData()`, `getErrors()` (list of `GraphQLError`), `hasErrors()`,
`getHttpCode()`, `getResponseErrorMessage()` (first error), and `isOk()` (HTTP
2xx **and** no errors). Subscriptions need the WebSocket support in the runtime;
queries and mutations are plain HTTPS POST.

## End-to-end reference

`scripts/protocol-e2e/` in the framework repo is a runnable example of all three:
one Spring Boot server exposing the same catalog over REST, gRPC-Web, and
GraphQL, with a Codename One client that generates all three typed clients at
build time from a single shared `specs/` directory and round-trips against the
server via `cn1:test`. Read it when you need a concrete, working setup that
exercises enums, nested objects, list/repeated fields, and multiple methods.
