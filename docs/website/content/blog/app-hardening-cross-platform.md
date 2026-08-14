---
title: "App Hardening: One Obfuscation Pipeline Across Every Port"
slug: app-hardening-cross-platform
url: /blog/app-hardening-cross-platform/
date: '2026-08-15'
author: Shai Almog
description: "Codename One App Hardening applies class and member renaming, string encryption, and platform-safe control-flow transforms before one application becomes Android, iOS, JavaScript, or native desktop output."
feed_html: '<img src="https://www.codenameone.com/blog/app-hardening-cross-platform.jpg" alt="Source code passing through a hardened build pipeline before splitting into mobile, web, and desktop binaries" /> Codename One App Hardening applies class and member renaming, string encryption, and platform-safe control-flow transforms before one application becomes Android, iOS, JavaScript, or native desktop output.'
series: ["release-2026-08-14"]
---

![Source code passing through a hardened build pipeline before splitting into mobile, web, and desktop binaries](/blog/app-hardening-cross-platform.jpg)

Obfuscating only the Android artifact is a poor security model for a cross-platform application. [Codename One App Hardening](https://github.com/codenameone/CodenameOne/pull/5527) transforms the merged application before it splits into Android, iOS, JavaScript, Windows, Linux, and desktop builds.

We set the target at DexGuard-class resistance: rename useful symbols, remove plaintext application strings where the target permits it, distort selected control flow, and keep crash reports readable. This is not a claim that reverse engineering becomes impossible. It is a commitment to make the same security decision cover the whole application instead of leaving every port to a different tool and configuration.

This post continues [this week's release overview and GUI Builder rewrite](/blog/third-generation-gui-builder/) and [last week's App Shield release](/blog/app-shield-server-attestation/).

## One transform before six platform builds

App Hardening runs on the merged application JAR inside the cloud build service. That placement matters. The engine sees application classes and bundled libraries before ParparVM translates bytecode to C, before R8 handles Android, and before the JavaScript or native desktop backends consume the program.

{{< mermaid >}}
flowchart TD
    A[Merged application JAR] --> B[Demux application and libraries]
    B --> C[Rename classes and members<br/>except Android, where R8 remains the renamer]
    C --> D[Encrypt eligible string constants]
    D --> E[Apply control-flow transforms<br/>only on safe targets]
    E --> F[Verify transformed bytecode]
    F --> G{Platform builders}
    G --> H[Android and R8]
    G --> I[iOS and ParparVM]
    G --> J[JavaScript]
    G --> K[Windows, Linux, and JavaSE]
    F --> L[Mapping and build report]
    L --> M[Crash Protection retrace]
{{< /mermaid >}}

[PR #5527](https://github.com/codenameone/CodenameOne/pull/5527) carries the open-source engine, retrace support, client API, build preflight, crash payload changes, and documentation. [BuildDaemon PR #173](https://github.com/codenameone/BuildDaemon/pull/173) wires the engine into the cloud builders and enforces the Enterprise entitlement on the server.

The server gate is intentional. A non-Enterprise build that requests hardening fails with an explanation. It never returns an ordinary binary that looks protected because the client asked for protection.

## Turn it on with one level

For most projects the only setting is:

```properties
codename1.arg.harden.level=standard
```

The levels are cumulative:

| Level | Renaming | String encryption | Control flow |
| --- | --- | --- | --- |
| `off` | No | No | No |
| `standard` | Yes | Constant strings | No |
| `aggressive` | Yes | All eligible strings | One opaque-predicate guard |
| `paranoid` | Yes | All eligible strings | Two guards per eligible method |

You can override the individual transforms with `harden.rename`, `harden.strings`, and `harden.controlFlow`. A per-platform `harden.<platform>.enabled=false` hint opts one target out. An unknown level fails the build instead of becoming `off`.

Classes resolved by name need a keep rule:

```properties
codename1.arg.harden.keep=-keep class com.example.payment.NativeGateway { *; }
```

The engine already keeps the application entry point, generated bootstraps, and native-interface peers. Codename One does not use runtime reflection to resolve ordinary application classes, which removes a large source of keep-rule guesswork. A third-party library that loads package-relative resources can still need an explicit rule because its package name changes while the resource path does not.

## The port matrix is deliberately uneven

Applying every transform everywhere would produce a larger binary without adding protection, or break a backend optimizer. App Hardening uses one policy but adapts the mechanics:

| Transform | Applied to | Why not everywhere |
| --- | --- | --- |
| Class, method, and field renaming | iOS, JavaScript, Windows, Linux, JavaSE | Android keeps R8 as its only renamer to avoid chained renaming during the build. |
| String encryption | iOS, Android, Windows, Linux, JavaSE | JavaScript strings can be live references passed through the native bridge. |
| Control-flow obfuscation | Android and JavaSE | It conflicts with ParparVM optimization and inflates JavaScript output. Constructors are excluded. |

The renaming dictionary uses a `zq` prefix rather than the familiar `a`, `b`, and `c`. Short names can appear inside ParparVM's generated native identifiers and confuse dead-code elimination. The longer prefix keeps the names opaque without disabling that optimizer.

A mapping looks like this in the retrace tests:

```text
com.example.MyForm -> zqaaaa:
    10:10:void onSave():42:42 -> zqa
com.example.util.Helper -> zqaaab:
```

The mapping gets a build-specific ID and stays on the server for Crash Protection. Hardened stack traces carry that ID, the raw stack, trace format, and hardening level. The server retraces the report before filing the GitHub issue.

## String encryption has sharp edges

Renaming hides labels. String encryption removes eligible application literals from the shipped binary and synthesizes a decoder inside each class with a per-class key. There is no single framework decoder for an attacker to hook.

The engine handles both ordinary `LDC` literals and `static final String` values stored in the class-file `ConstantValue` attribute. It also reports strings it cannot transform.

Three boundaries matter:

1. Annotation values remain in annotation metadata and stay readable.
2. JDK 9 style `invokedynamic` string-concatenation recipes can keep literal fragments outside ordinary `LDC` instructions. The build report counts those sites. Compiling with `-XDstringConcat=inline` moves them back into instructions the engine can transform.
3. An encrypted literal is value-equal to the same unencrypted framework string but may not be the same object. Compare strings with `.equals()`, not `==`.

None of this turns a client-side constant into a secret. If the app can decrypt a value, an attacker controlling the process can eventually observe it. Credentials and signing secrets still belong on a server or in platform-backed secure storage.

## Control flow is a cost, not a badge

Aggressive and paranoid levels add opaque predicates to eligible methods. A decompiler must keep branches that the application can resolve at runtime but static analysis cannot fold away.

The engine skips constructors and checks method growth before adding bytecode. It verifies the transformed classes with ASM's `CheckClassAdapter` before any platform builder sees them. The build report identifies methods or literals that were skipped instead of claiming a transform covered bytes it did not touch.

`paranoid` doubles the eligible control-flow guards. It also increases size and analysis complexity inside your own build. Start with `standard`, test a release build, then raise the level for code where the added cost has a reason.

## Crash reports remain part of the contract

Obfuscation without retrace trades one security problem for an operational one. A production crash that says `zqaaaa.zqa()` does not help the team responsible for fixing it.

App Hardening and Crash Protection share the mapping lifecycle. Mapping upload is required by default. If upload fails, the build fails rather than ship an artifact whose future crashes cannot be decoded.

Application code can inspect the stamped result:

```java
if (Hardening.isHardened()) {
    Log.p("Hardening level: " + Hardening.getLevel());
    Log.p("Mapping: " + Hardening.getMappingId());
}
```

The simulator and local source builds report `false` and `off` because they are never hardened. A local target fails preflight unless you explicitly set `harden.allowUnhardenedLocalBuild=true`. That escape hatch permits the build; it does not pretend to protect it.

## Hardening, Shield, and encrypted data stop different attacks

Last week's [App Shield](/blog/app-shield-server-attestation/) release gives a backend a server-verified attestation token. Patching a local boolean is no longer enough to impersonate a trusted app when the server enforces that token.

App Hardening works earlier in the attack. It raises the cost of finding the code and constants an attacker wants to patch. App Shield makes a successful local patch insufficient for protected server calls. The open [portable encrypted database PR](https://github.com/codenameone/CodenameOne/pull/5526) adds the data-at-rest layer with interoperable encrypted SQLite files and keystore-managed keys. That database work is not merged yet, so it is direction rather than part of this release.

{{< mermaid >}}
flowchart LR
    A[App Hardening<br/>binary inspection and tampering] --> B[App Shield<br/>app-to-server trust]
    B --> C[Backend authorization<br/>business operation]
    D[Encrypted database<br/>data at rest, PR open] --> A
{{< /mermaid >}}

The security lead we are building comes from covering these boundaries together. Release builds already obfuscate by default. Enterprise teams can now add one cross-platform hardening policy, server-enforced attestation, Crash Protection retrace, and, once PR #5526 finishes review, portable database encryption. Each layer has a named failure mode and a testable output.

App Hardening still cannot stop a determined attacker who controls the device. It does not replace authorization, rate limits, secure key custody, or review of the operation your backend performs. It makes static analysis and casual tampering more expensive across every artifact you ship, which is the job an obfuscation layer can defend.

Start with `standard`, submit a release cloud build, and check the hardening report before moving to a stronger profile. The [App Hardening guide](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/App-Hardening.asciidoc) documents every hint, exclusion, and local-build boundary.

---

## Discussion

_Which part of a shipped mobile binary has caused the most security review work for your team: symbols, string constants, control flow, or readable crash reports?_

{{< giscus >}}
