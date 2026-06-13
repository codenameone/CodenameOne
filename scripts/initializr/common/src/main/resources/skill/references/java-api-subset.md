# Java API Subset, IO, and Networking

Codename One does **not** ship with the full JDK. The Java code you write in `common/` is cross-compiled by ParparVM (iOS) and TeaVM (web), and runs on Android against a hand-curated JDK subset. If you call a class or method that isn't in the subset, the cloud build fails the **bytecode compliance check** (`cn1:bytecode-compliance`, which the `process-classes` phase runs automatically).

This document tells you (1) how to discover what *is* supported, and (2) where the IO and networking APIs differ from standard Java.

## How to discover the supported API

The supported API surface is defined by two artifacts that the Codename One Maven plugin resolves from Maven Central. The `bytecode-compliance` goal compares your compiled bytecode against both jars and fails on anything not present.

| Artifact | What's inside |
| --- | --- |
| `com.codenameone:java-runtime` | The JDK subset — every `java.*` / `javax.*` class and method CN1 supports. Use this jar as the **definitive** reference for "what JDK APIs can I call?" |
| `com.codenameone:codenameone-core` | The CN1 framework itself — `com.codename1.*`. |

Both land in the local Maven cache after a build, at:

```
~/.m2/repository/com/codenameone/java-runtime/<cn1.version>/java-runtime-<cn1.version>.jar
~/.m2/repository/com/codenameone/codenameone-core/<cn1.version>/codenameone-core-<cn1.version>.jar
```

To see what's actually supported, list the entries:

```bash
JR=$(ls ~/.m2/repository/com/codenameone/java-runtime/*/java-runtime-*.jar | tail -1)
unzip -l "$JR" | awk '{print $4}' | grep '\.class$' | sed 's|/|.|g;s|\.class$||' | sort -u | less
```

The output is the exhaustive list of supported classes. To check a specific method, open the class in `javap`:

```bash
javap -p -classpath "$JR" java.util.HashMap | less
```

Don't trust intuition — the JDK is big, and the CN1 subset is intentionally smaller. When in doubt, grep the jar.

If you violate the subset, `mvn -pl common compile` prints something like:

```
Compliance check failed with 3 forbidden API reference(s):
  java.nio.file.Files.readAllBytes(java.nio.file.Path) — not in Codename One Java Runtime API
```

That's the signal to substitute a CN1-supported alternative — see the IO section below.

The runtime authoritative reference is also published as JavaDoc: <https://www.codenameone.com/javadoc/>.

## What's broadly supported

Codename One's `java-runtime` jar contains a curated slice of the JDK. Roughly:

- **`java.lang`** — primitive boxes, `String`, `StringBuilder`, `Math`, `Number`, `Throwable`, `Object`, `Runnable`, `Thread` (but see threading note below), `Comparable`, etc. No `Class.forName(String)` style dynamic dispatch — class names are obfuscated. `System.currentTimeMillis()` and `System.nanoTime()` both work on every port — `nanoTime()` is now backed by a real monotonic clock across all ParparVM targets (iOS, JavaScript, native Windows), so it's safe for frame timing / elapsed-time measurement.
- **`java.io`** — `InputStream`, `OutputStream`, `Reader`, `Writer`, `ByteArrayInput/OutputStream`, `BufferedReader`, `BufferedWriter`, `DataInputStream`, `DataOutputStream`, `IOException`, `PrintStream`. Use these for stream-based IO over `Storage` / `FileSystemStorage` handles.
- **`java.util`** — `ArrayList`, `LinkedList`, `HashMap`, `LinkedHashMap`, `TreeMap`, `HashSet`, `LinkedHashSet`, `TreeSet`, `Collections`, `Iterator`, `Comparator`, `Arrays`, `Date`, `Calendar`, `TimeZone`, `Hashtable`, `Vector`, `Stack`, `Random`, `UUID`, `Properties`. Most idiomatic collection code Just Works.
- **`java.text`** — partial (`DecimalFormat`, `NumberFormat`, basic `MessageFormat`); **`java.text.SimpleDateFormat` is partial** — prefer `com.codename1.l10n.SimpleDateFormat`.
- **`java.time`** — partial (`LocalDate`, `LocalDateTime`, `Instant`, `Duration`); a few `DateTimeFormatter` pieces are missing.
- **`java.util.zip`** — **not** in the base subset; pull in via the `ZipSupport` cn1lib if you need it (see *Resource files are a flat namespace* below).
- **`java.util.function`** — basic functional interfaces (`Supplier`, `Consumer`, `Function`, `Predicate`, `BiFunction`, ...) — supports lambdas + method references.
- **`java.util.stream`** — basic `Stream`, `IntStream` pipelines; performance is OK but not optimized for large data.

