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
package com.codename1.flutter.gestures;

/**
 * Flutter's {@code PointerMoveEvent}.
 *
 * <p>Carries {@code delta} as well as {@code position} because Flutter's drag
 * recognizers accumulate the DELTA, not the absolute position, to decide
 * whether the touch has travelled past the slop. A synthesised move with the
 * default zero delta therefore scrolls nothing in Flutter while scrolling
 * normally here, where the binding works from the absolute position -- which is
 * exactly how one scripted drag came to scroll one of the two stacks and not
 * the other.</p>
 */
public class PointerMoveEvent extends PointerEvent {

    private com.codename1.flutter.Offset delta;

    public PointerMoveEvent() {
    }

    /** How far the pointer moved since the previous event. */
    public void delta(com.codename1.flutter.Offset v) {
        this.delta = v;
    }

    /** How far the pointer moved since the previous event; null when unstated. */
    public com.codename1.flutter.Offset getDelta() {
        return delta;
    }
}
