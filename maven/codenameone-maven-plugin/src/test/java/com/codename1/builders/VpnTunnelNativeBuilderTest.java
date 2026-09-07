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
        builder.recordHandWrittenNatives(portDir);

        assertTrue(builder.isExcluded("IOSNative.m"));
        assertTrue(builder.isExcluded("CodenameOne_GLAppDelegate.m"));
        assertTrue(builder.isExcluded("CN1Bluetooth.m"),
                "not a UIApplication caller, and excluded anyway: it uses"
                + " toNSString, which IOSNative.m defines");
        assertTrue(builder.isExcluded("CN1Vpn.m"));
        assertTrue(builder.isExcluded("CN1MetalShaders.metal"));

        // The application's own natives arrive through a DIFFERENT root --
        // the translator copies every non-class file it walks, and the
        // tunnel pass is given the resource root too -- so both are
        // recorded. A NativeInterface the tunnel never mentions would
        // otherwise be compiled under APPLICATION_EXTENSION_API_ONLY and
        // could fail a valid build on somebody else's UIKit call.
        File appNatives = Files.createTempDirectory("cn1res").toFile();
        assertTrue(new File(appNatives, "MyAppNative.m").createNewFile());
        // NESTED, because unzip keeps a submitted archive's directories and
        // the translator descends into them and FLATTENS what it finds into
        // the translation's output -- so a native two directories down
        // arrives in the extension's tree beside the top-level ones.
        File nested = new File(appNatives, "ios/src".replace('/',
                File.separatorChar));
        assertTrue(nested.mkdirs());
        assertTrue(new File(nested, "DeepNative.m").createNewFile());
        // ...and a .bundle is copied as a directory rather than flattened,
        // so nothing inside one can become a source this target compiles.
        File bundle = new File(appNatives, "Assets.bundle");
        assertTrue(bundle.mkdirs());
        assertTrue(new File(bundle, "inside.m").createNewFile());

        builder.recordHandWrittenNatives(portDir, appNatives);
        assertTrue(builder.isExcluded("MyAppNative.m"));
        assertTrue(builder.isExcluded("DeepNative.m"));
        assertFalse(builder.isExcluded("inside.m"),
                "a .bundle's contents never become sources");

        // The ParparVM runtime comes from the translator rather than either
        // root, so it is in neither directory and is never excluded --
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
    void aNameTheTranslatorAlsoEmitsIsDecidedByItsBytes() throws Exception {
        // An application native may be called nativeMethods.m. The
        // translator emits a file of that name itself, and the one that
        // survives in the translation is the translator's -- the copy was
        // overwritten. Excluded on the name alone, the extension was staged
        // without a runtime source it has to link, and said so as a missing
        // symbol on a machine none of our tests run on.
        File root = Files.createTempDirectory("cn1natives").toFile();
        File app = new File(root, "app");
        assertTrue(app.mkdirs());
        File theirs = new File(app, "nativeMethods.m");
        write(theirs, "// the application's own\n");

        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        builder.parseHints(request("true", "com.example.app.MyTunnel"), true);
        builder.recordHandWrittenNatives(app);

        // The name is recorded either way; the content decides.
        assertTrue(builder.isExcluded("nativeMethods.m"),
                "the basename is what narrows it");
        File staged = new File(root, "nativeMethods.m");
        write(staged, "// the translator's runtime\n");
        assertFalse(builder.isExcluded(staged),
                "a file the translator emitted has to reach the extension");
        File copied = new File(root, "copy");
        assertTrue(copied.mkdirs());
        File same = new File(copied, "nativeMethods.m");
        write(same, "// the application's own\n");
        assertTrue(builder.isExcluded(same),
                "and the application's own copy still does not");
    }

    @Test
    void anUnrecordedPortIsRefusedRatherThanCompiledIn() {
        // Answering "exclude nothing" would compile the whole port into the
        // extension and fail at link.
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        BuildException refused = assertThrows(BuildException.class,
                () -> builder.recordHandWrittenNatives(
                        new File("no-such-directory-here")));
        assertTrue(refused.getMessage().contains("native"));
    }

    @Test
    void theStubSpellsANestedTunnelTheWayJavaDoes() {
        // The hint is a BINARY name -- what the class file is called, and
        // what verifyTunnelClass looks for -- and javac will not parse one.
        assertEquals("com.example.Outer.Tunnel",
                VpnTunnelNativeBuilder.sourceName("com.example.Outer$Tunnel"));
        assertEquals("com.example.MyTunnel",
                VpnTunnelNativeBuilder.sourceName("com.example.MyTunnel"));
    }

    @Test
    void theExtensionSignsUnderOneIdentifierEverywhere() {
        // Three things have to agree: the target, the profile check and the
        // CN1VpnTunnelExtensionIdentifier the host plist carries, which
        // CN1Vpn.m puts in providerBundleIdentifier. Resolved once, so an
        // override cannot make them disagree.
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        BuildRequest plain = request("true", "com.example.app.MyTunnel");
        builder.parseHints(plain, true);
        assertEquals("com.example.app.vpntunnel", builder.bundleId(plain));

        BuildRequest overridden = request("true", "com.example.app.MyTunnel");
        overridden.putArgument("ios.vpn.tunnel.buildSettings"
                + ".PRODUCT_BUNDLE_IDENTIFIER", "com.example.app.tunnel");
        assertEquals("com.example.app.tunnel", builder.bundleId(overridden));

        BuildRequest substituted = request("true", "com.example.app.MyTunnel");
        substituted.putArgument("ios.vpn.tunnel.buildSettings"
                + ".PRODUCT_BUNDLE_IDENTIFIER",
                "$(APP_BUNDLE_IDENTIFIER).vpntunnel");
        assertTrue(assertThrows(BuildException.class,
                () -> builder.bundleId(substituted)).getMessage()
                        .contains("substitutions"),
                "Xcode expands $(...) for its own target and nothing expands"
                + " it in the host plist or the profile check");

        BuildRequest outside = request("true", "com.example.app.MyTunnel");
        outside.putArgument("ios.vpn.tunnel.buildSettings"
                + ".PRODUCT_BUNDLE_IDENTIFIER", "com.other.tunnel");
        assertTrue(assertThrows(BuildException.class,
                () -> builder.bundleId(outside)).getMessage()
                        .contains("com.example.app"),
                "Apple rejects an embedded extension outside its host's"
                + " namespace, at upload and long after this build");
    }

    @Test
    void theTunnelClassHasToBeOneTheTranslatorParses() throws Exception {
        // A LOOSE class file. This briefly accepted one inside a submitted
        // jar, because foldInCallAndVpnLibraryUsage recognises library-only
        // tunnel usage -- but ByteCodeTranslator.execute() parses *.class
        // and COPIES every other file it walks, so a class that exists only
        // inside an archive is never translated and the provider calls an
        // allocator the extension has no definition of. Accepting it moved
        // the failure from a sentence about the hint to a link error.
        File classes = Files.createTempDirectory("cn1classes").toFile();
        File pkg = new File(classes, "com/example/app".replace('/',
                File.separatorChar));
        assertTrue(pkg.mkdirs());
        assertTrue(new File(pkg, "MyTunnel.class").createNewFile());

        VpnTunnelNativeBuilder loose = new VpnTunnelNativeBuilder(null);
        loose.parseHints(request("true", "com.example.app.MyTunnel"), true);
        loose.verifyTunnelClass(classes);

        VpnTunnelNativeBuilder missing = new VpnTunnelNativeBuilder(null);
        missing.parseHints(request("true", "com.example.lib.LibTunnel"), true);
        BuildException refused = assertThrows(BuildException.class,
                () -> missing.verifyTunnelClass(classes));
        assertTrue(refused.getMessage().contains("com.example.lib.LibTunnel"));
        assertTrue(refused.getMessage().contains("library jar"),
                "the message has to say what to do about it");
    }

    @Test
    void aTunnelInTheDefaultPackageIsRefused() {
        // The stub is generated into the application's package, and java in
        // a named package cannot name a class in the default one -- so
        // "Tunnel" would compile as <app package>.Tunnel and fail on a class
        // the developer never wrote. The refusal names the hint; the compile
        // error would not have.
        VpnTunnelNativeBuilder builder = new VpnTunnelNativeBuilder(null);
        builder.parseHints(request("true", "Tunnel"), true);
        BuildException refused = assertThrows(BuildException.class,
                () -> builder.verifyTunnelClass(new File(".")));
        assertTrue(refused.getMessage().contains("no package"),
                refused.getMessage());
    }

    @Test
    void theHostEntitlementHasToCarryTheTunnelValue() {
        // The renderer splits an array-valued hint on newlines and trims, so
        // a project asking for two provider kinds writes two lines and must
        // still be accepted. What is refused is a hint that leaves out the
        // value this feature IS.
        assertTrue(IPhoneBuilder.entitlementArrayDeclares(
                "packet-tunnel-provider", "packet-tunnel-provider"));
        assertTrue(IPhoneBuilder.entitlementArrayDeclares(
                "app-proxy-provider\npacket-tunnel-provider",
                "packet-tunnel-provider"));
        assertTrue(IPhoneBuilder.entitlementArrayDeclares(
                " packet-tunnel-provider \n", "packet-tunnel-provider"));
        assertFalse(IPhoneBuilder.entitlementArrayDeclares(
                "app-proxy-provider", "packet-tunnel-provider"));
        assertFalse(IPhoneBuilder.entitlementArrayDeclares(
                "packet-tunnel-provider-x", "packet-tunnel-provider"),
                "a value that merely contains ours is not ours");
        assertFalse(IPhoneBuilder.entitlementArrayDeclares(
                null, "packet-tunnel-provider"));
    }

    @Test
    void theGeneratedTargetIsWrittenToDisk() throws Exception {
        // The only unconditional save in the schemes script belongs to the
        // brought-in .ios.appext fragment and runs BEFORE the generated
        // extensions; the matter, widget and document-provider helpers save
        // after their own work. A build whose only generated extension is
        // the tunnel therefore mutated the project in memory and wrote none
        // of it -- no target, no dependency, no embed phase -- and shipped
        // an app with no extension in it.
        String source = new String(java.nio.file.Files.readAllBytes(
                new File("src/main/java/com/codename1/builders/"
                        + "IPhoneBuilder.java").toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        int method = source.indexOf(
                "private void appendVpnTunnelExtensionTarget(StringBuilder sb,");
        assertTrue(method > 0, "the tunnel target generator has to exist");
        int next = source.indexOf("\n    /**", method);
        assertTrue(next > method);
        assertTrue(source.substring(method, next)
                        .contains("xcproj.save(project_file)"),
                "the fragment has to save the project it mutated");
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

    /** One small file, written whole. */
    private static void write(File f, String text) throws Exception {
        java.io.FileOutputStream out = new java.io.FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
