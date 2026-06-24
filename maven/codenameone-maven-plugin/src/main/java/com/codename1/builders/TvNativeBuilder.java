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
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper extracted from {@link IPhoneBuilder} that owns the Apple TV (tvOS)
 * native build path. Activated by the build hint {@code tvNative.enabled=true}
 * (or implicitly when {@code codename1.tvMain} is declared).
 *
 * <p>Unlike the Apple Watch port (which has no UIKit / Metal and therefore ships
 * a dedicated Core Graphics backend), tvOS is much closer to iOS: it has UIKit,
 * UIView, {@code UIApplicationMain} and Metal -- it simply lacks OpenGL ES /
 * GLKit. tvOS is therefore handled exactly like the Mac Catalyst slice
 * ({@link MacNativeBuilder}): the build runs with {@code CN1_USE_METAL} (the
 * iOS default), the OpenGL-only source files are excluded, GLKit / OpenGLES
 * umbrella imports resolve to stub headers, and the absent frameworks are
 * weak-linked. The shared {@code UIApplicationMain} entry, the Metal view
 * controller and the UIKit peers are reused as-is.
 *
 * <p>Because tvOS uses a different SDK ({@code appletvos}) it cannot be a
 * variant of the {@code iphoneos} app target the way Mac Catalyst is; so -- like
 * the watch builder -- this delegate <b>adds a second Xcode target</b> that
 * compiles the same ParparVM-generated sources (minus the GL-only files) for
 * tvOS. Every change is additive: with the hint off the iOS build is
 * byte-for-byte unchanged.
 */
class TvNativeBuilder {
    private final IPhoneBuilder owner;

    // Parsed hints.
    private boolean enabled;
    private String bundleId;
    private String minDeploymentTarget; // TVOS_DEPLOYMENT_TARGET
    private String teamId;
    private String displayName;
    // Fully-qualified tvOS lifecycle entry class (codename1.tvMain). Optional;
    // the tvOS app reuses the shared UIApplicationMain entry (the phone main
    // class) so a distinct value is only a tree-shaking root / auto-enable
    // trigger. Empty when neither tvMain nor tvNative.mainClass is set.
    private String tvMain;

    // OpenGL-only source files with no tvOS substitute (tvOS has no OpenGL ES /
    // GLKit). Excluded from the tvOS target exactly as MacNativeBuilder excludes
    // them from the Mac Catalyst slice; the rendering-op .m files take their
    // internal `#elif defined(CN1_USE_METAL)` branch on tvOS. The four iOS XIBs
    // are excluded for the same reason they are on Mac (IBAgent UIKit errors /
    // the runtime never loads them by name on the non-iPhone slice).
    private static final String EXCLUDED_TV_SOURCES =
            "CN1ES2compat.m CN1ES1compat.m EAGLView.m "
            + "CodenameOne_GLViewController.xib MainWindow.xib "
            + "CodenameOne_METALViewController.xib MainWindowMETAL.xib";

    // Frameworks the iOS port links that are unavailable on tvOS; ParparVM
    // weak-links these (see -Doptional.frameworks) so the iOS slice is unchanged
    // while the tvOS slice tolerates the absent symbols. The tvOS SDK actually
    // ships the OpenGLES / GLKit / MessageUI headers (deprecated), so only the
    // genuinely-absent frameworks need weak-linking. OpenGL ES is not used at
    // runtime (the slice renders via Metal); WebKit has no tvOS equivalent.
    private static final String TV_OPTIONAL_FRAMEWORKS =
            "WebKit.framework;OpenGLES.framework;GLKit.framework";

