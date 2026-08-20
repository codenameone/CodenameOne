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

/// The normalized symbology names {@link Barcode#getFormat()} reports, as
/// constants instead of literals. Every backend maps its own vendor identifier
/// onto one of these before the result reaches application code, and reports
/// {@link #UNKNOWN} for a symbology outside this set.
///
/// ```java
/// scanner.process(VisionImage.encoded(jpeg)).ready(codes -> {
///     for (Barcode code : codes) {
///         if (BarcodeFormat.matches(code, BarcodeFormat.QR_CODE)) {
///             Log.p("QR: " + code.getValue());
///         } else if (BarcodeFormat.matches(code, BarcodeFormat.EAN_13,
///                 BarcodeFormat.UPC_A)) {
///             lookUpProduct(code.getValue());
///         }
///     }
/// });
/// ```
public final class BarcodeFormat {
    /// Aztec two-dimensional code, common on transit tickets.
    public static final String AZTEC = "AZTEC";
    /// Codabar linear code, used by libraries and blood banks.
    public static final String CODABAR = "CODABAR";
    /// Code 39 linear code.
    public static final String CODE_39 = "CODE_39";
    /// Code 93 linear code.
    public static final String CODE_93 = "CODE_93";
    /// Code 128 linear code, the usual choice for shipping labels.
    public static final String CODE_128 = "CODE_128";
    /// Data Matrix two-dimensional code, common on small parts.
    public static final String DATA_MATRIX = "DATA_MATRIX";
    /// EAN-8 retail product code.
    public static final String EAN_8 = "EAN_8";
    /// EAN-13 retail product code.
    public static final String EAN_13 = "EAN_13";
    /// Interleaved 2 of 5 linear code.
    public static final String ITF = "ITF";
    /// PDF417 stacked code, used by driving licences and boarding passes.
    public static final String PDF417 = "PDF417";
    /// QR code.
    public static final String QR_CODE = "QR_CODE";
    /// UPC-A retail product code.
    public static final String UPC_A = "UPC_A";
    /// UPC-E retail product code.
    public static final String UPC_E = "UPC_E";
    /// Reported when the backend decoded a code whose symbology has no
    /// portable name here.
    public static final String UNKNOWN = "UNKNOWN";

    private BarcodeFormat() {
    }

    /// Tests a decoded barcode against a set of accepted formats. An empty or
    /// {@code null} format list accepts every symbology, which is what a
    /// general-purpose scanner wants.
    ///
    /// @param barcode observation to test, or {@code null}
    /// @param formats accepted format constants
    /// @return {@code true} when the barcode's format is one of {@code formats}
    public static boolean matches(Barcode barcode, String... formats) {
        if (barcode == null) {
            return false;
        }
        if (formats == null || formats.length == 0) {
            return true;
        }
        String format = barcode.getFormat();
        for (String accepted : formats) {
            if (accepted != null && accepted.equals(format)) {
                return true;
            }
        }
        return false;
    }
}
