package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptCn1CoreCompletenessTest {

    @Test
    void translatesMeaningfulCodenameOneCoreSliceWithoutUncategorizedNativeGaps() throws Exception {
        Parser.cleanup();

        CompilerHelper.CompilerConfig config = selectRepresentativeCompiler();
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        Path distDir = translateCn1CoreSlice(config);
        assertTrue(Files.exists(distDir.resolve("translated_app.js")),
                "Translator should emit translated JS for the Codename One core slice");
        assertTrue(Files.exists(distDir.resolve("vm_protocol.md")),
                "Translator should emit the VM protocol artifact for the Codename One core slice");

        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        assertTrue(translatedApp.contains("JsCodenameOneCoreSliceApp"),
                "Translated bundle should contain the CN1 core slice app entrypoint");
        assertFalse(translatedApp.contains("Missing javascript native method "),
                "CN1 core slice translation should not retain uncategorized javascript native fallback stubs");
    }

    @Test
    void executesMeaningfulCodenameOneCoreSliceInWorkerRuntime() throws Exception {
        Parser.cleanup();

        CompilerHelper.CompilerConfig config = selectRepresentativeCompiler();
        Path distDir = translateCn1CoreSlice(config);

        JavascriptRuntimeSemanticsTest.WorkerRunResult result = JavascriptRuntimeSemanticsTest.runGeneratedWorkerBundle(distDir);
        assertEquals("result", result.type,
                "CN1 core slice should complete through the generated worker protocol. Raw worker payload: " + result.rawMessage);
        assertEquals(7, result.result,
                "CN1 core slice should execute JSON and StringUtil behavior correctly. "
                        + "Raw worker payload: " + result.rawMessage
                        + " | type=" + result.type
                        + " | error=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(),
                "CN1 core slice should not emit a worker error. Raw worker payload: " + result.rawMessage);
    }

    private static CompilerHelper.CompilerConfig selectRepresentativeCompiler() {
        String[] preferredTargets = new String[] {"11", "17", "21"};
        for (String target : preferredTargets) {
            for (CompilerHelper.CompilerConfig config : CompilerHelper.getAvailableCompilers(target)) {
                if (target.equals(config.targetVersion) && CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        throw new AssertionError("No representative JDK 11+ compiler available for Codename One core slice translation");
    }

    private static void compileFixtureAgainstJavaApiAndCore(CompilerHelper.CompilerConfig config, Path sourceDir,
                                                            Path classesDir, Path javaApiDir, Path coreJar) throws Exception {
        List<String> sources = new ArrayList<String>();
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> sources.add(path.toString()));
        }

        List<String> compileArgs = new ArrayList<String>();
        compileArgs.add("-source");
        compileArgs.add(config.targetVersion);
        compileArgs.add("-target");
        compileArgs.add(config.targetVersion);
        compileArgs.add("-classpath");
        compileArgs.add(javaApiDir.toString() + java.io.File.pathSeparator + coreJar.toString());
        if (!CompilerHelper.useClasspath(config)) {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.addAll(sources);

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult,
                "Compilation failed for Codename One core slice fixture with " + config + ": " + CompilerHelper.getLastErrorLog());
    }

    private static Path translateCn1CoreSlice(CompilerHelper.CompilerConfig config) throws Exception {
        Path sourceDir = Files.createTempDirectory("js-cn1-core-src");
        Path classesDir = Files.createTempDirectory("js-cn1-core-classes");
        Path javaApiDir = Files.createTempDirectory("js-cn1-core-javaapi");

        Files.write(sourceDir.resolve("JsCodenameOneCoreSliceApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsCodenameOneCoreSliceApp.java").getBytes(StandardCharsets.UTF_8));

        Path coreJar = findDependencyJar("codenameone-core");
        assertNotNull(coreJar, "codenameone-core dependency jar should be available in target/benchmark-dependencies");

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        compileFixtureAgainstJavaApiAndCore(config, sourceDir, classesDir, javaApiDir, coreJar);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        unzipMatching(coreJar, classesDir,
                "com/codename1/io/",
                "com/codename1/util/",
                "com/codename1/compat/java/",
                "com/codename1/l10n/",
                "com/codename1/ui/events/",
                "com/codename1/xml/");

        Path outputDir = Files.createTempDirectory("js-cn1-core-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsCodenameOneCoreSliceApp");
        return outputDir.resolve("dist").resolve("JsCodenameOneCoreSliceApp-js");
    }

    private static Path findDependencyJar(String namePart) throws IOException {
        Path depsDir = Paths.get("target", "benchmark-dependencies");
        if (!Files.exists(depsDir)) {
            return null;
        }
        try (Stream<Path> paths = Files.list(depsDir)) {
            return paths.filter(path -> path.getFileName().toString().contains(namePart))
                    .findFirst()
                    .map(Path::normalize)
                    .map(Path::toAbsolutePath)
                    .orElse(null);
        }
    }

    private static void unzipMatching(Path zipFile, Path outputDir, String... prefixes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                if (!matchesPrefix(entry.getName(), prefixes)) {
                    continue;
                }
                Path out = outputDir.resolve(entry.getName());
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static boolean matchesPrefix(String name, String... prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
