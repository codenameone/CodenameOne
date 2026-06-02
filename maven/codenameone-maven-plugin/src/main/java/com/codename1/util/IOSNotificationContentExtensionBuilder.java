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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates an iOS Notification Content Extension bundle ready to be picked up by
 * {@code IPhoneBuilder} during the Codename One iOS build, following the same
 * {@code .ios.appext} packaging convention as {@link IOSShareExtensionBuilder}.
 *
 * <p>A notification content extension provides a custom view for notifications whose
 * {@code categoryIdentifier} matches the extension's {@code UNNotificationExtensionCategory}.
 * It corresponds to {@code LocalNotification.setCustomView(...)} on the Codename One side:
 * the category used is {@code cn1-ln-<notificationId>} (see {@code IOSImplementation}).</p>
 *
 * <p>The generated archive contains:</p>
 * <ul>
 *   <li>An {@code Info.plist} with the NSExtension dictionary, point identifier
 *       {@code com.apple.usernotifications.content} and the matching category.</li>
 *   <li>A Swift {@code NotificationViewController} implementing
 *       {@code UNNotificationContentExtension} that renders the notification title and
 *       body (and may be customized further).</li>
 *   <li>An optional entitlements file declaring a shared App Group.</li>
 *   <li>A {@code buildSettings.properties} consumed by {@code IPhoneBuilder}.</li>
 * </ul>
 *
 * <p>This class produces ASCII-only, deterministic output.</p>
 */
public final class IOSNotificationContentExtensionBuilder {

    private String extensionName = "NotificationContentExtension";
    private String displayName;
    private String category = "cn1-notification";
    private String appGroupId;
    private String deploymentTarget = "12.0";

    /** Bare-bones constructor. Configure with the fluent setters. */
    public IOSNotificationContentExtensionBuilder() {}

    /**
     * Sets the extension target name (Xcode target, bundle and directory name). Must be an
     * ASCII identifier.
     * @param name extension name
     * @return this
     */
    public IOSNotificationContentExtensionBuilder setExtensionName(String name) {
        this.extensionName = name;
        return this;
    }

    /**
     * Sets the user-visible name. Defaults to the extension name.
     * @param name display name
     * @return this
     */
    public IOSNotificationContentExtensionBuilder setDisplayName(String name) {
        this.displayName = name;
        return this;
    }

