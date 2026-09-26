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
package com.codename1.certificatewizard;

import com.codename1.certificatewizard.api.SigningState;
import com.codename1.certificatewizard.project.SigningAssetInstaller;
import com.codename1.ui.Display;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What the wizard writes into the project when it installs a signing pair: the App ID the
/// build signs as (issue #5901) and the file each certificate lands in (issue #5834).
class SigningInstallIdentityTest {
    @BeforeAll
    static void initDisplay() {
        if (Display.getInstance() == null || !Display.isInitialized()) {
            Display.init(null);
        }
    }

    /// The shape of a real profile: an XML plist inside a binary CMS envelope.
    private static Path profile(String team) throws Exception {
        String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plist version=\"1.0\"><dict>\n"
                + "<key>Name</key><string>Demo STORE</string>\n"
                + "<key>Entitlements</key><dict><key>application-identifier</key><string>" + team
                + ".com.example.demo</string></dict>\n"
                + "<key>TeamIdentifier</key>\n<array>\n\t<string>" + team + "</string>\n</array>\n"
                + "<key>TeamName</key><string>Example</string>\n</dict></plist>";
        byte[] payload = plist.getBytes(StandardCharsets.UTF_8);
        byte[] wrapped = new byte[payload.length + 32];
        for (int i = 0; i < 16; i++) {
            wrapped[i] = (byte) (0x80 + i);
        }
        System.arraycopy(payload, 0, wrapped, 16, payload.length);
        Path f = Files.createTempFile("cn1-profile", ".mobileprovision");
        Files.write(f, wrapped);
        return f;
    }

    private static Path settings(String text) throws Exception {
        Path settings = Files.createTempFile("cn1-settings", ".properties");
        Files.writeString(settings, text, StandardCharsets.UTF_8);
        return settings;
    }

    @Test
    void teamIdentifierIsReadFromTheProfileEnvelope() throws Exception {
        assertEquals("A1B2C3D4E5",
                SigningAssetInstaller.profileTeamIdentifier(Files.readAllBytes(profile("A1B2C3D4E5"))));
        assertNull(SigningAssetInstaller.profileTeamIdentifier(null));
        assertNull(SigningAssetInstaller.profileTeamIdentifier("no plist here".getBytes(StandardCharsets.UTF_8)));
        assertNull(SigningAssetInstaller.profileTeamIdentifier(
                "<key>TeamIdentifier</key><array><string>not a team</string></array>"
                        .getBytes(StandardCharsets.UTF_8)));
        assertNull(SigningAssetInstaller.profileTeamIdentifier(
                "<key>TeamIdentifier</key><array></array><key>X</key><string>A1B2C3D4E5</string>"
                        .getBytes(StandardCharsets.UTF_8)),
                "a string after the array closes is some other key's value");
    }

    /// Issue #5901: the template's placeholder team, Q5GHSKAL2F, survived every wizard install,
    /// and the server writes it verbatim into the entitlements Xcode then refuses to sign.
    @Test
    void installingAProfileRewritesThePlaceholderAppIdToTheProfilesTeam() throws Exception {
        Path settings = settings("codename1.packageName=com.example.demo\n"
                + "codename1.ios.appid=Q5GHSKAL2F.com.example.demo\n");
        SigningAssetInstaller.applyReleaseCertificate(settings.toString(), "/tmp/dist.p12", "pw",
                profile("A1B2C3D4E5").toString());
        String written = Files.readString(settings, StandardCharsets.UTF_8);
        assertTrue(written.contains("codename1.ios.appid=A1B2C3D4E5.com.example.demo\n"), written);
        assertFalse(written.contains("Q5GHSKAL2F"), written);

        Path debug = settings("codename1.packageName=com.example.demo\n");
        SigningAssetInstaller.applyDebugCertificate(debug.toString(), "/tmp/dev.p12", "pw",
                profile("A1B2C3D4E5").toString());
        assertTrue(Files.readString(debug, StandardCharsets.UTF_8)
                .contains("codename1.ios.appid=A1B2C3D4E5.com.example.demo\n"));
    }

    /// With no team or no package name there is no correct value to write, so none is written.
    @Test
    void appIdIsLeftAloneWhenItCannotBeDetermined() throws Exception {
        Path settings = settings("codename1.packageName=com.example.demo\n"
                + "codename1.ios.appid=Q5GHSKAL2F.com.example.demo\n");
        SigningAssetInstaller.applyReleaseCertificate(settings.toString(), "/tmp/dist.p12", "pw",
                "/tmp/does-not-exist.mobileprovision");
        assertTrue(Files.readString(settings, StandardCharsets.UTF_8)
                .contains("codename1.ios.appid=Q5GHSKAL2F.com.example.demo\n"));

        Path noPackage = settings("codename1.ios.appid=Q5GHSKAL2F.com.example.demo\n");
        SigningAssetInstaller.applyReleaseCertificate(noPackage.toString(), "/tmp/dist.p12", "pw",
                profile("A1B2C3D4E5").toString());
        assertTrue(Files.readString(noPackage, StandardCharsets.UTF_8)
                .contains("codename1.ios.appid=Q5GHSKAL2F.com.example.demo\n"));
    }

    /// Issue #5834: a development and a distribution certificate with no display name were both
    /// written to ios-signing-asset.p12, each install with a fresh password, so one of the two
    /// slots named a file its password could no longer open.
    @Test
    void certificatesNeverShareAP12File() {
        SigningState.Certificate dev = new SigningState.Certificate(1L, "DEV111", "IOS_DEVELOPMENT",
                "", "S1", null, "ACTIVE", true);
        SigningState.Certificate dist = new SigningState.Certificate(2L, "DIST222", "IOS_DISTRIBUTION",
                null, "S2", null, "ACTIVE", true);
        assertNotEquals(CertificateWizard.p12FileName(dev), CertificateWizard.p12FileName(dist));

        SigningState.Certificate sameNameA = new SigningState.Certificate(3L, "AAA", "IOS_DISTRIBUTION",
                "iOS Distribution", "S3", null, "ACTIVE", true);
        SigningState.Certificate sameNameB = new SigningState.Certificate(4L, "BBB", "IOS_DISTRIBUTION",
                "iOS Distribution", "S4", null, "ACTIVE", true);
        assertNotEquals(CertificateWizard.p12FileName(sameNameA), CertificateWizard.p12FileName(sameNameB));

        SigningState.Certificate noAppleId = new SigningState.Certificate(5L, null, "IOS_DISTRIBUTION",
                "iOS Distribution", "S5", null, "ACTIVE", true);
        assertEquals("iOS-Distribution-5.p12", CertificateWizard.p12FileName(noAppleId));
        assertEquals("iOS-Distribution-AAA.p12", CertificateWizard.p12FileName(sameNameA));
    }
}
