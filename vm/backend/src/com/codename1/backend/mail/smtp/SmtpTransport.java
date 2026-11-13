/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.mail.smtp;

import com.codename1.backend.Config;
import com.codename1.backend.mail.MailException;
import com.codename1.backend.mail.MailTransport;
import com.codename1.backend.mail.Mailers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/// Sends mail to an SMTP server: a provider's submission endpoint, a relay,
/// or a mail server of one's own.
///
/// ```properties
/// cn1.mail.url=smtp://apikey:secret@smtp.example.com:587
/// ```
///
/// `smtp://` connects in the clear and upgrades with STARTTLS before anything
/// else is said; a server that does not offer it is refused. `smtps://`
/// speaks TLS from the first byte, as port 465 does. The defaults are port
/// 587 and 465. A user name and password in the URL are percent-encoded where
/// they contain `@`, `:` or `/`.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.mail.username`, `cn1.mail.password` | | credentials, when the URL carries none |
/// | `cn1.mail.smtp.starttls` | `true` | `false` sends in the clear to a server with no STARTTLS -- a relay on localhost |
/// | `cn1.mail.smtp.auth` | | `plain`, `login` or `xoauth2`; unset takes what the server offers. With `xoauth2` the password is an OAuth 2 access token |
/// | `cn1.mail.smtp.helo` | `localhost` | the name the client gives for itself |
/// | `cn1.mail.smtp.ca` | | a PEM file of certificates to trust instead of the system's |
/// | `cn1.mail.timeout.connect` | 5000 | milliseconds a connection may take to open |
/// | `cn1.mail.timeout.read` | 60000 | milliseconds a reply may take |
/// | `cn1.mail.pool.max` | 2 | idle connections kept for the next mail |
/// | `cn1.mail.pool.idle` | 30000 | milliseconds an idle connection is kept |
///
/// A password is never sent over a connection that is not encrypted, whatever
/// the settings say.
public final class SmtpTransport extends MailTransport {
    private final String host;
    private final int port;
    private final boolean implicitTls;
    private final boolean requireTls;
    private final String user;
    private final String password;
    private final String mechanism;
    private final String helo;
    private final String caFile;
    private final int connectTimeout;
    private final int readTimeout;
    private final int maxIdle;
    private final long idleMillis;
    private final List<SmtpConnection> idle = new ArrayList<SmtpConnection>();
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicBoolean closed = new AtomicBoolean();

    /// Opens `smtp:` and `smtps:` URLs; what the generated wiring registers.
    public static final class Factory implements Mailers.TransportFactory {
        @Override
        public MailTransport open(String url, Config config) throws IOException {
            return new SmtpTransport(url, config);
        }
    }

    /// A transport for `url`, with the settings of `config`, which may be
    /// null. Nothing connects until the first mail.
    public SmtpTransport(String url, Config config) throws IOException {
        String rest;
        if (url.regionMatches(true, 0, "smtps://", 0, 8)) {
            implicitTls = true;
            rest = url.substring(8);
        } else if (url.regionMatches(true, 0, "smtp://", 0, 7)) {
            implicitTls = false;
            rest = url.substring(7);
        } else {
            throw new IOException("Not an smtp:// or smtps:// URL");
        }
        int cut = rest.indexOf('?');
        if (cut >= 0) {
            rest = rest.substring(0, cut);
        }
        String u = null;
        String p = null;
        int at = rest.lastIndexOf('@');
        if (at >= 0) {
            String credentials = rest.substring(0, at);
            rest = rest.substring(at + 1);
            int colon = credentials.indexOf(':');
            if (colon < 0) {
                u = decode(credentials);
            } else {
                u = decode(credentials.substring(0, colon));
                p = decode(credentials.substring(colon + 1));
            }
        }
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        int portValue = implicitTls ? 465 : 587;
        int colon = rest.lastIndexOf(':');
        if (colon > 0 && rest.indexOf(']') < colon) {
            try {
                portValue = Integer.parseInt(rest.substring(colon + 1));
            } catch (NumberFormatException err) {
                throw new IOException("The port of cn1.mail.url must be a number"); // NOPMD PreserveStackTrace - the cause adds nothing
            }
            rest = rest.substring(0, colon);
        }
        if (rest.startsWith("[") && rest.endsWith("]")) {
            rest = rest.substring(1, rest.length() - 1);
        }
        host = rest.length() == 0 ? "localhost" : rest;
        port = portValue;
        if (u == null || u.length() == 0) {
            u = setting(config, "cn1.mail.username", "spring.mail.username");
            p = setting(config, "cn1.mail.password", "spring.mail.password");
        } else if (p == null) {
            p = setting(config, "cn1.mail.password", "spring.mail.password");
        }
        user = u;
        password = p;
        mechanism = setting(config, "cn1.mail.smtp.auth", null);
        String tls = setting(config, "cn1.mail.smtp.starttls", null);
        requireTls = !"false".equalsIgnoreCase(tls);
        String name = setting(config, "cn1.mail.smtp.helo", null);
        helo = name == null ? "localhost" : name;
        caFile = setting(config, "cn1.mail.smtp.ca", null);
        connectTimeout = config == null ? 5000 : config.getInt("cn1.mail.timeout.connect", 5000);
        readTimeout = config == null ? 60000 : config.getInt("cn1.mail.timeout.read", 60000);
        maxIdle = Math.max(0, config == null ? 2 : config.getInt("cn1.mail.pool.max", 2));
        idleMillis = config == null ? 30000 : config.getInt("cn1.mail.pool.idle", 30000);
    }

