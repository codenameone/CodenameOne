package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptCn1CoreNativeAuditTest {

    @Test
    void allCodenameOneCoreNativesAreCategorizedForJavascript() throws Exception {
        Path coreJar = findDependencyJar("codenameone-core");
        assertNotNull(coreJar, "codenameone-core dependency jar should be available in target/benchmark-dependencies");

        List<String> uncategorized = new ArrayList<String>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(coreJar))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                if (!entry.getName().startsWith("com/codename1/")) {
                    continue;
                }
                JavascriptNativeAuditSupport.inspectClass(new NonClosingInputStream(zis), uncategorized);
            }
        }

        Collections.sort(uncategorized);
        assertTrue(uncategorized.isEmpty(),
                "Every codenameone-core native reachable by the JS VM must be categorized. Missing: " + uncategorized);
    }

    private static Path findDependencyJar(String namePart) throws Exception {
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

    private static final class NonClosingInputStream extends InputStream {
        private final InputStream delegate;

        private NonClosingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws java.io.IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() {
        }
    }
}
