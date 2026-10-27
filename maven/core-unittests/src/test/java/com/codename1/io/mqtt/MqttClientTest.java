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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The client against an in-memory broker behind the package private transport.
class MqttClientTest {

    /// A broker and its pipe: what the client sends is decoded and answered
    /// through a queue that the test pumps, so nothing re-enters the client.
    static class FakeBroker implements MqttTransport {
        Handler handler;
        final List<MqttPacket> received = new ArrayList<MqttPacket>();
        final List<byte[]> pending = new ArrayList<byte[]>();
        int connackCode;
        boolean ackPublishes = true;
        boolean answerPings = true;
        boolean closed;
        boolean failWrites;

        public void open(Handler h) {
            handler = h;
            h.opened();
        }

        public void send(byte[] data) throws IOException {
            if (failWrites) {
                throw new IOException("broken pipe");
            }
            MqttPacket p = MqttCodec.read(new ByteArrayInputStream(data), 1 << 20);
            received.add(p);
            switch (p.getType()) {
                case MqttPacket.CONNECT:
                    pending.add(MqttCodec.encode(MqttPacket.connack(false, connackCode)));
                    break;
                case MqttPacket.PUBLISH:
                    if (p.getQos() == 1 && ackPublishes) {
                        pending.add(MqttCodec.encode(MqttPacket.puback(p.getPacketId())));
                    }
                    break;
                case MqttPacket.SUBSCRIBE:
                    int[] codes = new int[p.getFilters().length];
                    for (int i = 0; i < codes.length; i++) {
                        codes[i] = p.getFilters()[i].startsWith("deny") ? 0x80 : p.getQosList()[i];
                    }
                    pending.add(MqttCodec.encode(MqttPacket.suback(p.getPacketId(), codes)));
                    break;
                case MqttPacket.UNSUBSCRIBE:
                    pending.add(MqttCodec.encode(MqttPacket.unsuback(p.getPacketId())));
                    break;
                case MqttPacket.PINGREQ:
                    if (answerPings) {
                        pending.add(MqttCodec.encode(MqttPacket.pingresp()));
                    }
                    break;
                default:
                    break;
            }
        }

        public void close() {
            closed = true;
        }

        /// Delivers every queued answer, split into pieces of `chunk` bytes.
        void pump(int chunk) {
            List<byte[]> copy = new ArrayList<byte[]>(pending);
            pending.clear();
            for (int i = 0; i < copy.size(); i++) {
                push(copy.get(i), chunk);
            }
        }

        void push(byte[] data, int chunk) {
            for (int off = 0; off < data.length; off += chunk) {
                int n = Math.min(chunk, data.length - off);
                byte[] piece = new byte[n];
                System.arraycopy(data, off, piece, 0, n);
                handler.received(piece, n);
            }
        }

        MqttPacket last(int type) {
            for (int i = received.size() - 1; i >= 0; i--) {
                if (received.get(i).getType() == type) {
                    return received.get(i);
                }
            }
            return null;
        }

        int count(int type) {
            int n = 0;
            for (int i = 0; i < received.size(); i++) {
                if (received.get(i).getType() == type) {
                    n++;
                }
            }
            return n;
        }
    }

    static final class Recorder implements MqttListener, MqttMessageListener {
        int connected;
        final List<String> topics = new ArrayList<String>();
        final List<String> payloads = new ArrayList<String>();
        final List<Boolean> retained = new ArrayList<Boolean>();
        final List<String> lost = new ArrayList<String>();

        public void connected() {
            connected++;
        }

        public void messageArrived(String topic, byte[] payload, boolean r) {
            topics.add(topic);
            payloads.add(new String(payload));
            retained.add(Boolean.valueOf(r));
        }

        public void connectionLost(String reason) {
            lost.add(reason);
        }
    }

    private static MqttOptions opts() {
        return new MqttOptions("tcp://broker", "c1").keepAliveSeconds(10);
    }

