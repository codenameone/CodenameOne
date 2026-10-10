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

import com.codename1.flutter.EdgeInsets;

import dart.core.Duration;

/**
 * Animates changes to its padding over a {@link Duration} — Flutter's
 * {@code AnimatedPadding}. This pass hosts the child; the padding is applied
 * without the tween (final value).
 */
public class AnimatedPadding extends AnimatedChildWidget {

    private EdgeInsets padding;
    private Duration duration;
    private Curve curve;

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    public EdgeInsets getPadding() {
        return padding;
    }
}
