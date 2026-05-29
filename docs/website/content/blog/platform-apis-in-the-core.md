---
title: Platform APIs In The Core: Connectivity, Identity, Sharing, And AI
slug: platform-apis-in-the-core
url: /blog/platform-apis-in-the-core/
date: '2026-06-01'
author: Shai Almog
description: WiFi / Bonjour / USB / network-type APIs, a modern OIDC + WebAuthn identity stack, share-sheet result callbacks, and a com.codename1.ai package with LlmClient, ChatView, speech, and ML Kit cn1libs. Four surfaces, one auto-injection story for Android permissions and iOS entitlements.
feed_html: '<img src="https://www.codenameone.com/blog/platform-apis-in-the-core.jpg" alt="Platform APIs In The Core: Connectivity, Identity, Sharing, And AI" /> WiFi / Bonjour / USB, OIDC + WebAuthn, share-sheet result callbacks, and a com.codename1.ai package with LlmClient and ChatView. Four surfaces, one auto-injection story for Android permissions and iOS entitlements.'
---

![Platform APIs In The Core: Connectivity, Identity, Sharing, And AI](/blog/platform-apis-in-the-core.jpg)

This post covers the four surfaces that moved from "you need a cn1lib for this" to "it is in the framework" this release. The same direction the last two releases pulled NFC, biometrics, and cryptography in: fundamental device APIs should not require you to track down a cn1lib and hope it is maintained. They should be part of the framework, with auto-injected permissions, conservative defaults, and a simulator path that lets you test without a real radio.

The four are connectivity (WiFi / Bonjour / USB / network-type listeners), identity (OIDC + WebAuthn passkeys), sharing (share-sheet result callbacks), and AI (`LlmClient`, `ChatView`, speech and TTS, the ML Kit cn1libs). All four share the scanner-driven auto-injection that means an app that does not reference the API does not pay for it at App Store review time.

## Connectivity

