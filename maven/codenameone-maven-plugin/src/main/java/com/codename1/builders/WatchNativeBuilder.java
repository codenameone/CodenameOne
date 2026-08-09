/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.builders;

import org.apache.tools.ant.BuildException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper extracted from {@link IPhoneBuilder} that owns the Apple Watch
 * (watchOS) native build path. Activated by the project declaring a watch
 * lifecycle class, {@code codename1.watchMain}; there are no other watch build
 * hints, everything else is derived.
 *
 * <p>Unlike {@link MacNativeBuilder} (which Mac-Catalyst-slices the SAME iOS app
 * target), a watchOS app is a distinct product: it has its own bundle, its own
 * {@code WKApplication} Info.plist, and the {@code arm64_32} architecture. So
 * this builder <b>adds a second Xcode target</b> to the generated project,
 * compiles the ParparVM-generated sources (minus the GL/Metal-only files) for
 * watchOS, and embeds the watch app inside the iOS {@code .app} via an "Embed
 * Watch Content" copy-files phase so the pair installs together. A project that
 * sets {@code codename1.watchStandalone=true} ships a watch-only product with no
 * paired phone app instead. The watch UI is rendered by the Core Graphics
 * backend ({@code CN1CGGraphics} + {@code CN1WatchRenderingView}) driven by
 * {@code CN1WatchHost}.
 *
 * <p>The underlying mechanism is a Ruby {@code xcodeproj} script (same toolchain
 * macNative relies on). Like {@link MacNativeBuilder} this is a delegate owned
 * by {@link IPhoneBuilder}, invoked at hint-parse time and at the
 * post-project-generate patching point. Every change is additive: without a
 * {@code watchMain} the iOS build is byte-for-byte unchanged.
 */
class WatchNativeBuilder {
    private final IPhoneBuilder owner;

    // watchOS floor: single-target WKApplication apps, WidgetKit complications,
    // and the SwiftUI onChange(of:) two-parameter API the generated
    // CN1WatchRootView uses.
    private static final String MIN_DEPLOYMENT_TARGET = "10.0";

    // Derived build state.
    private boolean enabled;
    private boolean standalone;        // codename1.watchStandalone
    private String bundleId;
    private String teamId;
    private String displayName;
    // Fully-qualified watch lifecycle entry class (codename1.watchMain). Its
    // presence is what turns the watch build on, and it is the root the watch
    // slice is translated from. Empty when the project declares no watch app.
    private String watchMain;
    // Whether the watch shakes from its own root rather than the phone's.
    // A distinct root means the phone's health usage says nothing about
    // the watch's, so the HealthKit entitlement cannot be inferred from
    // the phone's privacy strings.
    private boolean distinctWatchMain;
    // Explicit opt-in/out for HealthKit on the watch bundle.
    private String healthHint;
    // watchNative.health.workoutProcessing, kept so the entitlement
    // decision can read it too -- a workout session is HealthKit.
    private String workoutProcessingHint;

    // Files the watch target cannot take at all -- not a policy list, a mechanical
    // one. Everything that CAN be guarded is guarded in the source instead, with
    // `#if !TARGET_OS_WATCH` wrapping the whole file, so a new GL/Metal/UIKit
    // source carries its own exclusion and cannot silently break the watch build
    // by being forgotten here. These five have no preprocessor to run:
    // a .metal shader is compiled by the Metal compiler (absent on watchOS) and a
    // .xib is Interface Builder data.
    private static final String[] EXCLUDED_WATCH_SOURCES = {
        "CN1MetalShaders.metal",
        "CodenameOne_GLViewController.xib", "MainWindow.xib",
        "CodenameOne_METALViewController.xib", "MainWindowMETAL.xib"
    };

    // Frameworks the iOS port links that are unavailable on watchOS; ParparVM
    // weak-links these (see -Doptional.frameworks) so the iOS slice is unchanged
    // while the watch slice tolerates the absent symbols.
    private static final String WATCH_OPTIONAL_FRAMEWORKS =
            "OpenGLES.framework;GLKit.framework;Metal.framework;"
            + "MapKit.framework;MediaPlayer.framework;MessageUI.framework;"
            + "AddressBookUI.framework;AddressBook.framework;"
            + "WebKit.framework;StoreKit.framework;"
            // CarPlay.framework is iOS-only (absent on watchOS); it is linked on the iOS slice when
            // the app references com.codename1.car, so weak-link it for the watch slice.
            + "CarPlay.framework;"
            // ARKit and SceneKit are absent on watchOS; they are linked on the iOS slice when the
            // app references com.codename1.ar, so weak-link them for the watch slice.
            + "ARKit.framework;SceneKit.framework";