When in doubt, list the jar (top of this file) and search the jar's class listing for the exact symbol you need.

## What's NOT supported (and what to use instead)

| Area | Status | Use instead |
| --- | --- | --- |
| `java.awt.*` / `javax.swing.*` | **Forbidden.** | `com.codename1.ui.*` — see `references/swing-comparison.md`. |
| `java.nio.file.*` (`Path`, `Files`, `Paths`) | **Forbidden.** | `com.codename1.io.FileSystemStorage` for files with hierarchy; `com.codename1.io.Storage` for the sandboxed app-private filesystem; `com.codename1.io.Preferences` for key/value config. |
| `java.net.http.*`, `java.net.URLConnection`, `java.net.Socket` | **Forbidden** on cross-build targets. | `com.codename1.io.ConnectionRequest` / `NetworkManager` / `Rest` (recommended). A small `URLConnection`-style subset is also exposed as inner classes of `com.codename1.io.URL` (`URL.openConnection()` returns a `URL.URLConnection` / `URL.HttpURLConnection`) — useful when you specifically need a `URLConnection`-shaped API for porting. |
| `java.util.concurrent.locks.*`, Fork/Join, complex `Executor`s | Mostly **forbidden**. | `synchronized` blocks; `Display.callSerially(...)`, `Display.callSeriallyAndWait(...)`, `Display.startThread(...)`. |
| `java.lang.reflect.*` | **Forbidden everywhere** — not in the bootclasspath. Won't compile against the CN1 subset; it does not "work in the simulator". | Use the `com.codename1.properties` framework for property-style binding. For per-platform behavior write a native interface (see `references/native-interfaces.md`). |
| `Class.getName()` / `Class.forName(String)` | The two methods compile, but CN1 **obfuscates class names by default** in iOS / Android / JavaScript cross-builds. Names you see at source time (`com.example.MyService`) are rewritten to short opaque tokens at package time. | Never persist or serialize a class name; never dispatch by string-matching class names. Reference the class **as a class literal** (`MyService.class`) — the obfuscator follows class literals correctly and re-points them at the renamed class. Look up by name only as a last resort, and accept that the lookup will fail in cross-builds. |
| `java.util.logging.*` | **Not supported.** | `com.codename1.io.Log` (`Log.p(message)`, `Log.e(throwable)`, `Log.sendLog()` to upload). |
| `java.util.regex` | **Not supported.** | For simple matching use `String.matches(...)` / `String.split(...)` / `String.replace(...)` — these are present and use a simplified pattern syntax under the hood. For real PCRE-style regex, look for a regex cn1lib or write the matcher by hand. |
| `java.lang.invoke.*`, `java.lang.module.*` | **Forbidden.** | Don't generate code at runtime. |
| `java.util.concurrent.atomic.AtomicLongArray`, `AtomicIntegerArray`, `AtomicReferenceArray`, `LongAdder`, `LongAccumulator` | **Forbidden** in the runtime subset. `AtomicReference`, `AtomicInteger`, `AtomicLong`, `AtomicBoolean` are all supported. | For atomic arrays, hand-roll a `synchronized` wrapper around a regular `int[]` / `long[]`. For high-throughput counters where `LongAdder`'s contention-splitting matters, use `synchronized` + a long field; the contention pattern that motivates `LongAdder` is rarely a bottleneck on mobile. |

