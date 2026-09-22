/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.example.bench;

import com.codename1.system.Lifecycle;

/**
 * Boots the transpiled Flutter Gallery. The transcode-flutter goal emits
 * com.codename1.generated.flutter.FlutterRegistry whose invokeMain() runs the
 * Dart app's main() through the Codename One Flutter runtime.
 */
public class Bench extends Lifecycle {

    /** See runApp: attribution costs real time, so it is not on by default. */
    private static final boolean TRACE_STARTUP = false;

    /// Skips the Codename One theme that Lifecycle.init would load.
    ///
    /// FlutterUI.runApp installs its own Material base theme, and
    /// UIManager.setThemeProps RESETS the property table before merging -- so the
    /// theme loaded here is discarded a moment later, along with the native theme
    /// its @includeNativeBool pulls in and every style built from both. A Flutter
    /// application draws its own visuals and never consults the Codename One
    /// theme, so loading one is pure start-up cost.
    ///
    /// Everything else Lifecycle.init does is kept.
    @Override
    public void init(Object context) {
        // The device this build stands in for. defaultTargetPlatform decides the back
        // chevron against the arrow, page transitions, switches and scrollbars, and the
        // JavaSE simulator reports "SE", which falls through to android -- while the skin
        // it wears and the reference frames it is measured against are both an iPhone.
        // Set before anything reads it: FoundationLib resolves it once, on first use.
        // One-arg getProperty and a null check, NOT the two-arg overload: ParparVM's
        // java.lang.System declares only the former, and the translator resolves a
        // missing method to an undeclared C function rather than to an error, so the
        // two-arg form builds here and fails to compile in the generated project.
        String platform = System.getProperty(
                com.codename1.flutter.foundation.FoundationLib.PLATFORM_PROPERTY);
        com.codename1.ui.Display.getInstance().setProperty(
                com.codename1.flutter.foundation.FoundationLib.PLATFORM_PROPERTY,
                platform == null ? "ios" : platform);
        com.codename1.ui.CN.updateNetworkThreadCount(2);
        com.codename1.ui.Toolbar.setGlobalToolbar(true);
        com.codename1.io.Log.bindCrashProtection(true);
    }

