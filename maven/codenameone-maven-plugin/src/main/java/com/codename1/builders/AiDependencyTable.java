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
 * <p>The build server's class scanners ({@link IPhoneBuilder} and
 * {@link AndroidGradleBuilder}) call into this table from inside their
 * existing {@link Executor.ClassScanner#usesClass(String)} blocks; the
 * resulting set of {@link Entry} records is then applied just before
 * iOS pods / SPM are resolved and just before the Android Gradle
 * dependencies / manifest fragments are written.</p>
 *
 * <p>Keep this table small and declarative: any class prefix whose
 * needs change (different pod version, additional plist entry) should
 * be edited here, not in the builder hot loop.</p>
 */
public final class AiDependencyTable {

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

        // ML Kit feature submodules. Class prefix matches the
        // (forward-referenced) cn1libs' package layout.
        e.add(new Entry("com/codename1/ai/mlkit/text/")
                .iosPod("GoogleMLKit/TextRecognition")
                .androidGradle("com.google.mlkit:text-recognition:16.0.0")
                .iosPlist("NSCameraUsageDescription",
                         "Used to recognise text from your camera.")
                .description("ML Kit Text Recognition"));

        e.add(new Entry("com/codename1/ai/mlkit/barcode/")
                .iosPod("GoogleMLKit/BarcodeScanning")
                .androidGradle("com.google.mlkit:barcode-scanning:17.2.0")
                .iosPlist("NSCameraUsageDescription",
                         "Used to scan barcodes with your camera.")
                .androidFeatures("android.hardware.camera")
                .description("ML Kit Barcode Scanning"));

        e.add(new Entry("com/codename1/ai/mlkit/face/")
                .iosPod("GoogleMLKit/FaceDetection")
                .androidGradle("com.google.mlkit:face-detection:16.1.5")
                .iosPlist("NSCameraUsageDescription",
                         "Used to detect faces in images.")
                .description("ML Kit Face Detection"));

        e.add(new Entry("com/codename1/ai/mlkit/labeling/")
                .iosPod("GoogleMLKit/ImageLabeling")
                .androidGradle("com.google.mlkit:image-labeling:17.0.7")
                .description("ML Kit Image Labeling"));

        e.add(new Entry("com/codename1/ai/mlkit/translate/")
                .iosPod("GoogleMLKit/Translate")
                .androidGradle("com.google.mlkit:translate:17.0.1")
                .description("ML Kit Translation"));

        e.add(new Entry("com/codename1/ai/mlkit/smartreply/")
                .iosPod("GoogleMLKit/SmartReply")
                .androidGradle("com.google.mlkit:smart-reply:17.0.2")
                .description("ML Kit Smart Reply"));

        e.add(new Entry("com/codename1/ai/mlkit/langid/")
                .iosPod("GoogleMLKit/LanguageID")
                .androidGradle("com.google.mlkit:language-id:17.0.4")
                .description("ML Kit Language ID"));

        e.add(new Entry("com/codename1/ai/mlkit/pose/")
                .iosPod("GoogleMLKit/PoseDetection")
                .androidGradle("com.google.mlkit:pose-detection:18.0.0-beta3")
                .description("ML Kit Pose Detection"));

        e.add(new Entry("com/codename1/ai/mlkit/segmentation/")
                .iosPod("GoogleMLKit/SegmentationSelfie")
                .androidGradle("com.google.mlkit:segmentation-selfie:16.0.0-beta4")
                .description("ML Kit Selfie Segmentation"));

        e.add(new Entry("com/codename1/ai/mlkit/docscan/")
                .iosPod("GoogleMLKit/DocumentScanner")
                .iosFrameworks("VisionKit")
                .androidGradle("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
                .description("ML Kit Document Scanner"));

        // TFLite has both Pods and SPM publishers. We register both
        // so IOSDependencyManager.resolve() can route to whichever
        // the project uses.
        e.add(new Entry("com/codename1/ai/tflite/")
                .iosPod("TensorFlowLiteSwift")
                .iosSpm("TensorFlowLiteSwift",
                        "https://github.com/tensorflow/tensorflow.git",
                        "from:2.13.0",
                        "TensorFlowLite")
                .androidGradle("org.tensorflow:tensorflow-lite:2.13.0")
                .androidGradle("org.tensorflow:tensorflow-lite-support:0.4.4")
                .description("TensorFlow Lite interpreter"));

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

        // Google AdMob provider for the modern advertising API. The GMA pod /
        // Gradle dependency and the INTERNET permission are declared in the
        // cn1-admob library's codenameone_library_required.properties (so they
        // are not repeated here to avoid duplicate dependency lines). This entry
        // injects the App Tracking Transparency prompt copy (overridable via the
        // ios.NSUserTrackingUsageDescription build hint). The per-app AdMob
        // application id is wired separately from the "admob.appId" build hint by
        // IPhoneBuilder (GADApplicationIdentifier + SKAdNetworkItems) and
        // AndroidGradleBuilder (APPLICATION_ID manifest meta-data).
        e.add(new Entry("com/codename1/ads/admob/AdMobProvider")
                .iosPlist("NSUserTrackingUsageDescription",
                         "This identifier will be used to deliver personalized ads to you.")
                .description("Google AdMob (modern advertising API)"));

        // On-device Stable Diffusion: bundled Core ML model on iOS,
        // ONNX runtime on Android. Flag the >2 GB upload concern so
        // the cloud build server can abort early with a helpful
        // message.
        e.add(new Entry("com/codename1/ai/imagegen/StableDiffusion")
                .iosFrameworks("CoreML", "Vision")
                .androidGradle("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
                .markBigUpload()
                .description("On-device Stable Diffusion (local-build only)"));

        ENTRIES = Collections.unmodifiableList(e);
    }

    private AiDependencyTable() {
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
            if (e.matches(internalClassName)) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * Builder/scanner output: the de-duplicated union of every entry
     * fired by a class scan. Use {@link Accumulator#consume(String)}
     * from inside an {@link Executor.ClassScanner#usesClass(String)}
     * implementation.
     */
    public static final class Accumulator {
        private final Set<Entry> hits = new LinkedHashSet<Entry>();

        public void consume(String internalClassName) {
            hits.addAll(matchesFor(internalClassName));
        }

        public Set<Entry> hits() {
            return hits;
        }

        public boolean anyRequiresBigUpload() {
            for (Entry e : hits) {
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
        private final List<String> iosPods = new ArrayList<String>();
        private final List<IosSpm> iosSpm = new ArrayList<IosSpm>();
        private final List<String> iosFrameworks = new ArrayList<String>();
        private final List<String[]> iosPlist = new ArrayList<String[]>();
        private final List<String> androidGradle = new ArrayList<String>();
        private final List<String> androidPermissions = new ArrayList<String>();
        private final List<String> androidFeatures = new ArrayList<String>();
        private boolean requiresBigUpload;
        private String description = "";

        Entry(String classPrefix) {
            this.classPrefix = classPrefix;
        }

        boolean matches(String internalClassName) {
            if (classPrefix.endsWith("/")) {
                return internalClassName.startsWith(classPrefix);
            }
            return internalClassName.equals(classPrefix);
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
