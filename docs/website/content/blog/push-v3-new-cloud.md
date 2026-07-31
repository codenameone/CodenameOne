---
title: "Push V3: One Message From Your Server to Every Surface"
slug: push-v3-new-cloud
url: /blog/push-v3-new-cloud/
date: '2026-07-31'
author: Shai Almog
description: "Codename One Push V3 adds typed messages, managed credentials, segmentation, analytics, and Surface updates. Existing push apps should test the new cloud endpoint before next week's cutover."
feed_html: '<img src="https://www.codenameone.com/blog/push-v3-new-cloud.jpg" alt="Push V3 connects one typed message to phones, widgets, and live surfaces" /> Codename One Push V3 adds typed messages, managed credentials, segmentation, analytics, and Surface updates. Existing push apps should test the new cloud endpoint before next week''s cutover.'
series: ["release-2026-07-31"]
---

![Push V3 connects one typed message to phones, widgets, and live surfaces](/blog/push-v3-new-cloud.jpg)

Push notifications should be application infrastructure, not a pile of expiring certificates and provider-specific JSON.

This week we merged [Push V3](https://github.com/codenameone/CodenameOne/pull/5440) into the Codename One core and completed its new cloud implementation. It gives an application a typed message model, managed provider credentials, subscriptions, server-side segments, campaigns, analytics, and a direct path into [Surfaces](/blog/widgets-live-activities-dynamic-island/).

There is also one thing every existing push developer should do now:

> Change the push service URL from `https://push.codenameone.com` to `https://cloud.codenameone.com` and send a real notification through your existing code.

Next week we plan to bring down the old push service and direct `push.codenameone.com` traffic to the new implementation. The compatibility endpoint accepts the existing request format, so existing code should keep working. It is still a completely new server, and “should” is not a test result. Please test before the cutover while both routes are easy to compare.

## TL;DR

Push is the lead story today. Over the next four posts, I will unpack the other changes in this release:

- [Our own Maven repository](#phase-one-of-our-maven-repository-move): We are moving Codename One releases to `repo.codenameone.com`. New projects get the setting automatically next week. Existing projects will need a short POM block before new versions stop appearing on Maven Central.
- [On-device AI and MCP](#on-device-ai-and-mcp-on-every-port): The core now has portable OCR, vision, language, and LiteRT APIs. The MCP server can inspect and operate a real application on mobile ports through loopback, with a release-build guard because another local process can also reach that socket.
- [Health data](#health-data-without-fake-certainty): A new API covers HealthKit, Health Connect, workouts, nutrition, eight Bluetooth health sensor profiles, and a deterministic simulator. The design preserves denied reads, missing values, source overlap, and compliance boundaries instead of flattening them into convenient answers.
- [Road-following map routes](#a-polyline-is-not-a-route): `Routing.showRoute(...)` can now turn two coordinates into road geometry, distance, duration, legs, and steps. OSRM provides the default test path, while `RouteService` keeps production provider choice in application code.

## Test the new push server now

If your server currently sends through the classic endpoint, keep the request exactly as it is and change only the host:

```diff
-https://push.codenameone.com/push/push
+https://cloud.codenameone.com/push/push
```

Send to test devices on every platform your application supports. Exercise a visible notification, a data payload, a cold start, and any badge, sound, category, image, or deep-link behavior you use. Compare the result with the old host and [open an issue](https://github.com/codenameone/CodenameOne/issues) if the two disagree.

The new server contains a classic compatibility layer. Existing applications do not need to adopt the Java V3 client or the new REST API before the hostname switch. That separation matters: validating the new transport is a small operational change, while adopting the V3 model is an application change you can schedule.

The queue records a provider response for each target. “Accepted” means APNs, FCM, or another provider accepted the request. It does not prove that the operating system displayed the notification, that the user saw it, or that the application opened. The console keeps those states separate because a comforting number with the wrong definition is worse than no number.

## V3 makes the message a real type

The classic API encoded behavior into numeric push types and positional strings. It worked, but it made provider differences and new destinations increasingly hard to express.

V3 uses an immutable schema:

```java
PushMessage message = PushMessage.builder()
        .title("Boarding changed")
        .body("Flight CN1 42 now leaves from gate C7")
        .deepLink("myapp://trip/CN142")
        .data("tripId", "CN142")
        .ttlSeconds(900)
        .build();
```

The same envelope can carry visible content, application data, an image, a deep link, collapse and lifetime rules, provider-specific options, and a `surface` command. Incoming messages are parsed before reaching application code, exposed through immutable maps, and rejected when their schema is unsupported.

V3 also replaces the old `PushCallback` contract. Your main application class no longer implements the push interface. You create a `PushClient` and give it a `PushListener`:

| Previous API | V3 API |
| --- | --- |
| `PushCallback.push(String value)` | `PushListener.onMessage(PushMessage message)` |
| `registeredForPush(String deviceId)` | `onRegistration(PushSubscription subscription)` |
| `pushRegistrationError(...)` | `onError(PushError error)` |

This is more than a method rename. The listener receives a parsed `PushMessage`, registration returns a subscription object, and errors carry a code plus retry information.

```java
private PushClient push;

public void init(Object context) {
    push = PushClient.builder("APP_KEY_FROM_CONSOLE")
            .listener(new PushListener() {
                public void onMessage(PushMessage message) {
                    Log.p("Push: " + message.getTitle());
                }

                public void onRegistration(PushSubscription subscription) {
                    Log.p("Registered " + subscription.getTransportId());
                }

                public void onError(PushError error) {
                    Log.p(error.getCode() + ": " + error.getMessage());
                }
            })
            .build();
}

public void start() {
    push.register();
}
```

Create one client in `init()`, retain it, and call `register()` from `start()`. Registration is idempotent. Do not unregister from `stop()`, because that removes the subscription rather than pausing it.

Applications that run their own push infrastructure are not trapped behind the managed service. `PushTransport` is a public seam for custom registration and delivery, while `PushRegistrationSink` lets an application mirror registration changes to its own backend.

## One push can update a notification or a Surface

A lock-screen notification is only one destination. Widgets, Live Activities, the Dynamic Island, watch complications, and other [Surfaces](/blog/widgets-live-activities-dynamic-island/) also need fresh state.

V3 reserves a typed `surface` object in the same envelope. Native bootstrap code can route a Surface command before the main application UI is running.

{{< mermaid >}}
flowchart TB
    A["Push V3 envelope"] --> B{"Payload kind"}
    B --> C["Visible notification"]
    B --> D["Application data"]
    B --> E["Surface command"]
    E --> F["Widget timeline"]
    E --> G["Live Activity"]
    E --> H["Dynamic Island or complication"]
    C --> I["PushListener on the Codename One EDT"]
    D --> I
{{< /mermaid >}}

Consider a delivery application that is not running while the customer waits for a courier. A server push can update its Live Activity and Dynamic Island to say “Driver is 2 minutes away” without launching the main Codename One UI. On a platform without that Surface, the same campaign can deliver a normal notification instead.

The message view still shows whether APNs or another provider accepted each update. That is useful when the Surface changes while no `PushListener` is running inside the application.

## The certificate stops being your server's problem

The old arrangement often made an application team generate a push certificate, place it on its own server, watch its expiry date, and repeat the process. That is fragile infrastructure disguised as setup.

The new console stores provider credentials for each application and environment. APNs can use a `.p8` signing key, which does not have the annual expiry cycle of the old certificate workflow. The push service signs provider requests and isolates credentials from campaign users.

![Push application settings and provider credentials](/blog/push-v3-new-cloud/push-v3-console-settings.png)

Credentials are encrypted at rest and treated as write-only secrets in the console. Reading application settings does not return the secret value. This removes certificate hosting from your application server, but it does not remove normal secret hygiene: use a narrowly scoped provider key, rotate it when a team member or system boundary changes, and separate production from development.

## Segmentation without handing identity to a device

The console separates applications, environments, subscriptions, audiences, messages, campaigns, and analytics.

![Push applications, environments, and operational state in the console](/blog/push-v3-new-cloud/push-v3-console-overview.png)

A device can register its provider token through the public client endpoint. It cannot declare an external user identity or attach arbitrary tags to itself. Those operations require the authenticated server API. Otherwise a modified client could simply label itself `premium`, `administrator`, or `patient-high-risk` and enter a segment it did not belong in.

![A saved push audience built from server-assigned subscription data](/blog/push-v3-new-cloud/push-v3-console-audience.png)

Saved segments are evaluated on the server against application-scoped subscription data. A segment might select a locale, application version, platform, or a tag assigned by your backend. The audience is resolved when the message is sent, so a corrected tag does not require rebuilding a static mailing list.

This is segmentation for application behavior, not an advertising profile. Codename One does not sell the subscription data or combine it across customers. The service still has to retain what delivery requires: provider tokens, installation and optional external identifiers, server-assigned tags, message payloads, target status, and provider responses.

Never place a password, access token, medical result, or other secret in a notification payload. Providers and operating systems participate in delivery, lock screens can expose visible text, and notification data may outlive the screen where you intended to show it.

## Monitoring that answers operational questions

The new message view exposes queued, accepted, failed, and dead targets, including provider error information.

![Per-message push state and provider outcomes](/blog/push-v3-new-cloud/push-v3-console-messages.png)

This makes several operational checks possible:

- Is the queue moving?
- Did one provider fail while the others accepted the message?
- Are stale device tokens being removed?
- Did a rate limit delay a large audience?
- Which environment and campaign produced this message?

Analytics are retained for 30 days. They are operational delivery analytics, not proof of attention. Application opens or business outcomes still belong in consent-aware product analytics under your control.

## What each plan includes

Push sending and managed provider credentials are available on every subscription level, including Free. The plans differ in monthly volume, rate limits, and persistent campaign tooling:

| Plan | Monthly deliveries per seat | Requests per minute | Recipients per minute | Persistent audiences and campaigns | Automation |
| --- | ---: | ---: | ---: | --- | --- |
| Free | 1,000 | 30 | 100 | No | No |
| Basic | 5,000 | 120 | 1,000 | No | No |
| Pro | 1,000,000 | 600 | 10,000 | Yes | No |
| Enterprise | 10,000,000 | 3,000 | 100,000 | Yes | Yes |

Free and Basic applications can send through the same durable provider pipeline. Pro adds saved templates, segments, campaigns, and analytics. Enterprise adds automation and higher operational limits. Quotas are organization and seat aware, so a team can see which allowance a notification run consumes.

These numbers are the initial policy, not a claim that every application needs a million notifications. Start with a small, explicit audience. A precise notification that helps 200 people is better than a vague blast that trains 200,000 people to turn notifications off.

## Phase one of our Maven repository move

We have also merged [phase one of a move from Maven Central to a Codename One repository on Cloudflare R2](https://github.com/codenameone/CodenameOne/pull/5497).

Maven Central has every right to set commercial usage limits and charge for infrastructure. Codename One also has a workload that is difficult to fit inside those limits. One release currently publishes enough duplicated fat-jar content that our dashboard reports 2.12 GB against an 80 MB storage guideline, 19,962 files against 1,000, and 27 releases against 7.

![Maven Central publishing usage shows Codename One far beyond the soft guidelines](/blog/maven-central-cloudflare-r2/maven-central-limit.png)

Those limits are soft guidelines, and the dashboard offers both open-source adjustments and a commercial plan. We are not leaving because Sonatype is doing something wrong. We are leaving because our weekly, multi-platform release shape is expensive to host there, while we can provide the same Maven layout free to users on infrastructure that fits it better.

Phase one reduced a measured release payload from 229.5 MB to 76.9 MB. The release pipeline now copies the signed Central staging tree to R2, so it does not perform a second build with potentially different bytes.

{{< mermaid >}}
flowchart LR
    A["July 31<br/>Shrink and dual publish"] --> B["August 7<br/>Generated POMs use R2"]
    B --> C["Three-week observation window"]
    C --> D["August 28<br/>New releases on R2 only"]
    A --> E["Maven Central remains authoritative"]
    B --> E
    E --> F["Existing Central versions remain available"]
{{< /mermaid >}}

Existing projects can prepare for post-cutover versions by adding the repository to both Maven resolution paths:

```xml
<repositories>
    <repository>
        <id>codenameone</id>
        <url>https://repo.codenameone.com/maven2</url>
    </repository>
</repositories>

<pluginRepositories>
    <pluginRepository>
        <id>codenameone-plugins</id>
        <url>https://repo.codenameone.com/maven2</url>
    </pluginRepository>
</pluginRepositories>
```

The second block matters. Maven resolves build plugins separately from ordinary dependencies. Adding only `<repositories>` can leave a future `codenameone-maven-plugin` version undiscoverable.

New projects receive this automatically on August 7. Existing versions remain on Central. The new repository guarantees at least six months of history, while the optimized payload currently fits about 1.6 years at the recent release rate.

R2 object storage and Cloudflare's edge should improve dependency resolution and remove Central throttling from our release path. We also expect CI and releases to become faster and more stable. Those are expectations we will measure during dual publication, not results we have already proved.

{{< post-link path="/blog/maven-central-cloudflare-r2" text="The Maven article publishes on August 4 with the full payload audit, cutover plan, R2 release safeguards, retention policy, and failure modes we are testing during dual publication." >}}

## On-device AI and MCP on every port

[PR #5467](https://github.com/codenameone/CodenameOne/pull/5467) brings vision, language, and LiteRT inference into the core. The API covers OCR, barcode recognition, face detection, image labels, pose detection, selfie segmentation, document correction, language identification, translation, smart reply, and application-owned `.tflite` models.

The public surface stays portable, but the work remains on the device. Android uses ML Kit for higher-level operations. Apple ports use Vision, Core Image, and Natural Language where they fit. Unsupported ports report an unsupported capability instead of silently uploading input to a cloud fallback.

OCR is deliberately asynchronous because native conversion and recognition cannot block the Codename One EDT:

```java
TextRecognizer recognizer = new TextRecognizer();
recognizer.process(VisionImage.encoded(jpegBytes))
        .ready(result -> textArea.setText(result.getText()))
        .except(error -> Log.e(error));
```

Inference sessions keep an application-owned model loaded across multiple runs. Runtime model downloads can use `ModelCache`, which requires HTTPS and verifies the SHA-256 digest before activating the file. A model changes application behavior, so accepting unverified model bytes would be a software supply chain bug.

[PR #5472](https://github.com/codenameone/CodenameOne/pull/5472) takes the existing semantic MCP server beyond JavaSE. On loopback-capable ports, an LLM can read the component tree, find a button by semantic identity, set text, activate an action, and inspect the resulting state in the actual application.

{{< mermaid >}}
sequenceDiagram
    participant Dev as Developer and LLM client
    participant Port as Device port forward
    participant MCP as 127.0.0.1 MCP server
    participant UI as Codename One EDT
    Dev->>Port: Connect to debug device
    Port->>MCP: Forward port 8642
    Dev->>MCP: ui_snapshot
    MCP->>UI: Read semantic component tree
    UI-->>Dev: Roles, text, state, and actions
    Dev->>MCP: Activate semantic target
    MCP->>UI: Perform component action
    UI-->>Dev: Updated state
{{< /mermaid >}}

This replaces coordinate guessing with application semantics. It also creates a control channel. Binding to `127.0.0.1` prevents accidental exposure to the local network, but it does not authenticate other processes on the device or workstation.

```java
if (Display.getInstance().isDebuggableBuild()) {
    MCP.startSocketServer(8642);
}
```

MCP refuses to start in a release build by default. An explicit override exists for controlled test labs, but it should not become a convenience flag in a consumer application.

{{< post-link path="/blog/on-device-ai-mcp-loopback" text="The AI and MCP article publishes on August 2 with the capability matrix, portable inference model, semantic debugging loop, and the reasons loopback still needs a release-build gate." >}}

## Health data without fake certainty

[PR #5475](https://github.com/codenameone/CodenameOne/pull/5475) adds a first-class API for HealthKit, Health Connect, recorded workouts, sparse nutrition data, deterministic simulation, and eight adopted Bluetooth health sensor profiles.

The public API is divided at the boundaries an application needs to reason about:

| Package | Responsibility |
| --- | --- |
| `com.codename1.health` | Stores, permissions, samples, queries, aggregates, sources, and change cursors |
| `com.codename1.health.workout` | Recorded workout sessions, events, configuration, and collected samples |
| `com.codename1.health.sensors` | Live standard Bluetooth health devices without requiring a phone health store |
| `com.codename1.health.nutrition` | Sparse nutrient records where an absent value remains absent |

Simulator, desktop, and JavaScript builds return `LOCAL_ONLY`. That is a supported store with reads and writes, not a missing provider. Only Android provider failures should send the user to provider setup:

```java
Health health = Health.getInstance();
HealthAvailability availability = health.getAvailability();
if (availability == HealthAvailability.PROVIDER_NOT_INSTALLED
        || availability == HealthAvailability.PROVIDER_UPDATE_REQUIRED) {
    health.openProviderSetup();
    return;
}
if (availability == HealthAvailability.NOT_SUPPORTED) {
    return;
}

HealthStore store = health.getStore();
```

{{< mermaid >}}
flowchart TB
    A["Application"] --> B["Health"]
    B --> C["HealthStore"]
    B --> D["WorkoutManager"]
    B --> E["HealthSensors"]
    C --> F["HealthKit"]
    C --> G["Health Connect"]
    C --> H["Local and simulated store"]
    E --> I["Eight Bluetooth LE profiles"]
    I --> D
    I --> C
{{< /mermaid >}}

Some of the API looks cautious because the platform contracts are cautious. HealthKit does not reveal whether a user denied read access. A completed authorization sheet means the user was asked, not that the application can read the category. The shared API therefore has no `hasReadPermission()` method that would lie on iOS.

The API preserves these distinctions throughout the model. An empty aggregate returns `null`, not zero. Calendar-day buckets require a time zone. Phone and watch sources remain distinguishable because silently adding overlapping samples can double-count a walk. Unsupported phone mappings fail with `TYPE_NOT_SUPPORTED` instead of returning an empty collection that looks successful.

The simulator includes a mode that grants writes while denying reads without an error. That lets you test the UI mistake that a permissive fake would miss: accusing a user of denying access when the only accurate statement is “no data available.”

This API does not make an application HIPAA compliant. It never uploads health data, and it can enforce specific purpose strings and reject unsupported writes. The application still owns access control, encryption, audit records, retention, consent, breach handling, backend contracts, and store disclosures.

{{< post-link path="/blog/health-api-false-certainty" text="The Health article publishes on August 1 with the platform matrix, authorization trap, sample model, change cursors, workouts, Bluetooth sensors, simulator failure modes, build configuration, and HIPAA boundary." >}}

## A polyline is not a route

A map polyline joins coordinates that already exist. It cannot discover the roads, travel time, maneuvers, or alternate paths between them.

[PR #5480](https://github.com/codenameone/CodenameOne/pull/5480) adds `com.codename1.maps.routing`. The smallest useful path is two coordinates:

```java
MapView map = new MapView();
Routing.showRoute(
        map,
        new LatLng(38.8977, -77.0365),
        new LatLng(38.8894, -77.0352)
);
```

The call returns immediately. The active `RouteService` finds a route, then the API draws its geometry and frames the map on the Codename One EDT.

{{< mermaid >}}
flowchart LR
    A["Origin, destination, and waypoints"] --> B["RouteRequest"]
    B --> C["RouteService"]
    C --> D["Road network calculation"]
    D --> E["Route alternatives"]
    E --> F["Geometry and bounds"]
    E --> G["Distance, duration, legs, and steps"]
    F --> H["MapSurface"]
    G --> I["Application UI"]
{{< /mermaid >}}

The default service is OSRM, so the first driving route needs no provider signup. It points to the public OSRM demonstration server, which has no production SLA and uses a car profile. A `WALKING` request does not turn that graph into a pedestrian route.

Production applications can point `OsrmRouteService` at their own server or install another provider:

```java
Routing.setService(new OsrmRouteService(
        "https://routing.example.com"
));
```

The application consumes portable `Route` objects rather than provider JSON. Those objects carry alternatives, distance, duration, bounds, geometry, legs, step instructions, maneuver locations, and provider metadata.

This is routing, not turn-by-turn navigation. The release does not claim rerouting, traffic prediction, offline map packages, or voice guidance. Those features need location updates, lifecycle state, and provider-specific rules.

{{< post-link path="/blog/road-following-map-routing" text="The routing article publishes on August 3 with custom route styling, ETA handling, encoded polyline support, OSRM limits, travel modes, provider replacement, and the boundary between a route result and navigation." >}}

Tomorrow I will start taking these changes one at a time with the new Health API. For today, please send a real notification through `cloud.codenameone.com`. This is the one week when you can compare the old and new push implementations side by side. If anything behaves differently, [open an issue](https://github.com/codenameone/CodenameOne/issues) before we move the traffic next week.

---

## Discussion

_Test the new push host, then tell us what happened via GitHub Discussions._

{{< giscus >}}
