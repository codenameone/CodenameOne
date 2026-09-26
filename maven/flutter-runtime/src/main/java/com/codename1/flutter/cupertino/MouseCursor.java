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
 * A pointer cursor kind — Flutter's {@code MouseCursor}. CN1 does not retarget
 * the desktop cursor per widget, so this is an opaque marker carried by
 * {@code MouseRegion(cursor:)} and never acted upon this pass.
 */
public class MouseCursor {

    /**
     * {@code MouseCursor.defer}: defers the cursor decision to the region behind
     * this one. An opaque marker in this runtime.
     */
    public static final MouseCursor defer = new MouseCursor("defer");

    private final String kind;

    public MouseCursor(String kind) {
        this.kind = kind;
    }

    public String kind() {
        return kind;
    }
}
