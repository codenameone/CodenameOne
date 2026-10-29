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

Bastion looks up prices on every quote. Calling the price feed on every quote is slow and burns the feed's quota. Caching forever shows stale prices. The new [cache](/developer-guide/backend-observability/#_application_caches) has a size bound, expiry, a removal listener and metrics:

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

Your server calls other servers: a Google API with a service account, a billing service with client credentials. [`AccessTokenProvider`](/developer-guide/backend-security/#backend-security-outbound) fetches the token, caches it and refreshes it before it expires:

```java {class="cn1-no-try"}
AccessTokenProvider billing(Config config) throws IOException {
    return new AccessTokenProvider(ClientCredentialsGrant
            .builder("https://auth.example.com/oauth/token", "orders-service")
            .clientSecret(config.get("billing.client-secret"))
            .scopes("invoices:write")
            .build());
}
```

The same change added [importing users from an existing identity store](/developer-guide/backend-security/#backend-security-migration), so moving an app's accounts over doesn't mean asking everyone to reset a password.

## Certificates without OpenSSL on the command line

Banks and partners love mutual TLS and private certificate authorities. The [PKI API](/developer-guide/backend-security/#backend-security-pki) parses PEM, validates a chain against your own anchors, creates certificate signing requests and reads and writes PKCS#12:

```java {class="cn1-no-try"}
List<Certificate> anchors = Certificate.listFromPem(KeyFiles.read("/etc/app/partner-ca.pem"));
List<Certificate> path = new CertificateValidator(anchors)
        .forHost("api.partner.example")
        .validate(Certificate.listFromPem(presentedPem));
```

## Errors in one place

[`@RestControllerAdvice` and `@ExceptionHandler`](/developer-guide/backend-web/#backend-web-errors) turn exceptions into responses once, instead of a try and catch in every handler. [Interceptors](/developer-guide/backend-web/#backend-web-interceptors) and [validation](/developer-guide/backend-web/#backend-web-validation) came with the same change.

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

Reel's uploads are video files. Reading them into memory isn't an option. [`request.getBodyStream()`](/developer-guide/backend-web/#_large_uploads_and_downloads) hands you the body as it arrives, and large uploads, multipart ones included, spool to disk instead of the heap:

```java {class="cn1-no-try"}
@PostMapping("/archive")
public Map<String, Long> archive(HttpServer.Request request) throws IOException {
    long total = 0;
    byte[] buffer = new byte[64 * 1024];
    InputStream in = request.getBodyStream();
    try {
        for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
            total += n;                 // store, hash or forward each block
        }
    } finally {
        in.close();
    }
    return Collections.singletonMap("bytes", total);
}
```

[`HttpProxy`](/developer-guide/backend-web/#_forwarding_to_another_service) forwards a request to an internal service. It reaches only its target and the hosts you allow, so a crafted path can't turn your server into an open proxy:

```java {class="cn1-no-try"}
images = new HttpProxy("https://images.internal:8443")
        .allowHost("cdn.internal");

@RequestMapping("/images/{name}")
public HttpServer.Response image(HttpServer.Request request) {
    return images.forward(request);  // the same path and query, upstream
}
```

## JSON codecs and java.time in the ORM

These two apply to the client as well as the server. [JSON mapping](/developer-guide/backend-web/#backend-json-contract) gained custom codecs through `JsonTypeAdapter`, constructor based creators, generics, arrays and explicit null handling. A codec turns a type into JSON and back, here a money amount kept as a decimal string so it never passes through a `double`:

```java {class="cn1-no-try"}
public final class MoneyAdapter implements JsonTypeAdapter<Money> {
    public Object toJson(Money m) {
        return m == null ? null : m.toDecimalString();
    }
    public Money fromJson(Object json) {
        if (json == null) {
            return null;
        }
        if (!(json instanceof String)) {
            throw new IllegalArgumentException("expected an amount");
        }
        return Money.parse((String) json);
    }
}
```

Put `@JsonAdapter(MoneyAdapter.class)` on the field or the class and the mapper uses it.

The [ORM now maps `java.time`](/developer-guide/annotation-sqlite-orm/#orm-native-dates) without an annotation: `Instant` and `LocalDateTime` as epoch milliseconds and `LocalDate` as an epoch day, so a date column is a number you can index and compare:

```java {class="cn1-no-try"}
@Entity
public class Booking {
    @Id
    private long id;
    private Instant created;        // epoch milliseconds
    private LocalDate checkIn;      // epoch day
    private LocalDateTime pickup;   // epoch milliseconds
}
```

## Tests that start only what they need

[`@WebMvcTest`](/developer-guide/backend-testing/#_testing_one_slice_of_the_application) starts one controller and the beans you list, not the whole application, and `MockMvc` calls it without a socket:

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

The same change defined a [metric schema](/developer-guide/backend-observability/#_the_metric_schema) and [probes](/developer-guide/backend-observability/#_probes_and_draining) for observability, so a dashboard means the same thing for every Codename One service.

The backend is still a tech preview. The [backend chapters](/developer-guide/backend/) of the developer guide cover each of these, with the code that the snippets above come from.

## A simulator that types

Here's a bug you've probably shipped. The login form looks great in the simulator. On a phone, the keyboard slides up and covers the password field, or the layout jumps, and nobody noticed because the simulator never had a keyboard.

Now it [does](/developer-guide/mcp-headless-api/#_simulator_keyboard_touches_and_recording). **Simulate > Virtual Keyboard** slides up a keyboard styled after the skin's platform, and it behaves the way that platform does:

- **iOS:** the screen keeps its size, `Display.getInvisibleAreaUnderVKB()` reports the keyboard height, and the focused field scrolls into view.
- **Android, resize mode:** the form gets a `sizeChanged` and lays out again.
- **Android, pan mode:** the whole screen shifts up.

{{< guide-block >}}
<figure style="margin:24px 0"><video controls playsinline muted loop preload="metadata" poster="/blog/release-2026-10-30/simulator-virtual-keyboard-poster.jpg" width="540" height="960" style="width:100%;height:auto;max-width:320px;display:block;margin:0 auto"><source src="/blog/release-2026-10-30/simulator-virtual-keyboard.mp4" type="video/mp4">The simulated iOS keyboard sliding up over a sign up form.</video><figcaption>Recorded by the simulator itself, driven over MCP by the script below.</figcaption></figure>
{{< /guide-block >}}

By default Android picks a mode per field, the way the real platform does. The **Android: Resize** and **Android: Pan** menu items, or the `androidSoftInput` setting, force one so you can test both.

The simulator also records. It writes MP4, MOV, WebM or an animated GIF, of the screen alone or inside the device skin, on a fixed frame clock so the video doesn't stutter when your machine is busy. Taps show touch indicators and can play a click, and you can add captions. That's a store preview, a bug report or a tweet, without a screen recorder.

Both are also tools in the simulator's built in MCP server, so a coding agent can drive a whole flow and hand you the video:

```
sim_keyboard_set   {"mode": "ios"}
sim_record_start   {"path": "/tmp/signup.mp4", "frame": "screen"}
sim_record_caption {"text": "The keyboard slides up, the field stays visible"}
ui_snapshot        {}
sim_tap            {"nodeId": 41}
sim_keyboard_type  {"text": "ada@example.com"}
sim_keyboard_key   {"key": "next"}
sim_keyboard_type  {"text": "correct horse"}
sim_keyboard_key   {"key": "done"}
sim_record_stop
```

It's an emulation of each platform's keyboard behavior, not the native keyboard itself, so do a final pass on a device. But the covered password field shows up on your desk now, not in a one-star review. The [simulator keyboard and recording section](/developer-guide/mcp-headless-api/#_simulator_keyboard_touches_and_recording) of the MCP chapter lists every tool.

## That's the week

Flutter, Compose, Swing, JavaFX and Android code, a bank, a streaming service, phones without Google and a better backend. Next week, we keep going. The [Flutter post](/blog/flutter-better-in-java/) links everything from this week in one place.

{{< giscus >}}
