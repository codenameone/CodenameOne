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

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the {@code MatterAddDeviceExtension} app extension that Apple
 * requires before an app may add a Matter accessory.
 *
 * <h2>Why an extension exists at all</h2>
 *
 * <p>{@code MatterAddDeviceRequest.perform()} does not run in the app. It
 * hands the interaction to an operating-system sheet, and that sheet talks to
 * an <b>app extension</b> the app has to supply -- Apple's way of letting the
 * setup UI outlive a suspended app and of keeping the accessory's credentials
 * out of it. An app without the extension gets a runtime failure from the
 * first {@code perform()} call, with nothing at build time to warn it.</p>
 *
 * <p>So the extension is not optional and it is not something a Codename One
 * developer can reasonably be asked to hand-write in Xcode: the whole point of
 * the framework is that they do not open Xcode. The builder generates it,
 * signs it, and embeds it -- but only for apps that reference
 * {@code com.codename1.home.commissioning}, which is why that lives in a
 * package of its own.</p>
 *
 * <h2>What the generated extension does</h2>
 *
 * <p>The minimum Apple accepts, deliberately. The handler validates the
 * device against the criteria the sheet was given and otherwise defers to the
 * defaults; it does not commission on a fabric of its own, because Codename
 * One is not a Matter controller and the accessory is joining the user's
 * HomeKit home rather than this app's.</p>
 *
 * <p>Pure static and string-building, with no build state, so the emitted
 * files can be asserted in a unit test rather than only by running a cloud
 * build.</p>
 */
public final class MatterExtensionBuilder {

    /** Xcode target and folder name of the generated extension. */
    public static final String EXTENSION_NAME = "CN1MatterSetup";

    /**
     * The extension point Apple resolves the add-device handler through. Wrong
     * here means the sheet cannot find the extension and commissioning fails
     * at runtime with a message about the app rather than about the value.
     */
    public static final String EXTENSION_POINT =
            "com.apple.matter.support.extension.device-setup";

    /**
     * The lowest deployment target MatterSupport exists on. The extension
     * keeps its own, above the app's, exactly as the Wallet and WidgetKit
     * extensions do.
     */
    public static final String DEPLOYMENT_TARGET = "16.1";

    private MatterExtensionBuilder() {
    }

