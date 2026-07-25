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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AndroidAiSourceSelectionTest {
    @Test
    void selectsOneSourcePerVisionFeature() {
        assertEquals("AndroidTextRecognitionAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/vision/TextRecognizer"));
        assertEquals("AndroidBarcodeScanningAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/vision/BarcodeScanner"));
        assertEquals("AndroidFaceDetectionAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/vision/FaceDetector"));
        assertEquals("AndroidImageLabelingAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/vision/ImageLabeler"));
        assertEquals("AndroidPoseDetectionAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/vision/PoseDetector"));
        assertEquals("AndroidSelfieSegmentationAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/vision/SelfieSegmenter"));
    }

    @Test
    void selectsOneSourcePerLanguageFeature() {
        assertEquals("AndroidLanguageIdAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/language/LanguageIdentifier"));
        assertEquals("AndroidTranslationAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/language/Translator"));
        assertEquals("AndroidSmartReplyAdapter.java",
                AndroidGradleBuilder.androidAiAdapterSource(
                        "com/codename1/ai/language/SmartReply"));
    }

    @Test
    void ignoresSharedApiAndUnrelatedClasses() {
        assertNull(AndroidGradleBuilder.androidAiAdapterSource(
                "com/codename1/ai/vision/VisionPipeline"));
        assertNull(AndroidGradleBuilder.androidAiAdapterSource(
                "com/codename1/ui/Form"));
    }
}
