---
title: AI, OAuth, And Other Platform APIs In The Core
slug: platform-apis-in-the-core
url: /blog/platform-apis-in-the-core/
date: '2026-06-01'
author: Shai Almog
description: A com.codename1.ai package with LlmClient, ChatView, streaming, tool calls, and a simulator Ollama redirect. A modern OAuth / OIDC stack that runs through the system browser and includes Sign in with Apple, Google, Microsoft, Auth0, Firebase, and WebAuthn passkeys. Built-in WiFi / Bonjour / USB and share-sheet result callbacks alongside. Deep tutorials and a note on why the ML Kit AI features stayed in cn1libs.
feed_html: '<img src="https://www.codenameone.com/blog/platform-apis-in-the-core.jpg" alt="AI, OAuth, And Other Platform APIs In The Core" /> A com.codename1.ai package with LlmClient and ChatView, a modern OAuth / OIDC stack with WebAuthn passkeys, plus built-in WiFi / Bonjour / USB and share-sheet result callbacks. Deep tutorials and a note on why ML Kit stayed in cn1libs.'
---

![AI, OAuth, And Other Platform APIs In The Core](/blog/platform-apis-in-the-core.jpg)

This is the second follow-up to [Friday's release post](/blog/metal-default-new-build-cloud-and-a-new-format/). It covers the platform APIs that moved into the framework core this release. There are two headline pieces (AI / LLM and the modern OAuth / OIDC stack), and two smaller pieces (WiFi / connectivity and share-sheet result callbacks). This continues the direction the previous release set when we moved NFC, biometrics, and cryptography into the framework core. The full background on that earlier set is in [NFC, Crypto, Biometrics, And A New Build Cloud](/blog/nfc-crypto-biometrics-and-build-cloud/).

The order below mirrors how much code each section is likely to change in your app. AI first, OAuth / OIDC second, the smaller items at the end.

## AI: a first-class LLM client and a ChatView component

[PR #5035](https://github.com/codenameone/CodenameOne/pull/5035) lands the `com.codename1.ai` package, the `ChatView` UI component, the speech and TTS additions, and the build-time dependency injection that wires the native pieces in. [PR #5057](https://github.com/codenameone/CodenameOne/pull/5057) lands the developer-guide chapter and the agent-skill addition so any project generated from the [Initializr](/initializr/) inherits the new APIs through its bundled `AGENTS.md`.

### LlmClient: the basic chat request

`com.codename1.ai.LlmClient` is the entry point. The simplest possible use:

```java
LlmClient client = LlmClient.openai(apiKey);

ChatRequest req = new ChatRequest.Builder()
        .model("gpt-4o-mini")
        .system("You are a helpful assistant.")
        .user("What is the capital of France?")
        .temperature(0.7)
        .build();

client.chat(req).onResult((resp, err) -> {
    if (err != null) {
        Log.e(err);
        return;
    }
    Log.p(resp.firstChoice().content());
});
```

`LlmClient.openai(...)`, `LlmClient.anthropic(...)`, `LlmClient.gemini(...)`, `LlmClient.ollama(...)`, and `LlmClient.openAiCompatible(baseUrl, apiKey)` are the factories. OpenAI has the fully implemented native client today. The same client drives Ollama, vLLM, and llama.cpp because their wire formats are OpenAI-compatible. Anthropic and Gemini compile and register, but their native clients are still in flight; they throw a clear error pointing you at the OpenAI-compat shim until the native ones land.

### Streaming chat (what you actually want for chat UIs)

For any UI that types responses out token-by-token, the streaming entry point is the one to reach for. The callback fires on the EDT, so you can append directly to a text component:

```java
client.chatStream(req, new ChatStreamListener() {

    @Override
    public void onDelta(ChatDelta d) {
        responseLabel.setText(responseLabel.getText() + d.contentDelta());
        responseLabel.getParent().revalidateLater();
    }

    @Override
    public void onComplete(ChatResponse fin) {
        sendButton.setEnabled(true);
    }

    @Override
    public void onError(Throwable t) {
        Log.e(t);
        sendButton.setEnabled(true);
    }
});
```

Under the hood this is a custom `ConnectionRequest` subclass that parses SSE line-by-line and dispatches each delta through `Display.callSerially`. `AsyncResource.cancel()` kills the socket. So a chat UI that has a cancel button is a one-line cancellation.

### Tool calls

If you want the model to call back into your app, `Tool` / `ToolChoice` give you OpenAI-style function calling. Define the tool, hand the model your model and the available tools, and the response surfaces structured `ToolCall` objects you dispatch:

```java
Tool getWeather = Tool.builder()
        .name("get_weather")
        .description("Look up the current weather for a city.")
        .parameter("city", "string", "The city name, e.g. \"Paris\".")
        .build();

ChatRequest req = new ChatRequest.Builder()
        .model("gpt-4o-mini")
        .user("Is it raining in Tel Aviv right now?")
        .tool(getWeather)
        .toolChoice(ToolChoice.AUTO)
        .build();

client.chat(req).onResult((resp, err) -> {
    if (err != null) return;
    for (ToolCall call : resp.firstChoice().toolCalls()) {
        if ("get_weather".equals(call.name())) {
            String city = call.argument("city").asString();
            String json = lookupWeather(city);
            // Loop the result back into the conversation
            client.chat(req.replyWithToolResult(call, json))
                  .onResult((followUp, e) -> updateUi(followUp));
        }
    }
});
```

The shape mirrors the OpenAI function-calling contract one for one, so anything you have written against the OpenAI API directly maps across without rethinking.

### Embeddings

`LlmClient.embed(...)` returns a vector for any input string. Useful for similarity search against a local SQLite store ([Wednesday's post](/blog/build-time-codegen/) will cover the new ORM that pairs with this):

```java
EmbeddingRequest er = new EmbeddingRequest.Builder()
        .model("text-embedding-3-small")
        .input("Codename One is a cross-platform mobile framework.")
        .build();

client.embed(er).onResult((emb, err) -> {
    float[] vector = emb.firstVector();
    // store, search, compare
});
```

### Image generation

DALL-E and a Replicate scaffold are surfaced through `ImageGenerator`:

```java
ImageGenerator gen = ImageGenerator.openAiDallE(apiKey);

gen.generate("A red bicycle leaning against an olive tree", "1024x1024")
   .onResult((img, err) -> {
       if (err != null) return;
       myImageComponent.setIcon(img);
   });
```

### Working against Ollama in the simulator (no API charges)

`JavaSEPort` pings `localhost:11434` at startup. If it finds Ollama, it sets the `cn1.ai.ollamaDetected` property. With `cn1.ai.simulatorRedirect=auto` (or `=ollama`) every `LlmClient.openai(...)` call routes through the local Ollama endpoint instead of OpenAI's. Production code does not change. The iteration loop, your tests, and your offline debugging stop costing money and stop needing an internet connection.

In `common/codenameone_settings.properties`:

```
simulator.cn1.ai.simulatorRedirect=auto
```

(The `simulator.` prefix scopes the property to the JavaSE simulator path.) Then run Ollama locally with whichever model your code expects (`ollama run llama3.2` or similar) and your existing `LlmClient.openai(...)` calls go to localhost.

### SecureStorage non-prompting overloads

Small thing that matters more than it sounds: the existing biometric-gated `SecureStorage.get / set / remove(account, options...)` methods got new single-argument overloads that do not prompt for biometrics. The reason is LLM API keys. You read them on every network call; you cannot prompt the user for Face ID every time. The new overloads store the key behind the keychain / keystore protection class without the user-presence gate. Existing biometric-gated calls keep working.

```java
SecureStorage.set("openai_api_key", apiKey);   // no prompt
String key = SecureStorage.get("openai_api_key");
```

### ChatView: a ready-made streaming chat UI

`com.codename1.components.ChatView` is the matching UI component. Scrollable message list, `ChatBubble` for the per-message bubble (theme-aware UIIDs so it picks up the iOS Modern / Material 3 native themes consistently), `ChatInput` for the bottom input bar, and a one-line `bindToLlm(...)` that wires the input to a streaming chat request:

```java
ChatView view = new ChatView();
view.bindToLlm(LlmClient.openai(SecureStorage.get("openai_api_key")),
               new ChatRequest.Builder()
                       .model("gpt-4o-mini")
                       .system("You are a friendly tutor for Codename One developers.")
                       .build());

Form f = new Form("Chat", new BorderLayout());
f.add(BorderLayout.CENTER, view);
f.show();
```

The on-screen shape that comes out of that is the standard messaging layout:

```
+---------------------------------------+
|   Chat                                |
+---------------------------------------+
|                                       |
|  +-------------------------+          |
|  | Hi! How can I help?     |          |  <- assistant bubble
|  +-------------------------+          |
|                                       |
|              +----------------------+ |
|              | What is a Form?      | |  <- user bubble
|              +----------------------+ |
|                                       |
|  +------------------------------+     |
|  | A Form is the top-level UI…  |     |  <- streaming
|  +------------------------------+     |
|                                       |
+---------------------------------------+
| Type your message…           [ Send ] |  <- ChatInput
+---------------------------------------+
```

`appendToLastMessage(...)` is the entry point if you want to drive the streaming yourself (the binding above already does it for you). It marshals through `callSerially` so deltas land on the EDT in order.

### Speech and TTS

Two new core APIs land alongside the LLM surface:

```java
TextToSpeech.getInstance().speak("Hello, world.");

SpeechRecognizer rec = SpeechRecognizer.getInstance();
if (!rec.isSupported()) {
    fallbackToTyping();
    return;
}
rec.recognize().onResult((text, err) -> {
    if (err == null) sendChatMessage(text);
});
```

The platform plan is iOS routed through `SFSpeechRecognizer` and `AVSpeechSynthesizer`, Android through `android.speech.*` and the `TextToSpeech` engine. The native bridges are tracked follow-ups (they need on-device testing before they ship). The simulator already has a best-effort TTS via `say` on macOS, `espeak` on Linux, SAPI on Windows; recognition stays unsupported in the simulator unless you add the `cn1-ai-whisper` cn1lib.

### Why are the ML Kit features still cn1libs?

A fair question given the opening framing of this post. If "fundamental device APIs should be in the core", why does AI ship with a set of cn1libs (`cn1-ai-mlkit-barcode`, `cn1-ai-mlkit-docscan`, `cn1-ai-mlkit-face`, `cn1-ai-whisper`, `cn1-ai-stablediffusion`) rather than rolling all of those into `com.codename1.ai` too?

The split is intentional. The core gets the things every modern app benefits from: a way to talk to an LLM, a chat UI, speech in / speech out, the storage primitive for API keys. Those are the building blocks the same way `Display` and `Form` are; an app that wants AI features at all wants those.

The ML Kit cn1libs are specialised verticals. Barcode scanning, document scanning, and face detection are each useful but only for some apps. They each bring a non-trivial native dependency (Google ML Kit on Android is large; the iOS Vision-framework wrappers add their own weight; the on-device Stable Diffusion model is gigabytes), and the cost of carrying every one of them in core would land on every app whether it used them or not.

Two of the optional libraries also fall into a "big upload" category that the cloud build server cannot handle within its usual timeouts. `cn1-ai-stablediffusion` bundles a multi-gigabyte model; the `AiDependencyTable` in the Maven plugin flags those with a `cn1.ai.requiresBigUpload` marker and the cloud build aborts pre-upload with a friendly "build this one locally" message. That kind of opt-in does not belong in a framework dependency that every app inherits.

So the rule we ended up with is: anything that is "AI plumbing" goes in core (the client, the streaming, the chat UI, speech, key storage). Anything that is a "model bundled with native glue for one specific use case" is a cn1lib. The bootstrapping script `scripts/create-ai-cn1lib.sh` generates a new AI cn1lib repo from the archetype with a publish workflow, so if you have a model that fits an opinion the framework does not ship yet, the path to a published cn1lib is one command.

The corresponding chapter, including the full `LlmClient` API table, the `ChatView` reference, the speech bridges, the `SecureStorage` overloads, the simulator Ollama redirect, and the cn1lib coverage, is at [AI, Chat UI, and Speech](https://www.codenameone.com/developer-guide/#_ai_chat_ui_and_speech) in the developer guide.

## OAuth and OIDC: the modern identity stack

The in-app-WebView `Oauth2` flow that Codename One has shipped since approximately forever was the way every cross-platform mobile framework solved "sign in with Google / Facebook / Microsoft" in the 2010s. It is also the way every one of those identity providers stopped wanting you to solve it. Google has been blocking embedded user agents for years. Apple does not want third-party apps wrapping the Apple ID flow in a `WKWebView`. Microsoft and Facebook joined the chorus. The right answer is the system browser: `ASWebAuthenticationSession` on iOS, Custom Tabs on Android, with PKCE on the wire. That is what [PR #5018](https://github.com/codenameone/CodenameOne/pull/5018) lands. [PR #5039](https://github.com/codenameone/CodenameOne/pull/5039) adds a portable WebAuthn / passkey client on top.

### Sign in with Google (or any OIDC provider)

`com.codename1.io.oidc.OidcClient` is the entry point. Point it at the discovery URL of an OIDC provider, hand it the client id and the redirect URI you registered with the provider, ask for tokens:

```java
OidcConfiguration cfg = OidcConfiguration.discover("https://accounts.google.com");

OidcClient client = OidcClient.builder()
        .configuration(cfg)
        .clientId("123-abc.apps.googleusercontent.com")
        .redirectUri("com.example.myapp:/oauthredirect")
        .scopes("openid", "email", "profile")
        .build();

client.signIn().onResult((tokens, err) -> {
    if (err != null) {
        OidcException oe = (OidcException) err;
        if (oe.getCode() == OidcException.USER_CANCELLED) return;
        Log.e(oe);
        return;
    }
    String idToken = tokens.getIdToken().raw();
    String email   = tokens.getIdToken().getClaim("email").asString();
    proceed(email, idToken);
});
```

Discovery JSON parsed and cached. PKCE S256 challenge generated and verified. State and nonce checked on the callback. ID-token claims decoded for you (we deliberately do not verify the signature client-side; the dev guide is explicit about why and points at the "re-validate on your backend" remedy). Refresh and revoke are first-class. The token store is pluggable via `TokenStore`; the default is `Storage`-backed, but a Keychain-backed or in-memory variant is a small class.

On iOS the system-browser piece routes through `ASWebAuthenticationSession`. On Android through `androidx.browser.customtabs` with a plain `ACTION_VIEW` fallback for the rare device with no Custom Tabs provider. `AuthenticationServices.framework` and `androidx.browser:browser` are auto-linked when the classpath scanner sees `OidcClient` in use.

### Provider wrappers: Google, Apple, Microsoft, Facebook, Auth0, Firebase

If you would rather not configure OIDC by hand, the existing social classes get a `signIn(...)` method that drives the same stack with the provider's issuer URL pre-wired:

```java
GoogleConnect.signIn(googleClientId,
                     "com.example.myapp:/oauthredirect",
                     "openid", "email", "profile")
    .onResult((tokens, err) -> { /* ... */ });

MicrosoftConnect.signIn(entraClientId,
                        "msauth.com.example.myapp://auth",
                        "User.Read")
    .onResult((tokens, err) -> { /* ... */ });

Auth0Connect.signIn("tenant.auth0.com", clientId, redirectUri,
                    "openid profile email")
    .onResult((tokens, err) -> { /* ... */ });
```

`FacebookConnect.signIn(...)` follows the same shape against the Facebook OIDC endpoint. `FirebaseAuth` covers the REST-based Firebase auth surface (email / password, IdP token exchange, refresh) which sits underneath any provider hand-off you might want to drive from app code.

### Sign in with Apple

Sign in with Apple is required on iOS for apps that offer any other social login, and on Android it must fall through to a web flow. `com.codename1.social.AppleSignIn` handles both transparently:

```java
AppleSignIn.signIn()
    .onResult((result, err) -> {
        if (err != null) return;
        String idToken = result.getIdToken();
        String code    = result.getAuthorizationCode();
        proceedToBackend(idToken, code);
    });
```

On iOS 13 and later this drops directly into the native Apple sheet via `ASAuthorizationAppleIDProvider`. On non-iOS platforms it falls through to the same OIDC web flow as everything else, so a single line of app code does the right thing on every port. The Maven plugin injects the `com.apple.developer.applesignin` entitlement on iOS when it sees `AppleSignIn` in use; Android does not see it because it is not there.

### Migration from the legacy Oauth2

`com.codename1.io.Oauth2` is now deprecated. Existing code still compiles, but the migration is short and almost always shorter than what it replaces:

```java
// Before
Oauth2 oauth = new Oauth2("https://accounts.google.com/o/oauth2/auth", clientId, redirectUri);
oauth.setClientSecret(clientSecret);
oauth.setScope("openid email profile");
oauth.setBrowserComponent(myBrowserComponent);   // tied to a WKWebView
String token = oauth.authenticate();             // blocks, opens the web view
```

```java
// After
OidcClient.builder()
        .configuration(OidcConfiguration.discover("https://accounts.google.com"))
        .clientId(clientId)
        .redirectUri(redirectUri)
        .scopes("openid", "email", "profile")
        .build()
        .signIn()
        .onResult((tokens, err) -> proceed(tokens.getIdToken().raw()));
```

You stop owning the browser. The OS owns it. The cookies live in the platform's authentication session. The user gets the same login experience they have everywhere else on their device.

### WebAuthn / passkeys

[PR #5039](https://github.com/codenameone/CodenameOne/pull/5039) layers a portable WebAuthn client on top:

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

One thing worth pulling out before you reach for it: if you sign in via OIDC against Google, Apple, Microsoft, Auth0, or Firebase, you usually already *get* passkeys for free. The identity provider runs the WebAuthn ceremony inside the system browser; OIDC just hands you the resulting tokens. So you do not need `WebAuthnClient` for that case. You need it for apps that run their own relying-party backend, and for apps driving the Auth0 or Firebase passkey grants directly.

Full chapter: [Authentication and Identity](https://www.codenameone.com/developer-guide/#_authentication_and_identity).

## Connectivity: WiFi, Bonjour, USB, network-type listeners

[PR #5021](https://github.com/codenameone/CodenameOne/pull/5021) lands four packages for apps that need to do more with the network than open an HTTP socket. The shape:

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

`com.codename1.io.wifi` for WiFi info, scan, and connect. `com.codename1.io.wifi.WiFiDirect` for peer-to-peer (Android only by platform reality). `com.codename1.io.bonjour` for mDNS / Zeroconf via `BonjourBrowser` and `BonjourPublisher`. `com.codename1.io.usb` for USB host (Android only). And `NetworkManager.addNetworkTypeListener(...)` plus `NETWORK_TYPE_*` constants so an app can react to a transition between cellular, WiFi, ethernet, or "none":

```java
NetworkManager.getInstance().addNetworkTypeListener(evt -> {
    int type = evt.getNetworkType();
    if (type == NetworkManager.NETWORK_TYPE_NONE)     showOfflineBanner();
    else if (type == NetworkManager.NETWORK_TYPE_CELLULAR) suppressLargeBackgroundDownloads();
    else                                              clearOfflineBanner();
});
```

iOS does not expose programmatic WiFi scanning to third-party apps; `scan()` throws `UnsupportedOperationException` on iOS. iOS also does not expose WiFi Direct or general USB host. None of those are Codename One limitations; they are Apple's. The dev guide is explicit about each platform's limits.

Three new compile-time defines (`CN1_INCLUDE_WIFI_INFO`, `CN1_INCLUDE_HOTSPOT`, `CN1_INCLUDE_BONJOUR`) wrap the iOS native code, set only when the classpath scanner sees the matching Java API in use. Apps that do not use these APIs do not pay for them at App Store review time. Same pattern as the NFC gating from the previous release.

Full reference: [Network Connectivity](https://www.codenameone.com/developer-guide/#_network_connectivity).

## Share-sheet result callbacks

[PR #5036](https://github.com/codenameone/CodenameOne/pull/5036) closes a small but persistent gap: `Display.share(...)` and `ShareButton` finally tell you what the user did with the share sheet:

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

iOS routes through `UIActivityViewController.completionWithItemsHandler`; Android through `Intent.createChooser` with an `IntentSender` callback (API 22+). The framework normalises the platform values into `SHARED_TO(packageName)`, `DISMISSED`, or `FAILED`.

The same PR also lands an `IOSShareExtensionBuilder` in the Maven plugin that emits a complete `.ios.appext` bundle (Info.plist, App Group entitlements, a minimal `ShareViewController.swift`, the matching `buildSettings.properties`) so apps that want to *receive* shared content from other apps no longer need to bootstrap an extension target in Xcode by hand.

## What ties this together

Every API in this batch flips a single per-feature flag (`usesOidc`, `usesAppleSignIn`, `usesWebAuthn`, `usesAi`, plus the existing WiFi / Bonjour / Hotspot defines) that drives framework linking, entitlement injection, plist injection, and Objective-C conditional compilation. Apps that do not reference the new APIs do not pay for them at App Store review time, and the binaries do not even contain the platform calls. That is the same pattern we shipped for NFC and biometrics in the previous release, and it is what makes "these APIs are part of the core" sustainable; the cost only lands on the apps that actually use them.

The companion cloud build server changes (BuildDaemon mirrors) ship together so local builds and cloud builds match.

## Wrapping up

The next post is on Wednesday and covers the architectural change in this release: a build-time bytecode annotation framework, the declarative router that is its first consumer, the SQLite ORM and JSON / XML mappers and component binder built on the same SPI, and the build-time SVG / Lottie transcoder that ships in the same release for related reasons.

Back to the [weekly index](/blog/metal-default-new-build-cloud-and-a-new-format/).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