## Resource files are a flat namespace

`Display.getInstance().getResourceAsStream("/path")` does **not** support nested directories at runtime. Resources packaged under `common/src/main/resources/sub/dir/foo.json` will fail to load on cross-build targets even though they work in the simulator JVM — it's a real CN1 classloader constraint, not a packaging quirk.

Workable patterns, easiest first:

1. **Flat resource at the top of `common/src/main/resources/`**. Read with `Display.getInstance().getResourceAsStream("/file.json")` plus `Util.readToString(...)`.

2. **A bundled HTML hierarchy via `/html.tar`** (built-in, no cn1lib needed). If you need to ship a directory tree of HTML / CSS / JS / images for `BrowserComponent`, drop the whole tree into `common/src/main/resources/html/` — at build time the CN1 plugin tars it into `html.tar` at the resource root. At runtime call:

   ```java
   BrowserComponent web = new BrowserComponent();
   form.add(BorderLayout.CENTER, web);
   try {
       web.setURLHierarchy("/index.html");      // path relative to the html/ tree
   } catch (IOException ex) { Log.e(ex); }
   ```

   `setURLHierarchy` extracts the tar into `FileSystemStorage` on first use (once per build), then points the WebView at the extracted directory. This is the right tool for shipping local docs, an HTML-based onboarding flow, a Mapbox-style offline tile viewer, or any other case where a `<script src="../lib/x.js">` style relative reference needs to resolve at runtime.

3. **A custom zip you read with `ZipInputStream`** — useful for "shovel a deep tree of mixed assets, then look up entries by path". `java.util.zip` is **not** in the CN1 JDK subset; you need the **ZipSupport** cn1lib (distributed as a `ZipSupport.cn1lib` binary, not currently published to Maven Central). Drop the `.cn1lib` file into `<project>/cn1libs/` — the simulator's *File → Install Cn1Lib* menu does this for you — then `mvn -pl common compile` wires it in and `net.sf.zipme.ZipInputStream` / `ZipEntry` become available:

   ```java
   import net.sf.zipme.ZipInputStream;
   import net.sf.zipme.ZipEntry;

   try (ZipInputStream zis = new ZipInputStream(
           Display.getInstance().getResourceAsStream("/data.zip"))) {
       ZipEntry e;
       while ((e = zis.getNextEntry()) != null) {
           if (e.getName().equals("templates/welcome.html")) {
               return Util.readToString(zis);
           }
       }
   }
   ```

   Note the package is `net.sf.zipme`, not `java.util.zip` — the cn1lib provides a CN1-portable copy under a different namespace. Only reach for option 3 when option 2 doesn't fit (i.e. the data isn't HTML and you specifically need random access by path).

4. **For larger or per-user data**, prefer `FileSystemStorage` (writable files) or `Storage` (key/value blobs in the per-app sandbox).

## IO — three different stores, each with a distinct purpose

CN1 separates persistence into three orthogonal APIs. Picking the right one matters; they look superficially similar but are not interchangeable.

### `Preferences` — small key/value config

The right tool for app settings, feature flags, last-used IDs, anything that maps cleanly to a key and a primitive/`String` value.

```java
import com.codename1.io.Preferences;

Preferences.set("user.email", email);
Preferences.set("notifications.enabled", true);
Preferences.set("last.sync", System.currentTimeMillis());

String email = Preferences.get("user.email", "");
boolean notif = Preferences.get("notifications.enabled", true);
long last  = Preferences.get("last.sync", 0L);

Preferences.delete("user.email");
```

Backed by a small file inside the per-app storage area. Cheap, persistent, key/value.

### `Storage` — a simplified app-private filesystem

`Storage` is **not** a key/value store. It's a flat, app-private filesystem with string names as filenames. Each "entry" is just a file inside the sandbox; you open input/output streams and write whatever bytes you want.

`Storage` happens to bundle a convenient `writeObject` / `readObject` pair that serializes Java objects via CN1's `Externalizable` registry — but **externalization is a general mechanism, not Storage-specific**, and `Storage` can save arbitrary files (binary, JSON, SQLite snapshots, anything).

