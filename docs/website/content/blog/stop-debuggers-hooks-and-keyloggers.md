---
title: "Stop Debuggers, Hooking Tools and Keyloggers Before They Reach Your Users' Money"
slug: stop-debuggers-hooks-and-keyloggers
url: /blog/stop-debuggers-hooks-and-keyloggers/
date: '2026-12-01'
author: Shai Almog
description: "android.debugGuard refuses to run under a debugger, and optionally with Developer Options or USB debugging on. Root checks now catch Magisk, KernelSU and APatch, hook checks catch Frida and the Xposed family. TextArea.SENSITIVE, third-party keyboard detection and SecureKeyboard keep keyloggers away from PINs and card numbers, on every platform."
feed_html: "<img src=\"https://www.codenameone.com/blog/stop-debuggers-hooks-and-keyloggers.jpg\" alt=\"Stop debuggers, hooks and keyloggers\" /> Debugger, root and hooking detection with one build hint each, and a keyboard of your own for PINs and card numbers, from one Java codebase."
series: ["release-2026-11-27"]
---

![Stop debuggers, hooks and keyloggers](/blog/stop-debuggers-hooks-and-keyloggers.jpg)

Most apps never need what's in this post. If you build a banking app, a wallet, a trading app or anything that moves money, you need all of it, and your security auditor has a checklist that says so. Penetration testers will attach a debugger to your app, hook it with Frida, install a keyboard that logs every key, and write up each one as a finding.

Each defense here is one build hint or a few lines of Java, and they run on the same codebase as the rest of your app.

## The attacks, in one picture

{{< mermaid >}}
flowchart LR
    A[Attacker] --> D[Attach a debugger<br/>step through your checks]
    A --> H[Hook with Frida or Xposed<br/>rewrite return values]
    A --> R[Root the device<br/>read your storage]
    A --> K[Third-party keyboard<br/>log every key]
    D --> G1[android.debugGuard]
    H --> G2[android.fridaDetection]
    R --> G3[android.rootCheck<br/>ios.detectJailbreak]
    K --> G4[SENSITIVE fields<br/>keyboard detection<br/>SecureKeyboard]
    G1 --> S[Enforce on the server<br/>with App Shield attestation]
    G2 --> S
    G3 --> S
{{< /mermaid >}}

## Refuse to run under a debugger

A debugger lets an attacker pause your app right after the PIN check and flip the result. `android.debugGuard=true` checks at launch for an attached debugger, Java or native, and exits.

```properties
codename1.arg.android.debugGuard=true
codename1.arg.android.debugGuard.checks=debugger,developerOptions,usbDebugging
codename1.arg.android.debugGuard.mode=exit
```

The default is the debugger alone. Developer Options and USB debugging are opt-in, because plenty of ordinary users switch them on, and so do Google Play's pre-launch devices. Banks that want them off can say so. `mode=warn` logs instead of exiting, so you can measure before you enforce. An unknown check name fails the build rather than silently doing nothing.

Two details matter in practice. A debuggable build always only logs, so you can still debug your own app from the IDE. And a tracer running as the app's own user ID isn't counted, because commercial protection products attach one to keep debuggers out.

## Catch root and hooking tools

Root detection now also looks for the manager apps of Magisk, KernelSU and APatch, the rootless tools that older checks miss. `android.fridaDetection=true` catches Frida and the Xposed family: classic Xposed, EdXposed, LSPatch, and Riru. On iOS, `ios.detectJailbreak=true` exits on a jailbroken device.

When you'd rather react in the app than exit, ask:

```java {class="cn1-no-try"}
if (DeviceIntegrity.isDeviceCompromised()) {
    for (String reason : DeviceIntegrity.getCompromiseReasons()) {
        Log.p("Device compromise signal: " + reason);
    }
}
```

On Android the reasons are `root`, `frida`, `emulator`, `debugger`, `developerOptions` and `usbDebugging`. That lets you block one risky operation, say a transfer to a new payee, require a second factor, or raise a silent alarm, instead of locking the user out of their balance.

