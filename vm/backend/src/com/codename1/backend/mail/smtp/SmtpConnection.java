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

import com.codename1.backend.Base64;
import com.codename1.backend.Tcp;
import com.codename1.backend.mail.MailAuthenticationException;
import com.codename1.backend.mail.MailException;
import com.codename1.backend.mail.MailSendException;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// One session with an SMTP server: RFC 5321 with the extensions a mail
/// provider expects of a client today.
///
/// - **STARTTLS** (RFC 3207). The connection is upgraded where it stands and
///   the greeting repeated, since nothing learned before the upgrade can be
///   trusted. Bytes that arrive between the server's go-ahead and the
///   handshake are an injection -- a response planted in the clear to be read
///   as if it came over TLS -- and end the session.
/// - **AUTH** `PLAIN`, `LOGIN` and `XOAUTH2`, and only over TLS.
/// - **PIPELINING** (RFC 2920): the sender, every recipient and `DATA` leave
///   in one write and their replies are read in order, which makes a mail two
///   round trips rather than three plus one per recipient.
/// - **SIZE**: a mail larger than the server accepts is refused before a byte
///   of it is sent.
/// - **SMTPUTF8** (RFC 6531), asked for only by a mail with an address that
///   needs it.
///
/// Not thread safe; [SmtpTransport] lends one to a thread at a time.
final class SmtpConnection {
    /// The most staged before a write: the body leaves in pieces this size
    /// rather than being copied whole into the buffer first.
    private static final int CHUNK = 32768;

    private final Tcp tcp;
    private WireBuffer wire;
    private boolean broken;
    private boolean pipelining;
    private boolean startTls;
    private boolean smtpUtf8;
    private boolean sizeOffered;
    private long maxSize;
    private String mechanisms = "";
    /// More lines than any server's reply has; an EHLO lists a dozen or two.
    private static final int MAX_REPLY_LINES = 100;
    private long lastUsed;
    // One reply at a time, emptied before each and capped per line where it is
    // filled: kept so reading a reply allocates nothing.
    private final StringBuilder text = new StringBuilder(128); // NOPMD AvoidStringBufferField - bounded, see above
    /// The lines of the last reply, each without its code.
    private final List<String> lines = new ArrayList<String>();

    private SmtpConnection(Tcp tcp) {
        this.tcp = tcp;
        this.wire = new WireBuffer(tcp);
    }

    /// Connects, secures the connection and signs in.
    ///
    /// - `implicitTls`: TLS from the first byte, as port 465 speaks it
    /// - `requireTls`: fail when the server offers no STARTTLS, rather than
    ///   carry on in the clear
    /// - `caFile`: a certificate file to trust instead of the system's, or null
    /// - `helo`: the name this client gives for itself
    /// - `mechanism`: `plain`, `login`, `xoauth2`, or null to take the first
    ///   of plain and login the server offers
    static SmtpConnection open(String host, int port, boolean implicitTls, boolean requireTls,
                               String caFile, String helo, String user, String password,
                               String mechanism, int connectTimeoutMillis,
                               int readTimeoutMillis) throws IOException {
        Tcp tcp = Tcp.connect(host, port, connectTimeoutMillis);
        try {
            if (readTimeoutMillis > 0) {
                tcp.setReadTimeout(readTimeoutMillis);
            }
            if (implicitTls) {
                secure(tcp, host, caFile);
            }
            SmtpConnection c = new SmtpConnection(tcp);
            c.expect(c.reply(), 220, "greeting");
            c.ehlo(helo);
            if (!tcp.isSecure()) {
                if (c.startTls) {
                    c.command("STARTTLS");
                    c.expect(c.reply(), 220, "STARTTLS");
                    if (c.wire.hasBuffered()) {
                        throw new MailException("The mail server sent data after agreeing to "
                                + "STARTTLS and before the handshake; that is a response "
                                + "injected in the clear, and the connection was dropped");
                    }
                    secure(tcp, host, caFile);
                    c.wire = new WireBuffer(tcp);
                    c.ehlo(helo);
                } else if (requireTls) {
                    throw new MailException("The mail server at " + host + ":" + port
                            + " offers no STARTTLS. Use smtps:// for a port that speaks TLS "
                            + "from the start, or set cn1.mail.smtp.starttls=false to send in "
                            + "the clear");
                }
            }
            if (user != null) {
                c.authenticate(user, password == null ? "" : password, mechanism);
            }
            c.lastUsed = System.currentTimeMillis();
            return c;
        } catch (IOException err) {
            tcp.close();
            throw err;
        } catch (RuntimeException err) {
            tcp.close();
            throw new IOException("Malformed reply from the mail server: " + err, err);
        }
    }

