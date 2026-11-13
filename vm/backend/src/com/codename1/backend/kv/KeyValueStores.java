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

import com.codename1.backend.Config;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;

/// Opens the [KeyValueStore] a configuration names, and finds the one of the
/// calling thread's server.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.kv.url` | `memory:` | `memory:`, `redis://...` or `rediss://...` |
/// | `cn1.kv.timeout` | 5000 | milliseconds a connection or a reply may take |
/// | `cn1.kv.pool.max` | 16 | idle connections kept |
///
/// Without `cn1.kv.url`, Spring Boot's `spring.data.redis.url` is used, and
/// without that `spring.data.redis.host` (with `.port`, `.username`,
/// `.password`, `.database` and `.ssl.enabled`) -- so a Spring configuration
/// carries over unchanged. Nothing set at all is the in-process store.
public final class KeyValueStores {
    /// The key a server keeps its store under.
    private static final String RESOURCE = "cn1.kv";

    private KeyValueStores() {
    }

    /// The store of the calling thread's server: opened from its configuration
    /// the first time it is asked for, and closed when the server stops.
    public static KeyValueStore current() throws IOException {
        return (KeyValueStore) BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return KeyValueStores.open(config);
                    }
                });
    }

    /// Whether `config` names a shared store, not the in-process one.
    public static boolean isConfigured(Config config) throws IOException {
        String url = url(config);
        return url != null && !url.startsWith("memory:");
    }

    /// The URL `config` names, or null for none.
    static String url(Config config) throws IOException {
        if (config == null) {
            return null;
        }
        String url = config.get("cn1.kv.url");
        if (url != null && url.trim().length() > 0) {
            return url.trim();
        }
        url = config.get("spring.data.redis.url");
        if (url != null && url.trim().length() > 0) {
            return url.trim();
        }
        String host = config.get("spring.data.redis.host");
        if (host == null || host.trim().length() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(
                config.getBoolean("spring.data.redis.ssl.enabled", false) ? "rediss://" : "redis://");
        String password = config.get("spring.data.redis.password");
        String user = config.get("spring.data.redis.username");
        if (password != null && password.length() > 0) {
            sb.append(user == null ? "" : escape(user)).append(':').append(escape(password))
              .append('@');
        }
        sb.append(host.trim()).append(':').append(config.getInt("spring.data.redis.port", 6379))
          .append('/').append(config.getInt("spring.data.redis.database", 0));
        return sb.toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' || c == '@' || c == ':' || c == '/') {
                sb.append('%').append(Character.forDigit((c >> 4) & 15, 16))
                  .append(Character.forDigit(c & 15, 16));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Opens the store `config` names; the caller closes it.
    public static KeyValueStore open(Config config) throws IOException {
        String url = url(config);
        int timeout = config == null ? 5000 : config.getInt("cn1.kv.timeout", 5000);
        int pool = config == null ? 16 : config.getInt("cn1.kv.pool.max", 16);
        return open(url == null ? "memory:" : url, timeout, pool);
    }

    /// Opens the store at `url`; the caller closes it.
    public static KeyValueStore open(String url, int timeoutMillis, int maxIdle)
            throws IOException {
        if (url.startsWith("memory:")) {
            return new MemoryKeyValueStore();
        }
        if (url.regionMatches(true, 0, "redis://", 0, 8)
                || url.regionMatches(true, 0, "rediss://", 0, 9)) {
            return new RedisKeyValueStore(url, timeoutMillis, timeoutMillis, maxIdle);
        }
        throw new IOException("cn1.kv.url \"" + url + "\" names no store this server "
                + "knows; use memory:, redis:// or rediss://");
    }
}
