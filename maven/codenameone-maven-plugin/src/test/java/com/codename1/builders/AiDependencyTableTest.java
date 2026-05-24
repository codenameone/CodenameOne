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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDependencyTableTest {

    @Test
    void mlkitTextRecognizerMapsToPodAndGradleDep() {
        List<AiDependencyTable.Entry> hits = AiDependencyTable.matchesFor(
                "com/codename1/ai/mlkit/text/TextRecognizer");
        assertEquals(1, hits.size(), "expected one entry to fire");
        AiDependencyTable.Entry e = hits.get(0);
        assertTrue(e.iosPods().contains("GoogleMLKit/TextRecognition"));
        assertTrue(e.androidGradleDeps().get(0).startsWith("com.google.mlkit:text-recognition"));
        // Camera plist string is injected because text recognition
        // is virtually always used with the camera.
        assertNotNull(findPlistDefault(e, "NSCameraUsageDescription"));
    }

    @Test
    void speechRecognizerInjectsMicAndSpeechPlist() {
        List<AiDependencyTable.Entry> hits = AiDependencyTable.matchesFor(
                "com/codename1/media/SpeechRecognizer");
        assertEquals(1, hits.size());
        AiDependencyTable.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("Speech"));
        assertNotNull(findPlistDefault(e, "NSMicrophoneUsageDescription"));
        assertNotNull(findPlistDefault(e, "NSSpeechRecognitionUsageDescription"));
        assertTrue(e.androidPermissions().contains("android.permission.RECORD_AUDIO"));
    }

    @Test
    void textToSpeechInjectsNoPermissions() {
        List<AiDependencyTable.Entry> hits = AiDependencyTable.matchesFor(
                "com/codename1/media/TextToSpeech");
        assertEquals(1, hits.size());
        AiDependencyTable.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("AVFAudio"));
        assertTrue(e.androidPermissions().isEmpty(),
                "TTS is built-in on every supported OS -- no permission needed");
        assertTrue(e.iosPlistEntries().isEmpty(),
                "TTS has no Apple-reviewed restricted entitlement");
    }

    @Test
    void llmClientNeedsNothingExtra() {
        // The LlmClient entries are intentionally cheap: pure HTTPS
        // means no plist string, no extra permission. They still
        // register so future diagnostics ("which AI APIs does this
        // app use?") can enumerate them.
        List<AiDependencyTable.Entry> hits = AiDependencyTable.matchesFor(
                "com/codename1/ai/LlmClient");
        assertEquals(1, hits.size());
        AiDependencyTable.Entry e = hits.get(0);
        assertTrue(e.iosPods().isEmpty());
        assertTrue(e.androidGradleDeps().isEmpty());
        assertTrue(e.androidPermissions().isEmpty());
    }

    @Test
    void stableDiffusionFlagsBigUpload() {
        AiDependencyTable.Accumulator acc = new AiDependencyTable.Accumulator();
        acc.consume("com/codename1/ai/imagegen/StableDiffusion");
        assertTrue(acc.anyRequiresBigUpload(),
                "On-device SD ships a 1-2 GB Core ML model -- cloud builds must abort with a friendly message");
    }

    @Test
    void mlkitDoesNotFlagBigUpload() {
        AiDependencyTable.Accumulator acc = new AiDependencyTable.Accumulator();
        acc.consume("com/codename1/ai/mlkit/text/TextRecognizer");
        acc.consume("com/codename1/ai/mlkit/barcode/BarcodeScanner");
        acc.consume("com/codename1/ai/whisper/WhisperRecognizer");
        assertFalse(acc.anyRequiresBigUpload(),
                "ML Kit models stream lazily, Whisper bundles a small static lib -- neither exceeds the 2 GB cap");
    }

    @Test
    void unrelatedClassesProduceNoHits() {
        // Sanity: we mustn't false-positive on classes outside the
        // AI namespace, because the scanner walks every class in
        // the user's app.
        assertTrue(AiDependencyTable.matchesFor("com/codename1/ui/Form").isEmpty());
        assertTrue(AiDependencyTable.matchesFor("java/lang/Object").isEmpty());
        assertTrue(AiDependencyTable.matchesFor(null).isEmpty());
    }

    @Test
    void tfliteHasBothPodAndSpmSpec() {
        // TFLite is published as both a CocoaPod and a Swift Package.
        // The table records both so projects can route the dep
        // through whichever manager they prefer; the IPhoneBuilder
        // applies whichever matches the project's current
        // ios.dependencyManager setting.
        List<AiDependencyTable.Entry> hits = AiDependencyTable.matchesFor(
                "com/codename1/ai/tflite/Interpreter");
        assertEquals(1, hits.size());
        AiDependencyTable.Entry e = hits.get(0);
        assertFalse(e.iosPods().isEmpty(), "expected a CocoaPods spec");
        assertFalse(e.iosSpmSpecs().isEmpty(), "expected an SPM spec");
    }

    @Test
    void accumulatorDeduplicates() {
        // Same class twice in the same scan shouldn't add the entry
        // twice -- otherwise we'd inject duplicate Gradle / pod
        // lines on the wire.
        AiDependencyTable.Accumulator acc = new AiDependencyTable.Accumulator();
        acc.consume("com/codename1/ai/mlkit/text/TextRecognizer");
        acc.consume("com/codename1/ai/mlkit/text/OptionsBuilder");
        assertEquals(1, acc.hits().size());
    }

    private static String findPlistDefault(AiDependencyTable.Entry e, String key) {
        for (String[] entry : e.iosPlistEntries()) {
            if (key.equals(entry[0])) {
                return entry[1];
            }
        }
        return null;
    }
}
