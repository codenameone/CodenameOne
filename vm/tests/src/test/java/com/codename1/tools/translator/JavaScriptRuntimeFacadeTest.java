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
    private static final Path POINTER_STATE_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptPointerSessionState.java");
    private static final Path EVENT_WIRING_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptEventWiring.java");
    private static final Path BROWSER_INTERACTION_COORDINATOR_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptBrowserInteractionCoordinator.java");
    private static final Path KEYBOARD_INTERACTION_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptKeyboardInteractionAdapter.java");
    private static final Path BROWSER_LIFECYCLE_COORDINATOR_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptBrowserLifecycleCoordinator.java");
    private static final Path CANVAS_LAYOUT_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptCanvasLayout.java");
    private static final Path RENDER_QUEUE_COORDINATOR_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptRenderQueueCoordinator.java");
    private static final Path RENDER_QUEUE_STATE_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptRenderQueueState.java");
    private static final Path RENDER_STATE_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptRenderState.java");
    private static final Path PRIMITIVE_RENDER_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptPrimitiveRenderAdapter.java");
    private static final Path IMAGE_TRANSFORM_RENDER_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptImageTransformRenderAdapter.java");
    private static final Path SHAPE_GRADIENT_RENDER_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptShapeGradientRenderAdapter.java");
    private static final Path TEXT_METRICS_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptTextMetricsAdapter.java");
    private static final Path CANVAS_IMAGE_BUFFER_LIFECYCLE_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptCanvasImageBufferLifecycle.java");
    private static final Path EXECUTABLE_OP_FACTORY_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptExecutableOpFactory.java");
    private static final Path STORAGE_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptStorageAdapter.java");
    private static final Path NETWORK_ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptNetworkAdapter.java");
    private static final Path HTML5_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "HTML5Implementation.java");
    private static final Path HTML5_GRAPHICS_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "HTML5Graphics.java");
    private static final Path BUFFERED_GRAPHICS_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "BufferedGraphics.java");
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
        String html5GraphicsSource = new String(Files.readAllBytes(HTML5_GRAPHICS_SOURCE), StandardCharsets.UTF_8);
        String bufferedGraphicsSource = new String(Files.readAllBytes(BUFFERED_GRAPHICS_SOURCE), StandardCharsets.UTF_8);
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
        assertTrue(html5Source.contains("new JavaScriptPointerSessionState()"),
                "HTML5Implementation should store pointer lifecycle state in a dedicated session object");
        assertTrue(html5Source.contains("JavaScriptEventWiring.registerDocumentEvents("),
                "HTML5Implementation should delegate document event registration to the event-wiring helper");
        assertTrue(html5Source.contains("JavaScriptEventWiring.registerPeerPointerEvents("),
                "HTML5Implementation should delegate peer pointer registration to the event-wiring helper");
        assertTrue(html5Source.contains("JavaScriptEventWiring.registerCoreWindowEvents("),
                "HTML5Implementation should delegate core window event registration to the event-wiring helper");
        assertTrue(html5Source.contains("JavaScriptBrowserInteractionCoordinator.handleResize("),
                "HTML5Implementation should delegate resize handling to the browser interaction coordinator");
        assertTrue(html5Source.contains("JavaScriptBrowserInteractionCoordinator.handleHover("),
                "HTML5Implementation should delegate hover/cursor handling to the browser interaction coordinator");
        assertTrue(html5Source.contains("JavaScriptKeyboardInteractionAdapter.handleKeyDown("),
                "HTML5Implementation should delegate keydown semantics to the keyboard interaction adapter");
        assertTrue(html5Source.contains("JavaScriptKeyboardInteractionAdapter.handleKeyUp("),
                "HTML5Implementation should delegate keyup semantics to the keyboard interaction adapter");
        assertTrue(html5Source.contains("JavaScriptKeyboardInteractionAdapter.handleKeyPress("),
                "HTML5Implementation should delegate keypress semantics to the keyboard interaction adapter");
        assertTrue(html5Source.contains("JavaScriptBrowserLifecycleCoordinator.handleInboxEvent("),
                "HTML5Implementation should delegate inbox listener handling to the browser lifecycle coordinator");
        assertTrue(html5Source.contains("JavaScriptBrowserLifecycleCoordinator.handlePopState("),
                "HTML5Implementation should delegate popstate handling to the browser lifecycle coordinator");
        assertTrue(html5Source.contains("JavaScriptBrowserLifecycleCoordinator.handlePaste("),
                "HTML5Implementation should delegate paste handling to the browser lifecycle coordinator");
        assertTrue(html5Source.contains("new JavaScriptRenderQueueState<ExecutableOp>()"),
                "HTML5Implementation should keep pending draw state in a dedicated render-queue state object");
        assertTrue(html5Source.contains("JavaScriptCanvasLayout.compute("),
                "HTML5Implementation should delegate canvas sizing to the canvas layout helper");
        assertTrue(html5Source.contains("JavaScriptRenderQueueCoordinator.waitUntilFlushable("),
                "HTML5Implementation should delegate flush gating to the render queue coordinator");
        assertTrue(html5Source.contains("JavaScriptRenderQueueCoordinator.queueFlush("),
                "HTML5Implementation should delegate pending-op queue handoff to the render queue coordinator");
        assertTrue(html5Source.contains("JavaScriptRenderQueueCoordinator.beginFrame("),
                "HTML5Implementation should delegate animation-frame snapshots to the render queue coordinator");
        assertTrue(html5GraphicsSource.contains("new JavaScriptRenderState<NativeFont>()"),
                "HTML5Graphics should keep render color/alpha/font/clip state in a dedicated render-state object");
        assertTrue(html5GraphicsSource.contains("new JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp>("),
                "HTML5Graphics should delegate primitive draw ops to the primitive render adapter");
        assertTrue(html5GraphicsSource.contains("primitiveRenderAdapter.drawRect("),
                "HTML5Graphics should route drawRect through the primitive render adapter");
        assertTrue(html5GraphicsSource.contains("primitiveRenderAdapter.fillRect("),
                "HTML5Graphics should route fillRect through the primitive render adapter");
        assertTrue(html5GraphicsSource.contains("primitiveRenderAdapter.drawLine("),
                "HTML5Graphics should route drawLine through the primitive render adapter");
        assertTrue(html5GraphicsSource.contains("primitiveRenderAdapter.drawString("),
                "HTML5Graphics should route drawString through the primitive render adapter");
        assertTrue(html5GraphicsSource.contains("primitiveRenderAdapter.setClipRect("),
                "HTML5Graphics should route clip-rect updates through the primitive render adapter");
        assertTrue(html5GraphicsSource.contains("new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>("),
                "HTML5Graphics should delegate image, transform, and clip-shape ops to the shared image/transform adapter");
        assertTrue(html5GraphicsSource.contains("imageTransformRenderAdapter.drawImage("),
                "HTML5Graphics should route image draws through the shared image/transform adapter");
        assertTrue(html5GraphicsSource.contains("imageTransformRenderAdapter.tileImage("),
                "HTML5Graphics should route image tiling through the shared image/transform adapter");
        assertTrue(html5GraphicsSource.contains("imageTransformRenderAdapter.applyTransform("),
                "HTML5Graphics should route transform application through the shared image/transform adapter");
        assertTrue(html5GraphicsSource.contains("imageTransformRenderAdapter.setClipShape("),
                "HTML5Graphics should route clip-shape updates through the shared image/transform adapter");
        assertTrue(html5GraphicsSource.contains("new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>("),
                "HTML5Graphics should delegate shape and gradient ops to the shared shape/gradient adapter");
        assertTrue(html5GraphicsSource.contains("shapeGradientRenderAdapter.drawShape("),
                "HTML5Graphics should route drawShape through the shape/gradient adapter");
        assertTrue(html5GraphicsSource.contains("shapeGradientRenderAdapter.fillShape("),
                "HTML5Graphics should route fillShape through the shape/gradient adapter");
        assertTrue(html5GraphicsSource.contains("shapeGradientRenderAdapter.fillLinearGradient("),
                "HTML5Graphics should route linear gradients through the shape/gradient adapter");
        assertTrue(html5GraphicsSource.contains("shapeGradientRenderAdapter.fillRadialGradient("),
                "HTML5Graphics should route radial gradients through the shape/gradient adapter");
        assertTrue(html5GraphicsSource.contains("JavaScriptTextMetricsAdapter.charsWidth("),
                "HTML5Graphics should delegate char measurement to the text metrics adapter");
        assertTrue(html5GraphicsSource.contains("JavaScriptTextMetricsAdapter.stringWidth("),
                "HTML5Graphics should delegate string measurement to the text metrics adapter");
        assertTrue(html5GraphicsSource.contains("JavaScriptTextMetricsAdapter.getFontHeight("),
                "HTML5Graphics should delegate font height to the text metrics adapter");
        assertTrue(html5GraphicsSource.contains("JavaScriptTextMetricsAdapter.getFontDescent("),
                "HTML5Graphics should delegate font descent to the text metrics adapter");
        assertTrue(bufferedGraphicsSource.contains("new JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp>("),
                "BufferedGraphics should share the primitive render adapter for buffered primitive ops");
        assertTrue(bufferedGraphicsSource.contains("new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>("),
                "BufferedGraphics should share the image/transform render adapter for buffered image and transform ops");
        assertTrue(bufferedGraphicsSource.contains("new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>("),
                "BufferedGraphics should share the shape/gradient render adapter for buffered shape and gradient ops");
        assertTrue(html5Source.contains("JavaScriptCanvasImageBufferLifecycle.ensureScratchBuffer("),
                "HTML5Implementation should delegate scratch canvas lifecycle to the buffer lifecycle helper");
        assertTrue(html5Source.contains("JavaScriptCanvasImageBufferLifecycle.createBlankBuffer("),
                "HTML5Implementation should delegate blank canvas-backed image buffers to the buffer lifecycle helper");
        assertTrue(html5Source.contains("JavaScriptCanvasImageBufferLifecycle.createMutableBuffer("),
                "HTML5Implementation should delegate mutable canvas-backed image buffers to the buffer lifecycle helper");
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

    @Test
    void extractedPointerStateAndEventWiringCompileAndPreserveMinimalContracts() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-pointer-state-src");
        Path classesDir = Files.createTempDirectory("js-pointer-state-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(POINTER_STATE_SOURCE, packageDir.resolve("JavaScriptPointerSessionState.java"));
        Files.copy(EVENT_WIRING_SOURCE, packageDir.resolve("JavaScriptEventWiring.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptPointerSessionState.java").toString(),
                packageDir.resolve("JavaScriptEventWiring.java").toString()
        ));
        assertEquals(0, compileResult, "Pointer state and event wiring helpers should compile as standalone Java helpers");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> pointerStateClass = loader.loadClass("com.codename1.impl.html5.JavaScriptPointerSessionState");
        Object state = pointerStateClass.getConstructor().newInstance();
        pointerStateClass.getMethod("setMouseDown", boolean.class).invoke(state, true);
        pointerStateClass.getMethod("setTouchDown", boolean.class).invoke(state, true);
        pointerStateClass.getMethod("setGrabbedDrag", boolean.class).invoke(state, true);
        pointerStateClass.getMethod("setCapturingEvents", boolean.class).invoke(state, false);
        pointerStateClass.getMethod("setLastMousePosition", int.class, int.class).invoke(state, 10, 20);
        pointerStateClass.getMethod("setLastTouchUpPosition", int.class, int.class).invoke(state, 30, 40);
        pointerStateClass.getMethod("setTouchStart", int.class, int.class, long.class).invoke(state, 50, 60, 70L);
        pointerStateClass.getMethod("setTouches", int[].class, int[].class).invoke(state, new int[]{1, 2}, new int[]{3, 4});
        assertEquals(Boolean.TRUE, pointerStateClass.getMethod("isMouseDown").invoke(state));
        assertEquals(Boolean.TRUE, pointerStateClass.getMethod("isTouchDown").invoke(state));
        assertEquals(Boolean.TRUE, pointerStateClass.getMethod("isGrabbedDrag").invoke(state));
        assertEquals(Boolean.FALSE, pointerStateClass.getMethod("isCapturingEvents").invoke(state));
        assertEquals(10, pointerStateClass.getMethod("getLastMouseX").invoke(state));
        assertEquals(20, pointerStateClass.getMethod("getLastMouseY").invoke(state));
        assertEquals(30, pointerStateClass.getMethod("getLastTouchUpX").invoke(state));
        assertEquals(40, pointerStateClass.getMethod("getLastTouchUpY").invoke(state));
        assertEquals(50, pointerStateClass.getMethod("getTouchStartX").invoke(state));
        assertEquals(60, pointerStateClass.getMethod("getTouchStartY").invoke(state));
        assertEquals(70L, pointerStateClass.getMethod("getTouchStartTime").invoke(state));

        Class<?> wiringClass = loader.loadClass("com.codename1.impl.html5.JavaScriptEventWiring");
        Class<?> windowRegistrarClass = loader.loadClass("com.codename1.impl.html5.JavaScriptEventWiring$WindowRegistrar");
        Class<?> documentRegistrarClass = loader.loadClass("com.codename1.impl.html5.JavaScriptEventWiring$DocumentRegistrar");
        Class<?> elementRegistrarClass = loader.loadClass("com.codename1.impl.html5.JavaScriptEventWiring$ElementRegistrar");

        final java.util.List<String> windowEvents = new java.util.ArrayList<String>();
        Object windowRegistrar = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{windowRegistrarClass}, (proxy, method, args) -> {
            windowEvents.add(args[0] + ":" + args[2]);
            return null;
        });
        wiringClass.getMethod("registerCoreWindowEvents", windowRegistrarClass, boolean.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class)
                .invoke(null, windowRegistrar, true, new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        assertTrue(windowEvents.contains("cn1inbox:false"));
        assertTrue(windowEvents.contains("popstate:true"));
        assertTrue(windowEvents.contains("mousemove:false"));

        final java.util.List<String> documentEvents = new java.util.ArrayList<String>();
        Object documentRegistrar = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{documentRegistrarClass}, (proxy, method, args) -> {
            documentEvents.add((String) args[0]);
            return null;
        });
        wiringClass.getMethod("registerDocumentEvents", documentRegistrarClass, Object.class).invoke(null, documentRegistrar, new Object());
        assertEquals("paste", documentEvents.get(0));

        final java.util.List<String> peerEvents = new java.util.ArrayList<String>();
        Object elementRegistrar = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{elementRegistrarClass}, (proxy, method, args) -> {
            peerEvents.add(args[0] + ":" + args[2]);
            return null;
        });
        wiringClass.getMethod("registerPeerPointerEvents", elementRegistrarClass, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, String.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class)
                .invoke(null, elementRegistrar, true, true, true, true, true, "wheel", new Object(), new Object(), new Object(), new Object(), new Object(), new Object());
        assertTrue(peerEvents.contains("mousedown:true"));
        assertTrue(peerEvents.contains("pointerdown:true"));
        assertTrue(peerEvents.contains("hittest:true"));
        assertTrue(peerEvents.contains("wheel:true"));
    }

    @Test
    void extractedBrowserInteractionCoordinatorCompilesAndPreservesMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-browser-interaction-src");
        Path classesDir = Files.createTempDirectory("js-browser-interaction-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(BROWSER_INTERACTION_COORDINATOR_SOURCE, packageDir.resolve("JavaScriptBrowserInteractionCoordinator.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptBrowserInteractionCoordinator.java").toString()
        ));
        assertEquals(0, compileResult, "Browser interaction coordinator should compile as a standalone Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> coordinatorClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserInteractionCoordinator");
        Class<?> resizeHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserInteractionCoordinator$ResizeHooks");
        Class<?> hoverHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserInteractionCoordinator$HoverHooks");
        Class<?> cursorLocatorClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserInteractionCoordinator$CursorLocator");

        final java.util.List<String> resizeCalls = new java.util.ArrayList<String>();
        Object resizeHooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{resizeHooksClass}, (proxy, method, args) -> {
            resizeCalls.add(method.getName());
            return null;
        });
        coordinatorClass.getMethod("handleResize", resizeHooksClass).invoke(null, resizeHooks);
        assertEquals(java.util.Arrays.asList("waitForResizeStabilization", "updateCanvasSize", "sizeChanged", "revalidate"), resizeCalls);

        final java.util.List<String> hoverCalls = new java.util.ArrayList<String>();
        Object hoverHooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{hoverHooksClass}, (proxy, method, args) -> {
            hoverCalls.add(method.getName() + (args == null || args.length == 0 ? "" : ":" + args[0]));
            if ("callSerially".equals(method.getName())) {
                ((Runnable) args[0]).run();
            }
            return null;
        });
        Object cursorLocator = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{cursorLocatorClass}, (proxy, method, args) -> {
            if ("isCursorEnabled".equals(method.getName())) {
                return true;
            }
            if ("resolveCursorAt".equals(method.getName())) {
                return 7;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        coordinatorClass.getMethod("handleHover", hoverHooksClass, cursorLocatorClass, int.class, int.class, int.class)
                .invoke(null, hoverHooks, cursorLocator, 12, 34, 1);
        assertTrue(hoverCalls.contains("dispatchHover:12"));
        assertTrue(hoverCalls.contains("setCursor:7"));

        hoverCalls.clear();
        Object disabledCursorLocator = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{cursorLocatorClass}, (proxy, method, args) -> {
            if ("isCursorEnabled".equals(method.getName())) {
                return false;
            }
            if ("resolveCursorAt".equals(method.getName())) {
                return 99;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        coordinatorClass.getMethod("handleHover", hoverHooksClass, cursorLocatorClass, int.class, int.class, int.class)
                .invoke(null, hoverHooks, disabledCursorLocator, 12, 34, 1);
        assertTrue(hoverCalls.contains("setCursor:1"));
    }

    @Test
    void extractedKeyboardInteractionAdapterCompilesAndPreservesMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-keyboard-adapter-src");
        Path classesDir = Files.createTempDirectory("js-keyboard-adapter-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(KEYBOARD_INTERACTION_ADAPTER_SOURCE, packageDir.resolve("JavaScriptKeyboardInteractionAdapter.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptKeyboardInteractionAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "Keyboard interaction adapter should compile as a standalone Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> adapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptKeyboardInteractionAdapter");
        Class<?> eventViewClass = loader.loadClass("com.codename1.impl.html5.JavaScriptKeyboardInteractionAdapter$KeyEventView");
        Class<?> editingStateClass = loader.loadClass("com.codename1.impl.html5.JavaScriptKeyboardInteractionAdapter$EditingState");
        Class<?> backsideHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptKeyboardInteractionAdapter$BacksideHooks");
        Class<?> keyDispatchClass = loader.loadClass("com.codename1.impl.html5.JavaScriptKeyboardInteractionAdapter$KeyDispatch");

        Object tabEvent = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{eventViewClass}, (proxy, method, args) -> {
            if ("getKeyCode".equals(method.getName())) return 9;
            if ("getCharCode".equals(method.getName())) return 65;
            if ("isShiftKey".equals(method.getName())) return false;
            throw new UnsupportedOperationException(method.getName());
        });
        assertEquals(Boolean.TRUE, adapterClass.getMethod("shouldPreventDefaultOnKeyDown", boolean.class, eventViewClass).invoke(null, false, tabEvent));

        final java.util.List<String> calls = new java.util.ArrayList<String>();
        Object editingState = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{editingStateClass}, (proxy, method, args) -> false);
        Object backsideHooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{backsideHooksClass}, (proxy, method, args) -> {
            calls.add("installBacksideHooks");
            return null;
        });
        Object keyDispatch = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{keyDispatchClass}, (proxy, method, args) -> {
            String name = method.getName();
            String suffix = "";
            if (args != null && args.length > 0) {
                Object first = args[0];
                if (first instanceof Integer || first instanceof Boolean || first instanceof Long || first instanceof String) {
                    suffix = ":" + first;
                } else if (first instanceof Runnable) {
                    suffix = ":Runnable";
                } else {
                    suffix = ":arg";
                }
            }
            calls.add(name + suffix);
            if ("nativeCallSerially".equals(name) || "callSerially".equals(name)) {
                ((Runnable) args[0]).run();
            }
            if ("translateKeyCode".equals(name)) {
                return 123;
            }
            return null;
        });

        adapterClass.getMethod("handleKeyDown", editingStateClass, backsideHooksClass, keyDispatchClass, eventViewClass)
                .invoke(null, editingState, backsideHooks, keyDispatch, tabEvent);
        assertTrue(calls.contains("preventDefault"));
        assertTrue(calls.contains("setLastCharCode:65"));
        assertTrue(calls.contains("keyPressed:123"));

        calls.clear();
        Object shiftEvent = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{eventViewClass}, (proxy, method, args) -> {
            if ("getKeyCode".equals(method.getName())) return 16;
            if ("getCharCode".equals(method.getName())) return 0;
            if ("isShiftKey".equals(method.getName())) return true;
            throw new UnsupportedOperationException(method.getName());
        });
        adapterClass.getMethod("handleKeyUp", backsideHooksClass, keyDispatchClass, eventViewClass)
                .invoke(null, backsideHooks, keyDispatch, shiftEvent);
        assertTrue(calls.contains("setShiftKeyDown:false"));
        assertTrue(calls.contains("keyReleased:123"));

        calls.clear();
        adapterClass.getMethod("handleKeyPress", editingStateClass, backsideHooksClass, keyDispatchClass, eventViewClass)
                .invoke(null, editingState, backsideHooks, keyDispatch, tabEvent);
        assertTrue(calls.contains("editFocusedTextArea:arg"));
    }

    @Test
    void extractedBrowserLifecycleCoordinatorCompilesAndPreservesMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-browser-lifecycle-src");
        Path classesDir = Files.createTempDirectory("js-browser-lifecycle-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(BROWSER_LIFECYCLE_COORDINATOR_SOURCE, packageDir.resolve("JavaScriptBrowserLifecycleCoordinator.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptBrowserLifecycleCoordinator.java").toString()
        ));
        assertEquals(0, compileResult, "Browser lifecycle coordinator should compile as a standalone Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> coordinatorClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserLifecycleCoordinator");
        Class<?> inboxHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserLifecycleCoordinator$InboxHooks");
        Class<?> backNavHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserLifecycleCoordinator$BackNavigationHooks");
        Class<?> pasteHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserLifecycleCoordinator$PasteHooks");
        Class<?> backsideHooksClass = loader.loadClass("com.codename1.impl.html5.JavaScriptBrowserLifecycleCoordinator$BacksideHooks");

        final java.util.List<String> calls = new java.util.ArrayList<String>();
        Object inboxHooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{inboxHooksClass}, (proxy, method, args) -> {
            calls.add(method.getName() + (args == null || args.length == 0 ? "" : ":" + args[0]));
            if ("callSerially".equals(method.getName())) {
                ((Runnable) args[0]).run();
            }
            return null;
        });
        coordinatorClass.getMethod("handleInboxEvent", inboxHooksClass, String.class, int.class)
                .invoke(null, inboxHooks, "msg", 42);
        assertTrue(calls.contains("stopPropagation"));
        assertTrue(calls.contains("preventDefault"));
        assertTrue(calls.contains("dispatchMessage:msg"));

        calls.clear();
        Object backNavHooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{backNavHooksClass}, (proxy, method, args) -> {
            calls.add(method.getName());
            if ("callSerially".equals(method.getName())) {
                ((Runnable) args[0]).run();
            }
            return null;
        });
        coordinatorClass.getMethod("handlePopState", backNavHooksClass).invoke(null, backNavHooks);
        assertTrue(calls.contains("runBackCommand"));

        calls.clear();
        Object pasteHooks = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{pasteHooksClass}, (proxy, method, args) -> {
            calls.add(method.getName());
            return null;
        });
        Object handled = coordinatorClass.getMethod("handlePaste", pasteHooksClass, String.class, String.class, String[].class)
                .invoke(null, pasteHooks, "plain", "", null);
        assertEquals(Boolean.TRUE, handled);
        assertTrue(calls.contains("copyPlainText"));
        assertTrue(calls.contains("firePasteEvent"));

        calls.clear();
        Object backside = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{backsideHooksClass}, (proxy, method, args) -> {
            calls.add(method.getName());
            return null;
        });
        coordinatorClass.getMethod("handleInstallBacksideHooks", backsideHooksClass).invoke(null, backside);
        assertTrue(calls.contains("installBacksideHooksInUserInteraction"));
    }

    @Test
    void extractedCanvasLayoutAndRenderQueueHelpersCompileAndPreserveMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-render-src");
        Path classesDir = Files.createTempDirectory("js-render-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(CANVAS_LAYOUT_SOURCE, packageDir.resolve("JavaScriptCanvasLayout.java"));
        Files.copy(RENDER_QUEUE_STATE_SOURCE, packageDir.resolve("JavaScriptRenderQueueState.java"));
        Files.copy(RENDER_QUEUE_COORDINATOR_SOURCE, packageDir.resolve("JavaScriptRenderQueueCoordinator.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptCanvasLayout.java").toString(),
                packageDir.resolve("JavaScriptRenderQueueState.java").toString(),
                packageDir.resolve("JavaScriptRenderQueueCoordinator.java").toString()
        ));
        assertEquals(0, compileResult, "Canvas layout and render queue helpers should compile as standalone Java helpers");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> layoutClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasLayout");
        Class<?> dimensionsClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasLayout$Dimensions");
        Object hidpiDimensions = layoutClass.getMethod("compute", int.class, int.class, double.class).invoke(null, 320, 480, 2.0d);
        assertEquals(320, dimensionsClass.getMethod("getCssWidth").invoke(hidpiDimensions));
        assertEquals(480, dimensionsClass.getMethod("getCssHeight").invoke(hidpiDimensions));
        assertEquals(640, dimensionsClass.getMethod("getBackingWidth").invoke(hidpiDimensions));
        assertEquals(960, dimensionsClass.getMethod("getBackingHeight").invoke(hidpiDimensions));
        assertEquals("320px", dimensionsClass.getMethod("getStyleWidth").invoke(hidpiDimensions));
        assertEquals("480px", dimensionsClass.getMethod("getStyleHeight").invoke(hidpiDimensions));
        Object normalDimensions = layoutClass.getMethod("compute", int.class, int.class, double.class).invoke(null, 320, 480, 1.0d);
        assertEquals(320, dimensionsClass.getMethod("getBackingWidth").invoke(normalDimensions));
        assertEquals(480, dimensionsClass.getMethod("getBackingHeight").invoke(normalDimensions));
        assertEquals(null, dimensionsClass.getMethod("getStyleWidth").invoke(normalDimensions));

        Class<?> stateClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderQueueState");
        Class<?> snapshotClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderQueueState$FrameSnapshot");
        Class<?> coordinatorClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderQueueCoordinator");
        Class<?> flushBarrierClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderQueueCoordinator$FlushBarrier");
        Class<?> graphicsLockClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderQueueCoordinator$GraphicsLock");

        final boolean[] locked = new boolean[]{true};
        final int[] sleeps = new int[]{0};
        Object state = stateClass.getConstructor().newInstance();
        Object barrier = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{flushBarrierClass}, (proxy, method, args) -> {
            if ("isGraphicsLocked".equals(method.getName())) {
                boolean current = locked[0];
                if (locked[0]) {
                    locked[0] = false;
                }
                return current;
            }
            if ("sleep".equals(method.getName())) {
                sleeps[0] += (Integer) args[0];
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        coordinatorClass.getMethod("waitUntilFlushable", flushBarrierClass, stateClass).invoke(null, barrier, state);
        assertEquals(1, sleeps[0]);

        final java.util.List<Boolean> lockTransitions = new java.util.ArrayList<Boolean>();
        Object graphicsLock = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{graphicsLockClass}, (proxy, method, args) -> {
            lockTransitions.add((Boolean) args[0]);
            return null;
        });
        coordinatorClass.getMethod("queueFlush", graphicsLockClass, stateClass, java.util.List.class, int.class, int.class, int.class, int.class)
                .invoke(null, graphicsLock, state, java.util.Arrays.asList("draw"), 10, 20, 30, 40);
        assertEquals(java.util.Arrays.asList(Boolean.TRUE, Boolean.FALSE), lockTransitions);

        Object snapshot = coordinatorClass.getMethod("beginFrame", graphicsLockClass, stateClass).invoke(null, graphicsLock, state);
        assertEquals(false, snapshotClass.getMethod("isEmpty").invoke(snapshot));
        assertEquals(10, snapshotClass.getMethod("getCropX").invoke(snapshot));
        assertEquals(20, snapshotClass.getMethod("getCropY").invoke(snapshot));
        assertEquals(30, snapshotClass.getMethod("getCropW").invoke(snapshot));
        assertEquals(40, snapshotClass.getMethod("getCropH").invoke(snapshot));
        java.util.List<?> ops = (java.util.List<?>) snapshotClass.getMethod("getOps").invoke(snapshot);
        assertEquals("draw", ops.get(0));
        Object emptySnapshot = stateClass.getMethod("snapshotAndClear").invoke(state);
        assertEquals(true, snapshotClass.getMethod("isEmpty").invoke(emptySnapshot));
    }

    @Test
    void extractedRenderStateAndPrimitiveAdapterCompileAndPreserveMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-primitive-render-src");
        Path classesDir = Files.createTempDirectory("js-primitive-render-classes");
        Path html5PackageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Path graphicsPackageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5", "graphics"));
        Files.createDirectories(html5PackageDir);
        Files.createDirectories(graphicsPackageDir);
        Files.copy(RENDER_STATE_SOURCE, html5PackageDir.resolve("JavaScriptRenderState.java"));
        Files.copy(PRIMITIVE_RENDER_ADAPTER_SOURCE, html5PackageDir.resolve("JavaScriptPrimitiveRenderAdapter.java"));
        Files.copy(Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "graphics", "ClipState.java"),
                graphicsPackageDir.resolve("ClipState.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                graphicsPackageDir.resolve("ClipState.java").toString(),
                html5PackageDir.resolve("JavaScriptRenderState.java").toString(),
                html5PackageDir.resolve("JavaScriptPrimitiveRenderAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "Render state and primitive render adapter should compile as standalone Java helpers");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> renderStateClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderState");
        Class<?> primitiveAdapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptPrimitiveRenderAdapter");
        Class<?> sinkClass = loader.loadClass("com.codename1.impl.html5.JavaScriptPrimitiveRenderAdapter$OperationSink");
        Class<?> factoryClass = loader.loadClass("com.codename1.impl.html5.JavaScriptPrimitiveRenderAdapter$PrimitiveOpFactory");
        Class<?> clipStateClass = loader.loadClass("com.codename1.impl.html5.graphics.ClipState");

        Object state = renderStateClass.getConstructor().newInstance();
        renderStateClass.getMethod("setColor", int.class).invoke(state, 0x123456);
        renderStateClass.getMethod("setAlpha", int.class).invoke(state, 77);
        renderStateClass.getMethod("setFont", Object.class).invoke(state, "body-font");
        assertEquals(0x123456, renderStateClass.getMethod("getColor").invoke(state));
        assertEquals(77, renderStateClass.getMethod("getAlpha").invoke(state));
        assertEquals("body-font", renderStateClass.getMethod("getFont").invoke(state));
        assertTrue(clipStateClass.isInstance(renderStateClass.getMethod("getClipState").invoke(state)));

        final java.util.List<String> submitted = new java.util.ArrayList<String>();
        Object sink = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{sinkClass}, (proxy, method, args) -> {
            submitted.add((String) args[0]);
            return null;
        });
        Object factory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{factoryClass}, (proxy, method, args) -> {
            if ("createFillRect".equals(method.getName())) return "fill:" + args[4] + ":" + args[5];
            if ("createClearRect".equals(method.getName())) return "clear";
            if ("createDrawRect".equals(method.getName())) return "drawRect:" + args[4] + ":" + args[5];
            if ("createDrawLine".equals(method.getName())) return "drawLine:" + args[4] + ":" + args[5];
            if ("createDrawString".equals(method.getName())) return "drawString:" + args[3] + ":" + args[4] + ":" + args[5];
            if ("createClipRect".equals(method.getName())) return "clip:" + args[4].getClass().getSimpleName();
            throw new UnsupportedOperationException(method.getName());
        });

        Object adapter = primitiveAdapterClass.getConstructor(renderStateClass, sinkClass, factoryClass).newInstance(state, sink, factory);
        primitiveAdapterClass.getMethod("fillRect", int.class, int.class, int.class, int.class).invoke(adapter, 1, 2, 3, 4);
        primitiveAdapterClass.getMethod("clearRect", int.class, int.class, int.class, int.class).invoke(adapter, 1, 2, 3, 4);
        primitiveAdapterClass.getMethod("drawRect", int.class, int.class, int.class, int.class).invoke(adapter, 1, 2, 3, 4);
        primitiveAdapterClass.getMethod("drawLine", int.class, int.class, int.class, int.class).invoke(adapter, 1, 2, 3, 4);
        primitiveAdapterClass.getMethod("drawString", String.class, int.class, int.class).invoke(adapter, "hi", 5, 6);
        primitiveAdapterClass.getMethod("setClipRect", int.class, int.class, int.class, int.class).invoke(adapter, 7, 8, 9, 10);

        assertEquals("fill:1193046:77", submitted.get(0));
        assertEquals("clear", submitted.get(1));
        assertEquals("drawRect:1193046:77", submitted.get(2));
        assertEquals("drawLine:1193046:77", submitted.get(3));
        assertEquals("drawString:1193046:77:body-font", submitted.get(4));
        assertEquals("clip:ClipState", submitted.get(5));
    }

    @Test
    void extractedImageTransformRenderAdapterCompilesAndPreservesMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-image-transform-render-src");
        Path classesDir = Files.createTempDirectory("js-image-transform-render-classes");
        Path html5PackageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Path graphicsPackageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5", "graphics"));
        Files.createDirectories(html5PackageDir);
        Files.createDirectories(graphicsPackageDir);
        Files.copy(RENDER_STATE_SOURCE, html5PackageDir.resolve("JavaScriptRenderState.java"));
        Files.copy(IMAGE_TRANSFORM_RENDER_ADAPTER_SOURCE, html5PackageDir.resolve("JavaScriptImageTransformRenderAdapter.java"));
        Files.copy(Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "graphics", "ClipState.java"),
                graphicsPackageDir.resolve("ClipState.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                graphicsPackageDir.resolve("ClipState.java").toString(),
                html5PackageDir.resolve("JavaScriptRenderState.java").toString(),
                html5PackageDir.resolve("JavaScriptImageTransformRenderAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "Image/transform render adapter should compile as a standalone Java helper");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> renderStateClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderState");
        Class<?> adapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptImageTransformRenderAdapter");
        Class<?> sinkClass = loader.loadClass("com.codename1.impl.html5.JavaScriptImageTransformRenderAdapter$OperationSink");
        Class<?> factoryClass = loader.loadClass("com.codename1.impl.html5.JavaScriptImageTransformRenderAdapter$ImageTransformOpFactory");

        Object state = renderStateClass.getConstructor().newInstance();
        renderStateClass.getMethod("setAlpha", int.class).invoke(state, 88);

        final java.util.List<String> submitted = new java.util.ArrayList<String>();
        Object sink = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{sinkClass}, (proxy, method, args) -> {
            submitted.add((String) args[0]);
            return null;
        });
        Object factory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{factoryClass}, (proxy, method, args) -> {
            if ("createDrawImage".equals(method.getName()) && args.length == 4) return "drawImage:" + args[3];
            if ("createDrawImage".equals(method.getName()) && args.length == 6) return "drawImageScaled:" + args[5];
            if ("createTileImage".equals(method.getName())) return "tile:" + args[5];
            if ("createTransform".equals(method.getName())) return "transform:" + args[1];
            if ("createClipShape".equals(method.getName())) return "clipShape:" + args[2].getClass().getSimpleName();
            throw new UnsupportedOperationException(method.getName());
        });

        Object adapter = adapterClass.getConstructor(renderStateClass, sinkClass, factoryClass).newInstance(state, sink, factory);
        adapterClass.getMethod("drawImage", Object.class, int.class, int.class).invoke(adapter, "img", 1, 2);
        adapterClass.getMethod("drawImage", Object.class, int.class, int.class, int.class, int.class).invoke(adapter, "img", 1, 2, 3, 4);
        adapterClass.getMethod("tileImage", Object.class, int.class, int.class, int.class, int.class).invoke(adapter, "img", 1, 2, 3, 4);
        adapterClass.getMethod("applyTransform", Object.class, boolean.class).invoke(adapter, "tx", true);
        adapterClass.getMethod("setClipShape", Object.class, Object.class).invoke(adapter, "shape", "tx");

        assertEquals("drawImage:88", submitted.get(0));
        assertEquals("drawImageScaled:88", submitted.get(1));
        assertEquals("tile:88", submitted.get(2));
        assertEquals("transform:true", submitted.get(3));
        assertEquals("clipShape:ClipState", submitted.get(4));
    }

    @Test
    void extractedShapeGradientTextAndCanvasBufferHelpersCompileAndPreserveMinimalBehavior() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-render-deep-src");
        Path classesDir = Files.createTempDirectory("js-render-deep-classes");
        Path html5PackageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Path graphicsPackageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5", "graphics"));
        Files.createDirectories(html5PackageDir);
        Files.createDirectories(graphicsPackageDir);
        Files.copy(RENDER_STATE_SOURCE, html5PackageDir.resolve("JavaScriptRenderState.java"));
        Files.copy(SHAPE_GRADIENT_RENDER_ADAPTER_SOURCE, html5PackageDir.resolve("JavaScriptShapeGradientRenderAdapter.java"));
        Files.copy(TEXT_METRICS_ADAPTER_SOURCE, html5PackageDir.resolve("JavaScriptTextMetricsAdapter.java"));
        Files.copy(CANVAS_IMAGE_BUFFER_LIFECYCLE_SOURCE, html5PackageDir.resolve("JavaScriptCanvasImageBufferLifecycle.java"));
        Files.copy(Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java", "com", "codename1", "impl", "html5", "graphics", "ClipState.java"),
                graphicsPackageDir.resolve("ClipState.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                graphicsPackageDir.resolve("ClipState.java").toString(),
                html5PackageDir.resolve("JavaScriptRenderState.java").toString(),
                html5PackageDir.resolve("JavaScriptShapeGradientRenderAdapter.java").toString(),
                html5PackageDir.resolve("JavaScriptTextMetricsAdapter.java").toString(),
                html5PackageDir.resolve("JavaScriptCanvasImageBufferLifecycle.java").toString()
        ));
        assertEquals(0, compileResult, "Shape/gradient, text metrics, and canvas buffer helpers should compile as standalone Java helpers");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});

        Class<?> renderStateClass = loader.loadClass("com.codename1.impl.html5.JavaScriptRenderState");
        Class<?> shapeAdapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptShapeGradientRenderAdapter");
        Class<?> shapeSinkClass = loader.loadClass("com.codename1.impl.html5.JavaScriptShapeGradientRenderAdapter$OperationSink");
        Class<?> shapeFactoryClass = loader.loadClass("com.codename1.impl.html5.JavaScriptShapeGradientRenderAdapter$ShapeGradientOpFactory");
        Object renderState = renderStateClass.getConstructor().newInstance();
        renderStateClass.getMethod("setColor", int.class).invoke(renderState, 0xabcdef);
        renderStateClass.getMethod("setAlpha", int.class).invoke(renderState, 91);
        final java.util.List<String> shapeOps = new java.util.ArrayList<String>();
        Object shapeSink = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{shapeSinkClass}, (proxy, method, args) -> {
            shapeOps.add((String) args[0]);
            return null;
        });
        Object shapeFactory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{shapeFactoryClass}, (proxy, method, args) -> {
            if ("createDrawShape".equals(method.getName())) return "drawShape:" + args[2] + ":" + args[3];
            if ("createFillShape".equals(method.getName())) return "fillShape:" + args[1] + ":" + args[2];
            if ("createFillLinearGradient".equals(method.getName())) return "linear:" + args[7];
            if ("createFillRadialGradient".equals(method.getName())) return "radial:" + args[6] + ":" + args[7] + ":" + args[8];
            throw new UnsupportedOperationException(method.getName());
        });
        Object shapeAdapter = shapeAdapterClass.getConstructor(renderStateClass, shapeSinkClass, shapeFactoryClass).newInstance(renderState, shapeSink, shapeFactory);
        shapeAdapterClass.getMethod("drawShape", Object.class, Object.class).invoke(shapeAdapter, "shape", "stroke");
        shapeAdapterClass.getMethod("fillShape", Object.class).invoke(shapeAdapter, "shape");
        shapeAdapterClass.getMethod("fillLinearGradient", int.class, int.class, int.class, int.class, int.class, int.class, boolean.class)
                .invoke(shapeAdapter, 1, 2, 3, 4, 5, 6, true);
        shapeAdapterClass.getMethod("fillRadialGradient", int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class)
                .invoke(shapeAdapter, 1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals("drawShape:11259375:91", shapeOps.get(0));
        assertEquals("fillShape:11259375:91", shapeOps.get(1));
        assertEquals("linear:91", shapeOps.get(2));
        assertEquals("radial:91:7:8", shapeOps.get(3));

        Class<?> textAdapterClass = loader.loadClass("com.codename1.impl.html5.JavaScriptTextMetricsAdapter");
        Class<?> metricsContextClass = loader.loadClass("com.codename1.impl.html5.JavaScriptTextMetricsAdapter$FontMetricsContext");
        Class<?> cssSupplierClass = loader.loadClass("com.codename1.impl.html5.JavaScriptTextMetricsAdapter$FontCssSupplier");
        final String[] currentFont = new String[]{"old"};
        Object metricsContext = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{metricsContextClass}, (proxy, method, args) -> {
            if ("getCurrentFont".equals(method.getName())) return currentFont[0];
            if ("setCurrentFont".equals(method.getName())) {
                currentFont[0] = (String) args[0];
                return null;
            }
            if ("measureWidth".equals(method.getName())) return ((String) args[0]).length() * 10;
            throw new UnsupportedOperationException(method.getName());
        });
        Object cssSupplier = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{cssSupplierClass}, (proxy, method, args) -> {
            if ("getCss".equals(method.getName())) return "font-css";
            if ("getHeight".equals(method.getName())) return 18;
            if ("getAscent".equals(method.getName())) return 13;
            throw new UnsupportedOperationException(method.getName());
        });
        assertEquals(31, textAdapterClass.getMethod("charsWidth", metricsContextClass, cssSupplierClass, Object.class, char[].class, int.class, int.class)
                .invoke(null, metricsContext, cssSupplier, "font", new char[]{'a', 'b', 'c'}, 0, 3));
        currentFont[0] = "old";
        assertEquals(21, textAdapterClass.getMethod("stringWidth", metricsContextClass, cssSupplierClass, Object.class, String.class)
                .invoke(null, metricsContext, cssSupplier, "font", "ab"));
        assertEquals("old", currentFont[0]);
        assertEquals(18, textAdapterClass.getMethod("getFontHeight", cssSupplierClass, Object.class).invoke(null, cssSupplier, "font"));
        assertEquals(5, textAdapterClass.getMethod("getFontDescent", cssSupplierClass, Object.class).invoke(null, cssSupplier, "font"));

        Class<?> lifecycleClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasImageBufferLifecycle");
        Class<?> scratchFactoryClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasImageBufferLifecycle$ScratchCanvasFactory");
        Class<?> sizedCanvasFactoryClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasImageBufferLifecycle$SizedCanvasFactory");
        Class<?> sizeAccessClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasImageBufferLifecycle$CanvasSizeAccess");
        Class<?> graphicsFactoryClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasImageBufferLifecycle$GraphicsFactory");
        Class<?> bufferClass = loader.loadClass("com.codename1.impl.html5.JavaScriptCanvasImageBufferLifecycle$CanvasImageBuffer");

        final class CanvasState {
            int width;
            int height;
        }
        Object scratchFactory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{scratchFactoryClass}, (proxy, method, args) -> {
            CanvasState stateObj = new CanvasState();
            return stateObj;
        });
        Object sizeAccess = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{sizeAccessClass}, (proxy, method, args) -> {
            CanvasState stateObj = (CanvasState) args[0];
            if ("getWidth".equals(method.getName())) return stateObj.width;
            if ("getHeight".equals(method.getName())) return stateObj.height;
            if ("setWidth".equals(method.getName())) {
                stateObj.width = (Integer) args[1];
                return null;
            }
            if ("setHeight".equals(method.getName())) {
                stateObj.height = (Integer) args[1];
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        Object scratch = lifecycleClass.getMethod("ensureScratchBuffer", Object.class, int.class, int.class, scratchFactoryClass, sizeAccessClass)
                .invoke(null, null, 100, 50, scratchFactory, sizeAccess);
        assertEquals(100, ((CanvasState) scratch).width);
        assertEquals(50, ((CanvasState) scratch).height);
        Object resizedScratch = lifecycleClass.getMethod("ensureScratchBuffer", Object.class, int.class, int.class, scratchFactoryClass, sizeAccessClass)
                .invoke(null, scratch, 150, 120, scratchFactory, sizeAccess);
        assertEquals(150, ((CanvasState) resizedScratch).width);
        assertEquals(120, ((CanvasState) resizedScratch).height);

        Object sizedFactory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{sizedCanvasFactoryClass}, (proxy, method, args) -> {
            CanvasState stateObj = new CanvasState();
            stateObj.width = (Integer) args[0];
            stateObj.height = (Integer) args[1];
            return stateObj;
        });
        final java.util.List<String> graphicsCalls = new java.util.ArrayList<String>();
        Object graphicsFactory = java.lang.reflect.Proxy.newProxyInstance(loader, new Class[]{graphicsFactoryClass}, (proxy, method, args) -> {
            if ("createGraphics".equals(method.getName())) {
                CanvasState stateObj = (CanvasState) args[0];
                return "graphics:" + stateObj.width + "x" + stateObj.height;
            }
            if ("fillRect".equals(method.getName())) {
                graphicsCalls.add(args[1] + ":" + args[2] + "x" + args[3]);
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        Object blank = lifecycleClass.getMethod("createBlankBuffer", int.class, int.class, sizedCanvasFactoryClass, graphicsFactoryClass)
                .invoke(null, 40, 30, sizedFactory, graphicsFactory);
        assertEquals(40, bufferClass.getMethod("getWidth").invoke(blank));
        assertEquals(30, bufferClass.getMethod("getHeight").invoke(blank));
        assertEquals("graphics:40x30", bufferClass.getMethod("getGraphics").invoke(blank));
        Object mutable = lifecycleClass.getMethod("createMutableBuffer", int.class, int.class, int.class, sizedCanvasFactoryClass, graphicsFactoryClass)
                .invoke(null, 20, 10, 0xff336699, sizedFactory, graphicsFactory);
        assertEquals("graphics:20x10", bufferClass.getMethod("getGraphics").invoke(mutable));
        assertTrue(graphicsCalls.get(0).endsWith(":20x10"));
    }
}
