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
package com.codename1.backend;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Where a request came from: the connection's other end, and what the
/// forwarding headers say when -- and only when -- that end is a proxy this
/// server was told to believe.
class RemoteAddressTest {
    private static String text(String address) {
        return ForwardedHeaders.format(ForwardedHeaders.parse(address));
    }

    @Test
    @DisplayName("Addresses are read in every way they are written, and written one way")
    void addresses() {
        assertEquals("192.0.2.1", text("192.0.2.1"));
        assertEquals("0.0.0.0", text("0.0.0.0"));
        assertEquals("255.255.255.255", text("255.255.255.255"));
        // A port after IPv4, as some proxies write into X-Forwarded-For.
        assertEquals("192.0.2.1", text("192.0.2.1:49152"));
        assertEquals("2001:db8:0:0:0:0:0:1", text("2001:db8::1"));
        assertEquals("2001:db8:0:0:0:0:0:1", text("2001:DB8:0:0:0:0:0:1"));
        assertEquals("2001:db8:0:0:0:0:0:1", text("[2001:db8::1]"));
        assertEquals("2001:db8:0:0:0:0:0:1", text("[2001:db8::1]:8443"));
        assertEquals("0:0:0:0:0:0:0:1", text("::1"));
        assertEquals("0:0:0:0:0:0:0:0", text("::"));
        assertEquals("fe80:0:0:0:0:0:0:0", text("fe80::"));
        assertEquals("1:2:3:4:5:6:7:8", text("1:2:3:4:5:6:7:8"));
        assertEquals("1:0:0:0:0:0:7:8", text("1::7:8"));
        // IPv4 carried in IPv6 is the IPv4 address: one client, one key.
        assertEquals("192.0.2.1", text("::ffff:192.0.2.1"));
        assertEquals("192.0.2.1", text("::ffff:c000:201"));
        assertEquals("192.0.2.1", ForwardedHeaders.format(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            (byte) 0xff, (byte) 0xff, (byte) 192, 0, 2, 1}));
        assertEquals("64:ff9b:0:0:0:0:c000:201", text("64:ff9b::192.0.2.1"));
        assertNull(ForwardedHeaders.format(null));

