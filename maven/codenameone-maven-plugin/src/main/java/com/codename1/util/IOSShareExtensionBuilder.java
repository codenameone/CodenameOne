/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates an iOS Share Extension bundle ready to be picked up by
 * {@code IPhoneBuilder} during the Codename One iOS build.
 *
 * <p>iOS share extensions are independent app targets that the host OS
 * presents in the system share sheet (UIActivityViewController) when a
 * user shares from another app. Adding one to a Codename One project
 * requires:</p>
 *
 * <ul>
 *   <li>An {@code Info.plist} containing the NSExtension dictionary
 *       describing activation rules and the principal class.</li>
 *   <li>An entitlements file declaring a shared App Group identifier so
 *       the extension can write payloads readable by the host app.</li>
 *   <li>A Swift {@code ShareViewController} (subclassing
 *       {@code SLComposeServiceViewController}) that extracts the shared
 *       items from {@code extensionContext} and persists them into the
 *       App Group's shared {@code NSUserDefaults} or container.</li>
 *   <li>A {@code buildSettings.properties} file so the Xcode target uses
 *       the correct provisioning profile and entitlements.</li>
 * </ul>
 *
 * <p>The Codename One build pipeline (see {@code IPhoneBuilder}) extracts
 * any file under {@code src/main/resources/} ending in {@code .ios.appext}
 * (a zip archive of the files above) and wires it into the generated
 * Xcode project as an {@code app_extension} target. This class writes
 * exactly that archive (or the staged directory used to build it).</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * new IOSShareExtensionBuilder()
 *     .setExtensionName("MyShareExtension")
 *     .setDisplayName("Share to MyApp")
 *     .setHostBundleId("com.example.myapp")
 *     .setAppGroupId("group.com.example.myapp.shared")
 *     .acceptText(true)
 *     .acceptURLs(true)
 *     .acceptImages(true)
 *     .writeAppext(new File("src/main/resources/MyShareExtension.ios.appext"));
 * }</pre>
 *
 * <p>The host app reads the payload at next launch via NSUserDefaults
 * with the same suite name (App Group id). The generated Swift code uses
 * the key {@code cn1.shareExtension.payload} for the most recent shared
 * item.</p>
 *
 * <p>This class produces ASCII-only output; the generated Swift, plist
 * and entitlements files are deterministic given the same inputs.</p>
 *
 * @since 9.0
 */
public final class IOSShareExtensionBuilder {

    /** UserDefaults key used by the generated extension to publish its payload. */
    public static final String PAYLOAD_KEY = "cn1.shareExtension.payload";

    private String extensionName = "ShareExtension";
    private String displayName;
    private String hostBundleId;
    private String appGroupId;
    private boolean acceptText = true;
    private boolean acceptUrls;
    private boolean acceptImages;
    private int maxItemsPerActivation = 1;
    private String deploymentTarget = "12.0";

    /** Bare-bones constructor. Configure with the fluent setters. */
    public IOSShareExtensionBuilder() {}

    /**
     * Sets the extension target name. This is the Xcode target name, the
     * .appex bundle name and the on-disk directory name. Must be a
     * non-empty ASCII identifier (letters, digits, hyphens, underscores).
     *
     * @param name extension target name
     * @return this
     */
    public IOSShareExtensionBuilder setExtensionName(String name) {
        this.extensionName = name;
        return this;
    }

    /**
     * Sets the user-visible name shown in the share sheet. If not set,
     * the extension name is used.
     */
    public IOSShareExtensionBuilder setDisplayName(String name) {
        this.displayName = name;
        return this;
    }

    /**
     * The host iOS app's bundle identifier (the main app the extension
     * belongs to). Used to derive the extension bundle id and as a
     * sanity check. Required.
     */
    public IOSShareExtensionBuilder setHostBundleId(String id) {
        this.hostBundleId = id;
        return this;
    }

    /**
     * The App Group identifier shared between host app and extension.
     * Apple requires that this starts with {@code group.}. Required.
     */
    public IOSShareExtensionBuilder setAppGroupId(String id) {
        this.appGroupId = id;
        return this;
    }

