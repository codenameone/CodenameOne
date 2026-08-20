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

/// The joint names {@link Pose.Landmark#getName()} reports, as constants
/// instead of literals. Backends detect different subsets of this list: ML Kit
/// reports the hand and foot detail, Apple Vision reports {@link #NECK} and
/// {@link #ROOT} that ML Kit does not, and a joint the backend did not report
/// is simply absent. Read one through {@link Pose#getLandmark(String)} and
/// check for {@code null} rather than assuming a fixed skeleton.
///
/// ```java
/// detector.process(VisionImage.fromCameraFrame(frame)).ready(pose -> {
///     Pose.Landmark wrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST);
///     Pose.Landmark shoulder = pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER);
///     if (wrist != null && shoulder != null
///             && wrist.getConfidence() > 0.6f
///             && wrist.getPosition().getY() < shoulder.getPosition().getY()) {
///         Log.p("hand raised");
///     }
/// });
/// ```
public final class PoseLandmarks {
    /// Tip of the nose.
    public static final String NOSE = "nose";
    /// Inner corner of the left eye.
    public static final String LEFT_EYE_INNER = "leftEyeInner";
    /// Center of the left eye.
    public static final String LEFT_EYE = "leftEye";
    /// Outer corner of the left eye.
    public static final String LEFT_EYE_OUTER = "leftEyeOuter";
    /// Inner corner of the right eye.
    public static final String RIGHT_EYE_INNER = "rightEyeInner";
    /// Center of the right eye.
    public static final String RIGHT_EYE = "rightEye";
    /// Outer corner of the right eye.
    public static final String RIGHT_EYE_OUTER = "rightEyeOuter";
    /// Left ear.
    public static final String LEFT_EAR = "leftEar";
    /// Right ear.
    public static final String RIGHT_EAR = "rightEar";
    /// Left corner of the mouth.
    public static final String LEFT_MOUTH = "leftMouth";
    /// Right corner of the mouth.
    public static final String RIGHT_MOUTH = "rightMouth";
    /// Base of the neck. Reported by Apple Vision; ML Kit omits it.
    public static final String NECK = "neck";
    /// Left shoulder.
    public static final String LEFT_SHOULDER = "leftShoulder";
    /// Right shoulder.
    public static final String RIGHT_SHOULDER = "rightShoulder";
    /// Left elbow.
    public static final String LEFT_ELBOW = "leftElbow";
    /// Right elbow.
    public static final String RIGHT_ELBOW = "rightElbow";
    /// Left wrist.
    public static final String LEFT_WRIST = "leftWrist";
    /// Right wrist.
    public static final String RIGHT_WRIST = "rightWrist";
    /// Left little finger. Reported by ML Kit; Apple Vision omits it.
    public static final String LEFT_PINKY = "leftPinky";
    /// Right little finger. Reported by ML Kit; Apple Vision omits it.
    public static final String RIGHT_PINKY = "rightPinky";
    /// Left index finger. Reported by ML Kit; Apple Vision omits it.
    public static final String LEFT_INDEX = "leftIndex";
    /// Right index finger. Reported by ML Kit; Apple Vision omits it.
    public static final String RIGHT_INDEX = "rightIndex";
    /// Left thumb. Reported by ML Kit; Apple Vision omits it.
    public static final String LEFT_THUMB = "leftThumb";
    /// Right thumb. Reported by ML Kit; Apple Vision omits it.
    public static final String RIGHT_THUMB = "rightThumb";
    /// Left hip.
    public static final String LEFT_HIP = "leftHip";
    /// Right hip.
    public static final String RIGHT_HIP = "rightHip";
    /// Midpoint between the hips. Reported by Apple Vision; ML Kit omits it.
    public static final String ROOT = "root";
    /// Left knee.
    public static final String LEFT_KNEE = "leftKnee";
    /// Right knee.
    public static final String RIGHT_KNEE = "rightKnee";
    /// Left ankle.
    public static final String LEFT_ANKLE = "leftAnkle";
    /// Right ankle.
    public static final String RIGHT_ANKLE = "rightAnkle";
    /// Left heel. Reported by ML Kit; Apple Vision omits it.
    public static final String LEFT_HEEL = "leftHeel";
    /// Right heel. Reported by ML Kit; Apple Vision omits it.
    public static final String RIGHT_HEEL = "rightHeel";
    /// Ball of the left foot. Reported by ML Kit; Apple Vision omits it.
    public static final String LEFT_FOOT_INDEX = "leftFootIndex";
    /// Ball of the right foot. Reported by ML Kit; Apple Vision omits it.
    public static final String RIGHT_FOOT_INDEX = "rightFootIndex";
    /// Reported when a backend supplied a joint with no portable name here.
    public static final String UNKNOWN = "unknown";

    private PoseLandmarks() {
    }
}
