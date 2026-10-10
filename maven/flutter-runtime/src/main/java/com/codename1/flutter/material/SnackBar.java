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
package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.Duration;
import dart.core.UnsupportedError;

/**
 * A brief message shown at the bottom of the screen via
 * {@link ScaffoldMessengerState#showSnackBar}. The content widget is
 * CONSUMED as configuration (a {@code Text} child becomes the message
 * string, like button labels) — a SnackBar is never mounted as an element,
 * so {@link #createElement()} is unsupported.
 */
public class SnackBar extends Widget {

    /** Flutter's default SnackBar duration: 4 seconds. */
    public static final long DEFAULT_DURATION_MS = 4000;

    private Widget content;
    private Duration duration;
    private SnackBarAction action;
    private SnackBarBehavior behavior;
    private com.codename1.flutter.Color backgroundColor;

    public void action(SnackBarAction v) {
        this.action = v;
    }

    public void behavior(SnackBarBehavior v) {
        this.behavior = v;
    }

    public void backgroundColor(com.codename1.flutter.Color v) {
        this.backgroundColor = v;
    }

    public SnackBarAction getAction() {
        return action;
    }

    public void content(Widget v) {
        this.content = v;
    }

    public void duration(Duration v) {
        this.duration = v;
    }

    public Widget getContent() {
        return content;
    }

    public Duration getDuration() {
        return duration;
    }

    /**
     * The effective display time in milliseconds.
     */
    public long durationMillis() {
        return duration == null ? DEFAULT_DURATION_MS : duration.inMilliseconds();
    }

    @Override
    public Element createElement() {
        throw new UnsupportedError("SnackBar is consumed by ScaffoldMessengerState.showSnackBar, not mounted");
    }
}