    @Override
    public void runApp() {
        // The runtime's startup attribution is OFF here, because a benchmark has
        // to measure the build a user would ship. The counters it turns on are
        // per-build and per-layout -- a nanoTime pair, a class-name string and a
        // map lookup on every box in the tree -- so leaving them on measured the
        // instrumentation as well as the app.
        //
        // The simulator switches them on with
        // -Dcn1.flutter.startupTrace=true (JavaSE resolves an unknown Codename
        // One property from the system properties); a native build has no such
        // channel, so flip TRACE_STARTUP and rebuild when attributing one.
        if (TRACE_STARTUP) {
            try {
                com.codename1.ui.Display.getInstance()
                        .setProperty("cn1.flutter.startupTrace", "true");
            } catch (Throwable ignored) {
            }
        }
        // Error capture stays on the startup path: it is cheap, and a failure
        // during the first frame is exactly the kind the sweep must not miss.
        // Baseline mode: the same binary, booted to an EMPTY form instead of the
        // gallery. Subtracting this from the full run separates what every
        // Codename One app pays -- dyld, the VM's constant pool, NSApplication,
        // the first NSWindow, the first present -- from what THIS app's UI
        // costs, and the counterpart minimal Flutter app allows the same split
        // on the other side. Without it a start-up figure cannot say whether a
        // gap is framework overhead or application work.
        //
        // The switch is a file rather than an environment variable because
        // ParparVM has no System.getenv: neither vm/JavaAPI nor Ports/CLDC11
        // declares it, so a build using it would fail to link on the device.
        if (baselineMode()) {
            markFirstFrame();
            new com.codename1.ui.Form().show();
            return;
        }
        long t0 = System.currentTimeMillis();
        com.codename1.flutter.FlutterErrorReport.install();
        long t1 = System.currentTimeMillis();
        markFirstFrame();
        com.codename1.generated.flutter.FlutterRegistry.invokeMain();
        long t2 = System.currentTimeMillis();
        // The driving CHANNEL, though, starts only once the app is on screen.
        // It is test scaffolding with no counterpart in the build this is
        // measured against, and first touching it costs 200ms+ of one-off class
        // initialisation for the MCP subsystem -- which would otherwise land
        // inside the cold-start figure and be reported as the app being slow.
        // Left ON by default so the sweep and visdiff tooling keep working. A
        // benchmark drops it: the channel is scaffolding the app being compared
        // against has no counterpart for, and the 200ms+ of one-off class
        // initialisation noted above lands in any window that samples CPU after
        // the first frame, where it would be read as the app being slow.
        if (!markerFile("CN1_NO_MCP")) {
            com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    startMcpForDriving();
                }
            });
        }
        // Phase timings, so a slow start can be attributed instead of guessed
        // at. The harness's own clock still owns the headline number; these say
        // which part of the app owns the milliseconds.
        System.out.println("BENCH:PHASE errorReport=" + (t1 - t0) + "ms invokeMain="
                + (t2 - t1) + "ms");
        System.out.flush();
    }

    /**
     * Prints one line the moment the app has painted its first frame.
     *
     * <p>The benchmark's cold-start figure is the wall time from launching the
     * process to this line appearing, measured by the harness on the outside so
     * that neither runtime has to be trusted for its own clock. The Flutter
     * build prints the identical marker from its own wrapper, so the two
     * numbers measure the same thing.
     *
     * <p>Deliberately hooked to a PAINT rather than to the end of runApp: the
     * question is when the user sees the app, not when its widget tree exists.
     */
    private void markFirstFrame() {
        final long start = System.currentTimeMillis();
        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
            private boolean done;
            private int passes;

            @Override
            public void run() {
                if (done) {
                    return;
                }
                passes++;
                com.codename1.ui.Form f = com.codename1.ui.Display.getInstance().getCurrent();
                if (f == null || f.getComponentCount() == 0) {
                    // Not painted yet -- look again on the next pass.
                    com.codename1.ui.Display.getInstance().callSerially(this);
                    return;
                }
                done = true;
                // The pass count separates WORK from WAITING: several passes
                // means the event thread went round the loop with nothing to
                // show, which is a scheduling problem, not a slow app.
                // A frame that threw is not a frame. Without this the marker
                // certified a startup in which the widget tree had failed
                // half-way through layout -- and a broken screen paints
                // FASTER than a complete one, so the crash read as a good
                // benchmark result rather than as a bug.
                String failures = com.codename1.flutter.FlutterErrorReport.summary();
                if (failures != null && !failures.startsWith("0 ")) {
                    System.out.println("BENCH:FIRSTFRAME-INVALID after="
                            + (System.currentTimeMillis() - start) + "ms edtPasses=" + passes
                            + " -- the first frame reported errors:");
                    System.out.println(failures);
                    System.out.flush();
                    return;
                }
                System.out.println("BENCH:FIRSTFRAME after="
                        + (System.currentTimeMillis() - start) + "ms edtPasses=" + passes);
                System.out.flush();
            }
        });
    }

    /**
     * Opens Codename One's MCP channel so this build can be driven (snapshot /
     * activate) the same way the desktop simulator is. Bound to loopback, so on the
     * iOS simulator -- which shares the host's loopback -- the same client reaches it.
     * Harness only: a shipping app should not open a port it does not need.
     */


    /// Isolates the cost of creating ONE Codename One component, step by step.
    ///
    /// Mount is 94% createComponent(), and createComponent() is 78us per
    /// component on a real screen. That is far too slow for `new Container()`,
    /// so this splits the construction into the parts that could account for
    /// it: the bare constructor, assigning a UIID, resolving the unselected
    /// style, and materialising all five styles. Each runs in its own loop so
    /// the answer is a per-call cost, not a share of a screen.
    private static void addCreateBench() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_create",
                "Times CN1 component construction step by step over N iterations.",
                "{\"type\":\"object\",\"properties\":{\"n\":{\"type\":\"number\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final int n = Math.max(100, num(a, "n", 2000));
                        final String[] out = new String[1];
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                            @Override
                            public void run() {
                                out[0] = createBench(n);
                            }
                        });
                        return out[0];
                    }
                }));
    }

    private static String createBench(int n) {
        // warm the paths so the first iteration's one-off work is not the answer
        for (int i = 0; i < 200; i++) {
            com.codename1.ui.Container w = new com.codename1.ui.Container();
            w.setUIID("FlutterBox");
            w.getUnselectedStyle();
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            new com.codename1.ui.Container();
        }
        long t1 = System.nanoTime();
        com.codename1.ui.Container[] keep = new com.codename1.ui.Container[n];
        for (int i = 0; i < n; i++) {
            keep[i] = new com.codename1.ui.Container();
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            keep[i].setUIID("FlutterBox");
        }
        long t3 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            keep[i].getUnselectedStyle();
        }
        long t4 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            keep[i].getAllStyles();
        }
        long t5 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            new com.codename1.ui.Label("x");
        }
        long t6 = System.nanoTime();
        // The EXACT sequence the cheapest render elements run, so the 3.9us
        // measured for a Divider node can be accounted for rather than guessed
        // at. getAllStyles() builds a proxy over five Style objects and these
        // call sites ask for it twice.
        for (int i = 0; i < n; i++) {
            com.codename1.ui.Label strip = new com.codename1.ui.Label("", "FlutterDivider");
            strip.getAllStyles().setPadding(0, 0, 0, 0);
            strip.getAllStyles().setMargin(0, 0, 0, 0);
        }
        long t10 = System.nanoTime();
        // Same thing with the proxy fetched once.
        for (int i = 0; i < n; i++) {
            com.codename1.ui.Label strip = new com.codename1.ui.Label("", "FlutterDivider");
            com.codename1.ui.plaf.Style all = strip.getAllStyles();
            all.setPadding(0, 0, 0, 0);
            all.setMargin(0, 0, 0, 0);
        }
        long t11 = System.nanoTime();
        // And with neither call at all -- what the node would cost if the uiid's
        // theme entry already carried zero padding and margin.
        for (int i = 0; i < n; i++) {
            new com.codename1.ui.Label("", "FlutterDivider");
        }
        long t12 = System.nanoTime();
        // Font.stringWidth is a NATIVE text measurement on this port, and
        // TextRenderElement.performLayout calls it once per candidate while
        // wrapping, again while clamping, and again for every final line. If it
        // is expensive, text is being measured several times over.
        com.codename1.ui.Font bf = com.codename1.ui.Font.createSystemFont(
                com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_PLAIN,
                com.codename1.ui.Font.SIZE_MEDIUM);
        String sample = "The quick brown fox";
        int w = 0;
        for (int i = 0; i < n; i++) {
            w += bf.stringWidth(sample);
        }
        long t13 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            w += bf.charWidth('x');
        }
        long t14 = System.nanoTime();
        // Does the derived-font cache actually hit? Same size repeatedly is the
        // case a screen of icons presents; varying sizes is the case it cannot
        // help. If both cost the same, the native font peer was never the cost.
        com.codename1.ui.plaf.Style istyle = new com.codename1.ui.plaf.Style();
        int sameSizeIters = Math.min(n, 400);
        for (int i = 0; i < sameSizeIters; i++) {
            com.codename1.ui.FontImage.createMaterial((char) (0xE000 + (i % 200)), istyle, 4.0f);
        }
        long t15 = System.nanoTime();
        for (int i = 0; i < sameSizeIters; i++) {
            com.codename1.ui.FontImage.createMaterial((char) (0xE000 + (i % 200)), istyle,
                    3.0f + (i % 17) * 0.5f);
        }
        long t16 = System.nanoTime();
        if (w == -1) {
            System.out.println("");
        }
        // The loops above all reuse ONE uiid, so they measure the CACHED style
        // path -- 21ns for setUIID. A real screen uses ~30 distinct uiids, and
        // the first component of each pays for the theme style being built.
        // This forces that path: a uiid nobody has asked for before.
        int distinct = Math.min(n, 400);
        com.codename1.ui.Container[] fresh = new com.codename1.ui.Container[distinct];
        for (int i = 0; i < distinct; i++) {
            fresh[i] = new com.codename1.ui.Container();
        }
        long t7 = System.nanoTime();
        for (int i = 0; i < distinct; i++) {
            fresh[i].setUIID("BenchUnseen" + i);
            fresh[i].getUnselectedStyle();
        }
        long t8 = System.nanoTime();
        // And the same uiids a second time, now warm, to separate the one-off
        // theme work from anything that repeats.
        for (int i = 0; i < distinct; i++) {
            com.codename1.ui.Container c2 = new com.codename1.ui.Container();
            c2.setUIID("BenchUnseen" + i);
            c2.getUnselectedStyle();
        }
        long t9 = System.nanoTime();
        double d = n;
        return "{\"n\":" + n
                + ",\"newContainerNs\":" + Math.round((t1 - t0) / d)
                + ",\"newContainerKeptNs\":" + Math.round((t2 - t1) / d)
                + ",\"setUIIDNs\":" + Math.round((t3 - t2) / d)
                + ",\"getUnselectedStyleNs\":" + Math.round((t4 - t3) / d)
                + ",\"getAllStylesNs\":" + Math.round((t5 - t4) / d)
                + ",\"newLabelNs\":" + Math.round((t6 - t5) / d)
                + ",\"firstUseOfUiidNs\":" + Math.round((t8 - t7) / (double) distinct)
                + ",\"secondUseOfUiidNs\":" + Math.round((t9 - t8) / (double) distinct)
                + ",\"distinctUiids\":" + distinct
                + ",\"dividerPatternNs\":" + Math.round((t10 - t6) / d)
                + ",\"proxyOnceNs\":" + Math.round((t11 - t10) / d)
                + ",\"noStyleCallsNs\":" + Math.round((t12 - t11) / d)
                + ",\"stringWidthNs\":" + Math.round((t13 - t12) / d)
                + ",\"charWidthNs\":" + Math.round((t14 - t13) / d)
                + ",\"iconSameSizeNs\":" + Math.round((t15 - t14) / (double) sameSizeIters)
                + ",\"iconVaryingSizeNs\":" + Math.round((t16 - t15) / (double) sameSizeIters)
                + "}";
    }


    /// Hammers FIRST-USE style resolution so a sampler can see inside it.
    ///
    /// A uiid's first use costs 112us and every later use 800ns, so the cost is
    /// once-per-uiid theme work, not per-component work. 400 uiids is only 45ms
    /// -- too short to profile. This runs enough distinct uiids to hold the
    /// process in that code for seconds, which is what `sample` needs to
    /// attribute it to a function and a line.
    private static void addStyleStormBench() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_stylestorm",
                "Resolves styles for N never-before-seen uiids, for profiling.",
                "{\"type\":\"object\",\"properties\":{\"n\":{\"type\":\"number\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final int n = Math.max(1000, num(a, "n", 40000));
                        final String[] out = new String[1];
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                            @Override
                            public void run() {
                                long t0 = System.nanoTime();
                                for (int i = 0; i < n; i++) {
                                    com.codename1.ui.Container c = new com.codename1.ui.Container();
                                    c.setUIID("Storm" + i);
                                    c.getUnselectedStyle();
                                }
                                long t1 = System.nanoTime();
                                out[0] = "{\"n\":" + n + ",\"nsPerUiid\":"
                                        + Math.round((t1 - t0) / (double) n) + "}";
                            }
                        });
                        return out[0];
                    }
                }));
    }

    private void startMcpForDriving() {
        try {
            addAuditTools();
            // DRIVING BUILD ONLY. MCP refuses to open a port on a release build,
            // and driving the built artefact is the only way to measure anything
            // that needs a gesture -- the repaint ratio during a fling, frame
            // times under scroll. It costs a listening socket, a reader thread
            // and ~200ms of one-off class initialisation, all of it AFTER the
            // first frame, so cold-start-to-first-frame is unaffected but
            // memory-at-rest is NOT. Do not take memory numbers from a build
            // with this in.
            com.codename1.mcp.MCP.setAllowOnReleaseBuilds(true);
            com.codename1.mcp.MCP.startSocketServer(8766);
            com.codename1.io.Log.p("bench: MCP listening on loopback:8766");
        } catch (Throwable t) {
            com.codename1.io.Log.p("bench: could not start MCP: " + t);
        }
    }

    /**
     * Audit tools: jump straight to a named route and come back, so every demo in the
     * gallery can be visited without walking the UI for each one. Harness only.
     */
    private void addAuditTools() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_open_route",
                "Pushes a named route (e.g. /demo/app-bar) and reports whether it resolved.",
                "{\"type\":\"object\",\"properties\":{\"route\":{\"type\":\"string\"}},"
                        + "\"required\":[\"route\"]}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        final String route = com.codename1.io.JSONParser.getString(
                                parse(argumentsJson), "route");
                        // Framework mutations must happen on the EDT, and this tool is
                        // called from the MCP reader thread. Fire and forget rather than
                        // callSeriallyAndWait: mounting a route can itself block the EDT,
                        // and waiting on it from here deadlocks. The caller snapshots
                        // afterwards to see what appeared.
                        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.flutter.navigation.Navigator
                                        .pushNamed(null, route, null);
                            }
                        });
                        return "{\"route\":\"" + route + "\",\"requested\":true}";
                    }
                }));
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_errors",
                "Returns the collected Flutter error inventory (distinct failures with the "
                        + "widget and route that produced them), optionally resetting it.",
                "{\"type\":\"object\",\"properties\":{\"reset\":{\"type\":\"boolean\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        String s = com.codename1.flutter.FlutterErrorReport.summary();
                        if (String.valueOf(com.codename1.io.JSONParser.getString(
                                parse(argumentsJson), "reset")).equals("true")) {
                            com.codename1.flutter.FlutterErrorReport.reset();
                        }
                        return s;
                    }
                }));
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_pop",
                "Pops the current route, returning to the previous screen.",
                "{\"type\":\"object\",\"properties\":{}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.flutter.navigation.Navigator.pop(null);
                            }
                        });
                        return "{\"popped\":true}";
                    }
                }));
        addPointerTool();
        addPaintTool();
        addFpsTool();
        addShotTool();
        addComponentsTool();
        // What a requested timer delay actually costs to deliver. This once measured a
        // flat ~290ms whatever delay was asked for, which looked like setTimeout overhead
        // and was really the floor of one EDT pass (a full-Form revalidate treadmill): a
        // timer cannot arrive sooner than the pass that delivers it. Kept because a delay
        // that ignores its argument is a good smoke test for the EDT being blocked.
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_timer",
                "Chains N CN.setTimeout(ms) calls and reports the intervals actually "
                        + "delivered - the real animation clock rate.",
                "{\"type\":\"object\",\"properties\":{\"n\":{\"type\":\"number\"},"
                        + "\"ms\":{\"type\":\"number\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final int n = Math.max(2, num(a, "n", 30));
                        final int ms = Math.max(1, num(a, "ms", 16));
                        final java.util.List<Long> gaps =
                                java.util.Collections.synchronizedList(
                                        new java.util.ArrayList<Long>());
                        final long[] last = {0};
                        final int[] left = {n};
                        final Object done = new Object();
                        final Runnable[] step = new Runnable[1];
                        step[0] = new Runnable() {
                            @Override
                            public void run() {
                                long now = System.nanoTime();
                                if (last[0] != 0) {
                                    gaps.add((now - last[0]) / 1000);
                                }
                                last[0] = now;
                                if (--left[0] > 0) {
                                    com.codename1.ui.CN.setTimeout(ms, step[0]);
                                } else {
                                    synchronized (done) {
                                        done.notifyAll();
                                    }
                                }
                            }
                        };
                        com.codename1.ui.CN.callSerially(new Runnable() {
                            @Override
                            public void run() {
                                last[0] = System.nanoTime();
                                com.codename1.ui.CN.setTimeout(ms, step[0]);
                            }
                        });
                        synchronized (done) {
                            try {
                                done.wait(20000);
                            } catch (InterruptedException err) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        java.util.List<Long> g = new java.util.ArrayList<Long>(gaps);
                        if (g.isEmpty()) {
                            return "{\"ticks\":0}";
                        }
                        java.util.Collections.sort(g);
                        long sum = 0;
                        for (Long v : g) {
                            sum += v;
                        }
                        return "{\"requestedMs\":" + ms + ",\"ticks\":" + g.size()
                                + ",\"meanMs\":" + round2(sum / 1000.0 / g.size())
                                + ",\"minMs\":" + round2(g.get(0) / 1000.0)
                                + ",\"maxMs\":" + round2(g.get(g.size() - 1) / 1000.0) + "}";
                    }
                }));
        // Codename One's own property store, not System.setProperty: this runs on the
        // device too, where there is no way to pass -D at launch, and ParparVM has no
        // System.setProperty at all.
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_prop",
                "Sets a Codename One display property at runtime, e.g. "
                        + "{\"name\":\"cn1.edt.trace\",\"value\":\"true\"} to turn on the "
                        + "per-phase EDT trace on a device.",
                "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},"
                        + "\"value\":{\"type\":\"string\"}},\"required\":[\"name\"]}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        String name = com.codename1.io.JSONParser.getString(a, "name");
                        String value = com.codename1.io.JSONParser.getString(a, "value");
                        com.codename1.ui.Display.getInstance().setProperty(name, value);
                        return "{\"" + name + "\":\"" + value + "\"}";
                    }
                }));
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_frames",
                "Turns Flutter frame tracing on/off and returns what the traced build "
                        + "flushes cost. Pass {\"on\":true} to start, {} to read.",
                "{\"type\":\"object\",\"properties\":{\"on\":{\"type\":\"boolean\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        Object on = parse(argumentsJson).get("on");
                        if (on != null) {
                            com.codename1.flutter.BuildOwner.traceFrames(
                                    String.valueOf(on).equals("true"));
                        }
                        return com.codename1.flutter.BuildOwner.frameStats();
                    }
                }));
    }

    /**
     * Injects a real pointer gesture at screen coordinates - press, optional drags, release -
     * through the Form, which is the same hit-testing and dispatch a finger goes through.
     *
     * <p>This exists because ui_activate does NOT: it invokes a component's action directly,
     * so it reports a screen as working even when nothing there can actually be touched.
     * Anything about responsiveness or dragging has to be measured through this tool.</p>
     */
    /**
     * Times full-form paints into an offscreen image.
     *
     * <p>This exists because bench_frames measures only build and layout, and bench_pointer
     * runs its whole gesture inside ONE EDT slot - so nothing repaints between drag events
     * and neither tool can see the cost of a frame. Scroll smoothness is paint-bound once
     * layout is cached, so this is the number that decides whether a drag is fluid.</p>
     */
    /**
     * Records REAL inter-frame intervals while the pane is moving.
     *
     * <p>bench_paint times a synthetic paint into an offscreen image, which says what one
     * frame costs but nothing about how often frames actually arrive. Fluidity is the
     * interval distribution, so this drags, lets CN1's momentum run for real, and reports
     * what the EDT actually delivered.</p>
     */
    private void addFpsTool() {
        addCreateBench();
        addStyleStormBench();
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_fps",
                "Injects a flick and records real frame intervals during the momentum "
                        + "scroll. Returns count/mean/p95/max ms and the implied fps.",
                "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"number\"},"
                        + "\"y\":{\"type\":\"number\"},\"x2\":{\"type\":\"number\"},"
                        + "\"y2\":{\"type\":\"number\"},\"ms\":{\"type\":\"number\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final int x = num(a, "x", 500);
                        final int y = num(a, "y", 420);
                        final int x2 = num(a, "x2", 100);
                        final int y2 = num(a, "y2", 420);
                        final int windowMs = Math.max(300, num(a, "ms", 1500));
                        final java.util.List<Long> gaps =
                                java.util.Collections.synchronizedList(
                                        new java.util.ArrayList<Long>());
                        final long[] last = {0};
                        final com.codename1.ui.animations.Animation rec =
                                new com.codename1.ui.animations.Animation() {
                            @Override
                            public boolean animate() {
                                long now = System.nanoTime();
                                if (last[0] != 0) {
                                    gaps.add((now - last[0]) / 1000);
                                }
                                last[0] = now;
                                // NOTE: returning true here forces a repaint every cycle and
                                // WEDGES the simulator's EDT - do not do it. Passive observation
                                // cannot distinguish a slow frame from an idle EDT, so this
                                // tool's numbers are only meaningful while something is actually
                                // animating. That limitation is still unresolved.
                                return false;
                            }

                            @Override
                            public void paint(com.codename1.ui.Graphics g) {
                            }
                        };
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(
                                new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.ui.Form f =
                                        com.codename1.ui.Display.getInstance().getCurrent();
                                if (f == null) {
                                    return;
                                }
                                f.registerAnimated(rec);
                                f.pointerPressed(x, y);
                            }
                        });
                        // The drag events MUST be spread over real time. Dispatching them in
                        // one EDT slot gives CN1 no time delta to derive a velocity from, so
                        // no momentum is started and the EDT simply idles - which reads as a
                        // catastrophic frame rate that is really just an inert app.
                        final int steps = 10;
                        for (int i = 1; i <= steps; i++) {
                            final int px = x + (x2 - x) * i / steps;
                            final int py = y + (y2 - y) * i / steps;
                            com.codename1.ui.Display.getInstance().callSeriallyAndWait(
                                    new Runnable() {
                                @Override
                                public void run() {
                                    com.codename1.ui.Form f =
                                            com.codename1.ui.Display.getInstance().getCurrent();
                                    if (f != null) {
                                        f.pointerDragged(px, py);
                                    }
                                }
                            });
                            try {
                                Thread.sleep(16);
                            } catch (InterruptedException err) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(
                                new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.ui.Form f =
                                        com.codename1.ui.Display.getInstance().getCurrent();
                                if (f != null) {
                                    f.pointerReleased(x2, y2);
                                }
                            }
                        });
                        try {
                            Thread.sleep(windowMs);
                        } catch (InterruptedException err) {
                            Thread.currentThread().interrupt();
                        }
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(
                                new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.ui.Form f =
                                        com.codename1.ui.Display.getInstance().getCurrent();
                                if (f != null) {
                                    f.deregisterAnimated(rec);
                                }
                            }
                        });
                        java.util.List<Long> g = new java.util.ArrayList<Long>(gaps);
                        java.util.Collections.sort(g);
                        if (g.isEmpty()) {
                            return "{\"frames\":0}";
                        }
                        long sum = 0;
                        for (Long v : g) {
                            sum += v;
                        }
                        double mean = sum / 1000.0 / g.size();
                        double p95 = g.get(Math.min(g.size() - 1, (int) (g.size() * 0.95)))
                                / 1000.0;
                        double max = g.get(g.size() - 1) / 1000.0;
                        return "{\"frames\":" + g.size() + ",\"meanMs\":" + round2(mean)
                                + ",\"p95Ms\":" + round2(p95) + ",\"maxMs\":" + round2(max)
                                + ",\"fps\":" + round2(mean > 0 ? 1000.0 / mean : 0) + "}";
                    }
                }));
    }

    private void addPaintTool() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_paint",
                "Times N full-form paints into an offscreen image and returns ms per frame.",
                "{\"type\":\"object\",\"properties\":{\"n\":{\"type\":\"number\"}}}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        final int n = Math.max(1, num(parse(argumentsJson), "n", 30));
                        final double[] out = new double[2];
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.ui.Form f =
                                        com.codename1.ui.Display.getInstance().getCurrent();
                                if (f == null) {
                                    return;
                                }
                                com.codename1.ui.Image img = com.codename1.ui.Image.createImage(
                                        f.getWidth(), f.getHeight());
                                com.codename1.ui.Graphics g = img.getGraphics();
                                f.paintComponent(g, true);   // warm the caches
                                long worst = 0;
                                long t0 = System.nanoTime();
                                for (int i = 0; i < n; i++) {
                                    long s = System.nanoTime();
                                    f.paintComponent(g, true);
                                    worst = Math.max(worst, System.nanoTime() - s);
                                }
                                out[0] = (System.nanoTime() - t0) / 1000000.0 / n;
                                out[1] = worst / 1000000.0;
                            }
                        });
                        return "{\"frames\":" + n + ",\"msPerFrame\":" + round2(out[0])
                                + ",\"worstMs\":" + round2(out[1]) + "}";
                    }
                }));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Writes the current form to a PNG on the host filesystem so a visual defect can be
     * LOOKED at rather than reasoned about.
     *
     * <p>Optionally crops to a rectangle: a full 1125x2436 screenshot is mostly irrelevant
     * to any one artifact, and the crop is what makes a few-pixel fringe visible at all.</p>
     */
    private void addShotTool() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_motion",
                "Captures a transition frame by frame at named ANIMATION times. Freezes the "
                        + "Flutter clock, performs the action (`route` to push one, or x/y to "
                        + "tap), then for each entry of `samples` (milliseconds) advances the "
                        + "clock to it, runs one animation frame and writes <dir>/<ms>.png. "
                        + "The clock is released before returning.",
                "{\"type\":\"object\",\"properties\":{\"dir\":{\"type\":\"string\"},"
                        + "\"route\":{\"type\":\"string\"},\"x\":{\"type\":\"number\"},"
                        + "\"y\":{\"type\":\"number\"},\"hold\":{\"type\":\"boolean\"},"
                        + "\"samples\":{\"type\":\"string\"}},"
                        + "\"required\":[\"dir\",\"samples\"]}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final String dir = com.codename1.io.JSONParser.getString(a, "dir");
                        final String route = com.codename1.io.JSONParser.getString(a, "route");
                        final int tapX = num(a, "x", -1);
                        final int tapY = num(a, "y", -1);
                        final boolean hold = "true".equals(String.valueOf(a.get("hold")));
                        int[] samples = parseSamples(
                                com.codename1.io.JSONParser.getString(a, "samples"));
                        if (samples.length == 0) {
                            return "{\"error\":\"samples is empty\"}";
                        }
                        StringBuilder out = new StringBuilder("{\"frames\":[");
                        // Freeze and act on one EDT turn, then sample on later ones. The
                        // rebuild a tap or a push triggers is queued with callSerially, so
                        // holding the EDT across the whole capture would photograph a tree
                        // that never got the chance to rebuild.
                        // Freeze first, then fire the action WITHOUT waiting for it.
                        // Codename One runs a form transition by spinning the EDT until
                        // the animation queue drains (Display.flushEdt), so a push issued
                        // from inside a waited call never returns while the clock is held
                        // -- the transition it is waiting on cannot finish. Fired and
                        // forgotten, the EDT parks on the first frame of the transition
                        // and still services the sampling calls below.
                        onEdt(new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.flutter.animation.MotionClock.freeze();
                            }
                        });
                        if (route != null && route.length() > 0) {
                            com.codename1.ui.Display.getInstance().callSerially(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            com.codename1.flutter.navigation.Navigator
                                                    .pushNamed(null, route, null);
                                        }
                                    });
                        }
                        onEdt(new Runnable() {
                            @Override
                            public void run() {
                                if (tapX >= 0 && tapY >= 0) {
                                    com.codename1.ui.Form f = com.codename1.ui.Display
                                            .getInstance().getCurrent();
                                    if (f != null) {
                                        f.pointerPressed(tapX, tapY);
                                        // Held, for a press effect: the ink only exists
                                        // while the finger is down, so releasing before
                                        // the samples photographs the state after it.
                                        if (!hold) {
                                            f.pointerReleased(tapX, tapY);
                                        }
                                    }
                                }
                            }
                        });
                        long at = 0;
                        for (int i = 0; i < samples.length; i++) {
                            final long delta = samples[i] - at;
                            at = samples[i];
                            final String path = dir + "/" + pad4(samples[i]) + ".png";
                            final String[] err = new String[1];
                            if (com.codename1.ui.Display.getInstance().isInTransition()) {
                                // A form transition spins the EDT until the animation
                                // queue drains (Display.flushEdt), so nothing handed to
                                // the EDT is serviced while one is held still -- waiting
                                // on it is a deadlock, not a delay. The transition's own
                                // paint only reads, so its frame can be taken from here,
                                // and its clock moved from here too.
                                com.codename1.flutter.animation.MotionClock.advance(delta);
                                sleep(FRAME_SETTLE_MS);
                                try {
                                    err[0] = shoot(path, new int[] {0, 0, 0, 0, 1});
                                } catch (Throwable t) {
                                    err[0] = String.valueOf(t);
                                }
                            } else {
                                // Advance and run the frame...
                                onEdt(new Runnable() {
                                    @Override
                                    public void run() {
                                        com.codename1.flutter.animation.MotionClock
                                                .advanceAndPump(delta);
                                    }
                                });
                                // ...then let the queued build flush run before
                                // photographing what it produced.
                                onEdt(NOTHING);
                                onEdt(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            err[0] = shoot(path, new int[] {0, 0, 0, 0, 1});
                                        } catch (Throwable t) {
                                            err[0] = String.valueOf(t);
                                        }
                                    }
                                });
                            }
                            if (i > 0) {
                                out.append(',');
                            }
                            out.append("{\"ms\":").append(samples[i])
                                    .append(",\"ok\":").append(err[0] == null).append('}');
                        }
                        com.codename1.flutter.animation.MotionClock.release();
                        return out.append("]}").toString();
                    }
                }));
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_shot",
                "Renders the current form to a PNG at `path`. Optional x/y/w/h crop and "
                        + "`scale` (integer nearest-neighbour zoom, for inspecting fringes).",
                "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},"
                        + "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},"
                        + "\"w\":{\"type\":\"number\"},\"h\":{\"type\":\"number\"},"
                        + "\"scale\":{\"type\":\"number\"}},\"required\":[\"path\"]}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final String path = com.codename1.io.JSONParser.getString(a, "path");
                        final int[] box = new int[5];
                        box[0] = num(a, "x", 0);
                        box[1] = num(a, "y", 0);
                        box[2] = num(a, "w", 0);
                        box[3] = num(a, "h", 0);
                        box[4] = Math.max(1, num(a, "scale", 1));
                        final String[] err = new String[1];
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    err[0] = shoot(path, box);
                                } catch (Throwable t) {
                                    err[0] = String.valueOf(t);
                                }
                            }
                        });
                        return err[0] == null ? "{\"written\":\"" + path + "\"}"
                                : "{\"error\":\"" + err[0].replace('"', '\'') + "\"}";
                    }
                }));
    }

    /**
     * Lists the CN1 components whose absolute bounds intersect a rectangle, innermost
     * last, with class / UIID / bounds / scroll state.
     *
     * <p>Exists because pixel forensics answers "what colour is here" but never "what drew
     * it". Chasing a two-pixel artifact through screenshots cost more than writing this.</p>
     */
    private void addComponentsTool() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_components",
                "Lists components intersecting the rect (x,y,w,h) with class, UIID, "
                        + "absolute bounds, scrollability and scrollVisible.",
                "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"number\"},"
                        + "\"y\":{\"type\":\"number\"},\"w\":{\"type\":\"number\"},"
                        + "\"h\":{\"type\":\"number\"}},\"required\":[\"x\",\"y\"]}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final int x = num(a, "x", 0);
                        final int y = num(a, "y", 0);
                        final int w = Math.max(1, num(a, "w", 1));
                        final int h = Math.max(1, num(a, "h", 1));
                        final StringBuilder sb = new StringBuilder("[");
                        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.ui.Form f =
                                        com.codename1.ui.Display.getInstance().getCurrent();
                                if (f != null) {
                                    collect(f, x, y, w, h, sb, 0);
                                }
                            }
                        });
                        return sb.append("]").toString();
                    }
                }));
    }

    private static void collect(com.codename1.ui.Component c, int x, int y, int w, int h,
            StringBuilder sb, int depth) {
        int cx = c.getAbsoluteX();
        int cy = c.getAbsoluteY();
        // Recurse ALWAYS and report only what intersects: in the Flutter runtime's flat,
        // absolutely-positioned layout a child is regularly outside its parent's bounds,
        // so pruning on the parent hides exactly the components worth finding.
        boolean hit = cx < x + w && cy < y + h && cx + c.getWidth() > x && cy + c.getHeight() > y;
        if (hit) {
        if (sb.length() > 1) {
            sb.append(',');
        }
        sb.append("{\"d\":").append(depth)
                .append(",\"cls\":\"").append(c.getClass().getName())
                .append("\",\"uiid\":\"").append(c.getUIID())
                .append("\",\"b\":[").append(cx).append(',').append(cy).append(',')
                .append(c.getWidth()).append(',').append(c.getHeight())
                .append("],\"sx\":").append(c.isScrollableX())
                .append(",\"sy\":").append(c.isScrollableY())
                .append(",\"sv\":").append(c.isScrollVisible())
                .append('}');
        }
        if (c instanceof com.codename1.ui.Container) {
            com.codename1.ui.Container p = (com.codename1.ui.Container) c;
            for (int i = 0; i < p.getComponentCount(); i++) {
                collect(p.getComponentAt(i), x, y, w, h, sb, depth + 1);
            }
        }
    }



    /// Long enough for the painting loop to redraw at the clock's new value.
    private static final int FRAME_SETTLE_MS = 60;

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /// Runs {@code r} on the EDT and waits, so the next step sees its effects.
    private static void onEdt(Runnable r) {
        com.codename1.ui.Display.getInstance().callSeriallyAndWait(r);
    }

    private static final Runnable NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /// "0,50,100" -> {0,50,100}. Bad entries are skipped rather than failing the run.
    private static int[] parseSamples(String csv) {
        if (csv == null) {
            return new int[0];
        }
        // Split by hand: ParparVM's java.lang.String has no split(), and a call to one
        // translates to a C function nothing defines.
        int[] v = new int[csv.length() + 1];
        int n = 0;
        int from = 0;
        while (from <= csv.length()) {
            int comma = csv.indexOf(',', from);
            String part = (comma < 0 ? csv.substring(from) : csv.substring(from, comma))
                    .trim();
            if (part.length() > 0) {
                try {
                    v[n++] = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    // skip
                }
            }
            if (comma < 0) {
                break;
            }
            from = comma + 1;
        }
        int[] out = new int[n];
        System.arraycopy(v, 0, out, 0, n);
        return out;
    }

    /// Zero-padded so the frames sort in time order in a directory listing.
    private static String pad4(int ms) {
        String s = String.valueOf(ms);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }

    private static String shoot(String path, int[] box) throws java.io.IOException {
        com.codename1.ui.Form f = com.codename1.ui.Display.getInstance().getCurrent();
        if (f == null) {
            return "no current form";
        }
        com.codename1.ui.Image img =
                com.codename1.ui.Image.createImage(f.getWidth(), f.getHeight());
        // A form transition paints the frame BETWEEN two forms, so neither form can be
        // asked what is on screen while one is running: the destination gives the
        // finished state and the source gives the state before it began. Capturing a
        // page push by painting the destination is why every sampled frame of one came
        // back already settled.
        com.codename1.ui.animations.Transition running =
                com.codename1.ui.Display.getInstance().getRunningTransition();
        if (running != null) {
            running.paint(img.getGraphics());
        } else {
            f.paintComponent(img.getGraphics(), true);
        }
        int x = box[0];
        int y = box[1];
        int w = box[2] > 0 ? box[2] : f.getWidth() - x;
        int h = box[3] > 0 ? box[3] : f.getHeight() - y;
        w = Math.min(w, f.getWidth() - x);
        h = Math.min(h, f.getHeight() - y);
        if (w <= 0 || h <= 0) {
            return "empty crop";
        }
        // Codename One's own image API, not AWT: this class is compiled for every target,
        // and java.awt/javax.imageio do not exist under ParparVM - an AWT screenshot tool
        // breaks the iOS build of the whole app.
        com.codename1.ui.Image crop = img.subImage(x, y, w, h, true);
        int s = box[4];
        if (s > 1) {
            int[] rgb = crop.getRGB();
            int[] out = new int[w * s * h * s];
            for (int py = 0; py < h * s; py++) {
                int srcRow = (py / s) * w;
                int dstRow = py * w * s;
                for (int px = 0; px < w * s; px++) {
                    out[dstRow + px] = rgb[srcRow + px / s];
                }
            }
            crop = com.codename1.ui.Image.createImage(out, w * s, h * s);
        }
        com.codename1.ui.util.ImageIO io = com.codename1.ui.util.ImageIO.getImageIO();
        if (io == null) {
            return "no ImageIO on this platform";
        }
        // FileSystemStorage speaks file:// URLs, not host paths, on every platform.
        String target = path.startsWith("file:") ? path
                : (path.startsWith("/") ? "file://" + path
                        : com.codename1.io.FileSystemStorage.getInstance().getAppHomePath() + path);
        java.io.OutputStream os =
                com.codename1.io.FileSystemStorage.getInstance().openOutputStream(target);
        try {
            io.save(crop, os, com.codename1.ui.util.ImageIO.FORMAT_PNG, 1f);
        } finally {
            os.close();
        }
        return null;
    }

    private void addPointerTool() {
        com.codename1.mcp.MCP.addTool(new com.codename1.ai.Tool("bench_pointer",
                "Injects a pointer gesture at absolute screen coordinates: press at (x,y), "
                        + "drag in `steps` increments to (x2,y2) when given, then release. "
                        + "Exercises real hit-testing, unlike ui_activate.",
                "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"number\"},"
                        + "\"y\":{\"type\":\"number\"},\"x2\":{\"type\":\"number\"},"
                        + "\"y2\":{\"type\":\"number\"},\"steps\":{\"type\":\"number\"},"
                        + "\"hold\":{\"type\":\"boolean\"}},\"required\":[\"x\",\"y\"]}",
                new com.codename1.ai.ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) {
                        java.util.Map<String, Object> a = parse(argumentsJson);
                        final int x = num(a, "x", 0);
                        final int y = num(a, "y", 0);
                        final int x2 = num(a, "x2", x);
                        final int y2 = num(a, "y2", y);
                        // 0 is a REAL tap: press and release with nothing between them. It used to be
                        // floored at 1, so every "tap" this tool injected carried a drag event,
                        // and a tap inside a scrollable could be taken for the start of a scroll.
                        // A harness that cannot express the gesture it is testing reports the app
                        // as broken when it is the instrument.
                        // A TAP defaults to zero steps; only a drag defaults to eight.
                        //
                        // The floor was fixed once so that `steps: 0` could express a real
                        // tap, and the DEFAULT was left at eight -- so every tap injected
                        // without naming steps still carried eight drag events at the same
                        // coordinates. A zero-distance drag has no dominant axis, which
                        // makes a vertically scrollable ancestor grab the gesture, and the
                        // release then never reaches what was pressed. Whole screens read
                        // as dead: the Cupertino picker demo answered 0.0% to every tap and
                        // opens perfectly once the tap is a tap.
                        final boolean isDrag = x2 != x || y2 != y;
                        final int steps = Math.max(0, num(a, "steps", isDrag ? 8 : 0));
                        final boolean hold = "true".equals(String.valueOf(a.get("hold")));
                        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                            @Override
                            public void run() {
                                com.codename1.ui.Form f =
                                        com.codename1.ui.Display.getInstance().getCurrent();
                                if (f == null) {
                                    return;
                                }
                                // Time each phase on the EDT. A gesture that "works" but
                                // costs hundreds of ms per drag event is what an unresponsive
                                // UI actually is - real input arrives continuously, so the
                                // per-event cost IS the frame budget.
                                long t0 = System.currentTimeMillis();
                                f.pointerPressed(x, y);
                                long press = System.currentTimeMillis() - t0;
                                long worst = 0;
                                long total = 0;
                                for (int i = 1; i <= steps; i++) {
                                    long s = System.currentTimeMillis();
                                    f.pointerDragged(x + (x2 - x) * i / steps,
                                            y + (y2 - y) * i / steps);
                                    long d = System.currentTimeMillis() - s;
                                    total += d;
                                    worst = Math.max(worst, d);
                                }
                                if (hold) {
                                    // Leave the finger DOWN. Press feedback (the ink
                                    // highlight and splash) is only on screen for a few
                                    // hundred ms after a release, which a screenshot
                                    // round-trip cannot reliably catch; held, it stays put.
                                    com.codename1.io.Log.p("bench_pointer: holding at "
                                            + x2 + "," + y2);
                                    return;
                                }
                                long s = System.currentTimeMillis();
                                f.pointerReleased(x2, y2);
                                long release = System.currentTimeMillis() - s;
                                com.codename1.io.Log.p("bench_pointer: press=" + press
                                        + "ms drags=" + steps + " avg="
                                        + (steps == 0 ? 0 : total / steps) + "ms worst="
                                        + worst + "ms release=" + release + "ms");
                            }
                        });
                        return "{\"sent\":true,\"from\":[" + x + "," + y + "],\"to\":["
                                + x2 + "," + y2 + "]}";
                    }
                }));
    }

    private static int num(java.util.Map<String, Object> m, String key, int fallback) {
        Object v = m == null ? null : m.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return v == null ? fallback : (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException err) {
            return fallback;
        }
    }

    private static java.util.Map<String, Object> parse(String json) {
        try {
            return new com.codename1.io.JSONParser().parseJSON(
                    new java.io.StringReader(json == null ? "{}" : json));
        } catch (java.io.IOException err) {
            return new java.util.HashMap<String, Object>();
        }
    }

    /// True when the baseline-mode marker file is present. One stat() on the
    /// start-up path, and only in this benchmark harness -- never in the runtime.
    private static boolean baselineMode() {
        return markerFile("CN1_BASELINE");
    }

    /// True when the named switch file exists. Used instead of an environment
    /// variable because ParparVM has no System.getenv -- neither vm/JavaAPI nor
    /// Ports/CLDC11 declares it, so a build using it would fail to link.
    private static boolean markerFile(String name) {
        try {
            com.codename1.io.FileSystemStorage fs =
                    com.codename1.io.FileSystemStorage.getInstance();
            return fs.exists("file:///tmp/nat/" + name)
                    || fs.exists("/tmp/nat/" + name);
        } catch (Throwable t) {
            return false;
        }
    }
}
