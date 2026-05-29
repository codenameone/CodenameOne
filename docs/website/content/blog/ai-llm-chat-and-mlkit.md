---
title: An AI Package, A ChatView, Speech, And ML Kit cn1libs
slug: ai-llm-chat-and-mlkit
url: /blog/ai-llm-chat-and-mlkit/
date: '2026-06-05'
author: Shai Almog
description: A com.codename1.ai package with LlmClient for OpenAI, Anthropic, Gemini, and Ollama; a streaming ChatView component; SpeechRecognizer and TextToSpeech as core APIs; a simulator Ollama redirect for offline debugging; and the ML Kit cn1libs for barcode, document scan, and face. Plus the build-time dependency injection that wires the native pieces in automatically.
feed_html: '<img src="https://www.codenameone.com/blog/ai-llm-chat-and-mlkit.jpg" alt="An AI Package, A ChatView, Speech, And ML Kit cn1libs" /> A com.codename1.ai package with LlmClient for OpenAI/Anthropic/Gemini/Ollama, streaming ChatView, SpeechRecognizer + TextToSpeech in core, simulator Ollama redirect, and the ML Kit cn1libs for barcode/docscan/face.'
---

![An AI Package, A ChatView, Speech, And ML Kit cn1libs](/blog/ai-llm-chat-and-mlkit.jpg)

