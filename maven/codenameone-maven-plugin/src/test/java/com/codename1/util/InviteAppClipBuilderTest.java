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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The generated invite App Clip.
///
/// Everything asserted here fails silently on a device. The clip is a separate
/// binary, nothing links it to the application, and its entire job finishes
/// before anybody looks at the screen -- so a clip that records nothing looks
/// exactly like a clip that recorded something, and the symptom is an install
/// that reads as organic weeks later in a report.
class InviteAppClipBuilderTest {

    private static final String PKG = "com.example.myapp";
    private static final String GROUP = "group.com.example.myapp.cn1invite";

    private static Map<String, byte[]> files() {
        return InviteAppClipBuilder.buildFileMap(PKG, GROUP,
                "cloud.codenameone.com", "My App", "1.4", "17", "123456789");
    }

    private static String text(Map<String, byte[]> files, String name)
            throws Exception {
        byte[] content = files.get(name);
        assertNotNull(content, name + " was not generated");
        return new String(content, "UTF-8");
    }

    /// The three names the clip shares with the port's reader
    /// (`CN1InviteAppClip.m`). Nothing links the two binaries, so a rename on
    /// one side compiles, signs, ships and reads nothing.
    @Test
    void theHandoffNamesAreTheOnesTheReaderLooksFor() throws Exception {
        String src = text(files(), "CN1InviteClipDelegate.m");
        assertEquals("cn1-invite-app-clip-handoff", InviteAppClipBuilder.HANDOFF_KEY);
        assertEquals("code", InviteAppClipBuilder.CODE_FIELD);
        assertEquals("clicked", InviteAppClipBuilder.CLICKED_FIELD);
        assertTrue(src.contains("@\"cn1-invite-app-clip-handoff\""),
                "the clip must write the key the reader reads");
        assertTrue(src.contains("@\"code\":"), "the code field name");
        assertTrue(src.contains("@\"clicked\":"), "the click time field name");
    }

    /// The write has to be flushed inside the handler. A clip is terminated
    /// without notice the moment the store sheet takes over, and the write IS
    /// the attribution.
    @Test
    void theHandoffIsFlushedBeforeTheClipCanBeKilled() throws Exception {
        String src = text(files(), "CN1InviteClipDelegate.m");
        int write = src.indexOf("forKey:kHandoffKey");
        int flush = src.indexOf("[suite synchronize]");
        assertTrue(write > 0 && flush > write,
                "the shared container must be synchronized after the write");
    }

    /// A cold launch delivers the activity through continueUserActivity: and a
    /// warm one through the launch options. Reading only the first loses every
    /// second and subsequent tap, which is most of them.
    @Test
    void bothActivityDeliveryPathsAreRead() throws Exception {
        String src = text(files(), "CN1InviteClipDelegate.m");
        assertTrue(src.contains("continueUserActivity:(NSUserActivity *)userActivity"),
                "the cold-launch path");
        assertTrue(src.contains("UIApplicationLaunchOptionsUserActivityDictionaryKey"),
                "the warm-launch path");
    }

    /// All three entitlements, because each is absent in a different way. The
    /// app group is the one whose absence has no symptom at all.
    @Test
    void theClipCarriesEveryEntitlementItNeeds() throws Exception {
        String ent = text(files(), "CN1InviteClip.entitlements");
        assertTrue(ent.contains("$(AppIdentifierPrefix)com.example.myapp"),
                "parent application identifier");
        assertTrue(ent.contains("appclips:cloud.codenameone.com"),
                "the associated domain iOS offers the clip for");
        assertTrue(ent.contains(GROUP), "the shared app group");
    }

