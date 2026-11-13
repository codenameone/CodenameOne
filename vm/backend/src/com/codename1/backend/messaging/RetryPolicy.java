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
package com.codename1.backend.messaging;

import com.codename1.backend.Config;
import java.io.IOException;

/// How often a failed message is delivered again, and how long apart.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.messaging.retry.max-attempts` | 3 | deliveries before the dead-letter destination |
/// | `cn1.messaging.retry.backoff` | 1000 | milliseconds before the second delivery |
/// | `cn1.messaging.retry.multiplier` | 2 | how much longer each wait is than the last |
/// | `cn1.messaging.retry.max-backoff` | 30000 | the longest wait |
public final class RetryPolicy {
    private final int maxAttempts;
    private final long backoff;
    private final int multiplier;
    private final long maxBackoff;

    /// A policy of `maxAttempts` deliveries, waiting `backoffMillis` and then
    /// `multiplier` times longer each time, up to `maxBackoffMillis`.
    public RetryPolicy(int maxAttempts, long backoffMillis, int multiplier, long maxBackoffMillis) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoff = Math.max(0, backoffMillis);
        this.multiplier = Math.max(1, multiplier);
        this.maxBackoff = Math.max(this.backoff, maxBackoffMillis);
    }

    /// The policy `config` describes.
    public static RetryPolicy of(Config config) throws IOException {
        if (config == null) {
            return new RetryPolicy(3, 1000, 2, 30000);
        }
        return new RetryPolicy(config.getInt("cn1.messaging.retry.max-attempts", 3),
                config.getInt("cn1.messaging.retry.backoff", 1000),
                config.getInt("cn1.messaging.retry.multiplier", 2),
                config.getInt("cn1.messaging.retry.max-backoff", 30000));
    }

    /// Deliveries before a message is dead-lettered.
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /// Milliseconds to wait before delivery `attempt + 1`, after delivery
    /// `attempt` failed.
    public long delayAfter(int attempt) {
        long d = backoff;
        for (int i = 1; i < attempt && d < maxBackoff; i++) {
            d *= multiplier;
        }
        return Math.min(d, maxBackoff);
    }

    /// The dead-letter destination of `destination`.
    public static String deadLetter(String destination) {
        return destination + ".dlq";
    }
}
