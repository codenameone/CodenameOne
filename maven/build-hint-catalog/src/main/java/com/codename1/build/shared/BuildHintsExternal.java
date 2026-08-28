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
 * Hints the developer guide documents that nothing in this repository reads.
 *
 * <p>Most are consumed by build-daemon lanes whose source is not mirrored here,
 * so having no in-repo consumer is not evidence that a hint is dead. A few are
 * probably genuinely obsolete. Recording the distinction as
 * {@link Hint#isExternal()} keeps both the drift gate and the Settings tool
 * honest: the gate does not demand a consumer for these, and the tool still
 * offers them for editing.</p>
 *
 * <p>They are deliberately not annotated. Exposing a hint as a typed attribute
 * is a promise that setting it does something, and for these that promise
 * cannot be checked from this repository.</p>
 */
final class BuildHintsExternal {

    private BuildHintsExternal() {
    }

    static void register(List<Hint> h) {
        h.add(new Hint("android.fridaDebugLogging")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .external()
                .doc("Boolean true/false defaults to false. If true, it will add verbose debug logs during "
                        + "frida detection to show which check if fails on."));

        h.add(new Hint("android.fridaVersion")
                .group(HintGroup.ANDROID)
                .type(HintType.STRING)
                .platform("android")
                .external()
                .doc("x.y.z The version of [frida-blocker](https://github.com/shannah/frida-blocker) to use to "
                        + "perform frida detection. This is only relevant if `android.fridaDetection=true`. If "
                        + "omitted, it will use the latest tested version in the build server."));

        h.add(new Hint("android.signingV1")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .external()
                .doc("true/false Default true. See "
                        + "https://source.android.com/docs/security/features/apksigning"));

        h.add(new Hint("android.signingV2")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .external()
                .doc("true/false Default true. See "
                        + "https://source.android.com/docs/security/features/apksigning"));

        h.add(new Hint("android.signingV3")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .external()
                .doc("true/false Default true. See "
                        + "https://source.android.com/docs/security/features/apksigning"));

        h.add(new Hint("android.signingV4")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .external()
                .doc("true/false Default true. See "
                        + "https://source.android.com/docs/security/features/apksigning"));

        h.add(new Hint("android.supportV4")
                .group(HintGroup.ANDROID)
                .type(HintType.BOOLEAN)
                .platform("android")
                .external()
                .doc("Boolean true/false defaults to false but that can change based on usage (for example, "
                        + "push implicitly activates this). Indicates whether the android support v4 library should "
                        + "be included in the build"));

        h.add(new Hint("block_server_registration")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .platform("general")
                .external()
                .doc("true/false flag defaults to false. By default Codename One applications register with "
                        + "the Codename One server. Setting this to true blocks them from sending information to "
                        + "the Codename One cloud, which is kept for statistical purposes and may be used to "
                        + "provide more installation stats in the future."));

        h.add(new Hint("build.cn1Version")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .external()
                .doc("Pro/Enterprise only. Pins the cloud build to a specific released Codename One version "
                        + "using the Maven release scheme (for example `7.0.182`), or to `master` to build against "
                        + "the current development head. The build server fetches that version's framework "
                        + "artifacts. Pro accounts can target versions published within the last two months; "
                        + "Enterprise within the last six months. Requesting an older version, a version that was "
                        + "never published, or using this hint without a Pro/Enterprise subscription fails the "
                        + "build with an explanatory error. See Versioned builds."));

        h.add(new Hint("codename1.mac.appid")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .external()
                .doc("Mac Native cloud builds only. The Mac bundle identifier registered in App Store Connect "
                        + "/ Apple Developer. Distinct from `codename1.ios.appid` because Apple treats the iOS and "
                        + "Mac App Store records as separate products. Required for cloud Mac builds."));

        h.add(new Hint("codename1.mac.certificate")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .external()
                .doc("Mac Native cloud builds only. Path to the `.p12` file containing the Mac signing "
                        + "certificate(s) — _Mac App Distribution_ (3rd Party Mac Developer Application) for App "
                        + "Store builds, _Developer ID Application_ for Developer ID builds, or both bundled into "
                        + "the same P12 when `macNative.distribution=both`. Not interchangeable with the iOS "
                        + "distribution certificate. Required for cloud Mac builds."));

        h.add(new Hint("codename1.mac.certificatePassword")
                .group(HintGroup.GENERAL)
                .type(HintType.SECRET)
                .platform("general")
                .external()
                .doc("Mac Native cloud builds only. Password to unlock the P12 referenced by "
                        + "`codename1.mac.certificate`. Required for cloud Mac builds."));

        h.add(new Hint("codename1.mac.provision")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .external()
                .doc("Mac Native cloud builds only. Path to the Mac provisioning profile "
                        + "(`.provisionprofile`). Apple issues distinct provisioning profiles for Mac App Store and "
                        + "Developer ID distribution — pass the one that matches the chosen channel."));

        h.add(new Hint("desktop.fontSizes")
                .group(HintGroup.DESKTOP)
                .type(HintType.STRING)
                .platform("desktop")
                .external()
                .doc("Indicates the sizes in pixels for the system fonts as a comma delimited string "
                        + "containing 3 numbers for small,medium,large fonts."));

        h.add(new Hint("desktop.mac.cef")
                .group(HintGroup.DESKTOP)
                .type(HintType.BOOLEAN)
                .platform("mac")
                .external()
                .doc("Whetherto use CEF for media or BrowserComponent instead of JavaFX in Mac desktop builds. "
                        + "true/false. Default value is `false` (Jan 2021), but this will be changed to `true` in a "
                        + "future version."));

        h.add(new Hint("desktop.theme")
                .group(HintGroup.DESKTOP)
                .type(HintType.STRING)
                .platform("desktop")
                .external()
                .doc("Name of the theme res file (without the \".res\" extension) to use as the \"native\" theme. "
                        + "By default this is native indicating iOS theme on Mac and Windows Metro on Windows. If "
                        + "its something else then the app will try to load the file /themeName.res (placed in "
                        + "native/Java SE directory)."));

        h.add(new Hint("desktop.themeMac")
                .group(HintGroup.DESKTOP)
                .type(HintType.STRING)
                .platform("desktop")
                .external()
                .doc("Same as `desktop.theme` but specific to macOS"));

        h.add(new Hint("desktop.themeWin")
                .group(HintGroup.DESKTOP)
                .type(HintType.STRING)
                .platform("desktop")
                .external()
                .doc("Same as `desktop.theme` but specific to Windows"));

        h.add(new Hint("desktop.win.cef")
                .group(HintGroup.DESKTOP)
                .type(HintType.BOOLEAN)
                .platform("desktop")
                .external()
                .doc("Whether to use CEF for media and BrowserComponent instead of JavaFX in windows desktop "
                        + "builds. true/false. Default value is `false` (Jan 2021), but this will be changed to "
                        + "`true` in a future version."));

        h.add(new Hint("desktop.windowsOutput")
                .group(HintGroup.DESKTOP)
                .type(HintType.STRING)
                .platform("desktop")
                .external()
                .doc("Can be exe or msi depending on desired results"));

        h.add(new Hint("ios.NSXXXUsageDescription")
                .group(HintGroup.IOS_PRIVACY)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("iOS privacy flags for using certain APIs. Starting with Xcode 8, you're required to add "
                        + "usage description strings for certain APIs. Find a full list of the available keys in "
                        + "https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html[Apple's "
                        + "docs]. Some relevant ones include `ios.NSCameraUsageDescription`, "
                        + "`ios.NSContactsUsageDescription`, `ios.NSLocationAlwaysUsageDescription`, "
                        + "`NSLocationUsageDescription`, `ios.NSMicrophoneUsageDescription`, "
                        + "`ios.NSPhotoLibraryAddUsageDescription`, `ios.NSSpeechRecognitionUsageDescription`, "
                        + "`ios.NSSiriUsageDescription`"));

        h.add(new Hint("ios.appext.NAME.provisioningURL")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Cloud device builds only. URL of the provisioning profile for a generic app extension "
                        + "dropped into `ios/app_extensions/NAME/` (or a generated extension such as `CN1Widgets`), "
                        + "used when the extension folder doesn't bundle a `.mobileprovision` itself. The profile "
                        + "is installed on the build machine and added to the export options per bundle id. Used "
                        + "for both debug and release builds unless a qualified variant (below) is set. An "
                        + "extension is signed against its own App ID, so a device build with no profile for it -- "
                        + "by any of the three carriers -- is refused unless the app's own profile is a wildcard "
                        + "that covers the extension's bundle id."));

        h.add(new Hint("ios.application_exits")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .platform("ios")
                .external()
                .doc("true/false (defaults to false). Indicates whether the application should exit on home "
                        + "button press. The default is to exit, leaving the application running is only tested at "
                        + "the moment."));

        h.add(new Hint("ios.debug.archs")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Can be set to \"armv7\" to force iOS debug builds to be 32 bit. By default, debug builds "
                        + "are 64 bit only."));

        h.add(new Hint("ios.debug.distributionMethod")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Specifies distribution type for debug iOS builds only. This is used for enterprise or "
                        + "ad-hoc builds (using values \"enterprise\" and \"ad-hoc\" respectively)."));

        h.add(new Hint("ios.distributionMethod")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Specifies distribution type for debug iOS builds. This is used for enterprise or ad-hoc "
                        + "builds (using values \"enterprise\" and \"ad-hoc\" respectively)."));

        h.add(new Hint("ios.entitlementsInject")
                .group(HintGroup.IOS)
                .type(HintType.XML)
                .separator("")
                .platform("ios")
                .external()
                .doc("Content to inject into the iOS entitlements file. This should be in the Plist XML "
                        + "format. See "
                        + "https://developer.apple.com/documentation/bundleresources/entitlements?language=objc[Apple "
                        + "Entitlements Documentation]."));

        h.add(new Hint("ios.keychainAccessGroup")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Space-delimited list of keychain access groups that this app has access to as described "
                        + "in "
                        + "https://developer.apple.com/library/content/documentation/Security/Conceptual/keychainServConcepts/02concepts/concepts.html#//apple_ref/doc/uid/TP30000897-CH204-SW11[Apple's "
                        + "documentation]. These are added to the entitlements file with the key "
                        + "`keychain-access-groups`."));

        h.add(new Hint("ios.newPipeline")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .platform("ios")
                .external()
                .doc("Boolean true/false defaults to true. Allows toggling the OpenGL ES 2.0 drawing pipeline "
                        + "off to the older OGL ES 1.0 pipeline."));

        h.add(new Hint("ios.release.archs")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Can be set to \"arm64\" to only build iOS release builds for 64 bit. By default, release "
                        + "builds are both 32 and 64 bit."));

        h.add(new Hint("ios.release.distributionMethod")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("Specifies distribution type for release iOS builds only. This is used for enterprise or "
                        + "ad-hoc builds (using values \"enterprise\" and \"ad-hoc\" respectively)."));

        h.add(new Hint("ios.rpmalloc")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("`true`/`false` Use https://github.com/rampantpixels/rpmalloc[rpmalloc] instead of "
                        + "malloc/free for memory allocation in ParparVM. This will cause the deployment target to "
                        + "be changed to a minimum of iOS 8.0."));

        h.add(new Hint("ios.statusbar_hidden")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .platform("ios")
                .external()
                .doc("true/false defaults to false. Hides the iOS status bar if set to true."));

        h.add(new Hint("ios.testFlight")
                .group(HintGroup.IOS)
                .type(HintType.BOOLEAN)
                .platform("ios")
                .external()
                .doc("Boolean true/false defaults to false and works only for pro accounts. Enables the "
                        + "testflight support in the release binaries for easy beta testing. Notice that the IDE "
                        + "plugin has a \"Test Flight\" check box you *should* use under the iOS section."));

        h.add(new Hint("ios.xcode_version")
                .group(HintGroup.IOS)
                .type(HintType.STRING)
                .platform("ios")
                .external()
                .doc("The version of Xcode used on the server. Defaults to 4.5; accepts 5.0 as an option and "
                        + "nothing else."));

        h.add(new Hint("javascript.inject.afterHead")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .external()
                .doc("Content to be injected into the index.html file at the end of the `<head>` tag."));

        h.add(new Hint("javascript.inject.beforeHead")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .external()
                .doc("Content to be injected into the index.html file at the beginning of the `<head>` tag."));

        h.add(new Hint("javascript.minifying")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .platform("javascript")
                .external()
                .doc("true/false (defaults to `true`). By default the JavaScript code is minified to reduce "
                        + "file size. You may optionally disable minification by setting `javascript.minifying` to "
                        + "`false`."));

        h.add(new Hint("javascript.port")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .external()
                .doc("`parparvm` (default) or `teavm`. Selects the public JavaScript compiler for cloud "
                        + "builds. `teavm` retains the original builder as a compatibility fallback."));

        h.add(new Hint("javascript.sourceFilesCopied")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .platform("javascript")
                .external()
                .doc("true/false (defaults to `false`). Setting this flag to `true` will cause available java "
                        + "source files to be included in the resulting .zip and .war files. These may be used by "
                        + "Chrome during debugging."));

        h.add(new Hint("javascript.stopOnErrors")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .platform("javascript")
                .external()
                .doc("true/false (defaults to `true`). Causes a TeaVM JavaScript build to fail when the "
                        + "compiler reports warnings. Setting this to `false` may allow the fallback builder to "
                        + "complete, but can turn compiler diagnostics into runtime failures that are more "
                        + "difficult to debug."));

        h.add(new Hint("javascript.teavm.version")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .external()
                .doc("(Optional) The version of TeaVM to use for the build. *Use caution*, only use this "
                        + "property if you know what you're doing!"));

        h.add(new Hint("mac.desktop-vm")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .external()
                .doc("The JVM the should be bundled with Mac desktop build. Mac desktop builds only. Supported "
                        + "values: zuluFx8, zulu11, zuluFx11"));

        h.add(new Hint("macNative.entitlements.allowJit")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. `true` enables `com.apple.security.cs.allow-jit` for hardened "
                        + "runtime. ParparVM is AOT-compiled so this is `false` by default; flip when bundling a "
                        + "JIT-using cn1lib."));

        h.add(new Hint("macNative.entitlements.appSandbox")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. `true` enables `com.apple.security.app-sandbox`. Default is "
                        + "`true` for the `appStore` channel (Mac App Store requires the sandbox), `false` for "
                        + "`developerID`."));

        h.add(new Hint("macNative.entitlements.hardenedRuntime")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. `true` enables hardened runtime restrictions. Default is `true` "
                        + "for `developerID` (notarization requires it), `false` for `appStore`."));

        h.add(new Hint("macNative.entitlements.network.client")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. Toggles `com.apple.security.network.client`. Default `true`."));

        h.add(new Hint("macNative.entitlements.network.server")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. Toggles `com.apple.security.network.server`. Default `false`."));

        h.add(new Hint("macNative.provisioningProfile.appStore")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. Provisioning profile name for App Store distribution — used only "
                        + "when `macNative.signing.style=manual`."));

        h.add(new Hint("macNative.provisioningProfile.developerID")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .external()
                .doc("Mac Native builds only. Provisioning profile name for Developer ID distribution — used "
                        + "only when `macNative.signing.style=manual`."));

        h.add(new Hint("win.desktop-vm")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("windows")
                .external()
                .doc("The JVM that should be bundled in the Windows desktop build. Windows desktop builds "
                        + "only. Supported values: zulu8, zuluFx8, zulu8-32bit, zuluFx8-32bit, zulu11, zuluFx11, "
                        + "zulu11-32bit, zuluFx11-32bit"));

        h.add(new Hint("win.installDirName")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("windows")
                .external()
                .doc("Windows desktop builds only. Overrides the default installation folder name suggested by "
                        + "the installer (under `Program Files`). Defaults to the application's main class name for "
                        + "backward compatibility. Use this build hint to set a user-friendly installation folder "
                        + "name (for example, `win.installDirName=My Application`). The application ID used by "
                        + "Windows for upgrade detection is unaffected, so existing installations continue to "
                        + "upgrade."));

        h.add(new Hint("win.shortcutName")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("windows")
                .external()
                .doc("Windows desktop builds only. Overrides the name used for the Start Menu shortcut, the "
                        + "Desktop shortcut and (when `win.launchOnStart=true`) the autostart shortcut. Defaults to "
                        + "the application's main class name for backward compatibility. Use this build hint to set "
                        + "a user-friendly shortcut label (for example, `win.shortcutName=My Application`)."));

        h.add(new Hint("win.vm32bit")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .platform("windows")
                .external()
                .doc("true/false (defaults to false). Forces windows desktop builds to use the Win32 JVM "
                        + "instead of the 64 bit VM making them compatible with older Windows Machines. This is off "
                        + "by default at the moment because of a bug in JDK 8 update 112 that might cause this to "
                        + "fail for some cases"));

        h.add(new Hint("windows.extensions")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows")
                .external()
                .doc("Historical build hint for the discontinued UWP target. It's retained here only for "
                        + "legacy reference and isn't used by current supported build targets."));

        h.add(new Hint("xxx.minPlayServicesVersion")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .external()
                .doc("This is a special case build hint. You can use any prefix to the build hint and the "
                        + "convention is to use your cn1lib name. It's identical to "
                        + "`android.minPlayServicesVersion` with the exception that the \"highest version wins.\" "
                        + "That way if your cn1lib requires play services 9+ and uses: "
                        + "`myLib.minPlayServicesVersion=9.0.0` and another library has "
                        + "`otherLib.minPlayServicesVersion=10.0.0` then play services will be 10.0.0"));
    }
}
