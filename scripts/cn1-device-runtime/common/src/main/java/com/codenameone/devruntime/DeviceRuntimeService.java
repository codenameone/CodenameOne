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
package com.codenameone.devruntime;

import com.codename1.impl.interp.InterpBundle;
import com.codename1.impl.interp.InterpBundleReader;
import com.codename1.impl.interp.InterpPairingSecret;
import com.codename1.impl.interp.InterpPlatform;
import com.codename1.impl.interp.InterpRuntime;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.impl.interp.InterpThrowable;
import com.codename1.io.Preferences;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Listens for a pushed program and runs it.
 *
 * <p>One implementation for both platforms. The transport is Codename One's own
 * {@link Socket}, not {@code java.net.ServerSocket}, because ParparVM has no
 * {@code java.net} server socket at all -- iOS server sockets exist only behind
 * the port's {@code listenSocketLoopback}. Going through the framework's API
 * means the same code binds a listener on Android and on iOS.</p>
 *
 * <p>Loopback only, deliberately. Reaching it from a developer's machine goes
 * through {@code adb forward} on Android or the simulator's shared loopback on
 * iOS, both of which require possession of the device. That is the pairing
 * story for the development build; a store build gets the code-and-approve
 * handshake instead, and this listener is not what it will use.</p>
 *
 * @author Shai Almog
 */
public class DeviceRuntimeService {
    /** Wire magic: the ASCII bytes "CN1P". */
    static final int MAGIC = 0x434E3150;

    /** Unauthenticated push. Only meaningful over a loopback-bound listener. */
    static final int PROTOCOL_V1 = 1;

    /**
     * Challenge-response push, subject to pairing and per-connection approval.
     *
     * <p>There was a v2 in which the peer id alone authorised a push. It was a
     * bearer token in plaintext on a LAN -- capture one frame, push forever --
     * and it is gone rather than deprecated. Nothing has shipped that speaks
     * it, and leaving it in would have made the fix optional for an attacker.</p>
     */
    static final int PROTOCOL_V3 = 3;

    /**
     * "Are you a device runtime?" -- answered with this device's id, and
     * nothing else happens on the connection.
     *
     * <p>The desktop finds a device by connecting to every address on the
     * subnet, and a bare successful connect proves only that something on that
     * address accepts TCP on this port. Without a frame to ask, the first
     * unrelated service to answer won the race and the push then failed against
     * it while the real device sat unqueried.</p>
     */
    static final int FRAME_PING = 0;

    static final int FRAME_PAIR = 1;
    static final int FRAME_PUSH = 2;

    /** A bundle larger than this is a framing error, not a program. */
    private static final int MAX_BUNDLE = 64 * 1024 * 1024;

    /// Aggregate pre-authentication push memory cap, across all concurrent
    /// connections. A LAN peer that knows a paired peer id (transmitted in
    /// cleartext on ordinary pushes) could otherwise open many FRAME_PUSH
    /// connections, advertise the {@code MAX_BUNDLE} on each, and force the
    /// device to allocate 64 MiB per connection before proving possession of
    /// the pairing secret -- enough concurrent connections wedge the device
    /// heap. This budget caps the total unauthenticated allocation; a push
    /// that would exceed it is refused with a message the desktop can show
    /// rather than one that reads like the device is broken. Two full-sized
    /// bundles simultaneously covers the ordinary "second push before the
    /// first authenticated" race, and no more.
    private static final long PRE_AUTH_MEMORY_CAP = 2L * MAX_BUNDLE;

    /// The number of concurrent pre-authentication reservations. The memory
    /// cap alone still admits thousands of tiny-body pushes (128 MiB / 64 KiB
    /// = 2048 reservations), each holding a socket and its framework
    /// connection thread -- enough to exhaust the device's file descriptors
    /// or thread limit without ever having proved possession of the secret.
    /// Capping the count as well as the byte budget bounds those directly.
    /// Four leaves room for the "second push during approval" race the
    /// memory cap is sized for, plus two more.
    private static final int PRE_AUTH_MAX_CONCURRENT = 4;

    /// Monitor + counter for {@link #PRE_AUTH_MEMORY_CAP} and
    /// {@link #PRE_AUTH_MAX_CONCURRENT}. Held for a moment on reservation
    /// and release; the actual body read happens without the lock so slow
    /// senders do not block a legitimate second connection.
    private static final Object PRE_AUTH_LOCK = new Object();
    private static long preAuthAllocated;
    private static int preAuthConnections;

    private static final DeviceRuntimeService INSTANCE = new DeviceRuntimeService();

    private Thread dialer;
    private volatile InterpRuntime runtime;
    private volatile String status = "idle";
    private volatile String loadedSource = "";

    /// The entry class of whatever is running, for the screen to name.
    private volatile String loadedName = "";

    /// Mocked subsystems this program has used, for the screen to admit to.
    private volatile String mocksUsed = "";

    private DeviceRuntimeService() {
    }

    public static DeviceRuntimeService getInstance() {
        return INSTANCE;
    }

    /**
     * Whether this build can run pushed code at all.
     *
     * <p>False on an iOS build made without {@code ios.interpHost=true}: without
     * the invoke thunks and the symbol table there is nothing for interpreted
     * code to call, and saying so up front beats failing at the first
     * {@code new Form()}.</p>
     */
    public boolean isSupported() {
        return InterpPlatform.isAvailable() && Socket.isSupported();
    }

    /**
     * Starts dialling the desktop. Idempotent.
     *
     * <p>The device connects out; it does not listen. That is not a stylistic
     * choice -- a listening socket inside the iOS simulator is unreachable from
     * the host. The app binds it and reports success, the desktop gets
     * connection refused, and the two facts never meet. Outbound works on both
     * platforms: the simulator shares the host's loopback for connections it
     * makes, and on Android {@code adb reverse} maps the device's loopback onto
     * the host's.</p>
     *
     * <p>It also happens to be the shape a store build needs, where a phone on
     * a real network cannot accept inbound connections at all.</p>
     */
    public boolean startDialer(final int port) {
        return startDialer(getHost(), port);
    }

    /// Where the device dials, remembered across launches.
    ///
    /// Loopback is the default and is what a USB session uses: `adb reverse` on
    /// Android, the simulator's shared loopback on iOS. A phone on Wi-Fi has to
    /// be told the desktop's address instead, because loopback on a phone is
    /// the phone.
    public static String getHost() {
        return Preferences.get(PREF_HOST, "127.0.0.1");
    }

    /// Sets the desktop address and restarts the dialer against it.
    public static void setHost(String host) {
        Preferences.set(PREF_HOST, host == null || host.trim().length() == 0
                ? "127.0.0.1" : host.trim());
    }

    private static final String PREF_HOST = "cn1.devruntime.host";

