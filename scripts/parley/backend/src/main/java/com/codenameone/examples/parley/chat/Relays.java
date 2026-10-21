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
package com.codenameone.examples.parley.chat;

import com.codename1.backend.Transactions;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Isolation;
import com.codename1.backend.annotations.Propagation;

import java.util.ArrayList;
import java.util.List;

/// Where plain-text messages go besides their members' devices: the bridges
/// to Telegram and WhatsApp, and the demo bot. Each registers itself when the
/// server starts, which keeps [Messages] from depending on them -- they depend
/// on it, to post what they receive.
@Component
public class Relays {
    /// Something that wants the plain-text messages of some conversations.
    public interface Relay {
        /// A message was posted. Called after it was stored; a relay that
        /// does slow work hands it to a task of its own.
        void posted(String conversationId, String sender, String senderName, String text)
                throws Exception;
    }

    private final List<Relay> relays = new ArrayList<Relay>();

    public synchronized void register(Relay relay) {
        relays.add(relay);
    }

    /// Hands a posted message to the relays once its transaction commits:
    /// a bridge sends it on to Telegram or WhatsApp, and a message Parley
    /// rolled back must not reach a chat outside it, nor reach it twice when
    /// the user sends it again.
    void posted(String conversationId, String sender, String senderName, String text) {
        if (!Transactions.isActive()) {
            dispatch(conversationId, sender, senderName, text);
            return;
        }
        Transactions.registerSynchronization(new Transactions.Synchronization() {
            @Override
            public void afterCommit() {
                // The relays read the conversation, which takes a transaction
                // of its own once this one is over.
                try {
                    Transactions.execute(Propagation.REQUIRES_NEW, false, 0, Isolation.DEFAULT,
                            () -> {
                                dispatch(conversationId, sender, senderName, text);
                                return null;
                            });
                } catch (Exception failed) {
                    System.err.println("[relay] could not relay: " + failed);
                }
            }
        });
    }

    private void dispatch(String conversationId, String sender, String senderName, String text) {
        List<Relay> now;
        synchronized (this) {
            now = new ArrayList<Relay>(relays);
        }
        for (int iter = 0; iter < now.size(); iter++) {
            try {
                now.get(iter).posted(conversationId, sender, senderName, text);
            } catch (Exception failed) {
                System.err.println("[relay] a relay failed: " + failed);
            }
        }
    }
}
