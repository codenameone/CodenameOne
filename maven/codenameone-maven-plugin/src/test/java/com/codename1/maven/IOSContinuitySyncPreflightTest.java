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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An app that uses {@code com.codename1.continuity.sync} asks for the iCloud key-value store
 * entitlement, and Apple grants that only through an App ID with the iCloud capability enabled.
 *
 * <p>A profile issued before that was switched on matches the bundle id perfectly and authorizes
 * none of it, so the build runs all the way to codesign and fails there -- talking about an
 * entitlement rather than about the capability nobody enabled. Everything needed to say so is on
 * disk before the build is sent.</p>
 *
 * <p>Warned about rather than refused, unlike the App Group checks beside it: this entitlement has
 * a documented opt-out ({@code ios.continuity.sync=false}) that leaves the app working, so naming
 * the two ways out is more useful than a refusal.</p>
 */
public class IOSContinuitySyncPreflightTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
            + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\"><dict>\n";

    /// @param ubiquity true for a profile issued from an App ID with iCloud enabled
    private File profile(String name, boolean ubiquity) throws Exception {
        String kvStore = ubiquity
                ? "<key>com.apple.developer.ubiquity-kvstore-identifier</key>"
                        + "<string>ABCD1234.com.example.app</string>"
                : "";
        String plist = HEAD
                + "<key>Name</key><string>" + name + "</string>\n"
                + "<key>UUID</key><string>0f7ac3c1-4d0e-4e8a-9d1f-8b6a2c5e7d90</string>\n"
                + "<key>ExpirationDate</key><date>2099-01-01T00:00:00Z</date>\n"
                + "<key>DeveloperCertificates</key><array><data>Zm9v</data></array>\n"
                + "<key>Entitlements</key><dict>"
                + "<key>application-identifier</key>"
                + "<string>ABCD1234.com.example.app</string>"
                + kvStore
                + "<key>get-task-allow</key><false/></dict>\n"
                + "</dict></plist>";
        byte[] payload = plist.getBytes("UTF-8");
        // The parser skips a binary preamble, exactly as a real signed profile carries one.
        byte[] wrapped = new byte[payload.length + 24];
        for (int i = 0; i < 16; i++) {
            wrapped[i] = (byte) (0x80 + i);
        }
        System.arraycopy(payload, 0, wrapped, 16, payload.length);
        File f = tmp.newFile(name + ".mobileprovision");
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(wrapped);
        } finally {
            out.close();
        }
        return f;
    }

    private Properties settings(File appProfile) throws Exception {
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                appProfile.getAbsolutePath());
        p.setProperty("codename1.arg.ios.continuity.sync", "true");
        return p;
    }

    private static List<IOSProvisioningPreflight.Problem> check(Properties p) {
        return IOSProvisioningPreflight.checkContinuitySync(p, true);
    }

    @Test
    public void aProfileWithoutTheEntitlementIsWarnedAbout() throws Exception {
        List<IOSProvisioningPreflight.Problem> problems = check(settings(profile("NoCloud", false)));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).message.contains("ubiquity-kvstore-identifier"));
        // Both ways out are named, because either is a legitimate answer.
        assertTrue(problems.get(0).message.contains("iCloud"));
        assertTrue(problems.get(0).message.contains("ios.continuity.sync=false"));
        assertFalse("a documented opt-out exists, so this must not refuse the build",
                problems.get(0).fatal);
    }

    @Test
    public void aProfileWithTheEntitlementPassesQuietly() throws Exception {
        assertTrue(check(settings(profile("WithCloud", true))).isEmpty());
    }

    @Test
    public void theOptOutSkipsTheCheckEntirely() throws Exception {
        Properties p = settings(profile("NoCloud", false));
        p.setProperty("codename1.arg.ios.continuity.sync", "false");

        assertTrue(check(p).isEmpty());
    }

    /**
     * A project that has not declared the synced store is not checked. The builder decides that
     * from bytecode, which this cannot read.
     */
    @Test
    public void aProjectThatDeclaresNoSyncedStoreIsNotChecked() throws Exception {
        Properties p = settings(profile("NoCloud", false));
        p.remove("codename1.arg.ios.continuity.sync");

        assertTrue(check(p).isEmpty());
    }

    /**
     * The false warning this check used to produce. A project that uses continuity but NOT the
     * synced store gets no entitlement from the builder, so warning that its profile cannot sign
     * one told it to enable an iCloud capability it does not need.
     */
    @Test
    public void aContinuityOnlyProjectIsNotWarnedAboutICloud() throws Exception {
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                profile("NoCloud", false).getAbsolutePath());

        assertTrue(check(p).isEmpty());
    }

    /**
     * An app sharing a store with a sibling names that sibling's container. WHICH container a
     * profile grants is not a question this can answer from the key alone, so a profile that
     * grants the capability is left alone.
     */
    @Test
    public void anExplicitContainerOnAGrantingProfileIsLeftAlone() throws Exception {
        Properties p = settings(profile("WithCloud", true));
        p.setProperty("codename1.arg.ios.entitlements.com.apple.developer"
                + ".ubiquity-kvstore-identifier", "ABCD1234.com.example.shared");

        assertTrue(check(p).isEmpty());
    }

    /**
     * But a profile that grants NO key-value store at all is answerable, and naming a container
     * does not rescue it: the builder puts the entitlement in either way and codesigning rejects
     * it. This returned early on the override and suppressed the one warning it can give for
     * certain -- the unanswerable question is which container, not whether there is one.
     */
    @Test
    public void anExplicitContainerStillWarnsWhenTheProfileGrantsNothing() throws Exception {
        Properties p = settings(profile("NoCloud", false));
        p.setProperty("codename1.arg.ios.entitlements.com.apple.developer"
                + ".ubiquity-kvstore-identifier", "ABCD1234.com.example.shared");

        List<IOSProvisioningPreflight.Problem> problems = check(p);

        assertEquals(String.valueOf(problems), 1, problems.size());
        assertTrue("the warning does not name the container the project asked for: "
                        + problems.get(0).message,
                problems.get(0).message.contains("ABCD1234.com.example.shared"));
    }

    /** No readable profile is reported by check(), and is not something to warn about twice. */
    @Test
    public void anUnreadableProfileIsLeftToTheOtherChecks() throws Exception {
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty("codename1.arg.ios.continuity.sync", "true");
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                "/nowhere/missing.mobileprovision");

        assertTrue(check(p).isEmpty());
    }

    @Test
    public void nullSettingsProduceNoProblems() {
        assertTrue(IOSProvisioningPreflight.checkContinuitySync(null, true).isEmpty());
    }
}
