# Evidence map

Source: `docs/website/content/blog/platform-apis-in-the-core.md`
Canonical: https://www.codenameone.com/blog/platform-apis-in-the-core/

## Thesis

Moving AI, authentication, passkeys, and connectivity into stable framework APIs

## Supported beats

- **AI: a first-class LLM client and a ChatView component:** PR #5035 lands the com.codename1.ai package, the ChatView UI component, the speech and TTS additions, and the build-time dependency injection that wires the native pieces in.
- **LlmClient: the basic chat request:** LlmClient.openai(...), LlmClient.anthropic(...), LlmClient.gemini(...), LlmClient.ollama(...), and LlmClient.openAiCompatible(baseUrl, apiKey) are the factories. All five are fully implemented native clients. The OpenAI client also drives Ollama, vLLM, llama.cpp, and any other endpoint that speaks the OpenAI wire format, so most local-model stacks plug in through LlmClient.openAiCompatible(...) without a separate driver.
- **Streaming chat (what you actually want for chat UIs):** For any UI that types responses out token-by-token, the streaming entry point is the one to reach for. The callback fires on the EDT, so you can append directly to a text component.
- **Tool calls:** If you want the model to call back into your app, Tool / ToolChoice give you OpenAI-style function calling. Define the tool, hand the model your model and the available tools, and the response surfaces structured ToolCall objects you dispatch.
- **Embeddings:** LlmClient.embed(...) returns a vector for any input string. Useful for similarity search against a local SQLite store (tomorrow's post will cover the new ORM that pairs with this).
- **Working against Ollama in the simulator (no API charges):** JavaSEPort pings localhost:11434 at startup. If it finds Ollama, it sets the cn1.ai.ollamaDetected property. With cn1.ai.simulatorRedirect=auto (or =ollama) every LlmClient.openai(...) call routes through the local Ollama endpoint instead of OpenAI's. Production code does not change.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5035
- https://github.com/codenameone/CodenameOne/pull/5057
- https://www.codenameone.com/developer-guide/#_ai_chat_ui_and_speech
- https://github.com/codenameone/CodenameOne/pull/5018
- https://github.com/codenameone/CodenameOne/pull/5039
- https://accounts.google.com
- https://accounts.google.com/o/oauth2/auth
- https://www.codenameone.com/developer-guide/#_authentication_and_identity
- https://github.com/codenameone/CodenameOne/pull/5021
- https://www.codenameone.com/developer-guide/#_network_connectivity
- https://github.com/codenameone/CodenameOne/pull/5036
