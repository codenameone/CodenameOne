# The curated set: hints exposed as typed annotation attributes.
#
# name -> (group, attribute, enum-or-None, forced-type-or-None, forced-default-or-None)
#
# TYPE_OVERRIDES below carries the hints whose type cannot be inferred from a
# literal default -- the call site computes one -- but whose type is unambiguous
# in the code that reads it. Without them a boolean hint is exposed as a String
# attribute, which is barely better than the properties file.
#
# Every enum domain below was read off the code that consumes the hint, not
# guessed from a name or a doc sentence. Where the consumer accepts alias
# spellings (ios.themeMode takes "liquid" for "modern"), the enum exposes the
# canonical spelling only: on the annotation path an over-tight domain costs a
# fallback to the properties file, never a broken build, and the aliases stay
# reachable there.

ENUMS = {
    # HardeningPreflight.java:90 rejects anything else outright.
    "HardenLevel":        ["off", "standard", "aggressive", "paranoid"],
    "HardenStrings":      ["off", "constants", "all"],
    "HardenControlFlow":  ["off", "on"],
    # BuildHintSchemaDefaults.registerNativeTheme + JavaSEPort.resolveAutoNativeTheme
    "NativeThemeMode":    ["modern", "legacy", "custom"],
    "IosThemeMode":       ["auto", "modern", "ios7", "legacy"],
    "AndroidThemeMode":   ["auto", "modern", "hololight", "legacy"],
    # GenerateDesktopAppWrapperMojo.sanitizeTitleBarMode warns and silently
    # falls back to "native" on anything else -- the exact silent-typo case.
    "DesktopTitleBar":    ["native", "custom", "toolbar"],
    # IOSDependencyManager.fromHint throws on anything else.
    "IosDependencyManager": ["auto", "cocoapods", "spm", "both", "none"],
    # "one of ios, ipad, iphone (defaults to ios)" -- the guide states it and
    # the value is passed straight through to ParparVM (IPhoneBuilder.java:4572).
    "IosProjectType":     ["ios", "ipad", "iphone"],
    # The three values android:installLocation itself accepts.
    "InstallLocation":    ["auto", "internalOnly", "preferExternal"],
}