    /** Activation rule: accept plain-text items. Default true. */
    public IOSShareExtensionBuilder acceptText(boolean accept) {
        this.acceptText = accept;
        return this;
    }

    /** Activation rule: accept URL items. Default false. */
    public IOSShareExtensionBuilder acceptURLs(boolean accept) {
        this.acceptUrls = accept;
        return this;
    }

    /** Activation rule: accept image items. Default false. */
    public IOSShareExtensionBuilder acceptImages(boolean accept) {
        this.acceptImages = accept;
        return this;
    }

    /**
     * Maximum number of items per activation that the extension will
     * advertise. Apple defaults to 1; set higher for batch-friendly
     * extensions. Negative values are clamped to 1.
     */
    public IOSShareExtensionBuilder setMaxItemsPerActivation(int max) {
        this.maxItemsPerActivation = max < 1 ? 1 : max;
        return this;
    }

    /**
     * iOS deployment target for the extension target. Defaults to
     * {@code 12.0}.
     */
    public IOSShareExtensionBuilder setDeploymentTarget(String target) {
        this.deploymentTarget = target;
        return this;
    }

    // --- accessors used by callers/tests -------------------------------------

    public String getExtensionName() { return extensionName; }
    public String getDisplayName() { return displayName != null ? displayName : extensionName; }
    public String getHostBundleId() { return hostBundleId; }
    public String getAppGroupId() { return appGroupId; }
    public boolean isAcceptText() { return acceptText; }
    public boolean isAcceptURLs() { return acceptUrls; }
    public boolean isAcceptImages() { return acceptImages; }
    public int getMaxItemsPerActivation() { return maxItemsPerActivation; }
    public String getDeploymentTarget() { return deploymentTarget; }

