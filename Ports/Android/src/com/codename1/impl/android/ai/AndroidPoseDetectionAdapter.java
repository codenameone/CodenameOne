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
        client.process(input).addOnSuccessListener(
                new OnSuccessListener<com.google.mlkit.vision.pose.Pose>() {
            public void onSuccess(com.google.mlkit.vision.pose.Pose value) {
                List<PoseLandmark> source = value.getAllPoseLandmarks();
                Pose.Landmark[] landmarks = new Pose.Landmark[source.size()];
                for (int i = 0; i < landmarks.length; i++) {
                    PoseLandmark point = source.get(i);
                    landmarks[i] = new Pose.Landmark(
                            String.valueOf(point.getLandmarkType()),
                            new VisionPoint(
                                    point.getPosition().x / imageWidth,
                                    point.getPosition().y / imageHeight),
                            point.getInFrameLikelihood());
                }
                complete(out, new Pose(landmarks, METADATA));
            }
        }).addOnFailureListener(failure(out));
    }

    @Override
    void close() {
        client.close();
    }
}
