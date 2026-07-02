// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::ai-and-speech-java-001[]
import com.codename1.ai.*;

// OpenAI (also drives Ollama, vLLM, llama.cpp, and other
// OpenAI-compatible endpoints).
LlmClient openai = LlmClient.openai(apiKey);

// Local Ollama on the default port (http://localhost:11434).
LlmClient ollama = LlmClient.ollama("llama3.2");

// Any OpenAI-compatible endpoint (Together, Groq, Fireworks, vLLM, ...).
LlmClient together = LlmClient.localOpenAiCompatible(
        "https://api.together.xyz/v1",
        apiKey,
        "meta-llama/Llama-3.3-70B-Instruct-Turbo");

// Anthropic and Google Gemini, both via their OpenAI-compatible
// endpoints. The wire format is identical; only the base URL,
// default model, and auth differ.
LlmClient anthropic = LlmClient.anthropic(apiKey);
LlmClient gemini = LlmClient.gemini(apiKey);
// end::ai-and-speech-java-001[]

// tag::ai-and-speech-java-002[]
ChatRequest req = ChatRequest.builder()
        .model("gpt-4o-mini")
        .addMessage(ChatMessage.system("Reply in haiku."))
        .addMessage(ChatMessage.user("Describe a Codename One app."))
        .temperature(0.7f)
        .maxTokens(200)
        .build();

openai.chat(req).ready(resp -> {
    Dialog.show("Reply", resp.getText(), "OK", null);
}).except(err -> {
    Log.e(err);
});
// end::ai-and-speech-java-002[]

// tag::ai-and-speech-java-003[]
StringBuilder buffer = new StringBuilder();

openai.chatStream(req, new StreamingListener.Adapter() {
    @Override
    public void onContentDelta(String delta) {
        buffer.append(delta);
        chatView.appendToLastMessage(delta);
    }

    @Override
    public void onError(Throwable t) {
        Log.e(t);
    }
}).ready(resp -> {
    Log.p("Total tokens: " + resp.getUsage().getTotalTokens());
});
// end::ai-and-speech-java-003[]

// tag::ai-and-speech-java-004[]
Tool weather = new Tool(
        "get_weather",
        "Return the current weather for a city.",
        "{\"type\":\"object\",\"properties\":{" +
            "\"city\":{\"type\":\"string\"}}," +
            "\"required\":[\"city\"]}",
        argumentsJson -> {
            Map<String, Object> args = JsonHelper.parseObject(argumentsJson);
            return "{\"tempC\": 22, \"city\": \"" + args.get("city") + "\"}";
        });

ChatRequest req = ChatRequest.builder()
        .model("gpt-4o-mini")
        .addMessage(ChatMessage.user("What is the weather in Tel Aviv?"))
        .tools(Collections.singletonList(weather))
        .toolChoice(ToolChoice.AUTO)
        .build();

openai.chat(req).ready(resp -> {
    for (ToolCall call : resp.getToolCalls()) {
        String result = call.execute(Collections.singletonList(weather));
        // Feed the tool result back as a new turn and call chat() again.
    }
});
// end::ai-and-speech-java-004[]

// tag::ai-and-speech-java-005[]
ChatRequest req = ChatRequest.builder()
        .model("gpt-4o-mini")
        .responseFormat(ResponseFormat.JSON_OBJECT)
        .addMessage(ChatMessage.system(
            "Return a JSON object with keys city and population."))
        .addMessage(ChatMessage.user("Tel Aviv"))
        .build();
// end::ai-and-speech-java-005[]

// tag::ai-and-speech-java-006[]
ImagePart photo = new ImagePart(receiptBytes, "image/jpeg");
ChatMessage msg = ChatMessage.userWithImage(
        "Extract the line items from this receipt.", photo);

ChatRequest req = ChatRequest.builder()
        .model("gpt-4o")
        .addMessage(msg)
        .build();
// end::ai-and-speech-java-006[]

// tag::ai-and-speech-java-007[]
EmbeddingRequest req = EmbeddingRequest.builder()
        .model("text-embedding-3-small")
        .addInput("a cat sat on the mat")
        .addInput("a feline rested on the rug")
        .build();

openai.embed(req).ready(resp -> {
    float[] v0 = resp.getData().get(0).getVector();
    float[] v1 = resp.getData().get(1).getVector();
    // Compute cosine similarity, persist to Storage, etc.
});
// end::ai-and-speech-java-007[]

// tag::ai-and-speech-java-008[]
ImageGenerator gen = ImageGenerator.openai(apiKey);
GenerateImageRequest req = new GenerateImageRequest(
        "A pastel watercolor of a Tel Aviv beach at sunset");
req.setSize("1024x1024");
req.setQuality("hd");

gen.generate(req).ready(image -> {
    Label preview = new Label(image);
    Display.getInstance().getCurrent().add(preview).revalidate();
});
// end::ai-and-speech-java-008[]

