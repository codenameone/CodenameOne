---
title: "Connect Your App to Your Enterprise Traces"
slug: native-backend-observability
url: /blog/native-backend-observability/
date: '2026-10-06'
author: Shai Almog
description: "Connect your client app and native Java backend to distributed enterprise traces, export metrics through OpenTelemetry, and keep collector credentials on the server."
feed_html: '<img src="https://www.codenameone.com/blog/native-backend-observability.jpg" alt="One trace connects the client app, native backend and enterprise services" /> Connect your client app and native Java backend to distributed enterprise traces, export metrics through OpenTelemetry, and keep collector credentials on the server.'
series: ["release-2026-10-02"]
---

![One trace connects the client app, native backend and enterprise services](/blog/native-backend-observability.jpg)

Your customer taps “Place order.” The request crosses your app, an API gateway, a native backend, a payment service and a database. Your operations team needs to see that path in the same tracing system it uses for the rest of the company. Neither the client app nor a compiled Java server should disappear from that picture.

I wrote a book on debugging, so I am probably more opinionated about this than most people: in 2026, production diagnosis should not depend on somebody searching a pile of logs with `grep` and guessing which lines belong together.

Logs still matter. At enterprise scale, though, you need to connect work across machines, teams and runtimes, then see whether a failure is isolated or spreading. On a JVM, Java agents can instrument much of that work for us. Once our backend is translated to C and compiled into a native executable, the usual JVM instrumentation hook is gone.