CURATED = {
 # ---- @Ios -------------------------------------------------------------
 "ios.newStorageLocation":        ("IOS", "newStorageLocation", None, None, None),
 "ios.deployment_target":         ("IOS", "deploymentTarget", None, "VERSION", None),
 "ios.minDeploymentTarget":       ("IOS", "minDeploymentTarget", None, "VERSION", "6.0"),
 "ios.teamId":                    ("IOS", "teamId", None, None, None),
 "ios.includePush":               ("IOS", "includePush", None, None, None),
 "ios.add_libs":                  ("IOS", "addLibs", None, None, None),
 "ios.pods":                      ("IOS", "pods", None, None, None),
 "ios.pods.platform":             ("IOS", "podsPlatform", None, "VERSION", None),
 "ios.pods.sources":              ("IOS", "podsSources", None, None, None),
 "ios.applicationQueriesSchemes": ("IOS", "applicationQueriesSchemes", None, None, None),
 "ios.objC":                      ("IOS", "objC", None, None, None),
 "ios.plistInject":               ("IOS", "plistInject", None, None, None),
 "ios.glAppDelegateHeader":       ("IOS", "glAppDelegateHeader", None, None, None),
 "ios.beforeFinishLaunching":     ("IOS", "beforeFinishLaunching", None, None, None),
 "ios.themeMode":                 ("IOS", "themeMode", "IosThemeMode", None, None),
 "ios.interface_orientation":     ("IOS", "interfaceOrientation", None, None, None),
 "ios.project_type":              ("IOS", "projectType", "IosProjectType", None, None),
 "ios.prerendered_icon":          ("IOS", "prerenderedIcon", None, None, None),
 "ios.uiscene":                   ("IOS", "uiscene", None, None, None),
 "ios.urlScheme":                 ("IOS", "urlScheme", None, None, None),
 "ios.dependencyManager":         ("IOS", "dependencyManager", "IosDependencyManager", None, None),
 "ios.bundleVersion":             ("IOS", "bundleVersion", None, "VERSION", None),
 "ios.spm.packages":              ("IOS", "spmPackages", None, None, None),
 # ---- @Android ---------------------------------------------------------
 "android.min_sdk_version":       ("ANDROID", "minSdkVersion", None, None, None),
 "android.targetSDKVersion":      ("ANDROID", "targetSDKVersion", None, None, None),
 "android.buildToolsVersion":     ("ANDROID", "buildToolsVersion", None, None, None),
 "android.xpermissions":          ("ANDROID", "xpermissions", None, None, None),
 "android.xapplication":          ("ANDROID", "xapplication", None, None, None),
 "android.gradleDep":             ("ANDROID", "gradleDep", None, None, None),
 "android.proguardKeep":          ("ANDROID", "proguardKeep", None, None, None),
 "android.release":               ("ANDROID", "release", None, None, None),
 "android.debug":                 ("ANDROID", "debug", None, None, None),
 "android.useAndroidX":           ("ANDROID", "useAndroidX", None, None, None),
 "android.licenseKey":            ("ANDROID", "licenseKey", None, None, None),
 "android.installLocation":       ("ANDROID", "installLocation", "InstallLocation", None, None),
 "android.activity.launchMode":   ("ANDROID", "activityLaunchMode", None, None, None),
 "and.themeMode":                 ("ANDROID", "themeMode", "AndroidThemeMode", None, None),
 "android.appBundle":             ("ANDROID", "appBundle", None, None, None),
 "android.disableR8":             ("ANDROID", "disableR8", None, None, None),
 "android.enableProguard":        ("ANDROID", "enableProguard", None, None, None),
 "android.newFirebaseMessaging":  ("ANDROID", "newFirebaseMessaging", None, None, None),
 "android.multidex":              ("ANDROID", "multidex", None, None, None),
 "android.captureRecord":         ("ANDROID", "captureRecord", None, None, None),
 "android.hideStatusBar":         ("ANDROID", "hideStatusBar", None, None, None),
 "android.repositories":          ("ANDROID", "repositories", None, None, None),
 "android.topDependency":         ("ANDROID", "topDependency", None, None, None),
 "android.xgradle":               ("ANDROID", "xgradle", None, None, None),
 # ---- @Desktop ---------------------------------------------------------
 "desktop.titleBar":              ("DESKTOP", "titleBar", "DesktopTitleBar", None, None),
 "desktop.interactiveScrollbars": ("DESKTOP", "interactiveScrollbars", None, None, None),
 "desktop.width":                 ("DESKTOP", "width", None, "INT", None),
 "desktop.height":                ("DESKTOP", "height", None, "INT", None),
 "desktop.resizable":             ("DESKTOP", "resizable", None, None, None),
 "desktop.fullscreen":            ("DESKTOP", "fullscreen", None, None, None),
 "desktop.adaptToRetina":         ("DESKTOP", "adaptToRetina", None, None, None),
 # ---- @Hardening -------------------------------------------------------
 "harden.level":                  ("HARDENING", "level", "HardenLevel", None, None),
 "harden.strings":                ("HARDENING", "strings", "HardenStrings", None, None),
 "harden.controlFlow":            ("HARDENING", "controlFlow", "HardenControlFlow", None, None),
 "harden.rename":                 ("HARDENING", "rename", None, None, None),
 "harden.keep":                   ("HARDENING", "keep", None, "TEXT_BLOCK", None),
 "harden.allowUnhardenedLocalBuild": ("HARDENING", "allowUnhardenedLocalBuild", None, None, None),
 # ---- @OnDeviceDebug ---------------------------------------------------
 "ios.onDeviceDebug":                ("ON_DEVICE_DEBUG", "ios", None, None, None),
 "ios.onDeviceDebug.proxyHost":      ("ON_DEVICE_DEBUG", "iosProxyHost", None, None, None),
 "ios.onDeviceDebug.proxyPort":      ("ON_DEVICE_DEBUG", "iosProxyPort", None, "INT", None),
 "ios.onDeviceDebug.waitForAttach":  ("ON_DEVICE_DEBUG", "iosWaitForAttach", None, None, None),
 "android.onDeviceDebug":            ("ON_DEVICE_DEBUG", "android", None, None, None),
 # ---- @Build -----------------------------------------------------------
 "nativeTheme":                   ("GENERAL", "nativeTheme", "NativeThemeMode", None, None),
 "gcm.sender_id":                 ("GENERAL", "gcmSenderId", None, None, None),
 "facebook.appId":                ("GENERAL", "facebookAppId", None, None, None),
 "noExtraResources":              ("GENERAL", "noExtraResources", None, None, None),
}

# ios.NS*UsageDescription -> @IosPrivacy, attribute is the key minus "ios.NS"
# with a lowercased first letter; the UsageDescription suffix is kept so the
# plist key it maps to is mechanically recoverable.
PRIVACY_PREFIX = "ios.NS"

# Hints whose default the mining cannot state in one value, resolved by reading
# the code rather than by picking whichever call site came first.
DEFAULT_NOTES = {
 "android.debug":
   "Defaults conditionally rather than to a fixed value: when android.release is on "
   "it defaults to false, and when release is off it defaults to true, so a build "
   "that selects neither still produces something installable "
   "(AndroidGradleBuilder.java:447-451).",
 "ios.minDeploymentTarget":
   "The null and empty-string reads of this hint are presence checks; 6.0 is the "
   "substantive default (IPhoneBuilder.java:4671).",
}

