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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.ai.inference.InferenceOptions;
import com.codename1.ai.inference.InferenceSession;
import com.codename1.ai.inference.ModelSource;
import com.codename1.ai.inference.Tensor;
import com.codename1.ai.language.LanguageBackends;
import com.codename1.ai.language.LanguageOptions;
import com.codename1.ai.language.Translator;
import com.codename1.ai.vision.TextRecognizer;
import com.codename1.ai.vision.VisionBackends;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.io.Log;

class AiAndSpeechOnDeviceSnippet {
    private byte[] jpegBytes;
    private float[] pixels;

    void vision() {
        // tag::ai-and-speech-on-device-vision[]
        VisionOptions options = new VisionOptions()
                .backend(VisionBackends.mlKitTextRecognition())
                .minimumConfidence(0.5f);

        new TextRecognizer(options)
                .process(VisionImage.encoded(jpegBytes))
                .ready(result -> Log.p(result.getText()))
                .except(error -> Log.e(error));
        // end::ai-and-speech-on-device-vision[]
    }

    void inference() {
        // tag::ai-and-speech-on-device-inference[]
        InferenceSession.open(
                ModelSource.resource("/models/classifier.tflite"),
                new InferenceOptions().accelerator(
                        InferenceOptions.Accelerator.AUTO))
            .ready(session -> {
                Tensor input = Tensor.floats("serving_default_input",
                        new int[] {1, 224, 224, 3}, pixels);
                session.run(new Tensor[] {input})
                        .ready(outputs -> {
                            try {
                                consume(outputs[0]);
                            } finally {
                                session.close();
                            }
                        })
                        .except(error -> {
                            try {
                                Log.e(error);
                            } finally {
                                session.close();
                            }
                        });
            })
            .except(error -> Log.e(error));
        // end::ai-and-speech-on-device-inference[]
    }

    void language() {
        // tag::ai-and-speech-on-device-language[]
        LanguageOptions options = new LanguageOptions()
                .backend(LanguageBackends.mlKitTranslation());
        if (Translator.isSupported(options)) {
            Translator.translate("Where is the station?", "en", "fr",
                    options)
                .ready(value -> Log.p(value))
                .except(error -> Log.e(error));
        }
        // end::ai-and-speech-on-device-language[]
    }

    private void consume(Tensor output) {
    }
}