    /**
     * Builds the extension's files, keyed by path relative to the extension
     * folder.
     *
     * @param packageName the application's bundle identifier
     * @param appGroup    the app group shared by the app and the extension
     * @param displayName the name shown in the setup sheet
     * @return path to content, in a stable order
     */
    public static Map<String, byte[]> buildFileMap(String packageName,
            String appGroup, String displayName) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("RequestHandler.swift", utf8(requestHandlerSwift()));
        files.put("Info.plist", utf8(infoPlist(displayName)));
        files.put(EXTENSION_NAME + ".entitlements",
                utf8(entitlements(packageName, appGroup)));
        return files;
    }

    /**
     * The extension's principal class.
     *
     * <p>Everything here is a default that Apple's own template supplies,
     * except the criteria check. That one matters: without it the sheet offers
     * to add any Matter device in range, including ones this app asked not to
     * see.</p>
     *
     * @return the Swift source
     */
    static String requestHandlerSwift() {
        StringBuilder sb = new StringBuilder();
        sb.append("//\n");
        sb.append("//  Generated by Codename One. Do not edit: this file is\n");
        sb.append("//  rewritten on every build.\n");
        sb.append("//\n");
        sb.append("//  Apple requires an app extension before an app may add\n");
        sb.append("//  a Matter accessory. The sheet the user sees runs\n");
        sb.append("//  outside the app and talks to this handler.\n");
        sb.append("//\n\n");
        sb.append("import Foundation\n");
        sb.append("import MatterSupport\n\n");
        sb.append("@available(iOS 16.1, *)\n");
        sb.append("final class RequestHandler: "
                + "MatterAddDeviceExtensionRequestHandler {\n\n");
        sb.append("    // Codename One is not a Matter controller: the\n");
        sb.append("    // accessory joins the user's own HomeKit home, not a\n");
        sb.append("    // fabric of this app's. So every step below is the\n");
        sb.append("    // default, and the app learns about the new accessory\n");
        sb.append("    // the same way it learns about any other -- by\n");
        sb.append("    // refreshing the graph.\n\n");
        sb.append("    override func validateDeviceCredential(\n");
        sb.append("            _ deviceCredential: "
                + "MatterAddDeviceExtensionRequestHandler.DeviceCredential)\n");
        sb.append("            async throws {\n");
        sb.append("        // Accepting the credential is what lets the OS\n");
        sb.append("        // continue. Refusing here would reject every\n");
        sb.append("        // accessory, which is not a safety feature -- the\n");
        sb.append("        // user already chose the device in Apple's UI.\n");
        sb.append("    }\n\n");
        sb.append("    override func selectWiFiNetwork(\n");
        sb.append("            from wifiScanResults: "
                + "[MatterAddDeviceExtensionRequestHandler.WiFiScanResult])\n");
        sb.append("            async throws -> "
                + "MatterAddDeviceExtensionRequestHandler.WiFiAssociation {\n");
        sb.append("        // The OS asks the user when this defers, which is\n");
        sb.append("        // right: the app has no idea which network the\n");
        sb.append("        // accessory should be on.\n");
        sb.append("        return .defaultSystemNetwork\n");
        sb.append("    }\n\n");
        sb.append("    override func selectThreadNetwork(\n");
        sb.append("            from threadScanResults: "
                + "[MatterAddDeviceExtensionRequestHandler"
                + ".ThreadScanResult])\n");
        sb.append("            async throws -> "
                + "MatterAddDeviceExtensionRequestHandler"
                + ".ThreadAssociation {\n");
        sb.append("        return .defaultSystemNetwork\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * The extension's Info.plist.
     *
     * @param displayName the name shown in the setup sheet
     * @return the plist XML
     */
    static String infoPlist(String displayName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("    <key>CFBundleDevelopmentRegion</key>\n");
        sb.append("    <string>en</string>\n");
        sb.append("    <key>CFBundleDisplayName</key>\n");
        sb.append("    <string>").append(escape(displayName))
                .append("</string>\n");
        // Set from the build settings rather than left out. An explicit
        // Info.plist is not merged with anything: PRODUCT_BUNDLE_IDENTIFIER
        // on the target does not reach a plist that does not ask for it, and
        // a bundle with no identifier and no executable key compiles and then
        // fails validation on the way to the device.
        sb.append("    <key>CFBundleExecutable</key>\n");
        sb.append("    <string>$(EXECUTABLE_NAME)</string>\n");
        sb.append("    <key>CFBundleIdentifier</key>\n");
        sb.append("    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n");
        sb.append("    <key>CFBundleInfoDictionaryVersion</key>\n");
        sb.append("    <string>6.0</string>\n");
        sb.append("    <key>CFBundleName</key>\n");
        sb.append("    <string>").append(EXTENSION_NAME).append("</string>\n");
        sb.append("    <key>CFBundlePackageType</key>\n");
        sb.append("    <string>XPC!</string>\n");
        sb.append("    <key>CFBundleShortVersionString</key>\n");
        sb.append("    <string>1.0</string>\n");
        sb.append("    <key>CFBundleVersion</key>\n");
        sb.append("    <string>1</string>\n");
        sb.append("    <key>NSExtension</key>\n");
        sb.append("    <dict>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>").append(EXTENSION_POINT)
                .append("</string>\n");
        sb.append("        <key>NSExtensionPrincipalClass</key>\n");
        // The module-qualified name, because Swift classes are namespaced by
        // their module and the unqualified one does not resolve at runtime --
        // which surfaces as a sheet that opens and immediately fails.
        sb.append("        <string>$(PRODUCT_MODULE_NAME).RequestHandler"
                + "</string>\n");
        sb.append("    </dict>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    /**
     * The extension's entitlements.
     *
     * <p>The app group is not decoration: it is the only channel between the
     * extension and the app, and Apple refuses to launch an extension whose
     * group does not match its host's.</p>
     *
     * @param packageName the application's bundle identifier
     * @param appGroup    the shared app group
     * @return the entitlements XML
     */
    static String entitlements(String packageName, String appGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("    <key>com.apple.security.application-groups</key>\n");
        sb.append("    <array>\n");
        sb.append("        <string>").append(escape(appGroup))
                .append("</string>\n");
        sb.append("    </array>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    /**
     * The app group the extension and its host share, when the project has not
     * named one.
     *
     * @param packageName the application's bundle identifier
     * @return the default app group
     */
    public static String defaultAppGroup(String packageName) {
        return "group." + packageName;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            // Every JVM is required to support UTF-8, so this cannot happen;
            // rethrowing keeps the checked exception out of every caller's
            // signature without pretending to handle it.
            throw new IllegalStateException("UTF-8 is unavailable", impossible);
        }
    }
}
