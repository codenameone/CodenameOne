---
title: "App Shield: Your Server Should Not Trust the App Calling It"
slug: app-shield-server-attestation
url: /blog/app-shield-server-attestation/
date: '2026-08-07'
author: Shai Almog
description: "Codename One App Shield turns Apple App Attest and Google Play Integrity into short-lived tokens your backend can verify. This release also adds OpenType fonts, advances the R2 repository migration, and prepares new Windows desktop builders."
feed_html: '<img src="https://www.codenameone.com/blog/app-shield-server-attestation.jpg" alt="A shield between a mobile application and a protected server API" /> Codename One App Shield turns Apple App Attest and Google Play Integrity into short-lived tokens your backend can verify. This release also adds OpenType fonts, advances the R2 repository migration, and prepares new Windows desktop builders.'
series: ["release-2026-08-07"]
---

![A shield between a mobile application and a protected server API](/blog/app-shield-server-attestation.jpg)

Any security check that runs only on a phone can be patched out on that phone. [App Shield](https://github.com/codenameone/CodenameOne/pull/5486) moves the final decision to your server by attaching a short-lived, server-verified attestation token to protected requests.

Greetings from Thailand. My family dragged me here for a forced vacation. It is a lovely country, but beaches, sunshine, and the sea aren't really my thing. The GitHub Actions downtime didn't help either, so progress was slower than usual this week. Several interesting PRs are still in progress, and we chose not to rush them.

## This week in one page

- [App Shield](#a-local-check-is-not-a-security-boundary) connects device integrity, certificate pinning, and your backend instead of asking the app to trust its own verdict.
- [OpenType fonts](#opentype-fonts-now-work-without-renaming) now work as `.otf` files across the supported ports. The CSS compiler also catches several font failures before they reach a device.
- [Windows desktop builders](#new-builders-for-the-javase-windows-target) are moving to newer machines. This is the older JavaSE Windows target, not the native Win32 target.
- [The Maven repository migration](#new-projects-now-resolve-codename-one-through-r2) has entered phase two. Newly generated projects now point dependencies and plugins at our R2 repository.
- [`push.codenameone.com`](#the-push-hostname-switch-happens-saturday) redirects to the new cloud service on Saturday, August 8.

## A local check is not a security boundary

Codename One App Shield is an Enterprise application-attestation layer. It asks Apple App Attest or Google Play Integrity for a hardware-backed statement, verifies that statement through the Codename One service, and gives the app a short-lived ES256 token. Your backend verifies that token before it performs a sensitive operation.

We already serve [several banking customers](/blog/device-integrity-and-app-review/), and high-security requirements have shaped Codename One for years. Java is part of that fit. These teams get mature analysis tooling, a familiar type system, and one application codebase to review instead of separate iOS and Android implementations.

Java is not a security boundary by itself. The build pipeline adds useful friction for an attacker, but determined attackers can still reverse engineer a client they control.

| Control | Default | What it does |
| --- | --- | --- |
| iOS native compilation | On | ParparVM translates the application's Java bytecode to C and then builds a native binary. |
| Obfuscation | On | Removes useful names and makes static inspection harder. |
| Debug flags | Off in release builds | Blocks the ordinary production debugging path. |
| Root, jailbreak, Frida, and accessibility checks | Opt-in | Adds device-side signals or launch-time gates for specific risks. |
| Secure screen and clipboard restrictions | Opt-in | Reduces capture and clipboard exposure. It is not a keylogger defense. |
| App Shield | Enterprise opt-in | Makes your backend act on a server-verified attestation token rather than a client boolean. |

The distinction in the last row is the point. A modified app can force a local `isDeviceCompromised()` call to return `false`. It cannot mint a valid token signed by a key your server trusts.

## From hardware statement to protected API

App Shield joins the platform attestation provider, the Codename One verification service, and your backend:

{{< mermaid >}}
sequenceDiagram
    participant App as Codename One app
    participant Shield as Attestation service
    participant Platform as App Attest or Play Integrity
    participant API as Your backend
    App->>Shield: Request a one-time challenge
    Shield-->>App: Nonce
    App->>Platform: Attest app and device against nonce
    Platform-->>App: Hardware-backed statement
    App->>Shield: Statement and runtime signals
    Shield-->>App: Short-lived ES256 token and pin set
    App->>API: Request with X-CN1-Attest
    API->>API: Verify signature, app, verdict, policy, and expiry
    API-->>App: Serve or reject the operation
{{< /mermaid >}}

The nonce prevents a captured platform statement from becoming a permanent replay credential. The token identifies the expected package and platform, carries the policy decision, and can bind to one request body. The service can also include signals for root, jailbreak, hooking frameworks, emulators, debuggers, repackaging, or untrusted accessibility services.

Your backend remains the enforcement point. The client reports what it sees. The service evaluates the attestation and policy. Your API decides whether to move money, return personal data, request step-up authentication, or reject the call.

### We got the first App Attest path wrong

App Shield also fixes two defects in the earlier iOS App Attest implementation. The native method name missed ParparVM's return-type mangling, so a build with `ios.appAttest=true` failed while linking. The old flow then generated and attested a fresh key on every request. Apple's model is one attestation followed by many assertions against the registered key.

The new implementation keeps that key and its state in the keychain. It queues callers while the first attestation is in progress, recovers when iOS invalidates a key, and backs off when Apple throttles attestation. This is exactly the kind of failure that a security abstraction should remove from application code.

## The app-side setup is a host list

Enable the injected engine in `codenameone_settings.properties`:

```properties
codename1.arg.shield.enabled=true
```

Then register the hosts that should receive a token:

```java
AppShield.init(new ShieldConfig()
        // A request without a token must not leave the device.
        .protect("api.mybank.example", HostPolicy.ENFORCED)
        // Other subdomains get a token when one is available.
        .protect("*.mybank.example", HostPolicy.PROTECTED));

ConnectionRequest request = new ConnectionRequest(
        "https://api.mybank.example/transfer", true);
NetworkManager.getInstance().addToQueueAndWait(request);
```

`ConnectionRequest`, `Rest`, `RequestBuilder`, and other code built on `NetworkManager` pick up the guard automatically. App Shield attaches `X-CN1-Attest` on the network thread and checks the certificate chain against the current SPKI pin set. An unregistered host is untouched.

The two policies encode different outage choices:

| Policy | If no valid token is available |
| --- | --- |
| `HostPolicy.PROTECTED` | Send the request without a token. The backend can degrade or reject it. |
| `HostPolicy.ENFORCED` | Fail before the request leaves the device. Use this for the few endpoints where an unverified request is never acceptable. |

The simulator now has **Simulate > App Shield** controls for rejected attestations, expired tokens, compromised-device signals, and forced certificate-pin mismatches. That makes the failure path testable without misconfiguring a live server.

The public API lives in the open-source core. A build without the Enterprise engine degrades to a documented no-op, so shared code still compiles and runs. A cloud build that explicitly requests `shield.enabled=true` without entitlement fails with an explanation instead of silently producing an unprotected binary.

## What App Shield does not protect

App Shield does not make an application unhackable. It raises the cost of calling a protected backend from a modified app and gives the server a cryptographically verifiable input for its policy.

A genuine attested device can still relay requests for an attacker. Short token lifetimes and payload binding reduce replay, but they do not replace backend authorization, rate limits, or checks on the business operation itself.

It also cannot cover traffic it cannot see. `ConnectionRequest`-based APIs get automatic tokens and pinning. A third-party native HTTP client needs a token attached manually. A `BrowserComponent` can receive a token for its initial navigation, but requests made by the loaded page remain outside the framework. WebSocket handshake headers are not available through the platform socket on iOS or in the browser, and WebSocket certificate pinning is not exposed.

Certificate pinning has its own operational risk. App Shield pins public keys rather than whole certificates, so a certificate renewal on the same key does not break the app. You should still roll out the server policy in monitor mode first, measure the `would_deny` traffic, and only then reject requests.

The complete wire format, failure statuses, transport boundaries, pin lifecycle, and backend examples are in the [App Shield developer guide](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/App-Shield.asciidoc).

## OpenType fonts now work without renaming

[PR #5508](https://github.com/codenameone/CodenameOne/pull/5508) makes `.otf` a supported font extension across iOS, tvOS, watchOS, Android, JavaSE, JavaScript, Windows, and Linux. This corrects an inconsistent path where some tools could parse an OpenType font but device packaging ignored its extension.

You can now keep the original file in or below the directory containing your CSS:

```css
@font-face {
    font-family: "Brand Display";
    src: url("fonts/BrandDisplay.otf");
}

Title {
    font-family: "Brand Display";
}
```

The CSS compiler reads local fonts during the build. It now reports a missing file, an unreadable font, a missing PostScript name, a path outside the CSS directory, or two different files that would collide after packaging. Web font formats such as `.woff` remain unsupported.

## New builders for the JavaSE Windows target

We are bringing down the old Windows desktop build machines and replacing them with newer servers. These machines build the older JavaSE-based Windows desktop target. They are separate from the new native Win32 target.

The old builders crashed too often and held this target behind the rest of the toolchain. We expect the replacements to reduce those failures and finally make [Java 17 projects](/blog/official-experimental-java-17-support/) available for this desktop build path. That work is still a server rollout, so treat JDK 17 as the intended result until we finish validation on real builds.

## New projects now resolve Codename One through R2

[Phase two of the Maven repository migration](https://github.com/codenameone/CodenameOne/pull/5524) merged today. The application archetype, library archetype, and [Initializr](https://start.codenameone.com) now put `https://repo.codenameone.com/maven2` in both repository lists.

Maven keeps ordinary dependencies and build plugins in separate lists. Both blocks matter:

```xml
<repositories>
    <repository>
        <id>codenameone</id>
        <url>https://repo.codenameone.com/maven2</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
</repositories>

<pluginRepositories>
    <pluginRepository>
        <id>codenameone-plugins</id>
        <url>https://repo.codenameone.com/maven2</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>false</enabled></snapshots>
    </pluginRepository>
</pluginRepositories>
```

Newly generated builds now resolve Codename One releases through R2. Existing projects can add the same blocks before the planned August 28 cutover. We will stop publishing new versions to Maven Central on that date if the observation period remains clean. The archetype lookup itself still starts on Central; moving that lookup is phase three.

The [repository migration post](/blog/maven-central-cloudflare-r2/) explains the dates, artifact retention, signatures, and safeguards against partial releases.

## The push hostname switch happens Saturday

On Saturday, August 8, we will redirect `push.codenameone.com` to `cloud.codenameone.com`. We announced the new service and compatibility endpoint in [the Push V3 release post](/blog/push-v3-new-cloud/).

Existing push code should continue to work because the new service accepts the classic request format. Before the redirect, you can test the exact path by changing only the hostname on your server:

```diff
-https://push.codenameone.com/push/push
+https://cloud.codenameone.com/push/push
```

Send a real notification to every platform you support. Test a visible notification, a data payload, and a cold start. If the result differs from the old host after Saturday's switch, [open an issue](https://github.com/codenameone/CodenameOne/issues) or contact us through the website as soon as possible.

App Shield is the larger direction behind this release: security controls should compose across the app, the build, and the server. OpenType support, new Windows builders, the R2 migration, and the push cutover are smaller changes, but they follow the same rule. The dependable path should be the normal path, and failures should surface where we can act on them.

---

## Discussion

_Which API would you protect first with server-verified app attestation, and what would make you keep that endpoint in monitor mode?_

{{< giscus >}}
