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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.AckDto;
import com.codenameone.examples.parley.api.KeyCountDto;
import com.codenameone.examples.parley.api.MessageApiServer;
import com.codenameone.examples.parley.api.OutgoingDto;
import com.codenameone.examples.parley.api.SendResultDto;
import com.codenameone.examples.parley.api.SyncDto;

/// The server's half of `MessageApi`.
@Component
public class MessageEndpoint implements MessageApiServer {
    private final Messages messages;

    public MessageEndpoint(Messages messages) {
        this.messages = messages;
    }

    @Override
    public SendResultDto send(OutgoingDto message) throws Exception {
        return messages.send(Caller.name(), message);
    }

    @Override
    public SyncDto sync(String device, int limit) throws Exception {
        return messages.sync(Caller.name(), device, limit);
    }

    @Override
    public KeyCountDto ack(String device, AckDto ack) throws Exception {
        return messages.ack(Caller.name(), device, ack);
    }
}
