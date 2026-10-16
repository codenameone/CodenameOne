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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Offset;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.vectormath.Matrix4;

/**
 * The 4x4-matrix controller shared with an {@link InteractiveViewer} — Flutter's
 * {@code TransformationController} (a {@code ValueNotifier<Matrix4>}). The
 * transformations demo animates {@link #value()} and maps viewport points to the
 * child's coordinate space with {@link #toScene(Offset)}.
 */
public class TransformationController extends ValueNotifier<Matrix4> {

    public TransformationController() {
        super(Matrix4.identity());
    }

    public TransformationController(Matrix4 value) {
        super(value == null ? Matrix4.identity() : value);
    }

    /**
     * Maps a point from the viewport (widget) coordinate space to the child's
     * coordinate space. This pass returns the point unchanged (identity mapping)
     * until the live pan/zoom transform is applied.
     */
    public Offset toScene(Offset viewportPoint) {
        return viewportPoint;
    }
}