```java
import com.codename1.io.Storage;
import com.codename1.io.Util;

// Save arbitrary bytes
try (OutputStream os = Storage.getInstance().createOutputStream("token")) {
    os.write(tokenBytes);
}
try (InputStream is = Storage.getInstance().createInputStream("token")) {
    byte[] bytes = Util.readInputStream(is);
}

// Save a Java object via Externalizable (the class must implement Externalizable
// and be registered with Util.register("Profile", Profile.class)).
Storage.getInstance().writeObject("profile", profile);
Profile p = (Profile) Storage.getInstance().readObject("profile");

Storage.getInstance().deleteStorageFile("token");
String[] entries = Storage.getInstance().listEntries();
boolean has = Storage.getInstance().exists("profile");
```

`Storage` is the right tool for: cached API responses, serialized model objects, screenshot baselines, anything that's app-private and doesn't need directory hierarchy.

Because `Storage` is the most portable filesystem-like API across CN1 platforms (Android, iOS, JavaScript, desktop all back it identically), it's also the recommended fallback when an alternative like SQLite behaves differently per platform — see the *Database* section below.

### `FileSystemStorage` — real files, with hierarchy + cross-app reach

Use `FileSystemStorage` when you need:

- A directory tree.
- A file path you can hand to another app (a `file://` URL passed to `BrowserComponent`, the system share sheet, a media picker, etc.).
- Interaction with OS-visible storage areas (Documents on iOS, the Android shared storage, etc.).

```java
import com.codename1.io.FileSystemStorage;

FileSystemStorage fs = FileSystemStorage.getInstance();
String appHome = fs.getAppHomePath();              // e.g. file:/.../com.example.myapp/
String path = appHome + "downloads/report.pdf";

fs.mkdir(appHome + "downloads");
try (OutputStream os = fs.openOutputStream(path)) {
    Util.copy(networkInputStream, os);
}
try (InputStream is = fs.openInputStream(path)) { /* ... */ }
fs.delete(path);
fs.exists(path);
String[] files = fs.listFiles(appHome + "downloads");
fs.getLength(path);
```

Files written via `FileSystemStorage` can be exposed to other apps via the platform share mechanism (`Display.share(...)`).

### Loading classpath resources

```java
// Flat-namespace classpath resource (file at common/src/main/resources/seed.json).
// Display.getResourceAsStream is the JVM classloader path — it serves any file
// packaged at the root of common/src/main/resources/.
try (InputStream is = Display.getInstance().getResourceAsStream("/seed.json")) {
    String json = Util.readToString(is);
}
```

### `Resources` — entries from the binary theme/resource file

`com.codename1.ui.util.Resources` is **not** a general classpath loader. It reads entries (themes, images, l10n bundles, multi-images, animations, etc.) only from the binary `.res` file produced by the CSS compiler — typically `/theme.res`.

```java
// Reach for the cached, in-RAM global resources — the Lifecycle has already
// loaded /theme.res by the time your code runs.
Resources r = Resources.getGlobalResources();
Image logo = r.getImage("logo");                    // an image baked into theme.res
Hashtable<String, String> strings = r.getL10N("messages", "en");
```

(`Resources.openLayered("/theme")` exists but **re-reads** the resource bundle from the classpath every call. Use `getGlobalResources()` unless you're loading a *secondary* resource file that wasn't installed as the app theme.)

Files placed under `common/src/main/css/images/` and bundles under `common/src/main/l10n/` end up inside `theme.res`. Files placed elsewhere under `common/src/main/resources/` are reachable via `Display.getResourceAsStream` but **not** via `Resources` — different stores, different lookups.

### Multi-images — density-correct asset bundling

A **multi-image** is a single named resource that holds several per-density variants of the same image. At runtime CN1 picks the variant whose density matches `Display.getDeviceDensity()`, so a single named lookup renders crisply on a low-DPI Android tablet, a mid-range phone, and a 4K-class flagship without scaling artifacts.

