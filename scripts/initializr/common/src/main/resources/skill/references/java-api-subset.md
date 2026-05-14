# Java API Subset, IO, and Networking

Codename One does **not** ship with the full JDK. The Java code you write in `common/` is cross-compiled by ParparVM (iOS) and TeaVM (web), and runs on Android against a hand-curated JDK subset. If you call a class or method that isn't in the subset, the cloud build fails the **compliance check** (`cn1:compliance-check`, which the `process-classes` phase runs automatically).

This document tells you (1) how to discover what *is* supported, and (2) where the IO and networking APIs differ from standard Java.

## How to discover the supported API

The supported API surface is defined by two artifacts that the Codename One Maven plugin resolves from Maven Central. The `compliance-check` goal compares your compiled bytecode against both jars and fails on anything not present.

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

## What's broadly NOT supported (and why)

| Area | Status | What to use instead |
| --- | --- | --- |
| `java.awt.*` / `javax.swing.*` | **Forbidden.** | `com.codename1.ui.*` — see `references/swing-comparison.md`. |
| `java.nio.file.*` (`Path`, `Files`, `Paths`) | **Forbidden.** | `com.codename1.io.FileSystemStorage` + `Storage`. |
| `java.net.http.*`, `java.net.URLConnection`, `java.net.Socket` | **Forbidden** on cross-build targets. | `com.codename1.io.ConnectionRequest`, `NetworkManager`, or the higher-level `Rest`. |
| `java.util.concurrent.locks.*`, `Fork/Join`, complex executors | Mostly **forbidden**. | `synchronized` blocks; `Display.callSerially(...)`, `Display.callSeriallyAndWait(...)`, `Display.startThread(...)`. |
| `java.lang.reflect.*` | Works in the **simulator only**, fails on iOS/Android cross-builds. | Avoid reflection; use the `com.codename1.properties` framework for property-style binding. |
| `java.util.logging.*` | Subset only. | `com.codename1.io.Log`. |
| `java.util.regex` | Limited — basic `Pattern` / `Matcher` exists but watch out for advanced flags. | Stick to standard regex constructs; verify in the simulator AND a cross-build for anything non-trivial. |
| `java.time` (full set) | Partial — `LocalDate`, `LocalDateTime`, `Instant` are present; some formatter pieces are missing. | `com.codename1.l10n.SimpleDateFormat`, `com.codename1.util.DateUtil`. |
| `java.util.Random` | Supported. | — |
| `java.lang.invoke.*`, `java.lang.module.*` | **Forbidden.** | Don't generate code at runtime. |

## Resource files are a flat namespace

`Display.getInstance().getResourceAsStream("/path")` does **not** support nested directories at runtime. Resources packaged under `common/src/main/resources/sub/dir/foo.json` will fail to load on cross-build targets even though they work in the simulator JVM — it's a real CN1 classloader constraint, not a packaging quirk.

Patterns:

- Put plain resources at the top of `common/src/main/resources/` and read them with `Display.getInstance().getResourceAsStream("/file.json")`.
- If you need a deeper structure (a templates folder, a sample dataset, a small SQLite seed file), package the tree into a single zip placed at the top level, then read entries with `ZipInputStream`:

  ```java
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

- For larger or per-user data, prefer `FileSystemStorage` (writable files) or `Storage` (key/value blobs in the per-app sandbox).

## IO — file system and persistent storage

### `Storage` — small key/value blobs

The default place for app state. Lives under the per-app sandbox (the OS handles cleanup on uninstall). Keys are strings; values are `Object`s serialized through CN1's `Externalizable` registry, or raw byte streams.

```java
import com.codename1.io.Storage;

// Read/write byte streams
try (OutputStream os = Storage.getInstance().createOutputStream("token")) {
    os.write(tokenBytes);
}
try (InputStream is = Storage.getInstance().createInputStream("token")) {
    byte[] bytes = Util.readInputStream(is);
}

// Object roundtrip (the class must register with Util.register or extend PropertyBusinessObject)
Storage.getInstance().writeObject("profile", profile);
Profile p = (Profile) Storage.getInstance().readObject("profile");

Storage.getInstance().deleteStorageFile("token");
Storage.getInstance().listEntries();
Storage.getInstance().exists("profile");
```

`Storage` is the right tool for: auth tokens, user preferences, cached API responses (small), serialized model objects.

### `FileSystemStorage` — real files

For arbitrary files (downloaded images, generated PDFs, SQLite database files), use `FileSystemStorage`. Paths are platform-relative URIs (the API hands you the root).

```java
import com.codename1.io.FileSystemStorage;

FileSystemStorage fs = FileSystemStorage.getInstance();
String appHome = fs.getAppHomePath();              // e.g. file:/.../com.example.myapp/
String path = appHome + "downloads/report.pdf";

fs.mkdir(appHome + "downloads");
try (OutputStream os = fs.openOutputStream(path)) {
    Util.copy(networkInputStream, os);
}

try (InputStream is = fs.openInputStream(path)) {
    // ...
}

fs.delete(path);
fs.exists(path);
String[] files = fs.listFiles(appHome + "downloads");
fs.getLength(path);                                // size in bytes
```

`FileSystemStorage` is the right tool for: file downloads, generated assets, SQLite DB files (`Database` API), anything you'd hand off to `BrowserComponent.setURL("file:" + path)`.

### Reading classpath resources

```java
// Flat-namespace classpath resource (file at common/src/main/resources/seed.json)
try (InputStream is = Display.getInstance().getResourceAsStream("/seed.json")) {
    String json = Util.readToString(is);
}

// Or via Resources for the binary theme.res
Resources r = Resources.openLayered("/theme");
Image logo = r.getImage("logo");
Hashtable<String, String> strings = r.getL10N("messages", "en");
```

`Util.readToString(InputStream)` and `Util.readInputStream(InputStream)` are the CN1-friendly equivalents of `Files.readString(Path)` / `Files.readAllBytes(Path)`.

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

### 3. `WebServiceProxy` and `RAD`

For projects using CodeRAD, REST endpoints can be declared via the proxy/`@RAD` annotations. Out of scope of this skill — see the CN1 developer guide if your project already uses CodeRAD.

### What about `HttpClient` / `URLConnection` / `OkHttp`?

Not supported. They aren't in the `java-runtime` subset and OkHttp pulls in Android-only deps. Use `ConnectionRequest` or `Rest` instead.

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

`startThread` is preferred over `new Thread(...).start()` because it carries CN1's per-platform thread setup (iOS autorelease pool, EDT-aware cleanup). `synchronized` blocks work, but the higher-level `java.util.concurrent.locks` are mostly not in the runtime subset.

## Logging

```java
import com.codename1.io.Log;

Log.p("Loaded " + items.size() + " items");
Log.e(throwable);
Log.setLevel(Log.DEBUG);             // p with level
Log.sendLog();                       // upload the captured log to the CN1 server (debug builds)
```

`Log.p` and `Log.e` write to a rotating in-app log file under `Storage`. `System.out.println` works in the simulator but isn't reliably captured on device — use `Log` for anything you may need to read on a real phone.

## Crypto, hashing, base64

| Operation | API |
| --- | --- |
| Hash | `com.codename1.util.MessageDigestUtil` (MD5, SHA-1, SHA-256) |
| Base64 | `com.codename1.util.Base64.encode(byte[])` / `Base64.decode(String)` |
| HMAC / signing | Available in `com.codename1.io.crypto.*` (project may need the `codenameone-crypto` cn1lib) |
| Random | `java.util.Random` is fine; `SecureRandom` is partially supported |

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
