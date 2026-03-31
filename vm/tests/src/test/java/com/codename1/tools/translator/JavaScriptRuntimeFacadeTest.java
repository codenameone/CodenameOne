package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptRuntimeFacadeTest {
    private static final Path FACADE_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptRuntimeFacade.java");
    private static final Path HTML5_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "HTML5Implementation.java");
    private static final Path BOOTSTRAP_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptPortBootstrap.java");

    @Test
    void extractedFacadeCompilesAndPreservesKeyRuntimeRules() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-runtime-facade-src");
        Path classesDir = Files.createTempDirectory("js-runtime-facade-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(FACADE_SOURCE, packageDir.resolve("JavaScriptRuntimeFacade.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptRuntimeFacade.java").toString()
        ));
        assertEquals(0, compileResult, "Runtime facade should compile as a standalone pure-Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> facadeClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRuntimeFacade");

        Method wrapStorageKey = facadeClass.getMethod("wrapStorageKey", String.class);
        Method unwrapStorageKey = facadeClass.getMethod("unwrapStorageKey", String.class);
        Method wrapFile = facadeClass.getMethod("wrapFile", String.class);
        Method unwrapFile = facadeClass.getMethod("unwrapFile", String.class);
        Method stripTrailingSlash = facadeClass.getMethod("stripTrailingSlash", String.class);
        Method isRootFile = facadeClass.getMethod("isRootFile", String.class);
        Method shouldProxyUrl = facadeClass.getMethod("shouldProxyUrl", String.class, String.class, boolean.class, boolean.class, String.class);

        assertEquals("storage/theme.res", wrapStorageKey.invoke(null, "theme.res"));
        assertEquals("theme.res", unwrapStorageKey.invoke(null, "storage/theme.res"));
        assertEquals("cn1fs/foo/bar.txt", wrapFile.invoke(null, "file:///foo/bar.txt"));
        assertEquals("file:///foo/bar.txt", unwrapFile.invoke(null, "cn1fs/foo/bar.txt"));
        assertEquals("/foo", stripTrailingSlash.invoke(null, "/foo///"));
        assertEquals(Boolean.TRUE, isRootFile.invoke(null, "/"));
        assertEquals(Boolean.TRUE, shouldProxyUrl.invoke(null, "https://example.com/data", "", false, false, "https://proxy/?target="));
        assertEquals(Boolean.FALSE, shouldProxyUrl.invoke(null, "https://example.com/data", "https://example.com", false, false, "https://proxy/?target="));
        assertEquals(Boolean.FALSE, shouldProxyUrl.invoke(null, "file:///theme.res", "", false, false, "https://proxy/?target="));
    }

    @Test
    void html5ImplementationAndBootstrapDelegateToRuntimeFacade() throws Exception {
        String html5Source = new String(Files.readAllBytes(HTML5_SOURCE), StandardCharsets.UTF_8);
        String bootstrapSource = new String(Files.readAllBytes(BOOTSTRAP_SOURCE), StandardCharsets.UTF_8);

        assertTrue(html5Source.contains("JavaScriptRuntimeFacade.wrapStorageKey(name)"),
                "HTML5Implementation should delegate storage key wrapping to the runtime facade");
        assertTrue(html5Source.contains("JavaScriptRuntimeFacade.wrapFile(path)"),
                "HTML5Implementation should delegate filesystem path wrapping to the runtime facade");
        assertTrue(html5Source.contains("JavaScriptRuntimeFacade.unwrapStorageEntries(getLocalForage().keys())"),
                "HTML5Implementation should delegate storage key filtering to the runtime facade");
        assertTrue(bootstrapSource.contains("JavaScriptRuntimeFacade.proxifyUrl("),
                "JavaScriptPortBootstrap should delegate proxy decisions to the runtime facade");
    }
}
