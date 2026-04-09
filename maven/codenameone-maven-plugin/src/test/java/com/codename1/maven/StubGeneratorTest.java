package com.codename1.maven;

import com.codename1.maven.stubgen.TestNativeInterface;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubGeneratorTest {

    @Test
    void optionalSwiftAndKotlinStubsAreGeneratedOnlyWhenEnabled() throws Exception {
        File tempDir = Files.createTempDirectory("cn1-stubgen").toFile();
        try {
            StubGenerator defaultGenerator = StubGenerator.create(new SystemStreamLog(), TestNativeInterface.class, false, false);
            defaultGenerator.generateCode(tempDir, false);
            File iosSwift = new File(tempDir, "ios/src/main/objectivec/com_codename1_maven_stubgen_TestNativeInterfaceImpl.swift");
            File androidKotlin = new File(tempDir, "android/src/main/java/com/codename1/maven/stubgen/TestNativeInterfaceImpl.kt");
            assertFalse(iosSwift.exists());
            assertFalse(androidKotlin.exists());

            StubGenerator enabledGenerator = StubGenerator.create(new SystemStreamLog(), TestNativeInterface.class, true, true);
            enabledGenerator.generateCode(tempDir, false);
            assertTrue(iosSwift.exists());
            assertTrue(androidKotlin.exists());
        } finally {
            deleteTree(tempDir);
        }
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteTree(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
