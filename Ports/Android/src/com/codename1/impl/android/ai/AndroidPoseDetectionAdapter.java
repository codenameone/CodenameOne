/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<Pose> out = (AsyncResource<Pose>) resource;
        final PoseDetector client = PoseDetection.getClient(
                new PoseDetectorOptions.Builder()
                        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                        .build());
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
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
    }
}
