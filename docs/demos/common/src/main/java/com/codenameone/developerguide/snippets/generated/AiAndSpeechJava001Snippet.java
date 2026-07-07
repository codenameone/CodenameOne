package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;
import com.codename1.ai.*;

class AiAndSpeechJava001Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    void snippet() throws Exception {
        // tag::ai-and-speech-java-001[]
        // OpenAI (also drives Ollama, vLLM, llama.cpp, and other
        // OpenAI-compatible endpoints).
        LlmClient openai = LlmClient.openai(apiKey);

        // Local Ollama on the default port (http://localhost:11434).
        LlmClient ollama = LlmClient.ollama("llama3.2");

        // Any OpenAI-compatible endpoint (Together, Groq, Fireworks, vLLM, null).
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
    }
}
