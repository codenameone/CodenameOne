---
title: "SQLite Across Every Port: One Contract, One Encrypted File Format"
slug: sqlite-portable-encrypted
url: /blog/sqlite-portable-encrypted/
date: '2026-08-21'
author: Shai Almog
description: "Codename One now runs the same tested SQLite contract across Android, iOS, JavaScript, Windows, Linux, watchOS, tvOS, and the simulator, with passphrase, managed-key, and raw-key encryption."
feed_html: '<img src="https://www.codenameone.com/blog/sqlite-portable-encrypted.jpg" alt="A locked SQLite database connected to mobile, web, desktop, and watch applications" /> Codename One now runs the same tested SQLite contract across Android, iOS, JavaScript, Windows, Linux, watchOS, tvOS, and the simulator, with passphrase, managed-key, and raw-key encryption.'
series: ["release-2026-08-21"]
---

![A locked SQLite database connected to mobile, web, desktop, and watch applications](/blog/sqlite-portable-encrypted.jpg)

We have called `com.codename1.db` portable since 2012. The interface was portable. Its behavior was not.

On Windows and Linux, `Database.openOrCreate()` returned `null`. JavaScript depended on WebSQL, which Chrome removed and Firefox never implemented. An iOS cursor could report success on an empty result set, then read unset memory. The simulator counted rows from one while the other ports counted from zero. Encryption had no sensible place to live because there was no single database contract underneath it.

