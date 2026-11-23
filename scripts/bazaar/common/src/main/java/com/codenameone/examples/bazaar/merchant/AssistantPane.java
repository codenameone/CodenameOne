/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.codenameone.examples.bazaar.merchant;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.LlmClient;
import com.codename1.components.ChatView;
import com.codename1.ui.Form;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.InsightsDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The merchant assistant: questions about the store in plain words --
/// "how did the app do against the website this month?", "which carts should
/// I chase?" -- answered with the store's own numbers.
///
/// The answers come through `com.codename1.ai`: an OpenAI-compatible client
/// pointed at this store's AI route on the Bazaar server, which holds the
/// provider key. The model never sees the database; it calls the tools in
/// [AssistantTools], which answer from the last 30 days' dashboard, the
/// database-counted insights and the sales pipeline fetched here first.
final class AssistantPane extends MerchantPane {
    private static final String[] SUGGESTIONS = {
        "How are sales this month?",
        "Which channel sells the most?",
        "What are my best sellers?",
        "Where do shoppers drop off?",
        "Which carts should I chase?",
    };

    private AnalyticsDto data;
    private InsightsDto insights;
    private List<PipelineColumnDto> board;
    private ChatView chat;
    private final List<ChatMessage> history = new ArrayList<ChatMessage>();
    private boolean asking;
    /// The store the conversation is about; another store starts a new one.
    private String chatStore;

    AssistantPane() {
        super("Assistant", "assistant-pane");
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "assistant-refresh", this::load);
    }

    @Override
    void load() {
        final String store = MerchantState.storeId();
        if (chat != null && chatStore != null && !chatStore.equals(store)) {
            // Another store's numbers and answers must not carry over.
            history.clear();
            data = null;
            insights = null;
            board = null;
            asking = false;
            chat = null;
        }
        chatStore = store;
        Period p = Period.preset(Period.LAST_30, System.currentTimeMillis());
        Api.analytics().dashboard(store, p.fromParam(), p.toParam(), Net.to(found -> {
            data = found;
        }, (status, message) -> { }));
        Api.analytics().insights(store, p.fromParam(), p.toParam(), Net.to(found -> {
            insights = found;
        }, (status, message) -> { }));
        Api.pipeline().board(store, null, null, Net.to(found -> {
            board = found;
        }, (status, message) -> { }));
        if (chat == null) {
            paint();
        }
    }

    private void paint() {
        Container page = new Container(new BorderLayout());
        page.setName("assistant-page");
        chat = new ChatView();
        chat.setName("assistant-chat");
        chat.setOnSend(e -> send(chat.getInput().getText()));
        Container chips = new Container(new FlowLayout());
        chips.setName("assistant-suggestions");
        for (int iter = 0; iter < SUGGESTIONS.length; iter++) {
            final String q = SUGGESTIONS[iter];
            chips.add(Ui.secondary(Lang.tr(q), "assistant-ask-" + iter, e -> send(Lang.tr(q))));
        }
        page.add(BorderLayout.NORTH, chips);
        page.add(BorderLayout.CENTER, chat);
        chat.addMessage(ChatMessage.assistant(Lang.tr(
                "Ask me about your sales, channels, best sellers, funnel or open carts.")));
        root(page);
    }

    private LlmClient client() {
        return LlmClient.localOpenAiCompatible(Session.serverUrl() + "/api/merchant/stores/"
                + MerchantState.storeId() + "/ai/v1", "", "bazaar");
    }

    private void send(String text) {
        if (asking || text == null || text.trim().length() == 0) {
            return;
        }
        final String question = text.trim();
        chat.getInput().clear();
        chat.addMessage(ChatMessage.user(question));
        chat.setTypingIndicatorVisible(true);
        asking = true;
        AssistantTools tools = new AssistantTools(data, insights, board,
                data != null && data.currency() != null ? data.currency()
                        : MerchantState.currency());
        final List<ChatMessage> before = new ArrayList<ChatMessage>(history);
        final ChatView askedIn = chat;
        new AssistantRun(client(), tools).ask(before, question, new AssistantRun.Done() {
            @Override
            public void answered(String answer) {
                if (askedIn != chat) {
                    return;
                }
                asking = false;
                chat.setTypingIndicatorVisible(false);
                String shown = answer.length() == 0
                        ? Lang.tr("I could not answer that. Try asking another way.") : answer;
                history.add(ChatMessage.user(question));
                history.add(ChatMessage.assistant(shown));
                chat.addMessage(ChatMessage.assistant(shown));
                relayout();
            }

            @Override
            public void failed(Throwable error) {
                if (askedIn != chat) {
                    return;
                }
                asking = false;
                chat.setTypingIndicatorVisible(false);
                chat.addMessage(ChatMessage.assistant(Lang.tr(
                        "The assistant is not available right now.")));
                relayout();
            }
        });
    }

    /// Lays the whole screen out again once an answer is in: the bubble is
    /// added while the typing indicator goes away, and a pass of the list
    /// alone measured it one line tall with the rest of the answer scrolled
    /// out of sight.
    private void relayout() {
        Form f = getComponentForm();
        if (f != null) {
            f.revalidate();
        }
    }
}
