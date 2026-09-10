---
title: "Read the Key File You Have. End the Task You Meant to End."
slug: pem-keys-and-clearing-recents
url: /blog/pem-keys-and-clearing-recents/
date: '2026-09-16'
author: Shai Almog
description: "Codename One adds direct PEM key parsing and an Android exit that clears the task from recents. The APIs make format and lifecycle behavior explicit while leaving trust and sign-out to the application."
feed_html: '<img src="https://www.codenameone.com/blog/pem-keys-and-clearing-recents.jpg" alt="Keys And Clean Exits" /> Codename One adds direct PEM key parsing and an Android exit that clears the task from recents. The APIs make format and lifecycle behavior explicit while leaving trust and sign-out to the application.'
series: ["release-2026-09-11"]
---

![Keys And Clean Exits](/blog/pem-keys-and-clearing-recents.jpg)

An armored PEM file passed to an API expecting DER produces an unhelpful key-format error. Killing an Android process can leave its task in the task switcher, even though the application believes the session has ended. Both behaviors leave developers to infer what an API actually did.

Two changes in this week's release narrow that ambiguity: [PEM parsing in PR #5707](https://github.com/codenameone/CodenameOne/pull/5707) and [task-clearing exit in PR #5746](https://github.com/codenameone/CodenameOne/pull/5746).

## PEM is more than Base64 with a heading

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

## Exiting and removing a task are different requests

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

## Secure defaults need precise verbs

The {{< post-link path="/blog/performance-work-between-benchmarks" text="rest of the release" >}} reduces runtime work and adds platform integration. These two APIs reduce the amount of security-sensitive interpretation left to application glue code. `fromPem` states the input it accepts. `exitAndClearTask` states the lifecycle operation it requests.

That is a practical way to strengthen Codename One's security story: provide the common operation, validate its boundary, and document the responsibility that remains with the application. A safer default is useful when the next developer can tell exactly what it guarantees.

---

## Discussion

_Does your sign-out flow test relaunch from recents as well as launch from the app icon?_

{{< giscus >}}
