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
package com.codenameone.examples.splat.game;

import com.codename1.backend.WebSocketSession;

/// One place in a [MatchRoom]: a player and the socket they are on now, which
/// changes when they reconnect, or one of the server's bots.
final class Seat {
    final String username;
    final String name;
    final String marker;
    final int trophies;
    final int team;
    WebSocketSession session;
    /// Arrival ticks of recent inputs, for flood detection.
    final int[] recent = new int[48];
    int recentCount;
    int floods;
    /// Inputs that turned further in one tick than a wrist can.
    int snaps;
    /// The newest input sequence number received, and the one the last step used.
    int lastSeq = -1;
    int appliedSeq = -1;
    /// The player left; their paintballer stands there, bewildered, with no input.
    boolean left;

    Seat(String username, String name, String marker, int trophies, int team, WebSocketSession session) {
        this.username = username;
        this.name = name;
        this.marker = marker;
        this.trophies = trophies;
        this.team = team;
        this.session = session;
    }

    boolean isBot() {
        return username.length() == 0;
    }
}
