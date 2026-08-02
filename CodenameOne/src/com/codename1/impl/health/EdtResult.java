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

    /// Marshalling `complete` is not enough on its own: a listener attached
    /// *after* the resource has settled is run by
    /// `AsyncResource.ready`/`except` inline, on whichever thread attaches it.
    ///
    /// That is the whole contract leaking back out. The window is small but
    /// entirely reachable -- `openHealthSettings` and `openProviderSetup`
    /// complete before they return, so an off-EDT caller writing the ordinary
    /// `openHealthSettings().onResult(cb)` races the `callSerially` above
    /// between two adjacent statements. Win the race and the callback arrives
    /// on the EDT; lose it and the same line of app code delivers off the EDT.
    /// It is exactly the "may or may not be on the EDT" asymmetry this class
    /// exists to remove, and it fails intermittently, which is worse than
    /// failing always.
    ///
    /// So the guarantee is applied where the callback is *invoked* rather than
    /// where the resource is completed: whatever thread gets here, delivery
    /// hops to the EDT if it is not already on it. Already on the EDT still
    /// runs inline, so the no-runnable-per-link property above is kept.
    @Override
    public com.codename1.util.AsyncResource<T> ready(
            final com.codename1.util.SuccessCallback<T> callback,
            com.codename1.util.EasyThread t) {
        if (t != null) {
            // An explicit thread is the caller overriding the default on
            // purpose; honouring the request beats honouring the default.
            return super.ready(callback, t);
        }
        return super.ready(new OnEdt<T>(callback), null);
    }

    /// `except` is deliberately NOT wrapped the same way.
    ///
    /// A late `except` on an already-failed resource fires synchronously, and
    /// that is relied on to read an error back without waiting -- see
    /// `HealthFallbackTest.errorOf`, which documents the trick and shares it
    /// with `BtTestUtil`. Hopping the error to the EDT breaks every such
    /// reader: the callback has not run yet when the value is read, so the
    /// error reads as null and a failing call looks like a succeeding one.
    ///
    /// So the asymmetry is left in place on purpose rather than overlooked.
    /// Closing it means changing that read pattern everywhere it is used,
    /// which is a wider behavioural change than the delivery bug this fixes
    /// and belongs in its own change.

    /// Named, not anonymous, for the same SpotBugs reason as `Deliver`.
    private static final class OnEdt<X> implements com.codename1.util.SuccessCallback<X> {

        private final com.codename1.util.SuccessCallback<X> delegate;

        OnEdt(com.codename1.util.SuccessCallback<X> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSucess(final X value) {
            if (Display.getInstance().isEdt()) {
                delegate.onSucess(value);
                return;
            }
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    delegate.onSucess(value);
                }
            });
        }
    }
}
