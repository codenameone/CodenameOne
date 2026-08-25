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

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// Covers what a native macOS build emits, without Xcode and without a Mac.
///
/// The Catalyst path could not be tested this way: its settings were injected
/// into an already-generated iOS project by a Ruby script, so the only way to
/// see what a build produced was to run one. Everything here is a pure function
/// of the hints for exactly that reason.
public class MacOSXcodeProjectTest {

    private static MacOSBuildHints hints(final Map<String, String> raw, String pkg) {
        MacOSBuildHints h = new MacOSBuildHints();
        h.parse(new MacOSBuildHints.HintSource() {
            @Override
            public String get(String key, String defaultValue) {
                String v = raw.get(key);
                return v != null ? v : defaultValue;
            }
        }, pkg);
        return h;
    }

    private static Map<String, String> raw(String... kv) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    // ---- Info.plist ------------------------------------------------------

    @Test
    public void infoPlistIsAppKitAndCarriesNoUIKitKeys() {
        MacOSBuildHints h = hints(raw(), "com.example.app");
        Map<String, Object> plist = MacOSXcodeProject.infoPlist("My App", "com.example.app.mac",
                "1.0", "3", h);

        assertEquals("NSApplication", plist.get("NSPrincipalClass"));
        assertEquals("APPL", plist.get("CFBundlePackageType"));
        assertEquals(Boolean.TRUE, plist.get("NSHighResolutionCapable"));
        assertEquals(MacOSBuildHints.DEFAULT_DEPLOYMENT_TARGET, plist.get("LSMinimumSystemVersion"));

        // Each of these would make the bundle an iOS app wearing a macOS SDK.
        for (String key : plist.keySet()) {
            assertFalse("no UI* key belongs in a native macOS Info.plist, found " + key,
                    key.startsWith("UI"));
        }
        // Named individually as well, because these two are the ones a reader
        // coming from the Catalyst path would expect to find and should not.
        assertNull("multiple windows are what AppKit does, not a plist opt-in",
                plist.get("UIApplicationSceneManifest"));
        assertNull("the menu bar is built in code so there is no nib to name",
                plist.get("NSMainNibFile"));
        assertNull(plist.get("LSRequiresIPhoneOS"));
    }

    @Test
    public void copyrightIsOmittedWhenUnsetRatherThanEmpty() {
        assertNull(MacOSXcodeProject.infoPlist("A", "b", "1", "1", hints(raw(), "b"))
                .get("NSHumanReadableCopyright"));
        assertEquals("(c) 2026 Example",
                MacOSXcodeProject.infoPlist("A", "b", "1", "1",
                        hints(raw("macos.copyright", "(c) 2026 Example"), "b"))
                        .get("NSHumanReadableCopyright"));
    }

    @Test
    public void plistSerializesToParseableXml() throws Exception {
        Map<String, Object> plist = MacOSXcodeProject.infoPlist("My App", "com.example.mac",
                "1.0", "3", hints(raw(), "com.example"));
        File out = File.createTempFile("cn1-macos-plist", ".plist");
        out.deleteOnExit();
        MacOSXcodeProject.writePlist(plist, out);

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setValidating(false);
        // The DOCTYPE points at apple.com; resolving it would make this test
        // depend on the network.
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        assertNotNull(f.newDocumentBuilder().parse(out));
    }

    @Test
    public void plistEscapesMarkupInValues() throws Exception {
        Map<String, Object> plist = new LinkedHashMap<String, Object>();
        plist.put("CFBundleDisplayName", "Ben & Jerry's <App>");
        File out = File.createTempFile("cn1-macos-escape", ".plist");
        out.deleteOnExit();
        MacOSXcodeProject.writePlist(plist, out);

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setValidating(false);
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        // An unescaped ampersand or angle bracket makes the whole plist
        // unparseable, and a bundle whose Info.plist will not parse does not
        // launch -- with no diagnostic that points back at the app name.
        assertNotNull(f.newDocumentBuilder().parse(out));
    }

    @Test
    public void plistInjectionReportsCollisionsRatherThanSilentlyOverriding() {
        Map<String, Object> base = MacOSXcodeProject.infoPlist("A", "b", "1", "1",
                hints(raw(), "b"));
        Map<String, Object> inject = new LinkedHashMap<String, Object>();
        inject.put("NSCameraUsageDescription", "to take photos");
        inject.put("NSPrincipalClass", "MyApplication");

        List<String> collisions = MacOSXcodeProject.mergePlist(base, inject);

        assertTrue("an app overriding NSPrincipalClass must be caught: the bundle "
                + "would launch to nothing", collisions.contains("NSPrincipalClass"));
        assertFalse("a key the generated plist does not set is not a collision",
                collisions.contains("NSCameraUsageDescription"));
        assertEquals("to take photos", base.get("NSCameraUsageDescription"));
    }

