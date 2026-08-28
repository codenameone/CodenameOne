---
title: "Rootless Jailbreak Detection: Updating the Signals, Not the Claim"
slug: rootless-jailbreak-detection
url: /blog/rootless-jailbreak-detection/
date: '2026-09-01'
author: Shai Almog
description: "Codename One's iOS integrity checks now detect current rootless jailbreak layouts, cross-check hooked APIs, inspect mounts and loaded images, and rerun on foreground entry."
feed_html: '<img src="https://www.codenameone.com/blog/rootless-jailbreak-detection.jpg" alt="An iPhone integrity scan finding a rootless jailbreak path and hooked system call" /> Codename One&apos;s iOS integrity checks now detect current rootless jailbreak layouts, cross-check hooked APIs, inspect mounts and loaded images, and rerun on foreground entry.'
series: ["release-2026-08-28"]
---

![An iPhone integrity scan finding a rootless jailbreak path and hooked system call](/blog/rootless-jailbreak-detection.jpg)

`ios.detectJailbreak=true` did not fire on a device jailbroken with palera1n 2.1.1 in rootless mode. The build hint was wired correctly. The detector was asking questions that described the previous generation of jailbreaks.

[PR #5584](https://github.com/codenameone/CodenameOne/pull/5584) replaces that signal set. It recognizes rootless bootstraps, checks whether detection APIs disagree, inspects mounts and loaded images, and reruns the gate when the application returns from the background.

For native desktop windows, nearby devices, wearable follow-through, and the rest of this release, see the [weekly release overview](/blog/native-desktop-windows/).

## Rootless means the old signals can be correctly absent

A `rootful` jailbreak modifies or replaces files on the system volume. The traditional detector looked for `Cydia` and `apt` paths, Substrate-era injected libraries, a writable root filesystem, and a successful write outside the application sandbox.

Current jailbreaks such as `palera1n`, `Dopamine`, and `XinaA15` keep the signed system volume sealed. They place the `Procursus` bootstrap under `/var/jb`, inject through frameworks such as `ElleKit`, and commonly use `Sileo`. Writing to the root filesystem still fails because that is the design.

The old detector could therefore receive the correct answer to every old probe and make the wrong integrity decision.

{{< mermaid >}}
flowchart TD
    A[iOS integrity scan] --> P[Filesystem paths]
    A --> M[Mount table]
    A --> L[Loaded images]
    A --> U[Registered URL schemes]
    P --> R1[Rootful artifacts]
    P --> R2[Rootless /var/jb and Procursus artifacts]
    M --> R3[Writable root or bootstrap mount]
    L --> R4[ElleKit, Frida, Substrate and related images]
    U --> R5[Sileo, Filza, Zebra, Cydia]
    P --> C[Independent API cross-check]
    L --> C
    C --> H[Hooked detection API signal]
{{< /mermaid >}}

## `lstat` sees the symlink after its target is gone

`/var/jb` is commonly a symlink. On a rebooted semi-tethered device, the target can be absent while the link remains. `stat`, `access`, and `NSFileManager.fileExistsAtPath` follow the link and report that the target is missing.

The new check uses `lstat`, which reports the link itself. It checks rootless bootstrap paths separately from the `rootful` file list and emits a `rootlessPath` signal.

It also distinguishes three results: present, absent, and denied. Collapsing `EPERM` into `ENOENT` loses information and can manufacture a false disagreement between two probes that were both refused.

The mount scan does not depend on guessing one path. It detects a writable root file system and additional bootstrap file systems associated with a rootless jailbreak.

## A detector should notice when its own answers are filtered

Jailbreak-bypass tweaks hook common filesystem and image APIs so they return clean answers. Checking more paths through the same hooked method adds length without adding an independent signal.

The detector now asks equivalent questions through different entry points. It compares libc `lstat` with a raw system trap where the platform permits it. It compares the image name returned by `_dyld_get_image_name` with the path resolved by `dladdr`.

The two answers should agree on an untouched process. When one path sees an artifact and the other denies it, `DeviceIntegrity` reports the instrumentation as `frida`, which is the public reason category for hooking and dynamic instrumentation.

The raw trap is not compiled on watchOS or tvOS because those SDKs mark `syscall` unavailable. Rootless filesystem and image checks still compile there; only the second opinion is omitted on platforms where a jailbreak is not the target threat.

Loaded-image matching now includes current rootless injectors such as `ElleKit` alongside `Frida` and older Substrate names. The implementation avoids walking `dyld`'s live internal table, which can move under another thread. `dladdr` gives the independent answer without dereferencing a structure intended to be read while a process is suspended.

## The package-manager check now starts with Sileo

`canOpenURL` returns `false` for a scheme the application did not declare, even if the app handling it is installed. The build now adds jailbreak-related schemes to `LSApplicationQueriesSchemes` when detection is enabled.

The order is deliberate:

```text
sileo
filza
zbra
cydia
```

iOS limits how many query schemes an application can declare. The jailbreak probe is lower priority than the schemes requested by application features. The builder appends as many detector schemes as fit after every other feature has claimed its slots. `Sileo` comes first so a one-slot remainder checks the package manager used by current rootless devices rather than `Cydia`.

`canOpenURL` is only one signal and is deprecated as of iOS 27. The filesystem, mount, and image checks carry the main weight.

## Hard gate or runtime policy

The zero-code launch gate remains one build hint:

```properties
codename1.arg.ios.detectJailbreak=true
```

It now runs at launch and whenever the application returns from the background. That closes a simple attach-after-launch gap. A detected gate exits with a failure status instead of telling the operating system that the process ended normally.

An application that needs a narrower response can inspect the same signal family at runtime:

```java
if (DeviceIntegrity.isDeviceCompromised()) {
    String[] reasons = DeviceIntegrity.getCompromiseReasons();
    requireStepUpAuthentication(reasons);
}
```

The runtime API does not cache its result, so a check before a sensitive operation can see instrumentation attached after startup. It does not terminate the process. The application can warn, degrade one feature, or request additional authentication.

## This raises bypass cost; it does not establish trust

The PR compiled the detector across device, simulator, Mac Catalyst, watchOS, and tvOS configurations. The symlink behavior and independent-probe logic ran in a native harness. Builder tests pin scheme merging and ordering.

The change was not verified on a jailbroken device during the PR. The original report names real hardware, but the revised detector still needs an on-device confirmation before we call that case closed.

More importantly, every probe runs inside a process controlled by the attacker. A sufficiently capable bypass can patch the final boolean, skip the exit, or modify the application around the check.

High-value authorization belongs on the backend. Bind an Apple App Attest or Google Play Integrity token to a fresh server nonce, verify the token with the platform provider, and let the backend decide whether to permit the action. App Shield packages that server-side verification and policy when an application team does not want to build it directly.

{{< mermaid >}}
flowchart LR
    A[On-device jailbreak signals] --> B[Warn, degrade, or request step-up]
    C[Hardware-backed attestation] --> D[Backend verification]
    D --> E[Permit or reject sensitive action]
    A -. raises bypass cost .-> E
    A -. does not authorize .-> D
{{< /mermaid >}}

## Security work that states its boundary

This week adds multiple native desktop windows, selected document publication, nearby-device APIs, complete watch complications, and current rootless jailbreak signals. The features are broad. Their access paths are narrow.

A window owns one paint and input surface. A document provider publishes one read-only tree. Nearby features bring only the frameworks their package uses. A watch face receives a bounded timeline, not a live application process. Jailbreak detection supplies a local risk signal, while backend attestation remains the trust decision.

Codename One keeps strengthening its security lead by putting these boundaries in the ordinary API. Secure-by-default programming is not a claim that every device is safe. Restricted access, capability checks, explicit failure, and server verification should be the shortest supported path.

If you enable `ios.detectJailbreak`, test the generated build on the device classes your policy covers. Keep the runtime response proportionate, and gate money movement, credential changes, or sensitive exports on a verdict your backend verified.

---

## Discussion

_Which actions in your application require a backend integrity verdict, and which can use a local risk signal?_

{{< giscus >}}
