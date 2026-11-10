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
package com.codename1.impl.backend;

import com.codename1.backend.ByteSink;
import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.kv.KeyValueStore;
import com.codename1.backend.kv.KeyValueStores;
import java.io.IOException;

/// What the code the build weaves for `@Cacheable`, `@CachePut` and
/// `@CacheEvict` calls. Not application API.
///
/// Every entry lives in the calling thread's server's [KeyValueStore] under
/// `cn1:cache:<cache>:<key>`, as the JSON the generated codec wrote. A store
/// that cannot be reached is logged and treated as a miss: the method runs, as
/// it would with no cache, rather than the call failing.
public final class CacheRuntime {
    private static final String PREFIX = "cn1:cache:";

    private CacheRuntime() {
    }

    /// The stored JSON of `key` in `cache`, or null for a miss.
    public static byte[] get(String cache, String key) {
        try {
            return KeyValueStores.current().get(entry(cache, key));
        } catch (IOException err) {
            warn("read", cache, err);
            return null;
        }
    }

    /// Parses what [#get] answered, for the generated codec to read.
    public static Object parse(byte[] stored) throws IOException {
        return Json.parse(WireBuffer.fromUtf8(stored));
    }

    /// Keeps `value` under `key`; a null `value` removes the entry.
    public static void put(String cache, String key, Json.Writable value) {
        try {
            KeyValueStore store = KeyValueStores.current(); // NOPMD CloseResource - the server's store, closed when it stops
            if (value == null) {
                store.delete(entry(cache, key));
                return;
            }
            ByteSink out = new ByteSink(128);
            value.writeTo(out);
            byte[] data = new byte[out.length()];
            System.arraycopy(out.bytes(), 0, data, 0, data.length);
            store.set(entry(cache, key), data, ttlMillis(cache));
        } catch (IOException err) {
            warn("write", cache, err);
        }
    }

    /// Removes `key` from `cache`.
    public static void evict(String cache, String key) {
        try {
            KeyValueStores.current().delete(entry(cache, key));
        } catch (IOException err) {
            warn("evict from", cache, err);
        }
    }

    /// Removes every entry of `cache`.
    public static void evictAll(String cache) {
        try {
            KeyValueStores.current().deleteByPrefix(PREFIX + cache + ':');
        } catch (IOException err) {
            warn("clear", cache, err);
        }
    }

    /// Spring's default key: nothing for no parameter, the one parameter's text
    /// for one, and every parameter's otherwise -- separated so that `("a,b")`
    /// and `("a", "b")` stay two keys.
    public static String key(Object[] parts) {
        if (parts.length == 0) {
            return "";
        }
        if (parts.length == 1) {
            return String.valueOf(parts[0]);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String s = String.valueOf(parts[i]);
            for (int c = 0; c < s.length(); c++) {
                char ch = s.charAt(c);
                if (ch == ',' || ch == '\\') {
                    sb.append('\\');
                }
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String entry(String cache, String key) {
        return PREFIX + cache + ':' + key;
    }

    /// How long an entry of `cache` lives, 0 for until evicted.
    static long ttlMillis(String cache) throws IOException {
        Config config = BackendAccess.get().currentConfig();
        if (config == null) {
            return 0;
        }
        String own = config.get("cn1.cache." + cache + ".ttl");
        if (own == null) {
            own = config.get("cn1.cache.ttl");
        }
        if (own != null) {
            return seconds(own.trim(), "cn1.cache ttl") * 1000L;
        }
        String spring = config.get("spring.cache.redis.time-to-live");
        return spring == null ? 0 : duration(spring.trim());
    }

    private static long seconds(String value, String what) {
        try {
            long s = Long.parseLong(value);
            return s < 0 ? 0 : s;
        } catch (NumberFormatException err) {
            throw new IllegalStateException(what + " \"" + value + "\" is not a number of "
                    + "seconds", err);
        }
    }

    /// A Spring Boot duration: a bare number of milliseconds, a number with one of
    /// `ms s m h d`, or ISO-8601 `PT..H..M..S`.
    static long duration(String value) {
        String v = value;
        if (v.length() > 2 && (v.charAt(0) == 'P' || v.charAt(0) == 'p')) {
            return iso(v);
        }
        int end = 0;
        while (end < v.length() && Character.isDigit(v.charAt(end))) {
            end++;
        }
        if (end == 0) {
            throw new IllegalStateException("spring.cache.redis.time-to-live \"" + value
                    + "\" is not a duration");
        }
        long n = Long.parseLong(v.substring(0, end));
        String unit = v.substring(end).trim();
        long scale;
        if (unit.length() == 0 || "ms".equalsIgnoreCase(unit)) {
            scale = 1;
        } else if ("s".equalsIgnoreCase(unit)) {
            scale = 1000L;
        } else if ("m".equalsIgnoreCase(unit)) {
            scale = 60000L;
        } else if ("h".equalsIgnoreCase(unit)) {
            scale = 3600000L;
        } else if ("d".equalsIgnoreCase(unit)) {
            scale = 86400000L;
        } else {
            throw new IllegalStateException("spring.cache.redis.time-to-live \"" + value
                    + "\" has a unit this server does not know; use ms, s, m, h or d");
        }
        return n * scale;
    }

    private static long iso(String value) {
        long total = 0;
        long n = 0;
        boolean time = false;
        boolean digits = false;
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                n = n * 10 + (c - '0');
                digits = true;
                continue;
            }
            char u = c >= 'a' && c <= 'z' ? (char) (c - 32) : c;
            if (u == 'T') {
                time = true;
                continue;
            }
            if (!digits) {
                break;
            }
            if (u == 'D' && !time) {
                total += n * 86400000L;
            } else if (u == 'H' && time) {
                total += n * 3600000L;
            } else if (u == 'M' && time) {
                total += n * 60000L;
            } else if (u == 'S' && time) {
                total += n * 1000L;
            } else {
                throw new IllegalStateException("spring.cache.redis.time-to-live \"" + value
                        + "\" is not an ISO-8601 duration this server reads");
            }
            n = 0;
            digits = false;
        }
        return total;
    }

    private static void warn(String what, String cache, IOException err) {
        System.err.println("cn1: could not " + what + " cache \"" + cache + "\": "
                + err.getMessage());
    }
}
