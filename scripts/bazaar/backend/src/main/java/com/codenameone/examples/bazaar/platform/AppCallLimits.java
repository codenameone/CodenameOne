/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.platform;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.web.security.Throttle;

import java.util.HashMap;
import java.util.Map;

/// Each app installation's call allowance: a leaky bucket of [#capacity]
/// calls that drains [#leakPerSecond] a second. Every key API call an app's
/// token makes adds one; a call that would overflow it is refused with 429 and
/// `Retry-After`, and every answer carries `X-Bazaar-Api-Call-Limit:
/// <in the bucket>/<capacity>`, so an app can pace itself before it is refused.
///
/// `bazaar.apps.bucket` and `bazaar.apps.leak-per-second` change the two
/// numbers. Counts live in this process, as [Throttle]'s do.
@Component
public class AppCallLimits {
    public static final String HEADER = "X-Bazaar-Api-Call-Limit";

    private final int capacity;
    private final double leakPerSecond;
    /// Per installation: the level, and when it was last drained.
    private final Map<String, double[]> buckets = new HashMap<String, double[]>();
    private Throttle.Clock clock = new Throttle.Clock();

    public AppCallLimits(Config config) throws java.io.IOException {
        this.capacity = Math.max(1, config.getInt("bazaar.apps.bucket", 40));
        double leak = config.getInt("bazaar.apps.leak-per-second", 2);
        this.leakPerSecond = leak <= 0 ? 2 : leak;
    }

    /// Replaces the time source; for tests.
    public synchronized void useClock(Throttle.Clock clock) {
        this.clock = clock;
    }

    public int capacity() {
        return capacity;
    }

    /// Adds one call to `installationId`'s bucket. Answers the level after
    /// it, or, when the bucket is full, minus the seconds until it has room.
    public synchronized int take(String installationId) {
        long now = clock.now();
        double[] b = buckets.get(installationId);
        if (b == null) {
            b = new double[] {0, now};
            buckets.put(installationId, b);
        }
        double level = Math.max(0, b[0] - (now - b[1]) / 1000.0 * leakPerSecond);
        b[1] = now;
        if (level + 1 > capacity) {
            b[0] = level;
            int wait = (int) Math.ceil((level + 1 - capacity) / leakPerSecond);
            return -Math.max(1, wait);
        }
        b[0] = level + 1;
        return (int) Math.ceil(b[0]);
    }
}