    /// Archive validation rejects the whole application when an embedded
    /// bundle's versions differ from the host's.
    @Test
    void theVersionsMatchTheHostApplication() throws Exception {
        String plist = text(files(), "Info.plist");
        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>\n  <string>1.4</string>"),
                plist);
        assertTrue(plist.contains("<key>CFBundleVersion</key>\n  <string>17</string>"),
                plist);
        assertTrue(plist.contains("<key>NSAppClip</key>"), "the clip marker");
    }

    /// Apple requires the clip's bundle identifier to extend the
    /// application's; an unrelated one is not recognised as its clip.
    @Test
    void theBundleIdExtendsTheApplications() {
        assertTrue(InviteAppClipBuilder.bundleId(PKG).startsWith(PKG + "."),
                InviteAppClipBuilder.bundleId(PKG));
    }

    /// Derived per application. A constant group would be shared by every
    /// Codename One app on one developer account, and each could read the
    /// others' invite codes.
    @Test
    void theDefaultAppGroupIsPerApplication() {
        assertFalse(InviteAppClipBuilder.defaultAppGroup("com.example.a")
                        .equals(InviteAppClipBuilder.defaultAppGroup("com.example.b")),
                "two applications must not share a container");
        assertTrue(InviteAppClipBuilder.defaultAppGroup(PKG).startsWith("group."),
                "an app group identifier must start with group.");
    }

    /// A build before the first release has no store id, and that must cost
    /// the attribution nothing: the code is still recorded, only the install
    /// sheet is absent.
    @Test
    void noStoreIdStillRecordsTheCode() throws Exception {
        Map<String, byte[]> f = InviteAppClipBuilder.buildFileMap(PKG, GROUP,
                "cloud.codenameone.com", "My App", "1.4", "17", "");
        String src = text(f, "CN1InviteClipDelegate.m");
        assertTrue(src.contains("kStoreItemId.length == 0"),
                "the overlay must be skipped, not the recording");
        int guard = src.indexOf("kStoreItemId.length == 0");
        int record = src.indexOf("- (void)recordInviteFromURL:");
        assertTrue(record < guard, "recording must not sit behind the store-id guard");
    }

    /// The url is somebody else's input and the code taken out of it is what
    /// the application claims with, so the grammar is enforced where it is
    /// known rather than trusted downstream.
    @Test
    void theCodeIsConstrainedBeforeItIsStored() throws Exception {
        String src = text(files(), "CN1InviteClipDelegate.m");
        assertTrue(src.contains("cn1InviteCodeIsWellFormed"), "a grammar check exists");
        int check = src.indexOf("if (!cn1InviteCodeIsWellFormed(code)) { return; }");
        int store = src.indexOf("forKey:kHandoffKey");
        assertTrue(check > 0 && check < store,
                "the check must precede the write, not follow it");
    }

    /// A display name is the developer's text and is interpolated into an
    /// Objective-C string literal. A quotation mark in it would end the
    /// literal and leave the rest as code.
    @Test
    void aQuotedDisplayNameCannotEscapeItsLiteral() {
        assertEquals("Bob\\\"s \\\\ App",
                InviteAppClipBuilder.escapeObjC("Bob\"s \\ App"));
        assertEquals("one two", InviteAppClipBuilder.escapeObjC("one\ntwo"));
    }
    /// The build hint documentation is part of the contract here, because the
    /// two halves it governs are easy to conflate: the value of
    /// ios.invite.appClip decides whether a clip is GENERATED, and never
    /// whether a handoff is read. A developer shipping their own clip turns
    /// generation off and still needs the app group, the native reader and the
    /// registration -- without them their clip writes the documented handoff
    /// into the documented container and nothing ever looks.
    @Test
    void theClipNameAndSuffixAreTheOnesTheServerAuthorises() {
        // BuildCloud names each clip <team>.<bundle>.Clip in the association
        // document, from its own copy of this suffix. The two repositories
        // share no code, so a rename here is not a compile error there -- it
        // is a clip iOS is never offered, with nothing reporting why.
        assertEquals(".Clip", InviteAppClipBuilder.bundleId("x").substring(1));
        assertEquals("CN1InviteClip", InviteAppClipBuilder.CLIP_NAME);
    }

    /**
     * The clip holds a code to the same grammar the core parser does.
     *
     * <p>It used to take any 1-64 url-safe characters. The clip has ONE handoff
     * slot, so a second invocation carrying a malformed segment overwrote a
     * valid invite already recorded, and the full app then persisted and
     * claimed the malformed one -- settling the install as no-match with the
     * real invite gone. The two halves have to agree because one writes what
     * the other reads.</p>
     */
    @Test
    void theClipEnforcesTheCoreCodeLength() throws Exception {
        String src = text(files(), "CN1InviteClipDelegate.m");

        assertTrue(src.contains("code.length != " + InviteAppClipBuilder.CODE_CHARS),
                "the clip does not require the core's exact code length, so it can "
                        + "overwrite a valid handoff with a malformed code");
        assertFalse(src.contains("code.length > 64"),
                "the clip still accepts a length RANGE, which is not the core's grammar");
        assertEquals(22, InviteAppClipBuilder.CODE_CHARS,
                "CODE_CHARS drifted from Invites.CODE_CHARS, which the clip must match");
    }
}
