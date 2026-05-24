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
package com.codename1.components;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.Role;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;

/// One row in a [ChatView]. Renders a [ChatMessage] as a styled
/// container holding a `TextArea` for the body text. Defaults the
/// UIID based on the message [Role]: `ChatBubbleUser`,
/// `ChatBubbleAssistant`, `ChatBubbleSystem`.
///
/// The body `TextArea` is non-editable and uses native scrolling
/// behaviour off; it wraps within the bubble. Apps that want richer
/// rendering (markdown, code blocks) can subclass and override
/// [#renderBody] without rewriting the wrapper.
public class ChatBubble extends Container {
    private final TextArea body;
    private final ChatMessage message;

    public ChatBubble(ChatMessage message) {
        super(new BorderLayout());
        this.message = message;
        setUIID(defaultUiidFor(message.getRole()));
        this.body = new TextArea(message.getText());
        body.setEditable(false);
        body.setUIID("ChatBubbleText");
        body.setGrowByContent(true);
        body.setActAsLabel(true);
        body.getAllStyles().setBgTransparency(0);
        add(BorderLayout.CENTER, body);
    }

    /// Replace the bubble's body text and re-render. Safe to call
    /// from any thread; the actual mutation is marshalled to the
    /// EDT.
    public void setText(final String text) {
        if (Display.getInstance().isEdt()) {
            applyText(text);
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                applyText(text);
            }
        });
    }

    private void applyText(String text) {
        body.setText(text == null ? "" : text);
        revalidateLater();
    }

    /// Append a token-sized delta to the bubble's body. Used by
    /// [ChatView#appendToLastMessage] during LLM streaming.
    public void appendText(final String delta) {
        if (delta == null || delta.length() == 0) {
            return;
        }
        if (Display.getInstance().isEdt()) {
            applyText(body.getText() + delta);
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                applyText(body.getText() + delta);
            }
        });
    }

    public ChatMessage getMessage() {
        return message;
    }

    public String getBubbleText() {
        return body.getText();
    }

    /// Returns the inner `TextArea` for styling tweaks beyond the
    /// UIID hooks (e.g. setting a custom font).
    protected TextArea getBody() {
        return body;
    }

    private static String defaultUiidFor(Role role) {
        if (role == Role.USER) {
            return "ChatBubbleUser";
        }
        if (role == Role.ASSISTANT) {
            return "ChatBubbleAssistant";
        }
        if (role == Role.SYSTEM) {
            return "ChatBubbleSystem";
        }
        return "ChatBubble";
    }

    /// Subclass hook for custom rendering of the body. Default
    /// behaviour is to keep the inner TextArea in sync with whatever
    /// text has been set; override to swap in a different child
    /// component.
    protected void renderBody() {
        // Default: nothing to do -- the wrapper already adds the
        // TextArea in the constructor.
    }

    // No initComponent() override needed -- the framework consults
    // UIManager for the bubble's UIID-driven styles during the
    // default attach lifecycle.
}
