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
package com.codename1.backend.mongo;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;
import com.codename1.impl.backend.WireBuffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// One connection to a MongoDB server, sending commands as `OP_MSG`.
///
/// Not thread safe; [MongoClient] lends each connection to one caller at a time.
final class MongoConnection {
    private static final int OP_MSG = 2013;
    private static final int MAX_SCRAM_ITERATIONS = 1000000;

    private final WireBuffer wire;
    private int nextRequest = 1;
    private boolean broken;

    private MongoConnection(WireBuffer wire) {
        this.wire = wire;
    }

    static MongoConnection open(MongoUri uri, String host, int port) throws IOException {
        Tcp tcp = Tcp.connect(host, port, uri.connectTimeoutMillis);
        MongoConnection c = null;
        try {
            if (uri.tls) {
                tcp.startTls(host, uri.tlsCaFile, !uri.tlsAllowInvalidHostnames);
            }
            if (uri.socketTimeoutMillis > 0) {
                tcp.setReadTimeout(uri.socketTimeoutMillis);
            }
            c = new MongoConnection(new WireBuffer(tcp));
            Map hello = new LinkedHashMap();
            hello.put("hello", Integer.valueOf(1));
            // No client metadata: the server requires driver.version and os.type
            // in it once it is sent at all, and it is diagnostics only.
            c.command("admin", hello);
            if (uri.user != null) {
                c.scram(uri.authSource, uri.user, uri.password == null ? "" : uri.password);
            }
            return c;
        } catch (IOException err) {
            if (c != null) {
                c.close();
            } else {
                tcp.close();
            }
            throw err;
        }
    }

    boolean isBroken() {
        return broken;
    }

    /// Runs `command` against `database` and returns the reply, throwing a
    /// [MongoException] when it says `ok: 0`.
    Map command(String database, Map command) throws IOException {
        Map cmd = new LinkedHashMap(command);
        cmd.put("$db", database);
        byte[] doc = Bson.encode(cmd);
        boolean ok = false;
        try {
            int request = nextRequest++;
            ByteArrayOutputStream msg = new ByteArrayOutputStream(doc.length + 21);
            Bson.writeInt(msg, 16 + 4 + 1 + doc.length);
            Bson.writeInt(msg, request);
            Bson.writeInt(msg, 0);
            Bson.writeInt(msg, OP_MSG);
            Bson.writeInt(msg, 0);
            msg.write(0);
            msg.write(doc, 0, doc.length);
            wire.writeBytes(msg.toByteArray());
            wire.flush();
            Map reply = readReply(request);
            ok = true;
            Object okField = reply.get("ok");
            if (!(okField instanceof Number) || ((Number) okField).doubleValue() != 1.0) {
                throw MongoException.from(reply);
            }
            return reply;
        } finally {
            if (!ok) {
                // Anything but a clean reply leaves the stream position unknown.
                // A MongoException is thrown after ok is set, so it keeps the
                // connection.
                broken = true;
            }
        }
    }

    private Map readReply(int request) throws IOException {
        byte[] head = wire.readFully(16);
        int length = Bson.intAt(head, 0);
        int responseTo = Bson.intAt(head, 8);
        int op = Bson.intAt(head, 12);
        if (length < 21 || length > Bson.MAX_DOCUMENT + 64) {
            throw new IOException("MongoDB sent a message of " + length + " bytes");
        }
        byte[] body = wire.readFully(length - 16);
        if (op != OP_MSG || responseTo != request) {
            throw new IOException("MongoDB answered out of turn (op " + op + ")");
        }
        int flags = Bson.intAt(body, 0);
        int end = body.length - ((flags & 1) != 0 ? 4 : 0);
        int at = 4;
        Map reply = null;
        while (at < end) {
            int kind = body[at++];
            if (kind == 0) {
                if (at + 4 > end) {
                    throw new IOException("A MongoDB reply is truncated");
                }
                int size = Bson.intAt(body, at);
                if (size < 5 || at + size > end) {
                    throw new IOException("A MongoDB reply is truncated");
                }
                reply = Bson.decode(body, at, size);
                at += size;
            } else if (kind == 1) {
                if (at + 4 > end) {
                    throw new IOException("A MongoDB reply is truncated");
                }
                int size = Bson.intAt(body, at);
                if (size < 4 || at + size > end) {
                    throw new IOException("A MongoDB reply is truncated");
                }
                at += size;
            } else {
                throw new IOException("Unknown OP_MSG section kind " + kind);
            }
        }
        if (reply == null) {
            throw new IOException("A MongoDB reply carries no body");
        }
        return reply;
    }