This one covers the most surface area of any post in this series, so I am going to keep the prose to a minimum and let the API examples carry it. The two PRs are [#5035](https://github.com/codenameone/CodenameOne/pull/5035) (the package, the ChatView, speech, the simulator Ollama redirect, the build-time dependency injection) and [#5057](https://github.com/codenameone/CodenameOne/pull/5057) (the developer-guide chapter that documents all of it, the agent skill addition so generated projects' AGENTS.md picks up the new APIs, and the LanguageTool word list updates for `Ollama` / `vLLM` / `Anthropic` / `SAPI` / `espeak`).

The full chapter is at [Ai-And-Speech.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Ai-And-Speech.asciidoc).

## LlmClient

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

`LlmClient.openai(...)`, `LlmClient.anthropic(...)`, `LlmClient.gemini(...)`, `LlmClient.ollama(...)`, and `LlmClient.openAiCompatible(baseUrl, apiKey)` are the static factories. OpenAI is the fully implemented native client; the same client drives Ollama, vLLM, and llama.cpp because the wire format is OpenAI-compatible. Anthropic and Gemini compile and register but currently throw a clear error pointing callers at the OpenAI-compat shim while their native clients are in flight. The follow-up native clients are tracked; I will trail them in a future weekly post when they land.

Streaming chat is a separate entry point and is the one most chat UIs actually want:

```java
client.chatStream(req, new ChatStreamListener() {
    @Override public void onDelta(ChatDelta d) {
        appendToUi(d.contentDelta());
    }
    @Override public void onComplete(ChatResponse final_) { /* ... */ }
    @Override public void onError(Throwable t)            { /* ... */ }
});
```

Under the hood this is a custom `ConnectionRequest` subclass that parses SSE line-by-line and dispatches deltas through `Display.callSerially`, so your UI updates land on the EDT. `AsyncResource.cancel()` kills the socket cleanly.

The rest of the surface is what you would expect from a modern LLM SDK and want from one: `ChatRequest` / `ChatMessage` / `MessagePart` for multimodal content, `Tool` / `ToolCall` / `ToolChoice` for function calling, `ResponseFormat` for JSON-mode and JSON-schema-mode, `Embedding` for the embeddings endpoint, `ImageGenerator` for DALL-E (and a Replicate scaffold), and a `LlmException` taxonomy that surfaces the structured error fields OpenAI returns (`context_length_exceeded`, `rate_limit_exceeded`, `invalid_request_error`).

Utility extras: `PromptTemplate` (parameter substitution with escape rules), `Tokenizer` (BPE for the OpenAI families plus a heuristic fallback), `RetryPolicy` (exponential backoff with a jitter), `ConversationStore` (persistence of a multi-turn thread), and `SafetyFilter` (a lightweight content-filter scaffold).

## Simulator Ollama redirect

The simulator detail that matters most in practice is this: `JavaSEPort` runs a `probeOllamaAsync()` at startup that pings `localhost:11434`. If it finds Ollama running, it sets the `cn1.ai.ollamaDetected` property. A small `SimulatorRedirect` consumer reads that, and with `cn1.ai.simulatorRedirect=auto` (or `=ollama`), every `LlmClient.openai(...)` call routes through the local Ollama endpoint instead of OpenAI's. Production code does not change; your tests, your iteration loop, and your offline debugging stop costing money and stop needing an internet connection.

It also means a CI job can run a Codename One app's LLM-driven code paths against a deterministic local model without provisioning OpenAI keys. Whether you do that or not is a matter of taste; the option is there.

## ChatView

`com.codename1.components.ChatView` is the matching UI component. Scrollable message list, `ChatBubble` for the per-message bubble (theme-aware UIIDs so it picks up the iOS Modern / Material 3 native themes consistently), `ChatInput` for the bottom input bar, and the convenience method that wires the whole thing together:

```java
ChatView view = new ChatView();
view.bindToLlm(LlmClient.openai(apiKey),
               new ChatRequest.Builder()
                       .model("gpt-4o-mini")
                       .system("You are a friendly tutor.")
                       .build());

Form f = new Form("Chat", new BorderLayout());
f.add(BorderLayout.CENTER, view);
f.show();
```

That gives you a working streaming chat UI in five lines. `appendToLastMessage` is the streaming entry point if you want to drive the UI yourself; it marshals through `callSerially` so deltas land on the EDT in order.

## Speech: SpeechRecognizer + TextToSpeech

Two new core APIs:

```java
TextToSpeech tts = TextToSpeech.getInstance();
tts.speak("Hello, world.");
```

```java
SpeechRecognizer rec = SpeechRecognizer.getInstance();
if (!rec.isSupported()) {
    fallbackToTyping();
    return;
}
rec.recognize().onResult((text, err) -> {
    if (err == null) sendMessage(text);
});
```

The platform plan: iOS routes through `SFSpeechRecognizer` and `AVSpeechSynthesizer`; Android through `android.speech.*` and the `TextToSpeech` engine. The native bridges for both ports are tracked follow-ups in the PR description (they need real-device testing before they ship). The simulator already has a best-effort TTS via `say` on macOS, `espeak` on Linux, SAPI on Windows; speech recognition stays unsupported in the simulator unless you add the `cn1-ai-whisper` cn1lib.

## SecureStorage non-prompting overloads

Small thing that matters more than it sounds: the existing biometric-gated `SecureStorage.get / set / remove(account, options...)` methods got new single-argument overloads that do not prompt for biometrics. The reason is LLM API keys. You read them on every network call; you cannot prompt the user for Face ID every time. The new overloads store the key behind the keychain / keystore protection class without the user-presence gate. Existing biometric-gated calls keep working.

## Build-time dependency injection

`AiDependencyTable` in the Maven plugin is the piece that makes "add an LLM call to your app and the right native dependencies show up" actually true. The table has 18 entries mapping `com/codename1/ai/*` and the speech / TTS classes to iOS pods, Swift Packages, Android Gradle deps, `Info.plist` usage strings, and Android permissions. The existing `IPhoneBuilder` / `AndroidGradleBuilder` ASM scanners get new branches that consume the table; iOS routing goes through `IOSDependencyManager` so SPM-mode projects get SPM and Pods-mode projects get Pods automatically.

One detail worth knowing: entries that bundle native blobs over 2 GB (on-device Stable Diffusion is the obvious one) flip a `cn1.ai.requiresBigUpload` flag, and the cloud build server aborts pre-upload with a friendly "build this one locally" message rather than waste your time on a several-gig upload that would not complete inside the cloud builder's timeout.

## ML Kit cn1libs

The companion cn1libs in this release: `cn1-ai-mlkit-barcode`, `cn1-ai-mlkit-docscan`, `cn1-ai-mlkit-face`. The bootstrapping script `scripts/create-ai-cn1lib.sh` generates a new AI cn1lib repo from the archetype and drops in a publish workflow so it lands on Maven Central on every merge to master. The three above are the ones that ship in this batch; more are tracked as follow-ups. There is also a `cn1-ai-whisper` for on-device speech recognition and a `cn1-ai-stablediffusion` for on-device image generation in the pipeline; both fall into the "big upload" category above and are local-build only.

## Agent skill update

The other half of [PR #5057](https://github.com/codenameone/CodenameOne/pull/5057) updates the bundled Codename One authoring skill that ships in every Initializr-generated project. The `references/ai-and-speech.md` reference document lands so that Claude Code (or any AGENTS.md-aware agent) working inside a generated project understands the new APIs from the first prompt; the `SKILL.md` index gets a pointer; the `GeneratorModelMatrixTest` regression covers it.

If you regenerate a project from the Initializr today, an agent working inside it can write LLM-driven code without you having to paste in the API reference manually. The same `.agent-skills/codename-one/` layout from the [Skills, Java 17, and Theme Accents post](/blog/skills-java17-and-theme-accents/#agentsmd-and-the-codename-one-skill).

## Wrapping up

A lot of surface, intentionally documented in one chapter rather than three. The chapter is at [Ai-And-Speech.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Ai-And-Speech.asciidoc); the bundled agent skill picks up the same reference; the simulator Ollama path is the offline iteration loop; and the build-time dependency table means "add an LLM call" stays a one-line edit.

Tomorrow: the POJO ORM, the JSON / XML mappers, and the component binder; the rest of what the bytecode annotation framework from the [router post](/blog/declarative-router-and-deep-links/) enables.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
