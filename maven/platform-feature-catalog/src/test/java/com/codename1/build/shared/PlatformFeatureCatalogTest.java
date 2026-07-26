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
package com.codename1.build.shared;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformFeatureCatalogTest {

    @Test
    void builtInTextRecognizerMapsToAppleVisionAndAndroidMlKit() {
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ai/vision/TextRecognizer");
        assertEquals(1, hits.size(), "expected one entry to fire");
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("Vision"));
        assertTrue(e.iosPods().isEmpty());
        assertTrue(e.androidGradleDeps().get(0).startsWith("com.google.mlkit:text-recognition"));
        assertTrue(e.iosPlistEntries().isEmpty(),
                "Still-image analysis must not imply camera permission");
    }

    @Test
    void explicitMlKitBackendAddsOnlyUsedFeaturePod() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consumeMethod("com/codename1/ai/vision/VisionBackends",
                "mlKitTextRecognition");
        boolean foundTextPod = false;
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            foundTextPod |= e.iosPods().contains("GoogleMLKit/TextRecognition");
            assertFalse(e.iosPods().contains("GoogleMLKit/FaceDetection"),
                    "Unused vision features must not be bundled");
            if (!e.iosPods().isEmpty()) {
                assertFalse(e.iosDependenciesSupportArm64Simulator(),
                        "Google ML Kit binaries require the x86_64 iOS simulator");
            }
        }
        assertTrue(foundTextPod);
    }

    @Test
    void speechRecognizerInjectsMicAndSpeechPlist() {
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/media/SpeechRecognizer");
        assertEquals(1, hits.size());
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("Speech"));
        assertNotNull(findPlistDefault(e, "NSMicrophoneUsageDescription"));
        assertNotNull(findPlistDefault(e, "NSSpeechRecognitionUsageDescription"));
        assertTrue(e.androidPermissions().contains("android.permission.RECORD_AUDIO"));
    }

    @Test
    void textToSpeechInjectsNoPermissions() {
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/media/TextToSpeech");
        assertEquals(1, hits.size());
        PlatformFeatureCatalog.Entry e = hits.get(0);
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
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ai/LlmClient");
        assertEquals(1, hits.size());
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosPods().isEmpty());
        assertTrue(e.androidGradleDeps().isEmpty());
        assertTrue(e.androidPermissions().isEmpty());
    }

    @Test
    void stableDiffusionFlagsBigUpload() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/imagegen/StableDiffusion");
        assertTrue(acc.anyRequiresBigUpload(),
                "On-device SD ships a 1-2 GB Core ML model -- cloud builds must abort with a friendly message");
    }

    @Test
    void builtInVisionDoesNotFlagBigUpload() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consume("com/codename1/ai/vision/BarcodeScanner");
        acc.consume("com/codename1/ai/whisper/WhisperRecognizer");
        assertFalse(acc.anyRequiresBigUpload(),
                "ML Kit models stream lazily, Whisper bundles a small static lib -- neither exceeds the 2 GB cap");
    }

    @Test
    void unrelatedClassesProduceNoHits() {
        // Sanity: we mustn't false-positive on classes outside the
        // AI namespace, because the scanner walks every class in
        // the user's app.
        assertTrue(PlatformFeatureCatalog.matchesFor("com/codename1/ui/Form").isEmpty());
        assertTrue(PlatformFeatureCatalog.matchesFor("java/lang/Object").isEmpty());
        assertTrue(PlatformFeatureCatalog.matchesFor(null).isEmpty());
    }

    @Test
    void cameraEntryInjectsAvFoundationAndCameraXGradleDeps() {
        // Referencing any class in com.codename1.camera.* must auto-inject
        // the iOS frameworks, iOS plist usage descriptions, Android
        // permissions, and the four CameraX Gradle dependencies that the
        // AndroidCameraImpl reflection layer resolves at runtime.
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/camera/Camera");
        assertEquals(1, hits.size(), "expected the camera entry to fire");
        PlatformFeatureCatalog.Entry e = hits.get(0);

        // iOS side
        assertTrue(e.iosFrameworks().contains("AVFoundation"));
        assertTrue(e.iosFrameworks().contains("CoreMedia"));
        assertTrue(e.iosFrameworks().contains("CoreVideo"));
        assertNotNull(findPlistDefault(e, "NSCameraUsageDescription"));
        assertNotNull(findPlistDefault(e, "NSMicrophoneUsageDescription"));
        assertTrue(e.iosPods().isEmpty(),
                "Camera uses AVFoundation framework, not a pod");

        // Android side
        assertTrue(e.androidPermissions().contains("android.permission.CAMERA"));
        assertTrue(e.androidPermissions().contains("android.permission.RECORD_AUDIO"));
        assertTrue(e.androidFeatures().contains("android.hardware.camera"));

        boolean cameraCore = false, camera2 = false, lifecycle = false,
                view = false, video = false;
        for (String gav : e.androidGradleDeps()) {
            if (gav.startsWith("androidx.camera:camera-core:")) cameraCore = true;
            if (gav.startsWith("androidx.camera:camera-camera2:")) camera2 = true;
            if (gav.startsWith("androidx.camera:camera-lifecycle:")) lifecycle = true;
            if (gav.startsWith("androidx.camera:camera-view:")) view = true;
            if (gav.startsWith("androidx.camera:camera-video:")) video = true;
        }
        assertTrue(cameraCore, "missing androidx.camera:camera-core gradle dep");
        assertTrue(camera2,    "missing androidx.camera:camera-camera2 gradle dep");
        assertTrue(lifecycle,  "missing androidx.camera:camera-lifecycle gradle dep");
        assertTrue(view,       "missing androidx.camera:camera-view gradle dep");
        assertTrue(video,      "missing androidx.camera:camera-video gradle dep");
    }

    @Test
    void cameraEntryFiresOnAnySubpackageClass() {
        // The prefix matcher must hit any class inside com.codename1.camera,
        // not just the entry point.
        assertEquals(1, PlatformFeatureCatalog.matchesFor("com/codename1/camera/CameraView").size());
        assertEquals(1, PlatformFeatureCatalog.matchesFor("com/codename1/camera/CameraSession").size());
        assertEquals(1, PlatformFeatureCatalog.matchesFor("com/codename1/camera/internal/Foo").size());
    }

    @Test
    void inferenceUsesCoreMlEnabledObjectiveCPod() {
        // The Objective-C native bridge uses the Core ML-enabled TFLite
        // subspec.  The Swift package exposes a different surface and must
        // not be selected for this implementation.
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ai/inference/InferenceSession");
        assertEquals(1, hits.size());
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosPods().contains("TensorFlowLiteObjC/CoreML"));
        assertTrue(e.iosSpmSpecs().isEmpty());
        assertFalse(e.iosDependenciesSupportMacCatalyst(),
                "The official TensorFlow Lite Objective-C XCFramework has no Catalyst slice");
        assertTrue(e.iosDependenciesSupportArm64Simulator(),
                "TensorFlow Lite's XCFramework includes an arm64 simulator slice");
    }

    @Test
    void thirdPartyAppleAiPackagesAreExcludedFromCatalyst() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consumeMethod("com/codename1/ai/vision/VisionBackends",
                "mlKitTextRecognition");
        acc.consume("com/codename1/ai/language/LanguageIdentifier");
        acc.consume("com/codename1/ai/inference/InferenceSession");

        boolean foundSystemVision = false;
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            if (e.iosPods().isEmpty()) {
                foundSystemVision |= e.iosFrameworks().contains("Vision");
                assertTrue(e.iosDependenciesSupportMacCatalyst(),
                        "Apple system-framework entries should remain enabled");
            } else {
                assertFalse(e.iosDependenciesSupportMacCatalyst(),
                        e.description() + " must not inject an iOS-only package into Catalyst");
            }
        }
        assertTrue(foundSystemVision);
    }

    @Test
    void selectorMethodsAreMatchedExactlyAndLanguageIdDefaultsToApple() {
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consumeMethod("com/codename1/ai/vision/VisionBackends",
                "mlKitTextRecognitionFuture");
        for (PlatformFeatureCatalog.Entry entry : acc.hits()) {
            assertTrue(entry.iosPods().isEmpty(),
                    "A method-name prefix must not select an optional pod");
        }

        List<PlatformFeatureCatalog.Entry> language =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/ai/language/LanguageIdentifier");
        assertEquals(1, language.size());
        assertTrue(language.get(0).iosPods().isEmpty());
        assertTrue(language.get(0).iosFrameworks().contains("NaturalLanguage"));
        assertTrue(language.get(0).iosDependenciesSupportArm64Simulator());
    }

    @Test
    void androidAdaptersReceiveOnlyTheirFeatureDependency() {
        List<PlatformFeatureCatalog.Entry> vision = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ai/vision/TextRecognizer");
        assertEquals(1, vision.get(0).androidGradleDeps().size());
        assertTrue(vision.get(0).androidGradleDeps().get(0)
                .startsWith("com.google.mlkit:text-recognition:"));

        List<PlatformFeatureCatalog.Entry> language = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ai/language/LanguageIdentifier");
        assertEquals(1, language.get(0).androidGradleDeps().size());
        assertTrue(language.get(0).androidGradleDeps().get(0)
                .startsWith("com.google.mlkit:language-id:"));
    }

    @Test
    void documentCorrectionDoesNotInjectUnusedAndroidScanner() {
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ai/vision/DocumentScanner");
        assertEquals(1, hits.size());
        assertTrue(hits.get(0).androidGradleDeps().isEmpty(),
                "The interactive Google scanner cannot implement a still-image analyzer");
        assertTrue(hits.get(0).iosFrameworks().contains("VisionKit"));
    }

    @Test
    void accumulatorDeduplicates() {
        // Same class twice in the same scan shouldn't add the entry
        // twice -- otherwise we'd inject duplicate Gradle / pod
        // lines on the wire.
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        assertEquals(1, acc.hits().size());
    }

    @Test
    void arApiInjectsArKitCameraAndArCore() {
        List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(
                "com/codename1/ar/AR");
        assertEquals(1, hits.size(), "expected the AR entry to fire");
        PlatformFeatureCatalog.Entry e = hits.get(0);
        // iOS: ARKit + SceneKit (linked explicitly by IPhoneBuilder) and the
        // camera usage string, overridable via ios.NSCameraUsageDescription.
        assertTrue(e.iosFrameworks().contains("ARKit"));
        assertTrue(e.iosFrameworks().contains("SceneKit"));
        assertNotNull(findPlistDefault(e, "NSCameraUsageDescription"));
        // Android: the ARCore dependency, the camera permission and the
        // optional AR feature/meta-data pair so non-AR devices still install.
        assertEquals(1, e.androidGradleDeps().size());
        assertTrue(e.androidGradleDeps().get(0).startsWith("com.google.ar:core"));
        assertTrue(e.androidPermissions().contains("android.permission.CAMERA"));
        assertTrue(e.androidFeatures().contains("android.hardware.camera.ar"));
        assertEquals(1, e.androidMetaDataEntries().size());
        assertEquals("com.google.ar.core", e.androidMetaDataEntries().get(0)[0]);
        assertEquals("optional", e.androidMetaDataEntries().get(0)[1]);
    }

    @Test
    void arEntryMatchesTheWholePackageButNothingElse() {
        assertEquals(1, PlatformFeatureCatalog.matchesFor("com/codename1/ar/ARSession").size());
        assertEquals(1, PlatformFeatureCatalog.matchesFor("com/codename1/ar/ARNode").size());
        // The pure-core VR package must NOT pull the AR native dependencies.
        assertTrue(PlatformFeatureCatalog.matchesFor("com/codename1/vr/VRView").isEmpty());
    }

    @Test
    void nonArEntriesCarryNoMetaData() {
        // The meta-data field is new; make sure the existing entries did not
        // accidentally gain one.
        for (PlatformFeatureCatalog.Entry e : PlatformFeatureCatalog.entries()) {
            if (!e.classPrefix().startsWith("com/codename1/ar/")) {
                assertTrue(e.androidMetaDataEntries().isEmpty(),
                        e.classPrefix() + " should carry no manifest meta-data");
            }
        }
    }

    @Test
    void bluetoothEntryFiresForEverySubPackage() {
        String[] classes = {
            "com/codename1/bluetooth/Bluetooth",
            "com/codename1/bluetooth/le/BlePeripheral",
            "com/codename1/bluetooth/le/server/GattServer",
            "com/codename1/bluetooth/gatt/GattCharacteristic",
            "com/codename1/bluetooth/classic/RfcommConnection"
        };
        for (String cls : classes) {
            List<PlatformFeatureCatalog.Entry> hits = PlatformFeatureCatalog.matchesFor(cls);
            assertEquals(1, hits.size(), "expected the bluetooth entry for " + cls);
            PlatformFeatureCatalog.Entry e = hits.get(0);
            assertNotNull(findPlistDefault(e, "NSBluetoothAlwaysUsageDescription"));
            assertNotNull(findPlistDefault(e, "NSBluetoothPeripheralUsageDescription"));
            assertTrue(e.iosFrameworks().contains("CoreBluetooth"));
            // Android permissions deliberately live in
            // BluetoothManifestFragments (maxSdkVersion / neverForLocation
            // nuances the table cannot express), not in the entry.
            assertTrue(e.androidPermissions().isEmpty(),
                    "bluetooth Android permissions must come from BluetoothManifestFragments");
        }
    }

    @Test
    void bluetoothEntryDoesNotFireForUnrelatedClasses() {
        assertTrue(PlatformFeatureCatalog.matchesFor("com/codename1/ui/Form").isEmpty());
        // "bluetoothle" cn1lib package must NOT trigger the core entry
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/bluetoothle/Bluetooth").isEmpty());
    }

    private static String findPlistDefault(PlatformFeatureCatalog.Entry e, String key) {
        for (String[] entry : e.iosPlistEntries()) {
            if (key.equals(entry[0])) {
                return entry[1];
            }
        }
        return null;
    }
}
