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

import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A [KeyValueStore] on a Redis server -- or Valkey, KeyDB, Dragonfly, any
/// server that speaks RESP2. No client library: the protocol is spoken
/// directly over the backend's own sockets, so a binary that never names this
/// class carries none of it.
///
/// ```
///   redis://[[user]:password@]host[:port][/database]
///   rediss://...        # TLS, verifying the server's certificate
/// ```
///
/// ## Connections
///
/// Calls borrow a connection from a pool, use it and give it back; a pool that
/// has none idle opens another rather than making the caller wait, and keeps
/// at most `cn1.kv.pool.max` (16) idle. A connection that failed with anything
/// but an error reply is closed, never returned, so one dropped socket costs one
/// call. Commands that block go on a [#dedicated()] connection.
///
/// Subscriptions share ONE further connection and one receiving thread for the
/// whole store. When it drops, it reconnects with a back-off and subscribes
/// again; messages published meanwhile are lost, as pub/sub always loses them.
///
/// ## Atomic operations
///
/// [#compareAndSet] and [#increment] with an expiry are Lua scripts, loaded once
/// per server with `SCRIPT LOAD` and called by digest, so a call sends the
/// digest rather than the script. A server that forgot them (a restart, a
/// `SCRIPT FLUSH`) is answered `NOSCRIPT`, and they are loaded again.
public final class RedisKeyValueStore implements KeyValueStore {
    private static final String CAS_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) "
            + "if (ARGV[1] == '0' and v == false) or (ARGV[1] == '1' and v == ARGV[2]) then "
            + "if ARGV[3] == '1' then redis.call('DEL', KEYS[1]) "
            + "elseif tonumber(ARGV[5]) > 0 then redis.call('SET', KEYS[1], ARGV[4], 'PX', ARGV[5]) "
            + "else redis.call('SET', KEYS[1], ARGV[4]) end "
            + "return 1 end return 0";
    private static final String INCR_SCRIPT =
            "if tonumber(ARGV[2]) > 0 then redis.call('SET', KEYS[1], '0', 'NX', 'PX', ARGV[2]) end "
            + "return redis.call('INCRBY', KEYS[1], ARGV[1])";
    private static final byte[] EMPTY = new byte[0];

    private final String host;
    private final int port;
    private final boolean tls;
    private final String user;
    private final String password;
    private final int database;
    private final int connectTimeout;
    private final int readTimeout;
    private final int maxIdle;
    private final List<RedisConnection> idle = new ArrayList<RedisConnection>();
    private final Map<String, String> scripts = new HashMap<String, String>();
    private final java.util.concurrent.atomic.AtomicBoolean closed =
            new java.util.concurrent.atomic.AtomicBoolean();

    private final Object subscriberLock = new Object();
    private final Map<String, List<MessageListener>> listeners =
            new HashMap<String, List<MessageListener>>();
    private Subscriber subscriber;

    /// A store for `url` (see the class description).
    ///
    /// - `connectTimeoutMillis`: how long a connection may take to open
    /// - `readTimeoutMillis`: how long a reply may take; zero waits forever
    /// - `maxIdle`: how many idle connections the pool keeps
    public RedisKeyValueStore(String url, int connectTimeoutMillis, int readTimeoutMillis,
                              int maxIdle) throws IOException {
        Endpoint e = Endpoint.parse(url);
        this.host = e.host;
        this.port = e.port;
        this.tls = e.tls;
        this.user = e.user;
        this.password = e.password;
        this.database = e.database;
        this.connectTimeout = connectTimeoutMillis;
        this.readTimeout = readTimeoutMillis;
        this.maxIdle = Math.max(0, maxIdle);
    }

    /// A parsed `redis://` URL.
    static final class Endpoint {
        String host;
        int port = 6379;
        boolean tls;
        String user;
        String password;
        int database;

        static Endpoint parse(String url) throws IOException {
            Endpoint e = new Endpoint();
            String rest;
            if (url.regionMatches(true, 0, "rediss://", 0, 9)) {
                e.tls = true;
                rest = url.substring(9);
            } else if (url.regionMatches(true, 0, "redis://", 0, 8)) {
                rest = url.substring(8);
            } else {
                throw new IOException("Not a redis:// or rediss:// URL: " + url);
            }
            int query = rest.indexOf('?');
            if (query >= 0) {
                rest = rest.substring(0, query);
            }
            int at = rest.lastIndexOf('@');
            if (at >= 0) {
                String credentials = rest.substring(0, at);
                rest = rest.substring(at + 1);
                int colon = credentials.indexOf(':');
                if (colon < 0) {
                    e.password = decode(credentials);
                } else {
                    String u = credentials.substring(0, colon);
                    e.user = u.length() == 0 ? null : decode(u);
                    e.password = decode(credentials.substring(colon + 1));
                }
                if (e.password != null && e.password.length() == 0) {
                    e.password = null;
                }
            }
            int slash = rest.indexOf('/');
            if (slash >= 0) {
                String db = rest.substring(slash + 1);
                rest = rest.substring(0, slash);
                if (db.length() > 0) {
                    try {
                        e.database = Integer.parseInt(db);
                    } catch (NumberFormatException err) {
                        throw new IOException("Redis database must be a number: " + db); // NOPMD PreserveStackTrace - no cause constructor in JavaAPI
                    }
                }
            }
            int colon = rest.lastIndexOf(':');
            if (colon > 0 && rest.indexOf(']') < colon) {
                try {
                    e.port = Integer.parseInt(rest.substring(colon + 1));
                } catch (NumberFormatException err) {
                    throw new IOException("Redis port must be a number: " + rest); // NOPMD PreserveStackTrace - no cause constructor in JavaAPI
                }
                rest = rest.substring(0, colon);
            }
            if (rest.startsWith("[") && rest.endsWith("]")) {
                rest = rest.substring(1, rest.length() - 1);
            }
            e.host = rest.length() == 0 ? "localhost" : rest;
            return e;
        }

        /// Percent-decodes a URL user or password, ASCII escapes only.
        private static String decode(String s) {
            if (s.indexOf('%') < 0) {
                return s;
            }
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '%' && i + 2 < s.length()) {
                    int v = Character.digit(s.charAt(i + 1), 16) * 16
                            + Character.digit(s.charAt(i + 2), 16);
                    if (v >= 0) {
                        sb.append((char) v);
                        i += 2;
                        continue;
                    }
                }
                sb.append(c);
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------ the pool

    private RedisConnection borrow() throws IOException {
        synchronized (idle) {
            if (closed.get()) {
                throw new IOException("The Redis store is closed");
            }
            int n = idle.size();
            if (n > 0) {
                return idle.remove(n - 1);
            }
        }
        return RedisConnection.open(host, port, tls, user, password, database, connectTimeout,
                readTimeout);
    }

    private void release(RedisConnection c) {
        if (c.isBroken()) {
            c.close();
            return;
        }
        synchronized (idle) {
            if (!closed.get() && idle.size() < maxIdle) {
                idle.add(c);
                return;
            }
        }
        c.close();
    }

    /// A connection of its own, outside the pool, for a command that blocks.
    /// Its reply timeout is zero -- it waits as long as the command does. The
    /// caller closes it.
    public RedisConnection dedicated() throws IOException {
        return RedisConnection.open(host, port, tls, user, password, database, connectTimeout, 0);
    }

    /// Runs one command on a pooled connection and answers its reply, in the
    /// form [RedisConnection] describes -- for the commands this interface does
    /// not wrap (`XADD`, `HSET`, ...).
    public Object call(String... args) throws IOException {
        byte[][] raw = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            raw[i] = WireBuffer.utf8(args[i]);
        }
        return call(raw);
    }

    /// [#call(String...)] with arguments that are already bytes.
    public Object call(byte[][] args) throws IOException {
        RedisConnection c = borrow();
        try {
            return c.call(args);
        } finally {
            release(c);
        }
    }

    /// Sends several commands in one round trip and answers their replies in
    /// order; an error reply is in the array as a [RedisException], not thrown.
    public Object[] pipeline(byte[][][] commands) throws IOException {
        RedisConnection c = borrow();
        try {
            for (byte[][] command : commands) {
                c.write(command);
            }
            c.flush();
            Object[] replies = new Object[commands.length];
            for (int i = 0; i < commands.length; i++) {
                try {
                    replies[i] = c.read();
                } catch (RedisException err) {
                    replies[i] = err;
                }
            }
            return replies;
        } finally {
            release(c);
        }
    }

    private static byte[] b(String s) {
        return WireBuffer.utf8(s);
    }

    private static byte[] n(long v) {
        return WireBuffer.ascii(Long.toString(v));
    }

    private Object script(String body, byte[][] keysAndArgs) throws IOException {
        String digest;
        synchronized (scripts) {
            digest = scripts.get(body);
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            if (digest == null) {
                Object loaded = call("SCRIPT", "LOAD", body);
                digest = loaded instanceof byte[] ? WireBuffer.fromUtf8((byte[]) loaded)
                        : String.valueOf(loaded);
                synchronized (scripts) {
                    scripts.put(body, digest);
                }
            }
            byte[][] command = new byte[keysAndArgs.length + 2][];
            command[0] = b("EVALSHA");
            command[1] = b(digest);
            System.arraycopy(keysAndArgs, 0, command, 2, keysAndArgs.length);
            try {
                return call(command);
            } catch (RedisException err) {
                if (!"NOSCRIPT".equals(err.getCode())) {
                    throw err;
                }
                digest = null;
            }
        }
        throw new IOException("Redis would not keep a script loaded");
    }

    // ---------------------------------------------------------- the store

    private static long number(Object reply) throws IOException {
        if (reply instanceof Long) {
            return ((Long) reply).longValue();
        }
        throw new IOException("Redis answered " + reply + " where a number was expected");
    }

    @Override
    public byte[] get(String key) throws IOException {
        Object r = call(new byte[][] {b("GET"), b(key)});
        return r instanceof byte[] ? (byte[]) r : null;
    }

    @Override
    public void set(String key, byte[] value, long ttlMillis) throws IOException {
        if (ttlMillis > 0) {
            call(new byte[][] {b("SET"), b(key), value, b("PX"), n(ttlMillis)});
        } else {
            call(new byte[][] {b("SET"), b(key), value});
        }
    }

    @Override
    public boolean setIfAbsent(String key, byte[] value, long ttlMillis) throws IOException {
        Object r = ttlMillis > 0
                ? call(new byte[][] {b("SET"), b(key), value, b("NX"), b("PX"), n(ttlMillis)})
                : call(new byte[][] {b("SET"), b(key), value, b("NX")});
        return r != null;
    }

    @Override
    public boolean compareAndSet(String key, byte[] expected, byte[] value, long ttlMillis)
            throws IOException {
        byte[][] args = {b("1"), b(key), b(expected == null ? "0" : "1"),
                expected == null ? EMPTY : expected, b(value == null ? "1" : "0"),
                value == null ? EMPTY : value, n(ttlMillis)};
        Object r = script(CAS_SCRIPT, args);
        return number(r) == 1;
    }

    @Override
    public boolean delete(String key) throws IOException {
        return number(call(new byte[][] {b("DEL"), b(key)})) > 0;
    }

    @Override
    public int deleteByPrefix(String prefix) throws IOException {
        String pattern = globEscape(prefix) + "*";
        String cursor = "0";
        int deleted = 0;
        do {
            Object r = call("SCAN", cursor, "MATCH", pattern, "COUNT", "500");
            if (!(r instanceof Object[]) || ((Object[]) r).length != 2) {
                throw new IOException("Unexpected SCAN reply");
            }
            Object[] page = (Object[]) r;
            cursor = WireBuffer.fromUtf8((byte[]) page[0]);
            Object[] keys = (Object[]) page[1];
            if (keys.length > 0) {
                byte[][] command = new byte[keys.length + 1][];
                command[0] = b("UNLINK");
                for (int i = 0; i < keys.length; i++) {
                    command[i + 1] = (byte[]) keys[i];
                }
                deleted += (int) number(call(command));
            }
        } while (!"0".equals(cursor));
        return deleted;
    }

    /// `s` with Redis's glob characters escaped, so a prefix matches only itself.
    static String globEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '*' || c == '?' || c == '[' || c == ']' || c == '\\' || c == '^') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public long increment(String key, long delta, long ttlMillis) throws IOException {
        if (ttlMillis <= 0) {
            return number(call(new byte[][] {b("INCRBY"), b(key), n(delta)}));
        }
        return number(script(INCR_SCRIPT, new byte[][] {b("1"), b(key), n(delta),
            n(ttlMillis)}));
    }

    @Override
    public long ttl(String key) throws IOException {
        return number(call(new byte[][] {b("PTTL"), b(key)}));
    }

    @Override
    public long publish(String channel, byte[] message) throws IOException {
        return number(call(new byte[][] {b("PUBLISH"), b(channel), message}));
    }

    @Override
    public boolean ping() {
        try {
            return "PONG".equals(call("PING"));
        } catch (IOException err) {
            return false;
        }
    }

    @Override
    public boolean isShared() {
        return true;
    }

    // ------------------------------------------------------ subscriptions

    @Override
    public Subscription subscribe(final String channel, final MessageListener listener)
            throws IOException {
        Subscriber s;
        boolean first;
        synchronized (subscriberLock) {
            if (closed.get()) {
                throw new IOException("The Redis store is closed");
            }
            List<MessageListener> list = listeners.get(channel);
            first = list == null;
            if (first) {
                list = new ArrayList<MessageListener>();
                listeners.put(channel, list);
            }
            list.add(listener);
            if (subscriber == null) {
                subscriber = new Subscriber();
                subscriber.start();
            }
            s = subscriber;
        }
        if (first) {
            s.command("SUBSCRIBE", channel);
        }
        return new Subscription() {
            @Override
            public void close() {
                Subscriber current;
                boolean last = false;
                synchronized (subscriberLock) {
                    List<MessageListener> list = listeners.get(channel);
                    if (list != null && list.remove(listener) && list.isEmpty()) {
                        listeners.remove(channel);
                        last = true;
                    }
                    current = subscriber;
                }
                if (last && current != null) {
                    current.command("UNSUBSCRIBE", channel);
                }
            }
        };
    }

    private void deliver(String channel, byte[] message) {
        List<MessageListener> targets;
        synchronized (subscriberLock) {
            List<MessageListener> list = listeners.get(channel);
            if (list == null) {
                return;
            }
            targets = new ArrayList<MessageListener>(list);
        }
        for (MessageListener l : targets) {
            try {
                l.onMessage(channel, message);
            } catch (RuntimeException err) {
                System.err.println("Subscriber of " + channel + " failed: " + err);
            }
        }
    }

    /// The one connection and thread that receive every subscription's messages.
    private final class Subscriber implements Runnable {
        private RedisConnection connection;
        private boolean stopped;
        private Thread thread;

        void start() {
            thread = new Thread(this, "cn1-redis-subscriber");
            thread.setDaemon(true);
            thread.start();
        }

        /// Sends SUBSCRIBE or UNSUBSCRIBE on the live connection; a dropped one
        /// re-subscribes everything when it comes back, so a failure here is
        /// recovered there.
        void command(String verb, String channel) {
            synchronized (this) {
                if (connection == null) {
                    return;
                }
                try {
                    connection.write(new byte[][] {b(verb), b(channel)});
                    connection.flush();
                } catch (IOException err) {
                    connection.close();
                }
            }
        }

        void stop() {
            synchronized (this) {
                stopped = true;
                if (connection != null) {
                    connection.close();
                }
            }
        }

        @Override
        public void run() {
            long backoff = 100;
            while (true) {
                RedisConnection c;
                synchronized (this) {
                    if (stopped) {
                        return;
                    }
                }
                try {
                    c = dedicated();
                } catch (IOException err) {
                    if (!sleep(backoff)) {
                        return;
                    }
                    backoff = Math.min(backoff * 2, 5000);
                    continue;
                }
                List<String> channels;
                synchronized (subscriberLock) {
                    channels = new ArrayList<String>(listeners.keySet());
                }
                synchronized (this) {
                    if (stopped) {
                        c.close();
                        return;
                    }
                    connection = c;
                    if (!channels.isEmpty()) {
                        byte[][] command = new byte[channels.size() + 1][];
                        command[0] = b("SUBSCRIBE");
                        for (int i = 0; i < channels.size(); i++) {
                            command[i + 1] = b(channels.get(i));
                        }
                        c.write(command);
                        try {
                            c.flush();
                        } catch (IOException err) {
                            c.close();
                        }
                    }
                }
                backoff = 100;
                receive(c);
                synchronized (this) {
                    connection = null;
                }
            }
        }

        private void receive(RedisConnection c) {
            try {
                while (true) {
                    Object reply = c.read();
                    if (!(reply instanceof Object[])) {
                        continue;
                    }
                    Object[] parts = (Object[]) reply;
                    if (parts.length == 3 && parts[0] instanceof byte[]
                            && "message".equals(WireBuffer.fromUtf8((byte[]) parts[0]))
                            && parts[1] instanceof byte[] && parts[2] instanceof byte[]) {
                        deliver(WireBuffer.fromUtf8((byte[]) parts[1]), (byte[]) parts[2]);
                    }
                }
            } catch (IOException err) {
                c.close();
            }
        }

        private boolean sleep(long millis) {
            try {
                Thread.sleep(millis);
                return true;
            } catch (InterruptedException err) {
                return false;
            }
        }
    }

    @Override
    public void close() {
        List<RedisConnection> drop;
        Subscriber s;
        synchronized (idle) {
            closed.set(true);
            drop = new ArrayList<RedisConnection>(idle);
            idle.clear();
        }
        synchronized (subscriberLock) {
            s = subscriber;
            subscriber = null;
            listeners.clear();
        }
        if (s != null) {
            s.stop();
        }
        for (RedisConnection c : drop) {
            c.close();
        }
    }
}
