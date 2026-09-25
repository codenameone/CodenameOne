---
title: "OpenTelemetry Support from App to Database"
slug: follow-a-tap-with-opentelemetry
url: /blog/follow-a-tap-with-opentelemetry/
date: '2026-09-28'
author: Shai Almog
description: "Enable distributed tracing in the app and native Java backend, propagate request context, and export database and HTTP spans through OpenTelemetry."
feed_html: '<img src="https://www.codenameone.com/blog/follow-a-tap-with-opentelemetry.jpg" alt="One trace from the app to the database" /> Enable distributed tracing in the app and native Java backend, propagate request context, and export database and HTTP spans through OpenTelemetry.'
series: ["release-2026-09-25"]
---

![One trace from the app to the database](/blog/follow-a-tap-with-opentelemetry.jpg)

A user taps Save. The spinner stays up for three seconds. Your API dashboard says the server is healthy, and the database log contains thousands of queries. Which of those facts explains the wait?

Distributed tracing gives the operation a shared identity as it crosses process boundaries. Codename One now instruments app requests and backend work so you can follow that path into a tracing system your team already uses. The entry point is an annotation on each side.

## What OpenTelemetry contributes

Observability is the ability to investigate what a running system is doing from the evidence it emits. Metrics summarize behavior across many operations. Logs record events. Traces connect the operations involved in a particular request or action. [OpenTelemetry](https://opentelemetry.io/docs/concepts/observability-primer/) supplies a common framework for collecting and exchanging those signals.

A **span** describes one operation with a start, an end, and attributes. A **trace** connects related spans. The [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/) receives telemetry, processes it, and exports it to the systems used to store and inspect it.

For a large organization, the common format matters as much as the timing. The mobile team, API team, and database team may own different codebases and dashboards. A shared trace ID lets them discuss the same request. Standard collector endpoints and deployment variables let a new service join that infrastructure.

**This release adds tracing.** It doesn't turn an annotation into a complete monitoring installation or automatically export every possible metric and log. You still need a collector or compatible ingestion endpoint and a system that stores and displays the spans.

## Turn it on in the backend

Put this controller in the backend module. These are the backend annotation imports:

```java
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.OpenTelemetry;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;

@OpenTelemetry(serviceName = "notes")
@RestController
public class NotesController {
    @GetMapping("/notes/{id}")
    public String note(@PathVariable("id") String id) {
        return id;
    }
}
```

The generated entry point installs the tracer. An alternative is `cn1.otel.enabled=true` in `application.properties`. A server that supplies its own bootstrap can install `OtlpTracer` explicitly; the [backend guide source](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Backend.asciidoc) covers that path.

For a collector running on the same development machine, the default endpoint is `http://localhost:4318`. Deployment configuration uses standard names:

```bash
export OTEL_SERVICE_NAME=notes
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

The base endpoint gets `/v1/traces` appended. `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` instead takes the complete trace URL. OTLP/HTTP supports protobuf, the default, and JSON. This exporter doesn't use OTLP over gRPC.

## Join the app to the trace

The app annotation has a different package. Put it on the application's main class:

```java
import com.codename1.annotations.OpenTelemetry;
import com.codename1.system.Lifecycle;

@OpenTelemetry(
        relay = "https://api.example.com",
        serviceName = "notes-app",
        sampleRatio = 0.2,
        requireAnalyticsConsent = true)
public class NotesApp extends Lifecycle {
}
```

Replace the example URL with your backend. Enable the relay there:

```properties
cn1.otel.enabled=true
cn1.otel.relay=true
cn1.otel.relay.corsOrigin=https://app.example.com
```

Set `cn1.otel.relay.corsOrigin` to your web app's origin when it differs from the relay's origin. Without it, the relay returns HTTP 405 to the browser's `OPTIONS` preflight and the browser won't send the spans. Same-origin web deployments and native clients don't need this CORS setting.

The app sends spans to the backend's `/otel/v1/traces` route. The backend forwards them using its collector credentials. Those credentials stay on the server. Protect and size the ingestion route for your deployment; a relay doesn't make arbitrary client-supplied telemetry trustworthy.

The generated bootstrap installs app telemetry before startup. `ConnectionRequest` becomes the network instrumentation boundary, which also covers generated REST, GraphQL, and gRPC-Web clients using that transport. A handwritten native networking library outside that path isn't automatically covered.

To group requests belonging to one action, create a parent span:

```java
import com.codename1.telemetry.Telemetry;

Telemetry.run("save-note", () -> notesClient.save(note));
```

Here `notesClient.save(note)` is your application call. Requests queued inside the callback inherit the action's context. For an operation whose lifetime ends elsewhere, use `Telemetry.startSpan` and end the span when that operation completes; a synchronous wrapper doesn't wait for all asynchronous application work.

{{< mermaid >}}
flowchart TD
    App[App action and request span] -->|traceparent| API[Backend route span]
    API --> DB[Database statement span]
    App -. batched app spans .-> Relay[Backend trace relay]
    Relay --> Collector[OTel collector]
    API -. batched backend spans .-> Collector
    DB -. same backend exporter .-> Collector
{{< /mermaid >}}

Trace context and trace export are separate traffic. The request carries its identity to the service; batches of completed spans reach the collector afterward.

## What is automatic, and what still needs application code?

| Operation | Current behavior |
| --- | --- |
| App `ConnectionRequest` | Request span and outgoing `traceparent` |
| Backend HTTP route | Server span named for the matched route |
| Backend outbound `Web` call | Client span with propagated trace context |
| Backend `Database` statement | Statement span, without bound parameter values |
| WebSocket connection | Connection metrics; no automatic lifetime or per-message trace span |
| Application-specific work | Add a span or attributes explicitly |

A WebSocket can remain open for hours. Treating its whole lifetime as one request span would hide the operations you care about. The upgrade is answered before HTTP request tracing starts. Use `HttpServer.getMetrics()` to inspect connection counts, and add application spans for work triggered by messages when useful. Don't expect a per-message timeline from the annotation alone.

On the server, `Tracing.current()` exposes the current request span and `Tracing.inSpan` times a child operation. This is where an application can name work that the HTTP and database layers cannot infer.

## Keep useful traces without collecting everything

The app example samples 20 percent of new traces. An unsampled trace still propagates an unsampled context, and the backend's default parent-based sampler follows it. Independent services can configure different sampling policies, so check the whole path before assuming every trace will be complete.

With `requireAnalyticsConsent = true`, app tracing runs only when the Analytics API allows it. Without consent, there is no app span or trace header, and buffered spans are discarded when consent is withdrawn. That is a control for your app's policy, not a substitute for deciding which data belongs in telemetry.

The app excludes query strings and request or response bodies. Backend SQL spans retain the statement text with placeholders, while omitting bound values. If application code concatenates sensitive values into SQL, those values are part of the statement text. Parameterize the query or exclude `db.query.text` through `cn1.otel.attributes.exclude`.

On web builds, cross-origin `traceparent` headers can trigger CORS preflight. The app therefore limits propagation to the relay host and hosts listed in `propagateTo`. Configure the receiving service to allow the header. Other client platforms propagate to every host, so review that behavior against your network policy.

## A collector outage should not become an app outage

Backend export uses a bounded queue and a separate thread. A slow collector doesn't hold the HTTP response open. If the queue fills, spans are dropped and counted in server metrics. This is deliberate loss under pressure, which also means a trace viewer is not a durable audit ledger.

The app batches spans too. Call `Telemetry.flush()` when it pauses; in-memory spans can disappear if the OS reclaims the process. Choose sampling and retention with those limits in mind.

The implementation and tests are in [PR #5887](https://github.com/codenameone/CodenameOne/pull/5887). Start with one app action, one route, and one parameterized database query. Verify their parent-child relationship in your collector before expanding the instrumentation. [Yesterday's ORM article](/blog/orm-session-from-phone-to-server/) supplies a useful database workload; tomorrow's {{< post-link path="/blog/websocket-server-tests-our-apps" text="WebSocket story" >}} covers a connection that behaves differently from an ordinary request.

---

## Discussion

_When an app is slow, where does your team first lose the connection between the user action and the server work?_

{{< giscus >}}
