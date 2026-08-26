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

/// On-device debugging build hints for iOS and Android.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the builder's own default applies. Each attribute's `@Hint(def)` records
/// what that default is; the `default` clause below it is a neutral placeholder
/// with no meaning at runtime.
@Hint(consumedBy = {"IPhoneBuilder"})
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface OnDeviceDebug {

    @Hint(name = "android.onDeviceDebug",
            def = "false",
            platform = "android",
            doc = "Boolean true/false defaults to false. When `true`, the generated `AndroidManifest.xml` is marked `android:debuggable=\"true\"`, R8/proguard is disabled, and the build is pinned to debug-only (`android.release` is forced off and `android.debug` is forced on) so a stray hint can't ship a release-signed APK that's `debuggable=\"true\"`. Pair with the `cn1:android-on-device-debugging` Maven goal (or the bundled IntelliJ run configs) to install, launch, forward JDWP, and stream logcat through adb. Has no effect on builds that don't carry it -- release builds are unaffected. See the On-Device Debugging (Android) chapter for the full flow.",
            consumedBy = {"AndroidGradleBuilder", "CN1BuildMojo"})
    boolean android() default false;

    @Hint(name = "ios.onDeviceDebug",
            def = "false",
            platform = "ios",
            doc = "Boolean true/false defaults to false. When `true`, the iOS build links a small JDWP listener thread (`cn1_debugger`) into the binary and the ParparVM translator emits source-line and locals metadata so a desktop proxy can serve the running app to any JDWP-speaking debugger. Has no effect on release builds. See the On-Device Debugging (iOS) chapter for the full flow.")
    boolean ios() default false;

    @Hint(name = "ios.onDeviceDebug.proxyHost",
            def = "127.0.0.1",
            platform = "ios",
            doc = "Hostname or IP address the device-side listener dials to reach the desktop proxy. Default `127.0.0.1` (correct for the native iOS simulator). For a physical device, set this to the developer laptop's LAN IP. Has no effect unless `ios.onDeviceDebug=true`.")
    String iosProxyHost() default "";

    @Hint(name = "ios.onDeviceDebug.proxyPort",
            def = "55333",
            platform = "ios",
            doc = "TCP port on `ios.onDeviceDebug.proxyHost` where the proxy is listening for the device. Default `55333`. Has no effect unless `ios.onDeviceDebug=true`.")
    int iosProxyPort() default 0;

    @Hint(name = "ios.onDeviceDebug.waitForAttach",
            def = "false",
            platform = "ios",
            doc = "Boolean true/false defaults to false. When `true`, the app blocks at startup until the proxy connects and the IDE tells the VM to continue. Useful when the breakpoint to investigate fires during app boot. Has no effect unless `ios.onDeviceDebug=true`.")
    boolean iosWaitForAttach() default false;
}
