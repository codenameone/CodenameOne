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

import com.codename1.backend.Tcp;
import com.codename1.backend.security.pki.Asn1;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// One LDAPv3 session (RFC 4511) on one socket: simple bind, search, StartTLS
/// and unbind -- what signing users in against a directory needs, and no more.
/// Not shared between threads; open one per sign-in, as [LdapContextSource]
/// does, and close it.
///
/// Only simple binds are made, so the password travels as the server reads
/// it: over plain `ldap://` without StartTLS, anyone on the network path reads
/// every password. Use `ldaps://`, or StartTLS.
public final class LdapConnection implements AutoCloseable {
    /// The scope of a search: the base entry alone.
    public static final int SCOPE_BASE = 0;
    /// The scope of a search: the base entry's children.
    public static final int SCOPE_ONE_LEVEL = 1;
    /// The scope of a search: the base entry and everything under it.
    public static final int SCOPE_SUBTREE = 2;

    static final String START_TLS_OID = "1.3.6.1.4.1.1466.20037";
    /// The largest message read; a directory entry is kilobytes.
    static final int MAX_MESSAGE = 4 * 1024 * 1024;

    private final Tcp tcp;
    private final byte[] buffer = new byte[8192];
    private int bufferStart;
    private int bufferEnd;
    private int nextId = 1;

    private LdapConnection(Tcp tcp) {
        this.tcp = tcp;
    }

    /// Connects to `url`, `ldap://host[:port]` or `ldaps://host[:port]`
    /// (anything after the authority is ignored here).
    ///
    /// @param startTls upgrade an `ldap://` connection with StartTLS before anything else
    /// @param caFile PEM of the certificate authorities to trust; null for the system's
    /// @param connectTimeoutMillis zero for the platform's
    private static final int SIZE_LIMIT_EXCEEDED = 4;
    private static final int REFERRAL = 10;

    /// @param readTimeoutMillis how long an answer may take; zero waits forever
    public static LdapConnection open(String url, boolean startTls, String caFile,
            int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        LdapUrl u = LdapUrl.parse(url);
        Tcp tcp = Tcp.connect(u.host, u.port, connectTimeoutMillis);
        LdapConnection c = new LdapConnection(tcp);
        try {
            if (readTimeoutMillis > 0) {
                tcp.setReadTimeout(readTimeoutMillis);
            }
            if (u.secure) {
                tcp.startTls(u.host, caFile, true);
            } else if (startTls) {
                c.startTls(u.host, caFile);
            }
            return c;
        } catch (IOException err) {
            tcp.close();
            throw err;
        }
    }

    /// Whether the session is encrypted.
    public boolean isSecure() {
        return tcp.isSecure();
    }

    private void startTls(String host, String caFile) throws IOException {
        int id = send(Asn1.tlv(0x77, Asn1.tlv(0x80, LdapFilter.utf8(START_TLS_OID))));
        Asn1.Reader op = response(id, 0x78);
        result(op, "StartTLS");
        if (bufferEnd > bufferStart) {
            // Anything read past the answer came in the clear and would be
            // taken for a reply from inside the TLS session.
            throw new IOException("The directory sent data after agreeing to StartTLS and "
                    + "before the handshake; the connection was dropped");
        }
        tcp.startTls(host, caFile, true);
    }

    /// A simple bind as `dn`.
    ///
    /// An empty password is refused here rather than sent: RFC 4513 section
    /// 5.1.2 makes a name with no password an *unauthenticated* bind, which
    /// many directories answer with success -- and a sign-in that checked only
    /// that would let anyone in as anyone.
    ///
    /// @throws LdapException with [LdapException#INVALID_CREDENTIALS] when the
    /// directory refuses the password
    public void bind(String dn, String password) throws IOException {
        if (dn.length() > 0 && (password == null || password.length() == 0)) {
            throw new LdapException(LdapException.INVALID_CREDENTIALS,
                    "An empty password is never sent; it would be an unauthenticated bind", "");
        }
        int id = send(Asn1.tlv(0x60, Asn1.integer(3), Asn1.octets(LdapFilter.utf8(dn)),
                Asn1.tlv(0x80, LdapFilter.utf8(password == null ? "" : password))));
        result(response(id, 0x61), "bind as " + dn);
    }

