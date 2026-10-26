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
package com.codename1.io.mqtt;

import com.codename1.ui.CN;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/// An MQTT 3.1.1 client for QoS 0 and QoS 1.
///
/// #### Threading
///
/// Like the rest of the framework the client is single threaded: call it on the
/// EDT, and every [MqttListener] / [MqttMessageListener] callback arrives there.
/// Transport threads only post to the EDT, so the client itself holds no locks.
/// Writes happen on the calling thread.
///
/// #### Delivery
///
/// A QoS 1 publish stays in flight until its PUBACK and is sent again, with the
/// DUP flag, after a reconnect; with a clean session the in flight list is
/// dropped when connecting. A QoS 1 publish made while disconnected is queued
/// and sent after the next successful connect. QoS 0 needs a connection. An
/// incoming QoS 1 message is acknowledged before it is delivered.
///
/// #### Keep alive
///
/// A small daemon thread ticks once a second and posts the check to the EDT. A
/// PINGREQ goes out when nothing was sent for the keep alive interval; when no
/// PINGRESP arrives within one and a half intervals the connection is declared
/// lost. The same tick enforces the connect timeout.
///
/// #### Subscriptions
///
/// A subscription made with a listener routes matching messages to it only; a
/// message no such subscription claims goes to the [MqttListener]. The client
/// does not re-subscribe on reconnect: do it from [MqttListener#connected()]
/// (a broker keeps the subscriptions of a persistent session, `cleanSession(false)`).
public final class MqttClient {
    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    private static final class Route {
        final String filter;
        final MqttMessageListener listener;

        Route(String filter, MqttMessageListener listener) {
            this.filter = filter;
            this.listener = listener;
        }
    }

    private static final class NoListener implements MqttListener {
        public void connected() {
        }

        public void messageArrived(String topic, byte[] payload, boolean retained) {
        }

        public void connectionLost(String reason) {
        }
    }

    private final MqttTransport injected;
    private int state = DISCONNECTED;
    private MqttOptions options = new MqttOptions("tcp://localhost", "");
    private MqttListener listener = new NoListener();

    private MqttTransport transport;
    private int generation;
    private byte[] buf = new byte[256];
    private int bufLen;
    private long lastSend;
    private long pingSentAt;
    private long connectStart;
    private int nextId;
    private final ArrayList<MqttPacket> inFlight = new ArrayList<MqttPacket>();
    private final ArrayList<Route> routes = new ArrayList<Route>();
    private final HashMap<Integer, String[]> pendingSubs = new HashMap<Integer, String[]>();
    private AtomicBoolean tickerStop;
    private long fakeNow = -1;

    /// Creates a disconnected client.
    public MqttClient() {
        this.injected = null;
    }

    MqttClient(MqttTransport transport) {
        this.injected = transport;
    }

    /// Opens a connection and sends CONNECT. Returns at once; the outcome arrives
    /// as [MqttListener#connected()] or [MqttListener#connectionLost(String)].
    ///
    /// #### Parameters
    ///
    /// - `options`: broker and session settings
    ///
    /// - `listener`: receives the callbacks
    ///
    /// #### Throws
    ///
    /// `IllegalStateException` when already connected or connecting,
    /// `IllegalArgumentException` for an unsupported URI
    public void connect(MqttOptions options, MqttListener listener) {
        if (state != DISCONNECTED) {
            throw new IllegalStateException("Already connected");
        }
        MqttTransport t = injected != null ? injected : createTransport(options);
        this.options = options;
        this.listener = listener;
        if (options.isCleanSession()) {
            inFlight.clear();
            pendingSubs.clear();
        }
        bufLen = 0;
        pingSentAt = 0;
        state = CONNECTING;
        transport = t;
        connectStart = now();
        final int gen = ++generation;
        if (injected == null) {
            startTicker();
        }
        t.open(new MqttTransport.Handler() {
            public void opened() {
                if (gen == generation) {
                    onOpened();
                }
            }

            public void received(byte[] data, int len) {
                if (gen == generation) {
                    onReceived(data, len);
                }
            }

            public void closed(String reason) {
                if (gen == generation) {
                    fail(reason);
                }
            }
        });
    }

