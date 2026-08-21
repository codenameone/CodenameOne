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
package com.codename1.intents;

/// The circumstances one invocation runs under. Declare it as the first
/// parameter of a handler to receive it; handlers that do not care about any of
/// this simply leave it out.
///
/// ```java
/// @AppIntent(value = "sync_now", title = "Sync now", headless = true)
/// public static IntentResult sync(IntentContext ctx) {
///     while (Backend.hasMore()) {
///         if (ctx.isCancelled()) {
///             return IntentResult.failed("Sync did not finish in time");
///         }
///         Backend.syncNextPage();
///     }
///     return IntentResult.spoken("Everything is up to date");
/// }
/// ```
public final class IntentContext {

    private final IntentSource source;
    private final boolean headless;
    private final long deadline;
    // Guarded by its own monitor rather than declared volatile: the framework
    // cancels from the timeout thread while the handler polls from its own, so
    // the write has to be visible across threads, and the repo's static-analysis
    // gate does not permit volatile. Contention is nil -- one write, occasional
    // reads.
    private final boolean[] cancelled = new boolean[1];

    /// Framework entry point. Applications receive these, they do not build them.
    ///
    /// #### Parameters
    ///
    /// - `source`: where the invocation came from
    /// - `headless`: whether the app is running without a visible UI
    /// - `deadline`: absolute epoch millis after which the result is discarded
    public IntentContext(IntentSource source, boolean headless, long deadline) {
        this.source = source == null ? IntentSource.UNKNOWN : source;
        this.headless = headless;
        this.deadline = deadline;
    }

    /// Where this invocation came from.
    public IntentSource getSource() {
        return source;
    }

    /// True when the handler is running with no UI on screen.
    ///
    /// This is the flag that decides what the handler is allowed to touch: no
    /// `Form`, no `Dialog`, nothing that needs a window. See the package
    /// documentation for the full contract.
    public boolean isHeadless() {
        return headless;
    }

    /// The absolute epoch-millis instant after which this invocation's result is
    /// no longer wanted.
    ///
    /// Matches the shape of
    /// `com.codename1.background.BackgroundFetch#performBackgroundFetch(long, com.codename1.util.Callback)`
    /// so the two kinds of background work read the same way.
    public long getDeadline() {
        return deadline;
    }

    /// Milliseconds left before the deadline, or 0 once it has passed.
    public long getRemainingTime() {
        long remaining = deadline - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    /// True once the framework has stopped waiting for this invocation.
    ///
    /// Cancellation is cooperative: nothing interrupts a running handler, so a
    /// handler doing extended work should check this between steps. Anything it
    /// returns after cancellation is discarded, which is why durable work should
    /// be committed to storage as it completes rather than only at the end.
    public boolean isCancelled() {
        synchronized (cancelled) {
            if (cancelled[0]) {
                return true;
            }
        }
        return deadline > 0 && System.currentTimeMillis() > deadline;
    }

    /// Framework entry point: marks this invocation abandoned.
    public void cancel() {
        synchronized (cancelled) {
            cancelled[0] = true;
        }
    }

    @Override
    public String toString() {
        return "IntentContext[" + source + (headless ? ", headless" : "") + "]";
    }
}
