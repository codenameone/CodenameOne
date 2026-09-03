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

        // The view's history is what it displays. A streaming reply is a bubble
        // there, not a message -- appendText changes the bubble's text and never
        // the immutable ChatMessage the view stored -- so every completed reply
        // would go back to the model as a blank assistant turn. Keep the
        // conversation the request is built from separately.
        List<ChatMessage> history = new ArrayList<ChatMessage>();

        ChatMessage greeting = ChatMessage.assistant("How can I help?");
        view.addMessage(greeting);
        history.add(greeting);

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
            ChatMessage sent = ChatMessage.user(text);
            view.addMessage(sent);
            history.add(sent);
            view.setTypingIndicatorVisible(true);

            ChatRequest req = ChatRequest.builder()
                    // No model named: the client's default applies, and the
                    // simulator's Ollama redirect sets that for you. Naming a
                    // cloud model asks a local server for one it lacks.
                    .messages(new ArrayList<ChatMessage>(history))
                    .build();

            ChatBubble streaming = view.beginAssistantStream();
            LlmClient.openai(apiKey).chatStream(req, new StreamingListener.Adapter() {
                @Override public void onContentDelta(String d) {
                    // Append to the bubble this send opened, not to whichever is
                    // newest: a second send while this one is still streaming
                    // would otherwise divert this response into that bubble.
                    streaming.appendText(d);
                }
            }).ready(resp -> {
                sending[0] = false;
                view.setTypingIndicatorVisible(false);
                // Record what the assistant actually said, so the next turn
                // carries it rather than an empty placeholder.
                history.add(resp.getAssistantMessage());
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
}
