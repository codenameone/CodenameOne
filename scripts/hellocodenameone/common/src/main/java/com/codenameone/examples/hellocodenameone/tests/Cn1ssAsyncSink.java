/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.examples.hellocodenameone.tests;

import java.util.ArrayList;
import java.util.List;

/// The connection state machine behind the JavaScript port's non-blocking
/// screenshot sink (`Cn1ssWebSocketSink.trySendAsync`).
///
/// It names no Codename One API -- the socket is behind `Transport` -- so a plain
/// JVM can drive it; `scripts/ci/tests/cn1ss-async-sink-test.sh` does. The
/// fidelity app carries a byte-identical copy apart from the package line, and
/// that script fails if the two drift.
///
/// Why it reconnects: the CI test server sheds a websocket that has been idle
/// for its idle allowance (`CN1_WS_IDLE_TIMEOUT_MS`, five minutes) with a bare
/// TCP close, which the browser reports as close code 1006. Most browser tests
/// ship through port.js's own socket, so the socket owned here can sit unused
/// for several minutes between two Java-side captures (LottieAnimated, then
/// VideoIODecodedFrames, many tests later). The original state machine latched
/// FAILED on any close, so the next capture logged `websocket-unavailable` and
/// its screenshot never reached the server. A socket that has worked once and
/// then drops now goes back to IDLE and is redialled by the next send, and a
/// send that was still waiting for its ACK when it dropped is sent again once.
/// The number of consecutive failed dials stays bounded, so a server that is
/// really gone still fails the run rather than looping.
public final class Cn1ssAsyncSink {
    /// The socket, as the state machine sees it.
    public interface Transport {
        /// True when this platform has websockets at all.
        boolean isSupported();

        /// Opens a new connection. Every callback that connection produces must
        /// be reported with the same `generation`, so a late close from a socket
        /// already replaced is ignored instead of tearing down its successor.
        void connect(int generation) throws Exception;

        /// Sends one screenshot on the current connection: the META text frame,
        /// then the PNG as a binary frame.
        void send(String meta, byte[] png) throws Exception;
    }

    static final int IDLE = 0;
    static final int CONNECTING = 1;
    static final int OPEN = 2;
    static final int FAILED = 3;

    /// Consecutive dials that may fail before the sink gives up for the run.
    /// Reset whenever a connection opens.
    static final int MAX_CONNECT_ATTEMPTS = 3;

    /// How many times one screenshot is sent again after its connection dropped
    /// before it was acknowledged.
    static final int MAX_RESENDS = 1;

    private final Transport transport;
    private int state = IDLE;
    private int generation;
    private int failedConnects;
    private final List<Entry> queued = new ArrayList<Entry>();
    private final List<Entry> inFlight = new ArrayList<Entry>();

    private static final class Entry {
        final String name;
        final byte[] png;
        final String hash;
        final Runnable onComplete;
        int resends;

        Entry(String name, byte[] png, String hash, Runnable onComplete) {
            this.name = name;
            this.png = png;
            this.hash = hash;
            this.onComplete = onComplete;
        }
    }

    public Cn1ssAsyncSink(Transport transport) {
        this.transport = transport;
    }

    /// Hands one screenshot to the sink. Returns true when the sink has taken
    /// ownership of `onComplete` -- it runs once the host acknowledges the PNG, or
    /// when the sink gives up on it -- and false when websockets are unavailable
    /// for the rest of the run, in which case the caller completes the test.
    public boolean send(String name, byte[] png, String hash, Runnable onComplete) {
        List<Runnable> done = new ArrayList<Runnable>();
        boolean owned;
        synchronized (this) {
            owned = sendLocked(new Entry(name, png, hash, onComplete), done);
        }
        runAll(done);
        return owned;
    }

    /// The connection of `gen` is open.
    public void onOpen(int gen) {
        List<Runnable> done = new ArrayList<Runnable>();
        synchronized (this) {
            if (gen != generation || state != CONNECTING) {
                return;
            }
            state = OPEN;
            failedConnects = 0;
            List<Entry> flush = new ArrayList<Entry>(queued);
            queued.clear();
            for (int i = 0; i < flush.size(); i++) {
                sendNowLocked(flush.get(i), done);
            }
        }
        runAll(done);
    }

