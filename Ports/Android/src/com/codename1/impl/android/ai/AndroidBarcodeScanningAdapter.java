/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import android.graphics.Point;

import com.codename1.ai.vision.Barcode;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionPoint;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

/** ML Kit barcode scanning; retained only for {@code BarcodeScanner} users. */
final class AndroidBarcodeScanningAdapter extends AndroidVisionAdapter {
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<Barcode[]> out = (AsyncResource<Barcode[]>) resource;
        final BarcodeScanner client = BarcodeScanning.getClient();
        client.process(input).addOnSuccessListener(
                new OnSuccessListener<List<com.google.mlkit.vision.barcode.common.Barcode>>() {
            public void onSuccess(
                    List<com.google.mlkit.vision.barcode.common.Barcode> values) {
                Barcode[] result = new Barcode[values.size()];
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
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
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
