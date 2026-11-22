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
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.ratelimit.RateLimiter;

import java.util.HashMap;
import java.util.Map;

/// Rate limits by named rule and key: one service for every place a stranger
/// can make this server do something costly or guessable -- signing in, the
/// sign-in text message, placing an order, a register PIN, the public forms.
///
/// A rule is `count/seconds`, read from `bazaar.limit.<rule>` with the default
/// in [#DEFAULTS]; the test profile raises the ones a test suite would trip.
/// Each key of a rule has a bucket of `count` tokens that refills evenly over
/// `seconds`, so a burst is allowed and a steady stream is held to the rate.
///
/// [#lockout] is the other shape: failures are counted rather than attempts,
/// and once a key has too many the key is refused outright until the window
/// has run, whatever the next attempt holds. Signing in with a password uses
/// it three ways: per account and address (`signin-failures`, five a quarter
/// hour), per address across accounts (`signin-failures-ip`), and per account
/// from every address (`signin-failures-account`), bounding guessing one
/// user's password from many addresses. A stranger can trip that last one
/// on purpose, so a browser holding the account's known-device cookie
/// (`KnownDevices`, set on each successful sign-in) skips it: the lockout
/// then refuses only browsers that never signed in to the account.
///
/// Counts live in this process. A deployment of several instances multiplies
/// each allowance by the instance count -- still a bound, just a looser one.
@Component
public class Throttle {
    /// Rule -> "count/seconds".
    static final String[][] DEFAULTS = {
        {"signin", "20/60"},
        {"signin-failures", "5/900"},
        // Wrong passwords from one address across every account: a spray.
        {"signin-failures-ip", "30/900"},
        // Wrong passwords for one account from every address: distributed
        // guessing.
        {"signin-failures-account", "50/900"},
        {"sms", "5/600"},
        {"checkout", "10/60"},
        {"pin", "10/300"},
        {"form", "30/60"}, {"chat", "5/600"},
        // The floating chat: messages on a conversation that exists, and the
        // frames that are not messages (typing, read marks, call signalling).
        {"chat-live", "30/60"}, {"chat-signal", "600/60"},
        {"upload", "30/600"},
        {"totp-enrol", "10/600"},
        // A store API key on the key API.
        {"keyapi", "240/60"},
        // The merchant assistant: each request is a model call, the most
        // expensive thing a signed-in merchant can ask for.
        {"ai", "20/60"},
        // A review left through an e-mailed review link: a stranger with a
        // link, so held to what a person writes.
        {"review", "10/600"},
    };

    private final Map<String, int[]> rules = new HashMap<String, int[]>();
    private final Map<String, double[]> buckets = new HashMap<String, double[]>();
    private final Map<String, long[]> failures = new HashMap<String, long[]>();
    private Clock clock = new Clock();

    public Throttle(Config config) throws java.io.IOException {
        for (int iter = 0; iter < DEFAULTS.length; iter++) {
            String rule = DEFAULTS[iter][0];
            rules.put(rule, parse(config.get("bazaar.limit." + rule, DEFAULTS[iter][1]),
                    DEFAULTS[iter][1]));
        }
    }

    /// Time, replaceable by a test.
    public static class Clock {
        public long now() {
            return System.currentTimeMillis();
        }
    }

    public void useClock(Clock clock) {
        this.clock = clock;
    }

    static int[] parse(String value, String fallback) {
        String v = value == null ? fallback : value.trim();
        int slash = v.indexOf('/');
        try {
            int count = Integer.parseInt(v.substring(0, slash).trim());
            int seconds = Integer.parseInt(v.substring(slash + 1).trim());
            if (count > 0 && seconds > 0) {
                return new int[] {count, seconds};
            }
        } catch (RuntimeException bad) {
            // falls through to the default
        }
        return fallback == value ? new int[] {10, 60} : parse(fallback, fallback);
    }

    private int[] rule(String rule) {
        int[] r = rules.get(rule);
        if (r == null) {
            throw new IllegalArgumentException("No rate rule " + rule);
        }
        return r;
    }

    /// Takes a token from `key`'s bucket of `rule`; false when there is none.
    public synchronized boolean tryAcquire(String rule, String key) {
        int[] r = rule(rule);
        String k = rule + "\n" + (key == null ? "" : key);
        long now = clock.now();
        double[] b = buckets.get(k);
        if (b == null) {
            if (buckets.size() > 50000) {
                sweep(now);
            }
            b = new double[] {r[0], now};
            buckets.put(k, b);
        }
        double perMs = r[0] / (r[1] * 1000.0);
        b[0] = Math.min(r[0], b[0] + (now - (long) b[1]) * perMs);
        b[1] = now;
        if (b[0] < 1) {
            return false;
        }
        b[0] -= 1;
        return true;
    }

    /// [#tryAcquire], answering 429 when refused.
    public void check(String rule, String key) {
        if (!tryAcquire(rule, key)) {
            throw new ResponseStatusException(429, "Too many requests; try again shortly");
        }
    }

    private void sweep(long now) {
        java.util.Iterator<Map.Entry<String, double[]>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            if (now - (long) it.next().getValue()[1] > 3600000L) {
                it.remove();
            }
        }
    }

    /// Whether `key` has used up its failures under `rule` and is locked out.
    public synchronized boolean lockout(String rule, String key) {
        int[] r = rule(rule);
        long[] f = failures.get(rule + "\n" + key);
        if (f == null) {
            return false;
        }
        if (clock.now() - f[1] > r[1] * 1000L) {
            failures.remove(rule + "\n" + key);
            return false;
        }
        return f[0] >= r[0];
    }

    /// Counts a failure of `key` under `rule`; the window starts at the first.
    public synchronized void fail(String rule, String key) {
        int[] r = rule(rule);
        String k = rule + "\n" + key;
        long now = clock.now();
        long[] f = failures.get(k);
        if (f == null || now - f[1] > r[1] * 1000L) {
            f = new long[] {0, now};
            failures.put(k, f);
        }
        f[0]++;
    }

    /// Forgets `key`'s failures under `rule`: it has just succeeded.
    public synchronized void succeed(String rule, String key) {
        failures.remove(rule + "\n" + key);
    }

    /// `rule` as the security chain's limiter type, for `http.rateLimit`.
    /// Deliberately not a bean: the multi-factor configurer insists on a single
    /// `RateLimiter` bean when there is any.
    public RateLimiter limiter(final String rule) {
        rule(rule);
        return new RateLimiter() {
            @Override
            public boolean tryAcquire(String key) {
                return Throttle.this.tryAcquire(rule, key);
            }
        };
    }

    /// The client's address, as the key of a per-client rule.
    public static String client(HttpServer.Request request) {
        String a = request.getRemoteAddress();
        return a == null ? "" : a;
    }
}
