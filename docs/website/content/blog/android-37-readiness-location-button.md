---
title: "Android 17 Preparation and Less Security Glue"
slug: android-37-readiness-location-button
url: /blog/android-37-readiness-location-button/
date: '2026-09-17'
author: Shai Almog
description: "API 37 checks and a system location button prepare Android migration. PEM parsing and task-clearing exit put more security-sensitive platform and format handling into Codename One."
feed_html: '<img src="https://www.codenameone.com/blog/android-37-readiness-location-button.jpg" alt="Less Glue Clearer Boundaries" /> API 37 checks and a system location button prepare Android migration. PEM parsing and task-clearing exit put more security-sensitive platform and format handling into Codename One.'
series: ["release-2026-09-11"]
---

![Less Glue Clearer Boundaries](/blog/android-37-readiness-location-button.jpg)

An Android port can compile successfully against an old `android.jar` while referencing a class the next platform has removed. Until this week, our regular port compilation could give exactly that false reassurance.

[PR #5731](https://github.com/codenameone/CodenameOne/pull/5731) adds checks against API 37 and fixes builder assumptions about minor-versioned platforms. [PR #5738](https://github.com/codenameone/CodenameOne/pull/5738) adds the Android 17 location button. Both let us prepare the migration before changing every customer's target SDK.

## Three different meanings of ready

| Check | What it establishes |
| --- | --- |
| Compile port sources against the new platform | Detect removed platform symbols |
| Assemble the generated application against the new SDK | Exercise resources, manifest merging, dependency metadata, and packaging |
| Run and interact with the app on the new OS | Exercise changed runtime behavior and permissions |

A successful compile does not answer the third question. The API 37 PR deliberately leaves the normal SDK floor at 36 rather than silently changing all generated applications' target SDK and required downloads.

The source check compiles identical sources against two platform jars and compares errors. Optional packages with unresolved dependencies can fail identically in both runs; the useful signal is an error introduced only by the newer platform.

A second `android.jar` on the classpath would defeat that check by supplying a removed symbol from the old stub. The script removes those duplicate platform jars. A fault-injection probe using removed fingerprint APIs proved that the comparison detects the regression it was designed to catch.

## The platform name is no longer an integer

The installed API 37 packages use names such as `android-37.0`, `android-37.1`, and `android-37.2`. Treating the whole suffix as an integer failed outright on one builder path. Removing punctuation before parsing was worse: `37.2` became `372`, which silently passed every ordinary minimum-version check.

The builder now extracts the major API level correctly, updates build-tools handling, and uses the SDK manager associated with the SDK root actually being built. The generated-project check then assembles against the new platform, covering failures a source-only check cannot see.

This continues the [platform-watch work from last week](/blog/platform-deprecation-watch/). The goal is to turn a future platform requirement into a failing check while there is still time to fix the producer.

## One-time location belongs behind a deliberate action

Android 17 introduces a system-rendered location button for transactional precise-location access. Google's policy page, checked September 10, lists January 27, 2027, for compliance and describes the Android 17+ enforcement timing as subject to further updates. That is a location-policy timeline, not a blanket API 37 target-SDK deadline. [Google Play location policy](https://support.google.com/googleplay/android-developer/answer/17033915?hl=en).

The Codename One component presents the platform control where supported and an ordinary button elsewhere:

```java
import com.codename1.location.LocationButton;

LocationButton location = new LocationButton(
        LocationButton.TEXT_USE_PRECISE_LOCATION);
location.addLocationSharedListener(fix -> {
    if (fix != null) {
        searchNearby(fix.getLatitude(), fix.getLongitude());
    }
});
form.add(location);
```

`searchNearby` is application code. A `null` result is a normal outcome when permission is declined or no location is delivered. The component exposes `isSystemRendered()` so the application can inspect which path is active.

Google's design ties the request to a visible user action and a session-scoped precise-location grant. The application should still choose coarse location if that is enough for the task. [Android's location-button introduction](https://developer.android.com/blog/posts/redefining-location-privacy-new-tools-and-improvements-for-android-17?hl=en).

## Use the platform protocol without forcing an AndroidX upgrade

The earlier implementation depended on an AndroidX library whose metadata required compile SDK 37 and Android Gradle Plugin 9.1.0. That would have made a new button force a toolchain migration for every generated application using it.

The platform already exposes the session API beneath that wrapper. The merged implementation reaches it through reflection and implements the callback interface through a Java proxy. The system draws its control into a surface hosted inside the Codename One component.

{{< mermaid >}}
flowchart TD
    B[LocationButton component] --> S{Platform session available?}
    S -->|Yes| P[Host system-rendered surface]
    P --> T[User taps system control]
    T --> G[Platform consent and location result]
    S -->|No or session fails| F[Ordinary Codename One button]
    F --> O[Existing location permission flow]
    G --> L[Shared listener receives Location or null]
    O --> L
{{< /mermaid >}}

This is a specific native control integration. It does not change Codename One's general custom-rendered UI model.

## Permission injection and fallback still need attention

The generated manifest must include `USE_LOCATION_BUTTON`. The repository builder adds it when the application references `LocationButton`. The PR explicitly says the separate BuildDaemon mirror was still pending at that point; this article does not certify deployment to the cloud builder serving a particular account.

The implementation also avoids automatically restricting all precise-location permission to button-only access. An application may have a legitimate continuous-location feature as well. That decision requires reviewing the application's actual location use and current platform guidance, rather than inferring policy from the presence of one class.

If a platform session fails, the component falls back to the ordinary button. That preserves a usable control, but an app targeting the new transactional-location requirements should test that the system path really remains active. A visible button alone is insufficient evidence of the new permission flow.

## What was exercised

The location-button PR reports a real Android 17 emulator image running a generated app at compile SDK 37.2 and target SDK 37. The system button rendered, a human tap opened the platform consent sheet, and approval produced a location with the session-scoped permission flags. The same APK on API 36 used the ordinary button and permission flow.

The regular Android screenshot CI leg remains at API 36 in this work. Broader API 37 runtime behavior and policy compliance are not established by the compile gate or this single location flow. We will keep those checks separate so a passing build cannot conceal an untested migration assumption.


## A key file is another input boundary

Permissions govern what an app may request from the OS. Key parsing governs what it accepts from a file. [PR #5707](https://github.com/codenameone/CodenameOne/pull/5707) removes another piece of security-sensitive glue code that applications previously had to supply.

PEM is more than Base64 with a heading.

`PublicKey.rsa()` and `PrivateKey.rsa()` take DER bytes in the expected key container. PEM adds textual armor and Base64 encoding. Passing the whole armored file to a Base64 decoder does not reliably strip the labels for you, and stripping them still leaves the question of which container is inside.

The new entry points accept text or raw file bytes and read the algorithm identifier from the key:

```java
import com.codename1.security.PublicKey;
import com.codename1.security.PrivateKey;
import com.codename1.io.Util;

PublicKey publicKey = PublicKey.fromPem(
        Util.readInputStream(publicStream));
PrivateKey privateKey = PrivateKey.fromPem(
        Util.readInputStream(privateStream));
```

The streams in this excerpt come from the application's existing key-loading flow. The parser does not decide where a private key should be stored or whether a supplied public key is trusted.

| Input | Parser behavior |
| --- | --- |
| Supported PKCS#8 private key / SPKI public key | Decode and identify the algorithm |
| PKCS#1 RSA key | Rewrap into the appropriate supported container |
| SEC1 EC private key | Carry the curve identifier into PKCS#8 |
| Encrypted private key | Reject with an explanatory message |
| Certificate | Reject; extract its public key separately |
| Wrong public/private direction | Reject |
| Truncated or trailing DER body | Reject unless the body is exactly one complete element |

Unarmored Base64 is also accepted for a supported key container. CRLF, blank lines, and text around the PEM block are tolerated.

## Validate the container before the platform sees it

A truncated DER element can contain enough bytes to name an algorithm and still be unusable. The parser checks the complete element rather than handing a partially decoded key to the platform and waiting for a less useful error.

{{< mermaid >}}
flowchart LR
    P[PEM text or bytes] --> A[Decode armor and Base64]
    A --> D[Validate complete DER element]
    D --> C[Identify container and algorithm]
    C --> W[Rewrap supported legacy container]
    W --> K[Key object]
    C --> R[Reject unsupported or mismatched input]
{{< /mermaid >}}

The tests cross-check generated keys against the JCE and OpenSSL conversions. The PR reports a 294-case truncation check that ensures failures do not escape as `ArrayIndexOutOfBoundsException`. It also records a provider limitation in its Java 8 test environment: the SunEC provider was absent, so platform acceptance of EC keys was not exercised there even though the byte-rewrapping assertions ran.

Parsing a valid key is not certificate validation, signature verification, or proof of ownership. Those are separate operations. A reusable parser removes application-level format code while keeping that trust boundary visible.

## End the task when the session is finished

[PR #5746](https://github.com/codenameone/CodenameOne/pull/5746) adds an explicit way to remove an Android task from recents when exiting.

Android's process and its task have separate lifecycles. The old exit path killed the process but could leave the task available in the switcher.

The new request is explicit:

```java
import com.codename1.ui.CN;

// Call after the application's sign-out cleanup has completed.
CN.exitAndClearTask();
```

On Android, the port calls `Activity.finishAndRemoveTask()` on the UI thread and then follows the existing process-exit behavior. If no Activity exists, or on an Android version before API 21, it falls back to ordinary exit. Other ports retain their existing exit behavior. `CN.isExitAndClearTaskSupported()` allows the UI to distinguish support.

The `setOnExit` callback still runs. If sign-out requires asynchronous server work, finish that work before invoking the exit rather than assuming a callback can keep the process alive until a request completes.

## What task removal protects, and what it leaves to the app

Removing the task prevents that task from remaining as an ordinary entry in the task switcher. It does not revoke a token, erase local files, clear a server session, or stop someone launching the app again from its icon. It also does not substitute for protecting sensitive screenshots while the task is active.

For an app using {{< post-link path="/blog/continuity-restoring-work" text="continuity" >}}, clearing and disabling saved-state restoration is another explicit part of logout:

```java
Continuity.clear();
Continuity.disable();
// Complete account/session cleanup, then request the exit.
CN.exitAndClearTask();
```

This fragment shows the framework calls, not a complete sign-out implementation. Application cleanup belongs between restoring-state shutdown and process termination, including any credentials or cached account data the product stores.

The PR tested the exact Android removal-plus-immediate-kill sequence in a small API 36 emulator app. It reports removal in 29 of 29 executed trials, while the process-kill-only control left the task present. One additional driving broadcast did not reach the handler and therefore did not test the sequence.

## Make the common security operations harder to get wrong

The location button, PEM parser, and task-clearing exit each replace a piece of platform or format code that app teams should not have to reconstruct. They also have specific limits: a parser cannot decide whom to trust, and removing a task cannot revoke a credential. Clear APIs help developers put those responsibilities in the right place.

That work closes a week in which we also reduced garbage, made caches reclaimable, improved maps and boxed values, and removed unnecessary native waits and JavaScript suspension. Continuity and native drag and drop give users more ways to move their work; integrated Javadoc makes the contracts easier to find. The {{< post-link path="/blog/performance-work-between-benchmarks" text="release overview" >}} links the full series.

We are strengthening Codename One's advantage by maintaining the Java API, runtime, and platform integration together. That lets a reference-safety fix, a stricter key parser, or an OS-owned consent control reach applications through the framework. Customers should spend less time repairing platform glue and more time on the authorization and data rules specific to their product.

API 37 preparation continues before we require a migration, and more performance work is already underway. We will keep reporting the measurements that improve and the cases that still need attention. A passing compile and a flattering benchmark are useful starting points; neither finishes the job.

---

## Discussion

_Does your sign-out flow test saved-state restoration and relaunch from recents as well as launch from the app icon?_

{{< giscus >}}
