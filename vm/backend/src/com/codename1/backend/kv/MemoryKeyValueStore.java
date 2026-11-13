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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// The process-local [KeyValueStore]: what `cn1.kv.url=memory:` and an
/// unconfigured server get.
///
/// Everything a shared store does works here too -- expiry, compare-and-set,
/// counters and pub/sub -- within this process, so an application developed
/// against it behaves the same when Redis is configured, minus the sharing.
/// Expired entries are dropped when touched and by a sweep every 1024 writes.
public final class MemoryKeyValueStore implements KeyValueStore {
    private static final class Entry {
        final byte[] value;
        final long expires;

        Entry(byte[] value, long expires) {
            this.value = value;
            this.expires = expires;
        }
    }

    private final Map<String, Entry> entries = new HashMap<String, Entry>();
    private final Map<String, List<MessageListener>> channels =
            new HashMap<String, List<MessageListener>>();
    private int writes;
    private boolean closed;

    private static long expiry(long ttlMillis) {
        return ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : Long.MAX_VALUE;
    }

    private Entry live(String key, long now) {
        Entry e = entries.get(key);
        if (e != null && e.expires <= now) {
            entries.remove(key);
            return null;
        }
        return e;
    }

    private void wrote() {
        writes++;
        if (writes % 1024 == 0) {
            long now = System.currentTimeMillis();
            Iterator<Entry> it = entries.values().iterator();
            while (it.hasNext()) {
                if (it.next().expires <= now) {
                    it.remove();
                }
            }
        }
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("The key/value store is closed");
        }
    }

    @Override
    public synchronized byte[] get(String key) throws IOException {
        checkOpen();
        Entry e = live(key, System.currentTimeMillis());
        return e == null ? null : e.value;
    }

    @Override
    public synchronized void set(String key, byte[] value, long ttlMillis) throws IOException {
        checkOpen();
        entries.put(key, new Entry(value, expiry(ttlMillis)));
        wrote();
    }

    @Override
    public synchronized boolean setIfAbsent(String key, byte[] value, long ttlMillis)
            throws IOException {
        checkOpen();
        if (live(key, System.currentTimeMillis()) != null) {
            return false;
        }
        entries.put(key, new Entry(value, expiry(ttlMillis)));
        wrote();
        return true;
    }

    @Override
    public synchronized boolean compareAndSet(String key, byte[] expected, byte[] value,
                                              long ttlMillis) throws IOException {
        checkOpen();
        Entry e = live(key, System.currentTimeMillis());
        byte[] current = e == null ? null : e.value;
        if (expected == null ? current != null : !Arrays.equals(expected, current)) {
            return false;
        }
        if (value == null) {
            entries.remove(key);
        } else {
            entries.put(key, new Entry(value, expiry(ttlMillis)));
            wrote();
        }
        return true;
    }

    @Override
    public synchronized boolean delete(String key) throws IOException {
        checkOpen();
        return live(key, System.currentTimeMillis()) != null && entries.remove(key) != null;
    }

    @Override
    public synchronized int deleteByPrefix(String prefix) throws IOException {
        checkOpen();
        int n = 0;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            if (e.getKey().startsWith(prefix)) {
                if (e.getValue().expires > now) {
                    n++;
                }
                it.remove();
            }
        }
        return n;
    }

    @Override
    public synchronized long increment(String key, long delta, long ttlMillis)
            throws IOException {
        checkOpen();
        Entry e = live(key, System.currentTimeMillis());
        long current = 0;
        long expires = expiry(ttlMillis);
        if (e != null) {
            current = parseLong(e.value);
            expires = e.expires;
        }
        long next = current + delta;
        entries.put(key, new Entry(com.codename1.impl.backend.WireBuffer.ascii(
                Long.toString(next)), expires));
        wrote();
        return next;
    }

    static long parseLong(byte[] value) throws IOException {
        try {
            return Long.parseLong(com.codename1.impl.backend.WireBuffer.fromUtf8(value));
        } catch (NumberFormatException err) {
            throw new IOException("The value is not an integer"); // NOPMD PreserveStackTrace - JavaAPI's IOException takes no cause, and the message says it all
        }
    }

    @Override
    public synchronized long ttl(String key) throws IOException {
        checkOpen();
        long now = System.currentTimeMillis();
        Entry e = live(key, now);
        if (e == null) {
            return -2;
        }
        return e.expires == Long.MAX_VALUE ? -1 : e.expires - now;
    }

    @Override
    public long publish(String channel, byte[] message) throws IOException {
        List<MessageListener> targets;
        synchronized (this) {
            checkOpen();
            List<MessageListener> list = channels.get(channel);
            if (list == null) {
                return 0;
            }
            targets = new ArrayList<MessageListener>(list);
        }
        // Outside the lock, so a listener may use the store.
        for (MessageListener l : targets) {
            try {
                l.onMessage(channel, message);
            } catch (RuntimeException err) {
                System.err.println("Subscriber of " + channel + " failed: " + err);
            }
        }
        return targets.size();
    }

    @Override
    public synchronized Subscription subscribe(final String channel,
                                               final MessageListener listener)
            throws IOException {
        checkOpen();
        List<MessageListener> list = channels.get(channel);
        if (list == null) {
            list = new ArrayList<MessageListener>();
            channels.put(channel, list);
        }
        list.add(listener);
        return new Subscription() {
            @Override
            public void close() {
                synchronized (MemoryKeyValueStore.this) {
                    List<MessageListener> l = channels.get(channel);
                    if (l != null) {
                        l.remove(listener);
                        if (l.isEmpty()) {
                            channels.remove(channel);
                        }
                    }
                }
            }
        };
    }

    @Override
    public synchronized boolean ping() {
        return !closed;
    }

    @Override
    public boolean isShared() {
        return false;
    }

    @Override
    public synchronized void close() {
        closed = true;
        entries.clear();
        channels.clear();
    }
}
