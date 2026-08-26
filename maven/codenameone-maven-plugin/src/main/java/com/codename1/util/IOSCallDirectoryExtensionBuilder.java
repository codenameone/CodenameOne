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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the iOS Call Directory app extension behind
 * {@code com.codename1.call.directory}.
 *
 * <p>Caller identification on iOS is not something the app does. The system
 * starts a <em>separate process</em> on its own schedule, hands it a
 * {@code CXCallDirectoryExtensionContext}, and expects the numbers back; the
 * app never sees the incoming call. So the numbers have to be somewhere both
 * processes can reach, which is why this pairs with an App Group and why
 * {@code CallDirectory.setEntries} and {@code reload} are two steps rather
 * than one.</p>
 *
 * <p>The extension is generated rather than shipped prebuilt for the reason
 * {@link MatterExtensionBuilder} gives: an app extension is a target with its
 * own bundle identifier, and that identifier is derived from the host app's.
 * A prebuilt one could not be signed for anybody.</p>
 *
 * <p>Objective-C rather than Swift, unlike the Matter extension. That one has
 * no choice -- MatterSupport has no Objective-C interface -- while CallKit
 * does, and Objective-C spares the extension the Swift runtime it would
 * otherwise embed. An extension is memory-capped, so that is not free.</p>
 *
 * <p><b>Keep this file in sync with
 * {@code com.codename1.build.daemon.IOSCallDirectoryExtensionBuilder}.</b></p>
 */
public final class IOSCallDirectoryExtensionBuilder {

    /** The generated target's name. */
    public static final String EXTENSION_NAME = "CN1CallDirectory";

    /**
     * What tells iOS this is a call directory rather than some other kind.
     *
     * <p>Read from Xcode's own "Call Directory Extension" template rather
     * than from memory:
     * {@code Platforms/iPhoneOS.platform/Developer/Library/Xcode/Templates/
     * Project Templates/iOS/Application Extension/Call Directory
     * Extension.xctemplate/TemplateInfo.plist} sets
     * {@code NSExtensionPointIdentifier} to this value, and the visionOS
     * template agrees. The IdentityLookup family owns
     * {@code com.apple.identitylookup.message-filter} and
     * {@code .classification-ui}; it has no call-directory point, and
     * {@code com.apple.identitylookup.call-directory} appears nowhere in the
     * toolchain.</p>
     *
     * <p>Getting it wrong is silent in the way this whole feature is silent:
     * the extension builds, signs and embeds, and iOS never launches it, so
     * caller identification is simply absent with nothing to read anywhere.
     * Check the template before changing it.</p>
     */
    public static final String EXTENSION_POINT =
            "com.apple.callkit.call-directory";

    /**
     * The floor for a call directory extension.
     *
     * <p>Well under any deployment target Codename One still builds for; it is
     * stated so the generated target does not inherit a newer one from the
     * host app for no reason.</p>
     */
    public static final String DEPLOYMENT_TARGET = "12.0";

    /** The file both processes agree on, written by {@code CallDirectory}. */
    public static final String DATA_FILE = "cn1calldirectory.tsv";

    private IOSCallDirectoryExtensionBuilder() {
    }

