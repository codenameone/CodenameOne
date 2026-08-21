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

import com.codename1.util.IOSWidgetExtensionBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The watchOS complication extension is embedded in the WATCH app, not the phone app. That one
/// choice is what makes the companion and standalone distributions need no separate handling:
/// the companion case already copies the finished watch app into the phone app, .appex and all,
/// and the standalone case ships the watch app as the product.
class WatchWidgetExtensionTargetTest {

    private static final String WATCH_MAIN = "com.mycompany.myapp.MyWatchMain";

    private static BuildRequest request() {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.setPackageName("com.mycompany.myapp");
        req.setDisplayName("My App");
        req.setVersion("1.0");
        return req;
    }

    private static WatchNativeBuilder parse(BuildRequest req) {
        WatchNativeBuilder b = new WatchNativeBuilder(new IPhoneBuilder());
        b.parseHints(req);
        return b;
    }

    /// Writes a realistic extension folder so the script generator has files to reference.
    private static File extensionDir(Path tmp) throws Exception {
        File dist = new File(tmp.toFile(), "dist");
        File dir = new File(dist, IPhoneBuilder.SURFACES_WATCH_EXTENSION_NAME);
        dir.mkdirs();
        IOSWidgetExtensionBuilder b = new IOSWidgetExtensionBuilder()
                .setWatchTarget(true)
                .setExtensionName(IPhoneBuilder.SURFACES_WATCH_EXTENSION_NAME)
                .setHostBundleId("com.mycompany.myapp.watchkitapp")
                .setAppGroupId("group.com.mycompany.myapp")
                .addKind(new IOSWidgetExtensionBuilder.Kind("status")
                        .setIosFamilies(Arrays.asList("watchCircular")));
        for (java.util.Map.Entry<String, byte[]> e : b.buildFileMap().entrySet()) {
            Files.write(new File(dir, e.getKey()).toPath(), e.getValue());
        }
        return dir;
    }

    private static String script(BuildRequest req, Path tmp, boolean withExtension)
            throws Exception {
        WatchNativeBuilder b = parse(req);
        if (withExtension) {
            b.setWidgetExtension(extensionDir(tmp), "group.com.mycompany.myapp", "10.0");
        }
        return b.buildXcodeScript(req, tmp.toFile(), "1.0", new ArrayList<String>());
    }

    /// :watch2_extension is the LEGACY paired WatchKit app extension -- the same trap as
    /// :application vs :watch2_app for the app target itself. A WidgetKit extension is a plain
    /// app extension on every platform Apple ships it on.
    @Test
    void theExtensionIsAnAppExtensionNotAWatchKitOne(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String ruby = script(req, tmp, true);

        assertTrue(ruby.contains("xcproj.new_target(:app_extension, 'CN1WatchWidgets', :watchos"),
                ruby);
        assertFalse(ruby.contains("watch2_extension"), ruby);
    }

    /// PlugIns of the WATCH app. Embedding it in the phone app instead would put a watchOS
    /// binary in an iOS bundle, and would need a separate answer for the standalone case.
    @Test
    void theExtensionIsEmbeddedInTheWatchApp(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String ruby = script(req, tmp, true);

        assertTrue(ruby.contains("watch_target.add_dependency(ext_target)"), ruby);
        assertTrue(ruby.contains("watch_target.new_copy_files_build_phase("
                + "'Embed Foundation Extensions')"), ruby);
        assertTrue(ruby.contains("ext_embed.dst_subfolder_spec = \"13\""), ruby);
    }

    /// The whole point of embedding in the watch app: neither distribution needs a branch.
    @Test
    void theSameFragmentServesCompanionAndStandalone(@TempDir Path tmp) throws Exception {
        BuildRequest companion = request();
        companion.putArgument("watchMain", WATCH_MAIN);
        BuildRequest standalone = request();
        standalone.putArgument("watchMain", WATCH_MAIN);
        standalone.putArgument("watchStandalone", "true");

        String companionRuby = script(companion, tmp, true);
        String standaloneRuby = script(standalone, tmp, true);

        for (String marker : new String[] {
                "xcproj.new_target(:app_extension, 'CN1WatchWidgets', :watchos",
                "watch_target.add_dependency(ext_target)",
                "ext_embed.dst_subfolder_spec = \"13\"" }) {
            assertTrue(companionRuby.contains(marker), "companion: " + marker);
            assertTrue(standaloneRuby.contains(marker), "standalone: " + marker);
        }
    }