    /// Searches `base` for `filter` and answers the entries found, with the
    /// `attributes` asked for (none: all user attributes).
    ///
    /// @param filter an RFC 4515 filter; escape what a user typed with
    /// [LdapEncoder#filterEncode]
    /// @param sizeLimit the most entries wanted; zero for the server's limit.
    /// More than that is [LdapException#SIZE_LIMIT_EXCEEDED].
    public List<LdapEntry> search(String base, int scope, String filter, int sizeLimit,
                                  int timeLimitSeconds, String... attributes) throws IOException {
        byte[][] names = new byte[attributes.length][];
        for (int i = 0; i < attributes.length; i++) {
            names[i] = Asn1.octets(LdapFilter.utf8(attributes[i]));
        }
        int id = send(Asn1.tlv(0x63, Asn1.octets(LdapFilter.utf8(base)),
                Asn1.tlv(0x0a, new byte[] {(byte) scope}),
                Asn1.tlv(0x0a, new byte[] {0}),
                Asn1.integer(sizeLimit), Asn1.integer(timeLimitSeconds),
                Asn1.tlv(0x01, new byte[] {0}),
                LdapFilter.compile(filter), Asn1.sequence(names)));
        List<LdapEntry> found = new ArrayList<LdapEntry>();
        while (true) {
            Asn1.Reader message = message(id);
            int tag = message.peek();
            if (tag == 0x64) {
                found.add(entry(message.open(0x64)));
            } else if (tag == 0x73) {
                // A continuation reference to another server; not followed,
                // as Spring's default ("ignore") does not follow it.
                message.skip();
            } else if (tag == 0x65) {
                try {
                    result(message.open(0x65), "search of " + base);
                } catch (LdapException partial) {
                    // sizeLimitExceeded still delivered the entries up to the
                    // limit, which is how a caller asking for at most 2 learns
                    // there are several. A referral result is the base naming
                    // a context this server does not hold: not followed, as
                    // Spring's default ("ignore") does not follow it, so what
                    // arrived is the answer.
                    if (partial.getResultCode() != SIZE_LIMIT_EXCEEDED
                            && partial.getResultCode() != REFERRAL) {
                        throw partial;
                    }
                }
                return found;
            } else {
                throw new IOException("Unexpected LDAP response 0x" + Integer.toHexString(tag)
                        + " to a search");
            }
        }
    }

    private static LdapEntry entry(Asn1.Reader r) throws IOException {
        LdapEntry e = new LdapEntry(string(r.read(Asn1.OCTET_STRING)));
        Asn1.Reader attributes = r.open(Asn1.SEQUENCE);
        while (attributes.hasMore()) {
            Asn1.Reader a = attributes.open(Asn1.SEQUENCE);
            String type = string(a.read(Asn1.OCTET_STRING));
            Asn1.Reader values = a.open(0x31);
            while (values.hasMore()) {
                e.add(type, string(values.read(Asn1.OCTET_STRING)));
            }
        }
        return e;
    }

    /// Says goodbye and closes the socket. Never throws.
    @Override
    public void close() {
        try {
            int id = nextId++;
            byte[] m = Asn1.sequence(Asn1.integer(id), new byte[] {0x42, 0});
            tcp.write(m, 0, m.length);
        } catch (IOException ignored) {
            // The socket is closed next either way.
        } finally {
            tcp.close();
        }
    }

    private int send(byte[] op) throws IOException {
        int id = nextId++;
        byte[] m = Asn1.sequence(Asn1.integer(id), op);
        tcp.write(m, 0, m.length);
        return id;
    }

    private Asn1.Reader response(int id, int tag) throws IOException {
        Asn1.Reader message = message(id);
        if (message.peek() != tag) {
            throw new IOException("Unexpected LDAP response 0x"
                    + Integer.toHexString(message.peek()));
        }
        return message.open(tag);
    }

    /// The rest of the next message, after its ID, which must be `id`.
    private Asn1.Reader message(int id) throws IOException {
        Asn1.Reader m = new Asn1.Reader(frame()).open(Asn1.SEQUENCE);
        int got = m.smallInteger();
        if (got == 0) {
            // An unsolicited notification: in practice the Notice of
            // Disconnection, after which the server closes the socket.
            Asn1.Reader notice = m.open(0x78);
            int code = enumerated(notice.read(0x0a));
            notice.read(Asn1.OCTET_STRING);
            throw new LdapException(code, "The directory ended the session",
                    string(notice.read(Asn1.OCTET_STRING)));
        }
        if (got != id) {
            throw new IOException("An LDAP answer to message " + got + " came for " + id);
        }
        return m;
    }

    private static void result(Asn1.Reader r, String what) throws IOException {
        int code = enumerated(r.read(0x0a));
        r.read(Asn1.OCTET_STRING);
        String diagnostic = string(r.read(Asn1.OCTET_STRING));
        if (code != LdapException.SUCCESS) {
            throw new LdapException(code, "The directory refused the " + what + ": result "
                    + code + (diagnostic.length() == 0 ? "" : " (" + diagnostic + ")"),
                    diagnostic);
        }
    }

