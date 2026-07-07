/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.vr;

/// Identifies which eye a `VRRenderer` frame is being rendered for.
public enum VREye {
    /// The left eye in stereo mode. Offset half the interpupillary distance to
    /// the left of the head center.
    LEFT,

    /// The right eye in stereo mode. Offset half the interpupillary distance
    /// to the right of the head center.
    RIGHT,

    /// The single centered viewpoint used in mono mode.
    CENTER;

    /// The sign of this eye's horizontal offset from the head center: `-1`
    /// for `LEFT`, `1` for `RIGHT`, `0` for `CENTER`.
    public int offsetSign() {
        if (this == LEFT) {
            return -1;
        }
        if (this == RIGHT) {
            return 1;
        }
        return 0;
    }
}
