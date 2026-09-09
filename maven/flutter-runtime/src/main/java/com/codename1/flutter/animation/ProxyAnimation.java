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

    public void parent(Animation<Double> v) {
        if (parent != null) {
            cachedValue = value();
            cachedStatus = status();
        }
        this.parent = v;
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