    private static int enumerated(byte[] value) throws IOException {
        if (value.length == 0 || value.length > 4 || (value[0] & 0x80) != 0) {
            throw new IOException("A bad LDAP result code");
        }
        int out = 0;
        for (byte b : value) {
            out = (out << 8) | (b & 0xff);
        }
        return out;
    }

    private static String string(byte[] utf8) throws IOException {
        return new String(utf8, "UTF-8");
    }

    /// One whole BER element off the wire: tag, length and contents.
    private byte[] frame() throws IOException {
        int tag = readByte();
        if (tag != 0x30) {
            throw new IOException("Not an LDAP message: tag 0x" + Integer.toHexString(tag));
        }
        int first = readByte();
        int length;
        int lengthBytes = 0;
        if (first < 0x80) {
            length = first;
        } else {
            lengthBytes = first & 0x7f;
            if (lengthBytes == 0 || lengthBytes > 4) {
                throw new IOException("An LDAP message length that is not definite");
            }
            length = 0;
            for (int i = 0; i < lengthBytes; i++) {
                length = (length << 8) | readByte();
            }
        }
        if (length < 0 || length > MAX_MESSAGE) {
            throw new IOException("An LDAP message of " + length + " bytes is too large");
        }
        byte[] out = new byte[2 + lengthBytes + length];
        out[0] = (byte) tag;
        out[1] = (byte) first;
        int at = 2;
        for (int i = lengthBytes - 1; i >= 0; i--) {
            out[at++] = (byte) (length >> (8 * i));
        }
        while (at < out.length) {
            if (bufferStart == bufferEnd) {
                fill();
            }
            int n = Math.min(bufferEnd - bufferStart, out.length - at);
            System.arraycopy(buffer, bufferStart, out, at, n);
            bufferStart += n;
            at += n;
        }
        return out;
    }

    private int readByte() throws IOException {
        if (bufferStart == bufferEnd) {
            fill();
        }
        return buffer[bufferStart++] & 0xff;
    }

    private void fill() throws IOException {
        int n = tcp.read(buffer, 0, buffer.length);
        if (n <= 0) {
            throw new IOException("The directory closed the connection");
        }
        bufferStart = 0;
        bufferEnd = n;
    }

    /// The parts of an LDAP URL a connection needs.
    static final class LdapUrl {
        final String host;
        final int port;
        final boolean secure;
        final String base;

        private LdapUrl(String host, int port, boolean secure, String base) {
            this.host = host;
            this.port = port;
            this.secure = secure;
            this.base = base;
        }

        static LdapUrl parse(String url) throws IOException {
            boolean secure;
            int from;
            if (url.regionMatches(true, 0, "ldaps://", 0, 8)) {
                secure = true;
                from = 8;
            } else if (url.regionMatches(true, 0, "ldap://", 0, 7)) {
                secure = false;
                from = 7;
            } else {
                throw new IOException("Not an ldap:// or ldaps:// URL: " + url);
            }
            int slash = url.indexOf('/', from);
            String authority = slash < 0 ? url.substring(from) : url.substring(from, slash);
            String base = slash < 0 ? "" : percentDecode(url.substring(slash + 1));
            int colon = authority.lastIndexOf(':');
            String host = authority;
            int port = secure ? 636 : 389;
            if (colon > 0 && authority.indexOf(']', colon) < 0) {
                host = authority.substring(0, colon);
                try {
                    port = Integer.parseInt(authority.substring(colon + 1));
                } catch (NumberFormatException err) {
                    throw new IOException("A bad port in " + url, err);
                }
            }
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }
            if (host.length() == 0 || port <= 0 || port > 65535) {
                throw new IOException("No host in " + url);
            }
            return new LdapUrl(host, port, secure, base);
        }

        private static String percentDecode(String s) throws IOException {
            if (s.indexOf('%') < 0) {
                return s;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(s.length());
            int i = 0;
            while (i < s.length()) {
                int pct = s.indexOf('%', i);
                int end = pct < 0 ? s.length() : pct;
                byte[] run = LdapFilter.utf8(s.substring(i, end));
                out.write(run, 0, run.length);
                if (pct < 0) {
                    break;
                }
                if (pct + 2 >= s.length()) {
                    throw new IOException("A bad % escape in an LDAP URL");
                }
                int hi = Character.digit(s.charAt(pct + 1), 16);
                int lo = Character.digit(s.charAt(pct + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw new IOException("A bad % escape in an LDAP URL");
                }
                out.write((hi << 4) | lo);
                i = pct + 3;
            }
            return new String(out.toByteArray(), "UTF-8");
        }
    }
}
