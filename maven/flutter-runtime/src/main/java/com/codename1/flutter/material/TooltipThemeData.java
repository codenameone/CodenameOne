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

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.TextStyle;

import dart.core.Duration;

/**
 * Material {@code TooltipThemeData}: write-once tooltip styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class TooltipThemeData {

    private Double height;
    private EdgeInsets padding;
    private EdgeInsets margin;
    private Double verticalOffset;
    private Boolean preferBelow;
    private Boolean excludeFromSemantics;
    private Object decoration;
    private TextStyle textStyle;
    private Object textAlign;
    private Duration waitDuration;
    private Duration showDuration;
    private Object triggerMode;
    private Boolean enableFeedback;

    public void height(double v) {
        this.height = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void verticalOffset(double v) {
        this.verticalOffset = v;
    }

    public void preferBelow(boolean v) {
        this.preferBelow = v;
    }

    public void excludeFromSemantics(boolean v) {
        this.excludeFromSemantics = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    public void textAlign(Object v) {
        this.textAlign = v;
    }

    public void waitDuration(Duration v) {
        this.waitDuration = v;
    }

    public void showDuration(Duration v) {
        this.showDuration = v;
    }

    public void triggerMode(Object v) {
        this.triggerMode = v;
    }

    public void enableFeedback(boolean v) {
        this.enableFeedback = v;
    }
}
