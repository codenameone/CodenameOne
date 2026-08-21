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

/// Reads the text in an image. The default recognizes the Latin script; select
/// {@link VisionOptions#textScript(TextScript)} to read Chinese, Devanagari,
/// Japanese or Korean text.
///
/// Reading a photographed receipt or label:
///
/// ```java
/// TextRecognizer recognizer = new TextRecognizer();
/// recognizer.process(VisionImage.fromFile(photoPath)).ready(result -> {
///     textArea.setText(result.getText());
///     recognizer.close();
/// }).except(error -> {
///     Log.e(error);
///     recognizer.close();
/// });
/// ```
///
/// Reading text out of the live camera and stopping at the first line that
/// looks like the value you want:
///
/// ```java
/// VisionCameraView<TextRecognitionResult> view =
///         new VisionCameraView<TextRecognitionResult>(new TextRecognizer());
/// view.setListener(new VisionPipelineListener<TextRecognitionResult>() {
///     public void result(TextRecognitionResult text, VisionImage source) {
///         for (TextRecognitionResult.TextBlock block : text.getBlocks()) {
///             if (block.getText().startsWith("SN-")) {
///                 accept(block.getText());
///                 return;
///             }
///         }
///     }
///     public void error(Throwable error) {
///         Log.e(error);
///     }
/// });
/// ```
///
/// {@link TextRecognitionResult#getText()} is the whole page in reading order;
/// {@link TextRecognitionResult#getBlocks()} adds per-region text, confidence,
/// and normalized bounds. Recognize one script per analyzer -- create a second
/// analyzer when a screen needs two.
public final class TextRecognizer extends AbstractVisionAnalyzer<TextRecognitionResult> {
    /// Creates an analyzer using the platform default backend and options,
    /// reading the Latin script.
    /// @see VisionOptions
    public TextRecognizer() {
        this(null);
    }

    /// Creates a reusable analyzer with explicit backend and result options.
    /// The writing system comes from
    /// {@link VisionOptions#textScript(TextScript)}.
    ///
    /// @param options configuration captured by this analyzer; {@code null}
    ///        uses defaults
    public TextRecognizer(VisionOptions options) {
        super(VisionFeature.TEXT_RECOGNITION, options);
    }
}
