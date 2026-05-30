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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;

/**
 * Helper class extracted from {@link IPhoneBuilder} that owns every Mac
 * native specific code path. Activated when the build hint {@code
 * macNative.enabled=true} is set: parses the {@code macNative.*} hint
 * family, generates the per-channel {@code .entitlements} plists, the
 * {@code ExportOptions-AppStore-Mac.plist} / {@code ExportOptions-
 * DeveloperID-Mac.plist} archive-export plists, the Mac iconset under
 * {@code Images.xcassets/Mac.appiconset/}, the GLKit / OpenGL ES stub
 * headers used by the Mac Catalyst slice, and finally a Ruby
 * {@code xcodeproj}-based script that injects the {@code
 * SUPPORTS_MACCATALYST=YES} family of build settings plus the
 * {@code DEAD_CODE_STRIPPING / EXCLUDED_SOURCE_FILE_NAMES} workarounds
 * needed for the Catalyst slice to compile + link.
 *
 * <p>This class is NOT a separate {@link Executor} -- it is a delegate
 * owned by {@link IPhoneBuilder}, called at three well-defined points
 * inside that builder's pipeline (hint parsing, post-project-generate
 * patching, and asset-catalog finalisation). The Mac slice still
 * piggybacks on the existing iOS Xcode project; this helper just adds
 * the Mac-specific overlays on top.
 *
 * <p>The underlying technology is Mac Catalyst at the Xcode level, but
 * that is an implementation detail and never surfaces in the build
 * hint names, output directories, or methods on this class. The
 * user-facing surface uses {@code macNative.*}.
 */
class MacNativeBuilder {
    private final IPhoneBuilder owner;

    // Parsed hints.
    private boolean enabled;
    private String distribution;       // appStore | developerID | both
    private String teamId;
    private String bundleId;
    private boolean deriveBundleId;
    private String minDeploymentTarget;     // MACOSX_DEPLOYMENT_TARGET
    private String iosMinDeploymentTarget;  // IPHONEOS floor for Catalyst
    private String appCategory;
    private String copyright;
    private String signingStyle;       // automatic | manual
    private String signingIdentityAppStore;
    private String signingIdentityDeveloperID;
    private String fixedWindowSize;            // "<W>x<H>" or empty for native default

