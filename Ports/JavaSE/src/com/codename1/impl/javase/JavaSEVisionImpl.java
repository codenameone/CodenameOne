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
package com.codename1.impl.javase;

import com.codename1.ai.vision.Barcode;
import com.codename1.ai.vision.DocumentScanResult;
import com.codename1.ai.vision.Face;
import com.codename1.ai.vision.ImageLabel;
import com.codename1.ai.vision.Pose;
import com.codename1.ai.vision.SegmentationMask;
import com.codename1.ai.vision.TextRecognitionResult;
import com.codename1.ai.vision.VisionException;
import com.codename1.ai.vision.VisionFeature;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionMetadata;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionPoint;
import com.codename1.ai.vision.VisionRect;
import com.codename1.impl.VisionImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/// Scriptable simulator backend for `com.codename1.ai.vision`.
///
/// The desktop has no on-device vision models, so instead of reporting every
/// analyzer as unsupported -- which leaves a scanner screen impossible to
/// build without a device -- this backend returns whatever the developer
/// scripted in *Simulate &gt; Vision*. The results carry plausible geometry so
/// overlay, debounce, and "what happens when nothing is found" code can be
/// written and debugged in the simulator.
///
/// The values are process-wide and driven from the menu, exactly like
/// `JavaSENfc`'s virtual tag. Nothing here runs on a device.
///
/// @hidden
public class JavaSEVisionImpl extends VisionImpl {
    /// Outcome the next analysis produces, whatever the feature.
    public enum SimOutcome {
        /// Return the scripted result for the requested feature.
        RESULT,
        /// Return an empty result: no codes, no faces, no text.
        NOTHING_FOUND,
        /// Fail with `VisionException.UNSUPPORTED`, the way a target without
        /// that model behaves.
        UNSUPPORTED,
        /// Fail with `VisionException.BACKEND_ERROR`.
        ERROR
    }

    /// Whether the simulated device has any vision support at all. When false
    /// every `isSupported()` call returns false, which is what a port with no
    /// vision backend looks like.
    public static volatile boolean simSupported = true;
    /// Outcome applied to the next -- and every subsequent -- analysis.
    public static volatile SimOutcome outcome = SimOutcome.RESULT;
    /// Value the simulated barcode scanner decodes.
    public static volatile String barcodeValue = "https://www.codenameone.com/";
    /// Symbology the simulated barcode scanner reports.
    public static volatile String barcodeFormat = "QR_CODE";
    /// Text the simulated recognizer reads.
    public static volatile String recognizedText = "Codename One\nOn-device OCR";
    /// How many faces the simulated detector finds.
    public static volatile int faceCount = 1;
    /// Smile probability reported for each simulated face.
    public static volatile float smilingProbability = 0.8f;
    /// Comma-separated `label:confidence` pairs the simulated classifier returns.
    public static volatile String imageLabels =
            "person:0.94,indoor:0.71,computer:0.55";
    /// Whether the simulated pose detector finds a body.
    public static volatile boolean poseDetected = true;

    private boolean closed;

    /// @param feature the analyzer's feature
    /// @param backendId the selected backend id
    /// @return whether the simulated device supports that pair
    @Override
    public boolean isSupported(VisionFeature feature, String backendId) {
        return !closed && simSupported;
    }

