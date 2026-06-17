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
 * (watchOS) native build path. Activated by the build hint {@code
 * watchNative.enabled=true}.
 *
 * <p>Unlike {@link MacNativeBuilder} (which Mac-Catalyst-slices the SAME iOS app
 * target), a watchOS app is a distinct product: it has its own bundle, its own
 * {@code WKApplication} Info.plist, and the {@code arm64_32} architecture. So
 * this builder <b>adds a second Xcode target</b> to the generated project,
 * compiles the shared ParparVM-generated sources (minus the GL/Metal-only files)
 * for watchOS, and - in the default {@code companion} distribution - embeds the
 * watch app inside the iOS {@code .app} via an "Embed Watch Content" copy-files
 * phase. The watch UI is rendered by the Core Graphics backend
 * ({@code CN1CGGraphics} + {@code CN1WatchRenderingView}) driven by
 * {@code CN1WatchHost}.
 *
 * <p>The underlying mechanism is a Ruby {@code xcodeproj} script (same toolchain
 * macNative relies on). Like {@link MacNativeBuilder} this is a delegate owned
 * by {@link IPhoneBuilder}, invoked at hint-parse time and at the
 * post-project-generate patching point. Every change is additive: with the hint
 * off, the iOS build is byte-for-byte unchanged.
 */
class WatchNativeBuilder {
    private final IPhoneBuilder owner;

    // Parsed hints.
    private boolean enabled;
    private String distribution;       // companion | standalone
    private String bundleId;
    private String minDeploymentTarget; // WATCHOS_DEPLOYMENT_TARGET
    private String teamId;
    private String displayName;
    // Fully-qualified watch lifecycle entry class (codename1.watchMain). May
    // equal the phone main class; a distinct value lets the watch slice tree-
    // shake from its own root. Empty when neither watchMain nor an explicit
    // watchNative.mainClass hint is set (then we fall back to the phone main).
    private String watchMain;

