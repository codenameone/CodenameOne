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
 * Animates its own size along one axis, clipping its {@code child} — Flutter's
 * {@code SizeTransition}. The {@code sizeFactor} animation drives the visible
 * fraction (0..1) and {@code axisAlignment} anchors the reveal. This pass hosts
 * the child at full size; the animated clip is deferred (see
 * {@link AnimatedChildWidget}).
 */
public class SizeTransition extends AnimatedChildWidget {

    private Object axis;
    private Animation<Double> sizeFactor;
    private Double axisAlignment;

    public void axis(Object v) {
        this.axis = v;
    }

    public void sizeFactor(Animation<Double> v) {
        this.sizeFactor = v;
    }

    public void axisAlignment(double v) {
        this.axisAlignment = v;
    }

    public Animation<Double> getSizeFactor() {
        return sizeFactor;
    }
}
