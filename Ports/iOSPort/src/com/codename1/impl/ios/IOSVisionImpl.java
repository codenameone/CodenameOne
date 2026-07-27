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
package com.codename1.impl.ios;

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
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Apple Vision/Core Image and optional ML Kit implementation. */
public final class IOSVisionImpl extends VisionImpl {
    private volatile boolean closed;

    @Override
    public boolean isSupported(VisionFeature feature, String backendId) {
        if (closed || (!"auto".equals(backendId)
                && !"apple-vision".equals(backendId)
                && !"ml-kit".equals(backendId))) {
            return false;
        }
        return IOSImplementation.nativeInstance.cn1VisionIsSupported(
                feature.ordinal(), "ml-kit".equals(backendId));
    }

    @Override
    public <T> AsyncResource<T> analyze(final VisionFeature feature,
                                         String backendId, final VisionImage image,
                                         VisionOptions options) {
        final AsyncResource<T> out = new AsyncResource<T>();
        final boolean mlKit = "ml-kit".equals(backendId);
        if (!isSupported(feature, backendId)) {
            out.error(new VisionException(VisionException.UNSUPPORTED,
                    feature + " is unavailable in "
                            + (mlKit ? "ML Kit" : "Apple Vision")));
            return out;
        }
        byte[] encoded = image.getEncodedBytesUnsafe();
        final byte[] input = encoded == null
                ? image.getPixelsUnsafe() : encoded;
        final int width = encoded == null ? image.getWidth() : 0;
        final int height = encoded == null ? image.getHeight() : 0;
        final int frameFormat = encoded == null
                ? image.getFormat().ordinal() : 0;
        if (input == null || input.length == 0) {
            out.error(new VisionException(VisionException.INVALID_IMAGE,
                    "Apple Vision requires encoded, NV21, or RGBA8888 input"));
            return out;
        }
        analyzeInBackground(out, feature, mlKit, input,
                image.getRotationDegrees(), width, height, frameFormat,
                options.getMinimumConfidence(), options.getMaximumResults());
        return out;
    }

