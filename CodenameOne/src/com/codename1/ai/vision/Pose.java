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
package com.codename1.ai.vision;

/// Portable body-pose result. Landmark coordinates use the normalized
/// top-left-origin image space defined by {@link VisionPoint}; confidence is
/// in the range 0..1. Landmark names are backend-neutral where the native
/// backend exposes a known joint name.
public final class Pose {
    private final Landmark[] landmarks;
    private final VisionMetadata metadata;

    /// Creates a pose without backend metadata.
    /// @param landmarks detected named body landmarks, defensively copied
    public Pose(Landmark[] landmarks) {
        this(landmarks, null);
    }

    /// Creates a pose with backend diagnostics.
    /// @param landmarks detected named body landmarks, defensively copied
    /// @param metadata backend details, or {@code null}
    public Pose(Landmark[] landmarks, VisionMetadata metadata) {
        if (landmarks == null) {
            this.landmarks = new Landmark[0];
        } else {
            this.landmarks = new Landmark[landmarks.length];
            System.arraycopy(landmarks, 0, this.landmarks, 0, landmarks.length);
        }
        this.metadata = metadata;
    }

    /// @return defensive copy of detected body landmarks
    public Landmark[] getLandmarks() {
        Landmark[] out = new Landmark[landmarks.length];
        System.arraycopy(landmarks, 0, out, 0, landmarks.length);
        return out;
    }

    /// @return backend metadata, or {@code null} when manually constructed
    public VisionMetadata getMetadata() {
        return metadata;
    }

    /// One named body joint with normalized position and confidence.
    public static final class Landmark {
        private final String name;
        private final VisionPoint position;
        private final float confidence;

        /// Creates one detected body joint.
        /// @param name joint name
        /// @param position normalized joint position
        /// @param confidence in-frame confidence in 0..1
        public Landmark(String name, VisionPoint position, float confidence) {
            this.name = name;
            this.position = position;
            this.confidence = confidence;
        }

        /// @return portable/native joint name
        public String getName() {
            return name;
        }

        /// @return normalized top-left-origin joint position
        public VisionPoint getPosition() {
            return position;
        }

        /// @return in-frame confidence in the range 0..1
        public float getConfidence() {
            return confidence;
        }
    }
}
