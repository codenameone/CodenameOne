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
import com.codename1.util.AsyncResource;
import com.codename1.util.EasyThread;
import com.codename1.util.SuccessCallback;

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

    /// Completing on the EDT is only half of the guarantee. `AsyncResource`
    /// runs a callback registered against an already-finished resource
    /// immediately, on whichever thread registered it, so the outcome landing
    /// on the EDT does not mean the callback does.
    ///
    /// That is the ordinary case for the operations that resolve before they
    /// return -- the facade's `openHealthSettings` and `openProviderSetup`
    /// complete inside the call -- where the delivery thread came down to
    /// whether the EDT had drained the hop above before the caller got as far
    /// as `onResult`. The same call arrived on the EDT or off it from one run
    /// to the next.
    ///
    /// Wrapping every callback closes that half. A callback already reached on
    /// the EDT sees `isEdt()` and runs inline, so this costs a branch and
    /// never an extra queued runnable. Both arities funnel through the
    /// `EasyThread` overloads, so overriding these two covers `ready`,
    /// `except` and `onResult` alike.
    ///
    /// A caller who names an `EasyThread` is asking for delivery there
    /// specifically, which is the point of that overload, so those are left
    /// alone -- the default is the EDT, not an override of an explicit choice.
    @Override
    public AsyncResource<T> ready(SuccessCallback<T> callback, EasyThread t) {
        return super.ready(t == null ? new OnEdt<T>(callback) : callback, t);
    }

    @Override
    public AsyncResource<T> except(SuccessCallback<Throwable> callback, EasyThread t) {
        return super.except(t == null ? new OnEdt<Throwable>(callback) : callback, t);
    }

    /// Named rather than anonymous so the hop carries no synthetic reference
    /// to anything enclosing (SpotBugs `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class OnEdt<V> implements SuccessCallback<V> {

        private final SuccessCallback<V> delegate;

        OnEdt(SuccessCallback<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSucess(V value) {
            if (Display.getInstance().isEdt()) {
                delegate.onSucess(value);
                return;
            }
            Display.getInstance().callSerially(new Invoke<V>(delegate, value));
        }
    }

    private static final class Invoke<V> implements Runnable {

        private final SuccessCallback<V> delegate;
        private final V value;

        Invoke(SuccessCallback<V> delegate, V value) {
            this.delegate = delegate;
            this.value = value;
        }

        @Override
        public void run() {
            delegate.onSucess(value);
        }
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