    @Test
    void connectPublishAckAndSubscribe() throws IOException {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.setClock(1000);
        c.connect(opts(), r);
        assertFalse(c.isConnected());
        b.pump(1000);
        assertTrue(c.isConnected());
        assertEquals(1, r.connected);
        assertEquals("c1", b.last(MqttPacket.CONNECT).getClientId());

        c.publish("t/1", "hi".getBytes("UTF-8"), 1, false);
        assertEquals(1, c.getInFlightCount());
        MqttPacket sent = b.last(MqttPacket.PUBLISH);
        assertEquals(1, sent.getQos());
        assertFalse(sent.isDup());
        b.pump(1000);
        assertEquals(0, c.getInFlightCount());

        c.publish("t/0", new byte[] {1}, 0, true);
        assertEquals(0, b.last(MqttPacket.PUBLISH).getQos());
        assertTrue(b.last(MqttPacket.PUBLISH).isRetain());
        assertEquals(0, c.getInFlightCount());

        c.subscribe("s/#", 1);
        assertEquals("s/#", b.last(MqttPacket.SUBSCRIBE).getFilters()[0]);
        b.pump(1000);
        assertTrue(r.lost.isEmpty());
    }

    @Test
    void incomingQos1IsAcknowledgedAndRoutedEvenWhenSplitIntoBytes() throws IOException {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder all = new Recorder();
        Recorder routed = new Recorder();
        c.connect(opts(), all);
        b.pump(1000);
        c.subscribe("a/+", 1, routed);
        b.pump(1000);

        byte[] one = MqttCodec.encode(MqttPacket.publish("a/x", "p1".getBytes("UTF-8"), 1, true, false, 77));
        byte[] two = MqttCodec.encode(MqttPacket.publish("z", "p2".getBytes("UTF-8"), 0, false, false, 0));
        byte[] both = new byte[one.length + two.length];
        System.arraycopy(one, 0, both, 0, one.length);
        System.arraycopy(two, 0, both, one.length, two.length);
        b.push(both, 1);

        assertEquals(1, b.count(MqttPacket.PUBACK));
        assertEquals(77, b.last(MqttPacket.PUBACK).getPacketId());
        assertEquals("a/x", routed.topics.get(0));
        assertEquals("p1", routed.payloads.get(0));
        assertTrue(routed.retained.get(0).booleanValue());
        assertEquals(1, all.topics.size());
        assertEquals("z", all.topics.get(0));
    }

    @Test
    void unackedQos1IsResentWithDupAfterReconnect() throws IOException {
        FakeBroker b = new FakeBroker();
        b.ackPublishes = false;
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.connect(opts().cleanSession(false), r);
        b.pump(1000);
        c.publish("t", new byte[] {5}, 1, false);
        int id = b.last(MqttPacket.PUBLISH).getPacketId();

        b.push(new byte[] {(byte) 0xE0}, 1); // truncated garbage: not a packet yet
        b.handler.closed("link down");
        assertFalse(c.isConnected());
        assertEquals("link down", r.lost.get(0));
        assertTrue(b.closed);
        assertEquals(1, c.getInFlightCount());

        b.closed = false;
        b.received.clear();
        b.ackPublishes = true;
        c.connect(opts().cleanSession(false), r);
        b.pump(1000);
        assertEquals(2, r.connected);
        MqttPacket again = b.last(MqttPacket.PUBLISH);
        assertNotNull(again);
        assertTrue(again.isDup());
        assertEquals(id, again.getPacketId());
        b.pump(1000);
        assertEquals(0, c.getInFlightCount());
    }

