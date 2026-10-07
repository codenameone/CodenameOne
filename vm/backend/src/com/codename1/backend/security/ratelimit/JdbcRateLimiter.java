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
package com.codename1.backend.security.ratelimit;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.security.Clock;
import java.io.IOException;

/// Counts in the server's database, in the `cn1_rate_limit` table of
/// [com.codename1.backend.security.SecuritySchema], so that every process of a
/// deployment shares one limit.
///
/// ```java
/// http.rateLimit("/login", RateLimitKeys.clientAddress(),
///         new JdbcRateLimiter(dataSource, "login", 5, 60));
/// ```
///
/// A fixed window: `permits` requests in each `periodSeconds`, counted from the
/// first request of the window. Every request is decided by one conditional
/// `UPDATE` and the number of rows it changed, so two processes counting at
/// the same moment cannot both take the last permit.
///
/// It costs a statement or two per request it counts, which is the price of
/// agreeing across processes; [InMemoryRateLimiter] costs none and counts for
/// its own process alone. A database that cannot be reached refuses nobody:
/// the limit is a courtesy to the server, and a store that is down must not
/// take the application with it.
public final class JdbcRateLimiter implements RateLimiter {
    /// The longest `limit_key` written; the column holds at least this on every engine.
    static final int MAX_KEY = 200;

    private final DataSource dataSource;
    private final String name;
    private final int permits;
    private final long periodMillis;
    private Clock clock = Clock.SYSTEM;

    /// @param name what keeps this limiter's counts apart from another's in the
    /// same table; two limiters of one name share their counts on purpose
    public JdbcRateLimiter(DataSource dataSource, String name, int permits, long periodSeconds) {
        if (dataSource == null || name == null || name.length() == 0) {
            throw new IllegalArgumentException("A data source and a name are required");
        }
        if (permits < 1 || periodSeconds < 1) {
            throw new IllegalArgumentException("A rate limit needs at least one permit and a "
                    + "period of at least a second");
        }
        this.dataSource = dataSource;
        this.name = name;
        this.permits = permits;
        this.periodMillis = periodSeconds * 1000L;
    }

    /// A limiter over the same table, under `name`, read on the same clock.
    @Override
    public RateLimiter derive(String name, int permits, long periodSeconds) {
        JdbcRateLimiter derived = new JdbcRateLimiter(dataSource, name, permits, periodSeconds);
        derived.clock = clock;
        return derived;
    }

    public void setClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.clock = clock;
    }

    /// The value of `limit_key` for `key`: the limiter's name and the key, or, when that is
    /// longer than the column, as much of it as fits ahead of a digest of all of it.
    ///
    /// The column is bounded and a client chooses part of some keys, so a long one has to
    /// be shortened. Cutting it off is what this used to do, and that made every key with
    /// the same first 200 characters one row: a limiter with a long name counted all its
    /// callers together, and somebody who chose a long user name could spend another
    /// user's permits. The digest keeps keys that differ anywhere apart.
    static String row(String name, String key) {
        String full = name + "|" + key;
        if (full.length() <= MAX_KEY) {
            return full;
        }
        String digest;
        try {
            digest = Base64Url.encode(Crypto.sha256(full.getBytes("UTF-8")));
        } catch (IOException err) {
            throw new IllegalStateException("UTF-8 is not available", err);
        }
        return full.substring(0, MAX_KEY - 1 - digest.length()) + "#" + digest;
    }

    private String row(String key) {
        return row(name, key);
    }

    @Override
    public boolean tryAcquire(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        String row = row(key);
        long now = clock.currentTimeMillis();
        Long windowFloor = Long.valueOf(now - periodMillis);
        try {
            if (count(row, windowFloor)) {
                return true;
            }
            // The window has passed: start the next one. Only one of several
            // callers at this moment changes the row; the rest count in the
            // window it started.
            if (dataSource.execute("UPDATE cn1_rate_limit SET window_start = ?, hits = 1 WHERE "
                    + "limit_key = ? AND window_start <= ?", new Object[] {Long.valueOf(now), row,
                        windowFloor}) == 1) {
                return true;
            }
            if (dataSource.queryOne("SELECT hits FROM cn1_rate_limit WHERE limit_key = ?",
                    new Object[] {row}) != null) {
                // The row is there and neither statement took it: the window is
                // full, or another caller has just started the next one.
                return count(row, windowFloor);
            }
            try {
                dataSource.execute("INSERT INTO cn1_rate_limit (limit_key, window_start, hits) "
                        + "VALUES (?, ?, 1)", new Object[] {row, Long.valueOf(now)});
                return true;
            } catch (IOException raced) {
                // Another caller inserted the row first; count in its window.
                return count(row, windowFloor);
            }
        } catch (IOException err) {
            System.err.println("cn1: the rate limit store could not be reached, so the request "
                    + "is not counted: " + err.getMessage());
            return true;
        }
    }

    /// Deletes the row of `key`. A database that cannot be reached leaves the
    /// count as it is, which is the safe way round for a bound on guesses.
    @Override
    public void reset(String key) {
        if (key == null) {
            return;
        }
        try {
            dataSource.execute("DELETE FROM cn1_rate_limit WHERE limit_key = ?",
                    new Object[] {row(key)});
        } catch (IOException err) {
            System.err.println("cn1: the rate limit store could not be reached, so a count "
                    + "was not cleared: " + err.getMessage());
        }
    }

    /// One more hit in the row's window, when that window is still open and
    /// has room.
    private boolean count(String row, Long windowFloor) throws IOException {
        return dataSource.execute("UPDATE cn1_rate_limit SET hits = hits + 1 WHERE limit_key = ? "
                + "AND window_start > ? AND hits < ?", new Object[] {row, windowFloor,
                    Integer.valueOf(permits)}) == 1;
    }

    @Override
    public long retryAfterSeconds(String key) {
        if (key == null) {
            return 1;
        }
        try {
            java.util.Map found = dataSource.queryOne("SELECT window_start FROM cn1_rate_limit "
                    + "WHERE limit_key = ?", new Object[] {row(key)});
            Object start = found == null ? null : found.get("window_start");
            if (!(start instanceof Number)) {
                return 1;
            }
            long left = ((Number) start).longValue() + periodMillis - clock.currentTimeMillis();
            long seconds = (left + 999) / 1000;
            return seconds < 1 ? 1 : seconds;
        } catch (IOException err) {
            return 1;
        }
    }

    /// Deletes the counts of every limiter whose window ended more than
    /// `olderThanSeconds` ago; for a scheduled job, since a key that is never
    /// seen again leaves its row behind.
    ///
    /// @return how many rows were deleted
    public int deleteExpired(long olderThanSeconds) throws IOException {
        return dataSource.execute("DELETE FROM cn1_rate_limit WHERE window_start < ?",
                new Object[] {Long.valueOf(clock.currentTimeMillis() - olderThanSeconds * 1000L)});
    }
}
