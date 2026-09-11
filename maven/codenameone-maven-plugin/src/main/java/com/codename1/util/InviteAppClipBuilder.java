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
 * Generates the App Clip that makes invite attribution deterministic on iOS.
 *
 * <h2>Why a second binary exists</h2>
 *
 * <p>The App Store carries no referrer parameter. That is a platform fact, not
 * a gap in this implementation: an iOS install knows nothing about the link
 * that led to it, which is why every other product in this space answers the
 * question statistically, by matching a hashed profile of the visitor against
 * a hashed profile of the installer. Codename One did that too, once, and it
 * collected data about people who had installed nothing and agreed to
 * nothing.</p>
 *
 * <p>An App Clip removes the guess. The clip is launched <b>by the invite link
 * itself</b> and is handed that link exactly, so it knows the code with
 * certainty. It writes the code into the app group container it shares with
 * the full application and offers the App Store. When the person installs, the
 * application reads the container and the code has made the whole trip
 * intact -- no profile, no window, no probability.</p>
 *
 * <h2>What the generated clip is</h2>
 *
 * <p>Deliberately not a Codename One application. A clip is capped at 15 MB
 * uncompressed and must launch instantly, and it has exactly one job that
 * finishes before the person reads the screen. So it is a few hundred lines of
 * UIKit: one label, one button, and {@code SKOverlay} to offer the full app --
 * which is Apple's own install affordance and the one that carries the clip's
 * stored data forward.</p>
 *
 * <p>Pure static string-building with no build state, so the emitted files can
 * be asserted in a unit test rather than only by running a device build. The
 * three names it shares with the port's reader -- the defaults key and the two
 * field names -- are duplicated in {@code CN1InviteAppClip.m} and must move
 * together; nothing links the two binaries, so a mismatch is silent.</p>
 */
public final class InviteAppClipBuilder {

    /** Xcode target and folder name of the generated clip. */
    public static final String CLIP_NAME = "CN1InviteClip";

    /**
     * App Clips exist from iOS 14. Named here rather than inherited from the
     * application because the application's own floor is lower, and a clip
     * built against it does not launch.
     */
    public static final String DEPLOYMENT_TARGET = "14.0";

    /**
     * The product type Xcode gives an App Clip. {@code new_target} has no
     * symbol for it in every xcodeproj version we might meet, so the ruby
     * creates an application and assigns this afterwards.
     */
    public static final String PRODUCT_TYPE =
            "com.apple.product-type.application.on-demand-install-capable";

    /** The defaults key the clip writes and {@code CN1InviteAppClip.m} consumes. */
    public static final String HANDOFF_KEY = "cn1-invite-app-clip-handoff";

    /** The invite code, inside the handoff dictionary. */
    public static final String CODE_FIELD = "code";

    /** Seconds since the epoch at which the link was tapped. */
    public static final String CLICKED_FIELD = "clicked";

    private InviteAppClipBuilder() {
    }

    /**
     * The bundle identifier Apple requires of a clip: the application's own
     * with a suffix, so the pair is recognised as one product.
     *
     * @param packageName the application's bundle identifier
     * @return the clip's bundle identifier
     */
    public static String bundleId(String packageName) {
        return packageName + ".Clip";
    }

    /**
     * The app group the clip and the application exchange the code through,
     * when the developer named none.
     *
     * <p>Derived rather than fixed: an app group is namespaced to a developer
     * account, so a constant would collide between two Codename One apps on
     * the same account and let one read the other's invites.</p>
     *
     * @param packageName the application's bundle identifier
     * @return a {@code group.} identifier
     */
    public static String defaultAppGroup(String packageName) {
        return "group." + packageName + ".cn1invite";
    }