    private static void secure(Tcp tcp, String host, String caFile) throws IOException {
        if (caFile != null) {
            tcp.startTls(host, caFile);
        } else {
            tcp.startTls(host);
        }
    }

    boolean isBroken() {
        return broken;
    }

    /// When this connection last finished an exchange.
    long lastUsed() {
        return lastUsed;
    }

    private void command(String line) throws IOException {
        stage(line);
        flush();
    }

    private void stage(String line) {
        wire.writeBytes(WireBuffer.utf8(line));
        wire.writeByte('\r');
        wire.writeByte('\n');
    }

    private void flush() throws IOException {
        try {
            wire.flush();
        } catch (IOException err) {
            broken = true;
            throw err;
        }
    }

    /// Reads one reply, of however many lines, and answers its code; its text
    /// is left in [#text].
    private int reply() throws IOException {
        text.setLength(0);
        lines.clear();
        try {
            while (true) {
                String line = wire.readLine();
                if (lines.size() >= MAX_REPLY_LINES) {
                    // A server that never ends its reply would hold the one
                    // thread every mail leaves on.
                    throw new IOException("The mail server's reply has no end");
                }
                // Whole, for the extensions an EHLO lists: the text below is
                // capped, and an extension past the cap would read as one
                // the server does not have.
                lines.add(line.length() > 4 ? line.substring(4) : "");
                if (line.length() < 3) {
                    throw new IOException("Malformed reply from the mail server");
                }
                int code = 0;
                for (int i = 0; i < 3; i++) {
                    char c = line.charAt(i);
                    if (c < '0' || c > '9') {
                        throw new IOException("Malformed reply from the mail server");
                    }
                    code = code * 10 + (c - '0');
                }
                if (line.length() > 4) {
                    if (text.length() > 0) {
                        text.append('\n');
                    }
                    // A reply is the server's to word: what is quoted from it
                    // is capped, so it cannot fill a log or a table.
                    if (text.length() < 400) {
                        text.append(line, 4, Math.min(line.length(), 204));
                    }
                }
                if (line.length() == 3 || line.charAt(3) == ' ') {
                    return code;
                }
                if (line.charAt(3) != '-') {
                    throw new IOException("Malformed reply from the mail server");
                }
            }
        } catch (IOException err) {
            broken = true;
            throw err;
        }
    }

    private MailSendException refused(String what, int code) {
        return new MailSendException("The mail server refused " + what + ": " + code + " "
                + firstLine(), code, code >= 500);
    }

    private String firstLine() {
        int nl = text.indexOf("\n");
        return nl < 0 ? text.toString() : text.substring(0, nl);
    }

    /// For the replies before any mail is named -- the greeting, EHLO,
    /// STARTTLS. A refusal there says the server is not taking mail now: a
    /// 554 from one under maintenance, or one that has listed this host.
    /// It is the server that is refused, not the mail, so the mail is tried
    /// again, where a 5xx to the mail itself is final.
    private void expect(int code, int wanted, String what) throws IOException {
        if (code != wanted) {
            broken = true;
            throw new MailSendException("The mail server refused " + what + ": " + code + " "
                    + firstLine(), code, false);
        }
    }

    private void ehlo(String helo) throws IOException {
        command("EHLO " + helo);
        int code = reply();
        expect(code, 250, "EHLO");
        pipelining = false;
        startTls = false;
        smtpUtf8 = false;
        sizeOffered = false;
        maxSize = 0;
        mechanisms = "";
        // The first line is the server's name; each one after it an extension.
        for (int i = 1; i < lines.size(); i++) {
            extension(lines.get(i));
        }
    }

    private static boolean keyword(String line, String word) {
        return line.regionMatches(true, 0, word, 0, word.length())
                && (line.length() == word.length() || line.charAt(word.length()) == ' '
                || line.charAt(word.length()) == '=');
    }

