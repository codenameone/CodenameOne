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
package com.codename1.build.shared;

import com.codename1.build.shared.BuildHints.Hint;

import java.util.List;

/**
 * iOS build hints, including the Info.plist privacy strings.
 *
 * <p>Seeded by mining every {@code getArg} call site in the builders, so the
 * name and the default match what the build actually reads. Curated entries
 * carry an annotation attribute and, where the domain is provably closed, an
 * enum; the rest are described but set through
 * {@code codenameone_settings.properties}.</p>
 *
 * <p>Split out of {@link BuildHints} because a single class initializer
 * holding every entry would exceed the JVM's 64KB per-method limit.</p>
 */
final class BuildHintsIos {

    private BuildHintsIos() {
    }

    static void register(List<Hint> h) {
        h.add(new Hint("ios.NFCReaderUsageDescription")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.NSBonjourServices")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.UIRequiredDeviceCapabilities")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.actionSheetStyle")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.afterFinishLaunching")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Objective-C code that can be injected into the iOS app delegate at the bottom of the "
                        + "body of the didFinishLaunchingWithOptions callback method"));

        h.add(new Hint("ios.appAttest")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.appAttest.environment")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.appUsesNonExemptEncryption")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.app_groups")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Space-delimited list of app groups that this app belongs to as described in "
                        + "https://developer.apple.com/library/content/documentation/Miscellaneous/Reference/EntitlementKeyReference/Chapters/EnablingAppSandbox.html#//apple_ref/doc/uid/TP40011195-CH4-SW19[Apple's "
                        + "documentation]. These are added to the entitlements file with key "
                        + "`com.apple.security.application-groups`."));

        h.add(new Hint("ios.applicationDidEnterBackground")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Objective-C code that can be injected into the iOS callback method (message) "
                        + "`applicationDidEnterBackground`."));

        h.add(new Hint("ios.associatedDomains")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Comma-delimited list of domains associated with this app. Each domain should be prefixed "
                        + "by a supported prefix. For example, \"applinks:\" or \"webcredentials:.\" See "
                        + "https://developer.apple.com/documentation/security/password_autofill/setting_up_an_app_s_associated_domains?language=objc[Apple's "
                        + "documentation on Associated domains] for more information."));

        h.add(new Hint("ios.backgroundProcessingIds")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.background_modes")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.bitcode")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("true/false defaults to false. Enables bitcode support for the build."));

        h.add(new Hint("ios.blockScreenshotsOnEnterBackground")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("true/false (defaults to false). Indicates that app should prevent iOS from taking "
                        + "screenshots when app enters background. Described "
                        + "https://shannah.github.io/cn1-recipes/#_hiding_sensitive_data_when_entering_background[here]."));

        h.add(new Hint("ios.bluetooth.background")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.buildType")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("debug")
                .platform("ios"));

        h.add(new Hint("ios.carplay.audio")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.carplay.messaging")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.carplay.navigation")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.carplay.poi")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.convertSignalsToExceptions")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.criticalAlerts")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.crypto.gcm")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.debug.teamId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Specifies the team ID associated with the iOS debug provisioning profile and "
                        + "certificate."));

        h.add(new Hint("ios.delayPushCompletion")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.detectJailbreak")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("true/false (defaults to false). When true, the iOS app will exit on launch if it detects "
                        + "that it's running on a jailbroken device."));

        h.add(new Hint("ios.devLocale")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.disableScreenshots")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.enableAutoplayVideo")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Boolean true/false defaults to false. Makes videos \"autoplay\" when loaded on iOS"));

        h.add(new Hint("ios.enableBadgeClear")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios")
                .doc("Boolean true/false defaults to true. Clears the badge value with every load of the app, "
                        + "this is useful if the app doesn't manually keep track of number values for the badge"));

        h.add(new Hint("ios.enableGalleryMultiselect")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.enableStatusBar7")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer.applesignin")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer.healthkit")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer.homekit")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer.networking.HotspotConfiguration")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer.nfc.hce")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.entitlements.com.apple.developer.nfc.readersession.formats")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.facebook.usePods")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.facebook.version")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("~>5.6.0")
                .platform("ios"));

        h.add(new Hint("ios.facebook_permissions")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Permissions for Facebook used in the Android build target, applicable only if Facebook "
                        + "native integration is used."));

        h.add(new Hint("ios.failOnWarning")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.fieldNullChecks")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.fileSharingEnabled")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.firebaseAnalytics")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.firebaseAnalyticsVersion")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.force64")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.generateSplashScreens")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Boolean true/false defaults to false. Enables legacy generation of splash screen images "
                        + "instead of the current launch storyboards."));

        h.add(new Hint("ios.glAppDelegateBody")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Objective-C code that can be injected into the iOS app delegate within the body of the "
                        + "file before the end. This only makes sence for methods that aren't already declared in "
                        + "the class"));

        h.add(new Hint("ios.googleAdUnitId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Allows integrating admob/google play ads, this is effectively identical to "
                        + "google.adUnitId but only applies to iOS"));

        h.add(new Hint("ios.googleAdUnitIdPadding")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Indicates the amount of padding to pass to the Google Ads placed at the bottom of the "
                        + "screen with `google.adUnitId`"));

        h.add(new Hint("ios.googleAdUnitTestDevice")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("97cfc76e5efbc6dfa7eb2e6857b613a0")
                .platform("ios"));

        h.add(new Hint("ios.gplus.clientId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.hceAids")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.headphoneCallback")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Boolean true/false defaults to false. When set to true it assumes the main class has two "
                        + "methods: `headphonesConnected` & `headphonesDisconnected` which it invokes appropriately "
                        + "as needed"));

        h.add(new Hint("ios.health.backgroundDelivery")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.health.recalibrateEstimates")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.health.required")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.home.appGroup")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.home.commissioning")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.home.commissioning.displayName")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.home.commissioning.fabric")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.home.commissioning.vendorId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("0xFFF1")
                .platform("ios"));

        h.add(new Hint("ios.home.required")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.includeNullChecks")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.intents.appIntents")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.intents.minDeploymentTarget")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.keyboardOpen")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios")
                .doc("Flips between iOS keyboard open mode and autofold keyboard mode. Defaults to true which "
                        + "means the keyboard will remain open and not fold automatically when editing moves to "
                        + "another field."));

        h.add(new Hint("ios.launchPlaceholder")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.launchStoryboardName")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("LaunchScreen")
                .platform("ios"));

        h.add(new Hint("ios.locationUsageDescription")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("This flag is required for iOS 8 and newer if you're using the location API. It needs to "
                        + "include a description of the reason for which you need access to the users location"));

        h.add(new Hint("ios.lowMemCamera")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.maps.provider")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("iOS's own native map provider, overriding `maps.provider`."));

        h.add(new Hint("ios.metal")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios")
                .doc("Boolean true/false defaults to true. Selects the Metal rendering backend "
                        + "(`CAMetalLayer`) over the legacy OpenGL ES 2 path (`CAEAGLLayer`). Metal is the "
                        + "supported iOS graphics API; OpenGL ES is deprecated. Set to `false` to opt out if you "
                        + "hit a Metal-only rendering regression. See link:#_metal_renderer[Working with iOS / "
                        + "Metal renderer] for details."));

        h.add(new Hint("ios.metal.colorSpace")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("sRGB")
                .platform("ios")
                .doc("Selects the `CAMetalLayer.colorspace` for the Metal renderer. Accepts `sRGB` (default), "
                        + "`displayP3`, `deviceRGB`, `linearSRGB`, `extendedSRGB`, `extendedLinearSRGB`, or `none`. "
                        + "Has no effect when `ios.metal=false`. See "
                        + "link:#_choosing_a_color_space_for_the_metal_renderer[Working with iOS / Choosing a color "
                        + "space] for the full table."));

        h.add(new Hint("ios.mopubAdSize")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("MOPUB_BANNER_SIZE")
                .platform("ios"));

        h.add(new Hint("ios.mopubId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.mopubTabletAdSize")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("MOPUB_LEADERBOARD_SIZE")
                .platform("ios"));

        h.add(new Hint("ios.mopubTabletId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.multitasking")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios")
                .doc("Set to true to enable iOS multitasking and split-screen support. This only works if "
                        + "`ios.xcode_verson=9.2`."));

        h.add(new Hint("ios.nativeVerify")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("`nativeVerify` for the iOS translation alone."));

        h.add(new Hint("ios.nearby.accessoryServices")
                .group(HintGroup.IOS)
                .type(HintType.STRING_LIST)
                .separator(",")
                .platform("ios")
                .doc("Bluetooth service UUIDs published as `NSAccessorySetupBluetoothServices`, "
                        + "so AccessorySetupKit can show a picker for them. Unset publishes "
                        + "none."));

        h.add(new Hint("ios.nearby.background")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Requests the nearby-interaction entitlement and the background mode that "
                        + "go with ranging while backgrounded. Off by default because the "
                        + "entitlement has to be on the provisioning profile, and requesting it "
                        + "without one fails signing for every ranging app."));

        h.add(new Hint("ios.nearby.serviceType")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Bonjour service type the nearby transport advertises. Derived from the "
                        + "package name when unset."));

        h.add(new Hint("ios.noUIWebView")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.no_strip")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.notificationPermissionAtLaunch")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("true/false (defaults to false). Backward-compatibility flag for the pre-issue-#4876 "
                        + "behavior. By default, the iOS notification permission prompt is deferred until the app "
                        + "calls `Push.register()` or schedules a `LocalNotification`, matching the Android flow "
                        + "and giving the developer a chance to display a rationale screen first. Set this hint to "
                        + "`true` to restore the legacy behavior in which the prompt fires automatically inside "
                        + "`application:didFinishLaunchingWithOptions:` as soon as the app launches. Existing apps "
                        + "relying on the prompt being shown at launch should set this to `true`; new apps should "
                        + "leave it disabled and trigger the prompt explicitly when they're ready to ask for "
                        + "permission."));

        h.add(new Hint("ios.openURLInject")
                .group(HintGroup.IOS)
                .type(HintType.XML)
                .separator("")
                .platform("ios"));

        h.add(new Hint("ios.optimizer")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("on")
                .platform("ios"));

        h.add(new Hint("ios.pods.build.CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.pods.build.CLANG_ENABLE_MODULES")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.pods.use_frameworks!")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.release.teamId")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Specifies the team ID associated with the iOS release provisioning profile and "
                        + "certificate."));

        h.add(new Hint("ios.shareAppGroup")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.statusBarFG")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.documentProvider.enabled")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Declares that this project publishes documents to the system file browser. "
                        + "The build detects a reference to com.codename1.documents on its own, so "
                        + "this is redundant for the build itself; it exists because the "
                        + "Certificate Wizard and the signing preflight cannot read bytecode and "
                        + "need to know that the CN1Documents extension will be generated."));

        h.add(new Hint("ios.documentProvider.appGroup")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("App Group id starting with 'group.' shared by the app and the generated "
                        + "CN1Documents extension, defaulting to 'group.' followed by the app's "
                        + "package name. It is the entire transport between the two processes: "
                        + "the app publishes its index into the group and the extension reads it "
                        + "from there, so without it the published location appears empty."));

        h.add(new Hint("ios.documentProvider.displayName")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Name shown for this location in the file browser. Defaults to the app's "
                        + "display name."));

        h.add(new Hint("ios.documentProvider.deploymentTarget")
                .group(HintGroup.IOS)
                .type(HintType.VERSION)
                .def("16.0")
                .platform("ios")
                .doc("Minimum OS version the generated extension runs on. At or above 16.0 the "
                        + "extension is an NSFileProviderReplicatedExtension; below it Codename "
                        + "One generates the deprecated NSFileProviderExtension instead. The host "
                        + "app's own deployment target is unaffected."));

        h.add(new Hint("ios.documentProvider.extension")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios")
                .doc("Set false to skip the iOS lowering entirely -- no extension target, no app "
                        + "group, no plist keys -- leaving com.codename1.documents an inert no-op "
                        + "at runtime."));

        h.add(new Hint("ios.superfastBuild")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.surfaces.appGroup")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.surfaces.deploymentTarget")
                .group(HintGroup.IOS)
                .type(HintType.VERSION)
                .def("16.1")
                .platform("ios"));

        h.add(new Hint("ios.surfaces.extension")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.surfaces.frequentUpdates")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.swiftVersion")
                .group(HintGroup.IOS)
                .type(HintType.VERSION)
                .def("5.0")
                .platform("ios"));

        h.add(new Hint("ios.timeSensitiveNotifications")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.twoDigitVersion")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.urlSchemes")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.useAVKit")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios")
                .doc("Use AVKit for video components on iOS rather than `MPMoviePlayerController` on iOS "
                        + "versions 8 through 12. iOS 13 will always use AVKit, and iOS 7 and lower will always use "
                        + "`MPMoviePlayerController`. Default value `false`"));

        h.add(new Hint("ios.useJavascriptCore")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.usePhotoKitForMultigallery")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.usePrintf")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.useWKWebView")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));

        h.add(new Hint("ios.usesBackgroundProcessing")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios"));

        h.add(new Hint("ios.viewDidLoad")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Objective-C code that can be injected into the iOS callback method (message) "
                        + "`viewDidLoad`"));

        h.add(new Hint("ios.viewDidLoadInclude")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios"));

        h.add(new Hint("ios.wallet.appGroup")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("App Group id starting with `group.` shared by the app and the generated Wallet "
                        + "extensions. The app publishes pass entries into this group through "
                        + "`com.codename1.payment.WalletExtension` and the group is added to the app and extension "
                        + "entitlements automatically. Required when `ios.wallet.extension=true`."));

        h.add(new Hint("ios.wallet.authEndpoint")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("HTTPS URL the generated login UI extension POSTs `{\"username\",\"password\"}` to; the JSON "
                        + "response's `token` is stored in the App Group for the provisioning request. Required "
                        + "when `ios.wallet.includeUI=true`."));

        h.add(new Hint("ios.wallet.extension")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Boolean true/false defaults to false. Generates an Apple Wallet issuer provisioning "
                        + "extension (the \"From apps on your iPhone\" flow in the Wallet app) and embeds it in the "
                        + "build. Requires `ios.wallet.appGroup` and `ios.wallet.issuerEndpoint`. See the Apple "
                        + "Wallet Extension chapter."));

        h.add(new Hint("ios.wallet.includeUI")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .doc("Boolean true/false defaults to false. Also generates the Wallet authorization UI "
                        + "extension - a login form shown inside the Wallet app when the app reports that "
                        + "authentication is required. Requires `ios.wallet.authEndpoint`."));

        h.add(new Hint("ios.wallet.issuerEndpoint")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("HTTPS URL of the issuer backend endpoint that produces the encrypted provisioning "
                        + "payload. The generated extension POSTs Apple's certificates/nonce plus the card "
                        + "identifier and auth token there as JSON. Required when `ios.wallet.extension=true`."));

        h.add(new Hint("ios.wallet.nonuiExtensionName")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("WalletNonUIExtension")
                .platform("ios"));

        h.add(new Hint("ios.wallet.uiExtensionName")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .def("WalletUIExtension")
                .platform("ios"));

        h.add(new Hint("ios.wallet.generateRequestInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected at the generate-request marker of the non-UI Wallet extension."));

        h.add(new Hint("ios.wallet.generateResponseInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected at the generate-response marker of the non-UI Wallet extension."));

        h.add(new Hint("ios.wallet.nonuiImportsInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Extra `import` lines for the non-UI Wallet extension."));

        h.add(new Hint("ios.wallet.passEntriesInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected where the non-UI Wallet extension lists its pass entries."));

        h.add(new Hint("ios.wallet.remotePassEntriesInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected where the non-UI Wallet extension lists its remote pass entries."));

        h.add(new Hint("ios.wallet.statusInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected at the status marker of the non-UI Wallet extension."));

        h.add(new Hint("ios.wallet.uiAuthRequestInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected at the auth-request marker of the UI Wallet extension."));

        h.add(new Hint("ios.wallet.uiAuthResponseInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected at the auth-response marker of the UI Wallet extension."));

        h.add(new Hint("ios.wallet.uiImportsInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Extra `import` lines for the UI Wallet extension."));

        h.add(new Hint("ios.wallet.uiViewDidLoadInject")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .doc("Swift injected into `viewDidLoad` of the UI Wallet extension."));

        h.add(new Hint("ios.zbar_flash")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("ios"));
    }
}