[PR #5021](https://github.com/codenameone/CodenameOne/pull/5021) lands four packages. The new chapter at [Network-Connectivity.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Network-Connectivity.asciidoc) covers every surface in detail; the highlights:

```java
WiFi wifi = WiFi.getInstance();
String ssid    = wifi.getCurrentSSID();
String bssid   = wifi.getBSSID();
String gateway = wifi.getGateway();
String ip      = wifi.getIp();

wifi.scan(new ScanOptions().setTimeoutMillis(5000))
   .onResult((results, err) -> { /* ... */ });

wifi.connect("MyNetwork", "hunter2", Security.WPA2_PSK)
   .onResult((success, err) -> { /* ... */ });
```

`com.codename1.io.wifi` for WiFi info, scan, and connect. `com.codename1.io.wifi.WiFiDirect` for peer-to-peer (Android only by platform reality). `com.codename1.io.bonjour` for mDNS / Zeroconf with `BonjourBrowser` and `BonjourPublisher`. `com.codename1.io.usb` for USB host. And `NetworkManager.addNetworkTypeListener(...)` plus `NETWORK_TYPE_*` constants so an app can react to a transition between cellular, WiFi, ethernet, or "none":

```java
NetworkManager.getInstance().addNetworkTypeListener(evt -> {
    int type = evt.getNetworkType();
    if (type == NetworkManager.NETWORK_TYPE_NONE)     showOfflineBanner();
    else if (type == NetworkManager.NETWORK_TYPE_CELLULAR) suppressLargeBackgroundDownloads();
    else                                              clearOfflineBanner();
});
```

The per-platform implementation choices map to the standard system services. Android: `WifiManager` and `ConnectivityManager` (with `WifiNetworkSpecifier` on API 29+ and a `WifiConfiguration` fallback for older releases), `NsdManager` for Bonjour, `WifiP2pManager` for WiFi Direct, `UsbManager` for USB. iOS: `CNCopyCurrentNetworkInfo` for SSID / BSSID, `getifaddrs` for IP / gateway, `NEHotspotConfigurationManager` for connect, `NSNetServiceBrowser` for Bonjour, `SCNetworkReachability` for network-type tracking. JavaSE: derives WiFi info from `NetworkInterface`, returns fabricated scan results, picks up JmDNS if it is on the classpath.

iOS does not expose programmatic WiFi scanning to third-party apps; `scan()` throws `UnsupportedOperationException` on iOS regardless of what entitlements you add. iOS also does not expose WiFi Direct or general USB host to third-party apps. None of those are Codename One limitations; they are Apple's. The dev guide is explicit about each platform's limits so there is no runtime surprise.

Three new compile-time defines (`CN1_INCLUDE_WIFI_INFO`, `CN1_INCLUDE_HOTSPOT`, `CN1_INCLUDE_BONJOUR`) wrap the native code that calls `CNCopyCurrentNetworkInfo`, `NEHotspotConfigurationManager`, and `NSNetServiceBrowser`. The build server sets each define only when your classpath scanner sees the corresponding Java API in use, so an app that never references `WiFi.connect` does not even compile the `NEHotspotConfigurationManager` code, and Apple's API-usage scanner does not flag your binary for entitlements it has not seen the matching APIs in. Same shape as the NFC gating we shipped two weeks ago.

## Identity: OIDC and WebAuthn passkeys

The in-app-WebView `Oauth2` flow that Codename One has shipped since approximately forever was the way every cross-platform mobile framework solved the "sign in with Google / Facebook / Microsoft" problem in the 2010s. It is also the way every one of those identity providers stopped wanting you to solve it. Google has been blocking embedded user agents for years. Apple does not want third-party apps wrapping the Apple ID flow in a `WKWebView`. Microsoft and Facebook joined the chorus. The right answer is the system browser: `ASWebAuthenticationSession` on iOS, Custom Tabs on Android, with PKCE on the wire. That is what [PR #5018](https://github.com/codenameone/CodenameOne/pull/5018) does.

`com.codename1.io.oidc.OidcClient` is the new entry point:

```java
OidcConfiguration cfg = OidcConfiguration.discover("https://accounts.google.com");

OidcClient client = OidcClient.builder()
        .configuration(cfg)
        .clientId("123-abc.apps.googleusercontent.com")
        .redirectUri("com.example.myapp:/oauthredirect")
        .scopes("openid", "email", "profile")
        .build();

client.signIn().onResult((tokens, err) -> {
    if (err != null) return;
    String email = tokens.getIdToken().getClaim("email").asString();
    proceed(email);
});
```

Discovery JSON parsed and cached. PKCE S256 challenge generated and verified. State and nonce checked on the callback. ID-token claims decoded for you (we deliberately do not verify the signature client-side; the dev guide is explicit about why and points at the "re-validate on your backend" remedy). Refresh and revoke are first-class. The token store is pluggable via `TokenStore`. On iOS the system-browser piece routes through `ASWebAuthenticationSession`; on Android through `androidx.browser.customtabs` with a plain `ACTION_VIEW` fallback for the rare device that has no Custom Tabs provider. `AuthenticationServices.framework` and `androidx.browser:browser` are auto-linked when the classpath scanner sees `OidcClient` in use.

`com.codename1.social` gains `MicrosoftConnect` (Entra ID), `Auth0Connect`, and `FirebaseAuth` as new provider wrappers; `GoogleConnect.signIn(...)` and `FacebookConnect.signIn(...)` go through the new stack. `com.codename1.social.AppleSignIn` moves into the core, with native `ASAuthorizationAppleIDProvider` on iOS 13+ and an OIDC web fallback elsewhere.

`com.codename1.io.Oauth2` is now deprecated. It still works (we deliberately did not break it). The migration is a short edit: replace the issuer URL and credentials with their `OidcConfiguration` equivalents, swap the `accessToken()` call for `signIn()`, delete the `setBrowserComponent(...)` wiring. The result is shorter and the cookie state is owned by the OS instead of by a `WKWebView` you have to manage.

[PR #5039](https://github.com/codenameone/CodenameOne/pull/5039) layers a portable WebAuthn / passkey client on top:

```java
WebAuthnClient client = WebAuthnClient.getInstance();
if (!client.isAvailable()) { fallbackToPassword(); return; }

PublicKeyCredentialCreationOptions opts =
        PublicKeyCredentialCreationOptions.fromServerJson(serverJson);
client.create(opts).onResult((cred, err) -> {
    if (err == null) postToRelyingParty(cred.toJson());
});
```

W3C JSON wire format in both directions, so the response can be POSTed verbatim to any standard server-side WebAuthn library. iOS 16+ routes through `ASAuthorizationPlatformPublicKeyCredentialProvider`; Android API 28+ through `androidx.credentials.CredentialManager`. Provider helpers: `Auth0Connect.signInWithPasskey(...)` / `.registerPasskey(...)` and `FirebaseAuth.signInWithPasskey(...)` / `.registerPasskey(...)`.

One thing worth pulling out about WebAuthn before you reach for it: if you sign in via OIDC against Google, Apple, Microsoft, Auth0, or Firebase, you usually already *get* passkeys for free. The identity provider runs the WebAuthn ceremony inside the system browser; OIDC just hands you the resulting tokens. So you do not need `WebAuthnClient` for that case. You need it for two things specifically: apps that run their own relying-party backend, and apps that drive Auth0 or Firebase passkeys directly via their client-side WebAuthn grants. The dev guide section calls this out so nobody adds a WebAuthn dependency they do not need.

Full chapter: [Authentication-And-Identity.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Authentication-And-Identity.asciidoc).

## Sharing: result callbacks (and an iOS Share Extension authoring helper)

The native share sheet has been one of those small gaps that nobody complains about in isolation, but that hits you the day you try to do something useful with the result. The framework has had `Display.share(...)` and `ShareButton` since approximately forever. What it has not had is a way to know whether the user actually shared the content, where they shared it to, or whether they hit Cancel and walked away. [PR #5036](https://github.com/codenameone/CodenameOne/pull/5036) closes that:

```java
ShareButton btn = new ShareButton();
btn.setTextToShare("Look at this fox");
btn.setImageToShare("/fox.jpg");
btn.setShareResultListener(result -> {
    switch (result.getStatus()) {
        case SHARED_TO: track("share_completed", result.getTargetPackage()); break;
        case DISMISSED: track("share_dismissed"); break;
        case FAILED:    track("share_failed", result.getError()); break;
    }
});
```

iOS routes through `UIActivityViewController.completionWithItemsHandler` (Apple's API hands you a `UIActivityType` string plus a completed-boolean; the framework normalises it). Android routes through `Intent.createChooser` with an `IntentSender` callback, which is the API path that landed in API 22; the `IntentSender` is invoked with the `ComponentName` of the chosen target, so the framework sees the package name of the receiving app. Earlier-API devices fall back to `DISMISSED`. The same listener attaches to `Display.share(...)`.

The PR also lands an `IOSShareExtensionBuilder` in the Maven plugin for the *other* half of sharing: iOS apps that want to *receive* shared content from other apps via a Share Extension. The builder emits a complete `.ios.appext` bundle (`Info.plist` with the right `NSExtension` activation rules, the App Group entitlement, a minimal `ShareViewController.swift` that lands the payload in the App Group via `UserDefaults(suiteName:)`, and the matching `buildSettings.properties`). The result composes with the existing `IPhoneBuilder.extractAppExtensions` pipeline, so apps that use the builder do not need to bootstrap anything in Xcode and apps that already have a hand-rolled extension keep working.

## AI

The largest of the four surfaces. [PR #5035](https://github.com/codenameone/CodenameOne/pull/5035) lands the package, the ChatView, speech, the simulator Ollama redirect, and the build-time dependency injection; [PR #5057](https://github.com/codenameone/CodenameOne/pull/5057) lands the developer-guide chapter and the agent-skill update so generated projects' `AGENTS.md` covers the new APIs from the first prompt.

`com.codename1.ai.LlmClient` is the entry point:

```java
LlmClient client = LlmClient.openai(apiKey);

ChatRequest req = new ChatRequest.Builder()
        .model("gpt-4o-mini")
        .system("You are a helpful assistant.")
        .user("What is the capital of France?")
        .temperature(0.7)
        .build();

client.chat(req).onResult((resp, err) -> {
    if (err != null) return;
    System.out.println(resp.firstChoice().content());
});
```

`LlmClient.openai(...)`, `LlmClient.anthropic(...)`, `LlmClient.gemini(...)`, `LlmClient.ollama(...)`, and `LlmClient.openAiCompatible(baseUrl, apiKey)` are the static factories. OpenAI is the fully implemented native client; the same client drives Ollama, vLLM, and llama.cpp because the wire format is OpenAI-compatible. Anthropic and Gemini compile and register but currently throw a clear error pointing callers at the OpenAI-compat shim while their native clients are in flight.

Streaming chat is a separate entry point, and is the one most chat UIs actually want:

```java
client.chatStream(req, new ChatStreamListener() {
    @Override public void onDelta(ChatDelta d)         { appendToUi(d.contentDelta()); }
    @Override public void onComplete(ChatResponse fin) { /* ... */ }
    @Override public void onError(Throwable t)         { /* ... */ }
});
```

Under the hood this is a custom `ConnectionRequest` subclass that parses SSE line-by-line and dispatches deltas through `Display.callSerially`, so your UI updates land on the EDT. `AsyncResource.cancel()` kills the socket cleanly.

The rest of the surface is what a modern LLM SDK looks like: `ChatRequest` / `ChatMessage` / `MessagePart` for multimodal content, `Tool` / `ToolCall` / `ToolChoice` for function calling, `ResponseFormat` for JSON-mode and JSON-schema-mode, `Embedding` for the embeddings endpoint, `ImageGenerator` for DALL-E. Utility extras include `PromptTemplate`, `Tokenizer`, `RetryPolicy`, `ConversationStore`, and `SafetyFilter`.

### Simulator Ollama redirect

The simulator detail that matters most in practice: `JavaSEPort` pings `localhost:11434` at startup, and if it finds Ollama running, sets the `cn1.ai.ollamaDetected` property. A small `SimulatorRedirect` consumer reads that, and with `cn1.ai.simulatorRedirect=auto` (or `=ollama`), every `LlmClient.openai(...)` call routes through the local Ollama endpoint instead of OpenAI's. Production code does not change; your tests, your iteration loop, and your offline debugging stop costing money and stop needing an internet connection.

### ChatView

`com.codename1.components.ChatView` is the matching UI component. Scrollable message list, `ChatBubble` for the per-message bubble (theme-aware UIIDs so it picks up the iOS Modern / Material 3 native themes consistently), `ChatInput` for the bottom input bar, and the convenience method that wires the whole thing together:

```java
ChatView view = new ChatView();
view.bindToLlm(LlmClient.openai(apiKey),
               new ChatRequest.Builder().model("gpt-4o-mini").build());

Form f = new Form("Chat", new BorderLayout());
f.add(BorderLayout.CENTER, view);
f.show();
```

Five lines, working streaming chat UI.

### Speech and SecureStorage

Two new core APIs land alongside the LLM surface. `TextToSpeech.getInstance().speak("Hello")` and `SpeechRecognizer.getInstance().recognize().onResult(...)`. The native bridges (iOS `SFSpeechRecognizer` / `AVSpeechSynthesizer`, Android `android.speech.*` and `TextToSpeech`) are tracked follow-ups; the simulator already has a best-effort TTS via `say` on macOS, `espeak` on Linux, SAPI on Windows.

`SecureStorage` gains single-argument non-prompting overloads of `get / set / remove(account, ...)` next to the existing biometric-gated methods. The reason is LLM API keys: you read them on every network call; you cannot prompt the user for Face ID every time. The new overloads store the key behind the keychain / keystore protection class without the user-presence gate.

### Build-time dependency injection

`AiDependencyTable` in the Maven plugin is the piece that makes "add an LLM call to your app and the right native dependencies show up" actually true. 18 entries mapping `com/codename1/ai/*` and the speech / TTS classes to iOS pods, Swift Packages, Android Gradle deps, `Info.plist` usage strings, and Android permissions. Entries that bundle native blobs over 2 GB (on-device Stable Diffusion) flip a `cn1.ai.requiresBigUpload` flag and the cloud build aborts pre-upload with a friendly "build this one locally" message.

The companion cn1libs in this release: `cn1-ai-mlkit-barcode`, `cn1-ai-mlkit-docscan`, `cn1-ai-mlkit-face`. The bootstrapping script `scripts/create-ai-cn1lib.sh` generates a new AI cn1lib repo from the archetype with a publish workflow.

Full chapter: [Ai-And-Speech.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Ai-And-Speech.asciidoc).

## What ties this together

The shared structural element across all four surfaces is the scanner-driven gating. Every PR in this batch flips a single per-feature flag (`usesOidc`, `usesAppleSignIn`, `usesWebAuthn`, `usesAi`, the existing WiFi/Bonjour/Hotspot defines) that drives framework linking, entitlement injection, plist injection, and Objective-C conditional compilation. Apps that do not reference the new APIs do not pay for them at App Store review time, and the binaries do not even contain the platform calls. This is the pattern we settled on for NFC and biometrics two weeks ago, and it is the right answer for "these APIs are part of the core, but apps that do not use them should not pay for them at review time".

The companion cloud build server changes (BuildDaemon mirrors) ship together so local builds and cloud builds match across all four surfaces.

## Wrapping up

Connectivity, identity, sharing, and AI. All in the core. Same auto-injection story across the four of them. The corresponding chapters cover each surface in detail; the place to read across them is the build-hint and entitlement table in [Advanced-Topics-Under-The-Hood.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Advanced-Topics-Under-The-Hood.asciidoc).

Wednesday's post is the architectural one: the build-time bytecode annotation framework, the declarative router that is its first consumer, the SQLite ORM and JSON / XML mappers and component binder it carries, and the SVG / Lottie transcoder that ships in the same release because it sits on the same "emit Java at build time" pattern.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
