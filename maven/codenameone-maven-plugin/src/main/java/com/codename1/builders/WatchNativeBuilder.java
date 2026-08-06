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
        teamId = request.getArg("ios.release.teamId",
                request.getArg("ios.teamId",
                        request.getArg("ios.debug.teamId", "")));
        displayName = request.getDisplayName() != null
                ? request.getDisplayName() : request.getMainClass();
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
        String mainStub = mangle(mainFqn) + "Stub";
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
          .append("extern void cn1_watch_runtime_pointerReleased(int x, int y);\n\n")
          .append("// App-specific entry: register natives + set the main class, init\n")
          .append("// Display (starts the EDT) and block this thread inside initVM.\n")
          .append("extern void ").append(mainStub)
          .append("_main___java_lang_String_1ARRAY(struct ThreadLocalData* threadStateData, JAVA_OBJECT arg);\n")
          .append("void cn1_watch_app_main(void) {\n")
          .append("    ").append(mainStub)
          .append("_main___java_lang_String_1ARRAY(getThreadLocalData(), JAVA_NULL);\n")
          .append("}\n\n")
          .append("// Watch lifecycle entry class (mangled FQN): ").append(m).append("\n")
          .append("void cn1_watch_bootstrap(void) { cn1_watch_runtime_start(\"")
          .append(IPhoneBuilder.escapeRubyStr(watchMain)).append("\"); }\n")
          .append("void cn1_watch_paintFrame(void) { cn1_watch_runtime_paint(); }\n")
          .append("void cn1_watch_pointerPressed(int x, int y) { cn1_watch_runtime_pointerPressed(x, y); }\n")
          .append("void cn1_watch_pointerDragged(int x, int y) { cn1_watch_runtime_pointerDragged(x, y); }\n")
          .append("void cn1_watch_pointerReleased(int x, int y) { cn1_watch_runtime_pointerReleased(x, y); }\n")
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
        int close = inject.indexOf("</string>", open);
        if (close < 0) {
            return null;
        }
        return inject.substring(open + "<string>".length(), close).trim();
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
        plistString(sb, "CFBundleVersion", injectedBundle != null ? injectedBundle
                : request.getArg("ios.bundleVersion", watchShort));
        // Modern single-target watch app marker.
        sb.append("    <key>WKApplication</key>\n    <true/>\n");
        if (!isStandalone()) {
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
        String healthShare = trimToNull(request.getArg(
                "ios.NSHealthShareUsageDescription", null));
        if (healthShare != null) {
            plistString(sb, "NSHealthShareUsageDescription", healthShare);
        }
        String healthUpdate = trimToNull(request.getArg(
                "ios.NSHealthUpdateUsageDescription", null));
        if (healthUpdate != null) {
            plistString(sb, "NSHealthUpdateUsageDescription", healthUpdate);
        }
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
        boolean watchHealth =
                watchUsesHealth(healthShare != null || healthUpdate != null);
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
    boolean watchUsesHealth(boolean phoneUsesHealth) {
        if ("true".equalsIgnoreCase(healthHint)) {
            return true;
        }
        if ("false".equalsIgnoreCase(healthHint)) {
            return false;
        }
        if ("true".equalsIgnoreCase(workoutProcessingHint)) {
            return true;
        }
        return phoneUsesHealth && !distinctWatchMain;
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
    private String watchEntitlementsSetting(BuildRequest request,
            String mainClass) {
        // The same gate the entitlements file itself uses. Pointing the
        // target at a file that is not written, or writing one the target
        // never signs with, are two different ways to be wrong.
        // Trimmed, exactly as writeWatchInfoPlist trims them. A raw null
        // check here saw a whitespace-only hint as health usage and
        // pointed CODE_SIGN_ENTITLEMENTS at an entitlements file the plist
        // pass had decided not to write, so Xcode failed on a missing
        // file.
        boolean phoneUsesHealth = trimToNull(request.getArg(
                "ios.NSHealthShareUsageDescription", null)) != null
                || trimToNull(request.getArg(
                        "ios.NSHealthUpdateUsageDescription", null)) != null;
        if (!watchUsesHealth(phoneUsesHealth)) {
            return "";
        }
        return "  bs['CODE_SIGN_ENTITLEMENTS'] = '"
                + IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + mainClass
                        + "-Watch.entitlements") + "'\n";
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
    void applyXcodeSettings(BuildRequest request, File tmpFile, String buildVersion)
            throws BuildException {
        File hooksDir = new File(tmpFile, "hooks");
        hooksDir.mkdir();
        File scriptFile = new File(hooksDir, "apply_watch_native_settings.rb");
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
                .append("excluded = %w[").append(excluded).append("]\n")
                .append("app_target.source_build_phase.files.to_a.each do |bf|\n")
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
                .append("end\n")
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
                .append("  bs['SDKROOT'] = 'watchos'\n")
                // arm64_32 is the watchOS *device* ABI; the watch *simulator*
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
                .append("  bs['GCC_PREFIX_HEADER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + mainClass + "-Prefix.pch")).append("'\n")
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
                .append("  bs['HEADER_SEARCH_PATHS[sdk=watchos*]'] = '$(inherited) $(SRCROOT)/")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("-src/watchOSStubs'\n")
                .append("  bs['HEADER_SEARCH_PATHS[sdk=watchsimulator*]'] = '$(inherited) $(SRCROOT)/")
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
        s.append("stub_name = '").append(phoneStubName).append(".m'\n")
                .append("watch_target.source_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path && File.basename(ref.path) == stub_name\n")
                .append("  bf.settings = { 'COMPILER_FLAGS' => '-Dmain=cn1_watch_phone_main_unused -Wno-error=return-type -Wno-return-type' }\n")
                .append("end\n");

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

        try {
            owner.createFile(scriptFile, s.toString().getBytes(StandardCharsets.UTF_8));
            owner.exec(hooksDir, "chmod", "0755", scriptFile.getAbsolutePath());
            if (!owner.exec(hooksDir, scriptFile.getAbsolutePath())) {
                throw new BuildException("Failed to apply watchNative Xcode settings via xcodeproj");
            }
            owner.log("[watchNative] Added watchOS target " + watchTargetName
                    + " (" + (isStandalone() ? "standalone" : "companion") + ", "
                    + "watchOS " + MIN_DEPLOYMENT_TARGET + ", arm64_32)");
            if (isStandalone()) {
                // Said out loud at build time. The target is detached from the phone app and
                // installable, but the archive step still selects the phone scheme, so the IPA this
                // build returns is the phone app -- and a developer who asked for a watch-only
                // product would otherwise discover that by inspecting the artifact.
                owner.log("[watchNative] NOTE: codename1.watchStandalone builds " + watchTargetName
                        + " as a detached, installable product, but this build archives the phone "
                        + "scheme. To submit the watch app, open the generated Xcode project and "
                        + "archive the " + watchTargetName + " scheme directly.");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to apply watchNative Xcode settings", ex);
        }
    }
}
