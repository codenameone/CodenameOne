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
package com.codename1.backend.kv;

import com.codename1.backend.Tcp;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;

/// One connection to a Redis server speaking RESP2, the protocol every Redis,
/// Valkey and KeyDB release understands.
///
/// [RedisKeyValueStore] keeps a pool of these and lends one per call; ask it for
/// a [RedisKeyValueStore#dedicated()] connection for a command that blocks --
/// `XREADGROUP ... BLOCK`, `BLPOP`, `SUBSCRIBE` -- so it never holds a pooled one.
///
/// Replies come back as Java values: a simple string as `String`, a bulk string
/// as `byte[]`, an integer as `Long`, an array as `Object[]`, a nil as null. An
/// error reply is thrown as a [RedisException], and leaves the connection usable.
/// Any other failure leaves it in an unknown state; the caller closes it.
///
/// Not thread safe.
public final class RedisConnection {
    private final WireBuffer wire;
    private boolean broken;

    private RedisConnection(WireBuffer wire) {
        this.wire = wire;
    }

    /// Connects and authenticates.
    ///
    /// - `user`: the ACL user, or null for the default user
    /// - `password`: or null when the server asks for none
    /// - `database`: the logical database to select, 0 for the default
    /// - `readTimeoutMillis`: how long a reply may take; zero waits forever
    static RedisConnection open(String host, int port, boolean tls, String user,
                                String password, int database, int connectTimeoutMillis,
                                int readTimeoutMillis) throws IOException {
        Tcp tcp = Tcp.connect(host, port, connectTimeoutMillis);
        RedisConnection c = null;
        try {
            if (tls) {
                tcp.startTls(host);
            }
            if (readTimeoutMillis > 0) {
                tcp.setReadTimeout(readTimeoutMillis);
            }
            c = new RedisConnection(new WireBuffer(tcp));
            if (password != null) {
                if (user != null) {
                    c.call("AUTH", user, password);
                } else {
                    c.call("AUTH", password);
                }
            }
            if (database != 0) {
                c.call("SELECT", Integer.toString(database));
            }
            return c;
        } catch (IOException err) {
            tcp.close();
            throw err;
        }
    }

    /// Whether a failure other than an error reply happened on this connection,
    /// after which it must not be reused.
    public boolean isBroken() {
        return broken;
    }

    /// Changes how long a reply may take; zero waits forever.
    public void setReadTimeout(int millis) throws IOException {
        wire.getConnection().setReadTimeout(millis);
    }

    /// Sends one command and answers its reply.
    public Object call(String... args) throws IOException {
        byte[][] raw = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            raw[i] = WireBuffer.utf8(args[i]);
        }
        return call(raw);
    }

    /// Sends one command whose arguments are already bytes and answers its reply.
    public Object call(byte[][] args) throws IOException {
        write(args);
        flush();
        return read();
    }

    /// Stages one command without sending it, for a pipeline: stage several,
    /// [#flush] once, then [#read] each reply in order.
    public void write(byte[][] args) {
        wire.writeByte('*');
        wire.writeDecimal(args.length);
        wire.writeByte('\r');
        wire.writeByte('\n');
        for (byte[] a : args) {
            wire.writeByte('$');
            wire.writeDecimal(a.length);
            wire.writeByte('\r');
            wire.writeByte('\n');
            wire.writeBytes(a);
            wire.writeByte('\r');
            wire.writeByte('\n');
        }
    }

    /// Sends what [#write] staged.
    public void flush() throws IOException {
        try {
            wire.flush();
        } catch (IOException err) {
            broken = true;
            throw err;
        }
    }

    /// Reads the next reply. An error reply is thrown after it has been read
    /// whole, so the connection stays in step.
    public Object read() throws IOException {
        Object reply;
        try {
            reply = readValue();
        } catch (IOException err) {
            broken = true;
            throw err;
        } catch (RuntimeException err) {
            broken = true;
            throw new IOException("Malformed reply from Redis: " + err, err);
        }
        if (reply instanceof RedisException) {
            throw (RedisException) reply;
        }
        return reply;
    }

    private Object readValue() throws IOException {
        int type = wire.read();
        String line = wire.readLine();
        switch (type) {
            case '+':
                return line;
            case '-':
                return new RedisException(line);
            case ':':
                return Long.valueOf(Long.parseLong(line));
            case '$': {
                int length = Integer.parseInt(line);
                if (length < 0) {
                    return null;
                }
                byte[] data = wire.readFully(length);
                wire.skip(2);
                return data;
            }
            case '*': {
                int count = Integer.parseInt(line);
                if (count < 0) {
                    return null;
                }
                Object[] items = new Object[count];
                // An error nested in an array (EXEC) stays in it as an element:
                // the caller reads the rest of the array either way.
                for (int i = 0; i < count; i++) {
                    items[i] = readValue();
                }
                return items;
            }
            default:
                throw new IOException("Unknown RESP reply type " + type);
        }
    }

    /// Closes the socket. Never throws.
    public void close() {
        broken = true;
        wire.getConnection().close();
    }
}