Codename One uses **density buckets** (closer to Android's `ldpi/mdpi/hdpi/...` than iOS's `1x/2x/3x`):

| Constant on `Display` | Approx pixel density | Typical device |
| --- | --- | --- |
| `DENSITY_VERY_LOW` | ~88 ppi | very small / low-end |
| `DENSITY_LOW` | ~120 ppi | Android ldpi |
| `DENSITY_MEDIUM` | ~160 ppi | Android mdpi, iPhone 3GS, original iPad |
| `DENSITY_HIGH` | ~240 ppi | Android hdpi |
| `DENSITY_VERY_HIGH` | ~320 ppi | Android xhdpi, iPhone 4 / iPad Air 2 |
| `DENSITY_HD` | ~540 ppi | Android xxhdpi, iPhone 6+ |
| `DENSITY_560` | ~750 ppi | Android xxxhdpi |
| `DENSITY_2HD` | ~1000 ppi | |
| `DENSITY_4K` | ~1250 ppi | |

Multi-images are **authored in the Codename One Designer** (the resource editor): drop in the source image at its native density, tick the lower-density buckets you want auto-generated, and the designer creates the variants and writes them as a single named multi-image entry in `theme.res`. There's no auto-derivation from a plain `url('/foo.png')` in CSS — the multi-image has to be present in the resource file.

At runtime, fetch by name from the global resources cache (see *Resources — entries from the binary theme/resource file* above):

```java
Image logo = Resources.getGlobalResources().getImage("logo");
```

The returned `Image` is already the variant matching the device — no extra lookup or scaling code on your side.

Use multi-images for everything that ships with the app (icons, decorative graphics, splash artwork). For network-loaded images, use `URLImage` which handles its own density-aware caching.

### `Util.readToString` / `Util.readInputStream`

```java
String json  = Util.readToString(inputStream);
byte[] bytes = Util.readInputStream(inputStream);
```

CN1-friendly equivalents of `Files.readString(Path)` / `Files.readAllBytes(Path)` — work over any `InputStream`, including the ones from `Storage`, `FileSystemStorage`, network calls, and `getResourceAsStream`.

## Networking

CN1 has **three layers** of networking, lowest to highest:

### 1. `ConnectionRequest` — direct request object

The base class. Subclass it (or wire listeners) for full control over request lifecycle.

```java
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;

ConnectionRequest req = new ConnectionRequest("https://api.example.com/items");
req.setHttpMethod("GET");
req.addRequestHeader("Authorization", "Bearer " + token);
req.addResponseListener(evt -> {
    byte[] body = req.getResponseData();
    // already on the EDT — safe to update UI
    renderItems(parseJson(body));
});
req.addExceptionListener(evt -> {
    Log.e(evt.getError());
    ToastBar.showErrorMessage("Network failure");
});
NetworkManager.getInstance().addToQueue(req);
```

`NetworkManager` is a CN1 global that batches requests on background threads, handles retries, and routes responses back to the EDT.

### 2. `Rest` — fluent client (preferred for normal use)

A thin builder layer over `ConnectionRequest` that handles JSON, headers, and response unmarshalling cleanly.

```java
import com.codename1.io.rest.Rest;
import com.codename1.io.rest.Response;
import com.codename1.processing.Result;

// Sync (rare — only off the EDT):
Response<Map> r = Rest.get("https://api.example.com/items").getAsJsonMap();

// Async (the normal case):
Rest.get("https://api.example.com/items")
    .header("Authorization", "Bearer " + token)
    .fetchAsJsonMap(response -> {
        // Called on the EDT.
        Map data = response.getResponseData();
        renderItems(data);
    });

// POST with JSON body:
Rest.post("https://api.example.com/items")
    .header("Content-Type", "application/json")
    .body("{\"name\":\"new\"}")
    .fetchAsJsonMap(response -> {
        if (response.getResponseCode() == 201) {
            ToastBar.showMessage("Created");
        }
    });
```

Use `Rest` for ~95% of REST API work. The `fetchAs*` methods marshal into `byte[]`, `String`, `Map`, `JSONArray`, or `PropertyBusinessObject` and always invoke the callback on the EDT.