    /// Produces the scripted result for `feature`, delivered asynchronously on
    /// the EDT so that timing-sensitive application code behaves the way it
    /// will on a device.
    ///
    /// @param feature the analyzer's feature
    /// @param backendId the selected backend id
    /// @param image the input, used only for its dimensions
    /// @param options the analyzer's options
    /// @param <T> the analyzer's result type
    /// @return the scripted asynchronous result
    @Override
    @SuppressWarnings("unchecked")
    public <T> AsyncResource<T> analyze(VisionFeature feature, String backendId,
                                        VisionImage image, VisionOptions options) {
        final AsyncResource<T> result = new AsyncResource<T>();
        if (closed) {
            result.error(new VisionException(VisionException.BACKEND_ERROR,
                    "Vision backend is closed"));
            return result;
        }
        final SimOutcome requested = outcome;
        if (requested == SimOutcome.UNSUPPORTED) {
            result.error(new VisionException(VisionException.UNSUPPORTED,
                    feature + " is not supported by the simulated device"));
            return result;
        }
        if (requested == SimOutcome.ERROR) {
            result.error(new VisionException(VisionException.BACKEND_ERROR,
                    "Simulated " + feature + " failure"));
            return result;
        }
        final Object value = build(feature, requested, image);
        if (value == null) {
            result.error(new VisionException(VisionException.UNSUPPORTED,
                    feature + " has no simulated result"));
            return result;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                result.complete((T) value);
            }
        });
        return result;
    }

    /// Releases the simulated backend. Idempotent.
    @Override
    public void close() {
        closed = true;
    }

    private static Object build(VisionFeature feature, SimOutcome requested,
                                VisionImage image) {
        boolean empty = requested == SimOutcome.NOTHING_FOUND;
        switch (feature) {
            case BARCODE_SCANNING:
                return empty ? new Barcode[0] : barcodes();
            case TEXT_RECOGNITION:
                return empty ? TextRecognitionResult.EMPTY : text();
            case FACE_DETECTION:
                return empty ? new Face[0] : faces();
            case IMAGE_LABELING:
                return empty ? new ImageLabel[0] : labels();
            case POSE_DETECTION:
                return empty || !poseDetected
                        ? new Pose(new Pose.Landmark[0], metadata())
                        : pose();
            case SELFIE_SEGMENTATION:
                return mask(empty);
            case DOCUMENT_SCANNING:
                return document(empty, image);
            default:
                return null;
        }
    }

    private static VisionMetadata metadata() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("simulated", "true");
        return new VisionMetadata("simulator", values);
    }

    private static Barcode[] barcodes() {
        VisionRect bounds = new VisionRect(.3f, .35f, .4f, .3f);
        VisionPoint[] corners = {
            new VisionPoint(.3f, .35f), new VisionPoint(.7f, .35f),
            new VisionPoint(.7f, .65f), new VisionPoint(.3f, .65f)
        };
        String value = barcodeValue;
        byte[] raw = new byte[0];
        if (value != null) {
            try {
                raw = value.getBytes("UTF-8");
            } catch (UnsupportedEncodingException impossible) {
                raw = new byte[0];
            }
        }
        return new Barcode[] {
            new Barcode(value, barcodeFormat, raw, bounds, corners, metadata())
        };
    }

    private static TextRecognitionResult text() {
        String value = recognizedText == null ? "" : recognizedText;
        String[] lines = value.split("\n");
        TextRecognitionResult.TextBlock[] blocks =
                new TextRecognitionResult.TextBlock[lines.length];
        float lineHeight = lines.length == 0 ? 0 : .6f / lines.length;
        for (int i = 0; i < lines.length; i++) {
            blocks[i] = new TextRecognitionResult.TextBlock(lines[i], .9f,
                    new VisionRect(.1f, .2f + i * lineHeight, .8f,
                            lineHeight * .8f), "en");
        }
        return new TextRecognitionResult(value, blocks, metadata());
    }

    private static Face[] faces() {
        int count = Math.max(0, faceCount);
        Face[] out = new Face[count];
        float width = count == 0 ? 0 : Math.min(.35f, .8f / count);
        for (int i = 0; i < count; i++) {
            float left = .1f + i * (width + .05f);
            VisionRect bounds = new VisionRect(left, .25f, width, width * 1.3f);
            Map<String, VisionPoint> landmarks =
                    new HashMap<String, VisionPoint>();
            landmarks.put("leftEye",
                    new VisionPoint(left + width * .7f, .25f + width * .4f));
            landmarks.put("rightEye",
                    new VisionPoint(left + width * .3f, .25f + width * .4f));
            landmarks.put("noseBase",
                    new VisionPoint(left + width * .5f, .25f + width * .7f));
            landmarks.put("mouthLeft",
                    new VisionPoint(left + width * .7f, .25f + width * 1f));
            landmarks.put("mouthRight",
                    new VisionPoint(left + width * .3f, .25f + width * 1f));
            out[i] = new Face(bounds, landmarks, 0, 0, 0,
                    smilingProbability, i, metadata());
        }
        return out;
    }

    private static ImageLabel[] labels() {
        String value = imageLabels == null ? "" : imageLabels.trim();
        if (value.length() == 0) {
            return new ImageLabel[0];
        }
        String[] parts = value.split(",");
        ImageLabel[] out = new ImageLabel[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            int colon = part.lastIndexOf(':');
            String text = colon < 0 ? part : part.substring(0, colon);
            float confidence = .5f;
            if (colon >= 0) {
                try {
                    confidence = Float.parseFloat(part.substring(colon + 1));
                } catch (NumberFormatException ignored) {
                    text = part;
                }
            }
            out[i] = new ImageLabel(text.trim(), confidence, i, metadata());
        }
        return out;
    }

    private static Pose pose() {
        // A rough standing figure, enough to exercise skeleton drawing.
        String[] names = {
            "nose", "leftShoulder", "rightShoulder", "leftElbow", "rightElbow",
            "leftWrist", "rightWrist", "leftHip", "rightHip", "leftKnee",
            "rightKnee", "leftAnkle", "rightAnkle"
        };
        float[][] positions = {
            {.50f, .12f}, {.62f, .28f}, {.38f, .28f}, {.68f, .44f},
            {.32f, .44f}, {.72f, .58f}, {.28f, .58f}, {.58f, .56f},
            {.42f, .56f}, {.59f, .74f}, {.41f, .74f}, {.60f, .92f},
            {.40f, .92f}
        };
        Pose.Landmark[] landmarks = new Pose.Landmark[names.length];
        for (int i = 0; i < names.length; i++) {
            landmarks[i] = new Pose.Landmark(names[i],
                    new VisionPoint(positions[i][0], positions[i][1]), .9f);
        }
        return new Pose(landmarks, metadata());
    }

    private static SegmentationMask mask(boolean empty) {
        int width = 64;
        int height = 64;
        float[] confidence = new float[width * height];
        if (!empty) {
            // A centered ellipse standing in for the subject.
            float cx = width / 2f;
            float cy = height / 2f;
            float rx = width * .28f;
            float ry = height * .42f;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float dx = (x - cx) / rx;
                    float dy = (y - cy) / ry;
                    float d = dx * dx + dy * dy;
                    confidence[y * width + x] = d <= 1
                            ? Math.min(1f, 1.2f - d * .3f) : 0f;
                }
            }
        }
        return new SegmentationMask(width, height, confidence, metadata());
    }

    private static DocumentScanResult document(boolean empty, VisionImage image) {
        if (empty) {
            return new DocumentScanResult(new byte[0][], metadata());
        }
        byte[] page = image == null ? null : image.getEncodedBytes();
        if (page == null || page.length == 0) {
            return new DocumentScanResult(new byte[0][], metadata());
        }
        // The simulator has no perspective correction; the "corrected" page is
        // the page it was handed, which is enough to wire up a review screen.
        return new DocumentScanResult(new byte[][] {page}, metadata());
    }
}
