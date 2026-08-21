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

/// Finds a page in a photo and returns it flattened, with the perspective
/// corrected.
///
/// ```java
/// DocumentScanner scanner = new DocumentScanner();
/// if (!scanner.isSupported()) {
///     // Android's document scanner is an interactive Google flow rather than
///     // an analyzer, so this reports unsupported there.
///     scanner.close();
///     return;
/// }
/// scanner.process(VisionImage.fromFile(photoPath)).ready(result -> {
///     for (int i = 0; i < result.getPageCount(); i++) {
///         pages.add(EncodedImage.create(result.getPage(i)));
///     }
///     scanner.close();
/// }).except(error -> {
///     Log.e(error);
///     scanner.close();
/// });
/// ```
///
/// This is a still-image analyzer: capture the photo however you like -- the
/// {@link com.codename1.capture.Capture} API, a
/// {@link com.codename1.camera.CameraSession}, or the gallery -- and hand the
/// bytes over.
public final class DocumentScanner extends AbstractVisionAnalyzer<DocumentScanResult> {
    /// Creates an analyzer using the platform default backend and options.
    /// @see VisionOptions
    public DocumentScanner() {
        this(null);
    }

    /// Creates a reusable analyzer with explicit backend and result options.
    /// @param options configuration captured by this analyzer; {@code null}
    ///        uses defaults
    public DocumentScanner(VisionOptions options) {
        super(VisionFeature.DOCUMENT_SCANNING, options);
    }
}
