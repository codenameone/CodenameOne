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
package com.codename1.annotations.buildhints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Native macOS build hints, checked by the compiler.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the build server's own default applies. The `default` clause below each
/// attribute names a constant that says nothing -- see [HintUnset] -- and this
/// package deliberately does not record what the server would do instead,
/// because that is the server's to change.
///
/// The platform is stated once on the annotation, not on every attribute. An
/// attribute repeats it only to disagree with it.
///
/// These are the `macos.*` hints, which the native macOS build reads. Every one
/// of them is also accepted spelled `macNative.*`, the name the legacy Mac
/// Catalyst target uses, so a project moving between the two keeps building; the
/// canonical spelling is the one here.
@Hint(platform = "mac")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Mac {

    /// macOS builds. Frameworks to link in addition to the ones the build detects
    /// for itself, separated by a semicolon, a comma or a colon -- for example
    /// `Speech.framework;CoreMIDI.framework`. `ios.add_libs` is read when this is
    /// unset, so a project migrated from the Mac Catalyst build keeps linking
    /// what its native sources need.
    @Hint(appendable = true, name = "macos.add_libs", separator = ";")
    String[] addLibs() default {};

    /// macOS builds. `LSApplicationCategoryType` in the generated Info.plist.
    /// Default `public.app-category.utilities`. See
    /// https://developer.apple.com/documentation/bundleresources/information_property_list/lsapplicationcategorytype[Apple's
    /// category list].
    String appCategory() default "";

    /// macOS builds. The architectures to compile, as an `ARCHS` value. Default
    /// `arm64 x86_64`, which is what a Mac application is expected to be: a
    /// single-architecture build is the kind of thing nobody notices until an
    /// Intel user reports it.
    String arch() default "arm64 x86_64";

    /// macOS builds. Used only when `macos.deriveBundleId=false`. Default:
    /// `<packageName>.mac`.
    String bundleId() default "";

    /// macOS builds. `CFBundleVersion` in the Info.plist. `ios.bundleVersion` is
    /// read when this is unset, and the project's version when neither is set.
    String bundleVersion() default "";

    /// macOS builds. The Xcode configuration to archive, as passed to
    /// `xcodebuild -configuration`. Default `Release`.
    String configuration() default "Release";

    /// macOS builds. `NSHumanReadableCopyright` in the Info.plist. Defaults to
    /// `Copyright (c) <year> <vendor>`.
    String copyright() default "";

    /// macOS builds. `true` compiles AES-GCM support into the bundled crypto
    /// library. `ios.crypto.gcm` is read when this is unset.
    @Hint(name = "macos.crypto.gcm")
    Toggle cryptoGcm() default Toggle.DEFAULT;

    /// macOS builds. `false` (default) gives the app its own bundle identifier,
    /// `<packageName>.mac`, because a macOS app and an iOS app are separate
    /// products in App Store Connect. `true` reuses the iOS identifier. On the
    /// legacy Mac Catalyst target this maps instead to Xcode's
    /// `DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER`, which appends
    /// `.maccatalyst`.
    Toggle deriveBundleId() default Toggle.DEFAULT;

    /// macOS builds. Every `macos.*` hint below is also accepted spelled
    /// `macNative.*`, which is what the legacy Mac Catalyst target reads, so an
    /// existing Catalyst project keeps building unchanged. `developerID`
    /// (default), `appStore`, or `both`. Selects the signing certificate, the
    /// entitlements and the default packaging. `both` is genuinely two builds:
    /// the channels differ in the certificate and in the entitlements the
    /// signature carries -- the App Store one has to be sandboxed -- so one
    /// binary can't be relabelled into the other channel afterwards. It produces
    /// `<App>-appstore.app` and `<App>-developerid.app`, each with its own
    /// container.
    String distribution() default "";

    /// macOS builds. `true` enables `com.apple.security.cs.allow-jit` for
    /// hardened runtime. ParparVM is AOT-compiled so this is `false` by default;
    /// flip when bundling a JIT-using cn1lib.
    @Hint(name = "macos.entitlements.allowJit")
    Toggle entitlementsAllowJit() default Toggle.DEFAULT;

    /// macOS builds. `true` enables `com.apple.security.app-sandbox`. Default is
    /// `true` for the `appStore` channel, `false` for `developerID`. The App
    /// Store channel is always sandboxed whatever this says -- the Mac App Store
    /// requires it, and a package built without the sandbox gets rejected at
    /// submission rather than at build time. The refusal is reported in the build
    /// log.
    @Hint(name = "macos.entitlements.appSandbox")
    Toggle entitlementsAppSandbox() default Toggle.DEFAULT;

    /// macOS builds. Free-form XML inserted verbatim inside the `<dict>...</dict>`
    /// of the generated entitlements plist. Use for entitlements Codename One
    /// doesn't expose individually.
    @Hint(name = "macos.entitlements.extra")
    String entitlementsExtra() default "";

    /// macOS builds. `true` adds `com.apple.security.files.downloads.read-write`,
    /// which is access to the Downloads folder without a panel. Default `false`,
    /// and separate from `macos.entitlements.files.userSelected` above because
    /// it's a wider grant than picking a file.
    @Hint(name = "macos.entitlements.files.downloads")
    Toggle entitlementsFilesDownloads() default Toggle.DEFAULT;

    /// macOS builds. `readwrite` (default), `readonly`, or `none`. Sets the
    /// matching `com.apple.security.files.user-selected.*` entitlement -- the
    /// files the user picks in an open or save panel, and nothing else.
    @Hint(name = "macos.entitlements.files.userSelected")
    FileAccess entitlementsFilesUserSelected() default FileAccess.DEFAULT;

    /// macOS builds. `true` writes `com.apple.security.cs.allow-jit` and
    /// `com.apple.security.cs.allow-unsigned-executable-memory` into the
    /// entitlements as explicit denials; `false` leaves them out. It doesn't
    /// switch the hardened runtime on or off -- that's `macos.hardenedRuntime`
    /// above. Default is `true` for `developerID`, `false` for `appStore`.
    @Hint(name = "macos.entitlements.hardenedRuntime")
    Toggle entitlementsHardenedRuntime() default Toggle.DEFAULT;

    /// macOS builds. Toggles `com.apple.security.network.client`. Default `true`.
    @Hint(name = "macos.entitlements.network.client")
    Toggle entitlementsNetworkClient() default Toggle.DEFAULT;

    /// macOS builds. Toggles `com.apple.security.network.server`. Default
    /// `false`.
    @Hint(name = "macos.entitlements.network.server")
    Toggle entitlementsNetworkServer() default Toggle.DEFAULT;

    /// macOS builds. Opt-in. Format `<width>x<height>` -- for example `1024x685`.
    /// When set, the window's minimum and maximum size are pinned to the
    /// requested size so every launch produces a byte-identical window. Default
    /// unset, in which case the window is resizable. The CI screenshot pipeline
    /// turns this on to keep the strict-pixel golden comparison stable;
    /// production apps should leave it off.
    String fixedWindowSize() default "";

    /// macOS builds. Sets Xcode's `ENABLE_HARDENED_RUNTIME`. Default `true`,
    /// because notarization requires it. This is the build setting; the
    /// entitlement hint below is a different thing despite the similar name.
    Toggle hardenedRuntime() default Toggle.DEFAULT;

    /// macOS builds. `true` grants the hardened-runtime exception for loading
    /// unsigned libraries. A Codename One application doesn't load code that
    /// way, but a cn1lib shipping a dylib needs this, or the load is refused at
    /// runtime with nothing in the application's own logs.
    Toggle loadsExternalCode() default Toggle.DEFAULT;

    /// macOS builds. Minimum macOS version (`MACOSX_DEPLOYMENT_TARGET`). Default
    /// `11.0` on the native macOS build, which is the floor for a universal Apple
    /// silicon binary. The legacy Mac Catalyst target defaults to `10.15`.
    String minDeploymentTarget() default "";

    /// macOS builds. `app`, `dmg`, `pkg` or `both`. Unset, each channel takes its
    /// own default -- `pkg` for `appStore`, because productbuild's output is what
    /// you upload, and `dmg` for `developerID`. Set explicitly, the value applies
    /// to every channel. A cloud build always ships a file, so `app` there means
    /// the bundle zipped with `ditto` rather than the raw `.app` directory.
    String packaging() default "";

    /// macOS builds. Raw XML members added to the generated `Info.plist`, the
    /// same form `ios.plistInject` takes -- for example
    /// `<key>NSAppTransportSecurity</key><dict/>`. A key that the build also
    /// generates is replaced by the injected one, and the build log names it.
    /// `ios.plistInject` is read when this is unset, so a project migrated from
    /// the Mac Catalyst build keeps its injections.
    String plistInject() default "";

    /// macOS builds. Provisioning profile name for App Store distribution -- used
    /// only when `macNative.signing.style=manual`.
    @Hint(name = "macos.provisioningProfile.appStore")
    String provisioningProfileAppStore() default "";

    /// macOS builds. Provisioning profile name for Developer ID distribution --
    /// used only when `macNative.signing.style=manual`.
    @Hint(name = "macos.provisioningProfile.developerID")
    String provisioningProfileDeveloperID() default "";

    /// macOS builds. `manual` (default) signs with the certificate identity hints
    /// below, verbatim. `automatic` lets Xcode resolve the certificate from the
    /// team and provisioning profile instead. Manual is the default because a
    /// build server has an installed certificate and no Xcode account session,
    /// and automatic signing there stops to ask you to sign in; use automatic
    /// when building on your own machine.
    @Hint(name = "macos.signing.style")
    String signingStyle() default "";

    /// macOS builds. Signing certificate identity for the App Store channel.
    /// Default `Apple Distribution`. Set it to `none` to build unsigned -- an
    /// empty value can't say that, because an empty hint reads as unset and takes
    /// the default.
    @Hint(name = "macos.signingIdentity.appStore")
    String signingIdentityAppStore() default "";

    /// macOS builds. Signing certificate identity for the Developer ID channel.
    /// Default `Developer ID Application`. Set it to `none` to build unsigned.
    @Hint(name = "macos.signingIdentity.developerID")
    String signingIdentityDeveloperID() default "";

    /// macOS builds. The certificate `productbuild` signs a `.pkg` with -- `3rd
    /// Party Mac Developer Installer` for the App Store, `Developer ID Installer`
    /// for direct distribution. This is a different certificate from
    /// `macos.signingIdentity.appStore`, which signs the application, so it has a
    /// hint of its own rather than being derived from that one. Unset, the
    /// package is built unsigned, which the App Store upload refuses.
    @Hint(name = "macos.signingIdentity.installer")
    String signingIdentityInstaller() default "";

    /// macOS builds. The installer certificate for the App Store channel
    /// specifically, when `macos.distribution=both` produces a package on each
    /// side. They're different certificates, so one shared value signs both
    /// packages with the same one and leaves one of them unusable. Unset, the
    /// shared `macos.signingIdentity.installer` applies.
    @Hint(name = "macos.signingIdentity.installer.appStore")
    String signingIdentityInstallerAppStore() default "";

    /// macOS builds. The installer certificate for the Developer ID channel
    /// specifically. Unset, the shared `macos.signingIdentity.installer` applies.
    @Hint(name = "macos.signingIdentity.installer.developerID")
    String signingIdentityInstallerDeveloperID() default "";

    /// macOS builds. `true` stops after generating the Xcode project, which is
    /// what the `mac-source` target delivers. Set by that target rather than by
    /// hand.
    Toggle sourceOnly() default Toggle.DEFAULT;

    /// macOS builds. Apple Developer Team ID (alphanumeric). Falls back to
    /// `ios.release.teamId` -> `ios.teamId` -> `ios.debug.teamId` since most apps
    /// share a single Apple Developer Team for iOS and Mac.
    String teamId() default "";

    /// macOS builds. Custom URL schemes to register, comma separated.
    /// `ios.urlSchemes` and then `ios.urlScheme` are read when this is unset, so
    /// a project migrated from the Mac Catalyst build keeps its deep links.
    String urlSchemes() default "";
}
