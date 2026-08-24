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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Central registry that maps class-name prefixes used by the
 * {@code com.codename1.ai.*} family (and the speech/TTS sister APIs
 * in {@code com.codename1.media}) to the native dependencies and
 * permissions each one requires.
 *
 * <p>The build server's class scanners ({@code IPhoneBuilder} and
 * {@code AndroidGradleBuilder}) call into this table from inside their
 * existing {@code Executor.ClassScanner.usesClass(String)} blocks; the
 * resulting set of {@link Entry} records is then applied just before
 * iOS pods / SPM are resolved and just before the Android Gradle
 * dependencies / manifest fragments are written.</p>
 *
 * <p>Keep this table small and declarative: any class prefix whose
 * needs change (different pod version, additional plist entry) should
 * be edited here, not in the builder hot loop.</p>
 */
public final class PlatformFeatureCatalog {

    private static final String MLKIT_LANGUAGE_ID =
            "com.google.mlkit:language-id:17.0.6";
    private static final String MLKIT_TRANSLATE =
            "com.google.mlkit:translate:17.0.3";
    private static final String MLKIT_SMART_REPLY =
            "com.google.mlkit:smart-reply:17.0.4";
    private static final String MLKIT_SELFIE_SEGMENTATION =
            "com.google.mlkit:segmentation-selfie:16.0.0-beta5";
    /** Shared version of the per-script ML Kit text recognition bundles. */
    private static final String MLKIT_TEXT_SCRIPT_VERSION = "16.0.1";
    private static final List<Entry> ENTRIES;
    private static final List<String> CLASS_PREFIXES;
    private static final Set<String> METHOD_KEYS;