# Prose for curated hints the main developer-guide table does not describe.
# Sourced from the feature chapters (App-Hardening.asciidoc) or read off the
# code that consumes the hint. The privacy strings are generated mechanically
# by BuildHintCodeGenerator and are not listed here.
DOC_OVERRIDES = {
 "harden.level":
   "Master switch for app hardening: off, standard, aggressive or paranoid. An "
   "unrecognized value fails the build rather than being quietly treated as off.",
 "harden.rename":
   "Overrides symbol renaming independently of harden.level.",
 "harden.strings":
   "Overrides string obfuscation independently of harden.level: off, constants or all.",
 "harden.controlFlow":
   "Overrides control-flow obfuscation independently of harden.level.",
 "harden.keep":
   "Keep rules in ProGuard syntax, one per line, for classes that are resolved by "
   "name at runtime and so cannot be found by the automatic analysis. Same syntax "
   "as android.proguardKeep, so existing rules port directly. Rules are separated "
   "by newlines only, because a semicolon is legal inside a rule body such as "
   "{ *; }.",
 "harden.allowUnhardenedLocalBuild":
   "Permits a local or source build to run with hardening requested but not "
   "applied. Without it such a build is refused, so a hardened app is never "
   "shipped from a target that cannot actually harden it.",
 "desktop.titleBar":
   "How the desktop window is framed: native for the OS title bar and menu bar, "
   "custom for an undecorated window with a Codename One drawn title bar, or "
   "toolbar for the legacy in-app Toolbar. An unrecognized value falls back to "
   "native with a warning.",
 "desktop.interactiveScrollbars":
   "Enables grab-able, click-to-page desktop scrollbars.",
 "desktop.fullscreen":
   "Starts the desktop build in full-screen mode.",
 "ios.dependencyManager":
   "Which native dependency manager to use: auto picks one from whichever of "
   "ios.pods and ios.spm.packages is set, and cocoapods, spm or both require the "
   "matching hint to be set. An unrecognized value fails the build.",
 "ios.deployment_target":
   "Minimum iOS version the build targets. Set it to the lowest iOS you actually "
   "support; a higher value excludes older devices from the App Store listing.",
 "ios.pods.sources":
   "Extra CocoaPods spec repositories to search, in addition to the default trunk.",
 "ios.spm.packages":
   "Swift Package Manager packages to link, one per entry, each written as "
   "identity|url|requirement.",
 "android.appBundle":
   "Produces an Android App Bundle (.aab) rather than an APK. Required for new "
   "Play Store submissions.",
 "android.buildToolsVersion":
   "Android build-tools version. It also selects the compile SDK, so there is no "
   "separate compile-SDK hint.",
 "android.disableR8":
   "Turns off R8, falling back to the older shrinker. Note that hardening requires "
   "R8, so this conflicts with harden.level.",
 "android.gradleDep":
   "Gradle dependency statements to add to the app module, such as "
   "implementation 'com.example:lib:1.0'.",
 "android.topDependency":
   "Statements added to the top-level Gradle build file rather than the app module.",
 "android.repositories":
   "Extra Gradle repositories to resolve dependencies from.",
 "android.xgradle":
   "Arbitrary text spliced into the generated app-module Gradle file.",
 "android.hideStatusBar":
   "Hides the Android status bar.",
 "android.newFirebaseMessaging":
   "Uses the current Firebase Cloud Messaging integration. Requires AndroidX and "
   "Gradle 8.13 or newer.",
}


# hint -> (HintType, separator-or-None). Each verified at the call site, not guessed.
TYPE_OVERRIDES = {
 # request.getArg(...).equals("true"), with a computed rather than literal default
 "android.useAndroidX":      ("BOOLEAN", None),   # AndroidGradleBuilder.java:1169
 "android.appBundle":        ("BOOLEAN", None),   # AndroidGradleBuilder.java:1441
 "harden.rename":            ("BOOLEAN", None),   # hardenBoolArg(..., true)
 # version numbers whose default is computed from the installed toolchain
 "android.targetSDKVersion": ("INT", None),       # AndroidGradleBuilder.java:1401
 "android.buildToolsVersion": ("VERSION", None),  # AndroidGradleBuilder.java:1186
 # split by the consumer, so they are lists even though no merger entry exists
 "ios.spm.packages":         ("STRING_LIST", ";"),  # IOSDependencyManager.java:119 split("[;]")
 "ios.pods.sources":         ("STRING_LIST", ","),  # IPhoneBuilder.java:5159 split("[;,]")
 # free text that is expected to span lines
 "ios.beforeFinishLaunching": ("TEXT_BLOCK", None),
 "ios.glAppDelegateHeader":   ("TEXT_BLOCK", None),
}
