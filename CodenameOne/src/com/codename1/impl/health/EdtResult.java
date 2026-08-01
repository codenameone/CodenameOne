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
package com.codename1.impl.health;

import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import com.codename1.util.EasyThread;
import com.codename1.ui.Display;

/// The resource every public health operation hands back: one outcome,
/// delivered on the EDT.
///
/// This is what makes the threading contract a contract rather than a
/// description of whatever each port happens to do. It used to depend on the
/// backend: the mobile ports complete on the EDT and the result was handed
/// back there, while a local-backed store -- the simulator, desktop, the
/// JavaScript port -- completed on whichever thread called, so the same app
/// code updating a label from a read callback worked on a phone and produced a
/// repaint glitch on the desktop. That was documented as a known asymmetry,
/// which is not a design; a callback that may or may not be on the EDT is one
/// the caller has to defend against every time, and most callers will not.
///
/// Dispatching here rather than at each completion site is deliberate: the
/// ports complete these resources directly from `doAggregate`, `doWrite`,
/// `doDelete` and `doDrainChanges`, so a rule enforced at the call sites is
/// one the next port has to be told about. Enforced by the type, a port cannot
/// get it wrong.
///
/// Already on the EDT means completed inline, so a callback chain that
/// completes another resource does not queue a runnable per link.
public final class EdtResult<T> extends OneShot<T> {

    /// EVERY off-EDT registration is marshalled, not only one that finds the resource
    /// already settled.
    ///
    /// `AsyncResource.ready` runs the callback immediately, on the registering thread,
    /// when the resource has already settled -- so the guarantee this class exists to
    /// make held only for listeners attached before completion. Every facade action that
    /// answers without a backend (`openHealthSettings`, `openProviderSetup`) completes
    /// the resource before returning it, so the caller CANNOT attach in time, and which
    /// thread the callback ran on came down to whether the EDT had drained the hop yet.
    /// Off the EDT on a busy machine, on it on an idle one -- a callback that is usually
    /// on the EDT is exactly the thing the class doc calls not-a-design.
    ///
    /// Testing `isDone()` first reproduced that in miniature: a background caller that
    /// found the resource unfinished fell through to the synchronous path, and a
    /// completion landing in the gap before it registered made `AsyncResource.ready`
    /// deliver on the background thread after all. The window is small and the failure it
    /// produces -- a health callback touching a form off the EDT -- is a repaint glitch or
    /// a corrupted layout that nobody traces back to here. Hopping unconditionally has no
    /// such gap: registration and completion then both happen on the EDT, so they are
    /// ordered by the EDT rather than by a check.
    ///
    /// Already on the EDT still registers inline, which is what keeps a callback chain
    /// from queueing a runnable per link.
    ///
    /// Deliberately NOT done for `except`. Reading the error out of an already-failed
    /// resource by registering a callback and looking at what it captured is an
    /// established idiom here -- `HealthFallbackTest.errorOf` and `BtTestUtil` both do
    /// it, and it depends on that call being synchronous. The asymmetry is the honest
    /// one: this contract exists so a callback that acts on a VALUE -- updates a label,
    /// touches a form -- is on the EDT, and introspecting a failure that has already
    /// happened is not that.
    @Override
    public AsyncResource<T> ready(SuccessCallback<T> callback, EasyThread t) {
        if (!isCancelled() && Display.isInitialized()
                && !Display.getInstance().isEdt()) {
            final SuccessCallback<T> target = callback;
            final EasyThread thread = t;
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    EdtResult.super.ready(target, thread);
                }
            });
            return this;
        }
        return super.ready(callback, t);
    }

    @Override
    public void complete(T value) {
        if (Display.getInstance().isEdt()) {
            super.complete(value);
            return;
        }
        Display.getInstance().callSerially(new Deliver<T>(this, value, null));
    }

    @Override
    public void error(Throwable t) {
        if (Display.getInstance().isEdt()) {
            super.error(t);
            return;
        }
        Display.getInstance().callSerially(new Deliver<T>(this, null, t));
    }

    /// Named rather than anonymous so the hop carries no synthetic reference
    /// to anything enclosing (SpotBugs `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class Deliver<T> implements Runnable {

        private final EdtResult<T> target;
        private final T value;
        private final Throwable failed;

        Deliver(EdtResult<T> target, T value, Throwable failed) {
            this.target = target;
            this.value = value;
            this.failed = failed;
        }

        @Override
        public void run() {
            if (failed != null) {
                target.superError(failed);
            } else {
                target.superComplete(value);
            }
        }
    }

    /// Reaches the one-shot behaviour from the queued runnable, which cannot
    /// use `super` itself.
    void superComplete(T value) {
        super.complete(value);
    }

    void superError(Throwable t) {
        super.error(t);
    }
}
