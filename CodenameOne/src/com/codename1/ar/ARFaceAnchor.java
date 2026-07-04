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

/// An anchor tracking a face in a `ARTrackingMode#FACE` session. The anchor
/// pose is centered on the head; named regions and an optional face mesh give
/// finer geometry for effects such as filters or virtual try-on.
///
/// Platform capabilities differ: region poses and the mesh may be null on
/// platforms that do not supply them - always handle the null case. Delivered
/// through the session's `ARAnchorListener`; use `instanceof` to distinguish
/// face anchors from plain anchors.
public class ARFaceAnchor extends ARAnchor {
    private ARPose[] regionPoses;
    private float[] meshVertices;
    private int[] meshTriangles;

    /// Creates a face anchor. Intended for platform implementations and
    /// tests.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identifier of this anchor
    ///
    /// - `pose`: the initial world pose, centered on the head
    public ARFaceAnchor(String id, ARPose pose) {
        super(id, pose);
    }

    /// The pose of a named face region in world space, or null when the
    /// platform does not supply that region.
    ///
    /// #### Parameters
    ///
    /// - `region`: the face region to query
    ///
    /// #### Returns
    ///
    /// the region pose or null
    public ARPose getRegionPose(ARFaceRegion region) {
        if (region == null || regionPoses == null) {
            return null;
        }
        int i = region.ordinal();
        return i < regionPoses.length ? regionPoses[i] : null;
    }

    /// The face mesh vertices as `x, y, z` triples in the anchor's local
    /// frame, or null when the platform supplies no mesh. The array is the
    /// live buffer updated each frame; copy it if you need a stable snapshot.
    public float[] getMeshVertices() {
        return meshVertices;
    }

    /// The face mesh triangle indices (three per triangle into
    /// `#getMeshVertices()`), or null when the platform supplies no mesh.
    public int[] getMeshTriangles() {
        return meshTriangles;
    }

    /// Updates the face geometry from the platform. Called by the session on
    /// the EDT.
    void updateFace(ARPose[] regionPoses, float[] meshVertices, int[] meshTriangles) {
        if (regionPoses != null) {
            this.regionPoses = regionPoses;
        }
        if (meshVertices != null) {
            this.meshVertices = meshVertices;
        }
        if (meshTriangles != null) {
            this.meshTriangles = meshTriangles;
        }
    }
}