[PR #5526](https://github.com/codenameone/CodenameOne/pull/5526) replaces that collection of similar APIs with one specified and tested SQLite contract. It also resolves the long-standing [encrypted database request](https://github.com/codenameone/CodenameOne/issues/3848). The same encrypted file can move between ports and open in a standard SQLCipher 4 client.

That is the main work this week. The other releases extend the same idea beyond the database: one declared behavior, projected onto each platform, with the differences exposed instead of hidden.

## This week in one page

- [SQLite now has one tested contract](#the-interface-hid-five-different-databases), including native Windows and Linux implementations, SQLite compiled to WebAssembly for JavaScript, and encryption with three key models.
- The {{< post-link path="/blog/watch-apps-phone-channel" text="watch apps deep dive" >}} treats the phone and watch as separate applications with separate lifecycles. `WearableConnection` gives them one asynchronous API for messages, replicated state, and files.
- {{< post-link path="/blog/javascript-dom-text-search" text="JavaScript text" >}} now appears as real DOM text above the canvas. Browser search, selection, accessibility, autofill, and native-resolution rendering work without handing layout to the browser.
- {{< post-link path="/blog/smart-home-homekit-matter" text="Smart home support" >}} maps HomeKit, Matter, and Google Home concepts onto `com.codename1.home`, including a simulated house for desktop development.
- {{< post-link path="/blog/tapjacking-protection" text="Tapjacking protection" >}} detects and can reject Android gestures that begin behind another app's overlay.
- {{< post-link path="/blog/camera-vision-scanners" text="Camera and vision" >}} regain the one-call ergonomics that were lost when the old scanner libraries were replaced by lower-level on-device analyzers.
- {{< post-link path="/blog/app-intents-siri-spotlight-shortcuts" text="App Intents" >}} declare an application capability once and project it to Siri, Spotlight, Shortcuts, Android launcher shortcuts, widgets, and an internal command layer.

## The interface hid five different databases

The old API looked uniform because every port implemented the same Java methods. That said nothing about the result.

`execute(sql)` ran every statement on iOS, but silently stopped after the first statement in the simulator. `getBlob()` returned `nil` on iOS. The JavaScript cursor's `position(n)` always landed on row zero. The simulator leaked a prepared statement per query. `ThreadSafeDatabase.close()` returned before the close happened, so an immediate delete could race it.

These were not edge cases around a working abstraction. They meant a query tested in the simulator could have different transaction, cursor, or binding behavior on a device.

The new `DatabaseConformanceSuite` defines the contract once. Seven device tests exercise lifecycle, statements, cursors, transactions, encryption, and legacy behavior on every port-status target. The current reports show those tests passing on Android, both iOS renderers, JavaScript, native Linux, native Windows, macOS, tvOS, and watchOS.

{{< mermaid >}}
flowchart TD
    A[Application code<br/>com.codename1.db] --> B[Portable database contract]
    B --> C[Android<br/>platform SQLite or SQLCipher]
    B --> D[iOS and Apple targets<br/>system SQLite or cipher engine]
    B --> E[JavaScript<br/>SQLite in WebAssembly]
    B --> F[Windows and Linux<br/>native SQLite]
    B --> G[Simulator<br/>SQLite JDBC]
    H[One conformance suite] --> C
    H --> D
    H --> E
    H --> F
    H --> G
{{< /mermaid >}}

Cursor navigation now derives from two primitives, `rewind()` and `stepForward()`, instead of being reimplemented on every port. Transactions return to autocommit after either commit or rollback. Blobs and typed parameters behave consistently. Windows and Linux finally open a database instead of returning `null`.

Existing Ant projects keep their old behavior by default. Maven projects receive the portable contract. The `db.legacy` build hint makes the choice explicit in either direction, which matters when an Ant project moves to Maven.

## Encryption belongs above the ports

Encryption is selected by passing a `DatabaseConfig` when the database opens:

```java
if (Database.isEncryptionSupported()) {
    DatabaseConfig config = DatabaseConfig.managed();
    Database db = Database.openOrCreate("secure.db", config);
    config.wipe();
}
```

There are three key models:

| Key model | Where the key comes from | Good fit |
| --- | --- | --- |
| `DatabaseConfig.passphrase(...)` | A secret supplied by the user or server | Data that must move to another device |
| `DatabaseConfig.managed()` | A random key stored in Android Keystore or the iOS keychain | Local data with nobody to prompt |
| `DatabaseConfig.rawKey(...)` | Thirty-two random bytes supplied by the application | A key managed by an existing backend or protocol |

A passphrase compiled into the application is recoverable from the shipped binary. It is not a useful secret. A managed key avoids that mistake, but it also changes the recovery story: if the key-store entry is lost, the database is unreadable. Use a user-held or server-held passphrase when the data must survive a device loss.

Existing plaintext databases can be converted without replacing the API:

```java
if (!Database.isEncrypted("customer.db")) {
    Database.encrypt("customer.db", DatabaseConfig.managed());
}
```

The on-disk parameters are fixed to the SQLCipher 4 format: AES-256-CBC, PBKDF2-HMAC-SHA512 with 256,000 iterations, 4,096-byte pages, and per-page HMAC-SHA512. CI writes a database with the Codename One engine and reads it with the stock `sqlcipher` client, then reverses the direction. That cross-engine test catches a class of failure where every port can read its own incompatible file.

Encryption protects data at rest. It does not protect an open database from a debugger, a memory dump, or code already running on a compromised device. That boundary is why this work sits beside App Shield and App Hardening instead of replacing either one.

## JavaScript gets SQLite instead of a removed browser API

The JavaScript port used WebSQL because browsers once exposed it as a convenient SQL-shaped store. Chrome removed WebSQL in version 119. Firefox never shipped it. A portable API cannot depend on a feature that no longer exists in one browser and never existed in another.

JavaScript now runs SQLite compiled to WebAssembly and stores the database in the browser's storage pool. It passes the same statement, cursor, transaction, encryption, and compatibility tests as the native ports.

The new engine cannot read an old WebSQL store. `openOrCreate()` refuses to create an empty database when it detects that old data, because silently replacing a user's database with an empty state is worse than a visible migration. An application with WebSQL-era users must export from a build that can still read the old store, or deliberately opt into a new empty database with `cn1.db.ignoreLegacyWebSql=true`.

The build also remains pay-for-what-you-use. An application that never references `com.codename1.db` gets no database engine. A plain JavaScript database adds roughly 1.5 MB of WebAssembly. Referencing `DatabaseConfig` adds the cipher implementation where the platform needs it.

On Android, encryption raises the minimum SDK to 23 and requires AndroidX because those are SQLCipher's requirements. The unencrypted database path keeps the older floor.

## Watch apps are now two real applications

A watch is not a small second window owned by the phone process. It has its own executable, storage, startup sequence, and failure modes. [PR #5487](https://github.com/codenameone/CodenameOne/pull/5487) makes that model explicit.

One `codename1.watchMain` setting adds the Apple Watch companion entry point. On Android, `codename1.watchStandalone=true` builds that entry point as the Wear OS product instead of the phone application; a companion Wear artifact beside the phone APK is not generated yet. The simulator can launch the phone and watch as separate processes and connect them on the desktop. The pair shares source, resources, CSS, and the surfaces model. It does not share `Storage`, `Preferences`, or SQLite.

`WearableConnection` models the three transports the platforms actually provide. `sendMessage()` asks a live peer for an immediate reply. `putData()` replicates the latest state and survives sleep or relaunch. `transferFile()` moves a larger payload in the background. The API uses `WCSession` on Apple platforms and the Wearable Data Layer on Android.

```java
WearableConnection.putData(new WearableMessage("/steps")
        .put("count", stepCount)
        .put("goalReached", stepCount >= 10000));
```

Complications join the existing surfaces vocabulary through watch-specific `WidgetSize` families. This reuses the same content and timeline model already used for widgets and Live Activities. The generated watchOS complication target and Wear OS complication service are not part of this release yet. Android also supports standalone Wear apps today, but does not yet generate a companion Wear artifact beside a phone APK.

## A canvas app can still behave like a web page

The JavaScript port has always rendered through a canvas. That gives Codename One control over layout and keeps the UI consistent, but it also turned visible text into pixels. Browser search could not find it. Users could not select it. The browser could not rasterize it at native resolution or expose it as ordinary text.

[PR #5552](https://github.com/codenameone/CodenameOne/pull/5552) keeps the canvas renderer and adds two targeted DOM layers above it. The text layer holds visible text runs at the positions Codename One already calculated. The accessibility layer holds the ARIA projection and updates it incrementally.

```html
<canvas role="presentation" aria-hidden="true"></canvas>
<div id="cn1-text-layer">Selectable, searchable text</div>
<div id="cn1-accessibility-tree" aria-label="..."></div>
```

The browser never gets to reflow a line or decide where a component belongs. It gets text that is already measured, broken, and positioned. Find-in-page, text selection, accessibility, native input metadata, password-manager hints, and high-DPI rendering can then use browser machinery without changing application layout.

Drag selection is still off because the text layer cannot take pointer events without changing canvas hit testing. Shape-clipped and transformed text stays on the canvas. Same-form occlusion, such as a sheet over text in the underlying form, also needs more work.

## Smart-home portability starts with admitting the gaps

[PR #5554](https://github.com/codenameone/CodenameOne/pull/5554) adds `com.codename1.home`, a common model for HomeKit, Matter, and Google Home. An accessory contains services. Services expose canonical traits such as `ON_OFF`, `BRIGHTNESS`, and `TARGET_TEMPERATURE`. Platform identifiers and unit conventions stay in the port.

That sounds like a normal abstraction until the platforms disagree. Matter and HomeKit express covering position in opposite directions. Matter has no single thermostat setpoint in automatic mode. Android can commission a Matter accessory into Google Home with little setup, but reading the accessory graph requires Google Home developer registration that Codename One cannot create for you.

The API reports that default Android state as `COMMISSIONING_ONLY`, not `AVAILABLE`. On iOS it can read and write HomeKit traits, run scenes, and commission Matter accessories. The simulator, desktop ports, and JavaScript run a deliberately awkward synthetic house so missing values, partial failures, and polling behavior can be tested without hardware.

Automations, background accessory events, camera streams, alarm panels, and the Google Home accessory graph on Android are outside this release. Each gap has a capability query or a specific availability state instead of turning into an empty list that looks like a real answer.

## A secure screen must control the tap as well as the pixels

Tapjacking happens when another Android application draws over a sensitive screen and changes what the user believes a tap will do. [PR #5553](https://github.com/codenameone/CodenameOne/pull/5553) adds a `TapjackingPolicy` to `DeviceIntegrity`.

```java
DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);
DeviceIntegrity.addTapjackingListener(event -> {
    if (Boolean.TRUE.equals(event.getSource())) {
        showOverlayWarning();
    }
});

if (DeviceIntegrity.isHideOverlayWindowsSupported()) {
    DeviceIntegrity.setHideOverlayWindows(true);
}
```

`REPORT` observes without changing input. `BLOCK` drops a gesture that begins fully obscured. `STRICT` also drops partially obscured gestures, which can include ordinary system UI and therefore carries a real false-positive cost.

Detection is touch-driven because Android supplies the obscured state on `MotionEvent`. An overlay that appears without a touch is not detected by polling. On Android 12 and newer, `setHideOverlayWindows(true)` prevents overlay windows instead of reacting to their touches. iOS does not let one application draw over another, so the policy is an Android-only control rather than a fake cross-platform checkbox.

## The high-level camera path is back

The on-device vision work added barcode recognition, face and pose detection, text recognition, segmentation, document scanning, and image labeling. It also exposed every piece of the camera-to-analyzer pipeline. That was useful for custom camera products and needlessly low-level for common cases.

[PR #5575](https://github.com/codenameone/CodenameOne/pull/5575) adds `CodeScanner.scan()`, which owns a complete scanner screen and returns one asynchronous result:

```java
CodeScanner.scan().ready(code -> {
    if (code != null) {
        urlField.setText(code.getValue());
    }
}).except(error -> Log.e(error));
```

`VisionCameraView` packages the same pipeline as a component for a form you own. Typed `BarcodeFormat`, `FaceLandmarks`, and `PoseLandmarks` constants replace string literals. `VisionRect.toBounds(...)` and `VisionPoint.toPoint(...)` convert normalized analyzer geometry into component coordinates. `SegmentationMask.cutOut(...)` turns selfie segmentation into a usable image operation.

The preview remains a native peer. Components cannot be painted over it uniformly on every target, so controls and reticles should sit around it. One camera session can be open at a time. The simulator now scripts vision results, which lets the flow, overlay geometry, cancellation, and error paths run before the application reaches a device.

## One intent can reach several system surfaces

Siri, Spotlight, Shortcuts, an Android launcher shortcut, and a widget button all need a declaration of what an application can do. [PR #5559](https://github.com/codenameone/CodenameOne/pull/5559) puts that declaration in Java:

```java
@AppIntent(value = "log_workout", title = "Log a workout",
        phrases = {"Log a workout in ${applicationName}"}, headless = true)
public static IntentResult logWorkout(
        @IntentParam("minutes") int minutes) {
    WorkoutStore.append(minutes);
    return IntentResult.spoken("Logged " + minutes + " minutes.");
}
```

The Maven plugin reads the compiled bytecode and generates a reflection-free dispatch table plus the native declarations. The direct static call matters on iOS, where runtime annotation lookup is unavailable and dead-code elimination can remove a handler that has no Java caller.

Entities let the platform ask the user which application object they meant. `Intents.index(...)` publishes those objects to device search. `opensRoute` connects an intent to the existing route table when the result should foreground a screen.

Android is not presented as Siri parity. It gets launcher shortcuts, donation, indexing, and headless execution. Voice invocation, system disambiguation, and spoken assistant results are iOS capabilities. `Intents.invoke(...)` still works on every port as an internal command layer, even when the operating system exposes no intent surface.

## Security and portability are becoming properties we can test

This week closes several gaps that used to be explained away as platform differences. SQLite now has one documented contract and one portable encrypted format. The watch and phone are separate applications with a defined channel. Smart-home traits and app intents project one application model onto native system services without pretending those services are identical. The web port keeps our renderer while restoring browser behavior users expect.

The security work follows the same pattern. App Shield moves trust decisions to the backend. App Hardening raises the cost of reading and modifying the shipped binary. Encrypted SQLite protects stored data. Tapjacking protection rejects a class of misleading input on Android. None of these controls makes a device trustworthy by itself. Each one owns a boundary and states where that boundary ends.

That is how secure-by-default programming becomes practical in a cross-platform framework. The safe path must be available from ordinary application code, included only when used, and exercised on the targets we claim to support.

Start with the [database guide](/developer-guide/#sql-encryption) if you have data at rest to migrate. Existing Ant applications should set `db.legacy` explicitly before moving to Maven. Applications with WebSQL-era browser data need an export plan before taking the new JavaScript engine.

---

## Discussion

_Which platform difference has caused the most expensive production bug in your local data layer?_

{{< giscus >}}