    /// SCRAM-SHA-256 (RFC 7677) through `saslStart` and `saslContinue`.
    ///
    /// As in the PostgreSQL client, the server's signature is verified, so a peer
    /// that does not hold the credentials cannot pose as the server, and the
    /// iteration count the server names is bounded. The password goes into PBKDF2
    /// as raw UTF-8 without SASLprep: there is no Normalizer on this runtime. An
    /// ASCII password, which is what nearly every deployment uses, is unaffected.
    private void scram(String database, String user, String password) throws IOException {
        String clientNonce = Base64.encode(Crypto.randomBytes(18));
        String clientFirstBare = "n=" + saslName(user) + ",r=" + clientNonce;
        Map start = new LinkedHashMap();
        start.put("saslStart", Integer.valueOf(1));
        start.put("mechanism", "SCRAM-SHA-256");
        start.put("payload", WireBuffer.utf8("n,," + clientFirstBare));
        Map opts = new LinkedHashMap();
        opts.put("skipEmptyExchange", Boolean.TRUE);
        start.put("options", opts);
        Map first = command(database, start);
        String serverFirst = payload(first);
        String nonce = field(serverFirst, 'r');
        String saltText = field(serverFirst, 's');
        String iterText = field(serverFirst, 'i');
        if (nonce == null || saltText == null || iterText == null
                || !nonce.startsWith(clientNonce)) {
            throw new IOException("MongoDB's SCRAM message is malformed or replayed");
        }
        byte[] salt = Base64.decode(saltText);
        int iterations;
        try {
            iterations = Integer.parseInt(iterText);
        } catch (NumberFormatException err) {
            iterations = -1;
        }
        if (salt == null || iterations < 1 || iterations > MAX_SCRAM_ITERATIONS) {
            throw new IOException("MongoDB's SCRAM salt or iteration count is unusable");
        }
        byte[] salted = Crypto.pbkdf2Sha256(WireBuffer.utf8(password), salt, iterations, 32);
        byte[] clientKey = Crypto.hmacSha256(salted, WireBuffer.utf8("Client Key"));
        byte[] storedKey = Crypto.sha256(clientKey);
        String withoutProof = "c=biws,r=" + nonce;
        byte[] authMessage = WireBuffer.utf8(clientFirstBare + "," + serverFirst + ","
                + withoutProof);
        byte[] signature = Crypto.hmacSha256(storedKey, authMessage);
        byte[] proof = new byte[clientKey.length];
        for (int i = 0; i < proof.length; i++) {
            proof[i] = (byte) (clientKey[i] ^ signature[i]);
        }
        Map cont = new LinkedHashMap();
        cont.put("saslContinue", Integer.valueOf(1));
        cont.put("conversationId", first.get("conversationId"));
        cont.put("payload", WireBuffer.utf8(withoutProof + ",p=" + Base64.encode(proof)));
        Map last = command(database, cont);
        String serverFinal = payload(last);
        byte[] serverKey = Crypto.hmacSha256(salted, WireBuffer.utf8("Server Key"));
        byte[] expected = Crypto.hmacSha256(serverKey, authMessage);
        String v = field(serverFinal, 'v');
        byte[] actual = v == null ? null : Base64.decode(v);
        if (actual == null || !Crypto.equalsConstantTime(expected, actual)) {
            throw new IOException("MongoDB failed the SCRAM signature check; it does not "
                    + "hold the credentials it claims to");
        }
        // Without skipEmptyExchange support (servers before 4.4) one more empty
        // round completes the conversation.
        while (!Boolean.TRUE.equals(last.get("done"))) {
            Map more = new LinkedHashMap();
            more.put("saslContinue", Integer.valueOf(1));
            more.put("conversationId", first.get("conversationId"));
            more.put("payload", new byte[0]);
            last = command(database, more);
        }
    }

    private static String payload(Map reply) throws IOException {
        Object p = reply.get("payload");
        if (!(p instanceof byte[])) {
            throw new IOException("MongoDB's SASL reply carries no payload");
        }
        return WireBuffer.fromUtf8((byte[]) p);
    }

    /// A SCRAM user name, with `=` and `,` escaped as RFC 5802 requires.
    static String saslName(String user) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < user.length(); i++) {
            char c = user.charAt(i);
            if (c == '=') {
                sb.append("=3D");
            } else if (c == ',') {
                sb.append("=2C");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String field(String message, char key) {
        int at = 0;
        while (at < message.length()) {
            int end = message.indexOf(',', at);
            if (end < 0) {
                end = message.length();
            }
            if (end - at > 2 && message.charAt(at) == key && message.charAt(at + 1) == '=') {
                return message.substring(at + 2, end);
            }
            at = end + 1;
        }
        return null;
    }

    void close() {
        broken = true;
        wire.getConnection().close();
    }
}
