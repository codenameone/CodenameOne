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
 * Runs a parent animation in reverse — Flutter's {@code ReverseAnimation}. Its
 * {@link #value()} is {@code 1 - parent.value()} and its status is the parent's
 * status with {@code forward}/{@code reverse} swapped. The backdrop title uses
 * it to fade its front-layer title out as the parent reveal animates in.
 */
public class ReverseAnimation extends Animation<Double> {

    private final Animation<Double> parent;

    /**
     * Forwards the parent's changes to this animation's own listeners. Listeners were
     * stored on the wrapper and nothing ever notified them, so a fade driven by a
     * ReverseAnimation stayed at the value it first rendered with.
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
            notifyStatusListeners(reverse(s));
        }
    };

    public ReverseAnimation(Animation<Double> parent) {
        this.parent = parent;
    }

    @Override
    protected void didStartListening() {
        if (parent != null) {
            parent.addListener(valueRelay);
            parent.addStatusListener(statusRelay);
        }
    }

    @Override
    protected void didStopListening() {
        if (parent != null) {
            parent.removeListener(valueRelay);
            parent.removeStatusListener(statusRelay);
        }
    }

    public Animation<Double> getParent() {
        return parent;
    }

    @Override
    public Double value() {
        double v = parent == null || parent.value() == null ? 0.0 : parent.value();
        return 1.0 - v;
    }

    @Override
    public AnimationStatus status() {
        if (parent == null) {
            return AnimationStatus.dismissed;
        }
        return reverse(parent.status());
    }

    /** The parent's status as seen running backward. */
    private static AnimationStatus reverse(AnimationStatus s) {
        switch (s) {
            case forward:
                return AnimationStatus.reverse;
            case reverse:
                return AnimationStatus.forward;
            case completed:
                return AnimationStatus.dismissed;
            case dismissed:
                return AnimationStatus.completed;
            default:
                return s;
        }
    }
}
