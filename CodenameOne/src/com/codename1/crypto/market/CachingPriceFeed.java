/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.market;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// A [PriceFeed] that remembers each pair's answer for a while. A wallet screen that refreshes
/// every few seconds then asks the source once per pair per interval, which is what keeps it
/// inside a public API's rate limit. When the source fails and a remembered answer is no older
/// than the stale limit, that answer is returned rather than the failure.
///
/// The cache keeps at most 512 pairs and is not synchronized; on the backend give each thread
/// its own or guard it.
@com.codename1.impl.SharedWithBackend
public class CachingPriceFeed implements PriceFeed {
    private static final int MAX_ENTRIES = 512;

    private final PriceFeed source;
    private final long freshMillis;
    private final long staleMillis;
    private final Map<String, Entry> cache = new HashMap<String, Entry>();

    /// A cache answering from memory for `freshMillis`, and on a source failure for up to
    /// `staleMillis`.
    public CachingPriceFeed(PriceFeed source, long freshMillis, long staleMillis) {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        this.source = source;
        this.freshMillis = freshMillis;
        this.staleMillis = Math.max(freshMillis, staleMillis);
    }

    @Override
    public Quote getQuote(String base, String quote) throws IOException {
        String key = PriceFeeds.upper(base) + "/" + PriceFeeds.upper(quote);
        long now = now();
        Entry hit = cache.get(key);
        if (hit != null && now - hit.at < freshMillis) {
            return hit.quote;
        }
        try {
            Quote q = source.getQuote(base, quote);
            if (cache.size() >= MAX_ENTRIES && !cache.containsKey(key)) {
                cache.clear();
            }
            cache.put(key, new Entry(q, now));
            return q;
        } catch (IOException e) {
            if (hit != null && now - hit.at < staleMillis) {
                return hit.quote;
            }
            throw e;
        }
    }

    /// Forgets every remembered answer.
    public void clear() {
        cache.clear();
    }

    /// The clock, in milliseconds; a test overrides it.
    protected long now() {
        return System.currentTimeMillis();
    }

    private static final class Entry {
        final Quote quote;
        final long at;

        Entry(Quote quote, long at) {
            this.quote = quote;
            this.at = at;
        }
    }
}
