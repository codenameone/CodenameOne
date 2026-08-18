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

    /// This device's address on the network, or null.
    public static String getLocalAddress() {
        try {
            String ip = Socket.getHostOrIP();
            return ip != null && ip.indexOf('.') > 0 && !isLoopback(ip) ? ip : null;
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
        final boolean[] started = new boolean[1];
        final boolean[] finished = new boolean[1];
        final boolean[] spoke = new boolean[1];
        // The streams, so a connection that accepts and then says nothing can
        // be closed rather than left behind. Socket.connect gives the callback
        // its own thread and closes nothing when the caller gives up, so
        // without this every dial past a silent listener leaks a thread parked
        // in readInt and the socket under it, for the life of the app.
        final InputStream[] open = new InputStream[1];
        SocketConnection sc = new SocketConnection() {
            public void connectionEstablished(InputStream is, OutputStream os) {
                open[0] = is;
                try {
                    spoke[0] = handle(is, os, isLoopback(host), started);
                } finally {
                    finished[0] = true;
                    open[0] = null;
                }
            }

            public void connectionError(int errorCode, String message) {
                // Nobody listening: the ordinary case between pushes.
                finished[0] = true;
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
        while (!finished[0] && (started[0] || System.currentTimeMillis() < deadline)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return spoke[0];
            }
        }
        if (!finished[0]) {
            // Gave up on a peer that never said anything. Closing the stream is
            // what unblocks the read the handler is parked in, so its thread
            // ends rather than accumulating one per dial.
            closeQuietly(open[0]);
        }
        return spoke[0];
    }

    /// Closes everything a sweep batch left open, unblocking the readers.
    private static void closeAll(Vector streams) {
        synchronized (streams) {
            for (int i = 0; i < streams.size(); i++) {
                closeQuietly((InputStream) streams.elementAt(i));
            }
            streams.removeAllElements();
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
        if (self == null || self.indexOf('.') < 0 || isLoopback(self)) {
            return false;
        }
        String prefix = self.substring(0, self.lastIndexOf('.') + 1);
        String selfSuffix = self.substring(self.lastIndexOf('.') + 1);
        status = "looking for a computer on " + prefix + "*";

        // Streams the sweep has open, so an address that accepts and then says
        // nothing can be closed rather than left with a thread parked in
        // readInt. A sweep is 254 addresses; leaking one thread each would end
        // the app.
        final Vector openStreams = new Vector();
        for (int base = 1; base <= 254; base += SWEEP_BATCH) {
            final boolean[] found = new boolean[1];
            final String[] foundAt = new String[1];
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
                        // readInt forever, and a sweep is 254 of them.
                        synchronized (openStreams) {
                            openStreams.addElement(is);
                        }
                        boolean spoke = handle(is, os, false);
                        synchronized (openStreams) {
                            openStreams.removeElement(is);
                        }
                        if (spoke) {
                            synchronized (found) {
                                if (!found[0]) {
                                    found[0] = true;
                                    foundAt[0] = candidate;
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
            long deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS + 800;
            while (!found[0] && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    closeAll(openStreams);
                    return false;
                }
            }
            // Whatever this batch left parked on a silent address.
            closeAll(openStreams);
            if (found[0]) {
                setHost(foundAt[0]);
                status = "found " + foundAt[0] + ":" + port;
                return true;
            }
        }
        status = "no computer found on " + prefix + "*";
        return false;
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
    private void handlePairing(DataInputStream in, DataOutputStream out) throws IOException {
        String peerId = in.readUTF();
        String peerName = in.readUTF();
        String code = DeviceRuntimePairing.promptForCode(peerId, peerName);
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
        return handle(is, os, loopback, new boolean[1]);
    }

    /**
     * Serves one connection, reporting when the exchange actually began.
     *
     * <p>{@code started[0]} goes true once the magic has been read, which is
     * the moment a caller can stop applying a connect timeout: everything after
     * it -- a human typing a pairing code, a bundle crossing, an entry point
     * running -- takes as long as it takes. Before it, a connection that
     * accepts and then says nothing (an `adb reverse` with no listener behind
     * it does exactly that) must not be waited on forever.</p>
     */
    boolean handle(InputStream is, OutputStream os, boolean loopback, boolean[] started) {
        firstContact();
        DataInputStream in = new DataInputStream(is);
        DataOutputStream out = new DataOutputStream(os);
        try {
            String reject = null;
            byte[] payload = null;
            if (in.readInt() != MAGIC) {
                reject = "bad magic";
            } else {
                started[0] = true;
                int version = in.readInt();
                if (version == PROTOCOL_V1) {
                    if (!loopback) {
                        reject = "this app requires a paired computer; upgrade the push tool";
                    } else {
                        int length = in.readInt();
                        if (length <= 0 || length > MAX_BUNDLE) {
                            reject = "implausible bundle length " + length;
                        } else {
                            payload = new byte[length];
                            in.readFully(payload);
                        }
                    }
                } else if (version == PROTOCOL_V3) {
                    int frame = in.readInt();
                    if (frame == FRAME_PING) {
                        out.writeByte(1);
                        out.writeUTF(DeviceRuntimePairing.deviceId());
                        out.flush();
                        return true;
                    } else if (frame == FRAME_PAIR) {
                        handlePairing(in, out);
                        return true;
                    } else if (frame == FRAME_PUSH) {
                        String peerId = in.readUTF();
                        String desktopChallenge = in.readUTF();
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
                            } else {
                                byte[] body = new byte[length];
                                in.readFully(body);
                                if (!InterpPairingSecret.matches(response,
                                        InterpPairingSecret.respond(secret, challenge, body))) {
                                    // Covers the bundle as well as the
                                    // challenge, so this also rejects a program
                                    // altered in flight behind a valid answer.
                                    reject = "this connection did not authenticate";
                                } else if (!DeviceRuntimePairing.approve(peerId)) {
                                    // Only now: prompting before authentication
                                    // would let anyone on the network raise
                                    // dialogs on this phone until somebody
                                    // tapped Approve to make them stop.
                                    reject = "this device did not approve the connection";
                                } else {
                                    payload = body;
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
    private void applyPushedTheme(byte[] themeBytes) {
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

    private String loadAndRun(byte[] payload) throws Throwable {

        InterpBundle bundle = InterpBundleReader.read(new ByteArrayInputStream(payload));

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

        // Detach whatever was running first. A replaced program's peers are
        // still held by framework listeners and timers, and without this they
        // go on dispatching into the old runtime alongside the new one -- and
        // Stop, later, would only detach the newest.
        InterpRuntime previous = runtime;
        if (previous != null) {
            previous.detach();
        }
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

        final Throwable[] failure = new Throwable[1];
        final String[] outcome = new String[1];
        // On the event thread: a pushed program builds UI, and Codename One
        // requires that to happen there.
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    outcome[0] = runProgram(rt);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        if (failure[0] != null) {
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

    public void stopProgram() {
        InterpRuntime rt = runtime;
        if (rt == null) {
            return;
        }
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
