---
title: "Caches, Certificates, Streaming Uploads and a Simulator That Types"
slug: backend-caches-certificates-and-a-simulator-that-types
url: /blog/backend-caches-certificates-and-a-simulator-that-types/
date: '2026-11-05'
author: Shai Almog
description: "Eight backend upgrades driven by this month's templates: a bounded cache, outbound OAuth tokens, certificates and CSRs, JSON codecs, error advice, streaming uploads, java.time in the ORM and focused test slices. Plus a simulator that slides up a virtual keyboard and records video."
feed_html: "<img src=\"https://www.codenameone.com/blog/backend-caches-certificates-and-a-simulator-that-types.jpg\" alt=\"Backend code and the simulator with a virtual keyboard\" /> Eight backend upgrades, from a bounded cache to certificate validation and test slices, plus a simulator that slides up a virtual keyboard and records video."
series: ["release-2026-10-30"]
---

![Backend code and the simulator with a virtual keyboard](/blog/backend-caches-certificates-and-a-simulator-that-types.jpg)

The templates this month (a bank, a streaming service, a messenger, a delivery service and a game) all have servers, and every server ran into something a modern web app takes for granted. This post collects what we added to the Codename One backend to fill those gaps. Several of them apply to the client too. Then there's the simulator, which can now show a virtual keyboard and record a video of your app.

If you write Spring Boot during the day, most of this will look familiar on purpose. When there's no reason to differ, the backend behaves the way Spring does.

## A bounded cache

Bastion looks up prices on every quote. Calling the price feed every time is slow and rude; caching forever is wrong. The new cache has a size bound, expiry, a removal listener and metrics:

```java {class="cn1-no-try"}
private static final Cache<String, Price> PRICES = Cache.<String, Price>builder("prices")
        .maximumSize(10000)
        .expireAfterWriteMillis(60000)
        .removalListener((sku, price, cause) -> {
            if (cause == Cache.SIZE) {
                System.out.println("evicted " + sku);
            }
        })
        .build();
```

## Tokens for calling other services

Your server calls other servers: a Google API with a service account, a billing service with client credentials. `AccessTokenProvider` fetches the token, caches it and refreshes it before it expires:

```java {class="cn1-no-try"}
AccessTokenProvider billing(Config config) throws IOException {
    return new AccessTokenProvider(ClientCredentialsGrant
            .builder("https://auth.example.com/oauth/token", "orders-service")
            .clientSecret(config.get("billing.client-secret"))
            .scopes("invoices:write")
            .build());
}
```

The same change added importing users from an existing identity store, so moving an app's accounts over doesn't mean asking everyone to reset a password.

## Certificates without OpenSSL on the command line

Banks and partners love mutual TLS and private certificate authorities. The PKI API parses PEM, validates a chain against your own anchors, creates certificate signing requests and reads and writes PKCS#12:

```java {class="cn1-no-try"}
List<Certificate> anchors = Certificate.listFromPem(KeyFiles.read("/etc/app/partner-ca.pem"));
List<Certificate> path = new CertificateValidator(anchors)
        .forHost("api.partner.example")
        .validate(Certificate.listFromPem(presentedPem));
```

## Errors in one place

`@RestControllerAdvice` and `@ExceptionHandler` turn exceptions into responses once, instead of a try and catch in every handler. Interceptors and validation came with the same change.

```java {class="cn1-no-try"}
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    public HttpServer.Response badInput(IllegalArgumentException e, HttpServer.Request request) {
        return HttpServer.Response.text(400, e.getMessage()).header("X-Error", "input");
    }
}
```

## Streaming bodies and a proxy

Reel's uploads are video files. Reading them into memory isn't an option. `request.getBodyStream()` hands you the body as it arrives, large uploads spool to disk, and `HttpProxy` forwards a request to an internal service, with an allow list of the hosts it may reach.

## JSON codecs and java.time in the ORM

These two apply to the client as well as the server. JSON mapping gained custom codecs through `JsonTypeAdapter`, constructor based creators, generics, arrays and explicit null handling. The ORM now maps `Instant` and `LocalDateTime` as epoch milliseconds and `LocalDate` as an epoch day, so a date column is a number you can index and compare.

## Tests that start only what they need

`@WebMvcTest` starts one controller and the beans you list, not the whole application, and `MockMvc` calls it without a socket:

```java {class="cn1-no-try"}
@WebMvcTest(value = GreetingApi.class, includes = Greetings.class)
class GreetingSliceTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void greetsWithOnlyTheControllerWired() throws Exception {
        mvc.perform(get("/greet/{name}", "Ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting").value("Hello, Ada"));
    }
}
```

The same change defined a metric schema and probes for observability, so a dashboard means the same thing for every Codename One service.

The backend is still a tech preview. The [backend chapters](/developer-guide/backend/) of the developer guide cover each of these, with the code that the snippets above come from.

## A simulator that types

Here's a bug you've probably shipped. The login form looks great in the simulator. On a phone, the keyboard slides up and covers the password field, or the layout jumps, and nobody noticed because the simulator never had a keyboard.

Now it does. **Simulate > Virtual Keyboard** slides up a keyboard styled after the skin's platform, and it behaves the way that platform does:

- **iOS:** the screen keeps its size, `Display.getInvisibleAreaUnderVKB()` reports the keyboard height, and the focused field scrolls into view.
- **Android, resize mode:** the form gets a `sizeChanged` and lays out again.
- **Android, pan mode:** the whole screen shifts up.

By default Android picks a mode per field, the way the real platform does. The **Android: Resize** and **Android: Pan** menu items, or the `androidSoftInput` setting, force one so you can test both.

The simulator also records. It writes MP4, MOV, WebM or an animated GIF, of the screen alone or inside the device skin, on a fixed frame clock so the video doesn't stutter when your machine is busy. Taps show touch indicators and can play a click, and you can add captions. That's a store preview, a bug report or a tweet, without a screen recorder.

Both are also tools in the simulator's built in MCP server, so a coding agent can drive a whole flow and hand you the video:

```
sim_keyboard_set   {"mode": "auto"}
sim_record_start   {"path": "/tmp/signup.mp4", "tapSound": true}
sim_record_caption {"text": "Sign up in one step"}
sim_tap            {"nodeId": "email"}
sim_keyboard_type  {"text": "ada@example.com"}
sim_keyboard_key   {"key": "next"}
sim_record_stop
```

It's an emulation of each platform's keyboard behavior, not the native keyboard itself, so do a final pass on a device. But the covered password field shows up on your desk now, not in a one star review. The [headless and MCP chapter](/developer-guide/mcp-headless-api/) lists every tool.

## That's the week

Flutter, Compose, Swing, JavaFX and Android code, a bank, a streaming service, phones without Google and a better backend. Next week, we keep going. The [Flutter post](/blog/flutter-better-in-java/) links everything from this week in one place.

{{< giscus >}}