    private static String setting(Config config, String key, String fallback) throws IOException {
        if (config == null) {
            return null;
        }
        String v = config.get(key);
        if ((v == null || v.trim().length() == 0) && fallback != null) {
            v = config.get(fallback);
        }
        return v == null || v.trim().length() == 0 ? null : v.trim();
    }

    /// Percent-decodes a URL user or password.
    private static String decode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        byte[] out = new byte[s.length() * 3];
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && hex(s.charAt(i + 1)) >= 0
                    && hex(s.charAt(i + 2)) >= 0) {
                out[n++] = (byte) (hex(s.charAt(i + 1)) * 16 + hex(s.charAt(i + 2)));
                i += 2;
            } else {
                byte[] one = com.codename1.impl.backend.WireBuffer.utf8(String.valueOf(c));
                System.arraycopy(one, 0, out, n, one.length);
                n += one.length;
            }
        }
        byte[] exact = new byte[n];
        System.arraycopy(out, 0, exact, 0, n);
        return com.codename1.impl.backend.WireBuffer.fromUtf8(exact);
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    @Override
    public String getName() {
        return implicitTls ? "smtps" : "smtp";
    }

    /// The server's host name.
    public String getHost() {
        return host;
    }

    /// The server's port.
    public int getPort() {
        return port;
    }

    private SmtpConnection connect() throws IOException {
        return SmtpConnection.open(host, port, implicitTls, requireTls, caFile, helo, user,
                password, mechanism, connectTimeout, readTimeout);
    }

    /// An idle connection the server still answers on, or a new one.
    private SmtpConnection borrow() throws IOException {
        while (true) {
            SmtpConnection c; // NOPMD CloseResource - returned to the caller or closed below
            synchronized (idle) {
                if (idle.isEmpty()) {
                    return connect();
                }
                c = idle.remove(idle.size() - 1);
            }
            long quiet = System.currentTimeMillis() - c.lastUsed();
            if (quiet <= idleMillis && (quiet <= 1000 || c.alive())) {
                return c;
            }
            c.close();
        }
    }

    private void release(SmtpConnection c) {
        if (!c.isBroken()) {
            synchronized (idle) {
                // Read under the lock close() empties the list under, or a
                // send that ends as the transport closes leaves a connection
                // in a list nobody reads again.
                if (!closed.get() && idle.size() < maxIdle) {
                    idle.add(c);
                    return;
                }
            }
        }
        c.close();
    }

    @Override
    public void send(String from, String[] recipients, byte[] mime) throws IOException {
        if (closed.get()) {
            throw new MailException("The mail transport is closed");
        }
        SmtpConnection c; // NOPMD CloseResource - released below
        try {
            c = borrow();
        } catch (IOException err) {
            healthy.set(false);
            throw err;
        }
        healthy.set(true);
        try {
            c.send(from, recipients, mime);
        } finally {
            release(c);
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    @Override
    public void close() {
        closed.set(true);
        List<SmtpConnection> all;
        synchronized (idle) {
            all = new ArrayList<SmtpConnection>(idle);
            idle.clear();
        }
        for (SmtpConnection c : all) {
            c.close();
        }
    }
}
