package com.codename1.ai.whisper;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndroidWhisperAarProjectTest {

    @Test
    void androidJniBridgeExportsTimedSegmentsForAllAndroidAbis() throws Exception {
        String cpp = read("../android-aar/cn1-ai-whisper-android/src/main/cpp/native_whisper_jni.cpp");
        assertTrue(cpp.contains("NativeWhisperRecognizerImpl_nativeTranscribeSegments"), cpp);
        assertTrue(cpp.contains("whisper_full_get_segment_t0"), cpp);
        assertTrue(cpp.contains("whisper_full_get_segment_t1"), cpp);
        assertTrue(cpp.contains("whisper_init_from_file_with_params"), cpp);

        String gradle = read("../android-aar/cn1-ai-whisper-android/build.gradle");
        assertTrue(gradle.contains("abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'"), gradle);
        assertTrue(gradle.contains("ndkVersion '26.3.11579264'"), gradle);
    }

    @Test
    void stagedAndroidAarContainsJniSlicesWhenPresent() throws Exception {
        File aar = new File("../android/src/main/resources/cn1-ai-whisper-android.aar");
        if (!aar.isFile()) {
            return;
        }
        ZipFile zip = new ZipFile(aar);
        try {
            assertNotNull(zip.getEntry("jni/armeabi-v7a/libcn1aiwhisper.so"));
            assertNotNull(zip.getEntry("jni/arm64-v8a/libcn1aiwhisper.so"));
            assertNotNull(zip.getEntry("jni/x86/libcn1aiwhisper.so"));
            assertNotNull(zip.getEntry("jni/x86_64/libcn1aiwhisper.so"));
        } finally {
            zip.close();
        }
    }

    private static String read(String path) throws Exception {
        File file = new File(path);
        assertTrue(file.isFile(), "Missing " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