    // ---- entitlements ----------------------------------------------------

    @Test
    public void sandboxedChannelGetsNetworkAndUserSelectedFiles() {
        Map<String, Object> ent = MacOSXcodeProject.entitlements(true, true, null, false);
        assertEquals(Boolean.TRUE, ent.get(MacOSXcodeProject.ENT_SANDBOX));
        // Neither can be asked for at runtime, so a sandboxed build without them
        // fails the first time it opens a socket or a file dialog.
        assertEquals(Boolean.TRUE, ent.get(MacOSXcodeProject.ENT_NETWORK_CLIENT));
        assertEquals(Boolean.TRUE, ent.get(MacOSXcodeProject.ENT_FILES_USER_SELECTED));
    }

    @Test
    public void capabilityEntitlementsFollowWhatTheAppActuallyUses() {
        MacOSXcodeProject.MacOSCapabilities caps = new MacOSXcodeProject.MacOSCapabilities();
        caps.usesCamera = true;
        caps.usesBluetooth = true;

        Map<String, Object> ent = MacOSXcodeProject.entitlements(true, true, caps, false);
        assertEquals(Boolean.TRUE, ent.get(MacOSXcodeProject.ENT_CAMERA));
        assertEquals(Boolean.TRUE, ent.get(MacOSXcodeProject.ENT_BLUETOOTH));
        assertNull("an app that does not record audio must not ask for the microphone",
                ent.get(MacOSXcodeProject.ENT_MICROPHONE));
        assertNull(ent.get(MacOSXcodeProject.ENT_LOCATION));
    }

    @Test
    public void unsandboxedChannelHasNoSandboxEntitlements() {
        Map<String, Object> ent = MacOSXcodeProject.entitlements(false, false, null, false);
        assertNull(ent.get(MacOSXcodeProject.ENT_SANDBOX));
        assertNull(ent.get(MacOSXcodeProject.ENT_NETWORK_CLIENT));
    }

    @Test
    public void libraryValidationIsRelaxedOnlyWhenTheAppLoadsExternalCode() {
        // ParparVM output is ahead-of-time compiled, so relaxing this by default
        // would weaken the hardened runtime for every app and buy nothing.
        assertNull(MacOSXcodeProject.entitlements(false, false, null, false)
                .get(MacOSXcodeProject.ENT_DISABLE_LIBRARY_VALIDATION));
        assertEquals(Boolean.TRUE, MacOSXcodeProject.entitlements(false, false, null, true)
                .get(MacOSXcodeProject.ENT_DISABLE_LIBRARY_VALIDATION));
        // Never on the App Store channel, where it is a rejection.
        assertNull(MacOSXcodeProject.entitlements(true, true, null, true)
                .get(MacOSXcodeProject.ENT_DISABLE_LIBRARY_VALIDATION));
    }

    // ---- export options --------------------------------------------------

    @Test
    public void exportOptionsNameTheRightMethodPerChannel() {
        assertEquals("app-store",
                MacOSXcodeProject.exportOptions(true, "ABCDE12345", null).get("method"));
        assertEquals("developer-id",
                MacOSXcodeProject.exportOptions(false, "ABCDE12345", null).get("method"));
        assertEquals("ABCDE12345",
                MacOSXcodeProject.exportOptions(false, "ABCDE12345", null).get("teamID"));
    }

    // ---- fixed window size ----------------------------------------------

    @Test
    public void fixedWindowSizeParsesOrRejects() {
        assertArrayEquals2(new int[] {1024, 685}, MacOSXcodeProject.parseFixedWindowSize("1024x685"));
        assertArrayEquals2(new int[] {800, 600}, MacOSXcodeProject.parseFixedWindowSize(" 800 X 600 "));
        assertNull(MacOSXcodeProject.parseFixedWindowSize(null));
        assertNull(MacOSXcodeProject.parseFixedWindowSize("1024"));
        assertNull(MacOSXcodeProject.parseFixedWindowSize("x685"));
        assertNull(MacOSXcodeProject.parseFixedWindowSize("1024x"));
        assertNull(MacOSXcodeProject.parseFixedWindowSize("0x685"));
        assertNull(MacOSXcodeProject.parseFixedWindowSize("widexhigh"));
    }

    private static void assertArrayEquals2(int[] expected, int[] actual) {
        assertNotNull(actual);
        assertEquals(expected[0], actual[0]);
        assertEquals(expected[1], actual[1]);
    }
}
