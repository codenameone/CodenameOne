package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        List<String> compileArgs = new ArrayList<>();
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "FileTestApp.java compilation failed with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("file-test-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FileTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("FileTestApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

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
