package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileStreamsIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    public void testFileStreams(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("file-stream-sources");
        Path classesDir = Files.createTempDirectory("file-stream-classes");
        Path javaFile = sourceDir.resolve("FileStreamApp.java");

        Files.write(javaFile, fileStreamAppSource().getBytes(StandardCharsets.UTF_8));

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

        Files.walk(javaApiSrc)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> compileArgs.add(p.toString()));

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "FileStreamApp.java compilation failed with " + config);

        Path outputDir = Files.createTempDirectory("file-stream-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FileStreamApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("FileStreamApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

        assertTrue(Files.exists(srcRoot.resolve("java_io_FileStreams.c")), "java_io_FileStreams.c should exist");

        replaceLibraryWithExecutableTarget(cmakeLists);

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

        Path executable = buildDir.resolve("FileStreamApp");
        CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
    }

    private String fileStreamAppSource() {
        return "import java.io.*;\n" +
                "public class FileStreamApp {\n" +
                "    public static void main(String[] args) {\n" +
                "        try {\n" +
                "            File file = new File(\"stream_data.bin\");\n" +
                "            if (file.exists()) { file.delete(); }\n" +
                "            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));\n" +
                "            dos.writeInt(0x12345678);\n" +
                "            dos.writeUTF(\"hello\");\n" +
                "            dos.flush();\n" +
                "            dos.close();\n" +
                "            DataInputStream dis = new DataInputStream(new FileInputStream(file));\n" +
                "            int iv = dis.readInt();\n" +
                "            String sv = dis.readUTF();\n" +
                "            dis.close();\n" +
                "            if (iv != 0x12345678 || !\"hello\".equals(sv)) System.exit(1);\n" +
                "            FileWriter writer = new FileWriter(file, true);\n" +
                "            writer.write('!');\n" +
                "            writer.close();\n" +
                "            FileInputStream check = new FileInputStream(file);\n" +
                "            if (check.available() <= 0) System.exit(1);\n" +
                "            byte[] all = new byte[32];\n" +
                "            int count = check.read(all);\n" +
                "            check.close();\n" +
                "            if (count <= 0 || all[count-1] != '!') System.exit(1);\n" +
                "            File parent = file.getParentFile();\n" +
                "            if (parent != null) {\n" +
                "                File[] filtered = parent.listFiles(new FilenameFilter() {\n" +
                "                    public boolean accept(File dir, String name) { return \"stream_data.bin\".equals(name); }\n" +
                "                });\n" +
                "                if (filtered == null || filtered.length == 0) System.exit(1);\n" +
                "            }\n" +
                "        } catch (Exception e) {\n" +
                "            e.printStackTrace();\n" +
                "            System.exit(1);\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private void replaceLibraryWithExecutableTarget(Path cmakeLists) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        content = content.replaceAll("LANGUAGES\\s+C\\s+OBJC", "LANGUAGES C");
        content = content.replaceAll("(?m)^enable_language\\(OBJC OPTIONAL\\)\\s*$\\n?", "");
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})\ntarget_link_libraries(${PROJECT_NAME} m)"
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }
}
