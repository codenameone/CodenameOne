package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileClassIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    public void testFileClassMethods(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("file-test-sources");
        Path classesDir = Files.createTempDirectory("file-test-classes");
        Path javaFile = sourceDir.resolve("FileTestApp.java");

        Files.write(javaFile, fileTestAppSource().getBytes(StandardCharsets.UTF_8));

        // Use real JavaAPI sources
        Path javaApiSrc = Paths.get("../JavaAPI/src");
        if (!Files.exists(javaApiSrc)) {
            javaApiSrc = Paths.get("vm/JavaAPI/src");
        }

        List<String> compileArgs = new ArrayList<>();
        double jdkVer = 1.8;
        try { jdkVer = Double.parseDouble(config.jdkVersion); } catch (NumberFormatException ignored) {}

        if (jdkVer >= 9) {
             if (Double.parseDouble(config.targetVersion) < 9) {
                 return;
             }
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("--patch-module");
             compileArgs.add("java.base=" + javaApiSrc.toString());
             compileArgs.add("-Xlint:-module");
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());

        // Add all JavaAPI source files
        Files.walk(javaApiSrc)
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> compileArgs.add(p.toString()));

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "FileTestApp.java compilation failed with " + config);

        Path outputDir = Files.createTempDirectory("file-test-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FileTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("FileTestApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

        // Ensure java_io_File.m is included (ByteCodeTranslator should copy it)
        assertTrue(Files.exists(srcRoot.resolve("java_io_File.m")), "java_io_File.m should exist");

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        List<String> cmakeCommand = new ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString()
        ));
        cmakeCommand.addAll(CleanTargetIntegrationTest.cmakeCompilerArgs());
        CleanTargetIntegrationTest.runCommand(cmakeCommand, distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("FileTestApp");
        // Running the app. If it exits with 0, logic passed.
        CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
    }

    private String fileTestAppSource() {
        return "import java.io.File;\n" +
               "public class FileTestApp {\n" +
               "    public static void main(String[] args) {\n" +
               "        try {\n" +
               "            // Construct string manually to avoid constant pool issues in test env\n" +
               "            char[] pathChars = new char[]{'t','e','s','t','f','i','l','e','.','t','x','t'};\n" +
               "            String path = new String(pathChars);\n" +
               "            File f = new File(path);\n" +
               "            if (f.exists()) {\n" +
               "                f.delete();\n" +
               "            }\n" +
               "            boolean created = f.createNewFile();\n" +
               "            if (!created) throw new RuntimeException(\"Create failed\");\n" +
               "            if (!f.exists()) throw new RuntimeException(\"Exists failed\");\n" +
               "            if (f.isDirectory()) throw new RuntimeException(\"IsDirectory failed\");\n" +
               "            if (!f.delete()) throw new RuntimeException(\"Delete failed\");\n" +
               "            if (f.exists()) throw new RuntimeException(\"Delete verification failed\");\n" +
               "        } catch (Exception e) {\n" +
               "            // e.printStackTrace(); // Can't print stack trace without constants\n" +
               "            System.exit(1);\n" +
               "        }\n" +
               "    }\n" +
               "}";
    }

    private void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})\ntarget_link_libraries(${PROJECT_NAME} m)"
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }
}
