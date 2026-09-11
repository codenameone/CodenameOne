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

import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;

/**
 * A MySQL client speaking the client/server protocol directly, for the same
 * reason as {@link Postgres}: a translated server binary has no JDBC.
 *
 * Everything goes through prepared statements (COM_STMT_PREPARE / EXECUTE) and
 * therefore through MySQL's BINARY result format. That is more code than sending
 * COM_QUERY text, and it is the only way to bind a parameter -- a text-protocol
 * client has to build SQL by concatenation, and this API refuses to offer that.
 *
 * Authentication covers what is actually deployed: caching_sha2_password (the
 * MySQL 8 default) and mysql_native_password (5.7 and MariaDB). The caching_sha2
 * FULL exchange -- which the server demands the first time a password is used,
 * before its cache is warm -- sends the password to the server, so this client
 * does it only on a TLS connection and says so rather than falling back to the
 * RSA-wrapped variant, which would be a second cryptographic path to get wrong.
 */
public final class MySql {
    /** Capability bits, from the protocol's CLIENT_* set. */
    private static final int CLIENT_LONG_PASSWORD = 0x00000001;
    private static final int CLIENT_LONG_FLAG = 0x00000004;
    private static final int CLIENT_CONNECT_WITH_DB = 0x00000008;
    private static final int CLIENT_LOCAL_FILES = 0x00000080;
    private static final int CLIENT_PROTOCOL_41 = 0x00000200;
    private static final int CLIENT_SSL = 0x00000800;
    private static final int CLIENT_TRANSACTIONS = 0x00002000;
    private static final int CLIENT_SECURE_CONNECTION = 0x00008000;
    private static final int CLIENT_PLUGIN_AUTH = 0x00080000;
    private static final int CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA = 0x00200000;

    private static final int OK_PACKET = 0x00;
    private static final int EOF_PACKET = 0xfe;
    private static final int ERROR_PACKET = 0xff;

    private final Wire wire;
    private int sequence;
    private long lastInsertId;
    private boolean closed;

    private MySql(Wire wire) {
        this.wire = wire;
    }

    /**
     * Connects, optionally upgrades to TLS, and authenticates.
     *
     * `sslMode` is "require", "prefer" or "disable", as in {@link Postgres}.
     */
    public static MySql connect(String host, int port, String database, String user,
            String password, String sslMode, String caFile, int timeoutMillis) throws IOException {
        Tcp connection = Tcp.connect(host, port <= 0 ? 3306 : port, timeoutMillis);
        try {
            MySql session = new MySql(new Wire(connection));
            session.handshake(host, database, user, password, sslMode, caFile);
            return session;
        } catch (IOException err) {
            connection.close();
            throw err;
        }
    }

