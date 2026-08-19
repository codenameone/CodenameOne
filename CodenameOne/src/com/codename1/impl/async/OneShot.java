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
package com.codename1.impl.async;

import com.codename1.util.AsyncResource;

/// An [AsyncResource] that keeps the first outcome and ignores the
/// rest.
///
/// Shared by the subsystems that front a platform service through an
/// [AsyncResource] -- `com.codename1.health` and `com.codename1.home` --
/// because the hazards below are properties of that shape, not of any one
/// of them. The war stories are health's, since that is where they were
/// paid for.
///
/// Operations of this kind are armed with a timeout, and `AsyncResource`
/// itself allows a resource to be completed more than once -- so a
/// platform call that answered after its timeout had fired ran the
/// callbacks a second time. A write reported TIMEOUT and then
/// reported success; a caller that retried on the timeout had both
/// inserts commit, which is a duplicate record in the user's health
/// data. A late drain re-ran the gate and cleared the in-flight state
/// of whichever drain was running by then.
///
/// The port is not asked to be careful about this. It gets one of
/// these and may answer whenever it likes; a late answer is dropped.
///
/// #### Both halves of every operation, not just the port-facing one
///
/// This is also what those APIs hand *back*, for the mirror-image
/// reason: the caller owns a public [AsyncResource] and may cancel it.
/// While only the internal resource was a one-shot, cancelling a read or
/// a write left the port free to answer afterwards -- and the plain
/// resource then ran the success callback, delivering a value from
/// something whose `isCancelled()` answers true and whose `get()` throws
/// the cancellation. Cancellation is the third terminal transition and
/// gets the same treatment as the other two, so it lives here rather
/// than in each of the twenty-odd places an operation is started.
///
/// It is public because those places span several packages and both mobile
/// ports; nothing outside the implementation needs it.
/// Not final: [EdtResult] extends it to add the EDT hand-off that every
/// caller-facing result needs. Internal resources -- the one a port completes,
/// the one a permission flow runs on -- stay plain, because the base class
/// does its own threading around them.
public class OneShot<T> extends AsyncResource<T> {

    /// Cancellation is a terminal transition like the other two, and
    /// it has to share their monitor.
    ///
    /// `AsyncResource.cancel` guards on its own private lock while
    /// these guard on this instance, so a cancel arriving while a
    /// platform callback was inside `complete` passed both checks:
    /// the callback had already read `isDone()` as false, cancel then
    /// finished, and `super.complete` -- which does not consult
    /// `cancelled` -- ran the success callback anyway. The caller was
    /// handed a value by a resource whose `isCancelled()` answers
    /// true and whose `get()` throws the cancellation.
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            // Already settled, so there is nothing to cancel -- which
            // is the same answer the superclass gives, arrived at
            // without racing it.
            return false;
        }
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public synchronized void complete(T value) {
        if (isDone()) {
            return;
        }
        super.complete(value);
    }

    @Override
    public synchronized void error(Throwable t) {
        if (isDone()) {
            return;
        }
        super.error(t);
    }
}
