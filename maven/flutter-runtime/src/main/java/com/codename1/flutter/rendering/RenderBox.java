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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Offset;

/**
 * A render object laid out with the box protocol (a Cartesian size) — Flutter's
 * {@code RenderBox}. The transformations and reply studies read {@link #size()}
 * and map points through {@link #localToGlobal} / {@link #globalToLocal}. This
 * is a structural stub returning neutral geometry; a later rendering milestone
 * will back it with the live Codename One layout.
 */
public class RenderBox extends RenderObject {

    private Size size = Size.ZERO;

    /** The size of this box after layout. */
    public Size size() {
        return size;
    }

    /** Named setter used by the runtime once layout is known. */
    public void size(Size v) {
        this.size = v == null ? Size.ZERO : v;
    }

    /** Whether this box has been through layout and has a valid size. */
    public boolean hasSize() {
        return size != null;
    }

    /**
     * Converts a point from this box's local coordinate space to the global
     * (screen) space, optionally relative to {@code ancestor}. Identity in this
     * milestone.
     */
    public Offset localToGlobal(Offset point, RenderObject ancestor) {
        return point == null ? Offset.zero : point;
    }

    /**
     * Converts a point from global (screen) space to this box's local space,
     * optionally relative to {@code ancestor}. Identity in this milestone.
     */
    public Offset globalToLocal(Offset point, RenderObject ancestor) {
        return point == null ? Offset.zero : point;
    }

    /**
     * The transform mapping this object's coordinate space to {@code ancestor}
     * (a 4x4 matrix in Flutter). Returned opaque for this milestone.
     */
    public Object getTransformTo(RenderObject ancestor) {
        return null;
    }
}
