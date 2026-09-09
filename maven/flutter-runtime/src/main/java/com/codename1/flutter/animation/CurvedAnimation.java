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
 * An {@link Animation} that runs its {@code parent}'s value through a
 * {@link Curve} (and an optional {@code reverseCurve} while the parent is
 * running backward) — Flutter's {@code CurvedAnimation}. Status and listener
 * registration forward to the parent so dependents rebuild on every tick.
 */
public class CurvedAnimation extends Animation<Double> {

    private Animation<Double> parent;
    private Curve curve = Curves.linear;
    private Curve reverseCurve;

    /** Named-parameter setter for {@code parent:}. */
    public void parent(Animation<Double> v) {
        this.parent = v;
    }

    /** Named-parameter setter for {@code curve:}. */
    public void curve(Curve v) {
        this.curve = v == null ? Curves.linear : v;
    }

    /** Named-parameter setter for {@code reverseCurve:}. */
    public void reverseCurve(Curve v) {
        this.reverseCurve = v;
    }

    @Override
    public Double value() {
        double t = parent == null ? 0.0 : parent.value();
        Curve active = curve;
        if (reverseCurve != null && parent != null && parent.status() == AnimationStatus.reverse) {
            active = reverseCurve;
        }
        return active.transform(t);
    }

    @Override
    public AnimationStatus status() {
        return parent == null ? AnimationStatus.dismissed : parent.status();
    }

    @Override
    public void addListener(Funcs.VoidFunc0 listener) {
        if (parent != null) {
            parent.addListener(listener);
        }
    }

    @Override
    public void removeListener(Funcs.VoidFunc0 listener) {
        if (parent != null) {
            parent.removeListener(listener);
        }
    }

    @Override
    public void addStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        if (parent != null) {
            parent.addStatusListener(listener);
        }
    }

    @Override
    public void removeStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        if (parent != null) {
            parent.removeStatusListener(listener);
        }
    }
}
