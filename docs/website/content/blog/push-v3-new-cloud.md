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

Next week we plan to bring down the old push service and direct `push.codenameone.com` traffic to the new implementation. The compatibility endpoint accepts the existing request format, so the switch should be seamless. It is still a completely new server, and “should” is not a test result. Please test before the cutover while both routes are easy to compare.

## TL;DR

- [Push V3](#test-the-new-push-server-now): Existing push code can test `cloud.codenameone.com` today. V3 adds typed messages, managed credentials, segments, campaigns, analytics, and Surface commands.
- [Plans and privacy](#what-each-plan-includes): Sending works on every Codename One plan. Higher plans increase quotas and add persistent campaign tools. Credentials are encrypted, public registration cannot assign identity or tags, and application data stays isolated.
- [Health](#health-data-without-fake-certainty): A major HealthKit, Health Connect, workout, nutrition, and Bluetooth sensor API is in review. Its odd-looking rules preserve distinctions the platforms cannot safely hide.
- [AI and MCP](#on-device-ai-and-mcp-on-every-port): The core now exposes on-device vision, language, and LiteRT inference. MCP can debug applications across loopback-capable ports, with a release-build gate because loopback is local, not private.
- [Routing](#a-polyline-is-not-a-route): The new maps routing API turns coordinates into road-following routes through OSRM or a custom service.
- [Maven repository](#phase-one-of-our-maven-repository-move): We are starting a staged move from Maven Central to Cloudflare R2. Central remains authoritative during phase one, and generated projects will start using the new repository next week.

## Test the new push server now

If your server currently sends through the classic endpoint, keep the request exactly as it is and change only the host:

```diff
-https://push.codenameone.com/push/push
+https://cloud.codenameone.com/push/push
```

Send to test devices on every platform your application supports. Exercise a visible notification, a data payload, a cold start, and any badge, sound, category, image, or deep-link behavior you use. Compare the result with the old host and [open an issue](https://github.com/codenameone/CodenameOne/issues) if the two disagree.

The new server contains a classic compatibility layer. Existing applications do not need to adopt the Java V3 client or the new REST API before the hostname switch. That separation matters: validating the new transport is a small operational change, while adopting the V3 model is an application change you can schedule.

{{< mermaid >}}
flowchart LR
    A["Existing server code"] --> B["Classic /push/push request"]
    B --> C["cloud.codenameone.com compatibility layer"]
    C --> D["Durable delivery queue"]
    D --> E["APNs"]
    D --> F["FCM"]
    D --> G["Huawei"]
    D --> H["WNS and Web Push"]
{{< /mermaid >}}

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

The client is explicit too:

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

This connection is important for both architecture and monitoring. A campaign can target a segment once, then update the user-facing surface that is appropriate on each platform. The server records provider outcomes without pretending that every destination has the same lifecycle.

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

## Health data without fake certainty

[PR #5475](https://github.com/codenameone/CodenameOne/pull/5475) is in review with a first-class API for HealthKit, Health Connect, recorded workouts, nutrition, and eight adopted Bluetooth health sensor profiles.

Its most important design choice is refusing to invent certainty. HealthKit does not reveal whether read access was denied, so the API has no misleading `hasReadPermission()` method. An empty aggregate remains `null`, not zero. Calendar-day buckets require a time zone. Overlapping phone and watch sources remain visible instead of being silently guessed away.

{{< post-link path="/blog/health-api-false-certainty" text="Read the Health article for the package model, platform matrix, authorization trap, change cursors, workouts, sensors, simulator, build configuration, and compliance boundaries." >}}

## On-device AI and MCP on every port

[PR #5467](https://github.com/codenameone/CodenameOne/pull/5467) brings vision, language, and LiteRT inference into the core. OCR, barcode recognition, pose detection, document correction, language identification, translation, smart reply, and application-owned `.tflite` models now share one selective API.

[PR #5472](https://github.com/codenameone/CodenameOne/pull/5472) adds a loopback socket transport for MCP on mobile and desktop ports. An agent can inspect the semantic UI tree, find a component, activate it, set text, and reproduce a path through the real application.

Loopback is safer than listening on every network interface, but any local process can still try to connect. MCP therefore refuses to start in a release build by default. That guard is as important as the debugging feature.

{{< post-link path="/blog/on-device-ai-mcp-loopback" text="Read the AI and MCP article for platform backends, model verification, the semantic debugging loop, and the loopback threat boundary." >}}

## A polyline is not a route

A map polyline joins the coordinates you already have. It cannot discover the roads between two points.

[PR #5480](https://github.com/codenameone/CodenameOne/pull/5480) adds `com.codename1.maps.routing`, with road geometry, distance, duration, legs, steps, waypoints, alternatives, bounds, and encoded-polyline support. `Routing.showRoute(...)` can fetch, draw, and frame the best route, while a `RouteService` SPI lets production applications choose their own provider.

{{< post-link path="/blog/road-following-map-routing" text="Read the routing article for the two-line path, custom styling, OSRM limits, travel modes, and provider SPI." >}}

## Phase one of our Maven repository move

We are also starting [phase one of a move from Maven Central to a Codename One repository on Cloudflare R2](https://github.com/codenameone/CodenameOne/pull/5497).

Maven Central has every right to set commercial usage limits and charge for infrastructure. Codename One also has a workload that is difficult to fit inside those limits. One release currently publishes enough duplicated fat-jar content that our dashboard reports 2.12 GB against an 80 MB storage guideline, 19,962 files against 1,000, and 27 releases against 7.

This week we are reducing a measured release payload from 229.5 MB to 76.9 MB and adding dual publication. Central remains authoritative. Next week generated projects and Initializr will start including `https://repo.codenameone.com/maven2`. Three weeks after that, if the dual-publish period is clean, new Codename One versions will stop going to Central.

Existing versions on Central remain there. The new repository will guarantee at least six months of version history. At the current release rate and optimized payload, the measured capacity is closer to 1.6 years.

{{< post-link path="/blog/maven-central-cloudflare-r2" text="Read the repository migration article for the timeline, POM change, payload audit, R2 release safeguards, retention policy, and why this is not an attack on Maven Central." >}}

---

## Discussion

_Test the new push host, then tell us what happened via GitHub Discussions._

{{< giscus >}}
