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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

/**
 * Shared base for the transition and implicitly-animated widgets that wrap a single
 * {@code child} (FadeTransition, ScaleTransition, AnimatedContainer, ...).
 *
 * <p>It is an {@link AnimatedWidget}, so a subclass that names its driving animation
 * through {@link #listenable(com.codename1.flutter.foundation.Listenable)} is rebuilt on
 * every tick. Subclasses override {@link #build} to wrap the child in the effect they
 * describe — {@code Opacity} for a fade, {@code Transform} for a scale or a rotation — and
 * the default is the child unchanged, which is right for the implicitly-animated widgets
 * that have no Animation of their own.</p>
 *
 * <p>Until now the whole family rendered the child through with no effect at all: a
 * FadeTransition never faded, a ScaleTransition never scaled, and every page transition in
 * the app was a cut.</p>
 */
public abstract class AnimatedChildWidget extends AnimatedWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return getChild();
    }

    /** The animation's current value, or {@code fallback} before it has one. */
    protected static double valueOf(Animation<Double> animation, double fallback) {
        if (animation == null) {
            return fallback;
        }
        Double v = animation.value();
        return v == null ? fallback : v.doubleValue();
    }
}
