package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ai.ChatMessage;
import com.codename1.components.ChatView;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// ChatView screenshot: representative system + user + assistant
/// exchange with the typing indicator visible. Baselines ChatBubbleUser,
/// ChatBubbleAssistant, ChatBubbleSystem, ChatBubbleText, and
/// ChatTypingIndicator UIIDs against iOS Modern + Android Material
/// modern themes (emits a `_light` and `_dark` pair, like the other
/// theme-fidelity tests in this directory).
public class ChatViewScreenshotTest extends DualAppearanceBaseTest {

    @Override
    protected String baseName() {
        return "ChatView";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        ChatView chat = new ChatView();

        // System framing (small grey bubble centred).
        chat.addMessage(ChatMessage.system("AI Travel Assistant"));

        // User -> assistant -> user thread so all three bubble UIIDs render.
        chat.addMessage(ChatMessage.user(
                "Plan a 3-day Lisbon trip in November under €900."));
        chat.addMessage(ChatMessage.assistant(
                "Sure! Day 1: Alfama walking tour + fado dinner. "
                        + "Day 2: Belem + LX Factory. Day 3: Sintra day trip. "
                        + "Estimated total: €820 (flights from Madrid)."));
        chat.addMessage(ChatMessage.user("Any vegetarian dinner spots?"));

        // Streaming-in-progress assistant bubble.
        chat.addMessage(ChatMessage.assistant("Yes — in Alfama try "));
        chat.appendToLastMessage("Boi-Cavalo (modern Portuguese, vegan menu) ");
        chat.appendToLastMessage("and Ao 26 (vegan classics).");

        // Typing indicator visible -- exercises the dots animation styling.
        chat.setTypingIndicatorVisible(true);

        form.add(BorderLayout.CENTER, chat);
    }
}