    /// A project that publishes no complication must produce the script it always did.
    @Test
    void noExtensionMeansNoFragment(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String ruby = script(req, tmp, false);

        assertFalse(ruby.contains("ext_target"), ruby);
        assertFalse(ruby.contains("Embed Foundation Extensions"), ruby);
        assertFalse(ruby.contains("CN1SurfaceBridge.swift"), ruby);
    }

    /// IOSNative reaches the Swift bridge through NSClassFromString, so the watch target has to
    /// compile it. Its own translation does not carry it -- only the phone's -src does -- so
    /// without this the watch finds no bridge and every surfaces native answers unsupported.
    @Test
    void theWatchTargetCompilesTheAppSideSurfacesGlue(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String ruby = script(req, tmp, true);

        assertTrue(ruby.contains("CN1SurfaceBridge.swift"), ruby);
        assertTrue(ruby.contains("CN1SurfaceConfig.swift"), ruby);
    }

    /// The watch bundle is signed with its own entitlements and inherits nothing from the phone,
    /// so the App Group it shares with its extension has to be granted here.
    @Test
    void publishingSurfacesEntitlesTheWatchAppGroup() {
        BuildRequest req = request();
        String plist = WatchNativeBuilder.watchEntitlementsPlist(req, "false", false,
                "group.com.mycompany.myapp");

        assertTrue(plist.contains("com.apple.security.application-groups"), plist);
        assertTrue(plist.contains("group.com.mycompany.myapp"), plist);
    }

    /// Granting a capability nothing uses is not harmless: entitlement validation refuses a
    /// signature carrying one the provisioning profile does not have.
    @Test
    void aSurfacesOnlyWatchIsNotGrantedHealthKit() {
        BuildRequest req = request();
        String plist = WatchNativeBuilder.watchEntitlementsPlist(req, "false", false,
                "group.com.mycompany.myapp");

        assertFalse(plist.contains("com.apple.developer.healthkit"), plist);
    }

    /// And the reverse: a HealthKit watch that publishes nothing keeps exactly the entitlements
    /// it had before complications existed.
    @Test
    void aHealthOnlyWatchIsUnchanged() {
        BuildRequest req = request();

        String before = WatchNativeBuilder.watchEntitlementsPlist(req, "false");
        String after = WatchNativeBuilder.watchEntitlementsPlist(req, "false", true, null);

        assertTrue(before.equals(after), "before:\n" + before + "\nafter:\n" + after);
        assertTrue(before.contains("com.apple.developer.healthkit"), before);
        assertFalse(before.contains("application-groups"), before);
    }

    /// The natives compare the running OS against this key. The iOS default of 16.1 compared
    /// against a watchOS version is never met, so a watch left on it reports no widget support
    /// however well everything else is wired.
    @Test
    void theWatchPlistAdvertisesItsOwnSurfacesFloor(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        WatchNativeBuilder b = parse(req);
        b.setWidgetExtension(extensionDir(tmp), "group.com.mycompany.myapp", "10.0");
        File srcDir = new File(tmp.toFile(), "src");
        srcDir.mkdirs();

        b.writeWatchInfoPlist(req, srcDir);
        String plist = new String(Files.readAllBytes(
                new File(srcDir, "MyApp-Watch-Info.plist").toPath()), StandardCharsets.UTF_8);

        assertTrue(plist.contains("<key>CN1SurfacesAppGroup</key>"), plist);
        assertTrue(plist.contains("group.com.mycompany.myapp"), plist);
        assertTrue(plist.contains("<key>CN1SurfacesMinOS</key>"), plist);
        assertTrue(plist.contains("<string>10.0</string>"), plist);
    }

    /// A watch app that publishes nothing must not carry either key.
    @Test
    void aWatchThatPublishesNothingCarriesNoSurfacesKeys(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        WatchNativeBuilder b = parse(req);
        File srcDir = new File(tmp.toFile(), "src");
        srcDir.mkdirs();

        b.writeWatchInfoPlist(req, srcDir);
        String plist = new String(Files.readAllBytes(
                new File(srcDir, "MyApp-Watch-Info.plist").toPath()), StandardCharsets.UTF_8);

        assertFalse(plist.contains("CN1SurfacesAppGroup"), plist);
        assertFalse(plist.contains("CN1SurfacesMinOS"), plist);
    }
}