    @Test
    void qos1PublishWhileDisconnectedIsQueuedAndCleanSessionDropsIt() throws IOException {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        c.publish("q", new byte[0], 1, false);
        assertEquals(1, c.getInFlightCount());
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new MqttClient(new FakeBroker()).publish("q", new byte[0], 0, false);
            }
        });
        Recorder r = new Recorder();
        c.connect(opts().cleanSession(false), r);
        b.pump(1000);
        assertEquals(1, b.count(MqttPacket.PUBLISH));
        c.disconnect();
        assertEquals(1, c.getInFlightCount());
        c.connect(opts(), r); // clean session
        assertEquals(0, c.getInFlightCount());
    }

    @Test
    void refusedConnectionReportsConnectionLost() {
        FakeBroker b = new FakeBroker();
        b.connackCode = 5;
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.connect(opts(), r);
        b.pump(1000);
        assertFalse(c.isConnected());
        assertEquals(0, r.connected);
        assertEquals(1, r.lost.size());
        assertTrue(r.lost.get(0).contains("not authorized"));
        assertTrue(b.closed);
    }

    @Test
    void keepAlivePingsThenTimesOut() {
        FakeBroker b = new FakeBroker();
        b.answerPings = false;
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.setClock(0);
        c.connect(opts(), r); // keep alive 10 s
        b.pump(1000);
        c.tick(9000);
        assertEquals(0, b.count(MqttPacket.PINGREQ));
        c.tick(10000);
        assertEquals(1, b.count(MqttPacket.PINGREQ));
        c.tick(20000); // 10 s after the ping: still inside 1.5 intervals
        assertTrue(c.isConnected());
        c.tick(25001);
        assertFalse(c.isConnected());
        assertEquals("Keep alive timed out", r.lost.get(0));
    }

    @Test
    void pingResponseKeepsTheConnectionAndOtherTrafficDefersPings() {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.setClock(0);
        c.connect(opts(), r);
        b.pump(1000);
        c.setClock(5000);
        c.publish("t", new byte[0], 0, false);
        c.tick(10000); // sent 5 s ago: no ping yet
        assertEquals(0, b.count(MqttPacket.PINGREQ));
        c.tick(15000);
        assertEquals(1, b.count(MqttPacket.PINGREQ));
        b.pump(1000);
        c.tick(40000);
        assertEquals(2, b.count(MqttPacket.PINGREQ));
        assertTrue(r.lost.isEmpty());
    }

    @Test
    void connectTimeout() {
        FakeBroker b = new FakeBroker() {
            public void send(byte[] data) {
                // swallow CONNECT: the broker never answers
            }
        };
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.setClock(0);
        c.connect(opts().connectTimeoutMillis(5000), r);
        c.tick(5000);
        assertTrue(r.lost.isEmpty());
        c.tick(5001);
        assertEquals("Connect timed out", r.lost.get(0));
    }

    @Test
    void disconnectSendsDisconnectWithoutConnectionLost() {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.connect(opts(), r);
        b.pump(1000);
        c.disconnect();
        assertNotNull(b.last(MqttPacket.DISCONNECT));
        assertFalse(c.isConnected());
        assertTrue(b.closed);
        assertTrue(r.lost.isEmpty());
        c.disconnect(); // idempotent
    }

    @Test
    void protocolViolationsAndWriteFailuresFailTheConnection() throws IOException {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.connect(opts(), r);
        b.pump(1000);
        b.push(new byte[] {0x62, 0x02, 0x00, 0x01}, 4); // PUBREL
        assertEquals(1, r.lost.size());
        assertTrue(r.lost.get(0).startsWith("Protocol error"));

        FakeBroker b2 = new FakeBroker();
        MqttClient c2 = new MqttClient(b2);
        Recorder r2 = new Recorder();
        c2.connect(opts(), r2);
        b2.pump(1000);
        b2.failWrites = true;
        c2.publish("t", new byte[0], 0, false);
        assertTrue(r2.lost.get(0).startsWith("Write failed"));
    }

    @Test
    void oversizedPacketFailsTheConnection() {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder r = new Recorder();
        c.connect(opts().maxPacketSize(100), r);
        b.pump(1000);
        b.push(new byte[] {0x30, (byte) 0xE8, 0x07}, 3); // 1000 byte body announced
        assertEquals(1, r.lost.size());
    }

    @Test
    void deniedSubscriptionDropsItsRoute() throws IOException {
        FakeBroker b = new FakeBroker();
        MqttClient c = new MqttClient(b);
        Recorder all = new Recorder();
        Recorder routed = new Recorder();
        c.connect(opts(), all);
        b.pump(1000);
        c.subscribe("deny/x", 1, routed);
        b.pump(1000);
        b.push(MqttCodec.encode(MqttPacket.publish("deny/x", new byte[0], 0, false, false, 0)), 100);
        assertTrue(routed.topics.isEmpty());
        assertEquals(1, all.topics.size());
    }

    @Test
    void packetIdsSkipThoseInFlight() {
        FakeBroker b = new FakeBroker();
        b.ackPublishes = false;
        MqttClient c = new MqttClient(b);
        c.connect(opts(), new Recorder());
        b.pump(1000);
        c.publish("t", new byte[0], 1, false);
        c.publish("t", new byte[0], 1, false);
        assertEquals(1, b.received.get(1).getPacketId());
        assertEquals(2, b.received.get(2).getPacketId());
    }

    @Test
    void unsupportedSchemeIsRejected() {
        final MqttClient c = new MqttClient();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                c.connect(new MqttOptions("http://x", "id"), new Recorder());
            }
        });
    }
}
