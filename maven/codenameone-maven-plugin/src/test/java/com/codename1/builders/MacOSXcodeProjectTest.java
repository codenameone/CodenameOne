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
import java.util.Arrays;
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
    public void pushRegistrationEarnsTheApnsEntitlementOnBothChannels() {
        MacOSXcodeProject.MacOSCapabilities caps = new MacOSXcodeProject.MacOSCapabilities();
        caps.usesPush = true;

        // The macOS spelling of the key, which is not the iOS one -- Xcode's own
        // macOS capability template writes com.apple.developer.aps-environment
        // where the iOS template beside it writes a bare aps-environment, and a
        // build carrying the wrong one does not match its provisioning profile.
        assertEquals("com.apple.developer.aps-environment",
                MacOSXcodeProject.ENT_APS_ENVIRONMENT);

        // Both channels, because APNs is not a sandbox permission:
        // macOS refuses registerForRemoteNotifications for any signed executable
        // that does not carry it, and this build hands codesign an explicit
        // entitlements file -- so what is missing from the file is missing from
        // the signature however the provisioning profile is configured.
        assertEquals("production", MacOSXcodeProject.entitlements(true, true, caps, false)
                .get(MacOSXcodeProject.ENT_APS_ENVIRONMENT));
        assertEquals("production", MacOSXcodeProject.entitlements(false, false, caps, false)
                .get(MacOSXcodeProject.ENT_APS_ENVIRONMENT));

        // And not for an app that never registers -- an unused APNs entitlement
        // on an App Store build is a submission the reviewer asks about.
        assertNull(MacOSXcodeProject.entitlements(true, true,
                new MacOSXcodeProject.MacOSCapabilities(), false)
                .get(MacOSXcodeProject.ENT_APS_ENVIRONMENT));
        assertNull(MacOSXcodeProject.entitlements(true, true, null, false)
                .get(MacOSXcodeProject.ENT_APS_ENVIRONMENT));
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

    /**
     * The JIT keys the Mac Catalyst target has always written, so the two Mac
     * targets produce the same entitlements for the same hints. The resolved
     * value was being stored and never read, which left
     * macos.entitlements.hardenedRuntime with no effect on this target at all.
     */
    @Test
    public void hardenedRuntimeWritesTheJitDenialTheCatalystTargetWrites() {
        // Developer ID: hardened, not sandboxed, JIT not asked for.
        Map<String, Object> ent = MacOSXcodeProject.entitlements(false, false, null, false);
        assertEquals(Boolean.FALSE, ent.get(MacOSXcodeProject.ENT_ALLOW_JIT));
        assertEquals(Boolean.FALSE, ent.get(MacOSXcodeProject.ENT_ALLOW_UNSIGNED_MEMORY));

        // Asking for JIT wins over the denial rather than contradicting it.
        MacOSBuildHints.EntitlementOverrides jit = new MacOSBuildHints.EntitlementOverrides(
                false, true, MacOSBuildHints.EntitlementOverrides.UNSET, "readwrite",
                true, true, null,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET);
        Map<String, Object> withJit = MacOSXcodeProject.entitlements(false, jit, null, false);
        assertEquals(Boolean.TRUE, withJit.get(MacOSXcodeProject.ENT_ALLOW_JIT));
        assertEquals(Boolean.TRUE, withJit.get(MacOSXcodeProject.ENT_ALLOW_UNSIGNED_MEMORY));

        // Turned off, neither key is written -- which is what the hint is for.
        MacOSBuildHints.EntitlementOverrides off = new MacOSBuildHints.EntitlementOverrides(
                false, true, MacOSBuildHints.EntitlementOverrides.UNSET, "readwrite",
                false, false, null,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET);
        Map<String, Object> without = MacOSXcodeProject.entitlements(false, off, null, false);
        assertNull(without.get(MacOSXcodeProject.ENT_ALLOW_JIT));
        assertNull(without.get(MacOSXcodeProject.ENT_ALLOW_UNSIGNED_MEMORY));
    }

    /**
     * Downloads is a wider grant than the files the user picks in a panel, so
     * it is not part of files.userSelected and is not on by default.
     */
    @Test
    public void downloadsAccessIsItsOwnOptInRatherThanPartOfUserSelected() {
        Map<String, Object> ent = MacOSXcodeProject.entitlements(true, true, null, false);
        assertEquals(Boolean.TRUE, ent.get(MacOSXcodeProject.ENT_FILES_USER_SELECTED));
        assertNull("readwrite user-selected access must not carry the Downloads folder",
                ent.get(MacOSXcodeProject.ENT_FILES_DOWNLOADS));

        MacOSBuildHints.EntitlementOverrides on = new MacOSBuildHints.EntitlementOverrides(
                true, true, MacOSBuildHints.EntitlementOverrides.UNSET, "readwrite",
                false, false, null,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.UNSET,
                MacOSBuildHints.EntitlementOverrides.ON,
                MacOSBuildHints.EntitlementOverrides.UNSET);
        assertEquals(Boolean.TRUE, MacOSXcodeProject.entitlements(true, on, null, false)
                .get(MacOSXcodeProject.ENT_FILES_DOWNLOADS));
    }

    /**
     * plistInject carries raw XML in every place it is documented -- both
     * ios.plistInject and desktop.mac.plistInject -- and the raw form is the
     * only one that can express a dict or an array. The builder recognises it
     * and names the keys it is about to replace, because a generated key left
     * duplicated is not a valid plist.
     */
    @Test
    public void rawPlistFragmentsAreRecognisedAndTheirKeysNamed() {
        String xml = "<key>CFBundleDocumentTypes</key>\n<array><dict>"
                + "<key>CFBundleTypeName</key><string>Text</string></dict></array>\n"
                + "<key>NSAppTransportSecurity</key><dict/>";
        assertTrue(MacOSXcodeProject.isRawPlistFragment(xml));
        // ROOT members only. CFBundleTypeName lives inside the fragment's own
        // array/dict, so it replaces nothing at the top level -- this assertion
        // used to expect it, and what that pinned was the caller deleting a
        // generated top-level value the fragment never supplied. <dict/> is
        // self-closing and must not open a level, or NSAppTransportSecurity
        // after it would be read as nested.
        assertEquals(Arrays.asList("CFBundleDocumentTypes", "NSAppTransportSecurity"),
                MacOSXcodeProject.injectedPlistKeys(xml));

        // The shorthand keeps working and is not mistaken for XML.
        assertFalse(MacOSXcodeProject.isRawPlistFragment("CFBundleName=Thing"));
        assertTrue(MacOSXcodeProject.injectedPlistKeys("CFBundleName=Thing").isEmpty());

        // A scan, not a parser: an unterminated key ends the scan rather than
        // throwing, and null is simply nothing.
        assertEquals(Arrays.asList("A"),
                MacOSXcodeProject.injectedPlistKeys("<key>A</key><key>B"));
        assertTrue(MacOSXcodeProject.injectedPlistKeys(null).isEmpty());
        assertFalse(MacOSXcodeProject.isRawPlistFragment(null));
    }

    /**
     * A key the fragment does not actually declare at its root must not delete
     * the generated one.
     */
    @Test
    public void nestedAndCommentedKeysDoNotCountAsOverrides() {
        // Nested: a URL type whose sub-dictionary names CFBundleIdentifier. The
        // fragment adds a URL type; it does not replace the bundle identifier,
        // and removing the generated one leaves a bundle that cannot be signed.
        String nested = "<key>CFBundleURLTypes</key><array><dict>"
                + "<key>CFBundleIdentifier</key><string>com.example.url</string>"
                + "</dict></array>";
        assertEquals(Arrays.asList("CFBundleURLTypes"),
                MacOSXcodeProject.injectedPlistKeys(nested));

        // Commented out: the one form that is unmistakably not in effect.
        String commented = "<!-- <key>CFBundleIdentifier</key><string>x</string> -->"
                + "<key>CFBundleName</key><string>Thing</string>";
        assertEquals(Arrays.asList("CFBundleName"),
                MacOSXcodeProject.injectedPlistKeys(commented));

        // An unterminated comment swallows the rest, which is what a parser
        // would conclude too.
        assertTrue(MacOSXcodeProject.injectedPlistKeys(
                "<!-- <key>CFBundleName</key>").isEmpty());

        // Depth is restored on the way out, so a root key after a nested block
        // is still a root key.
        String after = "<key>A</key><array><dict><key>Inner</key><string>v</string></dict></array>"
                + "<key>B</key><string>v</string>";
        assertEquals(Arrays.asList("A", "B"), MacOSXcodeProject.injectedPlistKeys(after));
    }

    /** The raw fragment reaches the file verbatim, inside the dict. */
    @Test
    public void rawPlistFragmentIsWrittenIntoTheDict() throws Exception {
        Map<String, Object> plist = new LinkedHashMap<String, Object>();
        plist.put("CFBundleName", "Thing");
        File out = File.createTempFile("cn1-plist", ".plist");
        out.deleteOnExit();
        MacOSXcodeProject.writePlist(plist, "<key>NSAppTransportSecurity</key><dict/>", out);
        String written = new String(java.nio.file.Files.readAllBytes(out.toPath()), "UTF-8");
        assertTrue(written.contains("<key>NSAppTransportSecurity</key><dict/>"));
        assertTrue(written.indexOf("<key>NSAppTransportSecurity</key>")
                > written.indexOf("<key>CFBundleName</key>"));
        assertTrue(written.trim().endsWith("</plist>"));
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

    /**
     * The calendars entitlement without its usage descriptions is a capability
     * the signature grants and macOS kills the process for using: EventKit is
     * TCC-gated, and a bundle with no description gets no prompt and no
     * catchable error.
     */
    @Test
    public void calendarEntitlementBringsItsUsageDescriptions() {
        Map<String, Object> none = MacOSXcodeProject.privacyUsageDescriptions(
                new MacOSXcodeProject.MacOSCapabilities(), false, null);
        assertFalse(none.containsKey("NSCalendarsFullAccessUsageDescription"));

        Map<String, Object> granted = MacOSXcodeProject.privacyUsageDescriptions(
                new MacOSXcodeProject.MacOSCapabilities(), true, null);
        // Both halves of the macOS 14 split, because either API can be the first
        // one the application reaches.
        assertTrue(granted.containsKey("NSCalendarsFullAccessUsageDescription"));
        assertTrue(granted.containsKey("NSCalendarsWriteOnlyAccessUsageDescription"));
        assertTrue(granted.containsKey("NSRemindersFullAccessUsageDescription"));
        // And the pre-14 spellings: the deployment floor is 11.0, and macOS 11
        // through 13 read only these.
        assertTrue(granted.containsKey("NSCalendarsUsageDescription"));
        assertTrue(granted.containsKey("NSRemindersUsageDescription"));
    }

    /** The application's own wording wins over the generated floor. */
    @Test
    public void suppliedUsageDescriptionWinsOverTheDefault() {
        Map<String, Object> out = MacOSXcodeProject.privacyUsageDescriptions(
                new MacOSXcodeProject.MacOSCapabilities(), true,
                new MacOSXcodeProject.UsageDescriptionResolver() {
                    @Override
                    public String get(String key) {
                        return "NSRemindersFullAccessUsageDescription".equals(key)
                                ? "we schedule your workouts" : null;
                    }
                });
        assertEquals("we schedule your workouts", out.get("NSRemindersFullAccessUsageDescription"));
        assertTrue(((String) out.get("NSCalendarsFullAccessUsageDescription")).length() > 0);
    }

    /**
     * A deep link reaches the app only if the bundle claims the scheme. The stub
     * implements shouldApplicationHandleURL either way, so without
     * CFBundleURLTypes the callback simply never fires and there is nothing to
     * see.
     */
    @Test
    public void urlSchemesAreLiftedOutOfTheIosHintMarkup() {
        // The iOS hint carries raw plist markup, because that is what the iOS
        // path pastes straight into its template.
        List<Object> types = MacOSXcodeProject.urlTypes("com.example.app",
                "<string>myapp</string><string>myapp-alt</string>");
        assertNotNull(types);
        assertEquals(1, types.size());
        Map<?, ?> type = (Map<?, ?>) types.get(0);
        assertEquals("com.example.app", type.get("CFBundleURLName"));
        assertEquals(Arrays.asList("myapp", "myapp-alt"), type.get("CFBundleURLSchemes"));
    }

    /** A hand-written hint with no markup is one scheme, not a parse failure. */
    @Test
    public void aBareSchemeNameIsAccepted() {
        List<Object> types = MacOSXcodeProject.urlTypes("com.example.app", "myapp");
        assertNotNull(types);
        assertEquals(Arrays.asList("myapp"),
                ((Map<?, ?>) types.get(0)).get("CFBundleURLSchemes"));
    }

    /** No hint means no key at all, rather than an empty array Xcode complains about. */
    @Test
    public void noSchemesMeansNoKey() {
        assertNull(MacOSXcodeProject.urlTypes("com.example.app", null));
        assertNull(MacOSXcodeProject.urlTypes("com.example.app", "   "));
        assertNull(MacOSXcodeProject.urlTypes("com.example.app", "<string></string>"));
    }
}