    private static <T> void analyzeInBackground(final AsyncResource<T> out,
                                                 final VisionFeature feature,
                                                 final boolean mlKit,
                                                 final byte[] input,
                                                 final int rotation,
                                                 final int width,
                                                 final int height,
                                                 final int frameFormat,
                                                 final float minimumConfidence,
                                                 final int maximumResults) {
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    String json = IOSImplementation.nativeInstance.cn1VisionAnalyze(
                            input, feature.ordinal(), mlKit,
                            rotation, width, height, frameFormat);
                    if (json == null || json.length() == 0) {
                        throw new VisionException(VisionException.BACKEND_ERROR,
                                "Apple Vision returned no result");
                    }
                    final T value = parse(feature, json,
                            mlKit ? "ml-kit" : "apple-vision",
                            minimumConfidence, maximumResults);
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            out.complete(value);
                        }
                    });
                } catch (final Throwable error) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            out.error(error instanceof VisionException ? error
                                    : new VisionException(VisionException.BACKEND_ERROR,
                                            error.getMessage(), error));
                        }
                    });
                }
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> T parse(VisionFeature feature, String json,
                               String backendId, float minimumConfidence,
                               int maximumResults) throws Exception {
        Map root = new JSONParser().parseJSON(new StringReader(json));
        Object error = root.get("error");
        if (error != null) {
            throw new VisionException(VisionException.BACKEND_ERROR, String.valueOf(error));
        }
        List items = (List) root.get("items");
        if (items == null) {
            items = java.util.Collections.EMPTY_LIST;
        }
        items = filterItems(items, minimumConfidence, maximumResults);
        VisionMetadata metadata = new VisionMetadata(backendId);
        switch (feature) {
            case TEXT_RECOGNITION: {
                TextRecognitionResult.TextBlock[] blocks =
                        new TextRecognitionResult.TextBlock[items.size()];
                for (int i = 0; i < blocks.length; i++) {
                    Map value = (Map) items.get(i);
                    blocks[i] = new TextRecognitionResult.TextBlock(
                            string(value, "text"), number(value, "confidence"),
                            rect(value), stringOrNull(value, "language"));
                }
                return (T) new TextRecognitionResult(
                        string(root, "text"), blocks, metadata);
            }
            case BARCODE_SCANNING: {
                Barcode[] values = new Barcode[items.size()];
                for (int i = 0; i < values.length; i++) {
                    Map value = (Map) items.get(i);
                    List points = value.get("corners") instanceof List
                            ? (List) value.get("corners")
                            : java.util.Collections.EMPTY_LIST;
                    VisionPoint[] corners = new VisionPoint[points.size()];
                    for (int p = 0; p < corners.length; p++) {
                        Map point = (Map) points.get(p);
                        corners[p] = new VisionPoint(number(point, "x"),
                                number(point, "y"));
                    }
                    values[i] = new Barcode(stringOrNull(value, "value"),
                            string(value, "format"),
                            decodeBase64OrNull(value, "rawData"), rect(value),
                            corners, metadata);
                }
                return (T) values;
            }
            case FACE_DETECTION: {
                Face[] values = new Face[items.size()];
                for (int i = 0; i < values.length; i++) {
                    Map value = (Map) items.get(i);
                    Map<String, VisionPoint> landmarks =
                            parseLandmarks(value.get("landmarks"));
                    values[i] = new Face(rect(value),
                            landmarks,
                            number(value, "yaw"), number(value, "pitch"),
                            number(value, "roll"),
                            number(value, "smilingProbability", -1),
                            integer(value, "trackingId", -1), metadata);
                }
                return (T) values;
            }
            case IMAGE_LABELING: {
                ImageLabel[] values = new ImageLabel[items.size()];
                for (int i = 0; i < values.length; i++) {
                    Map value = (Map) items.get(i);
                    values[i] = new ImageLabel(string(value, "text"),
                            number(value, "confidence"),
                            integer(value, "index", -1), metadata);
                }
                return (T) values;
            }
            case POSE_DETECTION: {
                Pose.Landmark[] values = new Pose.Landmark[items.size()];
                for (int i = 0; i < values.length; i++) {
                    Map value = (Map) items.get(i);
                    values[i] = new Pose.Landmark(string(value, "name"),
                            new VisionPoint(number(value, "x"), number(value, "y")),
                            number(value, "confidence"));
                }
                return (T) new Pose(values, metadata);
            }
            case SELFIE_SEGMENTATION: {
                byte[] bytes = nativeData(root);
                float[] confidence = new float[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    confidence[i] = (bytes[i] & 0xff) / 255f;
                }
                return (T) new SegmentationMask(integer(root, "width"),
                        integer(root, "height"), confidence, metadata);
            }
            case DOCUMENT_SCANNING: {
                return (T) new DocumentScanResult(new byte[][] {
                        nativeData(root)
                }, metadata);
            }
            default:
                throw new VisionException(VisionException.UNSUPPORTED,
                        "Unknown Apple Vision feature " + feature);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List filterItems(List items, float minimumConfidence,
                                    int maximumResults) {
        if (items.isEmpty()
                || (minimumConfidence <= 0 && maximumResults == 0)) {
            return items;
        }
        List filtered = new ArrayList();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (minimumConfidence > 0 && item instanceof Map) {
                Object confidence = ((Map) item).get("confidence");
                if (confidence instanceof Number
                        && ((Number) confidence).floatValue()
                                < minimumConfidence) {
                    continue;
                }
            }
            filtered.add(item);
            if (maximumResults > 0 && filtered.size() >= maximumResults) {
                break;
            }
        }
        return filtered;
    }

    private static VisionRect rect(Map value) {
        return new VisionRect(number(value, "x"), number(value, "y"),
                number(value, "width"), number(value, "height"));
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, VisionPoint> parseLandmarks(Object value) {
        Map<String, VisionPoint> out = new HashMap<String, VisionPoint>();
        if (!(value instanceof Map)) {
            return out;
        }
        Map source = (Map) value;
        for (Object key : source.keySet()) {
            Object point = source.get(key);
            if (point instanceof Map) {
                out.put(String.valueOf(key), new VisionPoint(
                        number((Map) point, "x"),
                        number((Map) point, "y")));
            }
        }
        return out;
    }

    private static float number(Map value, String key) {
        return number(value, key, 0);
    }

    private static float number(Map value, String key, float fallback) {
        Object out = value.get(key);
        return out instanceof Number ? ((Number) out).floatValue() : fallback;
    }

    private static int integer(Map value, String key) {
        return integer(value, key, 0);
    }

    private static int integer(Map value, String key, int fallback) {
        Object out = value.get(key);
        return out instanceof Number ? ((Number) out).intValue() : fallback;
    }

    private static String string(Map value, String key) {
        String out = stringOrNull(value, key);
        return out == null ? "" : out;
    }

    private static String stringOrNull(Map value, String key) {
        Object out = value.get(key);
        return out == null ? null : String.valueOf(out);
    }

    private static byte[] nativeData(Map value) throws Exception {
        Object peerValue = value.get("dataPeer");
        if (peerValue instanceof Number) {
            long peer = ((Number) peerValue).longValue();
            if (peer == 0) {
                throw new VisionException(VisionException.BACKEND_ERROR,
                        "Apple Vision returned no binary result");
            }
            try {
                byte[] out = new byte[IOSImplementation.nativeInstance
                        .getNSDataSize(peer)];
                IOSImplementation.nativeInstance.nsDataToByteArray(peer, out);
                return out;
            } finally {
                IOSImplementation.nativeInstance.releasePeer(peer);
            }
        }
        String base64 = string(value, "data");
        return Base64.decode(base64.getBytes("UTF-8"));
    }

    private static byte[] decodeBase64OrNull(Map value, String key)
            throws Exception {
        String base64 = stringOrNull(value, key);
        return base64 == null ? null
                : Base64.decode(base64.getBytes("UTF-8"));
    }

    @Override
    public void close() {
        closed = true;
    }
}
