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
/// Vendor-neutral on-device vision APIs for still images and live camera
/// frames: barcodes and QR codes, OCR, faces, image labels, body pose,
/// person segmentation, and document correction.
///
/// <h2>Start at the level you need</h2>
///
/// <p>The package is three layers deep, and most applications only ever touch
/// the first one.</p>
///
/// <p><b>1. A finished screen.</b> {@link com.codename1.ai.vision.CodeScanner}
/// is a complete barcode and QR scanner in one call. It opens the camera,
/// decodes, restores the form you came from, and hands back the code -- or
/// {@code null} if the user backed out. This is the replacement for the old
/// {@code CodeScanner} cn1lib.</p>
///
/// ```java
/// CodeScanner.scan().ready(code -> {
///     if (code != null) {
///         urlField.setText(code.getValue());
///     }
/// });
/// ```
///
/// <p><b>2. A live preview inside your own form.</b>
/// {@link com.codename1.ai.vision.VisionCameraView} is a component that owns
/// the camera, streams frames through the analyzer you give it, and delivers
/// results on the EDT. It works with every analyzer, not just barcodes.</p>
///
/// ```java
/// VisionCameraView<Face[]> view =
///         new VisionCameraView<Face[]>(new FaceDetector());
/// view.setFacing(CameraFacing.FRONT);
/// view.setListener(new VisionPipelineListener<Face[]>() {
///     public void result(Face[] faces, VisionImage source) {
///         status.setText(faces.length + " face(s)");
///     }
///     public void error(Throwable error) {
///         Log.e(error);
///     }
/// });
/// form.add(BorderLayout.CENTER, view);
/// ```
///
/// <p><b>3. One image at a time.</b> Each analyzer --
/// {@link com.codename1.ai.vision.TextRecognizer},
/// {@link com.codename1.ai.vision.BarcodeScanner},
/// {@link com.codename1.ai.vision.FaceDetector},
/// {@link com.codename1.ai.vision.ImageLabeler},
/// {@link com.codename1.ai.vision.PoseDetector},
/// {@link com.codename1.ai.vision.SelfieSegmenter},
/// {@link com.codename1.ai.vision.DocumentScanner} -- takes a
/// {@link com.codename1.ai.vision.VisionImage} and returns a typed result.
/// Create one, reuse it for a sequence, and close it.</p>
///
/// ```java
/// TextRecognizer recognizer = new TextRecognizer();
/// recognizer.process(VisionImage.fromFile(path))
///         .ready(result -> Log.p(result.getText()))
///         .except(error -> Log.e(error));
/// ```
///
/// <h2>Reading the results</h2>
///
/// <p>Bounds and points are normalized to 0..1 rather than pixels, so a result
/// computed on a camera frame can be drawn over an image of any size.
/// {@link com.codename1.ai.vision.VisionRect#toBounds(com.codename1.ui.Component)}
/// and {@link com.codename1.ai.vision.VisionPoint#toPoint(com.codename1.ui.Component)}
/// convert them back. Names that would otherwise be string literals --
/// symbologies, face landmarks, body joints -- are constants on
/// {@link com.codename1.ai.vision.BarcodeFormat},
/// {@link com.codename1.ai.vision.FaceLandmarks} and
/// {@link com.codename1.ai.vision.PoseLandmarks}.</p>
///
/// <h2>Availability</h2>
///
/// <p>Call {@code isSupported()} before offering a feature: availability
/// depends on the target, the linked backend, and the OS version. The
/// automatic backend uses Apple Vision/Core Image on iOS and Mac Catalyst and
/// ML Kit on Android; optional backends are selected with
/// {@link com.codename1.ai.vision.VisionBackends}. In the simulator the
/// results are whatever you scripted under <i>Simulate &gt; Vision</i>, so a
/// scanner screen can be built without a device.</p>
///
/// <p>Each analyzer is a separate build-time feature. Referencing one causes
/// the builder to retain only its platform adapter and native dependency, so
/// a barcode app does not carry the pose and OCR models.</p>
package com.codename1.ai.vision;
