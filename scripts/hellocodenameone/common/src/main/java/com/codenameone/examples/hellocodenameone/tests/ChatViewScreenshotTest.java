package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ai.ChatMessage;
import com.codename1.components.ChatView;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Renders ChatView with a representative system+user+assistant exchange
/// plus the typing indicator visible. Baselines this PR's new AI UI surface
/// against iOS Modern + Android Material themes (the existing
/// build-ios-metal / build-ios / Build Android screenshot jobs pick up the
/// emitted "ChatView" image automatically).
public class ChatViewScreenshotTest extends BaseTest {
    private ChatView chat;

    @Override
    public boolean runTest() {
        Form form = createForm("ChatView", new BorderLayout(), "ChatView");
        chat = new ChatView();

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

        Container body = (Container) form.getContentPane();
        body.add(BorderLayout.CENTER, chat);
        form.show();
        return true;
    }
}
