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
package com.codenameone.examples.dash.api;

/// The game socket's message types, shared so both ends spell them the same.
///
/// Every message is a small JSON object with a `type`. The socket carries only
/// inputs one way and accepted inputs the other; every runner's run is computed
/// by the shared rules on every machine, in lockstep.
///
/// Parties, client to server: [#HOST] (`level`, an official id or a
/// `pack@hash#level` reference), [#JOIN] (`code`), [#QUICK] (race whoever is
/// waiting, bots fill the rest), [#GO] (the host starts), [#CANCEL].
///
/// In a race, client to server: [#INPUT] (`b`: [com.codenameone.examples.dash.rules.Input]
/// bits), [#SYNC] (`t`, `h`), [#RESUME], [#LEAVE], [#SPECTATE] (admins, `race`),
/// [#PING]. Anywhere: [#CHAT] (`text`), [#EMOTE] (`id`), [#VOICE] (`to`: a seat,
/// `data`: an opaque WebRTC offer, answer or candidate the server only relays),
/// [#MUTE] (`on`).
///
/// Server to client: [#READY], [#PARTY] (`code`, `host`, `level`, `names`,
/// `you`), [#QUEUED], [#START] (`race`, `level`, `seed`, `you`, `names`,
/// `rules`, `startsIn`, `tick`, `log`), [#TICKS] (`t`: the last tick the client
/// may simulate, `i`: inputs executed since the previous message, as
/// `tick,seat,bits;...`), [#HASH] (`t`, `h`), [#CHAT] (`from`, `text`),
/// [#EMOTE] (`from`, `id`), [#VOICE] (`from`, `data`), [#MUTE] (`from`, `on`),
/// [#END] (`places`, `scores`, `trophies`, `coins`, `race`), [#FLAG] (admins),
/// [#ERROR] (`message`), [#PONG], [#KICKED].
public final class GameProtocol {
    public static final String HOST = "host";
    public static final String JOIN = "join";
    public static final String QUICK = "quick";
    public static final String GO = "go";
    public static final String CANCEL = "cancel";
    public static final String INPUT = "input";
    public static final String SYNC = "sync";
    public static final String RESUME = "resume";
    public static final String LEAVE = "leave";
    public static final String SPECTATE = "spectate";
    public static final String CHAT = "chat";
    public static final String EMOTE = "emote";
    public static final String VOICE = "voice";
    public static final String MUTE = "mute";
    public static final String PING = "ping";

    public static final String READY = "ready";
    public static final String PARTY = "party";
    public static final String QUEUED = "queued";
    public static final String START = "start";
    public static final String TICKS = "ticks";
    public static final String HASH = "hash";
    public static final String END = "end";
    public static final String FLAG = "flag";
    public static final String ERROR = "error";
    public static final String PONG = "pong";
    public static final String KICKED = "kicked";

    /// Ticks between the server receiving an input and executing it. Every
    /// machine applies it on the same tick; 6 ticks is 100 ms, which hides most
    /// round trips. The client shows its own runner reacting at once and
    /// reconciles when the input lands.
    public static final int INPUT_DELAY = 6;

    /// The server sends a [#HASH] this often, in ticks.
    public static final int HASH_EVERY = 60;

    /// Milliseconds of countdown between [#START] and tick 0.
    public static final int COUNTDOWN_MILLIS = 3000;

    /// Most runners in one race.
    public static final int MAX_RUNNERS = 6;

    /// Longest message a client may send. A WebRTC offer with its candidates is
    /// a few kilobytes; nothing else comes near.
    public static final int MAX_MESSAGE = 16 * 1024;

    private GameProtocol() {
    }
}
