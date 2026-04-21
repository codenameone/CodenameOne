package com.codename1.tools.translator;

import com.codename1.ui.geom.GeneralPath;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Localized unit tests for the JavaScriptShapePathAdapter - the port code
/// that walks a CN1 Shape's PathIterator and dispatches to the canvas'
/// moveTo / lineTo / bezierCurveTo / quadraticCurveTo / closePath sinks.
///
/// The Sheet / Picker bg-fill bug (missing rounded panel background) could
/// be caused either by the CN1 core's RoundRectBorder.paintBorderBackground
/// short-circuiting before the fillShape call, or by the port failing to
/// translate the rounded-rect path into canvas operations. This test
/// addresses the second hypothesis: feed a typical rounded-rect path the
/// way RoundRectBorder would, and verify the adapter produces the expected
/// canvas calls. If this passes, the Sheet bg bug is upstream (Style /
/// Border precondition checks), not in the canvas path translation.
class JavaScriptShapePathAdapterTest {
    private static final Path ADAPTER_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort",
            "src", "main", "java", "com", "codename1", "impl", "html5", "JavaScriptShapePathAdapter.java");

    @Test
    void addShapeToPathMirrorsMoveAndLineOpsForSimpleRectangle() throws Exception {
        GeneralPath rect = new GeneralPath();
        rect.moveTo(0, 0);
        rect.lineTo(100, 0);
        rect.lineTo(100, 50);
        rect.lineTo(0, 50);
        rect.closePath();

        RecordingSink sink = drivePath(rect);

        assertEquals(Arrays.asList(
                "moveTo(0.0,0.0)",
                "lineTo(100.0,0.0)",
                "lineTo(100.0,50.0)",
                "lineTo(0.0,50.0)",
                "closePath()"
        ), sink.log, "Simple rect should emit five canvas ops in order");
    }

    @Test
    void addShapeToPathEmitsBezierForQuadraticAndCubic() throws Exception {
        GeneralPath path = new GeneralPath();
        path.moveTo(10, 10);
        path.quadTo(20, 20, 30, 10);
        path.curveTo(40, 40, 50, 40, 60, 10);
        path.closePath();

        RecordingSink sink = drivePath(path);

        assertEquals("moveTo(10.0,10.0)", sink.log.get(0));
        assertEquals("quadraticCurveTo(20.0,20.0,30.0,10.0)", sink.log.get(1));
        assertEquals("bezierCurveTo(40.0,40.0,50.0,40.0,60.0,10.0)", sink.log.get(2));
        assertEquals("closePath()", sink.log.get(3));
    }

    @Test
    void addShapeToPathTranslatesTypicalSheetRoundedRectWithFourRoundedCorners() throws Exception {
        // Approximate the path RoundRectBorder.createShape() builds for a
        // Sheet panel: 400 px wide, 300 px tall, 20 px corner radius on the
        // top two corners (bottom corners square because sheet anchors the
        // screen bottom). Produces: moveTo, lineTo (top edge), bezier (TR
        // corner), lineTo (right edge), lineTo (bottom edge), lineTo (left
        // edge), bezier (TL corner), closePath.
        GeneralPath gp = buildApproxSheetPanelPath(400, 300, 20f);
        RecordingSink sink = drivePath(gp);

        int moveToCount = 0;
        int lineToCount = 0;
        int bezierCount = 0;
        int closeCount = 0;
        for (String op : sink.log) {
            if (op.startsWith("moveTo(")) moveToCount++;
            else if (op.startsWith("lineTo(")) lineToCount++;
            else if (op.startsWith("bezierCurveTo(") || op.startsWith("quadraticCurveTo(")) bezierCount++;
            else if (op.equals("closePath()")) closeCount++;
        }

        assertEquals(1, moveToCount, "Sheet panel path should have exactly one moveTo");
        assertEquals(1, closeCount, "Sheet panel path should close exactly once");
        assertTrue(bezierCount >= 2, "Top two corners should produce at least 2 bezier/quadratic ops");
        assertTrue(lineToCount >= 3, "The 3 straight edges (right, bottom, left) should appear as lineTos");
    }

    @Test
    void addShapeToPathHandlesEmptyPathWithoutEmitting() throws Exception {
        GeneralPath empty = new GeneralPath();
        RecordingSink sink = drivePath(empty);
        assertTrue(sink.log.isEmpty(),
                "Empty path should produce no sink calls (regression guard for null-Shape codepath)");
    }

    @Test
    void resolveJoinMapsAllJoinStylesIncludingMiterFallback() throws Exception {
        Class<?> adapterClass = loadAdapterClass();
        Method resolveJoin = adapterClass.getMethod("resolveJoin", int.class);

        // Stroke constants: JOIN_MITER=0, JOIN_ROUND=1, JOIN_BEVEL=2.
        // Match the actual CN1 values by importing the class.
        assertEquals("miter", resolveJoin.invoke(null, 0));
        assertEquals("round", resolveJoin.invoke(null, 1));
        assertEquals("bevel", resolveJoin.invoke(null, 2));
        assertEquals("miter", resolveJoin.invoke(null, 999), "Unknown join styles should fall back to miter");
    }

    @Test
    void resolveCapMapsAllCapStylesIncludingButtFallback() throws Exception {
        Class<?> adapterClass = loadAdapterClass();
        Method resolveCap = adapterClass.getMethod("resolveCap", int.class);

        // Stroke constants: CAP_BUTT=0, CAP_ROUND=1, CAP_SQUARE=2.
        assertEquals("butt", resolveCap.invoke(null, 0));
        assertEquals("round", resolveCap.invoke(null, 1));
        assertEquals("square", resolveCap.invoke(null, 2));
        assertEquals("butt", resolveCap.invoke(null, 999), "Unknown cap styles should fall back to butt");
    }

    // ---- helpers ----

    private static RecordingSink drivePath(GeneralPath gp) throws Exception {
        Class<?> adapterClass = loadAdapterClass();
        Class<?> pathSinkIface = null;
        for (Class<?> c : adapterClass.getDeclaredClasses()) {
            if (c.getSimpleName().equals("PathSink")) {
                pathSinkIface = c;
                break;
            }
        }
        assertTrue(pathSinkIface != null, "PathSink nested interface must exist on adapter");

        RecordingSink sink = new RecordingSink();
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                adapterClass.getClassLoader(),
                new Class<?>[]{pathSinkIface},
                (ignored, method, args) -> {
                    String name = method.getName();
                    StringBuilder sb = new StringBuilder(name).append('(');
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            if (i > 0) sb.append(',');
                            sb.append(args[i]);
                        }
                    }
                    sb.append(')');
                    sink.log.add(sb.toString());
                    return null;
                }
        );

        Method addShapeToPath = adapterClass.getMethod("addShapeToPath", pathSinkIface,
                com.codename1.ui.geom.Shape.class);
        addShapeToPath.invoke(null, proxy, gp);
        return sink;
    }

    private static GeneralPath buildApproxSheetPanelPath(int w, int h, float radius) {
        GeneralPath gp = new GeneralPath();
        // Start at (radius, 0)
        gp.moveTo(radius, 0);
        // Top edge
        gp.lineTo(w - radius, 0);
        // TR corner
        gp.curveTo(w, 0, w, 0, w, radius);
        // Right edge -> bottom right
        gp.lineTo(w, h);
        // Bottom edge (square corners, sheet anchors the bottom)
        gp.lineTo(0, h);
        // Left edge -> TL corner
        gp.lineTo(0, radius);
        gp.curveTo(0, 0, 0, 0, radius, 0);
        gp.closePath();
        return gp;
    }

    private static Class<?> loadAdapterClass() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-shape-path-src");
        Path classesDir = Files.createTempDirectory("js-shape-path-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(ADAPTER_SOURCE, packageDir.resolve("JavaScriptShapePathAdapter.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        // The adapter depends on CN1 core types (Shape, PathIterator, Stroke).
        // Pull codenameone-core from the test classpath.
        String classpath = System.getProperty("java.class.path");
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-classpath", classpath,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptShapePathAdapter.java").toString()
        ));
        assertEquals(0, compileResult, "JavaScriptShapePathAdapter should compile against codenameone-core test dep");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        return loader.loadClass("com.codename1.impl.html5.JavaScriptShapePathAdapter");
    }

    private static final class RecordingSink {
        final List<String> log = new ArrayList<>();
    }
}