    WatchNativeBuilder(IPhoneBuilder owner) {
        this.owner = owner;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Resolve the watch build from the project's entry points. The watch app is
     * built whenever the project declares a watch lifecycle class
     * ({@code codenameone_settings.properties -> codename1.watchMain}, arriving
     * here as the {@code watchMain} argument); everything else is derived. The
     * only other recognized setting is {@code codename1.watchStandalone}, which
     * says the watch app ships on its own rather than inside the phone app --
     * the one thing that cannot be inferred from the project.
     *
     * <p>Caller flips Metal on (the watch slice cannot use GL ES; the iOS slice
     * still wants Metal) and raises the watch deployment floor.
     */
    void parseHints(BuildRequest request) {
        watchMain = request.getArg("watchMain", "").trim();
        // Read before the enablement check, deliberately: the HealthKit entitlement decision
        // consults these even for a project that declares no watch app, and returning early first
        // would silently turn an explicit watchNative.health=false back into inference.
        // getMainClass() is the SIMPLE class name while watchMain is fully qualified, so comparing
        // them directly marked every project as having a distinct watch root -- including one whose
        // watchMain names the very same class. That matters because "distinct" is what tells the
        // HealthKit inference it cannot read the watch's usage from the phone's privacy strings.
        String phoneMainFqn = request.getPackageName() == null || request.getPackageName().isEmpty()
                ? request.getMainClass()
                : request.getPackageName() + "." + request.getMainClass();
        distinctWatchMain = watchMain.length() > 0
                && !watchMain.equals(request.getMainClass())
                && !watchMain.equals(phoneMainFqn);
        healthHint = request.getArg("watchNative.health", "").trim();
        workoutProcessingHint = request.getArg(
                "watchNative.health.workoutProcessing", "false").trim();
        enabled = watchMain.length() > 0;
        if (!enabled) {
            return;
        }
        // Everything below is derived rather than hinted. The watchNative.* settings master used
        // here (distribution, bundleId, minDeploymentTarget, teamId, displayName) are gone: the
        // whole point of this change is that codename1.watchMain plus the optional
        // codename1.watchStandalone are the entire surface, and the rest comes from settings the
        // project already has. The health hints above are the exception -- they select an
        // entitlement, which is not derivable from anything else.
        standalone = "true".equals(request.getArg("watchStandalone", "false"));
        bundleId = request.getPackageName() + ".watchkitapp";
        // Selected by BUILD TYPE, exactly as the phone target selects it. Preferring the release
        // team unconditionally meant a debug build paired a debug provisioning profile with the
        // release team's DEVELOPMENT_TEAM, and manual signing of the embedded watch target failed
        // on the mismatch -- for a project that simply set both hints.
        String watchTeamDefault = request.getArg("ios.teamId", "");
        teamId = "debug".equals(request.getArg("ios.buildType", "debug"))
                ? request.getArg("ios.debug.teamId", watchTeamDefault)
                : request.getArg("ios.release.teamId", watchTeamDefault);
        displayName = request.getDisplayName() != null
                ? request.getDisplayName() : request.getMainClass();
    }

    /// The team id this build resolved for the watch target, selected by build type like the
    /// phone's. Package-visible for the tests that pin that selection.
    String getTeamId() {
        return teamId;
    }

    boolean isStandalone() {
        return standalone;
    }

    /** Fully-qualified watch lifecycle entry class. */
    String getWatchMain() {
        return watchMain;
    }

    /** ParparVM mangles a Java FQN to C by replacing '.', '/', '$' with '_'. */
    private static String mangle(String fqn) {
        return fqn == null ? "" : fqn.replace('.', '_').replace('/', '_').replace('$', '_');
    }

    /**
     * Generate the watch target's entry point into {@code appSrcDir}:
     * <ul>
     *   <li>a SwiftUI {@code @main} app shell that hosts {@link
     *       com.codename1 CN1WatchHost} frames and forwards Digital Crown + tap
     *       input;</li>
     *   <li>{@code CN1WatchBootstrap.m}, which defines the {@code cn1_watch_*}
     *       entry points {@code CN1WatchHost} calls, delegating to the CN1
     *       runtime started at the {@link #watchMain} lifecycle class;</li>
     *   <li>a bridging header exposing the host to Swift.</li>
     * </ul>
     * The watch app is SwiftUI-rooted, so the shared ParparVM {@code int main()}
     * (the phone entry) is excluded from the watch target in {@link
     * #applyXcodeSettings}.
     */
    /// Whether the watch app needs a ParparVM translation of its own.
    ///
    /// Only when {@code watchMain} names a class the phone does not boot. A project whose watch and
    /// phone entry points are the same class reaches the same code either way, so a second
    /// translation would double the build for an identical binary.
    boolean needsOwnTranslation() {
        return enabled && distinctWatchMain;
    }

    /// The translator root for the watch: the class name the second pass is given, matching the
    /// watch target's own name so the two are obviously the same thing.
    static String translationRoot(String mainClass) {
        return mainClass + "Watch";
    }

    /// Where the second translation is written, kept out of the phone's output root so neither pass
    /// can see the other's dist tree.
    static File translationDir(File tmpFile) {
        return new File(tmpFile, "watchvm");
    }

    /// Moves each entry-point Stub into a directory of its own, so a translation pass can be given
    /// exactly one of them.
    ///
    /// The translator parses everything on its classpath and REFUSES a classpath carrying two mains
    /// ("Multiple main classes"). Both stubs compile into the same classes directory, so leaving
    /// them there breaks both passes -- not just the watch one. Relocating them means the phone
    /// pass sees the phone stub, the watch pass sees the watch stub, and the application classes
    /// are shared by both without being copied.
    ///
    /// Inner classes travel with their outer class: a Stub with an anonymous Runnable in it emits
    /// Stub$1.class, and leaving that behind would fail to resolve.
    ///
    /// Only called when the watch has its own translation, so a project without one keeps the exact
    /// classpath it had before.
    ///
    /// @return the directory holding the requested stub
    File isolateStub(BuildRequest request, File classesDir, File tmpFile, boolean watch)
            throws IOException {
        String stubClass = watch
                ? translationRoot(request.getMainClass()) + "Stub"
                : request.getMainClass() + "Stub";
        File dest = new File(tmpFile, watch ? "watchstub" : "phonestub");
        String pkgPath = request.getPackageName() == null || request.getPackageName().isEmpty()
                ? "" : request.getPackageName().replace('.', File.separatorChar);
        File fromDir = pkgPath.isEmpty() ? classesDir : new File(classesDir, pkgPath);
        File toDir = pkgPath.isEmpty() ? dest : new File(dest, pkgPath);
        toDir.mkdirs();
        File[] candidates = fromDir.listFiles();
        if (candidates == null) {
            throw new IOException("no compiled classes at " + fromDir);
        }
        int moved = 0;
        for (File f : candidates) {
            String name = f.getName();
            if (!f.isFile() || !name.endsWith(".class")) {
                continue;
            }
            // The stub itself and its inner classes, and nothing whose name merely starts the same
            // way -- MyAppStub must not carry off MyAppStubHelper.
            if (!name.equals(stubClass + ".class") && !name.startsWith(stubClass + "$")) {
                continue;
            }
            IPhoneBuilder.copy(f, new File(toDir, name));
            if (!f.delete()) {
                throw new IOException("could not move " + f + " out of the shared classes tree; "
                        + "leaving it there would give the translator two main classes");
            }
            moved++;
        }
        if (moved == 0) {
            throw new IOException("expected " + stubClass + ".class in " + fromDir);
        }
        return dest;
    }

    /// Writes the watch app's own Stub -- the root the watch translation is tree-shaken from.
    ///
    /// The phone Stub cannot serve: it instantiates the PHONE lifecycle class, so a watch binary
    /// rooted there drags in the phone's entire reachable graph and then starts the phone UI. That
    /// is precisely what the {@code -Dmain=cn1_watch_phone_main_unused} hack existed to hide -- the
    /// watch target compiled the phone's translation and had its {@code main} defined away, so
    /// {@code watchMain} was never really the entry point and nothing was shaken out for the watch.
    ///
    /// Deliberately smaller than the phone stub. Push callbacks, URL handling, maps, sign-in
    /// providers and the ad padding are all phone concerns; pulling them in here would defeat the
    /// point of rooting the translation somewhere else.
    void writeWatchStubSource(BuildRequest request, File stubSource, String buildVersion,
            String nativeRegistration, String iosMode, String svgRegistryInstall,
            String healthBindingsInstall, String routeDispatcherInstall,
            String annotationFrameworksInstall)
            throws IOException {
        String stubClass = translationRoot(request.getMainClass()) + "Stub";
        String body = "package " + request.getPackageName() + ";\n\n"
                + "import com.codename1.ui.*;\n"
                // The native registration block below calls NativeLookup, exactly as the phone
                // stub's does. Importing only the UI package left the generated stub referring to
                // a class it had never imported -- which javac only reports once a project
                // actually HAS a native interface, so a sample without one would have hidden it.
                + "import com.codename1.system.*;\n\n"
                + "/** Generated watch entry point. Rooted at codename1.watchMain (" + watchMain
                + "). */\n"
                + "public class " + stubClass
                + " extends com.codename1.impl.ios.Lifecycle implements Runnable {\n"
                + "    public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\";\n"
                + "    public static final String APPLICATION_VERSION = \"" + buildVersion + "\";\n"
                + "    public static final String APPLICATION_NAME = \""
                + request.getDisplayName() + "\";\n"
                + "    private " + watchMain + " i = new " + watchMain + "();\n"
                + "    private boolean initialized = false;\n"
                + "    private boolean stopped = false;\n\n"
                + "    public void run() {\n"
                + "        Display.getInstance().setProperty(\"package_name\", PACKAGE_NAME);\n"
                + "        Display.getInstance().setProperty(\"AppVersion\", APPLICATION_VERSION);\n"
                + "        Display.getInstance().setProperty(\"AppName\", APPLICATION_NAME);\n"
                + "        if(!initialized) {\n"
                + "            initialized = true;\n"
                // The SAME registry installs the phone stub emits.
                //
                // They are static references, and that is the whole point: every one of these
                // registries is reached only reflectively, so these calls are what keep the
                // generated classes in the reachable graph. Emitting only some of them let the
                // watch translation shake out the rest -- correct tree-shaking, wrong result. The
                // SVG omission was visible (SVG and Lottie fell back to a placeholder render on the
                // watch while working on the phone); generated routes, annotation-backed
                // mappers/DAOs/clients and the health bindings fail the same way, silently, on a
                // watch app that happens to use them.
                + svgRegistryInstall
                + healthBindingsInstall
                + "            i.init(this);\n"
                + "        }\n"
                + "        i.start();\n"
                + "    }\n\n"
                // The same suspend/resume contract the phone stub implements, because it is the
                // same lifecycle class on both. Without these a watch app was only ever started
                // and terminated: leaving it stopped the paint pump but never called stop(), so
                // timers and resources it releases there ran on through the suspension, and
                // reopening it never called start() again, so refresh-on-foreground work that
                // happens on the phone silently did not happen on the watch.
                + "    public void applicationDidEnterBackground() {\n"
                + "        if(!stopped) {\n"
                + "            stopped = true;\n"
                + "            Display.getInstance().callSerially(new Runnable() {\n"
                + "                public void run() {\n"
                + "                    i.stop();\n"
                + "                }\n"
                + "            });\n"
                + "        }\n"
                + "    }\n\n"
                + "    public void applicationWillEnterForeground() {\n"
                // Re-runs this Runnable rather than calling start() directly, so the resumed
                // session goes through exactly the path the first launch did -- initialized is
                // already true, so it is start() and nothing else.
                + "        if(stopped) {\n"
                + "            stopped = false;\n"
                + "            Display.getInstance().callSerially(this);\n"
                + "        }\n"
                + "    }\n\n"
                + "    public void applicationWillTerminate() {\n"
                + "        if(!stopped) {\n"
                + "            i.stop();\n"
                + "            stopped = true;\n"
                + "        }\n"
                + "        i.destroy();\n"
                + "    }\n\n"
                + "    public static void main(String[] argv) {\n"
                + "        if(!(argv != null && argv.length > 0 && argv[0].equals(\"ignoreNative\"))) {\n"
                + nativeRegistration
                + "        }\n"
                + "        " + stubClass + " stub = new " + stubClass + "();\n"
                + "        com.codename1.impl.ios.IOSImplementation.setMainClass(stub.i);\n"
                + "        com.codename1.impl.ios.IOSImplementation.setIosMode(\"" + iosMode + "\");\n"
                // Same position as the phone stub's: before Display.init, after the implementation
                // is known. The generated route dispatcher and the annotation frameworks are found
                // reflectively too, so without these the second translation drops their bootstrap
                // classes and navigation on the watch resolves nothing.
                + routeDispatcherInstall
                + annotationFrameworksInstall
                + "        Display.init(stub);\n"
                + "    }\n"
                + "}\n";
        java.io.OutputStream out = new java.io.FileOutputStream(
                new File(stubSource, stubClass + ".java"));
        try {
            out.write(body.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        owner.log("[watchNative] Wrote " + stubClass + ".java; the watch slice is translated from "
                + watchMain + " rather than sharing the phone's translation");
    }

    /// Moves the watch translation next to the Xcode project and reports its source file names.
    ///
    /// The second pass writes into its own root so the two translations cannot see each other, but
    /// the project has to reference the files with a path relative to itself -- so the tree is
    /// copied into {@code <MainClass>-src/watch-src/} once, here.
    ///
    /// @return the base names the watch target must compile, empty when there is no own translation
    List<String> stageWatchTranslation(BuildRequest request, File tmpFile, File appSrcDir)
            throws IOException {
        List<String> compiled = new ArrayList<String>();
        if (!needsOwnTranslation()) {
            return compiled;
        }
        File from = new File(translationDir(tmpFile),
                "dist/" + translationRoot(request.getMainClass()) + "-src");
        if (!from.isDirectory()) {
            throw new IOException("the watch translation produced no sources at " + from);
        }
        File to = new File(appSrcDir, WATCH_SRC_DIR);
        to.mkdirs();
        File[] files = from.listFiles();
        if (files == null) {
            throw new IOException("could not read the watch translation at " + from);
        }
        for (File f : files) {
            String name = f.getName();
            if (!f.isFile()) {
                continue;
            }
            // .swift belongs here too. A NativeInterface implemented in Swift is emitted by the
            // second translator pass like any other source, and skipping it left the watch target
            // with no implementation for a native call the watch code makes -- while the phone's
            // Swift phase fixup, which globs <Main>-src/**/*.swift, would have swept the watch
            // copy into the PHONE target instead. It is excluded from that glob for the same
            // reason (see IPhoneBuilder's swift fixup).
            boolean source = name.endsWith(".m") || name.endsWith(".c")
                    || name.endsWith(".swift") || name.endsWith(".mm")
                    || name.endsWith(".cpp") || name.endsWith(".cc");
            if (!source && !name.endsWith(".h")) {
                // Only the code. The watch bundle's plist, resources and project file are written
                // by this builder against the PHONE project -- taking the second translation's
                // copies would overwrite them with ones describing a standalone app.
                continue;
            }
            IPhoneBuilder.copy(f, new File(to, name));
            if (source) {
                compiled.add(name);
            }
        }
        // A prefix header of the watch tree's OWN, so its quoted includes resolve there.
        //
        // The phone's pch does #include "cn1_class_method_index.h", and a quoted include resolves
        // against the directory of the file doing the including -- before any HEADER_SEARCH_PATHS.
        // Compiled with the phone's pch, every watch source therefore saw the PHONE's class index,
        // which does not declare the watch stub's class id: the watch translation's own files
        // failed on an identifier they had just been generated to use. Copying the same pch into
        // the watch tree changes nothing but where those includes land.
        File phonePch = new File(appSrcDir, request.getMainClass() + "-Prefix.pch");
        if (phonePch.isFile()) {
            IPhoneBuilder.copy(phonePch, new File(to, watchPrefixHeader(request.getMainClass())));
        }
        owner.log("[watchNative] Staged " + compiled.size()
                + " translated watch sources from " + watchMain);
        return compiled;
    }

    /// Where the staged watch translation lives, relative to the app's -src directory.
    static final String WATCH_SRC_DIR = "watch-src";

    /// The watch slice's prefix header, which must sit INSIDE the staged tree.
    static String watchPrefixHeader(String mainClass) {
        return translationRoot(mainClass) + "-Prefix.pch";
    }

    void writeWatchEntry(BuildRequest request, File appSrcDir) throws IOException {
        appSrcDir.mkdirs();
        String mainClass = request.getMainClass();

        // 1) SwiftUI @main shell.
        // The whole file is guarded with `#if os(watchOS)`: it is generated into
        // the shared <mainClass>-src/ dir and gets globbed into the iOS / Mac
        // Catalyst app target too, where WatchKit does not exist. The guard makes
        // it an empty translation unit everywhere except watchOS, so the iOS
        // build can't fail on `import WatchKit` even if the file is compiled.
        StringBuilder sw = new StringBuilder();
        sw.append("#if os(watchOS)\n")
          .append("import SwiftUI\n")
          .append("import WatchKit\n\n")
          .append("// Generated watch entry point. Hosts the Codename One Core\n")
          .append("// Graphics frames produced by CN1WatchHost (started at the\n")
          .append("// watchMain lifecycle class ").append(watchMain).append(").\n")
          .append("@main\n")
          .append("struct CN1WatchApp: App {\n")
          .append("    @WKApplicationDelegateAdaptor var delegate: CN1WatchAppDelegate\n")
          .append("    var body: some Scene {\n")
          .append("        WindowGroup { CN1WatchRootView() }\n")
          .append("    }\n")
          .append("}\n\n")
          .append("final class CN1WatchAppDelegate: NSObject, WKApplicationDelegate {\n")
          .append("    func applicationDidBecomeActive() { CN1WatchHost.shared().applicationDidBecomeActive() }\n")
          .append("    func applicationWillResignActive() { CN1WatchHost.shared().applicationWillResignActive() }\n")
          // The active/resign pair only starts and stops the paint pump. These two carry the CN1
          // application lifecycle -- the stub's stop() and start() -- which a watch app was never
          // given: leaving and reopening it left timers and resources running through the
          // suspension and skipped whatever the lifecycle class does on foreground, all of which
          // the same class receives on the phone.
          .append("    func applicationDidEnterBackground() { CN1WatchHost.shared().applicationDidEnterBackground() }\n")
          .append("    func applicationWillEnterForeground() { CN1WatchHost.shared().applicationWillEnterForeground() }\n")
          .append("}\n\n")
          .append("// Surface bridge: CN1WatchHost pushes rendered frames here (main thread);\n")
          .append("// SwiftUI observes `image` and redraws.\n")
          .append("final class CN1WatchFrameModel: NSObject, ObservableObject, CN1WatchSurface {\n")
          .append("    @Published var image: UIImage?\n")
          .append("    func displayFrame(_ frame: UIImage) { self.image = frame }\n")
          .append("}\n\n")
          .append("struct CN1WatchRootView: View {\n")
          .append("    @StateObject private var model = CN1WatchFrameModel()\n")
          .append("    @State private var crown: Double = 0\n")
          .append("    var body: some View {\n")
          .append("        GeometryReader { geo in\n")
          .append("            ZStack {\n")
          .append("                Color.black\n")
          .append("                if let img = model.image {\n")
          .append("                    Image(uiImage: img).resizable().frame(width: geo.size.width, height: geo.size.height)\n")
          .append("                }\n")
          .append("            }\n")
          .append("            .focusable(true)\n")
          .append("            .digitalCrownRotation($crown, from: -1_000_000, through: 1_000_000,\n")
          .append("                                  by: 1, sensitivity: .medium, isContinuous: true)\n")
          .append("            .onChange(of: crown) { oldValue, newValue in\n")
          .append("                CN1WatchHost.shared().crownRotated(by: newValue - oldValue)\n")
          .append("            }\n")
          .append("            .gesture(SpatialTapGesture().onEnded { e in\n")
          .append("                CN1WatchHost.shared().tapAt(x: Int32(e.location.x), y: Int32(e.location.y))\n")
          .append("            })\n")
          .append("            .ignoresSafeArea()\n")
          .append("            .onAppear {\n")
          .append("                let d = WKInterfaceDevice.current()\n")
          .append("                CN1WatchHost.shared().surface = model\n")
          .append("                CN1WatchHost.shared().start(withWidth: Int32(d.screenBounds.width),\n")
          .append("                                            height: Int32(d.screenBounds.height),\n")
          .append("                                            scale: d.screenScale)\n")
          .append("            }\n")
          .append("        }\n")
          .append("        .ignoresSafeArea()\n")
          .append("    }\n")
          .append("}\n")
          .append("#endif // os(watchOS)\n");
        owner.createFile(new File(appSrcDir, "CN1WatchApp.swift"),
                sw.toString().getBytes(StandardCharsets.UTF_8));

        // 2) Bootstrap: defines the cn1_watch_* hooks CN1WatchHost calls and
        //    routes them to the CN1 runtime started at the watchMain class.
        //    cn1_watch_runtime_* are emitted by the generated watch Stub /
        //    runtime (see WATCHOS_PORT.md); declared here so the watch target
        //    links against them.
        String m = mangle(watchMain);
        // The screenshot/test build runs the regular main class (it drives
        // Cn1ssDeviceRunner from start()), so the watch bootstrap enters through
        // the regular main class's generated Stub.main. cn1_watch_app_main is the
        // app-specific hook invoked by cn1_watch_runtime_start (CN1WatchRuntime.m)
        // on a dedicated thread; Stub.main sets the main class and calls
        // Display.init, which starts the EDT and blocks the thread inside initVM
        // (mirroring iOS main() + UIApplicationMain).
        // request.getMainClass() is the SIMPLE class name; the generated Stub's
        // C symbol is mangled from the fully-qualified <package>.<Main>Stub.
        String mainFqn = (request.getPackageName() == null || request.getPackageName().isEmpty())
                ? mainClass : (request.getPackageName() + "." + mainClass);
        // The stub the watch actually boots. With a translation of its own that is the WATCH stub
        // -- the phone's symbol is not in the watch binary at all, because the watch tree was
        // shaken from a different root. Sharing the phone's translation is the only case where the
        // phone stub is the right entry, and there its main is defined away so this call is what
        // starts the app.
        String stubFqn = needsOwnTranslation()
                ? ((request.getPackageName() == null || request.getPackageName().isEmpty())
                        ? translationRoot(mainClass)
                        : request.getPackageName() + "." + translationRoot(mainClass))
                : mainFqn;
        String mainStub = mangle(stubFqn) + "Stub";
        StringBuilder bs = new StringBuilder();
        bs.append("#include \"TargetConditionals.h\"\n")
          .append("#if TARGET_OS_WATCH\n")
          .append("#import \"CN1WatchHost.h\"\n")
          .append("#include \"cn1_globals.h\"\n\n")
          .append("// Implemented by CN1WatchRuntime.m (app-agnostic watch runtime glue).\n")
          .append("extern void cn1_watch_runtime_start(const char *watchMainClass);\n")
          .append("extern void cn1_watch_runtime_paint(void);\n")
          .append("extern void cn1_watch_runtime_pointerPressed(int x, int y);\n")
          .append("extern void cn1_watch_runtime_pointerDragged(int x, int y);\n")
          .append("extern void cn1_watch_runtime_pointerReleased(int x, int y);\n")
          .append("extern void cn1_watch_runtime_didEnterBackground(void);\n")
          .append("extern void cn1_watch_runtime_willEnterForeground(void);\n")
          .append("extern void cn1_watch_runtime_markJavaReady(void);\n\n")
          .append("// App-specific entry: register natives + set the main class, init\n")
          .append("// Display (starts the EDT) and block this thread inside initVM.\n")
          .append("extern void ").append(mainStub)
          .append("_main___java_lang_String_1ARRAY(struct ThreadLocalData* threadStateData, JAVA_OBJECT arg);\n")
          .append("void cn1_watch_app_main(void) {\n")
          .append("    ").append(mainStub)
          .append("_main___java_lang_String_1ARRAY(getThreadLocalData(), JAVA_NULL);\n")
          // Display.init has returned by here, so IOSImplementation.instance exists and the EDT is
          // running. THIS is the readiness signal: the runtime used to infer it from
          // [CodenameOne_GLViewController instance] != nil, and that accessor lazily allocates the
          // singleton, so the test made itself true the moment the runtime flag was set and a
          // lifecycle phase could be forwarded into a half-built VM.
          .append("    cn1_watch_runtime_markJavaReady();\n")
          .append("}\n\n")
          .append("// Watch lifecycle entry class (mangled FQN): ").append(m).append("\n")
          .append("void cn1_watch_bootstrap(void) { cn1_watch_runtime_start(\"")
          .append(IPhoneBuilder.escapeRubyStr(watchMain)).append("\"); }\n")
          .append("void cn1_watch_paintFrame(void) { cn1_watch_runtime_paint(); }\n")
          .append("void cn1_watch_pointerPressed(int x, int y) { cn1_watch_runtime_pointerPressed(x, y); }\n")
          .append("void cn1_watch_pointerDragged(int x, int y) { cn1_watch_runtime_pointerDragged(x, y); }\n")
          .append("void cn1_watch_pointerReleased(int x, int y) { cn1_watch_runtime_pointerReleased(x, y); }\n")
          .append("void cn1_watch_didEnterBackground(void) { cn1_watch_runtime_didEnterBackground(); }\n")
          .append("void cn1_watch_willEnterForeground(void) { cn1_watch_runtime_willEnterForeground(); }\n")
          .append("#endif\n");
        owner.createFile(new File(appSrcDir, "CN1WatchBootstrap.m"),
                bs.toString().getBytes(StandardCharsets.UTF_8));

        // 3) Bridging header.
        owner.createFile(new File(appSrcDir, mainClass + "-Watch-Bridging-Header.h"),
                "#import \"CN1WatchHost.h\"\n".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write stub GLKit / OpenGLES headers under {@code watchOSStubs/} so the
     * shared sources that {@code #import <GLKit/...>} / {@code <OpenGLES/...>}
     * (chiefly CN1ES2compat.h) compile on watchOS, where those frameworks don't
     * exist. The stubs provide the GL scalar types + GLKMatrix4/GLKVector*
     * typedefs the declarations reference; the GL functions are never called on
     * the watch slice (the TARGET_OS_WATCH op branches route to CN1CGGraphics).
     * Same approach MacNativeBuilder uses for the Catalyst slice.
     */
    void writeStubHeaders(File appSrcDir) throws IOException {
        File stubsDir = new File(appSrcDir, "watchOSStubs");
        File openGLESes1 = new File(new File(stubsDir, "OpenGLES"), "ES1");
        File openGLESes2 = new File(new File(stubsDir, "OpenGLES"), "ES2");
        File eagl = new File(stubsDir, "OpenGLES");
        File glkit = new File(stubsDir, "GLKit");
        openGLESes1.mkdirs();
        openGLESes2.mkdirs();
        glkit.mkdirs();
        String glTypes =
                "#ifndef CN1_WATCHOS_STUB_GLES_TYPES\n#define CN1_WATCHOS_STUB_GLES_TYPES\n"
                + "typedef unsigned int GLenum;\ntypedef unsigned int GLuint;\n"
                + "typedef int GLint;\ntypedef int GLsizei;\ntypedef float GLfloat;\n"
                + "typedef float GLclampf;\ntypedef unsigned char GLubyte;\n"
                + "typedef unsigned char GLboolean;\ntypedef void GLvoid;\n"
                + "typedef signed char GLbyte;\ntypedef short GLshort;\n"
                + "typedef unsigned short GLushort;\ntypedef int GLfixed;\n"
                + "typedef unsigned int GLbitfield;\ntypedef long GLintptr;\n"
                + "typedef long GLsizeiptr;\n#endif\n";
        writeStub(new File(eagl, "EAGL.h"),
                "#ifndef CN1_WATCHOS_STUB_EAGL_H\n#define CN1_WATCHOS_STUB_EAGL_H\n"
                + "#import <Foundation/Foundation.h>\n"
                + "@interface EAGLContext : NSObject @end\n"
                + "typedef enum { kEAGLRenderingAPIOpenGLES1 = 1, kEAGLRenderingAPIOpenGLES2 = 2,"
                + " kEAGLRenderingAPIOpenGLES3 = 3 } EAGLRenderingAPI;\n#endif\n");
        writeStub(new File(openGLESes1, "gl.h"), glTypes);
        writeStub(new File(openGLESes1, "glext.h"), "");
        writeStub(new File(openGLESes2, "gl.h"), glTypes);
        writeStub(new File(openGLESes2, "glext.h"), "");
        writeStub(new File(glkit, "GLKit.h"),
                "#ifndef CN1_WATCHOS_STUB_GLKIT_H\n#define CN1_WATCHOS_STUB_GLKIT_H\n"
                + "#import <Foundation/Foundation.h>\n#import <OpenGLES/ES2/gl.h>\n"
                + "typedef struct { float m[16]; } GLKMatrix4;\n"
                + "typedef struct { float v[4]; } GLKVector4;\n"
                + "typedef struct { float v[3]; } GLKVector3;\n"
                + "typedef struct { float v[2]; } GLKVector2;\n"
                // Inline GLKit math so the GLKMatrix4 transform machinery in the
                // op files (SetTransform/ClipRect/etc.) compiles on watchOS even
                // though the GLKit framework is absent. The watch render path uses
                // the Core Graphics backend (CN1CGGraphics); these helpers only
                // keep the transform bookkeeping (column-major 4x4) consistent.
                + "static const GLKMatrix4 GLKMatrix4Identity = { { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 } };\n"
                + "static inline GLKVector4 GLKVector4Make(float x,float y,float z,float w){ GLKVector4 v; v.v[0]=x; v.v[1]=y; v.v[2]=z; v.v[3]=w; return v; }\n"
                + "static inline GLKVector3 GLKVector3Make(float x,float y,float z){ GLKVector3 v; v.v[0]=x; v.v[1]=y; v.v[2]=z; return v; }\n"
                + "static inline GLKMatrix4 GLKMatrix4Multiply(GLKMatrix4 a, GLKMatrix4 b){ GLKMatrix4 r; for(int c=0;c<4;c++){ for(int row=0;row<4;row++){ float s=0; for(int k=0;k<4;k++){ s += a.m[k*4+row]*b.m[c*4+k]; } r.m[c*4+row]=s; } } return r; }\n"
                + "static inline GLKMatrix4 GLKMatrix4MakeTranslation(float tx,float ty,float tz){ GLKMatrix4 r = GLKMatrix4Identity; r.m[12]=tx; r.m[13]=ty; r.m[14]=tz; return r; }\n"
                + "static inline GLKMatrix4 GLKMatrix4Translate(GLKMatrix4 m,float tx,float ty,float tz){ return GLKMatrix4Multiply(m, GLKMatrix4MakeTranslation(tx,ty,tz)); }\n"
                + "static inline GLKMatrix4 GLKMatrix4MakeScale(float sx,float sy,float sz){ GLKMatrix4 r = GLKMatrix4Identity; r.m[0]=sx; r.m[5]=sy; r.m[10]=sz; return r; }\n"
                + "@interface GLKView : NSObject @end\n@interface GLKBaseEffect : NSObject @end\n"
                + "@interface GLKTextureLoader : NSObject @end\n@interface GLKTextureInfo : NSObject @end\n#endif\n");
        owner.log("[watchNative] Wrote watchOS stub headers under " + stubsDir.getAbsolutePath());
    }

    private void writeStub(File f, String content) throws IOException {
        owner.createFile(f, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Frameworks the ParparVM translator should weak-link so the iOS slice
     * still links normally while the watch slice tolerates absent symbols.
     */
    String parparvmOptionalFrameworksArg() {
        return "-Doptional.frameworks=" + WATCH_OPTIONAL_FRAMEWORKS;
    }

    /**
     * Write the watch app's Info.plist into {@code appSrcDir}. For the modern
     * single-target watch app the key marker is {@code WKApplication=true}; the
     * companion distribution additionally pins {@code
     * WKCompanionAppBundleIdentifier} to the iOS app so the pair installs
     * together.
     */
    /**
     * The marketing version the containing app will carry, reproducing {@code IPhoneBuilder}'s own
     * derivation: the project version, reformatted to two decimal places when
     * {@code ios.twoDigitVersion} asks for it. The watch app has to agree with the phone digit for
     * digit, so this cannot simply read {@code request.getVersion()}.
     *
     * @param request the build request
     * @return the version string, never null
     */

    /**
     * The value {@code ios.plistInject} gives a key, or null when it does not set one.
     *
     * <p>Deliberately literal: the hint is a raw plist fragment, so this looks for
     * {@code <key>NAME</key>} and takes the next {@code <string>} that follows it. Anything more
     * clever would be pretending to parse a document that is only ever a fragment.</p>
     */
    /// Every {@code <key>} the {@code ios.plistInject} fragment sets.
    ///
    /// Deliberately literal, like {@link #injectedPlistString}: the hint is a fragment, not a
    /// document, so this scans for the tags rather than pretending to parse XML.
    static java.util.List<String> injectedPlistKeys(BuildRequest request) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        String inject = request.getArg("ios.plistInject", null);
        if (inject == null) {
            return out;
        }
        int at = 0;
        while (true) {
            int open = inject.indexOf("<key>", at);
            if (open < 0) {
                return out;
            }
            int close = inject.indexOf("</key>", open);
            if (close < 0) {
                return out;
            }
            String key = inject.substring(open + "<key>".length(), close).trim();
            if (key.length() > 0 && !out.contains(key)) {
                out.add(key);
            }
            at = close + "</key>".length();
        }
    }

    static String injectedPlistString(BuildRequest request, String key) {
        String inject = request.getArg("ios.plistInject", null);
        if (inject == null) {
            return null;
        }
        int at = inject.indexOf("<key>" + key + "</key>");
        if (at < 0) {
            return null;
        }
        int open = inject.indexOf("<string>", at);
        if (open < 0) {
            return null;
        }
        int close = closeOfString(inject, open + "<string>".length());
        if (close < 0) {
            return null;
        }
        return plistStringContent(inject.substring(open + "<string>".length(), close));
    }

    /// The {@code </string>} that closes the element, skipping over CDATA sections.
    ///
    /// A plain {@code indexOf} finds the first literal occurrence, and inside
    /// {@code <![CDATA[a</string>b]]>} that occurrence is DATA -- the element would be cut in half
    /// at a point the XML parser reading the phone's plist never stops at.
    private static int closeOfString(String inject, int from) {
        int i = from;
        while (i <= inject.length()) {
            int close = inject.indexOf("</string>", i);
            if (close < 0) {
                return -1;
            }
            int cdata = inject.indexOf(CDATA_OPEN, i);
            int comment = inject.indexOf(COMMENT_OPEN, i);
            // Whichever construct starts first, if either starts before the candidate end tag.
            // A </string> written inside a comment is no more an end tag than one inside CDATA.
            boolean cdataFirst = cdata >= 0 && (comment < 0 || cdata < comment);
            int skipFrom = cdataFirst ? cdata : comment;
            if (skipFrom < 0 || skipFrom > close) {
                return close;
            }
            String opener = cdataFirst ? CDATA_OPEN : COMMENT_OPEN;
            String closer = cdataFirst ? CDATA_CLOSE : COMMENT_CLOSE;
            int end = inject.indexOf(closer, skipFrom + opener.length());
            if (end < 0) {
                // Unterminated. Nothing after it can be located reliably, so the key is treated as
                // absent rather than guessed at.
                return -1;
            }
            i = end + closer.length();
        }
        return -1;
    }

    private static final String COMMENT_OPEN = "<!--";

    private static final String COMMENT_CLOSE = "-->";

    /// Removes XML comments from text outside any CDATA section.
    ///
    /// A comment is markup, not content: the parser reading the phone's plist drops it, so keeping
    /// it here escaped "<!-- note -->" into the watch's visible string. Inside CDATA the same
    /// characters are data and are left exactly as written.
    private static String stripComments(String value) {
        if (value == null || value.indexOf(COMMENT_OPEN) < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            int open = value.indexOf(COMMENT_OPEN, i);
            if (open < 0) {
                out.append(value.substring(i));
                break;
            }
            out.append(value, i, open);
            int close = value.indexOf(COMMENT_CLOSE, open + COMMENT_OPEN.length());
            if (close < 0) {
                // Unterminated: everything after it is inside the comment, so nothing more is
                // content.
                break;
            }
            i = close + COMMENT_CLOSE.length();
        }
        return out.toString();
    }

    private static final String CDATA_OPEN = "<![CDATA[";

    private static final String CDATA_CLOSE = "]]>";

    /// The text a plist {@code <string>} actually carries.
    ///
    /// CDATA is not an entity, so the entity decoder leaves it exactly as written and the escaper
    /// downstream emits {@code &lt;![CDATA[1.2]]&gt;} as the value. An XML parser reading the
    /// phone's plist resolves the same markup to {@code 1.2}, so the watch ended up with a
    /// different version string -- or with the markup itself shown in a permission prompt.
    ///
    /// Inside a CDATA section the content is taken verbatim, because that is what CDATA means: an
    /// {@code &amp;} written there is an ampersand, not the start of a reference. Outside one the
    /// entity decoding applies as before. The assembled value is trimmed, matching what this did
    /// before CDATA was understood at all.
    static String plistStringContent(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.indexOf(CDATA_OPEN) < 0) {
            return decodeXmlEntities(stripComments(raw).trim());
        }
        StringBuilder out = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            int cdata = raw.indexOf(CDATA_OPEN, i);
            if (cdata < 0) {
                out.append(decodeXmlEntities(stripComments(raw.substring(i))));
                break;
            }
            out.append(decodeXmlEntities(stripComments(raw.substring(i, cdata))));
            int body = cdata + CDATA_OPEN.length();
            int end = raw.indexOf(CDATA_CLOSE, body);
            if (end < 0) {
                // Unterminated: take the remainder as data rather than emitting the marker as text.
                out.append(raw.substring(body));
                break;
            }
            out.append(raw, body, end);
            i = end + CDATA_CLOSE.length();
        }
        return out.toString().trim();
    }

    /// Turns the five predefined XML entities back into their characters.
    ///
    /// The value read out of {@code ios.plistInject} is SERIALIZED text: a disclosure written as
    /// "Health &amp;amp; Fitness" arrives with the entity intact, and re-emitting it through
    /// {@code plistString} escapes the ampersand again -- so the phone shows the intended text
    /// while the watch permission dialog shows "&amp;amp;" literally. Decoding here puts the value
    /// back into the plain form the escaper expects.
    ///
    /// {@code &amp;amp;} is decoded LAST. Doing it first would turn "&amp;amp;lt;" into "&lt;"
    /// rather than the literal "&amp;lt;" the author wrote.
    static String decodeXmlEntities(String value) {
        if (value == null || value.indexOf('&') < 0) {
            return value;
        }
        // ONE left-to-right pass, not a sequence of replaces. Chained replacements decode their own
        // output: turning "&amp;" into "&" first makes "&amp;#38;" -- an author writing a literal
        // "&#38;" -- come out as "&", and no ordering of replaces fixes that in general. Scanning
        // once consumes each reference exactly as written.
        StringBuilder out = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int end = value.indexOf(';', i + 1);
            // A bare ampersand is not a reference; leave it exactly as the author wrote it.
            if (end < 0 || end - i > 12) {
                out.append(c);
                i++;
                continue;
            }
            String body = value.substring(i + 1, end);
            String decoded = decodeReference(body);
            if (decoded == null) {
                out.append(c);
                i++;
                continue;
            }
            out.append(decoded);
            i = end + 1;
        }
        return out.toString();
    }

    /// The character a reference body stands for, or null when it is not one this decoder knows.
    ///
    /// Numeric forms are included because they are ordinary XML: a purpose string written with
    /// {@code &#38;} is as valid as one written with {@code &amp;}, and leaving it encoded put the
    /// literal text in front of the user in the permission dialog.
    private static String decodeReference(String body) {
        if ("lt".equals(body)) {
            return "<";
        }
        if ("gt".equals(body)) {
            return ">";
        }
        if ("quot".equals(body)) {
            return "\"";
        }
        if ("apos".equals(body)) {
            return "'";
        }
        if ("amp".equals(body)) {
            return "&";
        }
        if (body.length() < 2 || body.charAt(0) != '#') {
            return null;
        }
        try {
            int code = body.charAt(1) == 'x' || body.charAt(1) == 'X'
                    ? Integer.parseInt(body.substring(2), 16)
                    : Integer.parseInt(body.substring(1));
            if (code <= 0 || code > 0x10FFFF) {
                return null;
            }
            return new String(Character.toChars(code));
        } catch (RuntimeException notANumber) {
            return null;
        }
    }

    static String shortVersion(BuildRequest request) {
        String version = request.getVersion();
        if (version == null || version.length() == 0) {
            return "1.0";
        }
        if (!"true".equals(request.getArg("ios.twoDigitVersion", "false"))) {
            return version;
        }
        try {
            int intVersion = Math.round(100 * Float.parseFloat(version));
            int lsb = intVersion % 100;
            String out = "" + (intVersion / 100) + ".";
            if (lsb == 0) {
                return out + "00";
            }
            return out + (lsb < 10 ? "0" + lsb : "" + lsb);
        } catch (NumberFormatException notANumber) {
            // The phone builder swallows this too and keeps the raw string.
            return version;
        }
    }

    void writeWatchInfoPlist(BuildRequest request, File appSrcDir) throws IOException {
        appSrcDir.mkdirs();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        plistString(sb, "CFBundleDisplayName", displayName);
        plistString(sb, "CFBundleExecutable", "$(EXECUTABLE_NAME)");
        plistString(sb, "CFBundleIdentifier", "$(PRODUCT_BUNDLE_IDENTIFIER)");
        plistString(sb, "CFBundleName", "$(PRODUCT_NAME)");
        plistString(sb, "CFBundlePackageType", "$(PRODUCT_BUNDLE_PACKAGE_TYPE)");
        // Apple's validation compares the embedded watch app's versions against the containing app's
        // and rejects the archive when they differ, so both keys are derived exactly the way the
        // phone derives them -- including the ios.twoDigitVersion reformatting and the
        // ios.bundleVersion override -- rather than being pinned to a constant.
        // ios.plistInject wins where it sets either key. It REPLACES the phone's default version
        // injection rather than adding to it, so a project that overrides the version there ships a
        // phone app whose version is not shortVersion(request) at all -- and an embedded watch app
        // whose versions differ from its container is rejected by App Store validation, which is
        // the one failure that only shows up at submission.
        String injectedShort = injectedPlistString(request, "CFBundleShortVersionString");
        String watchShort = injectedShort != null ? injectedShort : shortVersion(request);
        String injectedBundle = injectedPlistString(request, "CFBundleVersion");
        plistString(sb, "CFBundleShortVersionString", watchShort);
        // The fallback stays shortVersion(request), NOT watchShort. The two keys are independent:
        // the phone's CFBundleVersion is ios.bundleVersion defaulting to the build version, and it
        // does not follow an injected marketing version -- so deriving the watch's from the
        // injected short string produced the very mismatch this code exists to prevent (project
        // 1.0 with an injected 9.9 gave phone 1.0 against watch 9.9).
        plistString(sb, "CFBundleVersion", injectedBundle != null ? injectedBundle
                : request.getArg("ios.bundleVersion", shortVersion(request)));
        // Modern single-target watch app marker.
        sb.append("    <key>WKApplication</key>\n    <true/>\n");
        if (isStandalone()) {
            // A standalone bundle must SAY it is watch-only, not merely omit the companion key.
            // Without WKWatchOnly the bundle is neither tied to a containing iOS app nor declared
            // independent, which installs unpredictably and can fail App Store validation.
            sb.append("    <key>WKWatchOnly</key>\n    <true/>\n");
        } else {
            plistString(sb, "WKCompanionAppBundleIdentifier", request.getPackageName());
        }
        // HealthKit privacy strings. The watch slice has its own Info.plist and
        // previously emitted none, so a health-enabled watch app would fail at
        // runtime on the richest HealthKit target of all -- the watch is where
        // heart rate and workouts actually come from.
        // Trimmed, and empty counts as absent -- the phone builder already
        // works this way. A whitespace-only hint used to emit a blank
        // purpose string that satisfied the check below, producing an
        // entitled watch bundle whose disclosure said nothing.
        // EVERY privacy description the project declares, not only the HealthKit pair. A watch app
        // that uses location, the microphone or motion needs its own purpose string in ITS bundle:
        // the phone's plist does not cover it, so authorization fails or watchOS terminates the app
        // when the API is exercised, on a project that configured the hint correctly. Collected the
        // same way the phone builder collects them, from every ios.NS*UsageDescription argument.
        // ONE map, from both sources, resolved before anything is written. An explicit argument
        // wins over the same key in ios.plistInject, so nothing is emitted twice, and the checks
        // further down read the same map rather than re-deriving a narrower view of it -- which is
        // how the HealthKit validation came to reject a purpose string the plist already carried.
        java.util.Map<String, String> purposeStrings = new java.util.LinkedHashMap<String, String>();
        for (String injectedKey : injectedPlistKeys(request)) {
            if (injectedKey.startsWith("NS") && injectedKey.endsWith("UsageDescription")) {
                String description = injectedPlistString(request, injectedKey);
                if (description != null && description.length() > 0
                        && !isPurposeStringOptOut(description)) {
                    purposeStrings.put(injectedKey, description);
                }
            }
        }
        for (String arg : request.getArgs()) {
            if (arg.startsWith("ios.NS") && arg.endsWith("UsageDescription")) {
                String description = trimToNull(request.getArg(arg, null));
                if (description != null && !isPurposeStringOptOut(description)) {
                    purposeStrings.put(arg.substring(arg.lastIndexOf('.') + 1), description);
                }
            }
        }
        // The location FALLBACK too. ios.locationUsageDescription is a supported hint -- the phone
        // builder even supplies one itself when it detects location use -- and the phone plist
        // turns it into NSLocationWhenInUseUsageDescription later. The watch plist is written
        // before that translation happens, so a loop over ios.NS* alone leaves the watch bundle
        // with no purpose string while the project is configured correctly.
        String locationFallback = trimToNull(request.getArg("ios.locationUsageDescription", null));
        if (locationFallback != null
                && !purposeStrings.containsKey("NSLocationWhenInUseUsageDescription")) {
            // Only when nothing else supplied that key -- from an argument OR from the injected
            // fragment. Checking arguments alone let the fallback be emitted a second time under a
            // key the injection had already set, which both duplicates the entry and lets a default
            // overwrite the developer's own disclosure.
            purposeStrings.put("NSLocationWhenInUseUsageDescription", locationFallback);
        }
        for (java.util.Map.Entry<String, String> purpose : purposeStrings.entrySet()) {
            plistString(sb, purpose.getKey(), purpose.getValue());
        }
        // Read again, not re-emitted: the HealthKit pair is already in the plist from the loop
        // above, but the entitlement checks below need to know whether they were declared.
        // From the resolved map, so a purpose string supplied through ios.plistInject counts. Read
        // from arguments alone, this validation aborted the build over a key the plist it had just
        // written did contain.
        String healthShare = purposeStrings.get("NSHealthShareUsageDescription");
        String healthUpdate = purposeStrings.get("NSHealthUpdateUsageDescription");
        // HKWorkoutSession keeps the app running while a workout records;
        // without this background mode watchOS suspends it mid-run.
        if ("true".equalsIgnoreCase(workoutProcessingHint)) {
            sb.append("    <key>WKBackgroundModes</key>\n    <array>\n")
              .append("        <string>workout-processing</string>\n")
              .append("    </array>\n");
        }
        sb.append("</dict>\n</plist>\n");
        // A workout session is HealthKit, so asking for one while opting
        // out of HealthKit cannot be honoured either way round: the plist
        // still declares the workout-processing background mode while the
        // bundle goes unentitled, and the session fails at runtime.
        if ("false".equalsIgnoreCase(healthHint)
                && "true".equalsIgnoreCase(workoutProcessingHint)) {
            owner.error("watchNative.health=false contradicts"
                    + " watchNative.health.workoutProcessing=true. A"
                    + " workout session is HealthKit, so the watch cannot"
                    + " record one without the entitlement. Drop one of"
                    + " the two hints.",
                    new RuntimeException("contradictory watch health hints"));
        }
        // The detected usage, not the purpose strings. A string can outlive the code that needed
        // it, and treating it as evidence entitled the watch bundle for an app that no longer
        // touches HealthKit -- which then failed codesigning against an App ID without the
        // capability, with nothing in the output to say why. Same rule, same accessor, as the
        // BuildDaemon mirror: a cloud build and a local build must reach the same verdict.
        boolean watchHealth = watchUsesHealth(owner.phoneUsesHealthData(request),
                owner.watchRootUsesHealthData(request));
        boolean workoutProcessing =
                "true".equalsIgnoreCase(workoutProcessingHint);
        if (needsPurposeString(watchHealth, healthShare, healthUpdate,
                workoutProcessing)) {
            // Entitled but with no purpose string in its own Info.plist,
            // which builds cleanly and then fails the moment the watch asks
            // for authorization. Apple requires a specific string and this
            // build never invents one, so the developer has to supply it.
            owner.error("This app enables HealthKit on the watch"
                    + " (watchNative.health), but declares neither"
                    + " ios.NSHealthShareUsageDescription nor"
                    + " ios.NSHealthUpdateUsageDescription. The watch has"
                    + " its own Info.plist, and watchOS refuses a HealthKit"
                    + " authorization request from a bundle with no purpose"
                    + " string. Set the one that matches what the watch"
                    + " does"
                    + (workoutProcessing
                        ? " -- a workout saves its session, so that is"
                            + " ios.NSHealthUpdateUsageDescription."
                        : "."),
                    new RuntimeException("watch health usage string unset"));
        }
        writeWatchEntitlements(request, appSrcDir, watchHealth);
        File plist = new File(appSrcDir, request.getMainClass() + "-Watch-Info.plist");
        owner.createFile(plist, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** The value with surrounding space removed, or null when empty. */
    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    /**
     * Whether the build has to stop for a missing watch purpose string.
     *
     * <p>An entitled bundle with no purpose string in its own Info.plist
     * builds cleanly and then fails the moment it asks for authorization.
     * Only reachable through {@code watchNative.health}: every other route
     * to an entitled watch runs through the phone's strings, which are
     * copied into the watch plist.</p>
     */
    static boolean needsPurposeString(boolean watchUsesHealth,
            String healthShare, String healthUpdate,
            boolean workoutProcessing) {
        if (!watchUsesHealth) {
            return false;
        }
        if (workoutProcessing) {
            // A workout writes: it saves the session and the child
            // samples the app fed it, and reads nothing -- the rollup is
            // computed from what it was given. So the update string is
            // the one that has to be there, and a watch declaring only
            // the share string passed this check and was refused the
            // moment it asked to save the workout.
            return healthUpdate == null;
        }
        // Direction unknown: watchNative.health says the bundle uses
        // HealthKit and nothing more, so either string is evidence that
        // somebody thought about what it does.
        return healthShare == null && healthUpdate == null;
    }

    /**
     * Whether the watch bundle itself uses HealthKit.
     *
     * <p>The phone's privacy strings are evidence about the phone. When
     * the watch shares the phone's main class it runs the same code, so
     * they are evidence about the watch too. When the watch has its own
     * {@code watchMain} it shakes from its own root and the phone's usage
     * says nothing -- entitling that bundle anyway made codesigning fail
     * for an ordinary non-health watch app whose App ID has no HealthKit
     * capability, with nothing in the output to explain it.</p>
     *
     * <p>{@code watchNative.health} settles it either way, and
     * {@code watchNative.health.workoutProcessing} implies it: a workout
     * session is HealthKit.</p>
     */
    boolean watchUsesHealth(boolean phoneUsesHealth, boolean watchRootUsesHealth) {
        if ("true".equalsIgnoreCase(healthHint)) {
            return true;
        }
        if ("false".equalsIgnoreCase(healthHint)) {
            return false;
        }
        if ("true".equalsIgnoreCase(workoutProcessingHint)) {
            return true;
        }
        // Inherited when the two lifecycles are the same code. When they are not, the answer is
        // whether the WATCH root reaches the health API -- which is the question the phone's flat
        // scan cannot answer. Refusing outright, as this used to, left a watch app whose own code
        // calls HealthKit unentitled and its authorization request refused, unless the project
        // happened to set watchNative.health.
        if (!distinctWatchMain) {
            return phoneUsesHealth;
        }
        return watchRootUsesHealth;
    }

    /**
     * Writes the watch target's own entitlements file when the watch uses
     * HealthKit.
     *
     * <p>The watch app is signed independently of the phone app, so the
     * phone's entitlement does not reach it. Emitting the privacy strings
     * alone produces a watch build that asks for HealthKit authorization
     * and is refused, with nothing in the build output to explain why.</p>
     */
    private void writeWatchEntitlements(BuildRequest request, File appSrcDir,
            boolean usesHealth) throws IOException {
        if (!usesHealth) {
            return;
        }
        owner.createFile(new File(appSrcDir,
                request.getMainClass() + "-Watch.entitlements"),
                watchEntitlementsPlist(request, workoutProcessingHint)
                        .getBytes(StandardCharsets.UTF_8));
    }

    /// Whether a HealthKit capability is asked for, in either spelling.
    ///
    /// The short hint promotes into the `ios.entitlements.*` namespace for
    /// the phone, and a project can equally set the canonical key itself
    /// -- the phone honours both. Reading only the short one signed the
    /// watch without a capability the build had granted, and the watch is
    /// the target where the workout code that needs it actually runs.
    ///
    /// Order-independent by construction, which matters because the
    /// promotion and this file are written by different passes.
    private static boolean healthCapability(BuildRequest request,
            String shortHint, String canonicalKey) {
        return "true".equalsIgnoreCase(request.getArg(shortHint, "false"))
                || "true".equalsIgnoreCase(request.getArg(
                        "ios.entitlements.com.apple.developer.healthkit."
                                + canonicalKey, "false"));
    }

    /// The watch target's entitlements, as the plist text that is signed.
    ///
    /// Separated from writing it so the capability set can be asserted
    /// without a build: every key here is one the watch does not inherit
    /// from the phone, and a missing one fails at runtime rather than at
    /// build time.
    static String watchEntitlementsPlist(BuildRequest request,
            String workoutProcessingHint) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" ")
          .append("\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n")
          .append("<plist version=\"1.0\">\n<dict>\n")
          .append("    <key>com.apple.developer.healthkit</key>\n")
          .append("    <true/>\n");
        // Background delivery only, not workout processing. A workout
        // keeps running through WKBackgroundModes=workout-processing in
        // the watch Info.plist; this entitlement covers HealthKit
        // *delivering updates* while the app is suspended, which a
        // workout app need not ask for. Granting it anyway put a
        // capability in the signature that a provisioning profile
        // carrying only base HealthKit does not have, and entitlement
        // validation refuses the build for something nothing uses.
        if (healthCapability(request, "ios.health.backgroundDelivery",
                "background-delivery")) {
            sb.append("    <key>com.apple.developer.healthkit")
              .append(".background-delivery</key>\n    <true/>\n");
        }
        // Recalibration too, for the same reason background delivery is
        // here: the watch is signed with this file and nothing else, so a
        // capability the phone was granted does not reach it. The estimate
        // recalibration a workout performs runs in shared code, on the
        // target where it is most likely to run at all.
        if (healthCapability(request, "ios.health.recalibrateEstimates",
                "recalibrate-estimates")) {
            sb.append("    <key>com.apple.developer.healthkit")
              .append(".recalibrate-estimates</key>\n    <true/>\n");
        }
        sb.append("</dict>\n</plist>\n");
        return sb.toString();
    }

    /**
     * The CODE_SIGN_ENTITLEMENTS setting for the watch target, or an empty
     * string when the watch does not use HealthKit.
     */
    String watchEntitlementsSetting(BuildRequest request,
            String mainClass) {
        // The same gate the entitlements file itself uses. Pointing the
        // target at a file that is not written, or writing one the target
        // never signs with, are two different ways to be wrong.
        // The SAME source of truth writeWatchInfoPlist uses -- detected usage, not the privacy
        // strings. Resolving it twice from different inputs is how these two came apart: reading
        // ios.NSHealth* here while the plist pass read the merged purpose strings meant a
        // description supplied through ios.plistInject produced a bundle that declared HealthKit
        // and was signed without the entitlement, so authorization failed on device.
        boolean phoneUsesHealth = owner.phoneUsesHealthData(request);
        if (!watchUsesHealth(phoneUsesHealth, owner.watchRootUsesHealthData(request))) {
            return "";
        }
        return "  bs['CODE_SIGN_ENTITLEMENTS'] = '"
                + IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + mainClass
                        + "-Watch.entitlements") + "'\n";
    }

    /// The established opt-out for a privacy hint: the phone's generic injector skips a usage
    /// description whose value is exactly {@code false}, so a project suppresses a key the builder
    /// would otherwise supply by setting it to that. Carried over verbatim, because the watch
    /// treating it as an ordinary description put the literal word "false" in front of the user in
    /// a watchOS permission prompt -- and in the HealthKit case that string is also what the
    /// entitlement validation reads.
    private static boolean isPurposeStringOptOut(String description) {
        return "false".equals(description);
    }

    private static void plistString(StringBuilder sb, String key, String value) {
        sb.append("    <key>").append(key).append("</key>\n    <string>")
                .append(escapeXml(value)).append("</string>\n");
    }

    /**
     * Escapes a value for XML content.
     *
     * <p>These strings come from build hints, so they are whatever the
     * developer wrote. A perfectly reasonable purpose string such as
     * "Reads &amp; analyzes workouts" produced a malformed plist and an
     * Xcode failure with no obvious cause.</p>
     */
    private static String escapeXml(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Add and configure the watchOS app target in the generated Xcode project
     * via the Ruby {@code xcodeproj} gem. Creates the target, compiles the
     * shared sources (minus {@link #EXCLUDED_WATCH_SOURCES}) for {@code
     * arm64_32}, points it at the watch Info.plist, and - for the companion
     * distribution - embeds it in the iOS app.
     */
    void applyXcodeSettings(BuildRequest request, File tmpFile, String buildVersion,
            List<String> watchSources)
            throws BuildException {
        File hooksDir = new File(tmpFile, "hooks");
        hooksDir.mkdir();
        File scriptFile = new File(hooksDir, "apply_watch_native_settings.rb");
        String script = buildXcodeScript(request, tmpFile, buildVersion, watchSources);
        String watchTargetName = translationRoot(request.getMainClass());
        try {
            owner.createFile(scriptFile, script.getBytes(StandardCharsets.UTF_8));
            owner.exec(hooksDir, "chmod", "0755", scriptFile.getAbsolutePath());
            if (!owner.exec(hooksDir, scriptFile.getAbsolutePath())) {
                throw new BuildException("Failed to apply watchNative Xcode settings via xcodeproj");
            }
            owner.log("[watchNative] Added watchOS target " + watchTargetName
                    + " (" + (isStandalone() ? "standalone" : "companion") + ", "
                    + (watchSources.isEmpty()
                            ? "sharing the phone translation"
                            : watchSources.size() + " own translated sources") + ")");
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to apply watchNative Xcode settings", ex);
        }
    }

    /// The xcodeproj script, separated from running it so the decisions in it can be tested.
    ///
    /// Running it needs a real generated project and the xcodeproj gem; the CHOICES -- which
    /// sources the watch target compiles, whether the phone stub's main is defined away -- are
    /// what the tests need to pin, and they are all here.
    String buildXcodeScript(BuildRequest request, File tmpFile, String buildVersion,
            List<String> watchSources) {
        String mainClass = request.getMainClass();
        String watchTargetName = mainClass + "Watch";
        String projectFile = new File(tmpFile, "dist/" + mainClass + ".xcodeproj").getAbsolutePath();
        String infoPlistPath = mainClass + "-src/" + mainClass + "-Watch-Info.plist";
        String resolvedTeamId = owner.sanitizeTeamId(teamId, "ios.teamId");

        StringBuilder excluded = new StringBuilder();
        for (String f : EXCLUDED_WATCH_SOURCES) {
            if (excluded.length() > 0) {
                excluded.append(' ');
            }
            excluded.append(f);
        }
        // The generated phone Stub is NOT excluded from the watch target -- it
        // carries the app's translated classes and the Stub.main the watch
        // bootstrap invokes. Its duplicate C `int main()` is neutralised with a
        // per-file -Dmain rename below (see "stub_name"), not by exclusion.

        StringBuilder s = new StringBuilder();
        s.append("#!/usr/bin/env ruby\n")
                .append("require 'xcodeproj'\n")
                .append("project_file = '").append(IPhoneBuilder.escapeRubyStr(projectFile)).append("'\n")
                .append("xcproj = Xcodeproj::Project.open(project_file)\n")
                .append("app_target = xcproj.targets.find { |t| t.name == '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("' }\n")
                .append("abort('Unable to find app target ").append(IPhoneBuilder.escapeRubyStr(mainClass))
                .append("') unless app_target\n")
                // Don't double-create on a re-run.
                .append("watch_name = '").append(IPhoneBuilder.escapeRubyStr(watchTargetName)).append("'\n")
                .append("watch_target = xcproj.targets.find { |t| t.name == watch_name }\n")
                .append("if watch_target.nil?\n")
                .append("  watch_target = xcproj.new_target(:application, watch_name, :watchos, '")
                .append(IPhoneBuilder.escapeRubyStr(MIN_DEPLOYMENT_TARGET)).append("')\n")
                .append("end\n")
                // Compile the shared ParparVM sources for the watch, minus the
                // GL/Metal-only files. Reuse the app target's compile sources so
                // we track exactly what was generated.
                .append("excluded = %w[").append(excluded).append("]\n");
        if (!watchSources.isEmpty()) {
            // The watch compiles its OWN translation, rooted at watchMain and shaken down to what
            // that entry point reaches. Nothing of the phone's tree is added: sharing it is what
            // made the watch binary carry the phone's whole graph, and the phone Stub's main then
            // had to be defined away to stop the two entry points colliding.
            StringBuilder names = new StringBuilder();
            for (String name : watchSources) {
                if (names.length() > 0) {
                    names.append(' ');
                }
                names.append(name);
            }
            // The app target's file SET, with the watch translation's CONTENTS.
            //
            // Adding every .m the watch translation emitted was wrong: its dist carries native
            // sources the app target never compiles -- UIWebViewEventDelegate.m among them, which
            // calls UIApplication and does not exist on watchOS. The shared-translation path has
            // always taken its file list from the app target, and that list is what belongs on the
            // watch too; only the translated bodies differ. So walk the app target exactly as the
            // shared path does, and swap each file for its watch-src counterpart where one exists.
            s.append("watch_sources = %w[").append(names).append("]\n")
                    // Relative to the PROJECT, not to the app's -src folder. Naming only
                    // "watch-src" pointed every reference at a directory that does not exist --
                    // and because most translated files share a basename with the phone's, Xcode
                    // resolved them against the phone tree instead and quietly compiled those.
                    // Only the handful of watch-only names failed outright, which is how a broken
                    // path looked like 16 missing files rather than the wrong sources entirely.
                    .append("watch_group_path = '")
                    .append(IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + WATCH_SRC_DIR))
                    .append("'\n")
                    // EVERY file the watch translation emitted, and only those.
                    //
                    // Not the app target's list: the phone's translation shakes out whatever the
                    // phone never reaches, and that includes the watch lifecycle class itself --
                    // taking the phone's set left the watch binary without its own entry class and
                    // the link failed on ___NEW_..Watch. The watch tree IS the watch program.
                    //
                    // Files that must not build for watchOS are excluded in the SOURCE, by
                    // TARGET_OS_WATCH guards in the port, rather than by a list here -- so a
                    // translated native like UIWebViewEventDelegate.m compiles to nothing on this
                    // target instead of having to be enumerated.
                    .append("watch_existing = watch_target.source_build_phase.files.to_a.map { |bf| bf.file_ref && bf.file_ref.path ? File.basename(bf.file_ref.path) : nil }\n")
                    // Per-file COMPILER_FLAGS, carried across by BASENAME from the app target.
                    //
                    // A cn1lib Objective-C source can require -fobjc-arc while the port itself
                    // builds with ARC off, and this target forces CLANG_ENABLE_OBJC_ARC=NO. The
                    // shared-translation branch copies bf.settings for exactly that reason; adding
                    // fresh references here dropped them, so such a source would compile under the
                    // wrong memory model. There is no bf to copy from when the list comes from the
                    // watch tree, so the app target's flags are indexed by file name first.
                    .append("app_flags = {}\n")
                    .append("app_target.source_build_phase.files.to_a.each do |bf|\n")
                    .append("  next unless bf.file_ref && bf.file_ref.path && bf.settings\n")
                    .append("  app_flags[File.basename(bf.file_ref.path)] = bf.settings\n")
                    .append("end\n")
                    .append("watch_sources.each do |name|\n")
                    .append("  next if excluded.include?(name)\n")
                    .append("  next if watch_existing.include?(name)\n")
                    .append("  ref = xcproj.main_group.new_reference(watch_group_path + '/' + name)\n")
                    .append("  added = watch_target.source_build_phase.add_file_reference(ref)\n")
                    .append("  added.settings = app_flags[name].dup if added && app_flags[name]\n")
                    .append("end\n");
        } else {
            // Same entry point on both slices, so there is one translation and the watch shares it.
            // The phone Stub's main is neutralised below in exactly that case.
            s.append("app_target.source_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  base = File.basename(ref.path)\n")
                .append("  next if excluded.include?(base)\n")
                .append("  unless watch_target.source_build_phase.files_references.include?(ref)\n")
                .append("    added = watch_target.source_build_phase.add_file_reference(ref)\n")
                // Carry the per-file COMPILER_FLAGS across, not just the reference. A cn1lib source
                // that requires ARC is compiled with -fobjc-arc on the iOS target while the port
                // itself builds with ARC off; copying the reference alone dropped that flag and the
                // watch slice failed with "requires ARC (-fobjc-arc)".
                .append("    added.settings = bf.settings.dup if added && bf.settings\n")
                .append("  end\n")
                .append("end\n");
        }
        s
                // Add the generated watch entry point (SwiftUI @main shell +
                // bootstrap). These live in <mainClass>-src/ next to the
                // translated sources.
                .append("watch_src = '").append(IPhoneBuilder.escapeRubyStr(mainClass)).append("-src'\n")
                .append("entry_existing = watch_target.source_build_phase.files.to_a.map { |bf| bf.file_ref && bf.file_ref.path ? File.basename(bf.file_ref.path) : nil }\n")
                .append("%w[CN1WatchApp.swift CN1WatchBootstrap.m].each do |name|\n")
                .append("  next if entry_existing.include?(name)\n")
                .append("  ref = xcproj.main_group.new_reference(watch_src + '/' + name)\n")
                .append("  watch_target.source_build_phase.add_file_reference(ref)\n")
                .append("end\n")
                // Build settings for the watch slice.
                .append("watch_target.build_configurations.each do |config|\n")
                .append("  bs = config.build_settings\n")
                .append("  bs['SDKROOT'] = 'watchos'\n");
        s                // arm64_32 is the watchOS *device* ABI; the watch *simulator*
                // on Apple Silicon is arm64 (and x86_64 on Intel). Set the arch
                // per-SDK so the simulator build doesn't try arm64_32 (whose
                // Swift stdlib slice doesn't exist -> 'Unable to find module Swift').
                .append("  bs['ARCHS[sdk=watchos*]'] = 'arm64_32'\n")
                // The watch SIMULATOR arch is left to ARCHS_STANDARD plus ONLY_ACTIVE_ARCH, so it
                // follows the host: arm64 on Apple Silicon, x86_64 on Intel. This was pinned to
                // arm64 because IOSSimd.m included <arm_neon.h> unconditionally and an x86_64 slice
                // could not satisfy it -- that has since been guarded (#if defined(__ARM_NEON)), so
                // the pin now only serves to make the watch target unbuildable on an Intel host.
                .append("  bs['ARCHS[sdk=watchsimulator*]'] = '$(ARCHS_STANDARD)'\n")
                .append("  bs['ONLY_ACTIVE_ARCH'] = 'YES'\n")
                .append("  bs['WATCHOS_DEPLOYMENT_TARGET'] = '")
                .append(IPhoneBuilder.escapeRubyStr(MIN_DEPLOYMENT_TARGET)).append("'\n")
                .append("  bs['TARGETED_DEVICE_FAMILY'] = '4'\n")
                .append("  bs['PRODUCT_BUNDLE_IDENTIFIER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(bundleId)).append("'\n")
                .append("  bs['PRODUCT_NAME'] = '$(TARGET_NAME)'\n")
                .append("  bs['INFOPLIST_FILE'] = '")
                .append(IPhoneBuilder.escapeRubyStr(infoPlistPath)).append("'\n")
                .append(watchEntitlementsSetting(request, mainClass))
                .append("  bs['MARKETING_VERSION'] = '")
                .append(IPhoneBuilder.escapeRubyStr(request.getVersion() == null ? "1.0" : request.getVersion())).append("'\n")
                .append("  bs['CURRENT_PROJECT_VERSION'] = '")
                .append(IPhoneBuilder.escapeRubyStr(buildVersion == null ? "1" : buildVersion)).append("'\n")
                .append("  bs['CLANG_ENABLE_MODULES'] = 'YES'\n")
                // The prefix header of whichever translation this target compiles.
                //
                // A pch's quoted includes resolve against ITS OWN directory before any search
                // path, and both trees carry a cn1_class_method_index.h describing different
                // programs. Compiled with the phone's pch, the watch sources saw a class index
                // that never declares the watch stub -- "use of undeclared identifier
                // cn1_class_id_..WatchStub" from a file the watch translation had just emitted.
                //
                // Set HERE rather than earlier in this loop: this assignment is the last one to
                // run, so anything set above it is silently overwritten.
                .append("  bs['GCC_PREFIX_HEADER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(watchSources.isEmpty()
                        ? mainClass + "-src/" + mainClass + "-Prefix.pch"
                        : mainClass + "-src/" + WATCH_SRC_DIR + "/"
                                + watchPrefixHeader(mainClass)))
                .append("'\n")
                .append("  bs['EXCLUDED_SOURCE_FILE_NAMES'] = '").append(excluded).append("'\n")
                // The CN1 sources (and CN1WatchBootstrap.m) compile without ARC,
                // matching the iOS port; the Swift shell is ARC regardless.
                .append("  bs['CLANG_ENABLE_OBJC_ARC'] = 'NO'\n")
                .append("  bs['SWIFT_VERSION'] = '5.0'\n")
                .append("  bs['SWIFT_OBJC_BRIDGING_HEADER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + mainClass + "-Watch-Bridging-Header.h")).append("'\n")
                // Resolve <GLKit/..> and <OpenGLES/..> to the watchOS stub
                // headers (writeStubHeaders) only when Xcode is actually
                // compiling the watch target for a watch SDK. If an old or
                // implicit app dependency makes Xcode visit this target during
                // an iOS Simulator build, these stubs must not shadow Apple's
                // real OpenGLES headers.
                .append("  bs.delete('HEADER_SEARCH_PATHS')\n")
                // The staged watch tree comes FIRST when it exists, so a header shared by name
                // with the phone's resolves to the watch translation's copy. These are the
                // SDK-conditional keys, which is what the plain HEADER_SEARCH_PATHS deleted just
                // above is replaced by -- setting the plain key has no effect on this target.
                .append("  bs['HEADER_SEARCH_PATHS[sdk=watchos*]'] = '$(inherited) ")
                .append(watchSources.isEmpty() ? "" : "$(SRCROOT)/"
                        + IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + WATCH_SRC_DIR) + " ")
                .append("$(SRCROOT)/")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("-src/watchOSStubs'\n")
                .append("  bs['HEADER_SEARCH_PATHS[sdk=watchsimulator*]'] = '$(inherited) ")
                .append(watchSources.isEmpty() ? "" : "$(SRCROOT)/"
                        + IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + WATCH_SRC_DIR) + " ")
                .append("$(SRCROOT)/")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("-src/watchOSStubs'\n")
                // A standalone watch app IS the product, so it must be installable; an embedded
                // companion is carried inside the phone app and must not be.
                .append(standalone
                        ? "  bs['SKIP_INSTALL'] = 'NO'\n"
                        : "  bs['SKIP_INSTALL'] = 'YES'\n");
        if (resolvedTeamId != null && !resolvedTeamId.isEmpty()) {
            s.append("  bs['DEVELOPMENT_TEAM'] = '").append(resolvedTeamId).append("'\n");
        }
        s.append("end\n");

        // The generated phone Stub (translated <MainClass>) defines the C
        // `int main()` (the iOS entry). The watch app is SwiftUI-rooted
        // (CN1WatchApp.swift @main), so both would define `_main` -> duplicate
        // symbol. We keep the Stub compiled on the watch (it carries the app's
        // translated classes + the Stub.main the bootstrap calls) and instead
        // rename its C main away with a per-file -Dmain. -Wno-error=return-type
        // covers the original main()'s implicit fallthrough once renamed.
        // The generated phone Stub source is named from the FQN-mangled class
        // (com_<pkg>_<Main>Stub.m), not the simple name -- match that so the
        // per-file -Dmain rename actually lands (else duplicate _main vs @main).
        String stubFqn = (request.getPackageName() == null || request.getPackageName().isEmpty())
                ? mainClass : (request.getPackageName() + "." + mainClass);
        String phoneStubName = mangle(stubFqn) + "Stub";
        {
            // The stub's C main is defined away in BOTH modes, on whichever stub this target
            // compiles.
            //
            // The watch app is SwiftUI-rooted: CN1WatchApp.swift carries @main, and the generated
            // bootstrap calls the stub's Java main METHOD from there. The translator also emits a C
            // main for any class with a Java main, so leaving it produced "duplicate symbol _main"
            // against Swift's. I had assumed an own translation needed its main kept -- it does
            // not; what it needs is its Java entry reachable, which the bootstrap already does.
            String neutralised = watchSources.isEmpty()
                    ? phoneStubName
                    : mangle(request.getPackageName() == null
                            || request.getPackageName().isEmpty()
                            ? translationRoot(mainClass)
                            : request.getPackageName() + "." + translationRoot(mainClass)) + "Stub";
            s.append("stub_name = '").append(neutralised).append(".m'\n")
                    .append("watch_target.source_build_phase.files.to_a.each do |bf|\n")
                    .append("  ref = bf.file_ref\n")
                    .append("  next unless ref && ref.path && File.basename(ref.path) == stub_name\n")
                    .append("  bf.settings = { 'COMPILER_FLAGS' => '-Dmain=cn1_watch_phone_main_unused -Wno-error=return-type -Wno-return-type' }\n")
                    .append("end\n");
        }

        // watchOS frameworks auto-link via modules; remove GL/Metal framework
        // refs that the template added for iOS so the watch target doesn't try
        // to link them.
        s.append("gl = %w[OpenGLES.framework GLKit.framework Metal.framework]\n")
                .append("watch_target.frameworks_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  bf.remove_from_project if gl.include?(File.basename(ref.path))\n")
                .append("end\n");

        // WatchConnectivity is linked EXPLICITLY on the watch target when the app uses the wearable
        // API, rather than left to the module auto-link above.
        //
        // IPhoneBuilder appends the framework to addLibs, and addLibs is consumed while generating
        // the PHONE target -- the watch target is created here, afterwards, and copies sources and
        // resources but not that list. What currently saves the link is CLANG_ENABLE_MODULES plus
        // the '#import <WatchConnectivity/WatchConnectivity.h>' in CN1WatchConnectivity.h, which
        // makes clang emit an autolink directive. That works, but it means the watch slice links a
        // framework it never names: turn modules off, or reach WCSession through a header that does
        // not import the umbrella, and the target compiles and then fails at link with no
        // indication of which build setting withdrew the framework.
        if (owner.usesWearable()) {
            s.append("wc = xcproj.frameworks_group.files.find { |f| f.path && "
                            + "File.basename(f.path) == 'WatchConnectivity.framework' }\n")
                    .append("if wc.nil?\n")
                    .append("  wc = xcproj.frameworks_group.new_file("
                            + "'System/Library/Frameworks/WatchConnectivity.framework')\n")
                    .append("  wc.source_tree = 'SDKROOT'\n")
                    .append("end\n")
                    .append("unless watch_target.frameworks_build_phase.files_references.include?(wc)\n")
                    .append("  watch_target.frameworks_build_phase.add_file_reference(wc)\n")
                    .append("end\n");
        }

        // Everything else the PHONE target links, filtered by what the watch SDKs actually have.
        //
        // ios.add_libs and the dependency manager attach frameworks to the app target while it is
        // generated; the watch target is created here, afterwards, and inherited none of them. That
        // was invisible while the watch compiled the phone's translation, because it only ever
        // built the shared CN1 sources. With a translation of its own it also compiles the native
        // implementation of any NativeInterface the WATCH root reaches, and that implementation's
        // framework was not on this target -- so the slice failed to link with undefined symbols
        // and nothing in the build named the missing dependency.
        //
        // The filter is the SDK itself, not a hand-written list of iOS-only frameworks. A list has
        // to be maintained against every OS release and is wrong in both directions: omitting one
        // watchOS gained breaks a valid project, including one it never had breaks the build. Both
        // watch SDKs are consulted and a framework must be in each -- they are NOT the same set,
        // BackgroundTasks.framework ships in the device SDK and not the simulator one, and the
        // target is built for both destinations, so satisfying only one produces a project that
        // links on device and fails in the simulator with "framework not found".
        //
        // Weak-linked, as the optional-framework list treats anything a slice may not need: a
        // symbol the watch code never calls costs it nothing.
        // A VENDORED framework is judged by what it says about itself.
        //
        // The SDK check below can only speak for system frameworks. A developer's own or a
        // third-party binary is referenced from a group rather than SDKROOT, and skipping every one
        // of those meant a watch-reachable native implementation calling into a framework that ships
        // a perfectly good watchOS slice never got it linked -- undefined symbols, with nothing in
        // the build naming what was left out.
        //
        // The bundle's own Info.plist is the honest answer: CFBundleSupportedPlatforms for a plain
        // .framework, AvailableLibraries/SupportedPlatform for an .xcframework. Both are written by
        // whoever built the binary. Guessing from the architecture list would not work -- arm64 is
        // both an iPhone and an Apple silicon watch simulator -- and guessing wrong here links an
        // iOS-only binary into a watch slice, which fails later and less clearly.
        s.append("def cn1_watch_bundle_supports_watchos(ref)\n")
                .append("  path = (ref.real_path.to_s rescue nil)\n")
                .append("  return false unless path && File.exist?(path)\n")
                .append("  plist = ['Info.plist', 'Resources/Info.plist']"
                        + ".map { |rel| File.join(path, rel) }.find { |c| File.file?(c) }\n")
                .append("  return false unless plist\n")
                .append("  begin\n")
                .append("    info = Xcodeproj::Plist.read_from_path(plist)\n")
                .append("  rescue StandardError\n")
                // An unreadable plist is not evidence of support. Leaving it out costs a link
                // error naming the framework; guessing yes costs a slice built against the wrong
                // platform.
                .append("    return false\n")
                .append("  end\n")
                .append("  return false unless info.is_a?(Hash)\n")
                .append("  platforms = info['CFBundleSupportedPlatforms']\n")
                .append("  if platforms.is_a?(Array) && platforms.any? { |p| "
                        + "p.to_s.downcase.start_with?('watch') }\n")
                .append("    return true\n")
                .append("  end\n")
                .append("  libs = info['AvailableLibraries']\n")
                .append("  return false unless libs.is_a?(Array)\n")
                .append("  libs.any? { |l| l.is_a?(Hash) && "
                        + "l['SupportedPlatform'].to_s.downcase.start_with?('watch') }\n")
                .append("end\n")
                .append("vendored_linked = false\n")
                .append("watch_sdks = ['watchos', 'watchsimulator'].map { |sdk| "
                        + "`xcrun --sdk #{sdk} --show-sdk-path 2>/dev/null`.strip }"
                        + ".reject { |dir| dir.empty? || !File.directory?(dir) }\n")
                .append("watch_fw_dirs = watch_sdks.map { |dir| "
                        + "[File.join(dir, 'System/Library/Frameworks'), "
                        + "File.join(dir, 'System/Library/SubFrameworks')] }\n")
                .append("already = watch_target.frameworks_build_phase.files_references"
                        + ".map { |r| r.path && File.basename(r.path) }.compact\n")
                .append("app_target.frameworks_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  base = File.basename(ref.path)\n")
                .append("  next if already.include?(base)\n")
                .append("  next if gl.include?(base)\n")
                // Linkable inputs only. The app target's frameworks phase also carries entries that
                // are not libraries -- the Info.plist and the prefix header are both in there.
                //
                // .a is deliberately absent. A static library is built, not provided by the SDK:
                // CocoaPods' libPods-*.a and any vendored archive are compiled for iOS and have no
                // watch slice, so linking one is a guaranteed failure rather than a possible one.
                .append("  next unless base.end_with?('.framework') "
                        + "|| base.end_with?('.xcframework') || base.end_with?('.dylib') "
                        + "|| base.end_with?('.tbd')\n")
                .append("  if base.end_with?('.framework') || base.end_with?('.xcframework')\n")
                .append("    if ref.source_tree == 'SDKROOT'\n")
                .append("      present = !watch_fw_dirs.empty? && watch_fw_dirs.all? { |dirs| "
                        + "dirs.any? { |d| File.directory?(File.join(d, base)) } }\n")
                .append("    else\n")
                // Vendored: the bundle's own declaration decides, and a yes means the search paths
                // and any embed phase have to follow it below.
                .append("      present = cn1_watch_bundle_supports_watchos(ref)\n")
                .append("      vendored_linked ||= present\n")
                .append("    end\n")
                .append("  else\n")
                .append("    stem = base.sub(/\\.(dylib|tbd)\\z/, '')\n")
                .append("    present = !watch_sdks.empty? && watch_sdks.all? { |sdk| "
                        + "[File.join(sdk, 'usr/lib', base), File.join(sdk, 'usr/lib', stem + '.tbd')]"
                        + ".any? { |c| File.exist?(c) } }\n")
                .append("  end\n")
                .append("  unless present\n")
                .append("    puts \"[watchNative] not linking #{base} into the watch target: "
                        + "#{ref.source_tree == 'SDKROOT' ? 'absent from a watchOS SDK' : "
                        + "'its Info.plist declares no watchOS slice'}\"\n")
                .append("    next\n")
                .append("  end\n")
                .append("  added = watch_target.frameworks_build_phase.add_file_reference(ref)\n")
                .append("  if added\n")
                .append("    settings = (added.settings || {}).dup\n")
                .append("    attrs = (settings['ATTRIBUTES'] || []).dup\n")
                .append("    attrs << 'Weak' unless attrs.include?('Weak')\n")
                .append("    settings['ATTRIBUTES'] = attrs\n")
                .append("    added.settings = settings\n")
                .append("  end\n")
                .append("end\n");

        // What a vendored framework needs beyond being listed in the link phase.
        //
        // FRAMEWORK_SEARCH_PATHS is where the linker looks, and it lives on the APP target -- the
        // watch target has never been told about the directory the binary sits in, so linking the
        // reference alone still fails with "framework not found". Mirrored per configuration, so a
        // project whose Debug and Release paths differ keeps that difference.
        //
        // Then the embed phase, mirrored rather than decided: a static framework must NOT be
        // copied into the bundle and a dynamic one must, and whether this particular binary is one
        // or the other is already recorded in the project -- if the phone app embeds it, it is
        // dynamic. Reading that beats parsing Mach-O headers to rediscover it.
        //
        // Both are gated on having actually linked a vendored framework, so a project without one
        // produces exactly the Xcode project it did before.
        s.append("if vendored_linked\n")
                .append("  watch_target.build_configurations.each do |config|\n")
                .append("    app_cfg = app_target.build_configurations.find { |c| "
                        + "c.name == config.name } || app_target.build_configurations.first\n")
                .append("    next unless app_cfg\n")
                .append("    paths = app_cfg.build_settings['FRAMEWORK_SEARCH_PATHS']\n")
                .append("    next unless paths\n")
                .append("    config.build_settings['FRAMEWORK_SEARCH_PATHS'] = paths\n")
                .append("  end\n")
                .append("  embedded = app_target.copy_files_build_phases.to_a.select { |ph| "
                        + "ph.symbol_dst_subfolder_spec == :frameworks }"
                        + ".flat_map { |ph| ph.files.to_a }"
                        + ".map { |bf| bf.file_ref }.compact\n")
                .append("  watch_embed = nil\n")
                .append("  watch_linked = watch_target.frameworks_build_phase.files_references\n")
                .append("  embedded.each do |ref|\n")
                .append("    next unless watch_linked.include?(ref)\n")
                .append("    watch_embed ||= watch_target.copy_files_build_phases.to_a.find { |ph| "
                        + "ph.symbol_dst_subfolder_spec == :frameworks }\n")
                .append("    if watch_embed.nil?\n")
                .append("      watch_embed = watch_target.new_copy_files_build_phase("
                        + "'Embed Frameworks')\n")
                .append("      watch_embed.symbol_dst_subfolder_spec = :frameworks\n")
                .append("    end\n")
                .append("    next if watch_embed.files_references.include?(ref)\n")
                .append("    bf = watch_embed.add_file_reference(ref)\n")
                // Signed on copy, as Xcode does for an embedded framework: an unsigned binary
                // inside a signed watch app is rejected at install time.
                .append("    bf.settings = { 'ATTRIBUTES' => ['CodeSignOnCopy', "
                        + "'RemoveHeadersOnCopy'] } if bf\n")
                .append("  end\n")
                .append("end\n");

        // Swift Package Manager products, which the loop above cannot see.
        //
        // A build file for a package product carries a `product_ref` and NO `file_ref`, so every
        // `next unless ref && ref.path` above skips it silently -- a project declaring
        // ios.swiftPackages got its packages linked into the phone and not the watch, and a watch
        // lifecycle calling into one failed to link with undefined symbols and nothing naming the
        // cause. The product dependency also has to be listed on the target itself, not only in the
        // frameworks phase, or Xcode does not resolve it for that target at all.
        //
        // A dependency object of its own per target, rather than the phone's shared between both:
        // that is what Xcode itself writes, and a target's package_product_dependencies is
        // conceptually its own list even though the pbxproj format would tolerate one object in
        // two.
        //
        // No SDK check is possible here, unlike the system frameworks above. A package declares its
        // supported platforms in its own Package.swift, which is not resolved until xcodebuild runs.
        // So this mirrors the declaration and says so: a package with no watchOS support fails with
        // Xcode naming the product, which is a better outcome than a link error naming nothing.
        s.append("app_target.package_product_dependencies.to_a.each do |dep|\n")
                .append("  name = dep.respond_to?(:product_name) ? dep.product_name : nil\n")
                .append("  next unless name\n")
                .append("  next if watch_target.package_product_dependencies.any? { |d| "
                        + "d.respond_to?(:product_name) && d.product_name == name }\n")
                .append("  mirrored = xcproj.new("
                        + "Xcodeproj::Project::Object::XCSwiftPackageProductDependency)\n")
                .append("  mirrored.package = dep.package if dep.respond_to?(:package)\n")
                .append("  mirrored.product_name = name\n")
                .append("  watch_target.package_product_dependencies << mirrored\n")
                .append("  linked = watch_target.frameworks_build_phase.files.to_a.any? { |bf| "
                        + "bf.respond_to?(:product_ref) && bf.product_ref && "
                        + "bf.product_ref.respond_to?(:product_name) && "
                        + "bf.product_ref.product_name == name }\n")
                .append("  unless linked\n")
                .append("    pbf = xcproj.new(Xcodeproj::Project::Object::PBXBuildFile)\n")
                .append("    pbf.product_ref = mirrored\n")
                .append("    watch_target.frameworks_build_phase.files << pbf\n")
                .append("  end\n")
                .append("  puts \"[watchNative] linking Swift package product #{name} into the "
                        + "watch target; scope the package to iOS if it has no watchOS support\"\n")
                .append("end\n");

        // Mirror the iOS app's bundle resources into the watch target. The CN1
        // runtime loads its theme + assets from the app bundle at runtime
        // (Resources.open(\"/iOS7Theme.res\"), the app theme.res / CN1Resource.res,
        // material-design-font.ttf for FontImage glyphs, etc.). The watch target
        // ships with an empty resources phase, so without this the watch app
        // can't find the native theme (falls back to the default look), the app
        // theme, or any bundled image/font -> wrong styling + missing images.
        // Copying the iOS app-icon PNGs along too is harmless -- they are simply
        // ignored, because watchOS takes its icon from an asset catalog
        // (ASSETCATALOG_COMPILER_APPICON_NAME), not from Info.plist keys.
        // Skip iOS-only UI / icon assets: the asset catalog's AppIcon set has no
        // watch-applicable content (build error), and storyboards/xibs are the
        // iOS UI. The CN1 runtime resources (.res/.ttf/data) are what we need.
        //
        // Consequence, and it is deliberate rather than overlooked: the watch app
        // therefore ships with no app icon. That does not affect building, running
        // or testing -- only archiving for App Store submission, which Apple
        // rejects without one. Generating a watch AppIcon catalog belongs with the
        // watchOS widget-extension target, where there is a real archive to verify
        // it against; until then the developer guide tells the reader to add an
        // AppIcon set to the watch target before submitting.
        s.append("res_skip = %w[.xcassets .storyboard .xib]\n")
                .append("app_target.resources_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  next if res_skip.any? { |ext| ref.path.to_s.end_with?(ext) }\n")
                .append("  unless watch_target.resources_build_phase.files_references.include?(ref)\n")
                .append("    watch_target.resources_build_phase.add_file_reference(ref)\n")
                .append("  end\n")
                .append("end\n");

        // A companion watch app is embedded in the iOS app so the pair installs
        // together -- that is the whole point of declaring a watchMain next to a
        // phone main, so it is not opt-in. A standalone watch app ships on its
        // own instead, so strip any dependency/copy phase Xcode or an earlier
        // generator run left behind.
        if (isStandalone()) {
            s.append("app_target.dependencies.to_a.each do |dep|\n")
                    .append("  proxy = dep.respond_to?(:target_proxy) ? dep.target_proxy : nil\n")
                    .append("  remote = proxy && proxy.respond_to?(:remote_global_id) ? xcproj.objects_by_uuid[proxy.remote_global_id] : nil\n")
                    .append("  dep_target = dep.respond_to?(:target) ? dep.target : nil\n")
                    .append("  dep_target = remote if dep_target.nil?\n")
                    .append("  next unless dep_target && dep_target.respond_to?(:name) && dep_target.name == watch_name\n")
                    .append("  dep.remove_from_project\n")
                    .append("  proxy.remove_from_project if proxy && proxy.respond_to?(:remove_from_project) && xcproj.objects.include?(proxy)\n")
                    .append("end\n")
                    .append("app_target.build_phases.to_a.each do |phase|\n")
                    .append("  next unless phase.respond_to?(:display_name) && phase.display_name == 'Embed Watch Content'\n")
                    .append("  phase.remove_from_project\n")
                    .append("end\n");
        } else {
            // Companion: embed the watch .app into the iOS app under
            // $(CONTENTS_FOLDER_PATH)/Watch and add a build dependency so the
            // pair archives together.
            s.append("app_target.add_dependency(watch_target)\n")
                    // Mac Catalyst builds this same app target for macOS, and macOS refuses to
                    // carry embedded watchOS content ("contains embedded content built for the
                    // watchOS platform, which is not allowed"). A platformFilter of ios keeps the
                    // dependency and the copy out of the Catalyst variant while leaving the iPhone
                    // build with its embedded watch app.
                    .append("app_target.dependencies.to_a.each do |dep|\n")
                    .append("  proxy = dep.respond_to?(:target_proxy) ? dep.target_proxy : nil\n")
                    .append("  remote = proxy && proxy.respond_to?(:remote_global_id) ? xcproj.objects_by_uuid[proxy.remote_global_id] : nil\n")
                    .append("  dep_target = dep.respond_to?(:target) ? dep.target : nil\n")
                    .append("  dep_target = remote if dep_target.nil?\n")
                    .append("  next unless dep_target && dep_target.respond_to?(:name) && dep_target.name == watch_name\n")
                    .append("  dep.platform_filter = 'ios' if dep.respond_to?(:platform_filter=)\n")
                    .append("end\n")
                    .append("embed = app_target.build_phases.find { |p| p.respond_to?(:symbol_dst_subfolder_spec) && p.display_name == 'Embed Watch Content' }\n")
                    .append("if embed.nil?\n")
                    .append("  embed = app_target.new_copy_files_build_phase('Embed Watch Content')\n")
                    .append("  embed.symbol_dst_subfolder_spec = :products_directory\n")
                    .append("  embed.dst_path = '$(CONTENTS_FOLDER_PATH)/Watch'\n")
                    .append("end\n")
                    .append("product = watch_target.product_reference\n")
                    .append("unless embed.files_references.include?(product)\n")
                    .append("  bf = embed.add_file_reference(product)\n")
                    .append("  bf.settings = { 'ATTRIBUTES' => ['RemoveHeadersOnCopy'] }\n")
                    .append("end\n")
                    .append("embed.files.to_a.each do |bf|\n")
                    .append("  bf.platform_filter = 'ios' if bf.respond_to?(:platform_filter=)\n")
                    .append("end\n");
        }

        // The generated watch entry point CN1WatchApp.swift (@main, imports
        // WatchKit) lives in <mainClass>-src/ and is globbed into the iOS app
        // target too. WatchKit doesn't exist for iOS, so the iOS target must not
        // compile it -- add it to the iOS app target's EXCLUDED_SOURCE_FILE_NAMES
        // (preserving any existing exclusions, e.g. the Mac Catalyst slice).
        s.append("app_target.build_configurations.each do |config|\n")
                .append("  cur = config.build_settings['EXCLUDED_SOURCE_FILE_NAMES']\n")
                .append("  list = cur.is_a?(Array) ? cur.dup : (cur.is_a?(String) && !cur.empty? ? cur.split(/\\s+/) : [])\n")
                .append("  list << 'CN1WatchApp.swift' unless list.include?('CN1WatchApp.swift')\n")
                .append("  config.build_settings['EXCLUDED_SOURCE_FILE_NAMES'] = list\n")
                .append("end\n");

        s.append("xcproj.save\n");
        if (isStandalone()) {
            // Said out loud at build time. The target is detached from the phone app and
            // installable, but the archive step still selects the phone scheme, so the IPA this
            // build returns is the phone app -- and a developer who asked for a watch-only product
            // would otherwise discover that by inspecting the artifact.
            owner.log("[watchNative] NOTE: codename1.watchStandalone builds " + watchTargetName
                    + " as a detached, installable product, but this build archives the phone "
                    + "scheme. To submit the watch app, open the generated Xcode project and "
                    + "archive the " + watchTargetName + " scheme directly.");
        }
        return s.toString();
    }
}