    private void handshake(String host, String database, String user, String password,
            String sslMode, String caFile) throws IOException {
        Packet greeting = readPacket();
        Reader reader = new Reader(greeting.body);
        int protocol = reader.u8();
        if(protocol == ERROR_PACKET) {
            throw errorFrom(greeting, null);
        }
        if(protocol != 10) {
            throw new IOException("Unsupported MySQL handshake protocol " + protocol);
        }
        reader.cString(); // server version
        reader.skip(4);   // connection id
        byte[] scrambleFirst = reader.bytes(8);
        reader.skip(1);   // filler
        int serverCapabilities = reader.u16();
        byte[] scramble = scrambleFirst;
        String plugin = "mysql_native_password";
        if(reader.remaining() > 0) {
            reader.skip(1); // character set
            reader.skip(2); // status flags
            serverCapabilities |= reader.u16() << 16;
            int scrambleLength = reader.u8();
            reader.skip(10); // reserved
            // The documented length is the total including the first 8 bytes and a
            // trailing NUL, and servers disagree about the NUL -- so take what is
            // there rather than what is claimed.
            int secondLength = scrambleLength > 8 ? scrambleLength - 8 : 12;
            if(secondLength > reader.remaining()) {
                secondLength = reader.remaining();
            }
            byte[] scrambleSecond = reader.bytes(secondLength);
            scramble = trimTrailingNul(concat(scrambleFirst, scrambleSecond));
            if(reader.remaining() > 0) {
                plugin = reader.cString();
            }
        }

        boolean useTls = !"disable".equals(sslMode);
        if(useTls && (serverCapabilities & CLIENT_SSL) == 0) {
            if("require".equals(sslMode)) {
                throw new IOException("The MySQL server at " + host
                        + " does not offer TLS and sslmode=require");
            }
            useTls = false;
        }

        // Deliberately NOT CLIENT_FOUND_ROWS. With it MySQL reports the rows an
        // UPDATE MATCHED rather than the rows it changed, so an update that found
        // its row and altered nothing answers 1 where Database.execute documents
        // "the number of rows changed" and where SQLite and Postgres both answer
        // 0. Code that reads 0 as "no such row" would have been told it succeeded.
        int capabilities = CLIENT_LONG_PASSWORD | CLIENT_LONG_FLAG
                | CLIENT_PROTOCOL_41 | CLIENT_TRANSACTIONS | CLIENT_SECURE_CONNECTION
                | CLIENT_PLUGIN_AUTH | CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA;
        if(database != null && database.length() > 0) {
            capabilities |= CLIENT_CONNECT_WITH_DB;
        }
        if(useTls) {
            capabilities |= CLIENT_SSL;
            // The SSLRequest packet is the first 32 bytes of the login packet and
            // nothing else: the credentials must not cross in the clear, which is
            // the entire point of sending it separately.
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            writeIntLE(request, capabilities);
            writeIntLE(request, 0x01000000); // max packet size
            request.write(45);               // utf8mb4_general_ci
            for(int iter = 0 ; iter < 23 ; iter++) {
                request.write(0);
            }
            sendPacket(request.toByteArray());
            try {
                wire.getConnection().startTls(host, caFile);
            } catch (IOException err) {
                // The SSLRequest packet has already gone out, so this connection
                // cannot go back to plaintext -- the server is waiting for a
                // handshake. Saying what to do beats a bare PKIX stack trace,
                // which is what a self-signed development server produces.
                throw new IOException("TLS to " + host + " could not be verified ("
                        + err.getMessage() + "). Point sslrootcert at the server's "
                        + "CA, or set sslmode=disable to connect in the clear "
                        + "deliberately.");
            }
        }
        // LOCAL INFILE lets a server ask the client for a file by path. Nothing
        // here needs it, and leaving it enabled turns a compromised or hostile
        // server into a file read on this host.
        capabilities &= ~CLIENT_LOCAL_FILES;

        byte[] authResponse = authResponse(plugin, password, scramble);
        ByteArrayOutputStream login = new ByteArrayOutputStream();
        writeIntLE(login, capabilities);
        writeIntLE(login, 0x01000000);
        login.write(45);
        for(int iter = 0 ; iter < 23 ; iter++) {
            login.write(0);
        }
        writeCString(login, user);
        writeLengthEncoded(login, authResponse);
        if((capabilities & CLIENT_CONNECT_WITH_DB) != 0) {
            writeCString(login, database);
        }
        writeCString(login, plugin);
        sendPacket(login.toByteArray());

        finishAuthentication(password, scramble, useTls);
    }

    /**
     * Drives the post-login exchange: an OK ends it, an AuthSwitchRequest changes
     * plugin, and caching_sha2_password may ask for the full exchange.
     */
    private void finishAuthentication(String password, byte[] scramble, boolean secure)
            throws IOException {
        while(true) {
            Packet packet = readPacket();
            int head = packet.body[0] & 0xff;
            if(head == OK_PACKET) {
                return;
            }
            if(head == ERROR_PACKET) {
                throw errorFrom(packet, null);
            }
            if(head == EOF_PACKET) {
                // AuthSwitchRequest: plugin name, then a fresh scramble.
                Reader reader = new Reader(packet.body);
                reader.skip(1);
                String plugin = reader.cString();
                byte[] fresh = trimTrailingNul(reader.rest());
                sendPacket(authResponse(plugin, password, fresh));
                scramble = fresh;
                continue;
            }
            if(head == 0x01) {
                // AuthMoreData. For caching_sha2_password 0x03 means the server's
                // cache already had this password and 0x04 means it did not.
                int status = packet.body.length > 1 ? packet.body[1] & 0xff : 0;
                if(status == 3) {
                    continue; // fast path accepted; an OK follows
                }
                if(status == 4) {
                    if(!secure) {
                        throw new IOException("This MySQL server needs the full "
                                + "caching_sha2_password exchange, which sends the "
                                + "password; connect with sslmode=require (or run "
                                + "ALTER USER ... IDENTIFIED WITH mysql_native_password)");
                    }
                    byte[] clear = Wire.utf8(password == null ? "" : password);
                    byte[] terminated = new byte[clear.length + 1];
                    System.arraycopy(clear, 0, terminated, 0, clear.length);
                    sendPacket(terminated);
                    continue;
                }
                throw new IOException("Unexpected MySQL auth continuation " + status);
            }
            throw new IOException("Unexpected MySQL authentication packet 0x"
                    + Integer.toHexString(head));
        }
    }

