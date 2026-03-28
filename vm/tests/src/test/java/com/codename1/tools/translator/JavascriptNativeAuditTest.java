package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptNativeAuditTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void allCompiledJavaApiNativesAreCategorizedForJavascript(CompilerHelper.CompilerConfig config) throws Exception {
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        Path javaApiDir = Files.createTempDirectory("java-api-native-audit");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        final List<String> uncategorized = new ArrayList<String>();
        try (Stream<Path> paths = Files.walk(javaApiDir)) {
            paths.filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> inspectClass(path, uncategorized));
        }

        Collections.sort(uncategorized);
        assertTrue(uncategorized.isEmpty(),
                "Every compiled JavaAPI native must be categorized for javascript backend. Missing: " + uncategorized);
    }

    private static void inspectClass(Path classFile, final List<String> uncategorized) {
        JavascriptNativeAuditSupport.inspectClass(classFile, uncategorized);
    }
}
