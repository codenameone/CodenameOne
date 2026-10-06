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

/// Counts requests under a key and says when there have been too many.
///
/// An application that keeps its counts somewhere of its own -- a database, so
/// that several servers share one count -- implements this.
public interface RateLimiter {
    /// Counts one request under `key`.
    ///
    /// @return whether the request is within the limit
    boolean tryAcquire(String key);

    /// How many seconds a request just refused under `key` should wait before
    /// trying again: what its `Retry-After` says. One second unless the limiter
    /// knows better.
    default long retryAfterSeconds(String key) {
        return 1;
    }

    /// A limiter that keeps its counts where this one does, apart from this
    /// one's, with a limit of its own; null when this limiter cannot make one,
    /// which is what it answers unless it says more.
    ///
    /// The parts of the layer that count something themselves -- attempts at
    /// a second factor, tries at a device's code -- ask the application's
    /// limiter bean for one each. That is how a bean declared to throttle an
    /// API at 600 a minute comes to count guesses at a one-time code at five
    /// in five minutes, in the same database, without the two sharing either
    /// a count or a limit.
    ///
    /// @param name what keeps the new limiter's counts apart
    /// @param permits how many requests a key may make in a period
    /// @param periodSeconds the length of the period
    default RateLimiter derive(String name, int permits, long periodSeconds) {
        return null;
    }
}
