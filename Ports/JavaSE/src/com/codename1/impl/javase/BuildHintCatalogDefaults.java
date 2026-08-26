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
package com.codename1.impl.javase;

/**
 * Build Hint editor schema for every hint that has a build hint annotation.
 *
 * <p>Generated from com.codename1.build.shared.BuildHints by
 * BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and re-run
 * scripts/gen-build-hint-annotations.sh.</p>
 *
 * <p>Registered after {@link BuildHintSchemaDefaults} and skipping every hint
 * that class already describes. Precedence cannot be left to the setter:
 * the group name is part of the property key, so registering harden.level
 * under both `hardening` and `Hardening` does not overwrite anything -- it
 * makes a second group, and the editor renders both, giving the user
 * duplicate controls for one setting.</p>
 */
final class BuildHintCatalogDefaults {

    private BuildHintCatalogDefaults() {
    }

    static void register() {
        java.util.Set<String> handWritten = BuildHintSchemaDefaults.declaredHints();

        set("{{@Ios}}.label", "iOS");
        if (!handWritten.contains("ios.add_libs")) {
        set("{{#Ios#ios.add_libs}}.label", "Add libs");
        set("{{#Ios#ios.add_libs}}.type", "TextArea");
        set("{{#Ios#ios.add_libs}}.description", "A semicolon separated list of libraries that should be linked to the app to build it");
        }
        if (!handWritten.contains("ios.applicationQueriesSchemes")) {
        set("{{#Ios#ios.applicationQueriesSchemes}}.label", "Application queries schemes");
        set("{{#Ios#ios.applicationQueriesSchemes}}.type", "TextArea");
        set("{{#Ios#ios.applicationQueriesSchemes}}.description", "Comma separated list of url schemes that `canExecute` will respect on iOS. If the url scheme isn't mentioned here `canExecute` will return false starting with iOS 9. Notice that this collides with `ios.plistInject` when used with the `<key>LSApplicationQueriesSchemes</key>...` value so you should use one or the other. For example, to enable `canExecute` for a url like `myurl://xys` you can use: `myurl,myotherurl`");
        }
        if (!handWritten.contains("ios.beforeFinishLaunching")) {
        set("{{#Ios#ios.beforeFinishLaunching}}.label", "Before finish launching");
        set("{{#Ios#ios.beforeFinishLaunching}}.type", "TextArea");
        set("{{#Ios#ios.beforeFinishLaunching}}.description", "Objective-C code that can be injected into the iOS app delegate at the top of the body of the didFinishLaunchingWithOptions callback method");
        }
        if (!handWritten.contains("ios.bundleVersion")) {
        set("{{#Ios#ios.bundleVersion}}.label", "Bundle version");
        set("{{#Ios#ios.bundleVersion}}.type", "TextField");
        set("{{#Ios#ios.bundleVersion}}.description", "Indicates the version number of the bundle, this is useful if you want to create a minor version number change for the beta testing support");
        }
        if (!handWritten.contains("ios.dependencyManager")) {
        set("{{#Ios#ios.dependencyManager}}.label", "Dependency manager");
        set("{{#Ios#ios.dependencyManager}}.type", "Select");
        set("{{#Ios#ios.dependencyManager}}.values", "auto,cocoapods,spm,both,none");
        set("{{#Ios#ios.dependencyManager}}.description", "Which native dependency manager to use: auto picks one from whichever of ios.pods and ios.spm.packages is set, and cocoapods, spm or both require the matching hint to be set. An unrecognized value fails the build.");
        }
        if (!handWritten.contains("ios.deployment_target")) {
        set("{{#Ios#ios.deployment_target}}.label", "Deployment target");
        set("{{#Ios#ios.deployment_target}}.type", "TextField");
        set("{{#Ios#ios.deployment_target}}.description", "Minimum iOS version the build targets. Set it to the lowest iOS you actually support; a higher value excludes older devices from the App Store listing.");
        }
        if (!handWritten.contains("ios.glAppDelegateHeader")) {
        set("{{#Ios#ios.glAppDelegateHeader}}.label", "Gl app delegate header");
        set("{{#Ios#ios.glAppDelegateHeader}}.type", "TextArea");
        set("{{#Ios#ios.glAppDelegateHeader}}.description", "Objective-C code that can be injected into the iOS app delegate at the top of the file. For example, if you need to include headers or make special imports for other injected code");
        }
        if (!handWritten.contains("ios.includePush")) {
        set("{{#Ios#ios.includePush}}.label", "Include push");
        set("{{#Ios#ios.includePush}}.type", "Checkbox");
        set("{{#Ios#ios.includePush}}.description", "true/false (defaults to false). Whether to include the push capabilities in the iOS build. Notice that the IDE plugin has an \"Include Push\" check box you *should* use under the iOS section.");
        }
        if (!handWritten.contains("ios.interface_orientation")) {
        set("{{#Ios#ios.interface_orientation}}.label", "Interface orientation");
        set("{{#Ios#ios.interface_orientation}}.type", "TextField");
        set("{{#Ios#ios.interface_orientation}}.description", "UIInterfaceOrientationPortrait by default. Indicates the orientation, one or more of (separated by colon :): `UIInterfaceOrientationPortrait`, `UIInterfaceOrientationPortraitUpsideDown`, `UIInterfaceOrientationLandscapeLeft`, `UIInterfaceOrientationLandscapeRight`. Notice that the IDE plugin has an \"Interface Orientation\" combo box you *should* use under the iOS section.");
        }
        if (!handWritten.contains("ios.minDeploymentTarget")) {
        set("{{#Ios#ios.minDeploymentTarget}}.label", "Min deployment target");
        set("{{#Ios#ios.minDeploymentTarget}}.type", "TextField");
        set("{{#Ios#ios.minDeploymentTarget}}.description", "The null and empty-string reads of this hint are presence checks; 6.0 is the substantive default (IPhoneBuilder.java:4671).");
        }
        if (!handWritten.contains("ios.newStorageLocation")) {
        set("{{#Ios#ios.newStorageLocation}}.label", "New storage location");
        set("{{#Ios#ios.newStorageLocation}}.type", "Checkbox");
        set("{{#Ios#ios.newStorageLocation}}.description", "Stores app files under the documents directory rather than caches, which is the location Apple recommends but which may break compatibility with an app that already shipped. Defaults to TRUE: `IPhoneBuilder` reads this hint with a default of `\"true\"`, so a build that says nothing gets it. Described in https://github.com/codenameone/CodenameOne/issues/1480[this issue]");
        }
        if (!handWritten.contains("ios.objC")) {
        set("{{#Ios#ios.objC}}.label", "Obj c");
        set("{{#Ios#ios.objC}}.type", "Checkbox");
        set("{{#Ios#ios.objC}}.description", "Added the `-ObjC` compile flag to the project files which some native libraries require");
        }
        if (!handWritten.contains("ios.plistInject")) {
        set("{{#Ios#ios.plistInject}}.label", "Plist inject");
        set("{{#Ios#ios.plistInject}}.type", "TextArea");
        set("{{#Ios#ios.plistInject}}.description", "entries to inject into the iOS plist file during build.");
        }
        if (!handWritten.contains("ios.pods")) {
        set("{{#Ios#ios.pods}}.label", "Pods");
        set("{{#Ios#ios.pods}}.type", "TextArea");
        set("{{#Ios#ios.pods}}.description", "A comma separated list of https://cocoapods.org/[Cocoa Pods] that should be linked to the app to build it. For example, `AFNetworking ~> 2.6, ORStackView ~> 3.0, SwiftyJSON ~> 2.3`");
        }
        if (!handWritten.contains("ios.pods.platform")) {
        set("{{#Ios#ios.pods.platform}}.label", "Pods platform");
        set("{{#Ios#ios.pods.platform}}.type", "TextField");
        set("{{#Ios#ios.pods.platform}}.description", "Sets the Cocoapods 'platform' for the Cocoapods. Some Cocoapods require a minimum platform level. For example, `ios.pods.platform=7.0`.");
        }
        if (!handWritten.contains("ios.pods.sources")) {
        set("{{#Ios#ios.pods.sources}}.label", "Pods sources");
        set("{{#Ios#ios.pods.sources}}.type", "TextArea");
        set("{{#Ios#ios.pods.sources}}.description", "Extra CocoaPods spec repositories to search, in addition to the default trunk.");
        }
        if (!handWritten.contains("ios.prerendered_icon")) {
        set("{{#Ios#ios.prerendered_icon}}.label", "Prerendered icon");
        set("{{#Ios#ios.prerendered_icon}}.type", "Checkbox");
        set("{{#Ios#ios.prerendered_icon}}.description", "true/false defaults to false. The iOS build process adapts the submitted icon for iOS conventions (adding an overlay) that might not be appropriate on some icons. Setting this to true leaves the icon unchanged (only scaled).");
        }
        if (!handWritten.contains("ios.project_type")) {
        set("{{#Ios#ios.project_type}}.label", "Project type");
        set("{{#Ios#ios.project_type}}.type", "Select");
        set("{{#Ios#ios.project_type}}.values", "ios,ipad,iphone");
        set("{{#Ios#ios.project_type}}.description", "one of ios, ipad, iphone (defaults to ios). Indicates whether the resulting binary is targeted to the iphone only or ipad only. Notice that the IDE plugin has a \"Project Type\" combo box you *should* use under the iOS section.");
        }
        if (!handWritten.contains("ios.spm.packages")) {
        set("{{#Ios#ios.spm.packages}}.label", "Spm packages");
        set("{{#Ios#ios.spm.packages}}.type", "TextArea");
        set("{{#Ios#ios.spm.packages}}.description", "Swift Package Manager packages to link, one per entry, each written as identity|url|requirement.");
        }
        if (!handWritten.contains("ios.teamId")) {
        set("{{#Ios#ios.teamId}}.label", "Team id");
        set("{{#Ios#ios.teamId}}.type", "TextField");
        set("{{#Ios#ios.teamId}}.description", "Specifies the team ID associated with the iOS provisioning profile and certificate. Use `ios.debug.teamId` and `ios.release.teamId` to specify different team IDs for debug and release builds respectively.");
        }
        if (!handWritten.contains("ios.themeMode")) {
        set("{{#Ios#ios.themeMode}}.label", "Theme mode");
        set("{{#Ios#ios.themeMode}}.type", "Select");
        set("{{#Ios#ios.themeMode}}.values", "auto,modern,ios7,legacy");
        set("{{#Ios#ios.themeMode}}.description", "`auto` (default), `modern`, `ios7`, `legacy`. `auto` (unset) keeps the existing iOS 7 flat theme so pre-refactor screenshot goldens and apps see no behavior change. `modern` / `liquid` opts in to the CSS-generated iOS Modern (liquid-glass) theme shipped from `native-themes/ios-modern/theme.css`. `ios7` / `flat` is the same as `auto` - pre-liquid iOS 7 flat theme; `legacy` / `iphone` loads the pre-iOS 7 iPhone theme. The `auto` -> modern flip is planned for a future release.");
        }
        if (!handWritten.contains("ios.uiscene")) {
        set("{{#Ios#ios.uiscene}}.label", "Uiscene");
        set("{{#Ios#ios.uiscene}}.type", "Checkbox");
        set("{{#Ios#ios.uiscene}}.description", "true/false (defaults to true). Enables iOS UIScene lifecycle support. UIScene lets iOS manage one or more app UI sessions independently, improving lifecycle handling in modern iOS versions. Apple has indicated UIScene will be required starting with iOS 27, so this is now on by default; set the flag to `false` only if you need to temporarily fall back to the legacy `UIApplicationDelegate` lifecycle.");
        }
        if (!handWritten.contains("ios.urlScheme")) {
        set("{{#Ios#ios.urlScheme}}.label", "Url scheme");
        set("{{#Ios#ios.urlScheme}}.type", "TextField");
        set("{{#Ios#ios.urlScheme}}.description", "Allows intercepting a URL call using the syntax `<string>urlPrefix<string>`");
        }

        set("{{@Android}}.label", "Android");
        if (!handWritten.contains("android.activity.launchMode")) {
        set("{{#Android#android.activity.launchMode}}.label", "Activity launch mode");
        set("{{#Android#android.activity.launchMode}}.type", "TextField");
        set("{{#Android#android.activity.launchMode}}.description", "Allows explicitly setting the `android:launchMode` attribute of the main activity in android. Default is \"singleTop,\" but for some applications you may need to change this behaviour. In particular, apps that are meant to open a file type will need to set this to \"singleTask.\" See https://developer.android.com/guide/topics/manifest/activity-element.html[Android docs for the activity element] for more information about the `android:launchMode` attribute.");
        }
        if (!handWritten.contains("android.appBundle")) {
        set("{{#Android#android.appBundle}}.label", "App bundle");
        set("{{#Android#android.appBundle}}.type", "Checkbox");
        set("{{#Android#android.appBundle}}.description", "Produces an Android App Bundle (.aab) rather than an APK. Required for new Play Store submissions.");
        }
        if (!handWritten.contains("android.buildToolsVersion")) {
        set("{{#Android#android.buildToolsVersion}}.label", "Build tools version");
        set("{{#Android#android.buildToolsVersion}}.type", "TextField");
        set("{{#Android#android.buildToolsVersion}}.description", "Android build-tools version. It also selects the compile SDK, so there is no separate compile-SDK hint.");
        }
        if (!handWritten.contains("android.captureRecord")) {
        set("{{#Android#android.captureRecord}}.label", "Capture record");
        set("{{#Android#android.captureRecord}}.type", "TextField");
        set("{{#Android#android.captureRecord}}.description", "Indicates whether the `RECORD_AUDIO` permission should be requested. Can be `enabled` or any other value to disable this option");
        }
        if (!handWritten.contains("android.debug")) {
        set("{{#Android#android.debug}}.label", "Debug");
        set("{{#Android#android.debug}}.type", "Checkbox");
        set("{{#Android#android.debug}}.description", "Whether to include the debug version in the build. This hint has NO single default, which is why none is recorded: `AndroidGradleBuilder` reads it with a default of `\"false\"` under `android.release` and `\"true\"` otherwise, so a build that selects neither still produces something installable (AndroidGradleBuilder.java:447-451, :530-531).");
        }
        if (!handWritten.contains("android.disableR8")) {
        set("{{#Android#android.disableR8}}.label", "Disable r8");
        set("{{#Android#android.disableR8}}.type", "Checkbox");
        set("{{#Android#android.disableR8}}.description", "Turns off R8, falling back to the older shrinker. Note that hardening requires R8, so this conflicts with harden.level.");
        }
        if (!handWritten.contains("android.enableProguard")) {
        set("{{#Android#android.enableProguard}}.label", "Enable proguard");
        set("{{#Android#android.enableProguard}}.type", "Checkbox");
        set("{{#Android#android.enableProguard}}.description", "Boolean true/false defaults to true. Allows disabling the proguard obfuscation even on release builds, notice that this isn't recommended");
        }
        if (!handWritten.contains("android.gradleDep")) {
        set("{{#Android#android.gradleDep}}.label", "Gradle dep");
        set("{{#Android#android.gradleDep}}.type", "TextArea");
        set("{{#Android#android.gradleDep}}.description", "Gradle dependency statements to add to the app module, such as implementation 'com.example:lib:1.0'.");
        }
        if (!handWritten.contains("android.hideStatusBar")) {
        set("{{#Android#android.hideStatusBar}}.label", "Hide status bar");
        set("{{#Android#android.hideStatusBar}}.type", "Checkbox");
        set("{{#Android#android.hideStatusBar}}.description", "Hides the Android status bar.");
        }
        if (!handWritten.contains("android.installLocation")) {
        set("{{#Android#android.installLocation}}.label", "Install location");
        set("{{#Android#android.installLocation}}.type", "Select");
        set("{{#Android#android.installLocation}}.values", "auto,internalOnly,preferExternal");
        set("{{#Android#android.installLocation}}.description", "Maps to android:installLocation manifest entry defaults to auto. Can also be set to internalOnly or preferExternal.");
        }
        if (!handWritten.contains("android.licenseKey")) {
        set("{{#Android#android.licenseKey}}.label", "License key");
        set("{{#Android#android.licenseKey}}.type", "TextField");
        set("{{#Android#android.licenseKey}}.description", "The license key for the Android app, this is required if you use in-app purchase on Android");
        }
        if (!handWritten.contains("android.min_sdk_version")) {
        set("{{#Android#android.min_sdk_version}}.label", "Min sdk version");
        set("{{#Android#android.min_sdk_version}}.type", "Select");
        set("{{#Android#android.min_sdk_version}}.values", "19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36");
        set("{{#Android#android.min_sdk_version}}.description", "The least SDK required to run this app, the default value changes based on functionality but can be as low as 7. This corresponds to the XML attribute `android:minSdkVersion`.");
        }
        if (!handWritten.contains("android.multidex")) {
        set("{{#Android#android.multidex}}.label", "Multidex");
        set("{{#Android#android.multidex}}.type", "Checkbox");
        set("{{#Android#android.multidex}}.description", "Multidex lets an Android binary reference more than 65536 methods. Defaults to TRUE: `AndroidGradleBuilder` reads this hint with a default of `\"true\"`, so a build that says nothing gets multidex. Set it to false to opt out, which builds a little faster and reinstates the limit.");
        }
        if (!handWritten.contains("android.newFirebaseMessaging")) {
        set("{{#Android#android.newFirebaseMessaging}}.label", "New firebase messaging");
        set("{{#Android#android.newFirebaseMessaging}}.type", "Checkbox");
        set("{{#Android#android.newFirebaseMessaging}}.description", "Uses the current Firebase Cloud Messaging integration. Requires AndroidX and Gradle 8.13 or newer.");
        }
        if (!handWritten.contains("android.proguardKeep")) {
        set("{{#Android#android.proguardKeep}}.label", "Proguard keep");
        set("{{#Android#android.proguardKeep}}.type", "TextArea");
        set("{{#Android#android.proguardKeep}}.description", "Arguments for the keep option in proguard allowing you to keep a pattern of files for example, `-keep class com.mypackage.ProblemClass { *; }`");
        }
        if (!handWritten.contains("android.release")) {
        set("{{#Android#android.release}}.label", "Release");
        set("{{#Android#android.release}}.type", "Checkbox");
        set("{{#Android#android.release}}.description", "true/false defaults to true - indicates whether to include the release version in the build");
        }
        if (!handWritten.contains("android.repositories")) {
        set("{{#Android#android.repositories}}.label", "Repositories");
        set("{{#Android#android.repositories}}.type", "TextArea");
        set("{{#Android#android.repositories}}.description", "Extra Gradle repositories to resolve dependencies from.");
        }
        if (!handWritten.contains("android.targetSDKVersion")) {
        set("{{#Android#android.targetSDKVersion}}.label", "Target sDKVersion");
        set("{{#Android#android.targetSDKVersion}}.type", "TextField");
        set("{{#Android#android.targetSDKVersion}}.description", "The Android SDK the build compiles against. Unset, the build server uses the highest platform it has installed, so leaving this alone tracks the server rather than pinning a number. Not every target works: the source may have limitations, and not all SDK targets are installed.");
        }
        if (!handWritten.contains("and.themeMode")) {
        set("{{#Android#and.themeMode}}.label", "Theme mode");
        set("{{#Android#and.themeMode}}.type", "Select");
        set("{{#Android#and.themeMode}}.values", "auto,modern,hololight,legacy");
        set("{{#Android#and.themeMode}}.description", "`auto`, `modern` / `material`, `hololight` (default for existing apps), `legacy`. `auto` and `modern` / `material` opt in to the CSS-generated Android Material 3 theme from `native-themes/android-material/theme.css`. `hololight` is Android Holo Light (what the framework shipped on API 14+ before this refactor). `legacy` loads the pre-Holo Android theme. The legacy alias `cn1.androidTheme` is still accepted, and `and.hololight=true` still maps to `hololight`. The default stays on `hololight` for existing apps until you flip in a future release.");
        }
        if (!handWritten.contains("android.topDependency")) {
        set("{{#Android#android.topDependency}}.label", "Top dependency");
        set("{{#Android#android.topDependency}}.type", "TextArea");
        set("{{#Android#android.topDependency}}.description", "Statements added to the top-level Gradle build file rather than the app module.");
        }
        if (!handWritten.contains("android.useAndroidX")) {
        set("{{#Android#android.useAndroidX}}.label", "Use android x");
        set("{{#Android#android.useAndroidX}}.type", "Checkbox");
        set("{{#Android#android.useAndroidX}}.description", "Use Android X instead of support libraries. This will also run a find/replace on all source files to replace support libraries and artifacts with AndroidX equivalents.");
        }
        if (!handWritten.contains("android.xapplication")) {
        set("{{#Android#android.xapplication}}.label", "Xapplication");
        set("{{#Android#android.xapplication}}.type", "TextArea");
        set("{{#Android#android.xapplication}}.description", "defaults to an empty string. Allows developers of native Android code to add text within the application block to define things such as widgets, services etc.");
        }
        if (!handWritten.contains("android.xgradle")) {
        set("{{#Android#android.xgradle}}.label", "Xgradle");
        set("{{#Android#android.xgradle}}.type", "TextArea");
        set("{{#Android#android.xgradle}}.description", "Arbitrary text spliced into the generated app-module Gradle file.");
        }
        if (!handWritten.contains("android.xpermissions")) {
        set("{{#Android#android.xpermissions}}.label", "Xpermissions");
        set("{{#Android#android.xpermissions}}.type", "TextArea");
        set("{{#Android#android.xpermissions}}.description", "more permissions for the Android manifest");
        }

        set("{{@DesktopBuild}}.label", "Desktop");
        if (!handWritten.contains("desktop.adaptToRetina")) {
        set("{{#DesktopBuild#desktop.adaptToRetina}}.label", "Adapt to retina");
        set("{{#DesktopBuild#desktop.adaptToRetina}}.type", "Checkbox");
        set("{{#DesktopBuild#desktop.adaptToRetina}}.description", "Boolean true/false defaults to true. When set to true some values will ve implicitly doubled to deal with retina displays and icons etc. Will use higher DPI's");
        }
        if (!handWritten.contains("desktop.fullscreen")) {
        set("{{#DesktopBuild#desktop.fullscreen}}.label", "Fullscreen");
        set("{{#DesktopBuild#desktop.fullscreen}}.type", "Checkbox");
        set("{{#DesktopBuild#desktop.fullscreen}}.description", "Starts the desktop build in full-screen mode.");
        }
        if (!handWritten.contains("desktop.height")) {
        set("{{#DesktopBuild#desktop.height}}.label", "Height");
        set("{{#DesktopBuild#desktop.height}}.type", "TextField");
        set("{{#DesktopBuild#desktop.height}}.description", "Height in pixels for the form in desktop builds, will be doubled for retina grade displays. Defaults to 600.");
        }
        if (!handWritten.contains("desktop.interactiveScrollbars")) {
        set("{{#DesktopBuild#desktop.interactiveScrollbars}}.label", "Interactive scrollbars");
        set("{{#DesktopBuild#desktop.interactiveScrollbars}}.type", "Checkbox");
        set("{{#DesktopBuild#desktop.interactiveScrollbars}}.description", "Enables grab-able, click-to-page desktop scrollbars.");
        }
        if (!handWritten.contains("desktop.resizable")) {
        set("{{#DesktopBuild#desktop.resizable}}.label", "Resizable");
        set("{{#DesktopBuild#desktop.resizable}}.type", "Checkbox");
        set("{{#DesktopBuild#desktop.resizable}}.description", "Boolean true/false defaults to true. Indicates whether the UI in the desktop build is resizable");
        }
        if (!handWritten.contains("desktop.titleBar")) {
        set("{{#DesktopBuild#desktop.titleBar}}.label", "Title bar");
        set("{{#DesktopBuild#desktop.titleBar}}.type", "Select");
        set("{{#DesktopBuild#desktop.titleBar}}.values", "native,custom,toolbar");
        set("{{#DesktopBuild#desktop.titleBar}}.description", "How the desktop window is framed: native for the OS title bar and menu bar, custom for an undecorated window with a Codename One drawn title bar, or toolbar for the legacy in-app Toolbar. An unrecognized value falls back to native with a warning.");
        }
        if (!handWritten.contains("desktop.width")) {
        set("{{#DesktopBuild#desktop.width}}.label", "Width");
        set("{{#DesktopBuild#desktop.width}}.type", "TextField");
        set("{{#DesktopBuild#desktop.width}}.description", "Width in pixels for the form in desktop builds, will be doubled for retina grade displays. Defaults to 800.");
        }

        set("{{@Hardening}}.label", "App Hardening");
        if (!handWritten.contains("harden.allowUnhardenedLocalBuild")) {
        set("{{#Hardening#harden.allowUnhardenedLocalBuild}}.label", "Allow unhardened local build");
        set("{{#Hardening#harden.allowUnhardenedLocalBuild}}.type", "Checkbox");
        set("{{#Hardening#harden.allowUnhardenedLocalBuild}}.description", "Permits a local or source build to run with hardening requested but not applied. Without it such a build is refused, so a hardened app is never shipped from a target that can't actually harden it.");
        }
        if (!handWritten.contains("harden.controlFlow")) {
        set("{{#Hardening#harden.controlFlow}}.label", "Control flow");
        set("{{#Hardening#harden.controlFlow}}.type", "Select");
        set("{{#Hardening#harden.controlFlow}}.values", "off,on");
        set("{{#Hardening#harden.controlFlow}}.description", "Overrides control-flow obfuscation independently of harden.level.");
        }
        if (!handWritten.contains("harden.keep")) {
        set("{{#Hardening#harden.keep}}.label", "Keep");
        set("{{#Hardening#harden.keep}}.type", "TextArea");
        set("{{#Hardening#harden.keep}}.description", "Keep rules in ProGuard syntax, one per line, for classes that are resolved by name at runtime and so can't be found by the automatic analysis. Same syntax as android.proguardKeep, so existing rules port directly. Rules are separated by newlines only, because a semicolon is legal inside a rule body such as { *; }.");
        }
        if (!handWritten.contains("harden.level")) {
        set("{{#Hardening#harden.level}}.label", "Level");
        set("{{#Hardening#harden.level}}.type", "Select");
        set("{{#Hardening#harden.level}}.values", "off,standard,aggressive,paranoid");
        set("{{#Hardening#harden.level}}.description", "Master switch for app hardening: off, standard, aggressive or paranoid. An unrecognized value fails the build rather than being treated as off.");
        }
        if (!handWritten.contains("harden.rename")) {
        set("{{#Hardening#harden.rename}}.label", "Rename");
        set("{{#Hardening#harden.rename}}.type", "Checkbox");
        set("{{#Hardening#harden.rename}}.description", "Overrides symbol renaming independently of harden.level.");
        }
        if (!handWritten.contains("harden.strings")) {
        set("{{#Hardening#harden.strings}}.label", "Strings");
        set("{{#Hardening#harden.strings}}.type", "Select");
        set("{{#Hardening#harden.strings}}.values", "off,constants,all");
        set("{{#Hardening#harden.strings}}.description", "Overrides string obfuscation independently of harden.level: off, constants or all.");
        }

        set("{{@OnDeviceDebug}}.label", "On-Device Debugging");
        if (!handWritten.contains("android.onDeviceDebug")) {
        set("{{#OnDeviceDebug#android.onDeviceDebug}}.label", "Android");
        set("{{#OnDeviceDebug#android.onDeviceDebug}}.type", "Checkbox");
        set("{{#OnDeviceDebug#android.onDeviceDebug}}.description", "Boolean true/false defaults to false. When `true`, the generated `AndroidManifest.xml` is marked `android:debuggable=\"true\"`, R8/proguard is disabled, and the build is pinned to debug-only (`android.release` is forced off and `android.debug` is forced on) so a stray hint can't ship a release-signed APK that's `debuggable=\"true\"`. Pair with the `cn1:android-on-device-debugging` Maven goal (or the bundled IntelliJ run configs) to install, launch, forward JDWP, and stream logcat through adb. Has no effect on builds that don't carry it -- release builds are unaffected. See the On-Device Debugging (Android) chapter for the full flow.");
        }
        if (!handWritten.contains("ios.onDeviceDebug")) {
        set("{{#OnDeviceDebug#ios.onDeviceDebug}}.label", "Ios");
        set("{{#OnDeviceDebug#ios.onDeviceDebug}}.type", "Checkbox");
        set("{{#OnDeviceDebug#ios.onDeviceDebug}}.description", "Boolean true/false defaults to false. When `true`, the iOS build links a small JDWP listener thread (`cn1_debugger`) into the binary and the ParparVM translator emits source-line and locals metadata so a desktop proxy can serve the running app to any JDWP-speaking debugger. Has no effect on release builds. See the On-Device Debugging (iOS) chapter for the full flow.");
        }
        if (!handWritten.contains("ios.onDeviceDebug.proxyHost")) {
        set("{{#OnDeviceDebug#ios.onDeviceDebug.proxyHost}}.label", "Ios proxy host");
        set("{{#OnDeviceDebug#ios.onDeviceDebug.proxyHost}}.type", "TextField");
        set("{{#OnDeviceDebug#ios.onDeviceDebug.proxyHost}}.description", "Hostname or IP address the device-side listener dials to reach the desktop proxy. Default `127.0.0.1` (correct for the native iOS simulator). For a physical device, set this to the developer laptop's LAN IP. Has no effect unless `ios.onDeviceDebug=true`.");
        }
        if (!handWritten.contains("ios.onDeviceDebug.proxyPort")) {
        set("{{#OnDeviceDebug#ios.onDeviceDebug.proxyPort}}.label", "Ios proxy port");
        set("{{#OnDeviceDebug#ios.onDeviceDebug.proxyPort}}.type", "TextField");
        set("{{#OnDeviceDebug#ios.onDeviceDebug.proxyPort}}.description", "TCP port on `ios.onDeviceDebug.proxyHost` where the proxy is listening for the device. Default `55333`. Has no effect unless `ios.onDeviceDebug=true`.");
        }
        if (!handWritten.contains("ios.onDeviceDebug.waitForAttach")) {
        set("{{#OnDeviceDebug#ios.onDeviceDebug.waitForAttach}}.label", "Ios wait for attach");
        set("{{#OnDeviceDebug#ios.onDeviceDebug.waitForAttach}}.type", "Checkbox");
        set("{{#OnDeviceDebug#ios.onDeviceDebug.waitForAttach}}.description", "Boolean true/false defaults to false. When `true`, the app blocks at startup until the proxy connects and the IDE tells the VM to continue. Useful when the breakpoint to investigate fires during app boot. Has no effect unless `ios.onDeviceDebug=true`.");
        }

        set("{{@IosPrivacy}}.label", "iOS Privacy Strings");
        if (!handWritten.contains("ios.NSBluetoothAlwaysUsageDescription")) {
        set("{{#IosPrivacy#ios.NSBluetoothAlwaysUsageDescription}}.label", "Bluetooth always usage description");
        set("{{#IosPrivacy#ios.NSBluetoothAlwaysUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSBluetoothAlwaysUsageDescription}}.description", "Why the app uses Bluetooth. Supplied automatically when the app references `com.codename1.bluetooth`; set it to say something more specific than the default.");
        }
        if (!handWritten.contains("ios.NSBluetoothPeripheralUsageDescription")) {
        set("{{#IosPrivacy#ios.NSBluetoothPeripheralUsageDescription}}.label", "Bluetooth peripheral usage description");
        set("{{#IosPrivacy#ios.NSBluetoothPeripheralUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSBluetoothPeripheralUsageDescription}}.description", "The pre-iOS 13 spelling of the Bluetooth usage description, supplied and overridable on the same terms.");
        }
        if (!handWritten.contains("ios.NSCalendarsFullAccessUsageDescription")) {
        set("{{#IosPrivacy#ios.NSCalendarsFullAccessUsageDescription}}.label", "Calendars full access usage description");
        set("{{#IosPrivacy#ios.NSCalendarsFullAccessUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSCalendarsFullAccessUsageDescription}}.description", "The text iOS shows when the app first asks for the calendars full access. It becomes the `NSCalendarsFullAccessUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSCalendarsUsageDescription")) {
        set("{{#IosPrivacy#ios.NSCalendarsUsageDescription}}.label", "Calendars usage description");
        set("{{#IosPrivacy#ios.NSCalendarsUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSCalendarsUsageDescription}}.description", "The text iOS shows when the app first asks for the calendars. It becomes the `NSCalendarsUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSCalendarsWriteOnlyAccessUsageDescription")) {
        set("{{#IosPrivacy#ios.NSCalendarsWriteOnlyAccessUsageDescription}}.label", "Calendars write only access usage description");
        set("{{#IosPrivacy#ios.NSCalendarsWriteOnlyAccessUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSCalendarsWriteOnlyAccessUsageDescription}}.description", "The text iOS shows when the app first asks for the calendars write only access. It becomes the `NSCalendarsWriteOnlyAccessUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSCameraUsageDescription")) {
        set("{{#IosPrivacy#ios.NSCameraUsageDescription}}.label", "Camera usage description");
        set("{{#IosPrivacy#ios.NSCameraUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSCameraUsageDescription}}.description", "The text iOS shows when the app first asks for the camera. It becomes the `NSCameraUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSHealthShareUsageDescription")) {
        set("{{#IosPrivacy#ios.NSHealthShareUsageDescription}}.label", "Health share usage description");
        set("{{#IosPrivacy#ios.NSHealthShareUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSHealthShareUsageDescription}}.description", "The text iOS shows when the app first asks for the health share. It becomes the `NSHealthShareUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSHealthUpdateUsageDescription")) {
        set("{{#IosPrivacy#ios.NSHealthUpdateUsageDescription}}.label", "Health update usage description");
        set("{{#IosPrivacy#ios.NSHealthUpdateUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSHealthUpdateUsageDescription}}.description", "The text iOS shows when the app first asks for the health update. It becomes the `NSHealthUpdateUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSLocalNetworkUsageDescription")) {
        set("{{#IosPrivacy#ios.NSLocalNetworkUsageDescription}}.label", "Local network usage description");
        set("{{#IosPrivacy#ios.NSLocalNetworkUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSLocalNetworkUsageDescription}}.description", "The text iOS shows when the app first asks for the local network. It becomes the `NSLocalNetworkUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSLocationAlwaysAndWhenInUseUsageDescription")) {
        set("{{#IosPrivacy#ios.NSLocationAlwaysAndWhenInUseUsageDescription}}.label", "Location always and when in use usage description");
        set("{{#IosPrivacy#ios.NSLocationAlwaysAndWhenInUseUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSLocationAlwaysAndWhenInUseUsageDescription}}.description", "The text iOS shows when the app first asks for the location always and when in use. It becomes the `NSLocationAlwaysAndWhenInUseUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSLocationAlwaysUsageDescription")) {
        set("{{#IosPrivacy#ios.NSLocationAlwaysUsageDescription}}.label", "Location always usage description");
        set("{{#IosPrivacy#ios.NSLocationAlwaysUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSLocationAlwaysUsageDescription}}.description", "The text iOS shows when the app first asks for the location always. It becomes the `NSLocationAlwaysUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSLocationWhenInUseUsageDescription")) {
        set("{{#IosPrivacy#ios.NSLocationWhenInUseUsageDescription}}.label", "Location when in use usage description");
        set("{{#IosPrivacy#ios.NSLocationWhenInUseUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSLocationWhenInUseUsageDescription}}.description", "The text iOS shows when the app first asks for the location when in use. It becomes the `NSLocationWhenInUseUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSMicrophoneUsageDescription")) {
        set("{{#IosPrivacy#ios.NSMicrophoneUsageDescription}}.label", "Microphone usage description");
        set("{{#IosPrivacy#ios.NSMicrophoneUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSMicrophoneUsageDescription}}.description", "The text iOS shows when the app first asks for the microphone. It becomes the `NSMicrophoneUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSNearbyInteractionAllowOnceUsageDescription")) {
        set("{{#IosPrivacy#ios.NSNearbyInteractionAllowOnceUsageDescription}}.label", "Nearby interaction allow once usage description");
        set("{{#IosPrivacy#ios.NSNearbyInteractionAllowOnceUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSNearbyInteractionAllowOnceUsageDescription}}.description", "The pre-iOS 16 spelling of the nearby-interaction usage description, supplied automatically when the app references the nearby APIs.");
        }
        if (!handWritten.contains("ios.NSNearbyInteractionUsageDescription")) {
        set("{{#IosPrivacy#ios.NSNearbyInteractionUsageDescription}}.label", "Nearby interaction usage description");
        set("{{#IosPrivacy#ios.NSNearbyInteractionUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSNearbyInteractionUsageDescription}}.description", "Why the app measures distance and direction to nearby devices. Supplied automatically when the app references the nearby APIs; set it to say something more specific than the default.");
        }
        if (!handWritten.contains("ios.NSRemindersFullAccessUsageDescription")) {
        set("{{#IosPrivacy#ios.NSRemindersFullAccessUsageDescription}}.label", "Reminders full access usage description");
        set("{{#IosPrivacy#ios.NSRemindersFullAccessUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSRemindersFullAccessUsageDescription}}.description", "The text iOS shows when the app first asks for the reminders full access. It becomes the `NSRemindersFullAccessUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSRemindersUsageDescription")) {
        set("{{#IosPrivacy#ios.NSRemindersUsageDescription}}.label", "Reminders usage description");
        set("{{#IosPrivacy#ios.NSRemindersUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSRemindersUsageDescription}}.description", "The text iOS shows when the app first asks for the reminders. It becomes the `NSRemindersUsageDescription` key in `Info.plist`. The App Store rejects an app that touches this resource without one.");
        }
        if (!handWritten.contains("ios.NSSpeechRecognitionUsageDescription")) {
        set("{{#IosPrivacy#ios.NSSpeechRecognitionUsageDescription}}.label", "Speech recognition usage description");
        set("{{#IosPrivacy#ios.NSSpeechRecognitionUsageDescription}}.type", "TextField");
        set("{{#IosPrivacy#ios.NSSpeechRecognitionUsageDescription}}.description", "Why the app sends speech for recognition. Supplied automatically when the app references the speech APIs; set it to say something more specific.");
        }

        set("{{@Build}}.label", "General");
        if (!handWritten.contains("facebook.appId")) {
        set("{{#Build#facebook.appId}}.label", "Facebook app id");
        set("{{#Build#facebook.appId}}.type", "TextField");
        set("{{#Build#facebook.appId}}.description", "The application ID for an app that requires native Facebook login integration, this defaults to null which means native Facebook support shouldn't be in the app");
        }
        if (!handWritten.contains("gcm.sender_id")) {
        set("{{#Build#gcm.sender_id}}.label", "Gcm sender id");
        set("{{#Build#gcm.sender_id}}.type", "TextField");
        set("{{#Build#gcm.sender_id}}.description", "The Android/chrome push identifier, see the push section for more details");
        }
        if (!handWritten.contains("nativeTheme")) {
        set("{{#Build#nativeTheme}}.label", "Native theme");
        set("{{#Build#nativeTheme}}.type", "Select");
        set("{{#Build#nativeTheme}}.values", "modern,legacy,custom");
        set("{{#Build#nativeTheme}}.description", "`modern`, `legacy`, `custom` (default unset). Cross-platform override that sets both `ios.themeMode` and `and.themeMode` together when those aren't set explicitly. `modern` = liquid glass + Material 3, `legacy` = iOS 7 flat + Holo Light, `custom` disables the framework native theme entirely. The legacy alias `cn1.nativeTheme` is still accepted.");
        }
        if (!handWritten.contains("noExtraResources")) {
        set("{{#Build#noExtraResources}}.label", "No extra resources");
        set("{{#Build#noExtraResources}}.type", "Checkbox");
        set("{{#Build#noExtraResources}}.description", "true/false (defaults to false). Blocks codename one from injecting its own resources when set to true, the only effect this has is in slightly reducing archive size. This might have adverse effects on some features of Codename One so it isn't recommended.");
        }
    }

    /** Idempotent setter: does not overwrite user or project-level metadata. */
    private static void set(String suffix, String value) {
        String key = "codename1.arg." + suffix;
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
