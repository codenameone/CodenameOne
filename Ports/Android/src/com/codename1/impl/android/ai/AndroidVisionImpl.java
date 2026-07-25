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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.codename1.ai.vision.VisionException;
import com.codename1.ai.vision.VisionFeature;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.camera.FrameFormat;
import com.codename1.impl.VisionImpl;
import com.codename1.util.AsyncResource;
import com.google.mlkit.vision.common.InputImage;

/**
 * Android vision dispatcher. Feature implementations live in separate source
 * files so the Android builder can retain one adapter and one ML Kit artifact
 * per analyzer used by the application.
 */
public final class AndroidVisionImpl extends VisionImpl {
    private volatile boolean closed;

    @Override
    public boolean isSupported(VisionFeature feature, String backendId) {
        return !closed
                && ("auto".equals(backendId) || "ml-kit".equals(backendId))
                && adapterClass(feature) != null
                && isClassPresent(adapterClass(feature));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> AsyncResource<T> analyze(VisionFeature feature, String backendId,
                                         VisionImage image, VisionOptions options) {
        AsyncResource<T> out = new AsyncResource<T>();
        if (!isSupported(feature, backendId)) {
            out.error(new VisionException(VisionException.UNSUPPORTED,
                    feature + " is not included in this Android build"));
            return out;
        }
        DecodedInput decoded = decode(image);
        if (decoded == null) {
            out.error(new VisionException(VisionException.INVALID_IMAGE,
                    "Vision input is not valid JPEG, PNG, NV21, or RGBA8888 data"));
            return out;
        }
        try {
            AndroidVisionAdapter adapter = (AndroidVisionAdapter)
                    Class.forName(adapterClass(feature)).newInstance();
            adapter.analyze(decoded.input, decoded.width, decoded.height,
                    options, (AsyncResource) out);
        } catch (Throwable error) {
            out.error(new VisionException(VisionException.BACKEND_ERROR,
                    "Could not start the Android " + feature + " adapter",
                    error));
        }
        return out;
    }

    private static DecodedInput decode(VisionImage image) {
        byte[] encoded = image.getEncodedBytes();
        if (encoded != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    encoded, 0, encoded.length);
            return bitmap == null ? null : new DecodedInput(
                    InputImage.fromBitmap(bitmap, image.getRotationDegrees()),
                    bitmap.getWidth(), bitmap.getHeight());
        }
        byte[] pixels = image.getPixels();
        int width = image.getWidth();
        int height = image.getHeight();
        if (pixels == null || width <= 0 || height <= 0) {
            return null;
        }
        if (image.getFormat() == FrameFormat.NV21) {
            if (pixels.length < width * height * 3 / 2) {
                return null;
            }
            return new DecodedInput(InputImage.fromByteArray(
                    pixels, width, height, image.getRotationDegrees(),
                    InputImage.IMAGE_FORMAT_NV21), width, height);
        }
        if (image.getFormat() == FrameFormat.RGBA8888) {
            if (pixels.length < width * height * 4) {
                return null;
            }
            int[] argb = new int[width * height];
            for (int i = 0, p = 0; i < argb.length; i++, p += 4) {
                argb[i] = ((pixels[p + 3] & 255) << 24)
                        | ((pixels[p] & 255) << 16)
                        | ((pixels[p + 1] & 255) << 8)
                        | (pixels[p + 2] & 255);
            }
            Bitmap bitmap = Bitmap.createBitmap(
                    argb, width, height, Bitmap.Config.ARGB_8888);
            return new DecodedInput(InputImage.fromBitmap(
                    bitmap, image.getRotationDegrees()), width, height);
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                pixels, 0, pixels.length);
        return bitmap == null ? null : new DecodedInput(
                InputImage.fromBitmap(bitmap, image.getRotationDegrees()),
                bitmap.getWidth(), bitmap.getHeight());
    }

    private static final class DecodedInput {
        final InputImage input;
        final int width;
        final int height;

        DecodedInput(InputImage input, int width, int height) {
            this.input = input;
            this.width = width;
            this.height = height;
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String adapterClass(VisionFeature feature) {
        if (feature == null) {
            return null;
        }
        switch (feature) {
            case TEXT_RECOGNITION:
                return "com.codename1.impl.android.ai.AndroidTextRecognitionAdapter";
            case BARCODE_SCANNING:
                return "com.codename1.impl.android.ai.AndroidBarcodeScanningAdapter";
            case FACE_DETECTION:
                return "com.codename1.impl.android.ai.AndroidFaceDetectionAdapter";
            case IMAGE_LABELING:
                return "com.codename1.impl.android.ai.AndroidImageLabelingAdapter";
            case POSE_DETECTION:
                return "com.codename1.impl.android.ai.AndroidPoseDetectionAdapter";
            case SELFIE_SEGMENTATION:
                return "com.codename1.impl.android.ai.AndroidSelfieSegmentationAdapter";
            default:
                // ML Kit's document scanner owns an Activity camera flow; it
                // cannot implement the still-image VisionAnalyzer contract.
                return null;
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
