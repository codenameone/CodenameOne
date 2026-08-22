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
 * Hints with no platform prefix, plus hardening and on-device debugging.
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
final class BuildHintsGeneral {

    private BuildHintsGeneral() {
    }

    static void register(List<Hint> h) {
        h.add(new Hint("KeepScreenOn")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("android.onDeviceDebug")
                .annotatedAs(HintGroup.ON_DEVICE_DEBUG, "android")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("android")
                .consumedBy("AndroidGradleBuilder", "CN1BuildMojo")
                .doc("Boolean true/false defaults to false. When `true`, the generated `AndroidManifest.xml` "
                        + "is marked `android:debuggable=\"true\"`, R8/proguard is disabled, and the build is pinned "
                        + "to debug-only (`android.release` is forced off and `android.debug` is forced on) so a "
                        + "stray hint can't ship a release-signed APK that's `debuggable=\"true\"`. Pair with the "
                        + "`cn1:android-on-device-debugging` Maven goal (or the bundled IntelliJ run configs) to "
                        + "install, launch, forward JDWP, and stream logcat through adb. Has no effect on builds "
                        + "that don't carry it — release builds are unaffected. See the On-Device Debugging "
                        + "(Android) chapter for the full flow."));

        h.add(new Hint("androidx.appcompat.version")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("build.incSources")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("CN1BuildMojo"));

        h.add(new Hint("build.testReporter")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("build.unitTest")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("CN1BuildMojo"));

        h.add(new Hint("cn1.androidTheme")
                .aliasOf("and.themeMode")
                .deprecated("Use and.themeMode, or @Android(themeMode = ...).")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder")
                .doc("Deprecated alias for and.themeMode (AndroidGradleBuilder.java:4097). "
                        + "Both names configure one setting, so declaring this alongside "
                        + "@Android(themeMode) is a conflict."));

        h.add(new Hint("cn1.buildKey")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("cn1.entitled")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("cn1.harden.forceOff")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("cn1.hardenLevel")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("off")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "Executor"));

