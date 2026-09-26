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
 * Direction-relative corner radii ({@code start}/{@code end} corners) —
 * Flutter's {@code BorderRadiusDirectional}.
 */
public final class BorderRadiusDirectional extends BorderRadiusGeometry {

    public static final BorderRadiusDirectional zero =
            new BorderRadiusDirectional(Radius.zero, Radius.zero, Radius.zero, Radius.zero);

    private final Radius topStart;
    private final Radius topEnd;
    private final Radius bottomStart;
    private final Radius bottomEnd;

    private BorderRadiusDirectional(Radius topStart, Radius topEnd,
                                    Radius bottomStart, Radius bottomEnd) {
        this.topStart = topStart == null ? Radius.zero : topStart;
        this.topEnd = topEnd == null ? Radius.zero : topEnd;
        this.bottomStart = bottomStart == null ? Radius.zero : bottomStart;
        this.bottomEnd = bottomEnd == null ? Radius.zero : bottomEnd;
    }

    public static BorderRadiusDirectional all(Radius radius) {
        return new BorderRadiusDirectional(radius, radius, radius, radius);
    }

    public static BorderRadiusDirectional circular(double radius) {
        return all(Radius.circular(radius));
    }

    public static BorderRadiusDirectional only(Radius topStart, Radius topEnd,
                                               Radius bottomStart, Radius bottomEnd) {
        return new BorderRadiusDirectional(topStart, topEnd, bottomStart, bottomEnd);
    }

    public static BorderRadiusDirectional vertical(Radius top, Radius bottom) {
        return new BorderRadiusDirectional(top, top, bottom, bottom);
    }

    public static BorderRadiusDirectional horizontal(Radius start, Radius end) {
        return new BorderRadiusDirectional(start, end, start, end);
    }

    public Radius topStart() {
        return topStart;
    }

    public Radius topEnd() {
        return topEnd;
    }

    public Radius bottomStart() {
        return bottomStart;
    }

    public Radius bottomEnd() {
        return bottomEnd;
    }
}