We have to carry that visibility into the binary. [OpenTelemetry support](/blog/follow-a-tap-with-opentelemetry/) started that work last week. The [expanded backend API](https://github.com/codenameone/CodenameOne/pull/5908) adds metrics and management alongside it. This article follows what happens after you turn them on.

## Follow an operation across your systems

Observability is the ability to understand a system's internal behavior from the evidence it emits. OpenTelemetry, usually shortened to OTel, defines APIs, data conventions and transport for producing that evidence. It is not itself the screen where you investigate an incident; a collector and an observability backend handle that part. The [OTel primer](https://opentelemetry.io/docs/concepts/observability-primer/) provides the broader model.

| Question | Useful evidence |
| --- | --- |
| Why did this request take two seconds? | A trace connecting its operations |
| Is database waiting increasing across the service? | Metrics over time |
| What did the application decide at the failure? | Logs and events with context |

A span records one timed operation. Related spans form a trace. Metrics aggregate behavior across operations: a count, a current value or a distribution such as request duration. Neither replaces the other. A single trace can explain a slow query without telling you how often it occurs.

Our integration exports traces and metrics. It does not turn every application log into an OTLP log record.

## Add tracing to your native executable

Put this in the backend's `application.properties` before building:

```properties
cn1.otel.enabled=true
```

Or use the backend's `@OpenTelemetry` annotation. The generator installs the tracer, making its implementation reachable to the translator. Without that build-time opt-in, the unused tracing code can be left out. Setting an environment variable after deploying a binary that omitted the tracer cannot add it back.

Once enabled, the runtime records an incoming request span named for its route template. A `Web` call creates a client span and propagates W3C trace headers. A database statement creates another client span. Incoming `traceparent` connects the request to its caller.

{{< mermaid >}}
sequenceDiagram
    participant App as CN1 client app
    participant Native as CN1 native backend
    participant Service as Existing enterprise service
    participant DB as Database
    participant Collector as Enterprise OTel collector
    App->>Native: Order request + traceparent
    Native->>Service: HTTP call + traceparent
    Service->>DB: SQL span under the same trace
    DB-->>Service: Result
    Service-->>Native: Result
    Native-->>App: Response
    App->>Native: Batched client spans via relay
    Native->>Collector: Client spans + server spans + metrics
    Service->>Collector: Existing service telemetry
{{< /mermaid >}}

The native backend propagates context on outbound `Web` calls. Each receiving service must accept and continue that context. OTel-compatible instrumentation lets those services participate regardless of their implementation language.

## Include the client, not just the servers

The client annotation belongs on your app's main class:

```java
import com.codename1.annotations.OpenTelemetry;
import com.codename1.system.Lifecycle;

@OpenTelemetry(
        relay = "https://api.example.com",
        serviceName = "orders-app",
        sampleRatio = 0.2,
        requireAnalyticsConsent = true)
public class OrdersApp extends Lifecycle {
}
```

Replace the URL with your backend. Enable its relay as shown below, and wire the app's analytics-consent flow before collecting telemetry. The client instruments supported app operations and `ConnectionRequest` calls, including outgoing trace context. It sends its recorded spans through the backend rather than carrying your collector credentials.

For browser apps, context propagation is limited to the relay host and configured `propagateTo` hosts. A cross-origin service must allow the tracing headers in CORS. The [client tracing walkthrough](/blog/follow-a-tap-with-opentelemetry/) covers consent, propagation and the supported span types.

That connects the experience on the device to the work behind it. A server-only trace cannot show the client work before the request or after the response. You can investigate that larger path in your enterprise tracing system instead of keeping a separate, disconnected account of what the app did.


Database spans record SQL as supplied, with placeholders intact; bound parameters are not recorded. If an application concatenates a secret into SQL, the resulting text already contains it. `cn1.otel.attributes.exclude=db.query.text` can exclude that attribute. Parameterized SQL remains the better habit.

WebSocket lifetime is outside this automatic request-span model. An hours-long connection with thousands of messages is not one useful HTTP request duration, and its upgrade is handled before this tracing path.

## Send it to the system your team already uses

For an enabled build, configure a collector at runtime:

```bash
export OTEL_SERVICE_NAME=orders-native
export OTEL_EXPORTER_OTLP_ENDPOINT=http://127.0.0.1:4318
export OTEL_TRACES_SAMPLER=parentbased_traceidratio
export OTEL_TRACES_SAMPLER_ARG=0.1
./build/Orders
```

Here `Orders` is the packaged application's executable. The ratio samples a tenth of newly rooted traces; the parent-based policy respects an upstream sampling decision.

The exporter uses OTLP over HTTP, with protobuf by default and JSON as an option. It does not implement OTLP/gRPC. It appends `/v1/traces` or `/v1/metrics` to the base endpoint unless you supply a signal-specific endpoint.

Dynatrace can receive that OTLP data. For traces, its [documented ingest endpoint](https://docs.dynatrace.com/docs/ingest-from/opentelemetry/otlp-api) is configured through `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`, with an ingest credential in `OTEL_EXPORTER_OTLP_HEADERS`. This uses explicit telemetry export; the translated server no longer has the JVM hooks used by Java instrumentation agents.

A dedicated exporter thread batches spans. If the collector is slow or unavailable, request handling does not wait indefinitely for it. A bounded queue eventually drops spans and records the drops. That preserves the service at the cost of incomplete telemetry, which is itself a condition to monitor.

## Track latency without a metric for every URL

The server records request duration, active requests, open connections, database pool counts and task queue depth. Scheduled jobs report duration and outcome. The default metric export interval is one minute, using cumulative temporality.

Route labels use `/orders/{id}`, not `/orders/81379`. Putting each order ID into a metric label would create a new time series for every order. Templates keep the series count bounded by the API's shape.

The application can instrument a method too:

```java
package example;

import com.codename1.backend.annotations.Counted;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Timed;

@Component
public class QuoteService {
    @Timed
    @Counted
    public int quoteCents(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return quantity * 250;
    }
}
```

This deliberately small example creates a duration histogram and call counters, including a failure counter. The default instrument names derive from the class and method. The build weaves the measurement into the method, so a call through `this` is measured too. For a real quote service, validate upper bounds and apply the application's monetary rules.

`@ManagedResource`, `@ManagedAttribute` and `@ManagedOperation` offer a JMX-inspired model for current values and operator actions. Numeric managed getters become gauges. The management transport is the backend's own HTTP API; this is not a JVM JMX server hidden inside the native executable. Follow the [management configuration](/developer-guide/backend-observability/) before exposing operational actions.

## Let an agent check the running application

MCP, the Model Context Protocol, gives a development tool a structured way to ask the server questions. The backend serves tools for listing routes, inspecting beans, reading recent requests and querying the database. After editing a handler, you can call it and inspect the row it wrote from your development tool.

A practical loop is:

1. Start the backend on its development profile.
2. Inspect `backend_routes`, `backend_beans` and `backend_schema`.
3. Make the edit, then stop and restart the server. There is no hot reload.
4. Exercise the handler with `backend_call`.
5. Check `backend_requests`, `backend_sql` and `backend_metrics`.

The default SQL tool uses a database-enforced read-only transaction unless its caller explicitly requests write access. An application can publish its own methods with `@McpTool`; the build derives JSON Schema from their supported parameter types and calls them directly.

For a tool of your own, here is a complete bean using the generated dispatch:

```java
package example;

import com.codename1.backend.annotations.McpParam;
import com.codename1.backend.annotations.McpTool;
import com.codename1.backend.annotations.Component;

@Component
public class QuoteTools {
    private final QuoteService quotes;

    public QuoteTools(QuoteService quotes) {
        this.quotes = quotes;
    }

    @McpTool(description = "Returns a quote in cents for a positive quantity.")
    public int quote(@McpParam("quantity") int quantity) {
        return quotes.quoteCents(quantity);
    }
}
```

It reuses `QuoteService` above. The description tells an agent when the tool is relevant, and `@McpParam` supplies a stable argument name independent of compiler parameter metadata. Runtime authentication still has to be configured before serving it outside development.

Development tools are omitted by `backendPackage` / `cn1:backend-package` unless deliberately enabled. Outside development, the MCP endpoint requires a bearer token. A tokenless development server is restricted to loopback. Origin checks also matter when a browser can reach that machine. The [MCP chapter](/developer-guide/backend-mcp/) spells out these boundaries.

## Keep collector credentials off the phone

An app can send its own spans through the backend relay. The relay validates the OTLP/JSON payload, applies size and span-count limits, then queues it for export using credentials held by the server.

```properties
cn1.otel.relay=true
cn1.otel.relay.token=${RELAY_TOKEN}
cn1.otel.relay.corsOrigin=https://app.example.com
```

The CORS setting is for a web app on another origin. An app-carried relay token is not an unextractable secret; the collector's ingest credentials should remain on the backend. A full queue returns 503 so the client can back off.

Start with one real app action that crosses the native backend and an existing enterprise service. Verify that its spans share a trace ID in your collector, then check consent, sampling, queue drops and access to the relay before expanding collection. The goal is to make the app and its native server observable parts of the system your operations team already runs.

---

## Discussion

_Which part of your application is hardest to connect to an end-to-end trace?_

{{< giscus >}}
