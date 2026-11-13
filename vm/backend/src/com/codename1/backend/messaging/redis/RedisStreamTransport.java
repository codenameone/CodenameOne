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
package com.codename1.backend.messaging.redis;

import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.kv.RedisConnection;
import com.codename1.backend.kv.RedisException;
import com.codename1.backend.kv.RedisKeyValueStore;
import com.codename1.backend.messaging.Message;
import com.codename1.backend.messaging.MessageTransport;
import com.codename1.backend.messaging.Messaging;
import com.codename1.backend.messaging.PollingTransport;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Redis Streams: `cn1.messaging.url=redis://...` (or `rediss://`). Works with
/// Redis 6.2 or newer, Valkey and KeyDB.
///
/// A destination is a stream and a group a consumer group, created on first
/// use: each group gets every message, and the consumers of one group share
/// them. A message is an entry with its payload, key and headers as fields;
/// acknowledging it is `XACK`. A consumer that dies leaves its messages
/// pending, and another consumer of the group claims them once they have been
/// idle for `cn1.messaging.lease` milliseconds (60000) -- counting that as
/// another delivery, so a message that crashes its consumer is dead-lettered
/// like one that throws.
///
/// A failed message stays pending, and its next delivery waits in a sorted
/// set per destination and group until it is due; only the group whose
/// listener failed sees it again.
///
/// Streams are capped at about `cn1.messaging.redis.max-length` entries
/// (100000, trimmed with `MAXLEN ~`), so a destination nobody reads cannot
/// grow without bound.
public final class RedisStreamTransport extends PollingTransport {
    private static final byte[] P = WireBuffer.utf8("p");
    private static final byte[] H = WireBuffer.utf8("h");
    private static final byte[] K = WireBuffer.utf8("k");

    private final RedisKeyValueStore redis;
    private final long lease;
    private final long maxLength;
    private final String node;
    private final Set<String> groups = new HashSet<String>();
    private final List<RedisConnection> blocking = new ArrayList<RedisConnection>();
    private final ThreadLocal<RedisConnection> mine = new ThreadLocal<RedisConnection>();

    /// Opens the transport for `redis://` and `rediss://` URLs; the generated
    /// wiring registers it when the configuration names one.
    public static final class Factory implements Messaging.TransportFactory {
        @Override
        public MessageTransport open(String url, Config config, RetryPolicy retry)
                throws IOException {
            int timeout = config == null ? 5000 : config.getInt("cn1.messaging.timeout", 5000);
            return new RedisStreamTransport(new RedisKeyValueStore(url, timeout, timeout, 16),
                    retry,
                    config == null ? 60000 : config.getInt("cn1.messaging.lease", 60000),
                    config == null ? 100000
                            : config.getInt("cn1.messaging.redis.max-length", 100000));
        }
    }

    /// A transport over `redis`, which it closes.
    public RedisStreamTransport(RedisKeyValueStore redis, RetryPolicy retry, long leaseMillis,
                                long maxLength) {
        super(retry);
        this.redis = redis;
        // Longer than the longest retry wait, so a message waiting for its
        // retry is never taken for an abandoned one.
        this.lease = Math.max(Math.max(1000, leaseMillis), retry.delayAfter(64) + 5000);
        this.maxLength = maxLength;
        this.node = Long.toString(System.currentTimeMillis(), 36) + "-"
                + Integer.toString(System.identityHashCode(this), 36);
    }

    @Override
    public String getName() {
        return "redis";
    }

