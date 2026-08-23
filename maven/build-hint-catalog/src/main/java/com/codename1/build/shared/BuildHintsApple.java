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
        h.add(new Hint("macNative.appCategory")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("public.app-category.utilities")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. `LSApplicationCategoryType` in the generated Info.plist. Default "
                        + "`public.app-category.utilities`. See "
                        + "https://developer.apple.com/documentation/bundleresources/information_property_list/lsapplicationcategorytype[Apple's "
                        + "category list]."));

        h.add(new Hint("macNative.bundleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Used only when `macNative.deriveBundleId=false`. Default: "
                        + "`<packageName>.mac`."));

        h.add(new Hint("macNative.copyright")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. `NSHumanReadableCopyright` in the Info.plist. Defaults to "
                        + "`Copyright (c) <year> <vendor>`."));

        h.add(new Hint("macNative.deriveBundleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. `true` (default) maps to Xcode's "
                        + "`DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER=YES` (Xcode appends `.maccatalyst` to the "
                        + "iOS bundle ID). Set to `false` to take the bundle ID verbatim from `macNative.bundleId`."));

        h.add(new Hint("macNative.distribution")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("appStore")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. `appStore` (default), `developerID`, or `both`. Selects which "
                        + "entitlements + ExportOptions plist + signing certificate to emit. `both` emits parallel "
                        + "`*-AppStore.entitlements` / `*-DeveloperID.entitlements` and matching "
                        + "`ExportOptions-*-Mac.plist` files so a single project can be archived to either channel."));

        h.add(new Hint("macNative.enabled")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("mac")
                .consumedBy("CN1BuildMojo", "IPhoneBuilder", "MacNativeBuilder"));

        h.add(new Hint("macNative.entitlements.extra")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Free-form XML inserted verbatim inside the `<dict>…</dict>` of "
                        + "the generated entitlements plist. Use for entitlements Codename One doesn't expose "
                        + "individually."));

        h.add(new Hint("macNative.entitlements.files.userSelected")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("readwrite")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. `readwrite` (default), `readonly`, or `none`. Sets the matching "
                        + "`com.apple.security.files.user-selected.*` entitlement."));

        h.add(new Hint("macNative.fixedWindowSize")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Opt-in. Format `<width>x<height>` — for example `1024x685`. When "
                        + "set, the Catalyst window's `UISceneSession.sizeRestrictions` minimum and maximum are "
                        + "pinned to the requested size so every launch produces a byte-identical window. Default "
                        + "unset, in which case the window is resizable. The CI screenshot pipeline turns this on "
                        + "to keep the strict-pixel golden comparison stable; production apps should leave it off."));

        h.add(new Hint("macNative.iosMinDeploymentTarget")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.VERSION)
                .def("13.1")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. iOS deployment-target floor for the Catalyst slice "
                        + "(`IPHONEOS_DEPLOYMENT_TARGET`). Default `13.1`. The plugin coerces the iOS slice's "
                        + "minimum upward when set."));

        h.add(new Hint("macNative.minDeploymentTarget")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.VERSION)
                .def("10.15")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Minimum macOS version (`MACOSX_DEPLOYMENT_TARGET`). Default "
                        + "`10.15` — earlier versions don't support Mac Catalyst."));

        h.add(new Hint("macNative.notarize")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("mac")
                .consumedBy("MacNativeBuilder"));

        h.add(new Hint("macNative.notarize.appleId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder"));

        h.add(new Hint("macNative.notarize.keychainProfile")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder"));

        h.add(new Hint("macNative.notarize.password")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.SECRET)
                .platform("mac")
                .consumedBy("MacNativeBuilder"));

        h.add(new Hint("macNative.notarize.teamId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder"));

        h.add(new Hint("macNative.signing.style")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("automatic")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. `automatic` (default) lets Xcode pick the signing certificate; "
                        + "`manual` forces the certificate identity hints below to be respected verbatim."));

        h.add(new Hint("macNative.signingIdentity.appStore")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("Apple Distribution")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Signing certificate identity for the App Store channel. Default "
                        + "`Apple Distribution`."));

        h.add(new Hint("macNative.signingIdentity.developerID")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .def("Developer ID Application")
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Signing certificate identity for the Developer ID channel. "
                        + "Default `Developer ID Application`."));

        h.add(new Hint("macNative.teamId")
                .group(HintGroup.MAC_NATIVE)
                .type(HintType.STRING)
                .platform("mac")
                .consumedBy("MacNativeBuilder")
                .doc("Mac Native builds only. Apple Developer Team ID (alphanumeric). Falls back to "
                        + "`ios.release.teamId` → `ios.teamId` → `ios.debug.teamId` since most apps share a single "
                        + "Apple Developer Team for iOS and Mac."));

        h.add(new Hint("tvNative.bundleId")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .consumedBy("TvNativeBuilder")
                .doc("Bundle identifier of the tvOS app. Defaults to `<packageName>.tvos`."));

        h.add(new Hint("tvNative.displayName")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .consumedBy("TvNativeBuilder")
                .doc("The tvOS app name shown on Apple TV. Defaults to the app's display name."));

        h.add(new Hint("tvNative.enabled")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("tv")
                .consumedBy("IPhoneBuilder", "TvNativeBuilder")
                .doc("true/false (defaults to false). Adds an Apple TV (tvOS) application target to the iOS "
                        + "build. The tvOS app is a separate `appletvos` target built from the same Java/Kotlin "
                        + "sources through ParparVM (UIKit + Metal; tvOS has no OpenGL ES). Enabling it doesn't "
                        + "change the iOS app -- in particular it doesn't override the iOS app's `ios.metal` "
                        + "setting. Also turned on implicitly by `codename1.tvMain`."));

        h.add(new Hint("tvNative.mainClass")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .consumedBy("IPhoneBuilder", "TvNativeBuilder"));

        h.add(new Hint("tvNative.minDeploymentTarget")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.VERSION)
                .def("13.0")
                .platform("tv")
                .consumedBy("TvNativeBuilder")
                .doc("`TVOS_DEPLOYMENT_TARGET` for the tvOS target. Defaults to `13.0`."));

        h.add(new Hint("tvNative.teamId")
                .group(HintGroup.TV_NATIVE)
                .type(HintType.STRING)
                .platform("tv")
                .consumedBy("TvNativeBuilder")
                .doc("Apple Developer Team ID used to sign the tvOS target. Falls back to the iOS team id "
                        + "(`ios.release.teamId` / `ios.teamId` / `ios.debug.teamId`)."));

        h.add(new Hint("watchNative.enabled")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("watch")
                .consumedBy("IPhoneBuilder"));

        h.add(new Hint("watchNative.health")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.STRING)
                .platform("watch")
                .consumedBy("WatchNativeBuilder"));

        h.add(new Hint("watchNative.health.workoutProcessing")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("watch")
                .consumedBy("WatchNativeBuilder"));

        h.add(new Hint("watchNative.mainClass")
                .group(HintGroup.WATCH_NATIVE)
                .type(HintType.STRING)
                .platform("watch")
                .consumedBy("IPhoneBuilder"));
    }
}
