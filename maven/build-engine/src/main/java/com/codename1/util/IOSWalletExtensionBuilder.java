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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the Apple Wallet issuer-provisioning extension pair that the
 * Codename One iOS build wires into the generated Xcode project when the
 * {@code ios.wallet.extension} build hint is enabled.
 *
 * <p>An Apple Wallet ("In-App Provisioning") extension lets a card
 * issuer's app surface its cards inside the Wallet app itself, under
 * "From apps on your iPhone", so users can provision a card without
 * launching the issuer's app. Apple defines two extension points:</p>
 *
 * <ul>
 *   <li>A <b>non-UI extension</b> ({@code com.apple.PassKit.issuer-provisioning})
 *       whose principal class subclasses {@code PKIssuerProvisioningExtensionHandler}.
 *       It must answer {@code status} within 100ms, list available passes,
 *       and produce the encrypted add-pass payload.</li>
 *   <li>An optional <b>authorization UI extension</b>
 *       ({@code com.apple.PassKit.issuer-provisioning.authorization}) that
 *       presents a login screen inside Wallet when the non-UI extension
 *       reports {@code requiresAuthentication}.</li>
 * </ul>
 *
 * <p>The generated extensions are fixed Objective-C sources that contain no
 * customer logic. They are driven entirely by data the Codename One app
 * publishes through {@code com.codename1.payment.WalletExtension} into the
 * shared App Group container (suite {@code NSUserDefaults} for entries,
 * auth token and flags; PNG card art under {@code cn1wallet/} in the group
 * container), plus two customer HTTPS endpoints:</p>
 *
 * <ul>
 *   <li>The <b>issuer endpoint</b> receives a JSON POST
 *       {@code {certificates:[base64], nonce, nonceSignature, cardIdentifier, authToken}}
 *       and must answer {@code {activationData, encryptedPassData, ephemeralPublicKey}}
 *       (all base64; {@code wrappedKey} supported for RSA_V2) — this is the
 *       step that only the issuer's backend can perform.</li>
 *   <li>The <b>auth endpoint</b> (UI extension only) receives
 *       {@code {username, password}} and answers {@code {token}}.</li>
 * </ul>
 *
 * <p>Customers needing custom behavior inject Objective-C snippets at the
 * marker comments via the {@code ios.wallet.*Inject} build hints (see the
 * {@code MARKER_*} constants); the markers survive injection so multiple
 * passes remain possible.</p>
 *
 * <p>Note that the {@code com.apple.developer.payment-pass-provisioning}
 * entitlement written into the generated {@code .entitlements} files is
 * restricted: Apple grants it per-app on request, and each extension needs
 * its own App ID and provisioning profile carrying it.</p>
 */
public class IOSWalletExtensionBuilder {

    /** Principal class name of the generated non-UI extension. */
    public static final String NONUI_PRINCIPAL_CLASS = "CN1WalletExtensionHandler";

    /** Principal class name of the generated authorization UI extension. */
    public static final String UI_PRINCIPAL_CLASS = "CN1WalletAuthViewController";

    /** Info.plist key holding the App Group id, read by extension and app alike. */
    public static final String APP_GROUP_PLIST_KEY = "CN1WalletAppGroup";

    /** Info.plist key holding the issuer endpoint URL in the non-UI extension. */
    public static final String ISSUER_ENDPOINT_PLIST_KEY = "CN1WalletIssuerEndpoint";

    /** Info.plist key holding the auth endpoint URL in the UI extension. */
    public static final String AUTH_ENDPOINT_PLIST_KEY = "CN1WalletAuthEndpoint";

    // Suite/user-defaults keys shared with IOSNative.m (the app side).
    public static final String PASS_ENTRIES_KEY = "cn1.wallet.passEntries";
    public static final String REMOTE_PASS_ENTRIES_KEY = "cn1.wallet.remotePassEntries";
    public static final String AUTH_TOKEN_KEY = "cn1.wallet.authToken";
    public static final String REQUIRES_AUTH_KEY = "cn1.wallet.requiresAuthentication";

    /** Directory inside the App Group container holding card-art PNGs. */
    public static final String ART_DIR = "cn1wallet";