    @Override
    public void send(Message message) throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>(message.getHeaders());
        headers.remove(Message.ATTEMPT);
        headers.remove(Message.NOT_BEFORE);
        List<byte[]> args = new ArrayList<byte[]>();
        args.add(WireBuffer.utf8("XADD"));
        args.add(WireBuffer.utf8(message.getDestination()));
        if (maxLength > 0) {
            args.add(WireBuffer.utf8("MAXLEN"));
            args.add(WireBuffer.utf8("~"));
            args.add(WireBuffer.utf8(Long.toString(maxLength)));
        }
        args.add(WireBuffer.utf8("*"));
        args.add(P);
        args.add(message.getPayload());
        if (!headers.isEmpty()) {
            args.add(H);
            args.add(WireBuffer.utf8(Json.write(headers)));
        }
        if (message.getKey() != null) {
            args.add(K);
            args.add(WireBuffer.utf8(message.getKey()));
        }
        Object id = redis.call(args.toArray(new byte[args.size()][]));
        identified(message, text(id));
    }

    private void ensureGroup(String destination, String group) throws IOException {
        String key = destination + "\n" + group;
        synchronized (groups) {
            if (groups.contains(key)) {
                return;
            }
        }
        try {
            // From the start of the stream: a group created after messages
            // were sent still receives them, as a queue would.
            redis.call("XGROUP", "CREATE", destination, group, "0", "MKSTREAM");
        } catch (RedisException err) {
            if (err.getMessage() == null || !err.getMessage().startsWith("BUSYGROUP")) {
                throw err;
            }
        }
        synchronized (groups) {
            groups.add(key);
        }
    }

    private String consumer() {
        return node + "-" + Thread.currentThread().getName();
    }

    private static String retryKey(String destination, String group) {
        return "cn1:msg:retry:" + destination + ":" + group;
    }

    @Override
    protected List<Delivery> poll(String destination, String group, int max, long waitMillis)
            throws IOException {
        ensureGroup(destination, group);
        List<Delivery> out = new ArrayList<Delivery>();
        try {
            due(destination, group, max, out);
            if (out.size() < max) {
                abandoned(destination, group, max - out.size(), out);
            }
            if (out.isEmpty()) {
                read(destination, group, max, waitMillis, out);
            }
        } catch (RedisException err) {
            if (err.getMessage() != null && err.getMessage().startsWith("NOGROUP")) {
                // The stream was deleted under us: create it again next time.
                synchronized (groups) {
                    groups.remove(destination + "\n" + group);
                }
                return out;
            }
            throw err;
        }
        return out;
    }

    /// Retries whose time has come.
    private void due(String destination, String group, int max, List<Delivery> out)
            throws IOException {
        String key = retryKey(destination, group);
        Object r = redis.call("ZRANGEBYSCORE", key, "-inf",
                Long.toString(System.currentTimeMillis()), "LIMIT", "0", Integer.toString(max));
        if (!(r instanceof Object[])) {
            return;
        }
        for (Object member : (Object[]) r) {
            String m = text(member);
            Object removed = redis.call("ZREM", key, m);
            if (!(removed instanceof Long) || ((Long) removed).longValue() != 1) {
                continue;                       // another consumer took it
            }
            int bar = m.lastIndexOf('|');
            String id = m.substring(0, bar);
            int attempt = Integer.parseInt(m.substring(bar + 1));
            Object claimed = redis.call("XCLAIM", destination, group, consumer(), "0", id);
            entries(destination, claimed, attempt, out);
        }
    }

    /// Messages a dead consumer left pending.
    private void abandoned(String destination, String group, int max, List<Delivery> out)
            throws IOException {
        Object r = redis.call("XPENDING", destination, group, "IDLE", Long.toString(lease), "-",
                "+", Integer.toString(max));
        if (!(r instanceof Object[])) {
            return;
        }
        for (Object o : (Object[]) r) {
            if (!(o instanceof Object[]) || ((Object[]) o).length < 4) {
                continue;
            }
            Object[] p = (Object[]) o;
            String id = text(p[0]);
            long deliveries = p[3] instanceof Long ? ((Long) p[3]).longValue() : 1;
            // Claimed only if still idle that long, so two consumers cannot
            // both take it.
            Object claimed = redis.call("XCLAIM", destination, group, consumer(),
                    Long.toString(lease), id);
            entries(destination, claimed, (int) Math.min(Integer.MAX_VALUE, deliveries + 1), out);
        }
    }

    /// New messages, waiting up to `waitMillis` for one.
    private void read(String destination, String group, int max, long waitMillis,
                      List<Delivery> out) throws IOException {
        RedisConnection c = mine.get(); // NOPMD CloseResource - this thread's, closed with the transport
        if (c == null || c.isBroken()) {
            if (c != null) {
                forget(c);
            }
            c = redis.dedicated();
            synchronized (blocking) {
                blocking.add(c);
            }
            mine.set(c);
        }
        Object r;
        try {
            r = c.call("XREADGROUP", "GROUP", group, consumer(), "COUNT", Integer.toString(max),
                    "BLOCK", Long.toString(Math.max(1, waitMillis)), "STREAMS", destination, ">");
        } catch (RedisException err) {
            throw err;
        } catch (IOException err) {
            forget(c);
            mine.remove();
            throw err;
        }
        if (!(r instanceof Object[])) {
            return;
        }
        for (Object stream : (Object[]) r) {
            if (stream instanceof Object[] && ((Object[]) stream).length == 2) {
                entries(destination, ((Object[]) stream)[1], 1, out);
            }
        }
    }

    private void forget(RedisConnection c) {
        synchronized (blocking) {
            blocking.remove(c);
        }
        c.close();
    }

    /// Adds the entries of an `XREADGROUP` or `XCLAIM` reply to `out`.
    private static void entries(String destination, Object reply, int attempt,
                                List<Delivery> out) throws IOException {
        if (!(reply instanceof Object[])) {
            return;
        }
        for (Object e : (Object[]) reply) {
            if (!(e instanceof Object[]) || ((Object[]) e).length != 2
                    || !(((Object[]) e)[1] instanceof Object[])) {
                continue;                       // deleted, or trimmed away
            }
            String id = text(((Object[]) e)[0]);
            Object[] fields = (Object[]) ((Object[]) e)[1];
            byte[] payload = new byte[0];
            String key = null;
            Map<String, String> headers = new LinkedHashMap<String, String>();
            for (int i = 0; i + 1 < fields.length; i += 2) {
                String f = text(fields[i]);
                if ("p".equals(f)) {
                    payload = fields[i + 1] instanceof byte[] ? (byte[]) fields[i + 1] : payload;
                } else if ("k".equals(f)) {
                    key = text(fields[i + 1]);
                } else if ("h".equals(f)) {
                    for (Object o : Json.parseObject(text(fields[i + 1])).entrySet()) {
                        Map.Entry entry = (Map.Entry) o;
                        if (entry.getValue() != null) {
                            headers.put(String.valueOf(entry.getKey()),
                                    String.valueOf(entry.getValue()));
                        }
                    }
                }
            }
            headers.put(Message.ATTEMPT, Integer.toString(Math.max(1, attempt)));
            out.add(new Delivery(identified(new Message(destination, key, payload, headers), id),
                    id));
        }
    }

    @Override
    protected void ack(String destination, String group, Delivery delivery) throws IOException {
        redis.call("XACK", destination, group, (String) delivery.handle);
    }

    @Override
    protected void redeliver(String destination, String group, Delivery delivery, int attempt,
                             long notBefore) throws IOException {
        // Left pending; the sorted set says when it is due, and for which group.
        redis.call("ZADD", retryKey(destination, group), Long.toString(notBefore),
                delivery.handle + "|" + attempt);
    }

    private static String text(Object reply) {
        if (reply instanceof byte[]) {
            return WireBuffer.fromUtf8((byte[]) reply);
        }
        return reply == null ? null : reply.toString();
    }

    @Override
    public boolean isHealthy() {
        return redis.ping();
    }

    @Override
    public void close() {
        // Consumers end within one blocking read of being told to.
        stopSubscriptions();
        List<RedisConnection> all;
        synchronized (blocking) {
            all = new ArrayList<RedisConnection>(blocking);
            blocking.clear();
        }
        for (RedisConnection c : all) {
            c.close();
        }
        redis.close();
    }
}
