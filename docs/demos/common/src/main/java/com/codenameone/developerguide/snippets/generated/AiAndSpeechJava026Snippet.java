/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

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
import java.util.List;
import java.util.*;
import com.codename1.ai.*;

class AiAndSpeechJava026Snippet {


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
        // tag::ai-and-speech-java-026[]
        Form chat = new Form("Assistant", new BorderLayout());
        ChatView view = new ChatView();
        chat.add(BorderLayout.CENTER, view);

        view.addMessage(ChatMessage.assistant("How can I help?"));

        // One request at a time. A second send while the first is still
        // streaming builds its request without the first reply, and the two
        // completions then append to the history in whatever order they finish,
        // producing turns like user A, user B, assistant B, assistant A.
        boolean[] sending = {false};

        view.setOnSend(e -> {
            if (sending[0]) {
                return;
            }
            sending[0] = true;
            String text = view.getInput().getText();
            view.getInput().clear();
            view.addMessage(ChatMessage.user(text));
            view.setTypingIndicatorVisible(true);

            // Snapshot the history before opening the assistant bubble. The view
            // records streamed text as it arrives, so completed replies are
            // already in there; the one thing to leave out is the empty
            // placeholder that beginAssistantStream() is about to append.
            //
            // No model named: the client's default applies, and the simulator's
            // Ollama redirect sets that for you.
            ChatRequest req = ChatRequest.builder()
                    .messages(new ArrayList<ChatMessage>(view.getHistory()))
                    .build();

            ChatBubble streaming = view.beginAssistantStream();
            // The proxy client from the credentials section: on a device
            // LlmClient.openai(apiKey) would need the billable provider key to
            // be present there, which that section says not to do.
            client.chatStream(req, new StreamingListener.Adapter() {
                @Override public void onContentDelta(String d) {
                    // Append to the bubble this send opened, not to whichever is
                    // newest: a second send while this one is still streaming
                    // would otherwise divert this response into that bubble.
                    streaming.appendText(d);
                }
            }).ready(resp -> {
                sending[0] = false;
                view.setTypingIndicatorVisible(false);
            }).except(err -> {
                // Without this the indicator stays up for good on a failure, and
                // the guard above would block every later send.
                sending[0] = false;
                view.setTypingIndicatorVisible(false);
                Log.e(err);
            });
        });
        chat.show();
        // end::ai-and-speech-java-026[]
    }

    LlmClient client = LlmClient.localOpenAiCompatible(
            "https://api.example.com/ai/v1", "session-token", "your-model");

}
