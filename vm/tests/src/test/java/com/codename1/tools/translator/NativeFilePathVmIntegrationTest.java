package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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

/**
 * Builds a small application that exercises java.io.File and java.nio.file.Path,
 * translates it with the native VM toolchain, compiles the generated C with gcc,
 * and verifies the expected files are created at runtime.
 */
class NativeFilePathVmIntegrationTest {

    @Test
    void nativeVmCreatesFilesAndDirectories() throws Exception {
        Parser.cleanup();

        Path javaApiDir = Files.createTempDirectory("java-api-classes");
        compileJavaAPI(javaApiDir);

        Path sourceDir = Files.createTempDirectory("native-file-path-src");
        Path classesDir = Files.createTempDirectory("native-file-path-classes");
        Path appSource = sourceDir.resolve("FilePathNativeApp.java");
        Files.write(appSource, appSource().getBytes(StandardCharsets.UTF_8));

        compileApp(javaApiDir, classesDir, appSource);

        copyJavaApiClasses(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("native-file-path-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FilePathNativeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("FilePathNativeApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        CleanTargetIntegrationTest.writeRuntimeStubs(srcRoot);
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());
        CleanTargetIntegrationTest.stripObjectiveC(cmakeLists);

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=gcc"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("FilePathNativeApp");
        CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);

        Path root = buildDir.resolve("vmtest_root");
        Path subDir = root.resolve("subdir");
        Path moved = root.resolve("moved.txt");
        Path uriFile = root.resolve("uri-created.txt");

        assertTrue(Files.exists(root), "Root directory should exist after native run");
        assertTrue(Files.exists(subDir), "Subdirectory should exist after native run");
        assertTrue(Files.exists(moved), "Renamed file should exist after native run");
        assertTrue(Files.exists(uriFile), "URI-created file should exist after native run");
    }

    private void compileApp(Path javaApiDir, Path classesDir, Path appSource) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<String>();
        boolean modernJdk = !System.getProperty("java.version").startsWith("1.");
        args.add("-source");
        args.add("1.8");
        args.add("-target");
        args.add("1.8");
        if (modernJdk) {
            args.add("-classpath");
            args.add(javaApiDir.toString());
            args.add("-Xlint:-options");
        } else {
            args.add("-bootclasspath");
            args.add(javaApiDir.toString());
            args.add("-Xlint:-options");
        }
        args.add("-d");
        args.add(classesDir.toString());
        args.add(appSource.toString());
        int result = compiler.run(null, null, null, args.toArray(new String[0]));
        assertEquals(0, result, "App compilation should succeed");
    }

    private void compileJavaAPI(Path outputDir) throws Exception {
        Files.createDirectories(outputDir);
        Path javaApiRoot = Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<String>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<String>();

        if (!System.getProperty("java.version").startsWith("1.")) {
            args.add("--patch-module");
            args.add("java.base=" + javaApiRoot.toString());
            args.add("-Xlint:-options");
        } else {
            args.add("-source");
            args.add("1.5");
            args.add("-target");
            args.add("1.5");
        }

        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(sources);

        int result = compiler.run(null, null, null, args.toArray(new String[0]));
        assertEquals(0, result, "JavaAPI compilation should succeed");
    }

    private void copyJavaApiClasses(Path from, Path to) throws Exception {
        Files.walk(from).forEach(p -> {
            try {
                Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(p, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String appSource() {
        return ""
                + "import java.io.File;\n"
                + "import java.net.URI;\n"
                + "import java.nio.file.Path;\n"
                + "import java.nio.file.Paths;\n"
                + "public class FilePathNativeApp {\n"
                + "  public static void main(String[] args) throws Exception {\n"
                + "    File root = new File(\"vmtest_root\");\n"
                + "    if (!root.exists()) root.mkdirs();\n"
                + "    File sub = new File(root, \"subdir\");\n"
                + "    sub.mkdirs();\n"
                + "    File file = new File(sub, \"file.txt\");\n"
                + "    if (!file.createNewFile()) throw new RuntimeException(\"createNewFile failed\");\n"
                + "    if (!file.exists() || !file.isFile()) throw new RuntimeException(\"file missing after create\");\n"
                + "    File moved = new File(root, \"moved.txt\");\n"
                + "    if (!file.renameTo(moved)) throw new RuntimeException(\"rename failed\");\n"
                + "    if (!moved.setLastModified(System.currentTimeMillis())) throw new RuntimeException(\"setLastModified failed\");\n"
                + "    Path rel = Paths.get(\"vmtest_root\", \"uri-created.txt\");\n"
                + "    File uriFile = new File(new URI(rel.toUri().toString()));\n"
                + "    uriFile.createNewFile();\n"
                + "    File[] listed = root.listFiles();\n"
                + "    if (listed == null || listed.length == 0) throw new RuntimeException(\"listing failed\");\n"
                + "  }\n"
                + "}\n";
    }
}
