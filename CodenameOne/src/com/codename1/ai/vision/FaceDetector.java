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

/// Finds faces, their bounding boxes, their head angles, and -- where the
/// backend supports it -- whether they are smiling.
///
/// Counting faces in the live camera, which is the usual shape for a selfie
/// screen or a "hold still" capture:
///
/// ```java
/// VisionCameraView<Face[]> view =
///         new VisionCameraView<Face[]>(new FaceDetector());
/// view.setFacing(CameraFacing.FRONT);
/// view.setListener(new VisionPipelineListener<Face[]>() {
///     public void result(Face[] faces, VisionImage source) {
///         shutter.setEnabled(faces.length == 1);
///         status.setText(faces.length == 1
///                 ? "Looking good" : "Fit exactly one face in the frame");
///     }
///     public void error(Throwable error) {
///         Log.e(error);
///     }
/// });
///
/// Form form = new Form("Selfie", new BorderLayout());
/// form.add(BorderLayout.CENTER, view);
/// form.add(BorderLayout.SOUTH, BoxLayout.encloseY(status, shutter));
/// form.show();
/// ```
///
/// Or over a still image, to crop to the face:
///
/// ```java
/// FaceDetector detector = new FaceDetector();
/// detector.process(VisionImage.fromImage(photo)).ready(faces -> {
///     if (faces.length > 0) {
///         Rectangle box = faces[0].getBounds()
///                 .toBounds(0, 0, photo.getWidth(), photo.getHeight());
///         avatar.setIcon(photo.subImage(box.getX(), box.getY(),
///                 box.getWidth(), box.getHeight(), true));
///     }
///     detector.close();
/// }).except(error -> {
///     Log.e(error);
///     detector.close();
/// });
/// ```
///
/// Read individual landmarks with {@link Face#getLandmark(String)} and the
/// {@link FaceLandmarks} constants. Not every backend fills in every field:
/// {@link Face#getSmilingProbability()} and {@link Face#getTrackingId()} are
/// negative when unavailable.
public final class FaceDetector extends AbstractVisionAnalyzer<Face[]> {
    /// Creates an analyzer using the platform default backend and options.
    /// @see VisionOptions
    public FaceDetector() {
        this(null);
    }

    /// Creates a reusable analyzer with explicit backend and result options.
    /// @param options configuration captured by this analyzer; {@code null}
    ///        uses defaults
    public FaceDetector(VisionOptions options) {
        super(VisionFeature.FACE_DETECTION, options);
    }
}
