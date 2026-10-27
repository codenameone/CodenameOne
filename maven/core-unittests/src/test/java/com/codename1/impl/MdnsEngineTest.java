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
package com.codename1.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Engines on an in-memory multicast segment: every packet one sends reaches
/// every engine, the sender included, as on a real link with loopback on.
class MdnsEngineTest {

    private final List<Node> nodes = new CopyOnWriteArrayList<Node>();
    private final List<Object> handles = new ArrayList<Object>();

    final class Node extends MdnsEngine {
        final String host;
        final String address;
        final BlockingQueue<byte[]> inbox = new LinkedBlockingQueue<byte[]>();
        volatile boolean closed;

        Node(String host, String address) {
            this.host = host;
            this.address = address;
            nodes.add(this);
        }

        @Override
        protected int rawOpen() {
            return 3;
        }

        @Override
        protected int rawSend(int handle, byte[] packet, int length) {
            for (Node n : nodes) {
                if (!n.closed) {
                    byte[] copy = new byte[length];
                    System.arraycopy(packet, 0, copy, 0, length);
                    n.inbox.add(copy);
                }
            }
            return length;
        }

        @Override
        protected int rawReceive(int handle, byte[] buffer, int timeoutMs) {
            if (closed) {
                return -9;
            }
            try {
                byte[] p = inbox.poll(timeoutMs, TimeUnit.MILLISECONDS);
                if (p == null) {
                    return 0;
                }
                System.arraycopy(p, 0, buffer, 0, p.length);
                return p.length;
            } catch (InterruptedException e) {
                return -4;
            }
        }

        @Override
        protected String rawHostInfo() {
            return host + "\n" + address + "\n";
        }

        @Override
        protected void rawClose(int handle) {
        }
    }

    static final class Events implements MdnsEngine.Listener {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        @Override
        public void resolved(Object browse, String name, String type, String host, int port,
                Map<String, String> txt) {
            events.add("resolved " + name + " " + type + " " + host + ":" + port + " " + txt);
        }

        @Override
        public void lost(Object browse, String name, String type) {
            events.add("lost " + name + " " + type);
        }

        @Override
        public void failed(Object browse, String message) {
            events.add("failed " + message);
        }

        String next() throws InterruptedException {
            String e = events.poll(10, TimeUnit.SECONDS);
            assertNotNull(e, "no event within 10 s");
            return e;
        }
    }

    @AfterEach
    void stop() {
        for (Node n : nodes) {
            n.closed = true;
        }
    }

    @Test
    void browserResolvesAPublishedService() throws Exception {
        Node device = new Node("thermostat", "192.168.1.20");
        Node phone = new Node("phone", "192.168.1.30");
        Map<String, String> txt = new HashMap<String, String>();
        txt.put("api", "/v1");
        Object published = device.publish("Hall Thermostat", "_cn1demo._tcp.", 8080, txt);
        Events events = new Events();
        Object browse = phone.browse("_cn1demo._tcp", events);
        assertEquals("resolved Hall Thermostat _cn1demo._tcp. 192.168.1.20:8080 {api=/v1}", events.next());

        device.unpublish(published);
        assertEquals("lost Hall Thermostat _cn1demo._tcp.", events.next());
        phone.stopBrowse(browse);
    }

    @Test
    void aServiceStartedAfterTheBrowseIsFound() throws Exception {
        Node device = new Node("gateway", "10.0.0.5");
        Node phone = new Node("phone", "10.0.0.6");
        Events events = new Events();
        phone.browse("_gw._tcp.local.", events);
        Thread.sleep(300);
        device.publish("Gateway", "_gw._tcp", 443, null);
        assertEquals("resolved Gateway _gw._tcp. 10.0.0.5:443 {}", events.next());
    }

    @Test
    void aTakenNameIsRenamed() throws Exception {
        Node first = new Node("board-a", "10.0.0.10");
        Node second = new Node("board-b", "10.0.0.11");
        Object a = first.publish("Sensor", "_sensor._udp", 5000, null);
        Events events = new Events();
        first.browse("_sensor._udp", events);
        assertTrue(events.next().startsWith("resolved Sensor "));
        Object b = second.publish("Sensor", "_sensor._udp", 5001, null);
        assertEquals("resolved Sensor (2) _sensor._udp. 10.0.0.11:5001 {}", events.next());
        assertEquals("Sensor", first.publishedName(a));
        assertEquals("Sensor (2)", second.publishedName(b));
    }

    @Test
    void namesWithDotsAndUnicodeSurviveTheWire() throws Exception {
        Node device = new Node("kiosk", "172.16.0.2");
        Node phone = new Node("phone", "172.16.0.3");
        String name = "Lobby v2.1 \u00e9cran";
        device.publish(name, "_kiosk._tcp", 80, null);
        Events events = new Events();
        phone.browse("_kiosk._tcp", events);
        assertEquals("resolved " + name + " _kiosk._tcp. 172.16.0.2:80 {}", events.next());
    }

    @Test
    void malformedPacketsAreIgnored() throws Exception {
        Node device = new Node("dev", "10.1.1.1");
        Node phone = new Node("phone", "10.1.1.2");
        Events events = new Events();
        phone.browse("_x._tcp", events);
        // A compression pointer to itself, a truncated header and noise.
        byte[][] junk = {
            {0, 0, (byte) 0x84, 0, 0, 0, 0, 1, 0, 0, 0, 0, (byte) 0xc0, 12, 0, 12, 0, 1},
            {1, 2, 3},
            new byte[600],
        };
        for (byte[] j : junk) {
            phone.inbox.add(j);
        }
        device.publish("X", "_x._tcp", 1, null);
        assertEquals("resolved X _x._tcp. 10.1.1.1:1 {}", events.next());
    }

    @Test
    void parsesIpv4() {
        assertArrayEquals(new byte[]{10, 0, 0, (byte) 255}, MdnsEngine.parseIpv4("10.0.0.255"));
        assertNull(MdnsEngine.parseIpv4("10.0.0"));
        assertNull(MdnsEngine.parseIpv4("10.0.0.256"));
        assertNull(MdnsEngine.parseIpv4("fe80::1"));
        assertEquals("_a._tcp.local", MdnsEngine.normalizeType("_a._tcp."));
        assertEquals("_a._tcp.local", MdnsEngine.normalizeType("_a._tcp.local."));
    }
}
