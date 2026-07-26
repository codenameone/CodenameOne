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

import com.codename1.ai.vision.Pose;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionPoint;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.ArrayList;
import java.util.List;

/** ML Kit pose detection; retained only for {@code PoseDetector} users. */
final class AndroidPoseDetectionAdapter extends AndroidVisionAdapter {
    private final PoseDetector client = PoseDetection.getClient(
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build());
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<Pose> out = (AsyncResource<Pose>) resource;
        final float minimumConfidence = options.getMinimumConfidence();
        final int maximumResults = options.getMaximumResults();
        client.process(input).addOnSuccessListener(
                new OnSuccessListener<com.google.mlkit.vision.pose.Pose>() {
            public void onSuccess(com.google.mlkit.vision.pose.Pose value) {
                List<PoseLandmark> source = value.getAllPoseLandmarks();
                List<Pose.Landmark> result = new ArrayList<Pose.Landmark>();
                for (int i = 0; i < source.size(); i++) {
                    PoseLandmark point = source.get(i);
                    if (point.getInFrameLikelihood() < minimumConfidence) {
                        continue;
                    }
                    result.add(new Pose.Landmark(
                            landmarkName(point.getLandmarkType()),
                            new VisionPoint(
                                    point.getPosition().x / imageWidth,
                                    point.getPosition().y / imageHeight),
                            point.getInFrameLikelihood()));
                    if (maximumResults > 0
                            && result.size() >= maximumResults) {
                        break;
                    }
                }
                complete(out, new Pose(result.toArray(
                        new Pose.Landmark[result.size()]), METADATA));
            }
        }).addOnFailureListener(failure(out));
    }

    @Override
    void close() {
        client.close();
    }

    private static String landmarkName(int type) {
        switch (type) {
            case PoseLandmark.NOSE: return "nose";
            case PoseLandmark.LEFT_EYE_INNER: return "leftEyeInner";
            case PoseLandmark.LEFT_EYE: return "leftEye";
            case PoseLandmark.LEFT_EYE_OUTER: return "leftEyeOuter";
            case PoseLandmark.RIGHT_EYE_INNER: return "rightEyeInner";
            case PoseLandmark.RIGHT_EYE: return "rightEye";
            case PoseLandmark.RIGHT_EYE_OUTER: return "rightEyeOuter";
            case PoseLandmark.LEFT_EAR: return "leftEar";
            case PoseLandmark.RIGHT_EAR: return "rightEar";
            case PoseLandmark.LEFT_MOUTH: return "leftMouth";
            case PoseLandmark.RIGHT_MOUTH: return "rightMouth";
            case PoseLandmark.LEFT_SHOULDER: return "leftShoulder";
            case PoseLandmark.RIGHT_SHOULDER: return "rightShoulder";
            case PoseLandmark.LEFT_ELBOW: return "leftElbow";
            case PoseLandmark.RIGHT_ELBOW: return "rightElbow";
            case PoseLandmark.LEFT_WRIST: return "leftWrist";
            case PoseLandmark.RIGHT_WRIST: return "rightWrist";
            case PoseLandmark.LEFT_PINKY: return "leftPinky";
            case PoseLandmark.RIGHT_PINKY: return "rightPinky";
            case PoseLandmark.LEFT_INDEX: return "leftIndex";
            case PoseLandmark.RIGHT_INDEX: return "rightIndex";
            case PoseLandmark.LEFT_THUMB: return "leftThumb";
            case PoseLandmark.RIGHT_THUMB: return "rightThumb";
            case PoseLandmark.LEFT_HIP: return "leftHip";
            case PoseLandmark.RIGHT_HIP: return "rightHip";
            case PoseLandmark.LEFT_KNEE: return "leftKnee";
            case PoseLandmark.RIGHT_KNEE: return "rightKnee";
            case PoseLandmark.LEFT_ANKLE: return "leftAnkle";
            case PoseLandmark.RIGHT_ANKLE: return "rightAnkle";
            case PoseLandmark.LEFT_HEEL: return "leftHeel";
            case PoseLandmark.RIGHT_HEEL: return "rightHeel";
            case PoseLandmark.LEFT_FOOT_INDEX: return "leftFootIndex";
            case PoseLandmark.RIGHT_FOOT_INDEX: return "rightFootIndex";
            default: return "unknown";
        }
    }
}