    /**
     * Writes the share extension's source files into {@code outputDir}.
     * Existing files inside {@code outputDir} are overwritten; siblings
     * outside the canonical set are left untouched.
     *
     * @param outputDir directory to populate. Created if missing.
     * @return the file map written, keyed by relative path inside the
     *         extension bundle.
     * @throws IOException on I/O failure
     * @throws IllegalStateException if required setters were not invoked
     */
    public java.util.Map<String, byte[]> writeTo(File outputDir) throws IOException {
        validate();
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create " + outputDir);
        }
        if (!outputDir.isDirectory()) {
            throw new IOException(outputDir + " is not a directory");
        }
        java.util.Map<String, byte[]> files = buildFileMap();
        for (java.util.Map.Entry<String, byte[]> e : files.entrySet()) {
            File target = new File(outputDir, e.getKey());
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create " + parent);
            }
            FileOutputStream fos = new FileOutputStream(target);
            try {
                fos.write(e.getValue());
            } finally {
                fos.close();
            }
        }
        return files;
    }

    /**
     * Writes the share extension as a single {@code .ios.appext} zip
     * archive. The archive layout matches what
     * {@code IPhoneBuilder.extractAppExtensions} expects: each entry sits
     * at the archive root, no leading folder.
     *
     * @param outputZip target archive. Parent directory created if
     *                  missing. Existing file is overwritten.
     * @return the file map written
     * @throws IOException on I/O failure
     */
    public java.util.Map<String, byte[]> writeAppext(File outputZip) throws IOException {
        validate();
        if (outputZip == null) {
            throw new IllegalArgumentException("outputZip must not be null");
        }
        File parent = outputZip.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        java.util.Map<String, byte[]> files = buildFileMap();
        FileOutputStream fos = new FileOutputStream(outputZip);
        try {
            ZipOutputStream zos = new ZipOutputStream(fos);
            try {
                for (java.util.Map.Entry<String, byte[]> e : files.entrySet()) {
                    ZipEntry entry = new ZipEntry(e.getKey());
                    zos.putNextEntry(entry);
                    zos.write(e.getValue());
                    zos.closeEntry();
                }
            } finally {
                zos.close();
            }
        } finally {
            try { fos.close(); } catch (IOException ignore) {}
        }
        return files;
    }

    private void validate() {
        if (extensionName == null || extensionName.length() == 0) {
            throw new IllegalStateException("extensionName must be set");
        }
        if (!isIdentifier(extensionName)) {
            throw new IllegalStateException(
                    "extensionName must be ASCII letters/digits/_/- only: " + extensionName);
        }
        if (hostBundleId == null || hostBundleId.length() == 0) {
            throw new IllegalStateException("hostBundleId must be set");
        }
        if (appGroupId == null || !appGroupId.startsWith("group.")) {
            throw new IllegalStateException(
                    "appGroupId must start with 'group.' (Apple requirement): " + appGroupId);
        }
        if (!acceptText && !acceptUrls && !acceptImages) {
            throw new IllegalStateException(
                    "At least one of acceptText / acceptURLs / acceptImages must be enabled");
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

    /**
     * Builds the in-memory file map. Public for unit testing; production
     * code should call {@link #writeTo} or {@link #writeAppext} instead.
     */
    public java.util.Map<String, byte[]> buildFileMap() {
        validate();
        java.util.LinkedHashMap<String, byte[]> map = new java.util.LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist()));
        map.put(extensionName + ".entitlements", utf8(buildEntitlements()));
        map.put("ShareViewController.swift", utf8(buildShareViewController()));
        map.put("buildSettings.properties", utf8(buildBuildSettings()));
        return map;
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String buildInfoPlist() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        plistKeyString(sb, "CFBundleDevelopmentRegion", "en");
        plistKeyString(sb, "CFBundleDisplayName", getDisplayName());
        plistKeyString(sb, "CFBundleExecutable", "$(EXECUTABLE_NAME)");
        plistKeyString(sb, "CFBundleIdentifier", "$(PRODUCT_BUNDLE_IDENTIFIER)");
        plistKeyString(sb, "CFBundleInfoDictionaryVersion", "6.0");
        plistKeyString(sb, "CFBundleName", "$(PRODUCT_NAME)");
        plistKeyString(sb, "CFBundlePackageType", "$(PRODUCT_BUNDLE_PACKAGE_TYPE)");
        plistKeyString(sb, "CFBundleShortVersionString", "1.0");
        plistKeyString(sb, "CFBundleVersion", "1");
        sb.append("    <key>NSExtension</key>\n");
        sb.append("    <dict>\n");
        sb.append("        <key>NSExtensionAttributes</key>\n");
        sb.append("        <dict>\n");
        sb.append("            <key>NSExtensionActivationRule</key>\n");
        sb.append("            <dict>\n");
        if (acceptText) {
            sb.append("                <key>NSExtensionActivationSupportsText</key>\n");
            sb.append("                <true/>\n");
        }
        if (acceptUrls) {
            sb.append("                <key>NSExtensionActivationSupportsWebURLWithMaxCount</key>\n");
            sb.append("                <integer>").append(maxItemsPerActivation).append("</integer>\n");
            sb.append("                <key>NSExtensionActivationSupportsWebPageWithMaxCount</key>\n");
            sb.append("                <integer>").append(maxItemsPerActivation).append("</integer>\n");
        }
        if (acceptImages) {
            sb.append("                <key>NSExtensionActivationSupportsImageWithMaxCount</key>\n");
            sb.append("                <integer>").append(maxItemsPerActivation).append("</integer>\n");
        }
        sb.append("            </dict>\n");
        sb.append("        </dict>\n");
        sb.append("        <key>NSExtensionMainStoryboard</key>\n");
        sb.append("        <string>MainInterface</string>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>com.apple.share-services</string>\n");
        sb.append("        <key>NSExtensionPrincipalClass</key>\n");
        sb.append("        <string>$(PRODUCT_MODULE_NAME).ShareViewController</string>\n");
        sb.append("    </dict>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
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

    private String buildShareViewController() {
        // The generated controller subclasses SLComposeServiceViewController
        // so we get the standard sheet for free. didSelectPost iterates
        // every attachment, normalises it to a string payload, and writes
        // the latest payload to NSUserDefaults(suiteName:) for the host
        // app to pick up on next launch.
        StringBuilder sb = new StringBuilder(2048);
        sb.append("import UIKit\n");
        sb.append("import Social\n");
        sb.append("import MobileCoreServices\n");
        sb.append("import UniformTypeIdentifiers\n");
        sb.append("\n");
        sb.append("/// Auto-generated by Codename One IOSShareExtensionBuilder.\n");
        sb.append("/// Persists the shared payload into App Group \"")
                .append(appGroupId).append("\" under key \"")
                .append(PAYLOAD_KEY).append("\".\n");
        sb.append("class ShareViewController: SLComposeServiceViewController {\n");
        sb.append("\n");
        sb.append("    private let appGroupId = \"").append(appGroupId).append("\"\n");
        sb.append("    private let payloadKey = \"").append(PAYLOAD_KEY).append("\"\n");
        sb.append("\n");
        sb.append("    override func isContentValid() -> Bool {\n");
        sb.append("        return true\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    override func didSelectPost() {\n");
        sb.append("        let composedText = self.contentText ?? \"\"\n");
        sb.append("        var collected: [[String: Any]] = []\n");
        sb.append("        let group = DispatchGroup()\n");
        sb.append("        if let items = self.extensionContext?.inputItems as? [NSExtensionItem] {\n");
        sb.append("            for item in items {\n");
        sb.append("                guard let attachments = item.attachments else { continue }\n");
        sb.append("                for provider in attachments {\n");
        sb.append("                    if provider.hasItemConformingToTypeIdentifier(\"public.url\") {\n");
        sb.append("                        group.enter()\n");
        sb.append("                        provider.loadItem(forTypeIdentifier: \"public.url\", options: nil) { (data, _) in\n");
        sb.append("                            if let u = data as? URL {\n");
        sb.append("                                collected.append([\"kind\": \"url\", \"value\": u.absoluteString])\n");
        sb.append("                            }\n");
        sb.append("                            group.leave()\n");
        sb.append("                        }\n");
        sb.append("                    } else if provider.hasItemConformingToTypeIdentifier(\"public.plain-text\") {\n");
        sb.append("                        group.enter()\n");
        sb.append("                        provider.loadItem(forTypeIdentifier: \"public.plain-text\", options: nil) { (data, _) in\n");
        sb.append("                            if let s = data as? String {\n");
        sb.append("                                collected.append([\"kind\": \"text\", \"value\": s])\n");
        sb.append("                            }\n");
        sb.append("                            group.leave()\n");
        sb.append("                        }\n");
        sb.append("                    } else if provider.hasItemConformingToTypeIdentifier(\"public.image\") {\n");
        sb.append("                        group.enter()\n");
        sb.append("                        provider.loadItem(forTypeIdentifier: \"public.image\", options: nil) { (data, _) in\n");
        sb.append("                            if let u = data as? URL {\n");
        sb.append("                                collected.append([\"kind\": \"image\", \"value\": u.absoluteString])\n");
        sb.append("                            }\n");
        sb.append("                            group.leave()\n");
        sb.append("                        }\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        group.notify(queue: .main) {\n");
        sb.append("            let payload: [String: Any] = [\n");
        sb.append("                \"text\": composedText,\n");
        sb.append("                \"items\": collected,\n");
        sb.append("                \"timestamp\": Date().timeIntervalSince1970\n");
        sb.append("            ]\n");
        sb.append("            if let defaults = UserDefaults(suiteName: self.appGroupId) {\n");
        sb.append("                defaults.set(payload, forKey: self.payloadKey)\n");
        sb.append("                defaults.synchronize()\n");
        sb.append("            }\n");
        sb.append("            self.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    override func configurationItems() -> [Any]! {\n");
        sb.append("        return []\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildBuildSettings() {
        // These properties override the defaults synthesised by
        // IPhoneBuilder when wiring the extension target into Xcode.
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated by Codename One IOSShareExtensionBuilder.\n");
        sb.append("# Picked up by com.codename1.builders.IPhoneBuilder when the\n");
        sb.append("# enclosing .ios.appext archive is extracted into the Xcode project.\n");
        sb.append("IPHONEOS_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
        sb.append("SWIFT_VERSION=5.0\n");
        sb.append("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=YES\n");
        sb.append("CODE_SIGN_ENTITLEMENTS=").append(extensionName).append("/")
                .append(extensionName).append(".entitlements\n");
        sb.append("INFOPLIST_FILE=").append(extensionName).append("/Info.plist\n");
        return sb.toString();
    }
}
