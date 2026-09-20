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
/// the build server's own default applies. The `default` clause below each
/// attribute names a constant that says nothing -- see [HintUnset] -- and this
/// package deliberately does not record what the server would do instead,
/// because that is the server's to change.
///
/// The platform is stated once on the annotation, not on every attribute. An
/// attribute repeats it only to disagree with it.
@Hint(platform = "ios")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Ios {

    /// A semicolon separated list of libraries that should be linked to the app to
    /// build it
    @Hint(appendable = true, name = "ios.add_libs", separator = ";")
    String[] addLibs() default {};

    /// Comma separated list of url schemes that `canExecute` will respect on iOS.
    /// If the url scheme isn't mentioned here `canExecute` will return false
    /// starting with iOS 9. Notice that this collides with `ios.plistInject` when
    /// used with the `<key>LSApplicationQueriesSchemes</key>...` value so you
    /// should use one or the other. For example, to enable `canExecute` for a url
    /// like `myurl://xys` you can use: `myurl,myotherurl`
    @Hint(appendable = true, separator = ",")
    String[] applicationQueriesSchemes() default {};

    /// Objective-C code that can be injected into the iOS app delegate at the top
    /// of the body of the didFinishLaunchingWithOptions callback method
    @Hint(kind = HintKind.TEXT_BLOCK)
    String beforeFinishLaunching() default "";

    /// Indicates the version number of the bundle, this is useful if you want to
    /// create a minor version number change for the beta testing support
    @Hint(kind = HintKind.VERSION)
    String bundleVersion() default "";

    /// Which native dependency manager to use: auto picks one from whichever of
    /// ios.pods and ios.spm.packages is set, and cocoapods, spm or both require
    /// the matching hint to be set. An unrecognized value fails the build.
    IosDependencyManager dependencyManager() default IosDependencyManager.DEFAULT;

    /// Minimum iOS version the build targets. Set it to the lowest iOS you
    /// actually support; a higher value excludes older devices from the App Store
    /// listing.
    @Hint(name = "ios.deployment_target", kind = HintKind.VERSION)
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
    @Hint(name = "ios.interface_orientation",
            // Longest alternative first, and the group repeated rather than
            // recursed: java.util.regex has no (?1).
            valuePattern = "(?i)UIInterfaceOrientation(PortraitUpsideDown|Portrait"
                    + "|LandscapeLeft|LandscapeRight)"
                    + "(:UIInterfaceOrientation(PortraitUpsideDown|Portrait"
                    + "|LandscapeLeft|LandscapeRight))*")
    String interfaceOrientation() default "";

    /// The null and empty-string reads of this hint are presence checks; 6.0 is
    /// the substantive one.
    @Hint(kind = HintKind.VERSION)
    String minDeploymentTarget() default "";

    /// Stores app files under the documents directory rather than caches, which
    /// is the location Apple recommends but which may break compatibility with
    /// an app that already shipped. Described in
    /// https://github.com/codenameone/CodenameOne/issues/1480[this issue]
    Toggle newStorageLocation() default Toggle.DEFAULT;

    /// Added the `-ObjC` compile flag to the project files which some native
    /// libraries require
    Toggle objC() default Toggle.DEFAULT;

    /// entries to inject into the iOS plist file during build.
    @Hint(appendable = true, kind = HintKind.XML)
    String plistInject() default "";

    /// A comma separated list of https://cocoapods.org/[Cocoa Pods] that should be
    /// linked to the app to build it. For example, `AFNetworking ~> 2.6,
    /// ORStackView ~> 3.0, SwiftyJSON ~> 2.3`
    @Hint(appendable = true, separator = ",")
    String[] pods() default {};

    /// Sets the Cocoapods 'platform' for the Cocoapods. Some Cocoapods require a
    /// minimum platform level. For example, `ios.pods.platform=7.0`.
    @Hint(name = "ios.pods.platform", kind = HintKind.VERSION)
    String podsPlatform() default "";

    /// Extra CocoaPods spec repositories to search, in addition to the default
    /// trunk.
    @Hint(appendable = true, name = "ios.pods.sources", separator = ",")
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
    @Hint(name = "ios.project_type")
    IosProjectType projectType() default IosProjectType.DEFAULT;

    /// Swift Package Manager packages to link, one per entry, each written as
    /// identity|url|requirement.
    @Hint(appendable = true, name = "ios.spm.packages", separator = ";")
    String[] spmPackages() default {};

    /// Specifies the team ID associated with the iOS provisioning profile and
    /// certificate. Use `ios.debug.teamId` and `ios.release.teamId` to specify
    /// different team IDs for debug and release builds respectively.
    String teamId() default "";

    /// `26` (default) or `27`: which iOS design generation the modern theme
    /// targets. Consulted only when [#themeMode()] resolves to modern/liquid;
    /// every other mode ignores it.
    ///
    /// Unset means 26, so an application that says nothing keeps the theme it
    /// has. `27` selects the generation built from
    /// `native-themes/ios-modern/gen27.css`.
    @Hint(valuePattern = "26|27")
    IosThemeGeneration themeGeneration() default IosThemeGeneration.DEFAULT;

    /// `auto` (default), `modern`, `ios7`, `legacy`. `auto` (unset) keeps the
    /// existing iOS 7 flat theme so pre-refactor screenshot goldens and apps see
    /// no behavior change. `modern` / `liquid` opts in to the CSS-generated iOS
    /// Modern (liquid-glass) theme shipped from
    /// `native-themes/ios-modern/theme.css`. `ios7` / `flat` is the same as `auto`
    /// - pre-liquid iOS 7 flat theme; `legacy` / `iphone` loads the pre-iOS 7
    /// iPhone theme. The `auto` -> modern flip is planned for a future release.
    @Hint(valuePattern = "auto|modern|ios7|legacy")
    ThemeMode themeMode() default ThemeMode.DEFAULT;

    /// Allows intercepting a URL call using the syntax `<string>urlPrefix<string>`
    String urlScheme() default "";

    /// Which Xcode the build server compiles with.
    ///
    /// Unset lets the server choose: it prefers its own default and falls back
    /// to the newest Xcode it carries, so a server that hasn't been re-imaged
    /// keeps building. Naming one opts out of that fallback -- a version the
    /// server doesn't have fails the build rather than substituting a toolchain
    /// nobody asked for, which would archive against an unintended SDK with
    /// nothing in the log to say so.
    ///
    /// Builds are claimed off a shared queue, so an Xcode that some servers
    /// carry and others don't makes a build pass or fail at random. That's a
    /// fleet out of step and worth reporting, not a hint to tune.
    ///
    /// The constants are the majors a current build server image carries, a set
    /// that belongs to the image rather than to this framework. To name a
    /// version outside them -- a minor such as `27.1`, or a major shipped since
    /// this release -- write a plain `codename1.arg.ios.xcode_version=<version>`
    /// line in `codenameone_settings.properties`. Leaving this attribute at
    /// [IosXcodeVersion#DEFAULT] writes nothing, so the two don't conflict.
    ///
    /// Read only by the build service; a local build uses the Xcode
    /// `xcode-select` points at and ignores this.
    @Hint(name = "ios.xcode_version", external = true)
    IosXcodeVersion xcodeVersion() default IosXcodeVersion.DEFAULT;
}