    static {
        List<Entry> e = new ArrayList<Entry>();

        // LLM clients: pure HTTPS. INTERNET is on by default on
        // Android so no permission needed; we still register the
        // entry so the scanner has a positive hit for diagnostics.
        e.add(new Entry("com/codename1/ai/LlmClient")
                .description("LLM client (OpenAI / Anthropic / Gemini / Ollama)"));
        e.add(new Entry("com/codename1/ai/OpenAiClient").description("OpenAI client"));
        e.add(new Entry("com/codename1/ai/AnthropicClient").description("Anthropic client"));
        e.add(new Entry("com/codename1/ai/GeminiClient").description("Gemini client"));

        // Core speech recognition: iOS Speech framework + mic & speech plist
        // strings; Android record-audio permission. The TTS API has no
        // plist requirement (AVSpeech is unrestricted) and no Android
        // permission (built-in).
        e.add(new Entry("com/codename1/media/SpeechRecognizer")
                .iosFrameworks("Speech", "AVFoundation")
                .iosPlist("NSSpeechRecognitionUsageDescription",
                         "Used to transcribe your voice into text.")
                .iosPlist("NSMicrophoneUsageDescription",
                         "Required to capture audio for speech recognition.")
                .androidPermissions("android.permission.RECORD_AUDIO")
                .description("On-device speech-to-text"));

        e.add(new Entry("com/codename1/media/TextToSpeech")
                .iosFrameworks("AVFoundation")
                .description("Text-to-speech"));

        // Compatibility mappings for the retired AI cn1libs. The artifacts
        // already published with these package names remain usable even
        // though new applications should use the built-in vision, language,
        // and inference APIs below.
        e.add(new Entry("com/codename1/ai/mlkit/text/")
                .iosPod("GoogleMLKit/TextRecognition")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle("com.google.mlkit:text-recognition:16.0.0")
                .androidMinimumSdk(21)
                .iosPlist("NSCameraUsageDescription",
                         "Used to recognise text from your camera.")
                .description("Legacy ML Kit Text Recognition cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/barcode/")
                .iosPod("GoogleMLKit/BarcodeScanning")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle("com.google.mlkit:barcode-scanning:17.2.0")
                .androidMinimumSdk(21)
                .iosPlist("NSCameraUsageDescription",
                         "Used to scan barcodes with your camera.")
                .androidFeatures("android.hardware.camera")
                .description("Legacy ML Kit Barcode Scanning cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/face/")
                .iosPod("GoogleMLKit/FaceDetection")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle("com.google.mlkit:face-detection:16.1.5")
                .androidMinimumSdk(21)
                .iosPlist("NSCameraUsageDescription",
                         "Used to detect faces in images.")
                .description("Legacy ML Kit Face Detection cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/labeling/")
                .iosPod("GoogleMLKit/ImageLabeling")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle("com.google.mlkit:image-labeling:17.0.7")
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Image Labeling cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/translate/")
                .iosPod("GoogleMLKit/Translate")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle(MLKIT_TRANSLATE)
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Translation cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/smartreply/")
                .iosPod("GoogleMLKit/SmartReply")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle(MLKIT_SMART_REPLY)
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Smart Reply cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/langid/")
                .iosPod("GoogleMLKit/LanguageID")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle(MLKIT_LANGUAGE_ID)
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Language ID cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/pose/")
                .iosPod("GoogleMLKit/PoseDetection")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle("com.google.mlkit:pose-detection:18.0.0-beta3")
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Pose Detection cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/segmentation/")
                .iosPod("GoogleMLKit/SegmentationSelfie")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .androidGradle(MLKIT_SELFIE_SEGMENTATION)
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Selfie Segmentation cn1lib"));
        e.add(new Entry("com/codename1/ai/mlkit/docscan/")
                .iosPod("GoogleMLKit/DocumentScanner")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .iosFrameworks("VisionKit")
                .androidGradle(
                        "com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
                .androidMinimumSdk(21)
                .description("Legacy ML Kit Document Scanner cn1lib"));
        e.add(new Entry("com/codename1/ai/tflite/")
                .iosPod("TensorFlowLiteSwift")
                .iosSpm("TensorFlowLiteSwift",
                        "https://github.com/tensorflow/tensorflow.git",
                        "from:2.13.0", "TensorFlowLite")
                .androidGradle("org.tensorflow:tensorflow-lite:2.13.0")
                .androidGradle(
                        "org.tensorflow:tensorflow-lite-support:0.4.4")
                .androidMinimumSdk(21)
                .description("Legacy TensorFlow Lite interpreter cn1lib"));

        // Built-in vision APIs. Android uses ML Kit by default. iOS uses
        // Apple Vision/VisionKit unless VisionBackends.mlKit() is selected.
        // The compound entries require both the feature and selector method,
        // so iOS only bundles ML Kit pods for features actually used.
        e.add(new Entry("com/codename1/ai/vision/TextRecognizer")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:text-recognition:16.0.0")
                .androidMinimumSdk(21)
                .description("Text recognition"));
        e.add(new Entry("com/codename1/ai/vision/TextRecognizer")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitTextRecognition")
                .iosPod("GoogleMLKit/TextRecognition")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS text-recognition backend"));

        // Non-Latin OCR. ML Kit ships one recognizer artifact per script, so a
        // script is a dependency of its own rather than a runtime flag; Latin
        // needs no entry because it is what the base recognizer already reads.
        // On iOS the script model is only fetched for a build that also
        // selected the ML Kit backend -- Apple Vision reads these scripts
        // itself through its recognition languages.
        for (String[] script : new String[][] {
                {"chinese", "Chinese"},
                {"devanagari", "Devanagari"},
                {"japanese", "Japanese"},
                {"korean", "Korean"}}) {
            e.add(new Entry("com/codename1/ai/vision/TextRecognizer")
                    .requiresMethod("com/codename1/ai/vision/TextScript",
                            script[0])
                    .androidGradle("com.google.mlkit:text-recognition-"
                            + script[0] + ":" + MLKIT_TEXT_SCRIPT_VERSION)
                    .androidMinimumSdk(21)
                    .description(script[1] + " text recognition"));
            e.add(new Entry("com/codename1/ai/vision/TextRecognizer")
                    .requiresMethod("com/codename1/ai/vision/VisionBackends",
                            "mlKitTextRecognition")
                    .requiresMethod("com/codename1/ai/vision/TextScript",
                            script[0])
                    .iosPod("GoogleMLKit/TextRecognition" + script[1])
                    .iosMinimumDeploymentTarget("15.5")
                    .iosDependenciesUnsupportedOnMacCatalyst()
                    .iosDependenciesUnsupportedOnArm64Simulator()
                    .description("ML Kit iOS " + script[1]
                            + " text-recognition model"));
        }

        e.add(new Entry("com/codename1/ai/vision/BarcodeScanner")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:barcode-scanning:17.2.0")
                .androidMinimumSdk(21)
                .description("Barcode scanning"));
        e.add(new Entry("com/codename1/ai/vision/BarcodeScanner")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitBarcodeScanning")
                .iosPod("GoogleMLKit/BarcodeScanning")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS barcode backend"));

        // CodeScanner is the ready-made scanner screen and builds a
        // BarcodeScanner internally. An application that references only the
        // high-level class never names BarcodeScanner, so without its own
        // entry the generated Android project retains the barcode adapter
        // source with no com.google.mlkit:barcode-scanning to compile it
        // against. The camera half is registered further down, beside the
        // low-level camera entry.
        e.add(new Entry("com/codename1/ai/vision/CodeScanner")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:barcode-scanning:17.2.0")
                .androidMinimumSdk(21)
                .description("Barcode scanning (ready-made scanner screen)"));
        e.add(new Entry("com/codename1/ai/vision/CodeScanner")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitBarcodeScanning")
                .iosPod("GoogleMLKit/BarcodeScanning")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS barcode backend"
                        + " (ready-made scanner screen)"));

        e.add(new Entry("com/codename1/ai/vision/FaceDetector")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:face-detection:16.1.5")
                .androidMinimumSdk(21)
                .description("Face detection"));
        e.add(new Entry("com/codename1/ai/vision/FaceDetector")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitFaceDetection")
                .iosPod("GoogleMLKit/FaceDetection")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS face-detection backend"));

        e.add(new Entry("com/codename1/ai/vision/ImageLabeler")
                .iosFrameworks("Vision", "CoreML")
                .androidGradle("com.google.mlkit:image-labeling:17.0.7")
                .androidMinimumSdk(21)
                .description("Image labeling"));
        e.add(new Entry("com/codename1/ai/vision/ImageLabeler")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitImageLabeling")
                .iosPod("GoogleMLKit/ImageLabeling")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS image-labeling backend"));

        e.add(new Entry("com/codename1/ai/vision/PoseDetector")
                .iosFrameworks("Vision", "CoreML")
                .androidGradle("com.google.mlkit:pose-detection:18.0.0-beta3")
                .androidMinimumSdk(21)
                .description("Pose detection"));
        e.add(new Entry("com/codename1/ai/vision/PoseDetector")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitPoseDetection")
                .iosPod("GoogleMLKit/PoseDetection")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS pose-detection backend"));

        e.add(new Entry("com/codename1/ai/vision/SelfieSegmenter")
                .iosFrameworks("Vision", "CoreML")
                .androidGradle(MLKIT_SELFIE_SEGMENTATION)
                .androidMinimumSdk(21)
                .description("Selfie segmentation"));
        e.add(new Entry("com/codename1/ai/vision/SelfieSegmenter")
                .requiresMethod("com/codename1/ai/vision/VisionBackends",
                        "mlKitSelfieSegmentation")
                .iosPod("GoogleMLKit/SegmentationSelfie")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS selfie-segmentation backend"));

        e.add(new Entry("com/codename1/ai/vision/DocumentScanner")
                .iosFrameworks("VisionKit", "Vision", "CoreImage")
                .description("Still-image document correction (Apple platforms)"));

        // The common inference API is backed by LiteRT/TensorFlow Lite.
        // iOS may select the Core ML delegate without changing
        // model formats.
        e.add(new Entry("com/codename1/ai/inference/InferenceSession")
                .iosPod("TensorFlowLiteObjC/CoreML")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosFrameworks("CoreML", "Metal", "Accelerate")
                .androidGradle("com.google.ai.edge.litert:litert:1.0.1")
                .androidMinimumSdk(21)
                .description("LiteRT inference"));

        e.add(new Entry("com/codename1/ai/language/LanguageIdentifier")
                .iosFrameworks("NaturalLanguage")
                .androidGradle(MLKIT_LANGUAGE_ID)
                .androidMinimumSdk(21)
                .description("On-device language identification"));
        e.add(new Entry("com/codename1/ai/language/LanguageIdentifier")
                .requiresMethod("com/codename1/ai/language/LanguageBackends",
                        "mlKitLanguageIdentification")
                .iosPod("GoogleMLKit/LanguageID")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS language-identification backend"));
        // CN1Language.m contains the dependency-free Apple identifier beside
        // the ML Kit adapters, so every target that compiles this source must
        // link the small system NaturalLanguage framework.
        e.add(new Entry("com/codename1/ai/language/Translator")
                .iosFrameworks("NaturalLanguage")
                .androidGradle(MLKIT_TRANSLATE)
                .androidMinimumSdk(21)
                .description("On-device translation"));
        e.add(new Entry("com/codename1/ai/language/Translator")
                .requiresMethod("com/codename1/ai/language/LanguageBackends",
                        "mlKitTranslation")
                .iosPod("GoogleMLKit/Translate")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS translation backend"));
        e.add(new Entry("com/codename1/ai/language/SmartReply")
                .iosFrameworks("NaturalLanguage")
                .androidGradle(MLKIT_SMART_REPLY)
                .androidMinimumSdk(21)
                .description("On-device smart reply"));
        e.add(new Entry("com/codename1/ai/language/SmartReply")
                .requiresMethod("com/codename1/ai/language/LanguageBackends",
                        "mlKitSmartReply")
                .iosPod("GoogleMLKit/SmartReply")
                .iosMinimumDeploymentTarget("15.5")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .iosDependenciesUnsupportedOnArm64Simulator()
                .description("ML Kit iOS Smart Reply backend"));

        e.add(new Entry("com/codename1/ai/whisper/")
                .iosFrameworks("Accelerate")
                .description("On-device Whisper transcription (libwhisper.a ships with the cn1lib)"));

        // Low-level cross-platform camera API: live preview + frame
        // stream + photo + video. iOS uses AVFoundation (framework
        // only, no pod); Android uses CameraX (androidx.camera) which
        // is added as Gradle deps below. Just referencing classes in
        // com.codename1.camera causes the build to inject the right
        // permissions and plist strings; developers may still override
        // the plist text via the ios.NSCameraUsageDescription build
        // hint.
        e.add(new Entry("com/codename1/camera/")
                .iosFrameworks("AVFoundation", "CoreMedia", "CoreVideo")
                .iosPlist("NSCameraUsageDescription",
                         "Used to capture photos and video.")
                .iosPlist("NSMicrophoneUsageDescription",
                         "Used to capture audio for video recording.")
                .androidPermissions("android.permission.CAMERA",
                                    "android.permission.RECORD_AUDIO")
                .androidFeatures("android.hardware.camera",
                                 "android.hardware.camera.autofocus")
                .androidGradle("androidx.camera:camera-core:1.3.4")
                .androidGradle("androidx.camera:camera-camera2:1.3.4")
                .androidGradle("androidx.camera:camera-lifecycle:1.3.4")
                .androidGradle("androidx.camera:camera-view:1.3.4")
                .androidGradle("androidx.camera:camera-video:1.3.4")
                .androidMinimumSdk(21)
                .description("Cross-platform camera (preview + frames + photo + video)"));

        // CodeScanner and VisionCameraView drive the camera themselves. An
        // application using one of them never references
        // com.codename1.camera, so without these entries it gets no CameraX,
        // no CAMERA permission and no AVFoundation or privacy string, and the
        // preview opens on hardware the build never provisioned.
        //
        // These deliberately do NOT request the microphone: both classes open
        // their session with captureAudio(false), and a barcode scanner asking
        // for RECORD_AUDIO is a privacy smell and an app-review question. The
        // CameraX artifact list mirrors the low-level entry rather than
        // trimming to preview-only, because AndroidCameraImpl reflects over
        // the whole CameraX surface in one class.
        String[][] cameraBackedVision = {
            {"com/codename1/ai/vision/CodeScanner",
             "ready-made barcode scanner screen"},
            {"com/codename1/ai/vision/VisionCameraView",
             "live analyzer camera preview"},
        };
        for (String[] cameraBacked : cameraBackedVision) {
            e.add(new Entry(cameraBacked[0])
                    .iosFrameworks("AVFoundation", "CoreMedia", "CoreVideo")
                    .iosPlist("NSCameraUsageDescription",
                             "Used to analyze the camera image on this device.")
                    .androidPermissions("android.permission.CAMERA")
                    .androidFeatures("android.hardware.camera",
                                     "android.hardware.camera.autofocus")
                    .androidGradle("androidx.camera:camera-core:1.3.4")
                    .androidGradle("androidx.camera:camera-camera2:1.3.4")
                    .androidGradle("androidx.camera:camera-lifecycle:1.3.4")
                    .androidGradle("androidx.camera:camera-view:1.3.4")
                    .androidGradle("androidx.camera:camera-video:1.3.4")
                    .androidMinimumSdk(21)
                    .description("Camera for the " + cameraBacked[1]));
        }

        // First-class Bluetooth (com.codename1.bluetooth.*): CoreBluetooth
        // on iOS with the two privacy strings defaulted only-if-unset via
        // the standard entry application. Android permissions are
        // deliberately NOT listed here -- the Android 12 permission split
        // needs maxSdkVersion / usesPermissionFlags attributes this table
        // cannot express, so AndroidGradleBuilder injects nuanced manifest
        // fragments through BluetoothManifestFragments instead. The
        // framework linking + CN1_INCLUDE_BLUETOOTH define flip likewise
        // happen in IPhoneBuilder (iosFrameworks is documentation-only).
        e.add(new Entry("com/codename1/bluetooth/")
                .iosFrameworks("CoreBluetooth")
                .iosPlist("NSBluetoothAlwaysUsageDescription",
                         "Communicates with nearby Bluetooth accessories.")
                .iosPlist("NSBluetoothPeripheralUsageDescription",
                         "Communicates with nearby Bluetooth accessories.")
                .description("Cross-platform Bluetooth (BLE central/peripheral, L2CAP, classic RFCOMM)"));

        // First-class health data (com.codename1.health.*): HealthKit on
        // iOS, Health Connect on Android.
        //
        // NOTE the two deliberate omissions, both of which a future
        // maintainer will be tempted to "fix" because every neighbouring
        // entry has them:
        //
        //  - NO iosPlist defaults. Unlike camera or bluetooth we do NOT
        //    inject placeholder NSHealth*UsageDescription strings. Apple
        //    reviews health privacy copy against what the app actually
        //    does, so a generic placeholder is precisely what gets an app
        //    rejected -- it would not even achieve the "keeps the build
        //    working" goal it was added for. IPhoneBuilder fails the build
        //    with an actionable message instead.
        //  - NO androidPermissions. Health Connect permissions are
        //    per-data-type and the type an app uses is a field reference,
        //    which the class scanner cannot see (Executor.visitFieldInsn is
        //    an empty override). They come from the android.health.read /
        //    android.health.write build hints via HealthManifestFragments.
        //  - NO androidGradle either, and this one is subtle. Entries match
        //    with startsWith, so this prefix ALSO matches
        //    com/codename1/health/sensors/ -- there is no way to say
        //    "except that subpackage". The sensor layer is pure
        //    com.codename1.bluetooth.le and needs no androidx.health at
        //    all, so putting the dependency here would add Health Connect
        //    (and, on the Play side, a health-permissions review) to an app
        //    that only reads a heart-rate strap. AndroidGradleBuilder adds
        //    it instead, gated on the scanner having seen health classes
        //    OUTSIDE the sensors subpackage.
        //
        //  - and NO iosFrameworks, for the same startsWith reason. The
        //    frameworks named here are linked for real -- IPhoneBuilder
        //    appends every matched entry's list to ios.add_libs -- so
        //    naming HealthKit would link it into an app that only reads a
        //    heart-rate strap, and Apple rejects a binary that links
        //    HealthKit without the health purpose strings a sensor-only app
        //    has no reason to declare. IPhoneBuilder links it under the
        //    usesHealthStore gate instead, which is the one that knows the
        //    difference.
        //
        // The CN1_INCLUDE_HEALTH define flip likewise happens in
        // IPhoneBuilder, gated the same way.
        e.add(new Entry("com/codename1/health/")
                .description("Cross-platform health data (samples, aggregates, background observers, workouts)"));

        // The health sensor layer talks to standard GATT devices over
        // com.codename1.bluetooth.le, so it needs the Bluetooth privacy
        // string whether or not the app also uses a health store. This
        // entry is additive on top of the one above and is safe for both
        // cases, unlike anything HealthKit- or Health-Connect-specific.
        e.add(new Entry("com/codename1/health/sensors/")
                .iosFrameworks("CoreBluetooth")
                .iosPlist("NSBluetoothAlwaysUsageDescription",
                         "Communicates with nearby heart rate and fitness sensors.")
                // The iOS 12 key as well, exactly as the Bluetooth entry
                // above carries it. NSBluetoothAlwaysUsageDescription
                // arrived in iOS 13, and iOS 12 checks the older key
                // before letting CoreBluetooth start -- so a sensor-only
                // app naming this facade rather than com.codename1.bluetooth
                // was terminated on the supported floor.
                .iosPlist("NSBluetoothPeripheralUsageDescription",
                         "Communicates with nearby heart rate and fitness sensors.")
                .description("Bluetooth health sensors (heart rate, power, cadence, scales, cuffs, glucose)"));

        // Smart home (com.codename1.home.*): HomeKit on Apple platforms,
        // Google Play services Matter commissioning on Android.
        //
        // NOTE three deliberate omissions.
        //
        //  - NO iosPlist default for NSHomeKitUsageDescription. Same reason
        //    the health entry injects no purpose string: Apple reviews this
        //    text against what the app actually does, so a generic
        //    placeholder is precisely what gets an app rejected. iOS also
        //    terminates an app that reaches HomeKit without one, so a
        //    placeholder would not even achieve the "keeps the build working"
        //    goal. IPhoneBuilder fails the build with an actionable message.
        //
        //  - NO androidPermissions. Play services runs the whole add-device
        //    interaction in its OWN activity, so the app needs no Bluetooth,
        //    location or local-network permission -- and the AAR's manifest
        //    declares none, which is the authority here. Adding them "to be
        //    safe" would put a Bluetooth permission prompt in front of users
        //    of an app that never scans.
        //
        //  - The HomeKit framework linkage and the CN1_INCLUDE_HOMEKIT define
        //    flip happen in IPhoneBuilder, because the entitlement decision is
        //    tied to them and has to distinguish an app that touches
        //    accessories from one that only asks whether HomeKit exists.
        //    iosFrameworks here is documentation, matching the bluetooth and
        //    health entries above.
        // No iosFrameworks: IPhoneBuilder links HomeKit itself, under the
        // same scan, and MatterSupport under a gate this table cannot
        // express -- the ios.home.commissioning=false opt-out. A framework
        // named here is linked for real, so a second copy of the decision
        // here would link MatterSupport into a build that opted out.
        //
        // And no androidGradle or androidMinimumSdk either, for the startsWith
        // reason the health entry above spells out: this prefix also covers
        // com/codename1/home/commissioning/SetupPayload, which is a pure-Java
        // parser for the string on an accessory's sticker. Naming
        // play-services-home here would put a Play Services AAR -- and the
        // API 21 floor that comes with it -- into an app that only checks
        // whether a scanned code is well formed. AndroidGradleBuilder adds
        // both inside the usesSmartHome gate, which is the one that knows the
        // difference, and where the delegate that imports the AAR is injected.
        e.add(new Entry("com/codename1/home/")
                .description("Smart home accessories (HomeKit, Matter, "
                        + "Google Home)"));

        // Adding a Matter accessory. Its own entry, and its own package on the
        // Java side, because on iOS it is far more expensive than the rest:
        // the MatterSupport framework, a com.apple.developer.matter
        // .allow-setup-payload entitlement, an app group and a whole generated
        // app-extension target. An app that only reads its lights should carry
        // none of that, and since entries match on a prefix with no way to
        // express an exclusion, the package boundary has to BE the permission
        // boundary.
        //
        // The deployment floor is real: MatterSupport arrived in iOS 16.1 and
        // linking it below that fails at launch rather than at build time.
        // And no iosMinimumDeploymentTarget either, for the same reason as
        // the frameworks above: raising the app's floor to what MatterSupport
        // needs is only right when MatterSupport is actually being linked,
        // and ios.home.commissioning=false is invisible from here.
        // IPhoneBuilder raises it inside that gate.
        e.add(new Entry("com/codename1/home/commissioning/")
                .description("Matter accessory commissioning"));

        // On-device Stable Diffusion: bundled Core ML model on iOS,
        // ONNX runtime on Android. Flag the >2 GB upload concern so
        // the cloud build server can abort early with a helpful
        // message.
        e.add(new Entry("com/codename1/ai/imagegen/StableDiffusion")
                .iosFrameworks("CoreML", "Vision")
                .androidGradle("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
                .androidMinimumSdk(21)
                .markBigUpload()
                .description("On-device Stable Diffusion (local-build only)"));

        // Cross-platform augmented reality (com.codename1.ar): ARKit +
        // SceneKit on iOS (linked explicitly by IPhoneBuilder, gated by
        // the INCLUDE_CN1_AR define since neither is default-linked),
        // Google Play Services for AR (ARCore) on Android. The camera
        // permission and plist string ride along because AR always
        // drives the camera. The com.google.ar.core meta-data marks
        // ARCore optional so the app still installs on non-AR devices;
        // the android.ar.required=true build hint flips it to required.
        // Encrypted databases. Keyed on DatabaseConfig rather than on the db package: every
        // application that uses a database references com.codename1.db, but only the ones that
        // encrypt reference DatabaseConfig, and SQLCipher's minimum SDK is above ours.
        e.add(new Entry("com/codename1/db/DatabaseConfig")
                .androidGradle("net.zetetic:sqlcipher-android:4.17.0@aar")
                .androidGradle("androidx.sqlite:sqlite:2.4.0")
                .androidMinimumSdk(23)
                .description("Encrypted SQLite databases (SQLCipher)"));

        // Nearby devices (com.codename1.nearby.*). Three entries, because
        // the three packages cost three different things and the scanner
        // matches on a prefix with no way to express an exclusion -- so the
        // package boundary is the only opt-in a developer performs.
        //
        // NOTE the Android permissions are deliberately NOT listed on any of
        // these. UWB_RANGING exists only from API 31, and the transport needs
        // the Android 12 Bluetooth split with maxSdkVersion caps and
        // usesPermissionFlags="neverForLocation" -- attributes this table
        // cannot express. NearbyManifestFragments injects all of them
        // instead, exactly as BluetoothManifestFragments does.
        //
        // The three CN1_NEARBY_* define flips likewise happen in
        // IPhoneBuilder, which is also where the AccessorySetupKit plist
        // arrays and the optional nearby-interaction entitlement live.
        e.add(new Entry("com/codename1/nearby/ranging/")
                .iosFrameworks("NearbyInteraction")
                // Both keys. NSNearbyInteractionUsageDescription is the iOS 14
                // form and NSNearbyInteractionAllowOnceUsageDescription the
                // iOS 15 one; iOS 14 checks the older key before letting a
                // session start, so an app on the supported floor that carried
                // only the newer one was terminated. The Bluetooth entry above
                // carries both of its own keys for the same reason.
                .iosPlist("NSNearbyInteractionAllowOnceUsageDescription",
                         "Measures how far away a nearby device is.")
                .iosPlist("NSNearbyInteractionUsageDescription",
                         "Measures how far away a nearby device is.")
                .androidGradle("androidx.core.uwb:uwb:1.0.0")
                // The Java-facing wrapper. The base library is Kotlin
                // coroutines -- prepareSession returns a Flow -- and the port
                // is Java, so AndroidUwbRanging consumes the Observable this
                // provides instead of hand-writing a Continuation.
                .androidGradle("androidx.core.uwb:uwb-rxjava3:1.0.0")
                // Declared optional, so the app still installs on the many
                // devices with no UWB radio. Ranging.isSupported() is what an
                // app branches on there.
                .androidFeatures("android.hardware.uwb")
                // The AAR's own floor. NOT 31, which is where UWB_RANGING and
                // the platform UwbManager arrive: androidx.core.uwb runs down
                // to 23 and reports the feature absent below 31, so raising
                // the whole app's minSdk to 31 would cost far more than the
                // feature is worth.
                .androidMinimumSdk(23)
                .description("Ultra-wideband precision ranging"));

        e.add(new Entry("com/codename1/nearby/transport/")
                .iosFrameworks("MultipeerConnectivity")
                .iosPlist("NSLocalNetworkUsageDescription",
                         "Finds and connects to nearby devices running this"
                         + " app.")
                // 21, and for the API rather than the artifact. It was
                // suggested play-services-nearby 18.4.0 forces 23; it does
                // not -- that AAR declares minSdkVersion 14, and so does
                // every artifact in its transitive closure
                // (play-services-base, -basement, -tasks, androidx.core
                // 1.0.0), so no manifest merger rejects the builder's
                // default of 19. What genuinely needs a floor is Nearby
                // Connections itself: it advertises over BLE, which is API
                // 21, and the newer play-services-nearby an app may resolve
                // declares 21 too. Below that the dependency merges cleanly
                // and the transport simply never starts.
                .androidMinimumSdk(21)
                .description("Nearby device-to-device transport"));

        e.add(new Entry("com/codename1/nearby/companion/")
                // AccessorySetupKit is iOS 18 and CoreBluetooth carries the
                // CBUUID its discovery descriptor takes. Naming a framework
                // newer than the deployment target is safe: its headers are
                // availability-annotated, so clang weak-imports the symbols
                // and the @available guards in CN1Nearby.m keep an older OS
                // from touching them.
                .iosFrameworks("AccessorySetupKit", "CoreBluetooth")
                .androidFeatures("android.software.companion_device_setup")
                .androidMinimumSdk(26)
                .description("Companion-device association and presence"));

        e.add(new Entry("com/codename1/ar/")
                .iosFrameworks("ARKit", "SceneKit")
                .iosPlist("NSCameraUsageDescription",
                         "Used to display augmented reality content.")
                .androidGradle("com.google.ar:core:1.44.0")
                .androidPermissions("android.permission.CAMERA")
                .androidFeatures("android.hardware.camera.ar")
                .androidMetaData("com.google.ar.core", "optional")
                .description("Cross-platform augmented reality (world/image/face tracking)"));

        ENTRIES = Collections.unmodifiableList(e);
        Set<String> classPrefixes = new LinkedHashSet<String>();
        Set<String> methodKeys = new LinkedHashSet<String>();
        for (Entry entry : e) {
            classPrefixes.add(entry.classPrefix);
            methodKeys.addAll(entry.methodRequirements());
        }
        CLASS_PREFIXES = Collections.unmodifiableList(
                new ArrayList<String>(classPrefixes));
        METHOD_KEYS = Collections.unmodifiableSet(methodKeys);
    }

    private PlatformFeatureCatalog() {
    }

    /**
     * Returns every registered platform feature in declaration order.
     *
     * @return immutable catalog entry list used by builders and tooling
     */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * Returns every entry whose {@link Entry#classPrefix} matches the
     * given internal-form class name (slashes, not dots). When the
     * prefix ends with a slash, package-prefix matching is used;
     * otherwise an exact class match is required.
     *
     * @param internalClassName JVM internal-form class name
     * @return immutable matching entries that have no method requirement
     */
    public static List<Entry> matchesFor(String internalClassName) {
        if (internalClassName == null) {
            return Collections.emptyList();
        }
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : ENTRIES) {
            if (e.matchesClass(internalClassName) && !e.hasMethodRequirement()) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * Builder/scanner output: the de-duplicated union of entries whose class
     * and optional method requirements were observed.
     */
    public static final class Accumulator {
        private final Set<String> classes = new LinkedHashSet<String>();
        private final Set<String> methods = new LinkedHashSet<String>();
        private Set<Entry> cachedHits = Collections.emptySet();
        private boolean dirty = true;

        /**
         * Records a class only when it can match at least one catalog entry.
         * The scanner calls this for the full application and framework
         * graph, so filtering here keeps memory proportional to catalog usage
         * rather than application size.
         *
         * @param internalClassName JVM internal-form class name
         */
        public void consume(String internalClassName) {
            if (isCatalogClass(internalClassName)
                    && classes.add(internalClassName)) {
                dirty = true;
            }
        }

        /**
         * Records a method reference only when an entry requires that exact
         * owner and method name.
         *
         * @param internalClassName JVM internal-form owner name
         * @param methodName referenced method name
         */
        public void consumeMethod(String internalClassName, String methodName) {
            if (internalClassName != null && methodName != null) {
                String key = internalClassName + "#" + methodName;
                if (METHOD_KEYS.contains(key) && methods.add(key)) {
                    dirty = true;
                }
            }
        }

        /**
         * Returns the immutable matched-entry set. The result is recomputed
         * only after a relevant class or method is newly observed.
         *
         * @return matched catalog entries in declaration order
         */
        public Set<Entry> hits() {
            if (!dirty) {
                return cachedHits;
            }
            Set<Entry> hits = new LinkedHashSet<Entry>();
            for (Entry entry : ENTRIES) {
                if (entry.requirementsMet(classes, methods)) {
                    hits.add(entry);
                }
            }
            cachedHits = Collections.unmodifiableSet(hits);
            dirty = false;
            return cachedHits;
        }

        /**
         * Tests whether any observed feature requires the builder's larger
         * upload allowance.
         *
         * @return {@code true} when at least one matched entry is marked as a
         *         large upload
         */
        public boolean anyRequiresBigUpload() {
            for (Entry e : hits()) {
                if (e.requiresBigUpload) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the highest Android API level required by the matched
         * catalog entries.
         *
         * <p>Builders should combine this value with the application's
         * {@code android.min_sdk_version} and retain the larger value before
         * writing either the manifest or Gradle configuration. This prevents
         * Android's manifest merger from rejecting a dependency whose own
         * minimum SDK is newer than Codename One's default.</p>
         *
         * @return the required Android API level, or {@code 0} when none of
         * the matched entries imposes an additional minimum
         */
        public int minimumAndroidSdk() {
            int minimum = 0;
            for (Entry entry : hits()) {
                minimum = Math.max(minimum, entry.androidMinimumSdk());
            }
            return minimum;
        }

        /**
         * Returns the de-duplicated Apple system frameworks required by the
         * matched entries.
         *
         * <p>Names are returned without the {@code .framework} suffix, exactly
         * as they appear in the catalog. Builders can append the suffix while
         * merging these values into an application's existing library list.
         * The returned set is immutable.</p>
         *
         * @return the framework names required by the observed API usage
         */
        public Set<String> iosFrameworks() {
            Set<String> frameworks = new LinkedHashSet<String>();
            for (Entry entry : hits()) {
                frameworks.addAll(entry.iosFrameworks());
            }
            return Collections.unmodifiableSet(frameworks);
        }
    }

    private static boolean isCatalogClass(String internalClassName) {
        if (internalClassName == null) {
            return false;
        }
        for (String prefix : CLASS_PREFIXES) {
            if (prefix.endsWith("/")) {
                if (internalClassName.startsWith(prefix)) {
                    return true;
                }
            } else if (internalClassName.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A single registry record. Mutable while the table is being
     * built (the fluent setters); semantically immutable once exposed
     * via {@link #entries()}.
     */
    public static final class Entry {
        private final String classPrefix;
        /**
         * Method requirements as {@code owner#name} keys. Every key must be
         * observed for the entry to fire, so an entry can name a combination
         * such as "the ML Kit text backend <em>and</em> the Japanese script"
         * without also firing for either one alone.
         */
        private final List<String> methodKeys = new ArrayList<String>();
        private final List<String> iosPods = new ArrayList<String>();
        private final List<IosSpm> iosSpm = new ArrayList<IosSpm>();
        private final List<String> iosFrameworks = new ArrayList<String>();
        private final List<String[]> iosPlist = new ArrayList<String[]>();
        private final List<String> androidGradle = new ArrayList<String>();
        private final List<String> androidPermissions = new ArrayList<String>();
        private final List<String> androidFeatures = new ArrayList<String>();
        private final List<String[]> androidMetaData = new ArrayList<String[]>();
        private int androidMinimumSdk;
        private boolean iosDependenciesSupportMacCatalyst = true;
        private boolean iosDependenciesSupportArm64Simulator = true;
        private String iosMinimumDeploymentTarget;
        private boolean requiresBigUpload;
        private String description = "";

        Entry(String classPrefix) {
            this.classPrefix = classPrefix;
        }

        boolean matchesClass(String internalClassName) {
            if (classPrefix.endsWith("/")) {
                return internalClassName.startsWith(classPrefix);
            }
            return internalClassName.equals(classPrefix);
        }

        boolean hasMethodRequirement() {
            return !methodKeys.isEmpty();
        }

        List<String> methodRequirements() {
            return Collections.unmodifiableList(methodKeys);
        }

        boolean requirementsMet(Set<String> classes, Set<String> methods) {
            boolean classSeen = false;
            for (String cls : classes) {
                if (matchesClass(cls)) {
                    classSeen = true;
                    break;
                }
            }
            if (!classSeen) {
                return false;
            }
            for (String key : methodKeys) {
                if (!methods.contains(key)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Adds a required method reference. Calling this more than once makes
         * every named method required, not any of them.
         *
         * @param owner internal-form declaring class of the method
         * @param methodName referenced method name
         * @return this entry
         */
        Entry requiresMethod(String owner, String methodName) {
            methodKeys.add(owner + "#" + methodName);
            return this;
        }

        Entry iosPod(String pod) {
            iosPods.add(pod);
            return this;
        }

        Entry iosMinimumDeploymentTarget(String target) {
            iosMinimumDeploymentTarget = target;
            return this;
        }

        Entry iosSpm(String identity, String url, String requirement, String... products) {
            iosSpm.add(new IosSpm(identity, url, requirement,
                    Arrays.asList(products)));
            return this;
        }

        Entry iosDependenciesUnsupportedOnMacCatalyst() {
            iosDependenciesSupportMacCatalyst = false;
            return this;
        }

        Entry iosDependenciesUnsupportedOnArm64Simulator() {
            iosDependenciesSupportArm64Simulator = false;
            return this;
        }

        Entry iosFrameworks(String... fws) {
            for (String f : fws) {
                iosFrameworks.add(f);
            }
            return this;
        }

        Entry iosPlist(String key, String defaultValue) {
            iosPlist.add(new String[]{key, defaultValue});
            return this;
        }

        Entry androidGradle(String gav) {
            androidGradle.add(gav);
            return this;
        }

        Entry androidMinimumSdk(int apiLevel) {
            androidMinimumSdk = apiLevel;
            return this;
        }

        Entry androidPermissions(String... perms) {
            for (String p : perms) {
                androidPermissions.add(p);
            }
            return this;
        }

        Entry androidFeatures(String... feats) {
            for (String f : feats) {
                androidFeatures.add(f);
            }
            return this;
        }

        Entry androidMetaData(String name, String value) {
            androidMetaData.add(new String[]{name, value});
            return this;
        }

        Entry markBigUpload() {
            this.requiresBigUpload = true;
            return this;
        }

        Entry description(String d) {
            this.description = d;
            return this;
        }

        /** @return exact class or package prefix that activates this entry */
        public String classPrefix() {
            return classPrefix;
        }

        /** @return immutable CocoaPods dependency specifications */
        public List<String> iosPods() {
            return Collections.unmodifiableList(iosPods);
        }

        /** @return immutable Swift Package Manager dependency descriptors */
        public List<IosSpm> iosSpmSpecs() {
            return Collections.unmodifiableList(iosSpm);
        }

        /**
         * Whether this entry's CocoaPod/SPM payload has a Mac Catalyst slice.
         * System frameworks are not affected by this flag.
         *
         * @return {@code true} when catalog dependencies support Catalyst
         */
        public boolean iosDependenciesSupportMacCatalyst() {
            return iosDependenciesSupportMacCatalyst;
        }

        /**
         * Whether this entry's CocoaPod/SPM payload has an arm64 iOS
         * Simulator slice. Dependencies that return {@code false} still
         * support the x86_64 simulator.
         *
         * @return {@code true} when catalog dependencies support arm64
         *         simulator builds
         */
        public boolean iosDependenciesSupportArm64Simulator() {
            return iosDependenciesSupportArm64Simulator;
        }

        /**
         * Returns the minimum iOS deployment target required by this entry's
         * CocoaPod or Swift package payload.
         *
         * <p>The iOS builder combines this value with the application's
         * requested deployment target and all other dependency floors, then
         * uses the highest version for the generated app target and Podfile.
         * A {@code null} value means that the entry does not impose an
         * additional deployment floor.</p>
         *
         * @return the required iOS version, such as {@code "15.5"}, or
         * {@code null} when the dependency has no catalog-specific minimum
         */
        public String iosMinimumDeploymentTarget() {
            return iosMinimumDeploymentTarget;
        }

        /** @return immutable Apple system-framework names without suffixes */
        public List<String> iosFrameworks() {
            return Collections.unmodifiableList(iosFrameworks);
        }

        /** Each entry is {key, defaultValue}. The builder injects the
         * value only if the app hasn't already declared one for the
         * same key in its build hints.
         *
         * @return immutable list of two-element property-list entries
         */
        public List<String[]> iosPlistEntries() {
            return Collections.unmodifiableList(iosPlist);
        }

        /** @return immutable Android Gradle dependency coordinates */
        public List<String> androidGradleDeps() {
            return Collections.unmodifiableList(androidGradle);
        }

        /**
         * Returns the minimum Android API level required by this entry's
         * native dependencies.
         *
         * <p>The Android builder raises the application's requested minimum
         * to at least this value before generating the manifest and Gradle
         * configuration. A value of {@code 0} means that the entry does not
         * impose an additional floor.</p>
         *
         * @return the required Android API level, or {@code 0} when no
         * catalog-specific minimum is required
         */
        public int androidMinimumSdk() {
            return androidMinimumSdk;
        }

        /** @return immutable Android manifest permission names */
        public List<String> androidPermissions() {
            return Collections.unmodifiableList(androidPermissions);
        }

        /** @return immutable Android manifest feature names */
        public List<String> androidFeatures() {
            return Collections.unmodifiableList(androidFeatures);
        }

        /** Each entry is {name, value}: an application-level manifest
         * &lt;meta-data&gt; element the Android builder injects unless the
         * app already declares the same name.
         *
         * @return immutable list of two-element manifest metadata entries
         */
        public List<String[]> androidMetaDataEntries() {
            return Collections.unmodifiableList(androidMetaData);
        }

        /** @return whether this entry requires the larger upload allowance */
        public boolean requiresBigUpload() {
            return requiresBigUpload;
        }

        /** @return user-readable feature or dependency description */
        public String description() {
            return description;
        }
    }

    /**
     * Immutable Swift Package Manager dependency descriptor consumed by the
     * iOS builder.
     */
    public static final class IosSpm {
        /** Stable package identity used for de-duplication. */
        public final String identity;
        /** Package repository URL. */
        public final String url;
        /** Builder-formatted version or branch requirement. */
        public final String requirement;
        /** Immutable package product names linked into the app. */
        public final List<String> products;

        IosSpm(String identity, String url, String requirement, List<String> products) {
            this.identity = identity;
            this.url = url;
            this.requirement = requirement;
            this.products = Collections.unmodifiableList(new ArrayList<String>(products));
        }
    }
}
