package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests the real JavaAPI classes, not accidentally the host JDK's java.util.zip. */
class ZipApiIntegrationTest {
    @Test
    void streamingArchivesInteroperateOnJvmAndTranslatedRuntime() throws Exception {
        Parser.cleanup();
        Path repo = Paths.get("..").toAbsolutePath().normalize();
        if (!Files.isDirectory(repo.resolve("vm")))
            repo = repo.getParent();
        Path work = Files.createTempDirectory("zip-api-integration-");
        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        Path api = work.resolve("api"), app = work.resolve("app"), patch = work.resolve("patch");
        Files.createDirectories(api);
        Files.createDirectories(app);
        CompilerHelper.compileJavaAPI(api, config);
        Path source = work.resolve("ZipApiApp.java");
        try (InputStream in = getClass().getResourceAsStream("/com/codename1/tools/translator/ZipApiApp.java")) {
            assertNotNull(in);
            Files.copy(in, source);
        }
        List<String> args = Arrays.asList("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-bootclasspath",
                api.toString(), "-d", app.toString(), source.toString(),
                repo.resolve("CodenameOne/src/com/codename1/io/zip/ZipArchiveOutputStream.java").toString(),
                repo.resolve("CodenameOne/src/com/codename1/impl/SharedWithBackend.java").toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, args), CompilerHelper.getLastErrorLog());
        String java = config.jdkHome.resolve("bin/java").toString();
        Path jdk = work.resolve("jdk"), patched = work.resolve("patched"), nativePrefix = work.resolve("native");
        assertTrue(run(work, java, "-cp", app.toString(), "ZipApiApp", jdk.toString()).contains("ZIP_API_OK"));
        // Patch only ZIP and its engine. Replacing Object/String on HotSpot is invalid.
        CompilerHelper.copyDirectory(api.resolve("java/util/zip"), patch.resolve("java/util/zip"));
        CompilerHelper.copyDirectory(api.resolve("com/codename1/impl/zip"), patch.resolve("com/codename1/impl/zip"));
        assertTrue(run(work, java, "-Xbootclasspath/p:" + patch, "-cp", app.toString(), "ZipApiApp", patched.toString(),
                jdk + "-standard.zip")
                        .contains("ZIP_API_OK"));
        verifyArchive(patched, jdk);
        CompilerHelper.copyDirectory(api, app);
        Path output = work.resolve("translated");
        Files.createDirectories(output);
        CleanTargetIntegrationTest.runTranslator(app, output, "ZipApiApp");
        Path dist = output.resolve("dist"), build = dist.resolve("build");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(dist.resolve("CMakeLists.txt"), "ZipApiApp-src");
        run(dist, "cmake", "-S", dist.toString(), "-B", build.toString(), "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang");
        run(dist, "cmake", "--build", build.toString(), "-j", "4");
        assertTrue(run(work, build.resolve("ZipApiApp").toString(), nativePrefix.toString(), jdk + "-standard.zip")
                        .contains("ZIP_API_OK"));
        verifyArchive(nativePrefix, jdk);
        System.out.println("ZIP artifacts verified in " + work);
    }
    private static String run(Path dir, String... command) throws Exception {
        Path log = Files.createTempFile(dir, "zip-command-", ".log");
        Process process = new ProcessBuilder(command).directory(dir.toFile())
                                  .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(180, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("ZIP validation command timed out: " + Arrays.toString(command));
        }
        String output = new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), Arrays.toString(command) + "\n" + output);
        return output;
    }
    private static void verifyArchive(Path prefix, Path reference) throws Exception {
        for (String suffix : Arrays.asList(".zip", "-standard.zip")) {
            try (ZipFile actual = new ZipFile(prefix + suffix); ZipFile expected = new ZipFile(reference + suffix)) {
                assertEquals(expected.size(), actual.size());
                Enumeration<? extends ZipEntry> entries = expected.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement(), a = actual.getEntry(e.getName());
                    assertNotNull(a);
                    assertEquals(e.getMethod(), a.getMethod());
                    assertEquals(e.getSize(), a.getSize());
                    assertEquals(e.getCrc(), a.getCrc());
                    assertEquals(e.getTime(), a.getTime());
                    assertEquals(e.getComment(), a.getComment());
                    try (InputStream x = expected.getInputStream(e); InputStream y = actual.getInputStream(a)) {
                        assertArrayEquals(bytes(x), bytes(y));
                    }
                }
            }
        }
        byte[] archive = Files.readAllBytes(Paths.get(prefix + ".zip"));
        // Central-directory Unix metadata is not exposed by the JDK ZipFile API.
        int end = archive.length - 22 - "archive comment".length();
        int at = (int) u32(archive, end + 16);
        int[] modes = {040755, 0100755, 0100644, 0100644};
        for (int mode : modes) {
            assertEquals(0x02014b50L, u32(archive, at));
            assertEquals(3, archive[at + 5]);
            assertEquals(mode, u32(archive, at + 38) >>> 16);
            assertEquals(0x800, u16(archive, at + 8) & 0x800);
            at += 46 + u16(archive, at + 28) + u16(archive, at + 30) + u16(archive, at + 32);
        }
        try (InputStream in = new GZIPInputStream(new FileInputStream(prefix + ".gz"));
                InputStream expected = new GZIPInputStream(new FileInputStream(reference + ".gz"))) {
            assertArrayEquals(bytes(expected), bytes(in));
        }
        run(prefix.getParent(), "unzip", "-t", prefix + ".zip");
        Path extracted = Files.createTempDirectory(prefix.getParent(), "unzip-");
        run(prefix.getParent(), "unzip", "-q", prefix + ".zip", "-d", extracted.toString());
        assertTrue(
                Files.isExecutable(extracted.resolve("project/run.sh")), "Unzip must retain launcher executable bits");
    }
    private static byte[] bytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        return out.toByteArray();
    }
    private static int u16(byte[] b, int at) {
        return (b[at] & 255) | ((b[at + 1] & 255) << 8);
    }
    private static long u32(byte[] b, int at) {
        return u16(b, at) | ((long) u16(b, at + 2) << 16);
    }
}
