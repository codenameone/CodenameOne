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

package com.codename1.ai.mlkit.barcode;

import com.codename1.ai.LlmException;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// ML Kit Barcode Scanning.
///
/// Decodes barcodes (QR, EAN, UPC, Data Matrix, PDF417, etc.) from images.
/// Bridges to `MLKitBarcodeScanning` on iOS and
/// `com.google.mlkit:barcode-scanning` on Android.
///
public final class BarcodeScanner {
    private BarcodeScanner() { }

    /// True only when the running platform has a native bridge wired up.
    public static boolean isSupported() {
        NativeBarcodeScanner bridge = NativeLookup.create(NativeBarcodeScanner.class);
        return bridge != null && bridge.isSupported();
    }

    public static AsyncResource<String[]> scan(final byte[] imageBytes) {
        final AsyncResource<String[]> out = new AsyncResource<String[]>();
        if (imageBytes == null || imageBytes.length == 0) {
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() { out.complete(new String[0]); }
            });
            return out;
        }
        final NativeBarcodeScanner bridge = NativeLookup.create(NativeBarcodeScanner.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException("BarcodeScanner.scan is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final String[] r = bridge.scan(imageBytes);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(r == null ? new String[0] : r); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("BarcodeScanner.scan failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }
}
