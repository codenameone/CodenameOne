/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import com.codename1.ai.vision.SegmentationMask;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.nio.ByteBuffer;

/**
 * ML Kit selfie segmentation; retained only for {@code SelfieSegmenter} users.
 */
final class AndroidSelfieSegmentationAdapter extends AndroidVisionAdapter {
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, int imageWidth, int imageHeight,
                 VisionOptions options, AsyncResource<?> resource) {
        final AsyncResource<SegmentationMask> out =
                (AsyncResource<SegmentationMask>) resource;
        final Segmenter client = Segmentation.getClient(
                new SelfieSegmenterOptions.Builder()
                        .setDetectorMode(
                                SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build());
        client.process(input).addOnSuccessListener(
                new OnSuccessListener<
                        com.google.mlkit.vision.segmentation.SegmentationMask>() {
            public void onSuccess(
                    com.google.mlkit.vision.segmentation.SegmentationMask value) {
                ByteBuffer buffer = value.getBuffer();
                buffer.rewind();
                float[] confidence =
                        new float[value.getWidth() * value.getHeight()];
                buffer.asFloatBuffer().get(confidence);
                complete(out, new SegmentationMask(
                        value.getWidth(), value.getHeight(), confidence,
                        METADATA));
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
    }
}
