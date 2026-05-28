# AI, Chat UI, and Speech Reference

Codename One ships a portable LLM client, a streaming chat component, speech recognition, text-to-speech, and ML Kit cn1lib bridges. All of it sits in the cross-platform `common/` module — the same call site runs on iOS, Android, JavaSE, and (where the backend supports it) JavaScript.

**Read this reference when** the user asks to integrate an LLM, build a chat UI, voice input, voice output, image generation, embeddings, on-device barcode/face/document scanning, or wants to store an API key.

## Core APIs at a glance

| Concern | Class | Module |
| --- | --- | --- |
| Chat / embeddings / image generation | `com.codename1.ai.LlmClient` | core (built-in) |
| Streaming-aware chat UI | `com.codename1.components.ChatView` | core (built-in) |
| Speech-to-text | `com.codename1.media.SpeechRecognizer` | core (built-in) |
| Text-to-speech | `com.codename1.media.TextToSpeech` | core (built-in) |
| Silent secret storage (LLM API keys, etc.) | Single-arg overloads on `com.codename1.security.SecureStorage` | core (built-in) |
| Barcode scanning | `com.codename1.ai.mlkit.barcode.BarcodeScanner` | cn1lib `cn1-ai-mlkit-barcode` |
| Document scanning | `com.codename1.ai.mlkit.docscan.DocumentScanner` | cn1lib `cn1-ai-mlkit-docscan` |
| Face detection | `com.codename1.ai.mlkit.face.FaceDetector` | cn1lib `cn1-ai-mlkit-face` |

The build-time scanner in the Codename One Maven plugin (`AiDependencyTable`) picks up references to any `com.codename1.ai.*` or `com.codename1.media.{Speech,Tts}*` class and automatically wires Pods (iOS), Swift Packages (iOS SPM), Gradle dependencies (Android), `Info.plist` usage strings, and Android permissions. You don't edit `codenameone_settings.properties` build hints for these classes.

## LlmClient — chat, embeddings, image generation

```java
import com.codename1.ai.*;

// OpenAI. Also drives Ollama, vLLM, llama.cpp, Together, Groq,
// Fireworks etc. via shared wire format.
LlmClient client = LlmClient.openai(apiKey);

// Local Ollama on http://localhost:11434
LlmClient local = LlmClient.ollama("llama3.2");

// Any OpenAI-compatible endpoint
LlmClient together = LlmClient.localOpenAiCompatible(
    "https://api.together.xyz/v1", apiKey, "meta-llama/Llama-3.3-70B-Instruct-Turbo");

// Anthropic + Gemini route through their OpenAI-compatible endpoints
// (Anthropic at /v1/chat/completions, Gemini at /v1beta/openai/
// chat/completions). Same ChatRequest / ChatResponse value types,
// same streaming, same tool-call API.
LlmClient claude = LlmClient.anthropic(apiKey);   // default model: claude-sonnet-4-5
LlmClient gemini = LlmClient.gemini(apiKey);      // default model: gemini-2.0-flash
```

Default models per provider when `ChatRequest.builder().model(...)` is not called:

| Provider | Default model | Override via |
| --- | --- | --- |
| OpenAI | `gpt-4o-mini` | `ChatRequest.builder().model("gpt-4o")` etc. |
| Anthropic | `claude-sonnet-4-5` | `ChatRequest.builder().model("claude-opus-4-1")` etc. |
| Gemini | `gemini-2.0-flash` | `ChatRequest.builder().model("gemini-2.5-pro")` etc. |
| Ollama | `llama3.2` | `LlmClient.ollama("qwen2.5-7b")` or per request |

`ChatRequest` is a fluent builder. `chat()` returns the response, `chatStream()` streams deltas:

```java
ChatRequest req = ChatRequest.builder()
    .model("gpt-4o-mini")
    .addMessage(ChatMessage.system("Reply in haiku."))
    .addMessage(ChatMessage.user("Describe a Codename One app."))
    .temperature(0.7f)
    .maxTokens(200)
    .build();

client.chat(req).ready(resp -> Log.p(resp.getText()));

// Streaming
client.chatStream(req, new StreamingListener.Adapter() {
    @Override public void onContentDelta(String delta) {
        chatView.appendToLastMessage(delta);
    }
}).ready(resp -> Log.p("usage=" + resp.getUsage().getTotalTokens()));
```

All callbacks fire on the EDT. `AsyncResource.cancel()` closes the socket on a streaming call.

### Roles, message parts, multi-modal