    /**
     * Sets the notification category this extension renders. Must match the
     * {@code categoryIdentifier} of the notifications it should display.
     * @param category the category id
     * @return this
     */
    public IOSNotificationContentExtensionBuilder setCategory(String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets an optional shared App Group id (must start with {@code group.}).
     * @param appGroupId the app group
     * @return this
     */
    public IOSNotificationContentExtensionBuilder setAppGroupId(String appGroupId) {
        this.appGroupId = appGroupId;
        return this;
    }

    /**
     * Sets the minimum iOS deployment target.
     * @param target deployment target, e.g. "12.0"
     * @return this
     */
    public IOSNotificationContentExtensionBuilder setDeploymentTarget(String target) {
        this.deploymentTarget = target;
        return this;
    }

    private String getDisplayName() {
        return displayName == null || displayName.length() == 0 ? extensionName : displayName;
    }

    private void validate() {
        if (extensionName == null || !isIdentifier(extensionName)) {
            throw new IllegalStateException("extensionName must be ASCII letters/digits/_/- only: " + extensionName);
        }
        if (category == null || category.length() == 0) {
            throw new IllegalStateException("category must be set");
        }
        if (appGroupId != null && appGroupId.length() > 0 && !appGroupId.startsWith("group.")) {
            throw new IllegalStateException("appGroupId must start with 'group.': " + appGroupId);
        }
    }

    private static boolean isIdentifier(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the in-memory file map. Public for unit testing.
     * @return the file map
     */
    public java.util.Map<String, byte[]> buildFileMap() {
        validate();
        java.util.LinkedHashMap<String, byte[]> map = new java.util.LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist()));
        if (appGroupId != null && appGroupId.length() > 0) {
            map.put(extensionName + ".entitlements", utf8(buildEntitlements()));
        }
        map.put("NotificationViewController.swift", utf8(buildViewController()));
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
        sb.append("            <key>UNNotificationExtensionCategory</key>\n");
        sb.append("            <string>").append(escapeXml(category)).append("</string>\n");
        sb.append("            <key>UNNotificationExtensionInitialContentSizeRatio</key>\n");
        sb.append("            <real>1</real>\n");
        sb.append("        </dict>\n");
        sb.append("        <key>NSExtensionMainStoryboard</key>\n");
        sb.append("        <string>MainInterface</string>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>com.apple.usernotifications.content</string>\n");
        sb.append("        <key>NSExtensionPrincipalClass</key>\n");
        sb.append("        <string>$(PRODUCT_MODULE_NAME).NotificationViewController</string>\n");
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

    private String buildViewController() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("import UIKit\n");
        sb.append("import UserNotifications\n");
        sb.append("import UserNotificationsUI\n");
        sb.append("\n");
        sb.append("/// Auto-generated by Codename One IOSNotificationContentExtensionBuilder.\n");
        sb.append("/// Renders a custom view for notifications in category \"").append(category).append("\".\n");
        sb.append("class NotificationViewController: UIViewController, UNNotificationContentExtension {\n");
        sb.append("\n");
        sb.append("    private let titleLabel = UILabel()\n");
        sb.append("    private let bodyLabel = UILabel()\n");
        sb.append("    private let imageView = UIImageView()\n");
        sb.append("\n");
        sb.append("    override func viewDidLoad() {\n");
        sb.append("        super.viewDidLoad()\n");
        sb.append("        titleLabel.font = UIFont.boldSystemFont(ofSize: 16)\n");
        sb.append("        titleLabel.numberOfLines = 0\n");
        sb.append("        bodyLabel.font = UIFont.systemFont(ofSize: 14)\n");
        sb.append("        bodyLabel.numberOfLines = 0\n");
        sb.append("        imageView.contentMode = .scaleAspectFit\n");
        sb.append("        let stack = UIStackView(arrangedSubviews: [titleLabel, bodyLabel, imageView])\n");
        sb.append("        stack.axis = .vertical\n");
        sb.append("        stack.spacing = 6\n");
        sb.append("        stack.translatesAutoresizingMaskIntoConstraints = false\n");
        sb.append("        view.addSubview(stack)\n");
        sb.append("        NSLayoutConstraint.activate([\n");
        sb.append("            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 12),\n");
        sb.append("            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -12),\n");
        sb.append("            stack.topAnchor.constraint(equalTo: view.topAnchor, constant: 12),\n");
        sb.append("            stack.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -12)\n");
        sb.append("        ])\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    func didReceive(_ notification: UNNotification) {\n");
        sb.append("        let content = notification.request.content\n");
        sb.append("        titleLabel.text = content.title\n");
        sb.append("        bodyLabel.text = content.body\n");
        sb.append("        if let attachment = content.attachments.first, attachment.url.startAccessingSecurityScopedResource() {\n");
        sb.append("            defer { attachment.url.stopAccessingSecurityScopedResource() }\n");
        sb.append("            if let data = try? Data(contentsOf: attachment.url) {\n");
        sb.append("                imageView.image = UIImage(data: data)\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildBuildSettings() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated by Codename One IOSNotificationContentExtensionBuilder.\n");
        sb.append("# Picked up by com.codename1.builders.IPhoneBuilder when the\n");
        sb.append("# enclosing .ios.appext archive is extracted into the Xcode project.\n");
        sb.append("IPHONEOS_DEPLOYMENT_TARGET=").append(deploymentTarget).append("\n");
        sb.append("SWIFT_VERSION=5.0\n");
        sb.append("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=YES\n");
        if (appGroupId != null && appGroupId.length() > 0) {
            sb.append("CODE_SIGN_ENTITLEMENTS=").append(extensionName).append("/")
                    .append(extensionName).append(".entitlements\n");
        }
        sb.append("INFOPLIST_FILE=").append(extensionName).append("/Info.plist\n");
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

    /**
     * Writes the extension as a single {@code .ios.appext} zip archive with each entry at
     * the archive root, matching what {@code IPhoneBuilder.extractAppExtensions} expects.
     * @param outputZip the target archive
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
}