    /**
     * Builds the extension's sources, in a stable order.
     *
     * @param packageName   the host application's bundle identifier
     * @param appGroup      the group the app and the extension share
     * @param displayName   the host application's display name
     * @param shortVersion  CFBundleShortVersionString
     * @param bundleVersion CFBundleVersion
     * @return path to content
     */
    public static Map<String, byte[]> buildFileMap(String packageName,
            String appGroup, String displayName, String shortVersion,
            String bundleVersion) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("CN1CallDirectoryHandler.h", utf8(handlerHeader()));
        files.put("CN1CallDirectoryHandler.m", utf8(handlerSource(appGroup)));
        files.put("Info.plist",
                utf8(infoPlist(displayName, shortVersion, bundleVersion)));
        files.put(EXTENSION_NAME + ".entitlements",
                utf8(entitlements(appGroup)));
        return files;
    }

    /** The bundle identifier the generated target signs under. */
    public static String bundleId(String packageName) {
        return packageName + ".calldirectory";
    }

    /** The app group the app and the extension share, absent a project one. */
    public static String defaultAppGroup(String packageName) {
        return "group." + packageName + ".cn1call";
    }

    static String handlerHeader() {
        return "#import <CallKit/CallKit.h>\n"
                + "\n"
                + "@interface CN1CallDirectoryHandler"
                + " : CXCallDirectoryProvider"
                // Declared, not implied. CallKit sends the failure to the
                // context's delegate through this protocol, and its one
                // method is required -- a delegate that does not conform can
                // be sent a selector it does not recognise, which terminates
                // the extension instead of logging the error it was told
                // about.
                + " <CXCallDirectoryExtensionContextDelegate>\n"
                + "@end\n";
    }

    /**
     * The extension's principal class.
     *
     * <p>Three things here are requirements rather than choices, and each one
     * fails silently when it is missed.</p>
     *
     * <p><b>Entries must be added in ascending numerical order.</b> iOS
     * rejects the whole list otherwise, naming no row.
     * {@code CallDirectory.setEntries} sorts before writing, so this reads the
     * file in order and trusts it.</p>
     *
     * <p><b>An incremental reload must be handled separately.</b> When
     * {@code isIncremental} is set the context wants only what changed, and
     * adding everything again is an error. There is no changelog here, so the
     * request is turned back into a full reload with
     * {@code removeAllIdentificationEntries}.</p>
     *
     * <p><b>The extension is memory-capped.</b> The file is streamed a line at
     * a time rather than read whole, because a large blocklist read into a
     * string is exactly what gets the extension killed.</p>
     */
    static String handlerSource(String appGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("#import \"CN1CallDirectoryHandler.h\"\n\n");
        sb.append("// The group the host app writes ").append(DATA_FILE)
                .append(" into. It is\n");
        sb.append("// baked in rather than read from a plist because this"
                + " process has no\n");
        sb.append("// other way to learn it.\n");
        sb.append("static NSString * const kCN1CallAppGroup = @\"")
                .append(escapeObjC(appGroup)).append("\";\n\n");
        sb.append("@implementation CN1CallDirectoryHandler\n\n");
        sb.append("- (void)beginRequestWithExtensionContext:"
                + "(CXCallDirectoryExtensionContext *)context {\n");
        sb.append("    context.delegate = self;\n");
        sb.append("    if (context.isIncremental) {\n");
        sb.append("        // No changelog is kept, and adding the whole list"
                + " again during an\n");
        sb.append("        // incremental request is an error -- so the"
                + " request is turned\n");
        sb.append("        // back into a full one.\n");
        sb.append("        [context removeAllIdentificationEntries];\n");
        sb.append("        [context removeAllBlockingEntries];\n");
        sb.append("    }\n");
        sb.append("    NSURL *container = [[NSFileManager defaultManager]\n");
        sb.append("            containerURLForSecurityApplicationGroupIdentifier:"
                + "kCN1CallAppGroup];\n");
        sb.append("    if (container == nil) {\n");
        sb.append("        [context completeRequestWithCompletionHandler:nil];\n");
        sb.append("        return;\n");
        sb.append("    }\n");
        sb.append("    NSURL *file = [container URLByAppendingPathComponent:@\"")
                .append(DATA_FILE).append("\"];\n");
        sb.append("    // Memory-mapped rather than read. A production"
                + " blocklist runs to\n");
        sb.append("    // six figures, and a call directory extension has a"
                + " tight memory\n");
        sb.append("    // budget -- reading it into an NSString is how the"
                + " extension gets\n");
        sb.append("    // killed and the reload silently fails. Mapping lets"
                + " the kernel\n");
        sb.append("    // evict pages behind the scan.\n");
        sb.append("    NSData *data = [NSData dataWithContentsOfURL:file\n");
        sb.append("            options:NSDataReadingMappedIfSafe error:nil];\n");
        sb.append("    if (data == nil || [data length] == 0) {\n");
        sb.append("        // Nothing installed yet. Completing with no"
                + " entries is the\n");
        sb.append("        // correct answer; failing would make iOS disable"
                + " the extension.\n");
        sb.append("        [context completeRequestWithCompletionHandler:nil];\n");
        sb.append("        return;\n");
        sb.append("    }\n");
        sb.append("    const char *bytes = (const char *)[data bytes];\n");
        sb.append("    NSUInteger length = [data length];\n");
        sb.append("    NSUInteger lineStart = 0;\n");
        sb.append("    int64_t previous = 0;\n");
        sb.append("    // One pool per batch of rows, drained as it goes:"
                + " a single pool\n");
        sb.append("    // around the whole loop would hold every temporary"
                + " to the end.\n");
        sb.append("    @autoreleasepool {\n");
        sb.append("        NSUInteger sinceDrain = 0;\n");
        sb.append("        for (NSUInteger i = 0; i <= length; i++) {\n");
        sb.append("            if (i != length && bytes[i] != '\\n') {\n");
        sb.append("                continue;\n");
        sb.append("            }\n");
        sb.append("            NSUInteger lineLength = i - lineStart;\n");
        sb.append("            if (lineLength == 0) {\n");
        sb.append("                lineStart = i + 1;\n");
        sb.append("                continue;\n");
        sb.append("            }\n");
        sb.append("            // The number is parsed from the raw bytes,"
                + " so the common\n");
        sb.append("            // case allocates nothing at all; only a row"
                + " that carries a\n");
        sb.append("            // label builds an NSString.\n");
        sb.append("            int64_t number = 0;\n");
        sb.append("            NSUInteger cursor = lineStart;\n");
        sb.append("            while (cursor < i && bytes[cursor] >= '0'"
                + " && bytes[cursor] <= '9') {\n");
        sb.append("                number = number * 10 + (bytes[cursor] - '0');\n");
        sb.append("                cursor++;\n");
        sb.append("            }\n");
        sb.append("            // Ascending order is a hard requirement, and"
                + " a row out of\n");
        sb.append("            // order would have iOS reject the whole list."
                + " The host app\n");
        sb.append("            // sorts before writing, so a violation means"
                + " the file was\n");
        sb.append("            // not written by it: skip rather than poison"
                + " the load.\n");
        sb.append("            if (number <= previous || cursor >= i"
                + " || bytes[cursor] != '\\t') {\n");
        sb.append("                lineStart = i + 1;\n");
        sb.append("                continue;\n");
        sb.append("            }\n");
        sb.append("            previous = number;\n");
        sb.append("            NSUInteger labelStart = cursor + 1;\n");
        sb.append("            NSUInteger labelEnd = labelStart;\n");
        sb.append("            while (labelEnd < i && bytes[labelEnd] != '\\t') {\n");
        sb.append("                labelEnd++;\n");
        sb.append("            }\n");
        sb.append("            BOOL blocked = labelEnd + 1 < i"
                + " && bytes[labelEnd + 1] == '1';\n");
        sb.append("            if (blocked) {\n");
        sb.append("                [context addBlockingEntryWithNextSequential"
                + "PhoneNumber:number];\n");
        sb.append("            }\n");
        sb.append("            if (labelEnd > labelStart) {\n");
        sb.append("                NSString *label = [[NSString alloc]"
                + " initWithBytes:bytes + labelStart\n");
        sb.append("                        length:labelEnd - labelStart\n");
        sb.append("                        encoding:NSUTF8StringEncoding];\n");
        sb.append("                if (label != nil) {\n");
        sb.append("                    [context addIdentificationEntryWithNext"
                + "SequentialPhoneNumber:number\n");
        sb.append("                            label:label];\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("            lineStart = i + 1;\n");
        sb.append("            sinceDrain++;\n");
        sb.append("        }\n");
        sb.append("        (void)sinceDrain;\n");
        sb.append("    }\n");
        sb.append("    [context completeRequestWithCompletionHandler:nil];\n");
        sb.append("}\n\n");
        // requestFailedForExtensionContext:withError:, which is the whole
        // selector CXCallDirectoryExtensionContextDelegate declares. The
        // shorter requestFailed:error: is not a CallKit selector at all, so
        // the delegate assignment above pointed at a method nothing calls and
        // the reload error went nowhere.
        sb.append("- (void)requestFailedForExtensionContext:"
                + "(CXCallDirectoryExtensionContext *)context\n");
        sb.append("                                withError:(NSError *)error {\n");
        sb.append("    // Logged and swallowed. There is nothing to retry"
                + " against and no\n");
        sb.append("    // user to tell; iOS reports the failure through"
                + " getEnabledStatus,\n");
        sb.append("    // which is what CallDirectory.getStatus reads.\n");
        sb.append("    NSLog(@\"CN1CallDirectory request failed: %@\","
                + " error);\n");
        sb.append("}\n\n");
        sb.append("@end\n");
        return sb.toString();
    }

    static String infoPlist(String displayName, String shortVersion,
            String bundleVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        sb.append("    <key>CFBundleDevelopmentRegion</key>\n");
        sb.append("    <string>en</string>\n");
        sb.append("    <key>CFBundleDisplayName</key>\n");
        sb.append("    <string>").append(escape(displayName))
                .append("</string>\n");
        sb.append("    <key>CFBundleExecutable</key>\n");
        sb.append("    <string>").append(EXTENSION_NAME).append("</string>\n");
        sb.append("    <key>CFBundleIdentifier</key>\n");
        sb.append("    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n");
        sb.append("    <key>CFBundleInfoDictionaryVersion</key>\n");
        sb.append("    <string>6.0</string>\n");
        sb.append("    <key>CFBundleName</key>\n");
        sb.append("    <string>").append(EXTENSION_NAME).append("</string>\n");
        sb.append("    <key>CFBundlePackageType</key>\n");
        sb.append("    <string>XPC!</string>\n");
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
        sb.append("        <string>CN1CallDirectoryHandler</string>\n");
        sb.append("    </dict>\n");
        sb.append("</dict>\n</plist>\n");
        return sb.toString();
    }

    static String entitlements(String appGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        sb.append("    <key>com.apple.security.application-groups</key>\n");
        sb.append("    <array>\n");
        sb.append("        <string>").append(escape(appGroup))
                .append("</string>\n");
        sb.append("    </array>\n");
        sb.append("</dict>\n</plist>\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeObjC(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException never) {
            throw new IllegalStateException(never);
        }
    }
}