```java
ChatMessage.system("You are a tour guide.");
ChatMessage.user("Describe Paris.");
ChatMessage.assistant("Paris is…");

// Multi-modal: attach an image
ImagePart photo = new ImagePart(jpegBytes, "image/jpeg");                  // inline
ImagePart byUrl = new ImagePart("https://example.com/x.png");              // remote
ChatMessage withImg = ChatMessage.userWithImage("Describe the photo.", photo);
```

### Tool calling

```java
Tool weather = new Tool(
    "get_weather",
    "Return the current weather for a city.",
    "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}," +
        "\"required\":[\"city\"]}",
    argsJson -> "{\"tempC\":22}");                                          // ToolHandler

ChatRequest req = ChatRequest.builder()
    .model("gpt-4o-mini")
    .addMessage(ChatMessage.user("Weather in Paris?"))
    .tools(Collections.singletonList(weather))
    .toolChoice(ToolChoice.AUTO)                                            // .NONE | .REQUIRED | .named("x")
    .build();

client.chat(req).ready(resp -> {
    for (ToolCall call : resp.getToolCalls()) {
        String result = call.execute(Collections.singletonList(weather));
        // Feed result back as ChatMessage.toolResult(call.getId(), result)
        // and call chat() again.
    }
});
```

### Structured output

```java
ChatRequest req = ChatRequest.builder()
    .model("gpt-4o-mini")
    .responseFormat(ResponseFormat.JSON_OBJECT)
    .addMessage(ChatMessage.system("Return JSON with keys city, population."))
    .addMessage(ChatMessage.user("Tel Aviv"))
    .build();
```

### Embeddings

```java
EmbeddingRequest req = EmbeddingRequest.builder()
    .model("text-embedding-3-small")
    .addInput("a cat sat on the mat")
    .build();

client.embed(req).ready(resp -> {
    float[] v = resp.getData().get(0).getVector();
    // store, compare cosine similarity, etc.
});
```

### Image generation

```java
ImageGenerator gen = ImageGenerator.openai(apiKey);                         // DALL-E 3
GenerateImageRequest req = new GenerateImageRequest("Watercolor of a beach at sunset");
req.setSize("1024x1024");
gen.generate(req).ready(image -> form.add(new Label(image)).revalidate());
```

`ImageGenerator.onDevice()` routes to the optional `cn1-ai-stablediffusion` cn1lib for on-device Stable Diffusion. That cn1lib carries multi-GB native blobs; the build server flips `cn1.ai.requiresBigUpload` and asks you to build locally if your project bundles it.

### Conversation persistence, retries, prompts, tokens

| Class | Purpose |
| --- | --- |
| `ConversationStore(key)` | JSON-serialize a `List<ChatMessage>` to/from `Storage` |
| `PromptTemplate.of("Translate {text} to {lang}")` | Trivial `{placeholder}` substitution |
| `Tokenizer.estimate(text)` / `Tokenizer.estimateMessages(history)` | Approximate token count |
| `RetryPolicy.exponentialBackoff()` | 4 attempts, 500ms→30s, jitter; honors `Retry-After` from `LlmException.getRetryAfterSeconds()` |
| `SafetyFilter.check(messages)` | Returns `null` to allow, non-null reason to block; pre-flight gate |

### LlmException

Single checked exception extending `IOException`. Use `LlmException.getType()` to switch on:

```
AUTH, RATE_LIMIT, INVALID_REQUEST, CONTEXT_LENGTH,
MODEL_OVERLOADED, SERVER, NETWORK, UNKNOWN
```

`getHttpStatus()`, `getProviderErrorCode()`, `getRawBody()`, `getRetryAfterSeconds()` are also exposed for logging and retry decisions.

### Simulator redirect (offline Ollama)

The JavaSE simulator probes `localhost:11434` at startup. Two system properties drive the redirect:

| Property | Default | Effect |
| --- | --- | --- |
| `cn1.ai.simulatorRedirect` | `auto` in simulator, `disabled` on device | `auto` redirects OpenAI calls to local Ollama when Ollama is reachable. `ollama` forces redirect. `disabled` always hits the configured provider. |
| `cn1.ai.ollamaUrl` | `http://localhost:11434/v1` | Override the Ollama endpoint URL |
| `cn1.ai.ollamaModel` | `llama3.2` | Override the local model name |
| `cn1.ai.ollamaDetected` | (read-only) | `true` if the startup probe found Ollama |

Production code calling `LlmClient.openai(...)` runs unchanged in the simulator against the local model. No API charges, no network round-trip.

## ChatView — streaming chat surface

