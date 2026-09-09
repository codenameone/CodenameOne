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
package com.codename1.flutter;

/**
 * Direction-relative edge insets ({@code start}/{@code end} instead of
 * {@code left}/{@code right}) — Flutter's {@code EdgeInsetsDirectional}.
 *
 * <p>It extends {@link EdgeInsets} so it remains assignable to the
 * {@code EdgeInsets}-typed padding parameters the widget stubs declare. Under
 * the default left-to-right text direction {@code start} maps to {@code left}
 * and {@code end} to {@code right}; full bidi resolution is deferred to the
 * render layer.</p>
 */
public final class EdgeInsetsDirectional extends EdgeInsets {

    public static final EdgeInsetsDirectional zero = new EdgeInsetsDirectional(0, 0, 0, 0);

    private final double start;
    private final double end;

    private EdgeInsetsDirectional(double start, double top, double end, double bottom) {
        // LTR mapping: start -> left, end -> right.
        super(start, top, end, bottom);
        this.start = start;
        this.end = end;
    }

    public static EdgeInsetsDirectional all(double value) {
        return new EdgeInsetsDirectional(value, value, value, value);
    }

    public static EdgeInsetsDirectional only(double start, double top, double end, double bottom) {
        return new EdgeInsetsDirectional(start, top, end, bottom);
    }

    public static EdgeInsetsDirectional symmetric(double horizontal, double vertical) {
        return new EdgeInsetsDirectional(horizontal, vertical, horizontal, vertical);
    }

    public static EdgeInsetsDirectional fromSTEB(double start, double top, double end, double bottom) {
        return new EdgeInsetsDirectional(start, top, end, bottom);
    }

    public double start() {
        return start;
    }

    public double end() {
        return end;
    }
}
