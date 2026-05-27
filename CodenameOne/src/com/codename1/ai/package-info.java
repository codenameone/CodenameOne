/*
    Document   : package
    Created on : May 24, 2026
    Author     : Shai Almog
*/

/// Codename One's AI / LLM client surface plus the value types,
/// streaming primitives, and chat-binding helpers that sit on top
/// of it.
///
/// `com.codename1.ai.LlmClient` provides a provider-agnostic chat /
/// embeddings / image-generation API. Four static factories pick the
/// backend; the rest of the surface is shared across all of them:
///
/// ```java
/// LlmClient gpt    = LlmClient.openai(SecureStorage.getInstance().get("openai_key"));
/// LlmClient claude = LlmClient.anthropic(key);
/// LlmClient gemini = LlmClient.gemini(key);
/// LlmClient ollama = LlmClient.ollama();                   // localhost:11434
/// LlmClient local  = LlmClient.localOpenAiCompatible(
///                        "http://10.0.0.5:8080/v1", "", "qwen2.5-7b");
/// ```
///
/// All calls return [com.codename1.util.AsyncResource] so they
/// compose naturally with the rest of the framework. Streaming chat
/// fires per-token deltas via a [StreamingListener] *and* completes
/// the returned resource with the aggregated `ChatResponse` once the
/// stream closes; cancelling the resource kills the underlying
/// socket.
///
/// #### Error handling
///
/// Every failure surfaces as a single [LlmException] whose
/// [LlmException#getType] returns one of the [LlmException.ErrorType]
/// enum values (`AUTH`, `RATE_LIMIT`, `INVALID_REQUEST`,
/// `CONTEXT_LENGTH`, `MODEL_OVERLOADED`, `SERVER`, `NETWORK`,
/// `UNKNOWN`). The recommended idiom is one `catch` + `switch`:
///
/// ```java
/// try {
///     ChatResponse r = client.chat(req).get();
///     // ...
/// } catch (AsyncExecutionException ae) {
///     if (ae.getCause() instanceof LlmException) {
///         LlmException e = (LlmException) ae.getCause();
///         switch (e.getType()) {
///             case RATE_LIMIT:        scheduleRetry(e.getRetryAfterSeconds()); break;
///             case AUTH:              showLoginScreen(); break;
///             case CONTEXT_LENGTH:    trimHistory();    break;
///             default:                showError(e);
///         }
///     }
/// }
/// ```
///
/// #### Tools / function calling
///
/// Construct a [Tool] with an optional [ToolHandler] and pass it via
/// [ChatRequest.Builder#tools]; when the model emits a [ToolCall]
/// the caller invokes [ToolCall#execute(java.util.List)] to dispatch
/// to the matching handler and feed the JSON result back as a
/// [ToolResultPart] on the next turn.
///
/// #### ChatView integration
///
/// `com.codename1.components.ChatView` is a backend-agnostic
/// messaging UI. Use [LlmChatBinding#bind] to wire it to an
/// `LlmClient` in one call; for peer-to-peer chats (e.g. a WhatsApp
/// clone) attach an `ActionListener` directly to the view's
/// `setOnSend(...)` and stream peer responses through
/// `view.addMessage(ChatMessage.assistant(text))`.
///
/// #### Image generation
///
/// `ImageGenerator.openai(key)` returns DALL-E results as a
/// `com.codename1.ui.Image`. `ImageGenerator.onDevice()` resolves
/// against the optional `cn1-ai-stablediffusion` cn1lib when
/// present; absent that cn1lib's native bridge it completes with an
/// `LlmException`.
package com.codename1.ai;
