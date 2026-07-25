/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import com.codename1.ai.vision.TextRecognitionResult;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

/** ML Kit text recognition; retained only for {@code TextRecognizer} users. */
final class AndroidTextRecognitionAdapter extends AndroidVisionAdapter {
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<TextRecognitionResult> out =
                (AsyncResource<TextRecognitionResult>) resource;
        final TextRecognizer client = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);
        client.process(input).addOnSuccessListener(new OnSuccessListener<Text>() {
            public void onSuccess(Text text) {
                List<Text.TextBlock> source = text.getTextBlocks();
                TextRecognitionResult.TextBlock[] blocks =
                        new TextRecognitionResult.TextBlock[source.size()];
                for (int i = 0; i < blocks.length; i++) {
                    Text.TextBlock block = source.get(i);
                    blocks[i] = new TextRecognitionResult.TextBlock(
                            block.getText(), 1f,
                            normalized(block.getBoundingBox(),
                                    imageWidth, imageHeight), null);
                }
                complete(out, new TextRecognitionResult(
                        text.getText(), blocks, METADATA));
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
    }
}
