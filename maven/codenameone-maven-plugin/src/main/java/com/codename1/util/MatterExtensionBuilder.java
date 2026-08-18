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

    /**
     * The floor a build that commissions onto its own fabric needs.
     *
     * <p>MTRDeviceControllerFactory arrived in iOS 16.4, three point releases
     * after MatterSupport itself. Left at 16.1 the extension still builds --
     * the controller sits behind an availability check -- and then does
     * nothing on 16.1 through 16.3 while the flow reports success, which is
     * the one outcome the app cannot detect. So a build that asks for a
     * fabric of its own raises the extension's floor to where its
     * implementation exists.</p>
     */
    public static final String DEPLOYMENT_TARGET_OWN_FABRIC = "16.4";

    /**
     * The extension's deployment target for a given build.
     *
     * @param ownFabric whether the app asked for a fabric of its own
     * @return the floor that build's extension needs
     */
    public static String deploymentTarget(boolean ownFabric) {
        return ownFabric ? DEPLOYMENT_TARGET_OWN_FABRIC : DEPLOYMENT_TARGET;
    }

    private MatterExtensionBuilder() {
    }

    /**
     * Builds the extension's files, keyed by path relative to the extension
     * folder.
     *
     * @param packageName the application's bundle identifier
     * @param appGroup    the app group shared by the app and the extension
     * @param displayName    the name shown in the setup sheet
     * @param shortVersion   the containing app's CFBundleShortVersionString
     * @param bundleVersion  the containing app's CFBundleVersion
     * @param ownFabric      whether the app asked for the accessory to join a
     *                       fabric of its own, which is what turns the
     *                       commissioning implementation from commented-out
     *                       scaffolding into live code
     * @param vendorId       the Matter vendor id the fabric commissions under
     * @return path to content, in a stable order
     */
    public static Map<String, byte[]> buildFileMap(String packageName,
            String appGroup, String displayName, String shortVersion,
            String bundleVersion, boolean ownFabric, String vendorId) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("RequestHandler.swift",
                utf8(requestHandlerSwift(packageName, appGroup, ownFabric,
                        vendorId)));
        files.put("Info.plist",
                utf8(infoPlist(displayName, shortVersion, bundleVersion)));
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
    /// The Matter-controller half, as Swift source lines.
    ///
    /// One implementation, emitted either live or commented out, so the code
    /// a build ships is the code a reader of a default build sees. The
    /// accessory reaching the user's ecosystem is the sheet's own doing; this
    /// is the second administrator an app asks for with
    /// CommissioningRequest.setCommissionToThisApp(true), and it is where the
    /// operating system's Matter stack -- Apple's, not one of ours -- joins
    /// the accessory to a fabric this app owns.
    ///
    /// @param packageName the app's bundle identifier, for the keychain tag
    /// @param appGroup    the group whose UserDefaults holds the fabric state
    /// @return the lines, without the comment prefix
    /**
     * The vendor id, checked to be a UInt16 literal and nothing else.
     *
     * <p>It is interpolated into Swift that the build compiles, so anything
     * this does not constrain is code. A value of "1, x: run()" or one
     * carrying a newline would be compiled into the extension -- and in a
     * build that never asked for a fabric it would also walk straight out of
     * the {@code //} that comments the implementation out, since a comment
     * ends at the end of its line.</p>
     *
     * @param vendorId the hint's value
     * @return the same text, once it is known to be a number
     * @throws IllegalArgumentException when it is anything else
     */
    /**
     * The app group, checked to be an app-group identifier and nothing else.
     *
     * <p>It goes into a Swift string literal the build compiles, and
     * {@code escape()} answers for XML, not for Swift: a value carrying a
     * quote closes the literal, and one carrying a newline walks out of the
     * {@code //} that comments the implementation out in a build with no
     * fabric of its own, because a comment ends at its line. Apple's own
     * grammar is the constraint -- {@code group.} and then the characters an
     * identifier may use.</p>
     *
     * @param appGroup the group the app and its extension share
     * @return the same text, once it is known to be an identifier
     * @throws IllegalArgumentException when it is anything else
     */
    public static String groupLiteral(String appGroup) {
        String value = appGroup == null ? "" : appGroup.trim();
        boolean ok = value.startsWith("group.")
                && value.length() > "group.".length()
                && value.length() <= 128;
        for (int i = 0; ok && i < value.length(); i++) {
            char c = value.charAt(i);
            ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '-';
        }
        if (!ok) {
            throw new IllegalArgumentException(
                    "ios.home.appGroup must be an app group identifier --"
                    + " 'group.' followed by letters, digits, dots or"
                    + " hyphens -- got '" + appGroup + "'. Its value is"
                    + " compiled into the generated commissioning extension,"
                    + " so it is accepted only in a shape that cannot carry"
                    + " anything else.");
        }
        return value;
    }

    public static String vendorLiteral(String vendorId) {
        String value = vendorId == null ? "" : vendorId.trim();
        long parsed = -1;
        try {
            if (value.length() > 2 && (value.startsWith("0x")
                    || value.startsWith("0X"))) {
                parsed = Long.parseLong(value.substring(2), 16);
            } else if (value.length() > 0) {
                parsed = Long.parseLong(value, 10);
            }
        } catch (NumberFormatException ex) {
            parsed = -1;
        }
        if (parsed < 0 || parsed > 0xFFFF) {
            throw new IllegalArgumentException(
                    "ios.home.commissioning.vendorId must be a Matter vendor"
                    + " id -- a number from 0 to 65535, decimal or 0x hex --"
                    + " got '" + vendorId + "'. Its value is compiled into"
                    + " the generated commissioning extension, so it is"
                    + " accepted only in a shape that cannot carry anything"
                    + " else.");
        }
        return value;
    }

    private static String[] fabricSwift(String packageName, String appGroup,
            String vendorId) {
        return new String[] {
            "@available(iOS 16.4, *)",
            "enum CN1MatterFabric {",
            "",
            "    // The app group, because the controller's storage and the",
            "    // fabric's keys have to survive the extension being torn",
            "    // down between sheets and have to be the same ones the app",
            "    // itself would see.",
            "    static let group = \"" + groupLiteral(appGroup) + "\"",
            "    static let keyTag = \"" + escape(packageName)
                    + ".cn1matter.signer\"",
            "    // 0xFFF1 is the Matter test vendor. A shipping product",
            "    // replaces it with its own through",
            "    // ios.home.commissioning.vendorId -- accessories are free to",
            "    // refuse a test vendor, and some do.",
            "    static let vendorID: UInt16 = " + vendorLiteral(vendorId),
            "",
            "    static func defaults() -> UserDefaults {",
            "        return UserDefaults(suiteName: group)"
                    + " ?? UserDefaults.standard",
            "    }",
            "",
            "    // The fabric's signing key, generated once and kept in the",
            "    // keychain. Losing it means every accessory commissioned on",
            "    // this fabric has to be commissioned again.",
            "    static func signingKey() throws -> SecKey {",
            "        let tag = keyTag.data(using: .utf8)!",
            "        let query: [String: Any] = [",
            "            kSecClass as String: kSecClassKey,",
            "            kSecAttrApplicationTag as String: tag,",
            "            kSecAttrKeyType as String:"
                    + " kSecAttrKeyTypeECSECPrimeRandom,",
            "            kSecReturnRef as String: true]",
            "        var item: CFTypeRef?",
            "        if SecItemCopyMatching(query as CFDictionary, &item)"
                    + " == errSecSuccess,",
            "           let existing = item {",
            "            return (existing as! SecKey)",
            "        }",
            "        let attributes: [String: Any] = [",
            "            kSecAttrKeyType as String:"
                    + " kSecAttrKeyTypeECSECPrimeRandom,",
            "            kSecAttrKeySizeInBits as String: 256,",
            "            kSecPrivateKeyAttrs as String: [",
            "                kSecAttrIsPermanent as String: true,",
            "                kSecAttrApplicationTag as String: tag]]",
            "        var error: Unmanaged<CFError>?",
            "        guard let key = SecKeyCreateRandomKey("
                    + "attributes as CFDictionary, &error) else {",
            "            throw error!.takeRetainedValue() as Error",
            "        }",
            "        return key",
            "    }",
            "",
            "    static func identityProtectionKey() -> Data {",
            "        if let stored = defaults().data(forKey: \"cn1.matter.ipk\"),",
            "           stored.count == 16 {",
            "            return stored",
            "        }",
            "        var bytes = [UInt8](repeating: 0, count: 16)",
            "        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count,"
                    + " &bytes)",
            "        let fresh = Data(bytes)",
            "        defaults().set(fresh, forKey: \"cn1.matter.ipk\")",
            "        return fresh",
            "    }",
            "",
            "    // The factory is a process-wide singleton and its start is",
            "    // one-shot: called a second time it errors, and the error",
            "    // arrives before createController can recover the fabric --",
            "    // so an extension process reused for a second accessory",
            "    // failed every commissioning after the first.",
            "    static var started = false",
            "    static var current: MTRDeviceController?",
            "",
            "    static func controller() throws -> MTRDeviceController {",
            "        if let existing = current {",
            "            return existing",
            "        }",
            "        let factory = MTRDeviceControllerFactory.sharedInstance()",
            "        if !started {",
            "            try factory.start("
                    + "MTRDeviceControllerFactoryParams(storage:"
                    + " CN1MatterStorage()))",
            "            started = true",
            "        }",
            "        let params = MTRDeviceControllerStartupParams(",
            "            ipk: identityProtectionKey(),"
                    + " fabricID: NSNumber(value: 1),",
            "            nocSigner: CN1MatterKeypair(key: try signingKey()))",
            "        params.vendorID = NSNumber(value: vendorID)",
            "        // The fabric outlives the first accessory, so the second",
            "        // commissioning joins the one already there rather than",
            "        // starting another the accessory would not recognise.",
            "        var controller = try? factory.createController("
                    + "onExistingFabric: params)",
            "        if controller == nil {",
            "            controller = try factory.createController("
                    + "onNewFabric: params)",
            "        }",
            "        guard let controller = controller else {",
            "            throw NSError(",
            "                domain: \"CN1MatterSetup\", code: 2,",
            "                userInfo: [NSLocalizedDescriptionKey:",
            "                    \"this app's Matter fabric could not be\"",
            "                    + \" opened\"])",
            "        }",
            "        current = controller",
            "        return controller",
            "    }",
            "",
            "    static func nextNodeID() -> NSNumber {",
            "        let next = defaults().integer(forKey: \"cn1.matter.node\")"
                    + " + 1",
            "        defaults().set(next, forKey: \"cn1.matter.node\")",
            "        return NSNumber(value: UInt64(next + 1))",
            "    }",
            "}",
            "",
            "@available(iOS 16.4, *)",
            "final class CN1MatterStorage: NSObject, MTRStorage {",
            "    func storageData(forKey key: String) -> Data? {",
            "        return CN1MatterFabric.defaults().data(forKey: key)",
            "    }",
            "    func setStorageData(_ value: Data, forKey key: String)"
                    + " -> Bool {",
            "        CN1MatterFabric.defaults().set(value, forKey: key)",
            "        return true",
            "    }",
            "    func removeStorageData(forKey key: String) -> Bool {",
            "        CN1MatterFabric.defaults().removeObject(forKey: key)",
            "        return true",
            "    }",
            "}",
            "",
            "@available(iOS 16.4, *)",
            "final class CN1MatterKeypair: NSObject, MTRKeypair {",
            "    private let key: SecKey",
            "    init(key: SecKey) { self.key = key }",
            "    func publicKey() -> Unmanaged<SecKey> {",
            "        return Unmanaged.passUnretained(SecKeyCopyPublicKey(key)!)",
            "    }",
            "    func signMessageECDSA_DER(_ message: Data) -> Data {",
            "        var error: Unmanaged<CFError>?",
            "        let signature = SecKeyCreateSignature(",
            "            key, .ecdsaSignatureMessageX962SHA256,"
                    + " message as CFData, &error)",
            "        return (signature as Data?) ?? Data()",
            "    }",
            "}",
            "",
            "// Commissioning is two steps with a delegate callback between",
            "// them -- establish the session, then commission the node -- and",
            "// commissionDevice is an async throws the OS waits on, so the",
            "// two are stitched back together with a continuation.",
            "@available(iOS 16.4, *)",
            "final class CN1MatterSession: NSObject,"
                    + " MTRDeviceControllerDelegate {",
            "    private var continuation: CheckedContinuation<Void, Error>?",
            "    private let nodeID: NSNumber",
            "    init(nodeID: NSNumber) { self.nodeID = nodeID }",
            "",
            "    func commission(payload: MTRSetupPayload) async throws {",
            "        let controller = try CN1MatterFabric.controller()",
            "        try await withCheckedThrowingContinuation {",
            "                (c: CheckedContinuation<Void, Error>) in",
            "            continuation = c",
            "            controller.setDeviceControllerDelegate(self,"
                    + " queue: DispatchQueue.main)",
            "            do {",
            "                try controller.setupCommissioningSession("
                    + "with: payload,",
            "                                                         "
                    + "newNodeID: nodeID)",
            "            } catch {",
            "                finish(error)",
            "            }",
            "        }",
            "    }",
            "",
            "    private func finish(_ error: Error?) {",
            "        guard let waiting = continuation else { return }",
            "        continuation = nil",
            "        if let error = error {",
            "            waiting.resume(throwing: error)",
            "        } else {",
            "            waiting.resume()",
            "        }",
            "    }",
            "",
            "    func controller(_ controller: MTRDeviceController,",
            "                    commissioningSessionEstablishmentDone"
                    + " error: Error?) {",
            "        if let error = error {",
            "            finish(error)",
            "            return",
            "        }",
            "        do {",
            "            try controller.commissionNode(",
            "                withID: nodeID,"
                    + " commissioningParams: MTRCommissioningParameters())",
            "        } catch {",
            "            finish(error)",
            "        }",
            "    }",
            "",
            "    func controller(_ controller: MTRDeviceController,",
            "                    commissioningComplete error: Error?,",
            "                    nodeID: NSNumber?) {",
            "        finish(error)",
            "    }",
            "}",
        };
    }

    static String requestHandlerSwift(String packageName, String appGroup,
            boolean ownFabric, String vendorId) {
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
        sb.append("import MatterSupport\n");
        // Imported either way. A commented-out implementation that needs two
        // more imports to work is a trap: the developer who uncomments it
        // gets errors about MTRSetupPayload rather than about what is
        // missing, and the imports cost a build nothing.
        sb.append("import Matter\n");
        sb.append("import Security\n\n");
        sb.append("@available(iOS 16.1, *)\n");
        sb.append("final class RequestHandler: "
                + "MatterAddDeviceExtensionRequestHandler {\n\n");
        sb.append("    // The accessory joins the user's own home, and the\n");
        sb.append("    // app learns about it the way it learns about any\n");
        sb.append("    // other accessory -- by refreshing the graph. The\n");
        sb.append("    // steps below are the defaults for everything the\n");
        sb.append("    // sheet does not need this app's answer to.\n\n");
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
        // WiFiNetworkAssociation, not WiFiAssociation: the override has to
        // match MatterSupport's own signature exactly or the extension does
        // not compile, and nothing in a Codename One build compiles this file
        // until a customer runs a commissioning build.
        sb.append("            async throws -> "
                + "MatterAddDeviceExtensionRequestHandler"
                + ".WiFiNetworkAssociation {\n");
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
                + ".ThreadNetworkAssociation {\n");
        sb.append("        return .defaultSystemNetwork\n");
        sb.append("    }\n");
        if (ownFabric) {
            sb.append("\n");
            sb.append("    // This app asked for the accessory to join a\n");
            sb.append("    // fabric of its own -- see\n");
            sb.append("    // CommissioningRequest.setCommissionToThisApp.\n");
            sb.append("    override func commissionDevice(\n");
            sb.append("            in home: MatterAddDeviceRequest.Home?,\n");
            sb.append("            onboardingPayload: String,\n");
            sb.append("            commissioningID: UUID) async throws {\n");
            sb.append("        guard #available(iOS 16.4, *) else {\n");
            sb.append("            // Unreachable in a build this generator\n");
            sb.append("            // produced -- the target's floor is 16.4\n");
            sb.append("            // when this code is live. Thrown rather\n");
            sb.append("            // than returned anyway: returning is\n");
            sb.append("            // reported as success, and a success this\n");
            sb.append("            // app's fabric did not get is the one\n");
            sb.append("            // outcome the caller cannot detect.\n");
            sb.append("            throw NSError(\n");
            sb.append("                domain: \"CN1MatterSetup\", code: 1,\n");
            sb.append("                userInfo: [NSLocalizedDescriptionKey:\n");
            sb.append("                    \"commissioning onto this app's own\"\n");
            sb.append("                    + \" fabric needs iOS 16.4\"])\n");
            sb.append("        }\n");
            sb.append("        let payload = try MTRSetupPayload(\n");
            sb.append("            onboardingPayload: onboardingPayload)\n");
            sb.append("        try await CN1MatterSession(\n");
            sb.append("            nodeID: CN1MatterFabric.nextNodeID())\n");
            sb.append("            .commission(payload: payload)\n");
            sb.append("    }\n");
        } else {
            sb.append("\n");
            sb.append("    // commissionDevice is where an app that runs its\n");
            sb.append("    // OWN Matter fabric joins the accessory to it, as\n");
            sb.append("    // a second administrator alongside the user's\n");
            sb.append("    // ecosystem. This app does not ask for that, so\n");
            sb.append("    // the override below is inert -- and it is here,\n");
            sb.append("    // rather than absent, because the machinery it\n");
            sb.append("    // needs is generated with it and a reader should\n");
            sb.append("    // see exactly what enabling it would ship.\n");
            sb.append("    //\n");
            sb.append("    // To switch it on, call\n");
            sb.append("    // CommissioningRequest.setCommissionToThisApp(true)\n");
            sb.append("    // -- the builder sees that call and generates this\n");
            sb.append("    // file live -- or set\n");
            sb.append("    // ios.home.commissioning.fabric=true when the call\n");
            sb.append("    // is somewhere the scanner cannot see it.\n");
            sb.append("//    override func commissionDevice(\n");
            sb.append("//            in home: MatterAddDeviceRequest.Home?,\n");
            sb.append("//            onboardingPayload: String,\n");
            sb.append("//            commissioningID: UUID) async throws {\n");
            sb.append("//        guard #available(iOS 16.4, *) else {\n");
            sb.append("//            throw NSError(\n");
            sb.append("//                domain: \"CN1MatterSetup\", code: 1,\n");
            sb.append("//                userInfo: [NSLocalizedDescriptionKey:\n");
            sb.append("//                    \"commissioning onto this app's own\"\n");
            sb.append("//                    + \" fabric needs iOS 16.4\"])\n");
            sb.append("//        }\n");
            sb.append("//        let payload = try MTRSetupPayload(\n");
            sb.append("//            onboardingPayload: onboardingPayload)\n");
            sb.append("//        try await CN1MatterSession(\n");
            sb.append("//            nodeID: CN1MatterFabric.nextNodeID())\n");
            sb.append("//            .commission(payload: payload)\n");
            sb.append("//    }\n");
        }
        sb.append("}\n\n");
        for (String line : fabricSwift(packageName, appGroup, vendorId)) {
            if (line.length() == 0) {
                sb.append(ownFabric ? "\n" : "//\n");
                continue;
            }
            sb.append(ownFabric ? "" : "//").append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * The extension's Info.plist.
     *
     * @param displayName the name shown in the setup sheet
     * @return the plist XML
     */
    static String infoPlist(String displayName, String shortVersion,
            String bundleVersion) {
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
        // The containing app's own versions, not 1.0/1. Apple validates an
        // embedded extension's marketing and build versions against its
        // host, so a hard-coded pair fails archive validation for every
        // release that is not literally 1.0 -- which is every release after
        // the first. The watch builder carries the same rule and the same
        // scar; see its CFBundleVersion comment.
        sb.append("    <key>CFBundleShortVersionString</key>\n");
        sb.append("    <string>").append(escape(shortVersion))
                .append("</string>\n");
        sb.append("    <key>CFBundleVersion</key>\n");
        sb.append("    <string>").append(escape(bundleVersion))
                .append("</string>\n");
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
