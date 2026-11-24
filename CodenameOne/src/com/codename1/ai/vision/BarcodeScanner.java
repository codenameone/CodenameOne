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

import java.util.Arrays;

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
///
/// A PDF417 is reported only once two frames have read it identically, since
/// one printed with little error correction (level 0, as on many payment
/// slips) misreads into plausible wrong payloads; see
/// {@link VisionOptions#strictBarcodes(boolean)} to change that. This default
/// applies to camera frames ({@link VisionImage#fromCameraFrame}, and every
/// frame a {@link VisionCameraView} or {@link VisionPipeline} analyzes); a still
/// image is reported as decoded.
public final class BarcodeScanner extends AbstractVisionAnalyzer<Barcode[]> {
    /// In strict mode, the codes of the last frame that had any, waiting for a
    /// later frame to confirm them.
    private Barcode[] pending;

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

    @Override
    synchronized Object filter(Object value, VisionImage image) {
        if (!(value instanceof Barcode[])) {
            return value;
        }
        Barcode[] codes = (Barcode[]) value;
        VisionOptions o = options();
        // The PDF417 default is for a camera stream, where the next frame is
        // a moment away. A still image is one read: it is reported as decoded
        // and leaves the pending read alone. An explicit strictBarcodes(true)
        // holds stills too, as documented there.
        boolean stream = image != null && image.isCameraFrame();
        if (!stream && !o.isStrictBarcodes()) {
            return codes;
        }
        int kept = 0;
        int strictCount = 0;
        boolean[] keep = new boolean[codes.length];
        for (int i = 0; i < codes.length; i++) {
            Barcode c = codes[i];
            if (c == null) {
                continue;
            }
            if (!o.isStrictBarcode(c.getFormat())) {
                keep[i] = true;
            } else {
                strictCount++;
                keep[i] = c.getValue() != null && pending != null
                        && confirmedBy(pending, c);
            }
            if (keep[i]) {
                kept++;
            }
        }
        // Only codes that need confirming are remembered, and only a frame
        // that has one replaces them: a QR code decoding alone in between
        // must not push out the PDF417 waiting for its second read. A frame
        // with no such code keeps the pending read for the same reason.
        if (strictCount > 0) {
            Barcode[] next = new Barcode[strictCount];
            int n = 0;
            for (int i = 0; i < codes.length; i++) {
                if (codes[i] != null && o.isStrictBarcode(codes[i].getFormat())) {
                    next[n++] = codes[i];
                }
            }
            pending = next;
        }
        if (kept == codes.length) {
            return codes;
        }
        Barcode[] out = new Barcode[kept];
        int n = 0;
        for (int i = 0; i < codes.length; i++) {
            if (keep[i]) {
                out[n++] = codes[i];
            }
        }
        return out;
    }

    private static boolean confirmedBy(Barcode[] previous, Barcode code) {
        for (Barcode p : previous) {
            if (p != null && sameRead(p, code)) {
                return true;
            }
        }
        return false;
    }

    /// Whether two reads carry the same payload. The raw bytes are compared
    /// when both have them, since two different byte sequences can decode to
    /// one string in a lossy charset.
    static boolean sameRead(Barcode a, Barcode b) {
        if (a.getValue() == null || !a.getValue().equals(b.getValue())
                || !String.valueOf(a.getFormat()).equals(String.valueOf(b.getFormat()))) {
            return false;
        }
        byte[] ra = a.getRawBytes();
        byte[] rb = b.getRawBytes();
        return ra == null || rb == null || Arrays.equals(ra, rb);
    }
}
