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

import com.codename1.util.IOSWidgetExtensionBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    /**
     * watchOS floor for the watch APP: single-target WKApplication apps and the SwiftUI
     * onChange(of:) two-parameter API the generated CN1WatchRootView uses.
     *
     * <p>Package-visible because the complication extension has to agree with it. WidgetKit
     * itself goes back to watchOS 9 and the extension can build there, but it is embedded in
     * this app -- so a watch that cannot install the app cannot show its complication either,
     * and an extension advertising 9 while the host requires 10 is advertising nothing.</p>
     */
    static final String MIN_DEPLOYMENT_TARGET = "10.0";

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
    // The generated CN1WatchWidgets folder under dist/, or null when the app declares no watch
    // complication. Set by IPhoneBuilder before applyXcodeSettings runs, because the extension
    // is embedded in the WATCH app target and that target does not exist until this builder
    // creates it -- which is why this cannot ride the ordinary app-extension path.
    private File watchWidgetExtensionDir;
    // The App Group the watch app and its extension share. Same identifier as the phone's; the
    // container behind it is watch-local, which is what makes the watch publish for itself.
    private String surfacesAppGroup;
    // The extension's watchOS deployment target, so the plist can advertise the same floor the
    // natives compare against.
    private String surfacesMinOS;
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

    // Frameworks the watch target must not link; ParparVM weak-links these (see
    // -Doptional.frameworks) so the iOS slice is unchanged while the watch slice tolerates the
    // absent symbols.
    //
    // Two ways in, and only the first is a build error when missed. A framework watchOS does NOT
    // HAVE cannot even be weak-linked -- weak linking still resolves against the SDK -- so leaving
    // one out fails the watch link with "framework 'X' not found". A framework watchOS HAS but the
    // port never references from watch-compiled sources is here too; dropping it is merely tidy.
    //
    // TO ADD AN ENTRY, check the SDK rather than guessing:
    //   ls "$(xcrun --sdk watchos --show-sdk-path)/System/Library/Frameworks" | grep X
    // The absences below were all verified that way. The port's own sources are already
    // #if !TARGET_OS_WATCH guarded around each of them -- this list is the link half of the same
    // conditional-system-library arrangement, and it is the half that silently lags.
    static final String WATCH_OPTIONAL_FRAMEWORKS =
            "OpenGLES.framework;GLKit.framework;Metal.framework;"
            + "MapKit.framework;MediaPlayer.framework;MessageUI.framework;"
            + "AddressBookUI.framework;AddressBook.framework;"
            + "WebKit.framework;StoreKit.framework;"
            // The three the translator puts in EVERY project's link phase
            // (ByteCodeTranslator.includeFrameworks) that watchOS does not have. Their headers
            // were already guarded, so the watch target compiled and then failed at the link --
            // which is why this went unnoticed until an unsigned device archive got that far.
            + "SystemConfiguration.framework;AudioToolbox.framework;QuickLook.framework;"
            // CarPlay.framework is iOS-only (absent on watchOS); it is linked on the iOS slice when
            // the app references com.codename1.car, so weak-link it for the watch slice.
            + "CarPlay.framework;"
            // CoreSpotlight backs the indexing half of com.codename1.intents and is linked on the
            // iOS slice when the app references that package. The watch never uses it: the
            // CN1_USE_INTENTS define is explicitly undone for TARGET_OS_WATCH, so the intent
            // natives compile to their unsupported stubs there and nothing calls into it.
            + "CoreSpotlight.framework;"
            // ARKit and SceneKit are absent on watchOS; they are linked on the iOS slice when the
            // app references com.codename1.ar, so weak-link them for the watch slice.
            + "ARKit.framework;SceneKit.framework;"
            // The CONDITIONAL ones -- added by IPhoneBuilder's API scan rather than by the
            // translator, so they appear only in projects that use the feature. That is why they
            // outlived two rounds of this list: a build that never touches Vision never links it,
            // and the watch link only fails for the app that does.
            + "AdSupport.framework;CoreImage.framework;CoreNFC.framework;"
            + "CoreTelephony.framework;JavaScriptCore.framework;Vision.framework;"
            // Named by PlatformFeatureCatalog rather than written in IPhoneBuilder, so they are
            // built as `name + ".framework"` and no quoted literal exists to grep for. That blind
            // spot is why they survived the audit that caught the six above; the partition test
            // now reads the catalog too.
            + "VisionKit.framework;Speech.framework;"
            // The ONLY framework whose availability differs between the two watch SDKs: present
            // for the device, absent for the simulator. A single declared list cannot be right
            // both ways, so the question is which side the watch actually needs -- and it needs
            // neither. Every BGTaskScheduler use in the port is #if !TARGET_OS_WATCH, with no-op
            // natives on the watch side, so the framework is dead weight on device and a broken
            // link on the simulator. Dropping it is correct for both.
            //
            // This is the "device-only framework" the SDK probe was once written to protect. It
            // never needed protecting.
            + "BackgroundTasks.framework;"
            // MatterSupport IS in the watchOS SDK -- verified with the ls above -- but the watch
            // slice compiles the add-device flow out (CN1SmartHome.h #undefs
            // CN1_INCLUDE_MATTER_SETUP for TARGET_OS_WATCH), because Apple's sheet is iOS and
            // iPadOS only. Present but unreferenced is exactly what this list is for.
            + "MatterSupport.framework";

    /**
     * Frameworks the watch target MAY link, so that every framework this builder can emit is
     * classified one way or the other and {@code WatchNativeBuilderTest} can prove it.
     *
     * <p>Nothing reads this at build time. It exists because the list above is a deny list, and a
     * deny list is silent about the thing nobody thought of: a framework added to
     * {@code IPhoneBuilder} and classified nowhere is simply kept, and if watchOS does not have it
     * the watch link fails -- three separate times, one framework per CI round, before this pair
     * was written. The test turns that into a failing assertion on the commit that adds it.</p>
     */
    static final String WATCH_LINKABLE_FRAMEWORKS =
            "Accelerate.framework;AuthenticationServices.framework;AVFoundation.framework;"
            + "AVKit.framework;CoreBluetooth.framework;"
            + "CoreLocation.framework;CoreMedia.framework;CoreML.framework;"
            + "CoreMotion.framework;CoreText.framework;CoreVideo.framework;"
            + "DeviceCheck.framework;EventKit.framework;GameKit.framework;"
            // HomeKit is present on watchOS and the watch slice genuinely uses it: CN1SmartHome.m
            // compiles for the watch and a wrist app controlling a light is the obvious case.
            + "HealthKit.framework;HomeKit.framework;"
            + "LocalAuthentication.framework;MobileCoreServices.framework;"
            + "NaturalLanguage.framework;NetworkExtension.framework;PhotosUI.framework;"
            + "QuartzCore.framework;Security.framework;UserNotifications.framework;"
            + "WatchConnectivity.framework";

    WatchNativeBuilder(IPhoneBuilder owner) {
        this.owner = owner;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Declares the generated watch widget extension so the Xcode script embeds it in the watch
     * app.
     *
     * @param extensionDir the CN1WatchWidgets folder under dist/, or null for no complication
     * @param appGroup the App Group shared by the watch app and its extension
     * @param minOS the extension's watchOS deployment target
     */
    void setWidgetExtension(File extensionDir, String appGroup, String minOS) {
        this.watchWidgetExtensionDir = extensionDir;
        this.surfacesAppGroup = appGroup;
        this.surfacesMinOS = minOS;
    }

    /** The App Group the watch bundle needs entitled, or null when it publishes no surfaces. */
    String getSurfacesAppGroup() {
        return surfacesAppGroup;
    }

    /** The extension's watchOS floor, advertised to the natives through the watch plist. */
    String getSurfacesMinOS() {
        return surfacesMinOS;
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

    /**
     * Where a mixed asset catalog's watch-compatible copy is staged, beside the project.
     *
     * <p>Beside rather than inside the app's {@code -src} folder because the catalog filter runs
     * whether or not the watch has a translation of its own, and the {@code watch-src} tree only
     * exists in the separately-rooted case.</p>
     */
    static final String WATCH_ASSET_STAGING_DIR = "cn1-watch-assets";

    /**
     * The watch target's own asset catalog, holding the app icon it cannot inherit.
     *
     * <p>Separate from the phone's Images.xcassets on purpose: that one carries the iOS AppIcon
     * set, whose idioms mean nothing to watchOS, and the filter that keeps it off the watch is why
     * the watch had no icon at all.</p>
     */
    static final String WATCH_ICON_CATALOG = "WatchImages.xcassets";

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
          .append("    var body: some Scene {\n");
        if (watchWidgetExtensionDir != null) {
            // A complication tap launches the watch app with the widgetURL rather than handing it
            // to a delegate -- there is no UIApplicationDelegate here at all -- so the scene is
            // the only place it can be caught. Without this the tap opened the app and the action
            // went nowhere. Surfaces.dispatchAction queues until the handler registers, which is
            // what makes the cold-start case (the usual one for a complication) work.
            sw.append("        WindowGroup {\n")
              .append("            CN1WatchRootView()\n")
              .append("                .onOpenURL { url in\n")
              .append("                    cn1_watch_surface_url(url.absoluteString)\n")
              .append("                }\n")
              .append("        }\n");
        } else {
            sw.append("        WindowGroup { CN1WatchRootView() }\n");
        }
        sw.append("    }\n")
          .append("}\n\n")
          .append("final class CN1WatchAppDelegate: NSObject, WKApplicationDelegate {\n")
          // BEFORE anything SwiftUI does, and deliberately not from initVM. A mirrored
          // complication update wakes a terminated watch app in the background, where the root
          // view is not guaranteed to appear -- so CN1WatchHost.startWithWidth() may never run,
          // initVM with it, and the WCSession activation that lives there never happens. That is
          // precisely the launch the mirror causes, so putting the activation anywhere the VM
          // gates would leave it unreachable in its own use case. This delegate callback runs on
          // every launch either way.
          .append("    func applicationDidFinishLaunching() { cn1_watch_bootstrap_didFinishLaunching() }\n")
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
          .append("    @State private var dragging = false\n")
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
          // A DRAG gesture, with a zero minimum distance, so it also covers a plain tap.
          //
          // SpatialTapGesture reports only a completed tap, at its final point, which the host
          // turned into a press immediately followed by a release. Nothing in between ever reached
          // pointerDraggedToX, so a Slider could not be moved, a scrollable container could not be
          // dragged, and a swipe gesture never fired -- every drag-driven control on the watch was
          // inert while looking like it had been touched.
          //
          // onChanged fires continuously from the moment the finger lands, so the FIRST one is the
          // press and the rest are drags; the flag is what tells them apart, and onEnded clears it
          // after the release. A tap produces exactly one onChanged and one onEnded, which is the
          // press/release pair it produced before.
          .append("            .gesture(DragGesture(minimumDistance: 0)\n")
          .append("                .onChanged { e in\n")
          .append("                    let x = Int32(e.location.x)\n")
          .append("                    let y = Int32(e.location.y)\n")
          .append("                    if dragging {\n")
          .append("                        CN1WatchHost.shared().pointerDraggedTo(x: x, y: y)\n")
          .append("                    } else {\n")
          .append("                        dragging = true\n")
          .append("                        CN1WatchHost.shared().pointerPressedAt(x: x, y: y)\n")
          .append("                    }\n")
          .append("                }\n")
          .append("                .onEnded { e in\n")
          .append("                    let x = Int32(e.location.x)\n")
          .append("                    let y = Int32(e.location.y)\n")
          // Press first if onChanged never ran: a gesture can end without one, and a release with
          // no press leaves the CN1 event stream unbalanced.
          .append("                    if !dragging {\n")
          .append("                        CN1WatchHost.shared().pointerPressedAt(x: x, y: y)\n")
          .append("                    }\n")
          .append("                    dragging = false\n")
          .append("                    CN1WatchHost.shared().pointerReleasedAt(x: x, y: y)\n")
          .append("                })\n")
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
          .append("extern void cn1_watch_runtime_willEnterForeground(void);\n\n")
          // Always defined, whatever this build carries, so the Swift delegate can call it
          // unconditionally. Its body is what changes.
          .append("// Called from the app delegate on EVERY launch, including a background wake\n")
          .append("// that never shows the root view -- see the comment on the call site.\n");
        if (watchWidgetExtensionDir != null) {
            bs.append("extern void cn1_watch_activate_connectivity(void);\n")
              .append("void cn1_watch_bootstrap_didFinishLaunching(void) {\n")
              .append("    cn1_watch_activate_connectivity();\n")
              .append("}\n\n");
        } else {
            bs.append("void cn1_watch_bootstrap_didFinishLaunching(void) { }\n\n");
        }
        bs.append("// App-specific entry: register natives + set the main class, init\n")
          .append("// Display (starts the EDT) and block this thread inside initVM.\n")
          .append("extern void ").append(mainStub)
          .append("_main___java_lang_String_1ARRAY(struct ThreadLocalData* threadStateData, JAVA_OBJECT arg);\n")
          .append("void cn1_watch_app_main(void) {\n")
          .append("    ").append(mainStub)
          .append("_main___java_lang_String_1ARRAY(getThreadLocalData(), JAVA_NULL);\n")
          // Nothing after the call above: it does not return. Display.init -> postInit ->
          // IOSNative.initVM blocks this thread forever on the watch, exactly as UIApplicationMain
          // does on the phone, so readiness is published from inside initVM's watch branch -- right
          // after the lifecycle callback that makes it true. A call placed here would never run.
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
        StringBuilder bridging = new StringBuilder("#import \"CN1WatchHost.h\"\n");
        // Always declared, because the generated delegate always calls it and the bootstrap
        // always defines it. What differs is whether its body does anything.
        bridging.append("\n// Launch hook: implemented in the generated CN1WatchBootstrap.m,\n")
                .append("// called from CN1WatchAppDelegate.applicationDidFinishLaunching.\n")
                .append("void cn1_watch_bootstrap_didFinishLaunching(void);\n");
        if (watchWidgetExtensionDir != null) {
            // The complication tap path. A plain C function rather than an Objective-C class, so
            // Swift only sees it if it is declared in the bridging header -- and the SwiftUI
            // scene's onOpenURL is the only place a watch app can catch that URL at all.
            bridging.append("\n// Complication tap: implemented in IOSNative.m, called from the\n")
                    .append("// generated CN1WatchApp scene's onOpenURL.\n")
                    .append("void cn1_watch_surface_url(const char *url);\n");
        }
        owner.createFile(new File(appSrcDir, mainClass + "-Watch-Bridging-Header.h"),
                bridging.toString().getBytes(StandardCharsets.UTF_8));
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
        return injectedPlistKeys(request.getArg("ios.plistInject", null));
    }

    /// The keys a plist FRAGMENT declares, for a caller holding the text rather than the request.
    ///
    /// The Info.plist renderer is that caller: it appends its own fragments to the injected one as
    /// it goes, so what it must not declare twice is decided by the string it has built, not by
    /// the build hint it started from.
    static java.util.List<String> injectedPlistKeys(String inject) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (inject == null) {
            return out;
        }
        int at = 0;
        while (true) {
            int content = contentAfterOpenTag(inject, "key", at);
            if (content < 0) {
                return out;
            }
            int close = closeOfElement(inject, content, "</key>");
            if (close < 0) {
                return out;
            }
            String key = plistStringContent(inject.substring(content, close)).trim();
            if (key.length() > 0 && !out.contains(key)) {
                out.add(key);
            }
            // Past the content, not past a fixed-width end tag: `</key >` is longer than `</key>`
            // and the scan for the next opening tag skips whatever sits between them anyway.
            at = close + 1;
        }
    }

    /// The tag name of the value a {@code ios.plistInject} fragment gives {@code key} --
    /// {@code "string"}, {@code "false"}, {@code "array"} -- or null when the fragment does not
    /// carry the key at all.
    ///
    /// {@link #injectedPlistString} answers only for {@code <string>} values and answers null for
    /// every other kind, which reads to a caller as "not supplied". A validator that then fills in
    /// a default gets neither: the renderer suppresses the generated value because the key IS
    /// there, and the injected non-string stays. Telling absent from present-and-not-a-string
    /// needs the tag itself.
    static String injectedPlistValueTag(BuildRequest request, String key) {
        String inject = request.getArg("ios.plistInject", null);
        if (inject == null) {
            return null;
        }
        int value = injectedValueAt(inject, key);
        if (value < 0) {
            return null;
        }
        int element = nextElementAt(inject, value);
        // Present, and with no element for a value: text, or nothing at all. Not the key's
        // absence, which is what the caller has to tell it from.
        return element < 0 ? "" : tagAt(inject, element);
    }

    /// The strings inside the array a {@code ios.plistInject} fragment gives {@code key}, trimmed
    /// and in document order.
    ///
    /// Empty both for a key the fragment does not carry and for one whose value is not an array of
    /// strings; a caller that has to tell those apart asks {@link #injectedPlistValueTag} first.
    ///
    /// The array belonging to the KEY. Found by searching for the key's name and reading the next
    /// {@code <array>} after it, a fragment that mentions the name in a comment first took the
    /// array of whatever key came next -- so a plist that declared the value perfectly well was
    /// reported as not declaring it.
    static java.util.List<String> injectedPlistStringArray(BuildRequest request, String key) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        String inject = request.getArg("ios.plistInject", null);
        if (inject == null) {
            return out;
        }
        int value = injectedValueAt(inject, key);
        int element = value < 0 ? -1 : nextElementAt(inject, value);
        if (element < 0 || !"array".equals(tagAt(inject, element))) {
            return out;
        }
        int body = contentAfterOpenTag(inject, "array", element);
        int end = body < 0 ? -1 : closeOfElement(inject, body, "</array>");
        if (end < 0) {
            return out;
        }
        int at = body;
        while (true) {
            int start = contentAfterOpenTag(inject, "string", at);
            if (start < 0 || start > end) {
                return out;
            }
            int close = closeOfString(inject, start);
            if (close < 0 || close > end) {
                return out;
            }
            out.add(plistStringContent(inject.substring(start, close)).trim());
            at = close + 1;
        }
    }

    /// Where the value belonging to {@code key} begins -- just past its {@code </key>} -- or -1
    /// when the fragment does not carry the key.
    private static int injectedValueAt(String inject, String key) {
        int at = 0;
        while (true) {
            int content = contentAfterOpenTag(inject, "key", at);
            if (content < 0) {
                return -1;
            }
            int close = closeOfElement(inject, content, "</key>");
            if (close < 0) {
                return -1;
            }
            at = close + 1;
            if (!key.equals(plistStringContent(inject.substring(content, close)).trim())) {
                continue;
            }
            int end = inject.indexOf('>', close);
            return end < 0 ? -1 : end + 1;
        }
    }

    /// The {@code <} of the next real element at or after {@code from}, or -1 when what follows is
    /// text or nothing. Whitespace, comments and CDATA sit between a key and its value in real
    /// fragments and none of them is the value.
    private static int nextElementAt(String inject, int from) {
        int i = from;
        while (i < inject.length()) {
            if (Character.isWhitespace(inject.charAt(i))) {
                i++;
                continue;
            }
            int skipped = skipMarkupBefore(inject, i, i);
            if (skipped < 0) {
                return -1;
            }
            if (skipped != i) {
                i = skipped;
                continue;
            }
            return inject.charAt(i) == '<' ? i : -1;
        }
        return -1;
    }

    /// The element name at an opening tag, lowercased. Empty for an end tag, which is not one.
    private static String tagAt(String inject, int element) {
        StringBuilder tag = new StringBuilder();
        for (int j = element + 1; j < inject.length()
                && Character.isLetterOrDigit(inject.charAt(j)); j++) {
            tag.append(inject.charAt(j));
        }
        return tag.toString().toLowerCase(java.util.Locale.ENGLISH);
    }

    static String injectedPlistString(BuildRequest request, String key) {
        String inject = request.getArg("ios.plistInject", null);
        if (inject == null) {
            return null;
        }
        // The key's CONTENT, resolved, rather than the literal text `<key>NAME</key>`.
        //
        // A plist may spell a key as <key><![CDATA[CFBundleShortVersionString]]></key>, or wrap a
        // comment around part of it, and an XML parser reads all of those as the same key -- which
        // is what the phone's plist does. Matching the serialized form here meant the phone
        // suppressed its default for a key the watch then failed to find, and the pair shipped with
        // different marketing versions, which archive validation rejects.
        //
        // And the key's OWN value. Scanning forward for the next <string> ANYWHERE after the key,
        // a key given <false/> answered with an unrelated later key's string -- so a purpose-string
        // check passed on a value the plist renderer keeps as the boolean false, and the app was
        // terminated on the device for a disclosure the build had just approved.
        int value = injectedValueAt(inject, key);
        int element = value < 0 ? -1 : nextElementAt(inject, value);
        if (element < 0 || !"string".equals(tagAt(inject, element))) {
            return null;
        }
        int content = contentAfterOpenTag(inject, "string", element);
        int close = content < 0 ? -1 : closeOfString(inject, content);
        if (close < 0) {
            return null;
        }
        return plistStringContent(inject.substring(content, close));
    }

    /// The {@code </string>} that closes the element, skipping over CDATA sections.
    ///
    /// A plain {@code indexOf} finds the first literal occurrence, and inside
    /// {@code <![CDATA[a</string>b]]>} that occurrence is DATA -- the element would be cut in half
    /// at a point the XML parser reading the phone's plist never stops at.
    private static int closeOfString(String inject, int from) {
        return closeOfElement(inject, from, "</string>");
    }

    /// The next occurrence of a tag that is really markup, skipping comments and CDATA.
    ///
    /// A plist fragment routinely carries an EXAMPLE in a comment --
    /// {@code <!-- <key>CFBundleVersion</key><string>9.9</string> -->} -- and the phone's XML parser
    /// ignores it. A raw indexOf did not: the watch took the commented version while the app it is
    /// embedded in kept its real one, and archive validation rejects that mismatch. CDATA is the
    /// same story from the other side: {@code <![CDATA[<key>x</key>]]>} is text, not an element.
    /// The end of the next real opening tag for an element, or -1.
    ///
    /// Returns where its CONTENT starts, because an opening tag is not a fixed width: `<key >` is
    /// the same element as `<key>` to an XML parser, and the literal search missed it -- the phone
    /// accepted the override and the watch fell back to its generated version, which is the
    /// mismatch archive validation rejects.
    private static int contentAfterOpenTag(String inject, String element, int from) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<" + element + "(?:\\s[^>]*)?>")
                .matcher(inject);
        int i = from;
        while (i <= inject.length() && m.find(i)) {
            int at = m.start();
            int skipped = skipMarkupBefore(inject, at, i);
            if (skipped == at) {
                return m.end();
            }
            if (skipped < 0) {
                return -1;
            }
            i = skipped;
        }
        return -1;
    }

    /// Where to resume scanning so that `at` is not inside a comment or a CDATA section.
    ///
    /// Returns `at` when it is already outside both, the position just past the enclosing
    /// construct when it is not, and -1 when that construct never ends.
    private static int skipMarkupBefore(String inject, int at, int from) {
        int cdata = inject.indexOf(CDATA_OPEN, from);
        int comment = inject.indexOf(COMMENT_OPEN, from);
        boolean cdataFirst = cdata >= 0 && (comment < 0 || cdata < comment);
        int skipFrom = cdataFirst ? cdata : comment;
        if (skipFrom < 0 || skipFrom > at) {
            return at;
        }
        String opener = cdataFirst ? CDATA_OPEN : COMMENT_OPEN;
        String closer = cdataFirst ? CDATA_CLOSE : COMMENT_CLOSE;
        int end = inject.indexOf(closer, skipFrom + opener.length());
        return end < 0 ? -1 : end + closer.length();
    }

    private static int nextMarkup(String inject, String tag, int from) {
        int i = from;
        while (i <= inject.length()) {
            int at = inject.indexOf(tag, i);
            if (at < 0) {
                return -1;
            }
            int cdata = inject.indexOf(CDATA_OPEN, i);
            int comment = inject.indexOf(COMMENT_OPEN, i);
            boolean cdataFirst = cdata >= 0 && (comment < 0 || cdata < comment);
            int skipFrom = cdataFirst ? cdata : comment;
            if (skipFrom < 0 || skipFrom > at) {
                return at;
            }
            String opener = cdataFirst ? CDATA_OPEN : COMMENT_OPEN;
            String closer = cdataFirst ? CDATA_CLOSE : COMMENT_CLOSE;
            int end = inject.indexOf(closer, skipFrom + opener.length());
            if (end < 0) {
                // Unterminated: nothing after it can be located reliably, so the key is treated as
                // absent rather than guessed at.
                return -1;
            }
            i = end + closer.length();
        }
        return -1;
    }

    /// The end tag that closes an element, skipping over CDATA sections and comments.
    private static int closeOfElement(String inject, int from, String closeTag) {
        // `</key >` closes the same element as `</key>`, so the tag is matched as a pattern rather
        // than as literal text -- the same reason the opening tags are.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(java.util.regex.Pattern.quote(
                        closeTag.substring(0, closeTag.length() - 1)) + "\\s*>")
                .matcher(inject);
        int i = from;
        while (i <= inject.length()) {
            if (!m.find(i)) {
                return -1;
            }
            int close = m.start();
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
        // Surfaces on the watch. Same keys, same meaning and the same reader as the phone's:
        // the natives resolve the App Group container through the first and compare the running
        // OS against the second. The floor differs, though -- the watch extension targets
        // watchOS 10 where the phone's targets iOS 16.1 -- and the natives compare against the
        // OS actually running, so leaving this to the iOS default would have every watch report
        // no widget support.
        if (surfacesAppGroup != null && surfacesAppGroup.length() > 0) {
            plistString(sb, IOSWidgetExtensionBuilder.APP_GROUP_PLIST_KEY, surfacesAppGroup);
            plistString(sb, "CN1SurfacesMinOS", surfacesMinOS == null
                    ? IOSWidgetExtensionBuilder.WATCH_MIN_DEPLOYMENT_TARGET : surfacesMinOS);
            // The complication tap's own scheme. A widget supplies a cn1surface:// widgetURL and
            // the generated CN1WatchApp scene waits for it in onOpenURL -- but the watch is a
            // separate bundle and inherits none of the phone's URL types, so without this
            // declaration watchOS has nothing to route the URL to and the tap dispatches nothing.
            sb.append("    <key>CFBundleURLTypes</key>\n    <array>\n        <dict>\n")
              .append("            <key>CFBundleURLName</key>\n")
              .append("            <string>").append(escapeXml(request.getPackageName()))
              .append(".cn1surface</string>\n")
              .append("            <key>CFBundleURLSchemes</key>\n")
              .append("            <array>\n                <string>cn1surface</string>\n")
              .append("            </array>\n        </dict>\n    </array>\n");
        }
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
        boolean watchHealth = watchUsesHealth(owner.phoneUsesHealthData(request));
        boolean workoutProcessing =
                "true".equalsIgnoreCase(workoutProcessingHint);
        // Direction, not just presence. Taken from the same root the entitlement decision uses, so
        // a watch with its own lifecycle is judged on what IT reaches.
        boolean detectedRead = watchDetectedHealthRead();
        boolean detectedWrite = watchDetectedHealthWrite() || workoutProcessing;
        if (needsPurposeString(watchHealth, healthShare, healthUpdate,
                workoutProcessing, detectedRead, detectedWrite)) {
            // Entitled but with no purpose string in its own Info.plist,
            // which builds cleanly and then fails the moment the watch asks
            // for authorization. Apple requires a specific string and this
            // build never invents one, so the developer has to supply it.
            owner.error("This app enables HealthKit on the watch, but its"
                    + " Info.plist is missing "
                    + missingPurposeStrings(healthShare, healthUpdate,
                            detectedRead, detectedWrite)
                    + ". The watch has"
                    + " its own Info.plist, and watchOS refuses a HealthKit"
                    + " authorization request from a bundle with no purpose"
                    + " string for the operation it performs"
                    + (workoutProcessing
                        ? " -- a workout saves its session, so it needs"
                            + " ios.NSHealthUpdateUsageDescription."
                        : "."),
                    new RuntimeException("watch health usage string unset"));
        }
        writeWatchEntitlements(request, appSrcDir, watchHealth);
        writeWatchAppIcon(request, appSrcDir);
        File plist = new File(appSrcDir, request.getMainClass() + "-Watch-Info.plist");
        owner.createFile(plist, sb.toString().getBytes(StandardCharsets.UTF_8));
    }


    /// Names the string or strings this build is waiting for.
    ///
    /// "declares neither X nor Y" was accurate only while nothing was detected. With a direction
    /// known it named the wrong remedy: a read-only watch that had already supplied the update
    /// string was told to supply either one, and supplying the one it had changed nothing.
    static String missingPurposeStrings(String healthShare, String healthUpdate,
            boolean detectedRead, boolean detectedWrite) {
        if (!detectedRead && !detectedWrite) {
            return "ios.NSHealthShareUsageDescription and"
                    + " ios.NSHealthUpdateUsageDescription (either will do)";
        }
        boolean needShare = detectedRead && healthShare == null;
        boolean needUpdate = detectedWrite && healthUpdate == null;
        if (needShare && needUpdate) {
            return "ios.NSHealthShareUsageDescription (it reads) and"
                    + " ios.NSHealthUpdateUsageDescription (it writes)";
        }
        if (needShare) {
            return "ios.NSHealthShareUsageDescription, the one this watch needs"
                    + " because its code reads from the store";
        }
        return "ios.NSHealthUpdateUsageDescription, the one this watch needs"
                + " because its code writes to the store";
    }

    /// Whether the SCAN saw the watch bundle reading from the store.
    ///
    /// Follows watchUsesHealth's choice of root exactly: the watch inherits the phone's answer
    /// only when it runs the phone's code. An explicit watchNative.health does not suppress this
    /// -- that hint ADDS usage the scan cannot see, it does not deny what the scan did see.
    boolean watchDetectedHealthRead() {
        if ("false".equalsIgnoreCase(healthHint)) {
            return false;
        }
        return owner.phoneReadsHealthData();
    }

    /// The same for writes.
    boolean watchDetectedHealthWrite() {
        if ("false".equalsIgnoreCase(healthHint)) {
            return false;
        }
        return owner.phoneWritesHealthData();
    }


    /**
     * Writes the watch target's app icon.
     *
     * <p>The watch cannot use the phone's. An iOS {@code AppIcon.appiconset} declares iPhone and
     * iPad idioms, which is why it is filtered out of the catalog staged for the watch -- and
     * nothing replaced it, so every watch product built here had no icon. That does not affect
     * building, running or testing; it fails App Store submission, which is the one place it
     * cannot be worked around, and it made the standalone watch product unshippable without
     * hand-editing the generated project.</p>
     *
     * <p>One 1024x1024 image, in the single-size form Xcode has accepted since 14. The per-device
     * idiom list it replaced had to enumerate every watch size ever shipped and needed a new entry
     * for each new one; this form does not, and the deployment floor here is watchOS
     * {@value #MIN_DEPLOYMENT_TARGET}, well past where it became valid.</p>
     *
     * <p>Scaled from the project's own icon, as the phone's is. A developer who wants a distinct
     * watch icon replaces the set in the generated project, exactly as on iOS.</p>
     */
    void writeWatchAppIcon(BuildRequest request, File appSrcDir) throws java.io.IOException {
        byte[] source = request.getIcon();
        if (source == null || source.length == 0) {
            // Nothing to scale. The phone build has already reported this; adding a second
            // complaint about the watch would only bury it.
            return;
        }
        File iconSet = new File(new File(appSrcDir, WATCH_ICON_CATALOG), "AppIcon.appiconset");
        if (!iconSet.mkdirs() && !iconSet.isDirectory()) {
            throw new java.io.IOException("could not create " + iconSet);
        }
        java.awt.image.BufferedImage icon = javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(source));
        // A source the decoder cannot read is the phone build's complaint to make, not ours.
        if (icon == null) {
            return;
        }
        owner.createIconFile(new File(iconSet, "AppIcon.png"), icon, 1024, 1024);
        owner.createFile(new File(iconSet, "Contents.json"),
                ("{\n"
                + "  \"images\" : [\n"
                + "    {\n"
                + "      \"filename\" : \"AppIcon.png\",\n"
                + "      \"idiom\" : \"universal\",\n"
                + "      \"platform\" : \"watchos\",\n"
                + "      \"size\" : \"1024x1024\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"info\" : {\n"
                + "    \"author\" : \"xcode\",\n"
                + "    \"version\" : 1\n"
                + "  }\n"
                + "}\n").getBytes(StandardCharsets.UTF_8));
        // The catalog itself needs one too, or actool does not treat the directory as a catalog.
        owner.createFile(new File(appSrcDir, WATCH_ICON_CATALOG + "/Contents.json"),
                ("{\n  \"info\" : {\n    \"author\" : \"xcode\",\n"
                + "    \"version\" : 1\n  }\n}\n").getBytes(StandardCharsets.UTF_8));
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
        return needsPurposeString(watchUsesHealth, healthShare, healthUpdate, workoutProcessing,
                false, false);
    }

    /// The same, told which direction the scan actually saw.
    ///
    /// Apple wants the string that matches the OPERATION. A watch that only reads and declares
    /// only NSHealthUpdateUsageDescription is refused at authorization exactly as if it had
    /// declared nothing, and the reverse holds too -- so accepting either string whenever one was
    /// present emitted an entitled bundle missing the disclosure for what it does. The phone pass
    /// keeps the two apart; this one had collapsed them into a single boolean.
    ///
    /// `detectedRead` and `detectedWrite` are both false when nothing was detected and the answer
    /// came from `watchNative.health` alone. That hint says the bundle uses HealthKit and nothing
    /// about which way, so there is no direction to require and either string is still evidence
    /// that somebody thought about what it does.
    static boolean needsPurposeString(boolean watchUsesHealth,
            String healthShare, String healthUpdate,
            boolean workoutProcessing, boolean detectedRead, boolean detectedWrite) {
        if (!watchUsesHealth) {
            return false;
        }
        if (detectedRead && healthShare == null) {
            return true;
        }
        if (detectedWrite && healthUpdate == null) {
            return true;
        }
        if (detectedRead || detectedWrite) {
            // Direction known and every string it calls for is present. A workout adds no
            // requirement here: it writes, which detectedWrite already covers.
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
     * <p>The answer is APP-WIDE, for the watch as for the phone, and
     * {@code watchNative.health} overrides it in either direction.</p>
     *
     * <p>It used to be per-root: a distinct {@code watchMain} got its own
     * class walk, on the reasoning that entitling a non-health watch app
     * fails codesigning against an App ID without the capability. That is
     * a real failure and this is still the answer to it -- by hint rather
     * than by inference. The walk was deleted because it did not work:
     * see {@code IPhoneBuilder.phoneUsesHealthData}. It also cost hundreds
     * of lines to guess at something the project can simply state, and a
     * wrong guess in the other direction -- omitting the entitlement from
     * a watch app that does use HealthKit -- fails at runtime instead,
     * which is worse and harder to diagnose.</p>
     *
     * <p>So: a watch app that does not use HealthKit in a project whose
     * phone does sets {@code watchNative.health=false}.
     * {@code watchNative.health.workoutProcessing} implies true, because a
     * workout session is HealthKit.</p>
     */
    boolean watchUsesHealth(boolean appUsesHealth) {
        if ("true".equalsIgnoreCase(healthHint)) {
            return true;
        }
        if ("false".equalsIgnoreCase(healthHint)) {
            return false;
        }
        if ("true".equalsIgnoreCase(workoutProcessingHint)) {
            return true;
        }
        // The app-wide answer, for the watch as for the phone. This used to ask whether the WATCH
        // root reached health, from a per-root class walk that has since been deleted -- see
        // IPhoneBuilder.phoneUsesHealthData for why that walk protected nothing. When the scan is
        // wrong in either direction, watchNative.health says so and is checked above.
        return appUsesHealth;
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
        if (!watchNeedsEntitlements(usesHealth)) {
            return;
        }
        owner.createFile(new File(appSrcDir,
                request.getMainClass() + "-Watch.entitlements"),
                watchEntitlementsPlist(request, workoutProcessingHint, usesHealth,
                        surfacesAppGroup).getBytes(StandardCharsets.UTF_8));
    }

    /// Whether the watch bundle needs an entitlements file at all.
    ///
    /// It started as "does it use HealthKit", which was the only capability the watch did not
    /// inherit from the phone. Publishing complications adds a second: the watch app and its
    /// widget extension reach each other through an App Group, and the watch bundle is signed
    /// with this file and nothing else.
    private boolean watchNeedsEntitlements(boolean usesHealth) {
        return usesHealth || (surfacesAppGroup != null && surfacesAppGroup.length() > 0);
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
        return watchEntitlementsPlist(request, workoutProcessingHint, true, null);
    }

    /// As above, but saying which capabilities the watch bundle actually needs.
    ///
    /// Both are opt-in and neither implies the other. HealthKit was the only capability the
    /// watch did not inherit from the phone until complications arrived; publishing them adds
    /// an App Group, and a watch app may want either, both or -- for the two-argument overload's
    /// historical callers -- just the first.
    ///
    /// Granting one that is not used is not harmless: entitlement validation refuses a signature
    /// carrying a capability the provisioning profile does not have, which is the same reason
    /// background-delivery and recalibrate-estimates below are conditional rather than always on.
    ///
    /// The App Group id is the same string the phone uses. What differs is the container behind
    /// it: on the watch it resolves to a watch-local directory the phone cannot see, which is
    /// why the watch publishes its own timelines rather than reading the phone's.
    ///
    /// @param request the build
    /// @param workoutProcessingHint the workout hint, which implies HealthKit
    /// @param usesHealth whether to grant the HealthKit capability
    /// @param appGroup the App Group to entitle, or null when the watch publishes no surfaces
    /// @return the entitlements plist text
    static String watchEntitlementsPlist(BuildRequest request,
            String workoutProcessingHint, boolean usesHealth, String appGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" ")
          .append("\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n")
          .append("<plist version=\"1.0\">\n<dict>\n");
        if (appGroup != null && appGroup.length() > 0) {
            sb.append("    <key>com.apple.security.application-groups</key>\n")
              .append("    <array>\n        <string>").append(escapeXml(appGroup))
              .append("</string>\n    </array>\n");
        }
        if (usesHealth) {
        sb.append("    <key>com.apple.developer.healthkit</key>\n")
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
        if (!watchNeedsEntitlements(watchUsesHealth(phoneUsesHealth))) {
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
                // For staging a watch copy of a mixed asset catalog.
                .append("require 'fileutils'\n")
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
                // :application, NOT :watch2_app -- deliberately, and this has been raised.
                //
                // com.apple.product-type.application.watchapp2 is the LEGACY WatchKit App, valid
                // only paired with a separate watchkit2-extension target that carries the code.
                // What this generates is the modern single-target watch app that replaced it:
                // WKApplication in the plist, SwiftUI @main with @WKApplicationDelegateAdaptor,
                // a watchOS 10 floor, one target. Xcode's own watchOS App.xctemplate declares
                // com.apple.product-type.application and never mentions watchapp2; the only
                // watchapp2 in that template directory is the legacy iOS *container*. Setting it
                // here would put a single-target app on the paired product type with no extension
                // beside it.
                .append("  watch_target = xcproj.new_target(:application, watch_name, :watchos, '")
                .append(IPhoneBuilder.escapeRubyStr(MIN_DEPLOYMENT_TARGET)).append("')\n")
                .append("end\n")
                // Compile the shared ParparVM sources for the watch, minus the
                // GL/Metal-only files. Reuse the app target's compile sources so
                // we track exactly what was generated.
                // A %w[] word list is safe here and only here: EXCLUDED_WATCH_SOURCES is a fixed
                // constant in this file, so no project-supplied name can carry a space into it.
                .append("excluded = %w[").append(excluded).append("]\n");
        if (!watchSources.isEmpty()) {
            // The watch compiles its OWN translation, rooted at watchMain and shaken down to what
            // that entry point reaches. Nothing of the phone's tree is added: sharing it is what
            // made the watch binary carry the phone's whole graph, and the phone Stub's main then
            // had to be defined away to stop the two entry points colliding.
            // Quoted strings, not a %w[] word list. A translated native source is named after the
            // class it came from, and a project with a space in one -- `My Bridge.m` -- had that
            // name split into two words, so the watch target referenced two files that do not
            // exist and linked without the symbols the real one carries.
            StringBuilder names = new StringBuilder();
            for (String name : watchSources) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append('\'').append(IPhoneBuilder.escapeRubyStr(name)).append('\'');
            }
            // The app target's file SET, with the watch translation's CONTENTS.
            //
            // Adding every .m the watch translation emitted was wrong: its dist carries native
            // sources the app target never compiles -- UIWebViewEventDelegate.m among them, which
            // calls UIApplication and does not exist on watchOS. The shared-translation path has
            // always taken its file list from the app target, and that list is what belongs on the
            // watch too; only the translated bodies differ. So walk the app target exactly as the
            // shared path does, and swap each file for its watch-src counterpart where one exists.
            s.append("watch_sources = [").append(names).append("]\n")
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
                .append("%w[CN1WatchApp.swift CN1WatchBootstrap.m")
                // The app-side surfaces glue, when the app publishes complications. IOSNative
                // reaches CN1SurfaceBridge through NSClassFromString and CN1SurfaceConfig
                // supplies the App Group constant, so without both on the watch target every
                // surfaces native finds no bridge and answers unsupported.
                //
                // It only needs saying for the watch's OWN translation: the shared-translation
                // branch above copies the app target's compile sources, which the schemes script
                // has already swept these into. The de-dupe by basename keeps that case correct.
                .append(watchWidgetExtensionDir == null
                        ? "" : " CN1SurfaceBridge.swift CN1SurfaceConfig.swift")
                .append("].each do |name|\n")
                .append("  next if entry_existing.include?(name)\n")
                .append("  path = File.join(File.dirname(project_file), watch_src, name)\n")
                .append("  next unless File.exist?(path)\n")
                .append("  ref = xcproj.main_group.new_reference(watch_src + '/' + name)\n")
                .append("  watch_target.source_build_phase.add_file_reference(ref)\n")
                .append("end\n")
                // Build settings for the watch slice.
                .append("watch_target.build_configurations.each do |config|\n")
                .append("  bs = config.build_settings\n")
                .append("  bs['SDKROOT'] = 'watchos'\n")
                // Without this the catalog compiles and its icon is still not the app's -- actool
                // only promotes a set to the app icon when the target names it. Nothing named one
                // for the watch, so every watch product built here shipped iconless and could not
                // be submitted.
                .append("  bs['ASSETCATALOG_COMPILER_APPICON_NAME'] = 'AppIcon'\n");
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

        appendWidgetExtension(s, tmpFile, resolvedTeamId);

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
        // A static archive is judged by the architectures it was actually built for.
        //
        // arm64_32 and armv7k exist on watchOS and nowhere else, so either one is proof of a watch
        // device slice with no ambiguity to resolve -- unlike arm64, which is an iPhone and an
        // Apple silicon watch simulator at once. An archive that names neither cannot link into a
        // watch device build, and saying so by name beats a link error listing its symbols.
        // The Mach-O platform of one slice, which is the only thing that distinguishes an Apple
        // silicon watch Simulator object from an iPhone one -- they share an architecture name.
        //
        // LC_BUILD_VERSION records it as a number: 2 is iOS, 4 watchOS, 7 iOS Simulator, 9 watchOS
        // Simulator. Some otool versions print the symbolic name instead, so both spellings are
        // accepted. Objects old enough to predate LC_BUILD_VERSION carry LC_VERSION_MIN_WATCHOS
        // instead, and on an architecture no watch has ever shipped as hardware that can only be
        // the simulator.
        //
        // Unreadable is answered NO, matching the plist helper below: an archive left out costs a
        // link error naming it, where one wrongly linked in costs a platform mismatch deep in a
        // build that had no reason to involve it.
        s.append("def cn1_watch_slice_is_watch_simulator(path, arch)\n")
                .append("  out = `otool -l -arch #{arch} \"#{path}\" 2>/dev/null`\n")
                .append("  return false if out.nil? || out.strip.empty?\n")
                .append("  out.scan(/^\\s*platform\\s+(\\S+)\\s*$/).each do |m|\n")
                .append("    v = m[0].to_s.downcase\n")
                .append("    return true if v == '9' || v == 'watchossimulator'\n")
                .append("  end\n")
                .append("  return false if out.include?('LC_BUILD_VERSION')\n")
                .append("  out.include?('LC_VERSION_MIN_WATCHOS')\n")
                .append("end\n");

        // Whether a text-based stub names a watch platform.
        //
        // Unreadable is answered NO, as the plist and archive checks are: leaving a library out
        // costs a link error naming it, where linking one built for another platform costs a
        // mismatch deep in a build that had no reason to involve it.
        s.append("def cn1_watch_tbd_supports_watchos(ref)\n")
                .append("  path = (ref.real_path.to_s rescue nil)\n")
                .append("  return false unless path && File.exist?(path)\n")
                .append("  begin\n")
                .append("    text = File.read(path)\n")
                .append("  rescue StandardError\n")
                .append("    return false\n")
                .append("  end\n")
                // v4 target triples (arm64_32-watchos, arm64-watchos-simulator) and the older
                // platform lines both carry the word, and nothing else in a tbd does.
                .append("  text =~ /watchos/i ? true : false\n")
                .append("end\n");

        s.append("def cn1_watch_archive_has_watch_slice(ref)\n")
                .append("  path = (ref.real_path.to_s rescue nil)\n")
                .append("  return false unless path && File.exist?(path)\n")
                .append("  archs = `lipo -archs \"#{path}\" 2>/dev/null`.split\n")
                // A thin archive answers nothing to lipo -archs; ask the file itself.
                .append("  archs = `file \"#{path}\" 2>/dev/null`.scan("
                        + "/arm64_32|armv7k/) if archs.empty?\n")
                // A watch DEVICE slice is unambiguous -- arm64_32 and armv7k exist nowhere else --
                // but the target also builds for watchsimulator, and an archive with only the
                // device slice fails every simulator run. A simulator slice is x86_64 (Intel) or
                // arm64 (Apple silicon); arm64 alone cannot be told apart from an iOS device slice
                // by architecture, so the pair is what is required, and the log names what was
                // found so a mismatch is attributable rather than mysterious.
                .append("  device = archs.any? { |a| a == 'arm64_32' || a == 'armv7k' }\n")
                // Which is more than the architecture can answer. A fat archive carrying an
                // iOS-device arm64 next to a watch-device arm64_32 -- an ordinary shape for a
                // library vending a phone slice and a watch slice -- passed an architecture-only
                // test and was linked into the Apple silicon watch Simulator against objects built
                // for iOS, failing on a platform mismatch. The slice itself records what it was
                // built for, so ask it.
                .append("  simulator = archs.any? { |a| (a == 'x86_64' || a == 'arm64') && "
                        + "cn1_watch_slice_is_watch_simulator(path, a) }\n")
                // EITHER slice, not both.
                //
                // Demanding both rejected a valid watchOS-only archive, and the framework phase
                // then skipped it and the watch target failed on undefined symbols -- over a
                // simulator slice that a device build was never going to load.
                //
                // Either, rather than a specific one, because this generator hands the developer
                // an Xcode project and does not know which destination they will build for. The
                // cloud builder does know -- it selects the SDK itself -- and requires exactly the
                // slice that destination links against.
                .append("  watch = device || simulator\n")
                .append("  puts \"[watchNative] #{File.basename(path)} #{watch ? 'has' : 'has no'}"
                        + " usable watchOS slices (#{archs.empty? ? 'unknown' : archs.join(' ')}; "
                        + "device=#{device} simulator=#{simulator})\"\n")
                .append("  watch\n")
                .append("end\n");

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
                // EITHER watch variant. Requiring both rejected a bundle that declares only
                // WatchOS, which links perfectly well on a device -- and this generator does not
                // know which destination the developer will pick, so it cannot require the other.
                // The cloud builder selects the SDK itself and asks for the matching one.
                .append("  platforms = info['CFBundleSupportedPlatforms']\n")
                .append("  if platforms.is_a?(Array)\n")
                .append("    names = platforms.map { |p| p.to_s.downcase }\n")
                .append("    if names.any? { |p| p.start_with?('watch') }\n")
                .append("      return names.include?('watchos') || "
                        + "names.include?('watchsimulator')\n")
                .append("    end\n")
                .append("  end\n")
                // An .xcframework lists one entry per platform+variant: the device library has no
                // Variant, the simulator one carries Variant = 'simulator'. A bundle with only the
                // device library builds on hardware and breaks every simulator run.
                .append("  libs = info['AvailableLibraries']\n")
                .append("  return false unless libs.is_a?(Array)\n")
                .append("  watch = libs.select { |l| l.is_a?(Hash) && "
                        + "l['SupportedPlatform'].to_s.downcase.start_with?('watch') }\n")
                .append("  return false if watch.empty?\n")
                .append("  device = watch.any? { |l| "
                        + "l['SupportedPlatformVariant'].to_s.strip.empty? }\n")
                .append("  simulator = watch.any? { |l| "
                        + "l['SupportedPlatformVariant'].to_s.downcase == 'simulator' }\n")
                .append("  device || simulator\n")
                .append("end\n")
                .append("vendored_linked = false\n")
                .append("watch_sdks = ['watchos', 'watchsimulator'].map { |sdk| "
                        + "`xcrun --sdk #{sdk} --show-sdk-path 2>/dev/null`.strip }"
                        + ".reject { |dir| dir.empty? || !File.directory?(dir) }\n")
                // The frameworks watchOS does not have, named rather than discovered. Same list
                // ParparVM weak-links through -Doptional.frameworks, so the declaration that keeps
                // the iOS slice linking and the one that keeps the watch target's phase honest
                // cannot drift apart.
                // Downcased on both sides. IPhoneBuilder writes "JavascriptCore.framework" with a
                // lowercase s, which resolves only because macOS filesystems are case-insensitive;
                // an exact match against Apple's spelling would have missed it and linked a
                // framework watchOS does not have. Casing is not a meaningful difference here, so
                // it is not allowed to be one.
                .append("watch_unavailable = %w[")
                .append(WATCH_OPTIONAL_FRAMEWORKS.toLowerCase().replace(';', ' '))
                .append("]\n")
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
                // .a is included, and then judged on what is actually in the archive.
                //
                // Skipping every static library was wrong for the developer's own: an ios.add_libs
                // archive built with a watchOS slice links perfectly well, and omitting it left the
                // watch target compiling the caller and failing on its symbols. CocoaPods'
                // libPods-*.a is still excluded outright above -- it is generated for the iOS
                // target and never has one.
                .append("  next unless base.end_with?('.framework') "
                        + "|| base.end_with?('.xcframework') || base.end_with?('.dylib') "
                        + "|| base.end_with?('.tbd') || base.end_with?('.a')\n")
                .append("  if base.end_with?('.framework') || base.end_with?('.xcframework')\n")
                .append("    if ref.source_tree == 'SDKROOT'\n")
                // DECLARED, not probed. WATCH_OPTIONAL_FRAMEWORKS already names every framework
                // the port links that watchOS does not have, and the port's own sources are
                // #ifdef'd around them -- that is the same conditional-system-library arrangement
                // every other platform uses here.
                //
                // Probing the SDK directories instead asked a question that has no single answer:
                // a build targets ONE destination, so requiring presence in both watch SDKs
                // dropped a device-only framework like BackgroundTasks from a device archive, and
                // requiring it in only the active one makes the project depend on which
                // destination generated it. The declaration has neither problem and is reviewable.
                .append("      present = !watch_unavailable.include?(base.downcase)\n")
                .append("    else\n")
                // Vendored: the bundle's own declaration decides, and a yes means the search paths
                // and any embed phase have to follow it below.
                .append("      present = cn1_watch_bundle_supports_watchos(ref)\n")
                .append("      vendored_linked ||= present\n")
                .append("    end\n")
                .append("  elsif base.end_with?('.a')\n")
                .append("    present = cn1_watch_archive_has_watch_slice(ref)\n")
                .append("    vendored_linked ||= present\n")
                .append("  elsif ref.source_tree == 'SDKROOT'\n")
                // A dylib or text-based stub the SDK vends: declared, like the frameworks above,
                // and downcased for the same reason.
                .append("    present = !watch_unavailable.include?(base.downcase)\n")
                .append("  else\n")
                // A VENDORED one, where the declaration says nothing -- it lists what the port
                // links, not what a developer dropped into the project. The same distinction the
                // .framework branch above draws, which this branch was missing: it accepted any
                // raw library whose basename happened not to be in the list, so an iOS-only dylib
                // was weak-linked into the watch target, and weak linkage does not save a
                // platform mismatch at link time.
                .append("    if base.end_with?('.dylib')\n")
                // Mach-O, so the slice test the static archives use answers this too.
                .append("      present = cn1_watch_archive_has_watch_slice(ref)\n")
                .append("    else\n")
                // A .tbd is text: it names its platforms, so read them. TBD v4 lists
                // `targets: [ arm64-ios, ... ]`, v2 and v3 a `platform:` line.
                .append("      present = cn1_watch_tbd_supports_watchos(ref)\n")
                .append("    end\n")
                .append("    vendored_linked ||= present\n")
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
        // Imports the Swift compiler will not see do not make a package a watch dependency.
        //
        // A staged source guarding an iOS-only package with `#if os(iOS) import PhoneSDK #endif`
        // still contains the word `import PhoneSDK`, and a raw text match read that as a watch
        // dependency -- attaching a package that intentionally supports only iOS and breaking
        // watchOS resolution over code the compiler excludes.
        //
        // Only a condition that is DEMONSTRABLY not watchOS is dropped: an os() test naming other
        // platforms, or one negating watchOS. Anything else -- a custom flag, a compiler-version
        // test, an expression this cannot evaluate -- is kept, because dropping an import the watch
        // does need is the worse failure of the two and the one that produces no explanation.
        // Comments first: an import the compiler never sees is not a dependency.
        //
        // A documentation example or a commented-out line -- `// import PhoneSDK`, or an ObjC
        // `/* #import <PhoneSDK/PhoneSDK.h> */` -- still contains the words the import regexes look
        // for, and an iOS-only product named in one was mirrored onto the watch target over code
        // that does not exist. Block comments are removed wholesale and a line comment to the end of
        // its line, which is where an import can legally appear and a comment cannot hide anything
        // else that matters here.
        s.append("def cn1_watch_strip_comments(src)\n")
                .append("  src.gsub(/\\/\\*.*?\\*\\//m, ' ').gsub(/\\/\\/[^\\n]*/, '')\n")
                .append("end\n");

        s.append("def cn1_watch_strip_non_watch(src)\n")
                .append("  src = cn1_watch_strip_comments(src)\n")
                .append("  out = []\n")
                // A STACK per level, and two facts about each: whether this arm is suppressed, and
                // whether an arm that positively applies to watchOS has already been taken. The
                // second is what makes `#if os(watchOS) ... #else ... #endif` drop its else -- a
                // single "am I suppressed" flag only ever switched arms when the FIRST one was
                // suppressed, so both arms of a watch-first branch were kept and an iOS-only
                // package imported by the else was mirrored onto the watch.
                .append("  suppressed = []\n")
                .append("  decided = []\n")
                .append("  src.each_line do |line|\n")
                .append("    t = line.strip\n")
                .append("    if t.start_with?('#if')\n")
                .append("      if cn1_watch_excludes_watch(t)\n")
                .append("        suppressed << true; decided << false\n")
                .append("      elsif cn1_watch_selects_watch(t)\n")
                .append("        suppressed << false; decided << true\n")
                .append("      else\n")
                // Unevaluatable -- a custom flag, an ObjC macro, a compiler-version test. Both arms
                // are kept, because guessing either way risks dropping an import the watch needs.
                .append("        suppressed << false; decided << false\n")
                .append("      end\n")
                .append("      next\n")
                // `#elif` is the C spelling and `#elseif` the Swift one. Reading only the Swift
                // form left a suppressed first arm suppressed straight through the watch arm of an
                // Objective-C `#if TARGET_OS_IOS ... #elif TARGET_OS_WATCH ... #endif`, so a
                // package imported only there was classified unused.
                .append("    elsif t.start_with?('#elseif') || t.start_with?('#elif') "
                        + "|| t.start_with?('#else')\n")
                .append("      next if suppressed.empty?\n")
                .append("      i = suppressed.length - 1\n")
                .append("      if decided[i]\n")
                .append("        suppressed[i] = true\n")
                .append("      elsif cn1_watch_excludes_watch(t)\n")
                .append("        suppressed[i] = true\n")
                .append("      else\n")
                .append("        suppressed[i] = false\n")
                // ONLY a demonstrably watchOS arm closes the branch. Marking an unevaluable
                // `#elseif FEATURE_B` as decided suppressed the `#else` behind it -- and when both
                // flags are off that else is the arm the watch compiles, so its import was dropped
                // and the target failed on a missing module.
                .append("        decided[i] = true if cn1_watch_selects_watch(t)\n")
                .append("      end\n")
                .append("      next\n")
                .append("    elsif t.start_with?('#endif')\n")
                .append("      suppressed.pop; decided.pop\n")
                .append("      next\n")
                .append("    end\n")
                .append("    out << line unless suppressed.any?\n")
                .append("  end\n")
                .append("  out.join\n")
                .append("end\n")
                // Parentheses that change nothing, removed before anything reads the expression.
                //
                // `#if (os(watchOS))` is as valid and as common as the bare spelling, and the
                // tests below are written against the bare one. The parenthesized arm was
                // therefore not recognized as selected, its `#else` survived, and an
                // `import PhoneSDK` meant only for the phone was mirrored into the watch target --
                // where an iOS-only product breaks watchOS package resolution outright.
                //
                // Only REDUNDANT parentheses go: a pair enclosing the whole expression, and a pair
                // around a single atom. `(os(iOS)) || FEATURE` keeps its structure, because the
                // balance check refuses to strip a pair that is not in fact enclosing.
                .append("def cn1_watch_balanced(s)\n")
                .append("  depth = 0\n")
                .append("  s.each_char do |ch|\n")
                .append("    depth += 1 if ch == '('\n")
                .append("    depth -= 1 if ch == ')'\n")
                .append("    return false if depth < 0\n")
                .append("  end\n")
                .append("  depth == 0\n")
                .append("end\n")
                .append("def cn1_watch_normalize_condition(condition)\n")
                .append("  c = condition.to_s.sub("
                        + "/\\A#\\s*(ifdef|ifndef|elseif|elif|if)\\b/, '').strip\n")
                .append("  loop do\n")
                .append("    before = c\n")
                .append("    if c =~ /\\A\\((.*)\\)\\z/m && cn1_watch_balanced($1)\n")
                .append("      c = $1.strip\n")
                .append("    end\n")
                .append("    c = c.gsub(/\\(\\s*(!?\\s*os\\(\\s*[A-Za-z0-9_]+\\s*\\))\\s*\\)/) "
                        + "{ $1.gsub(/\\s+/, '') }\n")
                .append("    c = c.gsub(/\\(\\s*(!?\\s*TARGET_OS_[A-Za-z0-9_]+)\\s*\\)/) "
                        + "{ $1.gsub(/\\s+/, '') }\n")
                .append("    break if c == before\n")
                .append("  end\n")
                .append("  c\n")
                .append("end\n")
                .append("def cn1_watch_excludes_watch(condition)\n")
                // Whitespace after a `!` is legal and would otherwise hide the negation. The
                // directive keyword is kept in front, because the two tests below are about the
                // directive itself and normalizing removes it.
                .append("  directive = condition.to_s[/\\A#\\s*\\w+/].to_s\n")
                .append("  c = (directive + ' ' + cn1_watch_normalize_condition(condition))"
                        + ".strip.gsub(/!\\s+/, '!')\n")
                // A NEGATED GROUP inverts every platform test inside it, and the scans below all
                // read a positive mention of another platform as proof the watch is excluded.
                // `#if !(os(iOS) || os(macOS))` is TRUE on watchOS -- that arm is precisely the one
                // the watch compiles -- so answering from the os(iOS) inside it dropped the arm and
                // whatever it imports. Left undecidable, which keeps the arm; the fall-through
                // below makes the rest of the function reachable for these, so it has to be said
                // here rather than relied on by accident.
                .append("  return false if c.include?('!(')\n")
                // A DISJUNCTION is true on the watch if ANY operand is, so one os() or TARGET_OS_
                // test on one side of an || proves nothing on its own. A conjunction is safe:
                // `os(iOS) && FEATURE` is false on the watch whatever FEATURE is.
                //
                // But every operand rejecting the watch settles it. `#if os(iOS) || os(macOS)` is
                // false on watchOS, so the compiler excludes that arm -- and returning "cannot
                // tell" kept the import inside it, which mirrored an iOS/macOS-only package into
                // the watch target and could fail its dependency resolution over source the watch
                // never compiles. Asked recursively, so a nested disjunction answers the same way.
                //
                // A single top-level operand FALLS THROUGH rather than answering "cannot tell":
                // in `(os(iOS) || os(macOS)) && FEATURE` the disjunction is parenthesized, so the
                // top-level split by `||` yields one operand and returning here settled the whole
                // condition before the conjunction rule below could see that the parenthesized
                // group is false on the watch. That kept an arm the compiler drops and mirrored
                // its iOS/macOS-only package into the watch target.
                .append("  if c.include?('||')\n")
                .append("    parts = cn1_watch_or_operands("
                        + "c.sub(/\\A#\\s*\\w+/, '').strip)\n")
                .append("    if parts.length > 1\n")
                .append("      return parts.all? { |o| "
                        + "cn1_watch_excludes_watch('#if ' + o.strip) }\n")
                .append("    end\n")
                .append("  end\n")
                // Objective-C guards its platforms with TargetConditionals macros, not Swift's
                // os() expressions, and `#if !TARGET_OS_WATCH` around a phone-only @import is the
                // standard spelling. Treating it as unevaluable kept the import, which attached an
                // iOS-only package to the watch target over code the compiler excludes.
                // A DEFINEDNESS test is not a platform test. TargetConditionals defines every one
                // of these macros on every platform -- as 0 or 1 -- so `#ifdef TARGET_OS_IOS` and
                // `#if defined(TARGET_OS_IOS)` are both TRUE on watchOS and the branch compiles
                // there. Reading them as iOS-only removed an arm the watch does build and dropped
                // the package imported inside it.
                .append("  return false if c =~ /\\A#(ifdef|ifndef)\\b/ || c.include?('defined(')\n")
                // `== 0` and `!= 1` say the same thing as the unary `!`, and the reading already
                // exists a few lines down for the OTHER platforms -- where it means the opposite,
                // because there a false test is the arm the watch DOES compile. Recognizing only
                // the unary spelling here let `#if TARGET_OS_WATCH == 0` read as a positive watch
                // test, so an arm that cannot compile on the watch was kept and the iOS-only
                // package it imports was mirrored into the watch target.
                .append("  return true if c =~ /!TARGET_OS_WATCH\\b/"
                        + " || c =~ /\\bTARGET_OS_WATCH\\s*(==\\s*0|!=\\s*1)\\b/\n")
                .append("  unless c =~ /\\bTARGET_OS_WATCH\\b/\n")
                // NEGATED is the opposite answer. `!TARGET_OS_IOS` is TRUE on watchOS, so that arm
                // is the one the watch compiles -- suppressing it dropped a package imported only
                // there. Only a POSITIVE test for another platform excludes the watch.
                //
                // TARGET_OS_IPHONE is absent from the list on purpose: it is 1 on watchOS, so a
                // block guarded by it does compile there.
                // `== 0` is the same statement as `!`. `#if TARGET_OS_IOS == 0` is TRUE on
                // watchOS, so that arm is the one the watch compiles -- reading the bare macro as
                // a positive iOS test suppressed it and dropped whatever it imported.
                .append("    unless c =~ /!TARGET_OS_(IOS|OSX|TV|MACCATALYST|VISION)\\b/"
                        + " || c =~ /\\bTARGET_OS_(IOS|OSX|TV|MACCATALYST|VISION)\\s*"
                        + "(==\\s*0|!=\\s*1)\\b/\n")
                .append("      return true if c =~ /\\bTARGET_OS_"
                        + "(IOS|OSX|TV|MACCATALYST|VISION)\\b/\n")
                .append("    end\n")
                .append("  end\n")
                .append("  return false unless c.include?('os(')\n")
                .append("  return true if c =~ /!\\s*os\\(\\s*watchOS\\s*\\)/\n")
                // A CONJUNCTION is false on the watch as soon as one operand is, so
                // `#if os(watchOS) && os(iOS)` is false everywhere -- nothing is both -- and that
                // arm never compiles, on the watch least of all. Returning "not excluded" on the
                // strength of os(watchOS) being present kept an unreachable arm and mirrored the
                // iOS-only package it imports into the watch target.
                //
                // The dual of the disjunction rule above: there EVERY operand had to exclude,
                // here ANY one does. Asked recursively, so each operand gets the whole test.
                .append("  conj = cn1_watch_or_operands(c.sub(/\\A#\\s*\\w+/, '').strip, '&')\n")
                .append("  if conj.length > 1\n")
                .append("    return true if conj.any? { |o| "
                        + "cn1_watch_excludes_watch('#if ' + o.strip) }\n")
                .append("  end\n")
                .append("  return false if c.include?('os(watchOS)')\n")
                // The same negation rule on the Swift side: `!os(iOS)` is true on the watch.
                .append("  return false if c =~ /!os\\(\\s*"
                        + "(iOS|macOS|tvOS|visionOS|Linux|Windows|Android)\\s*\\)/\n")
                .append("  c =~ /os\\(\\s*(iOS|macOS|tvOS|visionOS|Linux|Windows|Android)"
                        + "\\s*\\)/ ? true : false\n")
                .append("end\n")
                /// Positively naming watchOS AND nothing else, which is what lets the other arms be
                /// dropped.
                ///
                /// `os(watchOS) && FEATURE` mentions watchOS and is not therefore true: with FEATURE
                /// off Swift compiles the `#else`, and treating the first arm as selected suppressed
                /// that else and dropped a package imported only there. Selection is the only
                /// direction that can silence another arm, so it takes the whole expression being
                /// demonstrably true -- a bare watchOS test and nothing more.
                // One operand that is unconditionally true on the watch.
                .append("def cn1_watch_atom_selects_watch(atom)\n")
                .append("  a = atom.strip\n")
                .append("  return true if a =~ /\\Aos\\(\\s*watchOS\\s*\\)\\z/\n")
                // The Objective-C spelling of the same thing, bare or compared against a truth.
                // `== 1` and `!= 0` say exactly what the bare macro says, and accepting only the
                // bare form left `#if TARGET_OS_WATCH == 1` undecided -- so its `#else` survived
                // and the phone-only package in there was mirrored into the watch target. The
                // mirror of the `== 0` reading in cn1_watch_excludes_watch.
                //
                // Reached only for `#if`, never for `#ifdef`/`#ifndef` -- see the caller's guard.
                .append("  a =~ /\\ATARGET_OS_WATCH\\s*(==\\s*1|!=\\s*0)?\\z/ "
                        + "? true : false\n")
                .append("end\n")
                // Strips parentheses that wrap the WHOLE expression, so `(a || b)` splits into the
                // two operands it has rather than the one the depth counter sees. Balance-checked
                // as it goes, or `(a) && (b)` would be mistaken for a wrapped expression by its
                // first and last characters alone and lose its `&&`.
                .append("def cn1_watch_unwrap(expr)\n")
                .append("  s = expr.to_s.strip\n")
                .append("  while s.length > 1 && s[0, 1] == '(' && s[-1, 1] == ')'\n")
                .append("    depth = 0\n")
                .append("    wraps = true\n")
                .append("    i = 0\n")
                .append("    while i < s.length\n")
                .append("      ch = s[i, 1]\n")
                .append("      depth += 1 if ch == '('\n")
                .append("      depth -= 1 if ch == ')'\n")
                .append("      if depth == 0 && i < s.length - 1\n")
                .append("        wraps = false\n")
                .append("        break\n")
                .append("      end\n")
                .append("      i += 1\n")
                .append("    end\n")
                .append("    break unless wraps\n")
                .append("    s = s[1, s.length - 2].strip\n")
                .append("  end\n")
                .append("  s\n")
                .append("end\n")
                // Splits on `||` at the TOP level only, so a disjunction nested inside another
                // operand is left as the single operand it is -- but see cn1_watch_unwrap: a pair
                // of parentheses around the entire expression is not nesting, it is punctuation.
                .append("def cn1_watch_or_operands(expr, op = '|')\n")
                .append("  expr = cn1_watch_unwrap(expr)\n")
                .append("  parts = []\n")
                .append("  depth = 0\n")
                .append("  current = ''\n")
                .append("  i = 0\n")
                .append("  while i < expr.length\n")
                .append("    ch = expr[i, 1]\n")
                .append("    if ch == '('\n")
                .append("      depth += 1\n")
                .append("    elsif ch == ')'\n")
                .append("      depth -= 1\n")
                .append("    elsif depth == 0 && ch == op && expr[i + 1, 1] == op\n")
                .append("      parts << current\n")
                .append("      current = ''\n")
                .append("      i += 2\n")
                .append("      next\n")
                .append("    end\n")
                .append("    current += ch\n")
                .append("    i += 1\n")
                .append("  end\n")
                .append("  parts << current\n")
                .append("  parts\n")
                .append("end\n")
                .append("def cn1_watch_selects_watch(condition)\n")
                // A DEFINEDNESS test never selects, and the directive is the only thing that says
                // so -- which is why it is read here, before normalization strips it.
                //
                // TargetConditionals defines every one of these macros on every platform, as 0 or
                // 1. `#ifdef TARGET_OS_WATCH` is therefore true everywhere and says nothing about
                // the platform, and `#ifndef TARGET_OS_WATCH` is FALSE on the watch -- the arm the
                // watch compiles is the `#else`. Normalizing first left the bare macro behind and
                // read the ifndef as a positive watch test, so the watch arm was suppressed and a
                // package imported only there went missing from the target.
                .append("  raw = condition.to_s\n")
                .append("  return false if raw =~ /\\A#\\s*(ifdef|ifndef)\\b/ "
                        + "|| raw.include?('defined(')\n")
                .append("  bare = cn1_watch_normalize_condition(raw)\n")
                .append("  return true if cn1_watch_atom_selects_watch(bare)\n")
                // A DISJUNCTION with a watchOS operand is unconditionally true on the watch,
                // whatever the other operand does. `#if os(watchOS) || FEATURE` left the branch
                // undecided, so its `#else` survived and the iOS-only product imported there was
                // mirrored into the watch target.
                //
                // Not the mirror of the exclusion rule, which rejects disjunctions: there, one
                // operand being false proves nothing, because the other may still be true. Here
                // one operand being TRUE settles the whole expression.
                .append("  operands = cn1_watch_or_operands(bare)\n")
                .append("  return false if operands.length < 2\n")
                // Each operand normalized in turn, so `(os(watchOS)) || FEATURE` is recognized on
                // the same terms as the unparenthesized spelling.
                .append("  operands.any? { |o| cn1_watch_atom_selects_watch("
                        + "cn1_watch_normalize_condition(o)) }\n")
                .append("end\n");

        // Mirrored only when the WATCH sources actually import the product.
        //
        // No SDK check is possible here, unlike the system frameworks above: a package declares its
        // supported platforms in its own Package.swift, which xcodebuild does not resolve until
        // long after this runs. Copying the phone's whole dependency set across therefore made
        // Xcode resolve and build every one of them for watchOS -- so an iOS-only package used
        // solely by the phone broke the watch build outright, for code the watch never references.
        //
        // The staged watch tree is the evidence that IS available. It is the complete set of
        // sources this target compiles, so a product no file in it imports cannot be needed, and a
        // product one of them does import is needed whatever the phone uses.
        //
        // Matched on MODULE BOUNDARIES, not as a substring. A raw include? made a product named Foo
        // look used by a source importing FooBar, which mirrored an unrelated iOS-only package onto
        // the watch and broke the build in the exact way this gate exists to prevent. The name must
        // be followed by end-of-token: a newline, a dot (Foo.Bar), a semicolon, or the closing
        // bracket of an Objective-C import.
        //
        // Swift's declaration-scoped form counts too. `import struct Foo.Bar` names the module Foo
        // just as `import Foo` does, and reading only the unqualified form dropped a product the
        // source genuinely needs -- the opposite failure, and the worse one, because it breaks a
        // build that should work.
        //
        // If a watch source needs a product under a module name that differs from the product name
        // -- legal, and rare -- the skip is logged with the name, so the link error that follows
        // has something in the build output pointing at it.
        s.append("watch_import_src = ")
                .append(watchSources.isEmpty() ? "nil\n"
                        : "File.join(File.dirname(project_file), watch_group_path)\n")
                .append("watch_import_text = nil\n")
                .append("if watch_import_src && File.directory?(watch_import_src)\n")
                .append("  watch_import_text = ''\n")
                .append("  Dir.glob(File.join(watch_import_src, '**', '*.{h,m,mm,c,cpp,cc,swift}'))"
                        + ".each do |f|\n")
                .append("    begin\n")
                .append("      watch_import_text << cn1_watch_strip_non_watch(File.read(f))\n")
                .append("    rescue StandardError\n")
                .append("      next\n")
                .append("    end\n")
                .append("  end\n")
                .append("end\n")
        // A product name is not always its module name.
                //
                // A package may export product FooKit containing target Foo, and staged code then
                // says `import Foo` -- which matches no product, so the strict gate concluded FooKit
                // was unused and the watch failed to compile against a module it does import. The
                // mapping lives in the package's own Package.swift, which xcodebuild does not
                // resolve until long after this runs, so it cannot be looked up here.
                //
                // What CAN be established is whether the assumption holds for this project: every
                // module the watch sources import is either a product name or a framework in the
                // watchOS SDK. When that is true the gate is exact and stays on. When some import
                // is attributable to neither, product and module names demonstrably differ here, the
                // gate cannot decide, and it steps aside -- mirroring everything, and saying so.
                // A package that then fails on watchOS is named by Xcode; a module silently withheld
                // is not.
                .append("watch_modules = []\n")
                .append("if watch_import_text\n")
                .append("  watch_import_text.scan(/^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                        + "(?:(?:public|package|internal|fileprivate|private)\\s+)?import\\s+"
                        + "(?:typealias|struct|class|enum|protocol|let|var|func)?\\s*"
                        + "([A-Za-z_]\\w*)/) { |m| watch_modules << m[0] }\n")
                .append("  watch_import_text.scan(/@import\\s+([A-Za-z_]\\w*)\\s*;/) "
                        + "{ |m| watch_modules << m[0] }\n")
                .append("  watch_import_text.scan(/[<\\\"]([A-Za-z_]\\w*)\\//) "
                        + "{ |m| watch_modules << m[0] }\n")
                .append("  watch_modules.uniq!\n")
                .append("end\n")
                .append("product_names = app_target.package_product_dependencies.to_a"
                        + ".map { |d| d.respond_to?(:product_name) ? d.product_name : nil }"
                        + ".compact\n")
                // A module the SDK itself vends is attributed, not unknown.
                //
                // Only .framework directories were checked, so a staged source importing an SDK
                // module that is NOT a framework -- Darwin, Dispatch, ObjectiveC, the Swift
                // standard library -- matched nothing and was called unattributed. That switches
                // strict filtering off wholesale, and then an iOS-only package the phone happens
                // to carry is mirrored into the watch target and can break its package
                // resolution, over an import the watch code never made of anything shippable.
                // Read from the SDK's module maps rather than guessed at by path.
                //
                // Probing usr/lib/swift/<M>.swiftmodule and usr/include/<M> found the Swift
                // overlays -- Darwin, Dispatch, ObjectiveC all have one -- and missed almost
                // everything else. Apple declares its C modules in shared maps, so of the 78
                // modules the watchOS SDK's usr/include/module.modulemap names, 66 matched no
                // path: SQLite3, zlib, MachO, notify, os_object among them. Any staged source
                // importing one was called unattributed, which switches strict filtering off and
                // mirrors every phone package product into the watch target.
                //
                // Parsed once and memoised: ~2800 names in about a second, measured against the
                // watchOS 26.2 SDKs, most of it the toolchain glob rather than the 45 map files.
                // Once per build, against an Xcode build measured in minutes.
                .append("def cn1_watch_sdk_module_names(sdks)\n")
                .append("  $cn1_watch_sdk_modules ||= begin\n")
                .append("    found = {}\n")
                // The Swift standard modules are built into the compiler and have no map entry.
                .append("    %w[Swift _Concurrency _StringProcessing Builtin]"
                        + ".each { |n| found[n] = true }\n")
                .append("    sdks.each do |sdk|\n")
                .append("      Dir.glob(File.join(sdk, 'usr/include/**/*.modulemap')).each do |mm|\n")
                .append("        begin\n")
                // `module X`, and the extern/explicit/framework spellings in front of it.
                .append("          File.read(mm).scan("
                        + "/^\\s*(?:extern\\s+|explicit\\s+|framework\\s+)*module"
                        + "\\s+([A-Za-z_]\\w*)/) { |n| found[n[0]] = true }\n")
                .append("        rescue StandardError\n")
                // An unreadable map costs its own modules, not the whole answer.
                .append("        end\n")
                .append("      end\n")
                .append("      Dir.glob(File.join(sdk, 'usr/lib/swift/*.swiftmodule')).each "
                        + "{ |d| found[File.basename(d, '.swiftmodule')] = true }\n")
                .append("      Dir.glob(File.join(sdk, 'usr/lib/swift/*.swiftinterface')).each "
                        + "{ |d| found[File.basename(d, '.swiftinterface')] = true }\n")
                // A framework IS a module: `import WatchKit` resolves to one, and the SPM
                // attribution below needs it named here rather than probed separately.
                .append("      ['System/Library/Frameworks', 'System/Library/SubFrameworks']"
                        + ".each do |rel|\n")
                .append("        Dir.glob(File.join(sdk, rel, '*.framework')).each "
                        + "{ |d| found[File.basename(d, '.framework')] = true }\n")
                .append("      end\n")
                .append("    end\n")
                // Swift overlays the toolchain ships rather than the SDK, wherever it keeps them.
                .append("    toolchain = `xcrun --find swift 2>/dev/null`.strip\n")
                .append("    unless toolchain.empty?\n")
                .append("      root = File.expand_path('../..', toolchain)\n")
                .append("      Dir.glob(File.join(root, 'lib/swift/**/*.swiftmodule')).each "
                        + "{ |d| found[File.basename(d, '.swiftmodule')] = true }\n")
                .append("    end\n")
                .append("    found\n")
                .append("  end\n")
                .append("end\n")
                .append("def cn1_watch_sdk_provides_module(sdks, m)\n")
                .append("  cn1_watch_sdk_module_names(sdks).key?(m)\n")
                .append("end\n")
                .append("unattributed = watch_modules.reject { |m| product_names.include?(m) || "
                        + "cn1_watch_sdk_provides_module(watch_sdks, m) }\n")
                .append("strict_products = watch_import_text && unattributed.empty?\n")
                .append("if watch_import_text && !unattributed.empty?\n")
                .append("  puts \"[watchNative] linking every Swift package product into the watch "
                        + "target: #{unattributed.join(' ')} imported by the staged watch sources "
                        + "matches no product name, so a product's module name differs from it and "
                        + "the per-product check cannot decide\"\n")
                .append("end\n")
                .append("app_target.package_product_dependencies.to_a.each do |dep|\n")
                .append("  name = dep.respond_to?(:product_name) ? dep.product_name : nil\n")
                .append("  next unless name\n")
                // watch_import_text is nil when the watch shares the phone's translation, and then
                // the watch compiles the phone's sources and needs the phone's packages.
                .append("  if strict_products\n")
                .append("    q = Regexp.escape(name)\n")
                // Swift: `import Foo`, `import Foo.Bar`, the declaration-scoped
                // `import struct Foo.Bar`, and whatever decorates the line in front of it.
                //
                // ANY attribute, not a hard-coded @_exported: @preconcurrency,
                // @_implementationOnly, @_spi(Name) and the rest are all valid there, and a
                // pattern naming one of them classified `@preconcurrency import Foo` as unused and
                // dropped a module the source cannot compile without. Same for the access-level
                // modifiers Swift now allows on an import.
                // Objective-C / C: `#import <Foo/Foo.h>`, `#import "Foo/Foo.h"`, `@import Foo;`.
                .append("    swift_import = /^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                        + "(?:(?:public|package|internal|fileprivate|private)\\s+)?"
                        + "import\\s+"
                        + "(?:typealias|struct|class|enum|protocol|let|var|func)?\\s*"
                        + "#{q}(?:\\.|\\s|$)/\n")
                .append("    objc_import = /(?:@import\\s+#{q}\\s*;|[<\\\"]#{q}\\/)/\n")
                .append("    used = !(watch_import_text =~ swift_import).nil? || "
                        + "!(watch_import_text =~ objc_import).nil?\n")
                .append("    unless used\n")
                .append("      puts \"[watchNative] not linking Swift package product #{name} into "
                        + "the watch target: no staged watch source imports it\"\n")
                .append("      next\n")
                .append("    end\n")
                .append("  end\n")
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
        // An asset catalog is judged by what is IN it, not by its extension.
        //
        // Dropping every .xcassets was too broad: a project keeping its images in a custom catalog
        // had none of them on the watch, so UIImage(named:) from watch-reachable code returned nil
        // at runtime with nothing in the build to say why. Only a catalog carrying an app icon or a
        // launch image is iOS-specific -- those are the sets with no watch-applicable content --
        // and a catalog holding ordinary image and colour sets compiles for watchOS like any other.
        //
        // Nor is a catalog all one thing. The standard Assets.xcassets holds the app icon AND the
        // project's ordinary image and colour sets, so answering "does it contain an icon set"
        // skipped the whole catalog and took every compatible asset with it -- the same runtime
        // nil from watch-reachable UIImage(named:) that dropping all .xcassets caused, reached by
        // a narrower route. What is iOS-specific is the individual set, not its container, so a
        // mixed catalog is staged as a copy with the incompatible sets removed and the watch gets
        // that. Set names are what lookups use and they are unchanged by the copy, so the same
        // UIImage(named:) resolves.
        // A staging directory name that is unique per SOURCE catalog, and still recognizable.
        s.append("def cn1_watch_catalog_stage_name(path)\n")
                .append("  require 'digest'\n")
                .append("  \"#{File.basename(path, '.xcassets')}-"
                        + "#{Digest::MD5.hexdigest(path)[0, 8]}.xcassets\"\n")
                .append("end\n");

        s.append("def cn1_watch_catalog_for_watch(ref, staging)\n")
                .append("  path = (ref.real_path.to_s rescue nil)\n")
                // Unreadable: keep the old conservative answer. A missing asset is a runtime nil,
                // but a catalog that cannot be inspected might be the app's own icon set, and that
                // is a build error for everybody.
                .append("  return nil unless path && File.directory?(path)\n")
                .append("  incompatible = Dir.glob("
                        + "File.join(path, '**', '*.{appiconset,launchimage}'))\n")
                // The common case, and it costs nothing: no icon or launch set means the catalog
                // is compatible as it stands, and the watch shares the reference.
                .append("  return path if incompatible.empty?\n")
                // A catalog that is ONLY the app icon has nothing to preserve, so the old skip is
                // still the right answer -- and it avoids staging an empty catalog.
                .append("  usable = Dir.glob(File.join(path, '**', '*')).reject do |e|\n")
                .append("    !File.directory?(e) || e =~ /\\.(appiconset|launchimage)(\\/|\\z)/ || "
                        + "File.extname(e).empty?\n")
                .append("  end\n")
                .append("  return nil if usable.empty?\n")
                // Keyed by the catalog's own path, not its basename. Two brands each keeping a
                // BrandA/Assets.xcassets and a BrandB/Assets.xcassets staged to the same
                // destination: the second wiped and replaced the first, and BOTH watch references
                // then pointed at that one directory, so the first brand's images silently
                // vanished at runtime. The digest keeps the readable name and makes it unique.
                .append("  dest = File.join(staging, "
                        + "cn1_watch_catalog_stage_name(path))\n")
                .append("  begin\n")
                .append("    FileUtils.rm_rf(dest)\n")
                .append("    FileUtils.mkdir_p(File.dirname(dest))\n")
                .append("    FileUtils.cp_r(path, dest)\n")
                .append("    Dir.glob(File.join(dest, '**', '*.{appiconset,launchimage}'))"
                        + ".each { |d| FileUtils.rm_rf(d) }\n")
                .append("  rescue StandardError => e\n")
                // Staging failed: fall back to the conservative skip rather than referencing a
                // half-written catalog, which fails the build for everybody instead of one image.
                .append("    puts \"[watchNative] could not stage a watch copy of "
                        + "#{File.basename(path)} (#{e}); leaving it out of the watch target\"\n")
                .append("    return nil\n")
                .append("  end\n")
                .append("  dest\n")
                .append("end\n");

        // The watch's own catalog, carrying the icon it cannot take from the phone. Added before
        // the phone's resources are walked so a project with no catalog at all still gets one.
        // Under <Main>-src, where writeWatchAppIcon puts it -- the same convention the watch
        // Info.plist and the staged watch sources use. The project itself sits one level up, in
        // dist, so a reference relative to the project directory alone pointed at nothing: the
        // directory test failed silently and the catalog was never added to the watch's resources,
        // leaving ASSETCATALOG_COMPILER_APPICON_NAME naming a set that was not in the target.
        s.append("watch_icon_rel = '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + WATCH_ICON_CATALOG))
                .append("'\n")
                .append("watch_icon_dir = File.join(File.dirname(xcproj.path.to_s), "
                        + "watch_icon_rel)\n")
                .append("if File.directory?(watch_icon_dir)\n")
                .append("  icon_ref = xcproj.main_group.new_reference(watch_icon_rel)\n")
                .append("  unless watch_target.resources_build_phase.files_references"
                        + ".include?(icon_ref)\n")
                .append("    watch_target.resources_build_phase.add_file_reference(icon_ref)\n")
                .append("  end\n")
                .append("end\n");

        s.append("res_skip = %w[.storyboard .xib]\n")
                .append("proj_dir = File.dirname(xcproj.path.to_s)\n")
                .append("watch_asset_staging = File.join(proj_dir, '"
                        + IPhoneBuilder.escapeRubyStr(WATCH_ASSET_STAGING_DIR) + "')\n")
                .append("app_target.resources_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  next if res_skip.any? { |ext| ref.path.to_s.end_with?(ext) }\n")
                .append("  watch_ref = ref\n")
                .append("  if ref.path.to_s.end_with?('.xcassets')\n")
                .append("    use = cn1_watch_catalog_for_watch(ref, watch_asset_staging)\n")
                .append("    if use.nil?\n")
                .append("      puts \"[watchNative] not copying #{File.basename(ref.path.to_s)} "
                        + "into the watch target: it carries nothing but an app icon or launch "
                        + "image, which has no watch-applicable content\"\n")
                .append("      next\n")
                .append("    end\n")
                // A staged copy is a NEW reference: the original still belongs to the phone with
                // its icon set intact, and only the watch sees the filtered one.
                .append("    if use != (ref.real_path.to_s rescue nil)\n")
                .append("      puts \"[watchNative] staging #{File.basename(ref.path.to_s)} for "
                        + "the watch without its app icon or launch image; the phone keeps the "
                        + "original\"\n")
                .append("      rel = use.sub(/\\A#{Regexp.escape(proj_dir)}\\/?/, '')\n")
                .append("      watch_ref = xcproj.main_group.new_reference(rel)\n")
                .append("    end\n")
                .append("  end\n")
                .append("  unless watch_target.resources_build_phase.files_references"
                        + ".include?(watch_ref)\n")
                .append("    watch_target.resources_build_phase.add_file_reference(watch_ref)\n")
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
            // The cloud builder archives this scheme itself now. Said out loud anyway, because
            // an ios-source build hands the developer the project rather than an artifact, and
            // which of the two schemes to archive is the one thing about a watch-only product
            // that is not obvious from looking at it.
            owner.log("[watchNative] codename1.watchStandalone builds " + watchTargetName
                    + " as a detached, installable product. A cloud build archives that scheme;"
                    + " from an ios-source build, archive " + watchTargetName + " rather than the"
                    + " phone scheme.");
        }
        return s.toString();
    }

    /**
     * Creates the watchOS widget-extension target and embeds it in the WATCH app.
     *
     * <p>A complication is a WidgetKit widget, so this is the same shape as the iOS extension
     * {@code IPhoneBuilder.appendWidgetExtensionRuby} builds -- an app extension in the host's
     * {@code PlugIns/} folder -- with the watch app as the host instead of the phone app.</p>
     *
     * <p><b>Both distributions fall out of that choice.</b> Because the extension lives inside
     * the watch app rather than beside it, the companion case needs nothing extra: the iOS
     * app's "Embed Watch Content" phase copies the finished watch app with the .appex already
     * in it, and the platform filter that keeps the watch tree out of the Mac Catalyst slice
     * covers the extension for free. A standalone build removes that phase entirely and the
     * extension simply rides in the product. There is no branch here for either.</p>
     *
     * <p>The target type is {@code :app_extension}, not {@code :watch2_extension}. The latter
     * is the legacy paired WatchKit app extension -- the same trap as {@code :application} vs
     * {@code :watch2_app} above -- while a WidgetKit extension is a plain app extension on
     * every platform Apple ships it on.</p>
     */
    private void appendWidgetExtension(StringBuilder s, File tmpFile, String resolvedTeamId) {
        if (watchWidgetExtensionDir == null || !watchWidgetExtensionDir.isDirectory()) {
            return;
        }
        String extensionName = watchWidgetExtensionDir.getName();
        File distDir = new File(tmpFile, "dist");
        Map<String, String> buildSettings = new LinkedHashMap<String, String>();
        buildSettings.put("PRODUCT_NAME", "$(TARGET_NAME)");
        // The watch app is one bundle deeper than a phone app, and its PlugIns one deeper
        // still, so the runpath needs the extra level the phone extension does not.
        buildSettings.put("LD_RUNPATH_SEARCH_PATHS", "$(inherited) @executable_path/Frameworks "
                + "@executable_path/../../Frameworks @executable_path/../../../../Frameworks");
        buildSettings.put("CLANG_ENABLE_MODULES", "YES");
        File props = new File(watchWidgetExtensionDir, "buildSettings.properties");
        if (props.exists()) {
            Properties loaded = new Properties();
            FileInputStream in = null;
            try {
                in = new FileInputStream(props);
                loaded.load(in);
            } catch (IOException ex) {
                throw new BuildException("Failed to read " + props, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignore) {
                        // Nothing useful to do; the properties are already loaded or the read
                        // failed above.
                    }
                }
            }
            for (Object key : loaded.keySet()) {
                if (key instanceof String) {
                    buildSettings.put((String) key, loaded.getProperty((String) key));
                }
            }
            // Loaded, so it must not also be added to the Xcode group as a resource.
            props.delete();
        }
        if (resolvedTeamId != null && !resolvedTeamId.isEmpty()) {
            buildSettings.put("DEVELOPMENT_TEAM", resolvedTeamId);
        }
        String target = buildSettings.get("WATCHOS_DEPLOYMENT_TARGET");
        if (target == null || target.length() == 0) {
            target = IOSWidgetExtensionBuilder.WATCH_MIN_DEPLOYMENT_TARGET;
        }

        // Guarded so re-running the script (the build re-executes the schemes ruby after
        // dependency integration) does not duplicate the target.
        s.append("\nif xcproj.targets.find{|e| e.name=='")
                .append(IPhoneBuilder.escapeRubyStr(extensionName)).append("'}.nil?\n");
        s.append("  ext_target = xcproj.new_target(:app_extension, '")
                .append(IPhoneBuilder.escapeRubyStr(extensionName)).append("', :watchos, '")
                .append(IPhoneBuilder.escapeRubyStr(target)).append("')\n");
        s.append("  ext_group = xcproj.new_group('")
                .append(IPhoneBuilder.escapeRubyStr(extensionName)).append("')\n");
        IPhoneBuilder.appendFilesToXcodeProjGroup(s, watchWidgetExtensionDir,
                "ext_group", "ext_target", distDir);
        s.append("  watch_target.add_dependency(ext_target)\n");
        s.append("  ext_ref = xcproj.groups.find{|e| e.display_name=='Products'}.new_file('")
                .append(IPhoneBuilder.escapeRubyStr(extensionName))
                .append(".appex', \"BUILT_PRODUCTS_DIR\")\n");
        // "Embed Foundation Extensions" is what Xcode calls this phase on watchOS; the
        // destination is the same PlugIns folder (spec 13) the phone extension uses.
        s.append("  ext_embed = watch_target.copy_files_build_phases"
                + ".find{|p| p.name=='Embed Foundation Extensions'} || "
                + "watch_target.new_copy_files_build_phase('Embed Foundation Extensions')\n");
        s.append("  ext_embed.build_action_mask = \"2147483647\"\n");
        s.append("  ext_embed.dst_subfolder_spec = \"13\"\n");
        s.append("  ext_embed.run_only_for_deployment_postprocessing = \"0\"\n");
        s.append("  ext_embed.add_file_reference(ext_ref)\n");
        s.append("  ext_target.build_configurations.each{|e|\n");
        for (Map.Entry<String, String> e : buildSettings.entrySet()) {
            s.append("    e.build_settings['").append(IPhoneBuilder.escapeRubyStr(e.getKey()))
                    .append("'] = \"").append(IPhoneBuilder.escapeRubyStr(e.getValue()))
                    .append("\"\n");
        }
        s.append("  }\n");
        s.append("end\n");
    }

}
