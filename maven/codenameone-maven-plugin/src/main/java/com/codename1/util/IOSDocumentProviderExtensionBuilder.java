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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the file provider app-extension target that the Codename One Apple builds wire into
 * the generated Xcode project when the app references {@code com.codename1.documents} (see the
 * {@code ios.documentProvider.*} build hints).
 *
 * <p>The extension is fully generic: the static Swift sources shipped as plugin resources under
 * {@code com/codename1/builders/documents/ios/} serve whatever index the app published into the
 * shared App Group container, so the only per-project code generated here is
 * {@code CN1DocumentConfig.swift} -- the app group id and display name the provider resolves its
 * container and its domain from.</p>
 *
 * <h2>Two providers, never both</h2>
 *
 * <p>Apple replaced {@code NSFileProviderExtension} with {@code NSFileProviderReplicatedExtension}
 * in iOS 16 / macOS 13. Both claim the same extension point, so a target may contain exactly one
 * of them: {@code CN1FileProviderExtension} above the floor and {@code CN1FileProviderClassic}
 * below it. {@link #usesReplicatedApi()} makes that choice from the deployment target, and macOS
 * always takes the replicated one -- its floor is below the oldest macOS this port supports, and
 * AppKit marks the classic API's string UTI unavailable.</p>
 *
 * <p>The extension's deployment target defaults to {@link #MIN_REPLICATED_IOS}; the host app's own
 * deployment target is unaffected, the extension simply never runs on older systems.</p>
 */
public class IOSDocumentProviderExtensionBuilder {

    /** Info.plist key holding the App Group id, read by extension and app alike. */
    public static final String APP_GROUP_PLIST_KEY = "CN1DocumentsAppGroup";

    /** The default extension target name, and so the last component of its bundle id. */
    public static final String DEFAULT_EXTENSION_NAME = "CN1Documents";

    /** Lowest iOS carrying {@code NSFileProviderReplicatedExtension}. */
    public static final String MIN_REPLICATED_IOS = "16.0";

    /** Lowest macOS carrying {@code NSFileProviderReplicatedExtension}. */
    public static final String MIN_REPLICATED_MACOS = "13.0";

    /** Classpath folder holding the static Swift sources. */
    private static final String RESOURCE_ROOT = "/com/codename1/builders/documents/ios/";

    /** Static Swift sources every flavour of the extension carries. */
    private static final String[] SHARED_SOURCES = {
        "CN1DocumentIndex.swift",
        "CN1DocumentItem.swift",
        "CN1DocumentEnumerator.swift",
        "CN1DocumentRemote.swift",
    };

    /** The iOS 16 / macOS 13 provider. */
    public static final String REPLICATED_SOURCE = "CN1FileProviderExtension.swift";

    /** The pre-iOS-16 provider. */
    public static final String CLASSIC_SOURCE = "CN1FileProviderClassic.swift";

    private String extensionName = DEFAULT_EXTENSION_NAME;
    private String appGroupId;
    private String hostBundleId;
    private String displayName;
    private String deploymentTarget = MIN_REPLICATED_IOS;
    private String shortVersion = "1.0";
    private String bundleVersion = "1";
    private boolean macTarget;

    public IOSDocumentProviderExtensionBuilder() {}

    /** Sets the extension target name, which is also the last component of its bundle id. */
    public IOSDocumentProviderExtensionBuilder setExtensionName(String name) {
        this.extensionName = name;
        return this;
    }

    /** Sets the App Group both the app and the extension resolve their container from. */
    public IOSDocumentProviderExtensionBuilder setAppGroupId(String id) {
        this.appGroupId = id;
        return this;
    }

    /** Sets the host app's bundle id; the extension's is this plus the target name. */
    public IOSDocumentProviderExtensionBuilder setHostBundleId(String id) {
        this.hostBundleId = id;
        return this;
    }

    /** Sets the name shown for this location in the file browser. */
    public IOSDocumentProviderExtensionBuilder setDisplayName(String name) {
        this.displayName = name;
        return this;
    }

    /** Sets the extension's deployment target, which also chooses which provider is generated. */
    public IOSDocumentProviderExtensionBuilder setDeploymentTarget(String target) {
        this.deploymentTarget = target;
        return this;
    }

    /** Sets the version strings the embedded bundle must match the host app on. */
    public IOSDocumentProviderExtensionBuilder setVersions(String shortVersion,
            String bundleVersion) {
        if (shortVersion != null) {
            this.shortVersion = shortVersion;
        }
        if (bundleVersion != null) {
            this.bundleVersion = bundleVersion;
        }
        return this;
    }

    /** Targets macOS (the AppKit port) rather than iOS. */
    public IOSDocumentProviderExtensionBuilder setMacTarget(boolean macTarget) {
        this.macTarget = macTarget;
        return this;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getAppGroupId() {
        return appGroupId;
    }

    public String getDeploymentTarget() {
        return deploymentTarget;
    }

    public boolean isMacTarget() {
        return macTarget;
    }

    /** The bundle id the extension target carries. */
    public String getBundleId() {
        return hostBundleId + "." + extensionName;
    }

    /**
     * Whether the replicated (iOS 16 / macOS 13) provider is generated rather than the classic one.
     *
     * <p>macOS is always replicated: every macOS this port supports is past the floor, and AppKit
     * marks the classic API's string UTI unavailable, so the classic sources do not build there
     * at all.</p>
     */
    public boolean usesReplicatedApi() {
        if (macTarget) {
            return true;
        }
        return compareVersions(deploymentTarget, MIN_REPLICATED_IOS) >= 0;
    }

    /**
     * The files making up the extension target, keyed by their path inside the target folder.
     *
     * @return the generated files
     * @throws IOException if a static source resource is missing from the plugin
     */
    public Map<String, byte[]> buildFileMap() throws IOException {
        validate();
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist()));
        map.put(extensionName + ".entitlements", utf8(buildEntitlements()));
        map.put("buildSettings.properties", utf8(buildBuildSettings()));
        for (String source : SHARED_SOURCES) {
            map.put(source, utf8(loadResource(source)));
        }
        map.put(usesReplicatedApi() ? REPLICATED_SOURCE : CLASSIC_SOURCE,
                utf8(loadResource(usesReplicatedApi() ? REPLICATED_SOURCE : CLASSIC_SOURCE)));
        map.put("CN1DocumentConfig.swift", utf8(buildConfigSwift()));
        return map;
    }

    private void validate() {
        if (extensionName == null || !isIdentifier(extensionName)) {
            throw new IllegalStateException("The document provider extension name must be a plain "
                    + "identifier, was: " + extensionName);
        }
        if (appGroupId == null || appGroupId.trim().length() == 0) {
            throw new IllegalStateException("The document provider extension needs an App Group: "
                    + "set ios.documentProvider.appGroup");
        }
        if (hostBundleId == null || hostBundleId.trim().length() == 0) {
            throw new IllegalStateException("The document provider extension needs the host app's "
                    + "bundle id to derive its own");
        }
        if (macTarget && compareVersions(deploymentTarget, MIN_REPLICATED_MACOS) < 0) {
            throw new IllegalStateException("The macOS document provider requires macOS "
                    + MIN_REPLICATED_MACOS + " or newer, was: " + deploymentTarget);
        }
    }

    private static boolean isIdentifier(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    private String buildConfigSwift() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Auto-generated by Codename One. Do not edit.\n");
        sb.append("import Foundation\n\n");
        sb.append("enum CN1DocumentConfig {\n");
        sb.append("    static let appGroupId = \"").append(escapeSwift(appGroupId)).append("\"\n");
        sb.append("    static let displayName = \"")
                .append(escapeSwift(displayName == null ? extensionName : displayName))
                .append("\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildInfoPlist() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        plistKeyString(sb, "CFBundleDevelopmentRegion", "en");
        plistKeyString(sb, "CFBundleDisplayName",
                displayName == null ? extensionName : displayName);
        plistKeyString(sb, "CFBundleExecutable", "$(EXECUTABLE_NAME)");
        plistKeyString(sb, "CFBundleIdentifier", "$(PRODUCT_BUNDLE_IDENTIFIER)");
        plistKeyString(sb, "CFBundleInfoDictionaryVersion", "6.0");
        plistKeyString(sb, "CFBundleName", "$(PRODUCT_NAME)");
        plistKeyString(sb, "CFBundlePackageType", "$(PRODUCT_BUNDLE_PACKAGE_TYPE)");
        plistKeyString(sb, "CFBundleShortVersionString", shortVersion);
        plistKeyString(sb, "CFBundleVersion", bundleVersion);
        plistKeyString(sb, APP_GROUP_PLIST_KEY, appGroupId);
        // The system resolves the provider's own storage from this key, and it has to name the
        // same group the Swift resolves its container from or the two look at different
        // directories and the browser shows an empty location.
        plistKeyString(sb, "NSExtensionFileProviderDocumentGroup", appGroupId);
        sb.append("    <key>NSExtension</key>\n");
        sb.append("    <dict>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>com.apple.fileprovider-nonui</string>\n");
        sb.append("        <key>NSExtensionPrincipalClass</key>\n");
        sb.append("        <string>$(PRODUCT_MODULE_NAME).")
                .append(usesReplicatedApi() ? "CN1FileProviderExtension" : "CN1FileProviderClassic")
                .append("</string>\n");
        if (!usesReplicatedApi()) {
            // Only the classic provider advertises this; the replicated API enumerates by
            // definition and the key is meaningless there.
            sb.append("        <key>NSExtensionFileProviderSupportsEnumeration</key>\n");
            sb.append("        <true/>\n");
        }
        sb.append("    </dict>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    private String buildEntitlements() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("    <key>com.apple.security.application-groups</key>\n");
        sb.append("    <array>\n");
        sb.append("        <string>").append(escapeXml(appGroupId)).append("</string>\n");
        sb.append("    </array>\n");
        if (macTarget) {
            // A macOS extension runs sandboxed, and without the network entitlement the remote
            // mode fails with an error the user reads as "the file is missing".
            sb.append("    <key>com.apple.security.app-sandbox</key>\n");
            sb.append("    <true/>\n");
            sb.append("    <key>com.apple.security.network.client</key>\n");
            sb.append("    <true/>\n");
        }
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    private String buildBuildSettings() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated by Codename One IOSDocumentProviderExtensionBuilder.\n");
        sb.append("# Picked up by com.codename1.builders.IPhoneBuilder when the ")
                .append(extensionName).append("\n");
        sb.append("# extension folder is wired into the generated Xcode project.\n");
        if (macTarget) {
            sb.append("MACOSX_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
            sb.append("SDKROOT=macosx\n");
            sb.append("SUPPORTED_PLATFORMS=macosx\n");
        } else {
            sb.append("IPHONEOS_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
        }
        sb.append("SWIFT_VERSION=5.0\n");
        sb.append("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=YES\n");
        sb.append("SKIP_INSTALL=YES\n");
        sb.append("PRODUCT_BUNDLE_IDENTIFIER=").append(getBundleId()).append("\n");
        sb.append("CODE_SIGN_ENTITLEMENTS=").append(extensionName).append("/")
                .append(extensionName).append(".entitlements\n");
        sb.append("INFOPLIST_FILE=").append(extensionName).append("/Info.plist\n");
        return sb.toString();
    }

    /**
     * Compares two dotted version strings numerically.
     *
     * <p>Deliberately not {@code String.compareTo}: that orders "16.0" below "9.0", which would
     * generate the classic provider for a modern deployment target and leave the extension inert
     * on every device that could have run the good one.</p>
     */
    static int compareVersions(String a, String b) {
        String[] left = split(a);
        String[] right = split(b);
        int n = Math.max(left.length, right.length);
        for (int i = 0; i < n; i++) {
            int l = i < left.length ? parse(left[i]) : 0;
            int r = i < right.length ? parse(right[i]) : 0;
            if (l != r) {
                return l < r ? -1 : 1;
            }
        }
        return 0;
    }

    private static String[] split(String v) {
        return v == null ? new String[0] : v.trim().split("\\.");
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException err) {
            return 0;
        }
    }

    private static void plistKeyString(StringBuilder sb, String key, String value) {
        sb.append("    <key>").append(escapeXml(key)).append("</key>\n");
        sb.append("    <string>").append(escapeXml(value)).append("</string>\n");
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String escapeSwift(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private static String loadResource(String name) throws IOException {
        InputStream in = IOSDocumentProviderExtensionBuilder.class
                .getResourceAsStream(RESOURCE_ROOT + name);
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

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
