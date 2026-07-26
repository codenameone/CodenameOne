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
    private final TextRecognizer client = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS);
    @Override
    @SuppressWarnings("unchecked")
    void analyze(InputImage input, final int imageWidth,
                 final int imageHeight, VisionOptions options,
                 AsyncResource<?> resource) {
        final AsyncResource<TextRecognitionResult> out =
                (AsyncResource<TextRecognitionResult>) resource;
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
            }
        }).addOnFailureListener(failure(out));
    }

    @Override
    void close() {
        client.close();
    }
}
