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

/// Locates body joints, for rep counting, form feedback, or gesture input.
///
/// Counting arm raises from the live camera:
///
/// ```java
/// VisionCameraView<Pose> view =
///         new VisionCameraView<Pose>(new PoseDetector());
/// view.setListener(new VisionPipelineListener<Pose>() {
///     private boolean wasUp;
///
///     public void result(Pose pose, VisionImage source) {
///         Pose.Landmark wrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST);
///         Pose.Landmark shoulder =
///                 pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER);
///         if (wrist == null || shoulder == null
///                 || wrist.getConfidence() < 0.6f) {
///             return;
///         }
///         // Y grows downwards, so the wrist being "above" the shoulder is a
///         // smaller Y.
///         boolean up = wrist.getPosition().getY()
///                 < shoulder.getPosition().getY();
///         if (up && !wasUp) {
///             reps++;
///             repLabel.setText(String.valueOf(reps));
///         }
///         wasUp = up;
///     }
///
///     public void error(Throwable error) {
///         Log.e(error);
///     }
/// });
/// ```
///
/// Backends detect different subsets of the skeleton, so look joints up by
/// name with {@link Pose#getLandmark(String)} and handle {@code null} rather
/// than indexing a fixed array. {@link PoseLandmarks} holds the names.
public final class PoseDetector extends AbstractVisionAnalyzer<Pose> {
    /// Creates an analyzer using the platform default backend and options.
    /// @see VisionOptions
    public PoseDetector() {
        this(null);
    }

    /// Creates a reusable analyzer with explicit backend and result options.
    /// @param options configuration captured by this analyzer; {@code null}
    ///        uses defaults
    public PoseDetector(VisionOptions options) {
        super(VisionFeature.POSE_DETECTION, options);
    }
}
