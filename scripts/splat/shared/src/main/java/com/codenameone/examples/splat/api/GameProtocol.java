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
package com.codenameone.examples.splat.api;

/// The game socket's message types, shared so both ends spell them the same.
///
/// Every message is a small JSON object with a `type`. The server is the
/// authority: clients send their own inputs and receive snapshots of the whole
/// match, which they correct their own prediction against.
///
/// Parties, client to server: [#HOST] (`level`: `arena:MODE`), [#JOIN] (`code`),
/// [#QUICK] (play whoever is waiting, bots fill the rest), [#GO] (the host
/// starts), [#CANCEL].
///
/// In a match, client to server: [#INPUT] (`s`: a sequence number that only
/// grows, `i`: [com.codenameone.examples.splat.rules.Input#encode]), [#RESUME],
/// [#LEAVE], [#SPECTATE] (admins, `match`), [#PING]. Anywhere: [#CHAT] (`text`),
/// [#EMOTE] (`id`), [#VOICE] (`to`, `data`: opaque WebRTC signaling the server
/// only relays), [#MUTE] (`on`).
///
/// Server to client: [#READY], [#PARTY] (`code`, `host`, `level`, `names`),
/// [#QUEUED], [#START] (`match`, `level`, `seed`, `you`, `slots`, `players`,
/// `rules`, `startsIn`, `snap`), [#SNAP] (`t`, `ack`, `s`: the snapshot ints,
/// `e`: events as `kind,tick,a,b,x,y,z;...`), [#CHAT], [#EMOTE], [#VOICE],
/// [#MUTE], [#END] (`places`, `scores`, `trophies`, `coins`, `match`), [#FLAG]
/// (admins), [#ERROR] (`message`), [#PONG], [#KICKED].
public final class GameProtocol {
    public static final String HOST = "host";
    public static final String JOIN = "join";
    public static final String QUICK = "quick";
    public static final String GO = "go";
    public static final String CANCEL = "cancel";
    public static final String INPUT = "input";
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
    public static final String SNAP = "snap";
    public static final String END = "end";
    public static final String FLAG = "flag";
    public static final String ERROR = "error";
    public static final String PONG = "pong";
    public static final String KICKED = "kicked";

    /// The server sends a [#SNAP] every this many ticks: 15 a second.
    public static final int SNAP_EVERY = 2;

    /// Milliseconds of countdown between [#START] and tick 0.
    public static final int COUNTDOWN_MILLIS = 3000;

    /// Most players in one match: six a side.
    public static final int MAX_PLAYERS = 12;

    /// Longest message a client may send. A WebRTC offer with its candidates is
    /// a few kilobytes; nothing else comes near.
    public static final int MAX_MESSAGE = 16 * 1024;

    private GameProtocol() {
    }
}
