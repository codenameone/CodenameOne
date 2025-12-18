package com.codename1.tools.translator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        // Locate CodenameOne Core jar
        Path coreJar = findDependencyJar("codenameone-core");

        // Locate IOSPort jar
        Path iosPortJar = findDependencyJar("codenameone-ios-7.0.150.jar"); // Look for specific jar first
        if (iosPortJar == null) iosPortJar = findDependencyJar("codenameone-ios"); // Fallback

        // Locate IOS Bundle (for nativeios.jar)
        Path iosBundleJar = findDependencyJar("codenameone-ios-7.0.150-bundle.jar");
        if (iosBundleJar == null) iosBundleJar = findDependencyJar("bundle");

        // Locate HelloCodenameOne sources
        Path helloSrc = findPath("scripts", "hellocodenameone", "common", "src", "main", "java");

        // Ensure jars exist
        Assertions.assertTrue(Files.exists(javaApiJar), "JavaAPI.jar not found at " + javaApiJar);

        boolean hasCore = coreJar != null;
        if (!hasCore) {
            System.out.println("WARNING: CodenameOne Core jar not found in dependencies.");
        }

        List<Path> jarsToScan = new ArrayList<>();
        jarsToScan.add(javaApiJar);
        if (coreJar != null) jarsToScan.add(coreJar);
        if (iosPortJar != null) jarsToScan.add(iosPortJar);

        System.out.println("Scanning " + jarsToScan.size() + " jars...");

        Path outputDir = tempDir.resolve("benchmark-output");
        Path srcDir = tempDir.resolve("benchmark-src");
        Path classesDir = tempDir.resolve("benchmark-classes");

        Files.createDirectories(outputDir);
        Files.createDirectories(srcDir);
        Files.createDirectories(classesDir);

        // Compile the benchmark main - Setup Compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String cp = javaApiJar.toString();
        if (hasCore) {
            cp += File.pathSeparator + coreJar.toString();
        }
        if (iosPortJar != null) {
            cp += File.pathSeparator + iosPortJar.toString();
        }

        // Compile HelloCodenameOne first if available
        if (helloSrc != null && hasCore) {
            System.out.println("Compiling HelloCodenameOne from " + helloSrc);
            List<String> helloSources = Files.walk(helloSrc)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
            if (!helloSources.isEmpty()) {
                List<String> compileArgs = new ArrayList<>();
                compileArgs.add("-cp");
                compileArgs.add(cp);
                compileArgs.add("-d");
                compileArgs.add(classesDir.toString());
                compileArgs.addAll(helloSources);
                int helloResult = compiler.run(null, null, null, compileArgs.toArray(new String[0]));
                if (helloResult == 0) {
                    System.out.println("Compiled HelloCodenameOne successfully.");
                } else {
                    System.out.println("WARNING: Failed to compile HelloCodenameOne.");
                }
            }
        }

        // Scan jars for public classes
        List<String> publicClasses = new ArrayList<>();
        for (Path jar : jarsToScan) {
            publicClasses.addAll(scanPublicClasses(jar));
        }
        // Scan compiled app classes
        publicClasses.addAll(scanDirectory(classesDir));

        System.out.println("Found " + publicClasses.size() + " public classes total.");

        // Create a heavy main class that references these classes
        Path pkgDir = srcDir.resolve("com").resolve("benchmark");
        Files.createDirectories(pkgDir);
        Path javaFile = pkgDir.resolve("BenchmarkMain.java");

        StringBuilder source = new StringBuilder();
        source.append("package com.benchmark;\n");
        source.append("public class BenchmarkMain {\n");
        source.append("    public static void main(String[] args) {\n");
        source.append("        System.out.println(\"Starting benchmark...\");\n");

        // Split into chunks to avoid method size limits
        int methodCount = 0;
        int chunkSize = 500;
        for (int i = 0; i < publicClasses.size(); i += chunkSize) {
            source.append("        loadChunk").append(methodCount++).append("();\n");
        }
        source.append("    }\n");

        methodCount = 0;
        for (int i = 0; i < publicClasses.size(); i += chunkSize) {
            source.append("    private static void loadChunk").append(methodCount++).append("() {\n");
            source.append("        try {\n");
            source.append("            Class[] classes = new Class[] {\n");
            int end = Math.min(i + chunkSize, publicClasses.size());
            for (int j = i; j < end; j++) {
                String clsName = publicClasses.get(j);
                // Use Class.forName to avoid compilation issues with package-private classes/inner classes
                source.append("                Class.forName(\"").append(clsName).append("\"),\n");
            }
            source.append("            };\n");
            source.append("        } catch (Throwable t) {}\n");
            source.append("    }\n");
        }
        source.append("}\n");

        Files.write(javaFile, source.toString().getBytes());

        // Compile BenchmarkMain (referencing jars + classesDir)
        String cpWithClasses = cp + File.pathSeparator + classesDir.toString();
        int result = compiler.run(null, null, null, "-cp", cpWithClasses, "-d", classesDir.toString(), javaFile.toString());
        Assertions.assertEquals(0, result, "Compilation of BenchmarkMain failed");

        // Unzip jars into classesDir to avoid Jar scanning issues and ensure heavy load
        for (Path jar : jarsToScan) {
            unzip(jar, classesDir);
        }

        // Handle nativeios.jar from bundle if present
        if (iosBundleJar != null) {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(iosBundleJar))) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.getName().endsWith("nativeios.jar")) {
                        Path nativeJar = tempDir.resolve("nativeios.jar");
                        Files.copy(zis, nativeJar);
                        unzip(nativeJar, classesDir);
                        System.out.println("Extracted and unzipped nativeios.jar");
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("WARNING: Failed to extract nativeios.jar from bundle: " + e.getMessage());
            }
        }

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

    private List<String> scanPublicClasses(Path jarFile) throws IOException {
        List<String> classes = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarFile))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().endsWith(".class") && !ze.getName().startsWith("META-INF")) {
                    try {
                        ClassReader cr = new ClassReader(zis);
                        if ((cr.getAccess() & Opcodes.ACC_PUBLIC) != 0) {
                            String name = cr.getClassName().replace('/', '.');
                            if (!name.contains("-") && !name.equals("module-info")) {
                                classes.add(name);
                            }
                        }
                    } catch (Exception e) {
                        // ignore bad classes
                    }
                }
            }
        }
        return classes;
    }

    private Path findJar(String... parts) {
        return findPath(parts);
    }

    private Path findPath(String... parts) {
        // Try paths relative to vm/tests, vm, and root
        Path p = Paths.get("..", "..");
        for (String part : parts) p = p.resolve(part);
        if (Files.exists(p)) return p.normalize().toAbsolutePath();

        p = Paths.get("..");
        for (String part : parts) p = p.resolve(part);
        if (Files.exists(p)) return p.normalize().toAbsolutePath();

        p = Paths.get(".");
        for (String part : parts) p = p.resolve(part);
        if (Files.exists(p)) return p.normalize().toAbsolutePath();

        return null;
    }

    private Path findDependencyJar(String namePart) {
        Path depsDir = Paths.get("target", "benchmark-dependencies");
        if (!Files.exists(depsDir)) return null;
        try {
            return Files.list(depsDir)
                    .filter(p -> p.getFileName().toString().contains(namePart))
                    .findFirst()
                    .map(Path::normalize)
                    .map(Path::toAbsolutePath)
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private List<String> scanDirectory(Path dir) throws IOException {
        List<String> classes = new ArrayList<>();
        Files.walk(dir).forEach(p -> {
            if (Files.isRegularFile(p) && p.toString().endsWith(".class")) {
                try (java.io.InputStream is = Files.newInputStream(p)) {
                    ClassReader cr = new ClassReader(is);
                    if ((cr.getAccess() & Opcodes.ACC_PUBLIC) != 0) {
                        String name = cr.getClassName().replace('/', '.');
                        if (!name.contains("-") && !name.equals("module-info")) {
                            classes.add(name);
                        }
                    }
                } catch (Exception e) {}
            }
        });
        return classes;
    }

    private void unzip(Path zipFile, Path outputDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry ze = zis.getNextEntry();
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
