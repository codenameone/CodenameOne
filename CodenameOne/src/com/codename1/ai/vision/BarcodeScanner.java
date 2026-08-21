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

/// Decodes barcodes and QR codes out of a still image or a camera frame.
///
/// Scanning a picture the user already has:
///
/// ```java
/// BarcodeScanner scanner = new BarcodeScanner();
/// if (!scanner.isSupported()) {
///     ToastBar.showErrorMessage("This device cannot decode barcodes");
///     scanner.close();
///     return;
/// }
/// scanner.process(VisionImage.fromFile(photoPath)).ready(codes -> {
///     for (Barcode code : codes) {
///         Log.p(code.getFormat() + ": " + code.getValue());
///     }
///     scanner.close();
/// }).except(error -> {
///     Log.e(error);
///     scanner.close();
/// });
/// ```
///
/// For scanning with the camera there is usually no reason to wire this up
/// yourself. {@link CodeScanner#scan()} is a whole scanner screen in one call,
/// and {@link VisionCameraView} embeds the live preview in a form of your own:
///
/// ```java
/// VisionCameraView<Barcode[]> view =
///         new VisionCameraView<Barcode[]>(new BarcodeScanner());
/// view.setListener(new VisionPipelineListener<Barcode[]>() {
///     public void result(Barcode[] codes, VisionImage source) {
///         if (codes.length > 0) {
///             found(codes[0]);
///         }
///     }
///     public void error(Throwable error) {
///         Log.e(error);
///     }
/// });
/// ```
///
/// Create one analyzer and reuse it for a sequence of images; it keeps the
/// native detector alive between calls. Close it when you are finished.
public final class BarcodeScanner extends AbstractVisionAnalyzer<Barcode[]> {
    /// Creates an analyzer using the platform default backend and options.
    /// @see VisionOptions
    public BarcodeScanner() {
        this(null);
    }

    /// Creates a reusable analyzer with explicit backend and result options.
    /// @param options configuration captured by this analyzer; {@code null}
    ///        uses defaults
    public BarcodeScanner(VisionOptions options) {
        super(VisionFeature.BARCODE_SCANNING, options);
    }
}
