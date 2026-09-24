/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.animation;

import dart.runtime.Funcs;

/**
 * An {@link Animation} that delegates to a swappable inner animation —
 * Flutter's {@code ProxyAnimation}. When {@link #parent(Animation)} is null the
 * proxy holds the last value/status it saw. Reassigning the parent redirects
 * value and status queries to the new animation.
 */
public class ProxyAnimation extends Animation<Double> {

    private Animation<Double> parent;
    private double cachedValue;
    private AnimationStatus cachedStatus = AnimationStatus.dismissed;

    public ProxyAnimation() {
    }

    public ProxyAnimation(Animation<Double> animation) {
        parent(animation);
    }

    /**
     * Forwards the parent's changes to this proxy's listeners, as Flutter's does. They
     * were stored and never notified, so a widget listening to the proxy froze.
     */
    private final Funcs.VoidFunc0 valueRelay = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            notifyListeners();
        }
    };

    private final Funcs.VoidFunc1<AnimationStatus> statusRelay = new Funcs.VoidFunc1<AnimationStatus>() {
        @Override
        public void call(AnimationStatus s) {
            notifyStatusListeners(s);
        }
    };

    public void parent(Animation<Double> v) {
        if (v == parent) {
            return;
        }
        double oldValue = value();
        AnimationStatus oldStatus = status();
        if (parent != null) {
            cachedValue = oldValue;
            cachedStatus = oldStatus;
            if (hasAnyListeners()) {
                unsubscribe(parent);
            }
        }
        this.parent = v;
        if (parent != null) {
            if (hasAnyListeners()) {
                subscribe(parent);
            }
            // Swapping the parent is itself a change, when it moves the value or status.
            if (value() != oldValue) {
                notifyListeners();
            }
            if (status() != oldStatus) {
                notifyStatusListeners(status());
            }
        }
    }

    @Override
    protected void didStartListening() {
        if (parent != null) {
            subscribe(parent);
        }
    }

    @Override
    protected void didStopListening() {
        if (parent != null) {
            unsubscribe(parent);
        }
    }

    private void subscribe(Animation<Double> p) {
        p.addListener(valueRelay);
        p.addStatusListener(statusRelay);
    }

    private void unsubscribe(Animation<Double> p) {
        p.removeListener(valueRelay);
        p.removeStatusListener(statusRelay);
    }

    public Animation<Double> getParent() {
        return parent;
    }

    @Override
    public Double value() {
        if (parent == null) {
            return cachedValue;
        }
        Double v = parent.value();
        return v == null ? 0.0 : v;
    }

    @Override
    public AnimationStatus status() {
        return parent == null ? cachedStatus : parent.status();
    }
}