        String[] bad = {"", " ", "unknown", "_hidden", "1.2.3", "1.2.3.4.5", "256.1.1.1", "1.2.3.-4",
            "1..2.3", "01234.1.1.1", "1.2.3.4x", ":::", "1:2:3:4:5:6:7", "1:2:3:4:5:6:7:8:9",
            "1::2::3", "12345::1", "g::1", ":1:2:3:4:5:6:7", "1:2:3:4:5:6:7:", "[::1", "::1.2.3",
            "1:2:3:4:5:6:7:1.2.3.4", "1.2.3.4:5:6", null};
        for (String address : bad) {
            assertNull(ForwardedHeaders.parse(address), String.valueOf(address));
        }
    }

    @Test
    void mappedNativePeersUseTheIpv4ProxyPolicy() throws Exception {
        ForwardedHeaders policy = ForwardedHeaders.of("10.0.0.0/8");
        byte[] proxy = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 255, (byte) 255, 10, 0, 0, 1};
        assertTrue(policy.trusts(proxy));
        assertTrue(policy.secure(proxy, "https"));
        assertEquals("203.0.113.7", ForwardedHeaders.format(policy.client(proxy, "203.0.113.7")));
        proxy[12] = 11;
        assertFalse(policy.trusts(proxy));
        assertFalse(policy.secure(proxy, "https"));
        assertArrayEquals(proxy, policy.client(proxy, "203.0.113.7"));
    }

    @Test
    @DisplayName("Proxies: no trusted addresses unless explicitly listed")
    void trustedRanges() throws Exception {
        Properties settings = new Properties();
        assertNull(ForwardedHeaders.fromConfig(Config.of(settings, "test")),
                "the headers are not read unless asked for");
        settings.setProperty(ForwardedHeaders.ENABLED, "true");
        ForwardedHeaders internal = ForwardedHeaders.fromConfig(Config.of(settings, "test"));
        String[] trusted = {"127.0.0.1", "127.255.255.254", "10.0.0.1", "10.255.255.255", "172.16.0.1",
            "172.31.255.255", "192.168.1.1", "169.254.10.10", "::1", "fd00::1", "fc00::1",
            "fe80::1", "febf::1", "::ffff:10.0.0.1"};
        for (String address : trusted) {
            assertFalse(internal.trusts(ForwardedHeaders.parse(address)), address);
        }
        String[] untrusted = {"8.8.8.8", "11.0.0.1", "9.255.255.255", "172.15.255.255", "172.32.0.1",
            "192.169.0.1", "128.0.0.1", "::2", "2001:db8::1", "fec0::1", "fb00::1", "fe00::1"};
        for (String address : untrusted) {
            assertFalse(internal.trusts(ForwardedHeaders.parse(address)), address);
        }
        assertFalse(internal.trusts(null));

        settings.setProperty(ForwardedHeaders.TRUSTED, " 203.0.113.10 , 198.51.100.0/24,2001:db8:1::/48 ");
        ForwardedHeaders listed = ForwardedHeaders.fromConfig(Config.of(settings, "test"));
        assertTrue(listed.trusts(ForwardedHeaders.parse("203.0.113.10")));
        assertFalse(listed.trusts(ForwardedHeaders.parse("203.0.113.11")));
        assertTrue(listed.trusts(ForwardedHeaders.parse("198.51.100.255")));
        assertFalse(listed.trusts(ForwardedHeaders.parse("198.51.101.0")));
        assertTrue(listed.trusts(ForwardedHeaders.parse("2001:db8:1:ffff::9")));
        assertFalse(listed.trusts(ForwardedHeaders.parse("2001:db8:2::9")));
        // A list is the whole of it: loopback is no longer trusted by default.
        assertFalse(listed.trusts(ForwardedHeaders.parse("127.0.0.1")));
        assertFalse(listed.trusts(ForwardedHeaders.parse("10.0.0.1")));
        assertTrue(ForwardedHeaders.of("0.0.0.0/0").trusts(ForwardedHeaders.parse("8.8.8.8")));
        assertFalse(ForwardedHeaders.of("0.0.0.0/0").trusts(ForwardedHeaders.parse("::1")));

        assertEquals("cn1.server.trustedProxies: not an address or a CIDR range: 10.0.0.0/33",
                assertThrows(IOException.class, () -> ForwardedHeaders.of("10.0.0.0/33")).getMessage());
        assertEquals("cn1.server.trustedProxies: not an address or a CIDR range: loadbalancer",
                assertThrows(IOException.class, () -> ForwardedHeaders.of("10.0.0.1,loadbalancer"))
                        .getMessage());
        assertThrows(IOException.class, () -> ForwardedHeaders.of("10.0.0.0/x"));
    }

    @Test
    @DisplayName("X-Forwarded-For is read from the right, past proxies, and never from a stranger")
    void forwardedFor() throws Exception {
        ForwardedHeaders policy = ForwardedHeaders.of("10.0.0.0/8,127.0.0.1");
        byte[] proxy = ForwardedHeaders.parse("10.0.0.1");
        byte[] stranger = ForwardedHeaders.parse("198.51.100.77");
        // From a proxy: what the proxy says.
        assertEquals("203.0.113.7", ForwardedHeaders.format(policy.client(proxy, "203.0.113.7")));
        // Two proxies in a row: the inner one is skipped.
        assertEquals("203.0.113.7", ForwardedHeaders.format(
                policy.client(proxy, "203.0.113.7, 10.0.0.2")));
        // What the client put on the left is never reached.
        assertEquals("203.0.113.7", ForwardedHeaders.format(
                policy.client(proxy, "1.1.1.1, 127.0.0.1, 203.0.113.7, 10.0.0.2")));
        assertEquals("203.0.113.7", ForwardedHeaders.format(
                policy.client(proxy, "10.9.9.9,203.0.113.7")));
        // Every entry a proxy: the outermost is as far as anyone can say.
        assertEquals("10.0.0.3", ForwardedHeaders.format(policy.client(proxy, "10.0.0.3, 10.0.0.2")));
        // From anyone else, the header is the client's claim about itself.
        assertArrayEquals(stranger, policy.client(stranger, "203.0.113.7"));
        assertArrayEquals(stranger, policy.client(stranger, "127.0.0.1"));
        // No header, or one that is not addresses.
        assertArrayEquals(proxy, policy.client(proxy, null));
        assertArrayEquals(proxy, policy.client(proxy, ""));
        assertArrayEquals(proxy, policy.client(proxy, "unknown"));
        assertArrayEquals(proxy, policy.client(proxy, "203.0.113.7, <script>"));
        assertEquals("10.0.0.2", ForwardedHeaders.format(
                policy.client(proxy, "203.0.113.7, garbage, 10.0.0.2")));
        assertEquals("2001:db8:0:0:0:0:0:1", ForwardedHeaders.format(
                policy.client(proxy, "2001:DB8::1")));
        assertEquals("203.0.113.7", ForwardedHeaders.format(
                policy.client(proxy, "203.0.113.7:51000")));
        assertNull(policy.client(null, "203.0.113.7"));

        assertTrue(policy.secure(proxy, "https"));
        assertTrue(policy.secure(proxy, "HTTPS"));
        assertTrue(policy.secure(proxy, "https, https"));
        assertFalse(policy.secure(proxy, "http"));
        assertFalse(policy.secure(proxy, "https, http"));
        assertFalse(policy.secure(proxy, ""));
        assertFalse(policy.secure(proxy, null));
        assertFalse(policy.secure(stranger, "https"));
    }

    private static final HttpServer.Handler WHO = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, request.getRemoteAddress() + "|"
                    + request.getPeerAddress() + "|" + request.isSecure());
        }
    };

    private static String ask(int port, String... headerLines) throws IOException {
        StringBuilder request = new StringBuilder("GET /who HTTP/1.1\r\nHost: localhost\r\n"
                + "Connection: close\r\n");
        for (String line : headerLines) {
            request.append(line).append("\r\n");
        }
        request.append("\r\n");
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(10000);
            socket.getOutputStream().write(request.toString().getBytes("ISO-8859-1"));
            socket.getOutputStream().flush();
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            for (int read = in.read(buffer) ; read > 0 ; read = in.read(buffer)) {
                all.write(buffer, 0, read);
            }
            String response = new String(all.toByteArray(), "UTF-8");
            return response.substring(response.indexOf("\r\n\r\n") + 4);
        }
    }

    private static Backend start(Properties settings) throws Exception {
        settings.setProperty(Config.SERVER_PORT, "0");
        return Backend.builder(Config.of(settings, "test")).quiet().host("127.0.0.1").handler(WHO)
                .start();
    }

    @Test
    @DisplayName("A request knows the other end of its connection, and ignores headers by default")
    void peerByDefault() throws Exception {
        Backend backend = start(new Properties());
        try {
            int port = backend.getServer().getPort();
            assertEquals("127.0.0.1|127.0.0.1|false", ask(port));
            // Nobody was told to believe these.
            assertEquals("127.0.0.1|127.0.0.1|false", ask(port, "X-Forwarded-For: 203.0.113.7",
                    "X-Forwarded-Proto: https"));
        } finally {
            backend.stop();
        }
        // A request that arrived on no connection has no address, and says so.
        HttpServer.Response inProcess = com.codename1.impl.backend.BackendAccess.get().dispatch(
                backend = start(new Properties()), "GET", "/who",
                new java.util.LinkedHashMap<String, String>(), null);
        try {
            assertEquals("null|null|false", new String(
                    com.codename1.impl.backend.BackendAccess.get().body(inProcess), "UTF-8"));
        } finally {
            backend.stop();
        }
    }

    @Test
    void enablingForwardingWithoutProxyAddressesDoesNotTrustLoopbackClients() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(ForwardedHeaders.ENABLED, "true");
        for (String list : new String[] {null, "", "  "}) {
            if (list != null) {
                settings.setProperty(ForwardedHeaders.TRUSTED, list);
            }
            Backend backend = start(settings);
            try {
                assertEquals("127.0.0.1|127.0.0.1|false", ask(backend.getServer().getPort(),
                        "X-Forwarded-For: 203.0.113.7", "X-Forwarded-Proto: https"));
            } finally {
                backend.stop();
            }
        }
    }

    @Test
    @DisplayName("Behind a trusted proxy the forwarding headers are the client; behind none, not")
    void behindAProxy() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(ForwardedHeaders.ENABLED, "true");
        // This deployment explicitly trusts its proxy hops.
        settings.setProperty(ForwardedHeaders.TRUSTED, "127.0.0.1,10.0.0.0/8");
        Backend backend = start(settings);
        try {
            int port = backend.getServer().getPort();
            assertEquals("127.0.0.1|127.0.0.1|false", ask(port));
            assertEquals("203.0.113.7|127.0.0.1|true", ask(port,
                    "X-Forwarded-For: 198.51.100.9, 203.0.113.7, 10.1.2.3", "X-Forwarded-Proto: https"));
            assertEquals("203.0.113.7|127.0.0.1|false", ask(port, "X-Forwarded-For: 203.0.113.7",
                    "X-Forwarded-Proto: http"));
            assertEquals("2001:db8:0:0:0:0:0:1|127.0.0.1|false",
                    ask(port, "X-Forwarded-For: 2001:db8::1"));
            assertEquals("127.0.0.1|127.0.0.1|false", ask(port, "X-Forwarded-For: not an address"));
        } finally {
            backend.stop();
        }
        // The same server told its proxies are elsewhere: this connection is a
        // stranger's, and its headers are its own claim.
        settings.setProperty(ForwardedHeaders.TRUSTED, "10.0.0.0/8");
        backend = start(settings);
        try {
            assertEquals("127.0.0.1|127.0.0.1|false", ask(backend.getServer().getPort(),
                    "X-Forwarded-For: 203.0.113.7", "X-Forwarded-Proto: https"));
        } finally {
            backend.stop();
        }
        // A list that is not one stops the server from starting.
        final Properties bad = new Properties();
        bad.setProperty(ForwardedHeaders.ENABLED, "true");
        bad.setProperty(ForwardedHeaders.TRUSTED, "the-load-balancer");
        assertEquals("cn1.server.trustedProxies: not an address or a CIDR range: the-load-balancer",
                assertThrows(IOException.class, () -> start(bad)).getMessage());
    }
}