    MacNativeBuilder(IPhoneBuilder owner) {
        this.owner = owner;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Parse the {@code macNative.*} hint family off the request and
     * stash the values for later. Caller is expected to flip {@code
     * useMetal=true} and update the minimum deployment target since
     * Catalyst won't link OpenGL ES.
     */
    void parseHints(BuildRequest request) {
        enabled = "true".equals(request.getArg("macNative.enabled", "false"));
        if (!enabled) {
            return;
        }
        distribution = request.getArg("macNative.distribution", "appStore");
        teamId = request.getArg("macNative.teamId",
                request.getArg("ios.release.teamId",
                        request.getArg("ios.teamId",
                                request.getArg("ios.debug.teamId", ""))));
        bundleId = request.getArg("macNative.bundleId",
                request.getPackageName() + ".mac");
        deriveBundleId = !"false".equals(request.getArg("macNative.deriveBundleId", "true"));
        minDeploymentTarget = request.getArg("macNative.minDeploymentTarget", "10.15");
        iosMinDeploymentTarget = request.getArg("macNative.iosMinDeploymentTarget", "13.1");
        appCategory = request.getArg("macNative.appCategory", "public.app-category.utilities");
        String defaultCopyright = "Copyright (c) "
                + Calendar.getInstance().get(Calendar.YEAR)
                + " " + (request.getVendor() != null ? request.getVendor() : request.getPackageName());
        copyright = request.getArg("macNative.copyright", defaultCopyright);
        signingStyle = request.getArg("macNative.signing.style", "automatic");
        signingIdentityAppStore = request.getArg(
                "macNative.signingIdentity.appStore", "Apple Distribution");
        signingIdentityDeveloperID = request.getArg(
                "macNative.signingIdentity.developerID", "Developer ID Application");
        // Opt-in deterministic window size for headless screenshot CI.
        // Format "WxH", e.g., "1024x685". Empty/unset preserves the
        // default user-resizable Catalyst window.
        fixedWindowSize = request.getArg("macNative.fixedWindowSize", "").trim();
    }

    /**
     * iOS-port frameworks that must be weak-linked or omitted on the
     * Mac slice. ByteCodeTranslator already honours {@code
     * -Doptional.frameworks} and emits {@code ATTRIBUTES = (Weak, );}
     * for each entry, so the iOS slice still links normally while
     * the Mac slice tolerates absent runtime symbols at startup.
     */
    String parparvmOptionalFrameworksArg() {
        return "-Doptional.frameworks=AddressBookUI.framework;"
                + "AddressBook.framework;MessageUI.framework;"
                + "MediaPlayer.framework;GLKit.framework;OpenGLES.framework";
    }

    String getIosMinDeploymentTarget() {
        return iosMinDeploymentTarget;
    }

    /**
     * Write the per-channel {@code .entitlements} plists into {@code
     * appSrcDir}. For {@code distribution=both} two files are emitted
     * (suffixed {@code -AppStore} / {@code -DeveloperID}); for a single
     * channel a single file named after the main class is emitted.
     */
    void writeEntitlements(BuildRequest request, File appSrcDir) throws IOException {
        appSrcDir.mkdirs();
        if ("both".equalsIgnoreCase(distribution)) {
            writeEntitlementsFile(request, appSrcDir,
                    request.getMainClass() + "-AppStore", "appStore");
            writeEntitlementsFile(request, appSrcDir,
                    request.getMainClass() + "-DeveloperID", "developerID");
        } else {
            writeEntitlementsFile(request, appSrcDir,
                    request.getMainClass(), distribution);
        }
    }

    private void writeEntitlementsFile(BuildRequest request, File appSrcDir,
                                       String baseName, String channel) throws IOException {
        boolean sandbox = parseEntitlementBool(request,
                "macNative.entitlements.appSandbox",
                "appStore".equalsIgnoreCase(channel));
        boolean networkClient = parseEntitlementBool(request,
                "macNative.entitlements.network.client", true);
        boolean networkServer = parseEntitlementBool(request,
                "macNative.entitlements.network.server", false);
        String filesUserSelected = request.getArg(
                "macNative.entitlements.files.userSelected", "readwrite").toLowerCase();
        boolean hardenedRuntime = parseEntitlementBool(request,
                "macNative.entitlements.hardenedRuntime",
                "developerID".equalsIgnoreCase(channel));
        boolean allowJit = parseEntitlementBool(request,
                "macNative.entitlements.allowJit", false);
        String extra = request.getArg("macNative.entitlements.extra", "");

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        if (sandbox) {
            sb.append("    <key>com.apple.security.app-sandbox</key>\n    <true/>\n");
        }
        if (networkClient) {
            sb.append("    <key>com.apple.security.network.client</key>\n    <true/>\n");
        }
        if (networkServer) {
            sb.append("    <key>com.apple.security.network.server</key>\n    <true/>\n");
        }
        if ("readwrite".equals(filesUserSelected)) {
            sb.append("    <key>com.apple.security.files.user-selected.read-write</key>\n    <true/>\n");
            sb.append("    <key>com.apple.security.files.downloads.read-write</key>\n    <true/>\n");
        } else if ("readonly".equals(filesUserSelected)) {
            sb.append("    <key>com.apple.security.files.user-selected.read-only</key>\n    <true/>\n");
        }
        if (hardenedRuntime && !allowJit) {
            sb.append("    <key>com.apple.security.cs.allow-jit</key>\n    <false/>\n");
            sb.append("    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>\n    <false/>\n");
        } else if (allowJit) {
            sb.append("    <key>com.apple.security.cs.allow-jit</key>\n    <true/>\n");
        }
        if (extra != null && extra.trim().length() > 0) {
            sb.append(extra);
            if (!extra.endsWith("\n")) {
                sb.append("\n");
            }
        }
        sb.append("</dict>\n</plist>\n");

        File ent = new File(appSrcDir, baseName + ".entitlements");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(ent.toPath()), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        owner.log("Wrote Mac entitlements: " + ent.getAbsolutePath() + " (channel=" + channel + ")");
    }

    private static boolean parseEntitlementBool(BuildRequest request, String hint, boolean def) {
        return Boolean.parseBoolean(request.getArg(hint, Boolean.toString(def)));
    }

    /**
     * Write {@code ExportOptions-AppStore-Mac.plist} and/or {@code
     * ExportOptions-DeveloperID-Mac.plist} into {@code distDir}, plus
     * log the matching {@code xcodebuild archive} / {@code -exportArchive}
     * command so a downstream operator can complete the export.
     */
    void writeExportOptions(BuildRequest request, File distDir) throws IOException {
        distDir.mkdirs();
        if ("both".equalsIgnoreCase(distribution)) {
            writeExportOptionsFile(request, distDir, "appStore");
            writeExportOptionsFile(request, distDir, "developerID");
        } else {
            writeExportOptionsFile(request, distDir, distribution);
        }
        owner.log("Use xcodebuild to archive and export the Mac app, e.g.:");
        owner.log("  xcodebuild -project " + request.getMainClass() + ".xcodeproj"
                + " -scheme " + request.getMainClass()
                + " -destination 'generic/platform=macOS,variant=Mac Catalyst'"
                + " -archivePath build/" + request.getMainClass() + ".xcarchive archive");
        owner.log("  xcodebuild -exportArchive -archivePath build/" + request.getMainClass()
                + ".xcarchive -exportOptionsPlist ExportOptions-<channel>-Mac.plist"
                + " -exportPath build/export");
    }

    private void writeExportOptionsFile(BuildRequest request, File distDir, String channel)
            throws IOException {
        boolean isAppStore = "appStore".equalsIgnoreCase(channel);
        String method = isAppStore ? "app-store" : "developer-id";
        String signingIdentity = isAppStore
                ? signingIdentityAppStore : signingIdentityDeveloperID;
        String resolvedTeamId = owner.sanitizeTeamId(teamId, "macNative.teamId");

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        sb.append("    <key>destination</key>\n    <string>export</string>\n");
        sb.append("    <key>method</key>\n    <string>").append(method).append("</string>\n");
        if (resolvedTeamId != null && !resolvedTeamId.isEmpty()) {
            sb.append("    <key>teamID</key>\n    <string>").append(resolvedTeamId).append("</string>\n");
        }
        sb.append("    <key>signingStyle</key>\n    <string>")
                .append("manual".equalsIgnoreCase(signingStyle) ? "manual" : "automatic")
                .append("</string>\n");
        if (signingIdentity != null && !signingIdentity.isEmpty()) {
            sb.append("    <key>signingCertificate</key>\n    <string>")
                    .append(signingIdentity).append("</string>\n");
        }
        if ("manual".equalsIgnoreCase(signingStyle)) {
            String profile = request.getArg(
                    "macNative.provisioningProfile." + (isAppStore ? "appStore" : "developerID"),
                    "");
            if (!profile.isEmpty()) {
                String macBundleId = deriveBundleId
                        ? request.getPackageName() + ".maccatalyst"
                        : bundleId;
                sb.append("    <key>provisioningProfiles</key>\n    <dict>\n")
                        .append("        <key>").append(macBundleId).append("</key>\n")
                        .append("        <string>").append(profile).append("</string>\n")
                        .append("    </dict>\n");
            }
        }
        sb.append("</dict>\n</plist>\n");

        String label = isAppStore ? "AppStore" : "DeveloperID";
        File f = new File(distDir, "ExportOptions-" + label + "-Mac.plist");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        owner.log("Wrote Mac ExportOptions: " + f.getAbsolutePath());
    }

    /**
     * Emit {@code Images.xcassets/Mac.appiconset/} so {@code actool}
     * picks up the Mac icon during the Mac slice build. Maps the
     * existing 1024 source icon as the largest size; actool scales
     * down the rest.
     */
    void writeAppIconset(File assetCatalogDir, File icon512) throws IOException {
        File iconset = new File(assetCatalogDir, "Mac.appiconset");
        iconset.mkdirs();
        File source = icon512;
        if (source == null || !source.exists()) {
            File alt = new File(assetCatalogDir, "AppIcon.appiconset/Icon-1024.png");
            if (alt.exists()) {
                source = alt;
            }
        }
        if (source == null || !source.exists()) {
            owner.log("Skipping Mac.appiconset generation: no 512/1024 source icon available");
            return;
        }
        File dest = new File(iconset, "icon_512x512@2x.png");
        Executor.copy(source, dest);
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"images\" : [\n");
        json.append("    { \"size\" : \"512x512\", \"idiom\" : \"mac\", \"filename\" : \"icon_512x512@2x.png\", \"scale\" : \"2x\" }\n");
        json.append("  ],\n  \"info\" : { \"version\" : 1, \"author\" : \"xcode\" }\n}\n");
        File contents = new File(iconset, "Contents.json");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(contents.toPath()), StandardCharsets.UTF_8)) {
            w.write(json.toString());
        }
        owner.log("Wrote Mac.appiconset at " + iconset.getAbsolutePath());
    }

    /**
     * Stub headers for GLKit and OpenGL ES, used only by the Mac
     * Catalyst slice via {@code HEADER_SEARCH_PATHS[sdk=macosx*]}.
     * These satisfy the umbrella {@code #import} lines in the iOS-port
     * headers ({@code GLViewController.h}, {@code EAGLView.h},
     * {@code CN1ES2compat.h}, etc.) so the project compiles for Mac.
     * Real GL calls are preprocessed out by {@code #ifdef CN1_USE_METAL}
     * or in {@code EXCLUDED_SOURCE_FILE_NAMES}-excluded .m files.
     */
    void writeStubHeaders(File appSrcDir) throws IOException {
        File stubsDir = new File(appSrcDir, "macCatalystStubs");
        File openGLES = new File(stubsDir, "OpenGLES");
        File openGLESes1 = new File(openGLES, "ES1");
        File openGLESes2 = new File(openGLES, "ES2");
        File glkit = new File(stubsDir, "GLKit");
        openGLESes1.mkdirs();
        openGLESes2.mkdirs();
        glkit.mkdirs();
        writeStub(new File(openGLES, "EAGL.h"),
                "#ifndef CN1_MAC_CATALYST_STUB_EAGL_H\n"
                + "#define CN1_MAC_CATALYST_STUB_EAGL_H\n"
                + "#import <Foundation/Foundation.h>\n"
                + "@interface EAGLContext : NSObject @end\n"
                + "typedef enum { kEAGLRenderingAPIOpenGLES1 = 1,"
                + " kEAGLRenderingAPIOpenGLES2 = 2,"
                + " kEAGLRenderingAPIOpenGLES3 = 3 } EAGLRenderingAPI;\n"
                + "#endif\n");
        String glTypes =
                "#ifndef CN1_MAC_CATALYST_STUB_GLES_TYPES\n"
                + "#define CN1_MAC_CATALYST_STUB_GLES_TYPES\n"
                + "typedef unsigned int   GLenum;\n"
                + "typedef unsigned int   GLuint;\n"
                + "typedef int            GLint;\n"
                + "typedef int            GLsizei;\n"
                + "typedef float          GLfloat;\n"
                + "typedef float          GLclampf;\n"
                + "typedef unsigned char  GLubyte;\n"
                + "typedef unsigned char  GLboolean;\n"
                + "typedef void           GLvoid;\n"
                + "typedef signed char    GLbyte;\n"
                + "typedef short          GLshort;\n"
                + "typedef unsigned short GLushort;\n"
                + "typedef int            GLfixed;\n"
                + "typedef unsigned int   GLbitfield;\n"
                + "typedef long           GLintptr;\n"
                + "typedef long           GLsizeiptr;\n"
                + "#endif\n";
        writeStub(new File(openGLESes1, "gl.h"), glTypes);
        writeStub(new File(openGLESes1, "glext.h"), "");
        writeStub(new File(openGLESes2, "gl.h"), glTypes);
        writeStub(new File(openGLESes2, "glext.h"), "");
        writeStub(new File(glkit, "GLKit.h"),
                "#ifndef CN1_MAC_CATALYST_STUB_GLKIT_H\n"
                + "#define CN1_MAC_CATALYST_STUB_GLKIT_H\n"
                + "#import <Foundation/Foundation.h>\n"
                + "#import <OpenGLES/ES2/gl.h>\n"
                + "typedef struct { float m[16]; } GLKMatrix4;\n"
                + "typedef struct { float v[4];  } GLKVector4;\n"
                + "typedef struct { float v[3];  } GLKVector3;\n"
                + "typedef struct { float v[2];  } GLKVector2;\n"
                + "@interface GLKView : NSObject @end\n"
                + "@interface GLKBaseEffect : NSObject @end\n"
                + "@interface GLKTextureLoader : NSObject @end\n"
                + "@interface GLKTextureInfo : NSObject @end\n"
                + "#endif\n");
        owner.log("Wrote Mac Catalyst stub headers under " + stubsDir.getAbsolutePath());
    }

    private static void writeStub(File f, String content) throws IOException {
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8)) {
            w.write(content);
        }
    }

    /**
     * Patch the generated {@code project.pbxproj} via Ruby + the
     * {@code xcodeproj} gem so the app target gains {@code
     * SUPPORTS_MACCATALYST=YES}, the right deployment targets, the
     * signing wiring per channel, and the workarounds needed for the
     * Catalyst slice (excluded GL-only sources, stub-header search
     * path, OpenGLES iOS-only re-link, etc.).
     */
    void applyXcodeSettings(BuildRequest request, File tmpFile, String buildVersion)
            throws BuildException {
        File hooksDir = new File(tmpFile, "hooks");
        hooksDir.mkdir();
        File scriptFile = new File(hooksDir, "apply_mac_native_settings.rb");
        String mainClass = request.getMainClass();
        String projectFile = new File(tmpFile, "dist/" + mainClass + ".xcodeproj").getAbsolutePath();
        String resolvedTeamId = owner.sanitizeTeamId(teamId, "macNative.teamId");
        boolean manualSigning = "manual".equalsIgnoreCase(signingStyle);

        // For the "both" case the AppStore variant is wired as the default
        // CODE_SIGN_ENTITLEMENTS; xcodebuild -exportOptionsPlist picks up the
        // DeveloperID entitlements via the matching ExportOptions file.
        String entitlementsLeaf = "both".equalsIgnoreCase(distribution)
                ? mainClass + "-AppStore.entitlements"
                : mainClass + ".entitlements";
        String entitlementsPath = mainClass + "-src/" + entitlementsLeaf;

        StringBuilder s = new StringBuilder();
        s.append("#!/usr/bin/env ruby\n")
                .append("require 'xcodeproj'\n")
                .append("project_file = '").append(IPhoneBuilder.escapeRubyStr(projectFile)).append("'\n")
                .append("xcproj = Xcodeproj::Project.open(project_file)\n")
                .append("target = xcproj.targets.find { |t| t.name == '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("' }\n")
                .append("abort('Unable to find app target ").append(IPhoneBuilder.escapeRubyStr(mainClass))
                .append("') unless target\n")
                .append("target.build_configurations.each do |config|\n")
                .append("  bs = config.build_settings\n")
                .append("  bs['SUPPORTS_MACCATALYST'] = 'YES'\n")
                .append("  bs['SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD'] = 'NO'\n")
                .append("  bs['TARGETED_DEVICE_FAMILY'] = '1,2,6'\n")
                .append("  bs['DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER'] = '")
                .append(deriveBundleId ? "YES" : "NO").append("'\n");
        if (!deriveBundleId) {
            s.append("  bs['PRODUCT_BUNDLE_IDENTIFIER[sdk=macosx*]'] = '")
                    .append(IPhoneBuilder.escapeRubyStr(bundleId)).append("'\n");
        }
        s.append("  bs['MACOSX_DEPLOYMENT_TARGET'] = '")
                .append(IPhoneBuilder.escapeRubyStr(minDeploymentTarget)).append("'\n")
                .append("  bs['IPHONEOS_DEPLOYMENT_TARGET'] = '")
                .append(IPhoneBuilder.escapeRubyStr(iosMinDeploymentTarget)).append("'\n")
                .append("  bs['MARKETING_VERSION'] = '")
                .append(IPhoneBuilder.escapeRubyStr(request.getVersion() == null ? "1.0" : request.getVersion())).append("'\n")
                .append("  bs['CURRENT_PROJECT_VERSION'] = '")
                .append(IPhoneBuilder.escapeRubyStr(buildVersion == null ? "1" : buildVersion)).append("'\n")
                .append("  bs['LD_RUNPATH_SEARCH_PATHS'] = '$(inherited) @executable_path/Frameworks @executable_path/../Frameworks'\n")
                .append("  bs['INFOPLIST_KEY_LSApplicationCategoryType'] = '")
                .append(IPhoneBuilder.escapeRubyStr(appCategory)).append("'\n")
                .append("  bs['INFOPLIST_KEY_NSHumanReadableCopyright'] = '")
                .append(IPhoneBuilder.escapeRubyStr(copyright)).append("'\n");
        if (fixedWindowSize != null && !fixedWindowSize.isEmpty()) {
            // Custom Info.plist key consumed by CodenameOne_GLSceneDelegate
            // to lock the Catalyst window's sizeRestrictions. Off when the
            // hint is unset so production apps keep a resizable window.
            s.append("  bs['INFOPLIST_KEY_CN1MacFixedWindowSize'] = '")
                    .append(IPhoneBuilder.escapeRubyStr(fixedWindowSize)).append("'\n");
        }
        s.append("  bs['CODE_SIGN_ENTITLEMENTS'] = '")
                .append(IPhoneBuilder.escapeRubyStr(entitlementsPath)).append("'\n")
                .append("  bs['CODE_SIGN_STYLE'] = '")
                .append(manualSigning ? "Manual" : "Automatic").append("'\n");
        if (resolvedTeamId != null && !resolvedTeamId.isEmpty()) {
            s.append("  bs['DEVELOPMENT_TEAM[sdk=macosx*]'] = '").append(resolvedTeamId).append("'\n");
        }
        if (manualSigning) {
            if (signingIdentityAppStore != null && !signingIdentityAppStore.isEmpty()) {
                s.append("  bs['CODE_SIGN_IDENTITY[sdk=macosx*]'] = '")
                        .append(IPhoneBuilder.escapeRubyStr(signingIdentityAppStore)).append("'\n");
            }
        }
        // OpenGL ES backbone files have no Mac Catalyst equivalent (GLKit /
        // OpenGLES headers are missing from recent macOS SDKs). Exclude them
        // from the Mac slice via EXCLUDED_SOURCE_FILE_NAMES so the build
        // compiles. The rendering-op .m files have internal #ifdef CN1_USE_METAL
        // guards that route to the Metal path on Mac.
        // All four iOS XIBs trigger an IBAgent-macOS-UIKit internal error
        // when compiled for the Mac slice (observed on Xcode 26.x).
        // CodenameOne_GLAppDelegate.m has a TARGET_OS_MACCATALYST branch
        // that passes nil to initWithNibName: on Mac, so the runtime never
        // tries to load these NIBs by name and excluding them at compile
        // time is safe. The iOS slice keeps loading them normally.
        s.append("  bs['EXCLUDED_SOURCE_FILE_NAMES[sdk=macosx*]'] = ")
                .append("'CN1ES2compat.m CN1ES1compat.m EAGLView.m ")
                .append("CodenameOne_GLViewController.xib MainWindow.xib ")
                .append("CodenameOne_METALViewController.xib MainWindowMETAL.xib'\n");
        // Header search path stubs for the Mac slice: the iOS port ships an
        // umbrella set of empty/stub GLKit and OpenGLES headers under
        // macCatalystStubs/.
        s.append("  bs['HEADER_SEARCH_PATHS[sdk=macosx*]'] = ")
                .append("'$(inherited) $(SRCROOT)/").append(IPhoneBuilder.escapeRubyStr(mainClass))
                .append("-src/macCatalystStubs'\n");
        s.append("end\n");
        // OpenGLES.framework is absent from the macOS SDK. Drop the
        // OpenGLES build-file entry from the unconditional Frameworks phase
        // and re-add it only for the iOS slice via OTHER_LDFLAGS[sdk=iphoneos*].
        // GLKit stays in the build phase but is marked Weak so any genuinely-
        // absent symbol surfaces at runtime, not link.
        s.append("removed_refs = []\n");
        s.append("target.frameworks_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  base = File.basename(ref.path)\n")
                .append("  if base == 'OpenGLES.framework'\n")
                .append("    removed_refs << ref\n")
                .append("    bf.remove_from_project\n")
                .append("  elsif base == 'GLKit.framework'\n")
                .append("    bf.settings ||= {}\n")
                .append("    attrs = (bf.settings['ATTRIBUTES'] || []).dup\n")
                .append("    attrs << 'Weak' unless attrs.include?('Weak')\n")
                .append("    bf.settings['ATTRIBUTES'] = attrs\n")
                .append("  end\n")
                .append("end\n");
        // Force DEAD_CODE_STRIPPING=YES for the Mac slice. The iOS port
        // declares a handful of native JNI methods (java.io.File hidden /
        // directory probes, IOSNative biometrics, etc.) in headers but ships
        // their C bodies in template files outside the per-app source tree;
        // iOS strips them via `-dead_strip`, Mac Catalyst doesn't by default
        // in Debug, so those refs surface as link errors without this flag.
        s.append("target.build_configurations.each do |config|\n")
                .append("  bs = config.build_settings\n")
                .append("  existing = bs['OTHER_LDFLAGS[sdk=iphoneos*]'] || '$(inherited)'\n")
                .append("  bs['OTHER_LDFLAGS[sdk=iphoneos*]'] = existing + ' -framework OpenGLES'\n")
                .append("  existing_sim = bs['OTHER_LDFLAGS[sdk=iphonesimulator*]'] || '$(inherited)'\n")
                .append("  bs['OTHER_LDFLAGS[sdk=iphonesimulator*]'] = existing_sim + ' -framework OpenGLES'\n")
                .append("  bs['DEAD_CODE_STRIPPING[sdk=macosx*]'] = 'YES'\n")
                .append("end\n");
        s.append("xcproj.save\n");

        try {
            owner.createFile(scriptFile, s.toString().getBytes(StandardCharsets.UTF_8));
            owner.exec(hooksDir, "chmod", "0755", scriptFile.getAbsolutePath());
            if (!owner.exec(hooksDir, scriptFile.getAbsolutePath())) {
                throw new BuildException("Failed to apply macNative Xcode settings via xcodeproj");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to apply macNative Xcode settings via xcodeproj", ex);
        }
    }

    /**
     * Friendly error when the user combined macNative.enabled=true with
     * ios.project_type=iphone. Mac requires the iPad device family.
     */
    void validateProjectType(BuildRequest request) {
        if ("iphone".equalsIgnoreCase(request.getArg("ios.project_type", "ios"))) {
            throw new BuildException("macNative.enabled=true is incompatible with ios.project_type=iphone. "
                    + "Use 'ios' (universal) or 'ipad'.");
        }
    }
}
