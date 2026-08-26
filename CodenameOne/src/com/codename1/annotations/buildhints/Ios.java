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

/// iOS build hints, checked by the compiler.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the builder's own default applies. Each attribute's `@Hint(def)` records what
/// that default is; the `default` clause below it is a neutral placeholder with
/// no meaning at runtime.
///
/// The platform and the builders that read these hints are stated once on the
/// annotation, not on every attribute. An attribute repeats one only to
/// disagree with it.
@Hint(platform = "ios",
        consumedBy = {"IPhoneBuilder"})
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Ios {

    /// A semicolon separated list of libraries that should be linked to the app to
    /// build it
    @Hint(appendable = true,
            name = "ios.add_libs",
            separator = ";")
    String[] addLibs() default {};

    /// Comma separated list of url schemes that `canExecute` will respect on iOS.
    /// If the url scheme isn't mentioned here `canExecute` will return false
    /// starting with iOS 9. Notice that this collides with `ios.plistInject` when
    /// used with the `<key>LSApplicationQueriesSchemes</key>...` value so you
    /// should use one or the other. For example, to enable `canExecute` for a url
    /// like `myurl://xys` you can use: `myurl,myotherurl`
    @Hint(appendable = true,
            separator = ",")
    String[] applicationQueriesSchemes() default {};

    /// Objective-C code that can be injected into the iOS app delegate at the top
    /// of the body of the didFinishLaunchingWithOptions callback method
    @Hint(kind = HintKind.TEXT_BLOCK)
    String beforeFinishLaunching() default "";

    /// Indicates the version number of the bundle, this is useful if you want to
    /// create a minor version number change for the beta testing support
    @Hint(kind = HintKind.VERSION,
            consumedBy = {"IPhoneBuilder", "WatchNativeBuilder"})
    String bundleVersion() default "";

    /// Which native dependency manager to use: auto picks one from whichever of
    /// ios.pods and ios.spm.packages is set, and cocoapods, spm or both require
    /// the matching hint to be set. An unrecognized value fails the build.
    @Hint(consumedBy = {"IOSDependencyManager"})
    IosDependencyManager dependencyManager() default IosDependencyManager.DEFAULT;

    /// Minimum iOS version the build targets. Set it to the lowest iOS you
    /// actually support; a higher value excludes older devices from the App Store
    /// listing.
    @Hint(name = "ios.deployment_target",
            kind = HintKind.VERSION)
    String deploymentTarget() default "";

    /// Objective-C code that can be injected into the iOS app delegate at the top
    /// of the file. For example, if you need to include headers or make special
    /// imports for other injected code
    @Hint(kind = HintKind.TEXT_BLOCK)
    String glAppDelegateHeader() default "";

    /// true/false (defaults to false). Whether to include the push capabilities in
    /// the iOS build. Notice that the IDE plugin has an "Include Push" check box
    /// you *should* use under the iOS section.
    Toggle includePush() default Toggle.DEFAULT;

    /// UIInterfaceOrientationPortrait by default. Indicates the orientation, one
    /// or more of (separated by colon :): `UIInterfaceOrientationPortrait`,
    /// `UIInterfaceOrientationPortraitUpsideDown`,
    /// `UIInterfaceOrientationLandscapeLeft`,
    /// `UIInterfaceOrientationLandscapeRight`. Notice that the IDE plugin has an
    /// "Interface Orientation" combo box you *should* use under the iOS section.
    @Hint(name = "ios.interface_orientation")
    String interfaceOrientation() default "";

    /// The null and empty-string reads of this hint are presence checks; 6.0 is
    /// the substantive default (IPhoneBuilder.java:4671).
    @Hint(kind = HintKind.VERSION)
    String minDeploymentTarget() default "";

    /// Stores app files under the documents directory rather than caches, which
    /// is the location Apple recommends but which may break compatibility with
    /// an app that already shipped. Defaults to TRUE: `IPhoneBuilder` reads this
    /// hint with a default of `"true"`, so a build that says nothing gets it.
    /// Described in
    /// https://github.com/codenameone/CodenameOne/issues/1480[this issue]
    Toggle newStorageLocation() default Toggle.DEFAULT;

    /// Added the `-ObjC` compile flag to the project files which some native
    /// libraries require
    Toggle objC() default Toggle.DEFAULT;

    /// entries to inject into the iOS plist file during build.
    @Hint(appendable = true,
            kind = HintKind.XML,
            consumedBy = {"IPhoneBuilder", "WatchNativeBuilder"})
    String plistInject() default "";

    /// A comma separated list of https://cocoapods.org/[Cocoa Pods] that should be
    /// linked to the app to build it. For example, `AFNetworking ~> 2.6,
    /// ORStackView ~> 3.0, SwiftyJSON ~> 2.3`
    @Hint(appendable = true,
            separator = ",")
    String[] pods() default {};

    /// Sets the Cocoapods 'platform' for the Cocoapods. Some Cocoapods require a
    /// minimum platform level. For example, `ios.pods.platform=7.0`.
    @Hint(name = "ios.pods.platform",
            kind = HintKind.VERSION)
    String podsPlatform() default "";

    /// Extra CocoaPods spec repositories to search, in addition to the default
    /// trunk.
    @Hint(appendable = true,
            name = "ios.pods.sources",
            separator = ",")
    String[] podsSources() default {};

    /// true/false defaults to false. The iOS build process adapts the submitted
    /// icon for iOS conventions (adding an overlay) that might not be appropriate
    /// on some icons. Setting this to true leaves the icon unchanged (only
    /// scaled).
    @Hint(name = "ios.prerendered_icon")
    Toggle prerenderedIcon() default Toggle.DEFAULT;

    /// one of ios, ipad, iphone (defaults to ios). Indicates whether the resulting
    /// binary is targeted to the iphone only or ipad only. Notice that the IDE
    /// plugin has a "Project Type" combo box you *should* use under the iOS
    /// section.
    @Hint(name = "ios.project_type",
            consumedBy = {"IPhoneBuilder", "MacNativeBuilder"})
    IosProjectType projectType() default IosProjectType.DEFAULT;

    /// Swift Package Manager packages to link, one per entry, each written as
    /// identity|url|requirement.
    @Hint(appendable = true,
            name = "ios.spm.packages",
            separator = ";",
            consumedBy = {"IOSDependencyManager", "IPhoneBuilder"})
    String[] spmPackages() default {};

    /// Specifies the team ID associated with the iOS provisioning profile and
    /// certificate. Use `ios.debug.teamId` and `ios.release.teamId` to specify
    /// different team IDs for debug and release builds respectively.
    @Hint(consumedBy = {"IPhoneBuilder", "MacNativeBuilder", "TvNativeBuilder", "WatchNativeBuilder"})
    String teamId() default "";

    /// `auto` (default), `modern`, `ios7`, `legacy`. `auto` (unset) keeps the
    /// existing iOS 7 flat theme so pre-refactor screenshot goldens and apps see
    /// no behavior change. `modern` / `liquid` opts in to the CSS-generated iOS
    /// Modern (liquid-glass) theme shipped from
    /// `native-themes/ios-modern/theme.css`. `ios7` / `flat` is the same as `auto`
    /// - pre-liquid iOS 7 flat theme; `legacy` / `iphone` loads the pre-iOS 7
    /// iPhone theme. The `auto` -> modern flip is planned for a future release.
    IosThemeMode themeMode() default IosThemeMode.DEFAULT;

    /// true/false (defaults to true). Enables iOS UIScene lifecycle support.
    /// UIScene lets iOS manage one or more app UI sessions independently,
    /// improving lifecycle handling in modern iOS versions. Apple has indicated
    /// UIScene will be required starting with iOS 27, so this is now on by
    /// default; set the flag to `false` only if you need to temporarily fall back
    /// to the legacy `UIApplicationDelegate` lifecycle.
    Toggle uiscene() default Toggle.DEFAULT;

    /// Allows intercepting a URL call using the syntax `<string>urlPrefix<string>`
    String urlScheme() default "";
}
