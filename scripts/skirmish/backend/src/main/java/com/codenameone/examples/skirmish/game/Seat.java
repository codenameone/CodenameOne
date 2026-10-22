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
package com.codenameone.examples.skirmish.game;

import com.codename1.backend.WebSocketSession;

/// One side of a [MatchRoom]: a player and the socket they are on now, which
/// changes when they reconnect, or the server's bot.
final class Seat {
    final String username;
    final String name;
    final String[] deck;
    final int trophies;
    WebSocketSession session;
    /// Ticks at which this seat's recent plays arrived, for flood detection.
    final int[] recent = new int[8];
    int recentCount;
    int rejected;
    int floods;
    int desyncs;

    Seat(String username, String name, String[] deck, int trophies, WebSocketSession session) {
        this.username = username;
        this.name = name;
        this.deck = deck;
        this.trophies = trophies;
        this.session = session;
    }

    boolean isBot() {
        return username.length() == 0;
    }
}
