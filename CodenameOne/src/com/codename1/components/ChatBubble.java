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
    /// The view this bubble belongs to, set when it is added. Package private:
    /// this is bookkeeping between ChatView and its bubbles, not API.
    ChatView owner;

    /// The part of the rendered body that is conversation. Annotations are
    /// shown in the bubble but never recorded here.
    private String conversation = "";

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
        // A full replace makes everything shown conversational again: whatever
        // annotation was on the bubble is gone from the body too.
        conversation = text == null ? "" : text;
        applyText(text, true);
    }

    /// Sets the body text, and tells the owning view about it when the text is
    /// part of the conversation.
    ///
    /// `sync` travels with the work rather than living in a field: appendText
    /// hops to the EDT when called from another thread, and a field cleared by
    /// the caller as soon as it returns is already back to its old value by the
    /// time the queued update runs.
    ///
    /// #### Parameters
    ///
    /// - `text`: the new body text
    /// - `sync`: true to record it in the view's history
    private void applyText(String text, boolean sync) {
        body.setText(text == null ? "" : text);
        // Keep the view's history in step with what the bubble shows. A streamed
        // reply is otherwise only ever painted: the ChatMessage the view stored
        // when this bubble was created stays empty, and anything building a
        // request from getHistory() sends a blank assistant turn.
        if (sync && owner != null) {
            // The conversation, not the body: the body may also carry
            // annotations, and those are not something the model said.
            owner.bubbleTextChanged(this, conversation);
        }
        revalidateLater();
    }

    /// Appends text that is shown but is not part of the conversation -- a
    /// stream error, say. It reaches the bubble like `#appendText` and is
    /// deliberately kept out of the owning view's history, so a later turn does
    /// not replay a network failure back to the model as if the assistant had
    /// written it.
    ///
    /// #### Parameters
    ///
    /// - `note`: the annotation to show
    public void appendAnnotation(final String note) {
        append(note, false);
    }

    /// Append a token-sized delta to the bubble's body. Used by
    /// [ChatView#appendToLastMessage] during LLM streaming.
    public void appendText(final String delta) {
        append(delta, true);
    }

    private void append(final String delta, final boolean sync) {
        if (delta == null || delta.length() == 0) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Track the conversation separately from what is rendered. An
                // annotation already on the bubble is part of the body, so
                // deriving the conversation from the body would fold it in on
                // the next delta -- and the note would reach history after all,
                // one append later than the case this guards.
                if (sync) {
                    conversation = conversation + delta;
                }
                applyText(body.getText() + delta, sync);
            }
        };
        if (Display.getInstance().isEdt()) {
            r.run();
            return;
        }
        Display.getInstance().callSerially(r);
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
