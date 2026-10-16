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
package com.codename1.flutter.cupertino;

/**
 * The system-provided {@link MouseCursor} constants, mirroring Flutter's
 * {@code SystemMouseCursors}. Opaque markers in this runtime (see
 * {@link MouseCursor}).
 */
public final class SystemMouseCursors {

    private SystemMouseCursors() {
    }

    public static final MouseCursor none = new MouseCursor("none");
    public static final MouseCursor basic = new MouseCursor("basic");
    public static final MouseCursor click = new MouseCursor("click");
    public static final MouseCursor forbidden = new MouseCursor("forbidden");
    public static final MouseCursor wait = new MouseCursor("wait");
    public static final MouseCursor progress = new MouseCursor("progress");
    public static final MouseCursor text = new MouseCursor("text");
    public static final MouseCursor grab = new MouseCursor("grab");
    public static final MouseCursor grabbing = new MouseCursor("grabbing");
    public static final MouseCursor move = new MouseCursor("move");
    public static final MouseCursor resizeUpDown = new MouseCursor("resizeUpDown");
    public static final MouseCursor resizeLeftRight = new MouseCursor("resizeLeftRight");
    public static final MouseCursor resizeColumn = new MouseCursor("resizeColumn");
    public static final MouseCursor resizeRow = new MouseCursor("resizeRow");
    public static final MouseCursor copy = new MouseCursor("copy");
    public static final MouseCursor alias = new MouseCursor("alias");
    public static final MouseCursor cell = new MouseCursor("cell");
    public static final MouseCursor precise = new MouseCursor("precise");
}
