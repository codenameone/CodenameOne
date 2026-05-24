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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The input strip at the bottom of a [ChatView]. Contains an
/// optional attach button, a single-line text field, an optional
/// voice button, and a send button.
///
/// Each button is exposed via a setter ([#setOnAttach],
/// [#setOnVoice], [#setOnSend]) and visible only when its listener
/// is non-null. Listeners receive an [ActionEvent] whose `source`
/// is this [ChatInput].
///
/// Default UIIDs: `ChatInput` on the container, `ChatInputField` on
/// the text field, `ChatSendButton` / `ChatAttachButton` /
/// `ChatVoiceButton` on the respective buttons.
public class ChatInput extends Container {
    private final TextField field;
    private final Button send;
    private final Button attach;
    private final Button voice;

    private ActionListener onSend;
    private ActionListener onAttach;
    private ActionListener onVoice;

    public ChatInput() {
        super(new BorderLayout());
        setUIID("ChatInput");
        field = new TextField();
        field.setUIID("ChatInputField");
        field.setSingleLineTextArea(false);
        field.setHint("Message");
        send = new Button("Send");
        send.setUIID("ChatSendButton");
        send.setVisible(false);
        attach = new Button("+");
        attach.setUIID("ChatAttachButton");
        attach.setVisible(false);
        voice = new Button("Mic");
        voice.setUIID("ChatVoiceButton");
        voice.setVisible(false);

        // Pressing Enter (or "Done" on a mobile soft keyboard) acts
        // like tapping Send.
        field.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fireSendIfNonEmpty();
            }
        });
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fireSendIfNonEmpty();
            }
        });
        attach.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (onAttach != null) {
                    onAttach.actionPerformed(new ActionEvent(ChatInput.this));
                }
            }
        });
        voice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (onVoice != null) {
                    onVoice.actionPerformed(new ActionEvent(ChatInput.this));
                }
            }
        });

        Container right = new Container(new BoxLayout(BoxLayout.X_AXIS));
        right.add(voice);
        right.add(send);
        add(BorderLayout.WEST, attach);
        add(BorderLayout.CENTER, field);
        add(BorderLayout.EAST, right);
    }

    public TextField getField() {
        return field;
    }

    public String getText() {
        return field.getText() == null ? "" : field.getText();
    }

    public void setText(String text) {
        field.setText(text);
    }

    public void clear() {
        field.setText("");
    }

    public ChatInput setOnSend(ActionListener listener) {
        this.onSend = listener;
        send.setVisible(listener != null);
        return this;
    }

    public ChatInput setOnAttach(ActionListener listener) {
        this.onAttach = listener;
        attach.setVisible(listener != null);
        return this;
    }

    public ChatInput setOnVoice(ActionListener listener) {
        this.onVoice = listener;
        voice.setVisible(listener != null);
        return this;
    }

    public Button getSendButton() {
        return send;
    }

    public Button getAttachButton() {
        return attach;
    }

    public Button getVoiceButton() {
        return voice;
    }

    private void fireSendIfNonEmpty() {
        String t = getText();
        if (t.length() == 0 || onSend == null) {
            return;
        }
        onSend.actionPerformed(new ActionEvent(this));
    }
}
