/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import com.codename1.ai.vision.ImageLabel;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

/** ML Kit image labeling; retained only for {@code ImageLabeler} users. */
final class AndroidImageLabelingAdapter extends AndroidVisionAdapter {
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, int imageWidth, int imageHeight,
                 VisionOptions options, AsyncResource<?> resource) {
        final AsyncResource<ImageLabel[]> out =
                (AsyncResource<ImageLabel[]>) resource;
        final ImageLabeler client = ImageLabeling.getClient(
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(options.getMinimumConfidence())
                        .build());
        client.process(input).addOnSuccessListener(
                new OnSuccessListener<List<com.google.mlkit.vision.label.ImageLabel>>() {
            public void onSuccess(
                    List<com.google.mlkit.vision.label.ImageLabel> values) {
                ImageLabel[] result = new ImageLabel[values.size()];
                for (int i = 0; i < result.length; i++) {
                    com.google.mlkit.vision.label.ImageLabel value =
                            values.get(i);
                    result[i] = new ImageLabel(value.getText(),
                            value.getConfidence(), value.getIndex(), METADATA);
                }
                complete(out, result);
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
    }
}