Anything that runs on the attacker's device can in principle be hidden from, which is why the last box in the diagram matters. [App Shield](/blog/app-shield-server-attestation/) attestation lets your server refuse requests that don't come from a genuine copy of your app on a device with strong integrity. The checks on the device raise the cost of an attack. The check on the server decides it.

You can test all of this without a rooted phone. *Simulate, App Shield* in the simulator turns on any signal, so you can see how your app behaves when it fires.

## Keyloggers live in the keyboard

On Android the keyboard is just an app. Install a third-party one and it sees every key the user types, including the PIN and the card number. Even legitimate keyboards learn what you type and suggest it later, which is how card numbers end up in suggestion bars.

There are three layers of defense, and you pick how far to go.

**Keep secrets out of learned words.** `TextArea.PASSWORD` masks a field. The new `TextArea.SENSITIVE` is for things you want visible but private: card, account, one-time code and ID numbers. Both tell the Android keyboard not to learn from the field, and Gboard, Samsung Keyboard and SwiftKey honor that.

**Know which keyboard is listening.** `DeviceIntegrity.isThirdPartyKeyboardActive` tells you when the active keyboard isn't on your allow list, and `getActiveKeyboard()` names it. On iOS, `DeviceIntegrity.setThirdPartyKeyboardsAllowed(false)` from `init()` makes every field use Apple's keyboard.

**Bring your own keyboard.** `SecureKeyboard` draws a keyboard inside your app, so the platform keyboard never sees a key. Scramble it and a screen recorder learns nothing from the tap positions either.

```java {class="cn1-no-try"}
TextField pin = new TextField("", "PIN", 6, TextArea.NUMERIC | TextArea.PASSWORD);
if (DeviceIntegrity.isThirdPartyKeyboardActive(DeviceIntegrity.wellKnownKeyboards())) {
    SecureKeyboard keyboard = SecureKeyboard.install(pin);
    keyboard.setScrambled(true);
}
```

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,200px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/secure-keyboard-number.png" alt="The secure number keyboard" loading="lazy" style="width:100%;height:auto" /><figcaption>A PIN pad the OS never sees.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/secure-keyboard-scrambled.png" alt="The secure keyboard with scrambled keys" loading="lazy" style="width:100%;height:auto" /><figcaption>Scrambled keys.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/secure-keyboard-text.png" alt="The full secure text keyboard" loading="lazy" style="width:100%;height:auto" /><figcaption>A full text keyboard.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/secure-keyboard-text-dark.png" alt="The secure keyboard in dark mode" loading="lazy" style="width:100%;height:auto" /><figcaption>Dark mode.</figcaption></figure>
</div>
{{< /guide-block >}}

`SecureKeyboard` works on every platform, iOS included. Pair it with `setSecureScreen(true)` and `android.tapjackingGuard=true` on Android, and `ios.disableScreenshots=true` on iOS, and the screen, the overlay and the keyboard are all closed.

## The full set

Put this week next to what's already there and the security chapter reads like an auditor's checklist:

| Threat | Defense |
| --- | --- |
| Debugger attached | `android.debugGuard` |
| Rooted or jailbroken device | `android.rootCheck`, `ios.detectJailbreak` |
| Frida, Xposed, LSPatch | `android.fridaDetection` |
| Third-party keyboard logging keys | `TextArea.SENSITIVE`, keyboard detection, `SecureKeyboard` |
| Screenshots and screen recording | `setSecureScreen`, `ios.disableScreenshots` |
| Overlays and tapjacking | `android.tapjackingGuard` |
| Modified or repackaged app | App Shield attestation |
| Reverse engineering | App Hardening obfuscation |

Each one would be a separate integration and a separate review in two native codebases. Here it's one line per threat. The [security chapter](/developer-guide/#_security) has every hint and its options. Tomorrow: {{< post-link path="/blog/oracle-sql-server-and-your-own-map-server" text="Oracle, SQL Server and your own map server" >}}.