// tag::ai-and-speech-java-009[]
ConversationStore store = new ConversationStore("chat-history");
List<ChatMessage> history = store.load();        // empty list on first call

history.add(ChatMessage.user("Hello"));
ChatResponse resp = openai.chat(
        ChatRequest.builder().model("gpt-4o-mini").messages(history).build())
    .get();                                       // blocking helper, EDT-safe
history.add(resp.getAssistantMessage());
store.save(history);
// end::ai-and-speech-java-009[]

// tag::ai-and-speech-java-010[]
PromptTemplate t = PromptTemplate.of(
        "Translate the following from {source} to {target}: {text}");
t.put("source", "English");
t.put("target", "French");
t.put("text", "good morning");

ChatMessage user = t.asUser();                    // wraps the rendered string
// end::ai-and-speech-java-010[]

// tag::ai-and-speech-java-011[]
SecureStorage store = SecureStorage.getInstance();
store.set("openai.key", apiKeyFromServer);        // returns false when unsupported

String key = store.get("openai.key");             // returns null when absent
LlmClient client = LlmClient.openai(key);
// end::ai-and-speech-java-011[]

// tag::ai-and-speech-java-012[]
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
            .model("gpt-4o-mini")
            .messages(view.getHistory())
            .build();

    LlmClient.openai(apiKey).chatStream(req, new StreamingListener.Adapter() {
        @Override public void onContentDelta(String d) {
            view.appendToLastMessage(d);
        }
    }).ready(resp -> view.setTypingIndicatorVisible(false));
});
chat.show();
// end::ai-and-speech-java-012[]

// tag::ai-and-speech-java-013[]
LlmChatBinding.bind(view,
        LlmClient.openai(apiKey),
        ChatRequest.builder().model("gpt-4o-mini").build());
// end::ai-and-speech-java-013[]

// tag::ai-and-speech-java-014[]
import com.codename1.media.*;

if (!SpeechRecognizer.isSupported()) {
    Dialog.show("Unavailable", "Speech is not supported on this device.",
                "OK", null);
    return;
}

RecognitionOptions opts = new RecognitionOptions()
        .setLanguageTag("en-US")
        .setPartialResults(true)
        .setContinuous(false)
        .setMaxResults(3);

SpeechRecognizer.recognize(opts, new RecognitionCallback.Adapter() {
    @Override public void onPartialResult(String transcript) {
        chatView.getInput().setText(transcript);
    }
    @Override public void onResult(String transcript, float confidence,
                                   String[] alternatives) {
        chatView.addMessage(ChatMessage.user(transcript));
    }
});
// end::ai-and-speech-java-014[]

// tag::ai-and-speech-java-015[]
if (TextToSpeech.isSupported()) {
    TtsOptions opts = new TtsOptions()
            .setLanguageTag("fr-FR")
            .setRate(1.0f);
    TextToSpeech.speak("Bonjour", opts);
}
// end::ai-and-speech-java-015[]

// tag::ai-and-speech-java-016[]
import com.codename1.ai.mlkit.barcode.BarcodeScanner;

Capture.capturePhoto(new ActionListener() {
    @Override public void actionPerformed(ActionEvent evt) {
        String path = (String) evt.getSource();
        try (InputStream in = FileSystemStorage.getInstance().openInputStream(path)) {
            byte[] bytes = Util.readInputStream(in);
            BarcodeScanner.scan(bytes).ready(values -> {
                for (String v : values) {
                    Log.p("Detected: " + v);
                }
            });
        } catch (IOException ex) {
            Log.e(ex);
        }
    }
});
// end::ai-and-speech-java-016[]

// tag::ai-and-speech-java-017[]
import com.codename1.ai.mlkit.face.FaceDetector;

FaceDetector.detect(jpegBytes).ready(rects -> {
    for (int i = 0; i < rects.length; i += 4) {
        Log.p("Face at (" + rects[i] + "," + rects[i + 1]
                + ") size " + rects[i + 2] + "x" + rects[i + 3]);
    }
});
// end::ai-and-speech-java-017[]

// tag::ai-and-speech-java-018[]
Capture.capturePhoto(evt -> {
    String path = (String) evt.getSource();
    byte[] bytes = readAllBytes(path);
    ImagePart img = new ImagePart(bytes, "image/jpeg");

    ChatRequest req = ChatRequest.builder()
            .model("gpt-4o")
            .addMessage(ChatMessage.userWithImage(
                "Describe the photo in one sentence.", img))
            .build();

    chatView.addMessage(req.getMessages().get(0));
    ChatBubble streaming = chatView.beginAssistantStream();
    StringBuilder full = new StringBuilder();

    LlmClient.openai(apiKey).chatStream(req, new StreamingListener.Adapter() {
        @Override public void onContentDelta(String d) {
            full.append(d);
            chatView.appendToLastMessage(d);
        }
    }).ready(resp -> {
        TextToSpeech.speak(full.toString());
    });
});
// end::ai-and-speech-java-018[]
