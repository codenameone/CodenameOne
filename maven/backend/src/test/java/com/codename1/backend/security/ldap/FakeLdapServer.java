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
package com.codename1.backend.security.ldap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A directory for the tests: a socket speaking the LDAPv3 subset the client
/// uses, over entries held in memory. Written against RFC 4511 with the JDK
/// alone, so it shares no code with the client it tests.
public final class FakeLdapServer implements AutoCloseable {
    public static final class Entry {
        final String dn;
        final Map<String, List<String>> attrs = new LinkedHashMap<>();

        public Entry(String dn, String... pairs) {
            this.dn = dn;
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                attrs.computeIfAbsent(pairs[i].toLowerCase(java.util.Locale.ROOT),
                        k -> new ArrayList<>()).add(pairs[i + 1]);
            }
        }
    }

    public final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    public final Map<String, String> passwords = Collections.synchronizedMap(new LinkedHashMap<>());
    /// Every bind seen, as `dn / password`.
    final List<String> binds = Collections.synchronizedList(new ArrayList<>());
    /// Every search's filter, as raw BER hex.
    final List<String> filters = Collections.synchronizedList(new ArrayList<>());
    /// Diagnostic message for a refused bind, by DN (Active Directory sub-errors).
    final Map<String, String> bindDiagnostics = Collections.synchronizedMap(new LinkedHashMap<>());
    volatile boolean acceptEmptyPassword = true;

    private final ServerSocket socket;
    private final Thread thread;

    public FakeLdapServer() throws IOException {
        current = this;
        socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        thread = new Thread(this::serve, "fake-ldap");
        thread.setDaemon(true);
        thread.start();
    }

    public String url() {
        return "ldap://127.0.0.1:" + socket.getLocalPort();
    }

    private void serve() {
        while (!socket.isClosed()) {
            try {
                Socket s = socket.accept();
                Thread t = new Thread(() -> session(s), "fake-ldap-session");
                t.setDaemon(true);
                t.start();
            } catch (IOException closed) {
                return;
            }
        }
    }

    // ---------------------------------------------------------------- BER

    static final class Tlv {
        final int tag;
        final byte[] value;

        Tlv(int tag, byte[] value) {
            this.tag = tag;
            this.value = value;
        }

        List<Tlv> children() {
            List<Tlv> out = new ArrayList<>();
            int[] pos = {0};
            while (pos[0] < value.length) {
                out.add(read(value, pos));
            }
            return out;
        }

        String str() {
            return new String(value, StandardCharsets.UTF_8);
        }

        int num() {
            int n = 0;
            for (byte b : value) {
                n = (n << 8) | (b & 0xff);
            }
            return n;
        }
    }

    static Tlv read(byte[] data, int[] pos) {
        int tag = data[pos[0]++] & 0xff;
        int len = data[pos[0]++] & 0xff;
        if (len >= 0x80) {
            int n = len & 0x7f;
            len = 0;
            for (int i = 0; i < n; i++) {
                len = (len << 8) | (data[pos[0]++] & 0xff);
            }
        }
        byte[] v = new byte[len];
        System.arraycopy(data, pos[0], v, 0, len);
        pos[0] += len;
        return new Tlv(tag, v);
    }

    static Tlv readStream(InputStream in) throws IOException {
        int tag = in.read();
        if (tag < 0) {
            return null;
        }
        int len = in.read();
        if (len >= 0x80) {
            int n = len & 0x7f;
            len = 0;
            for (int i = 0; i < n; i++) {
                len = (len << 8) | in.read();
            }
        }
        byte[] v = new byte[len];
        int at = 0;
        while (at < len) {
            int r = in.read(v, at, len - at);
            if (r < 0) {
                return null;
            }
            at += r;
        }
        return new Tlv(tag, v);
    }

    static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            body.write(p, 0, p.length);
        }
        byte[] b = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (b.length < 0x80) {
            out.write(b.length);
        } else {
            out.write(0x84);
            out.write(b.length >>> 24);
            out.write(b.length >>> 16);
            out.write(b.length >>> 8);
            out.write(b.length);
        }
        out.write(b, 0, b.length);
        return out.toByteArray();
    }

    static byte[] str(int tag, String s) {
        return tlv(tag, s.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] integer(int tag, int n) {
        return n < 0x80 ? tlv(tag, new byte[] {(byte) n})
                : tlv(tag, new byte[] {0, (byte) (n >> 8), (byte) n});
    }

    static byte[] result(int tag, int code, String diagnostic) {
        return tlv(tag, integer(0x0a, code), str(0x04, ""), str(0x04, diagnostic));
    }

    // ---------------------------------------------------------------- protocol

    private void session(Socket s) {
        try (Socket sock = s) {
            InputStream in = sock.getInputStream();
            OutputStream out = sock.getOutputStream();
            while (true) {
                Tlv message = readStream(in);
                if (message == null) {
                    return;
                }
                List<Tlv> parts = message.children();
                byte[] id = integer(0x02, parts.get(0).num());
                Tlv op = parts.get(1);
                if (op.tag == 0x42) {
                    return;
                }
                if (op.tag == 0x60) {
                    List<Tlv> bind = op.children();
                    String dn = bind.get(1).str();
                    String pw = bind.get(2).str();
                    binds.add(dn + " / " + pw);
                    int code;
                    String diag = "";
                    if (dn.isEmpty() || (pw.isEmpty() && acceptEmptyPassword)) {
                        code = 0;
                    } else if (pw.equals(passwords.get(dn))) {
                        code = 0;
                    } else {
                        code = 49;
                        diag = bindDiagnostics.getOrDefault(dn, "");
                    }
                    out.write(tlv(0x30, id, result(0x61, code, diag)));
                } else if (op.tag == 0x63) {
                    List<Tlv> req = op.children();
                    String base = req.get(0).str();
                    int scope = req.get(1).num();
                    int sizeLimit = req.get(3).num();
                    Tlv filter = req.get(6);
                    filters.add(hex(filter));
                    int sent = 0;
                    List<Entry> snapshot;
                    synchronized (entries) {
                        snapshot = new ArrayList<>(entries);
                    }
                    for (Entry e : snapshot) {
                        if (!inScope(e.dn, base, scope) || !matches(e, filter)) {
                            continue;
                        }
                        if (sizeLimit > 0 && sent == sizeLimit) {
                            out.write(tlv(0x30, id, result(0x65, 4, "size limit")));
                            sent = -1;
                            break;
                        }
                        List<byte[]> attrs = new ArrayList<>();
                        for (Map.Entry<String, List<String>> a : e.attrs.entrySet()) {
                            List<byte[]> vals = new ArrayList<>();
                            for (String v : a.getValue()) {
                                vals.add(str(0x04, v));
                            }
                            attrs.add(tlv(0x30, str(0x04, a.getKey()),
                                    tlv(0x31, vals.toArray(new byte[0][]))));
                        }
                        out.write(tlv(0x30, id, tlv(0x64, str(0x04, e.dn),
                                tlv(0x30, attrs.toArray(new byte[0][])))));
                        sent++;
                    }
                    if (sent >= 0) {
                        out.write(tlv(0x30, id, result(0x65, 0, "")));
                    }
                } else {
                    out.write(tlv(0x30, id, result(0x78, 2, "unsupported")));
                }
                out.flush();
            }
        } catch (IOException | RuntimeException ended) {
            // The client went away.
        }
    }

    private static boolean inScope(String dn, String base, int scope) {
        String d = dn.toLowerCase(java.util.Locale.ROOT);
        String b = base.toLowerCase(java.util.Locale.ROOT);
        if (scope == 0) {
            return d.equals(b);
        }
        if (b.isEmpty()) {
            return true;
        }
        if (!d.endsWith("," + b)) {
            return false;
        }
        return scope == 2 || d.substring(0, d.length() - b.length() - 1).indexOf(',') < 0;
    }

    private static boolean matches(Entry e, Tlv f) {
        switch (f.tag) {
            case 0xa0:
                for (Tlv c : f.children()) {
                    if (!matches(e, c)) {
                        return false;
                    }
                }
                return true;
            case 0xa1:
                for (Tlv c : f.children()) {
                    if (matches(e, c)) {
                        return true;
                    }
                }
                return false;
            case 0xa2:
                return !matches(e, f.children().get(0));
            case 0x87:
                return f.str().equalsIgnoreCase("objectClass")
                        || e.attrs.containsKey(f.str().toLowerCase(java.util.Locale.ROOT));
            case 0xa3: {
                List<Tlv> ava = f.children();
                List<String> vals = e.attrs.get(ava.get(0).str().toLowerCase(java.util.Locale.ROOT));
                if (vals == null) {
                    return false;
                }
                for (String v : vals) {
                    if (v.equalsIgnoreCase(ava.get(1).str())) {
                        return true;
                    }
                }
                return false;
            }
            case 0xa9: {
                // Extensible match; only Active Directory's in-chain rule.
                String rule = null;
                String type = null;
                String value = null;
                for (Tlv c : f.children()) {
                    if (c.tag == 0x81) {
                        rule = c.str();
                    } else if (c.tag == 0x82) {
                        type = c.str();
                    } else if (c.tag == 0x83) {
                        value = c.str();
                    }
                }
                lastExtensible = rule + " " + type + " " + value;
                return "1.2.840.113556.1.4.1941".equals(rule) && type != null
                        && inChain(e, type.toLowerCase(java.util.Locale.ROOT), value, 0);
            }
            default:
                return false;
        }
    }

    /// The last extensible match a search asked for: "rule type value".
    public static volatile String lastExtensible;

    private boolean inChainOf(String dn, String type, String target, int depth) {
        synchronized (entries) {
            for (Entry g : entries) {
                if (g.dn.equalsIgnoreCase(dn)) {
                    return inChain(g, type, target, depth + 1);
                }
            }
        }
        return false;
    }

    private static FakeLdapServer current;

    private static boolean inChain(Entry e, String type, String target, int depth) {
        List<String> vals = e.attrs.get(type);
        if (vals == null || depth > 8) {
            return false;
        }
        for (String v : vals) {
            if (v.equalsIgnoreCase(target)
                    || (current != null && current.inChainOf(v, type, target, depth))) {
                return true;
            }
        }
        return false;
    }

    static String hex(Tlv t) {
        StringBuilder b = new StringBuilder();
        for (byte x : tlv(t.tag, t.value)) {
            b.append(String.format("%02x", x));
        }
        return b.toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
