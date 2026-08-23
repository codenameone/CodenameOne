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
 * Generates a WidgetKit app-extension target that the Codename One iOS build wires into the
 * generated Xcode project when the app references {@code com.codename1.surfaces} (see the
 * {@code surfaces.json} project manifest and the {@code ios.surfaces.*} build hints).
 *
 * <p>There are two flavours, selected with {@link #setWatchTarget(boolean)}. The default
 * {@code CN1Widgets} extension is embedded in the phone app and hosts home and lock-screen
 * widgets; the {@code CN1WatchWidgets} flavour is embedded in the watch app and hosts
 * complications. They are separate targets in one project and share every Swift source that
 * can be shared, differing in which WidgetKit families they may name -- see
 * {@link #mapFamily(String, boolean)} for why the two sets are not interchangeable.</p>
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
 *       live activities are enabled the iOS bundle also lists
 *       {@code CN1LiveActivityWidget()} unconditionally - the struct itself guards every
 *       ActivityKit reference with {@code #if canImport(ActivityKit)}, which keeps the
 *       composition simple and compiles cleanly on SDKs without ActivityKit. The watch
 *       flavour omits it entirely, watchOS having no ActivityKit at all.</li>
 * </ul>
 *
 * <p>{@link #buildAppTargetFileMap()} returns the glue compiled into the MAIN APP target
 * (the Swift {@code CN1SurfaceBridge} the Objective-C natives reach via
 * {@code NSClassFromString}, plus copies of the attributes/config files - ActivityKit
 * matches app and extension by the {@code ActivityAttributes} type, so both modules need
 * the identical struct).</p>
 *
 * <p>The iOS extension's deployment target defaults to 16.1 (ActivityKit's floor); the host
 * app's own deployment target is unaffected - the extension simply never runs on older iOS
 * versions. The watch flavour defaults to {@link #WATCH_MIN_DEPLOYMENT_TARGET} and refuses
 * anything below it.</p>
 */
public class IOSWidgetExtensionBuilder {

    /** Info.plist key holding the App Group id, read by extension and app alike. */
    public static final String APP_GROUP_PLIST_KEY = "CN1SurfacesAppGroup";

    /** Classpath folder holding the static Swift renderer sources. */
    private static final String RESOURCE_ROOT = "/com/codename1/builders/surfaces/ios/";

    /** Static Swift sources copied verbatim into the iOS extension target. */
    private static final String[] EXTENSION_SOURCES = {
        "CN1SurfaceModel.swift",
        "CN1SurfaceRenderer.swift",
        "CN1WidgetProvider.swift",
        "CN1DescriptorWidget.swift",
        "CN1SurfaceAttributes.swift",
    };

    /**
     * The same sources minus the two ActivityKit ones, for the watchOS extension target.
     *
     * <p>watchOS has no ActivityKit at all. Both files are already
     * {@code #if canImport(ActivityKit)} guarded, so shipping them would compile to nothing
     * rather than fail -- but a target carrying the attributes of a capability the platform
     * does not have is a claim, and the next person to read the target would believe it.</p>
     */
    private static final String[] WATCH_EXTENSION_SOURCES = {
        "CN1SurfaceModel.swift",
        "CN1SurfaceRenderer.swift",
        "CN1WidgetProvider.swift",
        "CN1DescriptorWidget.swift",
    };

    /**
     * Lowest watchOS the generated extension can target: WidgetKit's own floor.
     *
     * <p>This used to say 10.0, on the grounds that {@code containerBackground(for:)} is watchOS
     * 10 and every generated widget applies it. That is true of the API and not of the code:
     * {@code CN1DescriptorWidget} applies it inside {@code if #available(iOS 17.0, watchOS 10.0,
     * *)}, and an availability check compiles below the version it names -- that is what it is
     * for. Typechecking the whole extension against the watchOS 9 SDK confirms it, and holding
     * the floor at 10.0 excluded every watch still on 9 from a complication that would have
     * worked on it, losing only the background.</p>
     */
    public static final String WATCH_MIN_DEPLOYMENT_TARGET = "9.0";

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
    private boolean watchTarget;
    private final List<Kind> kinds = new ArrayList<Kind>();

    /** Bare-bones constructor. Configure with the fluent setters. */
    public IOSWidgetExtensionBuilder() {}

    /**
     * Sets the extension target name (Xcode target, .appex bundle and bundle-id suffix).
     * Must be an ASCII identifier. Defaults to {@code CN1Widgets}.
     */
    /**
     * The extension's marketing version. Defaults to the historical constant so a caller that
     * says nothing keeps its current output.
     */
    private String shortVersion = "1.0";

    /** The extension's build version; see {@link #shortVersion}. */
    private String bundleVersion = "1";

    /**
     * Sets the versions this extension declares.
     *
     * <p>Apple validates an embedded bundle's versions against the app that contains it, and an
     * extension pinned to 1.0/1 inside an app at any other version is rejected at submission --
     * a failure that appears only when the archive is uploaded, long after every build has gone
     * green. The container's resolved values are the ones to pass; they are not simply the
     * project version, because {@code ios.plistInject} and {@code ios.bundleVersion} both get a
     * say in what the app itself ends up declaring.</p>
     *
     * @param shortVersionValue the containing app's CFBundleShortVersionString
     * @param bundleVersionValue the containing app's CFBundleVersion
     * @return this builder
     */
    public IOSWidgetExtensionBuilder setVersions(String shortVersionValue,
            String bundleVersionValue) {
        if (shortVersionValue != null && shortVersionValue.length() > 0) {
            this.shortVersion = shortVersionValue;
        }
        if (bundleVersionValue != null && bundleVersionValue.length() > 0) {
            this.bundleVersion = bundleVersionValue;
        }
        return this;
    }

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

    /**
     * Builds the watchOS flavour of the extension rather than the iOS one.
     *
     * <p>The two are separate targets in the same project, embedded in different apps: the iOS
     * extension rides in the phone app and hosts home and lock-screen widgets, the watch one
     * rides in the watch app and hosts complications. They share every Swift source that can
     * be shared, and differ in which families they may name -- see
     * {@link #mapFamily(String, boolean)}.</p>
     *
     * @param watch true to generate the watch flavour
     * @return this builder
     */
    public IOSWidgetExtensionBuilder setWatchTarget(boolean watch) {
        this.watchTarget = watch;
        if (watch && "16.1".equals(deploymentTarget)) {
            // The iOS default is meaningless on the watch and would be rejected below.
            this.deploymentTarget = WATCH_MIN_DEPLOYMENT_TARGET;
        }
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
    public boolean isWatchTarget() { return watchTarget; }
    public List<Kind> getKinds() { return kinds; }

    /**
     * Builds the in-memory file map of the extension target, keyed by relative path
     * inside the extension folder.
     */
    public Map<String, byte[]> buildFileMap() throws IOException {
        validate();
        if (!hasSurface()) {
            // Every declared kind is a watch complication and there is no live activity, so nothing
            // would reach the bundle body -- and a WidgetBundle whose body holds no Widget expression
            // does not compile. Callers check hasIosSurface() and skip the extension; reaching here
            // means that check was missed, and failing loudly beats emitting Swift that breaks the
            // whole iOS build.
            //
            // Deliberately here rather than in validate(): the APP-target glue is still wanted when
            // the app publishes surfaces that only a watch can show, so buildAppTargetFileMap() must
            // not be blocked by this.
            throw new IllegalStateException((watchTarget
                    ? "the watchOS widget extension would be empty: no kind declares a watch "
                            + "complication family. Check hasWatchSurface() before "
                    : "the iOS widget extension would be empty: every kind declares only watch "
                            + "complication families. Check hasIosSurface() before ")
                    + "generating the extension");
        }
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist()));
        map.put(extensionName + ".entitlements", utf8(buildEntitlements()));
        map.put("buildSettings.properties", utf8(buildBuildSettings()));
        for (String source : (watchTarget ? WATCH_EXTENSION_SOURCES : EXTENSION_SOURCES)) {
            map.put(source, utf8(loadResource(source)));
        }
        // Live activities are an iOS capability; watchOS has no ActivityKit.
        if (liveActivitiesEnabled && !watchTarget) {
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
        // Only the kinds that actually reach the bundle count against the limit. Watch-only kinds
        // are skipped when it is generated, so counting them here would reject a manifest that
        // produces a perfectly legal bundle -- ten complications plus one iOS widget is one widget.
        int emitted = 0;
        for (Kind kind : kinds) {
            if (watchTarget ? hasWatchFamily(kind) : !isWatchOnly(kind)) {
                emitted++;
            }
        }
        // The live activity occupies a slot in the iOS bundle only; the watch has none.
        int limit = (liveActivitiesEnabled && !watchTarget) ? 9 : 10;
        if (emitted > limit) {
            throw new IllegalStateException("surfaces.json declares more than " + limit
                    + " widget kinds with " + (watchTarget ? "a watch" : "an iOS") + " surface; a "
                    + "single WidgetBundle supports at most 10 widgets");
        }
        if (watchTarget && compareVersions(deploymentTarget, WATCH_MIN_DEPLOYMENT_TARGET) < 0) {
            throw new IllegalStateException("the watch widget extension cannot target watchOS "
                    + deploymentTarget + ": WidgetKit's accessory families arrive in watchOS "
                    + WATCH_MIN_DEPLOYMENT_TARGET + ", so there is no complication to build "
                    + "below it");
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
        plistKeyString(sb, "CFBundleShortVersionString", shortVersion);
        plistKeyString(sb, "CFBundleVersion", bundleVersion);
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
        if (watchTarget) {
            sb.append("WATCHOS_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
            sb.append("SDKROOT=watchos\n");
            sb.append("SUPPORTED_PLATFORMS=watchos watchsimulator\n");
            // 4 is the watch device family. Without it the extension builds for the phone
            // families and is rejected when the watch app tries to embed it.
            sb.append("TARGETED_DEVICE_FAMILY=4\n");
            // The '=' inside the KEY is escaped, because this file is read back with
            // Properties.load and that treats the first unescaped '=' as the separator: the key
            // parsed as "ARCHS[sdk" with value "watchos*]=arm64_32", so the conditional setting
            // Xcode needs was never applied and the extension took whatever architectures the
            // containing project supplies -- phone ones, for a watch target.
            sb.append("ARCHS[sdk\\=watchos*]=arm64_32\n");
        } else {
            sb.append("IPHONEOS_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
        }
        sb.append("SWIFT_VERSION=5.0\n");
        // The watch app embeds the Swift runtime once, for itself and everything nested inside
        // it. An extension that embeds its own copy is dead weight in the bundle and can fail
        // validation, so the iOS answer here is the wrong one for a nested watch extension.
        sb.append("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=")
                .append(watchTarget ? "NO" : "YES").append("\n");
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
        sb.append("let cn1SurfaceScheme = \"").append(escapeSwift(surfaceScheme())).append("\"\n");
        return sb.toString();
    }

    /**
     * The app's own deep-link scheme for surface taps.
     *
     * <p>A URL scheme is a GLOBAL registration. Every Codename One app used to claim the bare
     * {@code cn1surface}, so two of them installed together were two claims on one name -- and on
     * the watch, where the complication's widgetURL is routed by nothing else, the tap could open
     * whichever bundle the system decided owned it. Any other app can also claim a known scheme
     * and hand us whatever src and id it likes.</p>
     *
     * <p>Qualifying it with the host bundle id makes the claim as unique as the bundle id itself,
     * which is the strongest uniqueness Apple offers. Dots are legal in a scheme (RFC 3986 allows
     * ALPHA, DIGIT, "+", "-" and "."), and reverse-DNS schemes are ordinary on Apple platforms.
     * This does not make the payload trusted -- a scheme never can -- but it stops a tap landing
     * in the wrong app, which is the part that broke without anyone being hostile.</p>
     *
     * @return the scheme this build registers and generates
     */
    public String surfaceScheme() {
        return surfaceScheme(hostBundleId);
    }

    /**
     * The scheme for a host bundle id, so the builders can register what this generates.
     *
     * @param hostBundleId the bundle id the surfaces belong to
     * @return the scheme
     */
    public static String surfaceScheme(String hostBundleId) {
        if (hostBundleId == null || hostBundleId.length() == 0) {
            return "cn1surface";
        }
        StringBuilder out = new StringBuilder("cn1surface.");
        for (int i = 0; i < hostBundleId.length(); i++) {
            char c = hostBundleId.charAt(i);
            // The scheme grammar, applied rather than assumed: a bundle id is normally already
            // within it, and anything outside becomes '-' so the result stays a legal scheme
            // instead of a plist Xcode rejects.
            boolean legal = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
            out.append(legal ? c : '-');
        }
        return out.toString();
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
            if (!hostsKind(kind)) {
                continue;
            }
            sb.append("        ").append(structName(kind)).append("()\n");
        }
        if (liveActivitiesEnabled && !watchTarget) {
            sb.append("        CN1LiveActivityWidget()\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        for (Kind kind : kinds) {
            if (!hostsKind(kind)) {
                // Nothing here to host it. In the iOS target that is a kind declaring only
                // complication families -- emitting it would fall through to the default
                // home-screen sizes and ship an iPhone widget the manifest never asked for. In
                // the watch target it is a kind declaring no complication family at all.
                continue;
            }
            sb.append("\n");
            sb.append("struct ").append(structName(kind)).append(": Widget {\n");
            sb.append("    var body: some WidgetConfiguration {\n");
            sb.append("        cn1MakeWidgetConfiguration(\n");
            sb.append("                kind: \"").append(escapeSwift(kind.getId())).append("\",\n");
            sb.append("                displayName: \"").append(escapeSwift(kind.getName())).append("\",\n");
            sb.append("                description: \"").append(escapeSwift(kind.getDescription())).append("\",\n");
            // .accessoryCorner exists only on watchOS. In the watch target that is simply one
            // more family in the list; in the iOS target it is emitted behind a platform guard,
            // because naming the symbol on iOS would not compile even in code that never runs.
            String shared = familiesSwift(kind, watchTarget);
            String watchOnly = watchOnlyFamiliesSwift(kind, watchTarget);
            if (watchOnly.length() == 0) {
                sb.append("                families: [").append(shared).append("])\n");
            } else if (watchTarget) {
                // Watch-only target: no guard, the corner family just joins the list.
                sb.append("                families: [").append(shared);
                if (shared.length() > 0) {
                    sb.append(", ");
                }
                sb.append(watchOnly).append("])\n");
            } else {
                sb.append("#if os(watchOS)\n");
                sb.append("                families: [").append(shared);
                if (shared.length() > 0) {
                    sb.append(", ");
                }
                sb.append(watchOnly).append("])\n");
                sb.append("#else\n");
                sb.append("                families: [").append(shared).append("])\n");
                sb.append("#endif\n");
            }
            sb.append("    }\n");
            sb.append("}\n");
        }
        return sb.toString();
    }

    private static String structName(Kind kind) {
        return "CN1Widget_" + kind.getId();
    }

    /// Whether this flavour of the extension has a surface for the kind: a watch target hosts
    /// the kinds declaring a complication family, an iOS target hosts everything else.
    private boolean hostsKind(Kind kind) {
        return watchTarget ? hasWatchFamily(kind) : !isWatchOnly(kind);
    }

    private static String familiesSwift(Kind kind, boolean watchTarget) {
        List<String> families = kind.getIosFamilies();
        StringBuilder sb = new StringBuilder();
        if (families != null) {
            for (String family : families) {
                String mapped = mapFamily(family, watchTarget);
                if (mapped != null && sb.indexOf(mapped) < 0) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(mapped);
                }
            }
        }
        if (sb.length() == 0) {
            if (watchTarget) {
                // The home-screen default is meaningless here and unnameable besides. A kind
                // with no usable watch family is skipped by the caller, which hasWatchFamily()
                // has already decided, so this is the empty-list case rather than a fallback.
                return "";
            }
            // No (usable) family declaration: all three home-screen sizes.
            return ".systemSmall, .systemMedium, .systemLarge";
        }
        return sb.toString();
    }

    /// The families that exist only on watchOS.
    ///
    /// Confined to a watch target: the corner complication has no iOS surface, so emitting it --
    /// and the platform guard that carried it -- into the iOS extension would advertise something
    /// the manifest never asked for. Inside the watch target no guard is needed at all, because
    /// the target's SUPPORTED_PLATFORMS is watchOS alone.
    private static String watchOnlyFamiliesSwift(Kind kind, boolean watchTarget) {
        if (!watchTarget) {
            return "";
        }
        List<String> families = kind.getIosFamilies();
        if (families != null) {
            for (String family : families) {
                if ("watchCorner".equals(normalizeFamily(family))) {
                    return ".accessoryCorner";
                }
            }
        }
        return "";
    }

    /// The portable family name for a declaration, resolving the WidgetKit spellings.
    ///
    /// Kept as a method here because this class and its tests read it by this name; the rule
    /// itself, and the long account of what getting it wrong costs, lives in
    /// [SurfaceKindFamilies#normalize(String)] so the Android builder applies exactly the same one.
    static String normalizeFamily(String family) {
        return SurfaceKindFamilies.normalize(family);
    }

    private static String mapFamily(String rawFamily, boolean watchTarget) {
        // Both the portable names (matching the core WidgetSize wire names) and the
        // WidgetKit-style spellings are accepted, so manifests written against either
        // naming in the docs resolve to the same families.
        String family = normalizeFamily(rawFamily);
        // The phone families have no watch surface, and the three system ones cannot even be
        // NAMED there: WidgetFamily.systemSmall and friends are @available(watchOS, unavailable),
        // so emitting one into the watch bundle fails the build rather than producing a widget
        // nobody sees. lockscreen joins them because an iPhone lock screen is not a watch face.
        if (watchTarget) {
            if ("small".equals(family) || "systemSmall".equals(family)
                    || "medium".equals(family) || "systemMedium".equals(family)
                    || "large".equals(family) || "systemLarge".equals(family)
                    || "lockscreen".equals(family)) {
                return null;
            }
            // The accessory spellings too, and for the reason hasWatchFamily already encodes:
            // they are NOT watch families here. A kind declaring only accessoryCircular produces
            // no watch extension at all, so letting one INTO a watch extension that some other
            // family opened is the system contradicting itself -- a kind asking for a lock-screen
            // circular and a rectangular complication got a circular complication it never asked
            // for. Nothing is lost by refusing them: every accessory family the watch can show
            // has a watch* name that maps to it, which is how a developer says they want it there.
            if ("accessoryCircular".equals(family) || "accessoryInline".equals(family)
                    || "accessoryRectangular".equals(family)) {
                return null;
            }
        }
        if ("small".equals(family) || "systemSmall".equals(family)) {
            return ".systemSmall";
        }
        if ("medium".equals(family) || "systemMedium".equals(family)) {
            return ".systemMedium";
        }
        if ("large".equals(family) || "systemLarge".equals(family)) {
            return ".systemLarge";
        }
        if ("lockscreen".equals(family) || "accessoryRectangular".equals(family)) {
            return ".accessoryRectangular";
        }
        // The other two shared families, in their WidgetKit spelling. Available on the iPhone lock
        // screen from iOS 16 and on the watch from watchOS 9, and the Swift renderer handles both
        // on either platform -- so unlike the portable watch* names these are NOT confined to the
        // watch target.
        if ("accessoryCircular".equals(family)) {
            return ".accessoryCircular";
        }
        if ("accessoryInline".equals(family)) {
            return ".accessoryInline";
        }
        // Watch complications. On Apple a complication is a WidgetKit widget in an accessory
        // family, which is why they map here rather than through an API of their own.
        // watchRectangular shares .accessoryRectangular with the lock screen -- the Swift renderer
        // picks the more specific published layout when both exist.
        //
        // They belong to the watch flavour of the extension only. Mapping them into the iOS target
        // would put a complication in front of the user as an iPhone lock-screen widget, which is
        // not the surface the manifest asked for.
        if (family.startsWith("watch") && !watchTarget) {
            return null;
        }
        if ("watchCircular".equals(family)) {
            return ".accessoryCircular";
        }
        if ("watchRectangular".equals(family)) {
            return ".accessoryRectangular";
        }
        if ("watchInline".equals(family)) {
            return ".accessoryInline";
        }
        if ("watchCorner".equals(family)) {
            // Emitted separately behind an os(watchOS) guard; see watchOnlyFamiliesSwift.
            return null;
        }
        // Unknown family names are skipped so newer manifests degrade gracefully.
        return null;
    }

    /// True when the kind declares at least one watch complication family, which is what decides
    /// whether the watch flavour of the extension hosts it.
    ///
    /// @param kind the kind to inspect
    /// @return true if the kind offers a complication
    /// True when a kind declares complication families and nothing else, so the iOS extension has no
    /// surface to offer it. Distinct from {@link #hasWatchFamily}, which is true for a kind that
    /// offers both a phone widget and a complication.
    ///
    /// @param kind the kind to inspect
    /// @return true if every declared family is a watch family
    /// Whether the iOS widget extension would host anything at all: at least one kind with an iOS
    /// family, or live activities. False means the extension should not be generated -- a project may
    /// legitimately declare only watch complications, and that should produce no iOS surface rather
    /// than a build failure.
    ///
    /// @return true if there is something for the iOS extension to show
    public boolean hasIosSurface() {
        if (liveActivitiesEnabled) {
            return true;
        }
        for (Kind kind : kinds) {
            if (!isWatchOnly(kind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether THIS flavour of the extension would host anything, so the caller can skip
     * generating a target that has nothing to show.
     *
     * @return true if there is something for this extension to host
     */
    public boolean hasSurface() {
        return watchTarget ? hasWatchSurface() : hasIosSurface();
    }

    /**
     * Whether the watch widget extension would host anything: at least one kind declaring a
     * watch complication family.
     *
     * <p>Live activities never count -- watchOS has no ActivityKit -- so unlike
     * {@link #hasIosSurface()} this is decided by the kinds alone.</p>
     *
     * @return true if there is a complication to show
     */
    public boolean hasWatchSurface() {
        for (Kind kind : kinds) {
            if (hasWatchFamily(kind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares two dotted version strings numerically, so "10.0" orders above "9.0" as it
     * would not under string comparison.
     *
     * @param a left version
     * @param b right version
     * @return negative, zero or positive as a orders below, with or above b
     */
    private static int compareVersions(String a, String b) {
        String[] left = (a == null ? "" : a).split("\\.");
        String[] right = (b == null ? "" : b).split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = parsePart(left, i);
            int r = parsePart(right, i);
            if (l != r) {
                return l < r ? -1 : 1;
            }
        }
        return 0;
    }

    private static int parsePart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static boolean isWatchOnly(Kind kind) {
        return SurfaceKindFamilies.isWatchOnly(kind.getIosFamilies());
    }

    public static boolean hasWatchFamily(Kind kind) {
        return SurfaceKindFamilies.hasWatchFamily(kind.getIosFamilies());
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
