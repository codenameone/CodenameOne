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

    private static final List<Entry> ENTRIES;

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
                .iosFrameworks("AVFAudio")
                .description("Text-to-speech"));

        // Built-in vision APIs. Android uses ML Kit by default. iOS uses
        // Apple Vision/VisionKit unless VisionBackends.mlKit() is selected.
        // The compound entries require both the feature and selector method,
        // so iOS only bundles ML Kit pods for features actually used.
        e.add(new Entry("com/codename1/ai/vision/TextRecognizer")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:text-recognition:16.0.0")
                .description("Text recognition"));
        e.add(new Entry("com/codename1/ai/vision/TextRecognizer")
                .requiresMethod("com/codename1/ai/vision/VisionBackends", "mlKit")
                .iosPod("GoogleMLKit/TextRecognition")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .description("ML Kit iOS text-recognition backend"));

        e.add(new Entry("com/codename1/ai/vision/BarcodeScanner")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:barcode-scanning:17.2.0")
                .description("Barcode scanning"));
        e.add(new Entry("com/codename1/ai/vision/BarcodeScanner")
                .requiresMethod("com/codename1/ai/vision/VisionBackends", "mlKit")
                .iosPod("GoogleMLKit/BarcodeScanning")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .description("ML Kit iOS barcode backend"));

        e.add(new Entry("com/codename1/ai/vision/FaceDetector")
                .iosFrameworks("Vision", "CoreImage")
                .androidGradle("com.google.mlkit:face-detection:16.1.5")
                .description("Face detection"));
        e.add(new Entry("com/codename1/ai/vision/FaceDetector")
                .requiresMethod("com/codename1/ai/vision/VisionBackends", "mlKit")
                .iosPod("GoogleMLKit/FaceDetection")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .description("ML Kit iOS face-detection backend"));

        e.add(new Entry("com/codename1/ai/vision/ImageLabeler")
                .iosFrameworks("Vision", "CoreML")
                .androidGradle("com.google.mlkit:image-labeling:17.0.7")
                .description("Image labeling"));
        e.add(new Entry("com/codename1/ai/vision/ImageLabeler")
                .requiresMethod("com/codename1/ai/vision/VisionBackends", "mlKit")
                .iosPod("GoogleMLKit/ImageLabeling")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .description("ML Kit iOS image-labeling backend"));

        e.add(new Entry("com/codename1/ai/vision/PoseDetector")
                .iosFrameworks("Vision", "CoreML")
                .androidGradle("com.google.mlkit:pose-detection:18.0.0-beta3")
                .description("Pose detection"));
        e.add(new Entry("com/codename1/ai/vision/PoseDetector")
                .requiresMethod("com/codename1/ai/vision/VisionBackends", "mlKit")
                .iosPod("GoogleMLKit/PoseDetection")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .description("ML Kit iOS pose-detection backend"));

        e.add(new Entry("com/codename1/ai/vision/SelfieSegmenter")
                .iosFrameworks("Vision", "CoreML")
                .androidGradle(
                        "com.google.mlkit:segmentation-selfie:16.0.0-beta5")
                .description("Selfie segmentation"));
        e.add(new Entry("com/codename1/ai/vision/SelfieSegmenter")
                .requiresMethod("com/codename1/ai/vision/VisionBackends", "mlKit")
                .iosPod("GoogleMLKit/SegmentationSelfie")
                .iosDependenciesUnsupportedOnMacCatalyst()
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
                .description("LiteRT inference"));

        e.add(new Entry("com/codename1/ai/language/LanguageIdentifier")
                .iosPod("GoogleMLKit/LanguageID")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .androidGradle("com.google.mlkit:language-id:17.0.6")
                .description("On-device language identification"));
        e.add(new Entry("com/codename1/ai/language/Translator")
                .iosPod("GoogleMLKit/Translate")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .androidGradle("com.google.mlkit:translate:17.0.3")
                .description("On-device translation"));
        e.add(new Entry("com/codename1/ai/language/SmartReply")
                .iosPod("GoogleMLKit/SmartReply")
                .iosDependenciesUnsupportedOnMacCatalyst()
                .androidGradle("com.google.mlkit:smart-reply:17.0.4")
                .description("On-device smart reply"));

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
                .description("Cross-platform camera (preview + frames + photo + video)"));

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

        // On-device Stable Diffusion: bundled Core ML model on iOS,
        // ONNX runtime on Android. Flag the >2 GB upload concern so
        // the cloud build server can abort early with a helpful
        // message.
        e.add(new Entry("com/codename1/ai/imagegen/StableDiffusion")
                .iosFrameworks("CoreML", "Vision")
                .androidGradle("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
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
    }

    private PlatformFeatureCatalog() {
    }

    /** All registered entries. Mostly useful for tests and tooling. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * Returns every entry whose {@link Entry#classPrefix} matches the
     * given internal-form class name (slashes, not dots). When the
     * prefix ends with a slash, package-prefix matching is used;
     * otherwise an exact class match is required.
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

        public void consume(String internalClassName) {
            if (internalClassName != null) {
                classes.add(internalClassName);
            }
        }

        public void consumeMethod(String internalClassName, String methodName) {
            if (internalClassName != null && methodName != null) {
                methods.add(internalClassName + "#" + methodName);
            }
        }

        public Set<Entry> hits() {
            Set<Entry> hits = new LinkedHashSet<Entry>();
            for (Entry entry : ENTRIES) {
                if (entry.requirementsMet(classes, methods)) {
                    hits.add(entry);
                }
            }
            return hits;
        }

        public boolean anyRequiresBigUpload() {
            for (Entry e : hits()) {
                if (e.requiresBigUpload) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A single registry record. Mutable while the table is being
     * built (the fluent setters); semantically immutable once exposed
     * via {@link #entries()}.
     */
    public static final class Entry {
        private final String classPrefix;
        private String methodOwner;
        private String methodPrefix;
        private final List<String> iosPods = new ArrayList<String>();
        private final List<IosSpm> iosSpm = new ArrayList<IosSpm>();
        private final List<String> iosFrameworks = new ArrayList<String>();
        private final List<String[]> iosPlist = new ArrayList<String[]>();
        private final List<String> androidGradle = new ArrayList<String>();
        private final List<String> androidPermissions = new ArrayList<String>();
        private final List<String> androidFeatures = new ArrayList<String>();
        private final List<String[]> androidMetaData = new ArrayList<String[]>();
        private boolean iosDependenciesSupportMacCatalyst = true;
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
            return methodOwner != null;
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
            if (!hasMethodRequirement()) {
                return true;
            }
            for (String method : methods) {
                if (method.startsWith(methodOwner + "#" + methodPrefix)) {
                    return true;
                }
            }
            return false;
        }

        Entry requiresMethod(String owner, String methodNamePrefix) {
            this.methodOwner = owner;
            this.methodPrefix = methodNamePrefix;
            return this;
        }

        Entry iosPod(String pod) {
            iosPods.add(pod);
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

        public String classPrefix() {
            return classPrefix;
        }

        public List<String> iosPods() {
            return Collections.unmodifiableList(iosPods);
        }

        public List<IosSpm> iosSpmSpecs() {
            return Collections.unmodifiableList(iosSpm);
        }

        /**
         * Whether this entry's CocoaPod/SPM payload has a Mac Catalyst slice.
         * System frameworks are not affected by this flag.
         */
        public boolean iosDependenciesSupportMacCatalyst() {
            return iosDependenciesSupportMacCatalyst;
        }

        public List<String> iosFrameworks() {
            return Collections.unmodifiableList(iosFrameworks);
        }

        /** Each entry is {key, defaultValue}. The builder injects the
         * value only if the app hasn't already declared one for the
         * same key in its build hints. */
        public List<String[]> iosPlistEntries() {
            return Collections.unmodifiableList(iosPlist);
        }

        public List<String> androidGradleDeps() {
            return Collections.unmodifiableList(androidGradle);
        }

        public List<String> androidPermissions() {
            return Collections.unmodifiableList(androidPermissions);
        }

        public List<String> androidFeatures() {
            return Collections.unmodifiableList(androidFeatures);
        }

        /** Each entry is {name, value}: an application-level manifest
         * &lt;meta-data&gt; element the Android builder injects unless the
         * app already declares the same name. */
        public List<String[]> androidMetaDataEntries() {
            return Collections.unmodifiableList(androidMetaData);
        }

        public boolean requiresBigUpload() {
            return requiresBigUpload;
        }

        public String description() {
            return description;
        }
    }

    /** Swift Package Manager dependency descriptor. */
    public static final class IosSpm {
        public final String identity;
        public final String url;
        public final String requirement;
        public final List<String> products;

        IosSpm(String identity, String url, String requirement, List<String> products) {
            this.identity = identity;
            this.url = url;
            this.requirement = requirement;
            this.products = Collections.unmodifiableList(new ArrayList<String>(products));
        }
    }
}
