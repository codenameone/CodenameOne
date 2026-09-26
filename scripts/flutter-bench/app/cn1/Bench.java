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
 *
 * <p>This is the MEASURED build, so it carries nothing the Flutter side's
 * lib/main_bench.dart does not: the gallery, the first-frame marker and the
 * phase timings. It used to also open an MCP driving channel with a dozen
 * harness tools, which put the MCP server, its JSON and socket code and every
 * tool into the binary being sized against Flutter -- scaffolding counted as
 * Codename One's code. Driving tools belong in a build that is not measured.
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

    /// Compute mode: the VM workloads (vm/benchmarks' CommonWorkloads, copied in
    /// by prepare.sh) instead of the gallery, in the same binary. The Flutter app
    /// runs the Dart port of the same workloads with the identical repetition
    /// scheme, and both print one `BENCH:COMPUTE` line per workload.
    private static final int COMPUTE_WARMUP = 2;
    private static final int COMPUTE_MEASURED = 5;

    /// Asked for by a marker file on the desktop, the launch intent's data URI on
    /// Android (which the port publishes as AppArg), or the page's query string on
    /// the web -- the channels the Flutter app reads the same request from.
    private static boolean computeRequested() {
        if (markerFile("BENCH_COMPUTE")) {
            return true;
        }
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        String arg = d.getProperty("AppArg", null);
        if (arg != null && arg.indexOf("benchcompute") >= 0) {
            return true;
        }
        String search = d.getProperty("browser.window.location.search", null);
        return search != null && search.indexOf("benchCompute") >= 0;
    }

    private interface Workload {
        long run();
    }

    private static void computeOne(String name, Workload w) {
        long checksum = 0;
        for (int i = 0; i < COMPUTE_WARMUP; i++) {
            checksum = w.run();
        }
        long best = -1;
        for (int i = 0; i < COMPUTE_MEASURED; i++) {
            long start = System.currentTimeMillis();
            checksum = w.run();
            long ms = System.currentTimeMillis() - start;
            if (best < 0 || ms < best) {
                best = ms;
            }
        }
        System.out.println("BENCH:COMPUTE name=" + name + " checksum=" + checksum + " ms=" + best);
        System.out.flush();
    }

    /// Off the event dispatch thread, as the Flutter side runs off the platform's
    /// main thread: a long computation on the EDT would stall the port's own loop.
    private static void runCompute() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                computeOne("intArithmetic", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.intArithmetic(); }
                });
                computeOne("longArithmetic", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.longArithmetic(); }
                });
                computeOne("mathTranscendental", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.mathTranscendental(); }
                });
                computeOne("arraySequential", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.arraySequential(); }
                });
                computeOne("arrayRandom", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.arrayRandom(); }
                });
                computeOne("objectAllocation", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.objectAllocation(); }
                });
                computeOne("valueEscape", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.valueEscape(); }
                });
                computeOne("hashMapChurn", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.hashMapChurn(); }
                });
                computeOne("stringBuilding", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.stringBuilding(); }
                });
                computeOne("recursion", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.recursion(); }
                });
                computeOne("quicksortBench", new Workload() {
                    public long run() { return com.bench.CommonWorkloads.quicksortBench(); }
                });
                System.out.println("BENCH:COMPUTE-DONE");
                System.out.flush();
            }
        }).start();
    }

    @Override
    public void runApp() {
        if (computeRequested()) {
            runCompute();
            return;
        }
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