    /// A text frame arrived. `ACK <name> ...` completes that screenshot.
    public void onText(String text) {
        if (text == null || !text.startsWith("ACK ")) {
            return;
        }
        String body = text.substring(4).trim();
        int sp = body.indexOf(' ');
        String name = sp > 0 ? body.substring(0, sp) : body;
        Runnable r = null;
        synchronized (this) {
            for (int i = 0; i < inFlight.size(); i++) {
                Entry e = inFlight.get(i);
                if (e.name.equals(name)) {
                    inFlight.remove(i);
                    r = e.onComplete;
                    break;
                }
            }
        }
        if (r != null) {
            r.run();
        }
    }

    /// The connection of `gen` closed or failed. A transport may report both an
    /// error and a close for one socket; the second is ignored because the first
    /// already moved the sink to a new generation or to IDLE/FAILED.
    public void onClosed(int gen, String reason) {
        List<Runnable> done = new ArrayList<Runnable>();
        synchronized (this) {
            closedLocked(gen, reason, done);
        }
        runAll(done);
    }

    synchronized int state() {
        return state;
    }

    private boolean sendLocked(Entry e, List<Runnable> done) {
        if (state == FAILED) {
            return false;
        }
        if (state == IDLE) {
            if (!transport.isSupported()) {
                state = FAILED;
                System.out.println("CN1SS:INFO:ws-sink-unavailable reason=not-supported");
                return false;
            }
            queued.add(e);
            connectLocked(done);
            // A dial that failed synchronously and exhausted its attempts has
            // already completed the entry; it is still owned, so the caller must
            // not complete it a second time.
            return true;
        }
        if (state == CONNECTING) {
            queued.add(e);
            return true;
        }
        sendNowLocked(e, done);
        return true;
    }

    private void connectLocked(List<Runnable> done) {
        state = CONNECTING;
        generation++;
        int gen = generation;
        try {
            transport.connect(gen);
        } catch (Throwable t) {
            closedLocked(gen, "connect-threw:" + t, done);
        }
    }

    private void sendNowLocked(Entry e, List<Runnable> done) {
        String meta = "META {\"test\":\"" + e.name + "\",\"png_bytes\":"
                + e.png.length + ",\"png_fnv1a64\":\"" + e.hash + "\"}";
        try {
            // Tracked before the send, so a close reported from inside send()
            // still finds it and sends it again.
            inFlight.add(e);
            transport.send(meta, e.png);
        } catch (Throwable t) {
            inFlight.remove(e);
            System.out.println("CN1SS:ERR:test=" + e.name + " message=ws-async-send-failed:" + t);
            if (e.onComplete != null) {
                done.add(e.onComplete); // never stall the sequential suite
            }
        }
    }

    private void closedLocked(int gen, String reason, List<Runnable> done) {
        if (gen != generation || state == FAILED || state == IDLE) {
            return;
        }
        boolean wasOpen = state == OPEN;
        if (!wasOpen) {
            failedConnects++;
        }
        // Whatever was sent on the dead socket and not acknowledged goes back to
        // the front of the queue, once; a screenshot that already had its resend
        // is given up so a socket that dies on every send cannot loop forever.
        List<Entry> retry = new ArrayList<Entry>();
        for (int i = 0; i < inFlight.size(); i++) {
            Entry e = inFlight.get(i);
            if (e.resends < MAX_RESENDS) {
                e.resends++;
                retry.add(e);
            } else {
                System.out.println("CN1SS:ERR:test=" + e.name + " message=ws-ack-lost");
                if (e.onComplete != null) {
                    done.add(e.onComplete);
                }
            }
        }
        inFlight.clear();
        queued.addAll(0, retry);
        System.out.println("CN1SS:INFO:ws-sink-dropped reason=" + reason
                + " wasOpen=" + wasOpen + " failedConnects=" + failedConnects
                + " requeued=" + retry.size());
        if (failedConnects >= MAX_CONNECT_ATTEMPTS) {
            state = FAILED;
            System.out.println("CN1SS:INFO:ws-sink-unavailable reason=" + reason);
            for (int i = 0; i < queued.size(); i++) {
                Runnable r = queued.get(i).onComplete;
                if (r != null) {
                    done.add(r);
                }
            }
            queued.clear();
            return;
        }
        if (queued.isEmpty()) {
            // Nothing waiting: the next send redials.
            state = IDLE;
            return;
        }
        connectLocked(done);
    }

    private static void runAll(List<Runnable> done) {
        for (int i = 0; i < done.size(); i++) {
            done.get(i).run();
        }
    }
}
