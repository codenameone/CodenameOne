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
package com.codename1.backend.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;

/**
 * A PostgreSQL client speaking the v3 frontend/backend protocol directly.
 *
 * Written rather than wrapped because there is nothing to wrap: JDBC needs a
 * driver manager, a class loader and reflection, none of which a translated
 * server binary has. The protocol is small, stable (v3 has been the wire format
 * since 7.4) and documented, so the honest option is to speak it. The same source
 * runs on both targets because it is built on {@link Tcp}, which each target
 * implements.
 *
 * Three decisions worth stating:
 *
 * - **The extended query protocol, always.** Simple Query would be fewer round
 *   trips, but it has no parameters, and a client with no way to bind a value is
 *   a client whose users concatenate SQL. Parse/Bind/Execute is what makes
 *   `query(sql, params)` safe by construction.
 * - **Text format for parameters and results.** The binary format saves parsing
 *   at the cost of a per-type encoder on both sides, and gets subtly wrong for
 *   the types nobody tested. Text is what psql sends.
 * - **SCRAM-SHA-256 is verified in both directions.** The server's final message
 *   proves it knew the stored key; skipping that check (which a client can do and
 *   still connect successfully) leaves the handshake open to a server that only
 *   pretends to be PostgreSQL.
 */
public final class Postgres {
    /** Message types the backend sends that this client acts on. */
    /** See where it is enforced, in the SCRAM exchange, for why this value. */
    private static final int MAX_SCRAM_ITERATIONS = 1000000;

    private static final int AUTHENTICATION = 'R';
    private static final int ERROR_RESPONSE = 'E';
    private static final int ROW_DESCRIPTION = 'T';
    private static final int DATA_ROW = 'D';
    private static final int COMMAND_COMPLETE = 'C';
    private static final int READY_FOR_QUERY = 'Z';

    private final Wire wire;
    private final String user;
    private final String password;
    private boolean closed;

    /**
     * @param passwordMayCrossInTheClear whether this connection is entitled to
     *        hand over the password itself. True only when TLS is up, or when the
     *        caller wrote sslmode=disable and therefore chose a clear channel
     *        knowingly.
     */
    private Postgres(Wire wire, String user, String password,
            boolean passwordMayCrossInTheClear) {
        this.wire = wire;
        this.user = user;
        this.password = password;
        this.passwordMayCrossInTheClear = passwordMayCrossInTheClear;
    }

    private final boolean passwordMayCrossInTheClear;

    /**
     * Connects, negotiates TLS when asked, authenticates, and returns a session
     * ready for queries.
     *
     * `sslMode` is "require", "prefer" or "disable". "prefer" exists because it is
     * what a local development database usually needs and a managed one usually
     * forbids; "require" fails rather than falling back, which is the only setting
     * that means anything against an attacker.
     */
    public static Postgres connect(String host, int port, String database, String user,
            String password, String sslMode, String caFile, int timeoutMillis) throws IOException {
        Tcp connection = Tcp.connect(host, port <= 0 ? 5432 : port, timeoutMillis);
        try {
            Wire wire = new Wire(connection);
            boolean plaintextByChoice = "disable".equals(sslMode);
            boolean secured = false;
            if(!plaintextByChoice) {
                boolean offered = requestTls(wire);
                boolean required = "require".equals(sslMode);
                if(!offered && required) {
                    throw new IOException("The server at " + host
                            + " refused TLS and sslmode=require");
                }
                if(offered) {
                    try {
                        connection.startTls(host, caFile);
                        secured = true;
                    } catch (IOException err) {
                        if(required) {
                            throw err;
                        }
                        // sslmode=prefer, so this falls back -- but it says so.
                        // A silent downgrade, or an unverified session presented as
                        // a verified one, is how a connection ends up looking
                        // encrypted and being neither authenticated nor private.
                        throw new IOException("TLS to " + host + " could not be "
                                + "verified (" + err.getMessage() + "). Point "
                                + "sslrootcert at the server's CA, or set "
                                + "sslmode=disable to connect in the clear "
                                + "deliberately.");
                    }
                }
            }
            // Whether the password may be sent AS ITSELF later. TLS up, or the
            // caller having written sslmode=disable, are the two ways that is the
            // caller's decision rather than the peer's.
            Postgres session = new Postgres(wire, user, password, secured || plaintextByChoice);
            session.startup(database, user);
            return session;
        } catch (IOException err) {
            connection.close();
            throw err;
        }
    }