```java
import com.codename1.components.*;

Form chat = new Form("Assistant", new BorderLayout());
ChatView view = new ChatView();
chat.add(BorderLayout.CENTER, view);

view.addMessage(ChatMessage.assistant("How can I help?"));

view.setOnSend(e -> {
    String text = view.getInput().getText();
    view.getInput().clear();
    view.addMessage(ChatMessage.user(text));
    view.setTypingIndicatorVisible(true);
    ChatBubble streaming = view.beginAssistantStream();

    ChatRequest req = ChatRequest.builder()
        .model("gpt-4o-mini").messages(view.getHistory()).build();

    LlmClient.openai(apiKey).chatStream(req, new StreamingListener.Adapter() {
        @Override public void onContentDelta(String d) {
            view.appendToLastMessage(d);
        }
    }).ready(resp -> view.setTypingIndicatorVisible(false));
});
chat.show();
```

The component is thread-safe: `addMessage`, `appendToLastMessage`, and `setTypingIndicatorVisible` marshal through `Display.callSerially` internally.

### One-line wiring

```java
LlmChatBinding.bind(view,
    LlmClient.openai(apiKey),
    ChatRequest.builder().model("gpt-4o-mini").build());
```

This wires the input bar to `chatStream(...)` and replays the conversation history on every turn — fine for prototypes, replace with a custom send pipeline when you need tool calls, structured output, or analytics.

### Theming

CSS-style UIIDs:

```
ChatView                — outer container
ChatViewMessages        — scrollable column
ChatBubbleUser          — user bubble container
ChatBubbleAssistant     — assistant bubble container
ChatBubbleSystem        — system bubble container
ChatBubbleText          — TextArea inside every bubble
ChatTypingIndicator     — animated dots
ChatInput               — input strip
ChatInputField          — text field
ChatSendButton          — send button
ChatAttachButton        — attach button   (hidden when setOnAttach not called)
ChatVoiceButton         — voice button    (hidden when setOnVoice not called)
```

Override `ChatView.createBubble(message)` to plug in a `ChatBubble` subclass with custom rendering.

## SpeechRecognizer

iOS uses `SFSpeechRecognizer`, Android uses `android.speech.SpeechRecognizer`. JavaSE is unsupported unless the optional `cn1-ai-whisper` cn1lib is on the classpath.

```java
import com.codename1.media.*;

if (!SpeechRecognizer.isSupported()) { /* degrade gracefully */ return; }

RecognitionOptions opts = new RecognitionOptions()
    .setLanguageTag("en-US")            // BCP-47
    .setPartialResults(true)
    .setContinuous(false)
    .setMaxResults(3);

SpeechRecognizer.recognize(opts, new RecognitionCallback.Adapter() {
    @Override public void onPartialResult(String t) { chatView.getInput().setText(t); }
    @Override public void onResult(String t, float confidence, String[] alternatives) {
        chatView.addMessage(ChatMessage.user(t));
    }
});

// SpeechRecognizer.stop()    // end the active session
```

The build-time scanner adds the `NSSpeechRecognitionUsageDescription` and `NSMicrophoneUsageDescription` `Info.plist` strings, and Android `RECORD_AUDIO` permission, automatically.

## TextToSpeech

iOS uses `AVSpeechSynthesizer`, Android uses `android.speech.tts.TextToSpeech`, JavaSE falls back to `say` on macOS, `espeak` on Linux, and SAPI on Windows.

```java
import com.codename1.media.*;

if (TextToSpeech.isSupported()) {
    TtsOptions opts = new TtsOptions()
        .setLanguageTag("fr-FR")
        .setRate(1.0f)
        .setPitch(1.0f)
        .setVolume(1.0f);
    TextToSpeech.speak("Bonjour", opts);
}

// TextToSpeech.stop();             // cancel current utterance
// TextToSpeech.getAvailableVoices();// platform voice identifiers
```

## SecureStorage — non-prompting overloads for LLM keys

**Never hard-code a provider API key in source, ship it in a bundled resource, or commit it to git.** Mobile binaries are trivially reverse-engineered; any key embedded in the app is a key your users can extract. Fetch the key from a server endpoint the user authenticates against, then cache it locally with the non-prompting `SecureStorage` overloads:

```java
SecureStorage store = SecureStorage.getInstance();
store.set("openai.key", apiKeyFromServer);       // returns false when unsupported
String key = store.get("openai.key");            // returns null when absent
LlmClient client = LlmClient.openai(key);
```

Base class returns `null` / `false` on platforms without an implementation, so you can wire this in without a platform check.

## ML Kit cn1libs

