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
/// in the range 0..1.
///
/// Common backend-neutral names include `nose`, `leftEye`, `rightEye`,
/// `leftEar`, `rightEar`, and the `left`/`right` forms of `Shoulder`, `Elbow`,
/// `Wrist`, `Hip`, `Knee`, and `Ankle`. A backend can additionally report
/// finer eye, mouth, finger, heel, foot, `neck`, or `root` landmarks using the
/// same lower-camel-case convention. {@link PoseLandmarks} holds those names
/// as constants.
///
/// ```java
/// PoseDetector detector = new PoseDetector();
/// detector.process(VisionImage.fromCameraFrame(frame)).ready(pose -> {
///     Pose.Landmark wrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST);
///     Pose.Landmark shoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER);
///     boolean raised = wrist != null && shoulder != null
///             && wrist.getConfidence() > 0.6f
///             && wrist.getPosition().getY() < shoulder.getPosition().getY();
///     repsLabel.setText(raised ? "up" : "down");
/// });
/// ```
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

    /// Looks up one named joint.
    ///
    /// Backends detect different subsets of the skeleton, so a {@code null}
    /// return means "this backend did not report that joint for this frame",
    /// not "unsupported". Check {@link Landmark#getConfidence()} before
    /// trusting a position.
    ///
    /// @param name one of the {@link PoseLandmarks} constants
    /// @return the detected joint, or {@code null} when it was not reported
    public Landmark getLandmark(String name) {
        if (name == null) {
            return null;
        }
        for (Landmark landmark : landmarks) {
            if (landmark != null && name.equals(landmark.getName())) {
                return landmark;
            }
        }
        return null;
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

        /// Returns the stable lower-camel-case joint name described by
        /// {@link Pose}. Known native constants are normalized rather than
        /// exposed as platform-specific numeric or symbolic identifiers.
        ///
        /// @return backend-neutral joint name, or `unknown` if unmapped
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
