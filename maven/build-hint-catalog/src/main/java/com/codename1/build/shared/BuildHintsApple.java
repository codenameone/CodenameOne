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
 * macOS Catalyst, tvOS and watchOS native-slice build hints.
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
final class BuildHintsApple {

    private BuildHintsApple() {
    }

    static void register(List<Hint> h) {
        // The native macOS port's canonical hints. macNative.* below is the
        // deprecated alias for the same build; macCatalyst.* names the legacy
        // Catalyst one. Registered here rather than left in the guide's table,
        // because that table is generated from this catalog now -- a hint absent
        // from here is a hint with no documentation at all.
        h.add(new Hint("macos.add_libs")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Frameworks to link in addition to the ones the build detects "
                        + "for itself, separated by a semicolon, a comma or a colon -- for example "
                        + "`Speech.framework;CoreMIDI.framework`. `ios.add_libs` is read when this is "
                        + "unset, so a project migrated from the Mac Catalyst build keeps linking what "
                        + "its native sources need."));

        h.add(new Hint("macos.appCategory")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `LSApplicationCategoryType` in the generated Info.plist. "
                        + "Default `public.app-category.utilities`. See "
                        + "https://developer.apple.com/documentation/bundleresources/information_property_list/lsapplicationcategorytype[Apple's "
                        + "category list]."));

        h.add(new Hint("macos.bundleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Used only when `macos.deriveBundleId=false`. Default: "
                        + "`<packageName>.mac`."));

        h.add(new Hint("macos.copyright")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `NSHumanReadableCopyright` in the Info.plist. Defaults to "
                        + "`Copyright (c) <year> <vendor>`."));

        h.add(new Hint("macos.deriveBundleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `false` (default) gives the app its own bundle identifier, "
                        + "`<packageName>.mac`, because a macOS app and an iOS app are separate "
                        + "products in App Store Connect. `true` reuses the iOS identifier. On the "
                        + "legacy Mac Catalyst target this maps instead to Xcode's "
                        + "`DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER`, which appends "
                        + "`.maccatalyst`."));

        h.add(new Hint("macos.distribution")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Every `macos.*` hint below is also accepted spelled "
                        + "`macNative.*`, which is what the legacy Mac Catalyst target reads, so an "
                        + "existing Catalyst project keeps building unchanged. `developerID` (default), "
                        + "`appStore`, or `both`. Selects the signing certificate, the entitlements and "
                        + "the default packaging. `both` is genuinely two builds: the channels differ "
                        + "in the certificate and in the entitlements the signature carries -- the App "
                        + "Store one has to be sandboxed -- so one binary can't be relabelled into the "
                        + "other channel afterwards. It produces `<App>-appstore.app` and "
                        + "`<App>-developerid.app`, each with its own container."));

        h.add(new Hint("macos.entitlements.allowJit")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `true` enables `com.apple.security.cs.allow-jit` for hardened "
                        + "runtime. ParparVM is AOT-compiled so this is `false` by default; flip when "
                        + "bundling a JIT-using cn1lib."));

        h.add(new Hint("macos.entitlements.appSandbox")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `true` enables `com.apple.security.app-sandbox`. Default is "
                        + "`true` for the `appStore` channel, `false` for `developerID`. The App Store "
                        + "channel is always sandboxed whatever this says -- the Mac App Store requires "
                        + "it, and a package built without the sandbox gets rejected at submission "
                        + "rather than at build time. The refusal is reported in the build log."));

        h.add(new Hint("macos.entitlements.extra")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.XML)
                .platform("mac")
                .doc("macOS builds. Free-form XML inserted verbatim inside the `<dict>...</dict>` of "
                        + "the generated entitlements plist. Use for entitlements Codename One doesn't "
                        + "expose individually."));

        h.add(new Hint("macos.entitlements.files.downloads")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `true` adds `com.apple.security.files.downloads.read-write`, "
                        + "which is access to the Downloads folder without a panel. Default `false`, "
                        + "and separate from `macos.entitlements.files.userSelected` above because it's "
                        + "a wider grant than picking a file."));

        h.add(new Hint("macos.entitlements.files.userSelected")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `readwrite` (default), `readonly`, or `none`. Sets the "
                        + "matching `com.apple.security.files.user-selected.*` entitlement -- the files "
                        + "the user picks in an open or save panel, and nothing else."));

        h.add(new Hint("macos.entitlements.hardenedRuntime")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `true` writes `com.apple.security.cs.allow-jit` and "
                        + "`com.apple.security.cs.allow-unsigned-executable-memory` into the "
                        + "entitlements as explicit denials; `false` leaves them out. It doesn't switch "
                        + "the hardened runtime on or off -- that's `macos.hardenedRuntime` above. "
                        + "Default is `true` for `developerID`, `false` for `appStore`."));

        h.add(new Hint("macos.entitlements.network.client")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Toggles `com.apple.security.network.client`. Default `true`."));

        h.add(new Hint("macos.entitlements.network.server")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Toggles `com.apple.security.network.server`. Default `false`."));

        h.add(new Hint("macos.fixedWindowSize")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Opt-in. Format `<width>x<height>` -- for example `1024x685`. "
                        + "When set, the window's minimum and maximum size are pinned to the requested "
                        + "size so every launch produces a byte-identical window. Default unset, in "
                        + "which case the window is resizable. The CI screenshot pipeline turns this on "
                        + "to keep the strict-pixel golden comparison stable; production apps should "
                        + "leave it off."));

        h.add(new Hint("macos.hardenedRuntime")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Sets Xcode's `ENABLE_HARDENED_RUNTIME`. Default `true`, "
                        + "because notarization requires it. This is the build setting; the entitlement "
                        + "hint below is a different thing despite the similar name."));

        h.add(new Hint("macos.minDeploymentTarget")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.VERSION)
                .platform("mac")
                .doc("macOS builds. Minimum macOS version (`MACOSX_DEPLOYMENT_TARGET`). Default "
                        + "`11.0` on the native macOS build, which is the floor for a universal Apple "
                        + "silicon binary. The legacy Mac Catalyst target defaults to `10.15`."));

        h.add(new Hint("macos.packaging")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `app`, `dmg`, `pkg` or `both`. Unset, each channel takes its "
                        + "own default -- `pkg` for `appStore`, because productbuild's output is what "
                        + "you upload, and `dmg` for `developerID`. Set explicitly, the value applies "
                        + "to every channel. A cloud build always ships a file, so `app` there means "
                        + "the bundle zipped with `ditto` rather than the raw `.app` directory."));

        h.add(new Hint("macos.plistInject")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.XML)
                .platform("mac")
                .doc("macOS builds. Raw XML members added to the generated `Info.plist`, the same "
                        + "form `ios.plistInject` takes -- for example "
                        + "`<key>NSAppTransportSecurity</key><dict/>`. A key that the build also "
                        + "generates is replaced by the injected one, and the build log names it. "
                        + "`ios.plistInject` is read when this is unset, so a project migrated from the "
                        + "Mac Catalyst build keeps its injections."));

        h.add(new Hint("macos.provisioningProfile.appStore")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Provisioning profile name for App Store distribution -- used "
                        + "only when `macNative.signing.style=manual`."));

        h.add(new Hint("macos.provisioningProfile.developerID")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Provisioning profile name for Developer ID distribution -- used "
                        + "only when `macNative.signing.style=manual`."));

        h.add(new Hint("macos.signing.style")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. `manual` (default) signs with the certificate identity hints "
                        + "below, verbatim. `automatic` lets Xcode resolve the certificate from the "
                        + "team and provisioning profile instead. Manual is the default because a build "
                        + "server has an installed certificate and no Xcode account session, and "
                        + "automatic signing there stops to ask you to sign in; use automatic when "
                        + "building on your own machine."));

        h.add(new Hint("macos.signingIdentity.appStore")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Signing certificate identity for the App Store channel. "
                        + "Default `Apple Distribution`. Set it to `none` to build unsigned -- an empty "
                        + "value can't say that, because an empty hint reads as unset and takes the "
                        + "default."));

        h.add(new Hint("macos.signingIdentity.developerID")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Signing certificate identity for the Developer ID channel. "
                        + "Default `Developer ID Application`. Set it to `none` to build unsigned."));

        h.add(new Hint("macos.signingIdentity.installer")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. The certificate `productbuild` signs a `.pkg` with -- `3rd "
                        + "Party Mac Developer Installer` for the App Store, `Developer ID Installer` "
                        + "for direct distribution. This is a different certificate from "
                        + "`macos.signingIdentity.appStore`, which signs the application, so it has a "
                        + "hint of its own rather than being derived from that one. Unset, the package "
                        + "is built unsigned, which the App Store upload refuses."));

        h.add(new Hint("macos.signingIdentity.installer.appStore")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. The installer certificate for the App Store channel "
                        + "specifically, when `macos.distribution=both` produces a package on each "
                        + "side. They're different certificates, so one shared value signs both "
                        + "packages with the same one and leaves one of them unusable. Unset, the "
                        + "shared `macos.signingIdentity.installer` applies."));

        h.add(new Hint("macos.signingIdentity.installer.developerID")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. The installer certificate for the Developer ID channel "
                        + "specifically. Unset, the shared `macos.signingIdentity.installer` applies."));

        h.add(new Hint("macos.teamId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("macOS builds. Apple Developer Team ID (alphanumeric). Falls back to "
                        + "`ios.release.teamId` -> `ios.teamId` -> `ios.debug.teamId` since most apps "
                        + "share a single Apple Developer Team for iOS and Mac."));
        h.add(new Hint("macNative.appCategory")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("public.app-category.utilities")
                .platform("mac")
                .doc("Mac Native builds only. `LSApplicationCategoryType` in the generated Info.plist. Default "
                        + "`public.app-category.utilities`. See "
                        + "https://developer.apple.com/documentation/bundleresources/information_property_list/lsapplicationcategorytype[Apple's "
                        + "category list]."));

        h.add(new Hint("macNative.bundleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("Mac Native builds only. Used only when `macNative.deriveBundleId=false`. Default: "
                        + "`<packageName>.mac`."));

        h.add(new Hint("macNative.copyright")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("Mac Native builds only. `NSHumanReadableCopyright` in the Info.plist. Defaults to "
                        + "`Copyright (c) <year> <vendor>`."));

        h.add(new Hint("macNative.deriveBundleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("mac")
                .doc("Mac Native builds only. `true` (default) maps to Xcode's "
                        + "`DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER=YES` (Xcode appends `.maccatalyst` to the "
                        + "iOS bundle ID). Set to `false` to take the bundle ID verbatim from `macNative.bundleId`."));

        h.add(new Hint("macNative.distribution")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("appStore")
                .platform("mac")
                .doc("Mac Native builds only. `appStore` (default), `developerID`, or `both`. Selects which "
                        + "entitlements + ExportOptions plist + signing certificate to emit. `both` emits parallel "
                        + "`*-AppStore.entitlements` / `*-DeveloperID.entitlements` and matching "
                        + "`ExportOptions-*-Mac.plist` files so a single project can be archived to either channel."));

        h.add(new Hint("macNative.enabled")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("mac"));

        h.add(new Hint("macNative.entitlements.device.camera")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .platform("mac")
                .doc("Sandboxed Mac Native builds only. Toggles "
                        + "`com.apple.security.device.camera`. Defaults to whether the app sets "
                        + "`ios.NSCameraUsageDescription`, so an app that asks for the camera gets "
                        + "the entitlement without naming it twice."));

        h.add(new Hint("macNative.entitlements.device.microphone")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .platform("mac")
                .doc("Sandboxed Mac Native builds only. Toggles "
                        + "`com.apple.security.device.microphone`. Defaults to whether the app sets "
                        + "`ios.NSMicrophoneUsageDescription`."));

        h.add(new Hint("macNative.entitlements.personalInformation.calendars")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .platform("mac")
                .doc("Sandboxed Mac Native builds only. Toggles "
                        + "`com.apple.security.personal-information.calendars`, which gates all "
                        + "EventKit access. Defaults to whether the app sets any calendar or "
                        + "reminder usage description, including the write-only and reminders-only "
                        + "ones."));

        h.add(new Hint("macNative.entitlements.extra")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("Mac Native builds only. Free-form XML inserted verbatim inside the `<dict>...</dict>` of "
                        + "the generated entitlements plist. Use for entitlements Codename One doesn't expose "
                        + "individually."));

        h.add(new Hint("macNative.entitlements.files.userSelected")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("readwrite")
                .platform("mac")
                .doc("Mac Native builds only. `readwrite` (default), `readonly`, or `none`. Sets the matching "
                        + "`com.apple.security.files.user-selected.*` entitlement."));

        h.add(new Hint("macNative.fixedWindowSize")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("Mac Native builds only. Opt-in. Format `<width>x<height>` -- for example `1024x685`. When "
                        + "set, the Catalyst window's `UISceneSession.sizeRestrictions` minimum and maximum are "
                        + "pinned to the requested size so every launch produces a byte-identical window. Default "
                        + "unset, in which case the window is resizable. The CI screenshot pipeline turns this on "
                        + "to keep the strict-pixel golden comparison stable; production apps should leave it off."));

        h.add(new Hint("macNative.iosMinDeploymentTarget")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.VERSION)
                .def("13.1")
                .platform("mac")
                .doc("Mac Native builds only. iOS deployment-target floor for the Catalyst slice "
                        + "(`IPHONEOS_DEPLOYMENT_TARGET`). Default `13.1`. The plugin coerces the iOS slice's "
                        + "minimum upward when set."));

        h.add(new Hint("macNative.minDeploymentTarget")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.VERSION)
                .def("10.15")
                .platform("mac")
                .doc("Mac Native builds only. Minimum macOS version (`MACOSX_DEPLOYMENT_TARGET`). Default "
                        + "`10.15` -- earlier versions don't support Mac Catalyst."));

        h.add(new Hint("macNative.multiWindow")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("mac")
                .doc("Mac Native builds only. Declares that the app uses `com.codename1.ui.Window`, which "
                        + "needs multiple UIScenes and exists only on the Mac Catalyst slice. Writes "
                        + "`UIApplicationSupportsMultipleScenes` and a scene configuration into the Mac "
                        + "slice's own `Info.plist`; `getWindowManager()` reads that key back out of the "
                        + "bundle, so without it windows are reported unsupported and constructing one "
                        + "throws. Off by default: multi-window support relayouts the app into a "
                        + "resizable window, a change an app that never asked for windows has no "
                        + "reason to take on."));

        h.add(new Hint("macNative.notarize")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("mac"));

        h.add(new Hint("macNative.notarize.appleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac"));

        h.add(new Hint("macNative.notarize.keychainProfile")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac"));

        h.add(new Hint("macNative.notarize.password")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.SECRET)
                .platform("mac"));

        h.add(new Hint("macNative.notarize.teamId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac"));

        h.add(new Hint("macNative.signing.style")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("automatic")
                .platform("mac")
                .doc("Mac Native builds only. `automatic` (default) lets Xcode pick the signing certificate; "
                        + "`manual` forces the certificate identity hints below to be respected verbatim."));

        h.add(new Hint("macNative.signingIdentity.appStore")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("Apple Distribution")
                .platform("mac")
                .doc("Mac Native builds only. Signing certificate identity for the App Store channel. Default "
                        + "`Apple Distribution`."));

        h.add(new Hint("macNative.signingIdentity.developerID")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("Developer ID Application")
                .platform("mac")
                .doc("Mac Native builds only. Signing certificate identity for the Developer ID channel. "
                        + "Default `Developer ID Application`."));

        h.add(new Hint("macNative.teamId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .doc("Mac Native builds only. Apple Developer Team ID (alphanumeric). Falls back to "
                        + "`ios.release.teamId` -> `ios.teamId` -> `ios.debug.teamId` since most apps share a single "
                        + "Apple Developer Team for iOS and Mac."));

        h.add(new Hint("tvNative.bundleId")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .doc("Bundle identifier of the tvOS app. Defaults to `<packageName>.tvos`."));

        h.add(new Hint("tvNative.displayName")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .doc("The tvOS app name shown on Apple TV. Defaults to the app's display name."));

        h.add(new Hint("tvNative.enabled")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("tv")
                .doc("true/false (defaults to false). Adds an Apple TV (tvOS) application target to the iOS "
                        + "build. The tvOS app is a separate `appletvos` target built from the same Java/Kotlin "
                        + "sources through ParparVM (UIKit + Metal; tvOS has no OpenGL ES). Enabling it doesn't "
                        + "change the iOS app -- in particular it doesn't override the iOS app's `ios.metal` "
                        + "setting. Also turned on implicitly by `codename1.tvMain`."));

        h.add(new Hint("tvNative.mainClass")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv"));

        h.add(new Hint("tvNative.minDeploymentTarget")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.VERSION)
                .def("13.0")
                .platform("tv")
                .doc("`TVOS_DEPLOYMENT_TARGET` for the tvOS target. Defaults to `13.0`."));

        h.add(new Hint("tvNative.teamId")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .doc("Apple Developer Team ID used to sign the tvOS target. Falls back to the iOS team id "
                        + "(`ios.release.teamId` / `ios.teamId` / `ios.debug.teamId`)."));

        h.add(new Hint("watchNative.enabled")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("watch"));

        h.add(new Hint("watchNative.health")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.STRING)
                .platform("watch"));

        h.add(new Hint("watchNative.health.workoutProcessing")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("watch"));

        h.add(new Hint("watchNative.surfaces.deploymentTarget")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.STRING)
                .def("10.0")
                .platform("watch")
                .doc("Deployment target of the WidgetKit extension that carries the watch complication. This "
                        + "is the WATCH APP's floor rather than the extension's own: WidgetKit reaches back to "
                        + "watchOS 9, but the extension is embedded in the watch app, so advertising a version "
                        + "the app itself refuses to install on claims support the user never gets."));

        h.add(new Hint("watchNative.mainClass")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.STRING)
                .platform("watch"));
    }
}
