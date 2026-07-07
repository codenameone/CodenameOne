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
package com.codename1.ar;

/// Named face regions whose poses can be queried from an `ARFaceAnchor`.
/// Platform availability differs: ARCore natively supplies the nose tip and
/// forehead regions, while eye poses are an ARKit capability. Query
/// `ARFaceAnchor#getRegionPose(ARFaceRegion)` and handle a null result.
public enum ARFaceRegion {
    /// The tip of the nose.
    NOSE_TIP,

    /// The left side of the forehead (the person's left).
    FOREHEAD_LEFT,

    /// The right side of the forehead (the person's right).
    FOREHEAD_RIGHT,

    /// The center of the left eye (the person's left). May be unavailable on
    /// some platforms.
    LEFT_EYE,

    /// The center of the right eye (the person's right). May be unavailable on
    /// some platforms.
    RIGHT_EYE
}