    TvNativeBuilder(IPhoneBuilder owner) {
        this.owner = owner;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Parse the {@code tvNative.*} hint family. The tvOS slice auto-enables when
     * the project declares a {@code codename1.tvMain} entry, so the dual app is
     * produced as part of the regular iPhone build; {@code tvNative.enabled=true}
     * forces it on even without a distinct tvMain.
     */
    void parseHints(BuildRequest request) {
        tvMain = request.getArg("tvMain",
                request.getArg("tvNative.mainClass", "")).trim();
        enabled = "true".equals(request.getArg("tvNative.enabled", "false"))
                || tvMain.length() > 0;
        if (!enabled) {
            return;
        }
        if (tvMain.length() == 0) {
            tvMain = request.getMainClass();
        }
        bundleId = request.getArg("tvNative.bundleId",
                request.getPackageName() + ".tvos");
        // tvOS 13 is a safe modern floor: Metal is fully supported and the focus
        // engine / UIKit surface the port relies on are all present.
        minDeploymentTarget = request.getArg("tvNative.minDeploymentTarget", "13.0");
        teamId = request.getArg("tvNative.teamId",
                request.getArg("ios.release.teamId",
                        request.getArg("ios.teamId",
                                request.getArg("ios.debug.teamId", ""))));
        displayName = request.getArg("tvNative.displayName",
                request.getDisplayName() != null ? request.getDisplayName() : request.getMainClass());
    }

    String getMinDeploymentTarget() {
        return minDeploymentTarget;
    }

    /** Fully-qualified tvOS lifecycle entry class (or the phone main class). */
    String getTvMain() {
        return tvMain;
    }

    /**
     * Frameworks the ParparVM translator should weak-link so the iOS slice still
     * links normally while the tvOS slice tolerates absent symbols.
     */
    String parparvmOptionalFrameworksArg() {
        return "-Doptional.frameworks=" + TV_OPTIONAL_FRAMEWORKS;
    }

    /**
     * Write the tvOS app's Info.plist into {@code appSrcDir}. tvOS needs only the
     * standard CFBundle keys plus the arm64 device capability; the app icon is
     * left unset (ASSETCATALOG_COMPILER_APPICON_NAME empty) so the build does not
     * require a tvOS Brand Assets set, mirroring how the watch target ships its
     * own minimal plist.
     */
    void writeTvInfoPlist(BuildRequest request, File appSrcDir, File resDir) throws IOException {
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
        sb.append("    <key>UIRequiredDeviceCapabilities</key>\n    <array>\n")
                .append("        <string>arm64</string>\n    </array>\n");
        // Register the bundled TrueType fonts (material-design-font.ttf for the
        // FontImage glyphs the UI relies on -- tab icons, FAB, toolbar -- plus any
        // app-supplied fonts) under UIAppFonts. The iOS createTruetypeFont native
        // loads fonts by name via [UIFont fontWithName:], which only resolves once
        // the font is registered through UIAppFonts; the .ttf files are already
        // mirrored into the tvOS target's bundle resources by applyXcodeSettings.
        // Without this the Material font fails to load on tvOS and the icon glyphs
        // render blank (e.g. the Tabs demo shows labels but no icons).
        File[] fontFiles = resDir == null ? null : resDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".ttf");
            }
        });
        if (fontFiles != null && fontFiles.length > 0) {
            sb.append("    <key>UIAppFonts</key>\n    <array>\n");
            for (File f : fontFiles) {
                sb.append("        <string>").append(f.getName()).append("</string>\n");
            }
            sb.append("    </array>\n");
        }
        sb.append("</dict>\n</plist>\n");
        File plist = new File(appSrcDir, request.getMainClass() + "-TV-Info.plist");
        owner.createFile(plist, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void plistString(StringBuilder sb, String key, String value) {
        sb.append("    <key>").append(key).append("</key>\n    <string>")
                .append(value == null ? "" : value).append("</string>\n");
    }

    /**
     * Add and configure the tvOS app target in the generated Xcode project via
     * the Ruby {@code xcodeproj} gem. Creates the target, compiles the shared
     * sources (minus {@link #EXCLUDED_TV_SOURCES}) for {@code appletvos} with the
     * Metal backend, points it at the tvOS Info.plist + GL stub headers, mirrors
     * the iOS bundle resources, and weak-links / drops the absent GL frameworks.
     * The tvOS app reuses the shared {@code UIApplicationMain} entry, so unlike
     * the watch target there is no SwiftUI shell or duplicate-{@code main} rename.
     */
    void applyXcodeSettings(BuildRequest request, File tmpFile, String buildVersion)
            throws BuildException {
        File hooksDir = new File(tmpFile, "hooks");
        hooksDir.mkdir();
        File scriptFile = new File(hooksDir, "apply_tv_native_settings.rb");
        String mainClass = request.getMainClass();
        String tvTargetName = mainClass + "TV";
        String projectFile = new File(tmpFile, "dist/" + mainClass + ".xcodeproj").getAbsolutePath();
        String infoPlistPath = mainClass + "-src/" + mainClass + "-TV-Info.plist";
        String resolvedTeamId = owner.sanitizeTeamId(teamId, "tvNative.teamId");

        StringBuilder s = new StringBuilder();
        s.append("#!/usr/bin/env ruby\n")
                .append("require 'xcodeproj'\n")
                .append("project_file = '").append(IPhoneBuilder.escapeRubyStr(projectFile)).append("'\n")
                .append("xcproj = Xcodeproj::Project.open(project_file)\n")
                .append("app_target = xcproj.targets.find { |t| t.name == '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass)).append("' }\n")
                .append("abort('Unable to find app target ").append(IPhoneBuilder.escapeRubyStr(mainClass))
                .append("') unless app_target\n")
                .append("tv_name = '").append(IPhoneBuilder.escapeRubyStr(tvTargetName)).append("'\n")
                .append("tv_target = xcproj.targets.find { |t| t.name == tv_name }\n")
                .append("if tv_target.nil?\n")
                .append("  tv_target = xcproj.new_target(:application, tv_name, :tvos, '")
                .append(IPhoneBuilder.escapeRubyStr(minDeploymentTarget)).append("')\n")
                .append("end\n")
                // Compile the shared ParparVM sources for tvOS, minus the OpenGL-
                // only files. Reuse the app target's compile sources so we track
                // exactly what was generated (incl. the translated Stub + main()).
                .append("excluded = %w[").append(EXCLUDED_TV_SOURCES).append("]\n")
                .append("app_target.source_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  base = File.basename(ref.path)\n")
                .append("  next if excluded.include?(base)\n")
                .append("  unless tv_target.source_build_phase.files_references.include?(ref)\n")
                .append("    tv_target.source_build_phase.add_file_reference(ref)\n")
                .append("  end\n")
                .append("end\n")
                // Build settings for the tvOS slice.
                .append("tv_target.build_configurations.each do |config|\n")
                .append("  bs = config.build_settings\n")
                .append("  bs['SDKROOT'] = 'appletvos'\n")
                .append("  bs['TVOS_DEPLOYMENT_TARGET'] = '")
                .append(IPhoneBuilder.escapeRubyStr(minDeploymentTarget)).append("'\n")
                .append("  bs['TARGETED_DEVICE_FAMILY'] = '3'\n")
                .append("  bs['PRODUCT_BUNDLE_IDENTIFIER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(bundleId)).append("'\n")
                .append("  bs['PRODUCT_NAME'] = '$(TARGET_NAME)'\n")
                .append("  bs['INFOPLIST_FILE'] = '")
                .append(IPhoneBuilder.escapeRubyStr(infoPlistPath)).append("'\n")
                .append("  bs['MARKETING_VERSION'] = '")
                .append(IPhoneBuilder.escapeRubyStr(request.getVersion() == null ? "1.0" : request.getVersion())).append("'\n")
                .append("  bs['CURRENT_PROJECT_VERSION'] = '")
                .append(IPhoneBuilder.escapeRubyStr(buildVersion == null ? "1" : buildVersion)).append("'\n")
                .append("  bs['GCC_PREFIX_HEADER'] = '")
                .append(IPhoneBuilder.escapeRubyStr(mainClass + "-src/" + mainClass + "-Prefix.pch")).append("'\n")
                .append("  bs['EXCLUDED_SOURCE_FILE_NAMES'] = '").append(EXCLUDED_TV_SOURCES).append("'\n")
                // The CN1 sources compile without ARC, matching the iOS port.
                .append("  bs['CLANG_ENABLE_OBJC_ARC'] = 'NO'\n")
                // No tvOS Brand Assets set is generated; leave the app-icon unset
                // so actool does not fail the build (dev/screenshot builds only).
                .append("  bs['ASSETCATALOG_COMPILER_APPICON_NAME'] = ''\n")
                .append("  bs['SKIP_INSTALL'] = 'YES'\n");
        if (resolvedTeamId != null && !resolvedTeamId.isEmpty()) {
            s.append("  bs['DEVELOPMENT_TEAM'] = '").append(resolvedTeamId).append("'\n");
        }
        s.append("end\n");

        // The iOS XIBs / OpenGLES.framework have no tvOS equivalent. Drop GL
        // framework refs from the tvOS target (GLKit/OpenGLES are absent on tvOS;
        // the GL types come from the stub headers, the rendering uses Metal).
        s.append("gl = %w[OpenGLES.framework GLKit.framework]\n")
                .append("tv_target.frameworks_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  bf.remove_from_project if gl.include?(File.basename(ref.path))\n")
                .append("end\n");

        // Add the generated tvOS Info.plist file reference to the project group so
        // INFOPLIST_FILE resolves.
        s.append("tv_src = '").append(IPhoneBuilder.escapeRubyStr(mainClass)).append("-src'\n");

        // Mirror the iOS app's bundle resources into the tvOS target so the CN1
        // runtime finds its theme + assets at runtime (the native theme .res, the
        // app theme/CN1Resource.res, material-design-font.ttf, bundled images).
        // Skip the iOS UI / icon assets: the asset catalog has no tvOS-applicable
        // content and storyboards/xibs are the iOS UI.
        s.append("res_skip = %w[.xcassets .storyboard .xib]\n")
                .append("app_target.resources_build_phase.files.to_a.each do |bf|\n")
                .append("  ref = bf.file_ref\n")
                .append("  next unless ref && ref.path\n")
                .append("  next if res_skip.any? { |ext| ref.path.to_s.end_with?(ext) }\n")
                .append("  unless tv_target.resources_build_phase.files_references.include?(ref)\n")
                .append("    tv_target.resources_build_phase.add_file_reference(ref)\n")
                .append("  end\n")
                .append("end\n");

        s.append("xcproj.save\n");

        try {
            owner.createFile(scriptFile, s.toString().getBytes(StandardCharsets.UTF_8));
            owner.exec(hooksDir, "chmod", "0755", scriptFile.getAbsolutePath());
            if (!owner.exec(hooksDir, scriptFile.getAbsolutePath())) {
                throw new BuildException("Failed to apply tvNative Xcode settings via xcodeproj");
            }
            owner.log("[tvNative] Added tvOS target " + tvTargetName
                    + " (standalone, tvOS " + minDeploymentTarget + ", Metal)");
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to apply tvNative Xcode settings", ex);
        }
    }
}
