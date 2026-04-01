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
    private static final Path BOOTSTRAP_COORDINATOR_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptBootstrapCoordinator.java");
    private static final Path INITIALIZATION_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptInitializationAdapter.java");
    private static final Path RUNTIME_ENVIRONMENT_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptRuntimeEnvironment.java");
    private static final Path INPUT_COORDINATOR_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptInputCoordinator.java");
    private static final Path STORAGE_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptStorageAdapter.java");
    private static final Path NETWORK_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptNetworkAdapter.java");
    private static final Path HTML5_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "HTML5Implementation.java");
    private static final Path BOOTSTRAP_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptPortBootstrap.java");
    private static final Path STUB_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "Stub.java");

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
        String stubSource = new String(Files.readAllBytes(STUB_SOURCE), StandardCharsets.UTF_8);

        assertTrue(html5Source.contains("JavaScriptRuntimeFacade.wrapStorageKey(name)"),
                "HTML5Implementation should delegate storage key wrapping to the runtime facade");
        assertTrue(html5Source.contains("JavaScriptRuntimeFacade.wrapFile(path)"),
                "HTML5Implementation should delegate filesystem path wrapping to the runtime facade");
        assertTrue(html5Source.contains("JavaScriptStorageAdapter.listStorageEntries(createStorageBackend())"),
                "HTML5Implementation should delegate storage entry listing to the storage adapter");
        assertTrue(html5Source.contains("JavaScriptStorageAdapter.createStorageOutputStream(createStorageBackend(), name)"),
                "HTML5Implementation should delegate storage writes to the storage adapter");
        assertTrue(html5Source.contains("JavaScriptNetworkAdapter.connect(url, read, write, timeout"),
                "HTML5Implementation should delegate connection setup to the network adapter");
        assertTrue(html5Source.contains("JavaScriptBootstrapCoordinator.bindMainClass(main"),
                "HTML5Implementation should delegate main-class bootstrap binding to the bootstrap coordinator");
        assertTrue(html5Source.contains("JavaScriptInitializationAdapter.applyEnvironment("),
                "HTML5Implementation should delegate environment property setup to the initialization adapter");
        assertTrue(html5Source.contains("JavaScriptInitializationAdapter.resolveAppArg(environment)"),
                "HTML5Implementation should delegate app-arg resolution to the initialization adapter");
        assertTrue(html5Source.contains("JavaScriptInitializationAdapter.runPostInit("),
                "HTML5Implementation should delegate post-init hooks to the initialization adapter");
        assertTrue(html5Source.contains("JavaScriptInputCoordinator.shouldInstallKeyboard(isPhoneOrTablet_())"),
                "HTML5Implementation should delegate keyboard-install gating to the input coordinator");
        assertTrue(html5Source.contains("JavaScriptInputCoordinator.beginPointerRouting("),
                "HTML5Implementation should delegate pointer routing decisions to the input coordinator");
        assertTrue(html5Source.contains("new JavaScriptRuntimeEnvironment("),
                "HTML5Implementation should build a compact runtime environment object");
        assertTrue(bootstrapSource.contains("JavaScriptRuntimeFacade.proxifyUrl("),
                "JavaScriptPortBootstrap should delegate proxy decisions to the runtime facade");
        assertTrue(bootstrapSource.contains("JavaScriptBootstrapCoordinator.createLifecycle(className)"),
                "JavaScriptPortBootstrap should delegate lifecycle creation to the bootstrap coordinator");
        assertTrue(stubSource.contains("JavaScriptBootstrapCoordinator.APP_CLASS_PROPERTY"),
                "Stub should read the shared JavaScript bootstrap app-class property");
    }

    @Test
    void extractedStorageAndNetworkAdaptersCompileAndPreserveMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-runtime-adapters-src");
        Path classesDir = Files.createTempDirectory("js-runtime-adapters-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(FACADE_SOURCE, packageDir.resolve("JavaScriptRuntimeFacade.java"));
        Files.copy(STORAGE_ADAPTER_SOURCE, packageDir.resolve("JavaScriptStorageAdapter.java"));
        Files.copy(NETWORK_ADAPTER_SOURCE, packageDir.resolve("JavaScriptNetworkAdapter.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptRuntimeFacade.java").toString(),
                packageDir.resolve("JavaScriptStorageAdapter.java").toString(),
                packageDir.resolve("JavaScriptNetworkAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "Extracted storage/network helpers should compile as standalone Java helpers");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> storageAdapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptStorageAdapter");
        Class<?> networkAdapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptNetworkAdapter");
        Class<?> backendInterface = loader.loadClass("com.codename1.impl.html5.JavaScriptStorageAdapter$Backend");
        Class<?> transformerInterface = loader.loadClass("com.codename1.impl.html5.JavaScriptNetworkAdapter$UrlTransformer");
        Class<?> connectionFactoryInterface = loader.loadClass("com.codename1.impl.html5.JavaScriptNetworkAdapter$ConnectionFactory");
        Class<?> fileProviderInterface = loader.loadClass("com.codename1.impl.html5.JavaScriptNetworkAdapter$FileOutputStreamProvider");

        final java.util.Map<String, byte[]> storage = new java.util.LinkedHashMap<String, byte[]>();
        Object backend = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{backendInterface}, (proxy, method, args) -> {
            String name = method.getName();
            if ("removeItem".equals(name)) {
                storage.remove((String) args[0]);
                return null;
            }
            if ("openOutputStream".equals(name)) {
                final String key = (String) args[0];
                return new java.io.ByteArrayOutputStream() {
                    @Override
                    public void close() throws java.io.IOException {
                        super.close();
                        storage.put(key, toByteArray());
                    }
                };
            }
            if ("openInputStream".equals(name)) {
                byte[] data = storage.get((String) args[0]);
                return new java.io.ByteArrayInputStream(data == null ? new byte[0] : data);
            }
            if ("getItem".equals(name)) {
                return storage.get((String) args[0]);
            }
            if ("getSize".equals(name)) {
                byte[] data = storage.get((String) args[0]);
                return data == null ? 0 : data.length;
            }
            if ("keys".equals(name)) {
                return storage.keySet().toArray(new String[storage.size()]);
            }
            throw new UnsupportedOperationException(name);
        });

        Method createStorageOutputStream = storageAdapterClass.getMethod("createStorageOutputStream", backendInterface, String.class);
        Method createStorageInputStream = storageAdapterClass.getMethod("createStorageInputStream", backendInterface, String.class);
        Method storageFileExists = storageAdapterClass.getMethod("storageFileExists", backendInterface, String.class);
        Method getStorageEntrySize = storageAdapterClass.getMethod("getStorageEntrySize", backendInterface, String.class);
        Method listStorageEntries = storageAdapterClass.getMethod("listStorageEntries", backendInterface);
        Method deleteStorageFile = storageAdapterClass.getMethod("deleteStorageFile", backendInterface, String.class);

        java.io.OutputStream out = (java.io.OutputStream) createStorageOutputStream.invoke(null, backend, "theme.res");
        out.write("abc".getBytes(StandardCharsets.UTF_8));
        out.close();
        assertEquals(Boolean.TRUE, storageFileExists.invoke(null, backend, "theme.res"));
        assertEquals(3, getStorageEntrySize.invoke(null, backend, "theme.res"));
        java.io.InputStream in = (java.io.InputStream) createStorageInputStream.invoke(null, backend, "theme.res");
        byte[] read = new byte[3];
        assertEquals(3, in.read(read));
        assertEquals("abc", new String(read, StandardCharsets.UTF_8));
        assertEquals("theme.res", ((String[]) listStorageEntries.invoke(null, backend))[0]);
        deleteStorageFile.invoke(null, backend, "theme.res");
        assertEquals(Boolean.FALSE, storageFileExists.invoke(null, backend, "theme.res"));

        Object transformer = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{transformerInterface}, (proxy, method, args) -> "proxy:" + args[0]);
        Object factory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{connectionFactoryInterface}, (proxy, method, args) -> args[0] + "|timeout=" + args[3]);
        Method connect = networkAdapterClass.getMethod("connect", String.class, boolean.class, boolean.class, int.class, transformerInterface, connectionFactoryInterface);
        assertEquals("proxy:https://example.com|timeout=25", connect.invoke(null, "https://example.com", true, false, 25, transformer, factory));

        Object fileProvider = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{fileProviderInterface}, (proxy, method, args) -> new java.io.ByteArrayOutputStream());
        Method openOutputStream = networkAdapterClass.getMethod("openOutputStream", Object.class, fileProviderInterface);
        assertTrue(openOutputStream.invoke(null, "file:///tmp/test.txt", fileProvider) instanceof java.io.OutputStream,
                "Network adapter should route String connections through the file output provider");
    }

    @Test
    void extractedInitializationAdapterCompilesAndAppliesRuntimeEnvironment() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-runtime-init-src");
        Path classesDir = Files.createTempDirectory("js-runtime-init-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(RUNTIME_ENVIRONMENT_SOURCE, packageDir.resolve("JavaScriptRuntimeEnvironment.java"));
        Files.copy(INITIALIZATION_ADAPTER_SOURCE, packageDir.resolve("JavaScriptInitializationAdapter.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptRuntimeEnvironment.java").toString(),
                packageDir.resolve("JavaScriptInitializationAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "Initialization adapter should compile as a standalone Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> initClass = loader.loadClass("com.codename1.impl.html5.JavaScriptInitializationAdapter");
        Class<?> propertySinkInterface = loader.loadClass("com.codename1.impl.html5.JavaScriptInitializationAdapter$PropertySink");
        Class<?> environmentClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRuntimeEnvironment");
        Class<?> runtimeHooksInterface = loader.loadClass("com.codename1.impl.html5.JavaScriptInitializationAdapter$RuntimeHooks");

        final java.util.Map<String, String> properties = new java.util.LinkedHashMap<String, String>();
        Object propertySink = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{propertySinkInterface}, (proxy, method, args) -> {
            properties.put((String) args[0], (String) args[1]);
            return null;
        });
        Object environment = environmentClass.getConstructor(String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class)
                .newInstance("MacIntel", "Mozilla/Test", "en-US", "Netscape", "Mozilla", "5.0", "directory", "https://example.com/app/index.html");

        Method applyEnvironment = initClass.getMethod("applyEnvironment", propertySinkInterface, environmentClass);
        Method resolveAppArg = initClass.getMethod("resolveAppArg", environmentClass);
        Method runPostInit = initClass.getMethod("runPostInit", runtimeHooksInterface, boolean.class);

        applyEnvironment.invoke(null, propertySink, environment);
        assertEquals("MacIntel", properties.get("Platform"));
        assertEquals("Mozilla/Test", properties.get("User-Agent"));
        assertEquals("en-US", properties.get("browser.language"));
        assertEquals("Netscape", properties.get("browser.name"));
        assertEquals("Mozilla", properties.get("browser.codeName"));
        assertEquals("5.0", properties.get("browser.version"));
        assertEquals("JS", properties.get("OS"));
        assertEquals("1.0", properties.get("OSVer"));
        assertEquals("directory", properties.get("javascript.deployment.type"));
        assertEquals("https://example.com/app/index.html", resolveAppArg.invoke(null, environment));

        final java.util.List<String> hookCalls = new java.util.ArrayList<String>();
        Object hooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{runtimeHooksInterface}, (proxy, method, args) -> {
            hookCalls.add(method.getName() + (args == null || args.length == 0 ? "" : ":" + args[0]));
            return null;
        });
        runPostInit.invoke(null, hooks, false);
        assertTrue(hookCalls.contains("setDragStartPercentage:1"));
        assertTrue(hookCalls.contains("initVideoCaptureConstraints"));
        assertTrue(hookCalls.contains("registerSaveBlobToFile"));
        assertTrue(hookCalls.contains("initGoogle"));

        hookCalls.clear();
        runPostInit.invoke(null, hooks, true);
        assertTrue(!hookCalls.contains("initVideoCaptureConstraints"),
                "Initialization adapter should skip video capture setup on iOS");
    }

    @Test
    void extractedInputCoordinatorCompilesAndPreservesPointerRules() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-input-coordinator-src");
        Path classesDir = Files.createTempDirectory("js-input-coordinator-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(INPUT_COORDINATOR_SOURCE, packageDir.resolve("JavaScriptInputCoordinator.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptInputCoordinator.java").toString()
        ));
        assertEquals(0, compileResult, "Input coordinator should compile as a standalone Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> inputClass = loader.loadClass("com.codename1.impl.html5.JavaScriptInputCoordinator");
        Class<?> routingDecisionClass = loader.loadClass("com.codename1.impl.html5.JavaScriptInputCoordinator$PointerRoutingDecision");
        Class<?> touchDecisionClass = loader.loadClass("com.codename1.impl.html5.JavaScriptInputCoordinator$TouchStartDecision");

        Method shouldInstallKeyboard = inputClass.getMethod("shouldInstallKeyboard", boolean.class);
        Method beginPointerRouting = inputClass.getMethod("beginPointerRouting", boolean.class, boolean.class);
        Method shouldIgnoreMousePress = inputClass.getMethod("shouldIgnoreMousePress", boolean.class, boolean.class, boolean.class);
        Method resolveTouchStart = inputClass.getMethod("resolveTouchStart", boolean.class, boolean.class, boolean.class, boolean.class);
        Method shouldCreatePreemptiveTextField = inputClass.getMethod("shouldCreatePreemptiveTextField", boolean.class, long.class, long.class, int.class, int.class, int.class, int.class);

        assertEquals(Boolean.TRUE, shouldInstallKeyboard.invoke(null, true));
        assertEquals(Boolean.FALSE, shouldInstallKeyboard.invoke(null, false));

        Object routing = beginPointerRouting.invoke(null, true, false);
        assertEquals(Boolean.FALSE, routingDecisionClass.getMethod("shouldConsumeEvent").invoke(routing));
        assertEquals(Boolean.FALSE, routingDecisionClass.getMethod("grabbedDrag").invoke(routing));
        routing = beginPointerRouting.invoke(null, false, false);
        assertEquals(Boolean.TRUE, routingDecisionClass.getMethod("shouldConsumeEvent").invoke(routing));
        assertEquals(Boolean.TRUE, routingDecisionClass.getMethod("grabbedDrag").invoke(routing));

        assertEquals(Boolean.TRUE, shouldIgnoreMousePress.invoke(null, true, false, false));
        assertEquals(Boolean.TRUE, shouldIgnoreMousePress.invoke(null, false, true, false));
        assertEquals(Boolean.FALSE, shouldIgnoreMousePress.invoke(null, false, false, false));

        Object touch = resolveTouchStart.invoke(null, true, false, false, false);
        assertEquals(Boolean.FALSE, touchDecisionClass.getMethod("shouldFirePointerPressed").invoke(touch));
        assertEquals(Boolean.TRUE, touchDecisionClass.getMethod("shouldCancelMouseTracking").invoke(touch));
        assertEquals(Boolean.FALSE, touchDecisionClass.getMethod("shouldIgnoreEvent").invoke(touch));
        touch = resolveTouchStart.invoke(null, false, true, false, false);
        assertEquals(Boolean.TRUE, touchDecisionClass.getMethod("shouldIgnoreEvent").invoke(touch));

        assertEquals(Boolean.TRUE, shouldCreatePreemptiveTextField.invoke(null, true, 1000L, 1100L, 10, 10, 15, 15));
        assertEquals(Boolean.FALSE, shouldCreatePreemptiveTextField.invoke(null, true, 1000L, 1300L, 10, 10, 15, 15));
    }
}
