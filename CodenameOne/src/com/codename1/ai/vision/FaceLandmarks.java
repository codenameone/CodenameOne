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

/// The keys {@link Face#getLandmarks()} uses, as constants instead of
/// literals. Every backend normalizes its own landmark identifiers onto these
/// names; a landmark the backend could not locate is absent from the map
/// rather than present with a meaningless position, so read one through
/// {@link Face#getLandmark(String)} and check for {@code null}.
///
/// ```java
/// detector.process(VisionImage.fromCameraFrame(frame)).ready(faces -> {
///     for (Face face : faces) {
///         VisionPoint left = face.getLandmark(FaceLandmarks.LEFT_EYE);
///         VisionPoint right = face.getLandmark(FaceLandmarks.RIGHT_EYE);
///         if (left != null && right != null) {
///             placeGlasses(left.toPoint(preview), right.toPoint(preview));
///         }
///     }
/// });
/// ```
public final class FaceLandmarks {
    /// Center of the subject's left eye, which appears on the right of the image.
    public static final String LEFT_EYE = "leftEye";
    /// Center of the subject's right eye, which appears on the left of the image.
    public static final String RIGHT_EYE = "rightEye";
    /// Base of the nose, between the nostrils.
    public static final String NOSE_BASE = "noseBase";
    /// Left corner of the mouth.
    public static final String MOUTH_LEFT = "mouthLeft";
    /// Right corner of the mouth.
    public static final String MOUTH_RIGHT = "mouthRight";

    private FaceLandmarks() {
    }
}
