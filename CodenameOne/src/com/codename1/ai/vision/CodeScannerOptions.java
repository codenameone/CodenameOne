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

import com.codename1.camera.CameraFacing;

/// Configuration for {@link CodeScanner#scan(CodeScannerOptions)}. Every
/// setting has a working default, so an application usually changes only the
/// wording and the accepted symbologies.
///
/// ```java
/// CodeScanner.scan(new CodeScannerOptions()
///         .title("Scan the ticket")
///         .hint("Hold the QR code inside the frame")
///         .formats(BarcodeFormat.QR_CODE, BarcodeFormat.AZTEC))
///     .ready(code -> {
///         if (code == null) {
///             return;              // the user pressed back
///         }
///         admit(code.getValue());
///     })
///     .except(error -> Log.e(error));
/// ```
public final class CodeScannerOptions {
    private String title = "Scan";
    private String hint = "Point the camera at a code";
    private String[] formats = new String[0];
    private CameraFacing facing = CameraFacing.BACK;
    private boolean torchButton = true;
    private boolean strict;
    private boolean strictSet;
    private VisionOptions visionOptions;

    /// Sets the scanner form's title.
    ///
    /// @param value the title, or {@code null} for no title
    /// @return this options object
    public CodeScannerOptions title(String value) {
        title = value;
        return this;
    }

    /// Sets the instruction shown below the preview.
    ///
    /// @param value the instruction, or {@code null} to show none
    /// @return this options object
    public CodeScannerOptions hint(String value) {
        hint = value;
        return this;
    }

    /// Restricts which symbologies are accepted, using the
    /// {@link BarcodeFormat} constants. A code in any other format is ignored
    /// and scanning continues, which is what an application that expects a
    /// specific kind of code wants: a stray product barcode in the frame does
    /// not end the scan. The default accepts every format the backend decodes.
    ///
    /// This filters results rather than configuring the detector, so it does
    /// not make scanning faster.
    ///
    /// @param values accepted format constants; empty or {@code null} accepts
    ///        every format
    /// @return this options object
    public CodeScannerOptions formats(String... values) {
        if (values == null) {
            formats = new String[0];
        } else {
            formats = new String[values.length];
            System.arraycopy(values, 0, formats, 0, values.length);
        }
        return this;
    }

    /// Selects which camera the scanner opens.
    ///
    /// @param value the camera to use, or {@code null} for the back camera
    /// @return this options object
    public CodeScannerOptions facing(CameraFacing value) {
        facing = value == null ? CameraFacing.BACK : value;
        return this;
    }

    /// Whether to offer a torch toggle. The button appears only when the open
    /// camera actually has a flash, so leaving this enabled is safe. The
    /// default is {@code true}.
    ///
    /// @param value whether the toggle may be shown
    /// @return this options object
    public CodeScannerOptions torchButton(boolean value) {
        torchButton = value;
        return this;
    }

    /// Accepts a code only when two frames read it identically. By default
    /// this applies to PDF417 codes alone; {@code true} extends it to every
    /// symbology and {@code false} turns it off for PDF417 too.
    ///
    /// Turn this on for codes printed with little or no error correction,
    /// such as a PDF417 at level 0; see
    /// {@link VisionOptions#strictBarcodes(boolean)}, which this sets on the
    /// scanner's detector, for why and how frames are compared. It also
    /// reaches a {@link BarcodeScanner} built from {@link #getVisionOptions()},
    /// and overrides a {@code strictBarcodes} set on the options passed to
    /// {@link #visionOptions(VisionOptions)}.
    ///
    /// @param value whether two matching reads are required
    /// @return this options object
    public CodeScannerOptions strict(boolean value) {
        strict = value;
        strictSet = true;
        return this;
    }

    /// Passes analyzer options through to the underlying
    /// {@link BarcodeScanner}, which is how an iOS build selects
    /// {@link VisionBackends#mlKitBarcodeScanning()} instead of Apple Vision.
    ///
    /// @param value analyzer configuration, or {@code null} for the defaults
    /// @return this options object
    public CodeScannerOptions visionOptions(VisionOptions value) {
        visionOptions = value;
        return this;
    }

    /// @return the scanner form's title, possibly {@code null}
    public String getTitle() {
        return title;
    }

    /// @return the instruction shown below the preview, possibly {@code null}
    public String getHint() {
        return hint;
    }

    /// @return defensive copy of the accepted formats; empty accepts all
    public String[] getFormats() {
        String[] out = new String[formats.length];
        System.arraycopy(formats, 0, out, 0, formats.length);
        return out;
    }

    /// @return the camera the scanner opens
    public CameraFacing getFacing() {
        return facing;
    }

    /// @return whether a torch toggle may be shown
    public boolean isTorchButton() {
        return torchButton;
    }

    /// @return whether every code must be read identically twice before it is
    ///         accepted (PDF417 is by default either way)
    public boolean isStrict() {
        return strict;
    }

    /// The options a {@link BarcodeScanner} needs to scan the way these options
    /// describe: a copy of the ones passed to
    /// {@link #visionOptions(VisionOptions)} (or the defaults), plus the
    /// accepted {@link #formats(String...)} unless those options name formats
    /// of their own, plus {@link #strict(boolean)} when it was called. So
    /// `new BarcodeScanner(options.getVisionOptions())` in a pipeline of your
    /// own detects exactly what {@link CodeScanner#scan(CodeScannerOptions)}
    /// would. Each call returns a new object.
    ///
    /// @return the analyzer configuration, never {@code null}
    public VisionOptions getVisionOptions() {
        VisionOptions out = visionOptions == null
                ? new VisionOptions() : visionOptions.snapshot();
        if (out.getBarcodeFormats().length == 0) {
            out.barcodeFormats(formats);
        }
        if (strictSet) {
            out.strictBarcodes(strict);
        }
        return out;
    }
}
