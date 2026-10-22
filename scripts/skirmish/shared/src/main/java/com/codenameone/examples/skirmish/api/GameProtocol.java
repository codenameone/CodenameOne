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
package com.codenameone.examples.skirmish.api;

/// The game socket's message types, shared so both ends spell them the same.
///
/// Every message is a small JSON object with a `type`. The socket carries only
/// intents one way and accepted commands the other; the match itself is computed
/// by the shared rules on both ends.
///
/// Client to server: [#QUEUE], [#CHALLENGE] (with `code`), [#CANCEL], [#PLAY]
/// (`card`, `x`, `y`), [#EMOTE] (`id`), [#SYNC] (`t`, `h`: the client's own hash
/// at tick t), [#RESUME], [#LEAVE], [#SPECTATE] (admins, `match`), [#PING].
///
/// Server to client: [#READY], [#QUEUED], [#START] (`match`, `seed`, `you`,
/// `deck0`, `deck1`, `name0`, `name1`, `rules`, `tick`, `log`), [#TICKS] (`t`: the
/// last tick the client may simulate, `c`: commands executed since the previous
/// message), [#HASH] (`t`, `h`), [#EMOTE], [#END] (`result`, `crowns0`,
/// `crowns1`, `trophies`, `coins`), [#FLAG] (admins), [#ERROR] (`message`),
/// [#PONG], [#KICKED].
public final class GameProtocol {
    public static final String QUEUE = "queue";
    public static final String CHALLENGE = "challenge";
    public static final String CANCEL = "cancel";
    public static final String PLAY = "play";
    public static final String EMOTE = "emote";
    public static final String SYNC = "sync";
    public static final String RESUME = "resume";
    public static final String LEAVE = "leave";
    public static final String SPECTATE = "spectate";
    public static final String PING = "ping";

    public static final String READY = "ready";
    public static final String QUEUED = "queued";
    public static final String START = "start";
    public static final String TICKS = "ticks";
    public static final String HASH = "hash";
    public static final String END = "end";
    public static final String FLAG = "flag";
    public static final String ERROR = "error";
    public static final String PONG = "pong";
    public static final String KICKED = "kicked";

    /// Ticks between the server receiving a play and executing it. Both players see
    /// it land on the same tick; 4 ticks is 200 ms, which hides most round trips.
    public static final int INPUT_DELAY = 4;

    /// The server sends a [#HASH] this often, in ticks.
    public static final int HASH_EVERY = 40;

    private GameProtocol() {
    }
}