For **top-level JSON arrays** (`[{...}, {...}]`) use `fetchAsJsonList(OnComplete<Response<List>>)` -- it parses through the same `JSONParser` (which internally wraps top-level arrays under a synthetic `"root"` key) and hands you the unwrapped `List` directly.

### Typed responses — `fetchAsMapped` / `fetchAsMappedList`

Once a DTO is `@Mapped`-annotated (see *Build-time POJO binding* below), use `fetchAsMapped(Class<T>, callback)` instead of `fetchAsJsonMap` — the callback receives the typed object directly:

```java
@Mapped public class Pet {
    @JsonProperty("id")        public long id;
    @JsonProperty("name")      public String name;
    @JsonProperty("photoUrls") public List<String> photoUrls;
}

Rest.get(baseUrl + "/pet/" + petId)
    .header("Authorization", "Bearer " + token)
    .acceptJson()
    .fetchAsMapped(Pet.class, response -> {
        Pet pet = response.getResponseData();   // already typed
        renderPet(pet);
    });

// List variant for endpoints returning a top-level JSON array of DTOs:
Rest.get(baseUrl + "/albums")
    .acceptJson()
    .fetchAsMappedList(Album.class, response -> {
        List<Album> albums = response.getResponseData();
    });
```

If no `Mapper<T>` is registered for `Class<T>` (typical cause: the class isn't `@Mapped`, or the `process-annotations` Mojo didn't run), the callback completes with `null` data and a normal response code — inspect `response.getResponseCode()` to differentiate "server error" from "no mapper registered".

For **bulk REST clients** (an existing OpenAPI 3.x spec, dozens of endpoints), use the `cn1:generate-openapi` Maven goal — it emits `@Mapped` records / classes per schema and one `@RestClient`-annotated interface per OpenAPI tag into `common/src/main/java`. The annotation processors run on the next compile and write the wire impls into generated-sources, so the implementation isn't part of the project source. Call sites instantiate via the generated `<Tag>Api.of(baseUrl)` static factory.

This is one of **three** "spec to typed client" generators that share the same architecture — the others are **gRPC** (`cn1:generate-grpc` + `@GrpcClient`, over gRPC-Web) and **GraphQL** (`cn1:generate-graphql` + `@GraphQLClient`, with subscriptions over WebSocket). When the backend has a `.proto` or a GraphQL schema instead of (or alongside) an OpenAPI spec, read `references/api-clients.md` for the goal, the annotations, the response envelopes (`Response<T>` / `GrpcResponse<T>` / `GraphQLResponse<T>`), and enum support.

### Writing JSON — `JSONWriter`

For ad-hoc request bodies use `com.codename1.io.JSONWriter`. Two access modes:

```java
// One-shot: pass any Map / List / String / Number / Boolean / null tree
String json = JSONWriter.toJson(Map.of("name", "ada", "values", List.of(1, 2, 3)));

// Fluent builder for tiny request bodies (faster to read than a Map literal):
String body = JSONWriter.object()
        .put("email", email)
        .put("password", password)
        .toJson();

// Streaming variants for large outputs:
JSONWriter.toJson(value, writer);
JSONWriter.toJson(value, outputStream);   // UTF-8
```

For *typed* DTO serialization (POJOs annotated with `@Mapped`), use `com.codename1.mapping.Mappers#toJson` from the build-time binding framework instead. `JSONWriter` is for untyped maps/lists.

### Authenticated image URLs — `URLImage.RequestDecorator`

`URLImage.createToStorage(placeholder, key, url, adapter)` is the standard CN1 idiom for lazy network-loaded images cached to `Storage`. When the URL is behind a bearer token (or any other custom header), use the decorator hook:

