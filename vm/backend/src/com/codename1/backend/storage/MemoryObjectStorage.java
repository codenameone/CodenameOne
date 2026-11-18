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
package com.codename1.backend.storage;

import com.codename1.backend.Crypto;
import com.codename1.backend.Http1Date;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// An [ObjectStorage] held in this process, for tests and local development.
/// Nothing survives a restart.
///
/// It cannot presign: a URL has to name a server that answers it, and this store
/// has none. Both presign methods throw, so code that hands one to a client fails
/// in development rather than shipping a URL that leads nowhere.
public final class MemoryObjectStorage implements ObjectStorage {
    private final Map<String, Map<String, Entry>> buckets =
            new HashMap<String, Map<String, Entry>>();

    /// The order every provider lists in: UTF-8 bytes, which is code point
    /// order. String's own compareTo is UTF-16 order, and the two disagree for
    /// a character above U+FFFF against one in U+E000-U+FFFF -- so `max` would
    /// truncate a different set here than in production.
    private static final Comparator<String> UTF8_ORDER = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            int i = 0;
            int j = 0;
            while (i < a.length() && j < b.length()) {
                int ca = a.codePointAt(i);
                int cb = b.codePointAt(j);
                if (ca != cb) {
                    return ca < cb ? -1 : 1;
                }
                i += Character.charCount(ca);
                j += Character.charCount(cb);
            }
            return (a.length() - i) - (b.length() - j);
        }
    };

    private static final class Entry {
        final byte[] content;
        final ObjectInfo info;

        Entry(byte[] content, ObjectInfo info) {
            this.content = content;
            this.info = info;
        }
    }

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public synchronized void createBucket(String bucket) {
        if (!buckets.containsKey(bucket)) {
            buckets.put(bucket, new TreeMap<String, Entry>(UTF8_ORDER));
        }
    }

    private Map<String, Entry> bucket(String bucket) throws IOException {
        Map<String, Entry> b = buckets.get(bucket);
        if (b == null) {
            throw new IOException("No such bucket: " + bucket);
        }
        return b;
    }

    @Override
    public synchronized String putObject(String bucket, String key, byte[] content,
            String contentType) throws IOException {
        byte[] copy = content == null ? new byte[0] : copyOf(content);
        String etag = hex(Crypto.md5(copy));
        bucket(bucket).put(key, new Entry(copy, new ObjectInfo(key, copy.length,
                contentType == null ? "application/octet-stream" : contentType, etag,
                Http1Date.format(System.currentTimeMillis()))));
        return etag;
    }

    @Override
    public synchronized byte[] getObject(String bucket, String key) throws IOException {
        Entry e = bucket(bucket).get(key);
        if (e == null) {
            throw new IOException("No such object: " + bucket + "/" + key);
        }
        return copyOf(e.content);
    }

    @Override
    public synchronized ObjectInfo headObject(String bucket, String key) throws IOException {
        // A missing bucket answers null, as every provider's 404 does: a test
        // must see what production sees for a misnamed bucket.
        Map<String, Entry> b = buckets.get(bucket);
        Entry e = b == null ? null : b.get(key);
        return e == null ? null : e.info;
    }

    @Override
    public synchronized void deleteObject(String bucket, String key) throws IOException {
        // Idempotent, a missing bucket included, as the providers are.
        Map<String, Entry> b = buckets.get(bucket);
        if (b != null) {
            b.remove(key);
        }
    }

    @Override
    public synchronized List<String> listObjects(String bucket, String prefix, int max)
            throws IOException {
        if (max < 0) {
            throw new IllegalArgumentException("listObjects max must not be negative: " + max);
        }
        List<String> out = new ArrayList<String>();
        for (String key : bucket(bucket).keySet()) {
            if (out.size() >= max) {
                break;
            }
            if (prefix == null || key.startsWith(prefix)) {
                out.add(key);
            }
        }
        return out;
    }

    @Override
    public String presignGet(String bucket, String key, int seconds) throws IOException {
        throw new IOException("The in-memory storage cannot presign a URL: no server "
                + "answers it. Set cn1.storage.url to a real provider or an emulator");
    }

    @Override
    public String presignPut(String bucket, String key, int seconds) throws IOException {
        return presignGet(bucket, key, seconds);
    }

    private static byte[] copyOf(byte[] data) {
        byte[] out = new byte[data.length];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
        }
        return sb.toString();
    }
}