    /// Whether an address is this device rather than something on the network.
    public static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equals(host) || "::1".equals(host);
    }

    /**
     * Also accept connections, so the desktop can find this device.
     *
     * <p>Having the phone hunt for the computer is the weaker half of the deal:
     * it has no thread pool worth the name, and a sweep of 254 addresses from a
     * phone is slow enough to look broken. A desktop scanning its own subnet
     * does the same job in about a second. So the device listens as well as
     * dials, and whichever side finds the other first wins.</p>
     *
     * <p>Android only -- iOS has no server socket, which is why dialling
     * exists at all and why it stays.</p>
     */
    private void startListener(int port) {
        try {
            if (!Socket.isServerSocketSupported()) {
                return;
            }
            Socket.listen(port, DeviceRuntimeConnection.class);
            listening = true;
        } catch (Throwable t) {
            // Another app may hold the port; dialling still works.
            System.out.println("CN1SS:DEVRUNTIME listener unavailable: " + t);
        }
    }

    private volatile boolean listening;

    /// Whether this device can be found by a computer scanning the network.
    public boolean isListening() {
        return listening;
    }

    /// This device's address on the network, or null. Accepts both IPv4
    /// (contains `.`) and IPv6 (contains `:`) forms -- an IPv6-only Wi-Fi
    /// network is otherwise reachable and the UI needs to show the address
    /// so the user can type it on the desktop.
    public static String getLocalAddress() {
        try {
            String ip = Socket.getHostOrIP();
            if (ip == null || isLoopback(ip)) {
                return null;
            }
            if (ip.indexOf('.') > 0 || ip.indexOf(':') >= 0) {
                return ip;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean startDialer(final String host, final int port) {
        if (dialer != null) {
            return true;
        }
        if (!isSupported()) {
            status = InterpPlatform.isAvailable()
                    ? "sockets unavailable on this platform"
                    : "this build has no interpreter bindings; rebuild with interpHost=true";
            return false;
        }
        // Not a daemon thread: Thread.setDaemon is outside the Codename One
        // API subset. The loop instead exits when the app does, which is the
        // same outcome on both platforms.
        // Pairing is not optional off loopback, and this is enforced rather
        // than documented. On loopback the authentication is physical: the
        // connection can only come from a USB-authorised host or the
        // simulator. On a network any machine can answer, and the bundle
        // carries the program's full source, so an unpaired push would hand
        // that to whoever replied first.

        startListener(port);
        Thread t = new Thread(new Runnable() {
            public void run() {
                dialLoop(host, port);
            }
        }, "cn1-device-runtime");
        dialer = t;
        t.start();
        status = "dialling " + host + ":" + port
                + (isLoopback(host) ? "" : " (pairing required)");
        return true;
    }

    /**
     * Retries forever, so the IDE can be started before or after the app.
     *
     * <p>Three places are tried, in the order that costs least: the computer
     * this device last spoke to, loopback (which is a USB session, where
     * {@code adb reverse} maps the desktop onto the device's own address), and
     * failing both, every address on the local network.</p>
     *
     * <p>A refused connection is the normal state -- nobody is pushing most of
     * the time -- so it is not worth logging, only worth waiting between.</p>
     */
    private void dialLoop(String startingHost, int port) {
        while (true) {
            // Re-read every pass rather than trusting what was configured at
            // startup: "Look for my computer" clears the remembered address,
            // and a device carried to another network has to notice.
            String host = getHost();
            boolean served = dial(host, port);
            if (!served && !isLoopback(host)) {
                served = dial("127.0.0.1", port);
            }
            if (!served) {
                served = sweep(port);
            }
            try {
                Thread.sleep(served ? 250 : 2000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /// Dials one address and serves whatever it finds. Returns whether anybody
    /// was there.
    private boolean dial(final String host, final int port) {
        // Three separate facts, and conflating any two of them is a bug this
        // has already had. "Somebody answered" is what stops the timeout;
        // "the exchange finished" is what ends the wait; and "the peer spoke
        // our protocol" is the answer -- something else listening on 18234
        // would otherwise hold the dial loop and stop the sweep from ever
        // looking for the real desktop.
        //
        // Volatile holders rather than one-element arrays: the socket callback
        // runs on a thread of its own and this one only polls, and an array
        // element carries no visibility guarantee at all -- the poll is
        // entitled to never see the write, time out and close an exchange that
        // was working.
        final Progress progress = new Progress();
        final Flag finished = new Flag();
        final Flag spoke = new Flag();
        // The stream, so a connection that accepts and then says nothing can
        // be closed rather than left behind. Socket.connect gives the callback
        // its own thread and closes nothing when the caller gives up, so
        // without this every dial past a silent listener leaks a thread parked
        // in readInt and the socket under it, for the life of the app.
        final StreamHolder open = new StreamHolder();
        SocketConnection sc = new SocketConnection() {
            public void connectionEstablished(InputStream is, OutputStream os) {
                open.set(is);
                try {
                    if (handle(is, os, isLoopback(host), progress)) {
                        spoke.set();
                    }
                } finally {
                    open.set(null);
                    // Last, so a poll that sees "finished" also sees the rest.
                    finished.set();
                }
            }

            public void connectionError(int errorCode, String message) {
                // Nobody listening: the ordinary case between pushes.
                finished.set();
            }
        };
        sc.setConnectTimeout(CONNECT_TIMEOUT_MS);
        Socket.connect(host, port, sc);
        // Socket.connect runs on its own thread, so wait for the attempt rather
        // than racing past it and calling everything unreachable. Once the
        // exchange has begun -- the magic read, not merely a connection
        // accepted -- wait for it however long it takes: a pairing code is
        // typed by a human, a bundle takes as long as it takes, and a program's
        // entry point runs before the handler returns. Timing out there would
        // start the loopback fallback and the subnet sweep against a live
        // connection, and let the sweep's status overwrite "running".
        //
        // The distinction matters on Android: `adb reverse` accepts a
        // connection whether or not the push tool is listening behind it, and
        // then says nothing. Waiting on *that* forever wedges the dial loop and
        // the device never calls again.
        long deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS + 500;
        while (!finished.isSet()
                && (progress.deservesWaiting() || System.currentTimeMillis() < deadline)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return spoke.isSet();
            }
        }
        if (!finished.isSet()) {
            // Gave up on a peer that never said anything. Closing the stream is
            // what unblocks the read the handler is parked in, so its thread
            // ends rather than accumulating one per dial.
            closeQuietly(open.get());
        }
        return spoke.isSet();
    }

    /**
     * How far an exchange has got, for the thread deciding whether to wait.
     *
     * <p>Three states, because they license different waits. Nothing yet: the
     * connect timeout applies, and an address that accepts and says nothing is
     * closed. Identified: the peer spoke the protocol and named itself, which
     * is worth a bounded grace -- a round trip and an HMAC, not a human and
     * not a transfer. Open-ended: the exchange reached a phase that genuinely
     * takes as long as it takes, a person typing a pairing code or a bundle
     * crossing, and only then is waiting forever right.</p>
     *
     * <p>The distinction is what stops an unauthenticated peer from wedging
     * discovery: sending a header is cheap, so it may not buy an unbounded
     * wait.</p>
     */
    private static final class Progress {
        private volatile long identifiedAt;
        private volatile boolean openEnded;

        void identify() {
            if (identifiedAt == 0) {
                identifiedAt = System.currentTimeMillis();
            }
        }

        void allowLongWait() {
            identify();
            openEnded = true;
        }

        /// Restarts the bounded grace, without granting an unbounded wait.
        ///
        /// What a transfer needs: bytes arriving is progress, and a stalled
        /// transfer should still be closed. Called as each chunk lands, so a
        /// slow link is fine and a peer that stops sending is not.
        void touch() {
            identifiedAt = System.currentTimeMillis();
        }

        /// Ends the open-ended phase and restarts the bounded grace.
        ///
        /// The human part of pairing is over the moment the dialog closes, and
        /// what follows -- the response, the verdict -- is a round trip like
        /// any other. Leaving the exchange open-ended let a peer that never
        /// answered hold the waiters forever.
        void endLongWait() {
            openEnded = false;
            identifiedAt = System.currentTimeMillis();
        }

        boolean isIdentified() {
            return identifiedAt != 0;
        }

        boolean isOpenEnded() {
            return openEnded;
        }

        /// Whether this exchange still deserves to be waited on.
        boolean deservesWaiting() {
            return openEnded
                    || (identifiedAt != 0
                        && System.currentTimeMillis() - identifiedAt < PREAUTH_TIMEOUT_MS);
        }
    }

    /// How long an identified but unauthenticated exchange may take.
    ///
    /// One round trip and one deliberately slow HMAC. Everything past that
    /// point declares itself open-ended, so this only bounds the phase anything
    /// on the network can reach.
    private static final int PREAUTH_TIMEOUT_MS = 10000;

    /// A flag two threads share: set on the socket callback's thread, polled on
    /// the dial or sweep thread.
    ///
    /// Volatile because that is the whole point -- an ordinary field (or an
    /// array element, which is what this replaced) gives the polling thread no
    /// guarantee it will ever observe the write.
    private static final class Flag {
        private volatile boolean value;

        boolean isSet() {
            return value;
        }

        void set() {
            value = true;
        }
    }

    /// The stream a connection callback published, for the thread that may have
    /// to close it. Volatile for the same reason [Flag] is.
    private static final class StreamHolder {
        private volatile InputStream stream;

        InputStream get() {
            return stream;
        }

        void set(InputStream stream) {
            this.stream = stream;
        }
    }

    /// The address a sweep batch found, published across threads.
    private static final class AddressHolder {
        private volatile String address;

        String get() {
            return address;
        }

        void set(String address) {
            this.address = address;
        }
    }

    /// One sweep connection, and whether the peer behind it has answered.
    private static final class SweepConnection {
        private final InputStream is;

        /// How far this exchange has got; shared with handle.
        private final Progress progress = new Progress();

        SweepConnection(InputStream is) {
            this.is = is;
        }
    }

    /// Whether any connection in this batch is in the middle of an exchange.
    private static boolean anyStarted(Vector connections) {
        synchronized (connections) {
            for (int i = 0; i < connections.size(); i++) {
                if (((SweepConnection) connections.elementAt(i)).progress.deservesWaiting()) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Closes the connections a sweep batch left parked on a silent address,
    /// unblocking their readers, and leaves an exchange in progress alone.
    private static void closeSilent(Vector connections) {
        synchronized (connections) {
            for (int i = connections.size() - 1; i >= 0; i--) {
                SweepConnection conn = (SweepConnection) connections.elementAt(i);
                if (conn.progress.deservesWaiting()) {
                    continue;
                }
                closeQuietly(conn.is);
                connections.removeElementAt(i);
            }
        }
    }

    private static void closeQuietly(OutputStream os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (Throwable alreadyGone) {
            // Same as the InputStream overload: this is a refusal path, not
            // an error report.
        }
    }

    private static void closeQuietly(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Throwable alreadyGone) {
            // Closing to unblock a reader; whether it was already shut is not
            // something this can act on.
        }
    }

    /// How long to wait for one address. Short: most of the subnet is nothing.
    private static final int CONNECT_TIMEOUT_MS = 1200;

    /// How many addresses to try at once.
    private static final int SWEEP_BATCH = 24;

    /**
     * Looks for the desktop on the local network.
     *
     * <p>There is no UDP in the Codename One API, so there is no broadcast to
     * announce with; what there is, is this device's own address. Every address
     * on its /24 gets a TCP connection attempt, and the tool answers with a
     * frame that identifies itself -- so the sweep and the push are the same
     * connection, and finding the computer costs nothing beyond the attempt.</p>
     *
     * <p>The address that answers is remembered, so this happens once rather
     * than every couple of seconds.</p>
     */
    private boolean sweep(final int port) {
        String self = null;
        try {
            self = Socket.getHostOrIP();
        } catch (Throwable t) {
            // Some platforms decline; there is nothing to sweep without it.
        }
        if (self == null || isLoopback(self)) {
            return false;
        }
        if (self.indexOf('.') < 0) {
            // IPv6-only network: the address space is astronomical, so a
            // sweep of every host on the local subnet is not a search. The
            // manual-address flow still works (the UI shows this device's
            // address via getLocalAddress; the user types it into the
            // desktop) and dial() reaches an IPv6 desktop the same way it
            // reaches an IPv4 one.
            status = "IPv6-only network: enter this device's address on the desktop";
            return false;
        }
        String prefix = self.substring(0, self.lastIndexOf('.') + 1);
        String selfSuffix = self.substring(self.lastIndexOf('.') + 1);
        status = "looking for a computer on " + prefix + "*";

        // Connections the sweep has open, so an address that accepts and then
        // says nothing can be closed rather than left with a thread parked in
        // readInt. A sweep is 254 addresses; leaking one thread each would end
        // the app. Each carries whether it has spoken the magic, because only
        // the silent ones may be closed: a peer that answered is pairing (a
        // human is typing a code) or transferring a bundle, and neither
        // finishes inside a batch deadline.
        final Vector openStreams = new Vector();
        for (int base = 1; base <= 254; base += SWEEP_BATCH) {
            final Flag found = new Flag();
            final AddressHolder foundAt = new AddressHolder();
            int last = Math.min(base + SWEEP_BATCH - 1, 254);
            for (int i = base; i <= last; i++) {
                final String candidate = prefix + i;
                if (candidate.equals(self) || String.valueOf(i).equals(selfSuffix)) {
                    continue;
                }
                SocketConnection sc = new SocketConnection() {
                    public void connectionEstablished(InputStream is, OutputStream os) {
                        // Something is listening, which is not the same as it
                        // being the push tool. Remember the address only if the
                        // exchange actually spoke our protocol -- otherwise the
                        // first unrelated service on the subnet becomes "the
                        // desktop" and every later dial goes to it while the
                        // real one is never contacted.
                        //
                        // Held so the sweep can close it: an address that
                        // accepts and then says nothing parks this thread in
                        // readInt forever, and a sweep is 254 of them. The
                        // holder's flag goes true the moment this one answers,
                        // which is what takes it out of the batch's reach.
                        SweepConnection conn = new SweepConnection(is);
                        synchronized (openStreams) {
                            openStreams.addElement(conn);
                        }
                        boolean spoke;
                        try {
                            spoke = handle(is, os, false, conn.progress);
                        } finally {
                            synchronized (openStreams) {
                                openStreams.removeElement(conn);
                            }
                        }
                        if (spoke) {
                            synchronized (openStreams) {
                                // The address before the flag, so the sweeping
                                // thread that sees "found" also sees which
                                // address it was. Two candidates can answer at
                                // once; the first one to arrive is kept.
                                if (!found.isSet()) {
                                    foundAt.set(candidate);
                                    found.set();
                                }
                            }
                        }
                    }

                    public void connectionError(int errorCode, String message) {
                        // The overwhelmingly common answer while sweeping.
                    }
                };
                sc.setConnectTimeout(CONNECT_TIMEOUT_MS);
                Socket.connect(candidate, port, sc);
            }
            // Same rule as the dial: wait out the connect attempts, but once an
            // address has spoken the magic wait for that exchange however long
            // it takes. Ending the batch on the clock closed the one connection
            // that was working -- pairing waits on a human, and a bundle takes
            // as long as it takes -- so discovery could never complete on iOS,
            // where the sweep is the only way in.
            long deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS + 800;
            while (!found.isSet()
                    && (anyStarted(openStreams) || System.currentTimeMillis() < deadline)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    closeSilent(openStreams);
                    return false;
                }
            }
            // Whatever this batch left parked on a silent address.
            closeSilent(openStreams);
            if (found.isSet()) {
                setHost(foundAt.get());
                status = "found " + foundAt.get() + ":" + port;
                return true;
            }
        }
        status = "no computer found on " + prefix + "*";
        return false;
    }

    /// Whether a pairing prompt is on screen right now.
    private final boolean[] pairingPromptOpen = new boolean[1];

    /// When the next pairing prompt may be raised, as a wall clock.
    private long pairingPromptNotBefore;

    /// How long the device ignores pairing frames after one has been answered.
    ///
    /// Long enough that repeating the frame cannot fill the screen with
    /// dialogs, short enough that a person retyping a code they mistyped is not
    /// kept waiting: pairing is a deliberate act taking several seconds anyway.
    private static final long PAIRING_PROMPT_COOLDOWN_MS = 3000;

    /**
     * Takes the right to raise a pairing prompt, or refuses.
     *
     * <p>Unauthenticated by definition -- pairing is what establishes the
     * secret -- so this frame is the one thing anything on the network can make
     * the device do. Serializing it and pausing between prompts is what keeps
     * that from becoming a way to make the app unusable.</p>
     */
    private boolean claimPairingPrompt() {
        synchronized (pairingPromptOpen) {
            if (pairingPromptOpen[0] || System.currentTimeMillis() < pairingPromptNotBefore) {
                return false;
            }
            pairingPromptOpen[0] = true;
            return true;
        }
    }

    /// Gives the prompt slot back. Called when the dialog closes and again
    /// from the frame's finally, so it has to be safe to call twice -- the
    /// second call only pushes the cooldown out by a few milliseconds.
    private void releasePairingPrompt() {
        synchronized (pairingPromptOpen) {
            pairingPromptOpen[0] = false;
            pairingPromptNotBefore = System.currentTimeMillis() + PAIRING_PROMPT_COOLDOWN_MS;
        }
    }

    /**
     * Asks the person to approve an authenticated push, without a clock on it.
     *
     * <p>Everything up to here was bounded because anything on the network can
     * reach it. This is the other side of that line: the peer proved it holds
     * the secret and the bundle matched, so what remains is a human deciding --
     * and closing the connection under them would run the program while the
     * desktop was told the push failed.</p>
     */
    private boolean approveWhileWaiting(String peerId, Progress progress) {
        progress.allowLongWait();
        return DeviceRuntimePairing.approve(peerId);
    }

    /**
     * Serves a connection this device accepted, under a deadline.
     *
     * <p>An accepted connection has no poller behind it -- the dial and the
     * sweep watch their own -- and the framework gives every accepted
     * connection a thread. A peer that connects and sends nothing would park
     * one of those forever, and enough of them would take the app's threads and
     * sockets with no authentication anywhere in sight. A watchdog closes the
     * silent ones, which is what unblocks the read they are parked in.</p>
     */
    void handleAccepted(InputStream is, OutputStream os) {
        Watched w = new Watched(is);
        synchronized (accepted) {
            if (accepted.size() >= MAX_ACCEPTED) {
                // Refuse before spending a framework connection thread on
                // reading the header. `reservePreAuth` alone was not enough:
                // it only kicks in once the peer id and challenge response
                // have been read, so a burst of drip-fed or silent sockets
                // could still exhaust file descriptors and threads faster
                // than the five-second watchdog closes them.
                closeQuietly(is);
                closeQuietly(os);
                return;
            }
            accepted.addElement(w);
            if (!watchdogRunning) {
                watchdogRunning = true;
                new Thread(new Runnable() {
                    public void run() {
                        watchAccepted();
                    }
                }, "cn1-devruntime-accept-watchdog").start();
            }
        }
        try {
            handle(is, os, false, w.progress);
        } finally {
            synchronized (accepted) {
                accepted.removeElement(w);
            }
        }
    }

    /// One accepted connection and how far it has got.
    private static final class Watched {
        private final InputStream stream;
        private final Progress progress = new Progress();
        private final long acceptedAt = System.currentTimeMillis();

        Watched(InputStream stream) {
            this.stream = stream;
        }

        /// Whether this connection has outstayed what it has earned.
        boolean expired() {
            if (progress.isOpenEnded()) {
                return false;
            }
            if (progress.isIdentified()) {
                return !progress.deservesWaiting();
            }
            return System.currentTimeMillis() - acceptedAt > SILENT_ACCEPT_TIMEOUT_MS;
        }
    }

    /// How long an accepted connection may stay silent before it is closed.
    ///
    /// Four bytes of magic is not much to ask for, and a peer that cannot send
    /// them is not a push tool -- it is a port scanner, or a crash.
    private static final int SILENT_ACCEPT_TIMEOUT_MS = 5000;

    /// The absolute number of accepted connections in flight, per device.
    /// The pre-auth reservation cap protects the byte budget and only trips
    /// after the header, challenge and length have been read; a burst of
    /// silent or drip-fed sockets never reaches it. Refusing at accept keeps
    /// the framework's connection threads and file descriptors bounded even
    /// when nobody sends anything. Sized to comfortably cover a legitimate
    /// desktop's few concurrent PING + PUSH probes while cutting off a
    /// flood.
    private static final int MAX_ACCEPTED = 16;

    private final Vector accepted = new Vector();

    private boolean watchdogRunning;

    /// Closes accepted connections that stopped making progress, and stops when
    /// there are none left to watch.
    private void watchAccepted() {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
            synchronized (accepted) {
                for (int i = accepted.size() - 1; i >= 0; i--) {
                    Watched w = (Watched) accepted.elementAt(i);
                    if (w.expired()) {
                        // Closing is what ends the read its thread is parked in;
                        // the handler's own finally then unregisters it.
                        closeQuietly(w.stream);
                    }
                }
                if (accepted.isEmpty()) {
                    watchdogRunning = false;
                    return;
                }
            }
        }
    }

    /**
     * Reads a bundle body, treating arrival as progress.
     *
     * <p>Not {@code readFully}: this is the one long read that happens before
     * anything has authenticated -- the response covers the bundle, so it
     * cannot be checked until the bundle is here -- and a peer that declares a
     * plausible length and then stops sending would otherwise hold the dial and
     * the sweep for good. Each chunk restarts the grace, so a slow link
     * finishes and a stalled one is closed.</p>
     */
    private static void readBody(DataInputStream in, byte[] body, Progress progress)
            throws IOException {
        int off = 0;
        while (off < body.length) {
            int n = in.read(body, off, Math.min(BODY_CHUNK, body.length - off));
            if (n < 0) {
                throw new java.io.EOFException("the bundle ended after " + off + " of "
                        + body.length + " bytes");
            }
            off += n;
            progress.touch();
        }
    }

    /// How much of a bundle is read between heartbeats.
    private static final int BODY_CHUNK = 64 * 1024;

    /// Reserves {@code length} bytes against the aggregate pre-authentication
    /// memory budget. Returns false when the reservation would push the
    /// running total past {@link #PRE_AUTH_MEMORY_CAP} or when the
    /// concurrent-reservation count is already at
    /// {@link #PRE_AUTH_MAX_CONCURRENT}; the caller then rejects rather
    /// than allocating and holding a heap slot behind an unauthenticated
    /// peer. The count cap matters even at trivial byte sizes because
    /// each reservation is backed by a socket and a framework connection
    /// thread, both of which are exhaustible.
    private static boolean reservePreAuth(int length) {
        synchronized (PRE_AUTH_LOCK) {
            if (preAuthAllocated + length > PRE_AUTH_MEMORY_CAP
                    || preAuthConnections >= PRE_AUTH_MAX_CONCURRENT) {
                return false;
            }
            preAuthAllocated += length;
            preAuthConnections++;
            return true;
        }
    }

    /// Releases a prior {@link #reservePreAuth} reservation. Called from the
    /// caller's finally, and also as soon as authentication succeeds so a
    /// legitimate second push during the approval dialog is not queued
    /// behind the first.
    private static void releasePreAuth(int length) {
        synchronized (PRE_AUTH_LOCK) {
            preAuthAllocated -= length;
            preAuthConnections--;
            if (preAuthAllocated < 0) {
                // A double release is a caller bug, but do not let the
                // counter drift negative -- a later legitimate push would
                // then reserve more than the cap permits.
                preAuthAllocated = 0;
            }
            if (preAuthConnections < 0) {
                preAuthConnections = 0;
            }
        }
    }

    /**
     * Runs the pairing handshake on an open connection.
     *
     * <p>Two round trips, because the device cannot issue a challenge until a
     * human has typed the code the challenge will be answered with. The secret
     * derived here is never sent -- both ends compute it from the code, the peer
     * id and the device id -- so what an eavesdropper sees is a nonce and an
     * HMAC over it.</p>
     */
    private void handlePairing(String peerId, String peerName, Progress progress,
                               DataInputStream in, DataOutputStream out) throws IOException {
        String code = DeviceRuntimePairing.promptForCode(peerId, peerName);
        // The dialog is closed: the prompt slot goes back so the next computer
        // can pair, and the connection goes back to a bounded wait -- what
        // remains is a round trip, and a peer that stops answering here must
        // not hold the waiters for good.
        progress.endLongWait();
        releasePairingPrompt();
        if (code == null) {
            out.writeByte(0);
            out.writeUTF(DeviceRuntimePairing.lastFailure());
            out.flush();
            return;
        }
        String deviceId = DeviceRuntimePairing.deviceId();
        String challenge = InterpPairingSecret.challenge();
        out.writeByte(1);
        out.writeUTF(deviceId);
        out.writeUTF(challenge);
        out.flush();

        // Deliberately slow, and deliberately off the event thread: the whole
        // point of the iteration count is that grinding the six-digit code costs
        // an attacker real time.
        byte[] secret = InterpPairingSecret.derive(code, peerId, deviceId);
        String response = in.readUTF();
        if (!InterpPairingSecret.matches(response,
                InterpPairingSecret.respond(secret, challenge))) {
            DeviceRuntimePairing.reportCodeMismatch();
            out.writeByte(0);
            out.writeUTF(DeviceRuntimePairing.lastFailure());
            out.flush();
            return;
        }
        DeviceRuntimePairing.completePairing(peerId, peerName, secret);
        status = "paired with " + peerName;
        out.writeByte(1);
        out.writeUTF("paired with this device as \"" + peerName + "\"");
        out.flush();
    }

    /**
     * Reads one frame and replies.
     *
     * <p>Wire format: magic, protocol version, then a version-specific body.</p>
     *
     * <p>v1 is {@code length, bundle} and is accepted only over loopback.</p>
     *
     * <p>v3 opens with a frame type. {@code FRAME_PAIR} sends
     * {@code peerId, peerName}; the device prompts for the code, replies
     * {@code 1, deviceId, challenge}, reads the computer's response and replies
     * again with the verdict. {@code FRAME_PUSH} sends
     * {@code peerId, desktopChallenge}; the device replies
     * {@code 1, deviceId, challenge, answerToDesktopChallenge}, then reads
     * {@code response, length, bundle} and checks the response against the
     * challenge <em>and the bundle</em> before anything is run.</p>
     *
     * <p>Both ends prove possession of the secret, and the device proves it
     * first: a device id is public, so an unauthenticated peer that answered
     * the desktop's dial would otherwise be handed the bundle, which carries
     * the program's whole source.</p>
     *
     * <p>The reply is a status byte and a UTF message either way, so the desktop
     * learns whether the program actually started rather than only that the
     * bytes arrived.</p>
     */
    void handle(InputStream is, OutputStream os) {
        handle(is, os, true);
    }

    /// Whether the peer on this connection spoke the push protocol at all.
    ///
    /// Distinct from whether the push succeeded: a refused push is still a
    /// desktop push tool at the other end, and a subnet sweep wants to know
    /// which address that is.

    /**
     * Serves one connection.
     *
     * <p>Whether pairing is required is a property of <em>this connection</em>,
     * not of the app: a push arriving over loopback can only have come from a
     * USB-authorised host or a simulator on the same machine, and possession is
     * the authentication there. Treating it as a mode instead meant a device
     * that had once seen a network address refused USB pushes for the rest of
     * its life.</p>
     */
    boolean handle(InputStream is, OutputStream os, boolean loopback) {
        return handle(is, os, loopback, new Progress());
    }

    /**
     * Serves one connection, reporting when the exchange actually began.
     *
     * <p>{@code started} is set once the frame is identified -- the magic, the
     * protocol version and, on v3, the frame type -- which is the moment a
     * caller can stop applying a connect timeout: everything after it, a human
     * typing a pairing code, a bundle crossing, an entry point running, takes
     * as long as it takes. Not at the magic: four bytes are cheap to send, and
     * a peer that sent them and then stalled would otherwise be waited on
     * forever and exempted from the cleanup, which is all it takes to stop
     * discovery for good. Before it, a connection that accepts and says nothing
     * (an `adb reverse` with no listener behind it does exactly that) is closed
     * when the batch deadline passes.</p>
     */
    boolean handle(InputStream is, OutputStream os, boolean loopback, Progress progress) {
        firstContact();
        DataInputStream in = new DataInputStream(is);
        DataOutputStream out = new DataOutputStream(os);
        try {
            String reject = null;
            byte[] payload = null;
            if (in.readInt() != MAGIC) {
                reject = "bad magic";
            } else {
                int version = in.readInt();
                if (version == PROTOCOL_V1) {
                    progress.identify();
                    // Loopback is authenticated by physical presence only in
                    // the JavaSE simulator, where it is the same process's
                    // peer. On a real device -- notably Android, where
                    // every app shares the TCP loopback namespace -- a local
                    // app can connect to this listener over 127.0.0.1 and
                    // would otherwise get an unpaired v1 bundle accepted
                    // and its entry point run. Require the paired v3
                    // protocol on the device so the loopback address is
                    // never mistaken for authorisation.
                    if (!loopback || !Display.getInstance().isSimulator()) {
                        reject = "this app requires a paired computer; upgrade the push tool";
                    } else {
                        int length = in.readInt();
                        if (length <= 0 || length > MAX_BUNDLE) {
                            reject = "implausible bundle length " + length;
                        } else {
                            payload = new byte[length];
                            readBody(in, payload, progress);
                        }
                    }
                } else if (version == PROTOCOL_V3) {
                    int frame = in.readInt();
                    if (frame == FRAME_PING) {
                        progress.identify();
                        out.writeByte(1);
                        out.writeUTF(DeviceRuntimePairing.deviceId());
                        out.flush();
                        return true;
                    } else if (frame == FRAME_PAIR) {
                        // The identity first, and only then the prompt. Both
                        // fields are bounded and are the peer's to send at once;
                        // claiming before reading them let anything that opened
                        // a connection and stalled hold the single prompt slot
                        // for the life of the app, refusing every real pairing.
                        String peerId = in.readUTF();
                        String peerName = in.readUTF();
                        progress.identify();
                        // One prompt at a time, and a pause after a refusal.
                        // Nothing has authenticated yet at this point -- that is
                        // what pairing is for -- so anything on the network can
                        // reach here, and without this it could stack modal
                        // dialogs until the runtime's own UI was unusable.
                        if (!claimPairingPrompt()) {
                            out.writeByte(0);
                            out.writeUTF("a pairing prompt is already open on the device");
                            out.flush();
                            return true;
                        }
                        try {
                            // A person is about to be asked to type six digits,
                            // which is exactly the case an unbounded wait is
                            // for -- and it is reached only after the prompt
                            // slot was claimed, so it cannot be claimed by
                            // everything at once.
                            progress.allowLongWait();
                            handlePairing(peerId, peerName, progress, in, out);
                        } finally {
                            releasePairingPrompt();
                        }
                        return true;
                    } else if (frame == FRAME_PUSH) {
                        String peerId = in.readUTF();
                        String desktopChallenge = in.readUTF();
                        // Identified, which buys a bounded grace and no more:
                        // this much is cheap for anything on the network to
                        // send, and an unbounded wait here is a way to wedge
                        // discovery for good.
                        progress.identify();
                        byte[] secret = DeviceRuntimePairing.secretFor(peerId);
                        if (secret == null) {
                            // Said precisely, so the push tool can offer to pair
                            // rather than leaving the user to guess. A device
                            // that was reinstalled has forgotten pairings the
                            // desktop still believes in.
                            reject = "this computer is not paired with this device";
                        } else {
                            String challenge = InterpPairingSecret.challenge();
                            out.writeByte(1);
                            out.writeUTF(DeviceRuntimePairing.deviceId());
                            out.writeUTF(challenge);
                            // Authentication goes both ways. A device id is
                            // public, so without this any host on the LAN could
                            // answer the desktop's dial, claim to be a paired
                            // device and be handed the bundle -- which carries
                            // the program's whole source.
                            out.writeUTF(InterpPairingSecret.respond(secret, desktopChallenge));
                            out.flush();

                            String response = in.readUTF();
                            int length = in.readInt();
                            if (length <= 0 || length > MAX_BUNDLE) {
                                reject = "implausible bundle length " + length;
                            } else if (!reservePreAuth(length)) {
                                // Aggregate pre-authentication push memory
                                // cap. Refusing early with a message the
                                // desktop can show is better than allocating
                                // and getting OOM after a slow read.
                                reject = "this device is busy with another push"
                                        + " (pre-authentication buffer is full)";
                            } else {
                                boolean released = false;
                                try {
                                    // Read with a heartbeat rather than an
                                    // unbounded wait: nothing has authenticated
                                    // yet -- the answer is checked against the
                                    // bundle itself, so it cannot be until the
                                    // bytes are here -- and a peer that
                                    // declares a length and then stops sending
                                    // must not hold the waiters.
                                    byte[] body = new byte[length];
                                    readBody(in, body, progress);
                                    if (!InterpPairingSecret.matches(response,
                                            InterpPairingSecret.respond(secret, challenge, body))) {
                                        // Covers the bundle as well as the
                                        // challenge, so this also rejects a program
                                        // altered in flight behind a valid answer.
                                        reject = "this connection did not authenticate";
                                    } else {
                                        // Reservation was against the pre-auth
                                        // budget; release it now that this
                                        // peer has proven possession of the
                                        // pairing secret, so a second legitimate
                                        // push during approval does not have
                                        // to queue behind this one.
                                        releasePreAuth(length);
                                        released = true;
                                        if (!approveWhileWaiting(peerId, progress)) {
                                            // Only now: prompting before
                                            // authentication would let anyone
                                            // on the network raise dialogs on
                                            // this phone until somebody tapped
                                            // Approve to make them stop.
                                            reject = "this device did not approve the connection";
                                        } else {
                                            payload = body;
                                        }
                                    }
                                } finally {
                                    if (!released) {
                                        releasePreAuth(length);
                                    }
                                }
                            }
                        }
                    } else {
                        reject = "unknown frame type " + frame;
                    }
                } else {
                    reject = "push protocol version " + version + ", this app speaks "
                            + PROTOCOL_V1 + " and " + PROTOCOL_V3;
                }
            }
            if (reject != null) {
                status = "refused: " + reject;
                out.writeByte(0);
                out.writeUTF(reject);
                out.flush();
                // "bad magic" is the one refusal that says the peer is not a
                // push tool at all; every other one is our protocol saying no.
                return !"bad magic".equals(reject);
            }
            // Installing and starting a program takes as long as the program
            // takes, and the desktop is waiting for the result on this same
            // connection. Nothing unauthenticated reaches here: a v3 push
            // answered the challenge and was approved, and v1 is loopback only.
            progress.allowLongWait();
            try {
                String result = loadAndRun(payload);
                out.writeByte(1);
                out.writeUTF(result);
            } catch (Throwable t) {
                String described = describe(runtime, t);
                status = "error: " + described;
                out.writeByte(0);
                out.writeUTF(described);
            }
            out.flush();
            return true;
        } catch (Throwable t) {
            // The peer is gone or the framing is broken; there is nobody left to
            // tell, so record it for the on-device status line and move on.
            status = "push failed: " + describe(t);
            return false;
        }
    }

    /// Run once, when the first program arrives.
    ///
    /// The host app registers this; the runtime does not know what it does. In
    /// this repository's test host it stands down the screenshot suite, which
    /// otherwise competes with pushed programs for the display and the event
    /// thread. Reflection would have been the obvious way to reach back into
    /// the host and is not available: ParparVM has none, and the bytecode
    /// compliance gate rejects it outright.
    private static Runnable onFirstPush;

    /// Registers work to run when the first program is pushed.
    public static void setOnFirstPush(Runnable r) {
        onFirstPush = r;
    }

    /// Runs the host's stand-down hook, once, as soon as a desktop connects.
    ///
    /// On connection rather than on a served push: pairing happens first, and
    /// its prompt is exactly what a busy host app steals.
    private static void firstContact() {
        Runnable r = onFirstPush;
        if (r != null) {
            onFirstPush = null;
            r.run();
        }
    }

    /**
     * Applies the pushed program's own theme, or restores the runtime's.
     *
     * <p>Done here rather than left to {@code Lifecycle.init}: the pushed
     * program may not reach that at all -- a program entered through
     * {@code main} never does -- and a program wearing the host's theme is
     * indistinguishable from the host, which is precisely the doubt a runtime
     * has to avoid. Its own design travelling with it is the visible proof that
     * what is on screen is the pushed code.</p>
     */
    private void applyPushedTheme(final byte[] themeBytes) {
        // On the event thread. UIManager.refreshTheme() walks the current
        // form and updates its cached styles, and Form components published
        // through setGlobalResources are read by paint(); doing this from
        // the socket worker while the EDT may be painting or handling
        // input caused race-dependent corruption or exceptions. Install
        // reaches here off-EDT, so a callSeriallyAndWait hop is required;
        // teardown paths that already run from the UI reuse the inline
        // branch to avoid re-entering the pump.
        if (Display.getInstance().isEdt()) {
            applyPushedThemeOnEdt(themeBytes);
            return;
        }
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                applyPushedThemeOnEdt(themeBytes);
            }
        });
    }

    private void applyPushedThemeOnEdt(byte[] themeBytes) {
        try {
            if (themeBytes == null) {
                if (hostTheme != null) {
                    Resources.setGlobalResources(hostTheme);
                    UIManager.getInstance().setThemeProps(
                            hostTheme.getTheme(hostTheme.getThemeResourceNames()[0]));
                    UIManager.getInstance().refreshTheme();
                }
                return;
            }
            if (hostTheme == null) {
                // Remembered once, so stopping a program can put the runtime's
                // own look back.
                hostTheme = Resources.getGlobalResources();
            }
            Resources pushed = Resources.open(new java.io.ByteArrayInputStream(themeBytes));
            String[] names = pushed.getThemeResourceNames();
            System.out.println("CN1SS:DEVRUNTIME pushed theme has "
                    + (names == null ? 0 : names.length) + " theme(s)");
            if (names != null && names.length > 0) {
                Resources.setGlobalResources(pushed);
                UIManager.getInstance().setThemeProps(pushed.getTheme(names[0]));
                // Styles are cached per UIID, and components already built hold
                // theirs. Without this the new theme only reaches whatever is
                // created next, which on a device that is already showing a
                // form means it appears not to have worked at all.
                UIManager.getInstance().refreshTheme();
                System.out.println("CN1SS:DEVRUNTIME applied theme " + names[0]);
            }
        } catch (Throwable t) {
            System.out.println("CN1SS:DEVRUNTIME could not apply the pushed theme: " + t);
        }
    }

    private Resources hostTheme;

    /**
     * Serializes installing a bundle.
     *
     * <p>Two pushes can arrive at once -- every accepted connection is served on
     * a thread of its own, and the outbound dialer is a third -- and installing
     * is not one step but several: clearing and republishing global resources,
     * applying a theme, detaching the previous runtime and publishing the new
     * one. Interleaved, one program starts with the other's resources, or is
     * detached the moment it starts while its push reports success.</p>
     *
     * <p>Held only by the threads that serve a push. The event thread must
     * never take it: installing calls {@code callSeriallyAndWait}, so an event
     * thread blocking here while the installer waits for the event thread is a
     * deadlock. That is why stopping a program -- which runs from the UI --
     * does not.</p>
     */
    private final Object installLock = new Object();

    private String loadAndRun(byte[] payload) throws Throwable {
        synchronized (installLock) {
            return install(payload);
        }
    }

    private String install(byte[] payload) throws Throwable {
        // Before anything, parsing included: a stop pressed while the bundle is
        // being read is a stop during this installation, and a snapshot taken
        // later would record that stop as this install's own baseline.
        final int generation = stopGeneration;

        InterpBundle bundle = InterpBundleReader.read(new ByteArrayInputStream(payload));

        // Retire whatever is running before publishing anything of the new
        // program's. Stop is on screen throughout an install -- a no-UI program
        // leaves the runtime's own form up -- and stopping tears down exactly
        // the resources and theme published here; doing it in this order means
        // a stop that lands mid-install ends the old program, not the new one's
        // assets, and the install then fails its own current-runtime check.
        // A Stop arriving during an install tears down what is being
        // published, and the counter above is how the installer notices rather
        // than finishing on top of a teardown.
        retirePrevious();
        requireNotStopped(generation);

        // The program's own theme, CSS and images, published where the
        // framework looks for them. Cleared first so a program that ships no
        // theme does not inherit the previous one's.
        CodenameOneImplementation.clearLocalResources();
        java.util.Hashtable res = bundle.getResources();
        java.util.Enumeration paths = res.keys();
        while (paths.hasMoreElements()) {
            String path = (String)paths.nextElement();
            CodenameOneImplementation.setLocalResource(path, (byte[])res.get(path));
        }
        applyPushedTheme((byte[])res.get("/theme.res"));

        StringBuilder src = new StringBuilder();
        Enumeration e = bundle.getSourceFileNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            src.append("// ").append(name).append('\n')
               .append(bundle.getSource(name)).append('\n');
        }
        loadedSource = src.toString();
        String main = bundle.getMainClass();
        loadedName = main == null ? "" : main.replace('/', '.');

        ShimObjectFactory factory = new ShimObjectFactory();
        final InterpRuntime rt = new InterpRuntime(bundle, InterpPlatform.getLinker(), factory);
        factory.attach(rt);
        // Purchases and social logins are answered by mocks: see
        // DeviceRuntimeMocks for what this runtime cannot honestly provide and
        // why standing in for it beats reporting it unsupported.
        rt.setHostInterceptor(new DeviceRuntimeMocks());
        DeviceRuntimeMocks.reset();
        mocksUsed = "";
        runtime = rt;
        if (stopGeneration != generation) {
            // The stop landed between the guard above and this publication, so
            // it tore down a runtime this one has just replaced. Undo the
            // publication rather than leaving a stopped program reported as
            // loaded with its callbacks live.
            rollback(rt);
            requireNotStopped(generation);
        }

        final Throwable[] failure = new Throwable[1];
        final String[] outcome = new String[1];
        final boolean[] stopped = new boolean[1];
        // On the event thread: a pushed program builds UI, and Codename One
        // requires that to happen there.
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                // Stop can land between publishing this runtime and the event
                // thread reaching here -- the wait above is exactly that
                // window. Entering it anyway starts a program the user has
                // already ended: runMain clears the cancel flag, so the entry
                // point runs and shows a form while the service reports
                // nothing loaded.
                if (runtime != rt || rt.isDetached() || stopGeneration != generation) {  //NOPMD CompareObjectsWithEquals - this runtime, not an equal one
                    stopped[0] = true;
                    return;
                }
                try {
                    outcome[0] = runProgram(rt);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        if (stopped[0]) {
            status = "stopped before it started";
            throw new IllegalStateException(
                    "the program was stopped before its entry point ran");
        }
        if (failure[0] != null) {
            // Its stop() first: start() may have acquired a recorder or a
            // sensor before it threw, and stop is where a program releases
            // those -- after detaching, that callback would be a no-op like
            // every other. On the event thread, like every other lifecycle
            // callback: this runs on the thread serving the push.
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                public void run() {
                    stopLifecycleQuietly(rt);
                }
            });
            // Detach before reporting. The entry point may have shown a form or
            // registered a listener before it threw, and those callbacks would
            // go on running a program the desktop was just told had failed --
            // and isProgramLoaded() would agree that it is loaded.
            rt.detach();
            if (runtime == rt) {  //NOPMD CompareObjectsWithEquals - this runtime, not an equal one
                runtime = null;
                loadedName = "";
                loadedSource = "";
                CodenameOneImplementation.clearLocalResources();
                applyPushedTheme(null);
            }
            status = "failed to start";
            // And put the runtime's own screen back. The entry point may have
            // shown a form before it threw, and leaving it up strands the user
            // on a half-built screen whose callbacks are all detached --
            // exactly what stopProgram avoids by doing the same thing.
            DeviceRuntimeForm.showIt();
            throw failure[0];
        }
        // Form.show() queues the switch rather than performing it inline, so the
        // current form has to be read on a later pass of the event thread.
        // Reading it in the same pass reports the previous screen and makes a
        // working push look like it did nothing.
        final String[] shown = new String[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                Form current = Display.getInstance().getCurrent();
                shown[0] = current == null ? null : current.getTitle();
            }
        });
        // Stop can also land while the entry point was finishing -- a program
        // that shows nothing leaves the runtime's own form and its Stop button
        // on screen throughout. Reporting "running" then would overwrite the
        // status the stop just set, and tell the desktop a push succeeded into
        // a runtime that is no longer there.
        if (runtime != rt || rt.isDetached() || stopGeneration != generation) {  //NOPMD CompareObjectsWithEquals - this runtime, not an equal one
            status = "stopped before it started";
            throw new IllegalStateException(
                    "the program was stopped while its entry point ran");
        }
        status = "running " + rt.getBundle().getMainClass();
        if (shown[0] != null) {
            return outcome[0] + "; showing \"" + shown[0] + "\"";
        }
        return outcome[0];
    }

    /**
     * Runs the pushed program. A program whose main returns a {@code Form} -- or
     * which leaves one current -- is shown; anything else simply runs.
     */
    private String runProgram(InterpRuntime rt) throws Throwable {
        Form before = Display.getInstance().getCurrent();
        Object result = rt.runMain(new String[0]);
        if (result instanceof Form) {
            ((Form)result).show();
            return "showed " + result.getClass().getName();
        }
        Form after = Display.getInstance().getCurrent();
        if (after != before && after != null) {
            return "showed " + after.getTitle();
        }
        // A program with no UI is legitimate; say so rather than implying it
        // failed.
        return "ran " + rt.getBundle().getMainClass();
    }

    public boolean isProgramLoaded() {
        return runtime != null;
    }

    /// Counts stops, so an install can tell one happened while it was working.
    ///
    /// Comparing the running runtime is not enough on its own: a stop landing
    /// between retiring the old program and publishing the new one clears the
    /// resources just published, and the field would then agree that the new
    /// runtime is the current one.
    private volatile int stopGeneration;  //NOPMD AvoidUsingVolatile - written from the UI, read on the install thread

    /// Retires a runtime this install published and cannot go on to start.
    ///
    /// The same teardown a failed entry point does: detach it, forget the
    /// program, and put the resources and theme back the way a stop leaves
    /// them -- otherwise a stopped program stays reported as loaded and its
    /// callbacks keep running.
    private void rollback(InterpRuntime rt) {
        stopLifecycleQuietly(rt);
        rt.detach();
        if (runtime == rt) {  //NOPMD CompareObjectsWithEquals - this runtime, not an equal one
            runtime = null;
        }
        loadedName = "";
        loadedSource = "";
        CodenameOneImplementation.clearLocalResources();
        applyPushedTheme(null);
        DeviceRuntimeForm.showIt();
    }

    /// Fails the install when a stop happened since it started.
    private void requireNotStopped(int generation) {
        if (stopGeneration != generation) {
            status = "stopped before it started";
            throw new IllegalStateException("the program was stopped while it was being installed");
        }
    }

    /**
     * Ends the program that is running, if any, before another is installed.
     *
     * <p>A replaced program's peers are still held by framework listeners and
     * timers, and without detaching they go on dispatching into the old runtime
     * alongside the new one -- and a later Stop would only detach the newest.
     * Its stop() is delivered first, on the event thread, because that is where
     * a Lifecycle's callbacks belong: releasing a recorder or a sensor from a
     * socket thread is not something the framework expects.</p>
     */
    private void retirePrevious() {
        final InterpRuntime previous = runtime;
        if (previous == null) {
            return;
        }
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                stopLifecycleQuietly(previous);
                // The retired program's form is now inert -- its callbacks
                // are on their way out with the detach below -- so put the
                // runtime's own form back before the replacement runs. A
                // successor that shows no UI would otherwise leave the old,
                // now-dead UI on screen and `runProgram`'s before/after
                // comparison would see "no form change" and report the new
                // program as a legitimate no-UI run.
                DeviceRuntimeForm.showIt();
            }
        });
        previous.detach();
    }

    /**
     * Delivers stop() to a pushed Lifecycle, reporting rather than propagating.
     *
     * <p>Stopping has to finish. A program whose stop() throws is a program
     * with a bug, not a reason to leave the runtime half-detached with its
     * screen still owned by the thing the user asked to end.</p>
     */
    private void stopLifecycleQuietly(InterpRuntime rt) {
        try {
            if (rt.stopLifecycle()) {
                System.out.println("CN1SS:DEVRUNTIME delivered stop() to the pushed program");
            }
        } catch (Throwable t) {
            System.out.println("CN1SS:DEVRUNTIME the pushed program's stop() threw: " + t);
        }
    }

    public void stopProgram() {
        // Counted whether or not something is running: an install in flight has
        // to notice a stop that arrives before it published its runtime.
        stopGeneration++;
        InterpRuntime rt = runtime;
        if (rt == null) {
            return;
        }
        // stop() first, while the runtime still answers: a Lifecycle that
        // opened a media player, a socket or a sensor releases it there, and
        // detaching without delivering stop leaves those running against the
        // runtime's own screen and against whatever is pushed next.
        stopLifecycleQuietly(rt);
        rt.detach();
        // Cancellation alone stops interpreted code that is *running*. A normal
        // Lifecycle program is not running when Stop is pressed: its start()
        // returned after showing a Form, and what remains is listeners the
        // framework still holds -- each holding a peer that holds the runtime,
        // so dropping this field alone would not stop them. detach() makes the
        // runtime itself refuse every later callback; putting the runtime's own
        // screen back is what tells the user it worked.
        runtime = null;
        loadedName = "";
        loadedSource = "";
        CodenameOneImplementation.clearLocalResources();
        // And the theme with them: a pushed program that shipped its own
        // theme.res left the runtime's screen wearing it, until some later
        // theme-less push happened to put it back.
        applyPushedTheme(null);
        status = "stopped";
        DeviceRuntimeForm.showIt();
    }

    public String getStatus() {
        return status;
    }

    /// Records that a pushed program used a mocked subsystem.
    void noteMockUsed(String subsystem) {
        if (mocksUsed.indexOf(subsystem) < 0) {
            mocksUsed = mocksUsed.length() == 0 ? subsystem : mocksUsed + ", " + subsystem;
        }
        status = "running (mocked: " + mocksUsed + ")";
    }

    /// The mocked subsystems this program has used, empty when it has used none.
    public String getMocksUsed() {
        return mocksUsed;
    }

    public String getLoadedSource() {
        return loadedSource;
    }

    /// The entry class of the running program, or "" when nothing is running.
    public String getLoadedName() {
        return loadedName;
    }

    static String describe(Throwable t) {
        return describe(null, t);
    }

    /// A failure as the person who pushed the program needs to read it.
    ///
    /// The interpreted frames are the whole point: a host exception thrown by
    /// pushed code carries a stack trace naming the interpreter, which says
    /// nothing about the program. The runtime records where the program threw,
    /// so that is what gets reported when it is available.
    static String describe(InterpRuntime rt, Throwable t) {
        if (t instanceof InterpThrowable) {
            return ((InterpThrowable)t).getInterpretedStackTrace();
        }
        String m = t.getMessage();
        String head = t.getClass().getName() + (m == null ? "" : ": " + m);
        String hostCall = rt == null ? null : rt.hostCallFor(t);
        if (hostCall != null) {
            head = head + " (thrown by " + hostCall + ")";
        }
        String[] frames = rt == null ? null : rt.interpretedStackFor(t);
        if (frames == null || frames.length == 0) {
            return head;
        }
        StringBuilder sb = new StringBuilder(head);
        for (int i = 0; i < frames.length; i++) {
            sb.append("\n\tat ").append(frames[i]);
        }
        return sb.toString();
    }
}