Three cn1libs, each backed by ML Kit on iOS and Android. Drop the `<type>pom</type>` dependency in `common/pom.xml` and the build-time scanner takes care of Pods, Gradle deps, permissions, and usage strings.

```xml
<dependency>
    <groupId>com.codenameone</groupId>
    <artifactId>cn1-ai-mlkit-barcode-lib</artifactId>
    <version>${cn1.version}</version>
    <type>pom</type>
</dependency>
```

### Barcode scanner

```java
import com.codename1.ai.mlkit.barcode.BarcodeScanner;

Capture.capturePhoto(evt -> {
    String path = (String) evt.getSource();
    byte[] bytes = readAllBytes(path);
    BarcodeScanner.scan(bytes).ready(values -> {
        for (String v : values) Log.p("Detected: " + v);
    });
});
```

Decodes QR, EAN, Code 128, and the other ML Kit-supported formats.

### Document scanner

```java
import com.codename1.ai.mlkit.docscan.DocumentScanner;

DocumentScanner.scanToFile(jpegBytes).ready(filePath -> {
    // filePath points to the cropped, corrected document image
});
```

iOS uses `VisionKit`. Android uses the Google Play Services document scanner — on devices without Play Services, the call resolves through `AsyncResource.except(...)`.

### Face detector

```java
import com.codename1.ai.mlkit.face.FaceDetector;

FaceDetector.detect(jpegBytes).ready(rects -> {
    for (int i = 0; i < rects.length; i += 4) {
        int x = rects[i], y = rects[i + 1], w = rects[i + 2], h = rects[i + 3];
        // draw rectangle, crop, etc.
    }
});
```

Returns a packed `int[]` — four ints per face.

## Common patterns

### Capture photo → describe via multi-modal LLM → speak the result

```java
Capture.capturePhoto(evt -> {
    String path = (String) evt.getSource();
    byte[] bytes = readAllBytes(path);
    ImagePart img = new ImagePart(bytes, "image/jpeg");

    ChatRequest req = ChatRequest.builder()
        .model("gpt-4o")
        .addMessage(ChatMessage.userWithImage("Describe the photo.", img))
        .build();

    chatView.addMessage(req.getMessages().get(0));
    chatView.beginAssistantStream();
    StringBuilder full = new StringBuilder();

    LlmClient.openai(apiKey).chatStream(req, new StreamingListener.Adapter() {
        @Override public void onContentDelta(String d) {
            full.append(d);
            chatView.appendToLastMessage(d);
        }
    }).ready(resp -> TextToSpeech.speak(full.toString()));
});
```

### Voice-driven turn

```java
view.setOnVoice(e -> SpeechRecognizer.recognize(
    new RecognitionOptions().setPartialResults(true),
    new RecognitionCallback.Adapter() {
        @Override public void onPartialResult(String t) {
            view.getInput().setText(t);
        }
        @Override public void onResult(String t, float c, String[] alts) {
            view.getInput().setText(t);
            // Trigger setOnSend manually if you want auto-send
        }
    }));
```

### Offline development against Ollama

1. `brew install ollama && ollama pull llama3.2`
2. `ollama serve` (default port `11434`)
3. Run the simulator. Production code calling `LlmClient.openai(...)` is automatically redirected. Override with `-Dcn1.ai.simulatorRedirect=ollama -Dcn1.ai.ollamaModel=llama3.2`.

## What NOT to do

- **Don't reach for `Class.forName(...)` to discover providers.** Obfuscation renames classes in shipped builds; reflective name lookups work in the simulator but fail in production. The factory methods on `LlmClient` and `ImageGenerator` already give you the indirection you need.
- **Don't store an API key in source.** Use `SecureStorage.get("openai.key")` (single-arg overload) or pull it from a server-side proxy. Hard-coded keys leak through reverse-engineered binaries.
- **Don't call `chatStream` from a tight UI loop.** A streaming call holds an HTTP connection until the response completes; one per user turn is correct, one per keystroke is a bug.
- **Don't mutate `ChatView` on a non-EDT thread without going through the documented mutators.** `addMessage`, `appendToLastMessage`, and `setTypingIndicatorVisible` are thread-safe; arbitrary `view.add(...)` calls are not.
- **Don't assume the document scanner works on every Android device.** It requires Google Play Services. Wrap the call and fall back to `Capture.capturePhoto(...)` if the scanner returns an error.
- **Don't ship a project that bundles `cn1-ai-stablediffusion` to the cloud build server without checking.** The cn1lib carries multi-GB native blobs; the build will reject the upload with a `cn1.ai.requiresBigUpload` hint. Build locally for those projects.