    // Injection markers in the non-UI extension sources.
    public static final String MARKER_NONUI_IMPORTS = "//CN1_WALLET_NONUI_IMPORTS_MARKER";
    public static final String MARKER_STATUS = "//CN1_WALLET_STATUS_MARKER";
    public static final String MARKER_PASS_ENTRIES = "//CN1_WALLET_PASS_ENTRIES_MARKER";
    public static final String MARKER_REMOTE_PASS_ENTRIES = "//CN1_WALLET_REMOTE_PASS_ENTRIES_MARKER";
    public static final String MARKER_GENERATE_REQUEST = "//CN1_WALLET_GENERATE_REQUEST_MARKER";
    public static final String MARKER_GENERATE_RESPONSE = "//CN1_WALLET_GENERATE_RESPONSE_MARKER";

    // Injection markers in the UI extension sources.
    public static final String MARKER_UI_IMPORTS = "//CN1_WALLET_UI_IMPORTS_MARKER";
    public static final String MARKER_UI_VIEWDIDLOAD = "//CN1_WALLET_UI_VIEWDIDLOAD_MARKER";
    public static final String MARKER_UI_AUTH_REQUEST = "//CN1_WALLET_UI_AUTH_REQUEST_MARKER";
    public static final String MARKER_UI_AUTH_RESPONSE = "//CN1_WALLET_UI_AUTH_RESPONSE_MARKER";

    private String nonUIExtensionName = "WalletNonUIExtension";
    private String uiExtensionName = "WalletUIExtension";
    private String appGroupId;
    private String issuerEndpoint;
    private String authEndpoint;
    private String applicationIdentifierPrefix;
    private final Map<String, String> injections = new LinkedHashMap<String, String>();

    /** Bare-bones constructor. Configure with the fluent setters. */
    public IOSWalletExtensionBuilder() {}

    /**
     * Sets the non-UI extension target name (Xcode target, .appex bundle
     * and bundle-id suffix). Must be an ASCII identifier. Defaults to
     * {@code WalletNonUIExtension}.
     */
    public IOSWalletExtensionBuilder setNonUIExtensionName(String name) {
        this.nonUIExtensionName = name;
        return this;
    }

    /**
     * Sets the authorization UI extension target name. Defaults to
     * {@code WalletUIExtension}.
     */
    public IOSWalletExtensionBuilder setUIExtensionName(String name) {
        this.uiExtensionName = name;
        return this;
    }

    /**
     * The App Group identifier shared between the host app and both
     * extensions. Apple requires it to start with {@code group.}. Required.
     */
    public IOSWalletExtensionBuilder setAppGroupId(String id) {
        this.appGroupId = id;
        return this;
    }

    /**
     * HTTPS endpoint POSTed by the non-UI extension's
     * {@code generateAddPaymentPassRequest...} step. Required for the
     * non-UI extension.
     */
    public IOSWalletExtensionBuilder setIssuerEndpoint(String url) {
        this.issuerEndpoint = url;
        return this;
    }

    /**
     * HTTPS endpoint POSTed by the UI extension's login form. Required
     * only when the UI extension is generated.
     */
    public IOSWalletExtensionBuilder setAuthEndpoint(String url) {
        this.authEndpoint = url;
        return this;
    }

    /**
     * Optional application-identifier prefix (e.g. {@code TEAMID.com.example.app});
     * when set, each extension's entitlements include
     * {@code application-identifier = <prefix>.<ExtensionName>}. The cloud
     * builder passes the build request's app id here; local builds omit it
     * and let Xcode signing fill it in.
     */
    public IOSWalletExtensionBuilder setApplicationIdentifierPrefix(String prefix) {
        this.applicationIdentifierPrefix = prefix;
        return this;
    }

    /**
     * Registers an Objective-C snippet to inject at one of the
     * {@code MARKER_*} comments. The marker is preserved after the injected
     * code so repeated processing stays possible. Unknown markers are
     * rejected to catch build-hint typos early.
     */
    public IOSWalletExtensionBuilder setInjection(String marker, String objcCode) {
        if (!isKnownMarker(marker)) {
            throw new IllegalArgumentException("Unknown wallet extension injection marker: " + marker);
        }
        if (objcCode != null && objcCode.length() > 0) {
            injections.put(marker, objcCode);
        }
        return this;
    }

    public String getNonUIExtensionName() { return nonUIExtensionName; }
    public String getUIExtensionName() { return uiExtensionName; }
    public String getAppGroupId() { return appGroupId; }
    public String getIssuerEndpoint() { return issuerEndpoint; }
    public String getAuthEndpoint() { return authEndpoint; }
    public String getApplicationIdentifierPrefix() { return applicationIdentifierPrefix; }

