package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Localized unit tests for JavaScriptNativeImageAdapter - the pure-Java helper
/// that decides what NativeImage.getWidth() / getHeight() return, how drawImage
/// dispatches, and how tile patterns are cached.
///
/// Why this matters for the Switch huge-pill bug: Switch.calcPreferredSize()
/// adds the track image's getWidth()/getHeight() to Style padding to produce
/// the preferred size. If the adapter returns a scaled-up value (e.g. twice
/// the mutable canvas width because of DPR misreading), Switch preferredSize
/// inflates without any theme / layout mistake. These tests drive the adapter
/// with a Proxy ImageModel reporting controlled dimensions and verify the
/// resolution ladder matches the documented contract.
class JavaScriptNativeImageAdapterTest {
    private static final Path ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort",
            "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptNativeImageAdapter.java");

    @Test
    void resolveWidthPrefersExplicitWidthWhenSet() throws Exception {
        Class<?> adapter = loadAdapter();
        Method resolveWidth = adapter.getMethod("resolveWidth", loadImageModelIface(adapter));

        Object model = imageModel(adapter, 48, 24, false, 0, 0, true, 96, 48);
        assertEquals(48, resolveWidth.invoke(null, model),
                "Explicit width must win over mutable surface width");
    }

    @Test
    void resolveWidthFallsBackToMutableSurfaceWhenExplicitUnset() throws Exception {
        // Mirrors the live createMutableImage code path: NativeImage.width is
        // zero on creation, so getWidth() should fall through to the mutable
        // canvas's actual size. If this test ever fails, Switch preferred
        // width would be wrong for every mutable-image track / thumb.
        Class<?> adapter = loadAdapter();
        Method resolveWidth = adapter.getMethod("resolveWidth", loadImageModelIface(adapter));

        Object model = imageModel(adapter, 0, 0, false, 0, 0, true, 48, 24);
        assertEquals(48, resolveWidth.invoke(null, model),
                "Mutable surface width should surface when no explicit width is set");
    }

    @Test
    void resolveWidthFallsBackToLoadedImageWhenExplicitUnset() throws Exception {
        Class<?> adapter = loadAdapter();
        Method resolveWidth = adapter.getMethod("resolveWidth", loadImageModelIface(adapter));

        Object model = imageModel(adapter, 0, 0, true, 128, 64, false, 0, 0);
        assertEquals(128, resolveWidth.invoke(null, model));
    }

    @Test
    void resolveWidthFallsBackToSentinelWhenNothingKnown() throws Exception {
        Class<?> adapter = loadAdapter();
        Method resolveWidth = adapter.getMethod("resolveWidth", loadImageModelIface(adapter));

        Object model = imageModel(adapter, 0, 0, false, 0, 0, false, 0, 0);
        assertEquals(10, resolveWidth.invoke(null, model),
                "Sentinel 10 px fallback protects layout math from NaN/0");
    }

    @Test
    void resolveHeightMirrorsWidthLadder() throws Exception {
        Class<?> adapter = loadAdapter();
        Method resolveHeight = adapter.getMethod("resolveHeight", loadImageModelIface(adapter));

        assertEquals(30, resolveHeight.invoke(null, imageModel(adapter, 0, 30, false, 0, 60, true, 48, 96)));
        assertEquals(60, resolveHeight.invoke(null, imageModel(adapter, 0, 0, true, 128, 60, false, 0, 0)));
        assertEquals(96, resolveHeight.invoke(null, imageModel(adapter, 0, 0, false, 0, 0, true, 48, 96)));
        assertEquals(10, resolveHeight.invoke(null, imageModel(adapter, 0, 0, false, 0, 0, false, 0, 0)));
    }

    @Test
    void resolveSurfaceKindPrefersLoadedOverMutable() throws Exception {
        Class<?> adapter = loadAdapter();
        Method resolveKind = adapter.getMethod("resolveSurfaceKind", loadImageModelIface(adapter));
        Class<?> surfaceKind = loadSurfaceKindEnum(adapter);

        assertEquals(Enum.valueOf((Class) surfaceKind, "LOADED_IMAGE"),
                resolveKind.invoke(null, imageModel(adapter, 0, 0, true, 100, 100, true, 50, 50)));
        assertEquals(Enum.valueOf((Class) surfaceKind, "MUTABLE_SURFACE"),
                resolveKind.invoke(null, imageModel(adapter, 0, 0, false, 0, 0, true, 50, 50)));
        assertEquals(Enum.valueOf((Class) surfaceKind, "NONE"),
                resolveKind.invoke(null, imageModel(adapter, 0, 0, false, 0, 0, false, 0, 0)));
    }

    @Test
    void drawSkipsZeroOrNegativeDimensionsWithoutDispatching() throws Exception {
        Class<?> adapter = loadAdapter();
        Class<?> drawTargetIface = loadNested(adapter, "DrawTarget");
        Method draw = adapter.getMethod("draw", loadImageModelIface(adapter), drawTargetIface,
                int.class, int.class, int.class, int.class);

        Object model = imageModel(adapter, 0, 0, true, 100, 100, false, 0, 0);
        RecordingDrawTarget target = new RecordingDrawTarget();
        Object proxy = proxyDrawTarget(drawTargetIface, target);

        draw.invoke(null, model, proxy, 0, 0, 0, 100);
        draw.invoke(null, model, proxy, 0, 0, 100, 0);
        draw.invoke(null, model, proxy, 0, 0, -1, 50);
        draw.invoke(null, model, proxy, 0, 0, 50, -1);
        assertTrue(target.calls.isEmpty(), "draw() with non-positive w/h should no-op");

        // Positive dims do dispatch to the right branch
        draw.invoke(null, model, proxy, 10, 20, 30, 40);
        assertEquals(1, target.calls.size());
        assertEquals("drawLoadedImage(10,20,30,40)", target.calls.get(0));
    }

    @Test
    void drawDispatchesMutableBranchWhenOnlyMutableSurfacePresent() throws Exception {
        Class<?> adapter = loadAdapter();
        Class<?> drawTargetIface = loadNested(adapter, "DrawTarget");
        Method draw = adapter.getMethod("draw", loadImageModelIface(adapter), drawTargetIface,
                int.class, int.class, int.class, int.class);

        Object model = imageModel(adapter, 0, 0, false, 0, 0, true, 48, 24);
        RecordingDrawTarget target = new RecordingDrawTarget();
        Object proxy = proxyDrawTarget(drawTargetIface, target);

        draw.invoke(null, model, proxy, 10, 20, 30, 40);
        assertEquals(Collections.singletonList("drawMutableSurface(10,20,30,40)"), target.calls);
    }

    @Test
    void drawSkipsDispatchForNoneSurfaceKind() throws Exception {
        Class<?> adapter = loadAdapter();
        Class<?> drawTargetIface = loadNested(adapter, "DrawTarget");
        Method draw = adapter.getMethod("draw", loadImageModelIface(adapter), drawTargetIface,
                int.class, int.class, int.class, int.class);

        Object model = imageModel(adapter, 0, 0, false, 0, 0, false, 0, 0);
        RecordingDrawTarget target = new RecordingDrawTarget();
        Object proxy = proxyDrawTarget(drawTargetIface, target);

        draw.invoke(null, model, proxy, 10, 20, 30, 40);
        assertTrue(target.calls.isEmpty(), "NONE kind should silently drop the draw");
    }

    @Test
    void invalidatePatternCacheClearsCachedPattern() throws Exception {
        Class<?> adapter = loadAdapter();
        Method invalidate = adapter.getMethod("invalidatePatternCache", loadImageModelIface(adapter));

        MutableImageModelState state = new MutableImageModelState();
        state.patternCache = new Object();
        Object model = proxyImageModel(adapter, state);

        assertNotNull(state.patternCache);
        invalidate.invoke(null, model);
        assertNull(state.patternCache, "invalidatePatternCache must null the model's patternCache");
    }

    // ---- helpers ----

    private static Class<?> loadImageModelIface(Class<?> adapter) {
        return loadNested(adapter, "ImageModel");
    }

    private static Class<?> loadSurfaceKindEnum(Class<?> adapter) {
        return loadNested(adapter, "SurfaceKind");
    }

    private static Class<?> loadNested(Class<?> adapter, String simpleName) {
        for (Class<?> c : adapter.getDeclaredClasses()) {
            if (c.getSimpleName().equals(simpleName)) {
                return c;
            }
        }
        throw new IllegalStateException(simpleName + " not found");
    }

    private static Object imageModel(Class<?> adapter,
                                     int explicitWidth, int explicitHeight,
                                     boolean hasLoaded, int loadedWidth, int loadedHeight,
                                     boolean hasMutable, int mutableWidth, int mutableHeight) {
        MutableImageModelState state = new MutableImageModelState();
        state.explicitWidth = explicitWidth;
        state.explicitHeight = explicitHeight;
        state.hasLoadedImage = hasLoaded;
        state.loadedImageWidth = loadedWidth;
        state.loadedImageHeight = loadedHeight;
        state.hasMutableSurface = hasMutable;
        state.mutableSurfaceWidth = mutableWidth;
        state.mutableSurfaceHeight = mutableHeight;
        return proxyImageModel(adapter, state);
    }

    private static Object proxyImageModel(Class<?> adapter, MutableImageModelState state) {
        Class<?> iface = loadImageModelIface(adapter);
        return Proxy.newProxyInstance(
                adapter.getClassLoader(),
                new Class<?>[]{iface},
                (ignored, method, args) -> {
                    switch (method.getName()) {
                        case "getExplicitWidth": return state.explicitWidth;
                        case "getExplicitHeight": return state.explicitHeight;
                        case "hasLoadedImage": return state.hasLoadedImage;
                        case "getLoadedImageWidth": return state.loadedImageWidth;
                        case "getLoadedImageHeight": return state.loadedImageHeight;
                        case "hasMutableSurface": return state.hasMutableSurface;
                        case "getMutableSurfaceWidth": return state.mutableSurfaceWidth;
                        case "getMutableSurfaceHeight": return state.mutableSurfaceHeight;
                        case "getPatternCache": return state.patternCache;
                        case "setPatternCache":
                            state.patternCache = args[0];
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
        );
    }

    private static Object proxyDrawTarget(Class<?> iface, RecordingDrawTarget recorder) {
        return Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (ignored, method, args) -> {
                    StringBuilder sb = new StringBuilder(method.getName()).append('(');
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(args[i]);
                    }
                    sb.append(')');
                    recorder.calls.add(sb.toString());
                    return null;
                }
        );
    }

    private static Class<?> loadAdapter() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-native-image-src");
        Path classesDir = Files.createTempDirectory("js-native-image-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(ADAPTER_SOURCE, packageDir.resolve("JavaScriptNativeImageAdapter.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptNativeImageAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "JavaScriptNativeImageAdapter should compile standalone (pure Java)");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        return loader.loadClass("com.codename1.impl.html5.JavaScriptNativeImageAdapter");
    }

    private static final class MutableImageModelState {
        int explicitWidth;
        int explicitHeight;
        boolean hasLoadedImage;
        int loadedImageWidth;
        int loadedImageHeight;
        boolean hasMutableSurface;
        int mutableSurfaceWidth;
        int mutableSurfaceHeight;
        Object patternCache;
    }

    private static final class RecordingDrawTarget {
        final List<String> calls = new ArrayList<>();
    }
}
