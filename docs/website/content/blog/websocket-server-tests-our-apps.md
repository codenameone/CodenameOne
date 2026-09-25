---
title: "WebSocket Support for Your Java Backend"
slug: websocket-server-tests-our-apps
url: /blog/websocket-server-tests-our-apps/
date: '2026-09-29'
author: Shai Almog
description: "Add live updates with Java WebSocket endpoints for text and binary messages. Learn the API, connection limits, and how we use it in device-test CI."
feed_html: '<img src="https://www.codenameone.com/blog/websocket-server-tests-our-apps.jpg" alt="WebSocket support for the Codename One Java backend" /> Add live updates with Java WebSocket endpoints for text and binary messages. Learn the API, connection limits, and how we use it in device-test CI.'
series: ["release-2026-09-25"]
---

![WebSocket support for the Codename One Java backend](/blog/websocket-server-tests-our-apps.jpg)

A job finishes on the server. The app should know about it without asking the same HTTP endpoint every few seconds. WebSockets keep a two-way connection open so either side can send a message when there's something to report.

The Codename One backend now supports RFC 6455 WebSockets, with Java callbacks for text and binary messages. The client API, `com.codename1.io.WebSocket`, already existed. This release adds the server half, `com.codename1.backend.WebSocket`, and `@WebSocketMapping` to register an endpoint.

We're using it in our own device tests. Their screenshots now travel through the same backend API you can use in your app. We'll build a small endpoint first, then look at that receiver and the resource limits you'll need to consider for a live service.

## Start with a complete endpoint

Put this class in the backend module of a Codename One project:

```java
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;
import java.io.IOException;

@WebSocketMapping("/echo")
public class EchoEndpoint implements WebSocket {
    public void onOpen(WebSocketSession session) throws IOException {
        session.sendText("connected");
    }

    public void onText(WebSocketSession session, String message)
            throws IOException {
        session.sendText(message);
    }

    public void onBinary(WebSocketSession session, byte[] data,
                         int offset, int length) throws IOException {
        session.sendBinary(data, offset, length);
    }
}
```

Run the generated backend:

```bash
mvn -pl backend -Dcodename1.platform=backend cn1:backend
```

Connect a WebSocket client to `ws://localhost:8080/echo`. The build discovers the annotation and registers the route before the listener starts. `onOpen`, `onText`, and `onBinary` are required. Ping, pong, close, and error callbacks have defaults; the server answers a ping even when the endpoint doesn't override its callback.

There is one endpoint instance per route, shared across connections. Put connection-specific state on `WebSocketSession`, using `setAttachment`, rather than in a field that every client will overwrite.

## The protocol our device tests use

The screenshot protocol is intentionally small:

{{< mermaid >}}
flowchart TD
    Meta[Device sends META text] --> Image[Device sends PNG binary]
    Image --> Receive[Backend receives the image]
    Receive --> Write[Validate name and write PNG]
    Write --> Ack[Backend sends ACK]
    Ack --> Next[Device continues to next test]
{{< /mermaid >}}

The metadata identifies the capture. The binary message carries the image. An acknowledgment tells the runner when it can proceed. That exercises text and binary messages, connection lifetime, and the application-level ordering we actually depend on.

The current receiver is [Cn1ssScreenshotServer in the backend demo tree](https://github.com/codenameone/CodenameOne/blob/master/vm/backend/demo/cn1ss/com/demo/Cn1ssScreenshotServer.java). [The runner helper](https://github.com/codenameone/CodenameOne/blob/master/scripts/lib/cn1ss.sh) selects the transport implementation. We retain the earlier standalone server for the native Windows screenshot path, which doesn't use this backend arm.

That scope matters. We are using the backend in our primary screenshot infrastructure; we haven't replaced every host-side test tool. The benefit is still direct: fixes to the public WebSocket implementation are exercised by a workload producing artifacts we inspect.

## A connection holds resources longer than a request

An HTTP handler usually returns and releases its execution slot. An idle WebSocket remains open.

On supported native backend builds, the virtual-thread path can park a connection without dedicating an operating-system thread to its entire lifetime. The local JVM development loop uses the thread pool. TLS also uses pool mode in the current implementation. In pool mode, one connection occupies one worker, so `workers` must account for long-lived connections as well as ordinary requests.

The timeouts reflect that difference:

```bash
# Allow a connection to remain idle for five minutes.
export CN1_WS_IDLE_TIMEOUT_MS=300000

# Bound the size of a reassembled message.
export CN1_WS_MAX_MESSAGE_MB=2
```

Five minutes is the default idle timeout. Zero disables it. The message limit bounds what a peer can ask the server to reassemble. Set both for the workload; a mostly silent dashboard and an image receiver have different needs.

`getMetrics()` reports open connections in `webSocketConnections` separately from `activeRequests`. Otherwise an idle server with live subscriptions would look as though it had permanently unfinished HTTP requests. [Yesterday's tracing article](/blog/follow-a-tap-with-opentelemetry/) explains why these connections don't automatically become one span each.

## Sending has back pressure

The server delivers complete messages to callbacks. Fragmented binary messages are reassembled, and text is checked as UTF-8 before decoding. The byte array passed to `onBinary` is valid only until the callback returns. Copy the relevant range if another task needs to retain it.

Callbacks run on the connection's owning thread. A slow callback stops further reads for that connection. Sends may originate from another thread, and each session serializes its writers so two sends can't interleave message fragments.

But `sendText` and `sendBinary` write to the socket. They can block if the peer stops reading. A broadcast loop that writes to every client in sequence can therefore stall behind one slow client. Use bounded application work queues and an appropriate sending pool when broadcasting; don't replace the socket's back pressure with an unbounded list of pending messages.

## What using it ourselves does and doesn't prove

Our screenshot receiver exercises real image traffic. It doesn't prove every hostile frame is handled, so the implementation also has protocol tests and a conformance harness. It doesn't prove a production connection limit either; that depends on TLS, the execution mode, message sizes, and what the handler does.

The current server supports RFC 6455 over HTTP/1.1. It doesn't provide `permessage-deflate` or HTTP/2 extended CONNECT. Those are concrete limits to check before placing it behind infrastructure with different expectations.

When the server stops, it sends open connections a 1001 “going away” close before draining. Clients can treat that as a planned disconnect and reconnect according to their own policy. Authentication, authorization, and any replay of missed business events remain application concerns; an echo route is only the smallest runnable starting point.

[PR #5880](https://github.com/codenameone/CodenameOne/pull/5880) contains both the server API and the screenshot receiver. Try the endpoint, then give it a client that stops reading. The second test will teach you more about the design of a live application than the first successful echo.

---

## Discussion

_Where would server-pushed updates replace polling in your app?_

{{< giscus >}}