    /**
     * Builds the clip's sources and resources.
     *
     * @param packageName   the application's bundle identifier
     * @param appGroup      the shared app group, already validated
     * @param inviteHost    the host the invite links are served from
     * @param displayName   what the clip card calls the app
     * @param shortVersion  the host's marketing version
     * @param bundleVersion the host's build version
     * @param storeItemId   the App Store item identifier, or empty when it is
     *                      not known at build time
     * @return path to content, in a stable order
     */
    public static Map<String, byte[]> buildFileMap(String packageName,
            String appGroup, String inviteHost, String displayName,
            String shortVersion, String bundleVersion, String storeItemId) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("main.m", utf8(mainSource()));
        files.put("CN1InviteClipDelegate.h", utf8(delegateHeader()));
        files.put("CN1InviteClipDelegate.m",
                utf8(delegateSource(appGroup, displayName, storeItemId)));
        files.put("Info.plist",
                utf8(infoPlist(displayName, shortVersion, bundleVersion)));
        files.put(CLIP_NAME + ".entitlements",
                utf8(entitlements(packageName, appGroup, inviteHost)));
        return files;
    }

    private static String mainSource() {
        return "// Generated by Codename One. Do not edit.\n"
                + "#import <UIKit/UIKit.h>\n"
                + "#import \"CN1InviteClipDelegate.h\"\n\n"
                + "int main(int argc, char * argv[]) {\n"
                + "    @autoreleasepool {\n"
                + "        return UIApplicationMain(argc, argv, nil,\n"
                + "                NSStringFromClass([CN1InviteClipDelegate class]));\n"
                + "    }\n"
                + "}\n";
    }

    private static String delegateHeader() {
        return "// Generated by Codename One. Do not edit.\n"
                + "#import <UIKit/UIKit.h>\n\n"
                + "@interface CN1InviteClipDelegate : UIResponder <UIApplicationDelegate>\n"
                + "@property (nonatomic, strong) UIWindow *window;\n"
                + "@end\n";
    }

    /**
     * The clip itself.
     *
     * <p>Two things here are load-bearing and easy to get wrong. The invite
     * code is recorded in {@code continueUserActivity}, which on a cold launch
     * arrives <b>after</b> {@code didFinishLaunching} -- so the recording
     * cannot live in the launch path, and the launch path must tolerate having
     * no code yet. And the write is flushed immediately rather than at the
     * clip's convenience: a clip is terminated without warning the moment the
     * person taps through to the App Store, and an unflushed write is the
     * attribution.</p>
     *
     * @param appGroup    the shared container
     * @param displayName what the card calls the app
     * @param storeItemId the numeric App Store id, or empty
     * @return the source
     */
    private static String delegateSource(String appGroup, String displayName,
            String storeItemId) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by Codename One. Do not edit.\n")
          .append("#import \"CN1InviteClipDelegate.h\"\n")
          .append("#import <StoreKit/StoreKit.h>\n\n")
          .append("static NSString * const kAppGroup = @\"")
          .append(escapeObjC(appGroup)).append("\";\n")
          .append("static NSString * const kHandoffKey = @\"")
          .append(HANDOFF_KEY).append("\";\n")
          .append("static NSString * const kDisplayName = @\"")
          .append(escapeObjC(displayName)).append("\";\n")
          .append("static NSString * const kStoreItemId = @\"")
          .append(escapeObjC(storeItemId)).append("\";\n\n")
          .append("@interface CN1InviteClipDelegate ()\n")
          .append("@property (nonatomic, strong) UILabel *status;\n")
          .append("@end\n\n")
          .append("@implementation CN1InviteClipDelegate\n\n");

        // The code extraction, kept in one function so the clip and any future
        // reader of this file can see the whole grammar at once.
        sb.append("// /i/<slug>/<code>, /i/<code>, or ?code=<code>. The last\n")
          .append("// non-empty path component after /i/ is the code in both path\n")
          .append("// forms, so one rule covers them and a third form would only\n")
          .append("// need the query fallback below.\n")
          .append("static NSString *cn1InviteCodeFromURL(NSURL *url) {\n")
          .append("    if (url == nil) { return nil; }\n")
          .append("    NSURLComponents *c = [NSURLComponents componentsWithURL:url\n")
          .append("            resolvingAgainstBaseURL:NO];\n")
          .append("    for (NSURLQueryItem *item in c.queryItems) {\n")
          .append("        if ([item.name isEqualToString:@\"code\"] && item.value.length > 0) {\n")
          .append("            return item.value;\n")
          .append("        }\n")
          .append("    }\n")
          .append("    NSMutableArray<NSString *> *parts = [NSMutableArray array];\n")
          .append("    for (NSString *p in [c.percentEncodedPath componentsSeparatedByString:@\"/\"]) {\n")
          .append("        if (p.length > 0) { [parts addObject:p]; }\n")
          .append("    }\n")
          .append("    if (parts.count < 2 || ![parts[0] isEqualToString:@\"i\"]) { return nil; }\n")
          .append("    NSString *last = [parts lastObject];\n")
          .append("    return [last stringByRemovingPercentEncoding];\n")
          .append("}\n\n");

        sb.append("// Only characters an invite code can contain. The url is\n")
          .append("// somebody else's input and this value is handed to the\n")
          .append("// application, which claims with it -- so it is constrained\n")
          .append("// here, where the grammar is known, rather than trusted there.\n")
          .append("static BOOL cn1InviteCodeIsWellFormed(NSString *code) {\n")
          .append("    if (code.length == 0 || code.length > 64) { return NO; }\n")
          .append("    NSCharacterSet *allowed = [NSCharacterSet\n")
          .append("            characterSetWithCharactersInString:\n")
          .append("            @\"ABCDEFGHIJKLMNOPQRSTUVWXYZ\"\n")
          .append("            @\"abcdefghijklmnopqrstuvwxyz0123456789-_\"];\n")
          .append("    NSCharacterSet *rejected = [allowed invertedSet];\n")
          .append("    return [code rangeOfCharacterFromSet:rejected].location == NSNotFound;\n")
          .append("}\n\n");

        sb.append("- (void)recordInviteFromURL:(NSURL *)url {\n")
          .append("    NSString *code = cn1InviteCodeFromURL(url);\n")
          .append("    if (!cn1InviteCodeIsWellFormed(code)) { return; }\n")
          .append("    NSUserDefaults *suite = [[NSUserDefaults alloc] initWithSuiteName:kAppGroup];\n")
          .append("    if (suite == nil) { return; }\n")
          .append("    [suite setObject:@{ @\"").append(CODE_FIELD).append("\": code,\n")
          .append("                        @\"").append(CLICKED_FIELD)
          .append("\": @((long long)[[NSDate date] timeIntervalSince1970]) }\n")
          .append("              forKey:kHandoffKey];\n")
          .append("    // Flushed now. The clip is killed without notice the moment\n")
          .append("    // the App Store sheet takes over, and the write IS the\n")
          .append("    // attribution -- there is no second chance to make it.\n")
          .append("    [suite synchronize];\n")
          .append("    self.status.text = [NSString stringWithFormat:\n")
          .append("            @\"You were invited to %@\", kDisplayName];\n")
          .append("}\n\n");

        sb.append("- (BOOL)application:(UIApplication *)application\n")
          .append("        continueUserActivity:(NSUserActivity *)userActivity\n")
          .append("        restorationHandler:(void (^)(NSArray<id<UIUserActivityRestoring>> *))handler {\n")
          .append("    if ([userActivity.activityType isEqualToString:NSUserActivityTypeBrowsingWeb]) {\n")
          .append("        [self recordInviteFromURL:userActivity.webpageURL];\n")
          .append("    }\n")
          .append("    return YES;\n")
          .append("}\n\n");

        sb.append("- (BOOL)application:(UIApplication *)application\n")
          .append("        didFinishLaunchingWithOptions:(NSDictionary *)options {\n")
          .append("    self.window = [[UIWindow alloc] initWithFrame:UIScreen.mainScreen.bounds];\n")
          .append("    UIViewController *root = [[UIViewController alloc] init];\n")
          .append("    root.view.backgroundColor = UIColor.systemBackgroundColor;\n")
          .append("    self.status = [[UILabel alloc] init];\n")
          .append("    self.status.numberOfLines = 0;\n")
          .append("    self.status.textAlignment = NSTextAlignmentCenter;\n")
          .append("    self.status.font = [UIFont preferredFontForTextStyle:UIFontTextStyleTitle2];\n")
          .append("    self.status.text = kDisplayName;\n")
          .append("    self.status.translatesAutoresizingMaskIntoConstraints = NO;\n")
          .append("    [root.view addSubview:self.status];\n")
          .append("    UIButton *get = [UIButton buttonWithType:UIButtonTypeSystem];\n")
          .append("    [get setTitle:@\"Get the app\" forState:UIControlStateNormal];\n")
          .append("    get.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleHeadline];\n")
          .append("    [get addTarget:self action:@selector(offerFullApp)\n")
          .append("            forControlEvents:UIControlEventTouchUpInside];\n")
          .append("    get.translatesAutoresizingMaskIntoConstraints = NO;\n")
          .append("    [root.view addSubview:get];\n")
          .append("    UILayoutGuide *g = root.view.layoutMarginsGuide;\n")
          .append("    [NSLayoutConstraint activateConstraints:@[\n")
          .append("        [self.status.centerYAnchor constraintEqualToAnchor:g.centerYAnchor constant:-40],\n")
          .append("        [self.status.leadingAnchor constraintEqualToAnchor:g.leadingAnchor],\n")
          .append("        [self.status.trailingAnchor constraintEqualToAnchor:g.trailingAnchor],\n")
          .append("        [get.topAnchor constraintEqualToAnchor:self.status.bottomAnchor constant:24],\n")
          .append("        [get.centerXAnchor constraintEqualToAnchor:g.centerXAnchor]\n")
          .append("    ]];\n")
          .append("    self.window.rootViewController = root;\n")
          .append("    [self.window makeKeyAndVisible];\n")
          .append("    // A warm launch delivers the activity in the launch options\n")
          .append("    // instead of calling continueUserActivity:, so both are read.\n")
          .append("    NSDictionary *activityDict = options[UIApplicationLaunchOptionsUserActivityDictionaryKey];\n")
          .append("    for (id value in activityDict.allValues) {\n")
          .append("        if ([value isKindOfClass:[NSUserActivity class]]) {\n")
          .append("            [self recordInviteFromURL:((NSUserActivity *)value).webpageURL];\n")
          .append("        }\n")
          .append("    }\n")
          .append("    [self offerFullApp];\n")
          .append("    return YES;\n")
          .append("}\n\n");

        sb.append("// SKOverlay is Apple's own App Clip install affordance, and the\n")
          .append("// only one that carries the clip's stored data to the installed\n")
          .append("// app. Without a store id -- which a build before first release\n")
          .append("// does not have -- the clip still records the code and simply\n")
          .append("// shows no sheet; the handoff works the moment the app exists.\n")
          .append("//\n")
          .append("// AppClipConfiguration is the RIGHT configuration here, and it\n")
          .append("// takes no app identifier on purpose. A review round read that\n")
          .append("// as the bug -- kStoreItemId only guarding, never identifying\n")
          .append("// the app -- and asked for SKOverlayAppConfiguration with the\n")
          .append("// store id instead. The SDK headers settle it:\n")
          .append("//   SKOverlayAppClipConfiguration: \"an overlay configuration\n")
          .append("//     that can be used to show an app clip's full app\",\n")
          .append("//     initWithPosition: only.\n")
          .append("//   SKOverlayAppConfiguration: \"...to show any app from the\n")
          .append("//     App Store\", initWithAppIdentifier:position:.\n")
          .append("// The clip's parent app is known from the bundle relationship,\n")
          .append("// so there is nothing to pass. Switching to the app-identifier\n")
          .append("// form would offer an arbitrary store listing rather than THIS\n")
          .append("// clip's parent, which is also what the data handoff is keyed\n")
          .append("// to. The store id stays what it is: the build's own answer to\n")
          .append("// whether there is a released app to offer yet.\n")
          .append("- (void)offerFullApp {\n")
          .append("    if (kStoreItemId.length == 0) { return; }\n")
          .append("    if (@available(iOS 14.0, *)) {\n")
          .append("        SKOverlayAppClipConfiguration *config =\n")
          .append("                [[SKOverlayAppClipConfiguration alloc] initWithPosition:SKOverlayPositionBottom];\n")
          .append("        SKOverlay *overlay = [[SKOverlay alloc] initWithConfiguration:config];\n")
          .append("        UIWindowScene *scene = (UIWindowScene *)self.window.windowScene;\n")
          .append("        if (scene != nil) { [overlay presentInScene:scene]; }\n")
          .append("    }\n")
          .append("}\n\n")
          .append("@end\n");
        return sb.toString();
    }

    private static String infoPlist(String displayName, String shortVersion,
            String bundleVersion) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\""
                + " \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n"
                + "<dict>\n"
                + "  <key>CFBundleDevelopmentRegion</key>\n"
                + "  <string>en</string>\n"
                + "  <key>CFBundleDisplayName</key>\n"
                + "  <string>" + escapeXml(displayName) + "</string>\n"
                + "  <key>CFBundleExecutable</key>\n"
                + "  <string>$(EXECUTABLE_NAME)</string>\n"
                + "  <key>CFBundleIdentifier</key>\n"
                + "  <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n"
                + "  <key>CFBundleInfoDictionaryVersion</key>\n"
                + "  <string>6.0</string>\n"
                + "  <key>CFBundleName</key>\n"
                + "  <string>$(PRODUCT_NAME)</string>\n"
                + "  <key>CFBundlePackageType</key>\n"
                + "  <string>APPL</string>\n"
                // Both versions must equal the host's or archive validation
                // rejects the whole app, the same rule the extensions follow.
                + "  <key>CFBundleShortVersionString</key>\n"
                + "  <string>" + escapeXml(shortVersion) + "</string>\n"
                + "  <key>CFBundleVersion</key>\n"
                + "  <string>" + escapeXml(bundleVersion) + "</string>\n"
                + "  <key>LSRequiresIPhoneOS</key>\n"
                + "  <true/>\n"
                + "  <key>NSAppClip</key>\n"
                + "  <dict>\n"
                // False deliberately. The ephemeral notification asks for
                // permission to message somebody who has installed nothing;
                // this clip records a code and offers the store, and has no
                // reason to speak to them again.
                + "    <key>NSAppClipRequestEphemeralUserNotification</key>\n"
                + "    <false/>\n"
                + "    <key>NSAppClipRequestLocationConfirmation</key>\n"
                + "    <false/>\n"
                + "  </dict>\n"
                + "  <key>UILaunchScreen</key>\n"
                + "  <dict/>\n"
                + "  <key>UIRequiredDeviceCapabilities</key>\n"
                + "  <array>\n"
                + "    <string>armv7</string>\n"
                + "  </array>\n"
                + "  <key>UISupportedInterfaceOrientations</key>\n"
                + "  <array>\n"
                + "    <string>UIInterfaceOrientationPortrait</string>\n"
                + "    <string>UIInterfaceOrientationLandscapeLeft</string>\n"
                + "    <string>UIInterfaceOrientationLandscapeRight</string>\n"
                + "  </array>\n"
                + "</dict>\n"
                + "</plist>\n";
    }

    /**
     * The clip's entitlements.
     *
     * <p>All three are required and each fails differently when absent. Without
     * the parent identifier the clip is not recognised as belonging to the app
     * and does not install. Without the associated domain iOS never offers the
     * clip for the link, so nothing runs. Without the app group the clip runs,
     * shows its card, records nothing, and every install reads as organic --
     * the failure with no symptom.</p>
     *
     * @param packageName the application's bundle identifier
     * @param appGroup    the shared container
     * @param inviteHost  the host serving the invite links
     * @return the plist
     */
    private static String entitlements(String packageName, String appGroup,
            String inviteHost) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\""
                + " \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n"
                + "<dict>\n"
                + "  <key>com.apple.developer.parent-application-identifiers</key>\n"
                + "  <array>\n"
                + "    <string>$(AppIdentifierPrefix)" + escapeXml(packageName) + "</string>\n"
                + "  </array>\n"
                + "  <key>com.apple.developer.associated-domains</key>\n"
                + "  <array>\n"
                + "    <string>appclips:" + escapeXml(inviteHost) + "</string>\n"
                + "  </array>\n"
                + "  <key>com.apple.security.application-groups</key>\n"
                + "  <array>\n"
                + "    <string>" + escapeXml(appGroup) + "</string>\n"
                + "  </array>\n"
                + "</dict>\n"
                + "</plist>\n";
    }

    /**
     * Objective-C string-literal escaping for the handful of values
     * interpolated into generated source.
     *
     * <p>Not cosmetic: the display name is the developer's, and a quotation
     * mark in it would end the literal and leave the rest as code. A newline
     * would do the same, so both are removed rather than escaped.</p>
     *
     * @param value the raw value
     * @return a value safe inside an {@code @"..."} literal
     */
    static String escapeObjC(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c >= ' ' && c < 127) {
                sb.append(c);
            } else if (c >= 127) {
                // The generated file is compiled as UTF-8 and a display name
                // legitimately carries accents and CJK; only the control range
                // is dropped.
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String escapeXml(String value) {
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
            throw new IllegalStateException("UTF-8 is unavailable", impossible);
        }
    }
}