    /**
     * The SSLRequest packet. It is not a normal message -- no type byte, and the
     * reply is a single character rather than a framed message -- because it is
     * sent before the protocol proper begins.
     */
    private static boolean requestTls(Wire wire) throws IOException {
        wire.writeIntBE(8);
        wire.writeIntBE(80877103); // 1234 << 16 | 5679
        wire.flush();   // static, pre-session: connect() closes the socket on failure
        int answer = wire.read();
        if(answer == 'S') {
            return true;
        }
        if(answer == 'N') {
            return false;
        }
        throw new IOException("The server did not answer the TLS request (got " + answer + ")");
    }

    private void startup(String database, String user) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeInt(body, 196608); // protocol 3.0
        writeCString(body, "user");
        writeCString(body, user);
        if(database != null && database.length() > 0) {
            writeCString(body, "database");
            writeCString(body, database);
        }
        // Errors come back in whatever the server's locale says otherwise, which
        // makes a failure unreadable in a log written by someone else.
        writeCString(body, "client_encoding");
        writeCString(body, "UTF8");
        body.write(0);
        byte[] payload = body.toByteArray();
        wire.writeIntBE(payload.length + 4);
        wire.writeBytes(payload);
        flushOrClose();
        authenticate();
        // Everything from here to ReadyForQuery is parameter status, the backend
        // key and notices: none of it changes what this client does.
        readUntilReady();
    }

    private void authenticate() throws IOException {
        while(true) {
            Message message = readMessage();
            if(message.type == ERROR_RESPONSE) {
                throw errorFrom(message);
            }
            if(message.type != AUTHENTICATION) {
                throw new IOException("Expected an authentication message, got '"
                        + (char)message.type + "'");
            }
            // THE METHOD CODE ITSELF is four bytes the peer sends. sslmode=prefer
            // lets whoever answers decline TLS, so an unauthenticated peer can send
            // a shorter body -- and the AIOOBE that followed is not an IOException,
            // so connect()'s cleanup did not run and the socket was leaked rather
            // than refused.
            requireBytes(message.body, 0, 4);
            int method = intAt(message.body, 0);
            if(method == 0) {
                return; // authentication complete
            }
            if(method == 3) {
                // AuthenticationCleartextPassword sends the password AS ITSELF. On
                // a clear socket that is the credential, readable by anyone on the
                // path -- and with the default sslmode=prefer the socket is clear
                // whenever the PEER answers 'N' to the SSLRequest, which an on-path
                // attacker can do for it. Impersonate the server for one more
                // message, ask for method 3, and the password arrives.
                //
                // Refused unless TLS is up or the caller wrote sslmode=disable, which
                // is them choosing a clear channel rather than an attacker choosing
                // it for them. The other methods are not this: 5 is a salted digest
                // and 10 is SCRAM, so neither hands over the password outright.
                if(!passwordMayCrossInTheClear) {
                    throw new IOException("the server asked for a cleartext password "
                            + "on a connection that is not encrypted. TLS was not "
                            + "established -- with sslmode=prefer the server can "
                            + "decline it, and an attacker in the path can decline it "
                            + "for the server. Use sslmode=require, or sslmode=disable "
                            + "if the password is meant to cross in the clear");
                }
                sendPasswordMessage(Wire.utf8(password == null ? "" : password));
                continue;
            }
            if(method == 5) {
                // MD5 is a salted digest, so a clear socket leaks something
                // crackable rather than the password -- weaker than SCRAM and not
                // the outright disclosure method 3 is. Said out loud rather than
                // refused: many servers still ask for it, and refusing would turn a
                // working deployment into a broken one over a weakness the operator
                // may already have accepted.
                if(!passwordMayCrossInTheClear) {
                    System.err.println("warning: md5 authentication over an "
                            + "unencrypted connection; the digest is offline "
                            + "crackable. Prefer sslmode=require, or a server "
                            + "configured for scram-sha-256");
                }
                byte[] salt = new byte[4];
                requireBytes(message.body, 4, 4);   // method code, then the salt
                System.arraycopy(message.body, 4, salt, 0, 4);
                sendPasswordMessage(Wire.utf8(md5Password(user, password, salt)));
                continue;
            }
            if(method == 10) {
                scram(message);
                continue;
            }
            throw new IOException("Unsupported authentication method " + method
                    + "; this client speaks SCRAM-SHA-256, md5 and cleartext");
        }
    }

    /**
     * PostgreSQL's md5 method: md5(md5(password + user) as hex, then salted).
     * Deprecated by PostgreSQL itself, and supported here only because servers
     * configured for it are still deployed.
     */
    private static String md5Password(String user, String password, byte[] salt) {
        String inner = hex(Crypto.md5(Wire.utf8((password == null ? "" : password) + user)));
        byte[] withSalt = concat(Wire.utf8(inner), salt);
        return "md5" + hex(Crypto.md5(withSalt));
    }

    /**
     * SCRAM-SHA-256 (RFC 7677), the default since PostgreSQL 10 and the only
     * method a managed instance normally offers.
     *
     * The server's final message is checked. A client that skips it authenticates
     * itself TO the server and learns nothing about who it is talking to, which
     * defeats the mutual half of the mechanism.
     */
    private void scram(Message advertised) throws IOException {
        if(!mechanismsInclude(advertised.body, "SCRAM-SHA-256")) {
            throw new IOException("The server offers no SCRAM-SHA-256; this client "
                    + "does not implement the channel-binding variants");
        }
        String clientNonce = Base64.encode(Crypto.randomBytes(18));
        String clientFirstBare = "n=,r=" + clientNonce;
        byte[] initial = Wire.utf8("n,," + clientFirstBare);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeCString(body, "SCRAM-SHA-256");
        writeInt(body, initial.length);
        body.write(initial, 0, initial.length);
        sendMessage('p', body.toByteArray());

        Message serverFirstMessage = readMessage();
        if(serverFirstMessage.type == ERROR_RESPONSE) {
            throw errorFrom(serverFirstMessage);
        }
        // Four for the code, and the payload is what follows it: a body shorter
        // than that made the length below negative.
        requireBytes(serverFirstMessage.body, 0, 4);
        if(serverFirstMessage.type != AUTHENTICATION || intAt(serverFirstMessage.body, 0) != 11) {
            throw new IOException("Expected a SASL continue message");
        }
        String serverFirst = Wire.fromUtf8(serverFirstMessage.body, 4,
                serverFirstMessage.body.length - 4);
        String nonce = field(serverFirst, 'r');
        String saltText = field(serverFirst, 's');
        String iterationText = field(serverFirst, 'i');
        if(nonce == null || saltText == null || iterationText == null
                || !nonce.startsWith(clientNonce)) {
            // A nonce that does not extend ours means the exchange was replayed or
            // rewritten; there is nothing to continue.
            throw new IOException("The server's SCRAM message is malformed or replayed");
        }
        byte[] salt = Base64.decode(saltText);
        if(salt == null) {
            throw new IOException("The server's SCRAM salt is not base64");
        }
        int iterations = parseInt(iterationText, -1);
        if(iterations < 1) {
            throw new IOException("The server's SCRAM iteration count is not a number");
        }
        // AND AN UPPER BOUND, because that count is the peer's and it multiplies
        // straight into the PBKDF2 below. The default sslmode=prefer falls back to
        // plaintext, so whoever answers the connection -- an on-path peer included
        // -- can complete the exchange with a valid extended nonce and name a count
        // near Integer.MAX_VALUE. The HMAC rounds then run on the calling thread,
        // before this side has authenticated anything. Measured against a stub that
        // does exactly that, in the packaged binary: one connection attempt held the
        // thread for about 175 seconds, and nothing stops a peer repeating it. The
        // self-test check that drives the stub fails without this bound and takes
        // those 175 seconds to do it.
        //
        // The ceiling is deliberately generous rather than tight. PostgreSQL 16 asks
        // for 4096 -- its scram_iterations default, read back from a live server --
        // so a million leaves better than two orders of magnitude for a deployment
        // that raises it on purpose, while bounding the work at a fraction of a
        // second. A server beyond that is refused rather than served slowly, because
        // the alternative is letting the peer choose how long we compute.
        if(iterations > MAX_SCRAM_ITERATIONS) {
            throw new IOException("The server asked for " + iterations
                    + " SCRAM iterations, past the " + MAX_SCRAM_ITERATIONS
                    + " this client computes");
        }

        // The password goes into PBKDF2 as its raw UTF-8, WITHOUT SASLprep, and
        // that is a deliberate limitation rather than an oversight. SASLprep
        // (RFC 4013) is a stringprep profile whose mapping step is NFKC, and
        // there is no Normalizer on this platform -- this class is translated
        // for the packaged server, so it may only use what vm/JavaAPI and
        // CLDC11 define, and neither has java.text. Implementing the part that
        // needs no Unicode tables would make things WORSE, not better:
        // PostgreSQL falls back to the raw password whenever its own saslprep
        // rejects the input, so a half-prepared password would stop matching
        // verifiers that work today. Printable ASCII -- which SASLprep leaves
        // untouched -- is therefore correct here; a password that SASLprep
        // would normalise is rejected, and has to be set in ASCII or
        // authenticated by another method.
        byte[] saltedPassword = Crypto.pbkdf2Sha256(
                Wire.utf8(password == null ? "" : password), salt, iterations, 32);
        byte[] clientKey = Crypto.hmacSha256(saltedPassword, Wire.utf8("Client Key"));
        byte[] storedKey = Crypto.sha256(clientKey);
        String clientFinalWithoutProof = "c=biws,r=" + nonce; // biws is base64("n,,")
        byte[] authMessage = Wire.utf8(clientFirstBare + "," + serverFirst + ","
                + clientFinalWithoutProof);
        byte[] clientSignature = Crypto.hmacSha256(storedKey, authMessage);
        byte[] proof = new byte[clientKey.length];
        for(int iter = 0 ; iter < proof.length ; iter++) {
            proof[iter] = (byte)(clientKey[iter] ^ clientSignature[iter]);
        }
        sendMessage('p', Wire.utf8(clientFinalWithoutProof + ",p=" + Base64.encode(proof)));

        Message finalMessage = readMessage();
        if(finalMessage.type == ERROR_RESPONSE) {
            throw errorFrom(finalMessage);
        }
        requireBytes(finalMessage.body, 0, 4);
        if(finalMessage.type != AUTHENTICATION || intAt(finalMessage.body, 0) != 12) {
            throw new IOException("Expected the SASL final message");
        }
        String serverFinal = Wire.fromUtf8(finalMessage.body, 4, finalMessage.body.length - 4);
        String signatureText = field(serverFinal, 'v');
        byte[] serverKey = Crypto.hmacSha256(saltedPassword, Wire.utf8("Server Key"));
        byte[] expected = Crypto.hmacSha256(serverKey, authMessage);
        byte[] actual = signatureText == null ? null : Base64.decode(signatureText);
        if(actual == null || !Crypto.equalsConstantTime(expected, actual)) {
            throw new IOException("The server failed the SCRAM signature check; it does "
                    + "not hold the credentials it claims to");
        }
    }

    private static boolean mechanismsInclude(byte[] body, String wanted) {
        int at = 4;
        while(at < body.length) {
            int end = at;
            while(end < body.length && body[end] != 0) {
                end++;
            }
            if(end == at) {
                return false; // the empty string terminates the list
            }
            if(wanted.equals(Wire.fromUtf8(body, at, end - at))) {
                return true;
            }
            at = end + 1;
        }
        return false;
    }

    /** One `k=value` field out of a SCRAM message. */
    private static String field(String message, char key) {
        int at = 0;
        while(at < message.length()) {
            int end = message.indexOf(',', at);
            if(end < 0) {
                end = message.length();
            }
            if(end - at > 2 && message.charAt(at) == key && message.charAt(at + 1) == '=') {
                return message.substring(at + 2, end);
            }
            at = end + 1;
        }
        return null;
    }

    private void sendPasswordMessage(byte[] password) throws IOException {
        byte[] body = new byte[password.length + 1];
        System.arraycopy(password, 0, body, 0, password.length);
        sendMessage('p', body);
    }

    // ---------------- queries ----------------

    /** Runs a statement that returns no rows, and returns the number affected. */
    public int execute(String sql, Object[] params) throws IOException {
        Result result = run(sql, params);
        return result.affected;
    }

    /**
     * Runs a query and returns each row as a column-name to value map, with the
     * SAME value types the SQLite path produces: Long, Double, String, byte[] or
     * null. A handler must not be able to tell which engine answered it.
     */
    public List query(String sql, Object[] params) throws IOException {
        return run(sql, params).rows;
    }

    private Result run(String sql, Object[] params) throws IOException {
        checkOpen();
        // Parse into the unnamed statement, bind the unnamed portal, describe,
        // execute, sync. One round trip for the lot.
        ByteArrayOutputStream parse = new ByteArrayOutputStream();
        writeCString(parse, "");
        writeCString(parse, sql);
        writeShort(parse, 0); // let the server infer every parameter type
        stageMessage('P', parse.toByteArray());

        ByteArrayOutputStream bind = new ByteArrayOutputStream();
        writeCString(bind, "");  // portal
        writeCString(bind, "");  // statement
        writeShort(bind, 0);     // parameter formats: none given, so all text
        int count = params == null ? 0 : params.length;
        writeShort(bind, count);
        for(int iter = 0 ; iter < count ; iter++) {
            byte[] encoded = encodeParameter(params[iter]);
            if(encoded == null) {
                writeInt(bind, -1); // SQL NULL, which is not the empty string
            } else {
                writeInt(bind, encoded.length);
                bind.write(encoded, 0, encoded.length);
            }
        }
        writeShort(bind, 0); // result formats: none given, so all text
        stageMessage('B', bind.toByteArray());

        ByteArrayOutputStream describe = new ByteArrayOutputStream();
        describe.write('P');
        writeCString(describe, "");
        stageMessage('D', describe.toByteArray());

        ByteArrayOutputStream execute = new ByteArrayOutputStream();
        writeCString(execute, ""); // portal
        writeInt(execute, 0);      // no row limit
        stageMessage('E', execute.toByteArray());

        stageMessage('S', new byte[0]);
        flushOrClose();

        return collect(sql);
    }

    /**
     * A frame is only as long as it says it is, checked BEFORE it is indexed.
     *
     * <p>intAt and shortAt index raw, and the row frames walk offsets the PEER
     * chose. A truncated RowDescription, a column name with no terminator, or a
     * row claiming more columns than its body holds therefore ran off the end,
     * and what came back was ArrayIndexOutOfBoundsException -- not an IOException.
     * It left collect() without draining ReadyForQuery and without closing, so
     * the session went back to the pool with the rest of the exchange still on
     * the wire, and the next borrower read that as its own answer. The default
     * sslmode=prefer falls back to plaintext, so the peer doing it need not be
     * the server.
     *
     * <p>Closing is not optional here for the same reason the size ceiling closes:
     * a frame we could not parse leaves an unknown number of bytes unread, and
     * nothing in this protocol resynchronises.
     */
    private void requireBytes(byte[] body, int offset, int count) throws IOException {
        // offset > length - count rather than offset + count > length, which
        // overflows for the lengths a hostile peer is free to name.
        if(offset < 0 || count < 0 || offset > body.length - count) {
            close();
            throw new IOException("A PostgreSQL frame is shorter than it claims");
        }
    }

    private Result collect(String sql) throws IOException {
        Result result = new Result();
        String[] names = null;
        int[] types = null;
        IOException failure = null;
        while(true) {
            Message message = readMessage();
            switch(message.type) {
                case ROW_DESCRIPTION: {
                    requireBytes(message.body, 0, 2);
                    int columns = shortAt(message.body, 0);
                    names = new String[columns];
                    types = new int[columns];
                    int at = 2;
                    for(int iter = 0 ; iter < columns ; iter++) {
                        int end = at;
                        while(end < message.body.length && message.body[end] != 0) {
                            end++;
                        }
                        // TERMINATED, not merely run to the end of the frame: a name
                        // with no NUL after it leaves "end" at the length, and the
                        // descriptor read below then starts past it.
                        requireBytes(message.body, end, 1);
                        names[iter] = Wire.fromUtf8(message.body, at, end - at);
                        at = end + 1;
                        // table oid (4), column number (2), then the type oid (4)
                        requireBytes(message.body, at, 18);
                        types[iter] = intAt(message.body, at + 6);
                        at += 18; // + type size (2), modifier (4), format (2)
                    }
                    break;
                }
                case DATA_ROW: {
                    requireBytes(message.body, 0, 2);
                    int columns = shortAt(message.body, 0);
                    // EXACTLY WHAT THE DESCRIPTION SAID, in both directions. More
                    // than that indexes past names and types, which are sized from
                    // RowDescription -- the unchecked read one array along, and the
                    // only half an earlier version of this check covered. FEWER is
                    // the quieter half and needs saying too: the loop below simply
                    // stops early and the row comes back missing its trailing
                    // fields, so a peer can delete columns from an answer without
                    // anything failing. Under the default sslmode=prefer that peer
                    // need not be the server.
                    if(names != null && columns != names.length) {
                        close();        // desynchronised; see requireBytes
                        throw new IOException("A PostgreSQL row has " + columns
                                + " columns where its description had " + names.length);
                    }
                    Map row = new LinkedHashMap();
                    int at = 2;
                    for(int iter = 0 ; iter < columns ; iter++) {
                        requireBytes(message.body, at, 4);
                        int length = intAt(message.body, at);
                        at += 4;
                        Object value;
                        if(length < 0) {
                            value = null;
                        } else {
                            requireBytes(message.body, at, length);
                            value = decode(Wire.fromUtf8(message.body, at, length),
                                    types == null ? 0 : types[iter]);
                            at += length;
                        }
                        row.put(names == null ? String.valueOf(iter) : names[iter], value);
                    }
                    result.rows.add(row);
                    break;
                }
                case COMMAND_COMPLETE: {
                    // The NUL it counts on being there. An empty body makes the
                    // length below -1.
                    requireBytes(message.body, 0, 1);
                    String tag = Wire.fromUtf8(message.body, 0, message.body.length - 1);
                    // A COMMIT on a transaction the server has already marked
                    // aborted completes with the ROLLBACK tag rather than an
                    // error -- which happens whenever a statement failed and the
                    // transaction body caught it and carried on. Every change in
                    // the transaction is discarded, and reading only the row
                    // count out of this reports that as a successful commit and
                    // hands the caller the body's result.
                    if("ROLLBACK".equals(tag) && "COMMIT".equalsIgnoreCase(sql.trim())) {
                        failure = new IOException("COMMIT rolled the transaction back: the "
                                + "server had already marked it aborted, so nothing in it "
                                + "was applied");
                    }
                    result.affected = affectedFrom(tag);
                    break;
                }
                case ERROR_RESPONSE:
                    // Not thrown here: the server still owes us a ReadyForQuery, and
                    // leaving it unread desynchronises every later statement.
                    failure = errorFrom(message, sql);
                    break;
                case READY_FOR_QUERY:
                    if(failure != null) {
                        throw failure;
                    }
                    return result;
                default:
                    break; // ParseComplete, BindComplete, NoData, notices, parameter status
            }
        }
    }

    /**
     * "INSERT 0 3", "UPDATE 2", "DELETE 1", "SELECT 7": the count is the last
     * word, and INSERT is the one with an oid before it.
     */
    private static int affectedFrom(String tag) {
        int space = tag.lastIndexOf(' ');
        return space < 0 ? 0 : parseInt(tag.substring(space + 1), 0);
    }

    /**
     * Parameters go out as text, so this is a rendering rather than an encoding.
     * A byte[] becomes a bytea hex literal, which is what the server expects in
     * text format; everything else is its ordinary string form.
     */
    private static byte[] encodeParameter(Object value) {
        if(value == null) {
            return null;
        }
        if(value instanceof byte[]) {
            return Wire.utf8("\\x" + hex((byte[])value));
        }
        if(value instanceof Boolean) {
            return Wire.utf8(((Boolean)value).booleanValue() ? "t" : "f");
        }
        return Wire.utf8(String.valueOf(value));
    }

    /**
     * Maps a text-format value to the same Java types the SQLite path returns.
     * The OIDs are the stable built-in ones from pg_type; a type this does not
     * know stays a String, which is what the server sent.
     */
    private static Object decode(String text, int typeOid) {
        switch(typeOid) {
            case 16: // bool
                return Long.valueOf("t".equals(text) ? 1 : 0);
            case 20: // int8
            case 21: // int2
            case 23: // int4
            case 26: // oid
                try {
                    return Long.valueOf(Long.parseLong(text.trim()));
                } catch (NumberFormatException err) {
                    return text;
                }
            case 700:  // float4
            case 701:  // float8
                try {
                    return Double.valueOf(Double.parseDouble(text.trim()));
                } catch (NumberFormatException err) {
                    return text;
                }
            case 1700: // numeric
                // NOT a double. numeric is arbitrary precision, and
                // Double.parseDouble does not fail on the values it cannot
                // hold: 1e999 comes back as infinity, which Json then writes
                // as null, and a merely large numeric comes back quietly
                // rounded. Both report a successful query with a value the
                // database does not hold. The exact text is what the server
                // sent, so that is what the caller gets -- matching DECIMAL
                // on the MySQL path, which is the same kind of column.
                return text;
            case 17: { // bytea, sent as \x48656c6c6f
                if(text.length() >= 2 && text.charAt(0) == '\\' && text.charAt(1) == 'x') {
                    byte[] out = unhex(text.substring(2));
                    if(out != null) {
                        return out;
                    }
                }
                return text;
            }
            default:
                return text;
        }
    }

    // ---------------- plumbing ----------------

    public void close() {
        if(closed) {
            return;
        }
        closed = true;
        try {
            // Terminate, so the server logs a clean disconnect rather than a
            // broken connection for every pooled session that ends.
            stageMessage('X', new byte[0]);
            wire.flush();
        } catch (IOException ignored) {
            // the connection is going away regardless
        }
        wire.getConnection().close();
    }

    public boolean isClosed() {
        return closed;
    }

    private void checkOpen() throws IOException {
        if(closed) {
            throw new IOException("The PostgreSQL connection is closed");
        }
    }

    private void readUntilReady() throws IOException {
        while(true) {
            Message message = readMessage();
            if(message.type == ERROR_RESPONSE) {
                throw errorFrom(message);
            }
            if(message.type == READY_FOR_QUERY) {
                return;
            }
        }
    }

    private void stageMessage(int type, byte[] body) {
        wire.writeByte(type);
        wire.writeIntBE(body.length + 4);
        wire.writeBytes(body);
    }

    private void sendMessage(int type, byte[] body) throws IOException {
        stageMessage(type, body);
        flushOrClose();
    }

    /**
     * Sends what is staged, and ends the session if it cannot.
     *
     * <p>The read side closes on any failure; this is the same rule for the other
     * direction, which it did not have. take() clears the staged frame before the
     * write is attempted, so a peer that resets mid-write leaves a connection that
     * has had part of a protocol frame written to it and no record of the rest --
     * and `closed` stayed false, so isOpen() called it reusable and the pool
     * handed it out.
     *
     * <p>close() sends its own terminate through wire.flush() directly rather than
     * through here, which is also what keeps this from recursing.
     */
    private void flushOrClose() throws IOException {
        try {
            wire.flush();
        } catch (IOException failed) {
            close();
            throw failed;
        }
    }

    private Message readMessage() throws IOException {
        try {
            return readMessageFrame();
        } catch (IOException failed) {
            // ANY read failure here ends the session. EOF between messages threw
            // with `closed` still false, so isOpen() went on describing a dead
            // connection as usable: DbPool handed it out again and every
            // disconnect left another descriptor behind. A failure part way
            // through a message is the same answer for a different reason --
            // whatever is left on the wire is unknown, so nothing can be asked of
            // it. close() is idempotent, so the branches below that already close
            // before throwing lose nothing by passing through here.
            close();
            throw failed;
        }
    }

    private Message readMessageFrame() throws IOException {
        int type = wire.read();
        if(type < 0) {
            throw new IOException("The PostgreSQL connection closed unexpectedly");
        }
        int length = wire.readIntBE();
        if(length < 4) {
            // CLOSED, not merely refused. A length this code will not honour leaves
            // the message still on the wire, so the connection is desynchronised
            // from here on -- and DbPool returns a released connection to the idle
            // list without asking, so the next borrower would read the tail of
            // this message as the head of its own answer. There is no resync in
            // this protocol; the only honest thing left is to take the connection
            // out of service.
            close();
            throw new IOException("A PostgreSQL message claims length " + length);
        }
        // BEFORE THE ALLOCATION, and before anything about this peer is trusted.
        // The header carries a 32-bit length the SERVER chose, so a peer that is
        // merely on the path -- which the default sslmode=prefer permits, since it
        // can decline TLS on the server's behalf -- can name a length near
        // Integer.MAX_VALUE and have this ask for a 2GB array in one go. The
        // OutOfMemoryError that follows is not an IOException: it unwinds past
        // every catch here and takes the process with it, before a single
        // authentication message has been exchanged.
        if(length - 4 > SqlLimits.maxMessageBytes()) {
            close();                    // desynchronised; see the branch above
            throw new IOException("A PostgreSQL message claims " + (length - 4)
                    + " bytes, past the " + SqlLimits.maxMessageBytes()
                    + " CN1_DB_MAX_MESSAGE_MB allows");
        }
        Message message = new Message();
        message.type = type;
        message.body = wire.readFully(length - 4);
        return message;
    }

    private static IOException errorFrom(Message message) {
        return errorFrom(message, null);
    }

    /**
     * An ErrorResponse is a set of typed fields; 'M' is the human message, 'C' the
     * SQLSTATE. Both go into the exception, because the SQLSTATE is what tells a
     * caller apart a unique-violation from a syntax error.
     */
    private static IOException errorFrom(Message message, String sql) {
        String detail = null;
        String state = null;
        int at = 0;
        while(at < message.body.length && message.body[at] != 0) {
            int field = message.body[at];
            int end = at + 1;
            while(end < message.body.length && message.body[end] != 0) {
                end++;
            }
            String value = Wire.fromUtf8(message.body, at + 1, end - at - 1);
            if(field == 'M') {
                detail = value;
            } else if(field == 'C') {
                state = value;
            }
            at = end + 1;
        }
        return new IOException("PostgreSQL error"
                + (state == null ? "" : " " + state) + ": "
                + (detail == null ? "unknown" : detail)
                + (sql == null ? "" : " [" + sql + "]"));
    }

    private static final class Message {
        int type;
        byte[] body;
    }

    private static final class Result {
        final List rows = new ArrayList();
        int affected;
    }

    private static int intAt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
    }

    private static int shortAt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >> 24) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 8) & 0xff);
        out.write(value & 0xff);
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write((value >> 8) & 0xff);
        out.write(value & 0xff);
    }

    private static void writeCString(ByteArrayOutputStream out, String value) {
        byte[] data = Wire.utf8(value);
        out.write(data, 0, data.length);
        out.write(0);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    static String hex(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for(int iter = 0 ; iter < data.length ; iter++) {
            out.append(HEX[(data[iter] >> 4) & 0xf]).append(HEX[data[iter] & 0xf]);
        }
        return out.toString();
    }

    private static byte[] unhex(String text) {
        if((text.length() % 2) != 0) {
            return null;
        }
        byte[] out = new byte[text.length() / 2];
        for(int iter = 0 ; iter < out.length ; iter++) {
            int high = digit(text.charAt(iter * 2));
            int low = digit(text.charAt(iter * 2 + 1));
            if(high < 0 || low < 0) {
                return null;
            }
            out[iter] = (byte)((high << 4) | low);
        }
        return out;
    }

    private static int digit(char c) {
        if(c >= '0' && c <= '9') {
            return c - '0';
        }
        if(c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if(c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException err) {
            return fallback;
        }
    }
}
