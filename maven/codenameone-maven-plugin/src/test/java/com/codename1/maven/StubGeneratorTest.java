package com.codename1.maven;

import com.codename1.maven.stubgen.TestNativeInterface;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void existingFilesAreReportedForWarnMode() throws Exception {
        File tempDir = Files.createTempDirectory("cn1-stubgen-existing").toFile();
        try {
            StubGenerator generator = StubGenerator.create(new SystemStreamLog(), TestNativeInterface.class, true, true);
            generator.generateCode(tempDir, false);

            StubGenerator checker = StubGenerator.create(new SystemStreamLog(), TestNativeInterface.class, true, true);
            List<File> existing = checker.getExistingFiles(tempDir);
            assertEquals(8, existing.size());
            assertTrue(checker.isFilesExist(tempDir));
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
