/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI coverage for {@link ChatInput}: text accessors, the
 * visible-when-wired button contract, fluent setter identity, and the
 * send-only-when-non-empty firing rule.
 */
class ChatInputTest extends UITestBase {

    @FormTest
    void defaultButtonsAreHidden() {
        ChatInput in = new ChatInput();
        assertFalse(in.getSendButton().isVisible());
        assertFalse(in.getAttachButton().isVisible());
        assertFalse(in.getVoiceButton().isVisible());
        assertEquals("ChatInput", in.getUIID());
    }

    @FormTest
    void getTextDefaultsToEmptyNotNull() {
        assertEquals("", new ChatInput().getText());
    }

    @FormTest
    void setTextAndClearRoundTrip() {
        ChatInput in = new ChatInput();
        in.setText("draft");
        assertEquals("draft", in.getText());
        assertEquals("draft", in.getField().getText());
        in.clear();
        assertEquals("", in.getText());
    }

    @FormTest
    void wiringSendListenerRevealsSendButtonAndReturnsThis() {
        ChatInput in = new ChatInput();
        ChatInput chained = in.setOnSend(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        });
        assertSame(in, chained);
        assertTrue(in.getSendButton().isVisible());
        in.setOnSend(null);
        assertFalse(in.getSendButton().isVisible());
    }

    @FormTest
    void wiringAttachListenerRevealsAttachButton() {
        ChatInput in = new ChatInput();
        assertSame(in, in.setOnAttach(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        }));
        assertTrue(in.getAttachButton().isVisible());
        in.setOnAttach(null);
        assertFalse(in.getAttachButton().isVisible());
    }

    @FormTest
    void wiringVoiceListenerRevealsVoiceButton() {
        ChatInput in = new ChatInput();
        assertSame(in, in.setOnVoice(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        }));
        assertTrue(in.getVoiceButton().isVisible());
        in.setOnVoice(null);
        assertFalse(in.getVoiceButton().isVisible());
    }

    @FormTest
    void pressingSendFiresListenerWhenTextPresent() {
        ChatInput in = new ChatInput();
        final AtomicInteger fired = new AtomicInteger();
        final AtomicReference<Object> source = new AtomicReference<Object>();
        in.setOnSend(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
                source.set(evt.getSource());
            }
        });
        in.setText("payload");
        in.getSendButton().pressed();
        in.getSendButton().released();
        flushSerialCalls();
        assertEquals(1, fired.get());
        assertSame(in, source.get());
    }

    @FormTest
    void pressingSendDoesNothingWhenTextEmpty() {
        ChatInput in = new ChatInput();
        final AtomicInteger fired = new AtomicInteger();
        in.setOnSend(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        in.getSendButton().pressed();
        in.getSendButton().released();
        flushSerialCalls();
        assertEquals(0, fired.get());
    }

    @FormTest
    void attachButtonFiresAttachListener() {
        ChatInput in = new ChatInput();
        final AtomicInteger fired = new AtomicInteger();
        in.setOnAttach(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        in.getAttachButton().pressed();
        in.getAttachButton().released();
        flushSerialCalls();
        assertEquals(1, fired.get());
    }

    @FormTest
    void voiceButtonFiresVoiceListener() {
        ChatInput in = new ChatInput();
        final AtomicInteger fired = new AtomicInteger();
        in.setOnVoice(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        in.getVoiceButton().pressed();
        in.getVoiceButton().released();
        flushSerialCalls();
        assertEquals(1, fired.get());
    }
}