    private static byte[] authResponse(String plugin, String password, byte[] scramble)
            throws IOException {
        byte[] secret = Wire.utf8(password == null ? "" : password);
        if(secret.length == 0) {
            return new byte[0];
        }
        if("caching_sha2_password".equals(plugin)) {
            // XOR(SHA256(password), SHA256(SHA256(SHA256(password)) + scramble))
            byte[] first = Crypto.sha256(secret);
            byte[] second = Crypto.sha256(first);
            byte[] third = Crypto.sha256(concat(second, scramble));
            return xor(first, third);
        }
        // NOT mysql_old_password. It was accepted here and answered with the
        // mysql_native_password scramble, which is a different algorithm entirely --
        // the pre-4.1 one -- so such an account was always rejected by the server
        // while the message below already said this client does not speak it. The
        // plugin is removed in MySQL 8.0 and its hash is broken by design, so it
        // falls through to that message rather than being implemented.
        if("mysql_native_password".equals(plugin)) {
            // XOR(SHA1(password), SHA1(scramble + SHA1(SHA1(password))))
            byte[] first = Crypto.sha1(secret);
            byte[] second = Crypto.sha1(first);
            byte[] third = Crypto.sha1(concat(scramble, second));
            return xor(first, third);
        }
        throw new IOException("Unsupported MySQL authentication plugin '" + plugin
                + "'; this client speaks caching_sha2_password and mysql_native_password");
    }

    // ---------------- queries ----------------

    public int execute(String sql, Object[] params) throws IOException {
        return (int)runPrepared(sql, params, null);
    }

    public List query(String sql, Object[] params) throws IOException {
        List rows = new ArrayList();
        runPrepared(sql, params, rows);
        return rows;
    }

    /**
     * Runs a statement through COM_QUERY rather than the prepared-statement
     * protocol, and takes no parameters.
     *
     * This exists for exactly one reason: MySQL refuses to prepare its
     * transaction-control statements ("This command is not supported in the
     * prepared statement protocol yet"), so BEGIN, COMMIT and ROLLBACK cannot go
     * through {@link #execute}. It is private, and the three callers below pass
     * constants -- a text-protocol entry point taking a caller's string is the
     * concatenation hole this client exists to avoid.
     */
    private void command(String sql) throws IOException {
        checkOpen();
        sequence = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x03); // COM_QUERY
        byte[] text = Wire.utf8(sql);
        out.write(text, 0, text.length);
        sendPacket(out.toByteArray());