        h.add(new Hint("cn1.hardened")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "Executor"));

        h.add(new Hint("cn1.hardening.libraryJars")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("cn1.mappingId")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("cn1.nativeTheme")
                .aliasOf("nativeTheme")
                .deprecated("Use nativeTheme, or @Build(nativeTheme = ...).")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder")
                .doc("Deprecated alias for nativeTheme (AndroidGradleBuilder.java:4099, "
                        + "IPhoneBuilder.java:947). Both names configure one setting, so "
                        + "declaring this alongside @Build(nativeTheme) is a conflict."));

        h.add(new Hint("db.legacy")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor", "GenerateDesktopAppWrapperMojo"));

        h.add(new Hint("delayPushCompletion")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder"));

        h.add(new Hint("facebook.appId")
                .annotatedAs(HintGroup.GENERAL, "facebookAppId")
                .type(HintType.STRING)
                .def("706695982682332")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder")
                .doc("The application ID for an app that requires native Facebook login integration, this "
                        + "defaults to null which means native Facebook support shouldn't be in the app"));

        h.add(new Hint("facebook.clientToken")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder")
                .doc("The client token for an app that requires native Facebook login integration, this is "
                        + "required if the facebook.appId is set."));

        h.add(new Hint("gcm.sender_id")
                .annotatedAs(HintGroup.GENERAL, "gcmSenderId")
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder")
                .doc("The Android/chrome push identifier, see the push section for more details"));

        h.add(new Hint("google.adUnitId")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder")
                .doc("Allows integrating Admob/Google Play ads into the application see "
                        + "link:https://www.codenameone.com/blog/adding-google-play-ads.html[this]"));

        h.add(new Hint("gradleDependencies")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "MapsProviderInjector"));

        h.add(new Hint("harden.allowUnhardenedLocalBuild")
                .annotatedAs(HintGroup.HARDENING, "allowUnhardenedLocalBuild")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("CN1BuildMojo")
                .doc("Permits a local or source build to run with hardening requested but not applied. Without "
                        + "it such a build is refused, so a hardened app is never shipped from a target that "
                        + "can't actually harden it."));

        h.add(new Hint("harden.controlFlow")
                .annotatedAs(HintGroup.HARDENING, "controlFlow")
                .values("HardenControlFlow", "off", "on")
                .platform("general")
                .consumedBy("CN1BuildMojo")
                .doc("Overrides control-flow obfuscation independently of harden.level."));

        h.add(new Hint("harden.ios.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general")
                .consumedBy("CN1BuildMojo"));

        h.add(new Hint("harden.keep")
                .annotatedAs(HintGroup.HARDENING, "keep")
                .type(HintType.TEXT_BLOCK)
                .platform("general")
                .consumedBy("AndroidGradleBuilder")
                .doc("Keep rules in ProGuard syntax, one per line, for classes that are resolved by name at "
                        + "runtime and so can't be found by the automatic analysis. Same syntax as "
                        + "android.proguardKeep, so existing rules port directly. Rules are separated by newlines "
                        + "only, because a semicolon is legal inside a rule body such as { *; }."));

        h.add(new Hint("harden.level")
                .annotatedAs(HintGroup.HARDENING, "level")
                .values("HardenLevel", "off", "standard", "aggressive", "paranoid")
                .def("off")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "CN1BuildMojo", "Executor")
                .doc("Master switch for app hardening: off, standard, aggressive or paranoid. An unrecognized "
                        + "value fails the build rather than being treated as off."));

        h.add(new Hint("harden.mac.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general")
                .consumedBy("CN1BuildMojo"));

        h.add(new Hint("harden.rename")
                .annotatedAs(HintGroup.HARDENING, "rename")
                .type(HintType.BOOLEAN)
                .platform("general")
                .consumedBy("CN1BuildMojo")
                .doc("Overrides symbol renaming independently of harden.level."));

        h.add(new Hint("harden.strings")
                .annotatedAs(HintGroup.HARDENING, "strings")
                .values("HardenStrings", "off", "constants", "all")
                .platform("general")
                .consumedBy("CN1BuildMojo")
                .doc("Overrides string obfuscation independently of harden.level: off, constants or all."));

        h.add(new Hint("harden.tv.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general")
                .consumedBy("CN1BuildMojo"));

        h.add(new Hint("harden.watch.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general")
                .consumedBy("CN1BuildMojo"));

        h.add(new Hint("ios.onDeviceDebug")
                .annotatedAs(HintGroup.ON_DEVICE_DEBUG, "ios")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .consumedBy("IPhoneBuilder")
                .doc("Boolean true/false defaults to false. When `true`, the iOS build links a small JDWP "
                        + "listener thread (`cn1_debugger`) into the binary and the ParparVM translator emits "
                        + "source-line and locals metadata so a desktop proxy can serve the running app to any "
                        + "JDWP-speaking debugger. Has no effect on release builds. See the On-Device Debugging "
                        + "(iOS) chapter for the full flow."));

        h.add(new Hint("ios.onDeviceDebug.proxyHost")
                .annotatedAs(HintGroup.ON_DEVICE_DEBUG, "iosProxyHost")
                .type(HintType.STRING)
                .def("127.0.0.1")
                .platform("ios")
                .consumedBy("IPhoneBuilder")
                .doc("Hostname or IP address the device-side listener dials to reach the desktop proxy. "
                        + "Default `127.0.0.1` (correct for the native iOS simulator). For a physical device, set "
                        + "this to the developer laptop's LAN IP. Has no effect unless `ios.onDeviceDebug=true`."));

        h.add(new Hint("ios.onDeviceDebug.proxyPort")
                .annotatedAs(HintGroup.ON_DEVICE_DEBUG, "iosProxyPort")
                .type(HintType.INT)
                .def("55333")
                .platform("ios")
                .consumedBy("IPhoneBuilder")
                .doc("TCP port on `ios.onDeviceDebug.proxyHost` where the proxy is listening for the device. "
                        + "Default `55333`. Has no effect unless `ios.onDeviceDebug=true`."));

        h.add(new Hint("ios.onDeviceDebug.waitForAttach")
                .annotatedAs(HintGroup.ON_DEVICE_DEBUG, "iosWaitForAttach")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("ios")
                .consumedBy("IPhoneBuilder")
                .doc("Boolean true/false defaults to false. When `true`, the app blocks at startup until the "
                        + "proxy connects and the IDE tells the VM to continue. Useful when the breakpoint to "
                        + "investigate fires during app boot. Has no effect unless `ios.onDeviceDebug=true`."));

        h.add(new Hint("java.version")
                .group(HintGroup.GENERAL)
                .type(HintType.INT)
                .def("8")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "CN1BuildMojo", "CreateGameSceneMojo", "InstallCn1libsMojo", "OpenGameBuilderMojo")
                .doc("Valid values include 5 or 8. Indicates the JVM version that should be used for server "
                        + "compilation, this is defined by default for newly created apps based on the Java 8 mode "
                        + "selection"));

        h.add(new Hint("maps.provider")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("MapsProviderInjector"));

        h.add(new Hint("nativeTheme")
                .annotatedAs(HintGroup.GENERAL, "nativeTheme")
                .values("NativeThemeMode", "modern", "legacy", "custom")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder")
                .doc("`modern`, `legacy`, `custom` (default unset). Cross-platform override that sets both "
                        + "`ios.themeMode` and `and.themeMode` together when those aren't set explicitly. `modern` "
                        + "= liquid glass + Material 3, `legacy` = iOS 7 flat + Holo Light, `custom` disables the "
                        + "framework native theme entirely. The legacy alias `cn1.nativeTheme` is still accepted."));

        h.add(new Hint("noExtraResources")
                .annotatedAs(HintGroup.GENERAL, "noExtraResources")
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder")
                .doc("true/false (defaults to false). Blocks codename one from injecting its own resources "
                        + "when set to true, the only effect this has is in slightly reducing archive size. This "
                        + "might have adverse effects on some features of Codename One so it isn't recommended."));

        h.add(new Hint("requireKotlinStdlib")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder"));

        h.add(new Hint("tvMain")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("IPhoneBuilder", "TvNativeBuilder"));

        h.add(new Hint("vserv.allowSkipping")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.category")
                .group(HintGroup.GENERAL)
                .type(HintType.INT)
                .def("29")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.countryCode")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("null")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.locale")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("en_US")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.networkCode")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("null")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.scaleMode")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.transition")
                .group(HintGroup.GENERAL)
                .type(HintType.INT)
                .def("300000")
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("vserv.zone")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("Executor"));

        h.add(new Hint("watchMain")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "IPhoneBuilder", "WatchNativeBuilder"));

        h.add(new Hint("watchStandalone")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .consumedBy("AndroidGradleBuilder", "WatchNativeBuilder"));
    }
}
