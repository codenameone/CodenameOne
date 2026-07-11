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
package com.codename1.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the {@code CN1Widgets} WidgetKit app-extension target that the Codename One
 * iOS build wires into the generated Xcode project when the app references
 * {@code com.codename1.surfaces} (see the {@code surfaces.json} project manifest and the
 * {@code ios.surfaces.*} build hints).
 *
 * <p>The extension is fully generic: the static Swift renderer sources shipped as plugin
 * resources under {@code com/codename1/builders/surfaces/ios/} render whatever timeline
 * documents the app published into the shared App Group container, so the only
 * per-project code generated here is:</p>
 *
 * <ul>
 *   <li>{@code CN1SurfaceConfig.swift} - the app group id constant the provider and
 *       renderer use to resolve the shared container;</li>
 *   <li>{@code CN1WidgetBundle.swift} - the {@code @main WidgetBundle} plus one tiny
 *       concrete {@code CN1Widget_<kindid>} struct per widget kind. The Widget protocol
 *       requires {@code init()}, so a parameterized struct cannot serve every kind; each
 *       generated struct hardcodes its kind metadata and delegates to the shared
 *       {@code cn1MakeWidgetConfiguration} factory in CN1DescriptorWidget.swift. When
 *       live activities are enabled the bundle also lists {@code CN1LiveActivityWidget()}
 *       unconditionally - the struct itself guards every ActivityKit reference with
 *       {@code #if canImport(ActivityKit)}, which keeps the composition simple and
 *       compiles cleanly on SDKs/platforms without ActivityKit.</li>
 * </ul>
 *
 * <p>{@link #buildAppTargetFileMap()} returns the glue compiled into the MAIN APP target
 * (the Swift {@code CN1SurfaceBridge} the Objective-C natives reach via
 * {@code NSClassFromString}, plus copies of the attributes/config files - ActivityKit
 * matches app and extension by the {@code ActivityAttributes} type, so both modules need
 * the identical struct).</p>
 *
 * <p>The extension's deployment target defaults to 16.1 (ActivityKit's floor); the host
 * app's own deployment target is unaffected - the extension simply never runs on older
 * iOS versions.</p>
 */
public class IOSWidgetExtensionBuilder {

    /** Info.plist key holding the App Group id, read by extension and app alike. */
    public static final String APP_GROUP_PLIST_KEY = "CN1SurfacesAppGroup";

    /** Classpath folder holding the static Swift renderer sources. */
    private static final String RESOURCE_ROOT = "/com/codename1/builders/surfaces/ios/";

    /** Static Swift sources copied verbatim into the extension target. */
    private static final String[] EXTENSION_SOURCES = {
        "CN1SurfaceModel.swift",
        "CN1SurfaceRenderer.swift",
        "CN1WidgetProvider.swift",
        "CN1DescriptorWidget.swift",
        "CN1SurfaceAttributes.swift",
    };

    /**
     * One widget kind declared in surfaces.json. Ids must match
     * {@code [a-z][a-z0-9_]*} - they become Swift struct names and WidgetKit kind ids.
     */
    public static class Kind {
        private final String id;
        private String name;
        private String description;
        private List<String> iosFamilies = new ArrayList<String>();
        private String previewName;

        public Kind(String id) {
            this.id = id;
        }

        public Kind setName(String name) {
            this.name = name;
            return this;
        }

        public Kind setDescription(String description) {
            this.description = description;
            return this;
        }

        /** Families from {@code small}, {@code medium}, {@code large}, {@code lockscreen}. */
        public Kind setIosFamilies(List<String> families) {
            this.iosFamilies = families == null ? new ArrayList<String>() : families;
            return this;
        }

        public Kind setPreviewName(String previewName) {
            this.previewName = previewName;
            return this;
        }

        public String getId() { return id; }
        public String getName() { return name == null || name.length() == 0 ? id : name; }
        public String getDescription() { return description == null ? "" : description; }
        public List<String> getIosFamilies() { return iosFamilies; }
        public String getPreviewName() { return previewName; }
    }

    private String extensionName = "CN1Widgets";
    private String hostBundleId;
    private String appGroupId;
    private String deploymentTarget = "16.1";
    private boolean liveActivitiesEnabled;
    private final List<Kind> kinds = new ArrayList<Kind>();

    /** Bare-bones constructor. Configure with the fluent setters. */
    public IOSWidgetExtensionBuilder() {}

    /**
     * Sets the extension target name (Xcode target, .appex bundle and bundle-id suffix).
     * Must be an ASCII identifier. Defaults to {@code CN1Widgets}.
     */
    public IOSWidgetExtensionBuilder setExtensionName(String name) {
        this.extensionName = name;
        return this;
    }

    /** The host iOS app's bundle identifier. Required. */
    public IOSWidgetExtensionBuilder setHostBundleId(String id) {
        this.hostBundleId = id;
        return this;
    }

    /**
     * The App Group identifier shared between the host app and the extension. Apple
     * requires it to start with {@code group.}. Required.
     */
    public IOSWidgetExtensionBuilder setAppGroupId(String id) {
        this.appGroupId = id;
        return this;
    }

    /**
     * iOS deployment target of the extension target only (the host app's floor is
     * unchanged). Defaults to {@code 16.1}, ActivityKit's minimum.
     */
    public IOSWidgetExtensionBuilder setDeploymentTarget(String target) {
        this.deploymentTarget = target;
        return this;
    }

    /** Adds the live activity widget to the generated bundle. */
    public IOSWidgetExtensionBuilder setLiveActivitiesEnabled(boolean enabled) {
        this.liveActivitiesEnabled = enabled;
        return this;
    }

    /** Declares one widget kind (from surfaces.json). */
    public IOSWidgetExtensionBuilder addKind(Kind kind) {
        kinds.add(kind);
        return this;
    }

    public String getExtensionName() { return extensionName; }
    public String getHostBundleId() { return hostBundleId; }
    public String getAppGroupId() { return appGroupId; }
    public String getDeploymentTarget() { return deploymentTarget; }
    public boolean isLiveActivitiesEnabled() { return liveActivitiesEnabled; }
    public List<Kind> getKinds() { return kinds; }

    /**
     * Builds the in-memory file map of the extension target, keyed by relative path
     * inside the extension folder.
     */
    public Map<String, byte[]> buildFileMap() throws IOException {
        validate();
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist()));
        map.put(extensionName + ".entitlements", utf8(buildEntitlements()));
        map.put("buildSettings.properties", utf8(buildBuildSettings()));
        for (String source : EXTENSION_SOURCES) {
            map.put(source, utf8(loadResource(source)));
        }
        if (liveActivitiesEnabled) {
            map.put("CN1LiveActivityWidget.swift", utf8(loadResource("CN1LiveActivityWidget.swift")));
        }
        map.put("CN1SurfaceConfig.swift", utf8(buildConfigSwift()));
        map.put("CN1WidgetBundle.swift", utf8(buildBundleSwift()));
        return map;
    }

    /**
     * Builds the glue compiled into the MAIN APP target: the {@code CN1SurfaceBridge}
     * Objective-C-visible Swift class plus copies of the attributes/config files. The
     * caller writes these into the {@code <MainClass>-src} folder, which the generated
     * Xcode schemes script sweeps into the app target's compile sources.
     */
    public Map<String, byte[]> buildAppTargetFileMap() throws IOException {
        validate();
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        map.put("CN1SurfaceBridge.swift", utf8(loadResource("CN1SurfaceBridge.swift")));
        map.put("CN1SurfaceAttributes.swift", utf8(loadResource("CN1SurfaceAttributes.swift")));
        map.put("CN1SurfaceConfig.swift", utf8(buildConfigSwift()));
        return map;
    }

    private void validate() {
        if (extensionName == null || !isIdentifier(extensionName)) {
            throw new IllegalStateException(
                    "extension name must be ASCII letters/digits/_/- only: " + extensionName);
        }
        if (hostBundleId == null || hostBundleId.length() == 0) {
            throw new IllegalStateException("hostBundleId must be set");
        }
        if (appGroupId == null || !appGroupId.startsWith("group.")) {
            throw new IllegalStateException("appGroupId must start with 'group.' (Apple "
                    + "requirement; from surfaces.json or the ios.surfaces.appGroup build hint): "
                    + appGroupId);
        }
        if (kinds.isEmpty() && !liveActivitiesEnabled) {
            throw new IllegalStateException("surfaces.json declares neither widget kinds nor "
                    + "liveActivities: there is nothing to generate");
        }
        // WidgetBundleBuilder composes at most 10 widgets per bundle body; keeping the
        // generator single-bundle is simpler and 9 kinds is far beyond practical use.
        if (kinds.size() > (liveActivitiesEnabled ? 9 : 10)) {
            throw new IllegalStateException("surfaces.json declares more than "
                    + (liveActivitiesEnabled ? 9 : 10) + " widget kinds; a single WidgetBundle "
                    + "supports at most 10 widgets");
        }
        for (Kind kind : kinds) {
            if (kind.getId() == null || !isKindId(kind.getId())) {
                throw new IllegalStateException("widget kind ids must match [a-z][a-z0-9_]*: "
                        + kind.getId());
            }
        }
    }

    private static boolean isIdentifier(String s) {
        if (s.length() == 0) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (!ok) return false;
        }
        return true;
    }

    private static boolean isKindId(String s) {
        if (s.length() == 0) return false;
        char first = s.charAt(0);
        if (first < 'a' || first > 'z') return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) return false;
        }
        return true;
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String loadResource(String name) throws IOException {
        InputStream in = IOSWidgetExtensionBuilder.class.getResourceAsStream(RESOURCE_ROOT + name);
        if (in == null) {
            throw new IOException("Missing plugin resource " + RESOURCE_ROOT + name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private String buildInfoPlist() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        plistKeyString(sb, "CFBundleDevelopmentRegion", "en");
        plistKeyString(sb, "CFBundleDisplayName", extensionName);
        plistKeyString(sb, "CFBundleExecutable", "$(EXECUTABLE_NAME)");
        plistKeyString(sb, "CFBundleIdentifier", "$(PRODUCT_BUNDLE_IDENTIFIER)");
        plistKeyString(sb, "CFBundleInfoDictionaryVersion", "6.0");
        plistKeyString(sb, "CFBundleName", "$(PRODUCT_NAME)");
        plistKeyString(sb, "CFBundlePackageType", "$(PRODUCT_BUNDLE_PACKAGE_TYPE)");
        plistKeyString(sb, "CFBundleShortVersionString", "1.0");
        plistKeyString(sb, "CFBundleVersion", "1");
        plistKeyString(sb, APP_GROUP_PLIST_KEY, appGroupId);
        // No NSExtensionPrincipalClass: the @main CN1WidgetBundle is the entry point.
        // (NSSupportsLiveActivities belongs in the HOST APP's Info.plist, injected by
        // IPhoneBuilder, not here.)
        sb.append("    <key>NSExtension</key>\n");
        sb.append("    <dict>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>com.apple.widgetkit-extension</string>\n");
        sb.append("    </dict>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    private String buildEntitlements() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("    <key>com.apple.security.application-groups</key>\n");
        sb.append("    <array>\n");
        sb.append("        <string>").append(escapeXml(appGroupId)).append("</string>\n");
        sb.append("    </array>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    private String buildBuildSettings() {
        // These properties override the defaults synthesized by IPhoneBuilder when
        // wiring the extension target into Xcode (mirrors IOSShareExtensionBuilder).
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated by Codename One IOSWidgetExtensionBuilder.\n");
        sb.append("# Picked up by com.codename1.builders.IPhoneBuilder when the CN1Widgets\n");
        sb.append("# extension folder is wired into the generated Xcode project.\n");
        sb.append("IPHONEOS_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
        sb.append("SWIFT_VERSION=5.0\n");
        sb.append("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=YES\n");
        sb.append("SKIP_INSTALL=YES\n");
        sb.append("PRODUCT_BUNDLE_IDENTIFIER=").append(hostBundleId).append(".")
                .append(extensionName).append("\n");
        sb.append("CODE_SIGN_ENTITLEMENTS=").append(extensionName).append("/")
                .append(extensionName).append(".entitlements\n");
        sb.append("INFOPLIST_FILE=").append(extensionName).append("/Info.plist\n");
        return sb.toString();
    }

    private String buildConfigSwift() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Auto-generated by Codename One from surfaces.json / ios.surfaces.appGroup.\n");
        sb.append("// The provider, renderer and bridge resolve the shared App Group container\n");
        sb.append("// through this constant. Compiled into both the app and extension targets.\n");
        sb.append("import Foundation\n");
        sb.append("\n");
        sb.append("let cn1SurfacesAppGroup = \"").append(escapeSwift(appGroupId)).append("\"\n");
        return sb.toString();
    }

    private String buildBundleSwift() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("// Auto-generated by Codename One from surfaces.json. The @main entry point of\n");
        sb.append("// the CN1Widgets extension: one concrete widget struct per declared kind (the\n");
        sb.append("// Widget protocol requires init(), so kinds cannot share a parameterized\n");
        sb.append("// struct) plus the live activity widget when enabled.\n");
        sb.append("import SwiftUI\n");
        sb.append("import WidgetKit\n");
        sb.append("\n");
        sb.append("@main\n");
        sb.append("struct CN1WidgetBundle: WidgetBundle {\n");
        sb.append("    var body: some Widget {\n");
        for (Kind kind : kinds) {
            sb.append("        ").append(structName(kind)).append("()\n");
        }
        if (liveActivitiesEnabled) {
            sb.append("        CN1LiveActivityWidget()\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        for (Kind kind : kinds) {
            sb.append("\n");
            sb.append("struct ").append(structName(kind)).append(": Widget {\n");
            sb.append("    var body: some WidgetConfiguration {\n");
            sb.append("        cn1MakeWidgetConfiguration(\n");
            sb.append("                kind: \"").append(escapeSwift(kind.getId())).append("\",\n");
            sb.append("                displayName: \"").append(escapeSwift(kind.getName())).append("\",\n");
            sb.append("                description: \"").append(escapeSwift(kind.getDescription())).append("\",\n");
            sb.append("                families: [").append(familiesSwift(kind)).append("])\n");
            sb.append("    }\n");
            sb.append("}\n");
        }
        return sb.toString();
    }

    private static String structName(Kind kind) {
        return "CN1Widget_" + kind.getId();
    }

    private static String familiesSwift(Kind kind) {
        List<String> families = kind.getIosFamilies();
        StringBuilder sb = new StringBuilder();
        if (families != null) {
            for (String family : families) {
                String mapped = mapFamily(family);
                if (mapped != null && sb.indexOf(mapped) < 0) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(mapped);
                }
            }
        }
        if (sb.length() == 0) {
            // No (usable) family declaration: all three home-screen sizes.
            return ".systemSmall, .systemMedium, .systemLarge";
        }
        return sb.toString();
    }

    private static String mapFamily(String family) {
        if ("small".equals(family)) {
            return ".systemSmall";
        }
        if ("medium".equals(family)) {
            return ".systemMedium";
        }
        if ("large".equals(family)) {
            return ".systemLarge";
        }
        if ("lockscreen".equals(family)) {
            return ".accessoryRectangular";
        }
        // Unknown family names are skipped so newer manifests degrade gracefully.
        return null;
    }

    private static void plistKeyString(StringBuilder sb, String key, String value) {
        sb.append("    <key>").append(escapeXml(key)).append("</key>\n");
        sb.append("    <string>").append(escapeXml(value)).append("</string>\n");
    }

    private static String escapeXml(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':  out.append("&amp;"); break;
                case '<':  out.append("&lt;"); break;
                case '>':  out.append("&gt;"); break;
                case '"':  out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }

    private static String escapeSwift(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"':  out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }
}
