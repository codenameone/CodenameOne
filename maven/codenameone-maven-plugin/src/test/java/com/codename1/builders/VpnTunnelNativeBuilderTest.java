/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.builders;

import com.codename1.util.IOSVpnTunnelExtensionBuilder;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the build decides about the iOS packet-tunnel extension target.
 *
 * <p>{@code VpnTunnelExtensionTest} covers the Objective-C the generator
 * writes; this covers everything around it -- when a target is generated at
 * all, what it is translated from, and the three build settings that are the
 * difference between a target that compiles and one that cannot.</p>
 *
 * <p>None of it is compiled anywhere in CI. The generated provider is checked
 * against the real SDK by {@code scripts/check-vpn-tunnel-extension-
 * compiles.sh} on a machine with Xcode; the target around it is proved only
 * by an opt-in build.</p>
 */
class VpnTunnelNativeBuilderTest {

    private static BuildRequest request(String enabled, String tunnelClass) {
        BuildRequest request = new BuildRequest();
        request.setPackageName("com.example.app");
        request.setMainClass("MyApp");
        if (enabled != null) {
            request.putArgument(VpnTunnelNativeBuilder.HINT_ENABLED, enabled);
        }
        if (tunnelClass != null) {
            request.putArgument(VpnTunnelNativeBuilder.HINT_CLASS, tunnelClass);
        }
        return request;
    }

    @Test
    void aClassReferenceAloneGeneratesNothing() {
        // The entitlement this extension carries is one Apple grants case by
        // case, so an App ID without it fails codesigning with a message
        // naming the entitlement and not the reason it appeared. Referencing
        // the package must therefore never be enough on its own.
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        builder.parseHints(request(null, null), true);
        assertFalse(builder.isEnabled());
        builder.parseHints(request("false", "com.example.MyTunnel"), true);
        assertFalse(builder.isEnabled());
    }

    @Test
    void theTunnelClassHasToBeNamed() {
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        // Named rather than discovered: VpnTunnel is a class, so the shared
        // scanner skips it, and an app may have several subclasses while an
        // extension runs exactly one.
        BuildException refused = assertThrows(BuildException.class,
                () -> builder.parseHints(request("true", null), true));
        assertTrue(refused.getMessage().contains(
                VpnTunnelNativeBuilder.HINT_CLASS));
    }

    @Test
    void theHintWithoutThePackageIsRefused() {
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        BuildException refused = assertThrows(BuildException.class,
                () -> builder.parseHints(
                        request("true", "com.example.MyTunnel"), false));
        assertTrue(refused.getMessage().contains("com.codename1.vpn.tunnel"),
                "an extension that would run nothing has to be refused rather"
                + " than generated");
    }

    @Test
    void aNamedTunnelEnablesTheExtension() {
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        builder.parseHints(request("true", "  com.example.MyTunnel  "), true);
        assertTrue(builder.isEnabled());
        // TRIMMED. The name reaches the generated provider as a mangled C
        // symbol and the generated stub as a constructor call, so a padded
        // value would produce a symbol nothing defines.
        assertEquals("com.example.MyTunnel", builder.getTunnelClass());
    }

    @Test
    void theExtensionIsATranslationOfItsOwn() {
        // Rooted at the tunnel rather than shared with the app. This is the
        // difference between a target that compiles and one that cannot: the
        // application's translation reaches the port's UIKit natives, which
        // an APPLICATION_EXTENSION_API_ONLY target may not compile.
        assertEquals("MyAppVpnTunnel",
                VpnTunnelNativeBuilder.translationRoot("MyApp"));
        assertEquals("MyAppVpnTunnelStub",
                VpnTunnelNativeBuilder.stubClass("MyApp"));
        assertFalse(VpnTunnelNativeBuilder.SRC_DIR.equals(
                WatchNativeBuilder.WATCH_SRC_DIR),
                "the two second translations cannot stage into one directory");
    }

