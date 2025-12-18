package com.codename1.tools.translator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Tag("benchmark")
public class HeavyLoadBenchmarkTest {

    @TempDir
    Path tempDir;

    @Test
    public void benchmarkJavaAPITranslation() throws Exception {
        // Locate JavaAPI.jar
        Path javaApiJar = Paths.get("..", "JavaAPI", "dist", "JavaAPI.jar").normalize().toAbsolutePath();
        if (!Files.exists(javaApiJar)) {
            javaApiJar = Paths.get("vm", "JavaAPI", "dist", "JavaAPI.jar").normalize().toAbsolutePath();
        }

        // Ensure JavaAPI jar exists
        Assertions.assertTrue(Files.exists(javaApiJar), "JavaAPI.jar not found at " + javaApiJar + ". Make sure to build JavaAPI first.");

        Path outputDir = tempDir.resolve("benchmark-output");
        Path srcDir = tempDir.resolve("benchmark-src");
        Path classesDir = tempDir.resolve("benchmark-classes");

        Files.createDirectories(outputDir);
        Files.createDirectories(srcDir);
        Files.createDirectories(classesDir);

        // Create a heavy main class that references various parts of the API to force traversal
        Path pkgDir = srcDir.resolve("com").resolve("benchmark");
        Files.createDirectories(pkgDir);
        Path javaFile = pkgDir.resolve("BenchmarkMain.java");
        String source = "package com.benchmark;\n" +
                "public class BenchmarkMain {\n" +
                "    public static void main(String[] args) {\n" +
                "        java.util.ArrayList l = new java.util.ArrayList();\n" +
                "        l.add(\"Hello\");\n" +
                "        java.util.HashMap m = new java.util.HashMap();\n" +
                "        m.put(\"Key\", l);\n" +
                "        System.out.println(m);\n" +
                "        java.util.Vector v = new java.util.Vector();\n" +
                "        v.addElement(new java.util.Date());\n" +
                "        try {\n" +
                "            Class.forName(\"java.util.TimeZone\");\n" +
                "        } catch (Exception e) {}\n" +
                "    }\n" +
                "}";
        Files.write(javaFile, source.getBytes());

        // Compile the benchmark main
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-cp", javaApiJar.toString(), "-d", classesDir.toString(), javaFile.toString());
        Assertions.assertEquals(0, result, "Compilation of BenchmarkMain failed");

        // Unzip JavaAPI into classesDir to avoid Jar scanning issues and ensure heavy load
        unzip(javaApiJar, classesDir);

        SimpleProfiler profiler = new SimpleProfiler(Thread.currentThread());
        profiler.start();

        long startTime = System.currentTimeMillis();

        try {
            // Classpath is just the classesDir now, containing App + JavaAPI
            String classpath = classesDir.toString();
            System.out.println("Testing classpath: " + classpath);
            runTranslator(classpath, outputDir);
        } finally {
            profiler.stopProfiling();
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Translation took: " + duration + "ms");

        writeReport(duration, profiler.getHotspots(20));
    }

    private void runTranslator(String classpath, Path outputDir) throws Exception {
        Path translatorResources = Paths.get("..", "ByteCodeTranslator", "src").normalize().toAbsolutePath();
        if (!Files.exists(translatorResources)) {
             translatorResources = Paths.get("vm", "ByteCodeTranslator", "src").normalize().toAbsolutePath();
        }

        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        URL[] systemUrls;
        if (systemLoader instanceof URLClassLoader) {
            systemUrls = ((URLClassLoader) systemLoader).getURLs();
        } else {
             String[] paths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
             systemUrls = new URL[paths.length];
             for (int i=0; i<paths.length; i++) {
                 systemUrls[i] = Paths.get(paths[i]).toUri().toURL();
             }
        }

        URL[] urls = Arrays.copyOf(systemUrls, systemUrls.length + 1);
        urls[systemUrls.length] = translatorResources.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(urls, null);

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        try {
            Class<?> translatorClass = Class.forName("com.codename1.tools.translator.ByteCodeTranslator", true, loader);
            Method main = translatorClass.getMethod("main", String[].class);

            String[] args = new String[]{
                    "clean",
                    classpath,
                    outputDir.toString(),
                    "BenchmarkApp",
                    "com.benchmark",
                    "Benchmark",
                    "1.0",
                    "ios",
                    "none"
            };

            main.invoke(null, (Object) args);

        } catch (InvocationTargetException ite) {
             Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
             throw (Exception) cause;
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
            try { loader.close(); } catch(IOException e){}
        }
    }

    private void unzip(Path zipFile, Path outputDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zipFile))) {
            java.util.zip.ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                Path newFile = outputDir.resolve(ze.getName());
                if (ze.isDirectory()) {
                    Files.createDirectories(newFile);
                } else {
                    Files.createDirectories(newFile.getParent());
                    Files.copy(zis, newFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                ze = zis.getNextEntry();
            }
        }
    }

    private void writeReport(long duration, List<String> hotspots) throws IOException {
        Path targetDir = Paths.get("target");
        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
        Path reportFile = targetDir.resolve("benchmark-results.md");

        StringBuilder sb = new StringBuilder();
        sb.append("### Benchmark Results\n\n");
        sb.append("- **Execution Time:** ").append(duration).append(" ms\n");
        sb.append("- **Hotspots (Top 20 sampled methods):**\n");
        for (String s : hotspots) {
            sb.append("  - `").append(s).append("`\n");
        }

        Files.write(reportFile, sb.toString().getBytes());
        System.out.println("Benchmark report written to " + reportFile.toAbsolutePath());
    }

    static class SimpleProfiler extends Thread {
        private final Thread targetThread;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Map<String, Integer> counts = new HashMap<>();
        private int totalSamples = 0;

        public SimpleProfiler(Thread target) {
            this.targetThread = target;
        }

        @Override
        public void run() {
            while (running.get()) {
                StackTraceElement[] stack = targetThread.getStackTrace();
                if (stack.length > 0) {
                    // Simple sampling: just top of stack
                    StackTraceElement top = stack[0];
                    String signature = top.getClassName() + "." + top.getMethodName();
                    counts.merge(signature, 1, Integer::sum);
                    totalSamples++;
                }
                try {
                    Thread.sleep(5); // 5ms sample rate
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public void stopProfiling() {
            running.set(false);
            try {
                this.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public List<String> getHotspots(int limit) {
             if (totalSamples == 0) return Collections.emptyList();
             return counts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .map(e -> String.format("%.2f%% %s (%d samples)", (e.getValue() * 100.0 / totalSamples), e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        }
    }
}
