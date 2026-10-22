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
package com.codenameone.examples.parley.bridge;

/// A network a Parley conversation can be linked to.
///
/// A bridge carries text both ways: what a Parley member writes goes out to
/// the linked chat, and what is written there comes in through the bridge's
/// webhook and is posted into the conversation. To do the first it has to
/// read the message, so a bridged conversation is not end-to-end encrypted:
/// its members' apps send it to the server in plain text, and every one of
/// them shows that it is.
public interface Bridge {
    /// The network's name, as [com.codenameone.examples.parley.api.BridgeLinkDto]
    /// carries it: `TELEGRAM` or `WHATSAPP`.
    String network();

    /// Whether the server has the credentials this bridge needs. One that is
    /// not configured cannot be linked to.
    boolean configured();

    /// Sends `text` to a chat on the network, as written by `senderName`.
    void send(String externalChat, String senderName, String text) throws Exception;
}
