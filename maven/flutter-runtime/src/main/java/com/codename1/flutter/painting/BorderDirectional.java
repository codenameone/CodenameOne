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
package com.codename1.flutter.painting;

/**
 * A box border whose sides are resolved against the ambient text direction
 * ({@code start}/{@code end} rather than {@code left}/{@code right}) — Flutter's
 * {@code BorderDirectional}. The settings demo draws a leading rule with a
 * {@code start} side. The sides are held as {@code Object} because a
 * {@code BorderSide} is supplied by the widget layer; this pass retains them for
 * the box decoration to paint.
 */
public class BorderDirectional {

    private Object top;
    private Object bottom;
    private Object start;
    private Object end;

    public void top(Object v) {
        this.top = v;
    }

    public void bottom(Object v) {
        this.bottom = v;
    }

    public void start(Object v) {
        this.start = v;
    }

    public void end(Object v) {
        this.end = v;
    }

    public Object getTop() {
        return top;
    }

    public Object getBottom() {
        return bottom;
    }

    public Object getStart() {
        return start;
    }

    public Object getEnd() {
        return end;
    }
}