    private static boolean isKnownMarker(String marker) {
        return MARKER_NONUI_IMPORTS.equals(marker)
                || MARKER_STATUS.equals(marker)
                || MARKER_PASS_ENTRIES.equals(marker)
                || MARKER_REMOTE_PASS_ENTRIES.equals(marker)
                || MARKER_GENERATE_REQUEST.equals(marker)
                || MARKER_GENERATE_RESPONSE.equals(marker)
                || MARKER_UI_IMPORTS.equals(marker)
                || MARKER_UI_VIEWDIDLOAD.equals(marker)
                || MARKER_UI_AUTH_REQUEST.equals(marker)
                || MARKER_UI_AUTH_RESPONSE.equals(marker);
    }

    /**
     * Builds the in-memory file map of the non-UI extension, keyed by
     * relative path inside the extension folder.
     */
    public Map<String, byte[]> buildNonUIFileMap() {
        validateCommon(nonUIExtensionName);
        if (issuerEndpoint == null || issuerEndpoint.length() == 0) {
            throw new IllegalStateException("issuerEndpoint must be set (ios.wallet.issuerEndpoint build hint)");
        }
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist(nonUIExtensionName,
                "com.apple.PassKit.issuer-provisioning", NONUI_PRINCIPAL_CLASS,
                ISSUER_ENDPOINT_PLIST_KEY, issuerEndpoint)));
        map.put(nonUIExtensionName + ".entitlements", utf8(buildEntitlements(nonUIExtensionName)));
        map.put(NONUI_PRINCIPAL_CLASS + ".h", utf8(buildNonUIHeader()));
        map.put(NONUI_PRINCIPAL_CLASS + ".m", utf8(inject(buildNonUISource())));
        return map;
    }

    /**
     * Builds the in-memory file map of the authorization UI extension,
     * keyed by relative path inside the extension folder.
     */
    public Map<String, byte[]> buildUIFileMap() {
        validateCommon(uiExtensionName);
        if (authEndpoint == null || authEndpoint.length() == 0) {
            throw new IllegalStateException("authEndpoint must be set (ios.wallet.authEndpoint build hint)");
        }
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        map.put("Info.plist", utf8(buildInfoPlist(uiExtensionName,
                "com.apple.PassKit.issuer-provisioning.authorization", UI_PRINCIPAL_CLASS,
                AUTH_ENDPOINT_PLIST_KEY, authEndpoint)));
        map.put(uiExtensionName + ".entitlements", utf8(buildEntitlements(uiExtensionName)));
        map.put(UI_PRINCIPAL_CLASS + ".h", utf8(buildUIHeader()));
        map.put(UI_PRINCIPAL_CLASS + ".m", utf8(inject(buildUISource())));
        return map;
    }

    /**
     * Writes a file map into {@code outputDir}, creating it if needed.
     * Used by the IPhoneBuilders to materialize the extension folders
     * under {@code dist/}.
     */
    public static void writeFileMap(Map<String, byte[]> files, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create " + outputDir);
        }
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
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
    }

    private void validateCommon(String extensionName) {
        if (extensionName == null || !isIdentifier(extensionName)) {
            throw new IllegalStateException(
                    "extension name must be ASCII letters/digits/_/- only: " + extensionName);
        }
        if (appGroupId == null || !appGroupId.startsWith("group.")) {
            throw new IllegalStateException(
                    "appGroupId must start with 'group.' (Apple requirement, ios.wallet.appGroup build hint): " + appGroupId);
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

    private String inject(String source) {
        String result = source;
        for (Map.Entry<String, String> e : injections.entrySet()) {
            int idx = result.indexOf(e.getKey());
            if (idx >= 0) {
                result = result.substring(0, idx) + e.getValue() + "\n" + result.substring(idx);
            }
        }
        return result;
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String buildInfoPlist(String extensionName, String pointIdentifier,
            String principalClass, String endpointKey, String endpointValue) {
        // Apple's extension loader is intolerant of whitespace inside the
        // NSExtension string values - every <string> below must stay on a
        // single line with no padding around the value.
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        plistKeyString(sb, "CFBundleDevelopmentRegion", "$(DEVELOPMENT_LANGUAGE)");
        plistKeyString(sb, "CFBundleDisplayName", extensionName);
        plistKeyString(sb, "CFBundleExecutable", "$(EXECUTABLE_NAME)");
        plistKeyString(sb, "CFBundleIdentifier", "$(PRODUCT_BUNDLE_IDENTIFIER)");
        plistKeyString(sb, "CFBundleInfoDictionaryVersion", "6.0");
        plistKeyString(sb, "CFBundleName", "$(PRODUCT_NAME)");
        plistKeyString(sb, "CFBundlePackageType", "XPC!");
        plistKeyString(sb, "CFBundleShortVersionString", "1.0");
        plistKeyString(sb, "CFBundleVersion", "1");
        plistKeyString(sb, APP_GROUP_PLIST_KEY, appGroupId);
        plistKeyString(sb, endpointKey, endpointValue);
        sb.append("    <key>NSExtension</key>\n");
        sb.append("    <dict>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>").append(pointIdentifier).append("</string>\n");
        sb.append("        <key>NSExtensionPrincipalClass</key>\n");
        sb.append("        <string>").append(principalClass).append("</string>\n");
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

    private String buildEntitlements(String extensionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("    <key>com.apple.developer.payment-pass-provisioning</key>\n");
        sb.append("    <true/>\n");
        sb.append("    <key>com.apple.security.application-groups</key>\n");
        sb.append("    <array>\n");
        sb.append("        <string>").append(escapeXml(appGroupId)).append("</string>\n");
        sb.append("    </array>\n");
        if (applicationIdentifierPrefix != null && applicationIdentifierPrefix.length() > 0) {
            sb.append("    <key>application-identifier</key>\n");
            sb.append("    <string>").append(escapeXml(applicationIdentifierPrefix))
                    .append(".").append(escapeXml(extensionName)).append("</string>\n");
        }
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    private String buildNonUIHeader() {
        return "#import <PassKit/PassKit.h>\n"
                + "\n"
                + "API_AVAILABLE(ios(14.0))\n"
                + "@interface " + NONUI_PRINCIPAL_CLASS + " : PKIssuerProvisioningExtensionHandler\n"
                + "@end\n";
    }

    private String buildNonUISource() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("#import \"").append(NONUI_PRINCIPAL_CLASS).append(".h\"\n");
        sb.append("#import <UIKit/UIKit.h>\n");
        sb.append(MARKER_NONUI_IMPORTS).append("\n");
        sb.append("\n");
        sb.append("// Auto-generated by Codename One (ios.wallet.extension build hint).\n");
        sb.append("// Answers Wallet's issuer-provisioning callbacks from data the app\n");
        sb.append("// published into the App Group via com.codename1.payment.WalletExtension;\n");
        sb.append("// the encrypted pass payload is produced by the issuer endpoint configured\n");
        sb.append("// in the ios.wallet.issuerEndpoint build hint.\n");
        sb.append("\n");
        sb.append("static NSString *cn1WalletInfoString(NSString *key) {\n");
        sb.append("    id v = [[NSBundle mainBundle] objectForInfoDictionaryKey:key];\n");
        sb.append("    return [v isKindOfClass:[NSString class]] ? (NSString *)v : nil;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("static NSUserDefaults *cn1WalletDefaults() {\n");
        sb.append("    NSString *group = cn1WalletInfoString(@\"").append(APP_GROUP_PLIST_KEY).append("\");\n");
        sb.append("    return group == nil ? nil : [[NSUserDefaults alloc] initWithSuiteName:group];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("static NSURL *cn1WalletArtDir() {\n");
        sb.append("    NSString *group = cn1WalletInfoString(@\"").append(APP_GROUP_PLIST_KEY).append("\");\n");
        sb.append("    if (group == nil) return nil;\n");
        sb.append("    NSURL *container = [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:group];\n");
        sb.append("    return [container URLByAppendingPathComponent:@\"").append(ART_DIR).append("\" isDirectory:YES];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("static CGImageRef cn1WalletLoadArt(NSString *fileName) {\n");
        sb.append("    if (fileName == nil) return nil;\n");
        sb.append("    NSURL *url = [cn1WalletArtDir() URLByAppendingPathComponent:fileName];\n");
        sb.append("    if (url == nil) return nil;\n");
        sb.append("    UIImage *img = [UIImage imageWithContentsOfFile:url.path];\n");
        sb.append("    return img.CGImage;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("@implementation ").append(NONUI_PRINCIPAL_CLASS).append("\n");
        sb.append("\n");
        // status: 100ms deadline - local suite reads only, no PKPassLibrary
        // and no networking here.
        sb.append("- (void)statusWithCompletion:(void (^)(PKIssuerProvisioningExtensionStatus *status))completion {\n");
        sb.append("    PKIssuerProvisioningExtensionStatus *status = [[PKIssuerProvisioningExtensionStatus alloc] init];\n");
        sb.append("    NSUserDefaults *d = cn1WalletDefaults();\n");
        sb.append("    status.passEntriesAvailable = [d arrayForKey:@\"").append(PASS_ENTRIES_KEY).append("\"].count > 0;\n");
        sb.append("    status.remotePassEntriesAvailable = [d arrayForKey:@\"").append(REMOTE_PASS_ENTRIES_KEY).append("\"].count > 0;\n");
        sb.append("    status.requiresAuthentication = [d boolForKey:@\"").append(REQUIRES_AUTH_KEY).append("\"];\n");
        sb.append("    ").append(MARKER_STATUS).append("\n");
        sb.append("    completion(status);\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (NSMutableArray<PKIssuerProvisioningExtensionPassEntry *> *)cn1EntriesForKey:(NSString *)key remote:(BOOL)remote {\n");
        sb.append("    NSMutableArray<PKIssuerProvisioningExtensionPassEntry *> *result = [NSMutableArray array];\n");
        sb.append("    NSUserDefaults *d = cn1WalletDefaults();\n");
        sb.append("    NSArray *stored = [d arrayForKey:key];\n");
        sb.append("    if (stored.count == 0) return result;\n");
        sb.append("    // Wallet requires filtering out cards that are already provisioned\n");
        sb.append("    // on this device (or the paired watch for remote entries).\n");
        sb.append("    NSMutableSet *existing = [NSMutableSet set];\n");
        sb.append("    PKPassLibrary *lib = [[PKPassLibrary alloc] init];\n");
        sb.append("    if (remote) {\n");
        sb.append("        for (PKSecureElementPass *p in [lib remoteSecureElementPasses]) {\n");
        sb.append("            if (p.primaryAccountIdentifier != nil) [existing addObject:p.primaryAccountIdentifier];\n");
        sb.append("        }\n");
        sb.append("    } else {\n");
        sb.append("        for (PKPass *p in [lib passesOfType:PKPassTypeSecureElement]) {\n");
        sb.append("            PKSecureElementPass *se = p.secureElementPass;\n");
        sb.append("            if (se.primaryAccountIdentifier != nil) [existing addObject:se.primaryAccountIdentifier];\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    for (NSDictionary *e in stored) {\n");
        sb.append("        if (![e isKindOfClass:[NSDictionary class]]) continue;\n");
        sb.append("        NSString *identifier = e[@\"identifier\"];\n");
        sb.append("        if (identifier == nil || [existing containsObject:identifier]) continue;\n");
        sb.append("        PKAddPaymentPassRequestConfiguration *config =\n");
        sb.append("                [[PKAddPaymentPassRequestConfiguration alloc] initWithEncryptionScheme:PKEncryptionSchemeECC_V2];\n");
        sb.append("        config.primaryAccountIdentifier = identifier;\n");
        sb.append("        config.cardholderName = e[@\"cardholderName\"];\n");
        sb.append("        config.primaryAccountSuffix = e[@\"accountSuffix\"];\n");
        sb.append("        config.localizedDescription = e[@\"description\"];\n");
        sb.append("        if (e[@\"network\"] != nil) {\n");
        sb.append("            config.paymentNetwork = e[@\"network\"];\n");
        sb.append("        }\n");
        sb.append("        CGImageRef art = cn1WalletLoadArt(e[@\"art\"]);\n");
        sb.append("        if (art == nil) continue;\n");
        sb.append("        PKIssuerProvisioningExtensionPaymentPassEntry *entry =\n");
        sb.append("                [[PKIssuerProvisioningExtensionPaymentPassEntry alloc] initWithIdentifier:identifier\n");
        sb.append("                        title:(e[@\"title\"] != nil ? e[@\"title\"] : identifier)\n");
        sb.append("                        art:art\n");
        sb.append("                        addRequestConfiguration:config];\n");
        sb.append("        if (entry != nil) [result addObject:entry];\n");
        sb.append("    }\n");
        sb.append("    return result;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)passEntriesWithCompletion:(void (^)(NSArray<PKIssuerProvisioningExtensionPassEntry *> *entries))completion {\n");
        sb.append("    NSMutableArray<PKIssuerProvisioningExtensionPassEntry *> *entries =\n");
        sb.append("            [self cn1EntriesForKey:@\"").append(PASS_ENTRIES_KEY).append("\" remote:NO];\n");
        sb.append("    ").append(MARKER_PASS_ENTRIES).append("\n");
        sb.append("    completion(entries);\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)remotePassEntriesWithCompletion:(void (^)(NSArray<PKIssuerProvisioningExtensionPassEntry *> *entries))completion {\n");
        sb.append("    NSMutableArray<PKIssuerProvisioningExtensionPassEntry *> *entries =\n");
        sb.append("            [self cn1EntriesForKey:@\"").append(REMOTE_PASS_ENTRIES_KEY).append("\" remote:YES];\n");
        sb.append("    ").append(MARKER_REMOTE_PASS_ENTRIES).append("\n");
        sb.append("    completion(entries);\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)generateAddPaymentPassRequestForPassEntryWithIdentifier:(NSString *)identifier\n");
        sb.append("        configuration:(PKAddPaymentPassRequestConfiguration *)configuration\n");
        sb.append("        certificateChain:(NSArray<NSData *> *)certificates\n");
        sb.append("        nonce:(NSData *)nonce\n");
        sb.append("        nonceSignature:(NSData *)nonceSignature\n");
        sb.append("        completionHandler:(void (^)(PKAddPaymentPassRequest *request))completion {\n");
        sb.append("    NSMutableArray *certStrings = [NSMutableArray array];\n");
        sb.append("    for (NSData *c in certificates) {\n");
        sb.append("        [certStrings addObject:[c base64EncodedStringWithOptions:0]];\n");
        sb.append("    }\n");
        sb.append("    NSMutableDictionary *payload = [NSMutableDictionary dictionary];\n");
        sb.append("    payload[@\"certificates\"] = certStrings;\n");
        sb.append("    payload[@\"nonce\"] = [nonce base64EncodedStringWithOptions:0];\n");
        sb.append("    payload[@\"nonceSignature\"] = [nonceSignature base64EncodedStringWithOptions:0];\n");
        sb.append("    payload[@\"cardIdentifier\"] = identifier;\n");
        sb.append("    NSString *token = [cn1WalletDefaults() stringForKey:@\"").append(AUTH_TOKEN_KEY).append("\"];\n");
        sb.append("    if (token != nil) payload[@\"authToken\"] = token;\n");
        sb.append("    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:\n");
        sb.append("            [NSURL URLWithString:cn1WalletInfoString(@\"").append(ISSUER_ENDPOINT_PLIST_KEY).append("\")]];\n");
        sb.append("    req.HTTPMethod = @\"POST\";\n");
        sb.append("    [req setValue:@\"application/json\" forHTTPHeaderField:@\"Content-Type\"];\n");
        sb.append("    if (token != nil) {\n");
        sb.append("        [req setValue:[@\"Bearer \" stringByAppendingString:token] forHTTPHeaderField:@\"Authorization\"];\n");
        sb.append("    }\n");
        sb.append("    ").append(MARKER_GENERATE_REQUEST).append("\n");
        sb.append("    req.HTTPBody = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];\n");
        sb.append("    [[[NSURLSession sharedSession] dataTaskWithRequest:req\n");
        sb.append("            completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {\n");
        sb.append("        PKAddPaymentPassRequest *passRequest = nil;\n");
        sb.append("        if (error == nil && data != nil) {\n");
        sb.append("            NSDictionary *json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];\n");
        sb.append("            if ([json isKindOfClass:[NSDictionary class]]) {\n");
        sb.append("                passRequest = [[PKAddPaymentPassRequest alloc] init];\n");
        sb.append("                NSString *activationData = json[@\"activationData\"];\n");
        sb.append("                NSString *encryptedPassData = json[@\"encryptedPassData\"];\n");
        sb.append("                NSString *ephemeralPublicKey = json[@\"ephemeralPublicKey\"];\n");
        sb.append("                NSString *wrappedKey = json[@\"wrappedKey\"];\n");
        sb.append("                if (activationData != nil) {\n");
        sb.append("                    passRequest.activationData = [[NSData alloc] initWithBase64EncodedString:activationData options:0];\n");
        sb.append("                }\n");
        sb.append("                if (encryptedPassData != nil) {\n");
        sb.append("                    passRequest.encryptedPassData = [[NSData alloc] initWithBase64EncodedString:encryptedPassData options:0];\n");
        sb.append("                }\n");
        sb.append("                if (ephemeralPublicKey != nil) {\n");
        sb.append("                    passRequest.ephemeralPublicKey = [[NSData alloc] initWithBase64EncodedString:ephemeralPublicKey options:0];\n");
        sb.append("                }\n");
        sb.append("                if (wrappedKey != nil) {\n");
        sb.append("                    passRequest.wrappedKey = [[NSData alloc] initWithBase64EncodedString:wrappedKey options:0];\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        ").append(MARKER_GENERATE_RESPONSE).append("\n");
        sb.append("        completion(passRequest);\n");
        sb.append("    }] resume];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("@end\n");
        return sb.toString();
    }

    private String buildUIHeader() {
        return "#import <UIKit/UIKit.h>\n"
                + "#import <PassKit/PassKit.h>\n"
                + "\n"
                + "API_AVAILABLE(ios(14.0))\n"
                + "@interface " + UI_PRINCIPAL_CLASS + " : UIViewController <PKIssuerProvisioningExtensionAuthorizationProviding>\n"
                + "@property (nonatomic, copy) void (^completionHandler)(PKIssuerProvisioningExtensionAuthorizationResult result);\n"
                + "@end\n";
    }

    private String buildUISource() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("#import \"").append(UI_PRINCIPAL_CLASS).append(".h\"\n");
        sb.append(MARKER_UI_IMPORTS).append("\n");
        sb.append("\n");
        sb.append("// Auto-generated by Codename One (ios.wallet.includeUI build hint).\n");
        sb.append("// Presents a minimal login form inside Wallet; a successful POST to the\n");
        sb.append("// ios.wallet.authEndpoint URL stores the returned token in the App Group\n");
        sb.append("// so the non-UI extension can authorize the provisioning request.\n");
        sb.append("\n");
        sb.append("@interface ").append(UI_PRINCIPAL_CLASS).append(" ()\n");
        sb.append("@property (nonatomic, strong) UITextField *cn1UserField;\n");
        sb.append("@property (nonatomic, strong) UITextField *cn1PasswordField;\n");
        sb.append("@property (nonatomic, strong) UILabel *cn1ErrorLabel;\n");
        sb.append("@end\n");
        sb.append("\n");
        sb.append("@implementation ").append(UI_PRINCIPAL_CLASS).append("\n");
        sb.append("\n");
        sb.append("- (void)viewDidLoad {\n");
        sb.append("    [super viewDidLoad];\n");
        sb.append("    self.view.backgroundColor = [UIColor systemBackgroundColor];\n");
        sb.append("\n");
        sb.append("    self.cn1UserField = [[UITextField alloc] init];\n");
        sb.append("    self.cn1UserField.placeholder = @\"Username\";\n");
        sb.append("    self.cn1UserField.borderStyle = UITextBorderStyleRoundedRect;\n");
        sb.append("    self.cn1UserField.autocapitalizationType = UITextAutocapitalizationTypeNone;\n");
        sb.append("    self.cn1UserField.autocorrectionType = UITextAutocorrectionTypeNo;\n");
        sb.append("\n");
        sb.append("    self.cn1PasswordField = [[UITextField alloc] init];\n");
        sb.append("    self.cn1PasswordField.placeholder = @\"Password\";\n");
        sb.append("    self.cn1PasswordField.borderStyle = UITextBorderStyleRoundedRect;\n");
        sb.append("    self.cn1PasswordField.secureTextEntry = YES;\n");
        sb.append("\n");
        sb.append("    self.cn1ErrorLabel = [[UILabel alloc] init];\n");
        sb.append("    self.cn1ErrorLabel.textColor = [UIColor systemRedColor];\n");
        sb.append("    self.cn1ErrorLabel.font = [UIFont systemFontOfSize:13];\n");
        sb.append("    self.cn1ErrorLabel.numberOfLines = 0;\n");
        sb.append("\n");
        sb.append("    UIButton *signIn = [UIButton buttonWithType:UIButtonTypeSystem];\n");
        sb.append("    [signIn setTitle:@\"Sign In\" forState:UIControlStateNormal];\n");
        sb.append("    [signIn addTarget:self action:@selector(cn1SignIn) forControlEvents:UIControlEventTouchUpInside];\n");
        sb.append("\n");
        sb.append("    UIButton *cancel = [UIButton buttonWithType:UIButtonTypeSystem];\n");
        sb.append("    [cancel setTitle:@\"Cancel\" forState:UIControlStateNormal];\n");
        sb.append("    [cancel addTarget:self action:@selector(cn1Cancel) forControlEvents:UIControlEventTouchUpInside];\n");
        sb.append("\n");
        sb.append("    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:\n");
        sb.append("            @[self.cn1UserField, self.cn1PasswordField, self.cn1ErrorLabel, signIn, cancel]];\n");
        sb.append("    stack.axis = UILayoutConstraintAxisVertical;\n");
        sb.append("    stack.spacing = 12;\n");
        sb.append("    stack.translatesAutoresizingMaskIntoConstraints = NO;\n");
        sb.append("    [self.view addSubview:stack];\n");
        sb.append("    [NSLayoutConstraint activateConstraints:@[\n");
        sb.append("        [stack.centerYAnchor constraintEqualToAnchor:self.view.centerYAnchor],\n");
        sb.append("        [stack.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor constant:32],\n");
        sb.append("        [stack.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor constant:-32]\n");
        sb.append("    ]];\n");
        sb.append("    ").append(MARKER_UI_VIEWDIDLOAD).append("\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)cn1Cancel {\n");
        sb.append("    if (self.completionHandler != nil) {\n");
        sb.append("        self.completionHandler(PKIssuerProvisioningExtensionAuthorizationResultCanceled);\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)cn1SignIn {\n");
        sb.append("    NSString *endpoint = [[NSBundle mainBundle] objectForInfoDictionaryKey:@\"").append(AUTH_ENDPOINT_PLIST_KEY).append("\"];\n");
        sb.append("    NSMutableDictionary *payload = [NSMutableDictionary dictionary];\n");
        sb.append("    payload[@\"username\"] = self.cn1UserField.text != nil ? self.cn1UserField.text : @\"\";\n");
        sb.append("    payload[@\"password\"] = self.cn1PasswordField.text != nil ? self.cn1PasswordField.text : @\"\";\n");
        sb.append("    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:endpoint]];\n");
        sb.append("    req.HTTPMethod = @\"POST\";\n");
        sb.append("    [req setValue:@\"application/json\" forHTTPHeaderField:@\"Content-Type\"];\n");
        sb.append("    ").append(MARKER_UI_AUTH_REQUEST).append("\n");
        sb.append("    req.HTTPBody = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];\n");
        sb.append("    __weak ").append(UI_PRINCIPAL_CLASS).append(" *weakSelf = self;\n");
        sb.append("    [[[NSURLSession sharedSession] dataTaskWithRequest:req\n");
        sb.append("            completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {\n");
        sb.append("        NSString *token = nil;\n");
        sb.append("        if (error == nil && data != nil) {\n");
        sb.append("            NSDictionary *json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];\n");
        sb.append("            if ([json isKindOfClass:[NSDictionary class]]) {\n");
        sb.append("                token = json[@\"token\"] != nil ? json[@\"token\"] : json[@\"authToken\"];\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        ").append(MARKER_UI_AUTH_RESPONSE).append("\n");
        sb.append("        dispatch_async(dispatch_get_main_queue(), ^{\n");
        sb.append("            ").append(UI_PRINCIPAL_CLASS).append(" *strongSelf = weakSelf;\n");
        sb.append("            if (strongSelf == nil) return;\n");
        sb.append("            if (token != nil) {\n");
        sb.append("                NSString *group = [[NSBundle mainBundle] objectForInfoDictionaryKey:@\"").append(APP_GROUP_PLIST_KEY).append("\"];\n");
        sb.append("                NSUserDefaults *d = group != nil ? [[NSUserDefaults alloc] initWithSuiteName:group] : nil;\n");
        sb.append("                [d setObject:token forKey:@\"").append(AUTH_TOKEN_KEY).append("\"];\n");
        sb.append("                if (strongSelf.completionHandler != nil) {\n");
        sb.append("                    strongSelf.completionHandler(PKIssuerProvisioningExtensionAuthorizationResultAuthorized);\n");
        sb.append("                }\n");
        sb.append("            } else {\n");
        sb.append("                strongSelf.cn1ErrorLabel.text = @\"Sign in failed. Please try again.\";\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("    }] resume];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("@end\n");
        return sb.toString();
    }
}
