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
package com.codename1.impl.android.ai;

import android.graphics.Point;

import com.codename1.ai.vision.Barcode;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionPoint;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ML Kit barcode scanning; retained only for {@code BarcodeScanner} users. */
final class AndroidBarcodeScanningAdapter extends AndroidVisionAdapter {
    // One client per format mask, built on first use and kept until close().
    // A scanner limited to PDF417 and a BarcodeScanner.detect() with default
    // options share this adapter, so closing one client to build the other
    // would fail the scan still running on it.
    private final Map<Integer, BarcodeScanner> clients = new HashMap<Integer, BarcodeScanner>();

    private synchronized BarcodeScanner client(VisionOptions options) {
        int formats = mlKitFormats(options == null ? new String[0] : options.getBarcodeFormats());
        Integer key = Integer.valueOf(formats);
        BarcodeScanner client = clients.get(key);
        if (client == null) {
            if (formats == 0) {
                client = BarcodeScanning.getClient();
            } else {
                // A detector told the symbologies it is looking for skips the
                // others on every frame; for a dense PDF417 that is the
                // difference between reading and not.
                client = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(formats).build());
            }
            clients.put(key, client);
        }
        return client;
    }

    /// The ML Kit bit mask for the given format names; 0 (every format) when
    /// none is named or none is one ML Kit knows.
    static int mlKitFormats(String[] names) {
        int mask = 0;
        for (int i = 0; i < names.length; i++) {
            mask |= mlKitFormat(names[i]);
        }
        return mask;
    }

    private static int mlKitFormat(String name) {
        if ("AZTEC".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC;
        }
        if ("CODABAR".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR;
        }
        if ("CODE_39".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39;
        }
        if ("CODE_93".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93;
        }
        if ("CODE_128".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128;
        }
        if ("DATA_MATRIX".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX;
        }
        if ("EAN_8".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8;
        }
        if ("EAN_13".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13;
        }
        if ("ITF".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF;
        }
        if ("PDF417".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417;
        }
        if ("QR_CODE".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE;
        }
        if ("UPC_A".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A;
        }
        if ("UPC_E".equals(name)) {
            return com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E;
        }
        return 0;
    }
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<Barcode[]> out = (AsyncResource<Barcode[]>) resource;
        final int maximumResults = options.getMaximumResults();
        client(options).process(input).addOnSuccessListener(
                new OnSuccessListener<List<com.google.mlkit.vision.barcode.common.Barcode>>() {
            public void onSuccess(
                    List<com.google.mlkit.vision.barcode.common.Barcode> values) {
                int count = maximumResults == 0 ? values.size()
                        : Math.min(maximumResults, values.size());
                Barcode[] result = new Barcode[count];
                for (int i = 0; i < result.length; i++) {
                    com.google.mlkit.vision.barcode.common.Barcode value =
                            values.get(i);
                    Point[] points = value.getCornerPoints();
                    VisionPoint[] corners = points == null
                            ? new VisionPoint[0] : new VisionPoint[points.length];
                    for (int p = 0; p < corners.length; p++) {
                        corners[p] = new VisionPoint(
                                points[p].x / (float) imageWidth,
                                points[p].y / (float) imageHeight);
                    }
                    result[i] = new Barcode(value.getRawValue(),
                            barcodeFormat(value.getFormat()),
                            value.getRawBytes(),
                            normalized(value.getBoundingBox(),
                                    imageWidth, imageHeight),
                            corners, METADATA);
                }
                complete(out, result);
            }
        }).addOnFailureListener(failure(out));
    }

    @Override
    synchronized void close() {
        for (BarcodeScanner c : clients.values()) {
            c.close();
        }
        clients.clear();
    }

    private static String barcodeFormat(int format) {
        switch (format) {
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC:
                return "AZTEC";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR:
                return "CODABAR";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39:
                return "CODE_39";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93:
                return "CODE_93";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128:
                return "CODE_128";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX:
                return "DATA_MATRIX";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8:
                return "EAN_8";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13:
                return "EAN_13";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF:
                return "ITF";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417:
                return "PDF417";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE:
                return "QR_CODE";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A:
                return "UPC_A";
            case com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E:
                return "UPC_E";
            default:
                return "UNKNOWN";
        }
    }
}
