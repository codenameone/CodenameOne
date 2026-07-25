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
