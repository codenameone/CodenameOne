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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        assertEquals(21, e.androidMinimumSdk());
        assertTrue(e.iosPlistEntries().isEmpty(),
                "Still-image analysis must not imply camera permission");
    }

    @Test
    void callSessionBuysCallKitAndTheTelecomFloor() {
        List<PlatformFeatureCatalog.Entry> hits =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/call/session/Calls");
        assertEquals(1, hits.size(), "expected one entry to fire");
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("CallKit"));
        assertFalse(e.iosFrameworks().contains("PushKit"),
                "owning a call must not buy the VoIP push machinery");
        // A self-managed ConnectionService is exactly API 26; below it there
        // is nothing to degrade to.
        assertEquals(26, e.androidMinimumSdk());
    }

    @Test
    void onlyTheVoipPackageBuysPushKit() {
        // The voip background mode Apple rejects an app for carrying without
        // cause rides along with PushKit, so this boundary is the thing that
        // keeps an ordinary calling app shippable.
        assertFalse(PlatformFeatureCatalog.matchesFor(
                "com/codename1/call/session/CallSession")
                .get(0).iosFrameworks().contains("PushKit"));
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/call/voip/VoipPush")
                .get(0).iosFrameworks().contains("PushKit"));
    }

    @Test
    void theCallDirectoryIsNotASupersetOfOwningCalls() {
        // The load-bearing property of the whole package split. A caller-ID
        // app never owns a call, so referencing the directory must not drag
        // in what session/voip cost. The prefix match cannot express an
        // exclusion, so if these ever collapse into one entry this fails.
        List<PlatformFeatureCatalog.Entry> dir =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/call/directory/CallDirectory");
        assertEquals(1, dir.size());
        assertFalse(dir.get(0).iosFrameworks().contains("PushKit"));
        assertFalse(dir.get(0).iosFrameworks().contains("AVFoundation"),
                "labelling a number does not carry audio");
        assertTrue(dir.get(0).iosPlistEntries().isEmpty(),
                "labelling a number needs no microphone permission");
    }

    @Test
    void theSharedCallValueTypesCostNothing() {
        // com.codename1.call itself holds only value types. If it ever gained
        // an entry, every app that touched a CallHandle would start paying
        // for CallKit.
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/call/CallHandle").isEmpty());
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/call/CallId").isEmpty());
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/vpn/VpnStatus").isEmpty());
    }

    @Test
    void managedVpnNeedsNoAndroidFloor() {
        // VpnManager is API 30 but is reached reflectively, so the port
        // reports the capability absent below 30 rather than the whole app
        // refusing to install.
        List<PlatformFeatureCatalog.Entry> hits =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/vpn/profile/Vpn");
        assertEquals(1, hits.size());
        assertTrue(hits.get(0).iosFrameworks().contains("NetworkExtension"));
        assertEquals(0, hits.get(0).androidMinimumSdk(),
                "a reflective platform API must not raise the app's floor");
    }

    @Test
    void noCallOrVpnEntryInjectsAnEntitlement() {
        // Both VPN entitlements are single-element arrays, which the
        // ios.entitlements.<key> namespace cannot encode, and the
        // packet-tunnel one must never appear on an App ID that was not
        // granted it -- that fails codesigning with an error naming the
        // entitlement and not the reason it appeared. IPhoneBuilder emits
        // both explicitly; this table must stay out of it.
        String[] prefixes = {"com/codename1/call/session/Calls",
            "com/codename1/call/voip/VoipPush",
            "com/codename1/call/directory/CallDirectory",
            "com/codename1/vpn/profile/Vpn"};
        for (String prefix : prefixes) {
            for (PlatformFeatureCatalog.Entry e
                    : PlatformFeatureCatalog.matchesFor(prefix)) {
                for (String[] plist : e.iosPlistEntries()) {
                    assertFalse(plist[0].startsWith("com.apple.developer"),
                            prefix + " must not inject an entitlement through"
                            + " the plist table: " + plist[0]);
                }
            }
        }
    }

    @Test
    void retiredCn1libsRetainPublishedDependencyMappings() {
        List<PlatformFeatureCatalog.Entry> mlKit =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/ai/mlkit/text/TextRecognizer");
        assertEquals(1, mlKit.size());
        assertTrue(mlKit.get(0).iosPods().contains(
                "GoogleMLKit/TextRecognition"));
        assertTrue(mlKit.get(0).androidGradleDeps().get(0).startsWith(
                "com.google.mlkit:text-recognition:"));

        List<PlatformFeatureCatalog.Entry> tflite =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/ai/tflite/Interpreter");
        assertEquals(1, tflite.size());
        assertFalse(tflite.get(0).iosPods().isEmpty());
        assertFalse(tflite.get(0).iosSpmSpecs().isEmpty());
        assertEquals(2, tflite.get(0).androidGradleDeps().size());
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
    void textScriptSelectorAddsOnlyThatScriptModel() {
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consumeMethod("com/codename1/ai/vision/TextScript", "japanese");

        boolean foundJapanese = false;
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            for (String dependency : e.androidGradleDeps()) {
                foundJapanese |= dependency.startsWith(
                        "com.google.mlkit:text-recognition-japanese:");
                assertFalse(dependency.startsWith(
                        "com.google.mlkit:text-recognition-korean:"),
                        "Unselected script models must not be bundled");
                assertFalse(dependency.startsWith(
                        "com.google.mlkit:text-recognition-chinese:"),
                        "Unselected script models must not be bundled");
            }
            assertTrue(e.iosPods().isEmpty(),
                    "Apple Vision reads the script itself; a script selector "
                            + "alone must not pull an ML Kit pod");
        }
        assertTrue(foundJapanese, "expected the Japanese ML Kit bundle");
    }

    @Test
    void iosScriptModelNeedsBothTheMlKitBackendAndTheScript() {
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consumeMethod("com/codename1/ai/vision/VisionBackends",
                "mlKitTextRecognition");
        assertFalse(hasPod(acc, "GoogleMLKit/TextRecognitionJapanese"),
                "The backend alone must not add a script model");

        acc.consumeMethod("com/codename1/ai/vision/TextScript", "japanese");
        assertTrue(hasPod(acc, "GoogleMLKit/TextRecognitionJapanese"));
        assertFalse(hasPod(acc, "GoogleMLKit/TextRecognitionKorean"));
    }

    private static boolean hasPod(PlatformFeatureCatalog.Accumulator acc,
                                  String pod) {
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            if (e.iosPods().contains(pod)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void currentIosMlKitPodsRequireIos155() {
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consumeMethod("com/codename1/ai/vision/VisionBackends",
                "mlKitTextRecognition");
        acc.consume("com/codename1/ai/language/Translator");
        acc.consume("com/codename1/ai/language/SmartReply");

        int podEntries = 0;
        for (PlatformFeatureCatalog.Entry entry : acc.hits()) {
            if (!entry.iosPods().isEmpty()) {
                podEntries++;
                assertEquals("15.5",
                        entry.iosMinimumDeploymentTarget(),
                        entry.description());
            }
        }
        assertEquals(1, podEntries,
                "Language class references alone must preserve the arm64 simulator");

        acc.consumeMethod("com/codename1/ai/language/LanguageBackends",
                "mlKitTranslation");
        acc.consumeMethod("com/codename1/ai/language/LanguageBackends",
                "mlKitSmartReply");
        podEntries = 0;
        for (PlatformFeatureCatalog.Entry entry : acc.hits()) {
            if (!entry.iosPods().isEmpty()) {
                podEntries++;
                assertEquals("15.5", entry.iosMinimumDeploymentTarget(),
                        entry.description());
            }
        }
        assertEquals(3, podEntries);
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
        assertTrue(e.iosFrameworks().contains("AVFoundation"));
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
        assertEquals(21, acc.minimumAndroidSdk(),
                "ONNX Runtime Android requires API 21");
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
        assertEquals(21, e.androidMinimumSdk(),
                "LiteRT requires Android API 21");
    }

    @Test
    void accumulatorCombinesAndroidFloorAndAppleFrameworks() {
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/DocumentScanner");
        acc.consume("com/codename1/ai/vision/ImageLabeler");

        assertEquals(21, acc.minimumAndroidSdk());
        assertTrue(acc.iosFrameworks().contains("VisionKit"));
        assertTrue(acc.iosFrameworks().contains("CoreML"));
        assertTrue(acc.iosFrameworks().contains("Vision"));
    }

    @Test
    void everyMlKitAndLiteRtDependencyDeclaresAndroidFloor() {
        int checked = 0;
        for (PlatformFeatureCatalog.Entry entry
                : PlatformFeatureCatalog.entries()) {
            for (String dependency : entry.androidGradleDeps()) {
                if (dependency.startsWith("com.google.mlkit:")
                        || dependency.startsWith(
                                "com.google.ai.edge.litert:")
                        || dependency.startsWith(
                                "com.google.android.gms:play-services-mlkit-")
                        || dependency.startsWith(
                                "org.tensorflow:tensorflow-lite")) {
                    checked++;
                    assertEquals(21, entry.androidMinimumSdk(), dependency);
                }
            }
        }
        // 27 since CodeScanner carries its own barcode-scanning entry: the
        // ready-made screen builds a BarcodeScanner internally, and an app
        // that never names BarcodeScanner still needs the artifact.
        assertEquals(27, checked,
                "If an AI dependency is intentionally added or removed, "
                + "update this lock count after verifying its Android floor");
    }

    @Test
    void theReadyMadeScannerCarriesBarcodeScanningAndACamera() {
        // CodeScanner is a BarcodeScanner plus a camera, and an application
        // using it references neither. Without its own entries the generated
        // project keeps the barcode adapter with no artifact to compile it
        // against, and opens a preview on hardware the build never
        // provisioned.
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/CodeScanner");

        Set<String> android = new LinkedHashSet<String>();
        Set<String> permissions = new LinkedHashSet<String>();
        for (PlatformFeatureCatalog.Entry entry : acc.hits()) {
            android.addAll(entry.androidGradleDeps());
            permissions.addAll(entry.androidPermissions());
        }

        assertTrue(android.contains("com.google.mlkit:barcode-scanning:17.2.0"));
        assertTrue(android.contains("androidx.camera:camera-core:1.3.4"));
        assertTrue(permissions.contains("android.permission.CAMERA"));
        assertTrue(acc.iosFrameworks().contains("AVFoundation"));
        assertTrue(acc.iosFrameworks().contains("Vision"));
        assertEquals(21, acc.minimumAndroidSdk());

        // A scanner does not record audio, so it must not drag the microphone
        // permission in the way the general-purpose camera entry does.
        assertFalse(permissions.contains("android.permission.RECORD_AUDIO"));
    }

    @Test
    void theLiveAnalyzerViewCarriesACameraButNoAnalyzer() {
        // VisionCameraView is analyzer-agnostic: the application constructs
        // the analyzer, which is what selects that model. The view itself must
        // add the camera and nothing else.
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/ai/vision/VisionCameraView");

        Set<String> android = new LinkedHashSet<String>();
        for (PlatformFeatureCatalog.Entry entry : acc.hits()) {
            android.addAll(entry.androidGradleDeps());
        }

        assertTrue(android.contains("androidx.camera:camera-core:1.3.4"));
        for (String dependency : android) {
            assertFalse(dependency.startsWith("com.google.mlkit:"),
                    "the view must not pull a model the app never asked for: "
                            + dependency);
        }
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
    void accumulatorFiltersUnrelatedSymbolsAndMemoizesHits() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        for (int i = 0; i < 10000; i++) {
            acc.consume("example/unrelated/Class" + i);
            acc.consumeMethod("example/unrelated/Class" + i, "method" + i);
        }
        Set<PlatformFeatureCatalog.Entry> empty = acc.hits();
        assertTrue(empty.isEmpty());
        assertSame(empty, acc.hits(),
                "Unchanged scans should reuse the memoized result");

        acc.consume("com/codename1/ai/vision/TextRecognizer");
        acc.consume("com/codename1/ai/vision/TextRecognizer");
        assertEquals(1, acc.hits().size());
    }

    @Test
    void catalogUsesOneVersionPerAndroidArtifact() {
        Map<String, String> versions = new HashMap<String, String>();
        for (PlatformFeatureCatalog.Entry entry
                : PlatformFeatureCatalog.entries()) {
            for (String dependency : entry.androidGradleDeps()) {
                String[] parts = dependency.split(":");
                if (parts.length < 3) {
                    continue;
                }
                String artifact = parts[0] + ":" + parts[1];
                String previous = versions.put(artifact, parts[2]);
                if (previous != null) {
                    assertEquals(previous, parts[2],
                            "Version drift for " + artifact);
                }
            }
        }
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

    @Test
    void plainDatabaseUsageDoesNotPullInTheCipher() {
        // Every application that touches a database references com.codename1.db, so keying the
        // entry on the package would bundle SQLCipher for all of them and raise the minimum SDK
        // from 19 to 23 for people who never asked for encryption.
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/db/Database");
        acc.consume("com/codename1/db/Cursor");
        acc.consume("com/codename1/db/Row");
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            for (String gav : e.androidGradleDeps()) {
                assertFalse(gav.contains("sqlcipher"),
                        "plain database usage must not pull in SQLCipher, but got " + gav);
            }
        }
    }

    @Test
    void encryptedDatabaseUsagePullsInTheCipherAndRaisesTheMinimumSdk() {
        PlatformFeatureCatalog.Accumulator acc = new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/db/DatabaseConfig");

        boolean foundCipher = false;
        for (PlatformFeatureCatalog.Entry e : acc.hits()) {
            for (String gav : e.androidGradleDeps()) {
                foundCipher |= gav.contains("net.zetetic:sqlcipher-android");
            }
        }
        assertTrue(foundCipher, "DatabaseConfig must bring in the SQLCipher AAR");
        assertTrue(acc.minimumAndroidSdk() >= 23,
                "SQLCipher requires API 23; the accumulator reported " + acc.minimumAndroidSdk());
    }

    // ------------------------------------------------------------------
    // Nearby devices
    // ------------------------------------------------------------------

    @Test
    void rangingLinksNearbyInteractionAndCarriesBothPrivacyKeys() {
        List<PlatformFeatureCatalog.Entry> hits =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/nearby/ranging/Ranging");
        assertEquals(1, hits.size(), "expected one entry to fire");
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("NearbyInteraction"));
        Set<String> keys = new LinkedHashSet<String>();
        for (String[] entry : e.iosPlistEntries()) {
            keys.add(entry[0]);
        }
        // Both, deliberately: iOS 14 checks the older key before letting a
        // session start, so an app on the supported floor carrying only the
        // newer one was terminated.
        assertTrue(keys.contains("NSNearbyInteractionAllowOnceUsageDescription"));
        assertTrue(keys.contains("NSNearbyInteractionUsageDescription"));
        assertTrue(e.androidGradleDeps().contains("androidx.core.uwb:uwb:1.0.0"));
        assertTrue(e.androidGradleDeps()
                .contains("androidx.core.uwb:uwb-rxjava3:1.0.0"),
                "the Java-facing wrapper is what the port actually consumes");
        assertTrue(e.androidFeatures().contains("android.hardware.uwb"));
        // NOT 31. androidx.core.uwb runs down to 23 and reports the feature
        // absent below 31, so raising the whole app would cost more than the
        // feature is worth.
        assertEquals(23, e.androidMinimumSdk());
        assertTrue(e.androidPermissions().isEmpty(),
                "UWB_RANGING is version-conditional, so it belongs to"
                        + " NearbyManifestFragments and not to this table");
    }

    @Test
    void transportLinksMultipeerAndAsksForTheLocalNetwork() {
        List<PlatformFeatureCatalog.Entry> hits =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/nearby/transport/NearbyTransport");
        assertEquals(1, hits.size());
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("MultipeerConnectivity"));
        assertEquals("NSLocalNetworkUsageDescription",
                e.iosPlistEntries().get(0)[0]);
        // Nearby Connections is added through the builder's own Play-services
        // table, which knows which version this build resolved.
        assertTrue(e.androidGradleDeps().isEmpty());
        // Nearby Connections advertises over BLE, which is API 21. Below that
        // the dependency merges cleanly and the transport never starts, which
        // is the failure worth preventing at build time.
        assertEquals(21, e.androidMinimumSdk());
    }

    @Test
    void companionLinksAccessorySetupKitAndCoreBluetooth() {
        List<PlatformFeatureCatalog.Entry> hits =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/nearby/companion/CompanionDevices");
        assertEquals(1, hits.size());
        PlatformFeatureCatalog.Entry e = hits.get(0);
        assertTrue(e.iosFrameworks().contains("AccessorySetupKit"));
        // CBUUID is what ASDiscoveryDescriptor takes, so the framework that
        // declares it has to be linked too.
        assertTrue(e.iosFrameworks().contains("CoreBluetooth"));
        assertTrue(e.androidFeatures()
                .contains("android.software.companion_device_setup"));
        assertEquals(26, e.androidMinimumSdk());
    }

    @Test
    void eachNearbyPackagePaysOnlyForItself() {
        // The whole reason there are three packages: the scanner matches on a
        // prefix and cannot express an exclusion, so an app that only ranges
        // must not be handed MultipeerConnectivity or the companion feature.
        List<PlatformFeatureCatalog.Entry> ranging =
                PlatformFeatureCatalog.matchesFor(
                        "com/codename1/nearby/ranging/RangingSession");
        for (PlatformFeatureCatalog.Entry e : ranging) {
            assertFalse(e.iosFrameworks().contains("MultipeerConnectivity"));
            assertFalse(e.iosFrameworks().contains("AccessorySetupKit"));
        }
    }

    @Test
    void theSharedNearbyPackageCostsNothing() {
        // com.codename1.nearby itself holds only value types, and referencing
        // it must not pull a framework or a dependency in.
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/nearby/NearbyError").isEmpty());
        assertTrue(PlatformFeatureCatalog.matchesFor(
                "com/codename1/nearby/spi/NearbyBridge").isEmpty());
    }
}