    private static MqttTransport createTransport(MqttOptions o) {
        String uri = o.getUri();
        if (startsWithIgnoreCase(uri, "ws://") || startsWithIgnoreCase(uri, "wss://")) {
            return new WebSocketMqttTransport(uri, o.getConnectTimeoutMillis());
        }
        if (!startsWithIgnoreCase(uri, "tcp://")) {
            throw new IllegalArgumentException("Unsupported URI scheme (use tcp://, ws:// or wss://): " + uri);
        }
        String hp = uri.substring(6);
        int slash = hp.indexOf('/');
        if (slash >= 0) {
            hp = hp.substring(0, slash);
        }
        int port = 1883;
        int colon = hp.lastIndexOf(':');
        if (colon >= 0) {
            try {
                port = Integer.parseInt(hp.substring(colon + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad port in " + uri);
            }
            hp = hp.substring(0, colon);
        }
        if (hp.length() == 0) {
            throw new IllegalArgumentException("Missing host in " + uri);
        }
        return new SocketMqttTransport(hp, port, o.getConnectTimeoutMillis());
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /// Whether the broker has accepted the connection and it is still up.
    public boolean isConnected() {
        return state == CONNECTED;
    }

    /// The number of QoS 1 publishes not yet acknowledged, queued ones included.
    public int getInFlightCount() {
        return inFlight.size();
    }

    /// Publishes a message.
    ///
    /// #### Parameters
    ///
    /// - `topic`: the topic name, without wildcards
    ///
    /// - `payload`: the message body
    ///
    /// - `qos`: 0 (needs a connection) or 1 (queued until connected)
    ///
    /// - `retain`: ask the broker to retain the message
    public void publish(String topic, byte[] payload, int qos, boolean retain) {
        MqttTopic.validateName(topic);
        if (qos < 0 || qos > 1) {
            throw new IllegalArgumentException("Only QoS 0 and 1 are supported");
        }
        if (qos == 0) {
            requireConnected();
            send(MqttPacket.publish(topic, payload, 0, retain, false, 0));
            return;
        }
        MqttPacket p = MqttPacket.publish(topic, payload, 1, retain, false, allocId());
        inFlight.add(p);
        if (state == CONNECTED) {
            send(p);
        }
    }

    /// Subscribes; matching messages go to the [MqttListener].
    ///
    /// #### Parameters
    ///
    /// - `topicFilter`: the filter, `+` and `#` allowed
    ///
    /// - `qos`: the maximum QoS wanted, 0 or 1
    public void subscribe(String topicFilter, int qos) {
        subscribe(topicFilter, qos, null);
    }

    /// Subscribes and routes matching messages to a listener of their own.
    ///
    /// #### Parameters
    ///
    /// - `topicFilter`: the filter, `+` and `#` allowed
    ///
    /// - `qos`: the maximum QoS wanted, 0 or 1
    ///
    /// - `messageListener`: receives the matches; null means the [MqttListener]
    public void subscribe(String topicFilter, int qos, MqttMessageListener messageListener) {
        MqttTopic.validateFilter(topicFilter);
        if (qos < 0 || qos > 1) {
            throw new IllegalArgumentException("Only QoS 0 and 1 are supported");
        }
        requireConnected();
        if (messageListener != null) {
            removeRoutes(topicFilter);
            routes.add(new Route(topicFilter, messageListener));
        }
        int id = allocId();
        pendingSubs.put(Integer.valueOf(id), new String[] {topicFilter});
        send(MqttPacket.subscribe(id, new String[] {topicFilter}, new int[] {qos}));
    }

    /// Removes a subscription.
    ///
    /// #### Parameters
    ///
    /// - `topicFilter`: the filter given to subscribe
    public void unsubscribe(String topicFilter) {
        MqttTopic.validateFilter(topicFilter);
        requireConnected();
        removeRoutes(topicFilter);
        send(MqttPacket.unsubscribe(allocId(), new String[] {topicFilter}));
    }

    /// Sends DISCONNECT and closes the connection. No `connectionLost` follows.
    /// QoS 1 publishes still unacknowledged stay queued for the next connect.
    public void disconnect() {
        if (state == DISCONNECTED) {
            return;
        }
        if (state == CONNECTED) {
            try {
                transport.send(MqttCodec.encode(MqttPacket.disconnect()));
            } catch (IOException e) {
                // going away anyway
                state = DISCONNECTED;
            }
        }
        shutdown();
    }

    private void requireConnected() {
        if (state != CONNECTED) {
            throw new IllegalStateException("Not connected");
        }
    }

    private void removeRoutes(String filter) {
        for (int i = routes.size() - 1; i >= 0; i--) {
            if (routes.get(i).filter.equals(filter)) {
                routes.remove(i);
            }
        }
    }

    private int allocId() {
        for (int n = 0; n < 65535; n++) {
            nextId = nextId % 65535 + 1;
            if (!idInUse(nextId)) {
                return nextId;
            }
        }
        throw new IllegalStateException("No free packet id");
    }

    private boolean idInUse(int id) {
        for (int i = 0; i < inFlight.size(); i++) {
            if (inFlight.get(i).getPacketId() == id) {
                return true;
            }
        }
        return pendingSubs.containsKey(Integer.valueOf(id));
    }

    private long now() {
        return fakeNow >= 0 ? fakeNow : System.currentTimeMillis();
    }

    private void send(MqttPacket p) {
        try {
            transport.send(MqttCodec.encode(p));
            lastSend = now();
        } catch (IOException e) {
            fail("Write failed: " + e.getMessage());
        }
    }

    private void onOpened() {
        if (state == CONNECTING) {
            send(MqttPacket.connect(options));
        }
    }

    private void onReceived(byte[] data, int len) {
        if (bufLen + len > buf.length) {
            byte[] nb = new byte[Math.max(buf.length * 2, bufLen + len)];
            System.arraycopy(buf, 0, nb, 0, bufLen);
            buf = nb;
        }
        System.arraycopy(data, 0, buf, bufLen, len);
        bufLen += len;
        try {
            while (state != DISCONNECTED) {
                int total = MqttCodec.packetLength(buf, 0, bufLen, options.getMaxPacketSize());
                if (total < 0) {
                    return;
                }
                int hdr = 1;
                while ((buf[hdr] & 0x80) != 0) {
                    hdr++;
                }
                hdr++;
                byte[] body = new byte[total - hdr];
                System.arraycopy(buf, hdr, body, 0, body.length);
                int first = buf[0] & 0xff;
                System.arraycopy(buf, total, buf, 0, bufLen - total);
                bufLen -= total;
                handle(MqttCodec.decode(first, body));
            }
        } catch (IOException e) {
            fail("Protocol error: " + e.getMessage());
        }
    }

    private void handle(MqttPacket p) throws IOException {
        int type = p.getType();
        if (state == CONNECTING) {
            if (type != MqttPacket.CONNACK) {
                throw new MqttException("Expected CONNACK, got packet type " + type);
            }
            if (p.getReturnCode() != 0) {
                fail("Connection refused: " + refusal(p.getReturnCode()));
                return;
            }
            state = CONNECTED;
            pingSentAt = 0;
            for (int i = 0; i < inFlight.size() && state == CONNECTED; i++) {
                MqttPacket m = inFlight.get(i);
                m.setDup(true);
                send(m);
            }
            if (state == CONNECTED) {
                listener.connected();
            }
            return;
        }
        switch (type) {
            case MqttPacket.PUBLISH:
                if (p.getQos() == 1) {
                    send(MqttPacket.puback(p.getPacketId()));
                }
                deliver(p);
                break;
            case MqttPacket.PUBACK:
                for (int i = 0; i < inFlight.size(); i++) {
                    if (inFlight.get(i).getPacketId() == p.getPacketId()) {
                        inFlight.remove(i);
                        break;
                    }
                }
                break;
            case MqttPacket.SUBACK:
                String[] filters = pendingSubs.remove(Integer.valueOf(p.getPacketId()));
                if (filters != null && p.getQosList().length > 0 && p.getQosList()[0] == 0x80) {
                    removeRoutes(filters[0]);
                }
                break;
            case MqttPacket.UNSUBACK:
                break;
            case MqttPacket.PINGRESP:
                pingSentAt = 0;
                break;
            default:
                throw new MqttException("Unexpected packet type " + type);
        }
    }

    private void deliver(MqttPacket p) {
        boolean claimed = false;
        ArrayList<Route> snapshot = new ArrayList<Route>(routes);
        for (int i = 0; i < snapshot.size(); i++) {
            Route r = snapshot.get(i);
            if (MqttTopic.matches(r.filter, p.getTopic())) {
                claimed = true;
                r.listener.messageArrived(p.getTopic(), p.getPayload(), p.isRetain());
            }
        }
        if (!claimed) {
            listener.messageArrived(p.getTopic(), p.getPayload(), p.isRetain());
        }
    }

    private static String refusal(int code) {
        switch (code) {
            case 1:
                return "unacceptable protocol version";
            case 2:
                return "identifier rejected";
            case 3:
                return "server unavailable";
            case 4:
                return "bad user name or password";
            case 5:
                return "not authorized";
            default:
                return "return code " + code;
        }
    }

    /// Runs the keep alive and connect timeout checks for the given time.
    void tick(long nowMillis) {
        if (state == CONNECTING) {
            if (nowMillis - connectStart > options.getConnectTimeoutMillis()) {
                fail("Connect timed out");
            }
            return;
        }
        long ka = options.getKeepAliveSeconds() * 1000L;
        if (state != CONNECTED || ka == 0) {
            return;
        }
        if (pingSentAt != 0) {
            if (nowMillis - pingSentAt > ka + ka / 2) {
                fail("Keep alive timed out");
            }
        } else if (nowMillis - lastSend >= ka) {
            pingSentAt = nowMillis;
            send(MqttPacket.pingreq());
        }
    }

    /// Test hook: drives the clock by hand and turns the ticker thread off.
    void setClock(long millis) {
        fakeNow = millis;
    }

    void tickNow() {
        tick(now());
    }

    private void startTicker() {
        final AtomicBoolean stop = new AtomicBoolean();
        tickerStop = stop;
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (!stop.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (stop.get()) {
                        return;
                    }
                    CN.callSerially(new Runnable() {
                        public void run() {
                            if (!stop.get()) {
                                tickNow();
                            }
                        }
                    });
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void fail(String reason) {
        if (state == DISCONNECTED) {
            return;
        }
        MqttListener l = listener;
        shutdown();
        l.connectionLost(reason);
    }

    private void shutdown() {
        state = DISCONNECTED;
        generation++;
        pendingSubs.clear();
        if (tickerStop != null) {
            tickerStop.set(true);
            tickerStop = null;
        }
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }
}