    private void extension(String line) {
        if (keyword(line, "PIPELINING")) {
            pipelining = true;
        } else if (keyword(line, "STARTTLS")) {
            startTls = true;
        } else if (keyword(line, "SMTPUTF8")) {
            smtpUtf8 = true;
        } else if (keyword(line, "SIZE")) {
            sizeOffered = true;
            long n = 0;
            for (int i = 5; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c < '0' || c > '9' || n > Long.MAX_VALUE / 20) {
                    break;
                }
                n = n * 10 + (c - '0');
            }
            maxSize = n;
        } else if (keyword(line, "AUTH")) {
            mechanisms = " " + line.substring(4).replace('=', ' ') + " ";
        }
    }

    private boolean offers(String mechanism) {
        String wanted = " " + mechanism + " ";
        for (int i = 0; i + wanted.length() <= mechanisms.length(); i++) {
            if (mechanisms.regionMatches(true, i, wanted, 0, wanted.length())) {
                return true;
            }
        }
        return false;
    }

    private void authenticate(String user, String password, String mechanism)
            throws IOException {
        if (!tcp.isSecure()) {
            throw new MailAuthenticationException("The connection to the mail server is not "
                    + "encrypted, and a password is not sent over one that is not. Use "
                    + "smtps://, or a server that offers STARTTLS", 0);
        }
        String chosen = mechanism;
        if (chosen == null) {
            chosen = offers("PLAIN") ? "plain" : offers("LOGIN") ? "login" : null;
            if (chosen == null) {
                throw new MailAuthenticationException("The mail server offers none of the "
                        + "sign-in mechanisms PLAIN and LOGIN; set cn1.mail.smtp.auth=xoauth2 "
                        + "when the password is an OAuth 2 access token", 0);
            }
        }
        int code;
        if ("xoauth2".equalsIgnoreCase(chosen)) {
            command("AUTH XOAUTH2 " + Base64.encode(WireBuffer.utf8("user=" + user
                    + "\001auth=Bearer " + password + "\001\001")));
            code = reply();
            if (code == 334) {
                // The server's explanation of the failure; an empty line asks
                // for the verdict.
                command("");
                code = reply();
            }
        } else if ("login".equalsIgnoreCase(chosen)) {
            command("AUTH LOGIN");
            code = reply();
            if (code == 334) {
                command(Base64.encode(WireBuffer.utf8(user)));
                code = reply();
            }
            if (code == 334) {
                command(Base64.encode(WireBuffer.utf8(password)));
                code = reply();
            }
        } else if ("plain".equalsIgnoreCase(chosen)) {
            command("AUTH PLAIN " + Base64.encode(WireBuffer.utf8("\000" + user + "\000"
                    + password)));
            code = reply();
        } else {
            throw new MailAuthenticationException("cn1.mail.smtp.auth=" + chosen + " is not a "
                    + "mechanism this client has; it has plain, login and xoauth2", 0);
        }
        if (code != 235) {
            broken = true;
            throw new MailAuthenticationException("The mail server refused the sign-in: "
                    + code + " " + firstLine(), code);
        }
    }

    private static boolean ascii(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 126) {
                return false;
            }
        }
        return true;
    }

    /// Sends one mail. When it returns the server has taken it for every
    /// recipient. A [MailSendException] that is not permanent means nothing
    /// was sent to anyone; one that is permanent means the server will not
    /// take the mail, or took it for some recipients and refused the others
    /// for good.
    void send(String from, String[] recipients, byte[] mime) throws IOException {
        if (maxSize > 0 && mime.length > maxSize) {
            throw new MailSendException("The mail is " + mime.length + " bytes and the mail "
                    + "server accepts " + maxSize, 552, true);
        }
        boolean utf8 = !ascii(from);
        for (String r : recipients) {
            utf8 |= !ascii(r);
        }
        if (utf8 && !smtpUtf8) {
            throw new MailSendException("An address of the mail is not ASCII and the mail "
                    + "server does not offer SMTPUTF8", 553, true);
        }
        StringBuilder mailFrom = new StringBuilder(64).append("MAIL FROM:<").append(from)
                .append('>');
        if (sizeOffered) {
            mailFrom.append(" SIZE=").append(mime.length);
        }
        if (utf8) {
            mailFrom.append(" SMTPUTF8");
        }
        int[] codes = new int[recipients.length];
        int mailCode;
        int dataCode = 0;
        String firstRefusal = null;
        if (pipelining) {
            stage(mailFrom.toString());
            for (String r : recipients) {
                stage("RCPT TO:<" + r + ">");
            }
            stage("DATA");
            flush();
            mailCode = reply();
            String mailText = firstLine();
            for (int i = 0; i < codes.length; i++) {
                codes[i] = reply();
                if (firstRefusal == null && codes[i] / 100 != 2) {
                    firstRefusal = codes[i] + " " + firstLine();
                }
            }
            dataCode = reply();
            if (mailCode != 250) {
                abandon(dataCode);
                text.setLength(0);
                text.append(mailText);
                throw refused("the sender", mailCode);
            }
        } else {
            command(mailFrom.toString());
            mailCode = reply();
            if (mailCode != 250) {
                MailSendException err = refused("the sender", mailCode);
                reset();
                throw err;
            }
            for (int i = 0; i < codes.length; i++) {
                command("RCPT TO:<" + recipients[i] + ">");
                codes[i] = reply();
                if (firstRefusal == null && codes[i] / 100 != 2) {
                    firstRefusal = codes[i] + " " + firstLine();
                }
            }
        }
        int accepted = 0;
        int later = 0;
        int refusedCode = 0;
        for (int code : codes) {
            if (code / 100 == 2) {
                accepted++;
            } else if (code / 100 == 4) {
                later++;
                refusedCode = code;
            } else if (later == 0) {
                refusedCode = code;
            }
        }
        if (accepted == 0 || later > 0) {
            // Nothing is sent to anyone: a recipient the server may take
            // later means the whole mail is tried again, and sending it now
            // to the others would send it to them twice.
            abandon(dataCode);
            throw new MailSendException("The mail server " + (later > 0 ? "deferred " : "refused ")
                    + (codes.length - accepted) + " of " + codes.length + " recipient(s): "
                    + firstRefusal, refusedCode, later == 0);
        }
        if (!pipelining) {
            command("DATA");
            dataCode = reply();
        }
        if (dataCode != 354) {
            MailSendException err = refused("the message", dataCode);
            if (!broken) {
                reset();
            }
            throw err;
        }
        body(mime);
        int code = reply();
        if (code != 250) {
            throw refused("the message", code);
        }
        lastUsed = System.currentTimeMillis();
        if (accepted < codes.length) {
            throw new MailSendException("The mail was sent to " + accepted + " of "
                    + codes.length + " recipients; the mail server refused the rest: "
                    + firstRefusal, refusedCode, true);
        }
    }

    /// Gives up on a transaction. One that reached `DATA` and was told to go
    /// ahead cannot be taken back with a command -- anything sent now is the
    /// message -- so the connection is closed, which discards it.
    private void abandon(int dataCode) throws IOException {
        if (dataCode == 354) {
            close();
        } else if (!broken) {
            reset();
        }
    }

    private void reset() throws IOException {
        command("RSET");
        if (reply() != 250) {
            broken = true;
        }
    }

    /// Writes the message and the line that ends it. A line that begins with
    /// a dot is given a second one, which the server takes off again: alone
    /// on a line, a dot is the end of the message.
    private void body(byte[] mime) throws IOException {
        int run = 0;
        boolean lineStart = true;
        for (int i = 0; i < mime.length; i++) {
            byte b = mime[i];
            if (lineStart && b == '.') {
                wire.writeBytes(mime, run, i - run);
                wire.writeByte('.');
                run = i;
            }
            lineStart = b == '\n';
            if (lineStart && wire.pending() + (i + 1 - run) >= CHUNK) {
                wire.writeBytes(mime, run, i + 1 - run);
                run = i + 1;
                flush();
            }
        }
        wire.writeBytes(mime, run, mime.length - run);
        if (!lineStart) {
            wire.writeByte('\r');
            wire.writeByte('\n');
        }
        wire.writeByte('.');
        wire.writeByte('\r');
        wire.writeByte('\n');
        flush();
    }

    /// Whether the server still answers; a connection that sat idle may have
    /// been closed from the other side.
    boolean alive() {
        if (broken) {
            return false;
        }
        try {
            command("NOOP");
            if (reply() != 250) {
                broken = true;
            }
        } catch (IOException err) {
            broken = true;
        }
        return !broken;
    }

    /// Says goodbye when the session is in a state to, and closes the socket.
    /// Never throws.
    void close() {
        if (!broken) {
            broken = true;
            try {
                wire.writeBytes(WireBuffer.ascii("QUIT\r\n"));
                wire.flush();
            } catch (IOException | RuntimeException err) {
                // The socket is closed next either way.
            }
        }
        tcp.close();
    }
}
