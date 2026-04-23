package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class JavascriptBundleWriter {
    private static final String RESOURCE_ROOT = "/javascript/";

    private JavascriptBundleWriter() {
    }

    static void write(File outputDirectory, List<ByteCodeClass> classes) throws IOException {
        writeRuntime(outputDirectory);
        writeTranslatedClasses(outputDirectory, classes);
        copyJavaScriptPortWebAppAssets(outputDirectory);
        writeWorker(outputDirectory);
        writeBrowserBridge(outputDirectory);
        writeIndex(outputDirectory);
        writeProtocol(outputDirectory);
    }

    private static void writeRuntime(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "parparvm_runtime.js", "parparvm_runtime.js");
    }

    /**
     * Cap on how large any single emitted class-definitions file may grow
     * before we start a new chunk. Cloudflare Pages rejects uploads with any
     * individual file larger than ~25 MiB, so we stay comfortably under that
     * while keeping the chunk count small. The chunks are concatenated at
     * load time via the worker's generated importScripts list.
     */
    private static final int CLASS_CHUNK_MAX_BYTES = 20 * 1024 * 1024;

    private static void writeTranslatedClasses(File outputDirectory, List<ByteCodeClass> classes) throws IOException {
        List<ByteCodeClass> sorted = new ArrayList<ByteCodeClass>(classes);
        Collections.sort(sorted, new Comparator<ByteCodeClass>() {
            @Override
            public int compare(ByteCodeClass a, ByteCodeClass b) {
                int priorityDiff = bootstrapPriority(a) - bootstrapPriority(b);
                if (priorityDiff != 0) {
                    return priorityDiff;
                }
                return a.getClsName().compareTo(b.getClsName());
            }
        });

        // Stream class bodies into bounded chunks. We materialise every chunk
        // but the last one as translated_app_NN.js; the final chunk lands at
        // translated_app.js and carries the jvm.setMain(...) tail so that
        // call always runs after every class has been registered (writeWorker
        // imports translated_app.js last).
        List<StringBuilder> chunks = new ArrayList<StringBuilder>();
        StringBuilder current = new StringBuilder();
        chunks.add(current);
        for (ByteCodeClass cls : sorted) {
            String code = cls.generateJavascriptCode(classes);
            if (current.length() > 0 && current.length() + code.length() > CLASS_CHUNK_MAX_BYTES) {
                current = new StringBuilder();
                chunks.add(current);
            }
            current.append(code).append('\n');
        }

        StringBuilder tail = chunks.get(chunks.size() - 1);
        ByteCodeClass mainClass = ByteCodeClass.getMainClass();
        if (mainClass != null) {
            tail.append("jvm.setMain(\"").append(mainClass.getClsName()).append("\", \"")
                    .append(JavascriptNameUtil.methodIdentifier(mainClass.getClsName(), "main", "([Ljava/lang/String;)V"))
                    .append("\");\n");
        }

        // Lead chunks use zero-padded suffixes so writeWorker's lexicographic
        // scan of top-level *.js files imports them in the intended order
        // (they're all independent class definitions so the relative order
        // among them doesn't matter for correctness, but stable ordering
        // keeps debug output deterministic).
        int leadCount = chunks.size() - 1;
        for (int i = 0; i < leadCount; i++) {
            String suffix = leadCount >= 10 ? String.format("_%02d", i + 1) : String.format("_%d", i + 1);
            Files.write(new File(outputDirectory, "translated_app" + suffix + ".js").toPath(),
                    chunks.get(i).toString().getBytes(StandardCharsets.UTF_8));
        }
        Files.write(new File(outputDirectory, "translated_app.js").toPath(),
                tail.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static int bootstrapPriority(ByteCodeClass cls) {
        String name = cls.getClsName();
        if ("java_lang_Object".equals(name)) {
            return 0;
        }
        if ("java_lang_Class".equals(name)) {
            return 1;
        }
        if ("java_lang_String".equals(name)) {
            return 2;
        }
        if ("java_lang_Throwable".equals(name)) {
            return 3;
        }
        if (name.startsWith("java_lang_String_")) {
            return 4;
        }
        if (name.startsWith("java_lang_")) {
            return 5;
        }
        return 10;
    }

    private static void writeWorker(File outputDirectory) throws IOException {
        List<String> nativeScripts = new ArrayList<String>();
        List<String> classChunkScripts = new ArrayList<String>();
        File[] files = outputDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (!name.endsWith(".js")) {
                    continue;
                }
                if ("parparvm_runtime.js".equals(name)
                        || "translated_app.js".equals(name)
                        || "worker.js".equals(name)
                        || "sw.js".equals(name)
                        || "browser_bridge.js".equals(name)) {
                    continue;
                }
                // translated_app_NN.js are class-definition chunks split off
                // from translated_app.js for Cloudflare Pages' per-file size
                // limit. Group them separately so they load *before*
                // translated_app.js (which contains the trailing jvm.setMain
                // call) but *after* other runtime helpers / native shims.
                if (name.startsWith("translated_app_") && name.endsWith(".js")) {
                    classChunkScripts.add(name);
                } else {
                    nativeScripts.add(name);
                }
            }
        }
        // Deterministic order across OSes — listFiles() doesn't guarantee any.
        Collections.sort(nativeScripts);
        Collections.sort(classChunkScripts);

        StringBuilder imports = new StringBuilder();
        imports.append("importScripts('parparvm_runtime.js');\n");
        for (String script : nativeScripts) {
            imports.append("importScripts('").append(script).append("');\n");
        }
        for (String script : classChunkScripts) {
            imports.append("importScripts('").append(script).append("');\n");
        }
        imports.append("importScripts('translated_app.js');\n");

        String worker = loadResource("worker.js").replace("/*__IMPORTS__*/", imports.toString().trim());
        Files.write(new File(outputDirectory, "worker.js").toPath(), worker.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeIndex(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "index.html", "index.html");
    }

    private static void writeBrowserBridge(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "browser_bridge.js", "browser_bridge.js");
    }

    private static void writeProtocol(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "vm_protocol.md", "vm_protocol.md");
    }

    private static void copyJavaScriptPortWebAppAssets(File outputDirectory) throws IOException {
        Path webApp = locateJavaScriptPortWebApp();
        if (webApp == null) {
            return;
        }
        copyPathIfPresent(webApp.resolve("js"), outputDirectory.toPath().resolve("js"));
        copyPathIfPresent(webApp.resolve("css"), outputDirectory.toPath().resolve("css"));
        copyPathIfPresent(webApp.resolve("assets"), outputDirectory.toPath().resolve("assets"));
        copyPathIfPresent(webApp.resolve("style.css"), outputDirectory.toPath().resolve("style.css"));
        copyPathIfPresent(webApp.resolve("progress.gif"), outputDirectory.toPath().resolve("progress.gif"));
        copyPathIfPresent(webApp.resolve("manifest.json"), outputDirectory.toPath().resolve("manifest.json"));
        copyPathIfPresent(webApp.resolve("sw.js"), outputDirectory.toPath().resolve("sw.js"));
        copyPathIfPresent(webApp.resolve("port.js"), outputDirectory.toPath().resolve("port.js"));
    }

    private static Path locateJavaScriptPortWebApp() {
        String override = System.getProperty("codename1.javascriptport.webapp");
        if (override != null && !override.trim().isEmpty()) {
            Path path = Paths.get(override.trim());
            if (Files.isDirectory(path)) {
                return path;
            }
        }

        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(Paths.get("Ports", "JavaScriptPort", "src", "main", "webapp"));
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void copyPathIfPresent(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
                for (Path child : stream) {
                    Path childName = child.getFileName();
                    if (childName != null) {
                        copyPathIfPresent(child, target.resolve(childName.toString()));
                    }
                }
            }
            return;
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeResource(File outputDirectory, String targetName, String resourceName) throws IOException {
        Files.write(new File(outputDirectory, targetName).toPath(),
                loadResource(resourceName).getBytes(StandardCharsets.UTF_8));
    }

    private static String loadResource(String resourceName) throws IOException {
        InputStream input = JavascriptBundleWriter.class.getResourceAsStream(RESOURCE_ROOT + resourceName);
        if (input == null) {
            throw new IOException("Missing javascript backend resource " + resourceName);
        }
        try {
            byte[] data = new byte[8192];
            StringBuilder out = new StringBuilder();
            int len;
            while ((len = input.read(data)) > -1) {
                out.append(new String(data, 0, len, StandardCharsets.UTF_8));
            }
            return out.toString();
        } finally {
            input.close();
        }
    }
}