    @Test
    void theExtensionCompilesTheProgramAndNotThePort() throws Exception {
        // Derived rather than listed, and the difference is a link error.
        // This began as a list of the port sources that call
        // UIApplicationMain or [UIApplication sharedApplication] -- the ones
        // an APPLICATION_EXTENSION_API_ONLY target cannot compile. That list
        // excluded IOSNative.m and still compiled fifteen port sources that
        // reference symbols IOSNative.m defines (toNSString,
        // nsDataToByteArr, scaleValue, displayWidth, repaintUI), so the
        // target would have failed to link -- on a machine none of our tests
        // run on, since nothing here compiles it.
        File portDir = Files.createTempDirectory("cn1port").toFile();
        for (String name : new String[] {"IOSNative.m", "CN1Vpn.m",
                "CodenameOne_GLAppDelegate.m", "CN1MetalShaders.metal",
                "CN1Bluetooth.m"}) {
            assertTrue(new File(portDir, name).createNewFile());
        }
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        builder.recordPortNatives(portDir);

        assertTrue(builder.isExcluded("IOSNative.m"));
        assertTrue(builder.isExcluded("CodenameOne_GLAppDelegate.m"));
        assertTrue(builder.isExcluded("CN1Bluetooth.m"),
                "not a UIApplication caller, and excluded anyway: it uses"
                + " toNSString, which IOSNative.m defines");
        assertTrue(builder.isExcluded("CN1Vpn.m"));
        assertTrue(builder.isExcluded("CN1MetalShaders.metal"));

        // The ParparVM runtime comes from the translator rather than the
        // port, so it is not in that directory and is never excluded --
        // which matters, because the extension is nothing without it.
        assertFalse(builder.isExcluded("cn1_globals.m"));
        assertFalse(builder.isExcluded("nativeMethods.m"));
        assertFalse(builder.isExcluded("java_io_File.m"));
        // And the translated program itself.
        assertFalse(builder.isExcluded("com_example_app_MyTunnel.m"));
        assertFalse(builder.isExcluded(
                "com_codename1_impl_vpn_ExtensionTunnelHost.m"));
    }

    @Test
    void anUnrecordedPortIsRefusedRatherThanCompiledIn() {
        // Answering "exclude nothing" would compile the whole port into the
        // extension and fail at link.
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        BuildException refused = assertThrows(BuildException.class,
                () -> builder.recordPortNatives(
                        new File("no-such-directory-here")));
        assertTrue(refused.getMessage().contains("native"));
    }

    @Test
    void theTargetIsBuiltAsAnAppExtension() {
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        BuildRequest request = request("true", "com.example.app.MyTunnel");
        builder.parseHints(request, true);
        Map<String, String> settings = builder.buildSettings(request, "1,2");

        // The point of the target, stated rather than inherited.
        assertEquals("YES", settings.get("APPLICATION_EXTENSION_API_ONLY"));
        // An .appex has no main(). The translated tree carries one from the
        // generated stub, and this is what stops the loader running it.
        assertTrue(settings.get("OTHER_LDFLAGS").contains("-e _NSExtensionMain"));
        // OFF, matching the app target: the port, the translated sources and
        // the generated provider are all manual-retain.
        assertEquals("NO", settings.get("CLANG_ENABLE_OBJC_ARC"));
        // Without this the app half is compiled out, so an app whose
        // extension IS generated would report the capability absent.
        assertTrue(settings.get("GCC_PREPROCESSOR_DEFINITIONS")
                .contains("CN1_VPN_TUNNEL=1"));
        assertTrue(settings.get("GCC_PREPROCESSOR_DEFINITIONS")
                .contains("CN1_APP_EXTENSION=1"));
        assertEquals(IOSVpnTunnelExtensionBuilder.bundleId("com.example.app"),
                settings.get("PRODUCT_BUNDLE_IDENTIFIER"));
        // Its own prefix header, in its own tree. Compiled with the app's,
        // every source here would resolve "cn1_class_method_index.h" against
        // the APP's tree -- an index that does not declare the ids this
        // translation just generated.
        assertTrue(settings.get("GCC_PREFIX_HEADER")
                .contains(VpnTunnelNativeBuilder.SRC_DIR));
    }
}
