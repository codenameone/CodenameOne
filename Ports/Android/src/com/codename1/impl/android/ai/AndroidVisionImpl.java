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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.google.mlkit.vision.common.InputImage;

/**
 * Android vision dispatcher. Feature implementations live in separate source
 * files so the Android builder can retain one adapter and one ML Kit artifact
 * per analyzer used by the application.
 */
public final class AndroidVisionImpl extends VisionImpl {
    private volatile boolean closed;
    private AndroidVisionAdapter adapter;
    private VisionFeature adapterFeature;

    @Override
    public boolean isSupported(VisionFeature feature, String backendId) {
        return !closed
                && ("auto".equals(backendId) || "ml-kit".equals(backendId))
                && adapterClass(feature) != null
                && isClassPresent(adapterClass(feature));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> AsyncResource<T> analyze(final VisionFeature feature,
                                         String backendId,
                                         final VisionImage image,
                                         final VisionOptions options) {
        final AsyncResource<T> out = new AsyncResource<T>();
        final VisionOptions optionSnapshot = new VisionOptions()
                .backend(options.getBackend())
                .minimumConfidence(options.getMinimumConfidence())
                .maximumResults(options.getMaximumResults());
        if (!isSupported(feature, backendId)) {
            out.error(new VisionException(VisionException.UNSUPPORTED,
                    feature + " is not included in this Android build"));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    DecodedInput decoded = decode(image);
                    if (decoded == null) {
                        error(out, new VisionException(
                                VisionException.INVALID_IMAGE,
                                "Vision input is not valid JPEG, PNG, NV21, "
                                        + "or RGBA8888 data"));
                        return;
                    }
                    adapter(feature).analyze(decoded.input, decoded.width,
                            decoded.height, optionSnapshot,
                            (AsyncResource) out);
                } catch (Throwable cause) {
                    error(out, new VisionException(
                            VisionException.BACKEND_ERROR,
                            "Could not start the Android " + feature
                                    + " adapter", cause));
                }
            }
        });
        return out;
    }

    private synchronized AndroidVisionAdapter adapter(VisionFeature feature)
            throws Exception {
        if (closed) {
            throw new IllegalStateException("Vision backend is closed");
        }
        if (adapter == null) {
            adapter = (AndroidVisionAdapter) Class.forName(
                    adapterClass(feature)).newInstance();
            adapterFeature = feature;
        } else if (adapterFeature != feature) {
            throw new IllegalStateException(
                    "A vision backend instance cannot analyze multiple features");
        }
        return adapter;
    }

    private static DecodedInput decode(VisionImage image) {
        byte[] encoded = image.getEncodedBytesUnsafe();
        if (encoded != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    encoded, 0, encoded.length);
            return bitmap == null ? null : new DecodedInput(
                    InputImage.fromBitmap(bitmap, image.getRotationDegrees()),
                    bitmap.getWidth(), bitmap.getHeight(),
                    image.getRotationDegrees());
        }
        byte[] pixels = image.getPixelsUnsafe();
        int width = image.getWidth();
        int height = image.getHeight();
        if (pixels == null || width <= 0 || height <= 0) {
            return null;
        }
        long pixelCount = (long) width * (long) height;
        if (pixelCount > Integer.MAX_VALUE) {
            return null;
        }
        if (image.getFormat() == FrameFormat.NV21) {
            if ((width & 1) != 0 || (height & 1) != 0
                    || pixels.length < pixelCount + pixelCount / 2) {
                return null;
            }
            return new DecodedInput(InputImage.fromByteArray(
                    pixels, width, height, image.getRotationDegrees(),
                    InputImage.IMAGE_FORMAT_NV21), width, height,
                    image.getRotationDegrees());
        }
        if (image.getFormat() == FrameFormat.RGBA8888) {
            if (pixelCount * 4 > pixels.length) {
                return null;
            }
            int[] argb = new int[(int) pixelCount];
            for (int i = 0, p = 0; i < argb.length; i++, p += 4) {
                argb[i] = ((pixels[p + 3] & 255) << 24)
                        | ((pixels[p] & 255) << 16)
                        | ((pixels[p + 1] & 255) << 8)
                        | (pixels[p + 2] & 255);
            }
            Bitmap bitmap = Bitmap.createBitmap(
                    argb, width, height, Bitmap.Config.ARGB_8888);
            return new DecodedInput(InputImage.fromBitmap(
                    bitmap, image.getRotationDegrees()), width, height,
                    image.getRotationDegrees());
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                pixels, 0, pixels.length);
        return bitmap == null ? null : new DecodedInput(
                InputImage.fromBitmap(bitmap, image.getRotationDegrees()),
                bitmap.getWidth(), bitmap.getHeight(),
                image.getRotationDegrees());
    }

    private static final class DecodedInput {
        final InputImage input;
        final int width;
        final int height;

        DecodedInput(InputImage input, int width, int height, int rotation) {
            this.input = input;
            if (rotation == 90 || rotation == 270) {
                this.width = height;
                this.height = width;
            } else {
                this.width = width;
                this.height = height;
            }
        }
    }

    private static void error(final AsyncResource<?> out,
                              final VisionException error) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                out.error(error);
            }
        });
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
    public synchronized void close() {
        closed = true;
        if (adapter != null) {
            adapter.close();
            adapter = null;
        }
    }
}