```java
// Global: one line at app boot, applies to every URLImage from then on
URLImage.setDefaultBearerToken(Preferences.get("auth.token", null));

// Or the explicit form:
URLImage.setDefaultRequestDecorator(req ->
    req.addRequestHeader("Authorization", "Bearer " + token));

// Per-image: pass a decorator at construction time. Runs *after* the
// global default so it can override.
URLImage.createToStorage(placeholder, cacheKey, url,
        URLImage.RESIZE_SCALE_TO_FILL,
        req -> req.addRequestHeader("X-API-Version", "2"));
```

For one-off image fetches without `URLImage`'s caching (e.g. preloading neighbours in an asset viewer where you stream bytes straight into `Storage`), drop to a raw `ConnectionRequest` with `readResponse(InputStream)` overridden:

```java
ConnectionRequest req = new ConnectionRequest(url) {
    @Override protected void readResponse(InputStream in) throws IOException {
        try (OutputStream os = Storage.getInstance().createOutputStream(cacheKey)) {
            Util.copy(in, os);
        }
    }
};
req.addRequestHeader("Authorization", "Bearer " + token);
NetworkManager.getInstance().addToQueue(req);
```

### What about `HttpClient` / `URLConnection` / `OkHttp`?

`java.net.http.HttpClient` and standard `java.net.URLConnection` aren't in the subset. OkHttp pulls in Android-only deps. **Use `ConnectionRequest` or `Rest` instead.**

If you specifically need a `URLConnection`-shaped API for porting (e.g. translating Java code that calls `URL.openConnection().getInputStream()`), CN1 exposes a small subset as **inner classes of `com.codename1.io.URL`**:

```java
import com.codename1.io.URL;
URL u = new URL("https://example.com/data");
URL.URLConnection conn = u.openConnection();        // returns URL.HttpURLConnection for http(s)
conn.setRequestProperty("Authorization", "Bearer " + token);
try (InputStream is = conn.getInputStream()) {
    String body = Util.readToString(is);
}
```

This is a portability shim, not a feature-complete replacement. Prefer `Rest` for new code.

### OAuth 2.0 / OpenID Connect — `OidcClient`

For modern sign-in flows (Google, Apple, Microsoft Entra, Auth0, Okta, Keycloak — anything that publishes `.well-known/openid-configuration`), use `com.codename1.io.oidc.OidcClient`. It drives the **system browser** — `ASWebAuthenticationSession` on iOS, Custom Tabs on Android, the user's default browser on desktop/web — which is the only flow Apple/Google/Microsoft/Facebook still accept. PKCE S256 is mandatory, refresh-token rotation is first-class, ID-token claims are surfaced via `getClaim(String)`, and tokens persist through a pluggable `TokenStore`.

```java
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcTokens;

OidcClient.discover("https://accounts.google.com").ready(client -> {
    client.setClientId("YOUR_CLIENT_ID")
          .setRedirectUri("com.example.app:/oauth2redirect")
          .setScopes("openid", "email", "profile");
    client.authorize().ready((OidcTokens tokens) -> {
        String access = tokens.getAccessToken();
        String email  = (String) tokens.getClaim("email");
        Preferences.set("auth.token", access);
    });
});
```

The pre-existing `com.codename1.io.Oauth2` and `com.codename1.social.Login` classes are kept for source compatibility but **deprecated** — they use an in-app `BrowserComponent` WebView which the major IdPs now reject. New code should use `OidcClient`.

Companion classes in `com.codename1.io.oidc`: `OidcConfiguration` (endpoints + supported scopes), `OidcTokens` (access / id / refresh tokens + claim accessors), `PkceChallenge`, `SystemBrowser` (the per-platform browser driver), `TokenStore` (defaults to `Preferences`), `OidcException`.

For raw HTTP server-push (Server-Sent Events / long-poll), use `ConnectionRequest` with `readResponse(InputStream)` overridden to consume the stream incrementally — the same pattern as the authenticated-image loader above.

### TLS, redirects, gzip

`ConnectionRequest` handles HTTPS, gzip decompression, and HTTP redirects automatically. Override `shouldStop()`, `handleErrorResponseCode()`, or `postResponse()` for custom behavior. For self-signed certs in dev, see `ConnectionRequest.setSslCertificates(...)` — only enable in development builds.

## Concurrency

