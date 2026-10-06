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

import com.codename1.backend.security.Clock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/// A limit kept in this process's memory: a token bucket per key.
///
/// ```java
/// RateLimiter logins = new InMemoryRateLimiter(5, 60);     // 5 a minute, per key
/// ```
///
/// A key starts with `permits` tokens and gets them back at `permits` per
/// period, evenly: a client may spend them all at once and is then let through
/// at the steady rate, which is what "5 a minute" means to the person waiting.
///
/// **The count is per process.** A server run as several instances behind a
/// load balancer has one count in each, so a client is allowed up to the limit
/// times the number of instances. Where the limit is there to keep load down
/// that is usually what is wanted. Where it is a security bound -- attempts at
/// a password -- count somewhere the instances share, behind the [RateLimiter]
/// interface. The counts are also gone when the process restarts.
///
/// Memory is bounded: no more than a set number of keys are remembered, 100000
/// unless set. A key whose bucket has refilled is forgotten, since forgetting
/// it changes nothing; past the bound the keys idle longest are forgotten
/// early, which gives such a key its tokens back sooner than it earned them. A
/// flood of distinct keys therefore costs a bounded amount of memory, and the
/// most it buys the flooder is the full bucket a new key starts with anyway.
public final class InMemoryRateLimiter implements RateLimiter {
    private final int permits;
    private final long periodMillis;
    private final int maxKeys;
    /// key to {tokens, as of}; the least recently used first.
    private final LinkedHashMap<String, double[]> buckets = new LinkedHashMap<String, double[]>();
    private Clock clock = Clock.SYSTEM;

    /// @param permits how many requests a key may make in a period
    /// @param periodSeconds the length of the period
    public InMemoryRateLimiter(int permits, long periodSeconds) {
        this(permits, periodSeconds, 100000);
    }

    /// @param maxKeys the most keys remembered at once
    public InMemoryRateLimiter(int permits, long periodSeconds, int maxKeys) {
        if (permits < 1 || periodSeconds < 1 || maxKeys < 1) {
            throw new IllegalArgumentException("A rate limit needs at least one permit, a period "
                    + "of at least a second and room for at least one key");
        }
        this.permits = permits;
        this.periodMillis = periodSeconds * 1000L;
        this.maxKeys = maxKeys;
    }

    /// A limiter in this process with counts of its own, read on the same
    /// clock; the name is not needed to keep them apart.
    @Override
    public synchronized RateLimiter derive(String name, int permits, long periodSeconds) {
        InMemoryRateLimiter derived = new InMemoryRateLimiter(permits, periodSeconds, maxKeys);
        derived.setClock(clock);
        return derived;
    }

    /// Reads the time from `clock` instead of the machine.
    public synchronized void setClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.clock = clock;
    }

    /// How many keys are remembered now.
    public synchronized int size() {
        return buckets.size();
    }

    @Override
    public synchronized boolean tryAcquire(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        long now = clock.currentTimeMillis();
        // Taken out and put back, so the map stays in order of use.
        double[] bucket = buckets.remove(key);
        if (bucket == null) {
            makeRoom(now);
            bucket = new double[] {permits, now};
        } else {
            refill(bucket, now);
        }
        boolean allowed = bucket[0] >= 1.0;
        if (allowed) {
            bucket[0] -= 1.0;
        }
        buckets.put(key, bucket);
        return allowed;
    }

    @Override
    public synchronized long retryAfterSeconds(String key) {
        double[] bucket = key == null ? null : buckets.get(key);
        if (bucket == null) {
            return 1;
        }
        double tokens = tokensAt(bucket, clock.currentTimeMillis());
        if (tokens >= 1.0) {
            return 1;
        }
        double millis = (1.0 - tokens) * periodMillis / permits;
        // Whole milliseconds first, rounding away what floating point left
        // over: 0.05 of a 20 second token is one second, not 1000.0000000000008
        // milliseconds rounded up to two.
        long whole = (long) millis;
        if (millis - whole > 0.000001) {
            whole++;
        }
        long seconds = (whole + 999) / 1000;
        return seconds < 1 ? 1 : seconds;
    }

    private double tokensAt(double[] bucket, long now) {
        double elapsed = now - bucket[1];
        if (elapsed <= 0) {
            return bucket[0];
        }
        double tokens = bucket[0] + elapsed * permits / periodMillis;
        return tokens > permits ? permits : tokens;
    }

    private void refill(double[] bucket, long now) {
        bucket[0] = tokensAt(bucket, now);
        if (now > bucket[1]) {
            bucket[1] = now;
        }
    }

    /// Makes room for one more key: the ones whose buckets have refilled go
    /// first, and after them the one idle longest.
    private void makeRoom(long now) {
        if (buckets.size() < maxKeys) {
            return;
        }
        Iterator<Map.Entry<String, double[]>> eldest = buckets.entrySet().iterator();
        while (eldest.hasNext()) {
            double[] bucket = eldest.next().getValue();
            // In order of use: once one is still short, so is everything after
            // it that was used at the same rate, and the sweep has done enough.
            if (tokensAt(bucket, now) < permits) {
                break;
            }
            eldest.remove();
        }
        while (buckets.size() >= maxKeys) {
            Iterator<String> first = buckets.keySet().iterator();
            first.next();
            first.remove();
        }
    }
}