    // GL/Metal-only source files with no watchOS substitute. Excluded from the
    // watch target; the CG backend (CN1CGGraphics/CN1WatchRenderingView) and the
    // per-op TARGET_OS_WATCH branches replace them. Kept in sync with
    // Ports/iOSPort/nativeSources/WATCHOS_PORT.md.
    private static final String[] EXCLUDED_WATCH_SOURCES = {
        "EAGLView.m", "METALView.m",
        "CN1ES1compat.m", "CN1ES2compat.m", "CN1GL3D.m",
        "CN1Metalcompat.m", "CN1MetalGlyphAtlas.m", "CN1MetalPipelineCache.m",
        "CN1MetalShaders.metal",
        "DrawGradientTextureCache.m", "DrawStringTextureCache.m",
        "CodenameOne_GLSceneDelegate.m",
        // The UIApplication delegate is UIApplication/UIApplicationMain based
        // (unavailable on watchOS) and is replaced by the SwiftUI @main shell
        // (CN1WatchApp.swift) / CN1WatchHost. CodenameOne_GLViewController.m is
        // NOT excluded: it carries the shared CGContext/op-based graphics
        // primitives (createImage, fonts, the *Impl drawing entry points) that
        // the watch slice reuses. Its UIViewController class + UIKit event code
        // are guarded with #if !TARGET_OS_WATCH, and the watch render-driver
        // class (CodenameOne_GLViewController as an NSObject) lives in
        // CN1WatchViewController.m.
        "CodenameOne_GLAppDelegate.m",
        // UIWebView-based legacy browser peer (UIWebView + UIApplication
        // networkActivityIndicator are unavailable on watchOS).
        "UIWebViewEventDelegate.m",
        // UIKit peer components unavailable on watchOS: tap gesture
        // (UIGestureRecognizer), inline text editors (UITextField/UITextView),
        // and the low-level AudioQueue recorder (AudioToolbox). Their headers
        // are empty under #if !TARGET_OS_WATCH so importers still compile.
        "CN1TapGestureRecognizer.m", "CN1UITextField.m", "CN1UITextView.m",
        "CN1AudioUnit.m",
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
            + "WebKit.framework;StoreKit.framework";

    WatchNativeBuilder(IPhoneBuilder owner) {
        this.owner = owner;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Parse the {@code watchNative.*} hint family. Caller flips Metal on (the
     * watch slice cannot use GL ES; the iOS slice still wants Metal) and raises
     * the watch deployment floor.
     */
    void parseHints(BuildRequest request) {
        // The watch slice auto-enables when the project declares a watchMain
        // entry point (codenameone_settings.properties -> codename1.watchMain),
        // so the double app is produced seamlessly as part of the regular iPhone
        // build. watchNative.enabled=true forces it on even without a distinct
        // watchMain (the watch then shares the phone main class).
        watchMain = request.getArg("watchMain",
                request.getArg("watchNative.mainClass", "")).trim();
        enabled = "true".equals(request.getArg("watchNative.enabled", "false"))
                || watchMain.length() > 0;
        if (!enabled) {
            return;
        }
        if (watchMain.length() == 0) {
            // No distinct watch entry: reuse the phone main class as the watch
            // lifecycle root.
            watchMain = request.getMainClass();
        }
        distribution = request.getArg("watchNative.distribution", "companion");
        bundleId = request.getArg("watchNative.bundleId",
                request.getPackageName() + ".watchkitapp");
        // watchOS 10 is the floor: single-target WKApplication apps, WidgetKit
        // complications, and the SwiftUI onChange(of:) two-parameter API the
        // generated CN1WatchRootView uses. Lower only if the project explicitly
        // asks (and adjusts the generated shell accordingly).
        minDeploymentTarget = request.getArg("watchNative.minDeploymentTarget", "10.0");
        teamId = request.getArg("watchNative.teamId",
                request.getArg("ios.release.teamId",
                        request.getArg("ios.teamId",
                                request.getArg("ios.debug.teamId", ""))));
        displayName = request.getArg("watchNative.displayName",
                request.getDisplayName() != null ? request.getDisplayName() : request.getMainClass());
    }

    boolean isStandalone() {
        return "standalone".equalsIgnoreCase(distribution);
    }

    String getMinDeploymentTarget() {
        return minDeploymentTarget;
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
        StringBuilder sw = new StringBuilder();
        sw.append("import SwiftUI\n")
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
          .append("}\n");
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
        String mainStub = mangle(mainClass) + "Stub";
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
        plistString(sb, "CFBundleShortVersionString",
                request.getVersion() == null ? "1.0" : request.getVersion());
        plistString(sb, "CFBundleVersion", "1");
        // Modern single-target watch app marker.
        sb.append("    <key>WKApplication</key>\n    <true/>\n");
        if (!isStandalone()) {
            plistString(sb, "WKCompanionAppBundleIdentifier", request.getPackageName());
        }
        sb.append("</dict>\n</plist>\n");
        File plist = new File(appSrcDir, request.getMainClass() + "-Watch-Info.plist");
        owner.createFile(plist, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void plistString(StringBuilder sb, String key, String value) {
        sb.append("    <key>").append(key).append("</key>\n    <string>")
                .append(value == null ? "" : value).append("</string>\n");
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
        String resolvedTeamId = owner.sanitizeTeamId(teamId, "watchNative.teamId");

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
                .append(IPhoneBuilder.escapeRubyStr(minDeploymentTarget)).append("')\n")
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
                .append("    watch_target.source_build_phase.add_file_reference(ref)\n")
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
                .append("  bs['ARCHS[sdk=watchsimulator*]'] = '$(ARCHS_STANDARD)'\n")
                .append("  bs['WATCHOS_DEPLOYMENT_TARGET'] = '")
                .append(IPhoneBuilder.escapeRubyStr(minDeploymentTarget)).append("'\n")
                .append("  bs['TARGETED_DEVICE_FAMILY'] = '4'\n")
                .append("  bs['PRODUCT_BUNDLE_IDENTIFIER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(bundleId)).append("'\n")
                .append("  bs['PRODUCT_NAME'] = '$(TARGET_NAME)'\n")
                .append("  bs['INFOPLIST_FILE'] = '")
                .append(IPhoneBuilder.escapeRubyStr(infoPlistPath)).append("'\n")
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
                // headers (writeStubHeaders) since neither framework exists on
                // watchOS; the stubs supply the GL types the shared decls use.
                .append("  bs['HEADER_SEARCH_PATHS'] = '$(inherited) $(SRCROOT)/")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("-src/watchOSStubs'\n")
                .append("  bs['SKIP_INSTALL'] = 'YES'\n");
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
        s.append("stub_name = '").append(mangle(mainClass)).append("Stub.m'\n")
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

        if (!isStandalone()) {
            // Companion: embed the watch .app into the iOS app under
            // $(CONTENTS_FOLDER_PATH)/Watch and add a build dependency so the
            // pair archives together.
            s.append("app_target.add_dependency(watch_target)\n")
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
                    .append("end\n");
        }

        s.append("xcproj.save\n");

        try {
            owner.createFile(scriptFile, s.toString().getBytes(StandardCharsets.UTF_8));
            owner.exec(hooksDir, "chmod", "0755", scriptFile.getAbsolutePath());
            if (!owner.exec(hooksDir, scriptFile.getAbsolutePath())) {
                throw new BuildException("Failed to apply watchNative Xcode settings via xcodeproj");
            }
            owner.log("[watchNative] Added watchOS target " + watchTargetName
                    + " (" + (isStandalone() ? "standalone" : "companion") + ", "
                    + "watchOS " + minDeploymentTarget + ", arm64_32)");
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to apply watchNative Xcode settings", ex);
        }
    }
}