The single EDT is the only safe place to mutate UI. All event listeners, `Lifecycle.runApp()`, and `Form.show()` callbacks already run on it.

```java
// Background work:
Display.getInstance().startThread(() -> {
    final List<Item> items = api.fetch();
    Display.getInstance().callSerially(() -> render(items));
}, "items-fetcher").start();

// Wait for a result inline (rare — only outside the EDT!):
final Result[] result = new Result[1];
Display.getInstance().callSeriallyAndWait(() -> {
    result[0] = computeOnEdt();
});
```

`Display.startThread(Runnable, String)` is the recommended factory because it carries CN1's per-platform setup (notably the iOS autorelease pool around the thread body) and gives the thread a sensible name for debugging. Plain `new Thread(...).start()` is **functional** on every CN1 target — it doesn't leak resources or crash on iOS — but it skips that setup, so prefer `startThread` for new code. `synchronized` blocks work; `java.util.concurrent.locks` is mostly not in the runtime subset.

## Logging

```java
import com.codename1.io.Log;

Log.p("Loaded " + items.size() + " items");
Log.e(throwable);
Log.setLevel(Log.DEBUG);             // p with level
Log.sendLog();                       // upload the captured log to the CN1 server (debug builds)
```

`Log.p` and `Log.e` write to a rotating in-app log file under `Storage`. `System.out.println` works in the simulator but isn't reliably captured on device — use `Log` for anything you may need to read on a real phone.

## Database / SQLite

```java
import com.codename1.db.Database;
Database db = Display.getInstance().openOrCreate("app.db");
db.execute("CREATE TABLE IF NOT EXISTS items (id INTEGER PRIMARY KEY, name TEXT)");
db.execute("INSERT INTO items (name) VALUES (?)", new Object[]{"first"});

Cursor c = db.executeQuery("SELECT id, name FROM items");
while (c.next()) {
    int id = c.getRow().getInteger(0);
    String name = c.getRow().getString(1);
}
c.close();
db.close();
```

`Database` is a thin SQLite wrapper present on every platform that supports SQLite (iOS, Android, JavaSE simulator; JavaScript via sql.js).

> **Portability caveat.** The iOS and Android underlying SQLite versions and dialects differ substantially — different default `PRAGMA`s, different `JSON1`/`FTS5` availability, different statement-cache behavior under concurrent writes. Code that "works on Android" can fail on iOS at runtime. If your persistence needs are simple (a list of records, app state, blobs), **prefer `Storage`** — it has identical semantics on every CN1 target. Reach for `Database` only when you genuinely need SQL queries against more than a few thousand rows.

## Date and time

`java.time.LocalDate`, `LocalDateTime`, `Instant`, `Duration` are present. Some formatter pieces of `DateTimeFormatter` aren't — when something fails the compliance check, fall back to `com.codename1.l10n.SimpleDateFormat`:

```java
import com.codename1.l10n.SimpleDateFormat;

SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
Date d = fmt.parse(isoString);
String out = fmt.format(d);
```

`SimpleDateFormat` (CN1's L10N copy, **not** `java.text.SimpleDateFormat`) is the one to use for cross-platform date formatting — `java.text.*` is partial.

## When the compliance check fails

1. Read the violation: "`java.foo.Bar.method(...)` — not in Codename One Java Runtime API".
2. Search this document for the area (IO, networking, concurrency, etc.) and use the listed substitute.
3. If you can't find one, grep the `java-runtime` jar (see top of this file) for a nearby supported alternative.
4. As a last resort, write a native interface (`com.codename1.system.NativeInterface`) and implement it per platform — only do this when no portable substitute exists.

## Authoritative references

- Java API subset jar — `~/.m2/repository/com/codenameone/java-runtime/<version>/java-runtime-<version>.jar`
- CN1 framework jar — `~/.m2/repository/com/codenameone/codenameone-core/<version>/codenameone-core-<version>.jar`
- JavaDoc — <https://www.codenameone.com/javadoc/>
- Developer Guide — <https://www.codenameone.com/developer-guide/>