        Packet packet = readPacket();
        int head = packet.body[0] & 0xff;
        if(head == ERROR_PACKET) {
            throw errorFrom(packet, sql);
        }
        if(head == OK_PACKET || head == EOF_PACKET) {
            return;
        }
        // A result set. Nothing here wants the rows, but they have to be drained
        // or the next statement reads them as its own answer.
        Reader header = new Reader(packet.body);
        int columns = (int)header.lengthEncoded();
        for(int iter = 0 ; iter < columns ; iter++) {
            readPacket();
        }
        readPacket(); // EOF closing the definitions
        while(true) {
            Packet row = readPacket();
            int marker = row.body[0] & 0xff;
            if(marker == ERROR_PACKET) {
                throw errorFrom(row, sql);
            }
            if(marker == EOF_PACKET && row.body.length < 9) {
                return;
            }
        }
    }

    /** Opens a transaction. See {@link #command} for why this is not `execute`. */
    public void begin() throws IOException {
        command("BEGIN");
    }

    public void commit() throws IOException {
        command("COMMIT");
    }

    public void rollback() throws IOException {
        command("ROLLBACK");
    }

    public long lastInsertId() {
        return lastInsertId;
    }

    /**
     * Prepares, executes and closes one statement. Returns the affected-row count
     * and, when `rows` is not null, appends the decoded result set to it.
     *
     * The statement is closed rather than cached: a cache keyed by SQL text is
     * where a pooled connection starts leaking server-side handles, and preparing
     * costs one round trip.
     */
    private long runPrepared(String sql, Object[] params, List rows) throws IOException {
        checkOpen();
        sequence = 0;
        ByteArrayOutputStream prepare = new ByteArrayOutputStream();
        prepare.write(0x16); // COM_STMT_PREPARE
        byte[] text = Wire.utf8(sql);
        prepare.write(text, 0, text.length);
        sendPacket(prepare.toByteArray());

        Packet response = readPacket();
        if((response.body[0] & 0xff) == ERROR_PACKET) {
            throw errorFrom(response, sql);
        }
        Reader reader = new Reader(response.body);
        reader.skip(1);
        int statementId = reader.i32();
        int columnCount = reader.u16();
        int parameterCount = reader.u16();
        // Definitions for the parameters and then the columns, each list closed by
        // an EOF packet. They are read and discarded for parameters -- the values
        // are typed by this client, not by the server's guess.
        if(parameterCount > 0) {
            for(int iter = 0 ; iter < parameterCount ; iter++) {
                readPacket();
            }
            readPacket(); // EOF
        }
        Column[] columns = new Column[columnCount];
        if(columnCount > 0) {
            for(int iter = 0 ; iter < columnCount ; iter++) {
                columns[iter] = parseColumn(readPacket().body);
            }
            readPacket(); // EOF
        }

        try {
            // THE COUNT HAS TO MATCH, and it is checked HERE rather than left to the
            // server. COM_STMT_EXECUTE carries a null bitmap and a type table sized
            // by this client, while the server decodes them using the statement's
            // own parameter count -- so a mismatch does not reliably produce an
            // error, it produces a different reading of the same bytes.
            //
            // Measured against a real server: with one or more placeholders MySQL
            // does catch a wrong count, but a statement with NO placeholders and a
            // value supplied simply EXECUTED -- an insert the caller half wrote,
            // committed, with nothing said. Inside the try so the COM_STMT_CLOSE
            // below still runs; a refused statement must not also leak one.
            int supplied = params == null ? 0 : params.length;
            if(supplied != parameterCount) {
                throw new IOException("the statement has " + parameterCount + " parameter"
                        + (parameterCount == 1 ? "" : "s") + " and " + supplied
                        + " were supplied [" + sql + "]");
            }
            return executePrepared(statementId, params, columns, rows, sql);
        } finally {
            sequence = 0;
            ByteArrayOutputStream close = new ByteArrayOutputStream();
            close.write(0x19); // COM_STMT_CLOSE, which the server does not answer
            writeIntLE(close, statementId);
            try {
                sendPacket(close.toByteArray());
            } catch (IOException ignored) {
                // the connection is already broken; the caller sees the real error
            }
        }
    }

    private long executePrepared(int statementId, Object[] params, Column[] columns,
            List rows, String sql) throws IOException {
        sequence = 0;
        int count = params == null ? 0 : params.length;
        ByteArrayOutputStream execute = new ByteArrayOutputStream();
        execute.write(0x17); // COM_STMT_EXECUTE
        writeIntLE(execute, statementId);
        execute.write(0);    // no cursor
        writeIntLE(execute, 1); // iteration count, always 1
        if(count > 0) {
            byte[] nulls = new byte[(count + 7) / 8];
            for(int iter = 0 ; iter < count ; iter++) {
                if(params[iter] == null) {
                    nulls[iter / 8] |= (byte)(1 << (iter % 8));
                }
            }
            execute.write(nulls, 0, nulls.length);
            execute.write(1); // the types that follow are new
            for(int iter = 0 ; iter < count ; iter++) {
                int type = typeOf(params[iter]);
                execute.write(type);
                execute.write(0); // unsigned flag
            }
            for(int iter = 0 ; iter < count ; iter++) {
                writeBinaryValue(execute, params[iter]);
            }
        }
        sendPacket(execute.toByteArray());

        Packet first = readPacket();
        int head = first.body[0] & 0xff;
        if(head == ERROR_PACKET) {
            throw errorFrom(first, sql);
        }
        if(head == OK_PACKET && columns.length == 0) {
            Reader reader = new Reader(first.body);
            reader.skip(1);
            long affected = reader.lengthEncoded();
            long generated = reader.lengthEncoded();
            // Only when the statement actually generated one. Every successful
            // command answers with an OK packet, and an UPDATE or a DDL reports
            // zero here -- so assigning unconditionally let the next statement
            // after an insert wipe the id, and lastInsertId() is documented as
            // the MOST RECENT INSERT's. The SQLite and Java SE arms both keep
            // the last generated key, and the arms must not disagree.
            if(generated != 0) {
                lastInsertId = generated;
            }
            return affected;
        }
        // A result set: a column count, the definitions again, then binary rows.
        Reader header = new Reader(first.body);
        int resultColumns = (int)header.lengthEncoded();
        Column[] resultDefinitions = new Column[resultColumns];
        for(int iter = 0 ; iter < resultColumns ; iter++) {
            resultDefinitions[iter] = parseColumn(readPacket().body);
        }
        readPacket(); // EOF ending the definitions
        while(true) {
            Packet packet = readPacket();
            int marker = packet.body[0] & 0xff;
            if(marker == ERROR_PACKET) {
                throw errorFrom(packet, sql);
            }
            // An EOF packet is under 9 bytes; a row whose first byte is 0xfe is
            // longer, which is how the two are told apart.
            if(marker == EOF_PACKET && packet.body.length < 9) {
                return 0;
            }
            if(rows != null) {
                rows.add(decodeBinaryRow(packet.body, resultDefinitions));
            }
        }
    }

    /**
     * A binary row: a 0x00 marker, a null bitmap offset by two bits, then each
     * non-null value in its column's binary form.
     */
    private static Map decodeBinaryRow(byte[] body, Column[] columns) throws IOException {
        Reader reader = new Reader(body);
        reader.skip(1);
        byte[] nulls = reader.bytes((columns.length + 9) / 8);
        Map row = new LinkedHashMap();
        for(int iter = 0 ; iter < columns.length ; iter++) {
            int bit = iter + 2;
            boolean isNull = (nulls[bit / 8] & (1 << (bit % 8))) != 0;
            row.put(columns[iter].name, isNull ? null : readBinaryValue(reader, columns[iter]));
        }
        return row;
    }

    /**
     * Decoded to the same Java types the SQLite and PostgreSQL paths produce.
     * Everything textual is a String unless its column is binary (character set
     * 63), which is what separates a BLOB from a TEXT on this wire.
     */
    private static Object readBinaryValue(Reader reader, Column column) throws IOException {
        switch(column.type) {
            case 0x01: // TINY
                return Long.valueOf(column.unsigned ? reader.u8() : (byte)reader.u8());
            case 0x02: // SHORT
            case 0x0d: // YEAR
                return Long.valueOf(column.unsigned ? reader.u16() : (short)reader.u16());
            case 0x03: // LONG
            case 0x09: // INT24
                return Long.valueOf(column.unsigned ? (reader.i32() & 0xffffffffL) : reader.i32());
            case 0x08: { // LONGLONG
                // BIGINT UNSIGNED above Long.MAX_VALUE has no long that holds it:
                // the high bit is a sign bit to Java, so the value comes back
                // NEGATIVE -- an id or a counter arriving as a different number
                // than the row holds, in the query result and in the JSON built
                // from it. Such a value keeps its exact unsigned decimal as text,
                // the same answer DECIMAL gets above and numeric gets on the
                // PostgreSQL side: a type the API cannot hold is not rounded or
                // wrapped into one that fits. Everything that DOES fit stays a
                // Long, which is every signed BIGINT and every unsigned one below
                // the boundary.
                long value = reader.i64();
                if(column.unsigned && value < 0) {
                    return unsignedText(value);
                }
                return Long.valueOf(value);
            }
            case 0x04: // FLOAT
                return Double.valueOf(Float.intBitsToFloat(reader.i32()));
            case 0x05: // DOUBLE
                return Double.valueOf(Double.longBitsToDouble(reader.i64()));
            case 0x0a: // DATE
            case 0x0c: // DATETIME
            case 0x07: // TIMESTAMP
                return reader.temporal();
            case 0x0b: // TIME
                return reader.time();
            case 0x00: // DECIMAL
            case 0xf6: { // NEWDECIMAL
                // Sent as its decimal TEXT even in the binary protocol, and
                // flagged character set 63 like every other numeric column --
                // so the binary fallback below would hand a money column back
                // as a byte[], which JSON then base64s. Kept as the exact text
                // rather than parsed to a double: DECIMAL(65,30) is why the
                // column type was chosen, and a double cannot hold it.
                byte[] digits = reader.lengthEncodedBytes();
                return digits == null ? null : Wire.fromUtf8(digits);
            }
            default: {
                byte[] data = reader.lengthEncodedBytes();
                if(data == null) {
                    return null;
                }
                return column.binary ? (Object)data : (Object)Wire.fromUtf8(data);
            }
        }
    }

    /**
     * The exact decimal for a 64-bit value read as unsigned. Long.toString would
     * print the negative wrap, and there is no unsigned formatter to call here,
     * so it is divided out by hand: the top bit is worth 2^63, and the rest is
     * an ordinary positive long.
     */
    private static String unsignedText(long value) {
        long quotient = (value >>> 1) / 5;          // value / 10, unsigned
        long remainder = value - quotient * 10;
        if(remainder > 9) {                          // the halving can be one low
            quotient += remainder / 10;
            remainder %= 10;
        }
        return Long.toString(quotient) + (char)('0' + remainder);
    }

    private static int typeOf(Object value) {
        if(value == null) {
            return 0x06; // NULL
        }
        if(value instanceof Integer || value instanceof Long || value instanceof Short
                || value instanceof Byte || value instanceof Boolean) {
            return 0x08; // LONGLONG, so one encoder covers every integer width
        }
        if(value instanceof Double || value instanceof Float) {
            return 0x05; // DOUBLE
        }
        if(value instanceof byte[]) {
            return 0xfc; // BLOB
        }
        return 0xfd; // VAR_STRING
    }

    private static void writeBinaryValue(ByteArrayOutputStream out, Object value) {
        if(value == null) {
            return; // carried by the null bitmap, with no bytes on the wire
        }
        if(value instanceof Boolean) {
            writeLongLE(out, ((Boolean)value).booleanValue() ? 1 : 0);
            return;
        }
        if(value instanceof Integer || value instanceof Long || value instanceof Short
                || value instanceof Byte) {
            writeLongLE(out, ((Number)value).longValue());
            return;
        }
        if(value instanceof Double || value instanceof Float) {
            writeLongLE(out, Double.doubleToLongBits(((Number)value).doubleValue()));
            return;
        }
        if(value instanceof byte[]) {
            writeLengthEncoded(out, (byte[])value);
            return;
        }
        writeLengthEncoded(out, Wire.utf8(String.valueOf(value)));
    }

    private static Column parseColumn(byte[] body) throws IOException {
        Reader reader = new Reader(body);
        reader.lengthEncodedBytes(); // catalog
        reader.lengthEncodedBytes(); // schema
        reader.lengthEncodedBytes(); // table
        reader.lengthEncodedBytes(); // original table
        byte[] name = reader.lengthEncodedBytes();
        reader.lengthEncodedBytes(); // original name
        reader.lengthEncoded();      // length of the fixed fields
        Column column = new Column();
        column.name = Wire.fromUtf8(name);
        column.binary = reader.u16() == 63; // character set 63 is "binary"
        reader.skip(4);                     // column length
        column.type = reader.u8();
        // The flags follow the type, and 0x0020 is UNSIGNED. Skipping them meant every
        // integer was decoded at one fixed signedness, so a signed TINYINT of -1 came
        // back as 255 and a SMALLINT UNSIGNED of 65535 came back as -1. Nothing fails;
        // the row is simply wrong, which is the worst way for this to be wrong.
        column.unsigned = (reader.u16() & 0x0020) != 0;
        return column;
    }

    private static final class Column {
        String name;
        int type;
        boolean binary;
        boolean unsigned;
    }

    // ---------------- packets ----------------

    public void close() {
        if(closed) {
            return;
        }
        closed = true;
        try {
            sequence = 0;
            sendPacket(new byte[]{0x01}); // COM_QUIT
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
            throw new IOException("The MySQL connection is closed");
        }
    }

    /** The most one MySQL packet can carry: the length field is 24 bits. */
    private static final int MAX_PACKET_BODY = 0xffffff;

    /**
     * Sends a body, split across packets when it does not fit in one.
     *
     * A body of 16MB or more -- an ordinary large byte[] parameter -- has to go out
     * as consecutive full-length packets with running sequence numbers. Writing the
     * low 24 bits of the length and then the whole body left the server reading the
     * remainder as the next packet's header, which does not fail: the connection is
     * simply desynchronised from that point on, and every answer after it is
     * nonsense.
     *
     * A body whose length is an exact multiple of the maximum ends with an empty
     * packet, which is how the protocol says the sequence is over.
     */
    private void sendPacket(byte[] body) throws IOException {
        int offset = 0;
        while(true) {
            int chunk = body.length - offset;
            if(chunk > MAX_PACKET_BODY) {
                chunk = MAX_PACKET_BODY;
            }
            wire.writeByte(chunk & 0xff);
            wire.writeByte((chunk >> 8) & 0xff);
            wire.writeByte((chunk >> 16) & 0xff);
            wire.writeByte(sequence++ & 0xff);
            if(chunk > 0) {
                wire.writeBytes(body, offset, chunk);
            }
            offset += chunk;
            if(chunk < MAX_PACKET_BODY) {
                break;
            }
        }
        wire.flush();
    }

    private Packet readPacket() throws IOException {
        int low = wire.read();
        if(low < 0) {
            throw new IOException("The MySQL connection closed unexpectedly");
        }
        int length = low | (wire.read() << 8) | (wire.read() << 16);
        sequence = wire.read() + 1;
        Packet packet = new Packet();
        packet.body = wire.readFully(length);
        if(length == MAX_PACKET_BODY) {
            // A full-length packet is continued by the next one, and the value only
            // ends at a packet shorter than the maximum. Stopping at the first would
            // hand the caller a truncated value and leave the following header to be
            // read as data.
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            all.write(packet.body, 0, packet.body.length);
            // BOUNDED WHILE IT ACCUMULATES. A logical packet is assembled from
            // 16MB continuations and the protocol puts no limit on how many, so a
            // peer can send them without end and grow this until the process dies.
            // The greeting is read through this same path BEFORE TLS is
            // negotiated, so sslmode=require is no protection: whoever answers the
            // connection can do it, authenticated or not.
            long accumulated = all.size();
            long allowed = SqlLimits.maxMessageBytes();
            while(length == MAX_PACKET_BODY) {
                int next = wire.read();
                if(next < 0) {
                    throw new IOException("The MySQL connection closed mid-packet");
                }
                length = next | (wire.read() << 8) | (wire.read() << 16);
                sequence = wire.read() + 1;
                accumulated += length;
                if(accumulated > allowed) {
                    // Closed for the same reason the PostgreSQL bound closes: the
                    // rest of this logical packet is still on the wire and there
                    // is no resync, so a connection left open would hand its tail
                    // to whoever borrows it next.
                    close();
                    throw new IOException("A MySQL packet grew past the " + allowed
                            + " bytes CN1_DB_MAX_MESSAGE_MB allows");
                }
                byte[] more = wire.readFully(length);
                all.write(more, 0, more.length);
            }
            packet.body = all.toByteArray();
        }
        if(packet.body.length == 0) {
            throw new IOException("An empty MySQL packet");
        }
        return packet;
    }

    private static final class Packet {
        byte[] body;
    }

    private static IOException errorFrom(Packet packet, String sql) {
        Reader reader = new Reader(packet.body);
        reader.skip(1);
        int code = reader.u16();
        String state = "";
        if(reader.remaining() > 0 && packet.body[3] == '#') {
            reader.skip(1);
            state = Wire.fromUtf8(reader.bytes(5));
        }
        String message = Wire.fromUtf8(reader.rest());
        return new IOException("MySQL error " + code
                + (state.length() == 0 ? "" : " " + state) + ": " + message
                + (sql == null ? "" : " [" + sql + "]"));
    }

    /** A cursor over one packet body. MySQL is little endian throughout. */
    private static final class Reader {
        private final byte[] data;
        private int at;

        Reader(byte[] data) {
            this.data = data;
        }

        int remaining() {
            return data.length - at;
        }

        void skip(int count) {
            at += count;
        }

        int u8() {
            return data[at++] & 0xff;
        }

        int u16() {
            int value = (data[at] & 0xff) | ((data[at + 1] & 0xff) << 8);
            at += 2;
            return value;
        }

        int i32() {
            int value = (data[at] & 0xff) | ((data[at + 1] & 0xff) << 8)
                    | ((data[at + 2] & 0xff) << 16) | ((data[at + 3] & 0xff) << 24);
            at += 4;
            return value;
        }

        long i64() {
            long value = 0;
            for(int iter = 0 ; iter < 8 ; iter++) {
                value |= ((long)(data[at + iter] & 0xff)) << (iter * 8);
            }
            at += 8;
            return value;
        }

        byte[] bytes(int count) {
            byte[] out = new byte[count];
            System.arraycopy(data, at, out, 0, count);
            at += count;
            return out;
        }

        byte[] rest() {
            return bytes(remaining());
        }

        String cString() {
            int end = at;
            while(end < data.length && data[end] != 0) {
                end++;
            }
            String out = Wire.fromUtf8(data, at, end - at);
            at = end + 1;
            return out;
        }

        /** A length-encoded integer; 0xfb is the NULL marker, returned as -1. */
        long lengthEncoded() {
            int first = u8();
            if(first < 0xfb) {
                return first;
            }
            if(first == 0xfb) {
                return -1;
            }
            if(first == 0xfc) {
                return u16();
            }
            if(first == 0xfd) {
                int value = (data[at] & 0xff) | ((data[at + 1] & 0xff) << 8)
                        | ((data[at + 2] & 0xff) << 16);
                at += 3;
                return value;
            }
            return i64();
        }

        byte[] lengthEncodedBytes() {
            long length = lengthEncoded();
            return length < 0 ? null : bytes((int)length);
        }

        /**
         * DATE / DATETIME / TIMESTAMP, returned as an ISO string. A Java date type
         * would have to be one the translated runtime also has, and every consumer
         * of this data writes it into JSON anyway.
         */
        String temporal() {
            int length = u8();
            if(length == 0) {
                return null;
            }
            int year = u16();
            int month = u8();
            int day = u8();
            StringBuilder out = new StringBuilder();
            pad(out, year, 4).append('-');
            pad(out, month, 2).append('-');
            pad(out, day, 2);
            if(length > 4) {
                int hour = u8();
                int minute = u8();
                int second = u8();
                out.append(' ');
                pad(out, hour, 2).append(':');
                pad(out, minute, 2).append(':');
                pad(out, second, 2);
                if(length > 7) {
                    int micros = i32();
                    out.append('.');
                    pad(out, micros, 6);
                }
            }
            return out.toString();
        }

        String time() {
            int length = u8();
            if(length == 0) {
                return "00:00:00";
            }
            boolean negative = u8() != 0;
            int days = i32();
            int hour = u8();
            int minute = u8();
            int second = u8();
            StringBuilder out = new StringBuilder();
            if(negative) {
                out.append('-');
            }
            pad(out, days * 24 + hour, 2).append(':');
            pad(out, minute, 2).append(':');
            pad(out, second, 2);
            if(length > 8) {
                int micros = i32();
                out.append('.');
                pad(out, micros, 6);
            }
            return out.toString();
        }

        private static StringBuilder pad(StringBuilder out, int value, int width) {
            String text = String.valueOf(value);
            for(int iter = text.length() ; iter < width ; iter++) {
                out.append('0');
            }
            return out.append(text);
        }
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    private static void writeLongLE(ByteArrayOutputStream out, long value) {
        for(int iter = 0 ; iter < 8 ; iter++) {
            out.write((int)((value >> (iter * 8)) & 0xff));
        }
    }

    private static void writeCString(ByteArrayOutputStream out, String value) {
        byte[] data = Wire.utf8(value);
        out.write(data, 0, data.length);
        out.write(0);
    }

    private static void writeLengthEncoded(ByteArrayOutputStream out, byte[] data) {
        int length = data == null ? 0 : data.length;
        if(length < 251) {
            out.write(length);
        } else if(length < 65536) {
            out.write(0xfc);
            out.write(length & 0xff);
            out.write((length >> 8) & 0xff);
        } else if(length <= MAX_PACKET_BODY) {
            out.write(0xfd);
            out.write(length & 0xff);
            out.write((length >> 8) & 0xff);
            out.write((length >> 16) & 0xff);
        } else {
            // 0xfd carries three bytes of length and stops at 0xffffff. A larger
            // value needs 0xfe and eight, and writing the small form for it sends a
            // truncated length: the server then reads the rest of the value as the
            // next thing in the packet. Fragmenting the packet does not help, because
            // this prefix is inside it.
            out.write(0xfe);
            for(int iter = 0 ; iter < 8 ; iter++) {
                out.write((int)((long)length >> (8 * iter)) & 0xff);
            }
        }
        if(length > 0) {
            out.write(data, 0, length);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] out = new byte[a.length];
        for(int iter = 0 ; iter < a.length ; iter++) {
            out[iter] = (byte)(a[iter] ^ b[iter % b.length]);
        }
        return out;
    }

    private static byte[] trimTrailingNul(byte[] data) {
        int length = data.length;
        while(length > 0 && data[length - 1] == 0) {
            length--;
        }
        byte[] out = new byte[length];
        System.arraycopy(data, 0, out, 0, length);
        return out;
    }
}
