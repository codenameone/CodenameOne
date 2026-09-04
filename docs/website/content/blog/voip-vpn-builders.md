---
title: "VoIP, VPN, and the Build System Behind Them"
slug: voip-vpn-builders
url: /blog/voip-vpn-builders/
date: '2026-09-04'
author: Shai Almog
description: "Codename One adds native call management and VPN APIs, while its builders generate the platform services, frameworks, permissions, and extension plumbing only when an application uses them."
feed_html: '<img src="https://www.codenameone.com/blog/voip-vpn-builders.jpg" alt="A phone, secure tunnel, and native build pipeline" /> Codename One adds native call management and VPN APIs, while its builders generate the platform services, frameworks, permissions, and extension plumbing only when an application uses them.'
series: ["release-2026-09-04"]
---

![A phone, secure tunnel, and native build pipeline](/blog/voip-vpn-builders.jpg)

In 2019, a reader asked whether our WhatsApp clone included VoIP. [I said no](/blog/whatsapp-clone-ga/#comment-24081). It would require native integration, and the setup was too much for that project. For years, that was the line.

VPN sat behind the same line. These features do not live neatly inside the application process. On iOS, a VoIP push can arrive before the application installs a Java listener, yet CallKit expects the incoming call to reach the lock screen on its deadline. A Call Directory integration is a separate extension executable with its own lifecycle and signing identity. Managed VPN adds Network Extension entitlements and profile rules. Android solves the same jobs through `ConnectionService`, `VpnManager`, `VpnService`, manifest declarations, and foreground-service rules.

Wrapping those APIs in Java is the small part. A cross-platform runtime cannot add a service before installation, answer a native callback before the application is ready, or create and sign another executable inside the application bundle. Once every team must maintain those pieces in Xcode and Android Studio, the shared abstraction has stopped at the hardest part.

Codename One has a secret weapon for that problem: builders.

Builders already assemble the native product, so they can change its shape based on the Java packages the application actually uses. They link frameworks, inject permissions and background modes, generate native delegates and services, and create extension targets when needed. Leave a package out and its native machinery stays out too.

[PR #5604](https://github.com/codenameone/CodenameOne/pull/5604) is where we crossed that old line. It introduced first-class call management, VoIP push handling, managed VPN, and the packet-tunnel API and Android host. [PR #5693](https://github.com/codenameone/CodenameOne/pull/5693) completes the iOS path by generating a separately translated and signed Network Extension. The Java API is the visible result. The bigger change is that builders assemble the system integration around it without turning every application team into the maintainer of two native build projects.

## TL;DR

- [`Calls`](#a-call-is-a-system-session-before-it-is-a-media-session) connects application call state to CallKit on iOS and self-managed `ConnectionService` on Android. Your media and signaling stack remain yours.
- [`Vpn`](#managed-vpn-with-an-important-platform-boundary) installs and controls managed IKEv2 profiles on iOS and Android. iOS also supports managed IPsec profiles.
- [`VpnTunnel`](#the-packet-tunnel-boundary) exposes raw packets for an application-implemented tunnel on Android and iOS. [The iOS builder work](https://github.com/codenameone/CodenameOne/pull/5693) generates and signs a separate Network Extension containing the translated Java tunnel.
- {{< post-link path="/blog/dialogs-in-native-windows" text="Dialogs and secondary windows" >}} now work together, including an opt-in native modal window.
- {{< post-link path="/blog/native-appkit-mac-port" text="The new AppKit port" >}} builds a real Mac application instead of presenting an iOS application through Catalyst.
- {{< post-link path="/blog/sms-otp-autofill" text="SMS one-time-code autofill" >}} fills a verification code without asking to read the inbox.
- {{< post-link path="/blog/platform-deprecation-watch" text="A daily platform watch" >}} now turns Apple and Google notices into source-level checks before deadlines become fires.
- {{< post-link path="/blog/private-contact-picker" text="The new Contact Picker" >}} returns selected contact data without broad address-book access.

## A call is a system session before it is a media session

A VoIP application has two timelines. Its signaling server and media engine have one. The operating system has another. On iOS, an incoming VoIP push must be reported to CallKit immediately. The Java application may not be running yet. On Android, a self-managed connection participates in system call routing and audio focus even when the application's own screen is elsewhere.

The new API keeps those responsibilities separate:

```java
if (!Calls.isSupported()) {
    return;
}

Calls.configure(new CallConfiguration()
        .displayName("Acme Talk"));

Calls.addActionListener(new CallActionAdapter() {
    public void answerRequested(String callId, CallAction action) {
        signalling.accept(callId);
    }

    public void audioSessionActivated(CallAudioSession session) {
        media.start(session.getCallId());
    }

    public void providerReset() {
        media.stopEverything();
    }
});
```

`Calls` does not choose a codec, move audio packets, or invent a signaling protocol. It owns the bridge to the system call UI and lifecycle. The application starts media only after the native audio session activates, not merely because the user tapped Answer.

The package boundary also carries meaning. `com.codename1.call.session` adds the core system call integration. `com.codename1.call.voip` adds PushKit and the iOS VoIP background mode. `com.codename1.call.directory` adds directory integration without silently pulling in the self-managed calling permission. Apple can reject an application that declares VoIP background execution without using it, so importing less code must build less native machinery.

{{< mermaid >}}
sequenceDiagram
    participant Push as Push service
    participant Native as Native call bridge
    participant OS as System call UI
    participant Java as Codename One app
    participant Media as App media engine
    Push->>Native: Incoming call payload
    Native->>OS: Report call before deadline
    OS->>Java: answerRequested(callId)
    Java->>Java: Accept through signaling server
    OS->>Java: audioSessionActivated(callId)
    Java->>Media: Start media
{{< /mermaid >}}

The simulator supplies deterministic call and audio-session events. That makes the awkward orderings testable before a build reaches a phone.

## Managed VPN with an important platform boundary

Many enterprise applications do not implement a VPN protocol. They install an operating-system profile and ask the platform to connect it. That path now has one Java API:

```java
if (!Vpn.isSupported()) {
    return;
}

VpnProfile profile = new VpnProfile("vpn.example.com")
        .protocol(VpnProtocol.IKEV2)
        .remoteIdentifier("vpn.example.com")
        .localIdentifier("alice")
        .usernamePassword("alice", secret)
        .displayName("Acme Corporate");

Vpn.install(profile).onResult((ok, err) -> {
    if (err == null) {
        Vpn.start();
    }
});
```

iOS and Android 11 or newer support managed IKEv2. iOS also supports managed IPsec with a pre-shared key. The operating system owns user consent, credential storage, and the active configuration. The API exposes those boundaries rather than storing a VPN password in application preferences.

### The packet tunnel boundary

For work that does not fit a managed VPN profile, `VpnTunnel` lets application code inspect raw IP packets and decide whether to forward or drop them:

```java
public final class LocalTunnel extends VpnTunnel {
    public LocalTunnel() {
    }

    protected void onStart(TunnelConfiguration configuration) {
    }

    protected void onPacket(PacketBuffer packet) {
        if (packet.getFamily() == PacketBuffer.FAMILY_IPV4) {
            forward(packet);
        }
    }

    protected void onStop(TunnelStopReason reason) {
    }
}
```

The same Java packet loop runs on Android and iOS, but the process boundary is different. Android hosts it in a `VpnService` inside the application process. iOS requires a `NEPacketTunnelProvider` Network Extension: a separate executable with its own bundle identifier, provisioning profile, memory limit, and translated VM. The builder work in [PR #5693](https://github.com/codenameone/CodenameOne/pull/5693) creates that target, translates from the selected `VpnTunnel` class instead of the UIKit application shell, adds the framework and entitlements, and embeds the signed extension in the application.

The iOS extension contains the translated tunnel and VM, but not the Codename One networking stack. It can inspect, rewrite, drop, and `forward` packets on the device. It cannot open a remote connection through `com.codename1.io.Socket`, `ConnectionRequest`, or ParparVM's `java.net`. Android can relay packets because its tunnel runs in the application process. A protocol that needs a remote relay should put that transport behind a platform-specific interface instead of assuming the two hosts are identical.

Apple grants the Network Extension entitlement case by case, so the target is deliberately opt-in. Once the App ID has that entitlement, name the tunnel class in `codenameone_settings.properties`:

```properties
codename1.arg.ios.vpn.tunnel=true
codename1.arg.ios.vpn.tunnel.class=com.acme.LocalTunnel
codename1.arg.ios.appext.CN1VpnTunnel.provisioningURL=https://example.com/CN1VpnTunnel.mobileprovision
```

Both the host application and `<packageName>.vpntunnel` App IDs need Apple's Network Extension entitlement. The extension also needs its own provisioning profile. It starts fresh, requires an accessible no-argument constructor, and cannot see application statics or `Display`. Pass local configuration through `TunnelSetup.data` and read it in `VpnTunnel.onStart()`. This is a real cross-platform API without pretending the two operating systems have the same lifecycle or networking facilities.

## The builder is part of the runtime contract

A browser application cannot register CallKit, a self-managed Android connection service, or an Apple Network Extension. Flutter, React Native, and .NET MAUI can reach those APIs through native plugins and platform projects. The difficult work then lives in target membership, manifests, entitlements, background modes, native delegates, and signing.

Builders change where that difficult work lives. The build server already owns the native product graph. It can see which Java packages survive into the application and generate the platform pieces that match them:

{{< mermaid >}}
flowchart LR
    J[Referenced Java packages] --> C[Platform feature catalog]
    C --> I[iOS frameworks<br/>delegates and modes]
    C --> A[Android services<br/>permissions and manifest]
    C --> E[Extension targets<br/>and signing preflight]
    C --> S[Simulator implementation]
    U[Unused packages] --> N[No native baggage]
{{< /mermaid >}}

Dependency injection cannot do this work. The builder can add a service that must exist before Java starts, compile a native callback that meets an operating-system deadline, and sign an extension as part of the same product. Application teams keep control of their UI, protocol, and business logic without maintaining a parallel native build system.

## Dialog can become a native modal window

Last week's [native-window release](/blog/native-desktop-windows/) named several components that still assumed every top level was a `Form`. [PR #5624](https://github.com/codenameone/CodenameOne/pull/5624) closes most of that list. `Dialog`, `Sheet`, `ToastBar`, combo-box popups, floating-action submenus, progress overlays, tooltips, and `HTMLComponent` now resolve the window that contains them. Accessibility state is tracked per window as well.

A dialog can remain a lightweight overlay inside its owner, or opt into a real operating-system window:

```java
Dialog.setDefaultNativeWindowMode(true);
boolean delete = Dialog.show(
        "Confirm deletion",
        "Delete Quarterly report.pdf?",
        "Delete",
        "Cancel");
```

The {{< post-link path="/blog/dialogs-in-native-windows" text="window follow-up" >}} covers precedence, fallback behavior, anchored popups, and the remaining limits.

## A Mac application built on AppKit

Mac Catalyst helped us get an iOS application onto macOS, but its desktop behavior remained bounded by UIKit. It could not supply several ordinary window operations, and a secondary 4K surface could require roughly 33 MB for each intermediate frame before the image was copied into place.

[PR #5601](https://github.com/codenameone/CodenameOne/pull/5601) replaces the default Mac native path with AppKit. The generated application now owns `NSApplication`, `NSWindow`, `NSMenu`, `NSScreen`, and a `CAMetalLayer` per window. Always-on-top, utility windows, minimize, restore, maximize, native modality, and independent dirty-region painting are real desktop operations.

The {{< post-link path="/blog/native-appkit-mac-port" text="AppKit article" >}} explains the rendering change, build targets, test evidence, and the native accessibility bridge to VoiceOver.

## OTP without inbox permission

[PR #5642](https://github.com/codenameone/CodenameOne/pull/5642) adds a one-time-code constraint plus reusable phone-number and verification components. On iOS, Android, and the web, the operating system can offer the code from an SMS without giving the application permission to read messages.

```java
PhoneVerification verification = new PhoneVerification();
verification.setCodeSender((number, response) ->
        server.sendCode(number, response));
verification.setCodeVerifier((number, code, response) ->
        server.verifyCode(number, code, response));
verification.addVerifiedListener(evt -> showAccount());
```

Codename One does not send or verify the SMS. Your server still owns expiry, attempt limits, rate limits, and session issuance. The {{< post-link path="/blog/sms-otp-autofill" text="OTP article" >}} includes both the complete component and the lower-level text-field option.

## Stop learning about platform changes from failed builds

Until this week, an Apple or Google change usually reached us through a community report. That is useful, but it starts the clock after somebody is already exposed.

We now run a daily scheduled Codex task that reads official platform notices, extracts the requirement and date, traces the affected Codename One producer, and checks for an existing issue or fix. It found the Android 16 back-navigation gap repaired in [PR #5673](https://github.com/codenameone/CodenameOne/pull/5673), and the privacy direction that led to the Contact Picker in [PR #5680](https://github.com/codenameone/CodenameOne/pull/5680).

The task does not make a notice true, and it does not replace platform tests. Its job is to connect a primary-source deadline to the exact builder, generated project, or API that must change. The {{< post-link path="/blog/platform-deprecation-watch" text="platform-watch article" >}} shows that chain. Contact selection gets its own {{< post-link path="/blog/private-contact-picker" text="code-focused article" >}} because the permission model deserves more than a paragraph.

## Four smaller changes with a large blast radius

### Google Play Billing 8

[PR #5651](https://github.com/codenameone/CodenameOne/pull/5651) moves the default Android billing dependency to 8.0.0 and completes the move from the retired SKU API to `ProductDetails`. Google's [billing deprecation schedule](https://developer.android.com/google/play/billing/deprecation-faq) now makes version 8 the minimum for updates. The builder absorbs a generated-project migration that most application teams should never have to chase themselves.

### JavaScript density now means device pixels

[PR #5634](https://github.com/codenameone/CodenameOne/pull/5634) fixes display width, height, and millimeter conversion when the browser device-pixel ratio is neither one nor two. A viewport that is 390 CSS pixels wide at a ratio of three now reports 1,170 device pixels.

This can change layout and screenshot results in existing JavaScript applications. Read `CN.getProperty("browser.window.devicePixelRatio", "1")` when a breakpoint genuinely needs CSS pixels.

### The mouse wheel is no longer a fake drag

Desktop ports used to translate one wheel turn into a press, three drag events, and a release. That could trigger a click when nothing scrolled. [PR #5660](https://github.com/codenameone/CodenameOne/pull/5660) routes native wheel events directly.

Built-in scrolling finds the nearest scrollable ancestor. A custom component that consumes wheel input should now override `mouseWheel(WheelEvent)` or register a listener:

```java
chart.addMouseWheelListener(evt -> {
    WheelEvent wheel = (WheelEvent) evt;
    if (wheel.isControlDown()) {
        zoomBy(wheel.getDeltaY());
        evt.consume();
    }
});
```

### The developer guide became a maintained product

We removed obsolete IDE and Ant-era guidance, folded the Maven workflow into the main book, restored missing samples, and reworked the order of several chapters. More important, CI now checks the guide's structure, cross-references, promised code blocks, served links, images, prose, and generated output.

Documentation drift is now a failing build instead of a cleanup project for some future week. The restored call, VPN, desktop, security, identity, event-thread, graphics, and performance material is part of the release contract.

## Native reach with narrower defaults

This week stretches Codename One in both directions. Calls, VPN profiles, AppKit windows, contact selection, and OTP reach deeper into each operating system. Package-triggered builders keep that reach narrow. An application does not receive VoIP background execution because another product needed it. It does not receive broad contacts access to pick one person. It does not gain a message-reading permission to fill six digits.

Secure-by-default programming depends on small public APIs, generated native integration, explicit platform limits, and fewer opportunities to turn a convenience feature into permanent access. You still own the application UI and the protocol choices. The builders absorb the native product plumbing needed to ship them.

Start with the [Call Management](/developer-guide/#_call_management) or [VPN](/developer-guide/#_vpn) chapter, then test the failure path in the simulator before sending a native build.

---

## Discussion

_Which native capability have you avoided because the platform project was harder than the application code?_

{{< giscus >}}
