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
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Byte exact checks of the packet codec against MQTT 3.1.1 vectors.
class MqttCodecTest {
    private static byte[] b(int... v) {
        byte[] r = new byte[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = (byte) v[i];
        }
        return r;
    }

    @Test
    void remainingLengthBoundaries() throws IOException {
        int[] values = {0, 127, 128, 16383, 16384, 2097151, 2097152, 268435455};
        byte[][] wire = {b(0x00), b(0x7F), b(0x80, 0x01), b(0xFF, 0x7F), b(0x80, 0x80, 0x01),
            b(0xFF, 0xFF, 0x7F), b(0x80, 0x80, 0x80, 0x01), b(0xFF, 0xFF, 0xFF, 0x7F)};
        for (int i = 0; i < values.length; i++) {
            assertArrayEquals(wire[i], MqttCodec.encodeRemainingLength(values[i]), "encode " + values[i]);
            byte[] pkt = new byte[1 + wire[i].length];
            pkt[0] = (byte) 0x30;
            System.arraycopy(wire[i], 0, pkt, 1, wire[i].length);
            // header only: the packet is incomplete until the body arrives, unless it is empty
            int total = MqttCodec.packetLength(pkt, 0, pkt.length, MqttCodec.MAX_REMAINING_LENGTH);
            assertEquals(values[i] == 0 ? pkt.length : -1, total);
        }
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                MqttCodec.encodeRemainingLength(268435456);
            }
        });
    }

    @Test
    void remainingLengthFiveBytesIsMalformed() {
        final byte[] bad = b(0x30, 0x80, 0x80, 0x80, 0x80, 0x01);
        assertThrows(MqttException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                MqttCodec.packetLength(bad, 0, bad.length, MqttCodec.MAX_REMAINING_LENGTH);
            }
        });
        assertThrows(MqttException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                MqttCodec.read(new ByteArrayInputStream(bad), MqttCodec.MAX_REMAINING_LENGTH);
            }
        });
    }

    @Test
    void maxPacketGuard() {
        final byte[] big = b(0x30, 0x80, 0x80, 0x01);
        assertThrows(MqttException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                MqttCodec.read(new ByteArrayInputStream(big), 1000);
            }
        });
    }

    @Test
    void connectIsByteExact() throws IOException {
        MqttOptions o = new MqttOptions("tcp://h", "abc").keepAliveSeconds(60);
        byte[] expected = b(0x10, 0x0F, 0x00, 0x04, 'M', 'Q', 'T', 'T', 0x04, 0x02, 0x00, 0x3C,
                0x00, 0x03, 'a', 'b', 'c');
        assertArrayEquals(expected, MqttCodec.encode(MqttPacket.connect(o)));
    }

    @Test
    void connectWithCredentialsAndWillRoundTrips() throws IOException {
        MqttOptions o = new MqttOptions("tcp://h", "cid").cleanSession(false).keepAliveSeconds(30)
                .credentials("user", b(1, 2, 3)).will("w/t", b(9, 8), 1, true);
        byte[] wire = MqttCodec.encode(MqttPacket.connect(o));
        // flags: user 0x80 | pass 0x40 | will retain 0x20 | will qos1 0x08 | will 0x04 = 0xEC
        assertEquals(0xEC, wire[9] & 0xff);
        MqttPacket p = MqttCodec.read(new ByteArrayInputStream(wire), 1000);
        assertEquals(MqttPacket.CONNECT, p.getType());
        assertEquals("cid", p.getClientId());
        assertFalse(p.isCleanSession());
        assertEquals(30, p.getKeepAlive());
        assertEquals("user", p.getUsername());
        assertArrayEquals(b(1, 2, 3), p.getPassword());
        assertEquals("w/t", p.getWillTopic());
        assertArrayEquals(b(9, 8), p.getWillPayload());
        assertEquals(1, p.getWillQos());
        assertTrue(p.isWillRetain());
    }

    @Test
    void connackSpecVector() throws IOException {
        MqttPacket p = MqttCodec.read(new ByteArrayInputStream(b(0x20, 0x02, 0x01, 0x00)), 100);
        assertEquals(MqttPacket.CONNACK, p.getType());
        assertTrue(p.isSessionPresent());
        assertEquals(0, p.getReturnCode());
        assertArrayEquals(b(0x20, 0x02, 0x00, 0x05), MqttCodec.encode(MqttPacket.connack(false, 5)));
    }

    @Test
    void publishQos1RoundTrip() throws IOException {
        MqttPacket p = MqttPacket.publish("a/b", b(0x68, 0x69), 1, true, true, 10);
        byte[] wire = MqttCodec.encode(p);
        assertArrayEquals(b(0x3B, 0x09, 0x00, 0x03, 'a', '/', 'b', 0x00, 0x0A, 0x68, 0x69), wire);
        MqttPacket q = MqttCodec.read(new ByteArrayInputStream(wire), 100);
        assertEquals("a/b", q.getTopic());
        assertEquals(1, q.getQos());
        assertTrue(q.isRetain());
        assertTrue(q.isDup());
        assertEquals(10, q.getPacketId());
        assertArrayEquals(b(0x68, 0x69), q.getPayload());
    }

    @Test
    void publishQos0HasNoPacketIdAndUtf8Topic() throws IOException {
        MqttPacket p = MqttPacket.publish("caf\u00e9", new byte[0], 0, false, false, 0);
        byte[] wire = MqttCodec.encode(p);
        assertArrayEquals(b(0x30, 0x07, 0x00, 0x05, 'c', 'a', 'f', 0xC3, 0xA9), wire);
        assertEquals("caf\u00e9", MqttCodec.read(new ByteArrayInputStream(wire), 100).getTopic());
    }

    @Test
    void subscribeSubackUnsubscribe() throws IOException {
        byte[] sub = MqttCodec.encode(MqttPacket.subscribe(1, new String[] {"a/+"}, new int[] {1}));
        assertArrayEquals(b(0x82, 0x08, 0x00, 0x01, 0x00, 0x03, 'a', '/', '+', 0x01), sub);
        MqttPacket s = MqttCodec.read(new ByteArrayInputStream(sub), 100);
        assertEquals("a/+", s.getFilters()[0]);
        assertEquals(1, s.getQosList()[0]);

        byte[] ack = MqttCodec.encode(MqttPacket.suback(1, new int[] {1, 0x80}));
        assertArrayEquals(b(0x90, 0x04, 0x00, 0x01, 0x01, 0x80), ack);
        assertEquals(0x80, MqttCodec.read(new ByteArrayInputStream(ack), 100).getQosList()[1]);

        byte[] un = MqttCodec.encode(MqttPacket.unsubscribe(2, new String[] {"x"}));
        assertArrayEquals(b(0xA2, 0x05, 0x00, 0x02, 0x00, 0x01, 'x'), un);
        assertEquals("x", MqttCodec.read(new ByteArrayInputStream(un), 100).getFilters()[0]);
        assertArrayEquals(b(0xB0, 0x02, 0x00, 0x02), MqttCodec.encode(MqttPacket.unsuback(2)));
        assertArrayEquals(b(0x40, 0x02, 0x12, 0x34), MqttCodec.encode(MqttPacket.puback(0x1234)));
    }

    @Test
    void emptyPackets() throws IOException {
        assertArrayEquals(b(0xC0, 0x00), MqttCodec.encode(MqttPacket.pingreq()));
        assertArrayEquals(b(0xD0, 0x00), MqttCodec.encode(MqttPacket.pingresp()));
        assertArrayEquals(b(0xE0, 0x00), MqttCodec.encode(MqttPacket.disconnect()));
        assertEquals(MqttPacket.PINGRESP, MqttCodec.read(new ByteArrayInputStream(b(0xD0, 0x00)), 10).getType());
    }

    @Test
    void packetsStreamBackToBackAndEofIsReported() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MqttCodec.write(MqttPacket.pingreq(), out);
        MqttCodec.write(MqttPacket.puback(7), out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(MqttPacket.PINGREQ, MqttCodec.read(in, 10).getType());
        assertEquals(7, MqttCodec.read(in, 10).getPacketId());
        try {
            MqttCodec.read(in, 10);
            fail("expected EOF");
        } catch (EOFException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    void malformedAndUnsupportedPacketsAreRejected() {
        expectMqtt(b(0x62, 0x02, 0x00, 0x01)); // PUBREL: QoS 2
        expectMqtt(b(0x34, 0x05, 0x00, 0x01, 'a', 0x00, 0x01)); // PUBLISH at QoS 2
        expectMqtt(b(0x36, 0x03, 0x00, 0x01, 'a')); // PUBLISH QoS 3
        expectMqtt(b(0x41, 0x02, 0x00, 0x01)); // PUBACK with a flag
        expectMqtt(b(0x40, 0x02, 0x00, 0x00)); // packet id 0
        expectMqtt(b(0xC0, 0x01, 0x00)); // PINGREQ with a body
        expectMqtt(b(0x00, 0x00)); // reserved type
        expectMqtt(b(0x30, 0x02, 0x00, 0x05)); // truncated topic
    }

    private static void expectMqtt(final byte[] wire) {
        assertThrows(MqttException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                MqttCodec.read(new ByteArrayInputStream(wire), 1000);
            }
        });
    }

    @Test
    void encodeRejectsInvalidInput() {
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                MqttCodec.encode(MqttPacket.publish("a", new byte[0], 1, false, false, 0));
            }
        });
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                MqttCodec.encode(MqttPacket.publish("a\u0000b", new byte[0], 0, false, false, 0));
            }
        });
    }

    @Test
    void packetLengthWaitsForTheWholePacket() throws IOException {
        byte[] wire = MqttCodec.encode(MqttPacket.publish("t", b(1, 2, 3), 0, false, false, 0));
        for (int n = 0; n < wire.length; n++) {
            assertEquals(-1, MqttCodec.packetLength(wire, 0, n, 1000), "prefix " + n);
        }
        assertEquals(wire.length, MqttCodec.packetLength(wire, 0, wire.length, 1000));
    }
}
