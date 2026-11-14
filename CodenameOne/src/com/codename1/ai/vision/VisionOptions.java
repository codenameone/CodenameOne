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

/// Common analyzer configuration. An analyzer captures the supplied options
/// when constructed; reuse it for frames needing the same backend, confidence
/// threshold, and result limit.
///
/// ```java
/// ImageLabeler labeler = new ImageLabeler(new VisionOptions()
///         .minimumConfidence(0.6f)   // drop guesses below 60%
///         .maximumResults(5));       // keep the top five labels
/// ```
///
/// The analyzer snapshots these values, so mutating the options object
/// afterwards changes nothing -- construct a second analyzer instead. Not
/// every analyzer reads every setting: {@link #textScript(TextScript)} applies
/// only to {@link TextRecognizer}, and a backend may ignore a limit it has no
/// way to enforce.
public final class VisionOptions {
    private VisionBackend backend = VisionBackends.auto();
    private float minimumConfidence;
    private int maximumResults;
    private TextScript textScript;
    private String[] barcodeFormats = new String[0];

    /// @param value selector, or {@code null} to restore automatic selection
    /// @return this options object
    public VisionOptions backend(VisionBackend value) {
        backend = value == null ? VisionBackends.auto() : value;
        return this;
    }

    /// Selects the writing system {@link TextRecognizer} should read. Analyzers
    /// other than the text recognizer ignore it.
    ///
    /// @param value script selector, or {@code null} for the platform default
    ///        (Latin)
    /// @return this options object
    public VisionOptions textScript(TextScript value) {
        textScript = value;
        return this;
    }

    /// Sets the confidence threshold, clamped to 0..1. NaN has no meaningful
    /// ordering and is rejected instead of being passed to platform detectors.
    /// @param value requested threshold
    /// @return this options object
    /// @throws IllegalArgumentException if {@code value} is NaN
    public VisionOptions minimumConfidence(float value) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(
                    "minimum confidence must be a number");
        }
        minimumConfidence = Math.max(0, Math.min(1, value));
        return this;
    }

    /// Sets a non-negative result limit; zero means backend default/unlimited.
    /// @param value requested limit
    /// @return this options object
    public VisionOptions maximumResults(int value) {
        maximumResults = Math.max(0, value);
        return this;
    }

    /// Limits {@link BarcodeScanner} to the given {@link BarcodeFormat}
    /// constants. The platform detector is configured with only these
    /// symbologies, so it neither spends frame time on formats the application
    /// will never accept nor reports one of them by mistake -- which matters
    /// most for dense codes such as PDF417, where every frame counts.
    /// Analyzers other than the barcode scanner ignore it, as does a backend
    /// with no way to restrict its detector (it still reports every format).
    ///
    /// {@link CodeScannerOptions#formats(String...)} sets this for the
    /// scanner it opens, so a {@link CodeScanner} caller does not need both.
    ///
    /// @param values format constants; empty or {@code null} detects every
    ///        format the backend supports
    /// @return this options object
    public VisionOptions barcodeFormats(String... values) {
        if (values == null) {
            barcodeFormats = new String[0];
        } else {
            barcodeFormats = new String[values.length];
            System.arraycopy(values, 0, barcodeFormats, 0, values.length);
        }
        return this;
    }

    /// @return defensive copy of the barcode formats to detect; empty detects
    ///         every format
    public String[] getBarcodeFormats() {
        String[] out = new String[barcodeFormats.length];
        System.arraycopy(barcodeFormats, 0, out, 0, barcodeFormats.length);
        return out;
    }

    /// @return selected backend, never {@code null}
    public VisionBackend getBackend() {
        return backend;
    }

    /// @return confidence threshold in the range 0..1
    public float getMinimumConfidence() {
        return minimumConfidence;
    }

    /// @return non-negative result limit; zero means backend default/unlimited
    public int getMaximumResults() {
        return maximumResults;
    }

    /// @return the selected writing system, or {@code null} for the platform
    ///         default
    public TextScript getTextScript() {
        return textScript;
    }

    VisionOptions snapshot() {
        return new VisionOptions().backend(backend)
                .minimumConfidence(minimumConfidence)
                .maximumResults(maximumResults)
                .textScript(textScript)
                .barcodeFormats(barcodeFormats);
    }
}
