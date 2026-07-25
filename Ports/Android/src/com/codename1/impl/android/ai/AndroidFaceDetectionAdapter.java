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

import com.codename1.ai.vision.Face;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionPoint;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ML Kit face detection; retained only for {@code FaceDetector} users. */
final class AndroidFaceDetectionAdapter extends AndroidVisionAdapter {
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<Face[]> out = (AsyncResource<Face[]>) resource;
        FaceDetectorOptions detectorOptions =
                new FaceDetectorOptions.Builder()
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(
                                FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking().build();
        final FaceDetector client =
                FaceDetection.getClient(detectorOptions);
        client.process(input).addOnSuccessListener(
                new OnSuccessListener<List<com.google.mlkit.vision.face.Face>>() {
            public void onSuccess(
                    List<com.google.mlkit.vision.face.Face> values) {
                Face[] result = new Face[values.size()];
                for (int i = 0; i < result.length; i++) {
                    com.google.mlkit.vision.face.Face value = values.get(i);
                    Map<String, VisionPoint> landmarks =
                            new HashMap<String, VisionPoint>();
                    addLandmark(landmarks, "leftEye",
                            value.getLandmark(FaceLandmark.LEFT_EYE),
                            imageWidth, imageHeight);
                    addLandmark(landmarks, "rightEye",
                            value.getLandmark(FaceLandmark.RIGHT_EYE),
                            imageWidth, imageHeight);
                    addLandmark(landmarks, "noseBase",
                            value.getLandmark(FaceLandmark.NOSE_BASE),
                            imageWidth, imageHeight);
                    addLandmark(landmarks, "mouthLeft",
                            value.getLandmark(FaceLandmark.MOUTH_LEFT),
                            imageWidth, imageHeight);
                    addLandmark(landmarks, "mouthRight",
                            value.getLandmark(FaceLandmark.MOUTH_RIGHT),
                            imageWidth, imageHeight);
                    Float smile = value.getSmilingProbability();
                    Integer tracking = value.getTrackingId();
                    result[i] = new Face(
                            normalized(value.getBoundingBox(),
                                    imageWidth, imageHeight),
                            landmarks, value.getHeadEulerAngleY(), 0,
                            value.getHeadEulerAngleZ(),
                            smile == null ? -1 : smile,
                            tracking == null ? -1 : tracking, METADATA);
                }
                complete(out, result);
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
    }

    private static void addLandmark(Map<String, VisionPoint> out, String name,
                                    FaceLandmark landmark, int imageWidth,
                                    int imageHeight) {
        if (landmark != null) {
            out.put(name, new VisionPoint(
                    landmark.getPosition().x / imageWidth,
                    landmark.getPosition().y / imageHeight));
        }
    }
}
