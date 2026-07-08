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

/// An anchor tracking a recognized `ARReferenceImage` in the real world. The
/// anchor pose is centered on the physical image with local X/Z spanning the
/// image surface. Delivered through the session's `ARAnchorListener`; use
/// `instanceof` to distinguish image anchors from plain anchors.
public class ARImageAnchor extends ARAnchor {
    private final String referenceImageName;
    private final float estimatedPhysicalWidth;

    /// Creates an image anchor. Intended for platform implementations and
    /// tests.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identifier of this anchor
    ///
    /// - `pose`: the initial world pose, centered on the physical image
    ///
    /// - `referenceImageName`: the `ARReferenceImage#getName()` of the
    ///   recognized image
    ///
    /// - `estimatedPhysicalWidth`: the platform's estimate of the physical
    ///   image width in meters
    public ARImageAnchor(String id, ARPose pose, String referenceImageName,
                         float estimatedPhysicalWidth) {
        super(id, pose);
        this.referenceImageName = referenceImageName;
        this.estimatedPhysicalWidth = estimatedPhysicalWidth;
    }

    /// The name of the recognized reference image.
    public String getReferenceImageName() {
        return referenceImageName;
    }

    /// The platform's estimate of the physical image width in meters. May
    /// differ slightly from the registered
    /// `ARReferenceImage#getPhysicalWidthMeters()`.
    public float getEstimatedPhysicalWidth() {
        return estimatedPhysicalWidth;
    }
}
