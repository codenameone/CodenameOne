# Evidence map

Source: `docs/website/content/blog/websockets-grpc-and-graphql.md`
Canonical: https://www.codenameone.com/blog/websockets-grpc-and-graphql/

## Thesis

Live sockets and generated typed clients through one Java application stack

## Supported beats

- **Part 1: WebSockets, no cn1lib required:** WebSockets used to require the cn1-websockets cn1lib. They are now part of the framework as com.codename1.io.WebSocket, implemented natively on every port (a hand-rolled RFC 6455 handshake on JavaSE and Android, NSURLSessionWebSocketTask on iOS, the browser WebSocket on JavaScript), with no third-party dependencies pulled into your build.
- **Step 1: open a connection:** There is no URL-in-constructor subclassing trap from the old API; the connection is an object you hold. send(...) has a String and a byte[] overload, getReadyState() returns a WebSocketState, and close() does a clean close handshake.
- **Step 2: build the chat screen:** Here is a compact chat form. Outgoing messages are added immediately; incoming ones arrive on the onTextMessage handler, and because the handler can touch the UI we wrap that in callSerially.
- **Step 3: negotiate a subprotocol when you need one:** One reason to trust this implementation: our own screenshot CI now runs on it. The pipeline that ships rendered PNGs from each device back to the host machine uses a WebSocket as its transport, so the same code your app calls is carrying the binary payloads that validate the framework on every commit.
- **Part 2: a typed GraphQL client:** cn1:generate-graphql turns a GraphQL schema into a typed client, and @GraphQLClient is the interface you write against. The runtime lives in com.codename1.io.graphql, and a GraphQLResponse carries data and errors together so partial results survive.
- **Step 1: declare the client:** The build-time processor emits the implementation and a bootstrap that registers it; you never write the HTTP plumbing. The generator has two modes. The precise operations mode emits per-selection types from your operation documents; the schema-only quick-start mode auto-selects fields to a bounded depth (cn1.graphql.maxDepth).

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5133
- https://github.com/codenameone/CodenameOne/pull/5141
- https://github.com/codenameone/CodenameOne/pull/5099
- https://swapi.example.com/graphql
- https://api.example.com
- https://github.com/codenameone/CodenameOne/issues
